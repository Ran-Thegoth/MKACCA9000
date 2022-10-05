package rs.mkacca.hw;


import rs.data.BarcodeValue;


public interface BarcodeReceiver {
	public void onBarcode(BarcodeValue code);
}
