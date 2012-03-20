package com.MobiTrade.network;

import java.util.Set;
import com.MobiTrade.ConfigTab;
import com.MobiTrade.DashboardTab;
import com.MobiTrade.MobiTradeMain;
import com.MobiTrade.R;
import com.MobiTrade.objectmodel.Content;
import com.MobiTrade.sqlite.DatabaseHelper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

public class MobiTradeService extends Service{

	private static String TAG = "MobiTrade";
	private int alwaysInDiscovery = 0;
	// set to 1 if the bluetooth adapter is active and to 0 otherwise
	private int bluetoothAdapterStatus = 0;

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_FOUND.equals(action)) 
			{

				BluetoothDevice newDev = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.i(TAG, "A new device has been found "+ newDev.getName());

				Intent ip = new Intent(DashboardTab.MOBITRADE_NEW_PEER_DISCOVERED);
				Bundle bp = new Bundle();
				bp.putString("NAME", newDev.getName());
				ip.putExtras(bp);

				sendBroadcast(ip);

				if(newDev != null)
				{
					MobiTradeProtocol.GetMobiTradeProtocol().AddNewDiscoveredDevice(newDev);
				}
			}else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
			{
				// End of discovery
				Log.i(TAG, "Discovery Finished @ "+DatabaseHelper.getCurrentTime());

				// Verify if the bluetooth server is still running or not
				MobiTradeProtocol.GetMobiTradeProtocol().StartMobiTradeBluetoothListener();

				// Mix the discovered devices with the already paired ones and start new sessions
				{
					Set<BluetoothDevice> pairedSet = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
					for( BluetoothDevice pd: pairedSet)
					{
						MobiTradeProtocol.GetMobiTradeProtocol().AddNewDiscoveredDevice(pd);
					}
				}

				int nbrDiscoveredNodes = MobiTradeProtocol.GetMobiTradeProtocol().GetNumberOfDiscoveredDevices();
				if(nbrDiscoveredNodes > 0)
				{	

					Intent i = new Intent(DashboardTab.MOBITRADE_DISCOVERY_1);
					Bundle b = new Bundle();
					b.putInt("NUMBER", nbrDiscoveredNodes);
					i.putExtras(b);
					sendBroadcast(i);

					MobiTradeProtocol.GetMobiTradeProtocol().StartMobiTradeNegociationWith(
							MobiTradeProtocol.GetMobiTradeProtocol().getNextDevice());
				}else
				{
					Log.i(TAG, "No new devices discovered, restarting the discovery");
					sendBroadcast(new Intent(DashboardTab.MOBITRADE_DISCOVERY_0));

					// We start a new discovery only if there is no running sessions
					if(MobiTradeProtocol.GetMobiTradeProtocol().GetNumberOfSessions() == 0 && alwaysInDiscovery == 1)
						MobiTradeProtocol.GetMobiTradeProtocol().StartDiscovery();
				}

			}else if(action.compareTo(DashboardTab.MOBITRADE_NEW_CONTENT_RECEIVED) == 0)
			{
				Bundle b = intent.getExtras();
				String content = b.getString(Content.CONTENT_NAME);

				// Load a possibly updated configuration entries
				int useVibratorNotification = DatabaseHelper.getDBManager().getConfigVibratorNotification();

				// Start the vibration
				if(useVibratorNotification == 1)
				{
					Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
					vibrator.vibrate(DashboardTab.VIBRATION_CONTENT_RECEIVED);
				}

				// Send a status bar notification

				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

				// Initiate the notification
				int icon = R.drawable.mobitradeiconstatusbar2424;
				CharSequence tickerText = "New content received: "+content;
				long when = System.currentTimeMillis();

				Notification notification = new Notification(icon, tickerText, when);



				CharSequence contentTitle = "MobiTrade, new content received";
				CharSequence contentText = "New content received: "+content;

				PendingIntent contentIntent = null;

				Intent notificationIntent = new Intent(MobiTradeService.this, MobiTradeMain.class);
				contentIntent = PendingIntent.getActivity(MobiTradeService.this, 0, notificationIntent, 0);
				
				notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

				mNotificationManager.notify(1, notification);

			}else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) 
			{
				// End of discovery
				Log.i(TAG, "Bluetooth Scan Mode changed, current adapter status: "+bluetoothAdapterStatus);

				if(BluetoothAdapter.getDefaultAdapter().getState() == BluetoothAdapter.STATE_OFF && bluetoothAdapterStatus == 1)
				{
					Log.i(TAG, "Bluetooth adapter is turned off, pausing the service");
					// The bluetooth adapter is switched off

					// Stopping MobiTrade Bluetooth server
					MobiTradeProtocol.GetMobiTradeProtocol().StopMobiTradeBluetoothListener();

					// Cancel the session timeOut timer
					MobiTradeProtocol.GetMobiTradeProtocol().CancelSessionsTimer();

					// Stopping all ongoing sessions
					MobiTradeProtocol.GetMobiTradeProtocol().StopOngoingSessions();


					// Notifies the dashboar that we are closing
					sendBroadcast(new Intent(DashboardTab.MOBITRADE_OFF));

					bluetoothAdapterStatus = 0;

				}else if(BluetoothAdapter.getDefaultAdapter().getState() == BluetoothAdapter.STATE_ON && bluetoothAdapterStatus == 0)
				{
					// The bluetooth adapter is switched on
					Log.i(TAG, "Bluetooth adapter is turned on, re starting");

					// Start bluetooth server
					MobiTradeProtocol.GetMobiTradeProtocol().StartMobiTradeBluetoothListener();

					MobiTradeProtocol.GetMobiTradeProtocol().StartSessionsTimer();

					// Verify if we should stay always in discovery mode or not
					alwaysInDiscovery = DatabaseHelper.getDBManager().getConfigAlwaysDiscovering();

					// Registers the broadcast receiver
					RegisterBroadcastReceiver();

					// Init the temporary status used via the Dashboard
					DatabaseHelper.getDBManager().updateConfigLiveStatus("none");
					DatabaseHelper.getDBManager().updateConfigLiveStatus2("none");

					if(MobiTradeProtocol.GetMobiTradeProtocol().GetNumberOfSessions() == 0 && !BluetoothAdapter.getDefaultAdapter().isDiscovering())
					{
						MobiTradeProtocol.GetMobiTradeProtocol().StartDiscovery();
					}

					// Notifies the dashboar that we are closing
					sendBroadcast(new Intent(DashboardTab.MOBITRADE_ON));

					bluetoothAdapterStatus = 1;
				} 

				int mode = intent.getExtras().getInt(BluetoothAdapter.EXTRA_SCAN_MODE);
				if(mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
				{
					// Still in the discoverable mode
				}else if(mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE)
				{
					// Not in the discoverable mode but still can receive connections
					//MakeTheDeviceDiscoverable();
					if(BluetoothAdapter.getDefaultAdapter().isEnabled())
						sendBroadcast(new Intent(DashboardTab.MOBITRADE_DISCOVERABLE_MODE_OFF));
				}else if(mode == BluetoothAdapter.SCAN_MODE_NONE)
				{
					// Not in the discoverble mode and cannot receive anymore connections
					if(BluetoothAdapter.getDefaultAdapter().isEnabled())
						sendBroadcast(new Intent(DashboardTab.MOBITRADE_DISCOVERABLE_MODE_OFF));
				}

			}else if(action.equals(DashboardTab.START_NEW_DISCOVERY_SESSION))
			{
				// We start a new discovery session if and only if there is no running sessions
				if(MobiTradeProtocol.GetMobiTradeProtocol().GetNumberOfSessions() == 0)
				{	
					if(!BluetoothAdapter.getDefaultAdapter().isDiscovering())
						MobiTradeProtocol.GetMobiTradeProtocol().StartDiscovery();
				}

			}else if(action.equals(ConfigTab.DISCOVERY_MODE_CHANGED))
			{
				int tmp = alwaysInDiscovery;
				alwaysInDiscovery = DatabaseHelper.getDBManager().getConfigAlwaysDiscovering();
				if(tmp == 0 && alwaysInDiscovery == 1)
				{
					MobiTradeProtocol.GetMobiTradeProtocol().StartDiscovery();
				}
			}

		}
	};

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		DatabaseHelper.getDBManager().updateConfigServiceStatus(1);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		DatabaseHelper.getDBManager().updateConfigServiceStatus(0);

		// Stopping MobiTrade Bluetooth server
		MobiTradeProtocol.GetMobiTradeProtocol().StopMobiTradeBluetoothListener();

		// Unregister the broadcast receiver
		UnregisterBroadcastReceiver();

		// Cancel the session timeOut timer
		MobiTradeProtocol.GetMobiTradeProtocol().CancelSessionsTimer();

		// Stopping all ongoing sessions
		MobiTradeProtocol.GetMobiTradeProtocol().StopOngoingSessions();

		// Notifies the dashboar that we are closing
		sendBroadcast(new Intent(DashboardTab.MOBITRADE_OFF));

		// Close the database 
		DatabaseHelper.getDBManager().close();

		// Disale the device bluetooth discovery and adapter
		MobiTradeProtocol.GetMobiTradeProtocol().DisableBluetoothInterface();

		Log.i(TAG, "MobiTrade Service shutdown");
	}

	private void RegisterBroadcastReceiver()
	{
		// Register the BroadcastReceiver
		if(mReceiver != null)
		{
			registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // Don't forget to unregister during onDestroy
			registerReceiver(mReceiver,  new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
			registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));

			registerReceiver(mReceiver, new IntentFilter(DashboardTab.START_NEW_DISCOVERY_SESSION));
			registerReceiver(mReceiver, new IntentFilter(ConfigTab.DISCOVERY_MODE_CHANGED));
			registerReceiver(mReceiver, new IntentFilter(DashboardTab.MOBITRADE_NEW_CONTENT_RECEIVED));
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
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		// TODO Auto-generated method stub

		bluetoothAdapterStatus = 1;

		// Init MobiTrade Protocol
		MobiTradeProtocol.InitMobiTradeProtocol(this);

		// Start bluetooth server
		MobiTradeProtocol.GetMobiTradeProtocol().StartMobiTradeBluetoothListener();

		MobiTradeProtocol.GetMobiTradeProtocol().StartSessionsTimer();

		// Verify if we should stay always in discovery mode or not
		alwaysInDiscovery = DatabaseHelper.getDBManager().getConfigAlwaysDiscovering();

		// Registers the broadcast receiver
		RegisterBroadcastReceiver();

		// Init the temporary status used via the Dashboard
		DatabaseHelper.getDBManager().updateConfigLiveStatus("none");
		DatabaseHelper.getDBManager().updateConfigLiveStatus2("none");

		if(MobiTradeProtocol.GetMobiTradeProtocol().GetNumberOfSessions() == 0 && !BluetoothAdapter.getDefaultAdapter().isDiscovering())
		{
			MobiTradeProtocol.GetMobiTradeProtocol().StartDiscovery();
		}

		Log.i(TAG, "MobiTrade Service started !");

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
