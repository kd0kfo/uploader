package com.davecoss.uploader.utils;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
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
import com.davecoss.uploader.ACL;
import com.davecoss.uploader.FileMetaData;
import com.davecoss.uploader.FileRevision;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.Permission;
import com.davecoss.uploader.UploadOutputStream;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.WebFSTask;
import com.davecoss.uploader.WebFile;
import com.davecoss.uploader.WebFileException;
import com.davecoss.uploader.WebResponse;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;

public class UploaderConsole {

	public enum ConsoleCommands {BUFFERSIZE, DEBUG, EXIT, HELP, HISTORY};

	private static Logger L = ConsoleLog.getInstance("UploaderConsole");
	
	public static final CLIOptionTuple[] optionTuples = {new CLIOptionTuple("base64", false, "Console writing should use base64 encoding (Default: off)"),
		new CLIOptionTuple("basic", false, "Use basic authentication. (Default: off)"),
		new CLIOptionTuple("d", true, "Set Debug Level (Default:  ERROR)"),
		new CLIOptionTuple("filename", true, "Write to the console and upload the file name provided as an argument to the -console flag."),
		new CLIOptionTuple("help", false, "Print help and usage information"),
		new CLIOptionTuple("ssl", true, "Specify Keystore")};
	
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
		AuthHash.init(new CommonsBase64());
		
		if(cliArgs.length == 0) {
			System.out.println("For help and usage information, use the -help flag.");
			System.exit(1);
		}
		
		CommandLine cmd = null;
		
    	try {
    		cmd = UploadWriter.parseCommandLine(cliArgs, optionTuples);
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
    	Properties properties = loadProperties();
	
		printVersionInfo(uri);
		System.out.println("Run \"help\" to get a list of commands.");

    	// Parse args
		String keystoreFilename = null;
		CredentialsProvider credsProvider = null;
		ArrayList<File> filesToUpload = new ArrayList<File>();
		if(cmd.hasOption("basic")) {
			CredentialPair creds = null;
			try {
				String username = console.readLine("Basic Auth Username: ");
				char[] passphrase = console.readPassword("Basic Auth Passphrase: ");
				creds = new CredentialPair(username, passphrase);
				credsProvider = ConsoleHTTPSClient.createCredentialsProvider(creds, uri);
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

		L.debug("Creating client");
    	HTTPSClient client = null;
    	
    	if(keystoreFilename != null) {
    		System.out.print("Keystore Passphrase? ");
	    	char[] passphrase = console.readPassword();
	    	
	    	client = new ConsoleHTTPSClient(keystoreFilename, passphrase);
	    	for(int i = 0;i<passphrase.length;i++)
	    		passphrase[i] = 0;
    	} else if(properties.containsKey("keystore") && properties.containsKey("keystorepassphrase")) {
    		client = new ConsoleHTTPSClient(properties.getProperty("keystore"),
    				properties.getProperty("keystorepassphrase").toCharArray());
        } else {
    		client = new ConsoleHTTPSClient();
    	}
    	
    	if(credsProvider != null)
    		client.startClient(credsProvider, uri);
    	
    	// Setup WebFS
    	uc.webfs = new WebFS(client);
    	uc.webfs.setBaseURI(uri);
    	L.debug("Downloading config");
    	uc.webfs.downloadConfig();
    	if(properties.containsKey("buffersize"))
    		uc.webfs.setUploadBufferSize(Integer.parseInt(properties.getProperty("buffersize")));
    	
    	String username = console.readLine("WebFS Username: ");
    	char[] passphrase = console.readPassword("WebFS Passphrase: ");
    	String totpString = console.readLine("WebFS One Time Passcode: ");
    	int totpToken = Integer.parseInt(totpString);
    	String serverSalt = (String)uc.webfs.getServerInfo().get("salt");
    	uc.webfs.setCredentials(new Credentials(username, passphrase, serverSalt));
    	WebResponse logonResponse = uc.webfs.logon(totpToken);
    	if(logonResponse.status != WebResponse.SUCCESS)
    	{
    		System.err.println("Error logon on");
    		System.err.println(logonResponse.message);
    		System.exit(logonResponse.status);
    	}
    	
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
			String[] webcommands = WebFSTask.getCommandNames();
			ConsoleCommands[] consoleCommands = ConsoleCommands.values();
			queries = new String[webcommands.length + consoleCommands.length];
			int idx = 0;
			for(String cmd : webcommands)
				queries[idx++] = cmd.toLowerCase();
			for(ConsoleCommands cmd : consoleCommands)
				queries[idx++] = cmd.name().toLowerCase();
		}
		
		if(output == null)
			output = System.out;

		ConsoleCommands cmd = ConsoleCommands.HELP;
		String msg = "";
		for(String query : queries) {
			msg = query.toLowerCase() + " -- ";
			
			if(WebFSTask.isTask(query)) {
				msg += WebFSTask.getCommandHelp(query);
				output.println(msg);
				continue;
			}
			
			query = query.toUpperCase();
			try {
				cmd = Enum.valueOf(ConsoleCommands.class, query);
			} catch(IllegalArgumentException iae) {
				output.println("Known command " + query);
				continue;
			}
			switch(cmd) {
			case BUFFERSIZE:
			{
				msg += String.format("Set size of buffer used to store bytes until posted to web. (Default: %d)", 
						UploadOutputStream.DEFAULT_BUFFER_SIZE);
				break;	
			}
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
			case EXIT:
				msg += "Leave the console.";
				break;
			case HELP:
				msg += "Get help for a given command";
				break;
			case HISTORY:
				msg += "Print list of commands run";
				break;
			}
			output.println(msg);
		}
	}
	
	public void doConsoleCommand(String[] tokens) throws IllegalArgumentException {
		ConsoleCommands command = ConsoleCommands.valueOf(tokens[0].toUpperCase());
		int numArgs = tokens.length - 1;
		switch(command) {
		case BUFFERSIZE:
		{
			if(numArgs != 0) {
				try {
					webfs.setUploadBufferSize(Integer.parseInt(tokens[1]));
				} catch(NumberFormatException nfe) {
					System.err.println("Buffer size not changed. Invalid size: " + tokens[1]);
					break;
				}
			}
			System.out.print("Buffer Size: ");
			System.out.print(webfs.getUploadBufferSize());
			System.out.println();
			break;
		}
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
		case EXIT:
			break;
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
		}
	}
	
	public void runCommand(String[] tokens) throws MalformedURLException, IOException, WebFileException, InterruptedException, ExecutionException {
		if(tokens == null || tokens.length == 0)
			return;
		
		String strCommand = tokens[0].toUpperCase();
		int numArgs = tokens.length - 1;
		
		WebFSTask.Commands command = null;
		try {
			command = Enum.valueOf(WebFSTask.Commands.class, strCommand);
		} catch(IllegalArgumentException iae) {
			try {
				doConsoleCommand(tokens);
			} catch(IllegalArgumentException iae2) {
				System.out.println("Invalid command: " + tokens[0]);
			}
			return;
		}
		
		// Process and run command.
		String path = "/";
		if(numArgs > 0)
			path = tokens[1];
		WebFSTask webfsTask = new WebFSTask(webfs, command);
		WebResponse response = null;
		WebFile webfile = null;
		switch(command) {
		case BASE64:
		{
			if(numArgs < 1)
			{
				System.out.println("Missing file");
				break;
			}
			webfsTask.addPath(path);
			if(numArgs > 1) {
				webfsTask.addArgument("encode", (tokens[2].equals("decode")) ? false : true);
			}
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case CHMOD:
		{
			if(numArgs != 3)
			{
				System.out.println("chmod requires file path, username and permission.");
				break;
			}
			webfsTask.addPath(path);
			webfsTask.addArgument("user", tokens[2]);
			webfsTask.addArgument("permission", new Integer(tokens[3]));
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case CLEAN:
		{
			if(numArgs == 0)
			{
				System.out.println("Missing file");
				break;
			}
			webfsTask.addPath(path);
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case GET: case PUT:
		{
			if(numArgs == 0)
			{
				System.out.println("Missing file");
				break;
			}
			if(command == WebFSTask.Commands.PUT)
				webfsTask.addFile(new File(path));
			else
				webfsTask.addPath(path);
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case POSTSTREAM:
		{
			if(numArgs == 0)
			{
				System.out.println("Missing file");
				break;
			}
			webfsTask.addArgument("base64", true);
			webfsTask.addFile(new File(path));
			response = WebFSTask.blockingRun(webfsTask);
		}
		case LS:
		{
			if(numArgs == 0)
				path = "/";
			webfsTask.addPath(path);
			response = WebFSTask.blockingRun(webfsTask);
			webfile = response.webfile;
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
			response = null;
			break;
		}
		case MD5:
		{
			if(numArgs == 0)
				break;
			webfsTask.addPath(path);
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case MERGE: case RM:
		{
			webfsTask.addPath(path);
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case MV:
		{
			if(numArgs < 2)
			{
				System.out.println("mv requires a source and destination path");
				break;
			}
			webfsTask.addPath(tokens[1]);
			webfsTask.addPath(tokens[2]);
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case MKDIR:
		{
			if(numArgs == 0)
			{
				System.out.println("mkdir requires a directory name");
				break;
			}
			webfsTask.addPath(tokens[1]);
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case SERVERINFO:
		{
			response = WebFSTask.blockingRun(webfsTask);
			break;
		}
		case STAT:
		{
			if(numArgs == 0)
				path = "/";
			webfsTask.addPath(path);
			response = WebFSTask.blockingRun(webfsTask);
			if(response.metadata == null)
				break;
			FileMetaData metadata = response.metadata;
			System.out.println("About: " + path);
			System.out.println("Size: " + metadata.size);
			System.out.println("Permissions:");
			Iterator<String> users = metadata.acl.getUsers().iterator();
			while(users.hasNext()) {
				String user = users.next();
				Permission perm = metadata.acl.getPermission(user);
				System.out.println(user + "(" + perm.toString() + ")");
			}
			Iterator<Integer> revids = metadata.revisionList.keySet().iterator();
			while(revids.hasNext()) {
				Integer revid = revids.next();
				FileRevision rev = metadata.revisionList.get(revid);
				System.out.println("Revision " + revid.toString() + ": " + rev.toString());
			}
			response = null;
			break;
		}
		}
		
		if(response != null) {
			System.out.println(response.message);
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
		BuildInfo bi = new BuildInfo(UploaderConsole.class);
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
	
	/**
	 * Attempts to load .webfsrc from the home directory. If that file does not exist, or is not a valid properties file,
	 * an empty Properties object is returned.
	 * 
	 * @return Map<String, String>
	 */
	private static Properties loadProperties() {
		try {
			String homedir = System.getProperty("user.home");
			if(homedir == null) {
				L.debug("user.home not defined.");
				return new Properties();
			}
			File propfile = new File(homedir, ".webfsrc");
			if(!propfile.exists())
				return new Properties();
			
			InputStream input = null;
			Properties props = null;
			try {
				input = new FileInputStream(propfile);
				props = new Properties();
				props.load(input);
			} finally {
				if(input != null)
					input.close();
			}
			return props;
		} catch(Exception e) {
			L.error("Error loading properties.", e);
			return new Properties();
		}
	}
}
