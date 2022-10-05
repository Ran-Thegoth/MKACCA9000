package rs.data;


import java.math.BigDecimal;
import java.util.List;

import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import cs.orm.ORMHelper;
import cs.ui.annotations.BindTo;
import rs.fncore.data.MeasureTypeE;
import rs.mkacca.R;


@DBTable(name="WB_PARSERS",unique="PREFIX",indeces = {})
public class WeightBarcodeParser extends DBObject  {

	
	@DBField(name="PREFIX")
	@BindTo(ui=R.id.ed_prefix,required=true)
	private String _prefix = "21";
	@DBField(name="CODE")
	@BindTo(ui=R.id.ed_code_len)
	private int _codeLength = 5;
	@DBField(name="WEIGHT")
	@BindTo(ui=R.id.ed_weight_len)
	private int _weightLength = 5;
	@DBField(name="MU")
	@BindTo(ui=R.id.sp_mu,required=true)
	private MeasureTypeE _mu = MeasureTypeE.KILOGRAM;
	public WeightBarcodeParser() {
	}
	
	public boolean isMatch(String barcode) {
		if(barcode == null || barcode.isEmpty()) return false;
		return barcode.startsWith(_prefix);
	}
	public String parseCode(String barcode) {
		if(barcode == null || barcode.isEmpty()) return null;
		if(barcode.startsWith(_prefix)) {
			barcode = barcode.substring(_prefix.length());
			return barcode.substring(0,_codeLength);
		}
		return null;
	}
	
	public BigDecimal parseWeight(String barcode) {
		if(!isMatch(barcode)) return BigDecimal.ZERO;
		int start = _prefix.length()+_codeLength;
		String weigth = barcode.substring(start,start+_weightLength);
		try {
			return  BigDecimal.valueOf(Double.parseDouble(weigth));
		} catch(NumberFormatException nfe) {
			return BigDecimal.ZERO;
		}
	}
	public MeasureTypeE mu() { return _mu; }
	@Override
	public String toString() {
		String s  = _prefix;
		for(int i=0;i<_codeLength;i++)
			s += "C";
		for(int i=0;i<_weightLength;i++)
			s += "W";
		return s;
	}
	public boolean store() {
		try {
			if(ORMHelper.save(this)) {
				if(!WeigthService.getAll().contains(this))
					WeigthService.getAll().add(this);
				return true;
			}
		} catch(Exception e) {
		}
		return false;
	}

	public boolean delete() {
		try {
			if(ORMHelper.delete(this)) {
				WeigthService.getAll().remove(this);
				return true;
			}
		} catch(Exception e) {
		}
		return false;
	}
	
	public static void load() {
		List<WeightBarcodeParser> barcodes = ORMHelper.loadAll(WeightBarcodeParser.class);
		WeigthService.setParsers(barcodes);
		if(barcodes.isEmpty()) 
			new WeightBarcodeParser().store();
	}


}
