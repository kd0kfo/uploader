package com.davecoss.uploader;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JDialog;

import com.davecoss.java.plugin.PluginException;
import com.davecoss.java.plugin.PluginInitException;
import com.davecoss.java.plugin.StoragePlugin;

public class Plugin implements StoragePlugin {
	
	private HTTPSClient client;
	private File jarfile;
	
	@Override
	public void init(Console console) throws PluginInitException {
		URI uri = null;
		try {
			uri = new URI(console.readLine("URL: "));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new PluginInitException("Error reading URL", e);
		}
		
		String keystorePath = useKeystore(console);
		if(keystorePath != null) {
			char[] password = null;
			try {
				password = console.readPassword("Keystore Password: ");
				if(password == null)
					throw new PluginInitException("Error reading password");
				client = new HTTPSClient(keystorePath, password);
			} catch(Exception e) {
				e.printStackTrace();
				throw new PluginInitException("Error reading URL", e);
			} finally {
				if(password != null) {
					for(int idx = 0;idx<password.length;idx++)
						password[idx] = 0;
				}
			}
		} else {
			client = new HTTPSClient();
		}
		
		try {
			client.startClient(HTTPSClient.createCredentialsProvider(console, uri), uri);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new PluginInitException("Error loading credentials", ioe);
		}
		
		
		
	}
	
	@Override
	public void init(PrintStream output, InputStream input)
			throws PluginInitException {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(JDialog parent) throws PluginInitException {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() throws PluginException {
		if(client != null)
		{
			try {
				client.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new PluginException("Error closing client", ioe);
			}
		}
		client = null;
	}

	@Override
	public File get_jarfile() {
		return jarfile;
	}

	@Override
	public File set_jarfile(File file) {
		jarfile = file;
		return jarfile;
	}

	@Override
	public String get_protocol() {
		return "davecoss";
	}

	@Override
	public URI mkdir(String path) {
		return null;
	}

	@Override
	public boolean isFile(URI uri) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean exists(URI uri) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public URI[] listFiles(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI saveStream(InputStream input, int amount_to_read, URI destination)
			throws PluginException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getInputStream(URI uri) throws PluginException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getOutputStream(URI uri) throws PluginException {
		// TODO Auto-generated method stub
		return null;
	}

	private static String useKeystore(Console console) {
		String response = console.readLine("Use Keystore [y/n]?");
		response = response.trim().toLowerCase();
		if(response.length() == 0 || response.charAt(0) != 'y')
			return null;
		
		return console.readLine("File path: ");
		
	}
}
