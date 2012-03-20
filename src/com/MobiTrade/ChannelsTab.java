package com.MobiTrade;

import java.util.HashMap;
import java.util.List;

import com.MobiTrade.R;
import com.MobiTrade.objectmodel.Channel;
import com.MobiTrade.sqlite.DatabaseHelper;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ChannelsTab extends Activity {
	private ListView channelsListView;
	private HashMap<Integer, Channel> mapChannelsPositions = new HashMap<Integer, Channel>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.channels);

		//Récupération de la listview créée dans le fichier main.xml
		channelsListView = (ListView) findViewById(com.MobiTrade.R.id.channelsList);
		registerForContextMenu(channelsListView);

		TextView text = (TextView)findViewById(R.id.channelsTitle);
		text.setText("All Channels:");
		
		// Create our own version of the list adapter
		List<Channel> channels = DatabaseHelper.getDBManager().getAllChannels();

		// Create a mapping between the channel object and the position in the listview
		if(mapChannelsPositions.size() > 0)
			mapChannelsPositions.clear();
		int i = 0;
		for(Channel ch:channels)
		{
			mapChannelsPositions.put(i, ch);
			i++;
		}
		
		ListAdapter adapter = new ChannelAdapter(this, channels, R.layout.channelresume, new String[] {
				Channel.CHANNEL_IMAGE,  Channel.CHANNEL_KEYWORDS ,  Channel.CHANNEL_NBR_CONTENTS, Channel.CHANNEL_CREATION_DATE, Channel.CHANNEL_TEXT_UTILITY},  
				new int[] {R.id.channelImg, R.id.channelDescription, R.id.channelNbrContents, R.id.channelCreationDate, R.id.channelMaxAllowedSpace});

		channelsListView.setAdapter(adapter);


		//on met un écouteur d'évènement sur notre listView
		channelsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			@SuppressWarnings("unchecked")
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				//on récupère la HashMap contenant les infos de notre item (titre, description, img)
				HashMap<String, String> map = (HashMap<String, String>) channelsListView.getItemAtPosition(position);

				long nbrContents = 	DatabaseHelper.getDBManager().getContentsForChannel(map.get(Channel.CHANNEL_KEYWORDS)).size();
				
				if(nbrContents > 0)
				{

					// Start an activity that shows the list of contents with respect to the selected channel
					// Create the bundle that will hot the details of the content
					Bundle channelDetails = new Bundle();
					channelDetails.putString(Channel.CHANNEL_KEYWORDS, map.get(Channel.CHANNEL_KEYWORDS));
					channelDetails.putString(Channel.CHANNEL_IMAGE, map.get(Channel.CHANNEL_IMAGE));	

					// Crate the intent that will be used to start the ContentDetails activity
					Intent goToDetails = new Intent(getParent(), ContentsTab.class);
					// Add the details bundle to the intent
					goToDetails.putExtras(channelDetails);

					((TabGroupActivity)getParent()).startChildActivity("Contents", goToDetails);
				}else
				{
					// The selected channel don't have any associated content
					// Show a dialog
					AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
					builder.setMessage("Actually, there is no content associated to this channel.")
					.setCancelable(false)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							// TODO Auto-generated method stub

						}

					});
					AlertDialog alert = builder.create();
					alert.setIcon(R.drawable.mobitradeicon);
					alert.show();

				}
			}
		});

	}
	private void UpdateListAvailableChannels()
	{
		
		// Create our own version of the list adapter
		List<Channel> channels = DatabaseHelper.getDBManager().getAllChannels();
		

		//Récupération de la listview créée dans le fichier main.xml
		channelsListView = (ListView) findViewById(com.MobiTrade.R.id.channelsList);
		
		// Create a mapping between the channel object and the position in the listview
		if(mapChannelsPositions.size() > 0)
			mapChannelsPositions.clear();
		int i = 0;
		for(Channel ch:channels)
		{
			mapChannelsPositions.put(i, ch);
			i++;
		}

		ListAdapter adapter = new ChannelAdapter(this, channels, R.layout.channelresume, new String[] {
				Channel.CHANNEL_IMAGE,  Channel.CHANNEL_KEYWORDS ,  Channel.CHANNEL_NBR_CONTENTS, Channel.CHANNEL_CREATION_DATE, Channel.CHANNEL_TEXT_UTILITY},  
				new int[] {R.id.channelImg, R.id.channelDescription, R.id.channelNbrContents, R.id.channelCreationDate, R.id.channelMaxAllowedSpace});

		channelsListView.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		UpdateListAvailableChannels();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.channelscontextmenu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) 
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		// Retriving the selected channel
		Channel selectedChannel = mapChannelsPositions.get((int)info.id);

		switch (item.getItemId()) {
		case R.id.joinChannel:
			// Updating the status of the channel within the database
			DatabaseHelper.getDBManager().updateChannelStatus(selectedChannel.get(Channel.CHANNEL_KEYWORDS), 1);
			// Updates the list of channels that appear within the listview
			UpdateListAvailableChannels();
			return true;
		case R.id.deleteNotRequestedChannel:
			// Deleting the selected channel
			DatabaseHelper.getDBManager().deleteChannel(selectedChannel.get(Channel.CHANNEL_KEYWORDS));
			UpdateListAvailableChannels();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
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
