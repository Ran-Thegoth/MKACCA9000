package rs.fncore.data;


import android.os.Parcel;

import java.security.InvalidParameterException;

import rs.fncore.Const;
import rs.fncore.FZ54Tag;

/**
 * Код маркировки
 *
 * @author amv
 */
public class MarkingCode extends Document {

    public static final String CLASS_NAME="MarkingCode";
    public static final String CLASS_UUID="72c6d356-ebaf-11eb-9a03-0242ac130003";

    /**
     * Тип кода маркировки тег 2100
     *
     * @author amv
     */
    public enum CodeTypesE {
        UNKNOWN(0, false),
        SHORT(1, false),
        RESP_88(2, true),
        RESP_44_NO_FN_CHECK(3, false),
        RESP_44_FN_CHECK(4, true),
        RESP_4_NO_FN_CHECK(5, false),
        ;

        CodeTypesE(int bVal, boolean needGSOffset) {
            this.bVal = (byte)bVal;
            this.needGSOffset=needGSOffset;
        }

        public final byte bVal;
        public final boolean needGSOffset;

        public static CodeTypesE fromByte(byte bVal) {
            for (CodeTypesE v : values())
                if (v.bVal == bVal) return v;
            return UNKNOWN;
        }
    }

    /**
     * Результат проверки кода маркировки, тег 2106
     *
     * @author amv
     */
    public static class ItemCheckResult2106 {
        private static final int CODE_PROCESSED_MASK =(1<<0);
        private static final int CODE_CHECKED_MASK =(1<<1);
        private static final int CODE_OISM_PROCESSED_MASK =(1<<2);
        private static final int CODE_OISM_CHECKED_MASK =(1<<3);
        private static final int AUTONOMOUS_MASK =(1<<4);

        public final boolean codeProcessed;
        public final boolean codeChecked;
        public final boolean codeOISMProcessed;
        public final boolean codeOISMChecked;
        public final boolean autonomousMode;

        public final byte bVal;

        public ItemCheckResult2106(byte val){
            bVal=val;
            codeProcessed = (val& CODE_PROCESSED_MASK)== CODE_PROCESSED_MASK;
            codeChecked = (val& CODE_CHECKED_MASK)== CODE_CHECKED_MASK;
            codeOISMProcessed = (val& CODE_OISM_PROCESSED_MASK)== CODE_OISM_PROCESSED_MASK;
            codeOISMChecked = (val& CODE_OISM_CHECKED_MASK)== CODE_OISM_CHECKED_MASK;
            autonomousMode = (val& AUTONOMOUS_MASK)== AUTONOMOUS_MASK;
        }

        public String getMarkTag(){
        	if(isPositiveChecked()) {
        		if(!codeOISMChecked)
        			return "[M]";
        		return "[M+]";
        	}
        	else if (!codeChecked){
                return "[M-]";
            } else 
                return "[M]";
        }

        public boolean isPositiveChecked(){
            return codeChecked;
        }
    }

    /**
     * Планируемый статус товара тег 2003, 2110
     *
     * @author amv
     */
    public enum PlannedItemStatusE {
        UNKNOWN(0),
        PIECE_ITEM_SELL(1),
        MEASURED_ITEM_SELL(2),
        PIECE_ITEM_RETURNED(3),
        MEASURED_ITEM_RETURNED(4),
        ITEM_NOT_CHANGED(255),
        ;

        PlannedItemStatusE(int bVal) {
            this.bVal = (byte)bVal;
        }

        public final byte bVal;
        public static PlannedItemStatusE fromByte(byte bVal) {
            for (PlannedItemStatusE v : values())
                if (v.bVal == bVal) return v;
            return UNKNOWN;
        }
    }

    protected ItemCheckResult2106 mMarkingCheckResult = new ItemCheckResult2106((byte)0);

    public MarkingCode(){}

    public MarkingCode(String code, PlannedItemStatusE itemStatus) throws InvalidParameterException{
        setCode(code);
        setPlannedItemStatus(itemStatus);
        add(FZ54Tag.T2102_MARKING_CODE_REGIME, (byte)0);
    }

    /**
     * Признак наличия кода маркировки
     *
     * @return признак наличия кода маркировки
     */
    public boolean isEmpty(){
        return getCode() == null || getCode().isEmpty();
    }

    /**
     * установить код маркировки
     *
     * @param code код маркировки
     */
    public void setCode(String code){
        add(FZ54Tag.T2000_MARKING_CODE,code);
    }

    /**
     * @return получить код маркировки
     */
    public String getCode(){
        return getTagString(FZ54Tag.T2000_MARKING_CODE);
    }

    /**
     * @return получить планируемый статус товара
     */
    public PlannedItemStatusE getPlannedItemStatus() {
        Tag data = getTag(FZ54Tag.T2003_PLANNED_ITEM_STATE);
        if (data == null) throw new InvalidParameterException("empty data");
        return PlannedItemStatusE.fromByte(data.asByte());
    }

    /**
     * @param itemStatus установить планируемый статус товара
     */
    public void setPlannedItemStatus(PlannedItemStatusE itemStatus) {
        add(FZ54Tag.T2003_PLANNED_ITEM_STATE, itemStatus.bVal);
        add(FZ54Tag.T2110_ASSIGNED_ITEM_STATUS, itemStatus.bVal);
    }

    /**
     * @return получить результат проверки маркировки
     */
    public ItemCheckResult2106 getCheckResult() {
        return mMarkingCheckResult;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        p.writeByte(mMarkingCheckResult.bVal);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        mMarkingCheckResult=new ItemCheckResult2106(p.readByte());
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassUUID() {
        return CLASS_UUID;
    }

    public static final Creator<MarkingCode> CREATOR = new Creator<MarkingCode>() {
        @Override
        public MarkingCode createFromParcel(Parcel p) {
            MarkingCode result = new MarkingCode();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public MarkingCode[] newArray(int size) {
            return new MarkingCode[size];
        }
    };

	@Override
	public String onKey(String key) {
		return Const.EMPTY_STRING;
	}
}
