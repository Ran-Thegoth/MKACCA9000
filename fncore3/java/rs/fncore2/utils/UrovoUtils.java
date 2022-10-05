package rs.fncore2.utils;

import android.device.DeviceManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import rs.log.Logger;


public class UrovoUtils {
    public static final String KKMNumber;

    private UrovoUtils() {
    }

    static {
        KKMNumber = getKKMNumber();
    }

    private static String getKKMNumber(){
        try {
            return (new DeviceManager()).getDeviceId();
        }
        catch (Exception e){
            Logger.e(e,"error get KKM number");
            return "1234567890";
        }
    }


    public static void resetFNPower() throws InterruptedException {
        Logger.e("WARNING ! resetting USB device");

        rs.fncore.UrovoUtils.switchOTG(false);
        Thread.sleep(500);
        rs.fncore.UrovoUtils.switchOTG(true);

        Thread.sleep(2000);
    }

    @SuppressWarnings("deprecation")
	public static boolean isUSBFN() {
        if ("true".equals(rs.fncore.UrovoUtils.getProp("pwv.have.scanner"))) {
            if (new File(Environment.getExternalStorageDirectory(), "MKACCA/UART").exists()) return false;
            return true;
        }
    	return true;
    }
    @SuppressWarnings("deprecation")
	public static int getUART() {
        if ("true".equals(rs.fncore.UrovoUtils.getProp("pwv.have.scanner"))) {
            if (new File(Environment.getExternalStorageDirectory(), "MKACCA/UART").exists()) return 1;
            Log.d("fncore2", "SDK version "+Build.VERSION.SDK_INT);
            if(Build.VERSION.SDK_INT == 27)
            	return 3;
            return 2;
        }

        if (rs.fncore.UrovoUtils.getProp("pwv.custom.product.model").equals("i9100/W")){
            return 0;
        }
        return 1;
    }

}
