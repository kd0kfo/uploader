package com.davecoss.uploader.android;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.FutureTask;
import com.davecoss.android.lib.AndroidLog;
import com.davecoss.android.lib.ExternalFile;
import com.davecoss.android.lib.ExternalFileUnavailable;
import com.davecoss.android.lib.FileChooser;
import com.davecoss.android.lib.Notifier;
import com.davecoss.android.lib.utils;
import com.davecoss.java.LogHandler;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.WebFSTask;
import com.davecoss.uploader.WebFile;
import com.davecoss.uploader.WebResponse;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.ListActivity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Uploader extends ListActivity {
	
	private final UploaderHandler handler = new UploaderHandler(this);
	// Handler Tasks
	public static final int MSG_UPDATE_LIST = 0;
	public static final int MSG_TOAST_MESSAGE = 1;
	public static final String MSG_CONTENTS = "message";
	
	public static final int REQUEST_LOGON = 0;
	public static final int REQUEST_FILE_DESCRIPTION = 1;
	public static final int REQUEST_UPLOAD = 2;
	
	private Logger L = AndroidLog.getInstance("Uploader");
	private Notifier notifier = null;
	private ExternalFile externalDir = null;
	
	private WebFS webfs = null;
	private HashMap<String, WebFile> dirTree = new HashMap<String, WebFile>(); // Maps filename to full path
	private String cwd = "/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_uploader);
		L.setLevel(LogHandler.Level.INFO);
		notifier = new Notifier(getApplicationContext());
		setupExternalDir();
		
		ListView filelist = getListView();
        filelist.setTextFilterEnabled(true);
        
        filelist.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(@SuppressWarnings("rawtypes") AdapterView parent, View view,
                                int position, long id) {
                        TextView tv = (TextView)view;
                        String filename = tv.getText().toString();
                        
                        WebFile file = Uploader.this.dirTree.get(filename);
                        if(file.isDirectory()) {
                        	cwd = file.getAbsolutePath();
                        	updateDirList();
                        }
                        else {
                        	Intent intent = new Intent(Uploader.this, FileDescription.class);
                        	intent.putExtra("filename", file.name);
                        	intent.putExtra("size", file.size);
                        	intent.putExtra("path", file.getAbsolutePath());
                        	startActivityForResult(intent, REQUEST_FILE_DESCRIPTION);
                        }
                };
        });
        
        showLogonActivity();
       
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.uploader, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_logon:
		{
			showLogonActivity();
			break;
		}
		case R.id.menu_upload:
			showUploadActivity();
			break;
		}
		return true;
	}
	
	private void showUploadActivity() {
		Intent intent = new Intent(this, FileChooser.class);
		intent.putExtra(FileChooser.START_DIRECTORY_KEY, "/mnt/sdcard");
		startActivityForResult(intent, REQUEST_UPLOAD);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	  if (resultCode == RESULT_OK) {
		if(requestCode == REQUEST_LOGON) {
			processLogonIntent(data);
		}
		else if(requestCode == REQUEST_FILE_DESCRIPTION) {
			String task = data.getStringExtra(FileDescription.TASK);
			if(task != null) {
				processFileDescriptionIntent(data, task);
			}
		} else if(requestCode == REQUEST_UPLOAD && data.hasExtra(FileChooser.RETURN_VALUE_KEY)) {
			try {
				processUploadIntent(data);
			} catch(IOException ioe) {
				String msg = "Error uploading file: " + ioe.getMessage();
				notifier.toast_message(msg);
				L.error(msg, ioe);
			}
		}
	  }
	} 
	
	private void processUploadIntent(Intent data) throws IOException {
		if(webfs == null) {
			String msg = "Not connected to Web Filesystem. Cannot upload file.";
			notifier.toast_message(msg);
			L.error(msg);
			return;
		}
		String path = data.getStringExtra(FileChooser.RETURN_VALUE_KEY);
		if(path != null && path.length() != 0) {
			ExternalFile file = new ExternalFile(path);
			if(!file.exists()) {
				String msg = "File does not exist: " + path;
				notifier.toast_message(msg);
				L.error(msg);
				return;
			} else if(!file.canRead()) {
				String msg = "Cannot read file: " + path;
				notifier.toast_message(msg);
				L.error(msg);
				return;
			}
			WebFSTask webfsTask = new WebFSTask(webfs, WebFSTask.Commands.PUT);
			webfsTask.addFile(file);
			Thread t = new Thread(new WebFSThread(webfsTask.createFutureTask()));
			t.start();
			notifier.toast_message("Uploading " + path);
		}
	}

	private void doDownload(Intent data) {
		String downloadFile = data.getStringExtra(FileDescription.DOWNLOAD_KEY);
		WebFile file = dirTree.get(downloadFile);
		if(file == null)
		{
			notifier.toast_message("Unknown file: " + downloadFile);
		}
		if(externalDir == null) {
			notifier.toast_message("External Directory not Available. Cannot download.");
		}
		String path = file.getAbsolutePath();
		ExternalFile dest = new ExternalFile(externalDir, file.name);
		WebFSTask webfsTask = new WebFSTask(webfs, WebFSTask.Commands.GET);
		webfsTask.addPath(path);
		webfsTask.addFile(dest);
		notifier.toast_message("Downloading " + file.name);
		Thread t = new Thread(new WebFSThread(webfsTask.createFutureTask()));
		t.start();
	}
	
	private void doDelete(Intent data) {
		String downloadFile = data.getStringExtra(FileDescription.DELETE_KEY);
		WebFile file = dirTree.get(downloadFile);
		
		WebFSTask webfsTask = new WebFSTask(webfs, WebFSTask.Commands.RM);
		webfsTask.addPath(file.getAbsolutePath());
		Thread t = new Thread(new WebFSThread(webfsTask.createFutureTask()));
		t.start();
		notifier.toast_message("Deleting " + file.name);
	}
	
	private void processFileDescriptionIntent(Intent data, String task) {
		if(task.equals(FileDescription.DOWNLOAD_KEY))
			doDownload(data);
		else if(task.equals(FileDescription.DELETE_KEY))
			doDelete(data);
	}

	private void processLogonIntent(Intent data) {
		String url = data.getStringExtra(Logon.URL);
		char[] passphrase = data.getCharArrayExtra(Logon.PASSPHRASE);
		String username = data.getStringExtra(Logon.USERNAME);
		String keystore = data.getStringExtra(Logon.KEYSTORE);
		char[] keystorePassphrase = data.getCharArrayExtra(Logon.KEYSTORE_PASSPHRASE);
		
		if(url == null || passphrase == null || username == null) {
			String msg = "Could not log on";
			notifier.toast_message(msg);
			L.debug(msg);
			return;
		}
		
		try {
			notifier.toast_message("Logging on to " + url);
			logonWebfs(url, username, passphrase, keystore, keystorePassphrase);
			notifier.toast_message("Logged on. Getting file list.");
			cwd = "/";
			updateDirList();
		} catch(Exception e) {
			String msg = String.format("Error logging onto %s: %s", url, e.getMessage());
			notifier.toast_message(msg);
			L.error(msg, e);
		} finally {
			if(passphrase != null)
				{
					for(int i = 0;i<passphrase.length;i++)
						passphrase[i] = 0;
				}
			if(keystorePassphrase != null)
			{
				for(int i = 0;i<keystorePassphrase.length;i++)
					keystorePassphrase[i] = 0;
			}
		}
	}

	private void logonWebfs(String url, String username, char[] passphrase, String keystore, char[] keystorePassphrase) throws Exception {
		HTTPSClient client = null;
		CredentialPair creds = new CredentialPair(username, passphrase);
		try {
			if(keystore == null || keystore.length() == 0)
				client = new HTTPSClient();
			else
				client = new HTTPSClient(keystore, keystorePassphrase, "BKS");
			URI uri = new URI(url);
			client.startClient(HTTPSClient.createCredentialsProvider(creds, uri), uri);
			webfs = new WebFS(client);
			webfs.setBaseURI(uri);
			webfs.downloadConfig();
		} catch(Exception e) {
			if(client != null)
				try {
					client.close();
				} catch (IOException ioe) {
					L.error("Error Closing HTTPSClient", ioe);
				}
			webfs = null;
			throw e;
		}
	}
	
	private void logonWebfs(String url, String username, char[] passphrase) throws Exception {
		logonWebfs(url, username, passphrase, null, null);
	}


	public void updateDirList() {
		if(cwd == null)
			return;
		updateDirList(cwd);
	}
	
	public void updateDirList(String path) {
		if(webfs != null)
		{
			try {
				WebFSTask webfsTask = new WebFSTask(webfs, WebFSTask.Commands.LS);
				webfsTask.addPath(path);
				Thread t = new Thread(new WebFSThread(webfsTask.createFutureTask()));
				t.start();
				this.setTitle(path);
			} catch(Exception e) {
				L.error("Error getting directory contents.", e);
			}
		}
	}
		
	private void populateDir(WebResponse response) {
		dirTree.clear();
		try {
			WebFile dirents = response.webfile;
			
			if(dirents.name.length() > 0) {
				dirTree.put("..", new WebFile(dirents.parent,"/","d",-1));
			}
			
			for(WebFile dirent : dirents.listFiles()) {
				dirTree.put(dirent.name, dirent);
			}
		} catch(Exception e) {
			L.error("Error getting directory contents.", e);
		}
	}
	
	public void refreshListAdapter() {
		String[] list = new String[dirTree.size()];
		Iterator<String> it = dirTree.keySet().iterator();
		int idx = 0;
		while(it.hasNext())
			list[idx++] = it.next();
		setListAdapter(new ArrayAdapter<String>(this, R.layout.activity_file_chooser, list));
	}
	
	public void showLogonActivity() {
		Intent intent = new Intent(this, Logon.class);
		startActivityForResult(intent, REQUEST_LOGON);
	}
	
	public void setupExternalDir() {
		try {
			externalDir = utils.get_external_dir(this);
			if(externalDir.mounted_read_only()) {
				L.error("External directory mounted read only. File Saving Not Available.");
				externalDir = null;
				return;
			}
			if(!externalDir.exists() && !externalDir.mkdirs()) {
				L.error("Unable to create external directory. File Saving Not Available.");
				return;
			}
		} catch (ExternalFileUnavailable e1) {
			L.error("Unable to get external directory. File Saving Not Available.", e1);
			externalDir = null;
		}
	}
	
	public class WebFSThread implements Runnable {

		private final FutureTask<WebResponse> task;
		public WebFSThread(FutureTask<WebResponse> task) {
			this.task = task;
		}
		
		@Override
		public void run() {
			WebResponse response;
			try {
				Thread t = new Thread(task);
				t.start();
				response = task.get();
			} catch (Exception e) {
				L.error("Error running Web FS thread", e);
				return;
			}
			
			if(response.webfile != null) {
				if(response.webfile.isDirectory()) {
					Uploader.this.populateDir(response);
					if(!handler.sendEmptyMessage(MSG_UPDATE_LIST))
						L.error("Sending message to Uploader Handler Failed.");
				}
			} else {
				Message message = handler.obtainMessage(MSG_TOAST_MESSAGE);
				Bundle data = new Bundle();
				data.putString(MSG_CONTENTS, response.message);
				message.setData(data);
				handler.sendMessage(message);
			}
		}
	}
		
	static class UploaderHandler extends Handler {
		private final WeakReference<Uploader> uploaderRef; 

		UploaderHandler(Uploader uploader) {
	        uploaderRef = new WeakReference<Uploader>(uploader);
	    }
		
		@Override
		public void handleMessage(Message data) {
			Uploader self = uploaderRef.get();
			int todo = data.what;
			switch(todo) {
			case Uploader.MSG_UPDATE_LIST:
				self.refreshListAdapter();
				break;
			case Uploader.MSG_TOAST_MESSAGE:
				String msg = data.getData().getString(Uploader.MSG_CONTENTS);
				if(msg != null)
					self.notifier.toast_message(msg);
			}
		}
	}
}
