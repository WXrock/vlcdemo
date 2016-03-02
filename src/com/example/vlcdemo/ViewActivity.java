package com.example.vlcdemo;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.opencv.android.OpenCVLoader;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.util.WeakHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class ViewActivity extends Activity {

	private Gallery gallery = null;
	private ImageAdapter adapter = null;
	private String path = null;
	private String[] paths = null;
	private TextView image_info;
	private Button mStitchBut = null;
	private Button mViewBut = null;
	private ImageView image = null;
	
	private ProgressDialog dialog = null;
	
	private Handler mHandler = null;
	private float mconf_thresh;
	private boolean isAuto;
	
	private static final String TAG = "ViewActivity";
	private CheckBoxes mBoxes = null;
	private int chosenPic = 0;
	private int startPos = -1;
	private int endPos = -1;
	
	private Bitmap bm_small;
	private Bitmap bm_big;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.view_layout);
		
		this.image = (ImageView) findViewById(R.id.imageView);
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		mconf_thresh = pref.getFloat(VLCApplication.CONF_THRESH, 1.0f);
		isAuto = pref.getBoolean(VLCApplication.AUTO,true);
		Log.d(TAG,String.valueOf(mconf_thresh));
		
		this.path = getIntent().getStringExtra("fileName");
		if(path != null) {
			this.paths = new String[8];
			for(int i=0;i<8;i++){
				paths[i] = path+i+".jpg";
			}
		}else {
			Log.e(TAG,"path is null");
		}
		
		this.mBoxes = new CheckBoxes();
		this.mBoxes.setBoxes((CheckBox) findViewById(R.id.pic1), (CheckBox) findViewById(R.id.pic2),
				(CheckBox) findViewById(R.id.pic3), (CheckBox) findViewById(R.id.pic4),
				(CheckBox) findViewById(R.id.pic5), (CheckBox) findViewById(R.id.pic6),
				(CheckBox) findViewById(R.id.pic7), (CheckBox) findViewById(R.id.pic8));

		
		this.image_info = (TextView) findViewById(R.id.text_image);
		this.mStitchBut = (Button) findViewById(R.id.proc);
		this.mViewBut = (Button) findViewById(R.id.view);
		this.dialog  = new ProgressDialog(this);
		dialog.setTitle("正在拼接");
		dialog.setMessage("please wait...");
		dialog.setCancelable(true);
		dialog.setIndeterminate(true);
		
		this.mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
					case 0:
						double time = msg.getData().getDouble("time");
						Toast.makeText(getBaseContext(), "用时"+time+"秒", Toast.LENGTH_LONG).show();
						break;
					case 1:
						Toast.makeText(getBaseContext(), "拼接失败", Toast.LENGTH_LONG).show();
						break;
					case 2:
						Toast.makeText(getBaseContext(), "请选择连续的图片", Toast.LENGTH_LONG).show();
						break;
					case 3:
						Toast.makeText(getBaseContext(), "请选择图片", Toast.LENGTH_LONG).show();
						break;
				}
				
			}
			
		};
		
		gallery = (Gallery) findViewById(R.id.gallery);
		adapter = new ImageAdapter(this);
		gallery.setAdapter(adapter);
		gallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				
				bm_big = getBitmap(paths[pos], 8);
				image.setImageBitmap(bm_big);
				ViewActivity.this.image_info.setText(paths[pos]);
			}
		});
		
		
		mStitchBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mBoxes.getCnt() == 0){
					Message msg = new Message();
					msg.what = 3;
					mHandler.sendMessage(msg);
					return;
				}
				if(mBoxes.isLegal() == false){
					Message msg = new Message();
					msg.what = 2;
					mHandler.sendMessage(msg);
					return;
				}
				dialog.show();
				//final ProgressDialog dialog = ProgressDialog.show(ViewActivity.this, "正在拼接 ", "please wait",true);
				new Thread(new Runnable(){
					private double time; 
					private int ret;
					
					@Override
					public void run() {
						
						//Log.e(TAG,mBoxes.getStart()+"TEST"+mBoxes.getCnt());
						if(isAuto == true)
							mconf_thresh = OptionActivity.CONF_THRESH_DEFAULT;
						Log.d(TAG,"start stitching,conf_thresh is "+mconf_thresh + "auto conf:"+isAuto);
						ret = ImageProc.proc(path,mBoxes.getStart(),mBoxes.getCnt(),mconf_thresh,isAuto);
						if(dialog.isShowing())
							dialog.dismiss();
						
						Message msg = new Message();
						if(ret == 0) {
							msg.what = 0;
							Bundle bundle = new Bundle();
							time = ImageProc.getTime();
							bundle.putDouble("time", time);
							msg.setData(bundle);
						}else {
							msg.what = 1;
						}
						mHandler.sendMessage(msg);
						
					}
					
				}).start();	
								
			}
		});
		
		mViewBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				 String resultName = path+"result.jpg";
				 File f = new File(resultName);
				 if(f.exists()){
					 Intent intent = new Intent();
					 intent.setAction(android.content.Intent.ACTION_VIEW);
					 intent.setDataAndType(Uri.parse("file://"+resultName), "image/*");
					 startActivity(intent);
				 }else{
					 Toast.makeText(ViewActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
				 }
				
			}
		});
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

	}
	



	public class ImageAdapter extends BaseAdapter {
		
		Context  context;
		int itemBackfround;
		
		public ImageAdapter(Context c) {
			context = c;
			TypedArray a = obtainStyledAttributes(R.styleable.Gallery1);
			itemBackfround = a.getResourceId(R.styleable.Gallery1_android_galleryItemBackground, 0);
			a.recycle();
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return paths.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			
			if(convertView==null) {
				imageView = new ImageView(context);
				bm_small = getBitmap(paths[position], 64);
				imageView.setImageBitmap(bm_small);
				//imageView.setScaleType(ImageView.ScaleType.FIT_XY);
				imageView.setLayoutParams(new Gallery.LayoutParams(320, 240));
			}else {
				imageView = (ImageView) convertView;
			}
			imageView.setBackgroundResource(itemBackfround);
			
			return imageView;
		}
		
		
	}
	
	
	private Bitmap getBitmap(String path,int ratio){
		
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inSampleSize = ratio;
		Bitmap bitmap = BitmapFactory.decodeFile(path, opt);
	
		return bitmap;
	
	}
	

	@Override
	protected void onResume() {
		super.onResume();
		if(OpenCVLoader.initDebug()){
			//mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
			System.loadLibrary("opencv");
			Log.d(TAG,"load success");
		}else{
			Log.d(TAG,"load failed");
		}
	}
	
	
}
