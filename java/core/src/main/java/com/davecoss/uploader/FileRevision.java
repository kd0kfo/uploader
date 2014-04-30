package com.davecoss.uploader;
import java.text.SimpleDateFormat;

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
		SimpleDateFormat df = new SimpleDateFormat("E, MMM d, YYYY kk:mm");
		String dateString = df.format(this.timestamp * 1000L);
		return this.command + " run by " + this.creator + " at " + dateString;
	}
	
}
