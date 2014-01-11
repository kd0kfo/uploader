package com.davecoss.uploader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class UploadOutputStream extends OutputStream {

	private int currIndex = 0;
	private String baseFilename;
	private String destinationURL;
	private File tempfile;
	private FileOutputStream stream;
	private HTTPSClient client;
	private int buffersize = 1024;
	private int bytesWritten = 0;
	
	public UploadOutputStream(HTTPSClient client, String destinationURL) throws IOException {
		tempfile = getTempFile();
		stream = new FileOutputStream(tempfile);
		baseFilename = tempfile.getName();
		this.client = client;
		this.destinationURL = destinationURL;
	}
	
	public UploadOutputStream(String baseFilename, HTTPSClient client, String destinationURL) throws IOException {
		tempfile = getTempFile();
		this.baseFilename = baseFilename;
		stream = new FileOutputStream(tempfile);
		this.client = client;
		this.destinationURL = destinationURL;
	}
	
	public int setBufferSize(int newsize) {
		buffersize = newsize;
		return buffersize;
	}
	
	public int getBufferSize() {
		return buffersize;
	}
	
	
	@Override 
	public void flush() throws IOException {
		// Null check
		if(client == null)
			throw new IOException("Client not connected.");

		// If stream is null, we are between two segments, but haven't written anything. Nothing to flush.
		
		// Close stream
		stream.close();
		
		// Move file to what it should be when uploaded
		File newfile = new File(baseFilename + "." + String.valueOf(currIndex));
		if(!tempfile.renameTo(newfile)) {
			throw new IOException("Could not rename " + tempfile.getName() + " to " + newfile.getName());
		}
		
		// Upload it
		client.postFile(this.destinationURL, newfile);
		
		// Cleanup
		newfile.delete();
		currIndex++;
		bytesWritten = 0;
	}
	
	@Override
	public void close() throws IOException {
		flush();
	}
	
	@Override
	public void write(int b) throws IOException {
		if(stream == null)
			stream = new FileOutputStream(tempfile);
		if(bytesWritten == buffersize)
			flush();
		stream.write(b);
	}
	
	@Override
	public void write(byte[] buf, int offset, int len) throws IOException {
		int amountToWrite = len;
		int currWriteAmount = 0;
		int totalAmountWritten = 0;
		
		if(stream == null)
			stream = new FileOutputStream(tempfile);
		while(amountToWrite > 0) {
			if(bytesWritten == buffersize)
				flush();
			if(amountToWrite == buffersize)
				currWriteAmount = buffersize - 1;
			else
				currWriteAmount = (amountToWrite % buffersize);
			if(bytesWritten + currWriteAmount > buffersize)
				currWriteAmount = buffersize - bytesWritten;
			stream.write(buf, offset + totalAmountWritten, currWriteAmount);
			
			totalAmountWritten += currWriteAmount;
			bytesWritten += currWriteAmount;
			amountToWrite -= currWriteAmount;
		}
		
		if(bytesWritten == buffersize) {
			flush();
		}
	}
	
	
	private static File getTempFile() throws IOException {
		File temp = File.createTempFile("tmp", null);
		temp.deleteOnExit();
		return temp;
	}
}