package com.davecoss.uploader.utils;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.davecoss.java.BuildInfo;
import com.davecoss.java.ConsoleLog;
import com.davecoss.java.LogHandler;
import com.davecoss.java.utils.CLIOptionTuple;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.WebFile;
import com.davecoss.uploader.WebFileException;

public class UploaderConsole {

	public enum Commands {RM, GET, HELP, HISTORY, JSON, LS, MD5, MERGE, MKDIR, MV, PUT, SERVERINFO, EXIT};
	
	private static LogHandler Log =  new ConsoleLog("UploaderConsole");
	private static final JSONParser jsonParser = new JSONParser();
	
	private ArrayList<String> history = new ArrayList<String>();
	private final Console console;
	private URI baseURI = null;
	private JSONObject serverInfo = null;
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
	
		printVersionInfo(uri);
		System.out.println("Run \"help\" to get a list of commands.");

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
    	uc.downloadConfig(uri.toString() + "/info.php");
    	
    	String line = null;
    	while((line = console.readLine("> ")) != null) {
		line = line.trim();
    		if(line.length() == 0)
    			continue;

		// If first character is a '!', lookup history based on index that follows '!'
		if(line.charAt(0) == '!') {
			String historyId = line.substring(1);
			line = uc.getPastCommand(historyId);
			if(line == null) {
				console.printf("List '%s' is not in the history list\n", historyId);
				continue;
			}
		}
    		String[] tokens = line.split(" ");
		String smallCmd = tokens[0].toLowerCase();
    		if(!smallCmd.equals("history"))
			uc.history.add(line);
		if(smallCmd.equals("exit"))
    			break;
    		try {
			uc.runCommand(tokens);
		} catch(IOException ie) {
			ie.printStackTrace();
		}
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

	public void printHelp(PrintStream output, String[] queries) {
		if(queries == null)
		{
			Commands[] cmds = Commands.values();
			queries = new String[cmds.length];
			for(int idx = 0;idx < cmds.length;idx++)
				queries[idx] = cmds[idx].name();
		}
		if(output == null)
			output = System.out;

		Commands cmd = Commands.HELP;
		String msg = "";
		for(String query : queries) {
			msg = query.toLowerCase() + " -- ";
			query = query.toUpperCase();
			try {
				cmd = Commands.valueOf(query);
			} catch(IllegalArgumentException iae) {
				output.println("Known command " + query);
				continue;
			}
			switch(cmd) {
			case RM:
				msg += "Delete specified file";
				break;
			case EXIT:
				msg += "Leave the console.";
				break;
			case GET:
				msg += "Download file to current working directory";
				break;
			case HELP:
				msg += "Get help for a given command";
				break;
			case HISTORY:
				msg += "Print list of commands run";
				break;
			case JSON:
				msg += "Get raw json file info for path";
				break;
			case LS:
				msg += "List file(s) for the provided path";
				break;
			case MD5:
				msg += "Print the MD5 hash for the specified file";
				break;
			case MERGE:
				msg += "Merge all files with the specified prefix";
				break;
			case MKDIR:
				msg += "Make a directory";
				break;
			case MV:
				msg += "Move file.";
				break;
			case PUT:
				msg += "Put a file on the server.";
				break;
			case SERVERINFO:
				msg += "Prints information about the server";
				break;
			}
			output.println(msg);
		}
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
		
		URI baseuri = getBaseURI();
		if(baseuri == null)
			throw new IOException("Missing Base URI for Communication");
		String currURL = baseuri.toString();
		
		String path = "/";
		if(numArgs > 0)
			path = tokens[1];
		CloseableHttpResponse response = null;
		switch(command) {
		case GET: case PUT:
		{
			if(numArgs == 0)
			{
				System.out.println("Missing file");
				break;
			}
			if(command == Commands.GET)
				downloadFile(path, null);
			else if(command == Commands.PUT)
				putFile(new File(path));
			break;
		}
		case HELP:
		{
			String[] query = null;
			if(numArgs > 0)
				query = Arrays.copyOfRange(tokens, 1, tokens.length);
			printHelp(System.out, query);
			break;
		}
		case HISTORY:
		{
			
			for(int counter = 0;counter < history.size();counter++)
				console.printf("%d: %s\n", counter, history.get(counter));
			break;
		}
		case JSON:
		{
			if(numArgs == 0)
				break;
			try {
				currURL += "/ls.php?filename=" + tokens[1];
				response = client.doGet(currURL);
				JSONObject json = responseJSON(response);
				System.out.println(json.toJSONString());
			} catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
			} finally {
				closeResponse(response);
			}
			break;
		}
		case LS:
		{
			currURL += "/ls.php?filename=" + path;
			response = client.doGet(currURL);
			JSONObject json;
			try {
				json = responseJSON(response);
				if(json.containsKey("message"))
				{
					System.out.println((String)json.get("message"));
					break;
				}
				WebFile file = WebFile.fromJSON(json);
				if(file == null)
					break;
				console.printf("%s\n", file.dirListing());
				if(json.containsKey("dirents"))
					printDir((JSONArray)json.get("dirents"), console);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		case MD5:
		{
			currURL += "/md5.php?filename=" + path;
			response = client.doGet(currURL);
			JSONObject json;
			try {
				json = responseJSON(response);
				if(json.containsKey("status") && ((Long)json.get("status")) != 0)
				{
					console.printf("Error: %s\n", (String)json.get("message"));
					break;
				}
				if(json.containsKey("md5"))
				{
					console.printf("%s", (String)json.get("md5"));
					if(json.containsKey("filename"))
						console.printf("\t%s", (String)json.get("filename"));
					console.printf("\n");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		case MERGE: case RM:
		{
			if(command == Commands.MERGE)
				currURL += "/merge.php?filename=" + path;
			else
				currURL += "/delete.php?filename=" + path;
			response = client.doGet(currURL);
			JSONObject json;
			try {
				json = responseJSON(response);
				if(json.containsKey("status") && ((Long)json.get("status")) != 0)
				{
					console.printf("%s failed: ", command.name().toLowerCase());
				}
				if(json.containsKey("message"))
					console.printf("%s\n", (String)json.get("message"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		case MV:
		{
			if(numArgs < 2)
			{
				System.out.println("mv requires a source and destination path");
				break;
			}
			moveFile(tokens[1], tokens[2]);
			break;
		}
		case MKDIR:
		{
			if(numArgs == 0)
			{
				System.out.println("mkdir requires a directory name");
				break;
			}
			mkdir(tokens[1]);
			break;
		}
		case SERVERINFO:
		{
			if(serverInfo == null)
				break;
			@SuppressWarnings("unchecked")
			Iterator<Object> it = serverInfo.keySet().iterator();
			String key = null;
			while(it.hasNext()) {
				key = (String)it.next();
				console.printf("%s: %s\n", key, (String)serverInfo.get(key));
			}
			break;
		}
		case EXIT:
			break;
		}
		
		closeResponse(response);
	}

	private void mkdir(String newDirectory) throws IOException {
		String dirURL = baseURI.toString() + "/mkdir.php?dirname=" + newDirectory;
		CloseableHttpResponse response = client.doGet(dirURL);
		JSONObject json = null;
		try {
			json = responseJSON(response);
			if(json.containsKey("status") && ((Long)json.get("status")) != 0)
			{
				System.out.println("Error making directory: " + newDirectory);
				System.out.println("Reason: " + (String)json.get("message"));
				return;
			}
		} catch(org.json.simple.parser.ParseException pe) {
			throw new IOException("Error making directory", pe);
		} finally {
			closeResponse(response);
		}
	}

	private void moveFile(String source, String destination) throws IOException {
		// Get info for file and verify that it is a file.
		String sourceURL = baseURI.toString() + "/ls.php?filename=" + source;
		CloseableHttpResponse response = client.doGet(sourceURL);
		JSONObject json = null;
		try {
			json = responseJSON(response);
			if(json.containsKey("status") && ((Long)json.get("status")) != 0)
			{
				System.out.println("Move failed: " + (String)json.get("message"));
				return;
			}	
			closeResponse(response);
			
			sourceURL = baseURI.toString() + "/move.php?source=" + source + "&destination=" + destination;
			response = client.doGet(sourceURL);
			json = responseJSON(response);
			if(json.containsKey("status") && ((Long)json.get("status")) != 0)
			{
				System.out.println("Move failed: " + (String)json.get("message"));
			}
			
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			closeResponse(response);
		}
	}

	private void putFile(File file) throws IOException {
		if(!file.exists())
		{
			System.out.println("File not found: " + file.getPath());
			return;
		}
		client.postFile(baseURI.toString() + "/upload.php", file);
	}

	private void downloadFile(String source, File dest) throws IOException {
		if(dest == null)
		{
			// if Dest is null, simply use the name of the source file.
			dest = new File(source);
			dest = new File(dest.getName());
		}
		
		// Get info for file and verify that it is a file.
		String contentdir = (String)serverInfo.get("contentdir");
		String sourceURL = baseURI.toString() + "/ls.php?filename=" + source;
		CloseableHttpResponse response = client.doGet(sourceURL);
		HttpEntity entity = null;
		JSONObject json = null;
		InputStream input = null;
		FileOutputStream output = null;
		try {
			json = responseJSON(response);
			String type = (String)json.get("type");
			if(!type.equals("f"))
			{
				System.out.println("Cannot download " + source);
				return;
			}
			sourceURL = baseURI.toString() + contentdir + (String)json.get("parent") + "/" + (String)json.get("name");
			System.out.println("Path: " + sourceURL);
			closeResponse(response);
			
			// Download content.
			response = client.doGet(sourceURL);
			entity = response.getEntity();
			
			// Write content to file
			input = entity.getContent();
			output = new FileOutputStream(dest);
			byte[] buffer = new byte[4096];
			int amountRead = -1;
			while((amountRead = input.read(buffer, 0, 4096)) != -1)
				output.write(buffer,0, amountRead);
		} catch(org.json.simple.parser.ParseException pe) {
			throw new IOException("Error getting file information", pe);
		} finally {
			// Cleanup
			if(entity != null)
				EntityUtils.consume(entity);
			closeResponse(response);
			if(output != null)
				output.close();
		}
		
	}

	public void downloadConfig(String url) throws IOException {
		CloseableHttpResponse response = client.doGet(url);
		JSONObject json = null;
		try {
			json = responseJSON(response);
		} catch(org.json.simple.parser.ParseException pe) {
			throw new IOException("Unable to load server config", pe);
		} finally {
			closeResponse(response);
		}
	
		this.serverInfo = json;
		if(!this.serverInfo.containsKey("contentdir"))
			throw new IOException("Unable to get a valid server configuration.");
	}

	public static JSONObject responseJSON(HttpResponse response) throws IOException, org.json.simple.parser.ParseException {
		HttpEntity entity = response.getEntity();
		JSONObject retval = null;
		try {
			InputStream jsoncontent = entity.getContent();
			retval = (JSONObject) jsonParser.parse(new InputStreamReader(jsoncontent));
		} finally {
			if(entity != null)
				EntityUtils.consume(entity);
		}
		return retval;
	}

	public static void printDir(JSONArray dirents, Console console) throws WebFileException {
		@SuppressWarnings("rawtypes")
		Iterator it = dirents.iterator();
		JSONObject dirent = null;
		while(it.hasNext()) {
			dirent = (JSONObject)it.next();
			WebFile file = WebFile.fromJSON(dirent);
			console.printf("%s\n", file.dirListing());
		}
	}

	public static void printVersionInfo(URI uri) {
		BuildInfo bi = new BuildInfo(HTTPSClient.class);
		String msg = "Running Uploader Console version " + bi.get_version();

		Properties props = bi.get_build_properties();
		if(props.containsKey("build_date"))
			msg += "  Built on " + props.get("build_date");

		System.out.println(msg);

		if(uri != null)
			System.out.println("Connecting to " + uri.toString());
	}
	
	private void closeResponse(CloseableHttpResponse response) throws IOException {
		if(response != null)
			response.close();
	}

	private String getPastCommand(String idxString) {
		String retval = null;
		try {
			int idx = Integer.parseInt(idxString);
			if(idx < history.size() && idx >= 0)
				retval = history.get(idx);
		} catch(NumberFormatException nfe) {
			retval = null;
		}
		return retval;
	}
}
