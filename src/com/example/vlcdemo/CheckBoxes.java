package com.example.vlcdemo;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class CheckBoxes {
	private CheckBox[] mPicBoxs = null;
	private boolean[] tips = null;
	private int count = 0;
	private int start = 0;
	private int end = 0;
	private MyCheckedChangeListener mListener = null;
	
	public CheckBoxes(){
		this.mPicBoxs = new CheckBox[8];
		this.tips = new boolean[8];
		this.mListener = new MyCheckedChangeListener();
		for(int i=0;i<8;i++){
			tips[i] = false;
		}
		
	}
	
	public void setBoxes(CheckBox b1,CheckBox b2,CheckBox b3,CheckBox b4,
			CheckBox b5,CheckBox b6,CheckBox b7,CheckBox b8){
		mPicBoxs[0] = b1;
		mPicBoxs[1] = b2;
		mPicBoxs[2] = b3;
		mPicBoxs[3] = b4;
		mPicBoxs[4] = b5;
		mPicBoxs[5] = b6;
		mPicBoxs[6] = b7;
		mPicBoxs[7] = b8;
		
		for(int i=0;i<8;i++) {
			mPicBoxs[i].setOnCheckedChangeListener(mListener);
		}
		
		
	}
	
	private class MyCheckedChangeListener implements OnCheckedChangeListener{
		
		private int num = 0;

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			switch(buttonView.getId()){
			case R.id.pic1:
				num = 0;
				break;
			case R.id.pic2:
				num = 1;
				break;
			case R.id.pic3:
				num = 2;
				break;
			case R.id.pic4:
				num = 3;
				break;
			case R.id.pic5:
				num = 4;
				break;
			case R.id.pic6:
				num = 5;
				break;
			case R.id.pic7:
				num = 6;
				break;
			case R.id.pic8:
				num = 7;
				break;
			}
			if(isChecked == true){
				count++;
				tips[num] = true;
			}else{
				count--;
				tips[num] = false;
			}
			
		}
	}
	
	public int getCnt(){
		return this.count;
	}
	
	public int getStart(){
		int i = 0;
		while(tips[i] == false)
			i++;
		start = i;
		return this.start;
	}
	
	public boolean isLegal(){
		int i =0;
		while(tips[i] == false)
			i++;
		start = i;
		while(tips[i] == true)
			i++;
		end = i;
		
		if(end-start == count)
			return true;
		else
			return false;
	}
}
