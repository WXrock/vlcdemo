LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES := off
OPENCV_INSTALL_MODULES := on
OPENCV_LIB_TYPE:=SHARED

include E:\Android\openSource\OpenCV-2.4.10-android-sdk\sdk\native\jni\OpenCV.mk   
LOCAL_MODULE    := opencv
LOCAL_SRC_FILES := opencv.cpp autocalib.cpp blenders.cpp camera.cpp exposure_compensate.cpp matchers.cpp \
				motion_estimators.cpp precomp.cpp seam_finders.cpp stitcher.cpp util.cpp warpers.cpp \
				surf.cpp nonfree_init.cpp sift.cpp
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib/ -llog
include $(BUILD_SHARED_LIBRARY)
