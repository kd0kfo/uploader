package com.davecoss.uploader.android;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

public class UploadDialog extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload_dialog);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.upload_dialog, menu);
		return true;
	}
	
	public void doUpload(View view) {
		
	}

}
