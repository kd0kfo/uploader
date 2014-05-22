package com.davecoss.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import com.davecoss.java.Logger;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;

import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class WebFS {
	static Logger L = Logger.getInstance();
	
	public static final String URL_ENCODE_TYPE = "UTF-8";
	
	private URI baseURI = null;
	private JSONObject serverInfo = null;
	private long timeOffset = 0;
	private HTTPSClient client = null;
	private Credentials credentials = null;
	private byte[] signingkey = null;
	private int uploadBufferSize;
	
	public WebFS(HTTPSClient client) {
		this.client = client;
		String bufferSize = System.getProperty(UploadOutputStream.PROPERTY_BUFFER_SIZE, String.valueOf(UploadOutputStream.DEFAULT_BUFFER_SIZE));
		try {
			uploadBufferSize = Integer.parseInt(bufferSize);
		} catch(NumberFormatException nfe) {
			L.error("Invalid buffer size: ");
			L.error(bufferSize);
			L.error("Using default buffer size:");
			L.error(Integer.toString(UploadOutputStream.DEFAULT_BUFFER_SIZE));
			uploadBufferSize = UploadOutputStream.DEFAULT_BUFFER_SIZE;
		}
	}
	
	public HTTPSClient getClient() {
		return client;
	}

	public void setClient(HTTPSClient client) {
		this.client = client;
	}
	
	public URI getBaseURI() {
		return baseURI;
	}

	public void setBaseURI(URI baseURI) {
		this.baseURI = baseURI;
	}

	public JSONObject getServerInfo() {
		return serverInfo;
	}
	
	public static String parseServerInfo(JSONObject serverInfo) {
		String retval = "";
		@SuppressWarnings("unchecked")
		Iterator<Object> it = serverInfo.keySet().iterator();
		String key = null;
		while(it.hasNext()) {
			key = (String)it.next();
			retval += String.format("%s: %s\n", key, (String)serverInfo.get(key));
		}
		return retval;
	}

	public void setServerInfo(JSONObject serverInfo) {
		this.serverInfo = serverInfo;
	}

	public void close() throws IOException {
		if(client != null)
			client.close();
		client = null;
	}
	
	public void downloadConfig() throws IOException {
		this.serverInfo = client.jsonGet(baseURI.toString() + "/info.php");
		if(this.serverInfo.containsKey("time")) {
			long now = getCurrentTime();
			this.timeOffset = ((Long)this.serverInfo.get("time")) - now;
		} else {
			this.timeOffset = 0;
		}
	}
	
	public WebResponse downloadFile(String source, File dest) throws IOException {
		if(dest == null)
		{
			// if Dest is null, simply use the name of the source file.
			dest = new File(source);
			dest = new File(dest.getName());
		}
		
		L.debug("Downloading " + source);
		// Get info for file and verify that it is a file.
		DownloadInputStream input = null;
		FileOutputStream output = null;
		try {
			AuthHash signature = signData(source);
			String sourceURL = String.format("%s/stream.php?filename=%s&signature=%s&username=%s",
					baseURI.toString(), urlEncode(source), signature.toURLEncoded(), urlEncode(credentials.getUsername()));
			System.out.println("Path: " + sourceURL); // TODO: replace with Logger
			
			// Download content.
			input = new DownloadInputStream(client, sourceURL);
			
			// Write content to file
			output = new FileOutputStream(dest);
			byte[] buffer = new byte[4096];
			int amountRead = -1;
			while((amountRead = input.read(buffer, 0, 4096)) != -1)
				output.write(buffer,0, amountRead);
		} catch (AuthHash.HashException ahhe) {
			String msg = "Error generating signature";
			L.error(msg, ahhe);
			throw new IOException(msg, ahhe);
		} finally {
			// Cleanup
			if(input != null)
				input.close();
			if(output != null)
				output.close();
		}
		return new WebResponse(0, String.format("Saved %s as %s", source, dest.getName()));
	}

	public WebResponse putFile(File file) throws IOException, AuthHash.HashException {
		if(!file.exists())
			return new WebResponse(1, "File not found: " + file.getPath());
		
		AuthHash signature = signData("upload");
		String postURL = String.format("%s/upload.php?username=%s&signature=%s",
				baseURI.toString(), urlEncode(credentials.getUsername()), signature.toURLEncoded());
		return client.postFile(postURL, file);
	}
	
	public WebResponse postStream(InputStream input, String filename) throws IOException, AuthHash.HashException {
		return postStream(input, filename, false);
	}
	
	public WebResponse postStream(InputStream input, String filename, boolean useBase64) throws IOException, AuthHash.HashException {
		AuthHash signature = signData(filename);
		UploadOutputStream postStream = openUploadStream(filename);
		OutputStream output = postStream;
		if(useBase64) {
			output = client.getEncoder().encodeOutputStream(postStream);
		}
		
		try {
			byte[] buffer = new byte[2048];
			int amountRead = -1;
			while((amountRead = input.read(buffer)) != -1)
				output.write(buffer, 0, amountRead);
		} finally {
			output.close();
		}
		
		return postStream.getUploadResponse();
	}
	
	public DownloadInputStream openDownloadStream(WebFile webfile) throws IOException {
		return openDownloadStream(webfile.getAbsolutePath());
	}
	
	public DownloadInputStream openDownloadStream(String path) throws IOException {
		AuthHash signature = null;
		try {
			signature = signData(path);
		} catch (AuthHash.HashException ahhe) {
			String msg = "Error generating signature for " + path;
			L.error(msg, ahhe);
			throw new IOException(msg, ahhe);
		}
		String uri = String.format("%s/stream.php?username=%s&signature=%s&filename=%s",
				baseURI.toString(), urlEncode(credentials.getUsername()), signature.toURLEncoded(), path);
		return new DownloadInputStream(client, uri);
	}
	
	public UploadOutputStream openUploadStream(String filename) throws IOException {
		AuthHash signature = null;
		try {
			signature = signData("upload");
		} catch (AuthHash.HashException ahhe) {
			String msg = "Error generating signature for " + filename;
			L.error(msg, ahhe);
			throw new IOException(msg, ahhe);
		}
		String uploadURL = String.format("%s/upload.php?username=%s&signature=%s",
				baseURI.toString(), urlEncode(credentials.getUsername()), signature.toURLEncoded());
		UploadOutputStream retval = new UploadOutputStream(filename, client, uploadURL);
		retval.setBufferSize(this.uploadBufferSize);
		return retval;
	}

	public JSONObject jsonGet(String apiFilename, HashMap<String, String> args, AuthHash signature) throws IOException {
		String currURL = baseURI.toString();
		if(currURL.charAt(currURL.length() - 1) != '/')
			currURL += "/";
		currURL += apiFilename;
		Iterator<String> keys = args.keySet().iterator();
		if(keys.hasNext()) {
			final String fmt = "%s=%s&";
			currURL += "?";
			while(keys.hasNext()) {
				String key = keys.next();
				String val = args.get(key);
				if(val == null)
					val = "";
				currURL += String.format(fmt, urlEncode(key), urlEncode(val));
			}
			if(signature != null) {
				currURL += String.format("username=%s&signature=%s", urlEncode(credentials.getUsername()), signature.toURLEncoded());
			}
			if(currURL.charAt(currURL.length() - 1) == '&')
				currURL = currURL.substring(0, currURL.length() - 1);
		}
		
		return client.jsonGet(currURL);
	}
	
	public WebResponse logon(int totpToken) throws IOException {
		if(credentials == null)
			throw new IOException("Cannot log on. Missing credentials.");
		
		
		AuthHash logonkey = null;
		try {
			logonkey = credentials.generateLogonKey(totpToken);
		} catch (Exception e) {
			throw new IOException("Error generating logon key", e);
		}
		
		String logonURL = String.format("%s/logon.php?username=%s&signature=%s",
				baseURI.toString(), urlEncode(credentials.getUsername()), logonkey.toURLEncoded());
		JSONObject json = client.jsonGet(logonURL);
		WebResponse retval = WebResponse.fromJSON(json);
		long status = (Long)json.get("status");
		if(status != WebResponse.SUCCESS) {
			if(json.containsKey("message"))
				L.error((String)json.get("message"));
			return retval;
		}
		
		// Generate signing key
		try {
			String key = (String)json.get("sessionkey");
			if(key == null)
				L.warn("Null signing key.");
			AuthHash signingHash = credentials.createSigningKey(key);
			signingkey = signingHash.bytes();
			credentials.destroyPassphrase(); // If there is a signing key, there is no need for the pass phrase. Destroy it.
		} catch (Exception e) {
			throw new IOException("Error generating signing key", e);
		}
				
		return retval;
	}

	public WebResponse ls(String path) throws IOException, WebFileException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(path.length() != 0) {
			if(path.charAt(0) != '/')
				path = "/" + path;
		}
		args.put("filename", path);
		try {
			WebFile webfile = WebFile.fromJSON(jsonGet("ls.php", args, signData(path)));
			return new WebResponse(0, webfile);
		} catch(Exception e) {
			throw new WebFileException("Error getting file information", e);
		}
	}
	
	public WebResponse stat(String path)  throws IOException, WebFileException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(path.length() != 0) {
			if(path.charAt(0) != '/')
				path = "/" + path;
		}
		args.put("filename", path);
		try {
			JSONObject json = jsonGet("stat.php", args, signData(path));
			FileMetaData metadata = FileMetaData.fromJSON(json);
			return new WebResponse(0, JSONValue.toJSONString(json), metadata);
		} catch(Exception e) {
			throw new WebFileException("Error doing file stat", e);
		}
	}
	
	public WebResponse checkout(String path)  throws IOException, WebFileException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(path.length() != 0) {
			if(path.charAt(0) != '/')
				path = "/" + path;
		}
		args.put("filename", path);
		try {
			return WebResponse.fromJSON(jsonGet("checkout.php", args, signData(path)));
		} catch(Exception e) {
			throw new WebFileException("Error doing file checkout", e);
		}
	}
	
	public WebResponse checkin(String path)  throws IOException, WebFileException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(path.length() != 0) {
			if(path.charAt(0) != '/')
				path = "/" + path;
		}
		args.put("filename", path);
		try {
			return WebResponse.fromJSON(jsonGet("checkin.php", args, signData(path)));
		} catch(Exception e) {
			throw new WebFileException("Error doing file checkin", e);
		}
	}
	
	public WebResponse chmod(String path, String user, int permission)  throws IOException, WebFileException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(path.length() != 0) {
			if(path.charAt(0) != '/')
				path = "/" + path;
		}
		args.put("filename", path);
		args.put("user", user);
		args.put("permission", Integer.toString(permission));
		try {
			JSONObject json = jsonGet("chmod.php", args, signData(path));
			return WebResponse.fromJSON(json);
		} catch(Exception e) {
			throw new WebFileException("Error changing file permission", e);
		}
	}

	/**
	 * Returns either the MD5 has as string or null if it could not be found.
	 * @param path
	 * @return WebResponse
	 * @throws IOException
	 */
	public WebResponse md5(String path) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		JSONObject json = jsonGet("md5.php", args, signData(path));
		if(json == null || !json.containsKey("md5"))
			return WebResponse.fromJSON(json);
		return new WebResponse(0, (String)json.get("md5"));
	}
	
	public WebResponse base64(String path, boolean encode) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		String action = (encode) ? "encode" : "decode";
		args.put("action", action);
		return WebResponse.fromJSON(jsonGet("base64.php", args, signData(path)));
	}

	public WebResponse merge(String path) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return WebResponse.fromJSON(jsonGet("merge.php", args, signData(path)));
	}
	
	public WebResponse remove(String path) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return WebResponse.fromJSON(jsonGet("delete.php", args, signData(path)));
	}

	public WebResponse move(String src, String dest) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("source", src);
		args.put("destination", dest);
		return WebResponse.fromJSON(jsonGet("mv.php", args, signData(src+dest)));
	}
	
	public WebResponse mkdir(String newdir) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("dirname", newdir);
		return WebResponse.fromJSON(jsonGet("mkdir.php", args, signData(newdir)));
	}

	public WebResponse clean(String filename, String md5hash) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(md5hash != null && md5hash.length() > 0) {
			args.put("md5", md5hash);
		}
		args.put("filename", filename);
		return WebResponse.fromJSON(jsonGet("clean.php", args, signData(filename)));
	}
	
	public WebResponse clean(String filename) throws IOException, AuthHash.HashException {
		return clean(filename, null);
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}
	
	public int getUploadBufferSize() {
		return uploadBufferSize;
	}
	
	public void setUploadBufferSize(int buffersize) {
		this.uploadBufferSize = buffersize;
	}
	
	public AuthHash signData(String data) throws AuthHash.HashException {
		long now = getCurrentTime() + timeOffset;
		return AuthHash.getInstance(data + Long.toString(now), signingkey);
	}
	
	public String urlEncode(String data) throws IOException {
		try {
			return URLEncoder.encode(data, URL_ENCODE_TYPE);
		} catch(UnsupportedEncodingException uee) {
			throw new IOException("Error encoding string: " + uee.getMessage(), uee);
		}
	}

	private long getCurrentTime() {
		return (new Date()).getTime() / 1000L;	
	}
}
