package com.example.vlcdemo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class SocketHandler extends Handler {

	private Context context;
	private Dialog dialog;
	public SocketHandler(Context context){
		this.context = context;
	}
	
	@Override
	public void handleMessage(Message msg) {
		
		switch(msg.arg1){
		case RecFileThread.REC_DONE:
			Toast.makeText(context, "接收图片完成", Toast.LENGTH_LONG).show();
			dialog.dismiss();
			break;
		case MainActivity.LINKERROR:
			Toast.makeText(context, "连接失败，查看服务器是否开启", Toast.LENGTH_LONG).show();
			break;
		case MainActivity.LINKSUCCESS:
			Toast.makeText(context, "连接成功", Toast.LENGTH_LONG).show();
			break;
		case MainActivity.PICSUCCESS:
			Toast.makeText(context, "拍照成功", Toast.LENGTH_LONG).show();
			dialog.dismiss();
			break;
		case MainActivity.PICERROR:
			Toast.makeText(context, "拍照失败", Toast.LENGTH_LONG).show();
			break;
		case MainActivity.DOWNSTART:
			dialog = ProgressDialog.show(context, "正在下载 ", "please wait",true);
			break;
		case MainActivity.PICSTART:
			dialog = ProgressDialog.show(context, "正在拍照 ", "please wait",true);
			break;
		}
		super.handleMessage(msg);
	}
}
