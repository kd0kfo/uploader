package com.davecoss.uploader;

import com.davecoss.java.Logger;
import com.davecoss.java.LogHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.StringBuilder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class FileMetaData {

	private static Logger L = Logger.getInstance();
	
	public final String path;
	public final String owner;
	public final Map<Integer, FileRevision> revisionList;
	public final Map<String, Long> checkouts;
	public final ACL acl;
	public final long size;
	
	public FileMetaData(String path, long size, String owner,
			Map<Integer, FileRevision> revisionList, ACL acl,
			Map<String, Long> checkouts) {
		this.path = path;
		this.owner = owner;
		this.acl = acl;
		this.revisionList = revisionList;
		this.size = size;
		this.checkouts = checkouts;
	}
	
	@SuppressWarnings("unchecked")
	public static FileMetaData fromJSON(JSONObject json) throws WebFileException {
		if(json == null)
			return null;

		L.debug("Extracting metadata from: " +  JSONValue.toJSONString(json));
		
		if(json.containsKey("status")) {
			long status = (Long)json.get("status");
			if(status != 0) {
				throw new WebFileException((String)json.get("message"));
			}
		}
		if(!json.containsKey("owner") || !json.containsKey("path") ||
				!json.containsKey("size")
				|| !json.containsKey("acl")
				|| !json.containsKey("revisions"))
			throw new  WebFileException("Missing WebFile JSON Element");
		
		JSONObject acllist = (JSONObject)json.get("acl");
		JSONObject revisions = (JSONObject)json.get("revisions");
		JSONObject checkoutList = (JSONObject)json.get("checkouts");
		long size = 0;
		if(json.containsKey("size")) {
			size = (Long)json.get("size");
		}
		String path = (String)json.get("path");
		String owner = (String)json.get("owner");
		
		ACL acl = new ACL();
		if(acllist != null) {
			Iterator<Object> aclit = acllist.keySet().iterator();
			while(aclit.hasNext()) {
				String username = (String)aclit.next();
				int permission = ((Long)acllist.get(username)).intValue();
				acl.addPermission(username, permission);
			}
		}
		
		HashMap<Integer, FileRevision> revisionList = new HashMap<Integer, FileRevision>();
		if(revisions != null) {
			Iterator<Object> revision = revisions.keySet().iterator();
			while(revision.hasNext()) {
				String strid = (String)revision.next();
				int revid = Integer.parseInt(strid);
				JSONObject revinfo = (JSONObject)revisions.get(strid);
				String creator = (String)revinfo.get("creator");
				long timestamp = (Long)revinfo.get("timestamp");
				String command = (String)revinfo.get("command");
				revisionList.put(revid, new FileRevision(creator, timestamp, command));
			}
		}
		HashMap<String, Long> checkouts = new HashMap<String, Long>();
		if(checkoutList != null) {
			Iterator<Object> checkoutIt = checkoutList.values().iterator();
			while(checkoutIt.hasNext()) {
				JSONObject val = (JSONObject)checkoutIt.next();
				String user = (String)val.get("user");
				Long timestamp = (Long)val.get("timestamp");
				checkouts.put(user, timestamp);
			}
		}
		
		return new FileMetaData(path, size, owner, revisionList, acl, checkouts);
	}
	
	public static FileMetaData fromFile(File file) throws WebFileException, IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
			sb.append('\r');
		}
		return fromJSON((JSONObject)JSONValue.parse(sb.toString()));
	}
	
	public Integer getLastRevision() {
		if(revisionList.isEmpty()) {
			return null;
		}
		Iterator<Integer> revids = revisionList.keySet().iterator();
		Integer max = null;
		while(revids.hasNext()) {
			if(max == null) {
				max = revids.next();
			}
			else {
				Integer revid = revids.next();
				if(revid.compareTo(max) > 0)
					max = revid;
			}
		}
		return max;
	}
	
	public FileRevision getRevision(Integer revid) {
		return revisionList.get(revid);
	}
}