package com.example.vlcdemo;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;

import org.opencv.android.OpenCVLoader;
import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.util.VLCInstance;
import org.videolan.vlc.util.WeakHandler;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements IVideoPlayer {

	private static final String TAG = "MAIN";
	private static final String CHANGE = "change";
	private static final String PIC = "pic";
	private static final String PREVIEW = "preview";		
	private static final String BYE = "bye";
	private static final String SEND = "send";
	private static final String MODE = "mode";
	private static final String PIC_DONE = "pic_done";
	private static final String PIC_FAIL = "pic_fail";
	private static final String FLIP = "flip"; 
	private static final String  WIFI = "wifi";
	private static final String  ETHERNET = "ethernet";
	
	private Button mPreviewBut;
	private Button mChangeBut;
	private Button mTakeBut;
	private Button mViewBut;
	private Button mSendBut;
	private EditText mipEditText;
	private Button mConnectBut;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	
	private LibVLC mLibVLC;

	private SharedPreferences pref;
	private Editor edit = null;	
	private static final int SURFACE_BEST_FIT = 0;
	private static final int SURFACE_FIT_HORIZONTAL = 1;
	private static final int SURFACE_FIT_VERTICAL = 2;
	private static final int SURFACE_FILL = 3;
	private static final int SURFACE_16_9 = 4;
	private static final int SURFACE_4_3 = 5;
	private static final int SURFACE_ORIGINAL = 6;
	private int mCurrentSize = SURFACE_BEST_FIT;
	// private EventManager mEventManger;
	private int mVideoHeight;
	private int mVideoWidth;
	private int mSarNum;
	private int mSarDen;
	private int mSurfaceAlign;

	private static final int SURFACE_SIZE = 3;

	
	public static final int LINKERROR = 7;
	public static final int LINKSUCCESS = 8;
	public static final int PICSUCCESS = 9;
	public static final int PICERROR = 10;
	public static final int PICSTART = 11;
	public static final int DOWNSTART =12;
	
	
	private EventHandler eventandler;
	Media rtspmedia;

	private Socket clientSocket = null;
	private SendThread mSendThread = null;
	private OutputStream pout = null;
	private String path = null;
	
	int x = 0;
	int y = 0;
	int action = -1;
	int hostw = 0;
	int hosth = 0;
	int surfx = 0;
	int surfy = 0;
	byte[] msgBuffer = null;
	String out = null;
	boolean change = false;
	boolean isconnected = false;
	DisplayMetrics dm = null;

	private String ip_adress; //id address of 536;
	InputMethodManager inputmanger;
	
	private final VideoEventHandler mEventHandler = new VideoEventHandler(this);
	private final Handler mVideoHandler = new VideoPlayerHandler(this);
	private final Handler mSocketHandler = new SocketHandler(this);

	private String curPath = null;
	private boolean isFlip = false; 
	private boolean isWifi = false;
	private boolean isEth = true;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);

		inputmanger = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mipEditText = (EditText) findViewById(R.id.ip);
		mConnectBut = (Button) findViewById(R.id.btn_CON);
		mPreviewBut = (Button) findViewById(R.id.preiew);
		mChangeBut = (Button) findViewById(R.id.changeCam);
		mTakeBut = (Button) findViewById(R.id.takePic);
		mViewBut = (Button) findViewById(R.id.viewPic);
		mSendBut = (Button) findViewById(R.id.download);
		
		surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(mSurfaceCallback);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceView.setKeepScreenOn(true);
		
		this.pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.edit = pref.edit();
		this.isFlip = pref.getBoolean(VLCApplication.FLIP, false);
		this.isWifi = pref.getBoolean(VLCApplication.WIFI, false);
		this.isEth =  pref.getBoolean(VLCApplication.ETHERNET, true);
		this.curPath = pref.getString("lastPath", null);
		
		
		int pitch;
		String chroma = pref.getString("chroma_format", "");
		if (LibVlcUtil.isGingerbreadOrLater() && chroma.equals("YV12")) {
			surfaceHolder.setFormat(ImageFormat.YV12);
			pitch = ImageFormat.getBitsPerPixel(ImageFormat.YV12) / 8;
		} else if (chroma.equals("RV16")) {
			surfaceHolder.setFormat(PixelFormat.RGB_565);
			PixelFormat info = new PixelFormat();
			PixelFormat.getPixelFormatInfo(PixelFormat.RGB_565, info);
			pitch = info.bytesPerPixel;
		} else {
			surfaceHolder.setFormat(PixelFormat.RGBX_8888);
			PixelFormat info = new PixelFormat();
			PixelFormat.getPixelFormatInfo(PixelFormat.RGBX_8888, info);
			pitch = info.bytesPerPixel;
		}
		mSurfaceAlign = 16 / pitch - 1;
		// LibVLC.useIOMX(getApplicationContext());
		try {
			mLibVLC = VLCInstance.getLibVlcInstance();
		} catch (LibVlcException e) {
			Log.i(TAG, "LibVLC.getInstance() error:" + e.toString());
			e.printStackTrace();
			return;
		}

		mLibVLC.eventVideoPlayerActivityCreated(true);
		mLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_DISABLED); //importante don't change!!!!!

		eventandler = EventHandler.getInstance();
		eventandler.addHandler(mEventHandler);

		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			path = Environment.getExternalStorageDirectory().toString()+"/rec/";
			Log.d(TAG,path);
		}
		File file = new File(path);
		if(!file.exists()){
			file.mkdir();
		}
		
		mConnectBut.setOnClickListener(new ConnectButonclick());
		mPreviewBut.setOnClickListener(new PreviewButonclick());
		mChangeBut.setOnClickListener(new ChangeCamButonclick());
		mTakeBut.setOnClickListener(new TakePicButonclick());
		mViewBut.setOnClickListener(new ViewButonclick());
		mSendBut.setOnClickListener(new SendButtononclick());
		mipEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				// TODO Auto-generated method stub
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					inputmanger.hideSoftInputFromWindow(mipEditText.getWindowToken(), 0);
					ip_adress = mipEditText.getText().toString();

					return true;
				}
				return false;
			}

		});

	}

	class ConnectButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			if(!isconnected){
				ip_adress = mipEditText.getText().toString();
				inputmanger.hideSoftInputFromWindow(mipEditText.getWindowToken(), 0);
				mSendThread = new SendThread();
				mSendThread.start();
			}

		}

	}
	
	class PreviewButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			if(isconnected){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				playvideo();
			}else{
				Toast.makeText(MainActivity.this, "connect first", Toast.LENGTH_LONG).show();
			}
			
		}

	}
	
	class ChangeCamButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			sendSocket(CHANGE);
		}

	}
	
	class TakePicButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			if(mLibVLC.isPlaying()){
				mLibVLC.stop();
			}

			Message msg = new Message();
			msg.arg1 = PICSTART;
			mSocketHandler.sendMessage(msg);
			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sendSocket(MODE);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sendSocket(PIC);
				}
				
				
			}).start();
			
		};
			


	}
	
	class SendButtononclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			sendSocket(SEND);
			
			Message msg = new Message();
			msg.arg1 = DOWNSTART;
			mSocketHandler.sendMessage(msg);
			
			SimpleDateFormat mdate = new SimpleDateFormat("yyyyMMddhhmmss");
			curPath = path + mdate.format(new java.util.Date()) + "_";
			edit.putString("lastPath", curPath);
			edit.commit();
			RecFileThread recThread = new RecFileThread(ip_adress, 9999, curPath,mSocketHandler);
			recThread.start();			
		}
		
	}
	
	class ViewButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			Intent i = new Intent(MainActivity.this, ViewActivity.class);
			if(curPath != null) {
				i.putExtra("fileName",curPath);
				startActivity(i);
			}else{
				Toast.makeText(MainActivity.this, "no picture found!", Toast.LENGTH_SHORT).show();
			}

		}

	}
	
	public class SendThread extends Thread{
		private boolean exit = false;
		private BufferedReader in;

		@Override
		public void run() {
	       try {
                //clientSocket = new Socket(ip_adress, 8888);
                clientSocket = new Socket();
                InetSocketAddress remoteAddr = new InetSocketAddress(ip_adress, 8888);
                clientSocket.connect(remoteAddr, 5000);
                //clientSocket.setSoTimeout(5000);//该方法设置的是读写的超时
                Log.e(TAG, "创建socket");
                isconnected = true;
                pout = clientSocket.getOutputStream();
                in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                mipEditText.setClickable(false);
                Message msg = new Message();
                msg.arg1 = LINKSUCCESS;
                mSocketHandler.sendMessage(msg);
                
                //set setting before start preview
                sendNetSetting();
                
                while(!exit){
                	String str = in.readLine();
                	if(str.equals(PIC_DONE)){
                		Log.e(TAG,"TEST");
                		Message msg1 = new Message();
        	            msg1.arg1 = PICSUCCESS;
                        mSocketHandler.sendMessage(msg1);
                	}else if(str.equals(PIC_FAIL)){
                		Message msg2 = new Message();
        	            msg2.arg1 = PICERROR;
                        mSocketHandler.sendMessage(msg2);
                		Log.e(TAG,"TEST1");
                	}else{
                		Log.e(TAG,"TEST2");
                	}
                }
            } catch (Exception e) {
            	// TODO Auto-generated catch block
                isconnected = false;
                Message msg = new Message();
	            msg.arg1 = LINKERROR;
                Log.d(TAG,"timeout");
                mSocketHandler.sendMessage(msg);
                e.printStackTrace();
            }
		}
		
		public void exit(){
			this.exit = true;
		};
	}
	
    public void reconnect() {
        try {

            clientSocket = new Socket();
            InetSocketAddress remoteAddr = new InetSocketAddress(ip_adress, 8888);
            clientSocket.connect(remoteAddr, 5000);
            Log.e(TAG, "重新连接");
            isconnected = true;
            mipEditText.setClickable(false);

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            isconnected = false;
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            isconnected = false;
            e.printStackTrace();
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent it =  new Intent(MainActivity.this,OptionActivity.class);
			startActivity(it);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void playvideo() {

		String path = "rtsp://"+ip_adress+":8086";
		mLibVLC.playMRL(path);
		Log.d(TAG,"start preview");

	}


	private void sendSocket(String command){
        if (isconnected) {
            try {
                msgBuffer = command.getBytes("UTF-8");
                pout.write(msgBuffer);
                pout.write('\n');
                Log.e(TAG, "Send msg:" + command);

            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //reconnect();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                //reconnect();
                e.printStackTrace();
            }
        }
	}
	
	
	private static class VideoPlayerHandler extends WeakHandler<MainActivity> {
		public VideoPlayerHandler(MainActivity owner) {
			super(owner);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = getOwner();
			if (activity == null) // WeakReference could be GC'ed early
				return;

			switch (msg.what) {
			case SURFACE_SIZE:
				activity.changeSurfaceSize();
				break;
			}
		}
	};

	@Override
	public void setSurfaceSize(int width, int height, int visible_width,
			int visible_height, int sar_num, int sar_den) {
		// TODO Auto-generated method stub
		if (width * height == 0)
			return;
		// store video size
		mVideoHeight = height;
		mVideoWidth = width;
		mSarNum = sar_num;
		mSarDen = sar_den;
		Message msg = mVideoHandler.obtainMessage(SURFACE_SIZE);
		mVideoHandler.sendMessage(msg);

	}

	private final SurfaceHolder.Callback mSurfaceCallback = new Callback() {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			if (format == PixelFormat.RGBX_8888)
				Log.d(TAG, "Pixel format is RGBX_8888");
			else if (format == PixelFormat.RGB_565)
				Log.d(TAG, "Pixel format is RGB_565");
			else if (format == ImageFormat.YV12)
				Log.d(TAG, "Pixel format is YV12");
			else
				Log.d(TAG, "Pixel format is other/unknown");
			if (mLibVLC != null) {
				mLibVLC.attachSurface(surfaceHolder.getSurface(),
						MainActivity.this);
			}
			Log.e(TAG, "Surface changed");
			int xy[] = new int[2];
			surfaceView.getLocationOnScreen(xy);
			Log.e(TAG, "x: " + xy[0] + ", Y: " + xy[1]);
			surfx = xy[0];
			surfy = xy[1];
			hosth = surfaceView.getHeight();
			hostw = surfaceView.getWidth();

		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (mLibVLC != null)
				mLibVLC.detachSurface();
		}
	};


	class VideoEventHandler extends WeakHandler<MainActivity> {
		public VideoEventHandler(MainActivity owner) {
			super(owner);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = getOwner();
			if (activity == null)
				return;
			switch (msg.getData().getInt("event")) {
			case EventHandler.MediaPlayerPlaying:
				Log.i(TAG, "MediaPlayerPlaying");
				break;
			case EventHandler.MediaPlayerPaused:
				Log.i(TAG, "MediaPlayerPaused");
				break;
			case EventHandler.MediaPlayerStopped:
				Log.i(TAG, "MediaPlayerStopped");
				break;
			case EventHandler.MediaPlayerEndReached:
				Log.i(TAG, "MediaPlayerEndReached");
				break;
			case EventHandler.MediaPlayerVout:
				break;
			default:
				// Log.e(TAG, String.format("Event not handled (0x%x)",
				// msg.getData().getInt("event")));
				break;
			}
			super.handleMessage(msg);
		}
	}

	private void changeSurfaceSize() {
		// get screen size
		int dw = surfaceView.getWidth();
		int dh = surfaceView.getHeight();

		// getWindow().getDecorView() doesn't always take orientation into
		// account, we have to correct the values
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		if (dw > dh && isPortrait || dw < dh && !isPortrait) {
			int d = dw;
			dw = dh;
			dh = d;
		}

		// sanity check
		if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
			Log.e(TAG, "Invalid surface size");
			return;
		}

		// compute the aspect ratio
		double ar, vw;
		double density = (double) mSarNum / (double) mSarDen;
		if (density == 1.0) {
			/* No indication about the density, assuming 1:1 */
			vw = mVideoWidth;
			ar = (double) mVideoWidth / (double) mVideoHeight;
		} else {
			/* Use the specified aspect ratio */
			vw = mVideoWidth * density;
			ar = vw / mVideoHeight;
		}

		// compute the display aspect ratio
		double dar = (double) dw / (double) dh;

		switch (mCurrentSize) {
		case SURFACE_BEST_FIT:
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_FIT_HORIZONTAL:
			dh = (int) (dw / ar);
			break;
		case SURFACE_FIT_VERTICAL:
			dw = (int) (dh * ar);
			break;
		case SURFACE_FILL:
			break;
		case SURFACE_16_9:
			ar = 16.0 / 9.0;
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_4_3:
			ar = 4.0 / 3.0;
			if (dar < ar)
				dh = (int) (dw / ar);
			else
				dw = (int) (dh * ar);
			break;
		case SURFACE_ORIGINAL:
			dh = mVideoHeight;
			dw = (int) vw;
			break;
		}

		// align width on 16bytes
		int alignedWidth = (mVideoWidth + mSurfaceAlign) & ~mSurfaceAlign;

		// force surface buffer size
		surfaceHolder.setFixedSize(alignedWidth, mVideoHeight);

		// set display size
		LayoutParams lp = surfaceView.getLayoutParams();
		lp.width = dw * alignedWidth / mVideoWidth;
		lp.height = dh;
		surfaceView.setLayoutParams(lp);
		surfaceView.invalidate();
	}
	
	
	
	void sendNetSetting(){
		//send flip setting
		if(isFlip){
		sendSocket(FLIP);
		Log.d(TAG,"FLIP");
		}

		//send wifi setting
		if(isWifi){
			sendSocket(WIFI);
			Log.d(TAG,"WIFI SET");
		}

		//send eth setting
		if(isEth){
			sendSocket(ETHERNET);
			Log.d(TAG,"WIFI SET");
		}
	}

	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub

		EventHandler em = EventHandler.getInstance();
		em.removeHandler(mVideoHandler);
		mLibVLC.eventVideoPlayerActivityCreated(false);
		mLibVLC.stop();
		mLibVLC.clearBuffer();
		mLibVLC.destroy();
		mLibVLC.closeAout();
		mLibVLC.detachSurface();
		mLibVLC.stopDebugBuffer();
				
		if (isconnected) {
            String out = BYE;
            try {
                msgBuffer = out.getBytes("UTF-8");
                pout = clientSocket.getOutputStream();
                pout.write(msgBuffer);
                pout.write('\n');
                Log.e(TAG, "Send msg:" + out);

            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //reconnect();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                //reconnect();
                e.printStackTrace();
            }
        }
		
		android.os.Process.killProcess(android.os.Process.myPid());
		super.onDestroy();

	}

}