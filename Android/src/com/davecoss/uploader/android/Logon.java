package com.davecoss.uploader.android;

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
		
		Intent result = new Intent();
		result.putExtra(USERNAME, txtUsername.getText().toString());
		result.putExtra(PASSPHRASE, txtPassphrase.getText().toString().toCharArray());
		result.putExtra(URL, txtURL.getText().toString());
		setResult(RESULT_OK, result);
		super.finish();
	}

}
