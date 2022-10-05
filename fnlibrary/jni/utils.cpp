//
// Created by MAxim Akristiniy on 15.12.20.
//

#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <assert.h>
#include <stdio.h>
#include "android/log.h"
#include <sys/system_properties.h>

#include "rs_fncore_UrovoUtils.h"

static const char *TAG = "fncore2";
#define LOGI(fmt, args...) {  __android_log_print(ANDROID_LOG_INFO,TAG,fmt,##args); }
#define LOGD(fmt, args...) { __android_log_print(ANDROID_LOG_DEBUG,TAG,fmt,##args); }
#define LOGE(fmt, args...) { __android_log_print(ANDROID_LOG_ERROR,TAG,fmt,##args); }

static const char * OTG_ENABLE_PATH_SQ27T=  "/sys/devices/soc.0/78d9000.usb/otg_enable";
static const char * OTG_ENABLE_PATH_SQ27TGW="/sys/devices/soc/78d9000.usb/otg_enable";

enum deviceModelE{
    DM_unknown,
    DM_SQ29,
    DM_SQ27T,
    DM_SQ27TGW,
};

bool getOtgStatus(const char * path){
    bool res=true;
    int fd;
    char curr_val[1]={};
    fd = open(path, O_RDONLY | O_SYNC);
    if (fd <=0 ) {
        return false;
    }

    if(lseek(fd, 0L, SEEK_SET)<0) {
        res = false;
    }

    if (read(fd,curr_val, sizeof(curr_val))!=sizeof(curr_val)) {
        res = false;
    }

    close(fd);

    LOGI("OTG current status: %c", curr_val[0]);
    return (curr_val[0]=='1'?true:false);
}

bool setOtgStatus(const char * path, bool status){
    int res=true;
    int fd;
    char new_val[2]={status?'1':'0',0};
    LOGI("set OTG status: %c", new_val[0]);

    fd = open(path, O_RDWR | O_SYNC);
    if (fd <=0 ) {
        LOGE("unable open file %s", path);
        return false;
    }

    if(lseek(fd, 0L, SEEK_SET)<0) {
        LOGE("could not reposition the indicator");
        res = false;
    }

    if (write(fd, new_val, sizeof(new_val))!=sizeof(new_val)){
        LOGE("unable write file %s", path);
        res = false;
    }

    close(fd);

    return res;
}

JNIEXPORT jboolean JNICALL Java_rs_fncore_UrovoUtils_getOtgStatus(JNIEnv *env, jclass clazz, jint deviceModel) {
    const char * path;
    switch (deviceModel){
        case DM_SQ27TGW:
            path=OTG_ENABLE_PATH_SQ27TGW;
            break;
        case DM_unknown:
        case DM_SQ29:
        case DM_SQ27T:
        default:
            path=OTG_ENABLE_PATH_SQ27T;
            break;
    }
    return getOtgStatus(path)?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_rs_fncore_UrovoUtils_enableOtg(JNIEnv *env, jclass clazz, jint deviceModel, jboolean enable) {
    const char * path;
    switch (deviceModel){
        case DM_SQ27TGW:
            path=OTG_ENABLE_PATH_SQ27TGW;
            break;
        case DM_unknown:
        case DM_SQ29:
        case DM_SQ27T:
        default:
            path=OTG_ENABLE_PATH_SQ27T;
            break;
    }

    bool newStatus=(enable == JNI_TRUE);
    bool oldStatus=getOtgStatus(path);
    if (newStatus != oldStatus){
        LOGI("OTG oldStatus: %d, newStatus:%d", oldStatus, newStatus);
        return setOtgStatus(path,newStatus) ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_TRUE;
}

static JNINativeMethod JNI_NATIVES[] = {
		{
				"enableOtg",
				"(IZ)Z",
				(void *)&Java_rs_fncore_UrovoUtils_enableOtg
		},
		{
				"getOtgStatus",
				"(I)Z",
				(void *)&Java_rs_fncore_UrovoUtils_getOtgStatus

		}
};

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	LOGI("Register natives");
	JNIEnv *env;
	vm->GetEnv((void **)&env, JNI_VERSION_1_6);
	jclass clazz = env->FindClass("rs/fncore/UrovoUtils");
	env->RegisterNatives(clazz,JNI_NATIVES,sizeof(JNI_NATIVES)/sizeof(JNINativeMethod));
	LOGI("Register natives done");
	return JNI_VERSION_1_6;
}
