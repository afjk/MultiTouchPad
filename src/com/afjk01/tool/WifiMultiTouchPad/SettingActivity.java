package com.afjk01.tool.WifiMultiTouchPad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SettingActivity extends PreferenceActivity
{
	final String PORT_KEY = "port";
	
	private SharedPreferences sp;

	private String mIP;
	
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		setContentView( R.layout.setting_preference);
		addPreferencesFromResource(R.xml.preference);

		/*
		ListView lv = (ListView)findViewById(android.R.id.list);

		lv.setOnItemClickListener( new OnItemClickListener(){
			// サーバIP選択時の処理。
			@Override
			public void onItemClick(AdapterView<?> parent, View view,int position, long id) 
			{
				Toast.makeText(SettingActivity2.this, "YAR!", Toast.LENGTH_SHORT).show();
			}
		});
		*/
		
		Preference PortPref;
		
		PortPref =  findPreference(PORT_KEY);
		PortPref.setSummary( getString(R.string.port_num_summary)+"(" + sp.getString(PORT_KEY, getString(R.string.default_port)) + ")" );
		

		PortPref =  findPreference("mouse_sense");
		PortPref.setSummary( getString(R.string.mouse_sense_summary)+"(" + String.valueOf( sp.getInt( "mouse_sense", 50 ) ) + "%)" );
		PortPref =  findPreference("scroll_sense");
		PortPref.setSummary( getString(R.string.scroll_sense_summary)+"(" + String.valueOf( sp.getInt( "scroll_sense", 50 ) ) + "%)" );
		
		// 
		EditTextPreference editTextPreference = (EditTextPreference)findPreference(PORT_KEY);
		EditText v = editTextPreference.getEditText();
		v.setInputType(InputType.TYPE_CLASS_NUMBER);
		
	}

	@Override  
	protected void onResume() {  
		super.onResume();  
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);  
	}

	@Override  
	protected void onPause() {  
		super.onPause();  
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);  
	}

	// ここで summary を動的に変更  
	private SharedPreferences.OnSharedPreferenceChangeListener listener =   
		new SharedPreferences.OnSharedPreferenceChangeListener() {  
		   
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {  

			Preference PortPref;		
			PortPref =  findPreference(key);
			
			if(key.equals(PORT_KEY))
			{
				PortPref.setSummary( getString(R.string.port_num_summary)+"(" + sharedPreferences.getString(key, getString(R.string.default_port)) + ")" );
			}
			else if(key.equals("mouse_sense"))
			{
				PortPref.setSummary( getString(R.string.mouse_sense_summary)+"(" + String.valueOf( sharedPreferences.getInt( key, 50 ) ) + "%)" );
			}
			else if(key.equals("scroll_sense"))
			{
				PortPref.setSummary( getString(R.string.scroll_sense_summary)+"(" + String.valueOf( sharedPreferences.getInt( key, 50 ) ) + "%)" );
			}
		}
	};

}
