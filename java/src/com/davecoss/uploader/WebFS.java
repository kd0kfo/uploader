package com.davecoss.uploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class WebFS {
	
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
	
	public static JSONObject responseJSON(HttpResponse response) throws IOException, org.json.simple.parser.ParseException {
		HttpEntity entity = response.getEntity();
		JSONObject retval = null;
		try {
			InputStream jsoncontent = entity.getContent();
			retval = (JSONObject) jsonParser.parse(new InputStreamReader(jsoncontent));
		} finally {
			if(entity != null)
				EntityUtils.consume(entity);
		}
		return retval;
	}

	public void downloadFile(String source, File dest) throws IOException {
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
			{
				System.out.println("Cannot download " + source);
				return;
			}
			sourceURL = baseURI.toString() + contentdir + (String)json.get("parent") + "/" + (String)json.get("name");
			System.out.println("Path: " + sourceURL);
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
				EntityUtils.consume(entity);
			HTTPSClient.closeResponse(response);
			if(output != null)
				output.close();
		}
		
	}

	public JSONObject putFile(File file) throws IOException {
		if(!file.exists())
		{
			System.out.println("File not found: " + file.getPath());
			return null;
		}
		
		JSONObject json = null;
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("status", (Long)0L);
		values.put("message", "JSON return from put not yet implemented.");
		CloseableHttpResponse response = null;
		try {
			response = client.postFile(baseURI.toString() + "/upload.php", file);
		} finally {
			HTTPSClient.closeResponse(response);
		}
		return json;
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

	public WebFile ls(String path) throws IOException, WebFileException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		JSONObject json = jsonGet("ls.php", args);
		return WebFile.fromJSON(json);
	}

	public JSONObject md5(String path) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return jsonGet("md5.php", args);
	}

	public JSONObject merge(String path) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return jsonGet("merge.php", args);
	}
	
	public JSONObject remove(String path) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		return jsonGet("delete.php", args);
	}

	public JSONObject move(String src, String dest) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("source", src);
		args.put("destination", dest);
		return jsonGet("mv.php", args);
	}
	
	public JSONObject mkdir(String newdir) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("dirname", newdir);
		return jsonGet("mkdir.php", args);
	}
	
}
