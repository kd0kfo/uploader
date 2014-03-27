package com.davecoss.uploader;

/**
 * Abstraction for responses to calls to the web filesystem. Contains a status message, webfile or local file.
 */

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONObject;

public class WebResponse {
	
	public static final int SUCCESS = 0;
	
	public final int status;
	public final String message;
	public final WebFile webfile;
	public final File localfile;
	public final FileMetaData metadata;
	
	public WebResponse(int status, String message) {
		this.status = status;
		this.message = message;
		this.webfile = null;
		this.localfile = null;
		this.metadata = null;
	}
	
	public WebResponse(int status, WebFile webfile) {
		this.status = status;
		this.message = webfile.name;
		this.webfile = webfile;
		this.localfile = null;
		this.metadata = null;
	}
	
	public WebResponse(int status, File localfile) {
		this.status = status;
		this.message = localfile.getName();
		this.webfile = null;
		this.localfile = localfile;
		this.metadata = null;
	}
	
	public WebResponse(int status, String filename, FileMetaData meatadata) {
		this.status = status;
		this.message = filename;
		this.webfile = null;
		this.localfile = null;
		this.metadata = meatadata;
	}
	
	public static WebResponse fromJSON(JSONObject json) throws IOException {
		if(json == null)
			return new WebResponse(1, "No json data provided.");
		if(!json.containsKey("status"))
			throw new IOException("Missing status field in JSONObject to be casted to a WebResponse");
		int status = ((Long)json.get("status")).intValue();
		
		String message = "";
		if(json.containsKey("message"))
			message = (String)json.get("message");
		
		return new WebResponse(status, message);
	}
}
