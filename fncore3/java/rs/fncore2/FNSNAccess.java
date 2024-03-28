package rs.fncore2;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexFile;
import rs.log.Logger;

@SuppressWarnings("deprecation")
public class FNSNAccess {

	private static final String NUMBERS = "0123456789";

	private Method mReadFNSn;
	private Method mInit, mRelease;
	private String mFNSN;

	public FNSNAccess(Context context) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			mFNSN = Build.getSerial();
			return;
		}
		int deviceModel = rs.fncore.UrovoUtils.getDeviceModelId();
		File f = new File(context.getFilesDir(), "dex");
		if (!f.exists())
			f.mkdir();

		f = new File(f, "fnsn.bin");
		InputStream is;

		try {
			switch (deviceModel) {
			case rs.fncore.UrovoUtils.DM_SQ29:
				is = context.getAssets().open("SQ29/classes.dex");
				break;
			default:
				Logger.e(String.format("unknown model: %s", deviceModel));
			case rs.fncore.UrovoUtils.DM_SQ27T:
			case rs.fncore.UrovoUtils.DM_SQ27TGW:
				is = context.getAssets().open("SQ27/classes.dex");
				break;
			}

			byte[] buff = new byte[32768];
			FileOutputStream fs = new FileOutputStream(f);
			int read;
			while ((read = is.read(buff)) > 0)
				fs.write(buff, 0, read);
			fs.close();
			is.close();

			DexFile dexer = DexFile.loadDex(f.getAbsolutePath(), f.getAbsolutePath() + ".odex", 0);

			switch (deviceModel) {
			case rs.fncore.UrovoUtils.DM_SQ29: {
				Class<?> loader = dexer.loadClass("android.device.KeyMaster", getClass().getClassLoader());
				mInit = loader.getDeclaredMethod("init");
				mInit.setAccessible(true);
				mRelease = loader.getDeclaredMethod("release");
				mReadFNSn = loader.getDeclaredMethod("readKey", byte[].class);
				break;
			}
			default:
				Logger.e(String.format("unknown model: %s", deviceModel));
			case rs.fncore.UrovoUtils.DM_SQ27T:
			case rs.fncore.UrovoUtils.DM_SQ27TGW: {
				dexer.loadClass("android.device.KeyMaster", getClass().getClassLoader());
				Class<?> loader = dexer.loadClass("android.device.FnSnAccess", getClass().getClassLoader());
				mReadFNSn = loader.getDeclaredMethod("fnSnRead", byte[].class);
				break;
			}
			}

			byte[] b = new byte[256];
			int l = readFnSN(b);
			mFNSN = "";

			for (int i = 0; i < l; i++) {
				if (NUMBERS.indexOf((char) b[i]) == -1)
					break;
				mFNSN += NUMBERS.charAt(NUMBERS.indexOf((char) b[i]));
			}

			if (mFNSN.isEmpty())
				mFNSN = Build.getSerial();

			Logger.i(String.format("Device serial is %s", mFNSN));

		} catch (Exception ioe) {
			Logger.e("Error reading device serial", ioe);
			mFNSN = "12345467890";
		}
	}

	public String getFnSN() {
		return mFNSN;
	}

	private int readFnSN(byte[] data) throws InvocationTargetException, IllegalAccessException {
		if (mReadFNSn == null)
			return 0;

		if (mInit != null) {
			mInit.setAccessible(true);
			mInit.invoke(null);
		}
		mReadFNSn.setAccessible(true);
		int result = ((Number) mReadFNSn.invoke(null, data)).intValue();
		if (mRelease != null) {
			mRelease.setAccessible(true);
			mRelease.invoke(null);
		}
		return result;
	}

	static String getFromBuildProp(String key) {
		try {
			Class<?> c = Class.forName("android.os.SystemProperties");
			try {
				Method method = c.getDeclaredMethod("get", String.class);
				method.setAccessible(true);
				try {
					Log.i("fncore3","PROP OK");
					return (String) method.invoke(null, key);
				} catch (IllegalAccessException e) {
					Log.e("fncore3","err0",e);
				} catch (InvocationTargetException e) {
					Log.e("fncore3","err1",e);
				}
			} catch (NoSuchMethodException e) {
				Log.e("fncore3","err2",e);
			}
		} catch (ClassNotFoundException e) {
			Log.e("fncore3","err3",e);
		}
		return null;
	}

}
