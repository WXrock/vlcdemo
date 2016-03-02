package com.example.vlcdemo;

public class ImageProc {
	public static native int proc(String path,int pstart,int pnum,float mconf_thresh,boolean isAuto);
	public static native double getTime();
	public static native String getResultName();

}

