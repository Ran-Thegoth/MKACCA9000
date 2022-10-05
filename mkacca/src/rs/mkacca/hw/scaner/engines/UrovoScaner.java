package rs.mkacca.hw.scaner.engines;

import com.google.zxing.BarcodeFormat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import rs.data.BarcodeValue;
import rs.mkacca.hw.BarcodeReceiver;
import rs.mkacca.hw.scaner.BarcodeScaner;

public class UrovoScaner extends BarcodeScaner {

	public static final String ENGINE_NAME = "Urovo SQ27";
	private BarcodeReceiver _rcvr;
	private boolean _oneShot;
	private class BarcodeEventReceiver extends BroadcastReceiver {
		private Context _ctx;
		public void bind(Context ctx) {
			if(_ctx != null) return;
			_ctx = ctx;
			_ctx.registerReceiver(this, new IntentFilter(ScanManager.ACTION_DECODE));
		}
		public void unbind() {
			if(_ctx == null) return;
			_ctx.unregisterReceiver(this);
			_ctx = null;
		}
		@Override
		public void onReceive(Context arg0, Intent e) {
			if(_rcvr != null) {
				int l = e.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0);
				byte [] b = e.getByteArrayExtra(ScanManager.DECODE_DATA_TAG);
				String code = new String(b,0, l > b.length ? b.length : l);
				int type = e.getByteExtra(ScanManager.BARCODE_TYPE_TAG,(byte)0);
				BarcodeFormat fmt = BarcodeFormat.CODE_39;
				switch(type) {
				case 119: fmt = BarcodeFormat.DATA_MATRIX; break;
				case 115: fmt = BarcodeFormat.QR_CODE; break;
				default:
					if(code.length() == 8)
						fmt = BarcodeFormat.EAN_8;
					else if(code.length() == 13)
						fmt = BarcodeFormat.EAN_13;
				}
				_rcvr.onBarcode(new BarcodeValue(code, fmt));
				if(_oneShot) {
					_rcvr = null;
					unbind();
				}
			}
			
			
		}
	};
	private BarcodeEventReceiver RCVR = new BarcodeEventReceiver();
	private ScanManager _scaner;
	public UrovoScaner() {
		_scaner = new ScanManager();
		if(_scaner.openScanner()) {
			_scaner.switchOutputMode(0);
			_scaner.setParameterInts(new int [] { PropertyID.GOOD_READ_BEEP_ENABLE}, new int [] { 1});
		}
	}

	@Override
	public void start(Context context, BarcodeReceiver rcvr) {
		_oneShot = false;
		_rcvr = rcvr;
		RCVR.bind(context);
		
	}

	@Override
	public void stop() {
		RCVR.unbind();
	}

	@Override
	public void scanOnce(Context context, BarcodeReceiver rcvr) {
		_oneShot = true;
		_rcvr = rcvr;
		RCVR.bind(context);
	}

}
