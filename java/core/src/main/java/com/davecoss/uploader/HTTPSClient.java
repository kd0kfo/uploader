package com.davecoss.uploader;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import com.davecoss.java.GenericBaseN;

import org.apache.http.HttpEntity;
import org.apache.http.client.CredentialsProvider;

import org.json.simple.JSONObject;

public interface HTTPSClient {
	
	public void startClient() throws IOException;
	
	public void startClient(CredentialsProvider creds, URI uri) throws IOException;
	
	public File downloadContent(String url) throws IOException;
	
	public JSONObject jsonGet(String url) throws IOException;
	
	public WebResponse doGet(String url) throws IOException;
	
	public WebResponse doGet(URL url) throws IOException;
	
	public WebResponse doGet(URI uri) throws IOException;
	
	public JSONObject jsonPost(String url, HttpEntity mpEntity) throws IOException;
	
	public WebResponse doPost(String url, HttpEntity mpEntity) throws IOException;

	public WebResponse doPost(URL url, HttpEntity mpEntity) throws IOException;
	
	public WebResponse doPost(URI uri, HttpEntity mpEntity) throws IOException;
	
	public WebResponse postFile(String url, File thefile) throws IOException;
	
	public GenericBaseN getEncoder();
	
	public void close() throws IOException;
	
}
