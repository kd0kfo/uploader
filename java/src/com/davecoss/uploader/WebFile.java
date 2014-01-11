package com.davecoss.uploader;

import org.json.simple.JSONObject;

public class WebFile {
	public final String name;
	public final String type;
	
	public WebFile(String name, String type) {
		this.name = name;
		this.type = type;
	}
	
	public static WebFile fromJSON(JSONObject json) throws WebFileException {
		if(!json.containsKey("name") || !json.containsKey("type"))
			throw new  WebFileException("Missing WebFile JSON Element");
		
		String name = (String)json.get("name");
		String type = (String)json.get("type");
		return new WebFile(name, type);
	}

	public String humanType() {
		if(type.equals("f"))
			return "file";
		if(type.equals("d"))
			return "directory";
		return "unknown";
	}
	
	
}
