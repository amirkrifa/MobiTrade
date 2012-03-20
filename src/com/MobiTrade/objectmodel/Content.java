package com.MobiTrade.objectmodel;

import java.io.File;
import java.util.HashMap;

import com.MobiTrade.sqlite.DatabaseHelper;

import android.util.Log;

public class Content extends HashMap<String, String>{

	public static final long serialVersionUID = 1L;
	
	public static final String CONTENT_DESCRIPTION = "c_description";
	public static final String CONTENT_UTILITY = "c_utility";
	public static final String CONTENT_SOURCE_ID = "c_source_id";
	public static final String CONTENT_EXPIRATION_DATE = "c_expiration_date";
	public static final String CONTENT_BINARY_DATAPATH = "c_binary_data_path";
	public static final String CONTENT_MESSAGE = "c_message";
	public static final String CONTENT_CHANNEL="c_channel";
	public static final String CONTENT_RECEPTION_DATE = "c_reception_date";
	public static final String CONTENT_CHANNEL_IMAGE = "c_channel_image";
	public static final String CONTENT_NAME = "c_name";
	public static final String CONTENT_TYPE = "c_type";
	public static final String CONTENT_SIZE_BYTE = "c_size";
	public static final String CONTENT_AGE_MINUTES = "c_age_minutes";
	public static final String CONTENT_AGE_HOURS = "c_age_hours";
	public static final String CONTENT_AGE_DAYS = "c_age_days";
	public static final String CONTENT_AGE = "c_age";


	public static final String CONTENT_TYPE_JPEG = ".jpg";
	public static final String CONTENT_TYPE_MP3 = ".mp3";
	public static final String CONTENT_TYPE_TEXT = ".txt";
	public static final String CONTENT_TYPE_TMP = ".tmp";

	
	public static final float CONTENT_DEFAULT_UTILITY = 1000;
	public static final String CONTENT_BINARY_DATA = "c_binary_data";

	private String sourceId;
	private String description;
	private String expirationDate;
	private String binaryDataPath;
	private String message;
	private Float utility;
	private String channel;
	private String reception_date; 	// "dd/MM/yyyy"
	private String channelImage;
	private String fileName;
	private String type;
	private Integer c_size;
	private String tmpFile;
	
	private int age_days;
	private int age_hours;
	private int age_min;


	//CONTENT_BINARY_DATAPATH, CONTENT_CHANNEL, CONTENT_DESCRIPTION, CONTENT_MESSAGE, CONTENT_SOURCE_ID, CONTENT_TTL, CONTENT_UTILITY, CONTENT_RECEPTION_DATE
	public Content(String binaryPath, String contentName, String contentChannel, String contentDescription, 
			String contentMessage, String contentSourceId, String contentExpDate, float contentUtility,
			String contentReceptionDate, String channelImg, String type, Integer c_size, int days, int hours, int min)
	{
		this.binaryDataPath = binaryPath;
		fileName = contentName;
		channel = contentChannel;
		description = contentDescription;
		message = contentMessage;
		sourceId = contentSourceId;
		expirationDate = contentExpDate;
		utility = contentUtility;
		reception_date = contentReceptionDate;
		channelImage = channelImg;
		this.type = type;
		this.c_size = c_size;
		age_days = days;
		age_hours = hours;
		age_min = min;

	}

	public Content(String desc, String util, String src, String exp, String msg, String ch, String name, String type, String size, String data, int days, int hours, int min)
	{
		this.description = desc;
		this.utility = Float.parseFloat(util);
		this.sourceId = src;
		this.expirationDate = exp;
		this.message = msg;
		this.channel = ch;
		this.fileName = name;
		this.type = type;
		this.c_size = Integer.parseInt(size);
		tmpFile = data;
		age_days = days;
		age_hours = hours;
		age_min = min;

	}

	public int getSize()
	{
		return c_size;
	}
	public ContentAge getAge()
	{
		return new ContentAge(age_days, age_hours, age_min);
	}
	
	public Content(String name)
	{
		this.fileName = name;
	}

	
	// decoded binary data
	public byte[] getBinData()
	{
		//return tmpFile;
		return DatabaseHelper.DecodeData(DatabaseHelper.LoadBinaryDataFromPath(tmpFile, CONTENT_TYPE_TMP));
	}
	
	public void renameTmpFileTo(File newPath)
	{
		File tmp = new File(tmpFile);
		if(!tmp.renameTo(newPath))
		{
			Log.e("MobiTrade", "Unable to rename current tmp file to: "+newPath.getAbsolutePath());
		}
	}
	
	public void  ShowDetails()
	{
		Log.i("MobiTrade", "Content Details: ");
		Log.i("MobiTrade", "description: "+description);
		Log.i("MobiTrade", "util: "+utility);
		Log.i("MobiTrade", "sourceId: "+sourceId);
		Log.i("MobiTrade", "exp date: "+expirationDate);
		Log.i("MobiTrade", "message: "+message);
		Log.i("MobiTrade", "channel: "+channel);
		Log.i("MobiTrade", "name: "+fileName);
		Log.i("MobiTrade", "type: "+type);
		Log.i("MobiTrade", "size: "+c_size);
		Log.i("MobiTrade", "bin data path: "+binaryDataPath);

	}

	@Override
	public String get(Object k) {
		String key = (String) k;

		if (CONTENT_BINARY_DATAPATH.equals(key))
			return binaryDataPath;
		else if (CONTENT_CHANNEL.equals(key))
			return channel;
		else if(CONTENT_DESCRIPTION.equals(key))
			return description.toString();
		else if(CONTENT_MESSAGE.equals(key))
			return message.toString();
		else if(CONTENT_RECEPTION_DATE.equals(key))
			return reception_date;
		else if(CONTENT_SOURCE_ID.equals(key))
			return sourceId.toString();
		else if(CONTENT_EXPIRATION_DATE.equals(key))
			return expirationDate.toString();
		else if(CONTENT_UTILITY.equals(key))
			return utility.toString();
		else if(CONTENT_CHANNEL_IMAGE.equals(key))
			return channelImage;
		else if(CONTENT_NAME.equals(key))
			return fileName;
		else if(CONTENT_TYPE.equals(key))
			return type;
		else if(CONTENT_SIZE_BYTE.equals(key))
			return c_size.toString();
		else if(CONTENT_AGE.equals(key))
			return "Created since "+Integer.toString(age_days) + "d" + Integer.toString(age_hours) + "h" + Integer.toString(age_min) + "m";

		return null;
	}

	
	
}
