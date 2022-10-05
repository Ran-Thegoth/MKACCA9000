package rs.fncore.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;
import rs.fncore.FZ54Tag;
import rs.utils.Utils;
/**
 * Счетчики смены/ФН
 * @author nick
 *
 */
public class FNCounters extends Document {
    public static final String CLASS_NAME="FNCounters";
    public static final String CLASS_UUID="ca5e362e-0d5e-11ec-82a8-0242ac130003";


    protected boolean mIsTotalCounters;
    protected long mOperationsCountTotal;

    protected OperationInfo mIncomeCounters = new OperationInfo();
    protected OperationInfo mReturnIncomeCounters  = new OperationInfo();
    protected OperationInfo mOutcomeCounters  = new OperationInfo();
    protected OperationInfo mReturnOutcomeCounters  = new OperationInfo();
    protected CorrectionOperationInfo mCorrectionCounters = new CorrectionOperationInfo();

    public FNCounters() { 
    	add(FZ54Tag.T1000_DOCUMENT_NAME,100);
    }
    public FNCounters(boolean isTotal) {
    	this();
    	mIsTotalCounters = isTotal;
    }
    public FNCounters(ByteBuffer bb, boolean isTotal){
        this(isTotal);
        Utils.readUint16LE(bb);
        mOperationsCountTotal = Utils.readUint32LE(bb);
        mIncomeCounters = new OperationInfo(bb);
        mReturnIncomeCounters = new OperationInfo(bb);
        mOutcomeCounters = new OperationInfo(bb);
        mReturnOutcomeCounters = new OperationInfo(bb);
        mCorrectionCounters = new CorrectionOperationInfo(bb);
    }


    /**
     * Счетчики по типам операций
     * @author nick
     *
     */
    public static class OperationInfo{
        public long count;
        public long totalSum;
        public long totalSumCash;
        public long totalSumCard;
        public long totalSumPrepayment;
        public long totalSumCredit;
        public long totalSumAhead;
        public long totalSumVat_20;
        public long totalSumVat_10;
        public long totalSumVat_0;
        public long totalSumVat_none;
        public long totalSumVat_20_120;
        public long totalSumVat_10_110;

        public OperationInfo() {} 
        public OperationInfo(ByteBuffer bb){
            count=Utils.readUint32LE(bb);
            totalSum=Utils.readUint48LE(bb);
            totalSumCash=Utils.readUint48LE(bb);
            totalSumCard=Utils.readUint48LE(bb);
            totalSumPrepayment=Utils.readUint48LE(bb);
            totalSumCredit=Utils.readUint48LE(bb);
            totalSumAhead=Utils.readUint48LE(bb);
            totalSumVat_20=Utils.readUint48LE(bb);
            totalSumVat_10=Utils.readUint48LE(bb);
            totalSumVat_0=Utils.readUint48LE(bb);
            totalSumVat_none=Utils.readUint48LE(bb);
            totalSumVat_20_120=Utils.readUint48LE(bb);
            totalSumVat_10_110=Utils.readUint48LE(bb);
            
        }

        public void parseTag(Tag t) { 
        	t.unpackSTLV();
        	count = t.UIntValue(FZ54Tag.T1135_BILLS_COUNT);
        	totalSum = t.UIntValue(FZ54Tag.T1201_OVERALL_FN_SUM);
        	totalSumCard = t.UIntValue(FZ54Tag.T1138_TOTAL_CARD_SUM);
        	totalSumCash = t.UIntValue(FZ54Tag.T1136_TOTAL_CASH_SUM);
        	totalSumPrepayment = t.UIntValue(FZ54Tag.T1218_TOTAL_PREPAY_SUM);
        	totalSumCredit = t.UIntValue(FZ54Tag.T1219_TOTAL_POSTPAY_SUM);
        	totalSumAhead = t.UIntValue(FZ54Tag.T1220_TOTAL_OTHER_SUM);
        	totalSumVat_20 = t.UIntValue(FZ54Tag.T1139_TOTAL_VAT_20_SUM);
        	totalSumVat_0 = t.UIntValue(FZ54Tag.T1143_TOTAL_VAT_0_SUM);
        	totalSumVat_none = t.UIntValue(FZ54Tag.T1183_TOTAL_NO_VAT_SUM);
        	totalSumVat_10 = t.UIntValue(FZ54Tag.T1140_TOTAL_VAT_10_SUM);
        	totalSumVat_10_110 = t.UIntValue(FZ54Tag.T1142_TOTAL_VAT_10_110_SUM);
        	totalSumVat_20_120 = t.UIntValue(FZ54Tag.T1141_TOTAL_VAT_20_120_SUM);
        }
        public void writeToParcel(Parcel p) {
        	p.writeLong(count);
        	p.writeLong(totalSum);
        	p.writeLong(totalSumCard);
        	p.writeLong(totalSumCash);
        	p.writeLong(totalSumPrepayment);
        	p.writeLong(totalSumCredit);
        	p.writeLong(totalSumAhead);
        	p.writeLong(totalSumVat_20);
        	p.writeLong(totalSumVat_0);
        	p.writeLong(totalSumVat_none);
        	p.writeLong(totalSumVat_10);
        	p.writeLong(totalSumVat_10_110);
        	p.writeLong(totalSumVat_20_120);
        	
        }
        public void readFromParcel(Parcel p) {
        	count = p.readLong();
        	totalSum = p.readLong();
        	totalSumCard = p.readLong();
        	totalSumCash = p.readLong();
        	totalSumPrepayment = p.readLong();
        	totalSumCredit = p.readLong();
        	totalSumAhead = p.readLong();
        	totalSumVat_20 = p.readLong();
        	totalSumVat_0 = p.readLong();
        	totalSumVat_none = p.readLong();
        	totalSumVat_10 = p.readLong();
        	totalSumVat_10_110 = p.readLong();
        	totalSumVat_20_120 = p.readLong();
        	
        }
        public String getVal(String key) {
            try {
                long data=this.getClass().getField(key).getLong(this);
                if (key.startsWith("count")){
                    return String.valueOf(data);
                }
                else{
                    BigDecimal value=new BigDecimal(data);
                    value=value.divide(new BigDecimal(100.0), MathContext.DECIMAL128);
                    return String.format(Locale.ROOT,"%.2f",value);
                }
            } catch (IllegalAccessException e) {
            } catch (NoSuchFieldException e) {
            }
            return "error";
        }
    }

    /**
     * Счетчики коррекций
     * @author nick
     *
     */
    public static class CorrectionOperationInfo {
        public long count;
        public long countIncome;
        public long incomeTotalSum;
        public long countReturnIncome;
        public long returnIncomeTotalSum;
        public long countOutcome;
        public long outcomeTotalSum;
        public long countReturnOutcome;
        public long returnOutcomeTotalSum;

        public CorrectionOperationInfo() {} 
        public CorrectionOperationInfo(ByteBuffer bb){
            count=Utils.readUint32LE(bb);
            countIncome =Utils.readUint32LE(bb);
            incomeTotalSum=Utils.readUint48LE(bb);
            countReturnIncome =Utils.readUint32LE(bb);
            returnIncomeTotalSum=Utils.readUint48LE(bb);
            countOutcome =Utils.readUint32LE(bb);
            outcomeTotalSum=Utils.readUint48LE(bb);
            countReturnOutcome =Utils.readUint32LE(bb);
            returnOutcomeTotalSum=Utils.readUint48LE(bb);
        }

        public void writeToParcel(Parcel p) {
        	p.writeLong(count);
        	p.writeLong(countIncome);
        	p.writeLong(incomeTotalSum);
        	p.writeLong(countReturnIncome);
        	p.writeLong(returnIncomeTotalSum);
        	p.writeLong(countOutcome);
        	p.writeLong(outcomeTotalSum);
        	p.writeLong(countReturnOutcome);
        	p.writeLong(returnOutcomeTotalSum);
        }
        public void readFromParcel(Parcel p) {
        	count = p.readLong();
        	countIncome = p.readLong();
        	incomeTotalSum = p.readLong();
        	countReturnIncome = p.readLong();
        	returnIncomeTotalSum = p.readLong();
        	countOutcome = p.readLong();
        	outcomeTotalSum = p.readLong();
        	countReturnOutcome = p.readLong();
        	returnOutcomeTotalSum = p.readLong();
        	
        }
        public void parseTag(Tag t) {
        	t.unpackSTLV();
        	for(Tag tag : t.getChilds()) {
        		switch(tag.getId()) {
        		case FZ54Tag.T1144_CORRECTION_COUNT:
        			count = tag.asUInt();
        			break;
        		case FZ54Tag.T1145_CORRECTION_INCOME_SUM_TLV:
        			tag.unpackSTLV();
        			countIncome = tag.UIntValue(FZ54Tag.T1135_BILLS_COUNT);
        			incomeTotalSum = tag.UIntValue(FZ54Tag.T1201_OVERALL_FN_SUM);
        			break;
        		case FZ54Tag.T1146_CORRECTION_OUTCOME_SUM_TLV:
        			tag.unpackSTLV();
        			countOutcome = tag.UIntValue(FZ54Tag.T1135_BILLS_COUNT);
        			outcomeTotalSum = tag.UIntValue(FZ54Tag.T1201_OVERALL_FN_SUM);
        			break;
        		case FZ54Tag.T1232_CORRECTION_RETURN_INCOME:
        			tag.unpackSTLV();
        			countReturnIncome = tag.UIntValue(FZ54Tag.T1135_BILLS_COUNT);
        			returnIncomeTotalSum = tag.UIntValue(FZ54Tag.T1201_OVERALL_FN_SUM);
        			break;
        		case FZ54Tag.T1233_CORRECTION_RETURN_OUTCOME:
        			tag.unpackSTLV();
        			countReturnOutcome = tag.UIntValue(FZ54Tag.T1135_BILLS_COUNT);
        			returnOutcomeTotalSum = tag.UIntValue(FZ54Tag.T1201_OVERALL_FN_SUM);
        			break;
        		}
        	}
        }
        public String getVal(String key) {
            try {
                long data=this.getClass().getField(key).getLong(this);
                if (key.startsWith("count")){
                    return String.valueOf(data);
                }
                else{
                    BigDecimal value=new BigDecimal(data);
                    value=value.divide(new BigDecimal(100.0), MathContext.DECIMAL128);
                    return String.format(Locale.ROOT, "%.2f",value);
                }
            } catch (IllegalAccessException e) {
            } catch (NoSuchFieldException e) {
            }
            return "error";
        }
    }


    protected static final String IS_TOTAL_COUTNERS = "is.Total.Counters";
    protected static final String TOTAL_BILLS = "total.Bills";

    protected static final String INCOME_TAG = "Income.";
    protected static final String OUTCOME_TAG = "Outcome.";
    protected static final String RETURN_INCOME_TAG = "ReturnIncome.";
    protected static final String RETURN_OUTCOME_TAG = "ReturnOutcome.";
    protected static final String CORRECTION_TAG = "Correction.";

    @Override
    public String onKey(String key) {
        switch (key){
            case IS_TOTAL_COUTNERS:
                return Document.bv(mIsTotalCounters);
            case TOTAL_BILLS: return String.valueOf(mOperationsCountTotal);

            default:
                if (key.startsWith(INCOME_TAG)) {
                    key = key.replace(INCOME_TAG, "");
                    return mIncomeCounters.getVal(key);
                } else if (key.startsWith(OUTCOME_TAG)) {
                    key = key.replace(OUTCOME_TAG, "");
                    return mOutcomeCounters.getVal(key);

                }
                else if (key.startsWith(RETURN_INCOME_TAG)) {
                    key = key.replace(RETURN_INCOME_TAG, "");
                    return mReturnIncomeCounters.getVal(key);
                }
                else if (key.startsWith(RETURN_OUTCOME_TAG)) {
                    key = key.replace(RETURN_OUTCOME_TAG, "");
                    return mReturnOutcomeCounters.getVal(key);
                }
                else if (key.startsWith(CORRECTION_TAG)) {
                    key = key.replace(CORRECTION_TAG, "");
                    return mCorrectionCounters.getVal(key);
                }
            	return null;

        }
    }

    protected boolean parseTag(Tag t) {
    	t.unpackSTLV();
    	for(Tag tag : t.getChilds()) {
    		switch(tag.getId()) {
    		case FZ54Tag.T1134_TOTAL_BILLS_AND_BSO:
    			mOperationsCountTotal = tag.asUInt();
    			break;
    		case FZ54Tag.T1133_CORRECTION_COUNTERS_TLV:
    			mCorrectionCounters.parseTag(tag);
    			break;
    		case FZ54Tag.T1129_INCOME_COUNTERS_TLV:
    			mIncomeCounters.parseTag(tag);
    			break;
    		case FZ54Tag.T1130_RETURN_INCOME_COUNTERS_TLV:
    			mReturnIncomeCounters.parseTag(tag);
    			break;
    		case FZ54Tag.T1131_OUTCOME_COUNTERS_TLV:
    			mOutcomeCounters.parseTag(tag);
    			break;
    		case FZ54Tag.T1132_RETURN_OUTCOME_COUNTERS_TLV:
    			mReturnOutcomeCounters.parseTag(tag);
    			break;
    		}
    	}
    	return true;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
    	super.writeToParcel(p, flags);
    	p.writeInt(mIsTotalCounters ? 1 :0);
    	p.writeLong(mOperationsCountTotal);
    	mIncomeCounters.writeToParcel(p);
    	mOutcomeCounters.writeToParcel(p);
    	mReturnIncomeCounters.writeToParcel(p);
    	mReturnOutcomeCounters.writeToParcel(p);
    	mCorrectionCounters.writeToParcel(p);
    	
    }
    @Override
    public void readFromParcel(Parcel p) {
    	super.readFromParcel(p);
    	mIsTotalCounters = p.readInt() != 0;
    	mOperationsCountTotal = p.readLong();
    	mIncomeCounters.readFromParcel(p);
    	mOutcomeCounters.readFromParcel(p);
    	mReturnIncomeCounters.readFromParcel(p);
    	mReturnOutcomeCounters.readFromParcel(p);
    	mCorrectionCounters.readFromParcel(p);
    }
    
    public static final Parcelable.Creator<FNCounters> CREATOR = new Parcelable.Creator<FNCounters>() {
        @Override
        public FNCounters createFromParcel(Parcel p) {
            return null;
        }

        @Override
        public FNCounters[] newArray(int size) {
            return new FNCounters[size];
        }
    };

	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public String getClassName() {
		return CLASS_NAME;
	}
	@Override
	public String getClassUUID() {
		return CLASS_UUID;
	}
	/**
	 * Счетчик операций по всем типам
	 * @return количество операций
	 */
	public long getTotalOperationCounter() { return mOperationsCountTotal; }
	/**
	 * Счетчики по типу операции "Приход"
	 * @return объект типа OperationInfo с данными по операции
	 */
	public OperationInfo Income() { return mIncomeCounters; }
	/**
	 * Счетчики по типу операции "Возврат прихода"
	 * @return объект типа OperationInfo с данными по операции
	 */
    public OperationInfo ReturnIncome() { return mReturnIncomeCounters; }
    /**
     * Счетчики по типу операции "Расход"
     * @return объект типа OperationInfo с данными по операции
     */
    public OperationInfo Outcome() { return mOutcomeCounters; }
    /**
     * Счетчики по типу операции "возврат расхода"
     * @return объект типа OperationInfo с данными по операции
     */
    public OperationInfo ReturnOutcome() { return mReturnOutcomeCounters; }
    /**
     * Счетчики операций коррекции
     * @return объект типа CorrectionOperationInfo с данными по операции
     */
    public CorrectionOperationInfo Corrections() { return mCorrectionCounters; }
 

}
