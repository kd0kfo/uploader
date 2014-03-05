package com.davecoss.uploader.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import com.davecoss.java.GenericBase64;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.WebResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
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

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.omg.CORBA_2_3.portable.OutputStream;


public class ConsoleHTTPSClient implements HTTPSClient {
	
	private static Logger L = Logger.getInstance();
	
	private static final JSONParser jsonParser = new JSONParser();
	private static final CommonsBase64 encoder = new CommonsBase64();

	private SSLContext sslcontext = null;
	private SSLConnectionSocketFactory sslsf = null;
	private CloseableHttpClient httpclient = null;
	private KeyStore keystore = null;
	private String keystoreType = KeyStore.getDefaultType();
	
	public ConsoleHTTPSClient() {
		httpclient = HttpClients.createDefault();
	}
	
	public ConsoleHTTPSClient(String keystoreFilename, char[] passphrase, String keystoreType) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		this.keystoreType = keystoreType;
		loadKeystore(keystoreFilename, passphrase);
		buildContext();
		initSSLSocketFactory();
		startClient();
	}
	
	public ConsoleHTTPSClient(String keystoreFilename, char[] passphrase) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
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
	
	@Override
	public void startClient() throws IOException {
		if(httpclient != null)
			httpclient.close();
		httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();
	}
	
	@Override
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

	@Override
	public InputStream getContent(String url) throws IOException {
		HttpGet httpget = new HttpGet(url);
		
        L.info("Opening input stream from " + httpget.getRequestLine());

        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
		InputStream content = null;
        InputStream retval = null;
        FileOutputStream output = null;
        try {
        	content = entity.getContent();
        	File tmpfile = File.createTempFile("download", null);
        	tmpfile.deleteOnExit();
        	output = new FileOutputStream(tmpfile);
        	byte[] buffer = new byte[4096];
        	int amountRead = -1;
        	while((amountRead = content.read(buffer)) != -1)
        		output.write(buffer, 0, amountRead);
        	output.close();
        	output = null;
        	retval = new FileInputStream(tmpfile);
        } finally {
        	closeResponse(response);
        	if(content != null)
        		content.close();
        	if(output != null)
        		output.close();
        }
        return retval;
	}
		
	@Override
	public JSONObject jsonGet(String url) throws IOException {
		HttpGet httpget = new HttpGet(url);
	    
        L.info("executing request " + httpget.getRequestLine());

        CloseableHttpResponse response = httpclient.execute(httpget);
        try {
        	return responseJSON(response);
        } catch(ParseException pe) {
        	throw new IOException("Error parsing json data.", pe);
        } finally {
        	closeResponse(response);
        }
	}
	
	@Override
	public WebResponse doGet(String url) throws IOException {
		return WebResponse.fromJSON(jsonGet(url));
	}
	
	@Override
	public WebResponse doGet(URL url) throws IOException {
		return doGet(url.toString());
	}
	
	@Override
	public WebResponse doGet(URI uri) throws IOException {
		return doGet(uri.toURL());
	}
	
	@Override
	public JSONObject jsonPost(String url, HttpEntity mpEntity) throws IOException {
		HttpPost httppost = new HttpPost(url);
	    
        System.out.println("executing request " + httppost.getRequestLine());

        if(mpEntity != null)
        {
        	httppost.setEntity(mpEntity);
        }
        
        CloseableHttpResponse response = httpclient.execute(httppost);
        try {
        	return responseJSON(response);
        } catch(ParseException pe) {
        	throw new IOException("Error parsing json data.", pe);
        } finally {
        	closeResponse(response);
        }
        
	}
	
	@Override
	public WebResponse doPost(String url, HttpEntity mpEntity) throws IOException {
		return WebResponse.fromJSON(jsonPost(url, mpEntity));
	}

	@Override
	public WebResponse doPost(URL url, HttpEntity mpEntity) throws IOException {
		return doPost(url.toString(), mpEntity);
	}
	
	@Override
	public WebResponse doPost(URI uri, HttpEntity mpEntity) throws IOException {
		return doPost(uri.toString(), mpEntity);
	}
	
	public static void closeResponse(CloseableHttpResponse response) throws IOException {
		if(response != null)
			response.close();
	}

	@Override
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
	
	@Override
	public WebResponse postFile(String url, File thefile) throws IOException {
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
	
	
	@SuppressWarnings("deprecation")
	public static JSONObject responseJSON(HttpResponse response) throws IOException, org.json.simple.parser.ParseException {
		HttpEntity entity = response.getEntity();
		JSONObject retval = null;
		try {
			InputStream jsoncontent = entity.getContent();
			retval = (JSONObject) jsonParser.parse(new InputStreamReader(jsoncontent));
		} finally {
			if(entity != null)
				entity.consumeContent(); // TODO: Replace this. Once refactoring is done, the console version can be up-to-date, while android uses its own. Thus no longer need this.
		}
		return retval;
	}
	
	@Override
	public GenericBase64 getEncoder() {
		return ConsoleHTTPSClient.encoder;
	}

}
