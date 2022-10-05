package rs.fncore.data;

import android.os.Parcel;
import java.math.BigDecimal;
import java.math.MathContext;
import java.security.InvalidParameterException;
import java.util.Locale;
import java.util.UUID;

import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.fncore.data.MarkingCode.ItemCheckResult2106;
import rs.utils.Utils;

/**
 * Предмет расчета
 *
 * @author nick
 */
public class SellItem extends Tag implements IReableFromParcel,TemplateProcessor,IAgentOwner {

    /**
     * Тип предмета расчета
     *
     * @author amv
     */

	public enum SellItemTypeE {
        GOOD(1, "ТОВАР", "Т"),
        EXCISES_GOOD(2, "ПОДАКЦИЗНЫЙ ТОВАР","АТ"),
        WORK(3, "РАБОТА","Р"),
        SERVICE(4, "УСЛУГА", "У"),
        BET(5, "СТАВКА ИГРЫ", "СА"),
        GAIN(6, "ВЫИГРЫШ АИ", "ВА"),
        LOTTERY_TICKET(7, "ЛОТЕРЕЙНЫЙ БИЛЕТ","СЛ"),
        LOTTERY_GAIN(8,"ВЫИГРЫШ ЛОТЕРЕИ","ВЛ"),
        RID(9, "ПРЕДОСТАВЛЕНИЕ РИД", "РИД"),
        PAYMENT(10, "ПЛАТЕЖ", "В"),
        AGENT_COMISSION(11, "АГЕНТСКОЕ ВОЗНАГРАЖДЕНИЕ","АВ"),
        COMPOSE(12, "ВЫПЛАТА", "В"),
        MISC(13,"ИНОЙ ПРЕДМЕТ РАСЧЕТА","ИПР"),
        PROPERTY(14, "ИМУЩЕСТВЕННОЕ ПРАВО", ""),
        NON_SALES(15,"ВНЕРЕАЛИЗАЦИОННЫЙ ДОХОД",""),
        ANOTHER_PAYMENTS(16, "ИНЫЕ ПЛАТЕЖИ И ВЗНОСЫ",""),
        TRADE_FEE(17, "ТОРГОВЫЙ СБОР",""),
        RESORT_FEE(18, "КУРОРТНЫЙ СБОР",""),
        PLEDGE(19,"ЗАЛОГ", "" ),
        PRODUCTION_COSTS(20, "РАСХОД",""),
        COMPULSORY_PENSION_INSURANCE_INDIVILUAL(21, "ВЗНОСЫ НА ОБЯЗАТЕЛЬНОЕ ПЕНСИОННОЕ СТРАХОВАНИЕ ИП",
                "ВЗНОСЫ НА ОПС ИП"),
        COMPULSORY_PENSION_INSURANCE(22, "ВЗНОСЫ НА ОБЯЗАТЕЛЬНОЕ ПЕНСИОННОЕ СТРАХОВАНИЕ",
                "ВЗНОСЫ НА ОПС"),
        COMPULSORY_MEDICAL_INSURANCE_INDIVILUAL(23, "ВЗНОСЫ НА ОБЯЗАТЕЛЬНОЕ МЕДИЦИНСКОЕ СТРАХОВАНИЕ ИП",
                "ВЗНОСЫ НА ОМС ИП"),
        COMPULSORY_MEDICAL_INSURANCE(24, "ВЗНОСЫ НА ОБЯЗАТЕЛЬНОЕ МЕДИЦИНСКОЕ СТРАХОВАНИЕ",
                "ВЗНОСЫ НА ОМС"),
        COMPULSORY_SOCIAL_INSURANCE(25, "ВЗНОСЫ НА ОБЯЗАТЕЛЬНОЕ СОЦИАЛЬНОЕ СТРАХОВАНИЕ",
                "ВЗНОСЫ НА ОСС"),
        CASINO_PAYMENT(26, "ПЛАТЕЖ КАЗИНО", "ПК"),
        FUNDS_DISTRIBUTION(27, "ВЫДАЧА ДЕНЕЖНЫХ СРЕДСТВ","ВЫДАЧА ДС"),
        EXCISABLE_MARK_GOODS_NO_MARK(30, "АТНМ","АТНМ"),
        EXCISABLE_MARK_GOODS_MARK(31, "АТМ","АТМ"),
        MARK_GOODS_NO_MARK(32, "ТНМ","ТНМ"),
        MARK_GOODS_MARK(33, "ТМ","ТМ"),
        ;

        public final byte bVal;
        public final String pName;
        public final String shortName;

        private SellItemTypeE(int value, String name, String shortName) {
            this.bVal = (byte)value;
            this.pName = name;
            this.shortName=shortName;
        }

        public static SellItemTypeE fromByte(byte number){
            for (SellItemTypeE val:values()){
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

    /**
     * Тип способа оплаты
     *
     * @author amv
     */
    public enum ItemPaymentTypeE {
        /**
         * Предоплата 100%
         */
        AHEAD_100(1, "Предоплата 100%"),
        /**
         * Предоплата
         */
        AHEAD(2, "Предоплата"),
        /**
         * Встречная
         */
        AVANCE(3, "Аванс"),
        /**
         * Полный расчет
         */
        FULL(4, "Полный расчет"),
        /**
         * Частичный кредит
         */
        PATRIAL_CREDIT(5, "Частичный расчет и кредит"),
        /**
         * Передача в кредит
         */
        CREDIT_TRANSFER(6, "Передача в кредит"),
        /**
         * Оплата кредита
         */
        CREDIT_PAYMENT(7, "Оплата кредита")
        ;

        public final byte bVal;
        public final String pName;

        private ItemPaymentTypeE(int value, String name) {
            this.bVal = (byte)value;
            this.pName = name;
        }

        public static ItemPaymentTypeE fromByte(byte number){
            for (ItemPaymentTypeE val:values()){
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

    public SellItem() {
        mTagId=FZ54Tag.T1059_ITEM_TLV;
    }
    public SellItem(Tag tag) {
    	this();
    	tag.unpackSTLV();
    	for(Tag t : tag.getChilds()) {
    		switch(t.getId()) {
    		case FZ54Tag.T2108_MEASURE_AMOUNT_ITEM:
    			mMeasure = MeasureTypeE.fromByte(t.asByte());
    			break;
    		case FZ54Tag.T1223_AGENT_DATA_TLV:
    		case FZ54Tag.T1224_SUPPLIER_DATA_TLV:
    			mAgentData.addAll(t.asRaw());
    			break;
    		case FZ54Tag.T1214_PAYMENT_TYPE:
    			mItemPaymentType = ItemPaymentTypeE.fromByte(t.asByte());
    			break;
    		case FZ54Tag.T1212_ITEM_TYPE:
    			mType = SellItemTypeE.fromByte(t.asByte());
    			break;
    		case FZ54Tag.T1030_SUBJECT:
    			mName = t.asString().trim();
    			break;
    		case FZ54Tag.T1222_AGENT_FLAGS:
    			mAgentData.setType(AgentTypeE.fromByte(t.asByte()));
    			break;
    		case FZ54Tag.T1079_ONE_ITEM_PRICE:
    			mPrice = BigDecimal.valueOf(t.asDouble());
    			break;
    		case FZ54Tag.T1023_QUANTITY:
    			mQtty = BigDecimal.valueOf(t.asDouble(DataTypeE.F));
    			break;
    		case FZ54Tag.T1199_VAT_ID:
    			mVat = VatE.fromByte(t.asByte());
    			break;
    		case FZ54Tag.T1163_GOODS_CODE:
    			mMarkingCode.addAll(t.asRaw());
    			break;
    		case FZ54Tag.T2106_MARKING_ITEM_CHECK:
    			mMarkResult = new ItemCheckResult2106(t.asByte());
    			break;
    		}
    		getChilds().add(t);
    	}
    }

    private ItemPaymentTypeE mItemPaymentType = ItemPaymentTypeE.FULL;
    private SellItemTypeE mType = SellItemTypeE.GOOD;
    private String mName = Const.EMPTY_STRING;
    protected MeasureTypeE mMeasure = MeasureTypeE.PIECE;
    private BigDecimal mQtty = BigDecimal.ONE;
    private BigDecimal mPrice = BigDecimal.ZERO;
    private VatE mVat = VatE.VAT_NONE;
    private volatile AgentData mAgentData = new AgentData();
    protected volatile MarkingCode mMarkingCode = new MarkingCode();
    protected ItemCheckResult2106 mMarkResult = new ItemCheckResult2106((byte)0); 
    public String mUUID = UUID.randomUUID().toString();
    

    public SellItem(SellItemTypeE type, ItemPaymentTypeE paymentType, String name, BigDecimal qtty, MeasureTypeE measure,
                    BigDecimal price, VatE vat) {
        this();
        mType = type;
        mName = name;
        mQtty = qtty;
        mMeasure = measure;
        mPrice = price;
        mVat = vat;
        mItemPaymentType = paymentType;

        if (mMeasure == null) {
            if (mQtty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) > 0) {
                mMeasure = MeasureTypeE.KILOGRAM;
            }
            else {
                mMeasure = MeasureTypeE.PIECE;
            }
        }
    }

    public SellItem(String name, BigDecimal qtty, BigDecimal price, VatE vat) {
        this(SellItemTypeE.GOOD, ItemPaymentTypeE.FULL, name, qtty,
                (qtty.remainder(BigDecimal.ONE).compareTo(BigDecimal.valueOf(0.001)) == -1 ?
                        MeasureTypeE.PIECE : MeasureTypeE.KILOGRAM),
                price, vat);
    }

    public SellItem(String name, BigDecimal qtty, MeasureTypeE measure, BigDecimal price, VatE vat) {
        this(SellItemTypeE.GOOD, ItemPaymentTypeE.FULL, name, qtty, measure, price, vat);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Установить код маркировки
     *
     * @param code код маркировки
     */
    public void setMarkingCode(MarkingCode code) {
        mMarkingCode = code;
    }

    /**
     * Установить код маркировки - с учетом типа заказа
     *
     * @param codeStr код маркировки
     * @param type тип чека
     */
    public void setMarkingCode(String codeStr, SellOrder.OrderTypeE type) {
        if (codeStr.isEmpty()) return;

        MarkingCode.PlannedItemStatusE itemStatus;
        switch (type){
            case OUTCOME:
            case RETURN_OUTCOME:
                itemStatus =  MarkingCode.PlannedItemStatusE.ITEM_NOT_CHANGED;
                break;
            case INCOME:
                if (mMeasure == MeasureTypeE.PIECE){
                    itemStatus =  MarkingCode.PlannedItemStatusE.PIECE_ITEM_SELL;
                }
                else{
                    itemStatus =  MarkingCode.PlannedItemStatusE.MEASURED_ITEM_SELL;
                }
                break;
            case RETURN_INCOME:
                if (mMeasure == MeasureTypeE.PIECE){
                    itemStatus =  MarkingCode.PlannedItemStatusE.PIECE_ITEM_RETURNED;
                }
                else{
                    itemStatus =  MarkingCode.PlannedItemStatusE.MEASURED_ITEM_RETURNED;
                }
                break;
            default : throw new InvalidParameterException("unknown sell order type");
        }
        setMarkingCode(new MarkingCode(codeStr,itemStatus));
    }

    /**
     * @return код маркировки
     */
    public MarkingCode getMarkingCode() {
        return mMarkingCode;
    }
    public ItemCheckResult2106 getMarkCheckResult() {
    	return mMarkingCode.mMarkingCheckResult;
    }

    /**
     * Установить дробное количество маркированного товара
     *
     * @param itemsNumber количество упаковок
     * @param intemsInPackage товара в упаковке
     */
    public void setMarkingFractionalAmount(int itemsNumber, int intemsInPackage) throws InvalidParameterException{
        if (mMeasure != MeasureTypeE.PIECE || !getMarkingCode().isEmpty()) {
            throw new InvalidParameterException("wrong measure type or marking not applied");
        }

        Tag tlv = new Tag(FZ54Tag.T1291_MARKING_FRACTIONAL_ITEM);

        tlv.add(FZ54Tag.T1293_FRACTIONAL_MARKING_ITEM_NUMERATOR, itemsNumber);
        tlv.add(FZ54Tag.T1294_FRACTIONAL_MARKING_ITEM_DENOMERATOR, intemsInPackage);
        tlv.add(FZ54Tag.T1292_FRACTIONAL_MARKING_ITEM_FRACT, "" + itemsNumber + "/" + intemsInPackage);

        add(tlv);
        add(FZ54Tag.T1023_QUANTITY, BigDecimal.ONE, 3);
    }

    /**
     * @return тип оплаты
     */
    public ItemPaymentTypeE getPaymentType() {
        return mItemPaymentType;
    }
    public SellItem setPaymentType(ItemPaymentTypeE value) {
    	mItemPaymentType = value;
    	return this;
    }
    public SellItem setPrice(BigDecimal value) {
    	mPrice = value;
    	return this;
    }

    /**
     * @return тип предмета расчета
     */
    public SellItemTypeE getType() {
        return mType;
    }

    public SellItem setMeasure(MeasureTypeE val) {
    	mMeasure = val;
    	return this;
    }
    /**
     * @return наименование предмета расчета
     */
    public String getName() {
        return mName;
    }

    /**
     * @return UUID генерирующийся при создании класса
     */
    public String getUUID() {
        return mUUID;
    }

    /**
     * @return наименование единицы измерения
     */
    public MeasureTypeE getMeasure() {
        return mMeasure;
    }


    /**
     * @return количество
     */
    public BigDecimal getQTTY() {
        return Utils.round2(mQtty, 3);
    }

    /**
     * @return стоимость
     */
    public BigDecimal getPrice() {
        return Utils.round2(mPrice, 2);
    }

    /**
     * @return сумму (стоимость * количество)
     */
    public BigDecimal getSum() {
        return Utils.round2(mQtty.multiply(mPrice, MathContext.DECIMAL128), 2);
    }

    /**
     * @return тип НДС
     */
    public VatE getVATType() {
        return mVat;
    }

    /**
     * @return значение ставки НДС
     */
    public BigDecimal getVATValue() {
        return Utils.round2(mVat.calc(mQtty.multiply(mPrice, MathContext.DECIMAL128)), 2);
    }

    /**
     * @return Агентские данные
     */
    public AgentData getAgentData() {
        return mAgentData;
    }

    /**
     * Упаковать предмет расчета в тег
     *
     * @return тег
     */
    public Tag formatTag() {
        remove(FZ54Tag.T1224_SUPPLIER_DATA_TLV);
        remove(FZ54Tag.T1223_AGENT_DATA_TLV);
        remove(FZ54Tag.T1057_AGENT_FLAG);

        if (mMeasure == null) {
            if (mQtty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) > 0) {
                mMeasure = MeasureTypeE.KILOGRAM;
            }
            else {
                mMeasure = MeasureTypeE.PIECE;
            }
        }

        if(mAgentData.getType() != AgentTypeE.NONE) {
        	add(FZ54Tag.T1222_AGENT_FLAGS,mAgentData.getType().bVal);
        	add(mAgentData.packAgent());
        	add(mAgentData.packSupplier());
        }
        
        add(FZ54Tag.T1214_PAYMENT_TYPE, mItemPaymentType.bVal);
        add(FZ54Tag.T1212_ITEM_TYPE, mType.bVal);
        if (mItemPaymentType != ItemPaymentTypeE.AVANCE) {
            add(FZ54Tag.T1030_SUBJECT, mName);
        }
        else{
            remove(FZ54Tag.T1030_SUBJECT);
        }
        add(FZ54Tag.T1079_ONE_ITEM_PRICE,mPrice, 2);
        add(FZ54Tag.T1023_QUANTITY, mQtty, 3);
        add(FZ54Tag.T1199_VAT_ID, mVat.bVal);
        add(FZ54Tag.T1043_ITEM_PRICE, getSum(), 2);
        if (mVat != VatE.VAT_NONE && mVat != VatE.VAT_0) {
            add(FZ54Tag.T1200_ITEM_VAT, getVATValue(), 2);
        }
        return this;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p,flags);
        p.writeByte(mType.bVal);
        p.writeByte(mItemPaymentType.bVal);
        p.writeByte(mVat.bVal);
        p.writeString(mName);
        p.writeByte(mMeasure.bVal);
        p.writeString(mQtty.toString());
        p.writeString(mPrice.toString());
        p.writeString(mUUID);
        mAgentData.writeToParcel(p, flags);
        mMarkingCode.writeToParcel(p,flags);
        p.writeByte(mMarkResult.bVal);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        mType = SellItemTypeE.fromByte(p.readByte());
        mItemPaymentType = ItemPaymentTypeE.fromByte(p.readByte());
        mVat = VatE.fromByte(p.readByte());
        mName = p.readString();
        mMeasure = MeasureTypeE.fromByte(p.readByte());
        mQtty = new BigDecimal(p.readString());
        mPrice = new BigDecimal(p.readString());
        mUUID=p.readString();
        mAgentData.readFromParcel(p);
        mMarkingCode.readFromParcel(p);
        mMarkResult = new ItemCheckResult2106(p.readByte()); 
    }
    
    /**
     * Установить количество предмета расчета
     * @param val новое значение количества
     * @return текущий предмет расчета
     */
    public SellItem setQtty(BigDecimal val) {
    	mQtty = val;
    	return this;
    }

    @Override
    public SellItem attach(Object o) {
    	return (SellItem)super.attach(o);
    }
    
    public static final Creator<SellItem> CREATOR = new Creator<SellItem>() {
        @Override
        public SellItem createFromParcel(Parcel p) {
            SellItem result = new SellItem();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public SellItem[] newArray(int size) {
            return new SellItem[size];
        }
    };

    private static final String ITEM_NAME = "item.name";
    private static final String ITEM_QTTY = "item.qtty";
    private static final String ITEM_MEASURE = "item.measure";
    private static final String ITEM_PRICE = "item.price";
    private static final String ITEM_SUM = "item.sum";
    private static final String ITEM_VAT_NAME = "item.Vat.Name";
    private static final String ITEM_VAT_VALUE = "item.Vat.Value";
    private static final String ITEM_SALE_TYPE = "item.PaymentType";
    private static final String ITEM_TYPE = "item.ItemType";
    private static final String ITEM_AGENT_TYPE = "item.AgentType";
    private static final String ITEM_MARK_CODE = "item.MarkCode";
    private static final String ITEM_CHECK_CODE = "item.CheckCode";
    private static final String TAG_NAME = "T_";
    
    @Override
    public String onKey(String key) {
        switch (key) {
            case ITEM_MARK_CODE: {
            	if(mMarkingCode.getChilds().isEmpty())
            		return Const.EMPTY_STRING;
            	String s =  mMarkResult.getMarkTag();
            	if(mMarkResult.bVal != 0) 
            		s = "KM? "+s;
            	return s;
            }
            case ITEM_CHECK_CODE: 
            	return getTagString(2115);
            case ITEM_NAME:
                return getItemName();
            case ITEM_AGENT_TYPE:
                if (getAgentData().getType() == AgentTypeE.NONE) return Const.EMPTY_STRING;
                return getAgentData().getType().pName;
            case ITEM_QTTY:
                return String.format(Locale.ROOT, "%.3f", getQTTY());
            case ITEM_MEASURE:
                return getMeasure().pName;
            case ITEM_PRICE:
                return String.format(Locale.ROOT,"%.2f",getPrice());
            case ITEM_SUM:
                return String.format(Locale.ROOT,"%.2f",getSum());
            case ITEM_VAT_NAME:
                return getVATType().pName;
            case ITEM_VAT_VALUE:
                return String.format(Locale.ROOT,"%.2f", getVATValue());
            case ITEM_SALE_TYPE:
                return getPaymentType().pName;
            case ITEM_TYPE:
                return getType().pName;

            default:
                if (key.startsWith(TAG_NAME)) {
                    key = key.replace(TAG_NAME, "");
                    try {
                        String[] v = key.split("\\.");
                        Tag tag = getTag(Integer.parseInt(v[0]));

                        for (int i = 1; i < v.length; i++) {
                            if (tag == null) break;
                            tag = tag.getTag(Integer.parseInt(v[i]));
                        }

                        if (tag != null) return tag.asString();
                        return Const.EMPTY_STRING;

                    } catch (Exception e) {
                        return Const.EMPTY_STRING;
                    }
                } else {
                    return Const.EMPTY_STRING;
                }
        }
    }
    
    /**
     * Получить полное наименование предмета расчета по коду
     * @return полное наименование предмета расчета согласно ФФД
     */
    public String getItemName() {
        String s = getName();
        if (getType() == SellItem.SellItemTypeE.NON_SALES || getType() == SellItem.SellItemTypeE.ANOTHER_PAYMENTS) {
            if ("1".equals(s)) s = "Доход от долевого участия в других организациях";
            if ("2".equals(s))
                s = "Доход в виде курсовой разницы, образующейся вследствие отклонения курса продажи (покупки) иностранной валюты от официального курса";
            if ("3".equals(s))
                s = "Доход в виде подлежащих уплате должником штрафов, пеней и (или) иных санкций за нарушение договорных обязательств";
            if ("4".equals(s))
                s = "Доход от сдачи имущества (включая земельные участки) в аренду (субаренду)";
            if ("5".equals(s))
                s = "Доход от предоставления в пользование прав на результаты интеллектуальной деятельности";
            if ("6".equals(s))
                s = "Доход в виде процентов, полученных по договорам займа и другим долговым обязательствам";
            if ("7".equals(s)) s = "Доход в виде сумм восстановленных резервов";
            if ("8".equals(s))
                s = "Доход в виде безвозмездно полученного имущества (работ, услуг) или имущественных прав";
            if ("9".equals(s))
                s = "Доход в виде дохода, распределяемого в пользу налогоплательщика при его участии в простом товариществе";
            if ("10".equals(s))
                s = "Доход в виде дохода прошлых лет, выявленного в отчетном (налоговом) периоде";
            if ("11".equals(s)) s = "Доход в виде положительной курсовой разницы";
            if ("12".equals(s))
                s = "Доход в виде основных средств и нематериальных активов, безвозмездно полученных атомными станциями";
            if ("13".equals(s))
                s = "Доход в виде стоимости полученных материалов при ликвидации выводимых из эксплуатации основных средств";
            if ("14".equals(s))
                s = "Доход в виде использованных не по целевому назначению имущества, работ, услуг";
            if ("15".equals(s))
                s = "Доход в виде использованных не по целевому назначению средств, предназначенных для формирования резервов по обеспечению безопасности производств";
            if ("16".equals(s))
                s = "Доход в виде сумм, на которые уменьшен уставной (складочный) капитал (фонд) организации";
            if ("17".equals(s))
                s = "Доход в виде сумм возврата от некоммерческой организации ранее уплаченных взносов (вкладов)";
            if ("18".equals(s))
                s = "Доход в виде сумм кредиторской задолженности, списанной в связи с истечением срока исковой давности или по другим основаниям";
            if ("19".equals(s))
                s = "Доход в виде доходов, полученных от операций с производными финансовыми инструментами";
            if ("20".equals(s))
                s = "Доход в виде стоимости излишков материально-производственных запасов и прочего имущества, которые выявлены в результате инвентаризации";
            if ("21".equals(s))
                s = "Доход в виде стоимости продукции СМИ и книжной продукции, подлежащей замене при возврате либо при списании";
            if ("22".equals(s))
                s = "Доход в виде сумм корректировки прибыли налогоплательщика";
            if ("23".equals(s))
                s = "Доход в виде возвращенного денежного эквивалента недвижимого имущества и (или) ценных бумаг, переданных на пополнение целевого капитала некоммерческой организации";
            if ("24".equals(s))
                s = "Доход в виде разницы между суммой налоговых вычетов из сумм акциза и указанных сумм акциза";
            if ("25".equals(s))
                s = "Доход в виде прибыли контролируемой иностранной компании";
            if ("26".equals(s)) s = "Взносы на ОПС";
            if ("27".equals(s)) s = "Взносы на ОСС в связи с нетрудоспособностью";
            if ("28".equals(s)) s = "Взносы на ОМС";
            if ("29".equals(s)) s = "Взносы на ОСС от несчастных случаев";
            if ("30".equals(s)) s = "Пособия по временной нетрудоспособности";
            if ("31".equals(s)) s = "Платежи по добровольному личному страхованию";
        }
        return s;
    }

}
