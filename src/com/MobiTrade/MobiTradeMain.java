package com.MobiTrade;

import java.util.Set;
import java.util.UUID;
import com.MobiTrade.R;
import com.MobiTrade.network.MobiTradeProtocol;
import com.MobiTrade.network.MobiTradeService;
import com.MobiTrade.sqlite.DatabaseHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MobiTradeMain extends TabActivity implements TabHost.TabContentFactory {

	private static String TAG = "MobiTrade";
	public static final String SERVICE_NAME = "MobiTrade";
	public static final UUID MobiTrade_UUID = UUID.fromString("62ed7d60-2480-11e0-ac64-0800200c9a20");

	// Used as dialog identifiers
	public static final int REQUEST_ENABLE_BT = 1;
	public static final int REQUEST_MAKE_DEVISE_DISCOVERABLE = 2;
	public static final int DISCOVERABLE_DURATION = 300;

	// On-Off CheckBox
	private CheckBox onOff = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Init database
		if(DatabaseHelper.getDBManager() == null)
			DatabaseHelper.CreateOpenDBManager(this);
		
		DatabaseHelper.getDBManager().updateConfigMainActivityStatus(1);
		
		TabHost tabHost = getTabHost();  // The activity TabHost
		Intent intent;  // Reusable Intent for each tab
		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, TabGroupDashboardActivity.class);
		tabHost.addTab(tabHost.newTabSpec("dashboard").setIndicator("Dashboard").setContent(intent));
		// Available Channels Tab
		intent = new Intent().setClass(this, TabGroupChannelsActivity.class);
		tabHost.addTab(tabHost.newTabSpec("channels").setIndicator("Channels").setContent(intent));
		// Requested Channels Tab
		intent = new Intent().setClass(this, TabGroupRequestedChannelsActivity.class);
		tabHost.addTab(tabHost.newTabSpec("requested Channels").setIndicator("Requested Channels").setContent(intent));
		// Config Tab
		intent = new Intent().setClass(this, ConfigTab.class);
		tabHost.addTab(tabHost.newTabSpec("config").setIndicator("Config").setContent(intent));

		setupUI();


		onOff = (CheckBox) findViewById(R.id.onOffCheck);



	}

	@Override
	protected void onStart() 
	{
		// TODO Auto-generated method stub
		super.onStart();

		if(DatabaseHelper.getDBManager().getConfigServiceStatus() == 1)
		{
			onOff.setChecked(true);
			onOff.setButtonDrawable(R.drawable.on70);
		}else
		{
			onOff.setChecked(false);
			onOff.setButtonDrawable(R.drawable.off70);
		}

		// Registers the broadcast receiver
		RegisterBroadcastReceiver();

		onOff.setOnCheckedChangeListener(new OnCheckedChangeListener() { 

			@Override 
			public void onCheckedChanged(CompoundButton buttonView, 
					boolean isChecked) { 
				// TODO Auto-generated method stub 
				if (buttonView.isChecked()) 
				{
					onOff.setButtonDrawable(R.drawable.on70);
					// Enable Bluetooth Interface as well as discovery if the interface gets up
					EnableBluetoothInterface();
				} 
				else 
				{ 
					// Disable bluetoth interface as well as discovery, ongoing sessions and the bluetooth server
					buttonView.setButtonDrawable(R.drawable.off70);

					if(DatabaseHelper.getDBManager().getConfigServiceStatus() == 1)
					{
						// Stops the mobitrade service
						stopService(new Intent(MobiTradeMain.this, MobiTradeService.class));
					}

				} 

			} 
		}); 
	
	}



	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();

		// Unregister the broadcast receiver
		UnregisterBroadcastReceiver();
		DatabaseHelper.getDBManager().updateConfigMainActivityStatus(0);

		Log.i(TAG, "MobiTradeMain OnStop called.");
	}


	// Registed the activity to receive indication whenever a new device is received
	private void RegisterBroadcastReceiver()
	{
		// Register the BroadcastReceiver
		if(mReceiver != null)
		{
			registerReceiver(mReceiver, new IntentFilter(DashboardTab.ACTIVE_DISCOVERABLE_MODE));

		}else
		{
			Log.e(TAG, "Invalid broadcast receiver when trying to register.");
		}
	}

	private void UnregisterBroadcastReceiver()
	{
		if(mReceiver != null)
			unregisterReceiver(mReceiver);
		else
		{
			Log.e(TAG, "Invalid boradcast receiver when trying to unregister.");
		}
	}


	@Override
	public View createTabContent(String tag) {
		TextView tv = new TextView(this);
		tv.setTextColor(Color.BLACK);
		tv.setTextSize(20);
		if (tag.equals("schedule")) {
			tv.setText("Schedule");
		} else if (tag.equals("conferences")) {
			tv.setText("conferences");
		} else if (tag.equals("exhibitors")) {
			tv.setText("Exhibitors");
		} else if (tag.equals("more")) {
			tv.setText("More");
		}else if (tag.equals("speackers")) {
			tv.setText("Speackers");
		}
		return tv;
	}

	private void setupUI() {
		RadioButtonCenter rbFirst = (RadioButtonCenter) findViewById(R.id.first);
		RadioButtonCenter rbSecond = (RadioButtonCenter) findViewById(R.id.second);
		RadioButtonCenter rbThird = (RadioButtonCenter) findViewById(R.id.third);
		RadioButtonCenter rbFourth = (RadioButtonCenter) findViewById(R.id.fourth);

		rbFirst.setButtonDrawable(R.drawable.dashboard8070);
		rbSecond.setButtonDrawable(R.drawable.channels8070);
		rbThird.setButtonDrawable(R.drawable.mychannels8070);
		rbFourth.setButtonDrawable(R.drawable.config8070);

		Display display = getWindowManager().getDefaultDisplay();
		rbFirst.setWidth(display.getWidth()/4);
		rbSecond.setWidth(display.getWidth()/4);
		rbThird.setWidth(display.getWidth()/4);
		rbFourth.setWidth(display.getWidth()/4);


		RadioGroup rg = (RadioGroup) findViewById(R.id.states);
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, final int checkedId) {
				switch (checkedId) {
				case R.id.first:
					getTabHost().setCurrentTab(0);
					break;
				case R.id.second:
					getTabHost().setCurrentTab(1);
					break;
				case R.id.third:
					getTabHost().setCurrentTab(2);
					break;
				case R.id.fourth:
					getTabHost().setCurrentTab(3);
					break;
				default:

				}
			}
		});
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();

			if(action.equals(DashboardTab.ACTIVE_DISCOVERABLE_MODE))
			{
				// We should make the device discoverable if it is not already in discoverable mode
				MakeTheDeviceDiscoverable();
			}
		}
	};

	// Enable the device bluetooth interface
	public void EnableBluetoothInterface()
	{
		// If the adapter is null, then Bluetooth is not supported
		if (BluetoothAdapter.getDefaultAdapter() == null) 
		{
			finish();
			return;
		}

		if(BluetoothAdapter.getDefaultAdapter().isEnabled())
		{

			// Start MobiTrade service
			if(DatabaseHelper.getDBManager().getConfigServiceStatus() == 0)
				startService(new Intent(this, MobiTradeService.class));

			sendBroadcast(new Intent(DashboardTab.MOBITRADE_ON));

			if(MobiTradeProtocol.GetMobiTradeProtocol() != null)
			{
				if(MobiTradeProtocol.GetMobiTradeProtocol().GetNumberOfSessions() == 0 && !BluetoothAdapter.getDefaultAdapter().isDiscovering())
				{
					MobiTradeProtocol.GetMobiTradeProtocol().StartDiscovery();
				}
			}
			MakeTheDeviceDiscoverable();

		}else 
		{
			// Enable the bluetooth adapter if it is not enabled
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, MobiTradeMain.REQUEST_ENABLE_BT);
		}
	}

	// Enable the device to be discovered
	public void MakeTheDeviceDiscoverable()
	{
		if(BluetoothAdapter.getDefaultAdapter().isEnabled())
		{
			Intent discoverableIntent = new	Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, MobiTradeMain.DISCOVERABLE_DURATION);
			startActivityForResult(discoverableIntent, MobiTradeMain.REQUEST_MAKE_DEVISE_DISCOVERABLE);
		}else
		{
			AlertDialog.Builder bluetoothOff = new AlertDialog.Builder(this);
			bluetoothOff.setMessage("The Bluetooth adapter is not enabled. Please start MobiTrade first.")
			.setCancelable(false)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			});

			AlertDialog noChannelsAlert = bluetoothOff.create();
			noChannelsAlert.setIcon(R.drawable.mobitradeicon);
			noChannelsAlert.show();
		}
	}


	// Checking the results from the dialogs asking for activating the bluetooth interface, ...
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub

		super.onActivityResult(requestCode, resultCode, data);

		switch(requestCode)
		{

		case REQUEST_ENABLE_BT:
			if(resultCode == Activity.RESULT_OK)
			{
				// Bluetooth activated, making the device discoverable
				Log.i(TAG, "Bluetooth activated, making the device discoverable");

				// Start MobiTrade service
				if(DatabaseHelper.getDBManager().getConfigServiceStatus() == 0)
					startService(new Intent(this, MobiTradeService.class));

				onOff.setChecked(true);
				onOff.setButtonDrawable(R.drawable.on70);

				sendBroadcast(new Intent(DashboardTab.MOBITRADE_ON));

				MakeTheDeviceDiscoverable();

			}else
			{
				Log.i(TAG, "Bluetooth activation aborded, result code: " + Integer.toString(resultCode));

				onOff.setChecked(false);
				onOff.setButtonDrawable(R.drawable.off70);

				sendBroadcast(new Intent(DashboardTab.MOBITRADE_OFF));

			}
			break;

		case REQUEST_MAKE_DEVISE_DISCOVERABLE:
			if(resultCode == DISCOVERABLE_DURATION)
			{
				// The device is discoverable
				Log.i(TAG, "Bluetooth device is discoverable");

				sendBroadcast(new Intent(DashboardTab.MOBITRADE_DISCOVERABLE_MODE_ON));


			}else
			{
				// Unable to make 
				Log.i(TAG, "Unable to make the Bluetooth device discoverable, result code: " + Integer.toString(resultCode));
				// The bluetooth adapter is active, we can run session with paired device but
				// the device is not in the discoverable mode
				sendBroadcast(new Intent(DashboardTab.MOBITRADE_DISCOVERABLE_MODE_OFF));
			}
			break;
		default:
			break;
		}

	}


}


