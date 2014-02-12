package com.davecoss.uploader.android;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import com.davecoss.android.lib.AndroidLog;
import com.davecoss.android.lib.Notifier;
import com.davecoss.java.LogHandler;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.WebFile;

import android.os.Bundle;
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

	public static final int REQUEST_LOGON = 1;
	
	private Logger L = AndroidLog.getInstance("Uploader");
	private Notifier notifier = null;
	
	private WebFS webfs = null;
	private HashMap<String, String> dirTree = new HashMap<String, String>(); // Maps filename to full path
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_uploader);
		L.setLevel(LogHandler.Level.DEBUG);
		notifier = new Notifier(getApplicationContext());
		
		ListView filelist = getListView();
        filelist.setTextFilterEnabled(true);
        
        filelist.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(@SuppressWarnings("rawtypes") AdapterView parent, View view,
                                int position, long id) {
                        TextView tv = (TextView)view;
                        String filename = tv.getText().toString();
                        String itemInfo = filename + " => " + dirTree.get(filename);
                        L.debug("Tapped item:");
                        L.debug(itemInfo);
                        Uploader.this.notifier.toast_message(itemInfo);
                };
        });
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
			Intent intent = new Intent(this, Logon.class);
			startActivityForResult(intent, REQUEST_LOGON);
			break;
		}
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	  if (resultCode == RESULT_OK) {
		if(requestCode == REQUEST_LOGON) {
			String url = data.getStringExtra(Logon.URL);
			char[] passphrase = data.getCharArrayExtra(Logon.PASSPHRASE);
			String username = data.getStringExtra(Logon.USERNAME);
			
			if(url == null || passphrase == null || username == null) {
				String msg = "Could not log on";
				notifier.toast_message(msg);
				L.debug(msg);
				return;
			}
			
			try {
				logonWebfs(url, username, passphrase);
				updateDirList("/");
			} finally {
				if(passphrase != null)
					{
						for(int i = 0;i<passphrase.length;i++)
							passphrase[i] = 0;
					}
			}
		}
	  }
	} 
	
	private void logonWebfs(String url, String username, char[] passphrase) {
		HTTPSClient client = new HTTPSClient();
		CredentialPair creds = new CredentialPair(username, passphrase);
		try {
			URI uri = new URI(url);
			client.startClient(HTTPSClient.createCredentialsProvider(creds, uri), uri);
			webfs = new WebFS(client);
			webfs.setBaseURI(uri);
		} catch(Exception e) {
			L.error("Error starting client.", e);
			if(client != null)
				try {
					client.close();
				} catch (IOException ioe) {
					L.error("Error Closing HTTPSClient", ioe);
				}
			webfs = null;
			return;
		}
	}

	public void updateDirList(String path) {
		String[] list = null;
		dirTree.clear();
		if(webfs != null)
		{
			try {
				WebFile dirents = webfs.ls(path);
				L.debug("Directory entries:");
				
				int listIdx = 0;
				
				if(dirents.name.length() > 0) {
					dirTree.put("..", dirents.parent);
					list = new String[dirents.listFiles().length + 1];
					list[listIdx++] = "..";
				} else {
					list = new String[dirents.listFiles().length];
				}
				
				for(WebFile dirent : dirents.listFiles()) {
					dirTree.put(dirent.name, dirent.getAbsolutePath());
					list[listIdx++] = dirent.name;
				}
				this.setTitle(path);
			} catch(Exception e) {
				L.error("Error getting directory contents.", e);
			}
		}
		
		setListAdapter(new ArrayAdapter<String>(this, R.layout.activity_file_chooser, list));
	}
}
