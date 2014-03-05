package com.davecoss.uploader;

public class WebFileException extends Exception {

	public WebFileException(String msg) {
		super(msg);
	}
	
	public WebFileException(String msg, Throwable e) {
		super(msg, e);
	}

	private static final long serialVersionUID = 6778761321402757552L;

}
