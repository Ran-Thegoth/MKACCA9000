package rs.data;

import cs.U;
import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import cs.orm.ORMHelper;
import rs.data.goods.ISellable;
import rs.mkacca.Core;

@DBTable(name = "SHIFT_SELLS",unique = {},indeces = {"SHIFT","SHIFT,STYPE,SID,PRICE"})
public class ShiftSells extends DBObject {

	@DBField(name =  "SHIFT")
	private int _shift;
	@DBField(name =  "STYPE")
	private int _sellableType;
	@DBField(name =  "SID")
	private long _sellableId;
	@DBField(name =  "QTTY")
	private double _qtty = 1;
	@DBField(name =  "PRICE")
	private double _price = 0;
	public ShiftSells() { }
	public ShiftSells(int shift, ISellable item, double price, double qtty) {
		_shift = shift;
		_sellableType = item.getType();
		_sellableId = item.id();
		_qtty = qtty;
		_price = price;
	}
	
	public static void updateSell(int shift, ISellable item, double price, double qtty) {
		ShiftSells rest = ORMHelper.load(ShiftSells.class, U.pair("SHIFT",shift), U.pair("STYPE", item.getType()),U.pair("SID", item.id()),U.pair("PRICE", price));
		if(rest == null ) 
			rest = new ShiftSells(shift, item, price, qtty) ;
		else
			rest._qtty += qtty;
		rest.store();
	}
	public static void clearSells(int shift) {
		Core.getInstance().db().getWritableDatabase().execSQL("DELETE FROM SHIFT_SELLS WHERE SHIFT="+shift);;
	}

}
