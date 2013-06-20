package com.afjk01.tool.WifiMultiTouchPad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ReceiveThread extends Thread
{
	public String mIP;
	private int mPort;
	private Handler mReceiveHandler;
	private boolean mStop;
	private ServerSocket mServerSoc;

	public ReceiveThread( int port, Handler mReceiveHandler )
	{
		mStop = false;
		mPort = port;
		this.mReceiveHandler = mReceiveHandler;
	}
	
	@Override
	public void run()
	{
		try {
			// ソケットを作成
			mServerSoc = new ServerSocket(mPort);
		
			//クライアントからの接続を待機するaccept()メソッド。
			//accept()は、接続があるまで処理はブロックされる。
			//もし、複数のクライアントからの接続を受け付けるようにするには
			//スレッドを使う。
			//accept()は接続時に新たなsocketを返す。これを使って通信を行なう。
			Socket socket=null;
			while(!mStop)
			{
				System.out.println("Waiting for Connection. ");
				socket = mServerSoc.accept();
//				Log.d( "[DEBUG]",socket.getInetAddress().getHostName());
				//接続があれば次の命令に移る。
//				System.out.println("Connect to " + socket.getInetAddress());
				
				//socketからのデータはInputStreamReaderに送り、さらに
				//BufferedReaderによってバッファリングする。
				
				mIP = socket.getInetAddress().toString();

				if( mIP.charAt(0) == '/' )
				{
					mIP = mIP.substring( 1, mIP.length());
				}
				
				BufferedReader reader = null;
				InputStreamReader in = null;
				in = new InputStreamReader(socket.getInputStream());
				reader = new BufferedReader( in );
				
				//読み取ったデータを表示する。
				String line = reader.readLine();
				
				if( line != null )
				{
					String rcvMsg = new String( line );
					/* イベント送信 */
					Message msg = new Message();

					Bundle data = new Bundle();
					data.putString( "Data", rcvMsg);
					data.putString( "IP", new String(mIP) );
					msg.setData( data );
					msg.what = MulitiTouchPadActivity.MESSAGE_RECEIVE;
					mReceiveHandler.sendMessage(msg);
				}
				in.close();
				reader.close();
			}
			mServerSoc.close();
			Log.d("debug", "End Receive Thread" );
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void setStopFlag() 
	{
		try {
			if( mServerSoc != null )
			{
				mServerSoc.close();
			}
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		mStop = true;
        interrupt();
	}

	public void waitForStop() 
	{
		setStopFlag();
		// スレッドが終わるのを待つ
		while (isAlive())
		{
			Log.d("debug", "Wainting End Receive Thread" );
		}
	}
}
