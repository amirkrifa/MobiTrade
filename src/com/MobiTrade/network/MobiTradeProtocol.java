package com.MobiTrade.network;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.MobiTrade.ConfigTab;
import com.MobiTrade.DashboardTab;
import com.MobiTrade.objectmodel.Channel;
import com.MobiTrade.objectmodel.Content;
import com.MobiTrade.objectmodel.ContentAge;
import com.MobiTrade.sqlite.DatabaseHelper;

public class MobiTradeProtocol {


	public static String TAG = "MobiTrade";

	private static final int maxAllowedParallelSessions = 1;
	// in second
	private static final long SESSION_TIME_OUT = 20; 
	private static final long SESSION_CLEANING_INTERVAL = 5; 
	private static final long MAX_TIME_TO_NEXT_MEETING = 30;

	public static final long INITIAL_LOCAL_CHANNEL_UTILITY = 1*ConfigTab.megabyte;

	// Index used to create tmpFiles
	private static int tmpIndex = 0;

	private AcceptBluetoothConnection listenThread = null;

	// The list of current active sessions
	private HashMap<String, MobiTradeSession> currentSessions = new HashMap<String, MobiTradeSession>();

	// Already encountered nodes and last meeting time in s
	private HashMap<String, Long> alreadySeen = new HashMap<String, Long>();

	// map of the new discovered nodes
	private HashMap<String, BluetoothDevice> mapDiscoveredNodes = new HashMap<String, BluetoothDevice>();

	private int INTIAL_NBR_CONTENTS_TO_FORWARD = 2;
	private Timer sessionsTimeOut;

	// Reference to the MobiTrade main activity
	private MobiTradeService service = null;

	// Used for the singleton design pattern
	private static MobiTradeProtocol mobitradeProtocol = null;



	private MobiTradeProtocol(MobiTradeService main)
	{
		service = main;
	}


	// Init the mobitrade protocal unique instance
	public static void InitMobiTradeProtocol(MobiTradeService main)
	{
		if(mobitradeProtocol == null)
		{
			mobitradeProtocol = new MobiTradeProtocol(main);
			mobitradeProtocol.dispatchAvailableStorage();
		}
	}

	// Return the unique MobiTrade protocol instance
	public static MobiTradeProtocol GetMobiTradeProtocol()
	{
		return mobitradeProtocol;
	}

	// Starts the timer which will control the sessions time out
	public void StartSessionsTimer()
	{
		// The sessions timeout timer
		sessionsTimeOut = new Timer();
		sessionsTimeOut.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}

		}, 0, SESSION_CLEANING_INTERVAL*1000);

	}

	// Cancels the sessiosn timeout timer
	public void CancelSessionsTimer()
	{
		sessionsTimeOut.cancel();
	}

	// Return the number of running sessions
	public synchronized int GetNumberOfSessions()
	{
		return currentSessions.size();
	}


	// Start the asynchronous bluetooth discovery
	public void StartDiscovery()
	{
         
		if(!BluetoothAdapter.getDefaultAdapter().isDiscovering())
		{
			if(BluetoothAdapter.getDefaultAdapter().startDiscovery())
			{
				
				Log.i(TAG, "Start discovery at: "+DatabaseHelper.getCurrentTime());
				service.sendBroadcast(new Intent(DashboardTab.MOBITRADE_DISCOVERING));

			}else
			{
				Log.i(TAG, "Unable to activate the discovering mode");
			}
		}
	}


	public void DisableBluetoothInterface()
	{
		BluetoothAdapter.getDefaultAdapter().disable();
	}



	private void TimerMethod()
	{
		//This method is called directly by the timer
		//and runs in the same thread as the timer.
		// Go through all the sessions and see which should expire
		Log.i(TAG, "Cleaning Out of Date Sessions, current active sessions: "+Integer.toString(currentSessions.size()));

		Iterator<?> i = currentSessions.entrySet().iterator();
		while (i.hasNext()) 
		{
			HashMap.Entry<String, MobiTradeSession> e = (HashMap.Entry<String, MobiTradeSession>)i.next();
			long diff = System.currentTimeMillis()/1000 - e.getValue().getLastInteraction();
			Log.i(TAG, "Diff: "+Long.toString(diff));
			if(diff > SESSION_TIME_OUT || e.getValue().isExceptionOccured())
			{
				Log.i(TAG, "Session Time Out, removing it");
				// Session should be closed
				removeSession(e.getValue().getRemoteDev());
			}
			e = null;
		}

	}



	// Returns the next device to which to connect and remove it from the map
	public BluetoothDevice getNextDevice()
	{
		if(mapDiscoveredNodes.size() == 0)
			return null;

		String nextAdr = null;
		BluetoothDevice nextDev = null;
		for(HashMap.Entry<String, BluetoothDevice> entry: mapDiscoveredNodes.entrySet())
		{
			nextAdr = entry.getKey();
			nextDev = entry.getValue();
			break;
		}

		mapDiscoveredNodes.remove(nextAdr);
		return nextDev;
	}

	public synchronized void AddNewDiscoveredDevice(BluetoothDevice device)
	{
		if(!mapDiscoveredNodes.containsKey(device.getAddress()))
			mapDiscoveredNodes.put(device.getAddress(), device);
	}

	public int GetNumberOfDiscoveredDevices()
	{
		return mapDiscoveredNodes.size();
	}

	// Starts the bluetooth listening thread
	public void StartMobiTradeBluetoothListener() {
		if(listenThread == null || listenThread.isRunning() == 0)
		{		
			listenThread = new AcceptBluetoothConnection(this);
			listenThread.start();
		}
	}

	// Stops the MobiTrade Bluetooth listener
	public void StopMobiTradeBluetoothListener() {
		if (listenThread != null) 
		{
			listenThread.cancel();
			listenThread = null;
		}
	}


	public synchronized  void setAlreadySeen(BluetoothDevice device)
	{
		if(!IsAlreadySeen(device))
			alreadySeen.put(device.getAddress(), new Long(System.currentTimeMillis()/1000));
	}


	private  boolean IsAlreadySeen(BluetoothDevice device)
	{
		return alreadySeen.containsKey(device.getAddress());
	}

	private boolean isTimeToStartNewSession(BluetoothDevice device)
	{
		if(IsAlreadySeen(device))
		{
			long diff = System.currentTimeMillis()/1000 - alreadySeen.get(device.getAddress());
			if(diff > MAX_TIME_TO_NEXT_MEETING)
			{
				alreadySeen.put(device.getAddress(), System.currentTimeMillis()/1000);
				Log.i(TAG, "It is time to start a new session with: "+device.getName());
				return true;
			}else 
			{
				Log.i(TAG, "Still early to start a new session with: "+device.getName()+ " lets wait.");
				return false;
			}

		}else
		{
			setAlreadySeen(device);
			return true;
		}
	}

	// Starts a MobiTrade negociation with the new discovered device
	public void StartMobiTradeNegociationWith(BluetoothDevice device) 
	{
		if(device != null )
		{
			if(!isSessionActive(device) )
			{
				String adr = device.getAddress();
				String localDeviceAddress = BluetoothAdapter.getDefaultAdapter().getAddress();

				if(isTimeToStartNewSession(device))
				{
					if(adr != null)
					{
						// The device who has lexicographically higher address start the
						// session
						if (localDeviceAddress.compareTo(adr) > 0) 
						{
							// Starting a new connection
							startNewSession(device);

							// Connecting to the new discovered device
							currentSessions.get(adr).setConnectThread(new ConnectToMobiTradeDevice(this, device));
							currentSessions.get(adr).getConnectThread().start();

						}else
						{
							BluetoothDevice d = getNextDevice();
							if(d != null)
							{
								StartMobiTradeNegociationWith(d);
								d = null;
							}else
							{
								if(DatabaseHelper.getDBManager().getConfigAlwaysDiscovering() == 1)
									StartDiscovery();
							}
						}
					}
				}else
				{
					BluetoothDevice d = getNextDevice();
					if(d != null)
					{
						StartMobiTradeNegociationWith(d);
						d = null;
					}else
					{
						if(DatabaseHelper.getDBManager().getConfigAlwaysDiscovering() == 1)
							StartDiscovery();
					}
				}
			}else
			{
				Log.i(TAG, "we already have a running session with device: "+device.getName());
			}

		}
	}

	public  void StopOngoingSessions()
	{
		Log.i(TAG, "Clearing all active sessions.");
		if(currentSessions.size() > 0)
		{
			Iterator<?> i = currentSessions.entrySet().iterator();
			while (i.hasNext()) 
			{
				HashMap.Entry<String, MobiTradeSession> e = (HashMap.Entry<String, MobiTradeSession>)i.next();
				MobiTradeSession s = (MobiTradeSession)e.getValue();
				s.cancel();
			}

			currentSessions.clear();
			// Clearing related maps
			alreadySeen.clear();
			mapDiscoveredNodes.clear();

		}else
		{
			Log.i(TAG, "There is no active sessions to close.");
		}
	}

	public synchronized void setExceptionOccuredOnSession(BluetoothDevice session) {

		if (isSessionActive(session))
		{
			currentSessions.get(session.getAddress()).setExceptionOccured();
		}
	}

	public synchronized void removeSession(BluetoothDevice session) {

		if (isSessionActive(session))
		{
			String adr = session.getAddress();
			MobiTradeSession ms = currentSessions.get(adr);
			Log.i(TAG, "Ending session with: "+session.getName());

			setAlreadySeen(session);

			ms.cancel();

			ms = null;

			currentSessions.remove(adr);

			Intent ios = new Intent(DashboardTab.MOBITRADE_SESSION_CLOSED);
			Bundle bos = new Bundle();
			bos.putString("NAME", session.getName());
			ios.putExtras(bos);
			service.sendBroadcast(ios);

			service.sendBroadcast(new Intent(DashboardTab.MOBITRADE_UPDATE_STATISTICS));

			// Re start the discovery once the session is finished
			if(currentSessions.size() == 0 && mapDiscoveredNodes.size() == 0)
			{
				// Re Start the discovery
				if(DatabaseHelper.getDBManager().getConfigAlwaysDiscovering() == 1)
				{
					StartDiscovery();
					Log.i(TAG, "Re Starting discovery at : "+DatabaseHelper.getCurrentTime());
				}
			}else if(currentSessions.size() == 0 && mapDiscoveredNodes.size() > 0)
			{
				StartMobiTradeNegociationWith(getNextDevice());
				Log.i(TAG, "Start negociation with new device");

			}
		}
	}

	public synchronized void startNewSession(BluetoothDevice session) {

		if (!isSessionActive(session))
		{
			if(BluetoothAdapter.getDefaultAdapter().isDiscovering())
				BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

			String adr = session.getAddress();
			Log.i(TAG, "Starting new session with: "+session.getName());
			currentSessions.put(adr, new MobiTradeSession(this, service, session));

			Intent ins = new Intent(DashboardTab.MOBITRADE_NEW_PEER_DISCOVERED);
			Bundle bns = new Bundle();
			bns.putString("NAME", session.getName());
			ins.putExtras(bns);
			service.sendBroadcast(ins);

		}
	}

	public synchronized  boolean isSessionActive(BluetoothDevice session) {
		return currentSessions.containsKey(session.getAddress());
	}


	private  void UpdateListOfReceivedChannelsAndMatchingContents(BluetoothDevice session, List<Channel> listRecvChannels) {

		if(isSessionActive(session))
		{
			// Create the list of Contents that match the list of received channels
			HashMap<String, List<String>> tmpMap = new HashMap<String, List<String>>();
			List<Channel> newReceivedChannels = new ArrayList<Channel>();
			for (Channel ch : listRecvChannels) 
			{
				if(ch != null){
					if(DatabaseHelper.getDBManager().isChannelAvailable(ch.get(Channel.CHANNEL_KEYWORDS)))
					{
						List<Content> listHeHas = ch.getAvailableContents(); 
						List<String> listMatchingContents = new ArrayList<String>();
						for(Content cc:DatabaseHelper.getDBManager().getContentsForChannel(ch.get(Channel.CHANNEL_KEYWORDS)))
						{
							boolean available = false;
							for(Content hc:listHeHas)
							{	
								// If the same name and description
								if(hc.get(Content.CONTENT_NAME).compareTo(cc.get(Content.CONTENT_NAME)) == 0)
								{
									available = true;
									break;
								}
							}
							if(!available)
							{
								// If he don't already have the content then, we send it to him
								listMatchingContents.add(cc.get(Content.CONTENT_NAME));
							}
						}

						tmpMap.put(ch.get(Channel.CHANNEL_KEYWORDS), listMatchingContents);

					}else
					{
						newReceivedChannels.add(ch);
					}
				}
			}

			MobiTradeSession ms = currentSessions.get(session.getAddress());
			// The channel is not availavble, consider it to be added once the session is closed
			ms.addANewReceivedChannel(newReceivedChannels);
			ms.addMatchingContents(tmpMap);

		}
	}



	private  List<Content> GetInitialSetOfContents(BluetoothDevice session)
	{
		MobiTradeSession ms = currentSessions.get(session.getAddress());
		List<Content> tmp = new ArrayList<Content>();

		if(ms != null)
		{
			for(int i =0; i < INTIAL_NBR_CONTENTS_TO_FORWARD; i++)
			{
				Content c = ms.GetNextContentToForward();
				if(c != null)
				{	
					tmp.add(c);
				}
			}
		}

		return tmp;
	}

	public String SerializeContentToXMLFile(Content c) {

		StringBuilder tmpFileName = new StringBuilder();
		tmpFileName.append("tmpSerializer");
		tmpFileName.append(tmpIndex);
		tmpFileName.append(".tmp");
		tmpIndex ++;
		FileOutputStream tmpFileOutputStream = null;

		File tmpFile = new File(DatabaseHelper.mobiTradeSdcardPath, tmpFileName.toString());
		try {
			tmpFileOutputStream = new FileOutputStream(tmpFile);
			tmpFileOutputStream.write(new String("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>").getBytes());

			tmpFileOutputStream.write(new String("<content>").getBytes());

			tmpFileOutputStream.write(new String("<c_name>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_NAME).getBytes());
			tmpFileOutputStream.write(new String("</c_name>").getBytes());

			tmpFileOutputStream.write(new String("<c_type>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_TYPE).getBytes());
			tmpFileOutputStream.write(new String("</c_type>").getBytes());

			tmpFileOutputStream.write(new String("<c_size>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_SIZE_BYTE).getBytes());
			tmpFileOutputStream.write(new String("</c_size>").getBytes());

			tmpFileOutputStream.write(new String("<c_message>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_MESSAGE).getBytes());
			tmpFileOutputStream.write(new String("</c_message>").getBytes());

			tmpFileOutputStream.write(new String("<c_description>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_DESCRIPTION).getBytes());
			tmpFileOutputStream.write(new String("</c_description>").getBytes());

			tmpFileOutputStream.write(new String("<c_utility>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_UTILITY).getBytes());
			tmpFileOutputStream.write(new String("</c_utility>").getBytes());

			tmpFileOutputStream.write(new String("<c_source_id>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_SOURCE_ID).getBytes());
			tmpFileOutputStream.write(new String("</c_source_id>").getBytes());

			tmpFileOutputStream.write(new String("<c_expiration_date>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_EXPIRATION_DATE).getBytes());
			tmpFileOutputStream.write(new String("</c_expiration_date>").getBytes());

			ContentAge ca = DatabaseHelper.getDBManager().updateContentAge(c.get(Content.CONTENT_DESCRIPTION));

			if(ca != null)
			{
				tmpFileOutputStream.write(new String("<c_age_days>").getBytes());
				tmpFileOutputStream.write(Integer.toString(ca.c_age_days).getBytes());
				tmpFileOutputStream.write(new String("</c_age_days>").getBytes());

				tmpFileOutputStream.write(new String("<c_age_hours>").getBytes());
				tmpFileOutputStream.write(Integer.toString(ca.c_age_hours).getBytes());
				tmpFileOutputStream.write(new String("</c_age_hours>").getBytes());

				tmpFileOutputStream.write(new String("<c_age_minutes>").getBytes());
				tmpFileOutputStream.write(Integer.toString(ca.c_age_minutes).getBytes());
				tmpFileOutputStream.write(new String("</c_age_minutes>").getBytes());
			}

			tmpFileOutputStream.write(new String("<c_channel>").getBytes());
			tmpFileOutputStream.write(c.get(Content.CONTENT_CHANNEL).getBytes());
			tmpFileOutputStream.write(new String("</c_channel>").getBytes());

			tmpFileOutputStream.write(new String("<c_binary_data>").getBytes());


			DatabaseHelper.AppendBinaryDataFromPathToFile(c.get(Content.CONTENT_BINARY_DATAPATH), c.get(Content.CONTENT_TYPE),tmpFileOutputStream);

			tmpFileOutputStream.write(new String("</c_binary_data>").getBytes());

			tmpFileOutputStream.write(new String("</content>").getBytes());
			tmpFileOutputStream.close();

			tmpFileOutputStream.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Error occured while creating tmp content xml msg : "+e);
			e.printStackTrace();

		}

		return tmpFile.getAbsolutePath();
	}




	public String GetListOfChannelsToSendXMLFile() {

		dispatchAvailableStorage();

		StringBuilder tmpFileName = new StringBuilder();
		tmpFileName.append("tmpChannelsSerializer");
		tmpFileName.append(tmpIndex);
		tmpFileName.append(".tmp");
		tmpIndex ++;
		FileOutputStream tmpFileOutputStream = null;

		File tmpFile = new File(DatabaseHelper.mobiTradeSdcardPath, tmpFileName.toString());
		try {
			tmpFileOutputStream = new FileOutputStream(tmpFile);
			tmpFileOutputStream.write(new String("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>").getBytes());

			List<Channel> availableChannels = DatabaseHelper.getDBManager().getAllChannels();

			tmpFileOutputStream.write(new String("<channels number=\""+Integer.toString(availableChannels.size())+"\">").getBytes());

			for (Channel ch : availableChannels) {

				tmpFileOutputStream.write(new String("<channel>").getBytes());

				tmpFileOutputStream.write(new String("<ch_keywords>").getBytes());
				tmpFileOutputStream.write(ch.get(Channel.CHANNEL_KEYWORDS).getBytes());
				tmpFileOutputStream.write(new String("</ch_keywords>").getBytes());

				tmpFileOutputStream.write(new String("<ch_utility>").getBytes());
				tmpFileOutputStream.write(ch.get(Channel.CHANNEL_UTILITY).getBytes());
				tmpFileOutputStream.write(new String("</ch_utility>").getBytes());

				tmpFileOutputStream.write(new String("<ch_creation_date>").getBytes());
				tmpFileOutputStream.write(ch.get(Channel.CHANNEL_CREATION_DATE).getBytes());
				tmpFileOutputStream.write(new String("</ch_creation_date>").getBytes());

				List<Content> lc = DatabaseHelper.getDBManager().getContentsForChannel(ch.get(Channel.CHANNEL_KEYWORDS));

				if(lc.size() > 0)
				{

					// Add the list of contents so the other peer don't send me already available contents
					for(Content cc:lc)
					{
						tmpFileOutputStream.write(new String("<have_content>").getBytes());
						tmpFileOutputStream.write(cc.get(Content.CONTENT_NAME).getBytes());
						tmpFileOutputStream.write(new String("</have_content>").getBytes());
					}
				}
				lc.clear();

				tmpFileOutputStream.write(new String("</channel>").getBytes());
			}

			tmpFileOutputStream.write(new String("</channels>").getBytes());

			availableChannels.clear();

			tmpFileOutputStream.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Error occured while creating tmp content xml msg : "+e);
			e.printStackTrace();

		}

		return tmpFile.getAbsolutePath();
	}

	public byte[] GetListOfChannelsToSendXML() {

		ByteArrayOutputStream stringos = new ByteArrayOutputStream();

		try {
			stringos.write(new String("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>").getBytes());

			List<Channel> availableChannels = DatabaseHelper.getDBManager().getAllChannels();

			stringos.write(new String("<channels number=\""+Integer.toString(availableChannels.size())+"\">").getBytes());

			for (Channel ch : availableChannels) {

				stringos.write(new String("<channel>").getBytes());

				stringos.write(new String("<ch_keywords>").getBytes());
				stringos.write(ch.get(Channel.CHANNEL_KEYWORDS).getBytes());
				stringos.write(new String("</ch_keywords>").getBytes());

				stringos.write(new String("<ch_utility>").getBytes());
				stringos.write(ch.get(Channel.CHANNEL_UTILITY).getBytes());
				stringos.write(new String("</ch_utility>").getBytes());

				stringos.write(new String("<ch_creation_date>").getBytes());
				stringos.write(ch.get(Channel.CHANNEL_CREATION_DATE).getBytes());
				stringos.write(new String("</ch_creation_date>").getBytes());

				List<Content> lc = DatabaseHelper.getDBManager().getContentsForChannel(ch.get(Channel.CHANNEL_KEYWORDS));

				if(lc.size() > 0)
				{

					// Add the list of contents so the other peer don't send me already available contents
					for(Content c:lc)
					{
						stringos.write(new String("<have_content>").getBytes());
						stringos.write(c.get(Content.CONTENT_NAME).getBytes());
						stringos.write(new String("</have_content>").getBytes());
					}
				}
				lc.clear();

				stringos.write(new String("</channel>").getBytes());
			}

			stringos.write(new String("</channels>").getBytes());
			stringos.close();

			availableChannels.clear();

		} catch (Exception e) {
			Log.e(TAG,
					"error occurred while creating xml list of channels: "+e);
			return null;
		}
		return stringos.toByteArray();
	}

	
	// Should be synchronized with sessions cancellation
	public synchronized  void ParseXmlMobiTradeMessage(ExchangeWithMobiTradeDevice exch, String filePath)
	{
		try 
		{
			String remoteDeviceAddress = exch.getSocket().getRemoteDevice().getAddress();
			BluetoothDevice remoteDev = exch.getSocket().getRemoteDevice();
			currentSessions.get(remoteDeviceAddress).updateLastInteraction();

			// create the reader (scanner)
			XMLReader xmlreader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			// instantiate our handler
			MobiTradeXmlHandler msgHandler = new MobiTradeXmlHandler();

			// assign our handler
			xmlreader.setContentHandler(msgHandler);
			// perform the synchronous parse
			FileInputStream in = new FileInputStream(filePath);

			xmlreader.parse(new InputSource(in));
			in.close();

			// Deleting the tmpFile
			File tmpFile = new File(filePath);
			if(!tmpFile.delete())
			{
				Log.e(TAG, "Unable to delete the tmpfile: "+filePath);
			}

			if(msgHandler.getType().compareTo("content") == 0 && isSessionActive(remoteDev))
			{
				// Store the received content
				Log.i(TAG, "a new content is received @ "+DatabaseHelper.getCurrentTime());
					
				currentSessions.get(remoteDev.getAddress()).addANewReceivedContent(msgHandler.getReceivedContent());

				MobiTradeSession ms = currentSessions.get(remoteDev.getAddress());

				Content c = ms.GetNextContentToForward();

				if(c != null)
				{	

					Intent ic = new Intent(DashboardTab.MOBITRADE_NEW_CONTENT_SENT);
					Bundle bc = new Bundle();
					bc.putString(Content.CONTENT_NAME, c.get(Content.CONTENT_NAME));
					bc.putString("NAME", remoteDev.getName());

					ic.putExtras(bc);
					service.sendBroadcast(ic);

					//	exch.write(SerializeContentToXML(c).toByteArray());	
					exch.writeFromFile(SerializeContentToXMLFile(c));	

					System.gc();

				}

			}else if(msgHandler.getType().compareTo("channels") == 0 && isSessionActive(remoteDev))
			{
				Log.i(TAG, "list of channels received");

				// Updating the list of received channels
				UpdateListOfReceivedChannelsAndMatchingContents(remoteDev, msgHandler.getListReceivedChannels());	
				msgHandler.clear();

				String localDeviceAddress = BluetoothAdapter.getDefaultAdapter().getAddress();

				if (localDeviceAddress.compareTo(remoteDeviceAddress) < 0) {
					// Sending back the list of local channels before updating them
					exch.writeFromFile(GetListOfChannelsToSendXMLFile());
				}

				// Sending the initial set of contents
				List<Content> initList = GetInitialSetOfContents(remoteDev);
				if(initList.size() > 0)
				{
					for(Content c:initList)
					{
						if(c != null)
						{	
							//	exch.write(SerializeContentToXML(c).toByteArray());
							exch.writeFromFile(SerializeContentToXMLFile(c));
						}
					}
				}
			}
		}
		
		catch (Exception e) {
			Log.e(TAG,
					"Exception occured while trying to parse the received xml message: "
					+ e );
			// Close the exchanging chanel which will remove the session

			e.printStackTrace();
		}
	}


	///////////////////////////////////////// MobiTrade Content storage management /////////////////////////////////////////////////////////////////////

	//  Verifies wether MobiTrade is using all the space allocated to it or not
	public synchronized boolean isLocalStorgeFull()
	{
		// in bytes
		long mobitradeUsedSpace = DatabaseHelper.getDBManager().getMobiTradeAllocatedSpace();
		long mobitradeCapacity = DatabaseHelper.getDBManager().getConfigMaxAllowedSpace() * ConfigTab.megabyte;
		return (mobitradeUsedSpace > mobitradeCapacity); 
	}

	public synchronized Channel getChannelThatExceedsTheMostItsShare()
	{
		Channel sel = null;
		float maxExceed = 0;

		List<Channel> listChannels = DatabaseHelper.getDBManager().getAllChannels();
		for(Channel ch:listChannels)
		{
			long channelSize = DatabaseHelper.getDBManager().getChannelAllocatedSpace(ch.get(Channel.CHANNEL_KEYWORDS));

			if(channelSize > ch.getUtility())
			{
				if(channelSize - ch.getUtility() > maxExceed)
				{
					maxExceed = channelSize - ch.getUtility();
					sel = ch;
				}
			}
		}

		return sel;
	}

	public synchronized Content getOldestContentForChannel(String keywords)
	{
		Content sel = null;
		ContentAge oldestAge = null;

		List<Content> listContents = DatabaseHelper.getDBManager().getContentsForChannel(keywords);
		if(listContents.size() > 0)
		{
			for(Content c:listContents)
			{
				ContentAge ca = DatabaseHelper.getDBManager().updateContentAge(c.get(Content.CONTENT_DESCRIPTION));
				if(ca.isOlderThan(oldestAge))
				{
					oldestAge = ca;
					sel = c;
				}
			}
		}
		return sel;
	}

	// Dispatches the local available storage and updates the channels utilities
	public synchronized void dispatchAvailableStorage()
	{
		Log.i(TAG, "Dispatching available free storage.");

		float sumChannelsUtilities = 0;
		List<Channel> listChannels = DatabaseHelper.getDBManager().getAllChannels();

		if(listChannels.size() > 0)
		{
			for(Channel ch: listChannels)
			{
				sumChannelsUtilities += ch.getUtility();
			}

			float spaceToAdd = 0;
			float maxAllowedSpace = DatabaseHelper.getDBManager().getConfigMaxAllowedSpace()*ConfigTab.megabyte;
			if(sumChannelsUtilities < maxAllowedSpace)
			{

				spaceToAdd = maxAllowedSpace - sumChannelsUtilities;

				float percentage = spaceToAdd / 100;

				//cout<<"space to add: "<<spaceToAdd<<" percentage: "<<percentage <<endl;
				for(Channel ch:listChannels)
				{
					float w = (ch.getUtility()*100)/sumChannelsUtilities;
					float nu = ch.getUtility() + percentage*w;
					DatabaseHelper.getDBManager().updateChannelUtility(ch.get(Channel.CHANNEL_KEYWORDS), nu);
					spaceToAdd -= percentage*w;
				}

				UpdateChannelsAllowedBytes();
			}
		}
	}


	public synchronized void UpdateChannelsAllowedBytes()
	{
		Log.i(TAG, "Updating channel allowed bytes.");
		List<Channel> listChannels = DatabaseHelper.getDBManager().getAllChannels();

		// Calculating the sum of utilities
		float sumFU = 0;
		float sumLU = 0;
		float totalU = 0;

		for(Channel ch:listChannels)
		{
			if(ch.isLocal())
			{
				sumLU += ch.getUtility();
			}else
			{
				sumFU += ch.getUtility();
			}
		}

		totalU = sumFU + sumLU;
		float maxAllowedSpace = DatabaseHelper.getDBManager().getConfigMaxAllowedSpace()*ConfigTab.megabyte;

		// Updating each channel allowed size of bytes
		for(Channel ch:listChannels)
		{
			float res = (ch.getUtility()/totalU)*maxAllowedSpace;
			DatabaseHelper.getDBManager().updateChannelMaxAllowedSize(ch.get(Channel.CHANNEL_KEYWORDS), (int)Math.floor(res));

			if(Math.floor(res - ch.getUtility()) > 0)
			{
				Log.e(TAG, "Invalid Channel max allowed bytes: " + res + " utility: " + ch.getUtility());
			}

		}
	}


	private void cleanUpTheStorage()
	{
		/*	// Cleaning up Channels that exceeds their allowed utilities
		while(mapContents.size() + generatedContents.size() > CONTENTS_STORAGE_CAPACITY)
		{
				Interest * selectedI = GetInterestThatExceedsTheMostItsShare();
	#ifdef DEBUG
				NS_ASSERT(selectedI != NULL);
				//While the occupied space is still greater then the utility
				NS_ASSERT(!selectedI->IsLocal());
	#endif
				while(selectedI->ExceedsAllowedUtilityBytes() && mapContents.size() + generatedContents.size() > CONTENTS_STORAGE_CAPACITY)
				{

					// Get the oldest or random content
					Content * sc = NULL;
	#ifdef ENABLE_TTL_AWAIRNESS
					sc = selectedI->GetOldestMatchingContent();
	#else
					sc = selectedI->GetRandomMatchingContent();
	#endif
					// Deleting the selected Content
					if(sc == NULL)
					{
						cout<<"Interest util: "<<selectedI->GetUtility()<<" allowed storage: "<<selectedI->GetInterestAllowedBytes()<<" occupied: "<<selectedI->GetTotalSizeOfMatchingContents() <<" local: "<<selectedI->IsLocal()<<endl;
						selectedI->ShowMatchingContents(cout);
						ShowInterests(cout);
						NS_FATAL_ERROR("Invalid selected Content");
					}

					sc->SyncWithMatchingInterest();

					bool removed = false;
	#ifdef DEBUG
					NS_ASSERT(sc->IsForeign());
	#endif
					removed = RemoveForeignContent(sc);
	#ifdef DEBUG
					NS_ASSERT(removed == true);
	#endif
					delete sc;
				}
		}
		 */
	}

	public synchronized void SetExchangeThreadForSession(String remoteAdr, ExchangeWithMobiTradeDevice thread)
	{
		currentSessions.get(remoteAdr).setExchangeThread(thread);
	}

	public synchronized void StartExchangeThreadForSession(String remoteAdr)
	{
		currentSessions.get(remoteAdr).getExchangeThread().start();
	}

	public synchronized void WriteListOfChannelsToExchangeThreadForSession(String remoteAdr )
	{
		currentSessions.get(remoteAdr).getExchangeThread().writeFromFile(GetListOfChannelsToSendXMLFile());
	}

	public synchronized boolean AreMoreSessionsAllowed()
	{
		return (currentSessions.size() < maxAllowedParallelSessions);
	}

	public synchronized void UpdateLastInteractionWith(String remoteAdr)
	{
		if(currentSessions.containsKey(remoteAdr))
			currentSessions.get(remoteAdr).updateLastInteraction();
	}


}
