package rs.data;

import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import rs.mkacca.DB;
import rs.mkacca.hw.scaner.BarcodeScaner;
import rs.mkacca.hw.scaner.DummyScaner;
import rs.mkacca.hw.scaner.engines.UrovoScaner;

@DBTable(name = "APPSETTING",unique = { DB.ID_FLD},indeces = {})
public class AppSettings extends DBObject {

	private BarcodeScaner _scaner;
	
	@DBField(name = "SCAN_ENGINE")
	private String _scanEngine = UrovoScaner.ENGINE_NAME;
	public AppSettings() {
	}
	@Override
	public boolean store() {
		if(_scaner != null)
			_scaner.stop();
		_scaner = null;
		return super.store();
	}
	
	public String scanEngineName() { return _scanEngine; }
	public void setScanEngineName(String engine) {
		_scanEngine = engine;
	}
	
	public BarcodeScaner scaner() {
		if(_scaner == null) try {
			_scaner = BarcodeScaner.knownEngines().get(_scanEngine).newInstance();
		} catch(Exception e) {
			_scaner = new DummyScaner();
		}
		return _scaner;
	}
	
	

}
