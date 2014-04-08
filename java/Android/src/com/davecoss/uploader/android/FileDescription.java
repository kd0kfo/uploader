package com.davecoss.uploader.android;


import com.davecoss.android.lib.ConfirmationActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class FileDescription extends Activity {
	
	public static final int REQUEST_CONFIRM_DELETE = 0;
	private static final int REQUEST_CONFIRM_MERGE = 1;
	private static final int REQUEST_CONFIRM_CLEAN = 2;
	private static final int REQUEST_CONFIRM_ENCODE = 3;
	private static final int REQUEST_CONFIRM_DECODE = 4;
	
	public static final String TASK = "TASK";
	public static final String DOWNLOAD_KEY = "download";
	public static final String DELETE_KEY = "delete";
	public static final String MERGE_KEY = "merge";
	public static final String CLEAN_KEY = "clean";
	public static final String BASE64_KEY = "base64";
	public static final String BASE64_ENCODE = "encode";
	
	private String fileName = null;
	private String path = null;
	private long size = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_description);
		
		Intent intent = getIntent();
		fileName = intent.getStringExtra("filename");
		path = intent.getStringExtra("path");
		size = intent.getLongExtra("size", -1);
		
		TextView curr = null;
		if(fileName != null) {
			curr = (TextView)findViewById(R.id.txtFilename);
			curr.setText(fileName);
		}
		curr = (TextView)findViewById(R.id.txtFilesize);
		curr.setText(Long.toString(size) + " bytes");
		if(path != null) {
			curr = (TextView)findViewById(R.id.txtFullPath);
			curr.setText(path);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.file_description, menu);
		return true;
	}
	
	public void doDownload(View view) {
		Intent intent = new Intent();
		intent.putExtra(TASK, DOWNLOAD_KEY);
		intent.putExtra(DOWNLOAD_KEY, fileName);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	public void doDelete(View view) {
		Intent intent = new Intent(this, ConfirmationActivity.class);
		intent.putExtra(ConfirmationActivity.MESSAGE, String.format("Delete %s?", path));
		intent.putExtra(ConfirmationActivity.OK_BTN_TEXT, "Yes");
		intent.putExtra(ConfirmationActivity.CANCEL_BTN_TEXT, "No");
		startActivityForResult(intent, REQUEST_CONFIRM_DELETE);
	}
	
	public void doMerge(View view) {
		Intent intent = new Intent(this, ConfirmationActivity.class);
		intent.putExtra(ConfirmationActivity.MESSAGE, String.format("Merge %s?", path));
		intent.putExtra(ConfirmationActivity.OK_BTN_TEXT, "Yes");
		intent.putExtra(ConfirmationActivity.CANCEL_BTN_TEXT, "No");
		startActivityForResult(intent, REQUEST_CONFIRM_MERGE);
	}
	
	public void doClean(View view) {
		Intent intent = new Intent(this, ConfirmationActivity.class);
		intent.putExtra(ConfirmationActivity.MESSAGE, String.format("Clean Intermediate Files for %s?", path));
		intent.putExtra(ConfirmationActivity.OK_BTN_TEXT, "Yes");
		intent.putExtra(ConfirmationActivity.CANCEL_BTN_TEXT, "No");
		startActivityForResult(intent, REQUEST_CONFIRM_CLEAN);
	}
	
	public void doEncode(View view) {
		Intent intent = new Intent(this, ConfirmationActivity.class);
		intent.putExtra(ConfirmationActivity.MESSAGE, String.format("Base64 Encode %s?", path));
		intent.putExtra(ConfirmationActivity.OK_BTN_TEXT, "Yes");
		intent.putExtra(ConfirmationActivity.CANCEL_BTN_TEXT, "No");
		startActivityForResult(intent, REQUEST_CONFIRM_ENCODE);
	}
	
	public void doDecode(View view) {
		Intent intent = new Intent(this, ConfirmationActivity.class);
		intent.putExtra(ConfirmationActivity.MESSAGE, String.format("Base64 Decode %s?", path));
		intent.putExtra(ConfirmationActivity.OK_BTN_TEXT, "Yes");
		intent.putExtra(ConfirmationActivity.CANCEL_BTN_TEXT, "No");
		startActivityForResult(intent, REQUEST_CONFIRM_DECODE);
	}
		
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK) {
			switch(requestCode) { 
			case REQUEST_CONFIRM_DELETE:
			{
				if(data.getBooleanExtra(ConfirmationActivity.RESULT, false)){
					Intent intent = new Intent();
					intent.putExtra(TASK, DELETE_KEY);
					intent.putExtra(DELETE_KEY, fileName);
					setResult(RESULT_OK, intent);
					finish();
				}
				break;
			}
			case REQUEST_CONFIRM_MERGE:
			{
				if(data.getBooleanExtra(ConfirmationActivity.RESULT, false)){
					Intent intent = new Intent();
					intent.putExtra(TASK, MERGE_KEY);
					intent.putExtra(MERGE_KEY, fileName);
					setResult(RESULT_OK, intent);
					finish();
				}
				break;
			}
			case REQUEST_CONFIRM_CLEAN:
			{
				if(data.getBooleanExtra(ConfirmationActivity.RESULT, false)){
					Intent intent = new Intent();
					intent.putExtra(TASK, CLEAN_KEY);
					intent.putExtra(CLEAN_KEY, fileName);
					setResult(RESULT_OK, intent);
					finish();
				}
				break;
			}
			case REQUEST_CONFIRM_ENCODE: case REQUEST_CONFIRM_DECODE:
			{
				if(data.getBooleanExtra(ConfirmationActivity.RESULT, false)){
					Intent intent = new Intent();
					intent.putExtra(TASK, BASE64_KEY);
					intent.putExtra(BASE64_KEY, fileName);
					intent.putExtra(BASE64_ENCODE, (requestCode == REQUEST_CONFIRM_ENCODE));
					setResult(RESULT_OK, intent);
					finish();
				}
				break;
			}
		} // SWITCH
		} // RESULT_OK
	}// onActivityResult

}
