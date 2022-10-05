package rs.fncore.data;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Системы налогообложения
 *
 * @author amv
 */

public enum TaxModeE {
    /**
     * Общая
     */
    COMMON((1<<0),"ОСН", "Общая"),
    /**
     * Упрощенная
     */
    SIMPLE_INCOME((1<<1),"УСН доход","Упрощенная"),
    /**
     * Упрощенная "доход - расход"
     */
    SIMPLE_INCOME_EXCENSE((1<<2),"УСН доход - расход","Упрощенная \"доход - расход\""),
    /**
     * Единый налог
     */
    UNITED_TAX((1<<3),"ЕНВД","Единый налог"),
    /**
     * Сельскохозяйственный налог
     */
    AGRO_TAX((1<<4),"ЕСН","Сельскохозяйственный налог"),
    /**
     * Патентная
     */
    PATENT((1<<5),"Патент","Патентная");

    public final byte bVal;
    public final String pName;
    public final String desc;

    private TaxModeE(int value,String name, String descr) {
        this.bVal = (byte)value;
        this.pName = name;
        this.desc = descr;
    }

    public static TaxModeE fromByte(byte number){
        for (TaxModeE val:values()){
            if (val.bVal == number){
                return val;
            }
        }
        throw new InvalidParameterException("unknown value");
    }

    public static byte toByteArray(Iterable<TaxModeE> modes) {
        byte b = 0;
        for (TaxModeE mode : modes)
            b |= mode.bVal;
        return b;
    }
    public static int indexOf(TaxModeE val) {
    	for(int i=0;i<values().length;i++)
    		if(val == values()[i]) return i;
    	return -1;
    }

    public static Set<TaxModeE> fromByteArray(byte val) {
        Set<TaxModeE> result = new HashSet<>();
        for (TaxModeE mode : values()) {
            if ((val & mode.bVal) == mode.bVal)
                result.add(mode);
        }
        return result;
    }

    /**
     * @return список наименований
     */
    public static List<String> getNames(){
        List<String> res = new ArrayList<>();
        for (TaxModeE val:values()){
            res.add(val.pName);
        }
        return res;
    }
    @Override
    public String toString() {
    	return pName;
    }
}
