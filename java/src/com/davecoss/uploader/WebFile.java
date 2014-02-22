package com.davecoss.uploader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WebFile {
	public final String name;
	public final long size;
	public final String type;
	public final String parent;
	public final WebFile[] dirents;
	
	public WebFile(String parent, String name, String type, long size) {
		this.parent = parent;
		this.name = name;
		this.type = type;
		this.size = size;
		this.dirents = new WebFile[0];
	}
	
	public WebFile(String parent, String name, String type, long size, WebFile[] dirents) {
		this.parent = parent;
		this.name = name;
		this.type = type;
		this.size = size;
		if(dirents == null)
			this.dirents = new WebFile[0];
		else
			this.dirents = dirents;
	}
	
	public static WebFile fromJSON(JSONObject json) throws WebFileException {
		if(json == null)
			return null;

		if(!json.containsKey("name") || !json.containsKey("type"))
			throw new  WebFileException("Missing WebFile JSON Element");
		
		String parent = (String)json.get("parent");
		String name = (String)json.get("name");
		String type = (String)json.get("type");
		long size = 0;
		if(json.containsKey("size")) {
			size = (Long)json.get("size");
		}
		if(!json.containsKey("dirents"))
			return new WebFile(parent, name, type, size);
		
		// Directory. Fill in dirents
		JSONArray rawDirents = (JSONArray)json.get("dirents");
		WebFile[] dirents = new WebFile[rawDirents.size()];
		int dirEntSize = dirents.length;
		for(int idx = 0;idx<dirEntSize;idx++)
			dirents[idx] = WebFile.fromJSON((JSONObject)rawDirents.get(idx));
		return new WebFile(parent, name, type, size, dirents);
	}
	
	public boolean isFile() {
		return type.equals("f");
	}

	public boolean isDirectory() {
		return type.equals("d");
	}
	
	public WebFile[] listFiles() {
		return dirents;
	}
	
	public String[] list() {
		String[] retval = new String[dirents.length];
		int arrSize = retval.length;
		for(int idx = 0;idx<arrSize;idx++)
			retval[idx] = dirents[idx].name;
		return retval;
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

	public String getAbsolutePath() {
		return parent + "/" + name;
	}
	
	
}
