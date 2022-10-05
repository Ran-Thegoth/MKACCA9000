package rs.data.goods;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import cs.orm.DBCollection;
import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import cs.ui.annotations.BindTo;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellItem.ItemPaymentTypeE;
import rs.mkacca.DB;
import rs.mkacca.R;
import rs.utils.QTTYVaidator;

@DBTable(name = DB.VARIANTS, unique = {"UUID",Good.GOOD_ID_FLD+",NAME"},indeces = {Good.GOOD_ID_FLD})
public class Variant extends DBObject implements IBarcodeOwner,ISellable {

	public static final String VARIANT_ID = "VAR_ID";
	
	
	@DBField(name = "NAME")
	@BindTo(ui =  { R.id.ed_name},required = true)
	private String _name;
	
	@DBField(name = Good.GOOD_ID_FLD)
	private Good _owner;

	@DBField(name = "MU")
	@BindTo(ui = R.id.sp_mu, required = true)
	private MeasureTypeE _mu;

	@DBField(name = "QTTY")
	@BindTo(ui = R.id.ed_qtty, validate = QTTYVaidator.class)
	private double _qtty = 1;
	@DBField(name = "PRICE")
	@BindTo(ui = {  R.id.ed_price})
	private double _price = 0;
	@DBField(name = "UUID")
	private String UUID = java.util.UUID.randomUUID().toString();
	

	@DBCollection(itemClass = Barcode.class,linkField = Barcode.OWNER_TYPE_FLD+"=1 AND  "+Barcode.OWNER_FLD,order = 1)
	private List<Barcode> _barcodes = new ArrayList<>();
	
	public Variant() {
	}

	public Variant(Good owner) {
		_owner = owner;
		_mu = _owner.baseMU();
		_price = _owner.price();
	}

	public Variant(Good owner, String uuid) {
		this(owner);
	}

	public MeasureTypeE mu() {
		return _mu;
	}

	public double price() {
		return _price;
	}

	public Good good() {
		return _owner;
	}

	@Override
	public void onLoaded() {
		super.onLoaded();
	}

	@Override
	public String toString() {
		return _name;
	}

	public double qtty() {
		return _qtty;
	}

	public Variant setQtty(double value) {
		_qtty = value;
		return this;

	}


	public Variant setPrice(double price) {
		_price = price;
		return this;
	}

	public Variant setMU(MeasureTypeE value) {
		_mu = value;
		return this;
	}
	public String name() { 
		return _name;
	}

	public Variant setName(String value) {
		_name = value;
		return this;
	}

	public List<Barcode> barcodes() { return _barcodes; }

	@Override
	public int getType() {
		return 1;
	}

	@Override
	public SellItem createSellItem() {
		BigDecimal qtty = BigDecimal.valueOf(_qtty);
		return new SellItem(_owner.itemType(), ItemPaymentTypeE.FULL, _owner.name()+","+_name, qtty, _mu, 
				BigDecimal.valueOf(_price).divide(qtty), _owner.vat()).attach(this);
	}

	@Override
	public MeasureTypeE measure() {
		return _mu;
	}

	@Override
	public double maxQtty() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel p) {
		p.writeByte((byte)1);
		p.writeLong(_owner.id());
		p.writeLong(id());
		
	}
	
}
