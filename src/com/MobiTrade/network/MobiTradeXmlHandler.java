package com.MobiTrade.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.MobiTrade.objectmodel.Channel;
import com.MobiTrade.objectmodel.Content;
import com.MobiTrade.sqlite.DatabaseHelper;

import android.util.Log;

public class MobiTradeXmlHandler extends DefaultHandler{

	private File tmpCurrentFile = null;
	private OutputStream outputTmp = null;
	private static int tmpIndex = 0;
	private StringBuilder tmpFileName;

	//private ByteArrayOutputStream currentValue = null;
	private StringBuilder msgType = null;

	// Attributes related to a channel message
	private StringBuilder ch_keywords = null;
	private StringBuilder ch_utility = null;
	private StringBuilder ch_creation_date = null;
	private List<Channel> receivedChannels = new ArrayList<Channel>();

	// Attributes related to a content message
	private StringBuilder c_description = null;
	private StringBuilder c_utility = null;
	private StringBuilder c_source_id = null;
	private StringBuilder c_expiration_date = null;
	private StringBuilder c_message = null;
	private StringBuilder c_channel = null;
	private StringBuilder c_name = null;
	private StringBuilder c_type = null;
	private StringBuilder c_size = null;
	private StringBuilder c_age_days = null;
	private StringBuilder c_age_hours = null;
	private StringBuilder c_age_minutes = null;
	
	private StringBuilder have_content_name = null;

	private List<Content> listAvailableContents = new ArrayList<Content>();

	private String tmpFileContentBinaryData = null;

	Content receivedContent = null;
	private final DatabaseHelper dbManager = DatabaseHelper.getDBManager();

	
	private void createAndOpenANewTMPFile()
	{
		tmpIndex ++;

		tmpFileName = new StringBuilder();
		tmpFileName.append("tmpParsing");
		tmpFileName.append(tmpIndex);
		tmpFileName.append(".tmp");
		
		File tmpFile = new File(DatabaseHelper.mobiTradeSdcardPath, tmpFileName.toString());
		
		try {
			outputTmp = new FileOutputStream(tmpFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e("MobiTrade", "Error occured while creating tmp recv msg file: "+e);
			e.printStackTrace();
		}
	}

	private void openTMPFile()
	{
		tmpFileName = new StringBuilder();
		tmpFileName.append("tmpParsing");
		tmpFileName.append(tmpIndex);
		tmpFileName.append(".tmp");
		
		File tmpFile = new File(DatabaseHelper.mobiTradeSdcardPath, tmpFileName.toString());
		
		try {
			outputTmp = new FileOutputStream(tmpFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e("MobiTrade", "Error occured while creating tmp recv msg file: "+e);
			e.printStackTrace();
		}
	}

	private void reOpenATMPFile()
	{
		File tmpFile = new File(DatabaseHelper.mobiTradeSdcardPath, tmpFileName.toString());
		try {
			tmpFile.createNewFile();
			outputTmp = new FileOutputStream(tmpFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e("MobiTrade", "Error occured while creating tmp recv msg file: "+e);
			e.printStackTrace();
		}
	}

	private void deleteCurrentTmpFile()
	{
		File tmpFile = new File(DatabaseHelper.mobiTradeSdcardPath, tmpFileName.toString());
		if(!tmpFile.delete())
		{
			Log.e("MobiTrade", "Unable to delete tmp file: "+tmpFileName);
		}
	}
	
	private void closeCurrentTmpFile()
	{
		try {
			outputTmp.flush();
			outputTmp.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.i("MobiTrade", "Error occured while trying to close the tmp file: "+tmpFileName.toString()+", "+e);
			e.printStackTrace();
		}
	}
	

	public MobiTradeXmlHandler()
	{
		super();
	}

	// Returns the list of received channels
	public List<Channel> getListReceivedChannels()
	{
		return receivedChannels;
	}
	
	public void clear()
	{
		receivedChannels.clear();
		listAvailableContents.clear();
	}
	
	public Content getReceivedContent()
	{
		return receivedContent;
	}

	public String getType()
	{
		return msgType.toString();
	}

	public int getNumberoOfReceivedChannels()
	{
		return receivedChannels.size();
	}

	@Override
	public void startDocument() throws SAXException {
		
		// Open the tmp file for the first time
		openTMPFile();
	}

	@Override
	public void endDocument() throws SAXException {
	
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {

		try 
		{
			if(qName.equals("channels") || qName.equals("content") || qName.equals("request_end_session")|| qName.equals("end_session"))
			{
				msgType = new StringBuilder();
				if (qName.equals("end_session")) 
				{
					msgType.append("end_session");

				}else if (qName.equals("channels")) 
				{
					msgType.append("channels");

				}else if (qName.equals("content")) 
				{
					msgType.append("content");

				}else if (qName.equals("request_end_session")) 
				{
					msgType.append("request_end_session");
				}
			}else if (qName.equals("channel"))
			{
				listAvailableContents.clear();

			}else if (qName.equals("ch_keywords")) 
			{
				ch_keywords = new StringBuilder();
			}else if (qName.equals("ch_utility")) 
			{
				ch_utility = new StringBuilder();
			}else if (qName.equals("ch_creation_date")) {
				ch_creation_date = new StringBuilder("");
			}else if (qName.equals("c_description")) {
				c_description = new StringBuilder("");
			}else if (qName.equals("c_utility")) {
				c_utility = new StringBuilder("");
			}else if (qName.equals("c_source_id")) {
				c_source_id = new StringBuilder("");
			}else if (qName.equals("c_expiration_date")) {
				c_expiration_date = new StringBuilder("");
			}else if (qName.equals("c_message")) {
				c_message = new StringBuilder("");
			}else if (qName.equals("c_channel")) {
				c_channel = new StringBuilder("");
			}else if (qName.equals("c_name")) {
				c_name = new StringBuilder("");
			}else if (qName.equals("c_age_days")) {
				c_age_days = new StringBuilder("");
			}else if (qName.equals("c_age_hours")) {
				c_age_hours = new StringBuilder("");
			}else if (qName.equals("c_age_minutes")) {
				c_age_minutes = new StringBuilder("");
			}else if (qName.equals("c_type")) {
				c_type = new StringBuilder("");
			}else if (qName.equals("c_size")) {
				c_size = new StringBuilder("");
			}else if (qName.equals("c_binary_data")) {
			}else if (qName.equals("have_content")) {
				have_content_name = new StringBuilder();
			}
		} 

		catch (Exception ee) 
		{
			Log.d("error in startElement", ee.getStackTrace().toString());
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
	{
		try{
			
			closeCurrentTmpFile();

			if (qName.equals("ch_keywords")) 
			{
				ch_keywords.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath + tmpFileName, ".txt")).trim());
			}else if (qName.equals("ch_utility")) 
			{
				ch_utility.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("ch_creation_date")) 
			{
				ch_creation_date.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());

			}else if(qName.equals("channel"))
			{
				// Create and add a new Channel object
				SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
				java.util.Date date = new java.util.Date();
				String today = df.format(date);

				//Log.i("MobiTrade", "Adding parsed channel, keywords: "+ch_keywords+" uti: "+ch_utility+" img: "+""+" cd: "+ch_creation_date);
				Channel ch = new Channel(ch_keywords.toString(), 0, Float.parseFloat(ch_utility.toString().trim()), 
						dbManager.isChannelAvailable(ch_keywords.toString())?1:0, "", 
								today, 0, 0);
				ch.addAvailableContent(listAvailableContents);
				ch_keywords = null;
				ch_utility = null;
				receivedChannels.add(ch);
				System.gc();

			}else if (qName.equals("have_content")) 
			{
				have_content_name.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
				// Add the new content record
				listAvailableContents.add(new Content(have_content_name.toString().trim()));

			}else if (qName.equals("c_description")) 
			{
				c_description.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_utility")) {
				c_utility.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_source_id")) {
				c_source_id.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_expiration_date")) {
				c_expiration_date.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_message")) {
				c_message.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_channel")) {
				c_channel.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_name")) {
				c_name.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_age_days")) {
				c_age_days.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_age_hours")) {
				c_age_hours.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_age_minutes")) {
				c_age_minutes.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_type")) {
				c_type.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_size")) {
				c_size.append(new String(DatabaseHelper.LoadBinaryDataFromPath(DatabaseHelper.mobiTradeSdcardPath+tmpFileName, ".txt")).trim());
			}else if (qName.equals("c_binary_data")) {
				//c_binary_data = dbManager.DecodeData(currentValue.toString().getBytes(), currentValue.length());
				tmpFileContentBinaryData = DatabaseHelper.mobiTradeSdcardPath+tmpFileName;
			}else if(qName.equals("content"))
			{
				receivedContent = new Content(c_description.toString().trim(), c_utility.toString().trim(), c_source_id.toString().trim(), 
						c_expiration_date.toString().trim(), c_message.toString().trim(), c_channel.toString().trim(), c_name.toString().trim(), 
						c_type.toString().trim(), c_size.toString().trim(), tmpFileContentBinaryData, Integer.parseInt(c_age_days.toString().trim()), 
						Integer.parseInt(c_age_hours.toString().trim()), Integer.parseInt(c_age_minutes.toString().trim()));
				
				c_description = null;
				c_utility = null;
				c_source_id = null;
				c_expiration_date = null;
				c_message = null;
				c_channel = null;
				c_name = null;
				c_type = null;
				c_size = null;
				c_age_days = null;
				c_age_hours = null;
				c_age_minutes = null;
				
				tmpFileContentBinaryData = null;
			}
			
			// Close to clear the tmp file
			if (!qName.equals("c_binary_data"))
			{
				// no need for a new file
				reOpenATMPFile();
			}else
			{
				createAndOpenANewTMPFile();
			}
		}

		catch(Exception e)
		{
			Log.e("MobiTrade", "Error occcured while parsing received message: "+e);
			e.printStackTrace();
		}
	}
	@Override
	public void characters(char ch[], int start, int length) 
	{
		try {

			String theString = new String(ch, start, length);
			if(theString.length() > 0)
			{
				outputTmp.write(theString.trim().getBytes());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("MobiTrade", "Error occured while getting current value of xml entry: "+e);
			e.printStackTrace();
		}

	}
}
