package rs.fncore.data;

import static rs.utils.Utils.formatInn;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.fncore.data.Payment.PaymentTypeE;
import rs.utils.Utils;

/**
 * Чек
 *
 * @author nick
 */
public class SellOrder extends Document implements IAgentOwner {

    public static final String CLASS_NAME="SellOrder";
    public static final String CLASS_UUID="c8bed264-ebae-11eb-9a03-0242ac130003";

    private static final SimpleDateFormat QR_DF = new SimpleDateFormat("yyyyMMdd'T'HHmm");

    private byte [] _payload = new byte[0];
    /**
     * Тип чека
     *
     * @author amv
     */
    public enum OrderTypeE {
        /**
         * Приход
         */
        INCOME(1,"ПРИХОД"),
        /**
         * Возврат прихода
         */
        RETURN_INCOME(2,"ВОЗВРАТ ПРИХОДА"),
        /**
         * Расход
         */
        OUTCOME(3, "РАСХОД"),
        /**
         * Возврат расхода
         */
        RETURN_OUTCOME(4,"ВОЗВРАТ РАСХОДА");

        public final byte bVal;
        public final String pName;

        private OrderTypeE(int value, String name) {
            this.bVal = (byte)value;
            this.pName = name;
        }

        public static int indexOf(OrderTypeE val) {
        	for(int i=0;i<values().length;i++)
        		if(val == values()[i]) return i;
        	return -1;
        }
        public static OrderTypeE fromByte(byte number){
            for (OrderTypeE val:values()){
                if (val.bVal == number){
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

    protected OrderTypeE mType = OrderTypeE.INCOME;
    protected ParcelableList<SellItem> mItems = new ParcelableList<>(SellItem.class);
    protected Map<PaymentTypeE, Payment> mPayments = new HashMap<>();
    protected BigDecimal mRefund = BigDecimal.ZERO;
    protected int mBillNumber;
    protected int mShiftNumber;
    protected String mFnsUrl;
    protected String mSenderEmail = Const.EMPTY_STRING;
    protected String mRecipientAddress = Const.EMPTY_STRING;
    private final AgentData mAgentData = new AgentData();
    private final ClientData mClientData = new ClientData();

    public SellOrder() {
    }

    /**
     * Создать новый чек
     * @param type - тип чека
     * @param mode - система налогообложения
     */
    public SellOrder(OrderTypeE type, TaxModeE mode) {
        mType = type;
        setTaxMode(mode);
    }

    /**
     * Агентские данные
     *
     * @return  Агентские данные
     */
    public AgentData getAgentData() {
        return mAgentData;
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
     * Режим налообложения получить
     *
     * @return tax mode
     */
    public TaxModeE getTaxMode() {
        Tag data = getTag(FZ54Tag.T1055_USED_TAX_SYSTEM);
        if (data == null) throw new InvalidParameterException("empty data");
        return TaxModeE.fromByte(data.asByte());
    }

    /**
     * Установить режим налообложения
     *
     * @param mode режим налообложения
     */
    public void setTaxMode(TaxModeE mode) {
        add(FZ54Tag.T1055_USED_TAX_SYSTEM, mode.bVal);
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        p.writeByte(mType.bVal);
        p.writeString(mRefund.toString());
        p.writeInt(mBillNumber);
        p.writeInt(mShiftNumber);
        p.writeString(mFnsUrl);
        p.writeString(mSenderEmail);
        p.writeString(mRecipientAddress);
        mItems.writeToParcel(p, flags);
        p.writeInt(mPayments.size());
        for (Payment payment : mPayments.values())
            payment.writeToParcel(p, flags);
        mAgentData.writeToParcel(p, flags);
        mClientData.writeToParcel(p, flags);
        mSignature.operator().writeToParcel(p, flags);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        mType = OrderTypeE.fromByte(p.readByte());
        mRefund = new BigDecimal(p.readString());
        mBillNumber = p.readInt();
        mShiftNumber = p.readInt();
        mFnsUrl = p.readString();
        mSenderEmail = p.readString();
        mRecipientAddress = p.readString();
        mItems.readFromParcel(p);
        int cnt = p.readInt();
        mPayments.clear();
        while (cnt-- > 0) {
            Payment payment = new Payment();
            payment.readFromParcel(p);
            mPayments.put(payment.getType(), payment);
        }
        mAgentData.readFromParcel(p);
        mClientData.readFromParcel(p);
        mSignature.operator().readFromParcel(p);
    }

    @Override
    public byte[][] packToFN() {
        remove(FZ54Tag.T1224_SUPPLIER_DATA_TLV);
        remove(FZ54Tag.T1223_AGENT_DATA_TLV);
        remove(FZ54Tag.T1057_AGENT_FLAG);

        add(FZ54Tag.T1009_TRANSACTION_ADDR, mLocation.getAddress());
        add(FZ54Tag.T1187_TRANSACTION_PLACE, mLocation.getPlace());

//        Set<AgentTypeE> agents = new HashSet<>();
        BigDecimal[] vat = new BigDecimal[VatE.values().length];
        boolean [] isZeo = new boolean[VatE.values().length];
        Arrays.fill(vat, BigDecimal.ZERO);
        for (SellItem item : mItems) {
        	if (item.getSum().compareTo(BigDecimal.ZERO) == 0)
        		isZeo[item.getVATType().ordinal()]  = true;
            vat[item.getVATType().ordinal()] = vat[item.getVATType().ordinal()].add(item.getVATValue());
            if (item.getAgentData().getType() != AgentTypeE.NONE) {
/*            	Tag supplier = item.getAgentData().getTag(FZ54Tag.T1226_SUPPLIER_INN);
            	if(supplier != null) {
            		item.add(supplier);
            		item.getAgentData().remove(FZ54Tag.T1226_SUPPLIER_INN);
            	}
*/
            }
        }

        if (vat[VatE.VAT_20.ordinal()].compareTo(BigDecimal.ZERO) > 0 || isZeo[VatE.VAT_20.ordinal()])
            add(FZ54Tag.T1102_VAT_20_SUM, vat[VatE.VAT_20.ordinal()]);
        if (vat[VatE.VAT_10.ordinal()].compareTo(BigDecimal.ZERO) > 0 || isZeo[VatE.VAT_10.ordinal()])
            add(FZ54Tag.T1103_VAT_10_SUM, vat[VatE.VAT_10.ordinal()]);
        if (vat[VatE.VAT_20_120.ordinal()].compareTo(BigDecimal.ZERO) > 0 || isZeo[VatE.VAT_20_120.ordinal()])
            add(FZ54Tag.T1106_VAT_20_120_SUM, vat[VatE.VAT_20_120.ordinal()]);
        if (vat[VatE.VAT_10_110.ordinal()].compareTo(BigDecimal.ZERO) > 0 ||  isZeo[VatE.VAT_10_110.ordinal()])
            add(FZ54Tag.T1107_VAT_10_110_SUM, vat[VatE.VAT_10_110.ordinal()]);
        if (vat[VatE.VAT_0.ordinal()].compareTo(BigDecimal.ZERO) > 0 ||  isZeo[VatE.VAT_0.ordinal()])
            add(FZ54Tag.T1104_VAT_0_SUM, vat[VatE.VAT_0.ordinal()]);
        if (vat[VatE.VAT_NONE.ordinal()].compareTo(BigDecimal.ZERO) > 0 ||  isZeo[VatE.VAT_NONE.ordinal()])
            add(FZ54Tag.T1105_NO_VAT_SUM, vat[VatE.VAT_NONE.ordinal()]);
        BigDecimal[] payments = new BigDecimal[PaymentTypeE.values().length];
        Arrays.fill(payments, BigDecimal.ZERO);
        for (Payment payment : mPayments.values()) {
            payments[payment.getType().ordinal()] = payment.getValue().setScale(2, RoundingMode.HALF_UP);
        }

        add(FZ54Tag.T1031_CASH_SUM, payments[PaymentTypeE.CASH.ordinal()]);
        add(FZ54Tag.T1081_CARD_SUM, payments[PaymentTypeE.CARD.ordinal()]);
        add(FZ54Tag.T1215_PREPAY_SUM, payments[PaymentTypeE.PREPAYMENT.ordinal()]);
        add(FZ54Tag.T1216_POSTPAY_SUM, payments[PaymentTypeE.CREDIT.ordinal()]);
        add(FZ54Tag.T1217_OTHER_SUM, payments[PaymentTypeE.AHEAD.ordinal()]);
        if (!mRecipientAddress.isEmpty()) {
            add(FZ54Tag.T1008_BUYER_PHONE_EMAIL, mRecipientAddress);
        }

        if (!mSenderEmail.isEmpty()) {
            add(FZ54Tag.T1117_SENDER_EMAIL, mSenderEmail);
        }

        if (mAgentData.getType() != AgentTypeE.NONE) {
            add(FZ54Tag.T1057_AGENT_FLAG, mAgentData.getType().bVal);
            for(int i=0;i<AgentData.TAGS_1223.size();i++) {
            	if(mAgentData.hasTag(AgentData.TAGS_1223.keyAt(i)))
            		add(mAgentData.getTag(AgentData.TAGS_1223.keyAt(i)));
            }
            for(int i=0;i<AgentData.TAGS_1224.size();i++) {
            	if(mAgentData.hasTag(AgentData.TAGS_1224.keyAt(i)))
            		add(mAgentData.getTag(AgentData.TAGS_1224.keyAt(i)));
            }
        }

        if (mClientData.isExist()){
            add(mClientData);
        }

        return super.packToFN();
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
     * Получить сдачу
     *
     * @return refund
     */
    public BigDecimal getRefund() {
        return Utils.round2(mRefund, 2);
    }

    /**
     * Установить значение сдачи
     *
     * @param val refund
     */
    public void setRefund(BigDecimal val) {
        if (val.compareTo(BigDecimal.ZERO) >= 0)
            mRefund = val;
    }

    /**
     * Номер смены (Доступен только после фискализации)
     *
     * @return shift
     */
    public int getShiftNumber() {
        return mShiftNumber;
    }

    /**
     * Номер чека (Доступен только после фискализации)
     *
     * @return bill number
     */
    public int getNumber() {
        return mBillNumber;
    }

    /**
     * Адрес сайта ФНС
     *
     * @return fns url
     */
    public String getFnsUrl() {
        return mFnsUrl;
    }

    /**
     * Почтовый адрес отправителя чеков
     *
     * @return sender mail
     */
    public String getSenderEmail() {
        return mSenderEmail;
    }

    /**
     * @return Адрес получателя чека
     */
    public String getRecipientAddress() {
        return mRecipientAddress;
    }

    /**
     * Установить адрес получателя чека
     *
     * @param v адрес получателя чека
     */
    public void setRecipientAddress(String v) {
        if (v == null) {
            v = Const.EMPTY_STRING;
        }
        mRecipientAddress = v;
    }

    /**
     * Найти оплату по типу
     *
     * @param type - тип оплаты
     * @return размер оплаты
     */
    public Payment getPaymentByType(PaymentTypeE type) {
        return mPayments.get(type);
    }

    /**
     * @return тип чека
     */
    public OrderTypeE getType() {
        return mType;
    }

    /**
     * Установить тип чека
     *
     * @param type Тип чека
     */
    public void setType(OrderTypeE type) {
        mType = type;
    }

    /**
     * Проверка на маркированные товары
     *
     * @return показатель успеха
     */
    public boolean haveMarkingItems() {
        for (SellItem item : mItems){
            if (!item.getMarkingCode().isEmpty()){
                return true;
            }
        }
        return false;
    }

    /**
     * Получить сумму НДС по чеку
     *
     * @param vat тип НДС
     * @return сумма
     */
    public BigDecimal getVatValue(VatE vat) {
        BigDecimal result = BigDecimal.ZERO;
        for (SellItem i : mItems)
            if (i.getVATType() == vat)
                result = result.add(i.getVATValue());
        return Utils.round2(result, 2);
    }

    /**
     * @return Список предметов расчета (копию)
     */
    public List<SellItem> getItems() {
        return mItems;
    }

    /**
     * Добавить новую поизцию
     *
     * @param item - позиция на добавление
     * @return успешное или не успешное досбалвение позиции
     */
    public boolean addItem(SellItem item) {
/*        if (item.getPaymentType() == ItemPaymentTypeE.CREDIT_PAYMENT && !mItems.isEmpty()) {
            return false;
        }
        if (!mItems.isEmpty() && mItems.get(0).getPaymentType() == ItemPaymentTypeE.CREDIT_PAYMENT) {
            return false;
        } */
        mItems.add(item);
        return true;
    }

    /**
     * Удалить позицию
     *
     * @param item позиция для удаления
     */
    public void removeItem(SellItem item){
        mItems.remove(item);
    }

    /**
     * Добавить оплату
     *
     * @param payment - тип оплаты
     * @return - успещное ил не успешное добавление
     */
    public boolean addPayment(Payment payment) {
        if (mPayments.containsKey(payment.getType())) return false;
        mPayments.put(payment.getType(), payment);
        return true;
    }

    /**
     * Очистить список платежей по чеку
     */
    public void clearPayments() {
    	mPayments.clear();
    }
    /**
     * @return копия списка оплат
     */
    public Collection<Payment> getPayments() {
        return mPayments.values();
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

    public static final Parcelable.Creator<SellOrder> CREATOR = new Parcelable.Creator<SellOrder>() {

        @Override
        public SellOrder createFromParcel(Parcel p) {
            SellOrder result = new SellOrder();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public SellOrder[] newArray(int size) {
            return new SellOrder[size];
        }

    };

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassUUID() {
        return CLASS_UUID;
    }

    private static final String ORDER_TYPE = "order.Type";
    private static final String ORDER_NUMEBER = "order.Number";
    private static final String ORDER_VAT_VALUE = "order.Vat_";
    private static final String ORDER_SUM = "order.Sum.";
    private static final String REFUND = "order.Refund";
    private static final String BARCODE = "order.Barcode";
    private static final String AGENT_TYPE = "order.AgentType";
    private static final String TOTAL = "Total";
    private static final String TAX_MODE = "taxMode";
    private static final String FNS_URL = "fns_url";
    private static final String SENDER_EMAIL = "sender_email";
    private static final String SHIFT_NUMBER = "shift.Number";

    @Override
    protected boolean parseTag(Tag t) {
    	switch(t.getId()) {
    	case FZ54Tag.T1038_SHIFT_NO:
    		mShiftNumber = t.asInt();
    		break;
    	case FZ54Tag.T1042_SHIFT_BILL_NO:
    		mBillNumber = t.asInt();
    		break;
    	case FZ54Tag.T1054_TRANSACTION_TYPE:
    		mType = OrderTypeE.fromByte(t.asByte());
    		break;
    	case FZ54Tag.T1102_VAT_20_SUM:
    	case FZ54Tag.T1107_VAT_10_110_SUM:

    	case FZ54Tag.T1020_TRANSACTION_SUM:
    		break;
    	case FZ54Tag.T1055_USED_TAX_SYSTEM:
    		break;
    	case FZ54Tag.T1060_FNS_URL:
    		mFnsUrl = t.asString().trim();
    		break;
    	case FZ54Tag.T1031_CASH_SUM:
    		mPayments.put(PaymentTypeE.CASH, new Payment(BigDecimal.valueOf(t.asDouble())));
    		break;
    	case FZ54Tag.T1081_CARD_SUM:
    		mPayments.put(PaymentTypeE.CARD, new Payment(BigDecimal.valueOf(t.asDouble())));
    		break;
    	case FZ54Tag.T1215_PREPAY_SUM:
    		mPayments.put(PaymentTypeE.PREPAYMENT, new Payment(BigDecimal.valueOf(t.asDouble())));
    		break;
    	case FZ54Tag.T1216_POSTPAY_SUM:
    		mPayments.put(PaymentTypeE.CREDIT, new Payment(BigDecimal.valueOf(t.asDouble())));
    		break;
    	case FZ54Tag.T1217_OTHER_SUM:
    		mPayments.put(PaymentTypeE.AHEAD, new Payment(BigDecimal.valueOf(t.asDouble())));
    		break;
    	case FZ54Tag.T1117_SENDER_EMAIL:
    		mSenderEmail = t.asString().trim();
    		break;
    	case FZ54Tag.T1059_ITEM_TLV:
    		mItems.add(new SellItem(t));
    		return true;
    	case FZ54Tag.T1057_AGENT_FLAG:
    		mAgentData.setType(AgentTypeE.fromByte(t.asByte()));
    		return true;
    	default:
    		if(AgentData.isAgentTag(t.getId())) {
    			mAgentData.add(t);
    			return true;
    		}
   			return super.parseTag(t);
    	}
    	return false;

    }
    @SuppressLint("DefaultLocale")
	@Override
    public String onKey(String key) {
        switch (key) {
            case AGENT_TYPE:
                if (getAgentData().getType() == AgentTypeE.NONE) return Const.EMPTY_STRING;
                return getAgentData().getType().pName;
            case FNS_URL:
                return getFnsUrl();
            case TAX_MODE:
                return getTaxMode().pName;
            case SENDER_EMAIL:
                return getSenderEmail();
            case ORDER_TYPE:
                return getType().pName;
            case ORDER_NUMEBER:
                return String.valueOf(getNumber());
            case SHIFT_NUMBER:
                return String.valueOf(getShiftNumber());
            case REFUND:
                return String.format(Locale.ROOT,"%.2f", getRefund());
            case BARCODE:
                return "t=" + QR_DF.format(new Date(signature().signDate())) + "&s=" + String.format(Locale.ROOT, "%.2f", getTotalSum())
                        + "&fn=" + signature().signer().FNNumber() + "&i=" + signature().getFdNumber() + "&fp=" + signature().getFpd()
                        + "&n=" + getType().bVal;
            default: {
                if (key.startsWith(ORDER_VAT_VALUE)) {
                    key = key.replace(ORDER_VAT_VALUE, "");
                    try {
                        VatE v = VatE.valueOf("VAT_" + key.toUpperCase());
                        BigDecimal d = getVatValue(v);
                        return String.format(Locale.ROOT,"%.2f", d);
                    } catch (Exception e) {
                        return String.format(Locale.ROOT,"%.2f", 0.0);
                    }

                } else if (key.startsWith(ORDER_SUM)) {
                    key = key.replace(ORDER_SUM, "");
                    if (TOTAL.equals(key)) {
                        return String.format(Locale.ROOT,"%.2f", getTotalSum());
                    }
                    try {
                        Payment p = getPaymentByType(PaymentTypeE.valueOf(key.toUpperCase()));
                        if (p == null) return "0.0";

                        BigDecimal v = p.getValue();
                        if (p.getType() == PaymentTypeE.CASH) {
                            v = v.add(getRefund());
                        }
                        return String.format(Locale.ROOT,"%.2f", v);
                    } catch (Exception e) {
                        return String.format(Locale.ROOT,"%.2f", 0.0);
                    }
                } else {
                    return super.onKey(key);
                }
            }
        }
    }
/*    public byte [] getPayload() { return _payload; }
    public void setPayload(byte [] val) {
    	_payload = val == null ? new byte[0] : val;
    } */
}
