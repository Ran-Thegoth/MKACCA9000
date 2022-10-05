package rs.fncore;

import java.lang.reflect.Method;

import android.device.PogoNativeWrapper;
import rs.log.Logger;

public class UrovoUtils {

	public static final int     DM_unknown = 0;
	public static final int     DM_SQ29 = 1;
	public static final int     DM_SQ27T = 2;
	public static final int     DM_SQ27TGW = 3;
	
	private UrovoUtils() { }
	
    private static Method mGetProp;
    private static int deviceModel;
    
    static {
        try {
            System.loadLibrary("urovo_utils");
        }
        catch (Exception e){
        }

        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            mGetProp = clazz.getDeclaredMethod("get", String.class);
        } catch (Exception e) {
            Logger.e(e, "getProp exc");
        }

        deviceModel = getDeviceModelId();
        Logger.i("Device model : %s", deviceModel);

    }
    /**
     * Получить наименование модели ККТ
     * @return строка, содержащая идентификатор модели.
     */
    public static int getDeviceModelId(){
        try {
            if (getDeviceBuildID().contains("SQ29")) {
                return DM_SQ29;
            } else if (getProject().equals("SQ27TGW")) {
                return DM_SQ27TGW;
            }else{
                return DM_SQ27T;
            }
        }
        catch (Exception e){
            Logger.e(e,"error get KKM number");
        }
        return DM_unknown;
    }

    /**
     * Переключение режима USB Host 
     * @param enable - включить режим USB Host
     * @return
     */
    public static boolean switchOTG(boolean enable){
        try {
            Logger.i("switching OTG %s to %s ...", deviceModel, enable ? "enable" : "disable");
            if (deviceModel == DM_SQ27TGW){
                PogoNativeWrapper.switchOtg(enable);
                return true;
            }
            else {
                return enableOtg(deviceModel, enable);
            }
        }
        catch (Exception e){
            Logger.e(e,"error work with OTG");
            return false;
        }
    }
    /**
     * Получить состояние USB Host
     * @return
     */
    public static boolean isOtgEnabled() {
    	return getOtgStatus(deviceModel);
    }


    /**
     * Прочитать системное свойство
     * @param name имя свойства
     * @return - значение
     */
    public static String getProp(String name) {
        if (mGetProp == null) return Const.EMPTY_STRING;
        try {
            return (String) mGetProp.invoke(null, name);
        } catch (Exception e) {
            return Const.EMPTY_STRING;
        }
    }


    /**
     * Получить полное наименование модели
     * @return
     */
    
    public static String getDeviceModelString() {
        String s = getProp("ro.uro.product.device");
        if (s == null || s.isEmpty()) s = getProp("ro.build.product");
        return s;
    }

    /**
     * Получить идентификатор проекта
     * @return
     */
    public static String getDeviceBuildID() {
        String s = getProp("ro.uro.product.id");
        if (s == null || s.isEmpty()) s = getProp("ro.build.id");
        return s;
    }
    private static String getProject() {
        return getProp("pwv.project");
    }
    private static native boolean enableOtg(int deviceModel, boolean enable);
    private static native boolean getOtgStatus(int deviceModel);
}
