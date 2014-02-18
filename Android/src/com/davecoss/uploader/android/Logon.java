package com.davecoss.uploader.android;

import com.davecoss.android.lib.FileChooser;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class Logon extends Activity {
	
	public static final String USERNAME = "USERNAME";
	public static final String PASSPHRASE = "PASSPHRASE";
	public static final String URL = "URL";
	public static final String KEYSTORE = "KEYSTORE";
	public static final String KEYSTORE_PASSPHRASE = "KEYSTORE_PASSPHRASE";
	
	public static final int REQUEST_KEYSTORE = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logon);

		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.logon, menu);
		
		
		return true;
	}
	
	public void doLogon(View view) {
		TextView txtURL = (TextView)findViewById(R.id.txtURL);
		TextView txtUsername = (TextView)findViewById(R.id.txtUsername);
		TextView txtPassphrase = (TextView)findViewById(R.id.txtPassphrase);
		TextView txtKeystore = (TextView)findViewById(R.id.txtKeystore);
		TextView txtKeystorePassphrase = (TextView)findViewById(R.id.txtKeystorePassphrase);
		
		Intent result = new Intent();
		result.putExtra(USERNAME, txtUsername.getText().toString());
		result.putExtra(PASSPHRASE, txtPassphrase.getText().toString().toCharArray());
		result.putExtra(URL, txtURL.getText().toString());
		result.putExtra(KEYSTORE, txtKeystore.getText().toString());
		result.putExtra(KEYSTORE_PASSPHRASE, txtKeystorePassphrase.getText().toString().toCharArray());
		setResult(RESULT_OK, result);
		super.finish();
	}
	
	public void getKeystore(View view) {
		Intent intent = new Intent(this, FileChooser.class);
		intent.putExtra("directory", "/mnt/sdcard");
		startActivityForResult(intent, REQUEST_KEYSTORE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	  if (resultCode == RESULT_OK) {
		if(requestCode == REQUEST_KEYSTORE) {
			String path = data.getStringExtra("file");
			if(path == null)
				return;
			
			TextView txtKeystore = (TextView)findViewById(R.id.txtKeystore);
			txtKeystore.setText(path);
		}
	  }
	}

}
