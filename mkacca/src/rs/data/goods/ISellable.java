package rs.data.goods;

import android.os.Parcel;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem;

public interface ISellable {
	public long id();
	public SellItem createSellItem();
	public String name();
	public Good good();
	public double price();
	public double maxQtty();
	public int getType();
	public double qtty();
	public MeasureTypeE measure();
	public void writeToParcel(Parcel p);
}
