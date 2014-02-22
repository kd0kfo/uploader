package com.davecoss.uploader.utils;

import java.io.Console;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.net.URI;
import java.net.URL;

import com.davecoss.java.ConsoleLog;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CLIOptionTuple;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.uploader.DataPoster;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.UploadOutputStream;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.WebResponse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.codec.binary.Base64OutputStream;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

public class UploadWriter {
	
	static Logger L = ConsoleLog.getInstance("HTTPSClient");
	
	public static final CLIOptionTuple[] optionTuples = {new CLIOptionTuple("base64", false, "Console writing should use base64 encoding (Default: off)"),
		new CLIOptionTuple("basic", false, "Use basic authentication. (Default: off)"),
		new CLIOptionTuple("d", true, "Set Debug Level (Default:  ERROR)"),
		new CLIOptionTuple("filename", true, "Write to the console and upload the file name provided as an argument to the -console flag."),
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
			CredentialsProvider credsProvider = null;
			ArrayList<File> filesToUpload = new ArrayList<File>();
			UploadOutputStream consoleUploader = null;
			if(cmd.hasOption("basic")) {
				String username = console.readLine("Username: ");
				char[] pass = console.readPassword("Passphrase: ");
				CredentialPair creds = new CredentialPair(username, pass);
				credsProvider = HTTPSClient.createCredentialsProvider(creds, uri);
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
		    	
		    	client = new HTTPSClient(keystoreFilename, passphrase);
		    	for(int i = 0;i<passphrase.length;i++)
		    		passphrase[i] = 0;
	    	} else {
	    		client = new HTTPSClient();
	    	}
	    	
	    	if(credsProvider != null)
	    		client.startClient(credsProvider, uri);
	    	
	    	
	    	// Running on Console?
	    	OutputStream outputStream = null;
	    	String filename = null;
	    	if(cmd.hasOption("filename")) {
	    		filename = cmd.getOptionValue("filename");
				consoleUploader = new DataPoster(filename, client, uri.toURL().toString() + "/postdata.php");
	    		outputStream = consoleUploader;
			}
	    	if(cmd.hasOption("base64")) {
	    		outputStream = new Base64OutputStream(outputStream);
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