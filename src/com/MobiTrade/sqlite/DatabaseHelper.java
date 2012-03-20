package com.MobiTrade.sqlite;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipOutputStream;

import com.MobiTrade.ConfigTab;
import com.MobiTrade.network.MobiTradeProtocol;
import com.MobiTrade.objectmodel.Channel;
import com.MobiTrade.objectmodel.Content;
import com.MobiTrade.objectmodel.ContentAge;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.*;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	// Content Table
	private static final String CONTENT_DESCRIPTION = "c_description";
	private static final String CONTENT_UTILITY = "c_utility";
	private static final String CONTENT_SOURCE_ID = "c_source_id";
	private static final String CONTENT_EXPIRATION_DATE = "c_expiration_date";
	private static final String CONTENT_BINARY_DATAPATH = "c_binary_data_path";
	private static final String CONTENT_MESSAGE = "c_message";
	private static final String CONTENT_CHANNEL="c_channel";
	private static final String CONTENT_RECEPTION_DATE = "c_reception_date";
	private static final String CONTENT_SIZE_BYTE = "c_size";
	public static final String CONTENT_NAME = "c_name";
	public static final String CONTENT_TYPE = "c_type";
	public static final String CONTENT_CHANNEL_IMAGE = "c_channel_image";
	public static final String CONTENT_AGE_MINUTES = "c_age_minutes";
	public static final String CONTENT_AGE_HOURS = "c_age_hours";
	public static final String CONTENT_AGE_DAYS = "c_age_days";
	public static final String CONTENT_UPDATE_DATE = "c_update_date";


	// Channel Table
	private static final String CHANNEL_KEYWORDS = "ch_keywords";
	private static final String CHANNEL_UTILITY = "ch_utility";
	private static final String CHANNEL_NBR_CONTENTS = "ch_nbr_contents";
	private static final String CHANNEL_STATUS = "is_local";
	private static final String CHANNEL_IMAGE = "ch_image";
	private static final String CHANNEL_CREATION_DATE = "ch_creation_date";
	private static final String CHANNEL_SIZE_BYTE = "ch_size";
	private static final String CHANNEL_MAX_ALLOWED_SIZE_BYTE = "ch_max_allowed_size";

	// Config Table
	private static final String CONFIG_MAX_ALLOWED_SPACE = "max_allowed_space";
	private static final String CONFIG_SOURCE = "source";
	private static final String CONFIG_KEY = "key";
	private static final String CONFIG_DISCOVERY_INTERVAL = "discovery_interval";
	private static final String CONFIG_USE_VIBRATOR = "use_vibrator";
	private static final String CONFIG_ALWAYS_DISCOVERING = "always_discovering";
	private static final String CONFIG_LIVE_STATUS = "live_status";
	private static final String CONFIG_LIVE_STATUS_2 = "live_status_2";
	private static final String CONFIG_SERVICE_STATUS = "service_status";
	private static final String CONFIG_MAIN_ACTIVITY = "main_activity_status";

	// Database related constants    
	private static final String DATABASE_NAME = "MobiTradeDB";
	private static String DB_PATH = "/data/data/com.MobiTrade/databases/";
	private static final String DATABASE_CONTENT_TABLE = "content";
	private static final String DATABASE_CHANNEL_TABLE = "channel";
	private static final String DATABASE_CONFIG_TABLE = "config";
	private static final String MOBITRADE_DIRECTORY_NAME = ".MobiTrade";


	private final static int MAX_BITMAP_SIZE = 10*1024;

	private final Context myContext;
	private SQLiteDatabase db;
	private static DatabaseHelper dbManager = null;
	private static int tmpIndex = 0;

	public static final String mobiTradeSdcardPath = new String(Environment.getExternalStorageDirectory().toString()+"/" + MOBITRADE_DIRECTORY_NAME + "/"); 
	public static void CreateOpenDBManager(Context ctx)
	{
		if(dbManager == null){
			dbManager = new DatabaseHelper(ctx);
			try
			{
				dbManager.createDataBase();
				dbManager.openDataBase();
				dbManager.InitMobiTradeSDcardDirectory();

				// Adding the built in channels
				//dbManager.insertTestRecords();

			}

			catch (IOException e) {

			}
		}
	}

	public void insertTestRecords()
	{
		// First, adding the channels
		dbManager.insertChannelsForTest();
	}

	public static DatabaseHelper getDBManager()
	{
		if(dbManager != null)
		{
			if(!dbManager.IsOpen())
				dbManager.openDataBase();
		}
		return dbManager;
	}

	/**
	 * Constructor
	 * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
	 * @param context
	 */
	public DatabaseHelper(Context context) 
	{
		super(context, DATABASE_NAME, null, 1);
		this.myContext = context;
	}	

	public Context GetContext()
	{
		return myContext;
	}

	/**
	 * Creates a empty database on the system and rewrites it with your own database.
	 * */
	private void createDataBase() throws IOException{

		boolean dbExist = checkDataBase();

		if(dbExist){

			//database already exist - delete the old database
			//myContext.deleteDatabase(DB_PATH + DATABASE_NAME);
			//copy the new one
			//copyDataBase();


			Log.i("MobiTrade:createDataBase: ", "The database already exist, do nothing");
		}else
		{
			//By calling this method and empty database will be created into the default system path
			//of your application so we are gonna be able to overwrite that database with our database.
			this.getReadableDatabase();
			copyDataBase();
			Log.i("MobiTrade:createDataBase: ", "A new database has been copied");
		}


	}  

	/**
	 * Check if the database already exist to avoid re-copying the file each time you open the application.
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase(){

		SQLiteDatabase checkDB = null;

		try{
			String myPath = DB_PATH + DATABASE_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		}catch(SQLiteException e){

			//database does't exist yet.
		}

		if(checkDB != null){
			Log.i("MobiTrade:checkDataBase: ", "The database already exist, closing it");
			checkDB.close();
		}

		return checkDB != null ? true : false;
	}

	/**
	 * Copies your database from your local assets-folder to the just created empty database in the
	 * system folder, from where it can be accessed and handled.
	 * This is done by transfering bytestream.
	 * */
	private void copyDataBase() throws IOException{

		//Open your local db as the input stream
		InputStream myInput = myContext.getAssets().open(DATABASE_NAME);
		if(myInput != null)
			Log.i("MobiTrade:CopyDatabase: ", "Input stream loaded");
		else
			Log.i("MobiTrade:CopyDatabase: ", "Input stream not loaded");

		// Path to the just created empty db
		String outFileName = DB_PATH + DATABASE_NAME;

		//Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		Log.i("MobiTrade:CopyDatabase: ", "Output stream loaded");


		//transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0){
			myOutput.write(buffer, 0, length);
		}

		Log.i("MobiTrade:copyDataBase: ", "New database copied.");

		//Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();

	}

	public void openDataBase() throws SQLException{

		//Open the database
		String myPath = DB_PATH + DATABASE_NAME;
		db = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
	}

	public boolean IsOpen()
	{
		return db.isOpen();
	}

	@Override
	public synchronized void close() {

		if(db != null)
			db.close();

		super.close();
	}

	public static String GetCurrentDate()
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		Date date = new Date();
		return dateFormat.format(date);
	}

	// Insert Channel
	public long insertChannel(String category, long nbr_contents, float util, int isChannelLocal, String channelImage)
	{
		if(!isChannelAvailable(category))
		{
			ContentValues values = new ContentValues();

			values.put(CHANNEL_KEYWORDS, category);
			values.put(CHANNEL_NBR_CONTENTS, nbr_contents);
			values.put(CHANNEL_UTILITY, util);
			values.put(CHANNEL_STATUS, isChannelLocal);
			values.put(CHANNEL_IMAGE, channelImage);
			values.put(CHANNEL_CREATION_DATE, GetCurrentDate());
			// 3:Set the algorithm that will calculate the max allowed channel size in bytes
			values.put(CHANNEL_MAX_ALLOWED_SIZE_BYTE, 0);
			values.put(CHANNEL_SIZE_BYTE, 0);


			Long ret = db.insert(DATABASE_CHANNEL_TABLE, null, values); 
			Log.i("insertChannel: ", ret.toString() + " new channel inserted.");
			return ret; 

		}
		else
		{
			Log.e("MobiTrade:insertChannel: ", "channel already available.");
			return -1;	
		}
		
	}


	public long insertChannel(Channel ch)
	{
		if(!isChannelAvailable(ch.get(Channel.CHANNEL_KEYWORDS)))
		{
			ContentValues values = new ContentValues();

			values.put(CHANNEL_KEYWORDS, ch.get(Channel.CHANNEL_KEYWORDS));
			values.put(CHANNEL_NBR_CONTENTS, ch.getNumberOfContents());
			values.put(CHANNEL_UTILITY, ch.getUtility());
			// It is a foreign channel by default
			values.put(CHANNEL_STATUS, 0);
			values.put(CHANNEL_IMAGE, String.valueOf(com.MobiTrade.R.drawable.channel));
			values.put(CHANNEL_CREATION_DATE, ch.get(Channel.CHANNEL_CREATION_DATE));
			// 3:Set the algorithm that will calculate the max allowed channel size in bytes
			values.put(CHANNEL_MAX_ALLOWED_SIZE_BYTE, 0);
			values.put(CHANNEL_SIZE_BYTE, ch.getChannelSize());

			Long ret = db.insert(DATABASE_CHANNEL_TABLE, null, values); 
			Log.i("insertChannel: ", ret.toString() + " new channel inserted.");
			return ret; 

		}
		else
		{
			Log.e("MobiTrade:insertChannel: ", "channel already available.");
			return -1;	
		}
	}

	//---insert a new content into the database---
	public long insertContent(String contentDescription, String contentSourceId, 
			String contentBinaryDataPath, String contentMessage, String contentExpDate, float contentUtility, 
			String channel, String rDate, String type, int contentSize, String contentName) 
	{
		if(!isContentAvailable(contentDescription) && isChannelAvailable(channel))
		{
			long nbrAAC = getContentsForChannel(channel).size();
			Channel tmpCh = getChannel(channel);

			ContentValues values = new ContentValues();

			values.put(CONTENT_DESCRIPTION, contentDescription);
			values.put(CONTENT_SOURCE_ID, contentSourceId);
			values.put(CONTENT_BINARY_DATAPATH, contentBinaryDataPath);
			values.put(CONTENT_MESSAGE, contentMessage);
			values.put(CONTENT_EXPIRATION_DATE, contentExpDate);
			values.put(CONTENT_UTILITY, contentUtility);
			values.put(CONTENT_CHANNEL, channel);
			values.put(CONTENT_RECEPTION_DATE, rDate);
			values.put(CONTENT_TYPE, type);
			values.put(CONTENT_SIZE_BYTE, contentSize);
			values.put(CONTENT_CHANNEL_IMAGE, tmpCh.get(Channel.CHANNEL_IMAGE));
			values.put(CONTENT_NAME, contentName);

			values.put(CONTENT_AGE_DAYS, 0);
			values.put(CONTENT_AGE_HOURS, 0);
			values.put(CONTENT_AGE_MINUTES, 0);

			values.put(CONTENT_UPDATE_DATE, GetCurrentDate());

			Long ret = db.insert(DATABASE_CONTENT_TABLE, null, values); 

			// Updates the associated channel number of contents
			updateChannelNbrContents(channel, nbrAAC+1);


			return ret; 
		}
		else
		{
			Log.e("MobiTrade:insertContent: ", "content already available or associated channel does not exist.");
			return -1;	
		}
	}

	public boolean updateChannelNbrContents(String channel, long l) 
	{
		ContentValues args = new ContentValues();
		args.put(CHANNEL_NBR_CONTENTS, l);
		return db.update(DATABASE_CHANNEL_TABLE, args, CHANNEL_KEYWORDS + "=" + "\"" + channel + "\"", null) > 0;
	}

	public boolean updateChannelStatus(String channel, int status) 
	{
		ContentValues args = new ContentValues();
		args.put(CHANNEL_STATUS, status);
		return db.update(DATABASE_CHANNEL_TABLE, args, CHANNEL_KEYWORDS + "=" + "\"" + channel + "\"", null) > 0;
	}

	public boolean updateChannelUtility(String channel, float util) 
	{
		ContentValues args = new ContentValues();
		args.put(CHANNEL_UTILITY, util);
		return db.update(DATABASE_CHANNEL_TABLE, args, CHANNEL_KEYWORDS + "=" + "\"" + channel + "\"", null) > 0;
	}

	public boolean updateChannelMaxAllowedSize(String channel, int maxSize) 
	{
		ContentValues args = new ContentValues();
		args.put(CHANNEL_MAX_ALLOWED_SIZE_BYTE, maxSize);
		return db.update(DATABASE_CHANNEL_TABLE, args, CHANNEL_KEYWORDS + "=" + "\"" + channel + "\"", null) > 0;
	}

	public boolean updateContentUtility(String description, float util) 
	{
		ContentValues args = new ContentValues();
		args.put(CONTENT_UTILITY, util);
		return db.update(DATABASE_CONTENT_TABLE, args, CONTENT_DESCRIPTION + "=" + "\"" + description + "\"", null) > 0;
	}


	public boolean updateConfigMaxAllowedSpace(int maxAllowedSpace) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_MAX_ALLOWED_SPACE, maxAllowedSpace);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}

	public int getConfigMaxAllowedSpace() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_MAX_ALLOWED_SPACE}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		int max = 0;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			max = Integer.parseInt(cur.getString(0));
			cur.close();
		}
		return max;
	}

	public boolean updateConfigDiscoveryInterval(int discoveryInterval) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_DISCOVERY_INTERVAL, discoveryInterval);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}

	public int getConfigDiscoveryInterval() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_DISCOVERY_INTERVAL}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		int interval = 0;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			interval = Integer.parseInt(cur.getString(0));
			cur.close();
		}
		return interval;
	}

	public boolean updateConfigVibratorNotification(int use) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_USE_VIBRATOR, use);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}

	public int getConfigVibratorNotification() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_USE_VIBRATOR}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		int use = 0;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			use = Integer.parseInt(cur.getString(0));
			cur.close();
		}
		return use;
	}

	public boolean updateConfigAlwaysDiscovering(int use) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_ALWAYS_DISCOVERING, use);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}

	public int getConfigAlwaysDiscovering() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_ALWAYS_DISCOVERING}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		int dis = 0;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			dis = Integer.parseInt(cur.getString(0));
			cur.close();
		}
		return dis;
	}

	public boolean updateConfigLiveStatus(String val) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_LIVE_STATUS, val);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}
	public String getConfigLiveStatus() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_LIVE_STATUS}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		String dis = null;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			dis = cur.getString(0);
			cur.close();
		}
		return dis;
	}

	public boolean updateConfigServiceStatus(int val) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_SERVICE_STATUS, val);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}

	public int getConfigServiceStatus() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_SERVICE_STATUS}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		int dis = 0;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			dis = cur.getInt(0);
			cur.close();
		}
		return dis;
	}

	public boolean updateConfigMainActivityStatus(int val) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_MAIN_ACTIVITY, val);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}

	public int getConfigMainActivityStatus() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_MAIN_ACTIVITY}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		int dis = 0;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			dis = cur.getInt(0);
			cur.close();
		}
		return dis;
	}

	public boolean updateConfigLiveStatus2(String val) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_LIVE_STATUS_2, val);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}

	public String getConfigLiveStatus2() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_LIVE_STATUS_2}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		String dis = null;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			dis = cur.getString(0);
			cur.close();
		}
		return dis;
	}


	public boolean updateConfigSource(String source) 
	{
		ContentValues args = new ContentValues();
		args.put(CONFIG_SOURCE, source);
		return db.update(DATABASE_CONFIG_TABLE, args, CONFIG_KEY + "=" + "\"mobitrade\"" , null) > 0;
	}

	public String getConfigSource() 
	{
		Cursor cur = db.query(false, DATABASE_CONFIG_TABLE, new String[] {CONFIG_SOURCE}, CONFIG_KEY + "=" + "\"mobitrade\"", null, null, null, null, null);
		String src = null;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			src = cur.getString(0);
			cur.close();
		}
		return src;
	}

	// Deletes a particular Content given its description
	public boolean deleteContent(String contentDescription)
	{
		// Delete the file related to the content
		Content tmpC = getContent(contentDescription);

		DeleteMobiTradeFile(tmpC.get(CONTENT_BINARY_DATAPATH));

		String associatedChannel = tmpC.get(CONTENT_CHANNEL);

		// Delete the content from the database
		if(db.delete(DATABASE_CONTENT_TABLE, CONTENT_DESCRIPTION + "=" + "\"" + contentDescription + "\"" , null) > 0)
		{

			if(associatedChannel != null)
			{
				int cnbr = getContentsForChannel(associatedChannel).size();
				// Update channel number of contents
				if(cnbr >= 2)
					updateChannelNbrContents(associatedChannel, cnbr - 1);
				else
					updateChannelNbrContents(associatedChannel, 0);
			}
			return true;
		}else
		{
			return false;
		}
	}

	// Deletes a particular Channel given its keywords
	public boolean deleteChannel(String keyWords)
	{
		List<Content> listRelatedContents = getContentsForChannel(keyWords);
		for(Content c:listRelatedContents)
		{
			deleteContent(c.get(Content.CONTENT_DESCRIPTION));
		}
		return (db.delete(DATABASE_CHANNEL_TABLE, CHANNEL_KEYWORDS + "=" + "\"" + keyWords + "\"", null) > 0);
	}

	//---retrieves all the contents---
	public List<Content> getAllContents() 
	{
		// Updating contents age
		updateAllContentsAge();

		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_BINARY_DATAPATH, CONTENT_NAME
				, CONTENT_CHANNEL, CONTENT_DESCRIPTION, CONTENT_MESSAGE, CONTENT_SOURCE_ID, CONTENT_EXPIRATION_DATE, CONTENT_UTILITY
				, CONTENT_RECEPTION_DATE, CONTENT_CHANNEL_IMAGE, CONTENT_TYPE, CONTENT_SIZE_BYTE, CONTENT_AGE_DAYS, CONTENT_AGE_HOURS, CONTENT_AGE_MINUTES}, null, null, null, null, null, null);
		List<Content> listContents = new ArrayList<Content>();
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) {
				Channel tmpCh = getChannel(cur.getString(1));
				//binaryPath, String contentName, String contentChannel, String contentDescription, String contentMessage, 
				// String contentSourceId, String contentExpDate, float contentUtility, String contentReceptionDate, String channelImg, String type, Integer c_size)

				Content c = new Content(cur.getString(0), cur.getString(1), cur.getString(2), cur.getString(3), cur.getString(4), cur.getString(5), cur.getString(6), cur.getFloat(7),cur.getString(8), tmpCh.get(CHANNEL_IMAGE),  cur.getString(9), Integer.parseInt(cur.getString(10)), cur.getInt(11), cur.getInt(12), cur.getInt(13));
				listContents.add(c);
				cur.moveToNext();
			}
			cur.close();
		}
		Log.i("MobiTrade:getAllContents() Returned number of contents: ", Integer.toString(listContents.size()));
		return listContents;
	}

	public int getTotalNumberOfContents() 
	{
		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_BINARY_DATAPATH}, null, null, null, null, null, null);
		int nbr = cur.getCount();
		cur.close();
		System.gc();
		return nbr;
	}


	public List<Content> getContentsForChannel(String channel) 
	{
		// Updating contents age
		updateContentsAgeForChannel(channel);


		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_BINARY_DATAPATH, CONTENT_NAME
				, CONTENT_CHANNEL, CONTENT_DESCRIPTION, CONTENT_MESSAGE, CONTENT_SOURCE_ID, CONTENT_EXPIRATION_DATE, CONTENT_UTILITY
				, CONTENT_RECEPTION_DATE, CONTENT_CHANNEL_IMAGE, CONTENT_TYPE, CONTENT_SIZE_BYTE, CONTENT_AGE_DAYS, CONTENT_AGE_HOURS, CONTENT_AGE_MINUTES},
				CONTENT_CHANNEL + "=" + "\"" + channel + "\"", null, null, null, null, null);
		List<Content> listContents = new ArrayList<Content>();
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) {
				Content c = new Content(cur.getString(0), cur.getString(1), cur.getString(2), cur.getString(3), cur.getString(4), 
						cur.getString(5), cur.getString(6), cur.getFloat(7),cur.getString(8), cur.getString(9),
						cur.getString(10), Integer.parseInt(cur.getString(11)), cur.getInt(12), cur.getInt(13), cur.getInt(14));
				listContents.add(c);
				cur.moveToNext();
			}
			cur.close();
		}
		return listContents;
	}

	// Returns the size in bytes of the space allocated to MobiTrade

	public long getMobiTradeAllocatedSpace()
	{

		List<Channel> listChannels = getAllChannels();
		long total = 0;
		if(listChannels.size() > 0)
		{
			for(Channel ch:listChannels)
			{
				total += getChannelAllocatedSpace(ch.get(Channel.CHANNEL_KEYWORDS));
			}
		}

		return total;
	}

	// Returns the size in bytes of the space allocated to a given channel
	public long getChannelAllocatedSpace(String channel)
	{
		long total = 0;
		List<Content> listContents = getContentsForChannel(channel);
		if(listContents.size() > 0)
		{
			for(Content c:listContents)
			{
				total += getContentSize(c.get(Content.CONTENT_BINARY_DATAPATH));
			}
		}

		return total;
	}

	// Returns the size in bytes of the space allocated to a given content
	public long getContentSize(String contentAbsoluetPath)
	{
		File f = new File(contentAbsoluetPath);
		return f.length();
	}

	public List<Content> getTodayContents() 
	{
		// Getting today date 
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		java.util.Date date = new java.util.Date();
		String today = df.format(date);

		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_BINARY_DATAPATH, CONTENT_NAME
				, CONTENT_CHANNEL, CONTENT_DESCRIPTION, CONTENT_MESSAGE, CONTENT_SOURCE_ID, CONTENT_EXPIRATION_DATE, CONTENT_UTILITY
				, CONTENT_RECEPTION_DATE, CONTENT_CHANNEL_IMAGE, CONTENT_TYPE, CONTENT_SIZE_BYTE, CONTENT_AGE_DAYS, CONTENT_AGE_HOURS, CONTENT_AGE_MINUTES}
		,CONTENT_RECEPTION_DATE + "=" + "\"" + today + "\""
		, null, null, null, null, null);

		List<Content> listContents = new ArrayList<Content>();
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) {


				Content c = new Content(cur.getString(0), cur.getString(1), cur.getString(2), cur.getString(3), cur.getString(4), 
						cur.getString(5), cur.getString(6), cur.getFloat(7),cur.getString(8), cur.getString(9),
						cur.getString(10), Integer.parseInt(cur.getString(11)), cur.getInt(12), cur.getInt(13), cur.getInt(14));
				listContents.add(c);
				cur.moveToNext();
			}
			cur.close();
		}
		return listContents;
	}

	// retrive all the available channels
	public List<Channel> getAllChannels()
	{
		Cursor cur = db.query(false, DATABASE_CHANNEL_TABLE, new String[] {CHANNEL_KEYWORDS, CHANNEL_NBR_CONTENTS, CHANNEL_UTILITY, CHANNEL_STATUS, CHANNEL_IMAGE, CHANNEL_CREATION_DATE, CHANNEL_SIZE_BYTE, CHANNEL_MAX_ALLOWED_SIZE_BYTE}, null, null, null, null, null, null);
		List<Channel> listChannels = new ArrayList<Channel>();
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) {
				Channel c = new Channel(cur.getString(0), cur.getInt(1), cur.getFloat(2), cur.getInt(3), cur.getString(4), cur.getString(5), Integer.parseInt(cur.getString(6)), Integer.parseInt(cur.getString(7)));
				listChannels.add(c);
				cur.moveToNext();
			}
			cur.close();
		}
		return listChannels;
	}

	// retrive all the locally requested channels
	public List<Channel> getAllRequestedChannels()
	{
		Cursor cur = db.query(false, DATABASE_CHANNEL_TABLE, new String[] {CHANNEL_KEYWORDS, CHANNEL_NBR_CONTENTS, CHANNEL_UTILITY, CHANNEL_STATUS, CHANNEL_IMAGE, CHANNEL_CREATION_DATE, CHANNEL_SIZE_BYTE, CHANNEL_MAX_ALLOWED_SIZE_BYTE}, 
				CHANNEL_STATUS + " = 1" , null, null, null, null, null);
		List<Channel> listChannels = new ArrayList<Channel>();
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) {
				Channel c = new Channel(cur.getString(0), cur.getInt(1), cur.getFloat(2), cur.getInt(3), cur.getString(4), cur.getString(5), Integer.parseInt(cur.getString(6)), Integer.parseInt(cur.getString(7)));
				listChannels.add(c);
				cur.moveToNext();
			}
			cur.close();
		}
		return listChannels;
	}

	public List<Channel> getAllForeignChannels()
	{
		Cursor cur = db.query(false, DATABASE_CHANNEL_TABLE, new String[] {CHANNEL_KEYWORDS, CHANNEL_NBR_CONTENTS, CHANNEL_UTILITY, CHANNEL_STATUS, CHANNEL_IMAGE, CHANNEL_CREATION_DATE, CHANNEL_SIZE_BYTE, CHANNEL_MAX_ALLOWED_SIZE_BYTE}, 
				CHANNEL_STATUS + " = 0" , null, null, null, null, null);
		List<Channel> listChannels = new ArrayList<Channel>();
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) {
				Channel c = new Channel(cur.getString(0), cur.getInt(1), cur.getFloat(2), cur.getInt(3), cur.getString(4), cur.getString(5), Integer.parseInt(cur.getString(6)), Integer.parseInt(cur.getString(7)));
				listChannels.add(c);
				cur.moveToNext();
			}
			cur.close();
		}
		return listChannels;
	}

	public int GetNumberOfRequestedChannels()
	{
		int nbr = 0;
		Cursor cur = db.query(false, DATABASE_CHANNEL_TABLE, new String[] {CHANNEL_KEYWORDS, CHANNEL_NBR_CONTENTS, CHANNEL_UTILITY, CHANNEL_STATUS, CHANNEL_IMAGE, CHANNEL_CREATION_DATE, CHANNEL_SIZE_BYTE, CHANNEL_MAX_ALLOWED_SIZE_BYTE}, 
				CHANNEL_STATUS + " = 1" , null, null, null, null, null);
		nbr = cur.getCount();
		cur.close();
		return nbr;

	}

	public boolean isLocalRequestedChannel(String keywords)
	{

		Cursor cur = db.query(false, DATABASE_CHANNEL_TABLE, new String[] {CHANNEL_STATUS}, 
				CHANNEL_KEYWORDS + "=" + "\"" + keywords + "\"" , null, null, null, null, null);

		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			int status = cur.getInt(0);
			cur.close();
			return (status == 1);
		}else
		{
			cur.close();
			return false;
		}
	}

	// retrive all non locally requested channels
	public List<Channel> getNotLocallyRequestedChannels()
	{
		Cursor cur = db.query(false, DATABASE_CHANNEL_TABLE, new String[] {CHANNEL_KEYWORDS, CHANNEL_NBR_CONTENTS, CHANNEL_UTILITY, CHANNEL_STATUS, CHANNEL_IMAGE, CHANNEL_CREATION_DATE, CHANNEL_SIZE_BYTE, CHANNEL_MAX_ALLOWED_SIZE_BYTE}, 
				CHANNEL_STATUS + " = 0" , null, null, null, null, null);
		List<Channel> listChannels = new ArrayList<Channel>();
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) {
				Channel c = new Channel(cur.getString(0), cur.getInt(1), cur.getFloat(2), cur.getInt(3), cur.getString(4), cur.getString(5), Integer.parseInt(cur.getString(6)), Integer.parseInt(cur.getString(7)));
				listChannels.add(c);
				cur.moveToNext();
			}
			cur.close();
		}
		return listChannels;
	}

	// retrive a channel based on its name, we suppose that there is a unique channel that matches the given keywords
	public Channel getChannel(String keywords)
	{
		Cursor cur = db.query(false, DATABASE_CHANNEL_TABLE, new String[] {CHANNEL_KEYWORDS, CHANNEL_NBR_CONTENTS, CHANNEL_UTILITY, CHANNEL_STATUS, CHANNEL_IMAGE, CHANNEL_CREATION_DATE, CHANNEL_SIZE_BYTE, CHANNEL_MAX_ALLOWED_SIZE_BYTE}, 
				CHANNEL_KEYWORDS + "=" + "\"" + keywords + "\"", null, null, null, null, null);
		Channel ch = null;
		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			ch = new Channel(cur.getString(0), cur.getInt(1), cur.getFloat(2), cur.getInt(3), cur.getString(4), cur.getString(5), Integer.parseInt(cur.getString(6)), Integer.parseInt(cur.getString(7)));
			cur.close();
		}
		return ch;
	}


	public Content getContent(String description) 
	{
		updateContentAge(description);

		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_BINARY_DATAPATH, CONTENT_NAME
				, CONTENT_CHANNEL, CONTENT_DESCRIPTION, CONTENT_MESSAGE, CONTENT_SOURCE_ID, CONTENT_EXPIRATION_DATE, CONTENT_UTILITY
				, CONTENT_RECEPTION_DATE, CONTENT_CHANNEL_IMAGE, CONTENT_TYPE, CONTENT_SIZE_BYTE, CONTENT_AGE_DAYS, CONTENT_AGE_HOURS, CONTENT_AGE_MINUTES},
				CONTENT_DESCRIPTION + "=" + "\"" + description + "\"", null, null, null, null, null);
		Content c = null;
		if(cur.getCount() > 0)
		{

			cur.moveToFirst();
			c = new Content(cur.getString(0), cur.getString(1), cur.getString(2), cur.getString(3), cur.getString(4), 
					cur.getString(5), cur.getString(6), cur.getFloat(7),cur.getString(8), cur.getString(9),
					cur.getString(10), Integer.parseInt(cur.getString(11)), cur.getInt(12), cur.getInt(13), cur.getInt(14));
			cur.close();
		}
		return c;
	}

	public ContentAge getContentAge(String description) 
	{
		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_AGE_DAYS, CONTENT_AGE_HOURS, CONTENT_AGE_MINUTES},
				CONTENT_DESCRIPTION + "=" + "\"" + description + "\"", null, null, null, null, null);
		ContentAge ca = null;
		if(cur.getCount() > 0)
		{

			cur.moveToFirst();
			ca = new ContentAge(cur.getInt(0), cur.getInt(1), cur.getInt(2));
			cur.close();
		}
		return ca;
	}

	public void updateAllContentsAge() 
	{
		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_DESCRIPTION}, null, null, null, null, null, null);

		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) 
			{
				updateContentAge(cur.getString(0));
				cur.moveToNext();
			}
			cur.close();
		}
	}

	public void updateContentsAgeForChannel(String keywords) 
	{
		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_DESCRIPTION}, CONTENT_CHANNEL + "=" + "\"" + keywords + "\"", null, null, null, null, null);

		if(cur.getCount() > 0)
		{
			cur.moveToFirst();
			while (cur.isAfterLast() == false) 
			{
				updateContentAge(cur.getString(0));
				cur.moveToNext();
			}
			cur.close();
		}
	}

	public boolean updateContentLastUpdateDate(String description, String date) 
	{
		ContentValues args = new ContentValues();
		args.put(CONTENT_UPDATE_DATE, date);
		return db.update(DATABASE_CONTENT_TABLE, args, CONTENT_DESCRIPTION + "=" + "\"" + description + "\"", null) > 0;
	}

	public String getContentLastUpdateDate(String description) 
	{
		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_UPDATE_DATE},
				CONTENT_DESCRIPTION + "=" + "\"" + description + "\"", null, null, null, null, null);
		String ud = null;
		if(cur.getCount() > 0)
		{

			cur.moveToFirst();
			ud = cur.getString(0);
			cur.close();
		}
		return ud;
	}

	public ContentAge updateContentAge(String description) 
	{

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		Date date = new Date();

		String lastUpdate = getContentLastUpdateDate(description);
		Date lastUpdateDate = null;

		ContentAge la = getContentAge(description);
		ContentAge ca = null;

		try 
		{
			lastUpdateDate = dateFormat.parse(lastUpdate);

			int diffDays = (int)daysBetween(lastUpdateDate, date);
			int diffHours = (int)hoursBetween(lastUpdateDate, date);
			int diffMin = (int)minutesBetween(lastUpdateDate, date);
			if( diffDays > 0 || diffHours > 0 || diffMin > 0)
			{
				ca = new ContentAge((int)daysBetween(lastUpdateDate, date), (int)hoursBetween(lastUpdateDate, date), (int)minutesBetween(lastUpdateDate, date));
				ca.addAge(la);

				ContentValues args = new ContentValues();
				args.put(CONTENT_AGE_DAYS, ca.c_age_days);
				args.put(CONTENT_AGE_HOURS, ca.c_age_hours);
				args.put(CONTENT_AGE_MINUTES, ca.c_age_minutes);

				db.update(DATABASE_CONTENT_TABLE, args, CONTENT_DESCRIPTION + "=" + "\"" + description + "\"", null);

				// Update the content age last update date
				updateContentLastUpdateDate(description, GetCurrentDate());
			}else ca = la;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ca;
	}


	public static long daysBetween(Date firstDate, Date secondDate) {
		// We only use the date part of the given dates
		double firstMSeconds = firstDate.getTime();
		double secondMSeconds = secondDate.getTime();

		long difference = (long)Math.floor((secondMSeconds-firstMSeconds)/(1000*60*60*24));

		return difference;
	}

	public static long hoursBetween(Date firstDate, Date secondDate) {
		// We only use the date part of the given dates
		double firstMSeconds = firstDate.getTime();
		double secondMSeconds = secondDate.getTime();

		long difference = (long)Math.floor((secondMSeconds-firstMSeconds)/(1000*60*60));

		return difference;
	}

	public static long minutesBetween(Date firstDate, Date secondDate) {
		// We only use the date part of the given dates
		double firstMSeconds = firstDate.getTime();
		double secondMSeconds = secondDate.getTime();

		long difference = (long)Math.floor((secondMSeconds-firstMSeconds)/(1000*60));

		return difference;
	}

	public static long secondsBetween(Date firstDate, Date secondDate) {
		// We only use the date part of the given dates
		double firstMSeconds = firstDate.getTime();
		double secondMSeconds = secondDate.getTime();

		long difference = (long)Math.floor((secondMSeconds-firstMSeconds)/(1000));

		return difference;
	}

	public Content getContentByName(String name) 
	{
		Cursor cur = db.query(false, DATABASE_CONTENT_TABLE, new String[] {CONTENT_BINARY_DATAPATH, CONTENT_NAME
				, CONTENT_CHANNEL, CONTENT_DESCRIPTION, CONTENT_MESSAGE, CONTENT_SOURCE_ID, CONTENT_EXPIRATION_DATE, CONTENT_UTILITY
				, CONTENT_RECEPTION_DATE, CONTENT_CHANNEL_IMAGE, CONTENT_TYPE, CONTENT_SIZE_BYTE, CONTENT_AGE_DAYS, CONTENT_AGE_HOURS, CONTENT_AGE_MINUTES},
				CONTENT_NAME + "=" + "\"" + name + "\"", null, null, null, null, null);
		Content c = null;
		if(cur.getCount() > 0)
		{

			cur.moveToFirst();
			c = new Content(cur.getString(0), cur.getString(1), cur.getString(2), cur.getString(3), cur.getString(4), 
					cur.getString(5), cur.getString(6), cur.getFloat(7),cur.getString(8), cur.getString(9),
					cur.getString(10), Integer.parseInt(cur.getString(11)), cur.getInt(12), cur.getInt(13), cur.getInt(14));
			cur.close();
		}
		return c;
	}

	// Get the number of available channels
	public int getTotalNumberOfChannels()
	{
		Cursor cur = db.query(false, DATABASE_CHANNEL_TABLE, new String[] {CHANNEL_KEYWORDS}, null, null, null, null, null, null);
		int nbr = cur.getCount();
		cur.close();
		System.gc();
		return nbr;
	}

	// Verifies if a given content entry is available within the database
	public boolean isContentAvailable(String contentDescription) throws SQLException
	{
		try
		{
			Cursor mCursor =
				db.query(true, DATABASE_CONTENT_TABLE, new String[] {
						CONTENT_DESCRIPTION
				}, 
				CONTENT_DESCRIPTION + "=" + "\""+ contentDescription + "\"" , 
				null,
				null, 
				null, 
				null, 
				null);

			return mCursor.getCount() != 0;
		}

		catch(SQLiteException e)
		{
			Log.e("MobiTrade:isContentAvailable", e.getMessage());
			return false;
		}
	}

	public boolean isChannelAvailable(String channelKeywords)
	{
		try
		{
			Cursor mCursor =
				db.query(true, DATABASE_CHANNEL_TABLE, new String[] {
						CHANNEL_NBR_CONTENTS, CHANNEL_UTILITY
				}, 
				CHANNEL_KEYWORDS + "=" + "\"" + channelKeywords +"\"", 
				null,
				null, 
				null, 
				null, 
				null);

			Log.i("isCategoryAvailable", "Query done");
			return mCursor.getCount() != 0;
		}
		catch (SQLiteException e)
		{
			Log.e("MobiTrade:isCategoryAvailable", e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}



	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub

	}

	// Load bytes from a file to be sent to a remote device
	public static byte [] LoadBinaryDataFromPath(String filePath, String fileType)
	{

		if(fileType.compareTo(Content.CONTENT_TYPE_JPEG) == 0)
		{
			Bitmap bm = BitmapFactory.decodeFile(filePath);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();  
			bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object   
			bm.recycle();
			System.gc();

			return baos.toByteArray();
		}else if(fileType.compareTo(Content.CONTENT_TYPE_MP3) == 0)
		{

		}
		else if(fileType.compareTo(Content.CONTENT_TYPE_TEXT) == 0 || fileType.compareTo(Content.CONTENT_TYPE_TMP) == 0)
		{
			try 
			{
				// open the file for reading
				File f = new File(filePath);
				int dataLength = (int)f.length();

				FileInputStream in = new FileInputStream(filePath);
				ByteArrayOutputStream out = new ByteArrayOutputStream((int)dataLength);

				PumpFile(in, out, (int)dataLength);
				in.close();
				out.close();

				return out.toByteArray();
			} 
			catch (Exception e) 
			{
				// do something if the myfilename.txt does not exits
				Log.e("MobiTrade", "Error occured while loading file data from sdcard "+e.getMessage());
				return null;
			}
		}

		return null;
	}

	public static void AppendBinaryDataFromPathToFile(String filePath, String fileType, FileOutputStream out) throws IOException
	{

		if(fileType.compareTo(Content.CONTENT_TYPE_JPEG) == 0)
		{
			//File tmpF = new File(filePath);
			BitmapFactory.Options options=new BitmapFactory.Options(); 

			//if(tmpF.length() > MAX_BITMAP_SIZE)
			//	options.inSampleSize = 4;

			//tmpF = null;

			Bitmap bm = BitmapFactory.decodeFile(filePath, options);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();  
			
			bm.compress(Bitmap.CompressFormat.JPEG, 20, baos); //bm is the bitmap object   
			
			bm.recycle();
			bm = null;
			
			out.write(EncodeData(baos.toByteArray()));
			baos.close();
			
			baos = null;
			System.gc();

		}else if(fileType.compareTo(Content.CONTENT_TYPE_MP3) == 0)
		{

		}
		else if(fileType.compareTo(Content.CONTENT_TYPE_TEXT) == 0)
		{
			try 
			{

				// open the file for reading
				File f = new File(filePath);
				int dataLength = (int)f.length();

				FileInputStream in = new FileInputStream(filePath);
				ByteArrayOutputStream outS = new ByteArrayOutputStream((int)dataLength);

				PumpFile(in, outS, (int)dataLength);

				out.write(EncodeData(outS.toByteArray()));
				outS.close();
				in.close();

			} 
			catch (Exception e) 
			{
				// do something if the myfilename.txt does not exits
				Log.e("MobiTrade", "Error occured while loading file data from sdcard "+e.getMessage());
			}
		}else if(fileType.compareTo(Content.CONTENT_TYPE_TMP) == 0)
		{
			try 
			{
				// open the file for reading
				File f = new File(filePath);
				int dataLength = (int)f.length();

				FileInputStream in = new FileInputStream(filePath);
				PumpFile(in, out, (int)dataLength);
				in.close();

			} 
			catch (Exception e) 
			{
				// do something if the myfilename.txt does not exits
				Log.e("MobiTrade", "Error occured while loading file data from sdcard "+e.getMessage());
			}

		}

	}

	public static boolean PumpFile(InputStream in, OutputStream out, int size) 
	{
		try 
		{
			byte[] buffer = new byte[1024]; 
			int done = 0;
			while (done < size) 
			{
				int read = in.read(buffer);
				if (read == -1) 
				{
					Log.e("MobiTrade", "PumpFile: Something went horribly wrong when trying to load a binary file");
				}
				out.write(buffer, 0, read);

				done += read;
			}

			buffer = null;
			return true;
		} 
		catch (IOException e) 
		{
			Log.e("MobiTrade:PumpFile", e.getMessage());
			return false;
		}
	}

	public static boolean PumpFileAnEncode(InputStream in, OutputStream out, int size) 
	{
		try 
		{
			byte[] buffer = new byte[4096]; 
			int done = 0;
			while (done < size) 
			{
				int read = in.read(buffer);
				if (read == -1) 
				{
					Log.e("MobiTrade", "PumpFile: Something went horribly wrong when trying to load a binary file");
				}
				out.write(EncodeData(buffer), 0, read);

				done += read;
			}
			buffer = null;
			return true;
		} 
		catch (IOException e) 
		{
			Log.e("MobiTrade:PumpFile", e.getMessage());
			return false;
		}
	}

	// Given a file name, write the binary data to local file
	public static void WriteBinaryDataToFile(String destFile, byte [] binaryData)
	{
		try 
		{
			// destFile is the file name, appending the absolute path
			StringBuilder fileAbsolutePath = new StringBuilder();
			fileAbsolutePath.append("/data/data/com.MobiTrade/");
			fileAbsolutePath.append(destFile);

			// Mybe we should modify the file path
			FileOutputStream f  = dbManager.myContext.openFileOutput(fileAbsolutePath.toString(), Activity.MODE_WORLD_WRITEABLE);
			f.write(binaryData);
			f.flush();
			f.close();
		} 
		catch (IOException e) 
		{
			Log.e("MobiTrade", e.getMessage());
		}
	}

	// Deletes a locally stored file, given its name
	public static void DeleteMobiTradeFile(String absoluteFilePath)
	{

		File f = new File(absoluteFilePath);

		if(!f.delete())
		{
			Log.e("MobiTrade", "Unable to delete the file: " + absoluteFilePath);
		}
	}

	public static byte[] EncodeData(byte[] Input)
	{
		return Base64.encode(Input, Base64.DEFAULT);
	}

	public static byte [] DecodeData(byte[] Input)
	{
		byte[] res = null;
		try{
			res = Base64.decode(Input, Base64.DEFAULT);
		}
		catch (Exception e) {
			Log.e("MobiTrade", "Unable to decode Base64 data: "+e.getMessage()+" input: "+Input.toString());

			e.printStackTrace();
		}
		return res;
	}


	// Downloads an Image and returns a Bitmap object
	public static Bitmap LoadImageFromUrl(String URL, BitmapFactory.Options options)
	{       
		Bitmap bitmap = null;
		InputStream in = null;       
		try {
			in = OpenHttpConnection(URL);
			bitmap = BitmapFactory.decodeStream(in, null, options);
			in.close();
		} catch (IOException e1) {
		}
		return bitmap;               
	}

	// Opens a http connexion to a given url andd returns an InputStream to read the data
	private static InputStream OpenHttpConnection(String strURL) throws IOException
	{
		InputStream inputStream = null;
		URL url = new URL(strURL);
		URLConnection conn = url.openConnection();

		try{
			HttpURLConnection httpConn = (HttpURLConnection)conn;
			httpConn.setRequestMethod("GET");
			httpConn.connect();

			if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				inputStream = httpConn.getInputStream();
			}
		}
		catch (Exception ex)
		{
		}
		return inputStream;
	}


	public static boolean CopyFileToMobiTradeSDcard(String absoluteSourcePath, String fileName, long fileSize)
	{
		FileInputStream fileIn = null;
		FileOutputStream fileOut = null;
		try 
		{
			File f = new File(absoluteSourcePath);
			fileIn = new FileInputStream(absoluteSourcePath);
			fileOut = new FileOutputStream(mobiTradeSdcardPath + fileName);

			PumpFile(fileIn, fileOut, (int)f.length());

			fileIn.close();
			fileOut.close();

			return true;
		}

		catch (FileNotFoundException e) 
		{
			e.printStackTrace();

			return false;
		}catch (IOException e) 
		{
			e.printStackTrace();
			return false;
		}

	}

	// Saves a Bitmap image to the SDCARD
	public static void SaveImageToMobiTradeSDcard(Bitmap image, String imageName)
	{
		OutputStream outStream = null;

		File file = new File(mobiTradeSdcardPath, imageName);
		try 
		{
			outStream = new FileOutputStream(file);
			image.compress(Bitmap.CompressFormat.PNG, 100, outStream);
			outStream.flush();
			outStream.close();
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static long GetContentSizeFromSdcard(String fileName)
	{
		File file = new File(mobiTradeSdcardPath, fileName);
		Log.i("File size:",Long.toString(file.length()));
		return file.length();
	}


	// Initialyzing and creating the MobiTrade directory within the SDcard
	private  void InitMobiTradeSDcardDirectory()
	{
		File fileDir = new File(mobiTradeSdcardPath);
		if(!fileDir.exists())
			fileDir.mkdirs();
	}

	private  void insertChannelsForTest()
	{
		dbManager.insertChannel("Videos", 0, MobiTradeProtocol.INITIAL_LOCAL_CHANNEL_UTILITY, 0, String.valueOf(com.MobiTrade.R.drawable.video));
		dbManager.insertChannel("Images", 0, MobiTradeProtocol.INITIAL_LOCAL_CHANNEL_UTILITY, 0, String.valueOf(com.MobiTrade.R.drawable.photo));
		dbManager.insertChannel("Messages", 0,MobiTradeProtocol.INITIAL_LOCAL_CHANNEL_UTILITY, 0, String.valueOf(com.MobiTrade.R.drawable.message));
		dbManager.insertChannel("News", 0, MobiTradeProtocol.INITIAL_LOCAL_CHANNEL_UTILITY, 0, String.valueOf(com.MobiTrade.R.drawable.channel));
	}


	public long insertContent(Content c) 
	{
		try{
			if(!isContentAvailable(c.get(Content.CONTENT_DESCRIPTION)) && isChannelAvailable(c.get(Content.CONTENT_CHANNEL)))
			{
				// Writing binary data to sdcard
				long nbrAAC = getContentsForChannel(c.get(Content.CONTENT_CHANNEL)).size();
				Channel tmpCh = getChannel(c.get(Content.CONTENT_CHANNEL));

				SaveReceivedContentToMobiTradeSDcard(c);

				ContentValues values = new ContentValues();
				values.put(CONTENT_DESCRIPTION, c.get(Content.CONTENT_DESCRIPTION));
				values.put(CONTENT_SOURCE_ID, c.get(Content.CONTENT_SOURCE_ID));
				values.put(CONTENT_BINARY_DATAPATH, mobiTradeSdcardPath+ c.get(Content.CONTENT_NAME));
				values.put(CONTENT_MESSAGE, c.get(Content.CONTENT_MESSAGE));
				values.put(CONTENT_EXPIRATION_DATE, c.get(Content.CONTENT_EXPIRATION_DATE));
				values.put(CONTENT_UTILITY, c.get(Content.CONTENT_UTILITY));
				values.put(CONTENT_CHANNEL, c.get(Content.CONTENT_CHANNEL));
				values.put(CONTENT_RECEPTION_DATE, c.get(Content.CONTENT_RECEPTION_DATE));
				values.put(CONTENT_TYPE, c.get(Content.CONTENT_TYPE));
				values.put(CONTENT_SIZE_BYTE, c.get(Content.CONTENT_SIZE_BYTE));
				values.put(CONTENT_CHANNEL_IMAGE, tmpCh.get(Channel.CHANNEL_IMAGE));
				values.put(CONTENT_NAME, c.get(Content.CONTENT_NAME));
				ContentAge ca = c.getAge();
				values.put(CONTENT_AGE_DAYS, ca.c_age_days);
				values.put(CONTENT_AGE_HOURS, ca.c_age_hours);
				values.put(CONTENT_AGE_MINUTES, ca.c_age_minutes);
				values.put(CONTENT_UPDATE_DATE, GetCurrentDate());


				Long ret = db.insert(DATABASE_CONTENT_TABLE, null, values); 
				Log.i("insertContent: ", ret.toString() + " new contents inserted, channel: "+c.get(Content.CONTENT_CHANNEL)+" content desc: "+c.get(Content.CONTENT_DESCRIPTION));

				// Updates the associated channel number of contents
				updateChannelNbrContents(c.get(Content.CONTENT_CHANNEL), nbrAAC+1);


				return ret; 
			}
			else
			{
				Log.e("MobiTrade", "content already available or associated channel does not exist.");
				return 0;	
			}
		}
		catch(Exception e)
		{
			Log.e("MobiTrade", "unable to insert received content: "+e.getMessage());
			e.printStackTrace();
			return 0;	
		}
	}

	// Write a tmp file and returns its absolute path
	public static String WriteTmpToMobiTradeSDcard(byte [] data)
	{
		StringBuilder tmpFileName = new StringBuilder();
		tmpFileName.append("tmp");
		tmpFileName.append(tmpIndex);
		tmpFileName.append(".tmp");
		tmpIndex ++;

		File file = new File(mobiTradeSdcardPath, tmpFileName.toString());
		try 
		{
			OutputStream output = new FileOutputStream(file);
			output.write(data);
			output.flush();
			output.close();
		} 

		catch (FileNotFoundException e) 
		{
			Log.e("MobiTrade", "Error occured while trying to write th received file to sdcard: "+e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		catch (IOException e) 
		{
			Log.e("MobiTrade", "Error occured while trying to write th received file to sdcard: "+e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mobiTradeSdcardPath + tmpFileName.toString();
	}

	public static void SaveReceivedContentToMobiTradeSDcard(Content c)
	{


		if(c.get(Content.CONTENT_TYPE).compareTo(Content.CONTENT_TYPE_JPEG) == 0 ||
				c.get(Content.CONTENT_TYPE).compareTo(Content.CONTENT_TYPE_TEXT) == 0)
		{

			Log.i("MobiTrade", "Writing JPEG content: "+c.get(Content.CONTENT_NAME)+" to sdcard: "
					+mobiTradeSdcardPath+ c.get(Content.CONTENT_NAME));
			OutputStream outStream = null;

			File file = new File(mobiTradeSdcardPath, c.get(Content.CONTENT_NAME));

			try 
			{
				outStream = new FileOutputStream(file);
				outStream.write(c.getBinData());
				outStream.flush();
				outStream.close();
			} 
			catch (FileNotFoundException e) 
			{
				Log.e("MobiTrade", "Error occured while trying to write th received file to sdcard: "+e);
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 

			catch (IOException e) 
			{
				Log.e("MobiTrade", "Error occured while trying to write th received file to sdcard: "+e);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}else if(c.get(Content.CONTENT_TYPE).compareTo(Content.CONTENT_TYPE_MP3) == 0)
		{
			Log.i("MobiTrade", "Writing MP3 content: "+c.get(Content.CONTENT_NAME)+" to sdcard: "
					+mobiTradeSdcardPath+ c.get(Content.CONTENT_NAME));
			OutputStream outStream = null;

			File file = new File(mobiTradeSdcardPath, c.get(Content.CONTENT_NAME));
			Log.i("MobiTrade", "Writing Text content: "+c.get(Content.CONTENT_NAME)+" to sdcard: "
					+mobiTradeSdcardPath+ c.get(Content.CONTENT_NAME));
			try 
			{
				outStream = new FileOutputStream(file);
				outStream.write(c.getBinData());
				outStream.flush();
				outStream.close();
			} 
			catch (FileNotFoundException e) 
			{
				Log.e("MobiTrade", "Error occured while trying to write th received file to sdcard: "+e);
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 

			catch (IOException e) 
			{
				Log.e("MobiTrade", "Error occured while trying to write th received file to sdcard: "+e);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	// Saves a message text to the SDCARD
	public static void SaveMessageToMobiTradeSDcard(String fileName, String content)
	{

		OutputStream outStream = null;

		File file = new File(mobiTradeSdcardPath, fileName );
		try 
		{
			outStream = new FileOutputStream(file);
			outStream.write(content.getBytes());
			outStream.flush();
			outStream.close();
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static byte[] zipBytes(byte[] input)  {
		try {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(baos);
			zos.write(input);
			zos.closeEntry();
			zos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
			return null;
		}

	}

	public static byte[] unzipBytes(byte []zipped)
	{
		return null;
	}

	
	public static String getCurrentTime()
	{
		 Date dt = new Date();
         int hours = dt.getHours();
         int minutes = dt.getMinutes();
         int seconds = dt.getSeconds();
         String curTime = hours + ":"+minutes + ":"+ seconds;
         return curTime;
	}
}

