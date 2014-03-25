package com.davecoss.uploader.utils;

import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import com.davecoss.java.ConsoleLog;
import com.davecoss.java.GenericBase64;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CLIOptionTuple;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.DownloadInputStream;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.WebResponse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


import org.apache.http.client.CredentialsProvider;

public class DownloadReader {
	
	static Logger L = ConsoleLog.getInstance("HTTPSClient");
	
	public static final CLIOptionTuple[] optionTuples = {new CLIOptionTuple("base64", false, "Console writing should use base64 encoding (Default: off)"),
		new CLIOptionTuple("basic", false, "Use basic authentication. (Default: off)"),
		new CLIOptionTuple("d", true, "Set Debug Level (Default:  ERROR)"),
		new CLIOptionTuple("help", false, "Usage information."),
		new CLIOptionTuple("o", true, "Save output as file."),
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
	    			System.out.println("DownloadReader -- Read Text from Web File System");
	    			System.out.println("Usage: DownloadReader [options] <Web Filesystem URL> <Web File Name>");
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
			String downloadPath = args[1];
			if(downloadPath.charAt(0) != '/') {
				downloadPath = "/" + downloadPath;
			}
			OutputStream output = System.out;
			
			// Parse args
			String keystoreFilename = null;
			CredentialsProvider credsProvider = null;
			DownloadInputStream consoleDownloader = null;
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
			if(cmd.hasOption("o")) {
				output = new BufferedOutputStream(new FileOutputStream(cmd.getOptionValue("o")));
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
	    	WebFS webfs = new WebFS(client);
	    	webfs.setBaseURI(uri);
	    	webfs.downloadConfig();
    		String serverSalt = (String)webfs.getServerInfo().get("salt");
    		webfs.setCredentials(new Credentials(username, passphrase, serverSalt));
	    	WebResponse logonResponse = webfs.logon();
	    	if(logonResponse.status != WebResponse.SUCCESS)
	    	{
	    		System.err.println("Error logon on");
	    		System.err.println(logonResponse.message);
	    		System.exit(logonResponse.status);
	    	}
	    	
	    	// Running on Console?
	    	InputStream inputStream = null;
	    	consoleDownloader = webfs.openDownloadStream(downloadPath);
	    	inputStream = consoleDownloader;
			
	    	if(cmd.hasOption("base64")) {
	    		inputStream = client.getEncoder().decodeInputStream(inputStream);
	    	}
	    	
	        try {
	        	if(consoleDownloader != null && inputStream != null) {
	        		byte[] buffer = new byte[4096];
	        		int amountRead = -1;
	        		while((amountRead = inputStream.read(buffer)) != -1)
	        			output.write(buffer, 0, amountRead);
	        	}
	        } finally {
	            client.close();
	            if(inputStream != null)
	            	inputStream.close();
	            if(output != null) {
	            	output.flush();
	            	if(args.length > 1)
	            		output.close();
	            }
	        }
	    }
	
}