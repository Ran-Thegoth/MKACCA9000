package rs.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import rs.fncore.data.MeasureTypeE;



public class WeigthService {
	private static List<WeightBarcodeParser> _parsers = new ArrayList<>();
	public static void setParsers(List<? extends WeightBarcodeParser> list) {
		_parsers.clear();
		for(WeightBarcodeParser p : list)
			_parsers.add(p);
	}
	public static List<WeightBarcodeParser> getAll() {
		return _parsers;
	}
	public static String getCode(String barcode) {
		for(WeightBarcodeParser p : _parsers)
			if(p.isMatch(barcode))
				return p.parseCode(barcode);
		return barcode;
	}

	public static BigDecimal getQtty(String barcode,MeasureTypeE dest) {
		for(WeightBarcodeParser p : _parsers)
			if(p.isMatch(barcode)) {
				BigDecimal w = p.parseWeight(barcode);
				BigDecimal mul = BigDecimal.ONE;
				if(p.mu() == dest) return w;
				switch(dest) {
				case GRAM:
					if(p.mu() == MeasureTypeE.GRAM)
						mul = BigDecimal.valueOf(1000);
					else if(p.mu() == MeasureTypeE.TON)
						mul = BigDecimal.valueOf(1000000);
					break;
				case KILOGRAM:
					if(p.mu() == MeasureTypeE.GRAM)
						mul = BigDecimal.valueOf(0.001);
					if(p.mu() == MeasureTypeE.TON)
						mul = BigDecimal.valueOf(0.000001);
					break;
				default:
					break;
				}
				return w.multiply(mul);
			}
		return BigDecimal.ONE;
	}

	
}
