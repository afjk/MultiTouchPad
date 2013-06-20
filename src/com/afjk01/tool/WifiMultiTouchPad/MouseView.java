package com.afjk01.tool.WifiMultiTouchPad;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MouseView extends SurfaceView
implements SurfaceHolder.Callback
{
	private int width;
	private int height;
	private Paint textPaint = new Paint();
	private float scale = 1.0F;
	private Handler mAppHandler;
	private long mPreTime;
	private int mDownX;
	private int mDownY;
	private long mDownTime;
	private float mPreX;
	private float mPreY;
	private boolean mTwoTouch;
	private boolean mDragging;
	final float CURSOR_MOVE_SCALE = 0.8f;
	final int WHEEL_AREA_WIDTH = 80;

	// タッチジェスチャーステータス
	private enum eTouchState
	{
		Wait,						// 待ち
		SinglePress,				// 1指押し
		SinglePressMove,			// 1指押し移動中
		SingleTapWait,				// 1指押し→開放　待ち
		SingleTapDoublePressWait,	// 1指2回押し待ち
		Dragging,					// 1指ドラッグ
		TwoPress,					// 2指同時押し
		PointTouchOnePress,			// ポイント&タッチ1指押し
		PointTouchTwoPress,			// ポイント&タッチ2指押し
		Scroll,						// 右(左)端ドラッグスクロール
		ThreePress,					// 3指押し
		ThreePressWait,				// 3指押し→開放　待ち
		ThreeDoublePress,			// 3指2回押し(未使用)
		ThreePressLR,				// 3指押し左右ドラッグ(未使用)
		FourPress,					// 4指押し
		FourPressLR,				// 4指押し右左ドラッグ(アプリ選択)
		FourPressUD,				// 4指押し上下ドラッグ(アプリ選択)
		TwoPressDrag,				// 2指押しドラッグ(未使用)
		TwoPressPintch,				// 2指押しピンチイン・ピンチアウト
		
	}
	
	private eTouchState mTouchState = eTouchState.Wait;
	
	private final int WAIT_MSEC = 200;	// タッチ認識待ち時間(msec)
	private final float WHEEL_SCALE = 0.5f;
	private TimerThread mTimerThread;
	private int mFingerCount;
	private int mScrollVal;
	private boolean mDoWindowNormal;
	private long mDownDistance;
	private String mStateStr = "";
	private MulitiTouchPadActivity mContext;
	private float mDx = 0.0f;
	private float mDy = 0.0f;
	private Bitmap padImage;
	private String mHostName;
	
	public MouseView(Context paramContext, Handler mAppHandler)
	{
		super(paramContext);
		mContext = (MulitiTouchPadActivity)paramContext;
		this.mAppHandler = mAppHandler;
		
		getHolder().addCallback(this);
		setFocusable(true);
		setFocusableInTouchMode(true);

		textPaint.setColor(Color.WHITE);
		
		Resources r = mContext.getResources();
		padImage = BitmapFactory.decodeResource(r, R.drawable.padimage);
		mHostName = new String(mContext.mHostName);
		
		mPreTime = System.currentTimeMillis();
		mTimerThread = new TimerThread( WAIT_MSEC );
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
	{
		this.width = width;
		this.height = height;
		
		if (width > height) 
		{
			this.scale = width / 480f;
		}
		else
		{
			this.scale = height / 480f;
		}
		textPaint.setTextSize(14 * scale);
		
		Canvas c = getHolder().lockCanvas();
		
		if (c != null) 
		{
			// clear screen
	        
	        int w = padImage.getWidth();
	        int h = padImage.getHeight();
			// 描画元の矩形イメージ
	        Rect src = new Rect(0, 0, w, h);
	        // 描画先の矩形イメージ
	        Rect dst = new Rect(0, 0, width, height);
	        c.drawBitmap(padImage, src, dst, null);
			
			// 文字を書く。
//			float tWidth = textPaint.measureText("test");
//			c.drawText("test", width / 2 - tWidth / 2, height / 2, textPaint);
	        
			drawHintMessage(c);
			
			getHolder().unlockCanvasAndPost(c);
		}
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder paramSurfaceHolder)
	{
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder)
	{
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent motionEvent)
	{
		// タッチ指数
		int fingerCount = motionEvent.getPointerCount();
		if( mFingerCount < fingerCount )
		{
			mFingerCount = fingerCount;
		}
		
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			/* イベント送信 */
			Message msg = new Message();
			msg.what = MulitiTouchPadActivity.MESSAGE_SHOW_INPUT;
			mAppHandler.sendMessage(msg);
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
			/* イベント送信 */
			Message msg = new Message();
			msg.what = MulitiTouchPadActivity.MESSAGE_HIDE_INPUT;
			mAppHandler.sendMessage(msg);
		}
		
		if( mTouchState == eTouchState.Wait )
		{
			stateWait(motionEvent);
		}
		else if( mTouchState == eTouchState.SinglePress )
		{
			stateSinglePress(motionEvent);
		}
		else if( mTouchState == eTouchState.SinglePressMove )
		{
			stateSinglePressMove(motionEvent);
		}
		else if( mTouchState == eTouchState.SingleTapWait )
		{
			stateSingleTapWait(motionEvent);
		}
		else if( mTouchState == eTouchState.SingleTapDoublePressWait )
		{
			stateSingleTapDoublePressWait(motionEvent);
		}
		else if( mTouchState == eTouchState.Dragging)
		{
			stateDragging(motionEvent);
		}
		else if( mTouchState == eTouchState.TwoPress)
		{
			stateTwoPress(motionEvent);
		}
		else if( mTouchState == eTouchState.PointTouchOnePress)
		{
			statePointTouchOnePress(motionEvent);
		}
		else if( mTouchState == eTouchState.PointTouchTwoPress)
		{
			statePointTouchTwoPress(motionEvent);
		}
		else if( mTouchState == eTouchState.Scroll)
		{
			stateScroll(motionEvent);
		}
		else if( mTouchState == eTouchState.ThreePress)
		{
			stateThreePress(motionEvent);
		}
		else if( mTouchState == eTouchState.ThreePressWait)
		{
			stateThreePressWait(motionEvent);
		}
		else if( mTouchState == eTouchState.ThreeDoublePress)
		{
			stateThreeDoublePress(motionEvent);
		}
		else if( mTouchState == eTouchState.ThreePressLR)
		{
			stateThreePressLR(motionEvent);
		}
		else if( mTouchState == eTouchState.FourPress)
		{
			stateFourPress(motionEvent);
		}
		else if( mTouchState == eTouchState.FourPressLR)
		{
			stateFourPressLR(motionEvent);
		}
		else if( mTouchState == eTouchState.FourPressUD)
		{
			stateFourPressUD(motionEvent);
		}
		else if( mTouchState == eTouchState.TwoPressDrag)
		{
			stateTwoPressDrag(motionEvent);
		}
		else if( mTouchState == eTouchState.TwoPressPintch)
		{
			stateTwoPressPintch(motionEvent);
		}
		
		drawCanvas( motionEvent );
		
		mPreX = (float)motionEvent.getX();
		mPreY = (float)motionEvent.getY();
		
		return true;
	}

	private void changeState( eTouchState touchState)
	{
		mTouchState = touchState;
		
		String stateStr = "";
		String hintMessage = "";
		
		// enterState処理。
		if( mTouchState == eTouchState.Wait )
		{
			stateStr = "Wait";
			hintMessage = mContext.getString( R.string.hint_wait ); 
		}
		else if( mTouchState == eTouchState.SinglePress )
		{
			stateStr = "SinglePress";
			hintMessage = mContext.getString( R.string.hint_wait ); 
		}
		else if( mTouchState == eTouchState.SinglePressMove )
		{
			stateStr = "SinglePressMove";
			hintMessage = mContext.getString( R.string.hint_1press );
		}
		else if( mTouchState == eTouchState.SingleTapWait )
		{
			// タイムアウト設定
			setTimeOut( WAIT_MSEC );
			stateStr = "SingleTapWait";
			hintMessage = mContext.getString( R.string.hint_wait ); 
		}
		else if( mTouchState == eTouchState.SingleTapDoublePressWait )
		{
			// タイムアウト設定
			setTimeOut( WAIT_MSEC );
			stateStr = "SingleTapDoublePressWait";
			hintMessage = mContext.getString( R.string.hint_wait ); 
		}
		else if( mTouchState == eTouchState.Dragging)
		{
			stateStr = "Dragging";
			hintMessage = mContext.getString( R.string.hint_dragging );
		}
		else if( mTouchState == eTouchState.TwoPress)
		{
			stateStr = "TwoPress";
			hintMessage = mContext.getString( R.string.hint_2press );
		}
		else if( mTouchState == eTouchState.PointTouchOnePress)
		{
			mDownTime = System.currentTimeMillis();
			setTimeOut( 100 );
			stateStr = "PointTouchOnePress";
			hintMessage = mContext.getString( R.string.hint_1press );
		}
		else if( mTouchState == eTouchState.PointTouchTwoPress)
		{
			stateStr = "PointTouchTwoPress";
			hintMessage = mContext.getString( R.string.hint_1press );
		}
		else if( mTouchState == eTouchState.Scroll)
		{
			stateStr = "Scroll";
			hintMessage = mContext.getString( R.string.hint_scroll );
		}
		else if( mTouchState == eTouchState.ThreePress)
		{
			mDownTime = System.currentTimeMillis();
			mDoWindowNormal = false;
			stateStr = "ThreePress";
			hintMessage = mContext.getString( R.string.hint_3press );
		}
		else if( mTouchState == eTouchState.ThreePressWait)
		{
			stateStr = "ThreePressWait";
			hintMessage = mContext.getString( R.string.hint_3press );
		}
		else if( mTouchState == eTouchState.ThreeDoublePress)
		{
			stateStr = "ThreeDoublePress";
			hintMessage = mContext.getString( R.string.hint_3press );
		}
		else if( mTouchState == eTouchState.ThreePressLR)
		{
			stateStr = "ThreePressLR";
			hintMessage = mContext.getString( R.string.hint_3press );
		}
		else if( mTouchState == eTouchState.FourPress)
		{
			mDownTime = System.currentTimeMillis();
			stateStr = "FourPress";
			hintMessage = mContext.getString( R.string.hint_4press );
		}
		else if( mTouchState == eTouchState.FourPressLR)
		{
			stateStr = "FourPressLR";
			hintMessage = mContext.getString( R.string.hint_4press_lr );
		}
		else if( mTouchState == eTouchState.FourPressUD)
		{
			stateStr = "FourPressUD";
			hintMessage = mContext.getString( R.string.hint_4press_ud );
		}
		else if( mTouchState == eTouchState.TwoPressDrag)
		{
			stateStr = "stateTwoPressDrag";
		}
		else if( mTouchState == eTouchState.TwoPressPintch)
		{
			stateStr = "stateTwoPressPintch";
			hintMessage = mContext.getString( R.string.hint_2press );
		}
		if( hintMessage.equals("") )
		{
			hintMessage = stateStr;
		}
		
		
//		drawText2( stateStr );
		mStateStr  = stateStr;
		
		/* イベント送信 */
		Message msg = new Message();
		
		Bundle data = new Bundle();
		data.putString( "State", mStateStr);
		data.putString( "HintMessage", hintMessage);
		msg.setData( data );
		msg.what = MulitiTouchPadActivity.MESSAGE_HINT_MESSAGE;
		mAppHandler.sendMessage(msg);
	}

	private void stateWait(MotionEvent motionEvent) 
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
			mDownTime = System.currentTimeMillis();
		
			mDownX = (int)motionEvent.getX();
			mDownY = (int)motionEvent.getY();
			
			if( mDownX > width - WHEEL_AREA_WIDTH )
			{
				changeState( eTouchState.Scroll );
			}
			else
			{
				changeState( eTouchState.SinglePress );

			}
//			sendMessageToActivity("/mempos: ");	// サーバーにタッチ開始座標を保存させる。
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
		}
	}
	
	private void stateSinglePress(MotionEvent motionEvent) 
	{
		// 一定時間経過でSinglePressMove状態へ移行。
		long nowTime = System.currentTimeMillis();
		
		if( (nowTime - mDownTime) > WAIT_MSEC )
		{
			//タイマー停止
			stopTimer();
			changeState( eTouchState.SinglePressMove );
		}
		else
		{
			// 指数の変化チェック。
			int fingerCount = motionEvent.getPointerCount();
			if( fingerCount == 2 )
			{
				mDownX = (int) motionEvent.getX();
				mDownY = (int) motionEvent.getY();
				mDownDistance = getTwoPointDistance(motionEvent);
				changeState( eTouchState.TwoPress );
			}
			else if(fingerCount == 3 )
			{
				mDownX = (int)motionEvent.getX();
				mDownY = (int)motionEvent.getY();
				changeState( eTouchState.ThreePress );
			}
			else if(fingerCount == 4 )
			{
				mDownX = (int)motionEvent.getX();
				mDownY = (int)motionEvent.getY();
				changeState( eTouchState.FourPress );
			}
			else
			{
				if( (motionEvent.getAction() == MotionEvent.ACTION_UP     ) ||
					(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
				{
					changeState( eTouchState.SingleTapWait );

					// シングルタップイベントは、この段階で送ってしまう。
					//sendMessageToActivity("/tap: ");
					sendMessageToActivity("/pressbutton1: ");
				}
				else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
				{
				}
				else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
				{
					if( mTimerThread != null )
					{
						// 一定距離Moveで移動したらタッチ無効と判断する。
						int dx = mDownX - (int)motionEvent.getX();
						int dy = mDownY - (int)motionEvent.getY();
						if( ( dx*dx + dy*dy ) > 30 )
						{
							//タイマー停止
							stopTimer();
							changeState( eTouchState.SinglePressMove );
						}
					}
					
					// 一定時間を過ぎたら、移動可能。
					nowTime = System.currentTimeMillis();
					if( (nowTime - mDownTime) > 100 )
					{
						mouseMove(motionEvent);
					}
				}
			}
		}
	}

	private void stateSinglePressMove(MotionEvent motionEvent)
	{
		// 指数の変化チェック。
		int fingerCount = motionEvent.getPointerCount();
		if( fingerCount == 2 )
		{
			changeState( eTouchState.PointTouchOnePress );
		}
		
		else
		{
			if( (motionEvent.getAction() == MotionEvent.ACTION_UP     ) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
			{
				changeState(eTouchState.Wait);
			}
			else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN )
			{
			}
			else if(motionEvent.getAction() == MotionEvent.ACTION_MOVE )
			{
				mouseMove(motionEvent);
			}
		}
	}

	private void stateSingleTapWait(MotionEvent motionEvent) 
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
			mDownTime = System.currentTimeMillis();
			mDownX = (int)motionEvent.getX();
			mDownY = (int)motionEvent.getY();
			changeState( eTouchState.SingleTapDoublePressWait );
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
		}
	}

	private void stateSingleTapDoublePressWait(MotionEvent motionEvent) 
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			long nowTime = System.currentTimeMillis();
			if( (nowTime - mDownTime) < WAIT_MSEC )
			{
				//タイマー停止
				stopTimer();
				// ダブルクリック
				sendMessageToActivity("/releasebutton1: ");
				sendMessageToActivity("/pressbutton1: ");
				sendMessageToActivity("/releasebutton1: ");
			}
			else
			{
			}
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			// 一定距離Moveで移動したらドラッグ開始と判断する。
			int dx = mDownX - (int)motionEvent.getX();
			int dy = mDownY - (int)motionEvent.getY();
			if( ( dx*dx + dy*dy ) > 30 )
			{
				//タイマー停止
				stopTimer();
				changeState( eTouchState.Dragging );
				// ドラッグ開始
//				sendMessageToActivity("/remempos: ");		// 保存した座標に戻す。
				sendMessageToActivity( "/pressbutton1: " );
			}
		}
	}

	private void stateDragging(MotionEvent motionEvent) 
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			changeState( eTouchState.Wait );
			// ドラッグ終了
			sendMessageToActivity( "/releasebutton1: " );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			mouseMove( motionEvent );
		}
	}
	
	private void stateTwoPress(MotionEvent motionEvent) 
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			sendMessageToActivity( "/twotap: " );
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			// 指数の変化チェック。
			int fingerCount = motionEvent.getPointerCount();
			if(fingerCount == 3 )
			{
				mDownX = (int)motionEvent.getX();
				mDownY = (int)motionEvent.getY();
				changeState( eTouchState.ThreePress );
			}
			else if(fingerCount == 4 )
			{
				mDownX = (int)motionEvent.getX();
				mDownY = (int)motionEvent.getY();
				changeState( eTouchState.FourPress );
			}
			else
			{
				if( twoPointDistanceChanged( motionEvent ) == true )
				{
					// 2指の距離変化→ピンチイン・ピンチアウト
					changeState( eTouchState.TwoPressPintch );
				}
				else if( Math.abs(getMoveDistance( motionEvent )) > 20 )
				{
					// 移動量が一定量を超えたら、2指ドラッグへ。
					//changeState( eTouchState.TwoPressDrag );
					changeState( eTouchState.Scroll );
				}
				else
				{
//					mouseMove(motionEvent);
				}
			}
		}
	}

	private void statePointTouchOnePress(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
			(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			if( stopTimer() == true )
			{
				// まだ、左ボタン押下を通知していなかったので。
				sendMessageToActivity( "/pushbutton1: " );
			}
			sendMessageToActivity( "/releasebutton1: " );
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			int fingerCount = motionEvent.getPointerCount();
			if( fingerCount == 1 )
			{
				if( stopTimer() == true )
				{
					// まだ、左ボタン押下を通知していなかったので。
					sendMessageToActivity( "/pushbutton1: " );
				}
				sendMessageToActivity( "/releasebutton1: " );
				changeState( eTouchState.SinglePressMove );
			}
			else if( fingerCount == 3 )
			{
				if( stopTimer() == true )
				{
					// 左ボタン通知前に3本目がタッチできたので、左ボタン押下に。
					sendMessageToActivity( "/pressbutton3: " );
					changeState( eTouchState.PointTouchTwoPress );
				}
			}
			else
			{
				// 一定時間を過ぎたら、移動可能。
				long nowTime = System.currentTimeMillis();
				if( (nowTime - mDownTime) > 200 )
				{
					mouseMove(motionEvent);
				}
			}
		}
	}

	private void statePointTouchTwoPress(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			sendMessageToActivity( "/releasebutton3: " );
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			
			int fingerCount = motionEvent.getPointerCount();
			if( fingerCount < 3 )
			{
				sendMessageToActivity( "/releasebutton3: " );
				changeState( eTouchState.SinglePressMove );
			}
			else
			{
				mouseMove(motionEvent);
			}
		}
	}

	private void stateScroll(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
			(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
//			changeState( eTouchState.Wait );
			setTimeOut( 10 );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
			if( mDownX > width - WHEEL_AREA_WIDTH )
			{
				stopTimer();
			}
			else
			{
				stopTimer();
				changeState( eTouchState.Wait );
				// 連続状態遷移理。
				onTouchEvent( motionEvent );
			}
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			if( twoPointDistanceChanged( motionEvent ) == true )
			{
				// 2指の距離変化→ピンチイン・ピンチアウト
				changeState( eTouchState.TwoPressPintch );
			}
			else
			{
				int y = (int)motionEvent.getY();
//mac				mScrollVal = (int) ((mPreY - y) * WHEEL_SCALE);
				mScrollVal = (int) ((y-mPreY) * WHEEL_SCALE);
				int scrollVal = (int) (mScrollVal * ( mContext.mScrollSense / 100f + 0.5f ) );
				sendMessageToActivity("/wheel:" + String.valueOf(scrollVal) + " ");
			}
		}
	}

	private void stateThreePress(MotionEvent motionEvent)
	{
		// 指数の変化チェック。
		int fingerCount = motionEvent.getPointerCount();
		if( fingerCount == 4 )
		{
			changeState( eTouchState.FourPress );
		}
		else
		{
			if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
					(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
			{
				long nowTime = System.currentTimeMillis();
				int x = (int)motionEvent.getX();
				int y = (int)motionEvent.getY();
				
				int dx = (x - mDownX);
				int dy = (y - mDownY);
				long distanceSquare = dx * dx + dy * dy;
				
				// タップ検出
//				if( ( nowTime - mDownTime < 1000 ) &&
//				    ( distanceSquare < 1600 ) )
				if( nowTime - mDownTime < 1000 )		// 移動量は、1指の移動量で考えないと、異なる指の押下点で見てしまうと大きく動いたように見えてしまう。
				{
					sendMessageToActivity("/copy: ");
					setTimeOut( 1000 );
					changeState( eTouchState.ThreePressWait );
				}
				else
				{
					changeState( eTouchState.Wait );
				}
			}
			else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
			{
			}
			else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
			{
				int x = (int)motionEvent.getX();
				int y = (int)motionEvent.getY();
				
				int dx = (x - mDownX);
				int dy = (y - mDownY);
				long distanceSquare = dx * dx + dy * dy;
				
				if( distanceSquare > 6400 )
				{
					if( Math.abs(dx) > Math.abs(dy) )
					{
						if( dx > 0 )
						{
							sendMessageToActivity("/next: ");
						}
						else
						{
							sendMessageToActivity("/back: ");
						}
						changeState( eTouchState.Wait );
					}
					else
					{
						if( dy > 0 )
						{
							/*	// ALT+Space+Rは2つ割り当てられている場合があるので、キャンセル。
							if( mDoWindowNormal == false )
							{
								mDoWindowNormal = true;
								mDownY = y;
								sendMessageToActivity("/windownormal: ");
							}
							else
							*/
							{
								sendMessageToActivity("/windowmin: ");
								changeState( eTouchState.Wait );
							}
						}
						else
						{
							sendMessageToActivity("/windowmax: ");
							changeState( eTouchState.Wait );
						}
					}
				}
			}
		}
	}
	private void stateThreePressWait(MotionEvent motionEvent)
	{
		int fingerCount = motionEvent.getPointerCount();
		if( fingerCount == 3 )
		{
			if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
					(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
			{
			}
			else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
			{
			}
			else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
			{
				sendMessageToActivity("/browser: ");
				changeState( eTouchState.Wait );
			}
		}
	}
	private void stateThreeDoublePress(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
		}
	}
	private void stateThreePressLR(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
		}
	}
	
	private void stateFourPress(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			long nowTime = System.currentTimeMillis();
			// タップ検出
			if( nowTime - mDownTime < 1000 )
			{
				sendMessageToActivity("/desktop: ");
			}
//			sendMessageToActivity("/endchangeapp: ");
//			sendMessageToActivity("/endchangewin: ");
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			int x = (int)motionEvent.getX();
			int y = (int)motionEvent.getY();
			
			int dx = ( x - mDownX );
			int dy = ( y - mDownY );
			
			if( dx * dx + dy * dy > 2500 )
			{
				if( Math.abs(dx) > Math.abs(dy) )
				{
					changeState( eTouchState.FourPressLR );
				}
				else
				{
					changeState( eTouchState.FourPressUD );
				}
			}
		}
	}
	
	private void stateFourPressLR(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			sendMessageToActivity("/endchangewin: ");
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			int x = (int)motionEvent.getX();
			int y = (int)motionEvent.getY();
			
			int dx = ( x - mDownX );
			int dy = ( y - mDownY );
			
			if( dx * dx + dy * dy > 2500 )
			{
//				if( Math.abs(dx) > Math.abs(dy) )
				{
					if( dx > 0 )
					{
						sendMessageToActivity("/nextwin: ");
					}
					else
					{
						sendMessageToActivity("/backwin: ");
					}
					mDownX = x;
					mDownY = y;
				}
//				else
//				{
//				}
			}
		}
	}
	
	private void stateFourPressUD(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			sendMessageToActivity("/endchangeapp: ");
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			int x = (int)motionEvent.getX();
			int y = (int)motionEvent.getY();
			
			int dx = ( x - mDownX );
			int dy = ( y - mDownY );
			
			if( dx * dx + dy * dy > 2500 )
			{
//				if( Math.abs(dx) > Math.abs(dy) )
//				{
//				}
//				else
				{
					if( dy > 0 )
					{
						sendMessageToActivity("/nextapp: ");
					}
					else
					{
						sendMessageToActivity("/backapp: ");
					}
					mDownX = x;
					mDownY = y;
				}
			}
		}
	}
	
	private void stateTwoPressDrag(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			sendMessageToActivity( "/enddragscroll: " );
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			int fingerCount = motionEvent.getPointerCount();
			
			if( fingerCount < 2 )
			{
				sendMessageToActivity( "/enddragscroll: " );
				changeState( eTouchState.Wait );
			}
			else if( twoPointDistanceChanged( motionEvent ) == true )
			{
				// 2指の距離変化→ピンチイン・ピンチアウト
				sendMessageToActivity( "/enddragscroll: " );
				changeState( eTouchState.TwoPressPintch );
			}
			else
			{
				// ドラッグスクロール
				String sendPosStr = "";

				int x = (int)motionEvent.getX();
				int y = (int)motionEvent.getY();
				
				int dx = (int) ((x - mPreX)*CURSOR_MOVE_SCALE);
				int dy = (int) ((y - mPreY)*CURSOR_MOVE_SCALE);

				if( dx > 0)
				{
					dx = dx*dx;
				}
				else
				{
					dx = -dx*dx;
				}
				
				if( dy > 0 )
				{
					dy = dy*dy;
				}
				else
				{
					dy = -dy*dy;
				}
				
				// マウス操作
				sendPosStr = "/dragscroll:" + String.valueOf(dx) + "," + String.valueOf(dy) + " ";
				
				sendMessageToActivity( sendPosStr );
			}
		}
	}
	
	private void stateTwoPressPintch(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
//			changeState( eTouchState.Wait );
			changeState( eTouchState.Scroll );
			setTimeOut( 10 );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
			long nowDistance = getTwoPointDistance(motionEvent);
			
			if( nowDistance != -1 )
			{
				if( Math.abs(nowDistance - mDownDistance) > 10 )
				{
					long dDistance = nowDistance - mDownDistance;
					
					sendMessageToActivity("/scale:" + String.valueOf(dDistance) + " ");
					
					mDownDistance = nowDistance;
				}
			}
			else
			{
				changeState( eTouchState.Wait );
			}
			
			// スクロールもさせる。
			int y = (int)motionEvent.getY();
//mac			mScrollVal = (int) ((mPreY - y) * WHEEL_SCALE);
			mScrollVal = (int) ((y-mPreY) * WHEEL_SCALE);
			int scrollVal = (int) ( mScrollVal * ( mContext.mScrollSense / 100.0f + 0.5f ) );
			sendMessageToActivity("/wheel:" + String.valueOf(scrollVal) + " ");
		}
	}
	
	/* 状態処理フォーマット
	private void state(MotionEvent motionEvent)
	{
		if( (motionEvent.getAction() == MotionEvent.ACTION_UP) ||
				(motionEvent.getAction() == MotionEvent.ACTION_CANCEL ) )
		{
			changeState( eTouchState.Wait );
		}
		else if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
		{
		}
		else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE )
		{
		}
	}
	*/

	//============状態定義ここまで================
	
	private long getMoveDistance( MotionEvent motionEvent )
	{
		long distance = 0;
		
		int x = (int) motionEvent.getX();
		int y = (int) motionEvent.getY();
		int dx = x - mDownX;
		int dy = y - mDownY;
		
		distance = (long) Math.sqrt( dx * dx + dy * dy );
		
		return distance;
	}
	
	private boolean twoPointDistanceChanged( MotionEvent motionEvent )
	{
		boolean result = false;
		
		long nowDistance = getTwoPointDistance( motionEvent );
		
		if( nowDistance != -1 )
		{
			if( Math.abs( nowDistance - mDownDistance ) > 50 )
			{
				result = true;
			}
		}
		
		return result;
	}
	
	private long getTwoPointDistance(MotionEvent motionEvent)
	{
		long distance = -1;
		
		int fingerCount = motionEvent.getPointerCount();
		
		if( fingerCount >= 2 )
		{
			int id1 = motionEvent.getPointerId(0);
			int id2 = motionEvent.getPointerId(1);

			int x1 = (int)motionEvent.getX(id1);
			int y1 = (int)motionEvent.getY(id1);
			int x2 = (int)motionEvent.getX(id2);
			int y2 = (int)motionEvent.getY(id2);
			int dx = x1 - x2;
			int dy = y1 - y2;
			
			distance = (long) Math.sqrt( dx * dx + dy * dy );
		}
		
		return distance;
	}
	
	private void mouseMove(MotionEvent motionEvent)
	{
		String sendPosStr = "";

		float x = (float)motionEvent.getX();
		float y = (float)motionEvent.getY();
		
		float dx = (x - mPreX)*CURSOR_MOVE_SCALE;
		float dy = (y - mPreY)*CURSOR_MOVE_SCALE;
		
		int distance = (int) Math.sqrt(dx*dx + dy*dy);
		
		if( distance < 5 )
		{
			float low_val = 1.0f;
			mDx = dx * low_val + mDx * ( 1.0f - low_val );
			mDy = dy * low_val + mDy * ( 1.0f - low_val );
			dx = (int) mDx;
			dy = (int) mDy;
		}
		else if( distance < 30 )
		{
			dx *= 3;
			dy *= 3;
		}
		else if( distance < 60 )
		{
			dx *= 6;
			dy *= 6;
		}
		else
		{
			dx *= 10;
			dy *= 10;
		}
		
		// Config値の反映
//		dx = dx * mContext.mMouseSense / 50.0f;
//		dy = dy * mContext.mMouseSense / 50.0f;
		dx = dx * ( mContext.mMouseSense / 100.0f + 0.5f );
		dy = dy * ( mContext.mMouseSense / 100.0f + 0.5f );
		
		int sx = (int)dx;
		int sy = (int)dy;
		
		// マウス操作
		sendPosStr = "/mouse:" + String.valueOf(sx) + "," + String.valueOf(sy) + " ";
		
		sendMessageToActivity( sendPosStr );
		
		//高速化

//		SendThread sThread = new SendThread( mContext.mIP, mContext.mPortNum, sendPosStr );
//		sThread.start();
	}

	// SocketMouseActivityへのイベント送信。
	// ActivityでSocket送信される。
	private void sendMessageToActivity( String sendPosStr )
	{
		/* イベント送信 */
		Message msg = new Message();
		
		Bundle data = new Bundle();
		data.putString( "Pos", sendPosStr);
		msg.setData( data );
		msg.what = MulitiTouchPadActivity.MESSAGE_SEND;
		mAppHandler.sendMessage(msg);
		
		// debug キャンバスに文字列出力。
		drawText( sendPosStr );
	}

	private void drawText(String str )
	{
		/*
		// キャンバスに座標を文字で書く。
		Canvas c = getHolder().lockCanvas();
		
		if (c != null)
		{
			// clear screen
			c.drawColor(Color.BLACK);
			
			float tWidth = textPaint.measureText(str);
			c.drawText(str, width / 2 - tWidth / 2, height / 2, textPaint);
			getHolder().unlockCanvasAndPost(c);
		}
		*/
	}

	private void drawText2(String str )
	{
		// キャンバスに座標を文字で書く。
		Canvas c = getHolder().lockCanvas();
		
		if (c != null)
		{
			// clear screen
//			c.drawColor(Color.DKGRAY);
	        int w = padImage.getWidth();
	        int h = padImage.getHeight();
			// 描画元の矩形イメージ
	        Rect src = new Rect(0, 0, w, h);
	        // 描画先の矩形イメージ
	        Rect dst = new Rect(0, 0, width, height);
	        c.drawBitmap(padImage, src, dst, null);
	        
			float tWidth = textPaint.measureText(str);
			c.drawText(str, width / 2 - tWidth / 2, height / 2, textPaint);
			getHolder().unlockCanvasAndPost(c);
		}
	}
	
	private void drawCanvas( MotionEvent motionEvent )
	{
		Paint paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStrokeWidth(10);
		paint.setStyle(Paint.Style.STROKE);
		
		Paint textPaint = new Paint();
		textPaint.setColor(Color.WHITE);
		textPaint.setTextSize(14 * scale);

		int id;
		
		Canvas c = getHolder().lockCanvas();
		
		if (c != null)
		{
//			c.drawColor(Color.DKGRAY);
	        int w = padImage.getWidth();
	        int h = padImage.getHeight();
			// 描画元の矩形イメージ
	        Rect src = new Rect(0, 0, w, h);
	        // 描画先の矩形イメージ
	        Rect dst = new Rect(0, 0, width, height);
	        c.drawBitmap(padImage, src, dst, null);
	        
			int fingerCount = motionEvent.getPointerCount();
			
			if( motionEvent.getAction() != MotionEvent.ACTION_UP )
			{
				for (int j = 0; j < fingerCount; j++)
				{
					id = motionEvent.getPointerId(j);
					paint.setColor(Color.argb(100,255,255,255));
					drawCircle((int)motionEvent.getX(j), (int)motionEvent.getY(j), 50, paint , c);
				}
				
				// デフォルトの位置に大円。
				/*
				paint.setColor( Color.WHITE );
				paint.setColor(Color.argb(155,255,255,255));
				paint.setStrokeWidth(20);
				drawCircle((int)motionEvent.getX(), (int)motionEvent.getY(), 50, paint , c);
				*/
				//paint.setColor( Color.BLUE );
				paint.setColor(Color.argb(100,255,255,255));
				paint.setStrokeWidth(20);
				drawCircle((int)motionEvent.getX(), (int)motionEvent.getY(), 53, paint , c);
			}
			
//			float tWidth = textPaint.measureText(mStateStr);
//			c.drawText(mStateStr, width / 2 - tWidth / 2, height / 2, textPaint);
			
			// 接続情報表示
//			c.drawText("IP  :" + mContext.mIP      , 10, 100, textPaint);
//			c.drawText("Host:" + mContext.mHostName, 10, 130, textPaint);
			drawHintMessage(c);
		}
		getHolder().unlockCanvasAndPost(c);
	}

	
	private void drawCircle(int x, int y, int radius, Paint paint, Canvas c) 
	{
		c.drawCircle(x, y, radius * scale , paint);
	}

	private void drawHintMessage( Canvas c )
	{
		
		float tWidth = textPaint.measureText(mStateStr);
		/*
		switch( mTouchState )
		{
			case Wait:
				String testStr = "aaa\nbbb";
				tWidth = textPaint.measureText(testStr);
				//textPaint.getFontMetrics().ascent;
				
				
				c.drawText(testStr, width / 2 - tWidth / 2, height / 2, textPaint);				
			break;
			default:
				c.drawText(mStateStr, width / 2 - tWidth / 2, height / 2, textPaint);
		}
		*/
		if( mTouchState == eTouchState.Wait )
		{
		}
		else if( mTouchState == eTouchState.SinglePress )
		{
		}
		else if( mTouchState == eTouchState.SinglePressMove )
		{
		}
		else if( mTouchState == eTouchState.SingleTapWait )
		{
		}
		else if( mTouchState == eTouchState.SingleTapDoublePressWait )
		{
		}
		else if( mTouchState == eTouchState.Dragging)
		{
		}
		else if( mTouchState == eTouchState.TwoPress)
		{
		}
		else if( mTouchState == eTouchState.PointTouchOnePress)
		{
		}
		else if( mTouchState == eTouchState.PointTouchTwoPress)
		{
		}
		else if( mTouchState == eTouchState.Scroll)
		{
		}
		else if( mTouchState == eTouchState.ThreePress)
		{
		}
		else if( mTouchState == eTouchState.ThreePressWait)
		{
		}
		else if( mTouchState == eTouchState.ThreeDoublePress)
		{
		}
		else if( mTouchState == eTouchState.ThreePressLR)
		{
		}
		else if( mTouchState == eTouchState.FourPress)
		{
		}
		else if( mTouchState == eTouchState.FourPressLR)
		{
		}
		else if( mTouchState == eTouchState.FourPressUD)
		{
		}
		else if( mTouchState == eTouchState.TwoPressDrag)
		{
		}
		else if( mTouchState == eTouchState.TwoPressPintch)
		{
		}
		
		
		// 接続情報表示
//		c.drawText("IP  :" + mContext.mIP      , 10, 100, textPaint);
//		c.drawText("Host:" + mContext.mHostName, 10, 130, textPaint);
//		c.drawText("Host:" + mHostName, 10, 130, textPaint);
	}
	
	private boolean stopTimer()
	{
		boolean result = false;
		
		if( mTimerThread != null )
		{
			mTimerThread.halt();
			mTimerThread = null;
			result = true;
		}
		
		return result;
	}
	
	private void setTimeOut( int waitTime )
	{
		if( mTimerThread != null )
		{
			mTimerThread.halt();
		}

		mTimerThread = new TimerThread( waitTime );
		mTimerThread.start();
	}

	//================================================================
	// TimerThread
	class TimerThread extends Thread
	{
		int mDelayTime;
		private boolean halt_;
		public TimerThread( int time )
		{
			mDelayTime = time;
			halt_ = false;
		}
		
		public void halt() {
	        halt_ = true;
	        interrupt();
		}

		@Override
		public void run()
		{
			while(!halt_){
				try
				{
					sleep( mDelayTime );
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
				timeOut();
				stopTimer();	// 自分自身をnullに。
			}
		}
		
		// タイムアウト時の処理
		public void timeOut()
		{
			if( mTouchState == eTouchState.Wait )
			{
			}
			else if( mTouchState == eTouchState.SinglePress )
			{
			}
			else if( mTouchState == eTouchState.SingleTapWait )
			{
				changeState( eTouchState.Wait );
				// クリック処理

				sendMessageToActivity("/releasebutton1: ");
				// シングルタップ
				//sendMessageToActivity("/tap: ");
			}
			else if( mTouchState == eTouchState.SingleTapDoublePressWait )
			{
				changeState( eTouchState.Dragging );
				// ドラッグ開始
				//sendMessageToActivity( "/pressbutton1: " );
			}
			else if( mTouchState == eTouchState.PointTouchOnePress )
			{
				sendMessageToActivity( "/pressbutton1: " );
			}
			else if( mTouchState == eTouchState.ThreePressWait )
			{
				changeState( eTouchState.Wait );
			}
			else if( mTouchState == eTouchState.Scroll )
			{
				mScrollVal *= 0.98f;
				if( Math.abs(mScrollVal) < 1 )
				{
					changeState( eTouchState.Wait );
				}
				else
				{
//					mScrollVal *= mContext.mScrollSense / 50.0f;
//					int scrollVal = (int) (mScrollVal * mContext.mScrollSense / 50.0f);
					sendMessageToActivity("/wheel:" + String.valueOf(mScrollVal) + " ");
					setTimeOut(20);
				}
			}
			else if( mTouchState == eTouchState.ThreePressWait )
			{
				changeState( eTouchState.Wait );
			}
		}
	} // TimerThread
}
