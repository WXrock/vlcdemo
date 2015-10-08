package com.example.vlcdemo;

import org.videolan.vlc.VLCApplication;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;

public class OptionActivity extends Activity {

	private TextView var_text = null;
	private SeekBar bar = null;
	private CheckBox box = null;
	private float var;
	private SharedPreferences pref =null;
	
	private static final String TAG = "OptionActivity";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.view_option);
		
		this.var_text = (TextView) findViewById(R.id.var_num);
		this.bar = (SeekBar) findViewById(R.id.seekbar);
		this.box = (CheckBox) findViewById(R.id.checkbox);
		
		
		this.bar.setOnSeekBarChangeListener(new SeekBarListener());
		
		pref = PreferenceManager.getDefaultSharedPreferences(OptionActivity.this);
		var = pref.getFloat(VLCApplication.MATCH_CONF, 0.3f); 
		var_text.setText(String.valueOf(var));
		
		if(pref.getBoolean(VLCApplication.FLIP, false) == true)
			box.setChecked(true);
		
		box.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
            	if(arg1 == true){
            		Editor edit = pref.edit();
        			edit.putBoolean(VLCApplication.FLIP, true);
        			edit.commit();
            	}
            }
        });
		
		
		
		
	}

	
	
	private class SeekBarListener implements SeekBar.OnSeekBarChangeListener{

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			var = 0.3f + progress*0.3f/seekBar.getMax();
			var_text.setText(String.format("%.2f", var));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			var = Float.parseFloat((String) var_text.getText());
			Editor edit = pref.edit();
			edit.putFloat(VLCApplication.MATCH_CONF, var);
			edit.commit();
			
		}
		
	}
	
}
