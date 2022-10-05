LOCAL_PATH:=$(call my-dir)

#include $(CLEAR_VARS)
#LOCAL_MODULE := update
#LOCAL_SRC_FILES := ../lib/libupdate.so
#include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := urovo_utils
LOCAL_SRC_FILES := utils.cpp
LOCAL_LDLIBS := -lm -llog -lstdc++
include $(BUILD_SHARED_LIBRARY)




