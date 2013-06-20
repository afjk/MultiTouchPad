package com.afjk01.tool.WifiMultiTouchPad;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
//import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;


public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener 
{
    private SeekBar bar;
    private Context mContext;
    private static SharedPreferences sp;

    private static final int OPT_SEEKBAR_DEF = 30;
    private static final int LAYOUT_PADDING = 10;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }
    public SeekBarPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
        mContext = context;
	}
    
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        // TODO Auto-generated method stub
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setPadding(LAYOUT_PADDING, LAYOUT_PADDING, LAYOUT_PADDING, LAYOUT_PADDING);
        bar = new SeekBar(mContext);
        bar.setOnSeekBarChangeListener(this);
        String key = getKey();
        
        bar.setProgress(getValue());
        layout.addView(bar, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        return layout;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            setValue(bar.getProgress());
        }
    }

    private void setValue(int value) {
        Editor ed = sp.edit();
        ed.putInt(getKey(), value);
        ed.commit();
    }

    private int getValue() {
        return sp.getInt(getKey(), 50);
    }
}