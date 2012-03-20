package com.MobiTrade.network;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.MobiTrade.MobiTradeMain;

public class AcceptBluetoothConnection extends Thread {

	private  BluetoothServerSocket mmServerSocket;
	public static String TAG = "MobiTrade";

	private int serverState = 0;
	private MobiTradeProtocol mobitradeProtocol = null;
	
	public AcceptBluetoothConnection(MobiTradeProtocol protocol) {
		mobitradeProtocol = protocol;
		// Use a temporary object that is later assigned to mmServerSocket,
		// because mmServerSocket is final
		BluetoothServerSocket tmp = null;
		try {
			// MY_UUID is the app's UUID string, also used by the client
			// code
			tmp = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(
					MobiTradeMain.SERVICE_NAME, MobiTradeMain.MobiTrade_UUID);
		} catch (IOException e) 
		{
			Log.e(TAG, "BluetoothServerSocker: " + e);
		}
		mmServerSocket = tmp;
	}

	@Override
	public void run() {
		BluetoothSocket socket = null;
		// Keep listening until exception occurs or a socket is returned
		while (true) 
		{

			try 
			{
				if(mmServerSocket == null)
					break;
				serverState = 1;
				Log.i(TAG,
				"Listening for incomming bluetooth connections");
				socket = mmServerSocket.accept();
			} 

			catch (IOException e) {
				Log.e(TAG, "Exception on bluetooth socket Accept: "
						+ e);
				e.printStackTrace();
				serverState = 0;
				
				mmServerSocket = null;
				
				break;
			}

			// If a connection was accepted
			if (socket != null) {

				// Do work to manage the connection (in a separate thread)
				Log.i(TAG, "New connection accepted from: "+ socket.getRemoteDevice().getName());
				manageAcceptedConnection(socket);
				// Each time we accept only one connection
			}
		}
	}

	private synchronized void manageAcceptedConnection(BluetoothSocket sock) 
	{
		BluetoothDevice remoteDev = sock.getRemoteDevice();
		String remoteAdr = remoteDev.getAddress();

		if (!mobitradeProtocol.isSessionActive(remoteDev) && mobitradeProtocol.AreMoreSessionsAllowed())
		{
			mobitradeProtocol.startNewSession(sock.getRemoteDevice());
			
			mobitradeProtocol.SetExchangeThreadForSession(remoteAdr, new ExchangeWithMobiTradeDevice(mobitradeProtocol, sock));
			mobitradeProtocol.StartExchangeThreadForSession(remoteAdr);

		}else
		{
			try {
				sock.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Trying to close a new socket for an already running session: "+e);
				e.printStackTrace();
			}
		}

	}

	public synchronized int isRunning()
	{
		return serverState;
	}
	
	/** Will cancel the listening socket, and cause the thread to finish */
	public synchronized void cancel() 
	{
		try {
			Log.i(TAG, "Closing the bluetooth server socket");
			if(mmServerSocket != null)
				mmServerSocket.close();
			
			mobitradeProtocol = null;
		} catch (IOException e) {
			Log.e(TAG, "Bluetooth listening server cancelled.");
		}
	}
}
