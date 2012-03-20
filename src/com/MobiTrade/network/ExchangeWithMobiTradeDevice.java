package com.MobiTrade.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.MobiTrade.sqlite.DatabaseHelper;

public class ExchangeWithMobiTradeDevice extends Thread {

	public static String TAG = "MobiTrade";

	private final BluetoothSocket mmSocket;
	private final InputStream mmInStream;
	private final OutputStream mmOutStream;
	private Parsing parser = null;
	private MobiTradeProtocol mobiTradeProtocol = null;
	
	

	public BluetoothSocket getSocket()
	{
		return mmSocket;
	}

	public ExchangeWithMobiTradeDevice(MobiTradeProtocol protocol, BluetoothSocket socket) 
	{
		mobiTradeProtocol = protocol;

		mmSocket = socket;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;
		// Get the input and output streams, using temp objects because
		// member streams are final
		try 
		{
			tmpIn = mmSocket.getInputStream();
			tmpOut = mmSocket.getOutputStream();
		} 
		catch (IOException e) 
		{
			Log.e(TAG,
			"Cannot get input and output streams from socket");
			mobiTradeProtocol.setExceptionOccuredOnSession(mmSocket.getRemoteDevice());
		}

		mmInStream = tmpIn;
		mmOutStream = tmpOut;
		parser = new Parsing(protocol, this);
	}

	public void run() {
		byte[] buffer = new byte[1024]; // buffer store for the stream
		int bytes; // bytes returned from read()

		// Keep listening to the InputStream until an exception occurs
		while (true) 
		{
			try 
			{
				// Read from the InputStream
				bytes = mmInStream.read(buffer);
				// Storing the list of received channels in the map and
				// identifying the matching contents
				if(bytes > 0)
				{
					mobiTradeProtocol.UpdateLastInteractionWith(mmSocket.getRemoteDevice().getAddress());

					String toParse = new String(buffer, 0, bytes);
					parser.parseMsg(toParse);
				}
			}

			catch (IOException e) 
			{
				Log.e(TAG,
						"Error occured while reading data from socket: "+mmSocket.getRemoteDevice().getName()+" "+ e);
				e.printStackTrace();
				
				mobiTradeProtocol.setExceptionOccuredOnSession(mmSocket.getRemoteDevice());
				break;
			}
		}
	}

	/* Call this from the main Activity to send data to the remote device */
	public synchronized void write(byte[] bytes) {
		try {
			mmOutStream.write(bytes);
			mobiTradeProtocol.UpdateLastInteractionWith(mmSocket.getRemoteDevice().getAddress());

		} catch (IOException e) {
			Log.e(TAG,
					"Error occured while writing data to socket: "+mmSocket.getRemoteDevice().getName()+" "+e);
			
			e.printStackTrace();
			mobiTradeProtocol.setExceptionOccuredOnSession(mmSocket.getRemoteDevice());

		}
	}

	public synchronized void writeFromFile(String filePath) {
		try 
		{
			File f = new File(filePath);
			int dataLength = (int)f.length();

			FileInputStream in = new FileInputStream(filePath);
			DatabaseHelper.PumpFile(in, mmOutStream, dataLength);

			in.close();
			if(!f.delete())
			{
				Log.e(TAG, "Unable to delete tmp file, after sending its content: "+filePath);
			}

			mobiTradeProtocol.UpdateLastInteractionWith(mmSocket.getRemoteDevice().getAddress());

		} catch (IOException e) {
			Log.e(TAG,
					"Error occured while writing data to socket: "+mmSocket.getRemoteDevice().getName()+" "+e);
			
			e.printStackTrace();
			mobiTradeProtocol.setExceptionOccuredOnSession(mmSocket.getRemoteDevice());

		}
	}

	/* Call this from the main Activity to shutdown the connection */
	public void cancel() 
	{
		try 
		{	
			if(parser != null)
			{	
				parser.cancel();
				parser = null;
			}

			mmInStream.close();
			mmOutStream.close();

			if(mmSocket != null)
				mmSocket.close();
		} 
		catch (IOException e) {
			Log.e(TAG, "Error occured while trying to close socket following a cancelling problem");
			e.printStackTrace();
			mobiTradeProtocol.setExceptionOccuredOnSession(mmSocket.getRemoteDevice());
		}
	}
}
