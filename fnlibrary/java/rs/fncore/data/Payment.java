package rs.fncore.data;

import android.os.Parcel;

import java.math.BigDecimal;
import java.security.InvalidParameterException;

/**
 * Оплата
 *
 * @author nick
 */
public class Payment implements IReableFromParcel {

    /**
     * Способы оплаты
     *
     * @author nick
     */
    public enum PaymentTypeE {
        /**
         * Наличные
         */
        CASH("Наличными"),
        /**
         * Безналичные
         */
        CARD("Безналичными"),
        /**
         * Предоплата
         */
        PREPAYMENT("Предварительная оплата"),
        /**
         * Кредит
         */
        CREDIT("Кредит, последующая оплата"),
        /**
         * Встречная
         */
        AHEAD("Встречная");


        public final String pName;

        private PaymentTypeE(String name) {
            this.pName = name;
        }


        public static PaymentTypeE fromString(String name){
            for (PaymentTypeE val:values()){
                if (val.pName.equals(name)){
                    return val;
                }
            }
            throw new InvalidParameterException("unknown value");
        }
        @Override
        public String toString() {
        	return pName;
        }
    }

    private PaymentTypeE mType = PaymentTypeE.CASH;
    private BigDecimal mValue = BigDecimal.ZERO;

    public Payment() {
    }

    /**
     * @param type  - тип способа оплаты
     * @param value - сумма
     */
    public Payment(PaymentTypeE type, BigDecimal value) {
        mType = type;
        mValue = value;
    }

    /**
     * Оплата наличными
     *
     * @param value сумма
     */
    public Payment(BigDecimal value) {
        this(PaymentTypeE.CASH, value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Получить тип оплаты
     *
     * @return тип оплаты
     */
    public PaymentTypeE getType() {
        return mType;
    }

    /**
     * Получить сумму оплаты
     *
     * @return сумма оплаты
     */
    public BigDecimal getValue() {
        return mValue;
    }

    /**
     * Изменить сумму оплаты
     *
     * @param val сумма оплаты
     */
    public void setValue(BigDecimal val) {
        if (val.compareTo(BigDecimal.ZERO) >= 0)
            mValue = val;
    }

    @Override
    public void writeToParcel(Parcel p, int arg1) {
        p.writeInt(mType.ordinal());
        p.writeString(mValue.toString());
    }

    @Override
    public void readFromParcel(Parcel p) {
        mType = PaymentTypeE.values()[p.readInt()];
        mValue = new BigDecimal(p.readString());

    }

    public static final Creator<Payment> CREATOR = new Creator<Payment>() {

        @Override
        public Payment createFromParcel(Parcel p) {
            Payment result = new Payment();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public Payment[] newArray(int size) {
            return new Payment[size];
        }
    };

}
