package com.afjk01.tool.WifiMultiTouchPad;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.Preference;
import android.util.AttributeSet;

public class CustomPreference extends Preference {

	public CustomPreference(Context context) {
		super(context);
		// 処理なし
	}
	public CustomPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// 処理なし
	}
	
	public CustomPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// 処理なし
	}

	@Override
	public void onClick()
	{
		Context context = getContext();
		
		String key = getKey();
		
		if( key.equals("server_ip") )
		{
			Intent intent = new Intent(  context, ServerSettingActivity.class);
			// 次画面のアクティビティ起動
			context.startActivity(intent);
		}
		else if( key.equals("about") )
		{
			String versionName = "";
			PackageManager packageManager = context.getPackageManager();

			try
			{
				PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
				versionName = packageInfo.versionName;
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}

			String message = "";
			message += context.getString( R.string.app_name );
			message += "\nversion:" + versionName;
			message += "\n" + context.getString( R.string.about_this_message);

	        Builder mBuilder = new AlertDialog.Builder(context)
	            .setTitle("about")
	            .setMessage( message )
	            .setPositiveButton("OK", null);

	        // Create the dialog
	        final Dialog dialog = mBuilder.create();
	        
	        dialog.show();
		}
	}
}
