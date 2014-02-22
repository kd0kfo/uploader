package com.davecoss.uploader;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.davecoss.java.Logger;
import com.davecoss.java.LogHandler;

public class WebFSTask implements Callable<WebResponse> {
	
	static Logger L = Logger.getInstance();
	
	public enum Commands {CLEAN, RM, GET, LS, MD5, MERGE, MKDIR, MV, PUT, PIPE, SERVERINFO};
	
	private final WebFS webfs;
	private final Commands task;
	private ArrayList<File> files = new ArrayList<File>();
	private ArrayList<String> paths = new ArrayList<String>();
	private HashMap<String, Object> args = new HashMap<String, Object>();
	
	public WebFSTask(WebFS webfs, Commands task) {
		this.webfs = webfs;
		this.task = task;
	}
	
	public ArrayList<String> getPaths() {
		return paths;
	}

	public void setPath(ArrayList<String> paths) {
		this.paths = paths;
	}
	
	public boolean addPath(String path) {
		return paths.add(path);
	}

	public ArrayList<File> getFiles() {
		return files;
	}

	public void setFiles(ArrayList<File> files) {
		this.files = files;
	}
	
	public boolean addFile(File file) {
		return files.add(file);
	}
	
	public HashMap<String, Object> getArgs() {
		return args;
	}

	public void setArgs(HashMap<String, Object> args) {
		this.args = args;
	}
	
	public void addArgument(String key, Object value) {
		args.put(key, value);
	}

	public FutureTask<WebResponse> createFutureTask() {
		return new FutureTask<WebResponse>(this);
	}
	
	public static WebResponse blockingRun(WebFSTask webfsTask) throws InterruptedException, ExecutionException {
		FutureTask<WebResponse> future = webfsTask.createFutureTask();
		Thread t = new Thread(future);
		t.start();
		return future.get();
	}
	
	@Override
	public WebResponse call() throws Exception {
		if(L.getLevel() == LogHandler.Level.INFO)
			L.info("Performing Web FS Task: " + task.name());
		switch(task) {
		case CLEAN:
		{
			if(paths.size() == 0)
				return new WebResponse(1, "Missing file");
			return webfs.clean(paths.get(0));
		}
		case GET:
		{
			if(paths.size() == 0)
				return new WebResponse(1, "Missing file");
			File dest = null;
			if(files.size() > 0)
				dest = files.get(0);
			return webfs.downloadFile(paths.get(0), dest);
		}
		case PUT:
		{
			if(files.size() == 0)
				return new WebResponse(1, "Missing file");
			//return webfs.putFile(files.get(0));
			File file = files.get(0);
			boolean useBase64 = getBooleanArgument("base64", false);
			return webfs.postStream(new FileInputStream(file), file.getName(), useBase64);
		}
		case LS:
		{
			String path;
			if(paths.size() == 0)
				path = "/";
			else
				path = paths.get(0);
			return webfs.ls(path);
		}
		case MD5:
		{
			String path;
			if(paths.size() == 0)
				return new WebResponse(0, "Missing path for MD5 hash.");
			path = paths.get(0);
			return webfs.md5(path);
		}
		case MERGE: case RM:
		{
			String path;
			if(paths.size() == 0)
				return new WebResponse(0, "Missing path.");
			path = paths.get(0);
			if(task == Commands.MERGE)
				return webfs.merge(path);
			else
				return webfs.remove(path);
		}
		case MV:
		{
			if(paths.size() < 2)
				return new WebResponse(1, "mv requires a source and destination path");
			return webfs.move(paths.get(0), paths.get(1));
		}
		case MKDIR:
		{
			if(paths.size() == 0)
				return new WebResponse(1, "mkdir requires a directory name");
			return webfs.mkdir(paths.get(0));
		}
		case SERVERINFO:
		{
			return new WebResponse(0, WebFS.parseServerInfo(webfs.getServerInfo()));
		}
		}
		return new WebResponse(1, "Invalid task state");
	}
	
	private boolean getBooleanArgument(String key, boolean defaultValue) {
		Object val = args.get(key);
		if(val != null && Boolean.class.isInstance(val))
			return ((Boolean)val).booleanValue();
		return defaultValue;
	}

	/**
	 * Give a string help message for each task. Returns null for an invalid task name.
	 * 
	 * @param task
	 * @return String Help Message or null if the task is not a valid command.
	 */
	public static String getCommandHelp(String taskName) {
		Commands command = null;
		
		try {
			command = Commands.valueOf(taskName.toUpperCase());
		} catch(IllegalArgumentException iae) {
			return null;
		}
		switch(command) {
		case CLEAN:
			return "Remove segment files.";
		case RM:
			return "Delete specified file";
		case GET:
			return "Download file to current working directory";
		case LS:
			return "List file(s) for the provided path";
		case MD5:
			return "Print the MD5 hash for the specified file";
		case MERGE:
			return "Merge all files with the specified prefix";
		case MKDIR:
			return "Make a directory";
		case MV:
			return "Move file.";
		case PUT:
			return "Put a file on the server.";
		case SERVERINFO:
			return "Prints information about the server";
		}

		return null;
	}
	
	public static String[] getCommandNames() {
		Commands[] cmds = Commands.values();
		String[] retval = new String[cmds.length];
		int idx = 0;
		for(Commands cmd : cmds) {
			retval[idx++] = cmd.name(); 
		}
		return retval;
	}
	
	public static boolean isTask(String taskName) {
		try {
			Commands.valueOf(taskName.toUpperCase());
			return true;
		} catch(IllegalArgumentException iae) {
			return false;
		}
	}

}