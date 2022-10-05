package rs.fncore.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ставка НДС
 *
 * @author amv
 */
public enum VatE {
    VAT_20(1,"НДС 20%"),
    VAT_10(2,"НДС 10%"),
    VAT_20_120(3,"НДС 20/120"),
    VAT_10_110(4,"НДС 10/110"),
    VAT_0(5,"НДС 0%"),
    VAT_NONE(6,"НДС НЕ ОБЛАГАЕТСЯ")
    ;

    public final byte bVal;
    public final String pName;

    private VatE(int value, String name) {
        this.bVal = (byte)value;
        this.pName = name;
    }

    public static VatE fromByte(byte number){
        for (VatE val:values()){
            if (val.bVal == number){
                return val;
            }
        }
        throw new InvalidParameterException("unknown value");
    }

    /**
     * Расчитать значение ставки для указаной суммы
     *
     * @param sum сумма
     * @return сумма ставки
     */
    public BigDecimal calc(BigDecimal sum) {
        switch (this) {
        case VAT_20:
        case VAT_20_120:
            return sum.multiply(BigDecimal.valueOf(20.0)).divide(BigDecimal.valueOf(120.0), MathContext.DECIMAL128);
        case VAT_10:
        case VAT_10_110:
            return sum.multiply(BigDecimal.valueOf(10.0)).divide(BigDecimal.valueOf(110.0), MathContext.DECIMAL128);
        default:
            return sum;
        }
    }

    /**
     * @return список наименований
     */
    public static List<String> getNames(){
        List<String> res = new ArrayList<>();
        for (VatE val:values()){
            res.add(val.pName);
        }
        return res;
    }
    @Override
    public String toString() {
    	return pName;
    }
}
