package com.davecoss.uploader.utils;

import java.io.Console;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import com.davecoss.java.ConsoleLog;
import com.davecoss.java.GenericBaseN;
import com.davecoss.java.Logger;
import com.davecoss.java.plugin.NamedOutputStream;
import com.davecoss.java.plugin.Plugin;
import com.davecoss.java.plugin.PluginUtils;
import com.davecoss.java.plugin.Types;
import com.davecoss.java.utils.CLIOptionTuple;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.UploadOutputStream;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.WebResponse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


import org.apache.http.client.CredentialsProvider;

public class UploadWriter {
	
	static Logger L = ConsoleLog.getInstance("HTTPSClient");
	
	public static final CLIOptionTuple[] optionTuples = {new CLIOptionTuple("base64", false, "Console writing should use base64 encoding (Default: off)"),
		new CLIOptionTuple("basic", false, "Use basic authentication. (Default: off)"),
		new CLIOptionTuple("d", true, "Set Debug Level (Default:  ERROR)"),
		new CLIOptionTuple("help", false, "Usage information."),
		new CLIOptionTuple("plugin", true, "Provide a path to the plugin through which output will be written. (Default: off)"),
		new CLIOptionTuple("ssl", true, "Specify Keystore")};
	
	public static CommandLine parseCommandLine(String[] cli_args, CLIOptionTuple[] optionArray) throws ParseException {
		// Define args
		Options options = new Options();
		for(CLIOptionTuple option : optionArray )
			options.addOption(option.name, option.hasArg, option.helpMessage);
		
		CommandLineParser parser = new GnuParser();
		
		return parser.parse( options, cli_args);
	}
	
	public static CommandLine parseCommandLine(String[] cli_args) throws ParseException {
		return parseCommandLine(cli_args, optionTuples);
	}

	public final static void main(String[] cli_args) throws Exception {
	    	
			AuthHash.init(new CommonsBase64());
	    	Console console = System.console();
	    	CommandLine cmd = null;
			
	    	try {
	    		cmd = parseCommandLine(cli_args);
	    		if(cmd.hasOption("help")) {
	    			System.out.println("UploadWriter -- Write Text to Web File System");
	    			System.out.println("Usage: UploadWriter [options] <Web Filesystem URL> <File Name>");
	    			System.out.println("Options:");
	    			CLIOptionTuple.printOptions(System.out, optionTuples);
	    			System.exit(0);
	    		}
	    	}
			catch(ParseException pe)
			{
				System.err.println("Error parsing command line arguments.");
				System.err.println(pe.getMessage());
				System.exit(1);
			}
			String[] args = cmd.getArgs();
			if(args.length != 2) {
				System.err.println("Invalid number of arguments. Run \"-help\" to get usage information.");
				System.exit(1);
			}
			URI uri = new URI(args[0]);
			String filename = args[1];
			if(filename.charAt(0) != '/') {
				filename = "/" + filename;
			}
	    	
			// Parse args
			String keystoreFilename = null;
			CredentialsProvider credsProvider = null;
			UploadOutputStream consoleUploader = null;
			if(cmd.hasOption("basic")) {
				String username = console.readLine("Basic Auth Username: ");
				char[] pass = console.readPassword("Basic Auth Passphrase: ");
				CredentialPair creds = new CredentialPair(username, pass);
				credsProvider = ConsoleHTTPSClient.createCredentialsProvider(creds, uri);
				creds.destroyCreds();
			}
			if(cmd.hasOption("d")) {
				L.setLevel(Logger.parseLevel(cmd.getOptionValue("d").toUpperCase()));
			}
			if(cmd.hasOption("ssl")) {
				keystoreFilename = cmd.getOptionValue("ssl");
			}
	
	    	HTTPSClient client = null;
	    	
	    	if(keystoreFilename != null) {
		    	System.out.print("Keystore Passphrase? ");
		    	char[] passphrase = console.readPassword();
		    	
		    	client = new ConsoleHTTPSClient(keystoreFilename, passphrase);
		    	for(int i = 0;i<passphrase.length;i++)
		    		passphrase[i] = 0;
	    	} else {
	    		client = new ConsoleHTTPSClient();
	    	}
	    	
	    	if(credsProvider != null)
	    		client.startClient(credsProvider, uri);
	    	
	    	String username = console.readLine("WebFS Username: ");
	    	char[] passphrase = console.readPassword("WebFS Passphrase: ");
	    	String totpString = console.readLine("WebFS One Time Passcode: ");
	    	int totpToken = Integer.parseInt(totpString);
	    	WebFS webfs = new WebFS(client);
	    	webfs.setBaseURI(uri);
	    	webfs.downloadConfig();
    		webfs.setCredentials(new Credentials(username, passphrase));
	    	WebResponse logonResponse = webfs.logon(totpToken);
	    	if(logonResponse.status != WebResponse.SUCCESS)
	    	{
	    		System.err.println("Error logon on");
	    		System.err.println(logonResponse.message);
	    		System.exit(logonResponse.status);
	    	}
	    	
	    	// Running on Console?
	    	OutputStream outputStream = null;
	    	consoleUploader = webfs.openUploadStream(filename);
	    	outputStream = consoleUploader;
			
	    	if(cmd.hasOption("plugin")) {
	    		// Add jar to class loader
	    		File newjar = new File(cmd.getOptionValue("plugin"));
	    		URL u = new URL("jar", "", "file:" + newjar.getPath() + "!/");
	    		URLClassLoader urlcl = (URLClassLoader)ClassLoader.getSystemClassLoader();
	    		@SuppressWarnings("rawtypes")
	    		Class urladder = URLClassLoader.class;
	    		@SuppressWarnings("unchecked")
	    		Method urlmethod = urladder.getDeclaredMethod("addURL",new Class[] {URL.class});
	    		urlmethod.setAccessible(true);
	    		urlmethod.invoke(urlcl, new Object[]{u});

	    		// Get main class for plugin and return it
	    		String class_name = PluginUtils.get_main_classname(newjar);
	    		L.info("Loading Plugin class " + class_name);
	    		@SuppressWarnings("rawtypes")
	    		Class c = urlcl.loadClass(class_name);
	    		Plugin mainPlugin = (Plugin)c.newInstance();
	    		NamedOutputStream streamPlugin = (NamedOutputStream)mainPlugin.getPluginByType(Types.PluginTypes.NAMED_OUTPUT_STREAM).newInstance();
	    		streamPlugin.init(console);
	    		outputStream = streamPlugin.getOutputStream(outputStream, filename);
	    	}
	    	
	    	
	    	if(cmd.hasOption("base64")) {
	    		outputStream = client.getEncoder().encodeOutputStream(outputStream);
	    	}
	    	
	        try {
	        	if(consoleUploader != null && outputStream != null) {
	        		String line = null;
	        		while((line = console.readLine("> ")) != null) {
	        			outputStream.write(line.getBytes());
	        			outputStream.write('\n');
	        		}
	        		outputStream.close();
	        		
	        		L.info("Upload Response:");
	        		WebResponse uploadResponse = consoleUploader.getUploadResponse();
	        		if(uploadResponse != null)
	        			L.info(uploadResponse.message);
	        		
	        		String consoleFilename = filename;
	        		WebResponse status = webfs.merge(consoleFilename);
	        		if(status.status == 0)
	        		{
	        			status = webfs.clean(consoleFilename);
	        			L.info("Cleaned up segments. Status: " + status.message);
	        		}
	        	}
	        } finally {
	            client.close();
	        }
	    }
	
}
