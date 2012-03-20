package com.MobiTrade;

import java.io.File;
import java.util.HashMap;
import java.util.List;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.MobiTrade.R;
import com.MobiTrade.objectmodel.Channel;
import com.MobiTrade.objectmodel.Content;
import com.MobiTrade.sqlite.DatabaseHelper;

public class ContentsTab extends Activity {

	// Attributes received throught the Intent
	private String channelKeywords;
	private HashMap<Integer, Content> mapContentsPositions = new HashMap<Integer, Content>();
	private ListView contentsListView;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.contents);

		
		Bundle extras = getIntent().getExtras();
		channelKeywords = extras.getString(Channel.CHANNEL_KEYWORDS);

		// Getting the textview describing the channel
		TextView channel = (TextView) findViewById(R.id.channelTitle);
		channel.setText("Channel: " + channelKeywords);
	

		//Récupération de la listview
		contentsListView = (ListView) findViewById(R.id.contentsList);
		
		// Register all the contentsListView to the context menu events
		registerForContextMenu(contentsListView);
		
		
		// Create our own version of the list adapter
		List<Content> contents = DatabaseHelper.getDBManager().getContentsForChannel(channelKeywords);

		// Create a mapping between the channel object and the position in the listview
		if(mapContentsPositions.size() > 0)
			mapContentsPositions.clear();
		int i = 0;
		for(Content c:contents)
		{
			mapContentsPositions.put(i, c);
			i++;
		}
		
		ListAdapter adapter = new ContentAdapter(this, contents,
				R.layout.contentresume, new String[] {
				Content.CONTENT_CHANNEL_IMAGE,  Content.CONTENT_DESCRIPTION ,  Content.CONTENT_AGE},  
				new int[] {R.id.contentChannelImg, R.id.contentDescription, R.id.contentAge});

		contentsListView.setAdapter(adapter);

		//on met un écouteur d'évènement sur notre listView
		contentsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				//on récupère la HashMap contenant les infos de notre item (titre, description, img)
				//HashMap<String, String> map = (HashMap<String, String>) contentsListView.getItemAtPosition(position);
				Content selectedC = mapContentsPositions.get(position);
				OpenContent(selectedC);
				
			}
		});
		
	}

	
	private void OpenContent(Content c)
	{
		String ext = c.get(Content.CONTENT_TYPE);
		if(ext.compareTo(".jpg") == 0)
		{
			File image2see = new File(c.get(Content.CONTENT_BINARY_DATAPATH));
			Intent jpg = new Intent();
			jpg.setAction(android.content.Intent.ACTION_VIEW);
			jpg.setDataAndType(Uri.fromFile(image2see), "image/jpg");
			getParent().startActivity(jpg);
	
		}else if(ext.compareTo(".png") == 0)
		{
			File image2see = new File(c.get(Content.CONTENT_BINARY_DATAPATH));
			Intent png = new Intent();
			png.setAction(android.content.Intent.ACTION_VIEW);
			png.setDataAndType(Uri.fromFile(image2see), "image/png");
			getParent().startActivity(png);
			
		}else if(ext.compareTo(".txt") == 0)
		{
			File text2read = new File(c.get(Content.CONTENT_BINARY_DATAPATH));
			Intent txt = new Intent();
			txt.setAction(android.content.Intent.ACTION_VIEW);
			txt.setDataAndType(Uri.fromFile(text2read), "text/plain");
			getParent().startActivity(txt);
		}else if(ext.compareTo(".mp3") == 0)
		{
			File musicFile2Play = new File(c.get(Content.CONTENT_BINARY_DATAPATH));
			Intent mp3 = new Intent();
			mp3.setAction(android.content.Intent.ACTION_VIEW);
			mp3.setDataAndType(Uri.fromFile(musicFile2Play), "audio/mp3");
			getParent().startActivity(mp3);
		}
		
	}
	
	private void UpdateListAvailableContents()
	{
		
		// Create our own version of the list adapter
		List<Content> contents = DatabaseHelper.getDBManager().getContentsForChannel(channelKeywords);
		if(contents.size() == 0)
			finish();
		ListView contentsListView = (ListView) findViewById(R.id.contentsList);
		
		// Create a mapping between the channel object and the position in the listview
		if(mapContentsPositions.size() > 0)
			mapContentsPositions.clear();
		int i = 0;
		for(Content c:contents)
		{
			mapContentsPositions.put(i, c);
			i++;
		}

		ListAdapter adapter = new ContentAdapter(this, contents, R.layout.contentresume, new String[] {
				Content.CONTENT_CHANNEL_IMAGE,  Content.CONTENT_DESCRIPTION ,  Content.CONTENT_AGE},  
				new int[] {R.id.contentChannelImg, R.id.contentDescription, R.id.contentAge});

		contentsListView.setAdapter(adapter);
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		UpdateListAvailableContents();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.contentscontextmenu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) 
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		// Retriving the selected channel
		Content selectedContent = mapContentsPositions.get((int)info.id);

		switch (item.getItemId()) {
		case R.id.openContent:
			// Updating the status of the channel within the database
			//DatabaseHelper.getDBManager().deleteContent(selectedContent.get(Content.CONTENT_DESCRIPTION));
			// Updates the list of channels that appear within the listview
			UpdateListAvailableContents();
			return true;
		case R.id.deleteContent:
			// Deleting the selected channel
			DatabaseHelper.getDBManager().deleteContent(selectedContent.get(Content.CONTENT_DESCRIPTION));			
			UpdateListAvailableContents();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

}
