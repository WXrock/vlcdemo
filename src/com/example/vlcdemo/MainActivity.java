package com.example.vlcdemo;


import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;
import org.videolan.vlc.util.VLCInstance;
import org.videolan.vlc.util.WeakHandler;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
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
	

	
	private Button mPreviewBut;
	private Button mChangeBut;
	private Button mTakeBut;
	private Button mViewBut;
	private EditText mipEditText;
	private Button mConnectBut;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	
	private LibVLC mLibVLC;
	private int savedIndexPosition = -1;
	private AudioManager mAudioManager;
	private int mAudioMax;
	private SharedPreferences mSettings;
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
	private static final int OVERLAY_TIMEOUT = 4000;
	private static final int OVERLAY_INFINITE = 3600000;
	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;
	private static final int SURFACE_SIZE = 3;
	private static final int AUDIO_SERVICE_CONNECTION_SUCCESS = 5;
	private static final int AUDIO_SERVICE_CONNECTION_FAILED = 6;
	private static final int FADE_OUT_INFO = 4;
	private EventHandler eventandler;
	Media rtspmedia;

	private Socket clientSocket = null;
	private OutputStream pout = null;
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
	private final Handler mHandler = new VideoPlayerHandler(this);


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		inputmanger = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mipEditText = (EditText) findViewById(R.id.ip);
		mConnectBut = (Button) findViewById(R.id.btn_CON);
		mPreviewBut = (Button) findViewById(R.id.preiew);
		mChangeBut = (Button) findViewById(R.id.changeCam);
		mTakeBut = (Button) findViewById(R.id.takePic);
		mViewBut = (Button) findViewById(R.id.viewPic);

		surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(mSurfaceCallback);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceView.setKeepScreenOn(true);
		
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
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
		mLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_AUTOMATIC);

		eventandler = EventHandler.getInstance();
		eventandler.addHandler(mEventHandler);

		mConnectBut.setOnClickListener(new ConnectButonclick());
		mPreviewBut.setOnClickListener(new previewButonclick());
		mChangeBut.setOnClickListener(new changeCamButonclick());
		mTakeBut.setOnClickListener(new takePicButonclick());
		mViewBut.setOnClickListener(new ViewButonclick());
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
				SendThead mThread = new SendThead();
				mThread.start();
			}

		}

	}
	
	class previewButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			if(isconnected){
				String out = PREVIEW;
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
				playvideo();
			}else{
				Toast.makeText(MainActivity.this, "connect first", Toast.LENGTH_LONG).show();
				playvideo();
			}
			
		}

	}
	
	class changeCamButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
	         if (isconnected) {
                 String out = CHANGE;
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

		}

	}
	
	class takePicButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
	         if (isconnected) {
                 String out = PIC;
                 try {
                     msgBuffer = out.getBytes("UTF-8");
                     pout = clientSocket.getOutputStream();
                     pout.write(msgBuffer);
                     pout.write('\n');
                     Log.e(TAG, "Send msg:" + out);

                 } catch (UnsupportedEncodingException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                    // reconnect();
                 } catch (IOException e) {
                     // TODO Auto-generated catch block
                    // reconnect();
                     e.printStackTrace();
                 }
             }
		}

	}
	
	class ViewButonclick implements Button.OnClickListener {

		@Override
		public void onClick(View v) {

		}

	}
	
	public class SendThead extends Thread{

		@Override
		public void run() {
	       try {
                clientSocket = new Socket(ip_adress, 8888);
                clientSocket.setSoTimeout(5000);
                Log.e(TAG, "¥¥Ω®socket");
                isconnected = true;
                mipEditText.setClickable(false);
            } catch (Exception e) {
            	// TODO Auto-generated catch block
                isconnected = false;
               // Message msg = new Message();
	           // msg.what = LINKERROR;
                Log.d(TAG,"timeout");
                e.printStackTrace();
            }
		}
	}
	
    public void reconnect() {
        try {

            clientSocket = new Socket(ip_adress, 8888);
            clientSocket.setSoTimeout(5000);
            Log.e("Send", "ÈáçÊñ∞ËøûÊé•ÊàêÂäü");
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
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public Void playvideo() {

		String path = "rtsp://"+ip_adress+":8086";
		mLibVLC.playMRL(path);
		Log.d(TAG,"start preview");
		return null;

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
		Message msg = mHandler.obtainMessage(SURFACE_SIZE);
		mHandler.sendMessage(msg);

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
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub

		EventHandler em = EventHandler.getInstance();
		em.removeHandler(mHandler);
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