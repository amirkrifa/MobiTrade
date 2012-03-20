package com.MobiTrade;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.MobiTrade.R;
import com.MobiTrade.network.MobiTradeProtocol;
import com.MobiTrade.objectmodel.Channel;
import com.MobiTrade.objectmodel.Content;
import com.MobiTrade.sqlite.DatabaseHelper;

public class DashboardTab extends Activity {


	// Created message details
	private String createdMessageTitle;
	private String createdMessageBody;
	private int mYear;
	private int mMonth;
	private int mDay;
	private StringBuilder selectedExpirationDate;
	private String selectedChannel;

	public static final String MOBITRADE_ON = "MobiTradeOn";
	public static final String MOBITRADE_OFF = "MobiTradeOff";
	public static final String MOBITRADE_DISCOVERABLE_MODE_ON = "DiscoverableOn";
	public static final String MOBITRADE_DISCOVERABLE_MODE_OFF = "DiscoverableOff";
	public static final String MOBITRADE_UPDATE_STATISTICS = "updateStatistics";
	public static final String MOBITRADE_NEW_CONTENT_RECEIVED = "MobiTradeContentReceived";
	public static final String MOBITRADE_NEW_CONTENT_SENT = "MobiTradeContentSent";
	public static final String MOBITRADE_NEW_CHANNEL_RECEIVED = "MobiTradeChannelReceived";
	public static final String MOBITRADE_NEW_PEER_DISCOVERED = "MobiTradePeerDiscovered";
	public static final String MOBITRADE_NEW_SESSION_STARTED = "MobiTradeSessionStarted";
	public static final String MOBITRADE_SESSION_CLOSED = "MobiTradeSessionClosed";
	public static final String MOBITRADE_DISCOVERING = "MobiTradeDiscovering";
	public static final String MOBITRADE_DISCOVERY_0 = "MobiTradeDDiscovery0";
	public static final String MOBITRADE_DISCOVERY_1 = "MobiTradeDDiscovery1";

	public static final String ACTIVE_DISCOVERABLE_MODE =  "ActiveDiscoverableMode";
	public static final String START_NEW_DISCOVERY_SESSION =  "StartNewDiscoverySession";

	// in ms
	public static final long VIBRATION_CONTENT_RECEIVED = 1000;
	private TextView liveStatus = null;
	private TextView liveStatus2 = null;

	private Button requestNewChannel = null;
	private Button publishNewContent = null;
	private Button turnDiscoverableMode = null;
	private Button startNewDiscoverySession = null;
	private int useVibratorNotification = 0;

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();


		DatabaseHelper.getDBManager().updateConfigLiveStatus(liveStatus.getText().toString());
		DatabaseHelper.getDBManager().updateConfigLiveStatus2(liveStatus2.getText().toString());

	}


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dashboard);

		// Request new channel button
		requestNewChannel = (Button) findViewById(R.id.requestChannel);
		requestNewChannel.setOnClickListener(new RequestNewChannelListener());

		// Publish a new content button
		publishNewContent = (Button) findViewById(R.id.publishContent);
		publishNewContent.setOnClickListener(new PublishNewContentListener());


		// Publish a new content button
		turnDiscoverableMode = (Button) findViewById(R.id.makeDeviceDiscoverable);
		turnDiscoverableMode.setOnClickListener(new MakeDeviceDiscoverable());

		startNewDiscoverySession = (Button) findViewById(R.id.startNewDiscoverySession);
		startNewDiscoverySession.setOnClickListener(new StartNewDiscoverySession());

		// Updating the statistics whithin the Dashboard	
		UpdateStatistics();

		liveStatus = (TextView)findViewById(R.id.liveStatus);
		liveStatus2 = (TextView)findViewById(R.id.liveStatus2);
		Log.i("MobiTrade", "onCreate called");

		liveStatus.setText("MobiTrade is Off.");
		liveStatus2.setText("...");

		useVibratorNotification = DatabaseHelper.getDBManager().getConfigVibratorNotification();
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.compareTo(MOBITRADE_ON) == 0)
			{
				liveStatus.setText("MobiTrade is On.");

			}else if(action.compareTo(MOBITRADE_OFF) == 0)
			{
				Log.i("MobiTrade", "MOBITRADE_OFF called");
				liveStatus.setText("MobiTrade is Off.");
				liveStatus2.setText("...");

			}else if(action.compareTo(MOBITRADE_DISCOVERABLE_MODE_ON) == 0)
			{
				liveStatus.setText("MobiTrade is ON & Discoverable.");

			}else if(action.compareTo(MOBITRADE_DISCOVERABLE_MODE_OFF) == 0)
			{
				liveStatus.setText("MobiTrade is ON & Not Discoverable.");

			}else if(action.compareTo(MOBITRADE_DISCOVERING) == 0)
			{
				liveStatus2.setText("Discovery Session Started ...");

			}else if(action.compareTo(MOBITRADE_DISCOVERY_0) == 0)
			{
				liveStatus2.setText("No new devices has been discovered ...");

			}else if(action.compareTo(MOBITRADE_DISCOVERY_1) == 0)
			{
				Bundle b = intent.getExtras();
				int n = b.getInt("NUMBER");
				liveStatus2.setText(Integer.toString(n)+" new devices has been discovered.");

			}else if(action.compareTo(MOBITRADE_UPDATE_STATISTICS) == 0)
			{
				UpdateStatistics();
			}else if(action.compareTo(MOBITRADE_NEW_CHANNEL_RECEIVED) == 0)
			{
				Bundle b = intent.getExtras();
				String channel = b.getString(Channel.CHANNEL_KEYWORDS);
				liveStatus2.setText("A new channel is received: "+channel);

			}else if(action.compareTo(MOBITRADE_NEW_CONTENT_RECEIVED) == 0)
			{
				Bundle b = intent.getExtras();
				String content = b.getString(Content.CONTENT_NAME);
				liveStatus2.setText("A new content is received: "+content);

			}else if(action.compareTo(MOBITRADE_NEW_CONTENT_SENT) == 0)
			{
				Bundle b = intent.getExtras();
				String content = b.getString(Content.CONTENT_NAME);
				String dev = b.getString("NAME");
				liveStatus2.setText("Content "+content+" sent to: "+dev);

			}else if(action.compareTo(MOBITRADE_NEW_PEER_DISCOVERED) == 0)
			{
				Bundle b = intent.getExtras();
				String name = b.getString("NAME");
				liveStatus2.setText("A new device is discovered: "+name);

			}else if(action.compareTo(MOBITRADE_NEW_SESSION_STARTED) == 0)
			{
				Bundle b = intent.getExtras();
				String name = b.getString("NAME");
				liveStatus2.setText("Starting new session with: "+name);

			}else if(action.compareTo(MOBITRADE_SESSION_CLOSED) == 0)
			{
				Bundle b = intent.getExtras();
				String name = b.getString("NAME");
				liveStatus2.setText("Ending session with: "+name);
			}else if(action.compareTo(ConfigTab.DISCOVERY_MODE_CHANGED) == 0)
			{
				// See wether we should enable or disable the start discovery button

				int tmp = DatabaseHelper.getDBManager().getConfigAlwaysDiscovering();
				if(tmp == 0)
					startNewDiscoverySession.setEnabled(true);
				else
					startNewDiscoverySession.setEnabled(false);

			}

		}
	};



	private void RegisterBroadcastReceiver()
	{
		// Register the BroadcastReceiver
		if(mReceiver != null)
		{
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_NEW_CHANNEL_RECEIVED));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_NEW_CONTENT_RECEIVED));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_NEW_CONTENT_SENT));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_NEW_PEER_DISCOVERED));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_NEW_SESSION_STARTED));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_OFF));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_ON));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_DISCOVERABLE_MODE_OFF));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_DISCOVERABLE_MODE_ON));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_SESSION_CLOSED));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_UPDATE_STATISTICS));
			registerReceiver(mReceiver, new IntentFilter(MOBITRADE_DISCOVERING));
			registerReceiver(mReceiver, new IntentFilter(ConfigTab.DISCOVERY_MODE_CHANGED));

		}else
		{
			Log.e("MobiTrade", "Invalid dashboard broadcast receiver when trying to register.");
		}
	}

	private void UnregisterBroadcastReceiver()
	{
		if(mReceiver != null)
			unregisterReceiver(mReceiver);
		else
		{
			Log.e("MobiTrade", "Invalid dashboard boradcast receiver when trying to unregister.");
		}
	}



	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		RegisterBroadcastReceiver();

	}


	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		UnregisterBroadcastReceiver();

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		if(DatabaseHelper.getDBManager().getConfigAlwaysDiscovering() == 0)
		{
			startNewDiscoverySession.setEnabled(true);
		}else
		{
			startNewDiscoverySession.setEnabled(false);
		}

		// Load a possibly updated configuration entries
		useVibratorNotification = DatabaseHelper.getDBManager().getConfigVibratorNotification();

		UpdateStatistics();
		String tmp1 = DatabaseHelper.getDBManager().getConfigLiveStatus();
		String tmp2 = DatabaseHelper.getDBManager().getConfigLiveStatus2();
		if(tmp1.compareTo("none") != 0)
		{
			liveStatus.setText(tmp1);
		}
		if(tmp2.compareTo("none") != 0)
		{
			liveStatus2.setText(tmp2);
		}

	}

	private void UpdateStatistics()
	{
		int totalNumberOfChannels = DatabaseHelper.getDBManager().getTotalNumberOfChannels();
		int totalNumberOfContents = DatabaseHelper.getDBManager().getTotalNumberOfContents();
		int totalNumberOfEncounteredUsers = 0;
		int numberOfRequestedChannels = DatabaseHelper.getDBManager().GetNumberOfRequestedChannels();
		// in bytes
		long mobitradeUsedSpace = DatabaseHelper.getDBManager().getMobiTradeAllocatedSpace();
		long mobitradeCapacity = DatabaseHelper.getDBManager().getConfigMaxAllowedSpace() * ConfigTab.megabyte;

		EditText nbrChannels = (EditText)findViewById(R.id.totalNumberOfChannelsVal);
		nbrChannels.setText(Integer.toString(totalNumberOfChannels));

		EditText nbrContents = (EditText)findViewById(R.id.totalNumberOfContentsVal);
		nbrContents.setText(Integer.toString(totalNumberOfContents));

		BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter();
		if(b != null)
			totalNumberOfEncounteredUsers = b.getBondedDevices().size();

		EditText nbrUsers = (EditText)findViewById(R.id.numberOfRequestedChannelsVal);
		nbrUsers.setText(Integer.toString(numberOfRequestedChannels));

		EditText nbrRequestedChannels = (EditText)findViewById(R.id.totalNumberOfEncounteredUsersVal);
		nbrRequestedChannels.setText(Integer.toString(totalNumberOfEncounteredUsers));

		int per = (int) Math.ceil((float)mobitradeUsedSpace/mobitradeCapacity);

		EditText mobitradeSpace = (EditText)findViewById(R.id.percentageOfUsedSpace);
		mobitradeSpace.setText("~ "+Integer.toString(per)+" %");

	}

	// request new channel button listener
	private class RequestNewChannelListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			//			showDialog(REQUEST_CHANNEL_DIALOG);
			final CharSequence[] channelTypes = {"Request an Existing Channel", "Create a New Channel"};
			final List<Channel> listAvailable = DatabaseHelper.getDBManager().getAllChannels();
			final LayoutInflater inflater=LayoutInflater.from(getParent());

			AlertDialog.Builder requestChannelBuilder = new AlertDialog.Builder(getParent());
			requestChannelBuilder.setTitle("Content Type");
			requestChannelBuilder.setSingleChoiceItems(channelTypes, -1, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					// Action to be executed once the content type is selected

					switch (item)
					{
					case 0:
						// Requesting an existing channel
						// Show the list of available channels so, the user can choose among them
						// Get the list of available channels from the DatabaseHelper
						if(listAvailable.size() > 0)
						{
							final CharSequence[] items;
							items = new CharSequence[listAvailable.size()];
							// Adding the names of the channels
							int i = 0;
							for(Channel ch:listAvailable)
							{
								items[i] = ch.get(Channel.CHANNEL_KEYWORDS);
								i++;
							}

							AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
							builder.setTitle("Join a channel");
							builder.setItems(items, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {

									DatabaseHelper.getDBManager().updateChannelStatus(items[item].toString(), 1);
								}
							});

							AlertDialog alert = builder.create();
							alert.setIcon(R.drawable.mobitradeicon);
							alert.show();
						}else if(listAvailable.size() == 0)
						{
							// The user have joined all the requested channels
							AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
							builder.setMessage("There is no channel available.")
							.setCancelable(false)
							.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {}

							});
							AlertDialog alert = builder.create();
							alert.setIcon(R.drawable.mobitradeicon);
							alert.show();

						}else
						{
							// The user have joined all the requested channels
							AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
							builder.setMessage("You've already joined all the existing channels.")
							.setCancelable(false)
							.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {}

							});
							AlertDialog alert = builder.create();
							alert.setIcon(R.drawable.mobitradeicon);
							alert.show();
						}
						break;
					case 1:
						// Requesting a new channel
						AlertDialog.Builder alert = new AlertDialog.Builder(getParent());
						alert.setView(inflater.inflate(R.layout.newchanneldialog, null));


						alert.setIcon(R.drawable.mobitradeicon);
						alert.setTitle("Channel keywords:");
						alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								EditText input = (EditText)((AlertDialog)dialog).findViewById(R.id.channelKeywordsEntry);
								String channelKeywords = input.getText().toString().trim();
								if(channelKeywords.length() > 0)
								{
									// Asking the database helper to create the channel
									if(DatabaseHelper.getDBManager().insertChannel(channelKeywords, 0, MobiTradeProtocol.INITIAL_LOCAL_CHANNEL_UTILITY, 1,  String.valueOf(com.MobiTrade.R.drawable.channel))
											!= -1)
									{
										if(MobiTradeProtocol.GetMobiTradeProtocol() != null)
											MobiTradeProtocol.GetMobiTradeProtocol().dispatchAvailableStorage();

										// Update the activity 
										UpdateStatistics();
									}else
									{
										// Channel already available
										Toast.makeText(DashboardTab.this, "The channel: "+channelKeywords+" is already available.", Toast.LENGTH_LONG).show();
									}
								}else
								{
									Toast.makeText(DashboardTab.this, "Please provide the channel keywords. Creation cancelled.", Toast.LENGTH_LONG).show();
								}
							}
						});

						alert.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								dialog.cancel();
							}
						});

						alert.show();	
						break;
					default:;
					};
					dialog.cancel();
				}
			});

			Dialog createdDialog = requestChannelBuilder.create();
			createdDialog.show();

		}	    
	}

	// publish a new content listener
	private class PublishNewContentListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			final CharSequence[] contentTypes = {"Publish an Existing File", "Write a Message"};
			final List<Channel> listAvailable = DatabaseHelper.getDBManager().getAllChannels();
			final LayoutInflater inflater=LayoutInflater.from(getParent());

			AlertDialog.Builder publishContentBuilder = new AlertDialog.Builder(getParent());
			publishContentBuilder.setTitle("Content Type");

			publishContentBuilder.setSingleChoiceItems(contentTypes, -1, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {

					switch(item)
					{

					// Publishing an existing content
					case 0:
						// Asking within which channel the user wants to publish its content

						if(listAvailable.size() > 0)
						{
							final CharSequence[] items;
							items = new CharSequence[listAvailable.size()];
							// Adding the names of the channels
							int i = 0;
							for(Channel ch:listAvailable)
							{
								items[i] = ch.get(Channel.CHANNEL_KEYWORDS);
								i++;
							}

							AlertDialog.Builder selectChannelBuilder = new AlertDialog.Builder(getParent());
							selectChannelBuilder.setTitle("Select the content' channel:");

							selectChannelBuilder.setItems(items, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {

									String selectedChannel = items[item].toString();
									Intent intentToExplorer = new Intent(getParent(), FileExplorer.class);
									intentToExplorer.putExtra(Channel.CHANNEL_KEYWORDS, selectedChannel);
									// Starting the file explorer to select and publish the content
									((TabGroupActivity)getParent()).startChildActivity("Explorer",intentToExplorer);
								}
							});

							AlertDialog selectChannelAlert = selectChannelBuilder.create();
							selectChannelAlert.setIcon(R.drawable.mobitradeicon);
							selectChannelAlert.show();

						}else if(listAvailable.size() == 0)
						{
							// 0 channels available
							AlertDialog.Builder noChannelsbuilder = new AlertDialog.Builder(getParent());
							noChannelsbuilder.setMessage("There is no channel available, please create a channel first.")
							.setCancelable(false)
							.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {}
							});

							AlertDialog noChannelsAlert = noChannelsbuilder.create();
							noChannelsAlert.setIcon(R.drawable.mobitradeicon);
							noChannelsAlert.show();

						}

						break;

						// Writing a text message, saving it to a file and publishing it
					case 1:

						// Asking within which channel the user wants to publish its content
						if(listAvailable.size() > 0)
						{
							final CharSequence[] items;
							items = new CharSequence[listAvailable.size()];
							// Adding the names of the channels
							int i = 0;
							for(Channel ch:listAvailable)
							{
								items[i] = ch.get(Channel.CHANNEL_KEYWORDS);
								i++;
							}


							AlertDialog.Builder channelChoiceBuilder = new AlertDialog.Builder(getParent());
							channelChoiceBuilder.setTitle("Select the content' channel:");

							channelChoiceBuilder.setItems(items, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {

									selectedChannel = items[item].toString();

									// Reading the message, title and content
									AlertDialog.Builder msgDetailsBuilder = new AlertDialog.Builder(getParent());

									msgDetailsBuilder.setIcon(R.drawable.mobitradeicon);
									msgDetailsBuilder.setView(inflater.inflate(R.layout.message_details, null));
									msgDetailsBuilder.setTitle("Message details:");

									msgDetailsBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											// Saving the selectedContentDescription and Asking for a time to live
											EditText inputTitle = (EditText)((AlertDialog)dialog).findViewById(R.id.messageTitleIn);
											EditText inputBody = (EditText)((AlertDialog)dialog).findViewById(R.id.messageBodyIn);

											createdMessageTitle = inputTitle.getText().toString().trim();
											createdMessageBody = inputBody.getText().toString().trim();

											if(createdMessageTitle.length() > 0 && createdMessageBody.length() > 0)
											{
												// Showing the Datepicker dialog
												// get the current date
												Calendar cal = Calendar.getInstance();
												mYear = cal.get(Calendar.YEAR);
												mMonth = cal.get(Calendar.MONTH);
												mDay = cal.get(Calendar.DAY_OF_MONTH);

												Dialog datePeekerDialog = new DatePickerDialog(getParent(), mDateSetListenerForMessage, mYear, mMonth, mDay);
												datePeekerDialog.setTitle("Choose an expiration date for this content (Optional)");
												datePeekerDialog.setOnCancelListener(new DatePickerDialog.OnCancelListener() {

													@Override
													public void onCancel(DialogInterface dialogw) {
														// TODO Auto-generated method stub
														// The DatePickerDialog is canceled and 
														// null ttl has been set
														Log.i("MobiTrade", "TTL selection cancelled.");

														// Creating a text file on the sdcard to save the user message
														DatabaseHelper.SaveMessageToMobiTradeSDcard(createdMessageTitle+".txt", createdMessageBody);
														// We have the expiration date, so add the content to the database
														DatabaseHelper.getDBManager().insertContent(createdMessageTitle, BluetoothAdapter.getDefaultAdapter().getName(), DatabaseHelper.mobiTradeSdcardPath + createdMessageTitle + ".txt", "", selectedExpirationDate.toString(), Content.CONTENT_DEFAULT_UTILITY, selectedChannel, DatabaseHelper.getDBManager().GetCurrentDate(), ".txt", (int)DatabaseHelper.GetContentSizeFromSdcard(createdMessageTitle + ".txt"), createdMessageTitle + ".txt");

														UpdateStatistics();

													}
												});

												datePeekerDialog.show();
											}else
											{
												Toast.makeText(DashboardTab.this, "Please provide all the content details. Creation cancelled.", Toast.LENGTH_LONG).show();
											}
										}
									});

									msgDetailsBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											dialog.cancel();
										}
									});

									AlertDialog msgDetailsDialog = msgDetailsBuilder.create();
									msgDetailsDialog.setIcon(R.drawable.mobitradeicon);
									msgDetailsDialog.show();
								}
							});

							AlertDialog selectChannelAlert = channelChoiceBuilder.create();
							selectChannelAlert.setIcon(R.drawable.mobitradeicon);
							selectChannelAlert.show();

						}else if(listAvailable.size() == 0)
						{
							// The user have joined all the requested channels
							AlertDialog.Builder noChannelsBuilder = new AlertDialog.Builder(getParent());
							noChannelsBuilder.setMessage("There is no channel available, please create a channel first.")
							.setCancelable(false)
							.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {}
							});
							AlertDialog noChannelsAlert = noChannelsBuilder.create();
							noChannelsAlert.setIcon(R.drawable.mobitradeicon);
							noChannelsAlert.show();
						}

						break;
					default:;

					};

					dialog.cancel();
				}
			});

			Dialog createdDialog = publishContentBuilder.create();
			createdDialog.show();

		}	    
	}

	private class MakeDeviceDiscoverable implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			// Send a broadcast intent to the main activity in order to turn on the discoverable mode
			Intent i = new Intent(ACTIVE_DISCOVERABLE_MODE);
			sendBroadcast(i);
		}
	}

	private class StartNewDiscoverySession implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			// Send a broadcast intent to the main activity in order to turn on the discoverable mode
			Intent i = new Intent(START_NEW_DISCOVERY_SESSION);
			sendBroadcast(i);
		}
	}

	private class AddImgAdp extends BaseAdapter 
	{
		int GalItemBg;
		private Context cont;
		// Adding images.

		private List<Channel> channels = new ArrayList<Channel>();


		private int getImage(int pos)
		{
			return Integer.parseInt(channels.get(pos).get(Channel.CHANNEL_IMAGE));
		}

		public int getCount() 
		{
			return channels.size();
		}
		public Object getItem(int position) 
		{		
			return position;
		}

		public long getItemId(int position) 
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) 
		{	
			ImageView imgView = new ImageView(cont);
			imgView.setImageResource(getImage(position));

			// Fixing width & height for image to display
			imgView.setLayoutParams(new Gallery.LayoutParams(150, 150));
			imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			imgView.setBackgroundResource(GalItemBg);
			return imgView;
		}

	}

	private DatePickerDialog.OnDateSetListener mDateSetListenerForMessage = new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, 
				int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			selectedExpirationDate = new StringBuilder()
			// Month is 0 based so add 1
			.append(mDay).append("/")
			.append(mMonth + 1).append("/")
			.append(mYear);

			// The expiration date should not be before the current one
			Date curerentdate = new Date();
			Date expDate = new Date(mYear - 1900, mMonth, mDay);
			
			if(expDate.after(curerentdate))
			{
				Log.i("MobiTrade", "Publishing the nes content.");
				// Creating a text file on the sdcard to save the user message
				DatabaseHelper.SaveMessageToMobiTradeSDcard(createdMessageTitle+".txt", createdMessageBody);
				// We have the expiration date, so add the content to the database
				DatabaseHelper.getDBManager().insertContent(createdMessageTitle, BluetoothAdapter.getDefaultAdapter().getName(), DatabaseHelper.mobiTradeSdcardPath + createdMessageTitle + ".txt", "", selectedExpirationDate.toString(), Content.CONTENT_DEFAULT_UTILITY, selectedChannel, DatabaseHelper.getDBManager().GetCurrentDate(), ".txt", (int)DatabaseHelper.GetContentSizeFromSdcard(createdMessageTitle + ".txt"), createdMessageTitle + ".txt");
				UpdateStatistics();
			}else
			{
				// The selected expiration date is invalid
				Toast.makeText(DashboardTab.this, "The selected expiration date: "+ selectedExpirationDate + "is invalid. Publishing process cancelled.", Toast.LENGTH_LONG).show();
			}
		}
	};




}
