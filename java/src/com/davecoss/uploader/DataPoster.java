package com.davecoss.uploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.lang.StringBuilder;
import java.net.URLEncoder;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;  
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicNameValuePair;  

public class DataPoster extends UploadOutputStream {
	
	public HashMap<String, String> extraPostParams = null;
	
	public DataPoster (String baseFilename, HTTPSClient client, String destinationURL) throws IOException {
		super(baseFilename, client, destinationURL);
	}
	
	public DataPoster (HTTPSClient client, String destinationURL) throws IOException {
		super(client, destinationURL);
	}
	
	@Override
	protected CloseableHttpResponse uploadFile(File file) throws IOException {
		StringBuilder sb = new StringBuilder((int) file.length());
		BufferedReader reader = null;
		String dataString = "";
		try {
			reader = new BufferedReader(new FileReader(file));
			char[] buffer = new char[4096];
			int amountRead = -1;
			while((amountRead = reader.read(buffer)) != -1)
				sb.append(buffer, 0, amountRead);
			dataString = sb.toString();
		} finally {
			if(reader != null)
				reader.close();
		}
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("filename", file.getName()));
        params.add(new BasicNameValuePair("data", dataString));
        
        if(extraPostParams != null) {
        	Iterator<String> keys = extraPostParams.keySet().iterator();
        	while(keys.hasNext()) {
        		String key = keys.next();
        		String val = extraPostParams.get(key);
        		params.add(new BasicNameValuePair(key, val));
        	}
        }
        
        UrlEncodedFormEntity data = new UrlEncodedFormEntity(params, "UTF-8");
        return client.doGet(destinationURL + "?filename=" + file.getName() + "&data=" + URLEncoder.encode(dataString, "UTF-8"));
	}
	
	public HashMap<String, String> getExtraPostParams() {
		return extraPostParams;
	}

	public void setExtraPostParams(HashMap<String, String> extraPostParams) {
		this.extraPostParams = extraPostParams;
	}

	public void addParameter(String key, String value) {
		if(extraPostParams == null)
			extraPostParams = new HashMap<String, String>();
		extraPostParams.put(key, value);
	}

}