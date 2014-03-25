package com.davecoss.uploader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.davecoss.java.Logger;

public class DownloadInputStream extends InputStream {

	static Logger L = Logger.getInstance();
	
	protected HTTPSClient client;
	protected String sourceURI;
	
	protected final File tempStorage;
	protected BufferedInputStream stream;
	
	public DownloadInputStream(HTTPSClient client, String uri) throws IOException {
		sourceURI = uri;
		tempStorage = client.downloadContent(uri);
		stream = new BufferedInputStream(new FileInputStream(tempStorage));
	}
	
	@Override
	public int read() throws IOException {
		return stream.read();
	}
	
	@Override
	public int	read(byte[] b) throws IOException {
		return stream.read(b);
	}
	
	@Override
	public int	read(byte[] b, int off, int len) throws IOException {
		return stream.read(b, off, len);
	}
	
	@Override
	public long skip(long n) throws IOException {
		return stream.skip(n);
	}
	
	@Override
	public int available() throws IOException {
		return stream.available();
	}
	
	@Override
	public void close() throws IOException {
		stream.close();
		tempStorage.delete();
	}
	
	@Override
	public void mark(int readlimit) {
		stream.mark(readlimit);
	}
	
	@Override
	public void reset() throws IOException {
		stream.reset();
	}
	
	@Override
	public boolean markSupported() {
		return stream.markSupported();
	}
	
}