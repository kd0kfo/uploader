package com.davecoss.uploader.utils;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.http.client.CredentialsProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.davecoss.java.BuildInfo;
import com.davecoss.java.ConsoleLog;
import com.davecoss.java.LogHandler;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CLIOptionTuple;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.WebFile;
import com.davecoss.uploader.WebFileException;
import com.davecoss.uploader.WebResponse;

public class UploaderConsole {

	public enum Commands {DEBUG, RM, GET, HELP, HISTORY, LS, MD5, MERGE, MKDIR, MV, PUT, SERVERINFO, EXIT};
	
	private static Logger L = ConsoleLog.getInstance("UploaderConsole");
	
	private ArrayList<String> history = new ArrayList<String>();
	private WebFS webfs = null;
	
	public static void printUsage(PrintStream out) {
		out.println("Usage: UploaderConsole [-basic] [-d LEVEL] [-ssl KEYSTORE] <URL>");
		out.println("Options:");
		out.println("-basic\t\tUses basic authentication.");
		out.println("-d LEVEL\tSets the debug level. (Default: ERROR)");
		out.println("-ssl KEYSTORE\tUses KEYSTORE to validate SSL certficates");
	}
	
	public static void main(String[] cliArgs) throws Exception {
		Console console = System.console();
		UploaderConsole uc = new UploaderConsole();
		
		if(cliArgs.length == 0) {
			System.out.println("For help and usage information, use the -help flag.");
			System.exit(1);
		}

		CLIOptionTuple[] optionTuples = Arrays.copyOf(HTTPSClient.optionTuples,
				HTTPSClient.optionTuples.length + 1);
		optionTuples[optionTuples.length - 1] = new CLIOptionTuple("help", false, "Print help and usage information");
		
		
		CommandLine cmd = null;
		
    	try {
    		cmd = HTTPSClient.parseCommandLine(cliArgs, optionTuples);
    		if(cmd.hasOption("help"))
    		{
    			printUsage(System.out);
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
    	URI uri = new URI(args[0]);
	
		printVersionInfo(uri);
		System.out.println("Run \"help\" to get a list of commands.");

    	// Parse args
		String keystoreFilename = null;
		CredentialsProvider credsProvider = null;
		ArrayList<File> filesToUpload = new ArrayList<File>();
		if(cmd.hasOption("basic")) {
			CredentialPair creds = null;
			try {
				creds = CredentialPair.fromInputStream(System.in);
				credsProvider = HTTPSClient.createCredentialsProvider(creds, uri);
			} finally {
				if(creds != null)
					creds.destroyCreds();
			}
		}
		if(cmd.hasOption("d")) {
			if(cmd.hasOption("d")) {
				L.setLevel(Logger.parseLevel(cmd.getOptionValue("d").toUpperCase()));
			}
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
    	
    	uc.webfs = new WebFS(client);
    	uc.webfs.setBaseURI(uri);
    	uc.webfs.downloadConfig();
    	
    	String line = null;
		while ((line = console.readLine("> ")) != null) {
			line = line.trim();
			if (line.length() == 0)
				continue;

			// If first character is a '!', lookup history based on index that
			// follows '!'
			if (line.charAt(0) == '!') {
				String historyId = line.substring(1);
				line = uc.getPastCommand(historyId);
				if (line == null) {
					console.printf("List '%s' is not in the history list\n",
							historyId);
					continue;
				}
			}
			String[] tokens = line.split(" ");
			String smallCmd = tokens[0].toLowerCase();
			if (!smallCmd.equals("history"))
				uc.history.add(line);
			if (smallCmd.equals("exit"))
				break;
			try {
				uc.runCommand(tokens);
			} catch (Exception e) {
				String msg = "Error running command: " + e.getMessage();
				System.out.println(msg);
				L.debug(msg, e);
			}
		}// end command while loop
    	
    	if(client != null)
    		client.close();
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
			case DEBUG:
			{
				msg += "Set debug level. Options are ";
				LogHandler.Level[] levels = LogHandler.Level.values();
				int idx = 0;
				for(;idx<levels.length - 1;idx++)
					msg += levels[idx].name().toLowerCase() + ", ";
				msg += levels[idx].name().toLowerCase();
				break;
			}	
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
	
	public void runCommand(String[] tokens) throws MalformedURLException, IOException, WebFileException {
		if(tokens == null || tokens.length == 0)
			return;
		
		String strCommand = tokens[0].toUpperCase();
		int numArgs = tokens.length - 1;
		
		Commands command = null;
		try {
			command = Commands.valueOf(strCommand);
		} catch(IllegalArgumentException iae) {
			System.out.printf("Unknown Command: %s\n", strCommand);
			return;
		}
		
		String path = "/";
		if(numArgs > 0)
			path = tokens[1];
		switch(command) {
		case DEBUG:
		{
			if(numArgs == 0)
			{
				System.out.printf("Missing argument for debug\n");
				break;
			}
			try {
				LogHandler.Level level = Logger.parseLevel(tokens[1].toUpperCase());
				L.setLevel(level);
			} catch(Exception e) {
				System.out.printf("Invalid debug level name: %s\n", tokens[1]);
				L.debug("Invalid debug level", e);
			}
			break;
		}
		case GET: case PUT:
		{
			if(numArgs == 0)
			{
				System.out.println("Missing file");
				break;
			}
			if(command == Commands.GET)
				webfs.downloadFile(path, null);
			else if(command == Commands.PUT)
				webfs.putFile(new File(path));
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
				System.out.printf("%d: %s\n", counter, history.get(counter));
			break;
		}
		case LS:
		{
			if(numArgs == 0)
				path = "/";
			WebFile webfile = webfs.ls(path);
			if(webfile == null)
					break;
			System.out.println(webfile.dirListing());
			if(webfile.isDirectory()) {
				WebFile[] dirents = webfile.listFiles();
				if(dirents == null)
					break;
				int size = dirents.length;
				for(int idx = 0;idx<size;idx++)
					System.out.println(dirents[idx].dirListing());
			}
			break;
		}
		case MD5:
		{
			if(numArgs == 0)
				break;
			String md5hash = webfs.md5(path);
			if(md5hash == null) {
				System.out.println("Error get md5 hash for " + path);
			} else {
				System.out.printf("%s\t%s\n", md5hash, path);
			}
			break;
		}
		case MERGE: case RM:
		{
			WebResponse status = null;
			if(command == Commands.MERGE)
				status = webfs.merge(path);
			else
				status = webfs.remove(path);
			if(status == null)
				break;
			System.out.println(status.message);
			break;
		}
		case MV:
		{
			if(numArgs < 2)
			{
				System.out.println("mv requires a source and destination path");
				break;
			}
			WebResponse status = webfs.move(tokens[1], tokens[2]);
			if(status.status != 0)
				System.out.printf("Error moving file: %s\n", status.message);
			break;
		}
		case MKDIR:
		{
			if(numArgs == 0)
			{
				System.out.println("mkdir requires a directory name");
				break;
			}
			webfs.mkdir(tokens[1]);
			break;
		}
		case SERVERINFO:
		{
			JSONObject serverInfo = webfs.getServerInfo();
			if(serverInfo  == null)
				break;
			@SuppressWarnings("unchecked")
			Iterator<Object> it = serverInfo.keySet().iterator();
			String key = null;
			while(it.hasNext()) {
				key = (String)it.next();
				System.out.printf("%s: %s\n", key, (String)serverInfo.get(key));
			}
			break;
		}
		case EXIT:
			break;
		}
		
	}

	public static void printDir(JSONArray dirents) throws WebFileException {
		@SuppressWarnings("rawtypes")
		Iterator it = dirents.iterator();
		JSONObject dirent = null;
		while(it.hasNext()) {
			dirent = (JSONObject)it.next();
			WebFile file = WebFile.fromJSON(dirent);
			System.out.println(file.dirListing());
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
