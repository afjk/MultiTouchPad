package com.afjk01.tool.WifiMultiTouchPad;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

import com.afjk01.tool.WifiMultiTouchPad.ServerSettingActivity.ScanServerHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
//import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ServerSettingActivity extends Activity 
{
//	private ArrayList<String> mIpList = new ArrayList<String>();
	private ArrayList<ServerBean> mServerList = new ArrayList<ServerBean>();
	private ServerSettingActivity mContext;
	private SharedPreferences mPref;
	
	// Dialog定義
	final private int DIALOG_ADD_IP_ID = 0;
	private ReceiveThread mRThread;
	private int mPortNum;
	private ScanServerHandler mScanServerHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = this;
		
		// 画面構築
		setContentView(R.layout.serversetting);
		
		ImageButton imgbtn = (ImageButton) findViewById(R.id.ImageButtonAdd);
		imgbtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				showDialog(DIALOG_ADD_IP_ID);
			}
		});
		
		ListView lv = (ListView)findViewById(R.id.listViewPC);
		
		lv.setOnItemClickListener( new OnItemClickListener(){
			// サーバIP選択時の処理。
			@Override
			public void onItemClick(AdapterView<?> parent, View view,int position, long id) 
			{
				/*
				String ipAddress = (String)ipList.get(position);
				Toast.makeText(mContext, ipAddress, Toast.LENGTH_SHORT).show();
				
				Intent intent = new Intent();
				intent.putExtra("Select_IP", ipAddress );
				*/
//				if( mIpList.size() == 0 )
				if( mServerList.size() == 0 )
				{
					showDialog(DIALOG_ADD_IP_ID);
				}
				else
				{
//					String ipAddress = (String)mIpList.get(position);
					String ipAddress = (String)mServerList.get(position).IP;
//					Toast.makeText(mContext, ipAddress, Toast.LENGTH_SHORT).show();
					
					Intent intent = new Intent();
					intent.putExtra("Select_IP_NO", position );
					intent.putExtra("Select_IP", ipAddress );
					setResult(RESULT_OK, intent); 

					if( mRThread != null )
					{
						mRThread.waitForStop();
						mRThread = null;
					}
					
					finish();
				}
			}
		});
		
		lv.setOnItemLongClickListener( new AdapterView.OnItemLongClickListener(){
			// 項目が長押しクリックされた時のハンドラ
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
			{
				final int delPos = position;
				
				AlertDialog.Builder builder = new AlertDialog.Builder(ServerSettingActivity.this);
				builder.setMessage( getString( R.string.delete_ip ) );
				builder.setCancelable(false);
				builder.setPositiveButton(getString( R.string.yes ), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
//						mIpList.remove( delPos );
						mServerList.remove( delPos );
						
						String[] ipListArray = null;
//						if( mIpList.size() > 0 )
						/*
						if( mServerList.size() > 0 )
						{
							ipListArray = (String[])mIpList.toArray(new String[0]);
						}
						else
						{
							ipListArray = new String[] { getString( R.string.add_server_ip ) };
						}
						
						ArrayAdapter<String> la = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, ipListArray);
						*/
						
						ServerAdapter la = null;
						if( mServerList.size() > 0 )
						{
							la = new ServerAdapter(mContext, R.layout.list_at, mServerList );
						}
						else
						{
							la = new ServerAdapter(mContext, R.layout.list_at, new ArrayList<ServerBean>(){{ add(new ServerBean(getString( R.string.add_server_ip ),"") ); } } );
						}
						ListView lv = (ListView)findViewById(R.id.listViewPC);
						lv.setAdapter(la);
						
						SaveIpToPreferences();
					}
				});
				builder.setNegativeButton(getString( R.string.no ), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

					}
				});
				
				AlertDialog alert = builder.create();
				alert.show();
				
				return true;
			}
		});
		
		// サーバーIP読み込み
		loadIpFromPreferences();
		/*
		String[] ipListArray = null;
		if( mIpList.size() > 0 )
		{
			ipListArray = (String[])mIpList.toArray(new String[0]);
		}
		else
		{
			ipListArray = new String[] { getString( R.string.add_server_ip ) };
		}
		ArrayAdapter<String> la = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, ipListArray);
		*/
//		ServerAdapter la = new ServerAdapter(mContext, R.layout.list_at, mServerList );
		ServerAdapter la = null;
		if( mServerList.size() > 0 )
		{
			la = new ServerAdapter(mContext, R.layout.list_at, mServerList );
		}
		else
		{
			la = new ServerAdapter(mContext, R.layout.list_at, new ArrayList<ServerBean>(){{ add(new ServerBean( getString( R.string.add_server_ip ) ,"") ); } } );
		}

		lv.setAdapter(la);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( this );
		try
		{
			mPortNum     = Integer.valueOf( sp.getString( "port", getString(R.string.default_port) ) );
		}
		catch( RuntimeException e )
		{
			mPortNum     = Integer.valueOf(getString(R.string.default_port));
		}
		if( mRThread == null )
		{
			mRThread = new ReceiveThread( mPortNum, mReceiveHandler );
			mRThread.start();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		
		switch( id )
		{
		case DIALOG_ADD_IP_ID:
			// サーバIP入力ダイアログの生成。
			
			// EditText部分のみ、AlertDialogにsetViewする方式と、ダイアログのレイアウトをxmlで作成し、レイアウトをsetViewする方式を検討。
			// 入力文字を得るため、後者を採用。
			/* 
			final EditText editTextView = new EditText(ServerSettingActivity.this);
			editTextView.setInputType( InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS );
//			editTextView.setKeyListener(new DigitsKeyListener(true,true) );
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Input IP Address")
				.setView( editTextView )
				.setPositiveButton("Add", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
						SpannableStringBuilder  IPAddress = (SpannableStringBuilder) editTextView.getText();
						String IpAddressString = IPAddress.toString();
						
						mIpList.add(IpAddressString);
						String[] ipListArray = (String[])mIpList.toArray(new String[0]);
						
						ArrayAdapter<String> la = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, ipListArray);

						ListView lv = (ListView)findViewById(R.id.listViewPC);
						lv.setAdapter(la);
						
						SaveIpToPreferences();
		           }
		       })
		       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
			dialog = builder.create();
			*/

			//レイアウトの呼び出し
			LayoutInflater factory = LayoutInflater.from(this);
			final View inputView = factory.inflate(R.layout.input_dialog, null);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView(inputView)
				.setPositiveButton(getString(R.string.add), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   final EditText editTextView = (EditText)inputView.findViewById( R.id.dialog_edittextIP );
						SpannableStringBuilder  IPAddress = (SpannableStringBuilder) editTextView.getText();
						String IpAddressString = IPAddress.toString();
						
						if( IpAddressString.length() > 0 )
						{
//							mIpList.add(IpAddressString);
//							String[] ipListArray = (String[])mIpList.toArray(new String[0]);
//							ArrayAdapter<String> la = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, ipListArray);
							mServerList.add( new ServerBean( IpAddressString, "" ) );
							ServerAdapter la = new ServerAdapter(mContext, R.layout.list_at, mServerList );
							
							ListView lv = (ListView)findViewById(R.id.listViewPC);
							lv.setAdapter(la);
							
							SaveIpToPreferences();
						}
		           }
		       })
		       .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });

			dialog = builder.create();
			
			// 自動でソフトキーボード表示
			dialog.setOnShowListener(new DialogInterface.OnShowListener() {  
			    @Override  
			    public void onShow(DialogInterface arg0) {  
			        EditText editText = (EditText)inputView.findViewById(R.id.dialog_edittextIP);  
			  
			        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);  
			        inputMethodManager.showSoftInput(editText, 0);  
			    }  
			});  
			
			break;
		default:
			dialog = null;
		}
		return dialog;
	}
	
	public void loadIpFromPreferences()
	{
//		mIpList.clear();
		mServerList.clear();
		
		// プリファレンスから値読みこみ
		mPref = getSharedPreferences("config", Context.MODE_PRIVATE);

		int i = 0;
		while( true )
		{
			String ipAddress = mPref.getString( "ipAddress" + String.valueOf( i ), "null");
			i++;
			
			if( ipAddress.equals("null"))
			{
				break;
			}
			else
			{
//				mIpList.add(ipAddress);
				mServerList.add( new ServerBean( ipAddress, "" ) );
			}
		}
	}

	public void SaveIpToPreferences()
	{
		// プリファレンスへ値書き込み
		SharedPreferences.Editor editor = mPref.edit();

		int i = 0;
		/*
		for( String val : mIpList )
		{
			editor.putString("ipAddress" + String.valueOf( i ), val );
			i++;
		}
		editor.putString("ipAddress" + String.valueOf(i), "null" );
		*/
		
		for( ServerBean val : mServerList )
		{
			editor.putString("ipAddress" + String.valueOf( i ), val.IP );
			i++;
		}
		editor.putString("ipAddress" + String.valueOf(i), "null" );
		editor.commit();
	}
	
    private void ScanServers() 
    {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        int netmask = dhcpInfo.netmask;
        
        int broadcastAddress = ~netmask | ipAddress;
        
        String strIPAddess = addressToString(ipAddress);
        String strNetmask = addressToString(netmask);
        String strBroadcastAddress = addressToString(broadcastAddress);
        
        Log.v("IP Address", strIPAddess);
        Log.v("Net Mask", strNetmask);
        Log.v("Broadcast Address", strBroadcastAddress);
        
        BroadCast( strBroadcastAddress );
    }
    
    private String addressToString( int address )
    {
        String strAddess =
                ((address >> 0) & 0xFF) + "." +
                ((address >> 8) & 0xFF) + "." +
                ((address >> 16) & 0xFF) + "." +
                ((address >> 24) & 0xFF);
        
        return strAddess;
    }
    
    private void BroadCast( String broadcastAddress )
    {
        InetSocketAddress isAddress = new InetSocketAddress( broadcastAddress, mPortNum );

        byte[] buffer = "/scanserver: ".getBytes();

        DatagramPacket packet;
		try {
			packet = new DatagramPacket( buffer, buffer.length, isAddress );
	        new DatagramSocket().send( packet );
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }
    
	private final Handler mReceiveHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
			case MulitiTouchPadActivity.MESSAGE_RECEIVE:
				// 受信。

				// メッセージの表示
				Bundle data = msg.getData();
				String dataStr = data.getString( "Data");
				String IPStr = data.getString( "IP");
				
//				Toast.makeText( mContext, dataStr, Toast.LENGTH_SHORT).show();
				Log.d("debug", dataStr);

				char wk = ' ';
				String command = "";
				for( int i = 0; i< dataStr.length(); ++i )
				{
					wk = dataStr.charAt(i);
					if( wk == ' ' )
					{
						// コマンドの区切り文字。
						// コマンド実行。
						ParthCommand(command, IPStr);
						command = "";
					}
					else
					{
						command += wk;
					}
				}
				break;
			}
		}
	};// mReceiveHandler
	
	
	private void ParthCommand(String command, String IPStr) 
	{
		if( command.charAt( 0) != '/' )
		{
			return;
		}
		String com = "";
		ArrayList<String> args = null;
		for( int i=0 ; i < command.length() ;i++ )
		{
			char wk = command.charAt( i );
			
			if( wk == ':' )
			{
				// 命令部取得終了。
				args = GetArgs( command.substring(i+1,command.length()) );
				break;
			}
			else
			{
				com += wk;
			}
		}
		ExecCommand( com, args, IPStr );
	}
	

	private ArrayList<String> GetArgs(String argstr) 
	{
		ArrayList<String> args = new ArrayList<String>();
		
		char wk = ' ';
		String wkStr = "";
		for( int i = 0; i< argstr.length(); i++ )
		{
			wk = argstr.charAt( i );
			if( wk == ',' )
			{
				args.add( wkStr);
				wkStr = "";
			}
			else
			{
				wkStr += wk;
			}
		}
		args.add( wkStr );
		return args;
	}
	private void ExecCommand(String com, ArrayList<String> args, String IPStr) 
	{
		if( com.equals("/hostname") )
		{
			
//			mIpList.add( args.get(0) + ":" +  IPStr );
//			String[] ipListArray = (String[])mIpList.toArray(new String[0]);
			
//			ArrayAdapter<String> la = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, ipListArray);

			// 登録済みIPであれば、情報更新。
			// 新規であれば、追加。
			int i = 0;
			for( i = 0; i< mServerList.size(); i++ )
			{
				ServerBean serverBean = mServerList.get(i);
				if( serverBean.IP.equals( IPStr ) )
				{
					serverBean.hostname = args.get( 0 );
					break;
				}
			}
			
			if( i >= mServerList.size() )
			{
				mServerList.add( new ServerBean( IPStr, args.get(0) ) );
			}
			
			ServerAdapter la = new ServerAdapter(mContext, R.layout.list_at, mServerList );
			
			ListView lv = (ListView)findViewById(R.id.listViewPC);
			lv.setAdapter(la);
			
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		//サーバースキャン。
		if( mScanServerHandler == null )
		{
			mScanServerHandler = new ScanServerHandler(1000);
			mScanServerHandler.start();
		}
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		Log.d("Debug","onPause");
		// レシーブスレッドを停止し、サーバ選択画面へ遷移。
		if( mRThread != null )
		{
			mRThread.waitForStop();
			mRThread = null;
		}
		mScanServerHandler.stop();
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
		Log.d("Debug","onStop");
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.d("Debug","onDestroy");
	}

    // ScanServerハンドラ
    class ScanServerHandler extends Handler
    {
    	private int delayTime;
    	
    	public ScanServerHandler( int delayTime )
    	{
    		this.delayTime = delayTime;
    	}
    	
    	public void start()
    	{
    		ScanServers();
    		this.sendMessageDelayed( obtainMessage(0), delayTime );
    	}
    	
    	public void stop()
    	{
    		delayTime = 0;
    	}
    	
    	public void handleMessage( Message msg )
    	{
    		if( delayTime == 0)
    		{
    			return;
    		}
    		ScanServers();
    		sendMessageDelayed( obtainMessage( 0), delayTime );
    	}
    } // class ScanServerHandler
}
