package com.example.vlcdemo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class RecFileThread extends Thread implements Runnable{
	public static final String TAG = "RecFileThread";
	public static final int REC_DONE = 0;
	private int port;
	private String ipaddress;
	private Socket client;
	private DataInputStream dis = null;
	private FileOutputStream fos = null;
	private DataOutputStream dos = null;
	private Handler handler;
	byte[] buffer = null;
	File[] files;
	int totalByte;
	int read;
	
	public RecFileThread(String ipaddress,int port,String path,Handler handler){
		this.handler = handler;
		this.port = port;
		this.ipaddress = ipaddress;
		this.buffer = new byte[2048];
		this.files = new File[8];
		for(int i=0;i<8;i++){
			files[i] = new File(path+i+".jpg");
		}
		
	}

	@Override
	public void run() {
		try {
			Thread.sleep(200);
			this.client = new Socket(ipaddress,port);
			client.setSoTimeout(5000);
			Log.d(TAG,"file socket init");
			
			this.dis = new DataInputStream(client.getInputStream());
			this.dos = new DataOutputStream(client.getOutputStream());
			//totalByte = dis.readInt();
			//Log.d(TAG,String.valueOf(totalByte));
			
			for(int i=0;i<8;i++){
				int fileLength = dis.readInt();
				Log.d(TAG,"file_length:"+String.valueOf(fileLength));
				int recFileLength = 0;
				fos = new FileOutputStream(files[i]);
				Log.d(TAG,files[i].toString());
				while(recFileLength <fileLength){
					read = dis.read(buffer);
					recFileLength += read;
					fos.write(buffer, 0, read);
					//Log.d(TAG,"RECEIVE:"+i+":"+recFileLength);
				}
				Log.d(TAG,"RECEIVE:"+i);
				fos.flush();
				fos.close();
				dos.write("ok".getBytes());
				dos.write('\n');
			}
			
			dis.close();
			fos.close();
			client.close();
			Message msg = new Message();
			msg.arg1 = REC_DONE;
			handler.sendMessage(msg);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e){
			e.printStackTrace();
		}				
		
		
	}
	
	
	
	
	

}
