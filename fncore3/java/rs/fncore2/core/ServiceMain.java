package rs.fncore2.core;

import static rs.fncore2.core.utils.NotificationsHelper.CHANNEL_ID;
import static rs.fncore2.core.utils.NotificationsHelper.CHANNEL_ID_STORAGE;
import static rs.fncore2.core.utils.NotificationsHelper.NOTIFICATION_ID;
import static rs.fncore2.core.utils.NotificationsHelper.NOTIFICATION_ID_STORAGE;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import rs.fncore.Const;
import rs.fncore2.FNCore;
import rs.fncore3.R;
import rs.fncore2.core.utils.NotificationsHelper;
import rs.fncore2.fn.FNManager;
import rs.fncore2.utils.UrovoUtils;
import rs.log.Logger;

public class ServiceMain extends ServiceBase implements PropertyChangeListener {

	
	private static final int FTDI_VID = 0x403;
//	private static final int FTDI_PID = 0x6001;

	
	static volatile Boolean mLastUsbCharge = null;
	static volatile Boolean mLastACCharge = null;

	@Override
	public void onCreate() {
		super.onCreate();
        Logger.init(this);
		NotificationsHelper.createServiceChannel(this);
		NotificationsHelper.initNotifications(this);
		updateNotification();

		
		if(UrovoUtils.getUART() == 2) {
			IntentFilter ifilter = new IntentFilter();
			ifilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
			ifilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//			ifilter.addAction(Intent.ACTION_BATTERY_CHANGED);
			ifilter.addAction(Intent.ACTION_SCREEN_ON);
			registerReceiver(ACTIONS_RECEIVER, ifilter);
		}

		mFNManager.addFNChangedListener(this);
		mFNManager.addFNReadyListener(this);

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				try {
					buildStorage();
					mBinder.waitFnReady(1);
					mWakelockPower.releaseWakeLock();
				} catch (Exception e) {
					Logger.e(e, "oncreate Handler exc: ");
				}
			}
		});

		Logger.i("finished main service init");
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(FNManager.FN_CHANGED)) {
			Intent intent = new Intent(Const.FN_STATE_CHANGED_ACTION);
			intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
			FNCore.getInstance().sendBroadcast(intent);
			updateNotification();
		} else if (evt.getPropertyName().equals(FNManager.FN_READY)) {
			updateNotification();
		}
	}

	private final BroadcastReceiver ACTIONS_RECEIVER = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("fncore2","Recived signal "+intent.getAction());
			switch (mSettings.getConnectionMode()) {
			case CLOUD:
			case VIRTUAL:
				return;
			default:
				break;
			}

			switch (intent.getAction()) {
			case UsbManager.ACTION_USB_DEVICE_ATTACHED: {
				UsbDevice usbdev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				Logger.i("USB device attached, VID: %s, PID: %s, name: %s", usbdev.getVendorId(), usbdev.getProductId(),
						usbdev.getDeviceName());
				if (usbdev.getVendorId() != FTDI_VID) {
					Logger.i("USB device attached ignored wrong PID");
					return;
				}

				if (mFNManager.isFNReady()) {
					Logger.w("USB device ignored, storage was not detached, may be glitch ?");
					return;
				}

				Logger.i("waiting FN init ...");

				destroyStorage();
				buildStorage();

				if (!mFNManager.isFNReady()) {
					Logger.e("can't init storage error unknown");
					return;
				}
			}
				break;

			case UsbManager.ACTION_USB_DEVICE_DETACHED: {
				UsbDevice usbdev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				Logger.i("USB device detached, VID: %s, PID: %s", usbdev.getVendorId(), usbdev.getProductId());

				if (usbdev.getVendorId() != FTDI_VID) {
					Logger.i("USB device detached ignored");
					return;
				}

				destroyStorage();
			}
				break;

/*			case Intent.ACTION_BATTERY_CHANGED: {
				// Are we charging / charged?
				int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
						|| status == BatteryManager.BATTERY_STATUS_FULL;

				// How are we charging?
				int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
				boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
				boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

				if ((mLastUsbCharge == null || (mLastUsbCharge != usbCharge && !usbCharge))
						|| (mLastACCharge == null || (mLastACCharge != acCharge && !acCharge))) {
					Logger.i("Charging: %s, chnum: %s, usbCharge: %s, acCharge: %s, level: %s", isCharging, chargePlug,
							usbCharge, acCharge, level);

					try {
						Logger.i("enabling OTG...");
						if (!UrovoUtils.switchOTG(true)) {
							throw new Exception("error enable OTG");
						}
					} catch (Exception e) {
						Logger.e(e, "Error enable OTG");
					}
				}

				if (mLastUsbCharge != null && mLastUsbCharge != usbCharge && usbCharge) {
					try {
						Logger.i("disabling OTG...");
						if (!UrovoUtils.switchOTG(false)) {
							throw new Exception("error disable OTG");
						}
					} catch (Exception e) {
						Logger.e(e, "Error disable OTG");
					}
				}

				mLastUsbCharge = usbCharge;
				mLastACCharge = acCharge;
				break;
			} */
			case Intent.ACTION_SCREEN_ON: {
				if(!USB_MONITOR) return;
				Logger.i("screen on detected");
				if(UrovoUtils.isUSBFN())
					rs.fncore.UrovoUtils.switchOTG(true);
				break;
			}
			default:
				Logger.e("not handled action: %s", intent.getAction());
				break;
			}
		}
	};

	@SuppressWarnings("deprecation")
	protected void updateNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		{ // storage notification
			Notification.Builder nb;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				nb = new Notification.Builder(this, CHANNEL_ID_STORAGE);
			} else {
				nb = new Notification.Builder(this);
			}

			String msg = "нет соединения с физическим ФН";
			int iconID = R.drawable.ic_no_terminal;

			if (mFNManager.isFNReady()) {
				msg = "ФН подключен";
				iconID = R.drawable.ic_terminal;
			}

			Logger.i("Notify: %s", msg);

			Notification notification = nb.setSmallIcon(iconID)
					.setLargeIcon(((BitmapDrawable) getDrawable(iconID)).getBitmap())
					.setContentTitle(getString(R.string.app_name))
					.setStyle(new Notification.BigTextStyle().bigText(msg)).setAutoCancel(false).setContentText(msg)
					.build();
			notificationManager.notify(NOTIFICATION_ID_STORAGE, notification);
		}

		// -------------- application notification
		Notification.Builder nb;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			nb = new Notification.Builder(this, CHANNEL_ID);
		} else {
			nb = new Notification.Builder(this);
		}

		nb.setLargeIcon(((BitmapDrawable) getDrawable(R.drawable.ic_launcher)).getBitmap());
		nb.setOngoing(true);
		nb.setContentTitle(getString(R.string.app_name));
		nb.setSmallIcon(R.drawable.ic_launcher_bw);

		if (mFNManager.isFNReady()) {
			String fnSerial = mFNManager.getFN().getKKMInfo().getFNNumber();

			nb.setContentText("ФН " + fnSerial);
			Notification.InboxStyle style = new Notification.InboxStyle();

			if (mOfdSender != null && mOfdSender.isProgress())
				style.addLine("Идет передача данных");
			else
				style.addLine("Готов к работе");

			nb.setStyle(style);
		} else {
			nb.setContentText("ФН не установлен");
		}

		notificationManager.notify(NOTIFICATION_ID, nb.build());
	}

	@Override
	public void onDestroy() {


		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
		try {
			unregisterReceiver(ACTIONS_RECEIVER);
		} catch(Exception | Error e) { }

		if (mPrinter != null) {
			mPrinter.interrupt();
			try {
				mPrinter.join();
			} catch (Exception | Error e) {
				Logger.e(e, "Printer destroy exc: ");
			}
			mPrinter = null;
		}

		destroyStorage();
		sendBroadcast(new Intent("rs.fncore2.restart").setPackage(getPackageName()));
		super.onDestroy();
	}
}
