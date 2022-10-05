package rs.fncore2.example;

import rs.fncore.FiscalStorage;
import rs.utils.app.AppCore;

public class Core extends AppCore {

	private static Core SHARED_INSTANCE;
	public static Core getInstance() { return SHARED_INSTANCE; }
	public Core() {
	}
	@Override
	public void onCreate() {
		super.onCreate();
		SHARED_INSTANCE = this;
	}
	public FiscalStorage storage() {
		return getStorage();
	}

}
