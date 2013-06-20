package com.afjk01.tool.WifiMultiTouchPad;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MulitiTouchPadActivity extends Activity 
implements AnimationListener
{
	public static final int MESSAGE_SEND = 1;
	public static final int MESSAGE_SHOW_INPUT = 2;
	public static final int MESSAGE_HIDE_INPUT = 3;
	public static final int MESSAGE_RECEIVE = 4;
	public static final int MESSAGE_HINT_MESSAGE = 5;
	
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// 設定アクティビティ起動用リクエストコード
	private static final int SHOW_WELLCOME = 0;
	private static final int SHOW_SETTING = 1;
	public static final int SHOW_SERVER_SETTING = 2;
	
	// オプションメニューID
	private static final int OPT_SERVER_SETTING = Menu.FIRST;
	private static final int OPT_SETTING = Menu.FIRST + 1;

	private Context mContext;
	public String mIP = "";
	public String mHostName = "";
	private String mOSName = "";
	private String mOSArch = "";
	private String mOSVersion = "";
	
	private ReceiveThread mRThread = null;
	private ClipboardManager mClipboardManager;

	private ArrayList<String> mIpList = new ArrayList<String>();
	
	private MouseView mMouseView;
	
	private float mInputShowAnimationVal = 1.0f;
	private long  mInputShowAnimationTime = 0;
	private boolean mFirstBoot = true;

	// config
	static SharedPreferences mSharedPreferences;
	
	public boolean mFullScreen;
	public boolean mAutoConnect;
	public int mPortNum;
	public int mMouseSense;
	public int mScrollSense;
	public boolean mScrollDirection;
	public boolean mLefty;
	public String mPreIP;
	
	private boolean mIsKeyInputVisible = false;
	private boolean mToKeyInputVisible = false;

	// キーコード定義
	final int VK_C = 67;
	final int VK_V = 86;
	final int VK_CONTROL = 17;
	final int VK_BACKSPASE = 8;
	final int VK_ENTER = 10;
	final int VK_SPACE = 32;
	final int VK_LEFT = 37;
	final int VK_TOP = 38;
	final int VK_RIGHT = 39;
	final int VK_BOTTOM = 40;
	private boolean mSharedClipboard;
	private boolean mShowHint;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		Log.d("Debug","onCreate");

		mContext = this;

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
		loadConfig();
		
		Window windows = getWindow();
		// 表示状態を常にON
		windows.addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
		// ピクトエリアを非表示にしてフルスクリーン
		if( mFullScreen == true )
		{
			windows.addFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN );
		}
		// タイトルバー非表示
		requestWindowFeature( Window.FEATURE_NO_TITLE);

		FrameLayout  layout = new FrameLayout (mContext);
		mMouseView = new MouseView(this, mAppHandler);
		layout.addView( mMouseView );
		
		View layoutView = ((Activity)mContext).getLayoutInflater().inflate(R.layout.main, null );
		layout.addView( layoutView );
		
		setContentView(layout);
		
		// EditTextで送信された際の処理。
		EditText editText = (EditText) findViewById( R.id.editTextMessage );
		editText.setOnEditorActionListener( new OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
//				if( ( actionId == EditorInfo.IME_ACTION_SEND) || ( actionId == EditorInfo.IME_ACTION_UNSPECIFIED) )
				if( ( actionId == EditorInfo.IME_ACTION_SEND) || ( event.getAction() == KeyEvent.ACTION_UP) )
				{
					EditText text = (EditText) findViewById(R.id.editTextMessage);
					String MsgString = text.getText().toString();
					
					if( MsgString.length() > 0 )
					{
						// URLエンコードする。
						String encstr;
						try {
							encstr = URLEncoder.encode(MsgString, "UTF-8");
							sendMessage("/text:" + encstr + " ");
						} catch (UnsupportedEncodingException e) {

							e.printStackTrace();
						}
						text.setText("");
						Toast.makeText( mContext, getString(R.string.send_message), Toast.LENGTH_SHORT).show();
						// ソフトウェアキーボードを閉じる
						//InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						//inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
						
					}
					else
					{
//						if( actionId == EditorInfo.IME_ACTION_SEND )
						{
						// 文字が入力されていない場合、ソフトキーのイベントを直接PCに送りたい。
						// 改行とか、左右とか、DELとか。
						
						// 文字が入力されていなければ、改行とする。
							sendMessage( "/keypress:"   + String.valueOf( VK_ENTER) + " " );
							sendMessage( "/keyrelease:" + String.valueOf( VK_ENTER) + " " );
						}
					}
				}
				return true;
			}
		});

		ImageButton imgbtn = (ImageButton) findViewById(R.id.ImageButtonAdd);
		imgbtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				//---------------------------------------------------------------
				// メッセージボックスの表示
				// 暫定で、高さを100としている。
				// これは、初回のみ、LinearLayoutが表示されていないため、getHeight()の結果が0になるため。
				
				mIsKeyInputVisible = true;
				changeKeyInputVisibility();

				LinearLayout linearLayoutMessage = (LinearLayout) findViewById(R.id.linearLayoutMessage);
				int height = linearLayoutMessage.getHeight();
				
				TranslateAnimation translate = new TranslateAnimation(0, 0, -100, 0);
				translate.setDuration(500);
				translate.setFillAfter( true );
				translate.setAnimationListener(MulitiTouchPadActivity.this);
				mToKeyInputVisible = true;
				
				//アニメーションスタート
				linearLayoutMessage.startAnimation(translate);
				
				//---------------------------------------------------------------
				// キーボタンの表示
				LinearLayout linearLayoutKeyButtons = (LinearLayout) findViewById(R.id.linearLayoutKeyButtons);

				translate = new TranslateAnimation(0, 0, 100, 0);
				translate.setDuration(500);
				translate.setFillAfter( true );
				translate.setAnimationListener(MulitiTouchPadActivity.this);

				//アニメーションスタート
				linearLayoutKeyButtons.startAnimation(translate);
				
				//---------------------------------------------------------------
				// 追加ボタンの消去
				ImageButton imageButton = (ImageButton) findViewById(R.id.ImageButtonAdd);
				
				AlphaAnimation alpha_animation = new AlphaAnimation(100f, 0.0f);
				alpha_animation.setDuration(500);
				alpha_animation.setFillAfter( true );

				//アニメーションスタート
				imageButton.startAnimation(alpha_animation);
			}
		});
		
		// キーボタン定義
		setBottonAction();
		
		mClipboardManager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
		loadIpFromPreferences();
		
	}

	private void setBottonAction() 
	{
		/*
		Button button = (Button)findViewById(R.id.button1);

		button.setOnTouchListener( new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// TODO 自動生成されたメソッド・スタブ
				if( event.getAction() == MotionEvent.ACTION_DOWN )
				{
					sendMessage( "/keypress:"   + String.valueOf( VK_CONTROL) + " " );
					sendMessage( "/keypress:"   + String.valueOf( VK_C) + " " );
				}
				else
				{
					sendMessage( "/keyrelease:" + String.valueOf( VK_C) + " " );
					sendMessage( "/keyrelease:" + String.valueOf( VK_CONTROL) + " " );
				}
				return false;
			}
		});

		button = (Button)findViewById(R.id.button2);
		button.setOnTouchListener( new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// TODO 自動生成されたメソッド・スタブ
				if( event.getAction() == MotionEvent.ACTION_DOWN )
				{
					sendMessage( "/keypress:"   + String.valueOf( VK_CONTROL) + " " );
					sendMessage( "/keypress:"   + String.valueOf( VK_V) + " " );
				}
				else
				{
					sendMessage( "/keyrelease:" + String.valueOf( VK_V) + " " );
					sendMessage( "/keyrelease:" + String.valueOf( VK_CONTROL) + " " );
				}
				return false;
			}
		});

		button = (Button)findViewById(R.id.button3);
		button.setOnTouchListener( new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// TODO 自動生成されたメソッド・スタブ
				if( event.getAction() == MotionEvent.ACTION_DOWN )
				{
					sendMessage( "/keypress:"   + String.valueOf( VK_SPACE) + " " );
				}
				else
				{
					sendMessage( "/keyrelease:" + String.valueOf( VK_SPACE) + " " );
				}
				return false;
			}
		});

		button = (Button)findViewById(R.id.button4);
		button.setOnTouchListener( new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// TODO 自動生成されたメソッド・スタブ
				if( event.getAction() == MotionEvent.ACTION_DOWN )
				{
					sendMessage( "/keypress:"   + String.valueOf( VK_BACKSPASE) + " " );
				}
				else
				{
					sendMessage( "/keyrelease:" + String.valueOf( VK_BACKSPASE) + " " );
				}
				return false;
			}
		});
		
		button = (Button)findViewById(R.id.button5);
		button.setOnTouchListener( new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// TODO 自動生成されたメソッド・スタブ
				if( event.getAction() == MotionEvent.ACTION_DOWN )
				{
					sendMessage( "/keypress:"   + String.valueOf( VK_ENTER) + " " );
				}
				else
				{
					sendMessage( "/keyrelease:" + String.valueOf( VK_ENTER) + " " );
				}
				return false;
			}
		});

		*/
		Button button = (Button)findViewById(R.id.button1);
		button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) 
			{
				sendMessage( "/keypress:"   + String.valueOf( VK_CONTROL) + " " );
				sendMessage( "/keypress:"   + String.valueOf( VK_C) + " " );
				sendMessage( "/keyrelease:" + String.valueOf( VK_C) + " " );
				sendMessage( "/keyrelease:" + String.valueOf( VK_CONTROL) + " " );
			}
		});

		button = (Button)findViewById(R.id.button2);
		button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) 
			{
				sendMessage( "/keypress:"   + String.valueOf( VK_CONTROL) + " " );
				sendMessage( "/keypress:"   + String.valueOf( VK_V) + " " );
				sendMessage( "/keyrelease:" + String.valueOf( VK_V) + " " );
				sendMessage( "/keyrelease:" + String.valueOf( VK_CONTROL) + " " );
			}
		});

		button = (Button)findViewById(R.id.button3);
		button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) 
			{
				sendMessage( "/keypress:"   + String.valueOf( VK_SPACE) + " " );
				sendMessage( "/keyrelease:" + String.valueOf( VK_SPACE) + " " );
			}
		});

		button = (Button)findViewById(R.id.button4);
		button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) 
			{
				sendMessage( "/keypress:"   + String.valueOf( VK_BACKSPASE) + " " );
				sendMessage( "/keyrelease:" + String.valueOf( VK_BACKSPASE) + " " );
			}
		});
		
		button = (Button)findViewById(R.id.button5);
		button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) 
			{
				sendMessage( "/keypress:"   + String.valueOf( VK_ENTER) + " " );
				sendMessage( "/keyrelease:" + String.valueOf( VK_ENTER) + " " );
			}
		});
	}

	private void loadConfig()
	{
//		mPref = getSharedPreferences("config", Context.MODE_PRIVATE);
		
//		mFullScreen	  = mPref.getBoolean( "FULL_SCREEN", false );
		mFullScreen      = mSharedPreferences.getBoolean( "full_screen", false );
//		mAutoConnect     = mPref.getBoolean( "AUTO_CONNECT", false );
		mAutoConnect     = mSharedPreferences.getBoolean( "auto_connect", false );
		try
		{
			mPortNum     = Integer.valueOf( mSharedPreferences.getString( "port", getString(R.string.default_port) ) );
		}
		catch( RuntimeException e )
		{
			mPortNum     = Integer.valueOf(getString(R.string.default_port));
		}
//		mMouseSense      = mPref.getInt( "MOUSE_SENSE", 50 );
		mMouseSense      = mSharedPreferences.getInt( "mouse_sense", 50 );
//		mScrollSense     = mPref.getInt( "SCROLL_SENSE", 50 );
		mScrollSense     = mSharedPreferences.getInt( "scroll_sense", 50 );
//		mScrollDirection = mPref.getBoolean( "SCROLL_DIRECTION", false );
//		mLefty           = mPref.getBoolean( "LEFTY", false );
//		mPreIP           = mPref.getString( "PRE_IP", "" );
		mPreIP           = mSharedPreferences.getString( "PRE_IP", "" );
		
		mSharedClipboard = mSharedPreferences.getBoolean( "shared_clipboard", true );
		
		mShowHint = mSharedPreferences.getBoolean( "show_hit", true );
	}
	@Override
	public void onStart()
	{
		super.onStart();
		Log.d("Debug","onStart");
	}

	@Override
	public void onResume()
	{
		super.onResume();
		Log.d("Debug","onResume");
		
		// 初回起動
		if( mFirstBoot  == true )
		{
			Intent intent = new Intent(MulitiTouchPadActivity.this, WellcomeActivity.class);
			// 次画面のアクティビティ起動
			startActivityForResult(intent, SHOW_WELLCOME);
		}
		else
		{

			if( mRThread == null )
			{
				mRThread = new ReceiveThread( mPortNum, mReceiveHandler );
				mRThread.start();
			}

			if( mIP.equals("") )
			{
				if( mAutoConnect == true )
				{
					mIP = mPreIP;
					sendMessage( "/connect: " );
				}
			}
			
			// WiFi接続
			WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);

			if( wifi.isWifiEnabled() )
			{
				if( mIP.equals("") )
				{
					// レシーブスレッドを停止し、サーバ選択画面へ遷移。
					if( mRThread != null )
					{
						mRThread.waitForStop();
						mRThread = null;
					}
					
					Intent intent = new Intent(MulitiTouchPadActivity.this, ServerSettingActivity.class);
					// 次画面のアクティビティ起動
					startActivityForResult(intent, SHOW_SERVER_SETTING);
				}
			}
			else
			{
				/* Wi-Fiを有効にする。
				if(wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLING)
				{
					wifi.setWifiEnabled(true);
				}
				*/
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(getString(R.string.wifi_connection) );//Wi-Fi接続を有効にしてください。
				builder.setCancelable(false);
				builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) 
					{
						Intent intent = new Intent("android.intent.action.MAIN");
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						if(android.os.Build.VERSION.SDK_INT >= 11){
						    //Honeycomb
						    intent.setClassName("com.android.settings"
						            , "com.android.settings.Settings$WifiSettingsActivity");
						}else{
						    //other versions
						    intent.setClassName("com.android.settings"
						            , "com.android.settings.wifi.WifiSettings");
						}
						startActivity(intent);
						dialog.cancel();
						
					}
				});
				builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						MulitiTouchPadActivity.this.finish();
					}
				});
				
				AlertDialog alert = builder.create();
				
				alert.show();
			}
		}
		
		changeKeyInputVisibility();
		
		TextView TextViewHintMessage = (TextView) findViewById( R.id.textViewHintMessage );
		if( mShowHint == true )
		{
			TextViewHintMessage.setVisibility(View.VISIBLE);
		}
		else
		{
			TextViewHintMessage.setVisibility(View.INVISIBLE);
		}
		
		// 縦横情報の取得。
		String orientation = "";
		if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) 
		{ 
			orientation = "landscape"; 
		} 
		else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) 
		{ 
			orientation = "portrait"; 
		}
	}
	
	private void changeKeyInputVisibility()
	{
		// キー入力の表示/非表示
		if( mIsKeyInputVisible == true )
		{
			LinearLayout lv = (LinearLayout)findViewById( R.id.linearLayoutMessage);
			lv.setVisibility(View.VISIBLE);
			View v = findViewById( R.id.editTextMessage);
			v.setVisibility(View.VISIBLE);
			v.setEnabled( true );
			
			lv = (LinearLayout)findViewById( R.id.linearLayoutKeyButtons);
			lv.setVisibility(View.VISIBLE);
			lv.setEnabled( true );

			Button button = (Button)findViewById(R.id.button1);
			button.setVisibility(View.VISIBLE);
			button.setEnabled( true );
			button = (Button)findViewById(R.id.button2);
			button.setVisibility(View.VISIBLE);
			button.setEnabled( true );
			button = (Button)findViewById(R.id.button3);
			button.setVisibility(View.VISIBLE);
			button.setEnabled( true );
			button = (Button)findViewById(R.id.button4);
			button.setVisibility(View.VISIBLE);
			button.setEnabled( true );
			button = (Button)findViewById(R.id.button5);
			button.setVisibility(View.VISIBLE);
			button.setEnabled( true );
			
//			View lvSub = ((Activity)mContext).getLayoutInflater().inflate(R.layout.buttons, null );
//			lv.addView( lvSub );
		}
		else
		{
			LinearLayout lv = (LinearLayout)findViewById( R.id.linearLayoutMessage);
			lv.setVisibility(View.GONE);
			View v = findViewById( R.id.editTextMessage);
			v.setVisibility(View.GONE);
			v.setEnabled( false );
			
			lv = (LinearLayout)findViewById( R.id.linearLayoutKeyButtons);
			lv.setVisibility(View.GONE);
			lv.setEnabled( false );
			
			Button button = (Button)findViewById(R.id.button1);
			button.setVisibility(View.GONE);
			button.setEnabled( false );
			button = (Button)findViewById(R.id.button2);
			button.setVisibility(View.GONE);
			button.setEnabled( false );
			button = (Button)findViewById(R.id.button3);
			button.setVisibility(View.GONE);
			button.setEnabled( false );
			button = (Button)findViewById(R.id.button4);
			button.setVisibility(View.GONE);
			button.setEnabled( false );
			button = (Button)findViewById(R.id.button5);
			button.setVisibility(View.GONE);
			button.setEnabled( false );
			
//			LinearLayout lvSub = (LinearLayout)findViewById( R.id.linearLayoutKeyButtonsTate);
//			lv.removeView( lvSub );
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
		Log.d("Debug","onPause");
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
		// レシーブスレッドが生きていたら、殺す。
		if( mRThread != null )
		{
			mRThread.waitForStop();
			mRThread = null;
		}
		
	}
	
	/**
	 * Sends a message.
	 * @param message  A string of text to send.
	 */
	private boolean sendMessage(String ipStr, String message) 
	{
		boolean result = true;
		
		if( true )
		{
			// TCP送信
			SendThread sThread = new SendThread( ipStr, mPortNum, message );
			sThread.start();
		}
		else
		{
			// UDP送信
	        InetSocketAddress isAddress = new InetSocketAddress( ipStr, mPortNum );
	
	        byte[] buffer = message.getBytes();
	
	        DatagramPacket packet;
			try {
				packet = new DatagramPacket( buffer, buffer.length, isAddress );
				DatagramSocket datagramSocket = new DatagramSocket();
				datagramSocket.send( packet );
				datagramSocket.close();
		        
			} catch (SocketException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		
		return result;
		/*
		boolean result = true;
		
		try {

			//アドレス情報を保持するsocketAddressを作成。
			InetSocketAddress socketAddress = new InetSocketAddress(ipStr, mPortNum);

			//socketAddressの値に基づいて通信に使用するソケットを作成する。
			Socket socket = new Socket();
			//タイムアウトは10秒(10000msec)
			socket.connect(socketAddress, 10000);

			//接続先の情報を入れるInetAddress型のinadrを用意する。
			InetAddress inadr;

			//inadrにソケットの接続先アドレスを入れ、nullである場合には
			//接続失敗と判断する。
			//nullでなければ、接続確立している。
			if ((inadr = socket.getInetAddress()) != null) 
			{
				System.out.println("Connect to " + inadr);
			}
			else 
			{
				System.out.println("Connection failed.");
				throw new IOException();
			}

			//PrintWriter型のwriterに、ソケットの出力ストリームを渡す。
			PrintWriter writer = new PrintWriter(socket.getOutputStream());

			//ソケットから出力する。
			writer.println(message);
			//終了処理
			writer.close();
			socket.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
			result = false;
		}
		return result;
		*/
	}
	
	private void sendMessage(String message) 
	{
		if( mIP.equals("") )
		{
			return;
		}
		
		if( sendMessage( mIP, message ) == false )
		{
			mIP = "";
		}
	}
	
	private final Handler mAppHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
				case MESSAGE_SEND:
				{
					// 送信。
	
					// メッセージの表示
					Bundle data = msg.getData();
					String posStr = data.getString( "Pos");
					
					MulitiTouchPadActivity act = (MulitiTouchPadActivity)mContext; 
					act.sendMessage(posStr);
					break;
				}
				case MESSAGE_SHOW_INPUT:
				{
					if( mIsKeyInputVisible == true )
					{
						LinearLayout linearLayoutMessage = (LinearLayout) findViewById(R.id.linearLayoutMessage);
						LinearLayout linearLayoutKeyButtons = (LinearLayout) findViewById(R.id.linearLayoutKeyButtons);
	
						long nowTime = System.currentTimeMillis();
						long dTime   = nowTime - mInputShowAnimationTime;
						
						mInputShowAnimationVal -= dTime / 500;
						
						if( mInputShowAnimationVal < 0.0f )
						{
							mInputShowAnimationVal = 0.0f;
						}
						mInputShowAnimationTime = nowTime;
						
						AlphaAnimation animation = new AlphaAnimation( mInputShowAnimationVal, 1.0f);
						animation.setDuration(500);
						animation.setFillAfter( true );
						linearLayoutMessage.startAnimation(animation);
						linearLayoutKeyButtons.startAnimation(animation);
					}
					break;
				}
				case MESSAGE_HIDE_INPUT:
				{
					if( mIsKeyInputVisible == true )
					{
						LinearLayout linearLayoutMessage = (LinearLayout) findViewById(R.id.linearLayoutMessage);
						LinearLayout linearLayoutKeyButtons = (LinearLayout) findViewById(R.id.linearLayoutKeyButtons);
						
						long nowTime = System.currentTimeMillis();
						long dTime   = nowTime - mInputShowAnimationTime;
						
						mInputShowAnimationVal += dTime / 500;
						if( mInputShowAnimationVal > 1.0f )
						{
							mInputShowAnimationVal = 1.0f;
						}
						mInputShowAnimationTime = nowTime;
						
						AlphaAnimation animation = new AlphaAnimation(mInputShowAnimationVal, 0.0f);
						animation.setDuration(500);
						animation.setFillAfter( true );
						
						linearLayoutMessage.startAnimation(animation);
						linearLayoutKeyButtons.startAnimation(animation);
					}
					break;
				}
				case MESSAGE_HINT_MESSAGE:
				{
					Bundle data = msg.getData();
					String stateStr = data.getString( "State");
					String hintMessage = data.getString( "HintMessage");
					TextView TextViewHintMessage = (TextView) findViewById( R.id.textViewHintMessage );
					TextViewHintMessage.setText( hintMessage );
					break;
				}
			}
		}
	};// mKeyHandler

	private final Handler mReceiveHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
			case MESSAGE_RECEIVE:
				// 受信。

				// メッセージの表示
				Bundle data = msg.getData();
				String dataStr = data.getString( "Data");
				
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
						ParthCommand(command);
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
	};// mKeyHandler
	
	private void ParthCommand(String command) 
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
		ExecCommand( com, args );
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
	private void ExecCommand(String com, ArrayList<String> args) 
	{
		if( com.equals("/rightover") )
		{
			/*
			// プリファレンスから値読みこみ
			mPref = getSharedPreferences("config", Context.MODE_PRIVATE);

			String ipStr = mPref.getString( "ipAddress" + String.valueOf( mIpPos + 1 ), "null");
			
			if( ipStr.equals("null"))
			{
				// PCの移動はなかったことに。
			}
			else
			{
				mIpPos += 1;
				mIP = ipStr;
			}
			*/
		}
		else if( com.equals("/leftover") )
		{
			/*
			// プリファレンスから値読みこみ
			mPref = getSharedPreferences("config", Context.MODE_PRIVATE);

			String ipStr = mPref.getString( "ipAddress" + String.valueOf( mIpPos - 1 ), "null");
			
			if( ipStr.equals("null"))
			{
				// PCの移動はなかったことに。
			}
			else
			{
				mIpPos -= 1;
				mIP = ipStr;
			}
			*/
		}
		else if( com.equals("/clipboard"))
		{
			if( mSharedClipboard == true )
			{
				String str;
				try {
					str = URLDecoder.decode(args.get(0), "UTF-8");
	//				Toast.makeText( mContext, str, Toast.LENGTH_SHORT).show();
					mClipboardManager.setText( str );
					
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			
			/* とりあえずコメント。
			// mIP以外のPCにクリップボード情報を伝播する。
			for( String ipStr : ipList )
			{
				if( ipStr.equals( mIP ))
				{
				}
				else
				{
					sendMessage( ipStr, "/clipboard:" + args.get(0) + " " );
				}
			}
			*/
		}
		else if( com.equals("/hostname"))
		{
			try {
				mHostName  = URLDecoder.decode(args.get(0), "UTF-8");
				
				Log.d("[Debug]", mHostName );
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
		else if( com.equals("/host_info"))
		{
			String ServerVersion = "";
			try {
				mHostName   = URLDecoder.decode(args.get(0), "UTF-8");
				mOSName     = URLDecoder.decode(args.get(1), "UTF-8");
				mOSArch     = URLDecoder.decode(args.get(2), "UTF-8");
				mOSVersion  = URLDecoder.decode(args.get(3), "UTF-8");
				if( args.size() > 4 )
				{
					ServerVersion =  URLDecoder.decode(args.get(4), "UTF-8");
				}
				Log.d("[Debug]", mHostName );
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			catch ( IndexOutOfBoundsException e )
			{
				e.printStackTrace();
			}
		}
	}
	// Optionメニュー
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);
		menu.add(0 , Menu.FIRST ,Menu.NONE , getString(R.string.server_select) );
		menu.add(0 , Menu.FIRST+1 ,Menu.NONE , getString(R.string.setting) );
		return ret;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch( item.getItemId() )
		{
			case OPT_SERVER_SETTING:
			{
				// レシーブスレッドを停止し、サーバ選択画面へ遷移。
				mRThread.waitForStop();
				mRThread = null;
				
				// サーバ設定アクティビティ起動
				Intent intent = new Intent(this, ServerSettingActivity.class);
				startActivityForResult(intent, SHOW_SERVER_SETTING);
				break;
			}
			case OPT_SETTING:
			{
				// 設定アクティビティ起動
//				Intent intent = new Intent(this, SettingActivity.class);
				Intent intent = new Intent(this, SettingActivity.class);
				startActivityForResult(intent, SHOW_SETTING);
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,Intent data)
	{
		// 設定画面から戻った際の処理
		// 設定値をIntentから取り出し、Configに設定する。
		if (requestCode == SHOW_WELLCOME)
		{
			if (resultCode == RESULT_OK)
			{ 
				mFirstBoot = false;
				
				// プリファレンスへ書き込む。
//				mPref = getSharedPreferences("config", Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = mSharedPreferences.edit();
				editor.putBoolean("FIRST_BOOT", false );
				editor.commit();
			}
			else if (resultCode == RESULT_CANCELED)
			{  
				finish();
			}
		}
		else if (requestCode == SHOW_SETTING) 
		{
			// ipListを再生成する。
			loadIpFromPreferences();
			
			loadConfig();
			
			Window windows = getWindow();

			// ピクトエリアを非表示にしてフルスクリーン
			if( mFullScreen == true )
			{
				windows.addFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN );
			}
			else
			{
				windows.clearFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN );
			}

			TextView TextViewHintMessage = (TextView) findViewById( R.id.textViewHintMessage );
			if( mShowHint == true )
			{
				TextViewHintMessage.setVisibility(View.VISIBLE);
			}
			else
			{
				TextViewHintMessage.setVisibility(View.INVISIBLE);
			}
			
			// レシーブスレッドを再起動する。
			mRThread.waitForStop();

			mRThread = new ReceiveThread( mPortNum, mReceiveHandler );
			mRThread.start();
			
		}
		else if (requestCode == SHOW_SERVER_SETTING)
		{
			// ipListを再生成する。
			loadIpFromPreferences();
			
			// レシーブスレッドを再起動する。
			if( mRThread != null )
			{
				mRThread.waitForStop();
			}
			mRThread = new ReceiveThread( mPortNum, mReceiveHandler );
			mRThread.start();
			
			if (resultCode == RESULT_OK)
			{  
				// mIP = data.getStringExtra("Select_IP");
//				mIpPos = data.getIntExtra("Select_IP_NO", 0 );

				
				// プリファレンスから値読みこみ
				//mPref = getSharedPreferences("config", Context.MODE_PRIVATE);

				//mIP = mPref.getString( "ipAddress" + String.valueOf( mIpPos ), "null");
//				mIP = ipList.get(mIpPos);

				mIP = data.getStringExtra("Select_IP" );
				if( mIP != "" )
				{
					mPreIP = mIP;
					SharedPreferences.Editor editor = mSharedPreferences.edit();
					editor.putString( "PRE_IP", mPreIP );
					editor.commit();
				}
				
				// 接続要求を投げる。
				sendMessage( "/connect: " );
			}
		}
	}
	public void loadIpFromPreferences()
	{
		// プリファレンスから値読みこみ
//		mPref = getSharedPreferences("config", Context.MODE_PRIVATE);
		
		mFirstBoot = mSharedPreferences.getBoolean( "FIRST_BOOT", true);
		
		mIpList.clear();
		
		int i = 0;
		while( true )
		{
			String ipAddress = mSharedPreferences.getString( "ipAddress" + String.valueOf( i ), "null");
			i++;
			
			if( ipAddress.equals("null"))
			{
				break;
			}
			else
			{
				mIpList.add(ipAddress);
			}
		}
	}
	
	@Override  
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);  
		Log.d("Debug","onSaveInstanceState");
		outState.putString("IP", mIP );
		outState.putString("HostName", mHostName );
		outState.putBoolean( "isKeyInputVisible", mIsKeyInputVisible );
	}

	@Override  
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		Log.d("Debug","onRestoreInstanceState");

		mIP = savedInstanceState.getString("IP");
		mHostName = savedInstanceState.getString("HostName");
		mIsKeyInputVisible = savedInstanceState.getBoolean( "isKeyInputVisible" );
	}
	
    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
		boolean result = false;
		
        switch (event.getAction()) {
        case KeyEvent.ACTION_DOWN:
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
            	if( mIsKeyInputVisible == true )
            	{
					//---------------------------------------------------------------
					LinearLayout linearLayoutMessage = (LinearLayout) findViewById(R.id.linearLayoutMessage);
					int height = linearLayoutMessage.getHeight();
					
					//TranslateAnimation(float fromX, float toX, float fromY, float toY)
					TranslateAnimation translate = new TranslateAnimation(0, 0, 0, -height);
					translate.setDuration(500);
					translate.setFillAfter( true );
					translate.setAnimationListener(this);
					mToKeyInputVisible = false;
					
					//アニメーションスタート
					linearLayoutMessage.startAnimation(translate);
					
					//---------------------------------------------------------------
					LinearLayout linearLayoutKeyButtons = (LinearLayout) findViewById(R.id.linearLayoutKeyButtons);
					height = linearLayoutKeyButtons.getHeight();

					//TranslateAnimation(float fromX, float toX, float fromY, float toY)
					translate = new TranslateAnimation(0, 0, 0, height);
					translate.setDuration(500);
					translate.setFillAfter( true );
					translate.setAnimationListener(this);
					mToKeyInputVisible = false;
					
					linearLayoutKeyButtons.startAnimation(translate);
					
					//---------------------------------------------------------------
					// 追加ボタンの表示
					ImageButton imageButton = (ImageButton) findViewById(R.id.ImageButtonAdd);

					AlphaAnimation alpha_animation = new AlphaAnimation(0.0f, 100f);
					alpha_animation.setDuration(500);
					alpha_animation.setFillAfter( true );

					//アニメーションスタート
					imageButton.startAnimation(alpha_animation);
					return false;
				}
				else
				{
	                return super.dispatchKeyEvent(event);
	            }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // AnimationListener実装 start
	@Override
	public void onAnimationEnd(Animation animation) 
	{
		if( mToKeyInputVisible == false )
		{
			mIsKeyInputVisible = false;
			changeKeyInputVisibility();
			TextView TextViewHintMessage = (TextView) findViewById( R.id.textViewHintMessage );
			TextViewHintMessage.setText( getString( R.string.hint_wait ) );
		}
		else
		{
			mIsKeyInputVisible = true;
			changeKeyInputVisibility();
			TextView TextViewHintMessage = (TextView) findViewById( R.id.textViewHintMessage );
			TextViewHintMessage.setText( getString( R.string.hint_input ) );
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) 
	{
	}

	@Override
	public void onAnimationStart(Animation animation) 
	{
	}
    // AnimationListener実装 end
}
