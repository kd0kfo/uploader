package com.davecoss.uploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;

import javax.net.ssl.SSLContext;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CredentialPair;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HTTPSClient {
	
	private static Logger L = Logger.getInstance();

	private SSLContext sslcontext = null;
	private SSLConnectionSocketFactory sslsf = null;
	private CloseableHttpClient httpclient = null;
	private KeyStore keystore = null;
	private String keystoreType = KeyStore.getDefaultType();
	
	public HTTPSClient() {
		httpclient = HttpClients.createDefault();
	}
	
	public HTTPSClient(String keystoreFilename, char[] passphrase, String keystoreType) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		this.keystoreType = keystoreType;
		loadKeystore(keystoreFilename, passphrase);
		buildContext();
		initSSLSocketFactory();
		startClient();
	}
	
	public HTTPSClient(String keystoreFilename, char[] passphrase) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		loadKeystore(keystoreFilename, passphrase);
		buildContext();
		initSSLSocketFactory();
		startClient();
	}
	
	public void loadKeystore(String keystoreFilename, char[] passphrase) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		keystore  = KeyStore.getInstance(keystoreType);
        FileInputStream instream = new FileInputStream(new File(keystoreFilename));
        try {
            keystore.load(instream, passphrase);
        } finally {
            instream.close();
        }
        
	}
	
	public void buildContext(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		this.keystore = keystore;
		buildContext();
	}
	
	public void buildContext() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		sslcontext = SSLContexts.custom()
	            .loadTrustMaterial(keystore)
	            .build();
	}

	public void initSSLSocketFactory() {
		sslsf = new SSLConnectionSocketFactory(sslcontext,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	}
	
	public void startClient() throws IOException {
		if(httpclient != null)
			httpclient.close();
		httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();
	}
	
	public void startClient(CredentialsProvider creds, URI uri) throws IOException {
		if(httpclient != null)
			httpclient.close();
		httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultCredentialsProvider(creds)
                .build();
		
		// Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local
        // auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(new HttpHost(uri.getHost(),uri.getPort()), basicAuth);

        // Add AuthCache to the execution context
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
	}

	public CloseableHttpResponse doGet(String url) throws IOException {
		HttpGet httpget = new HttpGet(url);
	    
        L.info("executing request " + httpget.getRequestLine());

        return httpclient.execute(httpget);
        
	}
	
	public CloseableHttpResponse doGet(URL url) throws IOException {
		return doGet(url.toString());
	}
	
	public CloseableHttpResponse doGet(URI uri) throws IOException {
		return doGet(uri.toURL());
	}
	
	public CloseableHttpResponse doPost(String url, HttpEntity mpEntity) throws IOException {
		HttpPost httppost = new HttpPost(url);
	    
        System.out.println("executing request " + httppost.getRequestLine());

        if(mpEntity != null)
        {
        	httppost.setEntity(mpEntity);
        }
        
        return httpclient.execute(httppost);
        
	}

	public CloseableHttpResponse doPost(URL url, HttpEntity mpEntity) throws IOException {
		return doPost(url.toString(), mpEntity);
	}
	
	public CloseableHttpResponse doPost(URI uri, HttpEntity mpEntity) throws IOException {
		return doPost(uri.toString(), mpEntity);
	}
	
	public static void closeResponse(CloseableHttpResponse response) throws IOException {
		if(response != null)
			response.close();
	}

	public void close() throws IOException {
		if(httpclient != null)
			httpclient.close();
	}
	
	public static void printResponse(CloseableHttpResponse response, PrintStream output) throws IOException {
	    byte[] buf = new byte[4096];
	    int amount_read = -1;

		HttpEntity entity = response.getEntity();
		
	    output.println("----------------------------------------");
	    output.println(response.getStatusLine());
		if (entity != null) {
			output.println("Response content length: " + entity.getContentLength());
		    InputStream htmlcontent = entity.getContent();
		    while((amount_read = htmlcontent.read(buf, 0, 4096)) != -1) {
		    	output.write(buf, 0, amount_read);
		    }
		}
		EntityUtils.consume(entity);
	}
	
	public CloseableHttpResponse postFile(String url, File thefile) throws IOException {
		L.error("Posting " + thefile.getName() + " to " + url);
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		FileBody fb = new FileBody(thefile);
		entityBuilder.addPart("file", fb);
		HttpEntity mpEntity = entityBuilder.build();
		L.debug("Attaching File " + thefile.getName());

    	return this.doPost(url, mpEntity);
    	
	}
	
	public static CredentialsProvider createCredentialsProvider(String username, char[] password, URI uri) throws IOException {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username, new String(password)));
        
        return credsProvider;
	}
	
	public static CredentialsProvider createCredentialsProvider(CredentialPair creds, URI uri) throws IOException {
		CredentialsProvider retval = null;
		retval = createCredentialsProvider(creds.getUsername(), creds.getPassphrase(), uri);
		return retval;
	}
	
    

}
