/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_wx_grayprossndk_ImageProc */

#ifndef _Included_com_wx_grayprossndk_ImageProc
#define _Included_com_wx_grayprossndk_ImageProc
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_wx_grayprossndk_ImageProc
 * Method:    grayProc
 * Signature: ([III)[I
 */
JNIEXPORT jint JNICALL Java_com_example_vlcdemo_ImageProc_proc
  (JNIEnv *env, jclass obj,jstring path,jint width,jint height);
JNIEXPORT jdouble JNICALL Java_com_example_vlcdemo_ImageProc_getTime
		(JNIEnv *env, jclass obj);
  
JNIEXPORT jstring JNICALL Java_com_example_vlcdemo_ImageProc_getResultName
		(JNIEnv *env, jclass obj);
#ifdef __cplusplus
}
#endif
#endif