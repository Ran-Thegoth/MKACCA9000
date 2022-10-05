package rs.data;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import rs.mkacca.Core;

@DBTable(name="APPS",unique="NAME",indeces = {})
public class App extends DBObject {

	@DBField(name="NAME")
	private String _package;
	private PackageInfo _pi;
	private BitmapDrawable _icon;
	public App() {
	}
	@Override
	public void onLoaded() {
		try {
			_pi = Core.getInstance().getPackageManager().getPackageInfo(_package, 0);
			retriveIcon();
		} catch(NameNotFoundException nfe) { }
		super.onLoaded();
	}
	@SuppressWarnings("deprecation")
	private void retriveIcon() {
		Drawable d = Core.getInstance().getPackageManager().getApplicationIcon(_pi.applicationInfo);
		int dp = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, Core.getInstance().getResources().getDisplayMetrics());
		d.setBounds(0,0,dp,dp);
		Bitmap b = Bitmap.createBitmap(dp, dp, Config.ARGB_8888);
		Canvas c = new Canvas(b);
		d.draw(c);
		_icon = new BitmapDrawable(b);
	}
	public App(PackageInfo pi) {
		_pi = pi;
		_package = pi.packageName;
		retriveIcon();
	}
	public String packageName() { return _package; }
	public String getName() {
		return Core.getInstance().getPackageManager().getApplicationLabel(_pi.applicationInfo).toString();
	}
	public Drawable getIcon() {
		return _icon;
	}
	public Intent getLauchIntent() {
		return Core.getInstance().getPackageManager().getLaunchIntentForPackage(_package);
	}
	@Override
	public String toString() {
		return getName();
	}

}
