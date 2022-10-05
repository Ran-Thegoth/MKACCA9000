package rs.mkacca.ui.fragments;


import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import cs.ui.BackHandler;
import cs.ui.fragments.BaseFragment;
import rs.fncore.Const;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.utils.ImagePrinter;

@SuppressWarnings("deprecation")
public class DeviceTestFragment extends BaseFragment implements View.OnClickListener, BackHandler {

	private View _content;
	private TextView _log;
	private CheckBox [] _toTest = new CheckBox[4];
	public DeviceTestFragment() {
	}

	
	private class TestTask extends AsyncTask<Integer, String, Void> implements Handler.Callback{
		
		
		private ProgressDialog _dialog;
		private volatile int _testStatus;
		private Handler _h;
		@Override
		protected void onPreExecute() {
			_test = this;
			_dialog = new ProgressDialog(getContext());
			_dialog.setIndeterminate(true);
			_dialog.setCancelable(false);
			_dialog.setMessage("Идет тестирование");
			_dialog.setCanceledOnTouchOutside(false);
			_h = new Handler(getContext().getMainLooper(),this);
			_dialog.show();
			_log.setText(Const.EMPTY_STRING);
		}
		private boolean wifiTest() {
			publishProgress("WiFi");
			final WifiManager wm = (WifiManager)getContext().getSystemService(Context.WIFI_SERVICE);
			if(!wm.isWifiEnabled()) {
				wm.setWifiEnabled(true);
				long start = System.currentTimeMillis();
				while(!wm.isWifiEnabled() && System.currentTimeMillis() - start < 5000) try { Thread.sleep(100); } catch(InterruptedException ie) { return false; };
				if(!wm.isWifiEnabled()) return false;
			}
			_testStatus = 0;
			BroadcastReceiver rcvr = new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context ctx, Intent e) {
					_testStatus = 2;
				}
			};
			IntentFilter ifl = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			ifl.addAction(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE);
			getContext().registerReceiver(rcvr, ifl);
			try {
			String s = "WiFi";
			wm.startScan();
			long start = System.currentTimeMillis();
			while(_testStatus == 0 && System.currentTimeMillis() - start < 20000L) try {
				s+=".";
				publishProgress(s);
				Thread.sleep(500); 
			} catch(InterruptedException ie) { return false; }
			} finally {
				getContext().unregisterReceiver(rcvr);
			}
			return _testStatus == 2;
		}
		private boolean phoneTest() {
			publishProgress("Сотовая свзяь...");
			final TelephonyManager tm = (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
			if(tm.getDeviceId() == null) return false;
			String s = tm.getLine1Number(); 
			return  s != null && !s.isEmpty();
		}
		private boolean btTest() {
			publishProgress("Bluetooth");
			final BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
			if(a == null) return false;
			if(!a.isEnabled()) {
				a.enable();
				long start = System.currentTimeMillis();
				while(!a.isEnabled() && System.currentTimeMillis() - start < 5000) try { Thread.sleep(100); } catch(InterruptedException ie) { return false; }
				if(!a.isEnabled()) return false;
			}
			_testStatus = 0;
			getContext().registerReceiver(new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context ctx, Intent arg1) {
					ctx.unregisterReceiver(this);
					_testStatus = 1;
				}
			}, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
			if(a.startDiscovery()) {
				String s = "Bluetooth";
				while(_testStatus == 0) try {
					s+=".";
					publishProgress(s);
					Thread.sleep(1000); 
				} catch(InterruptedException ie) { return false; }
				return _testStatus != 0;
			}
			return false;
		}
		private boolean prnTest() {
			publishProgress("Печать");
			try {
				_testStatus = 0;
				String printline = null;
				try (InputStream is = getContext().getResources().getAssets().open("sample.txt")) {
					byte [] b = new byte[is.available()];
					is.read(b);
					printline = new String(b);
				} catch(IOException ioe) { }
				Core.getInstance().getStorage().doPrint(printline);
				Message m = _h.obtainMessage(0);
				m.obj = new ImagePrinter().print(printline,Core.getInstance().getStorage().getPrintSettings()).toBitmap();
				_h.sendMessage(m);
				while(_testStatus == 0) try { Thread.sleep(1000); } catch(InterruptedException ie) { return false; }
				return _testStatus == 2;
			} catch(Exception ioe) {
				return false;
			}
		}
		@Override
		protected Void doInBackground(Integer... args) {
			int tests = args[0].intValue();
			if((tests & 1) != 0) 
				publishProgress(Const.EMPTY_STRING,"WiFi - "+(wifiTest() ?  "пройден" : "ошибка"));
			if((tests & 2) != 0)
				publishProgress(Const.EMPTY_STRING,"Сотовая связь - "+(phoneTest() ?  "пройден" : "ошибка"));
			if((tests & 4) != 0)
				publishProgress(Const.EMPTY_STRING,"Bluetooth - "+(btTest() ?  "пройден" : "ошибка"));
			if((tests & 8) != 0)
				publishProgress(Const.EMPTY_STRING,"Печать - "+(prnTest() ?  "пройден" : "ошибка"));
			return null;
		}
		@Override
		protected void onProgressUpdate(String... values) {
			_dialog.setMessage(values[0]);
			if(values.length > 1) {
				String s = _log.getText().toString();
				if(!s.isEmpty()) s+="\n";
				s+=values[1];
				_log.setText(s);
			}
		}
		@Override
		protected void onPostExecute(Void result) {
			_dialog.dismiss();
			_test = null;
			_content.findViewById(R.id.do_start).setEnabled(true);
			for(CheckBox c : _toTest)
				c.setEnabled(true);
		}
		@Override
		public boolean handleMessage(Message msg) {
			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
			ImageView iv = new ImageView(getContext());
			iv.setImageBitmap((Bitmap)msg.obj);
			b.setView(iv);
			b.setTitle("Печать соответствует?");
			b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					_testStatus = 2;
				}
			});
			b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					_testStatus = 1;
				}
			});
			AlertDialog d = b.create();
			d.setCancelable(false);
			d.setCanceledOnTouchOutside(false);
			d.show();
			return true;
		}
		
		
	}
	
	private TestTask _test;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_content == null) {
			_content = inflater.inflate(R.layout.diagnose, container,false);
			_toTest[0] = _content.findViewById(R.id.check_wifi);
			_toTest[1] = _content.findViewById(R.id.check_gsm);
			_toTest[2] = _content.findViewById(R.id.check_bt);
			_toTest[3] = _content.findViewById(R.id.check_prn);
			_content.findViewById(R.id.do_start).setOnClickListener(this);
			_log = _content.findViewById(R.id.v_log);
		}
		return _content;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle(R.string.do_check);
	}

	@Override
	public void onClick(View v) {
		int tests = 0;
		for(int i=0;i<_toTest.length;i++) {
			if(_toTest[i].isChecked())
				tests |= (1 << i);
			_toTest[i].setEnabled(false);
		}
		v.setEnabled(false);
		new TestTask().execute(tests);
		
	}

	@Override
	public boolean onBackPressed() {
		return _test == null;
	}
}
