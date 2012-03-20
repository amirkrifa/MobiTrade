package com.MobiTrade.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

import com.MobiTrade.network.ExchangeWithMobiTradeDevice;
import com.MobiTrade.sqlite.DatabaseHelper;

public class Parsing {

	public static String TAG = "MobiTrade";

	private ExchangeWithMobiTradeDevice exch;
	private String remoteAdr;
	// Indicates wether the received record is complete or not
	boolean incompleteRecord  = false;
	// The temporary file name
	StringBuilder tmpFileName;
	FileOutputStream tmpFileOutputStream = null;
	// Index used to create tmpFiles
	private static int tmpIndex = 0;
	private MobiTradeProtocol mobitradeProtocol = null;

	public Parsing(MobiTradeProtocol protocol, ExchangeWithMobiTradeDevice exch) {
		this.exch = exch;
		remoteAdr = exch.getSocket().getRemoteDevice().getAddress();
		mobitradeProtocol = protocol;
		createAndOpenANewTMPFile();
	}

	public void cancel()
	{
		tmpFileName = null;
		
		try {
			tmpFileOutputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mobitradeProtocol = null;
	}
	
	// Used to create a new tmp file where we will store the new received xml message
	private void createAndOpenANewTMPFile()
	{
		tmpFileName = new StringBuilder();
		tmpFileName.append("tmpParser");
		tmpFileName.append(tmpIndex);
		tmpFileName.append(".tmp");
		tmpIndex ++;

		File tmpFile = new File(DatabaseHelper.mobiTradeSdcardPath, tmpFileName.toString());

		try {
			tmpFileOutputStream = new FileOutputStream(tmpFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Error occured while creating tmp recv msg file: "+e);
			e.printStackTrace();
		}
	}

	private void closeCurrentTmpFile()
	{
		try {
			tmpFileOutputStream.flush();
			tmpFileOutputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.i(TAG, "Error occured while trying to close the tmp file: "+tmpFileName.toString()+", "+e);
			e.printStackTrace();
		}
	}

	private String getCurrentTmpFileAbsolutePath()
	{
		return DatabaseHelper.mobiTradeSdcardPath+tmpFileName;
	}

	public void parseMsg(String msg)
	{
		int pos =  0;
		while((pos =  msg.indexOf("<?xml")) > 0)
		{
			String tmp  = msg.substring(0, pos);
			parseXmlFile(tmp);
			msg = msg.substring(pos);
		}

		parseXmlFile(msg);

		mobitradeProtocol.UpdateLastInteractionWith(remoteAdr);
	}


	public void parseXmlFile(String file)
	{
		try {

			if(incompleteRecord)
			{

				// We've started parsing a content, we have to continue
				if(file.indexOf("</content>") == -1)
				{
					// Still incomplete record
					incompleteRecord = true;

					tmpFileOutputStream.write(file.getBytes());

				}else
				{
					// Complete content
					int posEndXML =  file.indexOf("</content>") + (new String("</content>")).length();
					tmpFileOutputStream.write(file.substring(0, posEndXML).getBytes());

					String tmpFN = getCurrentTmpFileAbsolutePath();
					closeCurrentTmpFile();
					createAndOpenANewTMPFile();

					mobitradeProtocol.ParseXmlMobiTradeMessage(exch, tmpFN);

					incompleteRecord = false;

					Log.i(TAG, "The content record is completed.");

				}

			}else
			{
				// We'll start a new record
				if(file.indexOf("<content>") > 0)
				{
					Log.i(TAG, "Start parsing a new content.");
					if(file.indexOf("</content>") == -1)
					{
						// Incomplete content
						incompleteRecord = true;

						tmpFileOutputStream.write(file.getBytes());

						Log.i(TAG, "The received content record is incomplete.");

					}else
					{
						// Complete content
						int posEndXML =  file.indexOf("</content>") + (new String("</content>")).length();

						tmpFileOutputStream.write(file.substring(0, posEndXML).getBytes());

						String tmpFN = getCurrentTmpFileAbsolutePath();

						closeCurrentTmpFile();
						createAndOpenANewTMPFile();

						mobitradeProtocol.ParseXmlMobiTradeMessage(exch, tmpFN);

						incompleteRecord = false;

						Log.i(TAG, "The received content record is complete.");

					}

				}else if(file.indexOf("<channels ") > 0)
				{
					Log.i(TAG, "Start parsing a new channels list");

					if(file.indexOf("</channels>") == -1)
					{
						Log.i(TAG, "The received channels list is incomplete");

						// Incomplete channels list record
						incompleteRecord = true;
						tmpFileOutputStream.write(file.getBytes());

					}else
					{
						// The list of channels is complete, start parsing
						int posEndXML =  file.indexOf("</channels>") + (new String("</channels>")).length();
						tmpFileOutputStream.write(file.substring(0, posEndXML).getBytes());

						String tmpFN = getCurrentTmpFileAbsolutePath();
						closeCurrentTmpFile();
						createAndOpenANewTMPFile();

						mobitradeProtocol.ParseXmlMobiTradeMessage(exch, tmpFN);

						Log.i(TAG, "The received channels list is complete");


					}
				}

			}

		}

		catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Error occured while receiving message: "+e);
			e.printStackTrace();
		}
	}

}
