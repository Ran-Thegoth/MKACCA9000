/*
 * Copyright (C) 2015, The Urovo. Ltd, co
 * This file is use on project SQ27TX, Because the POGO pin of the project SQ27TX is on the SE.
 * File path: Ucode/source/Customize/SQ27TG/frameworks/base/core_ex/java/android/android.device/PogoNative.java
 * {@hide}
 */

package android.device;
/**
 *{@hide}
 */
public class PogoNative {
    static{
        System.loadLibrary("se_pogo_jni");
    }

    /********************* POGO PIN, JUST USE ON SQ27TX ****************************/
    /**
     * sePogoStatusRW: Used to read/write MH1902 POGO IO status. <br/>
     *
     * @param rw: input data, Used to control read and write POGO IO,
     *            read: 0x01, write: 0x02. <br/>
     * @param io: output data, Used to select pins,
     *            POGO_To_MINI_SWITCH: 0x01, POGO_VCC_EN: 0x02. <br/>
     * @param value input/output data, Used to store IO status
     *            High: 1, Low: 0. <br/>
     * @return return errorCode: 0 is success, other is failed. <br/>
     */
    public static native int sePogoStatusRW(byte rw, byte io, byte[] value);


    /**
     * seSwitchToPogoUsb: Switch to POGO USB mode. <br/>
     *
     * @param ctlValue input data, Used to control POGO USB mode,
     *            turn on: 1, turn off: 0. <br/>
     * @return return errorCode: 0 is success, other is failed. <br/>
     */
    public static native int seSwitchToPogoUsb(byte ctlValue);

    /********************* SQ27TX POGO PIN END *************************************/
}