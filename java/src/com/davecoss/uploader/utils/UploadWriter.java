package com.davecoss.uploader.utils;

import java.io.Console;
import java.io.OutputStream;
import java.net.URI;
import com.davecoss.java.ConsoleLog;
import com.davecoss.java.GenericBase64;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CLIOptionTuple;
import com.davecoss.java.utils.CredentialPair;
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
	    	
			// Parse args
			String keystoreFilename = null;
			CredentialsProvider credsProvider = null;
			UploadOutputStream consoleUploader = null;
			if(cmd.hasOption("basic")) {
				String username = console.readLine("Username: ");
				char[] pass = console.readPassword("Passphrase: ");
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
	    	
	    	
	    	// Running on Console?
	    	OutputStream outputStream = null;
	    	consoleUploader = new UploadOutputStream(filename, client, uri.toURL().toString() + "/postdata.php");
	    	outputStream = consoleUploader;
			
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
	        		WebFS webfs = new WebFS(client);
	        		webfs.setBaseURI(uri);
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