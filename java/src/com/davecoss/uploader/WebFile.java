package com.davecoss.uploader;

import org.json.simple.JSONObject;

public class WebFile {
	public final String name;
	public final long size;
	public final String type;
	
	public WebFile(String name, String type, long size) {
		this.name = name;
		this.type = type;
		this.size = size;
	}
	
	public static WebFile fromJSON(JSONObject json) throws WebFileException {
		if(!json.containsKey("name") || !json.containsKey("type"))
			throw new  WebFileException("Missing WebFile JSON Element");
		
		String name = (String)json.get("name");
		String type = (String)json.get("type");
		long size = 0;
		if(json.containsKey("size")) {
			size = (Long)json.get("size");
		}
		return new WebFile(name, type, size);
	}

	public String humanType() {
		if(type.equals("f"))
			return "file";
		if(type.equals("d"))
			return "directory";
		return "unknown";
	}

	public String dirListing() {
		return humanType() + "\t" + size + "\t" + name;
	}
	
	
}
