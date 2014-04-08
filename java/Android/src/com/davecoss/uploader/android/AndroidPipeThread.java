package com.davecoss.uploader.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.davecoss.java.Logger;
import com.davecoss.uploader.WebResponse;

public class AndroidPipeThread implements Callable<WebResponse> {

	private Logger L = Logger.getInstance();
	
	private final InputStream input;
	private final OutputStream output;
	private final boolean closeEndOfCall;
	
	public AndroidPipeThread(InputStream input, OutputStream output, boolean closeEndOfCall) {
		this.input = input;
		this.output = output;
		this.closeEndOfCall = closeEndOfCall;
	}
	
	@Override
	public WebResponse call() {
		int amountRead = -1;
		byte[] buffer = new byte[4096];
		
		try {
			while((amountRead = input.read(buffer)) != -1)
				output.write(buffer, 0, amountRead);
			return new WebResponse(0, "Pipe completed");
		} catch(IOException ioe) {
			String msg = "Error piping stream";
			L.error(msg, ioe);
			return new WebResponse(1, msg);
		} finally {
			if(closeEndOfCall && output != null) {
				try {
					output.close();
				} catch(IOException ioe) {
					String msg = "Error closing output stream";
					L.error(msg, ioe);
					return new WebResponse(1, msg);
				}
			}
		}
	}
	
	public FutureTask<WebResponse> createFutureTask() {
		return new FutureTask<WebResponse>(this);
	}

}
