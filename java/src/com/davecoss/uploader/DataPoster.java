package com.davecoss.uploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.lang.StringBuilder;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;  
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicNameValuePair;  

public class DataPoster extends UploadOutputStream {
	
	public DataPoster (HTTPSClient client, String destinationURL) throws IOException {
		super(client, destinationURL);
	}
	
	@Override
	protected CloseableHttpResponse uploadFile(File file) throws IOException {
		StringBuilder sb = new StringBuilder((int) file.length());
		BufferedReader reader = new BufferedReader(new FileReader(file));
		char[] buffer = new char[4096];
		int amountRead = -1;
		while((amountRead = reader.read(buffer)) != -1)
			sb.append(buffer, 0, amountRead);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("data", sb.toString()));
        UrlEncodedFormEntity data = new UrlEncodedFormEntity(params, "UTF_8"); 
        return client.doPost(this.destinationURL, data);
	}
}