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


import com.davecoss.java.Logger;

public class WebFS {
	static Logger L = Logger.getInstance();
	
	private URI baseURI = null;
	private JSONObject serverInfo = null;
	private static final JSONParser jsonParser = new JSONParser();
	private HTTPSClient client = null;
	
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
	public WebResponse downloadFile(String source, File dest) throws IOException {
		if(dest == null)
		{
			// if Dest is null, simply use the name of the source file.
			dest = new File(source);
			dest = new File(dest.getName());
		}
		
		// Get info for file and verify that it is a file.
		String contentdir = (String)serverInfo.get("contentdir");
		String sourceURL = baseURI.toString() + "/ls.php?filename=" + source;
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
			System.out.println("Path: " + sourceURL); // TODO: replace with Logger
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

	public WebResponse putFile(File file) throws IOException {
		if(!file.exists())
			return new WebResponse(1, "File not found: " + file.getPath());
		
		JSONObject json = null;
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("status", (Long)0L);
		values.put("message", "JSON return from put not yet implemented.");
		CloseableHttpResponse response = null;
		try {
			response = client.postFile(baseURI.toString() + "/upload.php", file);
			json = responseJSON(response);
		} catch (ParseException e) {
			L.error("Error parsing JSON for putFile");
		} finally {
			HTTPSClient.closeResponse(response);
		}
		return WebResponse.fromJSON(json);
	}
	
	public WebResponse postStream(InputStream input, String filename) throws IOException {
		return postStream(input, filename, false);
	}
	
	public WebResponse postStream(InputStream input, String filename, boolean useBase64) throws IOException {
		DataPoster postStream = new DataPoster(filename, client, baseURI.toString() + "/postdata.php");
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

	public JSONObject jsonGet(String apiFilename, HashMap<String, String> args) throws IOException {
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

	public WebResponse ls(String path) throws IOException, WebFileException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(path.length() != 0) {
			if(path.charAt(0) != '/')
				path = "/" + path;
		}
		args.put("filename", path);
		WebFile webfile = WebFile.fromJSON(jsonGet("ls.php", args));
		return new WebResponse(0, webfile);
	}

	/**
	 * Returns either the MD5 has as string or null if it could not be found.
	 * @param path
	 * @return WebResponse
	 * @throws IOException
	 */
	public WebResponse md5(String path) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		JSONObject json = jsonGet("md5.php", args);
		if(json == null || !json.containsKey("md5"))
			return WebResponse.fromJSON(json);
		return new WebResponse(0, (String)json.get("md5"));
	}

	public WebResponse merge(String path) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return WebResponse.fromJSON(jsonGet("merge.php", args));
	}
	
	public WebResponse remove(String path) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return WebResponse.fromJSON(jsonGet("delete.php", args));
	}

	public WebResponse move(String src, String dest) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("source", src);
		args.put("destination", dest);
		return WebResponse.fromJSON(jsonGet("mv.php", args));
	}
	
	public WebResponse mkdir(String newdir) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("dirname", newdir);
		return WebResponse.fromJSON(jsonGet("mkdir.php", args));
	}

	public WebResponse clean(String filename, String md5hash) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		if(md5hash != null && md5hash.length() > 0) {
			args.put("md5", md5hash);
		}
		args.put("filename", filename);
		return WebResponse.fromJSON(jsonGet("clean.php", args));
	}
	
	public WebResponse clean(String filename) throws IOException {
		return clean(filename, null);
	}
	
	
}
