package com.davecoss.uploader;

public class FileRevision {
	
	public final String creator;
	public final long timestamp;
	public final String command;
	
	public FileRevision(String creator, long timestamp, String command) {
		this.creator = creator;
		this.timestamp = timestamp;
		this.command = command;
	}
	
	public String toString() {
		return this.command + " run by " + this.creator + " at " + this.timestamp;
	}
	
}