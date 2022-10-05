package rs.log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;
import rs.fncore.Const;

public class Logger {

	private static File _logFile;
	private static File _fnIOFile;
	private static int _logLevel = Log.DEBUG;
	private static String _source = Const.EMPTY_STRING;
	private static String _mark;
	private final static SimpleDateFormat DF = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
	public static String BUILD_VERSION;

	
	public static void beginMark(String m) {
		_mark = m;
	}
	public static void endMark() {
		_mark = null;
	}
	@SuppressWarnings("deprecation")
	public static void init(Context ctx) {
		try {
			PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			BUILD_VERSION = info.versionName;
			File logFolder = new File(Environment.getExternalStorageDirectory(), "MKACCA");
			if (!logFolder.exists())
				logFolder.mkdir();
			String s = ctx.getPackageName();
			for(int i=s.length()-1;i>0;i--) {
				if(s.charAt(i) == '.') break;
				_source = s.charAt(i)+_source;
			}
			
			_logFile = new File(logFolder, "MKACCA.log");
			if("fncore2".equals(_source)) {
				_fnIOFile = new File(logFolder, "FNIO.log");
				if(_logFile.exists()) try {
					FileInputStream fis = new FileInputStream(_logFile);
					long sz = fis.available();
					fis.close();
					if(sz > 1024 * 1024 * 1024 * 10L) {
						_fnIOFile.delete();
						_logFile.delete();
					}
				} catch(IOException ioe) {
					
				}
			}
			
			
		} catch (NameNotFoundException nfe) {
		}
		
	}

	public static void e(Throwable e) {
		e(e, null);
	}

	public static void e(Throwable e, String fmt, Object... args) {
		log(Log.ERROR, e, fmt, args);
	}

	public static void e(String fmt, Object... args) {
		e(null, fmt, args);
	}

	public static void w(Throwable e, String fmt, Object...args) {
		log(Log.WARN, e, fmt, args);
	}

	public static void w(String fmt, Object... args) {
		w(null, fmt, args);
	}

	public static void i(Throwable e, String fmt, Object... args) {
		log(Log.INFO, e, fmt, args);
	}

	public static void i(String fmt, Object... args) {
		i(null, fmt, args);
	}

	public static void d(Throwable e, String fmt, Object... args) {
		log(Log.DEBUG, e, fmt, args);
	}

	public static void d(String fmt, Object... args) {
		d(null, fmt, args);
	}

	private static String lvlName(int level) {
		switch (level) {
		case Log.INFO:
			return "ИНФО";
		case Log.WARN:
			return "ПРЕДУПРЕЖДЕНИЕ";
		case Log.ERROR:
			return "ОШИБКА";
		}
		return "ОТЛАДКА";

	}

	public static void logIO(String fmt, Object... args) {
		if(fmt == null || fmt.isEmpty()) return;
		String msg = "";
		if (fmt != null)
			msg = String.format(fmt, args);
		Log.i(_source, msg);
		if(_fnIOFile != null)
			try (FileOutputStream fos = new FileOutputStream(_fnIOFile, true)) {
				String s = DF.format(System.currentTimeMillis()) + "\t";
				if(_mark != null) s+=_mark+": ";
				s += msg+"\n";
				fos.write(s.getBytes());
			} catch(IOException ioe) { } 
		
	}
	public static void log(int level, Throwable e, String fmt, Object... args) {
		if((fmt == null || fmt.isEmpty()) && e  == null) return;
		if(level < _logLevel) return;
		String msg = "";
		if (fmt != null)
			msg = String.format(fmt, args);
		
		switch (level) {
		case Log.DEBUG:
			Log.d(_source, msg, e);
			break;
		case Log.INFO:
			Log.i(_source, msg, e);
			break;
		case Log.WARN:
			Log.w(_source, msg, e);
			break;
		case Log.ERROR:
			Log.e(_source, msg, e);
			break;
		}
		if (_logFile != null && level >= _logLevel)
			try (FileOutputStream fos = new FileOutputStream(_logFile, true)) {
				fos.write((DF.format(System.currentTimeMillis()) + "\t" + _source+"\t"+lvlName(level) + "\t").getBytes());
				if (msg != null && !msg.isEmpty()) {
					String[] v = msg.split("\n");
					fos.write((v[0] + "\n").getBytes());
					for (int i = 1; i < v.length; i++)
						fos.write(("\t\t\t" + v[i] + "\n").getBytes());
				}
				else 
					fos.write("\n".getBytes());
				if (e != null) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream(bos);
					e.printStackTrace(ps);
					String[] v = new String(bos.toByteArray()).split("\n");
					fos.write(("\t\t\t"+e.getClass().getName() + ": " + e.getLocalizedMessage() + "\n").getBytes());
					for (int i = 0; i < v.length; i++)
						fos.write(("\t\t\t" + v[i] + "\n").getBytes());
				}
			} catch (IOException ioe) {
			}
	}

}
