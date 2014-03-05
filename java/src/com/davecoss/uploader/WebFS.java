package com.davecoss.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.davecoss.java.GenericBase64;
import com.davecoss.java.Logger;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;

public class WebFS {
	static Logger L = Logger.getInstance();
	
	private URI baseURI = null;
	private JSONObject serverInfo = null;
	private static final JSONParser jsonParser = new JSONParser();
	private HTTPSClient client = null;
	private Credentials credentials = null;
	private byte[] signingkey = null;
	
	public WebFS(HTTPSClient client) {
		this.client = client;
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
		CloseableHttpResponse response = client.doGet(baseURI.toString() + "/info.php");
		JSONObject json = null;
		try {
			json = responseJSON(response);
		} catch(org.json.simple.parser.ParseException pe) {
			throw new IOException("Unable to load server config", pe);
		} finally {
			HTTPSClient.closeResponse(response);
		}
	
		this.serverInfo = json;
		if(!this.serverInfo.containsKey("contentdir"))
			throw new IOException("Unable to get a valid server configuration.");
	}
	
	@SuppressWarnings("deprecation")
	public static JSONObject responseJSON(HttpResponse response) throws IOException, org.json.simple.parser.ParseException {
		HttpEntity entity = response.getEntity();
		JSONObject retval = null;
		try {
			InputStream jsoncontent = entity.getContent();
			retval = (JSONObject) jsonParser.parse(new InputStreamReader(jsoncontent));
		} finally {
			if(entity != null)
				entity.consumeContent(); // TODO: Replace this.
		}
		return retval;
	}

	@SuppressWarnings("deprecation")
	public WebResponse downloadFile(String source, File dest) throws IOException, AuthHash.HashException {
		if(dest == null)
		{
			// if Dest is null, simply use the name of the source file.
			dest = new File(source);
			dest = new File(dest.getName());
		}
		
		// Get info for file and verify that it is a file.
		String contentdir = (String)serverInfo.get("contentdir");
		String sourceURL = String.format("%s/ls.php?filename=%s&username=%s&signature=%s",
				baseURI.toString(), source, credentials.getUsername(), AuthHash.getInstance(source, signingkey).toURLEncoded());
		CloseableHttpResponse response = client.doGet(sourceURL);
		HttpEntity entity = null;
		JSONObject json = null;
		InputStream input = null;
		FileOutputStream output = null;
		try {
			json = responseJSON(response);
			String type = (String)json.get("type");
			if(!type.equals("f"))
				throw new IOException("Cannot download " + source);
			sourceURL = baseURI.toString() + contentdir + (String)json.get("parent") + "/" + (String)json.get("name");
			HTTPSClient.closeResponse(response);
			
			// Download content.
			response = client.doGet(sourceURL);
			entity = response.getEntity();
			
			// Write content to file
			input = entity.getContent();
			output = new FileOutputStream(dest);
			byte[] buffer = new byte[4096];
			int amountRead = -1;
			while((amountRead = input.read(buffer, 0, 4096)) != -1)
				output.write(buffer,0, amountRead);
		} catch(org.json.simple.parser.ParseException pe) {
			throw new IOException("Error getting file information", pe);
		} finally {
			// Cleanup
			if(entity != null)
				entity.consumeContent(); // TODO: Replace this
			HTTPSClient.closeResponse(response);
			if(output != null)
				output.close();
		}
		return new WebResponse(0, String.format("Saved %s as %s", source, dest.getName()));
	}

	public WebResponse putFile(File file) throws IOException, AuthHash.HashException {
		if(!file.exists())
			return new WebResponse(1, "File not found: " + file.getPath());
		
		JSONObject json = null;
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("status", (Long)0L);
		values.put("message", "JSON return from put not yet implemented.");
		AuthHash signature = AuthHash.getInstance(file.getName(), signingkey);
		CloseableHttpResponse response = null;
		try {
			String postURL = String.format("%s/upload.php?username=%s&signature=%s",
					baseURI.toString(), credentials.getUsername(), signature.hash);
			response = client.postFile(postURL, file);
			json = responseJSON(response);
		} catch (ParseException e) {
			L.error("Error parsing JSON for putFile");
		} finally {
			HTTPSClient.closeResponse(response);
		}
		return WebResponse.fromJSON(json);
	}
	
	public WebResponse postStream(InputStream input, String filename) throws IOException, AuthHash.HashException {
		return postStream(input, filename, false);
	}
	
	public WebResponse postStream(InputStream input, String filename, boolean useBase64) throws IOException, AuthHash.HashException {
		AuthHash signature = AuthHash.getInstance(filename, signingkey);
		String postURL = String.format("%s/upload.php?username=%s&%signature=%s",
				baseURI.toString(), credentials.getUsername(), signature.hash);
		UploadOutputStream postStream = new UploadOutputStream(filename, client, postURL);
		OutputStream output = postStream;
		if(useBase64) {
			output = new Base64OutputStream(postStream);
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

	public JSONObject jsonGet(String apiFilename, HashMap<String, String> args, AuthHash signature) throws IOException {
		CloseableHttpResponse response = null;
		JSONObject json = null;
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
		try {
			response = client.doGet(currURL);
			json = responseJSON(response);
		} catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		} finally {
			HTTPSClient.closeResponse(response);
		}
		return json;
	}
	
	public JSONObject jsonGet(String apiFilename, HashMap<String, String> args) throws IOException {
		return jsonGet(apiFilename, args, null);
	}
	
	public WebResponse logon() throws IOException {
		if(credentials == null)
			throw new IOException("Cannot log on. Missing credentials.");
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("username", credentials.getUsername());
		try {
			args.put("hmac", credentials.generateLogonKey().hash);
		} catch (Exception e) {
			throw new IOException("Error generating logon key", e);
		}
		JSONObject json = jsonGet("logon.php", args);
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
			WebFile webfile = WebFile.fromJSON(jsonGet("ls.php", args, AuthHash.getInstance(path, signingkey)));
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
		JSONObject json = jsonGet("md5.php", args, AuthHash.getInstance(path, signingkey));
		if(json == null || !json.containsKey("md5"))
			return WebResponse.fromJSON(json);
		return new WebResponse(0, (String)json.get("md5"));
	}
	
	public WebResponse base64(String path, boolean encode) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		String action = (encode) ? "encode" : "decode";
		args.put("action", action);
		return WebResponse.fromJSON(jsonGet("base64.php", args, AuthHash.getInstance(path, signingkey)));
	}

	public WebResponse merge(String path) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return WebResponse.fromJSON(jsonGet("merge.php", args, AuthHash.getInstance(path, signingkey)));
	}
	
	public WebResponse remove(String path) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return WebResponse.fromJSON(jsonGet("delete.php", args, AuthHash.getInstance(path, signingkey)));
	}

	public WebResponse move(String src, String dest) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("source", src);
		args.put("destination", dest);
		return WebResponse.fromJSON(jsonGet("mv.php", args, AuthHash.getInstance(src+dest, signingkey)));
	}
	
	public WebResponse mkdir(String newdir) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("dirname", newdir);
		return WebResponse.fromJSON(jsonGet("mkdir.php", args, AuthHash.getInstance(newdir, signingkey)));
	}

	public WebResponse clean(String filename, String md5hash) throws IOException, AuthHash.HashException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(md5hash != null && md5hash.length() > 0) {
			args.put("md5", md5hash);
		}
		args.put("filename", filename);
		return WebResponse.fromJSON(jsonGet("clean.php", args, AuthHash.getInstance(filename, signingkey)));
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
	
	
}
