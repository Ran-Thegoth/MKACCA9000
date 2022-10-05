package rs.mkacca.hw.scaner;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import rs.mkacca.hw.BarcodeReceiver;
import rs.mkacca.hw.scaner.engines.SQ27EScaner;
import rs.mkacca.hw.scaner.engines.UrovoScaner;

public abstract class BarcodeScaner {

	private static Map<String, Class<? extends BarcodeScaner>> ENGINES = new HashMap<>();
	public static Map<String, Class<? extends BarcodeScaner>> knownEngines() { return ENGINES; }
	static {
		ENGINES.put(UrovoScaner.ENGINE_NAME, UrovoScaner.class);
		ENGINES.put(SQ27EScaner.ENGINE_NAME,SQ27EScaner.class);
	}
	
	public BarcodeScaner() {
	}
	public abstract void start(Context context,BarcodeReceiver rcvr);
	public abstract void stop();
	public abstract void scanOnce(Context context,BarcodeReceiver rcvr);
	
	

}
