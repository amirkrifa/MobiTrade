package com.MobiTrade;

import com.MobiTrade.sqlite.DatabaseHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;

public class ConfigTab extends Activity {

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		loadConfigFromDB();
	}

	// Default values
	// in bytes
	public static int megabyte = 1048576;
	public static int ko = 10024;
	
	private static int defaultMaxAllowedSpace = 1000;
	private static int defaultUseVibratorNotification = 1; 
	private static int defaultAlwaysDiscovering = 1; 

	private int maxAllowedSpace;
	private int useVibratorNotification;
	private int alwaysDiscovering;
	
	private static final int CONFIG_UPDATED = 0;
	private static final int DEFAULT_CONFIG_LOADED = 1;
	private CheckBox vb = null;
	private CheckBox alwaysDiscoveringCheck = null;
	private CheckBox onDemandDiscoveringCheck = null;
	public final static String DISCOVERY_MODE_CHANGED = "DiscoveryModeChanged"; 
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.config);

		
		ImageButton update = (ImageButton)findViewById(R.id.updateConfig);
		ImageButton restore = (ImageButton)findViewById(R.id.restoreConfig);
		Display display = getWindowManager().getDefaultDisplay();
		int buttonWidth = display.getWidth()/2;

		LayoutParams updateParams  = (LayoutParams) update.getLayoutParams();
		updateParams.width = buttonWidth;
		update.setLayoutParams(updateParams);

		LayoutParams restoreParams  = (LayoutParams) restore.getLayoutParams();
		restoreParams.width = buttonWidth;
		update.setLayoutParams(restoreParams);

		update.setOnClickListener(new UpdateConfigListener());
		restore.setOnClickListener(new RestoreDefaultConfigListener());

		vb = (CheckBox)findViewById(R.id.vibarateCheckBox);
		
		alwaysDiscoveringCheck = (CheckBox)findViewById(R.id.alwaysDiscoveringCheckBox);
		onDemandDiscoveringCheck = (CheckBox)findViewById(R.id.discoveringOnDemandCheckBox);
		
		vb.setOnCheckedChangeListener(new OnCheckedChangeListener() { 

			@Override 
			public void onCheckedChanged(CompoundButton buttonView, 
					boolean isChecked) { 
				// TODO Auto-generated method stub 
				if (buttonView.isChecked()) 
				{
					useVibratorNotification = 1;
				} 
				else 
				{ 
					useVibratorNotification = 0;
				} 

			} 
		}); 

		alwaysDiscoveringCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() { 

			@Override 
			public void onCheckedChanged(CompoundButton buttonView, 
					boolean isChecked) { 
				// TODO Auto-generated method stub 
				if (buttonView.isChecked()) 
				{
					alwaysDiscovering = 1;
					onDemandDiscoveringCheck.setChecked(false);
				} 
				else 
				{ 
					alwaysDiscovering = 0;
					onDemandDiscoveringCheck.setChecked(true);
				} 

			} 
		}); 
		
		onDemandDiscoveringCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() { 

			@Override 
			public void onCheckedChanged(CompoundButton buttonView, 
					boolean isChecked) { 
				// TODO Auto-generated method stub 
				if (buttonView.isChecked()) 
				{
					alwaysDiscovering = 0;
					alwaysDiscoveringCheck.setChecked(false);
				} 
				else 
				{ 
					alwaysDiscovering = 1;
					alwaysDiscoveringCheck.setChecked(true);
				} 

			} 
		}); 

		loadConfigFromDB();

	}

	private void loadConfigFromDB()
	{
		// Updating the max Allowed space 
		EditText maxAllowedSpaceEdit = (EditText)findViewById(R.id.maxAllowedSpace);
		maxAllowedSpace = DatabaseHelper.getDBManager().getConfigMaxAllowedSpace();	
		maxAllowedSpaceEdit.setText(Integer.toString(maxAllowedSpace));

		// Vibrator notification
		useVibratorNotification = DatabaseHelper.getDBManager().getConfigVibratorNotification();
		if(useVibratorNotification == 1)
		{
			vb.setChecked(true);
		}else
		{
			vb.setChecked(false);
		}

		// Discovery mode
		alwaysDiscovering = DatabaseHelper.getDBManager().getConfigAlwaysDiscovering();
		
		if(alwaysDiscovering == 1)
		{
			alwaysDiscoveringCheck.setChecked(true);
			onDemandDiscoveringCheck.setChecked(false);
		}else
		{
			alwaysDiscoveringCheck.setChecked(false);
			onDemandDiscoveringCheck.setChecked(true);
		}
	}

	private class UpdateConfigListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {

			// Read value from edit text and update the local database
			EditText maxAllowedSpaceEdit = (EditText)findViewById(R.id.maxAllowedSpace);
			maxAllowedSpace = Integer.parseInt(maxAllowedSpaceEdit.getText().toString());
			DatabaseHelper.getDBManager().updateConfigMaxAllowedSpace(maxAllowedSpace);	

			DatabaseHelper.getDBManager().updateConfigAlwaysDiscovering(alwaysDiscovering);
			DatabaseHelper.getDBManager().updateConfigVibratorNotification(useVibratorNotification);
			sendBroadcast(new Intent(DISCOVERY_MODE_CHANGED));
						
			showDialog(CONFIG_UPDATED);


		}	    
	}

	private class RestoreDefaultConfigListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {

			// Set the default values to the local database and update the view
			DatabaseHelper.getDBManager().updateConfigMaxAllowedSpace(defaultMaxAllowedSpace);	
			DatabaseHelper.getDBManager().updateConfigVibratorNotification(defaultUseVibratorNotification);	
			DatabaseHelper.getDBManager().updateConfigAlwaysDiscovering(defaultAlwaysDiscovering);	
			loadConfigFromDB();
			showDialog(DEFAULT_CONFIG_LOADED);
			
			sendBroadcast(new Intent(DISCOVERY_MODE_CHANGED));

		}	    
	}

	protected Dialog onCreateDialog(int id) {
		Dialog createdDialog = null;

		switch (id) {
		case CONFIG_UPDATED:

			AlertDialog.Builder configSavedBuilder = new AlertDialog.Builder(ConfigTab.this);
			configSavedBuilder.setMessage("Configuration successfully saved.")
			.setCancelable(false)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			});

			AlertDialog configSavedAlert = configSavedBuilder.create();
			configSavedAlert.setIcon(R.drawable.mobitradeicon);
			configSavedAlert.show();

			break;

		case DEFAULT_CONFIG_LOADED:

			AlertDialog.Builder defConfigBuilder = new AlertDialog.Builder(ConfigTab.this);
			defConfigBuilder.setMessage("Default configuration successfully loaded.")
			.setCancelable(false)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			});

			AlertDialog defConfigAlert = defConfigBuilder.create();
			defConfigAlert.setIcon(R.drawable.mobitradeicon);
			defConfigAlert.show();

			break;

		default:
			break;
		}
		return createdDialog;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			//preventing default implementation previous to android.os.Build.VERSION_CODES.ECLAIR
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
