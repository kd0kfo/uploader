package com.davecoss.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;

import com.davecoss.java.Logger;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;

import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;

public class WebFS {
	static Logger L = Logger.getInstance();
	
	private URI baseURI = null;
	private JSONObject serverInfo = null;
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
	}
	
	public void downloadConfig() throws IOException {
		this.serverInfo = client.jsonGet(baseURI.toString() + "/info.php");
		if(!this.serverInfo.containsKey("contentdir"))
			throw new IOException("Unable to get a valid server configuration.");
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
		String contentdir = (String)serverInfo.get("contentdir");
		DownloadInputStream input = null;
		FileOutputStream output = null;
		try {
			AuthHash signature = signData(source);
			String sourceURL = String.format("%s/ls.php?filename=%s&signature=%s&username=%s",
					baseURI.toString(), source, signature.toURLEncoded(), credentials.getUsername());
			WebFile fileInfo = WebFile.fromJSON(client.jsonGet(sourceURL));
			if(!fileInfo.type.equals("f"))
				throw new IOException("Cannot download " + source);
			sourceURL = baseURI.toString() + WebFile.join(contentdir, fileInfo.getAbsolutePath());
			System.out.println("Path: " + sourceURL); // TODO: replace with Logger
			
			// Download content.
			input = new DownloadInputStream(client, sourceURL);
			
			// Write content to file
			output = new FileOutputStream(dest);
			byte[] buffer = new byte[4096];
			int amountRead = -1;
			while((amountRead = input.read(buffer, 0, 4096)) != -1)
				output.write(buffer,0, amountRead);
		} catch(WebFileException wfe) {
			String msg = "Error getting file information";
			L.error(msg, wfe);
			throw new IOException(msg, wfe);
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
				baseURI.toString(), credentials.getUsername(), signature.toURLEncoded());
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
		String uri = String.format("%s/%s%s", baseURI.toString(), (String)serverInfo.get("contentdir"), path);
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
				baseURI.toString(), credentials.getUsername(), signature.toURLEncoded());
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
				currURL += String.format(fmt, key, val);
			}
			if(signature != null) {
				currURL += String.format("username=%s&signature=%s", credentials.getUsername(), signature.toURLEncoded());
			}
			if(currURL.charAt(currURL.length() - 1) == '&')
				currURL = currURL.substring(0, currURL.length() - 1);
		}
		
		return client.jsonGet(currURL);
	}
	
	public WebResponse logon() throws IOException {
		if(credentials == null)
			throw new IOException("Cannot log on. Missing credentials.");
		
		AuthHash logonkey = null;
		try {
			logonkey = credentials.generateLogonKey();
		} catch (Exception e) {
			throw new IOException("Error generating logon key", e);
		}
		
		String logonURL = String.format("%s/logon.php?username=%s&hmac=%s",
				baseURI.toString(), credentials.getUsername(), logonkey.toURLEncoded());
		JSONObject json = client.jsonGet(logonURL);
		WebResponse retval = WebResponse.fromJSON(json);
		long status = (Long)json.get("status");
		if(status != WebResponse.SUCCESS)
			return retval;
		
		// Generate signing key
		try {
			String key = (String)json.get("sessionkey");
			AuthHash signingHash = credentials.createSigningKey(key);
			signingkey = signingHash.bytes();
			credentials.destroyPassphrase(); // If there is a signing key, there is no need for the pass phrase. Destroy it.
			L.info("Signing Key: " + signingHash.hash);
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
		return AuthHash.getInstance(data, signingkey);
	}
}
