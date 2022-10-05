package rs.data.goods;

import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import rs.mkacca.DB;

@DBTable(name = DB.BARCODES,unique = {"CODE"},indeces = {"OWNER_TYPE,OWNER_ID"})
public class Barcode extends DBObject {
	
	public static final String OWNER_FLD = "OWNER_ID";
	public static final String OWNER_TYPE_FLD = "OWNER_TYPE";
	@DBField(name = OWNER_FLD)
	private long _owner_id;
	private IBarcodeOwner _owner;
	@DBField(name = "CODE")
	private String _code;
	@DBField(name = OWNER_TYPE_FLD)
	private int _owner_type;
	public Barcode() { }
	public Barcode(IBarcodeOwner owner) {
		_owner = owner;
	}
	public Barcode(IBarcodeOwner owner, String code) {
		_owner = owner;
		_code = code;
	}
	public Barcode setCode(String value) {
		_code = value;
		return this;
	}
	@Override
	public boolean store() {
		if(_owner_id == 0) {
			_owner_id = _owner.id();
			_owner_type = _owner.getType();
		}
		return super.store();
	}
	@Override
	public String toString() {
		return _code;
	}
}
