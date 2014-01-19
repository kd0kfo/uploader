package com.davecoss.uploader;

import java.io.IOException;

import org.json.simple.JSONObject;

public class WebResponse {
	public final int status;
	public final String message;
	
	public WebResponse(int status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public static WebResponse fromJSON(JSONObject json) throws IOException {
		if(!json.containsKey("status"))
			throw new IOException("Missing status field in JSONObject to be casted to a WebResponse");
		int status = ((Long)json.get("status")).intValue();
		
		String message = "";
		if(json.containsKey("message"))
			message = (String)json.get("message");
		
		return new WebResponse(status, message);
	}
}
