package com.davecoss.uploader.utils;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.davecoss.java.ConsoleLog;
import com.davecoss.java.LogHandler;
import com.davecoss.java.utils.CLIOptionTuple;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.WebFile;
import com.davecoss.uploader.WebFileException;

public class UploaderConsole {

	public enum Commands {LS};
	
	private static LogHandler Log =  new ConsoleLog("UploaderConsole");
	private static final JSONParser jsonParser = new JSONParser();
	
	private final Console console;
	private URI baseURI = null;
	private HTTPSClient client = null;
	
	public UploaderConsole(Console console) {
		this.console = console;
	}

	public HTTPSClient getClient() {
		return client;
	}

	public void setClient(HTTPSClient client) {
		this.client = client;
	}
	
	public static void main(String[] cliArgs) throws Exception {
		
		final Console console = System.console();

		CLIOptionTuple[] optionTuples = HTTPSClient.optionTuples;
		
		CommandLine cmd = null;
		
    	try {
    		cmd = HTTPSClient.parseCommandLine(cliArgs, optionTuples);
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
		if(cmd.hasOption("basic")) {
			credsProvider = HTTPSClient.createCredentialsProvider(console, uri);
		}
		if(cmd.hasOption("d")) {
			Log.setLevel(LogHandler.Level.DEBUG);
		}
		if(cmd.hasOption("f")) {
			for(String filename : cmd.getOptionValues("f")) {
				filesToUpload.add(new File(filename));
			}
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
    	
    	UploaderConsole uc = new UploaderConsole(console);
    	uc.setClient(client);
    	uc.setBaseURI(uri);
    	
    	String line = null;
    	while((line = console.readLine("> ")) != null) {
    		if(line.length() == 0)
    			continue;
    		if(line.toLowerCase().equals("exit"))
    			break;
    		String[] tokens = line.split(" ");
    		uc.runCommand(tokens);
    	}
    	
    	if(client != null)
    		client.close();
	}

	public URI getBaseURI() {
		return baseURI;
	}

	public void setBaseURI(URI baseURI) {
		this.baseURI = baseURI;
	}
	
	public void runCommand(String[] tokens) throws MalformedURLException, IOException {
		if(tokens == null || tokens.length == 0)
			return;
		
		String strCommand = tokens[0].toUpperCase();
		int numArgs = tokens.length - 1;
		
		Commands command = null;
		try {
			command = Commands.valueOf(strCommand);
		} catch(IllegalArgumentException iae) {
			console.printf("Unknown Command: %s\n", strCommand);
			return;
		}
		
		URL curr = getBaseURI().toURL();
		if(curr == null)
			throw new IOException("Missing Base URI for Communication");
		
		CloseableHttpResponse response = null;
		switch(command) {
		case LS:
		{
			String path = "/";
			if(numArgs > 0)
				path = tokens[1];
			curr = new URL(curr, "/ls.php?dir=" + path);
			response = client.doGet(curr.toString());
			JSONObject json;
			try {
				json = responseJSON(response);
				if(json.containsKey(path))
					printDir((JSONArray)json.get(path), console);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		}
		
		if(response != null)
			response.close();
	}

	public static JSONObject responseJSON(HttpResponse response) throws IOException, org.json.simple.parser.ParseException {
		HttpEntity entity = response.getEntity();
		InputStream jsoncontent = entity.getContent();
		
		return (JSONObject) jsonParser.parse(new InputStreamReader(jsoncontent));
	}
	
	public static void printDir(JSONArray dirents, Console console) throws WebFileException {
		@SuppressWarnings("rawtypes")
		Iterator it = dirents.iterator();
		JSONObject dirent = null;
		while(it.hasNext()) {
			dirent = (JSONObject)it.next();
			WebFile file = WebFile.fromJSON(dirent);
			console.printf("%s\t%s\n", file.humanType(), file.name);
		}
	}
}
