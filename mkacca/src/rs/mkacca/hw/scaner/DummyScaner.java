package rs.mkacca.hw.scaner;

import android.content.Context;
import rs.mkacca.hw.BarcodeReceiver;

public class DummyScaner extends BarcodeScaner {

	public DummyScaner() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void start(Context context, BarcodeReceiver rcvr) {
	}

	@Override
	public void stop() {
	}

	@Override
	public void scanOnce(Context context, BarcodeReceiver rcvr) {
	}

}
