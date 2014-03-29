package com.davecoss.uploader;

public class Permission {
	
	public final int perm;
	
	public Permission(int permission) {
		this.perm = permission;
	}
	
	public boolean canRead() {
		return (perm & 0x4) != 0;
	}
	
	public boolean canWrite() {
		return (perm & 0x2) != 0;
	}
	
	public boolean canExecute() {
		return (perm & 0x1) != 0;
	}
	
	public String toString() {
		String retval = "";
		if(canRead())
			retval += "r";
		else
			retval += "-";
		if(canWrite())
			retval += "w";
		else
			retval += "-";
		if(canExecute())
			retval += "x";
		else
			retval += "-";
		return retval;
	}
	
}