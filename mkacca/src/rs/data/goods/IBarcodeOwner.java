package rs.data.goods;

import java.util.List;

public interface IBarcodeOwner {
	public long id();
	public List<Barcode> barcodes();
	public int getType();
}
