package com.afjk01.tool.WifiMultiTouchPad;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WellcomeActivity extends Activity 
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.wellcome);
		
		Button button = (Button) findViewById(R.id.buttonStart);
		button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) 
				{
					Intent intent = new Intent();
					setResult( RESULT_OK, intent ); 
					finish();
				}
			});
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		switch (event.getAction()) {
		case KeyEvent.ACTION_DOWN:
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				// アプリ終了
				return super.dispatchKeyEvent(event);
			}
		}
		return super.dispatchKeyEvent(event);
	}
}
