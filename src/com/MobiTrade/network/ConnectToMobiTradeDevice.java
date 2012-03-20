package com.MobiTrade.network;

import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.MobiTrade.MobiTradeMain;

public class ConnectToMobiTradeDevice extends Thread {

	public static String TAG = "MobiTrade";

	private final BluetoothSocket mmSocket;
	private final BluetoothDevice mmDevice;
	private MobiTradeProtocol mobitradeProtocol = null;
	
	public ConnectToMobiTradeDevice(MobiTradeProtocol protocol, BluetoothDevice device) 
	{
		mobitradeProtocol = protocol;
		// Use a temporary object that is later assigned to mmSocket,
		// because mmSocket is final
		BluetoothSocket tmp = null;
		mmDevice = device;

		// Get a BluetoothSocket to connect with the given BluetoothDevice
		try {
			tmp = device.createRfcommSocketToServiceRecord(MobiTradeMain.MobiTrade_UUID);
		} 

		catch (Exception e) 
		{

			try 
			{
				if(tmp != null)
					tmp.close();
			} 
			catch (IOException e1) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Closing un successful socket: "+e);
				e1.printStackTrace();

			}
			Log.e(TAG,"Error occured while creating bluetooth socket for connection: "+ e);

		} 

		mmSocket = tmp;
	}

	public void run() {

		if(mmSocket != null)
		{
			try 
			{
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();

			} 
			catch (IOException connectException) 
			{
				// Unable to connect; close the socket and get out
				Log.e(TAG,
						"Unable to connet to: "+mmDevice.getName()+", trying to close the socket: "
						+ connectException.getMessage());
				
				connectException.printStackTrace();

				
				mobitradeProtocol.setExceptionOccuredOnSession(mmSocket.getRemoteDevice());
				
				return;
			}

			// Do work to manage the connection (in a separate thread)
			manageConnectedSocket(mmSocket);
		}else
		{
			mobitradeProtocol.setExceptionOccuredOnSession(mmSocket.getRemoteDevice());
		}
	}

	public void manageConnectedSocket(BluetoothSocket sock) 
	{
		String adr = mmDevice.getAddress();
		mobitradeProtocol.SetExchangeThreadForSession(adr, new ExchangeWithMobiTradeDevice(mobitradeProtocol, sock));

		// Start the exchanging thread to read the received messages
		// Sending the list of channels

		mobitradeProtocol.StartExchangeThreadForSession(adr);
		mobitradeProtocol.WriteListOfChannelsToExchangeThreadForSession(adr);
	}

	public void cancel()
	{
		try {
			if(mmSocket != null)
				mmSocket.close();

		} catch (IOException e) {
			Log.e(TAG, "Cancelling ongoing connection: "+e);
			// TODO Auto-generated catch block
			e.printStackTrace();
			mobitradeProtocol.setExceptionOccuredOnSession(mmSocket.getRemoteDevice());
		}
	}

}

