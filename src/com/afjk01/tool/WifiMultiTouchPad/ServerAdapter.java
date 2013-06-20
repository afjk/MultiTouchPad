package com.afjk01.tool.WifiMultiTouchPad;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ServerAdapter extends ArrayAdapter<ServerBean> 
{

	private ArrayList<ServerBean> items;
	private LayoutInflater inflater;

	public ServerAdapter(Context context, int textViewResourceId, ArrayList<ServerBean> items) 
	{
		super(context, textViewResourceId, items);
		/*
		if( items.size() == 0 )
		{
			items.add( new ServerBean(context.getString( R.string.add_server_ip ),"") );
		}
		*/
		this.items = items;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
	}
	  
	@Override  
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// ビューを受け取る  
		View view = convertView;
		if (view == null)
		{
			// 受け取ったビューがnullなら新しくビューを生成  
			view = inflater.inflate(R.layout.list_at, null);
		}
		
		ServerBean severBean = (ServerBean)items.get(position);
		
		TextView text = (TextView)view.findViewById(R.id.IPAddress);
		text.setText( severBean.IP );
		text = (TextView)view.findViewById(R.id.HostName);
		text.setText( severBean.hostname );
		
		return view;
	}

}
