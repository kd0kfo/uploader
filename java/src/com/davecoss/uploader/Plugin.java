package com.davecoss.uploader;

import java.awt.FlowLayout;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import com.davecoss.java.GUIUtils;
import com.davecoss.java.Logger;
import com.davecoss.java.plugin.PluginException;
import com.davecoss.java.plugin.PluginInitException;
import com.davecoss.java.plugin.StoragePlugin;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.java.utils.JDialogCredentialPair;
import com.davecoss.uploader.utils.ConsoleHTTPSClient;

public class Plugin implements StoragePlugin {
	
	static Logger L = Logger.getInstance();
	
	private WebFS webfs = null;
	private File jarfile = null;
	private String baseURI = "";
	private ArrayList<String> tempFiles = new ArrayList<String>();
	
	@Override
	public void init(Console console) throws PluginInitException {
		URI uri = null;
		try {
			uri = new URI(console.readLine("URL: "));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new PluginInitException("Error reading URL", e);
		}
		
		HTTPSClient client;
		String keystorePath = useKeystore(console);
		if(keystorePath != null) {
			char[] password = null;
			try {
				password = console.readPassword("Keystore Password: ");
				if(password == null)
					throw new PluginInitException("Error reading password");
				client = new ConsoleHTTPSClient(keystorePath, password);
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
			client = new ConsoleHTTPSClient();
		}
		
		CredentialPair creds = null;
		try {
			creds = CredentialPair.fromInputStream(System.in);
			client.startClient(ConsoleHTTPSClient.createCredentialsProvider(creds, uri), uri);
			webfs = new WebFS(client);
			webfs.setClient(client);
			webfs.setBaseURI(uri);
			webfs.downloadConfig();
		} catch (Exception e) {
			throw new PluginInitException("Error starting WebFS", e);
		} finally {
			if(creds != null)
				creds.destroyCreds();
		}
		System.out.println("Version: " + webfs.getServerInfo().get("version"));
		
		
		baseURI = webfs.getBaseURI().toString();
		if(baseURI.charAt(baseURI.length() - 1) == '/')
			baseURI = baseURI.substring(0, baseURI.length() - 1);
	}
	
	@Override
	@Deprecated
	public void init(PrintStream output, InputStream input)
			throws PluginInitException {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(JDialog parent) throws PluginInitException {
		baseURI = (String)JOptionPane.showInputDialog(parent, "Enter Base URL", JOptionPane.PLAIN_MESSAGE);
		if(baseURI == null)
			throw new PluginInitException("Missing Base URI");
		
		int useKeystore = JOptionPane.showConfirmDialog(
			    parent,
			    "Add keystore?",
			    "Use an extra keystore?",
			    JOptionPane.YES_NO_OPTION);
		
		HTTPSClient client = null;
		if(useKeystore == JOptionPane.YES_OPTION) {
			File keystorePath = GUIUtils.select_file(parent);
			char[] passphrase = getPassphrase(parent);
			if(passphrase == null)
				throw new PluginInitException("Error reading password");
			try {
				client = new ConsoleHTTPSClient(keystorePath.getAbsolutePath(), passphrase);
			} catch (Exception e) {
				throw new PluginInitException("Error starting HTTPSClient", e);
			}
		} else {
			client = new ConsoleHTTPSClient();
		}
		
		CredentialPair creds = null;
		try {
			URI uri = new URI(baseURI);
			creds = JDialogCredentialPair.showInputDialog(parent);
			client.startClient(ConsoleHTTPSClient.createCredentialsProvider(creds, uri), uri);
			webfs = new WebFS(client);
			webfs.setBaseURI(uri);
			webfs.downloadConfig();
		} catch (Exception e) {
			throw new PluginInitException("Error starting WebFS", e);
		} finally {
			if(creds != null)
				creds.destroyCreds();
		}
		System.out.println("Version: " + webfs.getServerInfo().get("version"));
	}

	@Override
	public void destroy() throws PluginException {
		if(webfs != null)
		{
			try {
				if(tempFiles != null && tempFiles.size() > 0)
				{
					Iterator<String> it = tempFiles.iterator();
					while(it.hasNext()) {
						String tempFile = it.next();
						WebResponse response = webfs.merge(tempFile);
						if(response.status != 0)
						{
							L.warn(String.format("Could not merge temporary files for %s. Reason: %s", tempFile, response.message));
							continue;
						}
						webfs.clean(tempFile);
						if(response.status != 0)
							L.warn(String.format("Could not clean temporary files for %s. Reason: %s", tempFile, response.message));
					}
				}
				webfs.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new PluginException("Error closing client", ioe);
			}
		}
		webfs = null;
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
	public URI mkdir(String path) throws PluginException {
		try {
			webfs.mkdir(path);
			return pathToURI(path);
		} catch (Exception e) {
			throw new PluginException("Error making directory: " + path, e);
		}
	}

	@Override
	public boolean isFile(URI uri) throws PluginException {
		try {
			WebResponse response = webfs.ls(uri.getPath());
			if(response.status != WebResponse.SUCCESS)
				throw new Exception(response.message);
			WebFile file = response.webfile;
			return file.isFile();
		} catch (Exception e) {
			throw new PluginException("Error getting file information for " + uri.toString(), e);
		}
	}

	@Override
	public boolean exists(URI uri) throws PluginException {
		try {
			WebResponse response = webfs.ls(uri.getPath());
			if(response.status != WebResponse.SUCCESS)
				throw new Exception(response.message);
			WebFile file = response.webfile;
			return file != null;
		} catch(WebFileException wfe) {
			L.info("Caught WebFileException and assuming it means there is no file. Message: " + wfe.getMessage());
			return false;
		} catch (Exception e) {
			throw new PluginException("Error getting file information for " + uri.toString(), e);
		}
	}

	@Override
	public URI[] listFiles(URI uri) throws PluginException {
		try {
			WebResponse response = webfs.ls(uri.getPath());
			if(response.status != WebResponse.SUCCESS)
				throw new Exception(response.message);
			WebFile dir = response.webfile;
			WebFile[] dirents = dir.listFiles();
			int size = dirents.length;
			URI[] retval = new URI[size];
			for(int idx = 0;idx<size;idx++) {
				retval[idx] = new URI(baseURI + dirents[idx].getAbsolutePath());
			}
			return retval;
		} catch (Exception e) {
			throw new PluginException("Error getting file information for " + uri.toString(), e);
		}
	}

	@Override
	public URI saveStream(InputStream input, int amount_to_read, URI destination)
			throws PluginException {
		L.info("calling saveStream");
		OutputStream output = getOutputStream(destination);
		byte[] buffer = new byte[4096];
		int amountRead = -1;
		try {
			while((amountRead = input.read(buffer, 0, 4096)) != -1)
				output.write(buffer, 0, amountRead);
			output.flush();
		} catch(IOException ioe) {
			throw new PluginException("Error writing output to " + destination, ioe);
		} finally {
			if(output != null)
				try {
					output.close();
				} catch (IOException ioe) {
					throw new PluginException("Error uploading stream", ioe);
				}
		}
		try {
			L.info("saveStream is about to merge " + destination);
			File filepath = new File(destination.getPath());
			String name = filepath.getName();
			WebResponse response = webfs.merge(name);
			if(response.status != 0)
			{
				L.debug("Unable to clean upload segemnts for destination: " + response.message);
				tempFiles.remove(name);
			}
		} catch (IOException e) {
			L.error("Unable to clean upload segemnts for destination: " + e.getMessage());
		}
		return destination;
	}

	@Override
	public InputStream getInputStream(URI uri) throws PluginException {
		// TODO Auto-generated method stub
		L.debug("Called getInputStream");
		return null;
	}

	@Override
	public OutputStream getOutputStream(URI uri) throws PluginException {
		L.debug("Called getOutputStream");
		try {
			File filepath = new File(uri.getPath());
			String name = filepath.getName();
			tempFiles.add(String.format("/%s/%s", name.substring(0, 2), name));// account for fanout, add directory
			return new UploadOutputStream(name, webfs.getClient(), baseURI + "/upload.php?fanout=1");
		} catch (IOException e) {
			throw new PluginException("Error opening Upload Stream", e);
		}
	}

	private static String useKeystore(Console console) {
		String response = console.readLine("Use Keystore [y/n]?");
		response = response.trim().toLowerCase();
		if(response.length() == 0 || response.charAt(0) != 'y')
			return null;
		
		return console.readLine("File path: ");
		
	}
	
	private URI pathToURI(String path) throws URISyntaxException {
		String uri = path;
		if(path.charAt(0) != '/')
			uri = "/" + path;
		uri = baseURI + uri;
		return new URI(uri);
	}
	
	public char[] getPassphrase(JDialog parent) {
		JPasswordField jPassphrase = new JPasswordField(10);
		JLabel label = new JLabel("Passphrase: ");
		label.setLabelFor(jPassphrase);
		JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        textPane.add(label);
        textPane.add(jPassphrase);
		JOptionPane.showMessageDialog(parent, textPane);
		
		return jPassphrase.getPassword();
	}
}
