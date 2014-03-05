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

import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;

public class WebFS {
	static Logger L = Logger.getInstance();
	
	private URI baseURI = null;
	private JSONObject serverInfo = null;
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
		String sourceURL = baseURI.toString() + "/ls.php?filename=" + source;
		InputStream input = null;
		FileOutputStream output = null;
		try {
			WebFile fileInfo = WebFile.fromJSON(client.jsonGet(sourceURL));
			if(!fileInfo.type.equals("f"))
				throw new IOException("Cannot download " + source);
			sourceURL = baseURI.toString() + WebFile.join(contentdir, fileInfo.getAbsolutePath());
			System.out.println("Path: " + sourceURL); // TODO: replace with Logger
			
			// Download content.
			input = client.getContent(sourceURL);
			
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
		} finally {
			// Cleanup
			if(input != null)
				input.close();
			if(output != null)
				output.close();
		}
		return new WebResponse(0, String.format("Saved %s as %s", source, dest.getName()));
	}

	public WebResponse putFile(File file) throws IOException {
		if(!file.exists())
			return new WebResponse(1, "File not found: " + file.getPath());
		
		return client.postFile(baseURI.toString() + "/upload.php", file);
	}
	
	public WebResponse postStream(InputStream input, String filename) throws IOException {
		return postStream(input, filename, false);
	}
	
	public WebResponse postStream(InputStream input, String filename, boolean useBase64) throws IOException {
		UploadOutputStream postStream = new UploadOutputStream(filename, client, baseURI.toString() + "/upload.php");
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

	public JSONObject jsonGet(String apiFilename, HashMap<String, String> args) throws IOException {
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
		
		return client.jsonGet(currURL);
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
	
	public WebResponse base64(String path, boolean encode) throws IOException {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", path);
		String action = (encode) ? "encode" : "decode";
		args.put("action", action);
		return WebResponse.fromJSON(jsonGet("base64.php", args));
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
