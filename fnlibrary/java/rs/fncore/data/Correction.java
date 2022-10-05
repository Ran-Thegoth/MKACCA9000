package rs.fncore.data;

import static rs.utils.Utils.formatInn;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.fncore.data.Payment.PaymentTypeE;
import rs.utils.Utils;

/**
 * Коррекция
 *
 * @author nick
 */
public class Correction extends SellOrder {

    public static final String CLASS_NAME="Correction";
    public static final String CLASS_UUID="632d0ae2-ebae-11eb-9a03-0242ac130003";

    /**
     * Тип коррекции
     *
     * @author nick
     */
    public enum CorrectionTypeE {
        /**
         * По предписанию
         */
        BY_ARBITARITY(1, "ПО ПРЕДПИСАНИЮ"),

        /**
         * Самостоятельно
         */
        BY_OWN(0, "САМОСТОЯТЕЛЬНО")
        ;

        public final byte bVal;
        public final String pName;

        private CorrectionTypeE(int value, String name) {
            this.bVal = (byte)value;
            this.pName = name;
        }

        @Override
        public String toString() {
        	return pName;
        }
        
        public static CorrectionTypeE fromByte(byte number){
            for (CorrectionTypeE val:values()){
                if (val.bVal == number){
                    return val;
                }
            }
            throw new InvalidParameterException("unknown value");
        }

        /**
         * @return список наименований
         */
        public static List<String> getNames(){
            List<String> res = new ArrayList<>();
            for (CorrectionTypeE val:values()){
                res.add(val.pName);
            }
            return res;
        }
    }

    protected CorrectionTypeE mType = CorrectionTypeE.BY_OWN;
    protected BigDecimal mSum = BigDecimal.ZERO;
    protected VatE mVat = VatE.VAT_20;
    protected String mBaseDocumentNo = Const.EMPTY_STRING;
    protected long mBaseDocumentDate = 0;
    private final ClientData mClientData = new ClientData();

    public Correction() {
    }

    /**
     * Создать новую коррекцию
     *
     * @param type      - тип коррекции
     * @param orderType - тип чека коррекции (приход, расход и т.д.)
     * @param sum       - сумма коррекции
     * @param vat       - ставка НДС
     * @param taxMode   - СНО
     */
    public Correction(CorrectionTypeE type, OrderTypeE orderType, BigDecimal sum, VatE vat, TaxModeE taxMode) {
        mSum = sum;
        mVat = vat;
        setType(type);
        setOrderType(orderType);
        setTaxMode(taxMode);
    }

    /**
     * Создать новую коррекцию
     *
     * @param type      - тип коррекции
     * @param orderType - тип чека коррекции (приход, расход и т.д.)
     * @param taxMode   - СНО
     */
    public Correction(CorrectionTypeE type, OrderTypeE orderType, TaxModeE taxMode) {
        setType(type);
        setOrderType(orderType);
        setTaxMode(taxMode);
    }

    /**
     * Данные клиента
     *
     * @return client data
     */
    public ClientData getClientData() {
        return mClientData;
    }

    /**
     * Список предметов расчета (копию)
     *
     * @return Список предметов расчета (копию)
     */
    public List<SellItem> getItems() {
        return new ArrayList<>(mItems);
    }

    /**
     * Добавить новую поизцию
     *
     * @param item позиция
     * @return успешно ли позиция была добавлена
     */
    public boolean addItem(SellItem item) {
        if (item.getPaymentType() == SellItem.ItemPaymentTypeE.CREDIT_PAYMENT && !mItems.isEmpty())
            return false;
        if (!mItems.isEmpty() && mItems.get(0).getPaymentType() == SellItem.ItemPaymentTypeE.CREDIT_PAYMENT)
            return false;
        mItems.add(item);
        return true;
    }

    /**
     * Удалить позицию
     *
     * @param item позиция
     */
    public void removeItem(SellItem item){
        mItems.remove(item);
    }

    /**
     * Получить платеж по типу
     *
     * @param type способ оплаты
     * @return тип оплаты
     */
    public Payment getPaymentByType(PaymentTypeE type) {
        return mPayments.get(type);
    }

    /**
     * Добавить тип оплаты
     * @param p тип оплаты
     * @return успешна ли операция
     */
    public boolean addPayment(Payment p) {
        if (mPayments.containsKey(p.getType()))
            return false;
        mPayments.put(p.getType(), p);
        return true;
    }

    /**
     * Сумма по предметам расчета
     *
     * @return sum
     */
    public BigDecimal getTotalSum() {
        BigDecimal result = BigDecimal.ZERO;
        for (SellItem item : mItems)
            result = result.add(item.getSum());
        return Utils.round2(result, 2);
    }

    /**
     * Сумма платежей
     *
     * @return payments
     */
    public BigDecimal getTotalPayments() {
        BigDecimal result = BigDecimal.ZERO;
        for (Payment payment : mPayments.values())
            result = result.add(payment.getValue());
        return Utils.round2(result, 2);
    }

    /**
     * Получить список платежей (копию)
     *
     * @return список платежей (копию)
     */
    public List<Payment> getPayments() {
        return new ArrayList<>(mPayments.values());
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        p.writeByte(mType.bVal);
        p.writeString(mSum.toString());
        p.writeInt(mVat.bVal);
        p.writeString(mBaseDocumentNo);
        p.writeLong(mBaseDocumentDate);
        p.writeInt(mBillNumber);
        p.writeInt(mShiftNumber);
        mItems.writeToParcel(p, flags);
        p.writeInt(mPayments.size());
        for (Payment payment : mPayments.values()) {
            payment.writeToParcel(p, flags);
        }
        mClientData.writeToParcel(p, flags);
        mSignature.operator().writeToParcel(p, flags);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        mType = CorrectionTypeE.fromByte(p.readByte());
        mSum = new BigDecimal(p.readString());
        mVat = VatE.fromByte(p.readByte());
        mBaseDocumentNo = p.readString();
        mBaseDocumentDate = p.readLong();
        mBillNumber = p.readInt();
        mShiftNumber = p.readInt();
        mItems.readFromParcel(p);
        int cnt = p.readInt();
        mPayments.clear();
        while (cnt-- > 0) {
            Payment payment = new Payment();
            payment.readFromParcel(p);
            mPayments.put(payment.getType(), payment);
        }
        mClientData.readFromParcel(p);
        mSignature.operator().readFromParcel(p);
    }

    /**
     * Номер смены
     *
     * @return Номер смены
     */
    public int getShiftNumber() {
        return mShiftNumber;
    }

    /**
     * Номер чека
     *
     * @return Номер чека
     */
    public int getNumber() {
        return mBillNumber;
    }

    /**
     * Номер документа-основния
     *
     * @return Номер документа-основния
     */
    public String getBaseDocumentNumber() {
        return mBaseDocumentNo;
    }

    /**
     * Указать номер документа-основания
     *
     * @param s номер документа-основания
     */
    public void setBaseDocumentNumber(String s) {
        if (s == null) s = Const.EMPTY_STRING;
        mBaseDocumentNo = s;
    }

    /**
     * Дата документа-основания
     *
     * @return Дата документа-основания
     */
    public long getBaseDocumentDate() {
        return mBaseDocumentDate;
    }

    /**
     * Установить дату документа-основания
     *
     * @param v Дата документа-основания
     */
    public void setBaseDocumentDate(long v) {
        mBaseDocumentDate = v;
    }

    public void setBaseDocumentDate(Calendar cal) {
        mBaseDocumentDate = cal.getTimeInMillis();
    }

    public void setBaseDocumentDate(Date date) {
        mBaseDocumentDate = date.getTime();
    }

    /**
     * Тип коррекции
     *
     * @return Тип коррекции
     */
    public CorrectionTypeE getCorrectionType() {
        return mType;
    }

    /**
     * Установить тип коррекции
     *
     * @param type тип коррекции
     */
    public void setType(CorrectionTypeE type) {
        mType = type;
    }

    /**
     * Тип чека коррекции
     *
     * @return Тип чека коррекции
     */
    public OrderTypeE getOrderType() {
        return getType();
    }

    /**
     * Установить тип чека
     *
     * @param type Тип чека коррекции
     */
    public void setOrderType(OrderTypeE type) {
        setType(mType);
    }

    /**
     * Получить используемую ставку НДС ФФД 1.05
     *
     * @return используемую ставку НДС ФФД 1.05
     */
    public VatE getVATMode() {
        return mVat;
    }

    /**
     * Установить используемую ставку НДС ФФД 1.05
     *
     * @param vat используемая ставка НДС ФФД 1.05
     */
    public void setVATMode(VatE vat) {
        mVat = vat;
    }

    /**
     * Получить сумму коррекции ФФД 1.05
     *
     * @return сумма коррекции ФФД 1.05
     */
    public BigDecimal getSum() {
        return mSum;
    }

    /**
     * Установить сумму коррекции ФФД 1.05
     *
     * @param sum сумма коррекции ФФД 1.05
     */
    public void setSum(BigDecimal sum){
        mSum = sum;
    }

    /**
     * Значение ставки НДС
     *
     * @return сумма ставки НДС
     */
    public BigDecimal getVatValue() {
        return mVat.calc(mSum);
    }

    /**
     * Получить сумму НДС по чеку
     *
     * @param vat тип НДС
     * @return сумма НДС чека для данного типа
     */
    public BigDecimal getVatValue(VatE vat) {
        BigDecimal result = BigDecimal.ZERO;
        for (SellItem i : mItems) {
            if (i.getVATType() == vat) {
                result = result.add(i.getVATValue());
            }
        }
        return Utils.round2(result, 2);
    }

    /**
     * Установить ИНН клиента
     *
     * @param inn ИНН клиента
     */
    public void setClientINN(String inn) {
        inn=formatInn(inn);
        add(FZ54Tag.T1228_CLIENT_INN, inn);
    }

    /**
     * @return ИНН клиента
     */
    public String getClientINN() {
        return getTagString(FZ54Tag.T1228_CLIENT_INN);
    }

    /**
     * Содержит маркированные товары
     *
     * @return cодержит ли маркированные товары
     */
    public boolean haveMarkingItems() {
        for (SellItem item : mItems){
            if (!item.getMarkingCode().isEmpty()){
                return true;
            }
        }
        return false;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassUUID() {
        return CLASS_UUID;
    }

    @Override
    public byte[][] packToFN() {
        add(FZ54Tag.T1009_TRANSACTION_ADDR, mLocation.getAddress());
        add(FZ54Tag.T1187_TRANSACTION_PLACE, mLocation.getPlace());
        add(FZ54Tag.T1055_USED_TAX_SYSTEM, getTaxMode().bVal);
        add(FZ54Tag.T1173_CORRECTION_TYPE, mType.bVal);
        {
            Tag tags = new Tag(FZ54Tag.T1174_CORRECTION_REASON_TLV);
            tags.add(FZ54Tag.T1178_CORRECTION_BASE_DATE, (int) (mBaseDocumentDate / 1000));
            if (getCorrectionType() == CorrectionTypeE.BY_ARBITARITY){
                tags.add(FZ54Tag.T1179_CORRECTION_BASE_NO, mBaseDocumentNo);
            }
            add(tags);
        }
        switch (mVat) {
            case VAT_20:
                add(FZ54Tag.T1102_VAT_20_SUM, getVatValue());
                break;
            case VAT_20_120:
                add(FZ54Tag.T1106_VAT_20_120_SUM, getVatValue());
                break;
            case VAT_10:
                add(FZ54Tag.T1103_VAT_10_SUM, getVatValue());
                break;
            case VAT_10_110:
                add(FZ54Tag.T1107_VAT_10_110_SUM, getVatValue());
                break;
            case VAT_NONE:
                add(FZ54Tag.T1105_NO_VAT_SUM, getSum());
                break;
            case VAT_0:
                add(FZ54Tag.T1104_VAT_0_SUM, getSum());
                break;
        }
        BigDecimal[] payments = new BigDecimal[PaymentTypeE.values().length];
        Arrays.fill(payments, BigDecimal.ZERO);
        for (Payment payment : mPayments.values())
            payments[payment.getType().ordinal()] = payments[payment.getType().ordinal()].add(payment.getValue());

        add(FZ54Tag.T1031_CASH_SUM, payments[PaymentTypeE.CASH.ordinal()]);
        add(FZ54Tag.T1081_CARD_SUM, payments[PaymentTypeE.CARD.ordinal()]);
        add(FZ54Tag.T1215_PREPAY_SUM, payments[PaymentTypeE.PREPAYMENT.ordinal()]);
        add(FZ54Tag.T1216_POSTPAY_SUM, payments[PaymentTypeE.CREDIT.ordinal()]);
        add(FZ54Tag.T1217_OTHER_SUM, payments[PaymentTypeE.AHEAD.ordinal()]);

        if (mClientData.isExist()){
            add(mClientData);
        }

        return super.packToFN();
    }

    @Override
    protected boolean parseTag(Tag t) {
    	switch(t.getId()) {
    	case FZ54Tag.T1173_CORRECTION_TYPE:
    		mType = CorrectionTypeE.fromByte(t.asByte());
    		break;
    	case FZ54Tag.T1174_CORRECTION_REASON_TLV:
    		t.unpackSTLV();
    		if(t.hasTag(FZ54Tag.T1178_CORRECTION_BASE_DATE))
    			mBaseDocumentDate = t.getTag(FZ54Tag.T1178_CORRECTION_BASE_DATE).asUInt() * 1000L;
    		if(t.hasTag(FZ54Tag.T1179_CORRECTION_BASE_NO))
    			mBaseDocumentNo = t.getTag(FZ54Tag.T1179_CORRECTION_BASE_NO).asString();
    		return true;
    	default:
    		return super.parseTag(t);
    	}
    	return true;
    }
    
    protected static final String COR_TYPE = "correction.Type";
    protected static final String BASE_DOC = "correction.baseDocument";
    protected static final String BASE_DOC_DATE = "correction.baseDocument.Date";

    @Override
    public String onKey(String key) {
        switch (key){
            case COR_TYPE: return getCorrectionType().pName;
            case BASE_DOC: return getBaseDocumentNumber();
            case BASE_DOC_DATE: return Utils.formatDateS(getBaseDocumentDate());
            default:
                return super.onKey(key);
        }
    }
    
    public static final Parcelable.Creator<Correction> CREATOR = new Parcelable.Creator<Correction>() {

        @Override
        public Correction createFromParcel(Parcel p) {
            Correction result = new Correction();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public Correction[] newArray(int size) {
            return new Correction[size];
        }
    };
}
