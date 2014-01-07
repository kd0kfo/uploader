package com.davecoss.java.http;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import com.davecoss.java.ConsoleLog;
import com.davecoss.java.LogHandler;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
	
	private static LogHandler Log = new ConsoleLog("HTTPSClient");
	
	private SSLContext sslcontext = null;
	private SSLConnectionSocketFactory sslsf = null;
	private CloseableHttpClient httpclient = null;
	private KeyStore keystore = null;
	
	public HTTPSClient() {
		httpclient = HttpClients.createDefault();
	}
	
	public HTTPSClient(String keystoreFilename, char[] passphrase) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		loadKeystore(keystoreFilename, passphrase);
		buildContext();
		initSSLSocketFactory();
		startClient();
	}
	
	public void loadKeystore(String keystoreFilename, char[] passphrase) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		keystore  = KeyStore.getInstance(KeyStore.getDefaultType());
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
	    
        System.out.println("executing request " + httpget.getRequestLine());

        return httpclient.execute(httpget);
        
	}
	
	public CloseableHttpResponse doPost(String url, HttpEntity mpEntity) throws IOException {
		HttpPost httppost = new HttpPost(url);
	    
        System.out.println("executing request " + httppost.getRequestLine());

        if(mpEntity != null)
        	httppost.setEntity(mpEntity);
        
        return httpclient.execute(httppost);
        
	}

	
	public void close() throws IOException {
		if(httpclient != null)
			httpclient.close();
	}

    public final static void main(String[] cli_args) throws Exception {
    	
    	Console console = System.console();
    	
    	// Define args
		Options options = new Options();
		options.addOption("basic", false, "Use basic authentication. (Default: off");
		options.addOption("d", false, "Set Debug Level (Default:  ERROR)");
		options.addOption("f", true, "POST File");
		options.addOption("ssl", true, "Specify Keystore");
		
		CommandLineParser parser = new GnuParser();
		CommandLine cmd = null;
		try
		{
			cmd = parser.parse( options, cli_args);
		}
		catch(ParseException pe)
		{
			System.err.println("Error parsing command line arguments.");
			System.err.println(pe.getMessage());
			System.exit(1);
		}
		String[] args = cmd.getArgs();
		URI uri = new URI(args[0]);
    	
		// Parse args
		String keystoreFilename = null;
		HttpEntity mpEntity = null;
		CredentialsProvider credsProvider = null;
		if(cmd.hasOption("basic")) {
			String username = console.readLine("Username: ");
			char[] password = console.readPassword("Password: ");
			credsProvider = new BasicCredentialsProvider();
	        credsProvider.setCredentials(
	                new AuthScope(uri.getHost(), uri.getPort()),
	                new UsernamePasswordCredentials(username, new String(password)));
	        for(int idx = 0;idx < password.length;idx++)
	        	password[idx] = 0;
		}
		if(cmd.hasOption("d")) {
			Log.setLevel(LogHandler.Level.DEBUG);
		}
		if(cmd.hasOption("f")) {
			MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
			File thefile = new File(cmd.getOptionValue("f"));
			FileBody fb = new FileBody(thefile);
			entityBuilder.addPart("file", fb);
			mpEntity = entityBuilder.build();
			Log.debug("Attaching File " + thefile.getName());
		}
		if(cmd.hasOption("ssl")) {
			keystoreFilename = cmd.getOptionValue("ssl");
		}

    	HTTPSClient client = null;
    	
    	if(keystoreFilename != null) {
	    	System.out.print("Keystore Passphrase? ");
	    	char[] passphrase = console.readPassword();
	    	
	    	client = new HTTPSClient(keystoreFilename, passphrase);
	    	for(int i = 0;i<passphrase.length;i++)
	    		passphrase[i] = 0;
    	} else {
    		client = new HTTPSClient();
    	}
    	
    	if(credsProvider != null)
    		client.startClient(credsProvider, uri);
    	
        try {
        	CloseableHttpResponse response = client.doPost(uri.toURL().toString(), mpEntity);
        	try {
                HttpEntity entity = response.getEntity();

                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                if (entity != null) {
                    System.out.println("Response content length: " + entity.getContentLength());
                    InputStream htmlcontent = entity.getContent();
                    byte[] buf = new byte[4096];
                    int amount_read = -1;
                    while((amount_read = htmlcontent.read(buf, 0, 4096)) != -1) {
                    	System.out.write(buf, 0, amount_read);
                    }
                }
                EntityUtils.consume(entity);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

}