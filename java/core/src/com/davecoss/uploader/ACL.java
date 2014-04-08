package com.davecoss.uploader;

import java.util.HashMap;
import java.util.Set;

public class ACL {
	
	private HashMap<String, Permission> permissions = new HashMap<String, Permission>();
	
	public ACL() {
	}
	
	public void addPermission(String username, Permission permission) {
		permissions.put(username, permission);
	}
	
	public void addPermission(String username, int permission) {
		addPermission(username, new Permission(permission));
	}
	
	public Set<String> getUsers() {
		return permissions.keySet();
	}
	
	public Permission getPermission(String username) {
		return permissions.get(username);
	}
	
	public boolean canRead(String username) {
		Permission perm = getPermission(username);
		if(perm == null)
			return false;
		return perm.canRead();
	}
	
	public boolean canWrite(String username) {
		Permission perm = getPermission(username);
		if(perm == null)
			return false;
		return perm.canWrite();
	}
	
}