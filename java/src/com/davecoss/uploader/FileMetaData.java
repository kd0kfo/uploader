package com.davecoss.uploader;

import com.davecoss.java.Logger;
import com.davecoss.java.LogHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class FileMetaData {

	private static Logger L = Logger.getInstance();
	
	public final Map<Integer, FileRevision> revisionList;
	public final ACL acl;
	public long size;
	
	public FileMetaData(long size, Map<Integer, FileRevision> revisionList, ACL acl) {
		this.acl = acl;
		this.revisionList = revisionList;
		this.size = size;
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
		if(!json.containsKey("size") || !json.containsKey("acl") || !json.containsKey("revisions"))
			throw new  WebFileException("Missing WebFile JSON Element");
		
		JSONObject acllist = (JSONObject)json.get("acl");
		JSONObject revisions = (JSONObject)json.get("revisions");
		long size = 0;
		if(json.containsKey("size")) {
			size = (Long)json.get("size");
		}
		
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
		
		return new FileMetaData(size, revisionList, acl);
	}
}