package rs.data;


import android.os.Parcel;
import cs.U;
import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import cs.orm.ORMHelper;
import rs.data.goods.Good;
import rs.data.goods.ISellable;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.mkacca.DB;

@DBTable(name = "BILLS",unique =  {}, indeces =  {})
public class CheckStorage extends DBObject{

	public static final String CHECK_TYPE_FLD = "CTYPE";
	@DBField(name = CHECK_TYPE_FLD)
	private OrderTypeE _type;
	@DBField(name = "CBODY")
	private byte [] _body = {};
	public CheckStorage() {
	}
	public void set(SellOrder o) {
		_type = o.getType();
		Parcel p = Parcel.obtain();
		o.writeToParcel(p, 0);
		for(int i=0;i<o.getItems().size();i++) {
			p.writeInt(i);
			ISellable sellable = (ISellable)o.getItems().get(i).attachment();
			if(sellable != null) {
				p.writeByte((byte)1);
				sellable.writeToParcel(p);
			} else
				p.writeByte((byte)0);
		}
		p.setDataPosition(0);
		_body = p.marshall();
		p.recycle();
	}
	
	public SellOrder get() {
		if(_body.length == 0) return null;
		Parcel p = Parcel.obtain();
		p.unmarshall(_body, 0, _body.length);
		p.setDataPosition(0);
		SellOrder so = new SellOrder();
		so.readFromParcel(p);
		while(p.dataAvail() > 0) {
			int index = p.readInt();
			SellItem item = so.getItems().get(index);
			if(p.readByte() != (byte)0) {
				ISellable sellable = null;
				int type  = p.readByte();
				if(type < 2) {
					sellable = ORMHelper.load(Good.class, U.pair(DB.ID_FLD, p.readLong()));
					if(type == 1) 
						sellable = ((Good)sellable).getVariantById(p.readLong());
				}
				item.attach(sellable);
			}
		}
		p.recycle();
		return so;
		
	}

	

}
