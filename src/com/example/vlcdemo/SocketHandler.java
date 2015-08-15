package com.example.vlcdemo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class SocketHandler extends Handler {

	private Context context;
	public SocketHandler(Context context){
		this.context = context;
	}
	
	@Override
	public void handleMessage(Message msg) {
		
		switch(msg.arg1){
		case RecFileThread.REC_DONE:
			Toast.makeText(context, "����ͼƬ���", Toast.LENGTH_LONG).show();
			break;
		case MainActivity.LINKERROR:
			Toast.makeText(context, "����ʧ�ܣ��鿴�������Ƿ���", Toast.LENGTH_LONG).show();
			break;
		case MainActivity.LINKSUCCESS:
			Toast.makeText(context, "���ӳɹ�", Toast.LENGTH_LONG).show();
			break;
		case MainActivity.PICSUCCESS:
			Toast.makeText(context, "���ճɹ�", Toast.LENGTH_LONG).show();
			break;
		case MainActivity.PICERROR:
			Toast.makeText(context, "���ճɹ�", Toast.LENGTH_LONG).show();
			break;
		}
		super.handleMessage(msg);
	}
}
