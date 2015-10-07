package com.example.vlcdemo;

import org.opencv.android.OpenCVLoader;
import org.videolan.vlc.util.WeakHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
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
	private Button mbut = null;
	private Handler mHandler = null;
	
	private static final String TAG = "ViewActivity";
	private static int WIDTH = 2048;
	private static int HEIGHT = 1536;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.view_layout);
		
		this.path = getIntent().getStringExtra("fileName");
		if(path != null) {
			this.paths = new String[8];
			for(int i=0;i<8;i++){
				paths[i] = path+i+".jpg";
			}
		}else {
			Log.e(TAG,"path is null");
		}
		
		this.image_info = (TextView) findViewById(R.id.text_image);
		this.mbut = (Button) findViewById(R.id.proc);
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
				ImageView image = (ImageView) findViewById(R.id.imageView);
				Bitmap bm = getBitmap(paths[pos], 4);
				image.setImageBitmap(bm);
				image.setAdjustViewBounds(true);
				image.setMaxHeight(640);
				//image.setMaxWidth(480);
				ViewActivity.this.image_info.setText(paths[pos]);
			}
		});
		
		
		mbut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				final ProgressDialog dialog = ProgressDialog.show(ViewActivity.this, "正在拼接 ", "please wait",true);
				new Thread(new Runnable(){
					private double time; 
					private int ret;
					
					@Override
					public void run() {
						
						ret = ImageProc.proc(path, WIDTH, HEIGHT);
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
			Bitmap bm;
			if(convertView==null) {
				imageView = new ImageView(context);
				bm = getBitmap(paths[position], 16);
				imageView.setImageBitmap(bm);
				imageView.setScaleType(ImageView.ScaleType.FIT_XY);
				imageView.setLayoutParams(new Gallery.LayoutParams(128, 96));
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
