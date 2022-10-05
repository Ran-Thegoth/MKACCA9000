package rs.fncore.data;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Типы агентских услуг
 *
 * @author amv
 */

public enum AgentTypeE {
    NONE(0, "НЕТ", "Без агента"),
    /**
     * Банковский агент
     */
    BANK_AGENT((1<<0), "БАНК. ПЛ. АГЕНТ", "Оказание услуг покупателю (клиенту) пользователем, являющимся банковским платежным агентом"),
    /**
     * Банковский субагент
     */
    BANK_SUB_AGENT((1<<1), "БАНК. ПЛ. СУБАГЕНТ","Оказание услуг покупателю (клиенту) пользователем, являющимся банковским платежным субагентом"),
    /**
     * Платежный агент
     */
    PAYMENT_AGENT((1<<2),"ПЛ. АГЕНТ", "Оказание услуг покупателю (клиенту) пользователем, являющимся платежным агентом"),
    /**
     * Платежный субагент
     */
    PAYMENT_SUB_AGENT((1<<3),"ПЛ. СУБАГЕНТ", "Оказание услуг покупателю (клиенту) пользователем, являющимся платежным субагентом"),
    /**
     * Доверенное лицо
     */
    ATTORNEY((1<<4),"ПОВЕРЕННЫЙ","Осуществление расчета с покупателем (клиентом) пользователем, являющимся поверенным"),
    /**
     * Комиссионер
     */
    COMMISIONARE((1<<5),"КОМИССИОНЕР","Осуществление расчета с покупателем (клиентом) пользователем, являющимся комиссионером"),
    /**
     * Другой тип агента
     */
    OTHER((1<<6),"АГЕНТ","Осуществление расчета с покупателем (клиентом) пользователем, являющимся агентом и не являющимся банковским платежным агентом (субагентом), платежным агентом (субагентом), поверенным, комиссионером")
    ;

    public final byte bVal;
    public final String pName;
    public final String desc;

    private AgentTypeE(int value, String pName, String desc) {
        this.bVal = (byte)value;
        this.pName = pName;
        this.desc = desc;
    }

    public static AgentTypeE fromByte(byte number){
        for (AgentTypeE val:values()){
            if (val.bVal == number){
                return val;
            }
        }
        throw new InvalidParameterException("unknown value");
    }

    /**
     * Декодировать битовую маску агентских услуг в список
     *
     * @param val типы агентов в байте
     * @return Set типов агентов
     */
    public static Set<AgentTypeE> fromByteArray(byte val) {
        HashSet<AgentTypeE> result = new HashSet<>();
        for (AgentTypeE a : values()) {
            if (a==AgentTypeE.NONE) continue;
            if ((val & a.bVal) == a.bVal) {
                result.add(a);
            }
        }
        return result;
    }

    /**
     * Закодировать список в битовую маску
     *
     * @param val массив типов агентов
     * @return типы агентов в байте
     */
    public static byte toByteArray(Iterable<AgentTypeE> val) {
        byte result = 0;
        for (AgentTypeE a : val) {
            if (a==AgentTypeE.NONE) continue;
            result |= a.bVal;
        }
        return result;
    }

    /**
     * @return список наименований
     */
    public static List<String> getNames(){
        List<String> res = new ArrayList<>();
        for (AgentTypeE val:values()){
            res.add(val.pName);
        }
        return res;
    }
    @Override
    public String toString() {
    	return pName;
    }
}
