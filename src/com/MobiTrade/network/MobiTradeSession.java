package com.MobiTrade.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.MobiTrade.ConfigTab;
import com.MobiTrade.DashboardTab;
import com.MobiTrade.MobiTradeMain;
import com.MobiTrade.objectmodel.Channel;
import com.MobiTrade.objectmodel.Content;
import com.MobiTrade.objectmodel.ContentAge;
import com.MobiTrade.sqlite.DatabaseHelper;

public class MobiTradeSession
{
	public static String TAG = "MobiTrade";

	// Constants that indicate the current connection state
	private ConnectToMobiTradeDevice connectThread = null;
	private ExchangeWithMobiTradeDevice exchangeThread = null;

	// The list of received contents from the remote peer
	private List<Content> listReceivedContents = new ArrayList<Content>();
	// The list of new received channels from the remote peer
	private List<Channel> listReceivedChannels = new ArrayList<Channel>();
	// Map that maintains the requested channels and their matching contents per peer
	private HashMap<String, List<String>> mapMatchingContents = new HashMap<String,List<String>>();
	// Map that maintains the number of bytes sent by channel
	private HashMap<String, Long> mapSentBytes = new HashMap<String,Long>();

	private boolean exceptionOccured = false;

	// TIME window over which we average our measures
	private static long TIME_WINDOW  = 86400;
	private static float WEIGHT  = (float) 0.1;
	private static long BETA = ConfigTab.megabyte;
	private static long ALPHA = ConfigTab.ko;

	private long lastInteraction = 0;
	private BluetoothDevice remoteDev = null;

	private MobiTradeProtocol mobitradeProtocol = null;
	private MobiTradeService mainActivity = null;


	public MobiTradeSession(MobiTradeProtocol protocol, MobiTradeService main, BluetoothDevice peer)
	{
		remoteDev = peer;
		lastInteraction = System.currentTimeMillis()/1000;
		mobitradeProtocol = protocol;
		mainActivity = main;
		exceptionOccured = false;
	}

	public synchronized void setExceptionOccured()
	{
		exceptionOccured = true;
	}

	public synchronized boolean isExceptionOccured()
	{
		return exceptionOccured;
	}

	public synchronized BluetoothDevice getRemoteDev()
	{
		return remoteDev;
	}
	public synchronized void updateLastInteraction()
	{
		mobitradeProtocol.setAlreadySeen(remoteDev);
		lastInteraction = System.currentTimeMillis()/1000;
	}

	public synchronized long getLastInteraction()
	{
		return lastInteraction;
	}

	public synchronized void addMatchingContents(HashMap<String, List<String>> tmpMap)
	{
		mapMatchingContents.putAll(tmpMap);

		//  Initializing the map that will track the number of sent bytes
		Iterator<?> i = mapMatchingContents.entrySet().iterator();
		while (i.hasNext()) 
		{
			HashMap.Entry<String, List<String>> e = (HashMap.Entry<String, List<String>>)i.next();

			// Set the number of matching contents to the received channel as well as its future size in bytes 
			for(Channel ch:listReceivedChannels)
			{
				if(ch.get(Channel.CHANNEL_KEYWORDS).compareTo(e.getKey()) == 0)
				{
					List<String> listMatchingContents = e.getValue();

					// Update the channel local number of contents
					ch.updateNumberOfContents(e.getValue().size());

					if(listMatchingContents.size() > 0)
					{
						long totalSize = 0;
						for(String c: e.getValue())
						{
							totalSize += DatabaseHelper.getDBManager().getContentByName(c).getSize();
						}

						// Update the channel local size bytes
						ch.updateChannelSize(totalSize);
					}

					break;
				}
			}
		}
	}

	public synchronized void addANewReceivedContent(Content c)
	{
		listReceivedContents.add(c);
	}

	public synchronized void addANewReceivedChannel(List<Channel> ch)
	{
		listReceivedChannels.addAll(ch);

		// Initializing the map of sent bytes
		for(Channel channel: ch)
		{
			mapSentBytes.put(channel.get(Channel.CHANNEL_KEYWORDS), (long)0);
		}

	}

	private synchronized void storeAndClearNewReceivedChannels()
	{
		// Update the utilities of the local foreign channels
		updateStoredForeignChannelsUtilities();

		// Update the utilities of the non existent new received channels and add them to the local store
		for(Channel ch:listReceivedChannels)
		{
			String keywords = ch.get(Channel.CHANNEL_KEYWORDS);
			if(!DatabaseHelper.getDBManager().isLocalRequestedChannel(keywords))
			{
				Log.i(TAG, "mapSentBytes size: "+mapSentBytes.size()+" looking for key: "+ keywords);

				long sentBytes = mapSentBytes.get(keywords);

				float nu = 0;

				if(sentBytes < BETA)
				{
					nu = (1 - WEIGHT) * Math.max(ALPHA, 2 * sentBytes);

				}else
				{
					nu = sentBytes + ALPHA;
				}

				ch.updateUtility(nu);
				// Adding the new channel to the local store
				DatabaseHelper.getDBManager().insertChannel(ch);

				mapSentBytes.remove(keywords);

				// Send a broadcast intent so, that the dashboard updates its statistics
				Intent i = new Intent(DashboardTab.MOBITRADE_NEW_CHANNEL_RECEIVED);
				Bundle b = new Bundle();
				b.putString(Channel.CHANNEL_KEYWORDS, keywords);
				i.putExtras(b);
				mainActivity.sendBroadcast(i);

			}else
			{
				// The received channel matches a locally requested one
				mapSentBytes.remove(keywords);
			}
		}

		// Clear the list of received channels
		listReceivedChannels.clear();

		mobitradeProtocol.dispatchAvailableStorage();

		// Each time some new Interests are added, we update the whole channels storage share

	}

	private synchronized void updateStoredForeignChannelsUtilities()
	{
		if(listReceivedChannels.size() > 0)
		{
			// Get the locally stored foreign channels
			List<Channel> localForeignChannels = DatabaseHelper.getDBManager().getAllForeignChannels();

			if(localForeignChannels.size() > 0)
			{
				for(Channel ch:localForeignChannels)
				{
					boolean found = false;
					Channel selectedChannel = null;
					for(Channel newCh:listReceivedChannels)
					{
						if(ch.get(Channel.CHANNEL_KEYWORDS).compareTo(newCh.get(Channel.CHANNEL_KEYWORDS)) == 0)
						{
							// The same channel is found locally
							found = true;
							selectedChannel = newCh;
							break;
						}
					}

					float lu = ch.getUtility();
					float nu = 0 ;
					String channelKeyWords = ch.get(Channel.CHANNEL_KEYWORDS);
					if(found)
					{
						// We update the utility of the already existing foreign channel and we romoved fromm the
						// list of received channels so, it is not considered to be added later as a new foreign channel

						long sentBytes = mapSentBytes.get(channelKeyWords);

						// Clear the considered channel
						mapSentBytes.remove(channelKeyWords);
						listReceivedChannels.remove(selectedChannel);

						if(sentBytes < BETA)
						{
							nu = WEIGHT*lu + (1 - WEIGHT)*Math.max(ALPHA, 2*sentBytes);

						}else
						{
							nu = WEIGHT*lu + (1 - WEIGHT)*(ALPHA + sentBytes);
						}
					}else
					{
						// Update the foreign channel utility as being not requested by the remote peer
						nu = BETA*lu ;
					}

					// Update the utility of the stored foreign channel
					DatabaseHelper.getDBManager().updateChannelUtility(ch.get(Channel.CHANNEL_KEYWORDS), nu);

				}
			}

		}
	}

	private synchronized void storeAndClearReceivedContents()
	{
		if(listReceivedContents.size() > 0)
		{
			for(Content c:listReceivedContents)
			{
				if(!mobitradeProtocol.isLocalStorgeFull())
				{
					DatabaseHelper.getDBManager().insertContent(c);

					Intent i = new Intent(DashboardTab.MOBITRADE_NEW_CONTENT_RECEIVED);
					Bundle b = new Bundle();
					b.putString(Content.CONTENT_NAME, c.get(Content.CONTENT_NAME));
					i.putExtras(b);
					mainActivity.sendBroadcast(i);

				}else
				{
					// Free some space towards adding the new received content
					while(mobitradeProtocol.isLocalStorgeFull())
					{
						Channel chr = mobitradeProtocol.getChannelThatExceedsTheMostItsShare();
						Content toDelete = mobitradeProtocol.getOldestContentForChannel(chr.get(Channel.CHANNEL_KEYWORDS));
						DatabaseHelper.getDBManager().deleteContent(toDelete.get(Content.CONTENT_DESCRIPTION));
					}

					// Add the new content
					DatabaseHelper.getDBManager().insertContent(c);

					Intent i = new Intent(DashboardTab.MOBITRADE_NEW_CONTENT_RECEIVED);
					Bundle b = new Bundle();
					b.putString(Content.CONTENT_NAME, c.get(Content.CONTENT_NAME));
					i.putExtras(b);
					mainActivity.sendBroadcast(i);
				}
			}

			listReceivedContents.clear();
		}
	}

	public synchronized void cancel()
	{
		Log.i(TAG, "Clearing session.");
		
		cancelThreads();

		if(DatabaseHelper.getDBManager().getConfigServiceStatus() == 1)
		{
			storeAndClearNewReceivedChannels();
			storeAndClearReceivedContents();
		}

		mapMatchingContents.clear();
		listReceivedChannels.clear();
		listReceivedContents.clear();
		mapSentBytes.clear();

	}



	public synchronized void setConnectThread(ConnectToMobiTradeDevice th)
	{
		connectThread = th;
	}

	public synchronized void setExchangeThread(ExchangeWithMobiTradeDevice th)
	{
		exchangeThread = th;
	}

	public synchronized ConnectToMobiTradeDevice getConnectThread()
	{
		return connectThread;
	}

	public synchronized ExchangeWithMobiTradeDevice getExchangeThread()
	{
		return exchangeThread;
	}

	private synchronized void cancelThreads()
	{
		if(exchangeThread != null && exchangeThread.isAlive())
		{
			exchangeThread.cancel();
		}

		if(connectThread != null && connectThread.isAlive())
		{
			connectThread.cancel();
		}

	}

	private synchronized void addToSentBytes(String keywords, long bytes)
	{
		if(mapSentBytes.containsKey(keywords))
		{
			long old = mapSentBytes.get(keywords);
			mapSentBytes.put(keywords, old+bytes);
		}else
		{
			Log.e(TAG, "Entry not found within mapSentBytes: "+keywords);
		}
	}

	public synchronized Content GetNextContentToForward()
	{
		Log.i(TAG, "Looking for the next content to forward.");

		Iterator<?> i = mapMatchingContents.entrySet().iterator();
		List<String> toRemove = new ArrayList<String>();
		while (i.hasNext()) 
		{
			HashMap.Entry<String, List<String>> e = (HashMap.Entry<String, List<String>>)i.next();
			List<String> contents = (List<String>)e.getValue();
			if(contents.size() > 0)
			{
				String cName = contents.get(0);
				contents.remove(0);

				Content c = DatabaseHelper.getDBManager().getContentByName(cName);

				// Updates the number of bytes sent throught the corresponding channel
				addToSentBytes(e.getKey(), c.getSize());


				return c;
			}else
			{
				toRemove.add((String)e.getKey());
			}
		}

		for(String c:toRemove)
		{
			mapMatchingContents.remove(c);
		}

		return null;
	}

}
