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
import android.widget.Toast;

public class OptionActivity extends Activity {

	private TextView var_text = null;
	private SeekBar bar = null;
	private CheckBox flipbox = null;
	private CheckBox wifibox = null;
	private CheckBox ethbox = null;
	private CheckBox autobox = null;
	private float var;
	private SharedPreferences pref =null;
	
	private static final float MIN_CONF_THRESH = 0.4f;
	public static final float CONF_THRESH_DEFAULT = 1.0f;
	
	private static final String TAG = "OptionActivity";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.view_option);
		
		this.var_text = (TextView) findViewById(R.id.var_num);
		this.bar = (SeekBar) findViewById(R.id.seekbar);
		this.flipbox = (CheckBox) findViewById(R.id.checkbox_flip);
		this.wifibox = (CheckBox) findViewById(R.id.checkbox_wifi);
		this.ethbox = (CheckBox) findViewById(R.id.checkbox_eth);
		this.autobox = (CheckBox) findViewById(R.id.checkbox_auto);
		
		this.bar.setOnSeekBarChangeListener(new SeekBarListener());
		
		
		pref = PreferenceManager.getDefaultSharedPreferences(OptionActivity.this);
		var = pref.getFloat(VLCApplication.CONF_THRESH, CONF_THRESH_DEFAULT); 
		var_text.setText(String.valueOf(var));
		
		setChecked();
		
		
		flipbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
            	Editor edit = pref.edit();
            	if(arg1 == true){       		
        			edit.putBoolean(VLCApplication.FLIP, true);
            	}else{
        			edit.putBoolean(VLCApplication.FLIP, false);      			
            	}
            	edit.commit();
            }
        });
		
		
		wifibox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
            	Editor edit = pref.edit();
            	if(arg1 == true){
            		if(pref.getBoolean(VLCApplication.ETHERNET, false) == true){
            			Toast.makeText(OptionActivity.this, "can't set both connection at same time!!", Toast.LENGTH_LONG).show();
            			wifibox.setChecked(false);
            			return;
            		}
        			edit.putBoolean(VLCApplication.WIFI, true);
            	}else{
        			edit.putBoolean(VLCApplication.WIFI, false);      			
            	}
            	edit.commit();
            }
        });
		
		ethbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
            	Editor edit = pref.edit();
            	if(arg1 == true){       		
            		if(pref.getBoolean(VLCApplication.WIFI, false) == true){
            			Toast.makeText(OptionActivity.this, "can't set both connection at same time!!", Toast.LENGTH_LONG).show();
            			ethbox.setChecked(false);
            			return;
            		}
        			edit.putBoolean(VLCApplication.ETHERNET, true);
            	}else{
        			edit.putBoolean(VLCApplication.ETHERNET, false);      			
            	}
            	edit.commit();
            }
        });
		
		autobox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
            	Editor edit = pref.edit();
            	if(arg1 == true){       		
        			edit.putBoolean(VLCApplication.AUTO, true);
            	}else{
        			edit.putBoolean(VLCApplication.AUTO, false); 
        			edit.putFloat(VLCApplication.CONF_THRESH, CONF_THRESH_DEFAULT);
        			var_text.setText(String.valueOf(CONF_THRESH_DEFAULT));
        			
            	}
            	edit.commit();
            }
        });
		
		
	}	
	
	private void setChecked() {
		
			flipbox.setChecked(pref.getBoolean(VLCApplication.FLIP, false));
			wifibox.setChecked(pref.getBoolean(VLCApplication.WIFI, false));
			ethbox.setChecked(pref.getBoolean(VLCApplication.ETHERNET, true));
			autobox.setChecked(pref.getBoolean(VLCApplication.AUTO, true));
		
	}

	private class SeekBarListener implements SeekBar.OnSeekBarChangeListener{

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			var = MIN_CONF_THRESH + progress*(MIN_CONF_THRESH+1.0f)/seekBar.getMax();
			var_text.setText(String.format("%.2f", var));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			var = Float.parseFloat((String) var_text.getText());
			Editor edit = pref.edit();
			edit.putFloat(VLCApplication.CONF_THRESH, var);
			edit.commit();
			
		}
		
	}
	
}
