/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class rs_fncore2_io_SAStorage */

#ifndef _Included_rs_fncore2_fn_storage_serial_SStorage
#define _Included_rs_fncore2_fn_storage_serial_SStorage
#ifdef __cplusplus
extern "C" {
#endif
#undef rs_fncore2_fn_storage_serial_SStorage_UART_DEVICE
#define rs_fncore2_fn_storage_serial_SStorage_UART_DEVICE 0L
#undef rs_fncore2_fn_storage_serial_SStorage_USB_DEVICE
#define rs_fncore2_fn_storage_serial_SStorage_USB_DEVICE 2L
/*
 * Class:     rs_fncore2_io_SAStorage
 * Method:    openFd
 * Signature: (I)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL Java_rs_fncore2_fn_storage_serial_SStorage_openFd
        (JNIEnv *, jobject, jint);

/*
 * Class:     rs_fncore2_io_SAStorage
 * Method:    closeFd
 * Signature: (Ljava/io/FileDescriptor;)V
 */
JNIEXPORT void JNICALL Java_rs_fncore2_fn_storage_serial_SStorage_closeFd
        (JNIEnv *, jobject, jobject);

#ifdef __cplusplus
}
#endif
#endif
