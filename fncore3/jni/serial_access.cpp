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

#include "rs_fncore2_fn_storage_serial_SStorage.h"

static const char *TAG = "rs.serial.fncore2";
#define LOGI(fmt, args...) {  __android_log_print(ANDROID_LOG_INFO,TAG,fmt,##args); }
#define LOGD(fmt, args...) { __android_log_print(ANDROID_LOG_DEBUG,TAG,fmt,##args); }
#define LOGE(fmt, args...) { __android_log_print(ANDROID_LOG_ERROR,TAG,fmt,##args); }

#define MAX_PATH 255

#define  property_get(name, value, default_value)  __system_property_get(name, value)

static const char *DEVICES[] =
        {"/dev/ttyHSL0", "/dev/ttyHSL1", "/dev/ttyUSB"};

static const char *PWR_PIN[] = {"/sys/class/ugp_ctrl/gp_usb_sw_ctrl/enable",
                                "/sys/class/ugp_ctrl/gp_sys_5v_ctrl/enable"};

static jboolean state = 0;

static int set_opt(int fd, int nSpeed, int nBits, char nEvent, int nStop) {
    struct termios newtio, oldtio;
    /*保存测试现有串口参数设置，在这里如果串口号出错，会有相关的出错信息*/
    if (tcgetattr(fd, &oldtio) != 0) {
        LOGE("SetupSerial 1");
        return -1;
    }

    memset(&newtio, 0, sizeof(newtio));

    /*步骤一：设置字符大小*/
    newtio.c_cflag |= CLOCAL | CREAD;
    newtio.c_cflag &= ~CSIZE;

    /*设置停止位*/
    switch (nBits) {
        case 5:
            newtio.c_cflag |= CS5;
            break;
        case 6:
            newtio.c_cflag |= CS6;
            break;
        case 7:
            newtio.c_cflag |= CS7;
            break;
        case 8:
            newtio.c_cflag |= CS8;
            break;
    }

    /*设置奇偶校验位*/
    switch (nEvent) {
        case 'O': //奇数
        case 'o':
            newtio.c_cflag |= PARENB;
            newtio.c_cflag |= PARODD;
            newtio.c_iflag |= (INPCK | ISTRIP);
            break;
        case 'E': //偶数
        case 'e':
            newtio.c_iflag |= (INPCK | ISTRIP);
            newtio.c_cflag |= PARENB;
            newtio.c_cflag &= ~PARODD;
            break;
        case 'N': //无奇偶校验位
        case 'n':
            newtio.c_cflag &= ~PARENB;
            break;
    }

    /*设置波特率*/
    switch (nSpeed) {
        case 2400:
            cfsetispeed(&newtio, B2400);
            cfsetospeed(&newtio, B2400);
            break;
        case 4800:
            cfsetispeed(&newtio, B4800);
            cfsetospeed(&newtio, B4800);
            break;
        case 9600:
            cfsetispeed(&newtio, B9600);
            cfsetospeed(&newtio, B9600);
            break;
        case 115200:

            cfsetispeed(&newtio, B115200);
            cfsetospeed(&newtio, B115200);
            break;
        case 460800:
            cfsetispeed(&newtio, B460800);
            cfsetospeed(&newtio, B460800);
            break;
        default:
            cfsetispeed(&newtio, B9600);
            cfsetospeed(&newtio, B9600);
            break;
    }

    /*设置停止位*/
    if (nStop == 1) {
        newtio.c_cflag &= ~CSTOPB;
    } else if (nStop == 2) {
        newtio.c_cflag |= CSTOPB;
    }

    /*设置等待时间和最小接收字符*/
    newtio.c_cc[VTIME] = 0;
    newtio.c_cc[VMIN] = 0;

    /*处理未接收字符*/
    tcflush(fd, TCIFLUSH);

    /*激活新配置*/
    if ((tcsetattr(fd, TCSANOW, &newtio)) != 0) {
        LOGE("COM set error");
        return -1;
    }
    LOGI("Set Done!");
    return 0;
}

/*****************************
 * 功能：打开串口函数
 *****************************/
static int open_port(const char *path_utf,/*int comport,*/int nSpeed, int nBits,
                     char nEvent, int nStop) {
    long vdisable;

    //int fd = open(dev[comport - 1], O_RDWR | O_NOCTTY | O_NDELAY);
    int fd = open(path_utf, O_RDWR | O_NOCTTY | O_NDELAY);
    if (fd == -1) {
        LOGE("Can't Open Serial Port %s, error=%d", path_utf, errno);
        return -1;
    }

    /*恢复串口为阻塞状态*/
    if (fcntl(fd, F_SETFL, 0) < 0) {
        LOGE("fcntl failed!/n");
    } else LOGI("fcntl=%d", fcntl(fd, F_SETFL, 0));

    if (set_opt(fd, nSpeed, nBits, nEvent, nStop) != 0) {
        return -1;
    }
    LOGI("fd-open=%d", fd);
    return fd;
}

static jint releaseScannerSerial(JNIEnv *env, jint flag) {

    jclass strClass = env->FindClass("android/device/ScanManager");
    if (strClass == NULL) {
        LOGE("find android/device/ScanManager error");
        return -2;
    }

    jmethodID ctorID = env->GetMethodID(strClass, "<init>", "()V");
    if (ctorID == NULL) {
        LOGE("find android/device/ScanManager init methodid error");
        return -2;
    }
    jobject obj = env->NewObject(strClass, ctorID);
    if (obj == NULL) {
        LOGE(" android/device/ScanManager NewObject error \n");
        return -2;
    }
    if (flag == 1) {
        jmethodID scanStateID = env->GetMethodID(strClass, "getScannerState",
                                                 "()Z");
        if (scanStateID == NULL) {
            LOGE(
                    " android/device/ScanManager get getScannerState method id error \n");
            return -2;
        }

        state = env->CallBooleanMethod(obj, scanStateID);
        LOGD(" current scanner =-==================================%d\n", state);
        if (state) {
            jmethodID closeID = env->GetMethodID(strClass, "closeScanner",
                                                 "()Z");
            if (closeID == NULL) {
                LOGE(
                        " android/device/ScanManager get closeScanner method id error");
                return -2;
            }
            env->CallBooleanMethod(obj, closeID);

            LOGD(" close scanner ================================== ok\n");
        }
    } else {
        if (state) {
            jmethodID openID = env->GetMethodID(strClass, "openScanner", "()Z");
            if (openID == NULL) {
                LOGE(
                        " android/device/ScanManager get openScanner method id error \n");
                return -2;
            }
            env->CallBooleanMethod(obj, openID);

            LOGD(" open scanner ================================== ok\n");
        }
    }
    usleep(10000);
    return 0;

}

static int checkDeviceHW() {
    char propertyValue[PROP_VALUE_MAX];
    memset(propertyValue, 0, PROP_VALUE_MAX);
    int ret = property_get("pwv.have.scanner", propertyValue, "");
    LOGI(" property_get=pwv.have.scanner: %s \n", propertyValue);
    if (strcmp(propertyValue, "true") != 0) {
        LOGI(" property_get pwv.have.scanner: %s \n", propertyValue);
        return -1;
    }
    return 0;
}

static int urovo_pin_powerOn(int pinId) {
    char buff[1];
    int fd = -1, ret = -1;
    LOGI("-------------------power on pin ID: %d------------", pinId);
    fd = open("/sys/class/ugp_ctrl/gp_scan_debug_switch/enable",
              O_RDWR | O_SYNC);
    if (fd > 0) {
        buff[0] = '0';
        ret = write(fd, buff, 1);
        close(fd);
    }
    LOGD("Enabling power on %s", PWR_PIN[pinId]);
    fd = open(PWR_PIN[pinId], O_RDWR | O_SYNC);
    if (fd <= 0) {
        LOGE("open PWR error %d\n", errno);
        return -1;
    }

    buff[0] = '1';
    ret = write(fd, buff, 1);
    close(fd);
    LOGI("-------------------power on write %d------------", ret);
    return 0;
}

int urovo_pin_powerOff(int pinId) {
    char buff[1];
    int fd = -1, ret = -1;
    LOGI("-------------------power off pin ID: %d------------", pinId);
    fd = open("/sys/class/ugp_ctrl/gp_scan_debug_switch/enable",
              O_RDWR | O_SYNC);
    if (fd > 0) {
        buff[0] = '1';
        ret = write(fd, buff, 1);
        close(fd);
    }
    fd = open(PWR_PIN[pinId], O_RDWR | O_SYNC);
    if (fd <= 0) {
        LOGE("open uart error %d\n", errno);
        return -1;
    }

    buff[0] = '0';
    ret = write(fd, buff, 1);
    close(fd);
    LOGI("-------------------power off write %d------------", ret);
    return 0;
}

static int pwr_on = 0;
static int has_scanner = 0;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    has_scanner = checkDeviceHW() == 0;
    return JNI_VERSION_1_6;
}

JNIEXPORT jobject JNICALL
Java_rs_fncore2_fn_storage_serial_SStorage_openFd (JNIEnv *env, jobject self, jint devid) {
    if (devid < 2) {
        if (has_scanner)
            releaseScannerSerial(env, 1);
        if (!pwr_on) {
            urovo_pin_powerOn(devid);
            usleep(100000);
            pwr_on = 1;
        }
    }
    jobject mFileDescriptor;

    int fd=-1;
    static char deviceName[MAX_PATH];
    memset(deviceName,0,sizeof(deviceName));
    snprintf(deviceName, sizeof(deviceName), "%s", DEVICES[devid]);
    if (devid==2){
        for (uint32_t i=0;i<10 && fd<0; i++) {
            snprintf(deviceName,sizeof(deviceName),"%s%d", DEVICES[devid],i);
            fd = open_port(deviceName, 115200, 8, 'N', 1);
        }
    } else {
        fd = open_port(deviceName, 115200, 8, 'N', 1);
    }

    LOGD("open(%s) fd = %d", deviceName, fd);
    if (fd == -1) {
        LOGE("Cannot open port");
        return NULL;
    }
    {
        jclass cFileDescriptor = env->FindClass("java/io/FileDescriptor");
        jmethodID iFileDescriptor = env->GetMethodID(cFileDescriptor, "<init>",
                                                     "()V");
        jfieldID descriptorID = env->GetFieldID(cFileDescriptor, "descriptor",
                                                "I");
        mFileDescriptor = env->NewObject(cFileDescriptor, iFileDescriptor);
        env->SetIntField(mFileDescriptor, descriptorID, (jint) fd);
    }
    return mFileDescriptor;
}

JNIEXPORT void JNICALL
Java_rs_fncore2_fn_storage_serial_SStorage_closeFd (JNIEnv *env, jobject self, jobject fd) {
    jclass FileDescriptorClass = env->FindClass("java/io/FileDescriptor");
    jfieldID descriptorID = env->GetFieldID(FileDescriptorClass, "descriptor", "I");
    jint descriptor = env->GetIntField(fd, descriptorID);
    LOGD("close(fd = %d)", descriptor);
    close(descriptor);
}
