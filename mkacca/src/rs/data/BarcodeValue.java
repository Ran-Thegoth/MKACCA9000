package rs.data;

import com.google.zxing.BarcodeFormat;

import rs.fncore.Const;

public class BarcodeValue {

	public final String CODE;
	public final String GOOD_CODE;
	public final String MARK_CODE;
	public final BarcodeFormat FORMAT;

	public BarcodeValue(String code, BarcodeFormat fmt) {
		FORMAT = fmt;
		CODE =  code;
		switch (fmt) {
		case DATA_MATRIX:
			if(code.startsWith("01")) {
				GOOD_CODE = code.substring(3,16);
				MARK_CODE = code;
			} else if(code.startsWith("0000")) { 
				GOOD_CODE = code.substring(6,14);
				MARK_CODE = code;
			} else {
				GOOD_CODE = code;
				MARK_CODE = Const.EMPTY_STRING;
			}
			break;
		default:
			GOOD_CODE = WeigthService.getCode(CODE);
			MARK_CODE = Const.EMPTY_STRING;
			break;
		}
	}

}
