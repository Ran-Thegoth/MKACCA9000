package rs.fncore2;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import rs.fncore.Const;
import rs.fncore.data.FNCounters;
import rs.fncore2.utils.UrovoUtils;
import rs.utils.Utils;
import rs.utils.app.AppCore;
import rs.log.Logger;

@SuppressWarnings("deprecation")
@SuppressLint("LogNotLogger")
public class FNCore extends AppCore implements Utils.SerialReader, Utils.CountersPrinter {
	private FNSNAccess mFNsn;

	private static final File ROOT_MKACCA_PATH;
	private static final File TEMPLATES_MKACCA_PATH;

	private volatile static FNCore mInstance;
	private DB mDb;
	private String CHECKSUM = Const.EMPTY_STRING;

	
	
	public static FNCore getInstance() {
		return mInstance;
	}

	public FNCore() {
	}

	static {
		ROOT_MKACCA_PATH = new File(Environment.getExternalStorageDirectory(), "MKACCA");
		TEMPLATES_MKACCA_PATH = new File(ROOT_MKACCA_PATH, "templates");

		try {
			if (!ROOT_MKACCA_PATH.exists())
				ROOT_MKACCA_PATH.mkdir();
			if (!TEMPLATES_MKACCA_PATH.exists())
				TEMPLATES_MKACCA_PATH.mkdir();

		} catch (Exception e) {
			Log.e("fncore2","init FnCore exc",e);
		}
	}

	
	public static String crcCheck(InputStream is) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte []buf = new byte[8192];
			int read;
			while((read = is.read(buf)) > 0)
				md.update(buf,0,read);
			byte [] d =  md.digest();
			String CRC = "";
			for(byte b : d)
				CRC += String.format("%02X", b);
			return CRC;
		} catch(Exception e) {
			return "0102030405060708090A0B0C0D0E0F0102030405";
		}
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mInstance = this;
		Utils.setSerialReader(this);
		Utils.setCountersPrinter(this);
		mFNsn = new FNSNAccess(this);
		mDb = new DB(this);
		try {
			File f = getFilesDir().getParentFile();
			f = new File(f,"lib");
			f = new File(f, "liburovo_utils.so");
			InputStream is = new FileInputStream(f);
			CHECKSUM = crcCheck(is);
			is.close();
		} catch (IOException e) {
			Logger.e(e,"Ошибка подсчета контрольной суммы");
			CHECKSUM = "0102030405060708090A0B0C0D0E0F0102030405";
		}

		if (UrovoUtils.getUART() == 2)
			rs.fncore.UrovoUtils.switchOTG(true);
	}

	public String getDeviceSerial() {
		return mFNsn.getFnSN();
	}

	public DB getDB() {
		return mDb;
	}

	public String getTemplate(String name) {
		File f = new File(TEMPLATES_MKACCA_PATH, name + ".txt");
		InputStream is = null;
		try {

			if (f.exists())
				is = new FileInputStream(f);
			else
				is = getAssets().open("templates/" + name + ".txt");
			return readContent(is);

		} catch (IOException ioe) {
			Logger.e(ioe, "Ошибка чтения шаблона %s", name);
			return Const.EMPTY_STRING;
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException ioe) {
				}
		}
	}

	private String readContent(InputStream is) throws IOException {
		byte[] b = new byte[is.available()];
		is.read(b);
		is.close();
		return new String(b);
	}

	public String checksum() {
		return CHECKSUM;
	}

	@Override
	public String getSerial() {
		return UrovoUtils.KKMNumber;
	}
	@Override
	public String printCounters(FNCounters c) {
		return PrintHelper.processTemplate(PrintHelper.loadTemplate(null, "fn_counters"), c);
	}
}
