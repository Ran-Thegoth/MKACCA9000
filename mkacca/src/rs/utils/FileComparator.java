package rs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import cs.U;
import net.dongliu.apk.parser.ApkFile;
import rs.fncore.Const;
import rs.mkacca.Core;
import rs.mkacca.R;

public class FileComparator {
	private static final String PKG_NAME = "rs.fncore2";

	public FileComparator(Context ctx, String file) {
		try {
			ApkFile aFile = new ApkFile(file);
			String cVer = ctx.getPackageManager().getPackageInfo(PKG_NAME, 0).versionName;
			String pkg = aFile.getApkMeta().getPackageName();
			String ver = aFile.getApkMeta().getVersionName();
			String sha = Const.EMPTY_STRING;
			if (pkg.equals(PKG_NAME)) {
				ZipFile zf = new ZipFile(file);
				ZipEntry ze = zf.getEntry("lib/armeabi-v7a/liburovo_utils.so");
				if (ze != null) {
					InputStream is = zf.getInputStream(ze);
					sha = crcCheck(is);
					is.close();
				}
				zf.close();
			}
			String bSha = Core.getInstance().getStorage().getCheckSum();
			AlertDialog.Builder b = new AlertDialog.Builder(ctx);
			View v = LayoutInflater.from(ctx).inflate(R.layout.compare, new LinearLayout(ctx), false);
			b.setView(v);
			b.setPositiveButton(android.R.string.ok, null);
			b.setTitle("Результат сравнения");
			TextView tv = v.findViewById(R.id.lb_c_pkg);
			tv.setText(PKG_NAME);
			tv = v.findViewById(R.id.lb_e_pkg);
			tv.setText(pkg);
			tv = v.findViewById(R.id.lb_c_ver);
			tv.setText(cVer);
			tv = v.findViewById(R.id.lb_e_ver);
			tv.setText(ver);
			tv = v.findViewById(R.id.lb_с_crc);
			tv.setText(bSha);
			tv = v.findViewById(R.id.lb_e_crc);
			tv.setText(sha);
			tv = v.findViewById(R.id.lb_result);
			if (sha.equals(bSha) && cVer.equals(ver) && pkg.equals(PKG_NAME)) {
				tv.setText("Совпадают");
				tv.setTextColor(Color.GREEN);
			} else {
				tv.setText("Не совпадают");
				tv.setTextColor(Color.RED);
			}
			aFile.close();
			b.show();
		} catch (Exception ioe) {
			Log.e("fncore2", "Fail", ioe);
			U.notify(ctx, "Ошибка чтения файла " + file);
		}
	}

	public static String crcCheck(InputStream is) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] buf = new byte[8192];
			int read;
			while ((read = is.read(buf)) > 0)
				md.update(buf, 0, read);
			byte[] d = md.digest();
			String CRC = "";
			for (byte b : d)
				CRC += String.format("%02X", b);
			return CRC;
		} catch (Exception e) {
			return Const.EMPTY_STRING;
		}

	}

}
