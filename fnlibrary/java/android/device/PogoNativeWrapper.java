package android.device;

import java.io.FileOutputStream;


/**
 * Внутрениий класс. 
 * @author nick
 *
 */
public class PogoNativeWrapper {
    static final String OTG_ENABLE_PATH_SQ27TGW="/sys/devices/soc/78d9000.usb/otg_enable";
    static void enableOtg(){
        byte [] value ={1};
        byte rw=0x2;
        byte io=0x2;
        byte switchToPogoUsb = 1;

        writeNode(OTG_ENABLE_PATH_SQ27TGW, Integer.toString(1));
        PogoNative.seSwitchToPogoUsb(switchToPogoUsb);
        PogoNative.sePogoStatusRW(rw,io,value);
    }

    static void disableOtg(){
        byte [] value ={0};
        byte rw=0x2;
        byte io=0x2;
        byte switchToPogoUsb = 0;

        writeNode(OTG_ENABLE_PATH_SQ27TGW, Integer.toString(0));
        PogoNative.seSwitchToPogoUsb(switchToPogoUsb);
        PogoNative.sePogoStatusRW(rw,io,value);
    }

    static void writeNode(String path, String data){
        try(FileOutputStream fRed = new FileOutputStream(path)){
            fRed.write(data.getBytes());
        }
        catch (Exception e){
        }
    }

    public static void switchOtg(boolean enable){
        if (enable){
            enableOtg();
        }
        else{
            disableOtg();
        }
    }
}
