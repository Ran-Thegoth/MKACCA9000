package rs.fncore.data;

import android.os.Parcel;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.utils.Utils;

/**
 * Фискальный документ
 *
 * @author nick
 */
public abstract class Document extends Tag implements TemplateProcessor  {

	private static final String [] BOOL_VAL = {"Да","Нет"}; 
    public static String bv(boolean v) {
        return BOOL_VAL[v ? 0 : 1];
    }
	
    public static class FnWarnings {
        public static final int MASK_REPLACE_FN_REMAIN_3_DAYS=(1<<0);
        public static final int MASK_FN_REMAIN_30_DAYS =(1<<1);
        public static final int MASK_FN_MEMORY_FILL_99=(1<<2);
        public static final int MASK_OFD_TIMEOUT_OVERLOAD=(1<<3);
        public static final int MASK_FAILURE_FORMAT=(1<<4);
        public static final int MASK_NEED_KKT_SETUP=(1<<5);
        public static final int MASK_OFD_CANCELED=(1<<6);
        public static final int MASK_CRITICAL_FN_ERROR=(1<<7);

        public int iVal;

        public FnWarnings(byte val) {
            this.iVal = val&0xFF;
        }

        public FnWarnings(int val) {
            this.iVal = val;
        }

        public boolean isReplaceUrgent3Days(){
            return (iVal &MASK_REPLACE_FN_REMAIN_3_DAYS)==MASK_REPLACE_FN_REMAIN_3_DAYS;
        }

        public boolean isReplace30Days(){
            return (iVal & MASK_FN_REMAIN_30_DAYS)== MASK_FN_REMAIN_30_DAYS;
        }

        public boolean isMemoryFull99(){
            return (iVal &MASK_FN_MEMORY_FILL_99)==MASK_FN_MEMORY_FILL_99;
        }

        public boolean isOFDTimeout(){
            return (iVal &MASK_OFD_TIMEOUT_OVERLOAD)==MASK_OFD_TIMEOUT_OVERLOAD;
        }

        public boolean isFailureFormat(){
            return (iVal &MASK_FAILURE_FORMAT)==MASK_FAILURE_FORMAT;
        }

        public boolean isNeedKKTSetup(){
            return (iVal &MASK_NEED_KKT_SETUP)==MASK_NEED_KKT_SETUP;
        }

        public boolean isOFDCanceled(){
            return (iVal &MASK_OFD_CANCELED)==MASK_OFD_CANCELED;
        }

        public boolean isFNCriticalError(){
            return (iVal &MASK_CRITICAL_FN_ERROR)==MASK_CRITICAL_FN_ERROR;
        }
    }
    
	
    private static final String CLASS_NAME_TAG = "Class";
    private static final String LOCATION_TAG = "Location";
    protected static final int DDL_VERSION = 300;
    protected FnWarnings mFnWarnings = new FnWarnings((byte)0);

    /**
     * Несовпадение версий документа
     *
     * @author nick
     */
    public class DDLException extends RuntimeException {

        private static final long serialVersionUID = 5030735810889733060L;

        public DDLException(int v) {
            super("Document version mismatch. Got " + v + " await " + DDL_VERSION);
        }
    }

    protected int mDDL = DDL_VERSION;
    protected Signature mSignature = new Signature(this);
    protected Location mLocation = new Location();

    public Document(JSONObject json) throws JSONException {
        super(json);

        if (json.has(LOCATION_TAG)) {
            mLocation = new Location(json.getJSONObject(LOCATION_TAG));
        }
    }

    public Document() {
    }

    /**
     * Получить теги документа в виде байт для записи с помощью команды 07
     * Длина каждого блока не более 1024 байт
     *
     * @return tags
     */
    public byte[][] packToFN() {
        List<byte[]> result = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.allocate(Const.MAX_TAG_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (Tag tag: getChilds()){
        	if(tag.getId() == FZ54Tag.T1000_DOCUMENT_NAME || 
        			tag.getId() == FZ54Tag.T1012_DATE_TIME || 
        			tag.getId() == FZ54Tag.T1040_DOCUMENT_NO || 
        			tag.getId() == FZ54Tag.T1068_OPERATOR_MESSAGE_TLV || 
        			tag.getId() == FZ54Tag.T1077_DOCUMENT_FISCAL_SIGN || 
        			tag.getId() == FZ54Tag.T1078_OPERATOR_FISCAL_SIGN) continue;
        	if(tag.getId() > 3000) continue;
            if (buffer.position() + tag.size() >= buffer.capacity()) {
                byte[] b = new byte[buffer.position()];
                System.arraycopy(buffer.array(), 0, b, 0, b.length);
                result.add(b);
                buffer.clear();
            }
            buffer.put(tag.pack(true));
        }

        if (buffer.position() > 0) {
            byte[] b = new byte[buffer.position()];
            System.arraycopy(buffer.array(), 0, b, 0, b.length);
            result.add(b);
        }
        return result.toArray(new byte[result.size()][]);
    }

    protected boolean parseTag(Tag t) {
    	switch(t.getId()) {
    	case FZ54Tag.T1050_FN_EXPIRE_FLAG:
    		if(t.asBoolean())
    			mFnWarnings.iVal |= FnWarnings.MASK_FN_REMAIN_30_DAYS;
    		break;
    	case FZ54Tag.T1051_FN_REPLACE_FLAG:
    		if(t.asBoolean())
    			mFnWarnings.iVal |= FnWarnings.MASK_REPLACE_FN_REMAIN_3_DAYS;
    		break;
    	case FZ54Tag.T1052_FN_OVERFLOW_FLAG:	
    		if(t.asBoolean())
    			mFnWarnings.iVal |= FnWarnings.MASK_FN_MEMORY_FILL_99;
    		break;
    	case FZ54Tag.T1053_OFD_TIMEOUT_FLAG:	
    		if(t.asBoolean())
    			mFnWarnings.iVal |= FnWarnings.MASK_OFD_TIMEOUT_OVERLOAD;
    		break;
    	case FZ54Tag.T1206_OPERATOR_MESSAGE_FLAGS:
    		mFnWarnings.iVal = t.asByte();
    		break;
    	}
    	return mSignature.parseTag(t);
    }
    
    public void fromTag(Tag tag) {
    	getChilds().clear();
    	if(mSignature.mSigner == null) {
    		Log.d("fncore2", "Building signer");
    		mSignature.mSigner = new Signer();
    	}
    	Log.d("fncore2", "Restoring");
    	for(Tag t : tag.getChilds()) {
    		if(!parseTag(t)) 
    			getChilds().add(t);
    	}
    }
    
    public StringBuilder printTags(){
        StringBuilder res = new StringBuilder();
        for (Tag tag: getChilds()){
            res.append("tagId: ").append(tag.mTagId).append(",data: ").append(tag.toString()).append(System.getProperty("line.separator"));
        }
        return res;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        p.writeInt(mDDL);
        p.writeInt(mFnWarnings.iVal);
        mLocation.writeToParcel(p, flags);
        if (mSignature != null) {
            p.writeInt(1);
            mSignature.writeToParcel(p, flags);
        } else
            p.writeInt(0);

    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        mDDL = p.readInt();
        mFnWarnings = new FnWarnings(p.readInt());
        mLocation.readFromParcel(p);
        if (p.readInt() != 0) {
            mSignature = new Signature(this);
            mSignature.readFromParcel(p);
        }
    }

    /**
     * Получить адрес расчетов
     *
     * @return location
     */
    public Location getLocation() {
        return mLocation;
    }

    /**
     * Документ сохранен в ФН?
     *
     * @return check signature
     */
    public boolean isSigned() {
        return mSignature.mFPD != 0;
    }

    /**
     * Получить фискальную подпись документа
     *
     * @return signature
     */
    public Signature signature() {
        return mSignature;
    }

    protected void sign(ByteBuffer bb, Signer signer, OU operator, long signTime) {
        mSignature = new Signature(this, signer, signTime);
        operator.cloneTo(mSignature.operator());
        mSignature.mFdNumber = bb.getInt();
        mSignature.mFPD = Utils.readUint32LE(bb);
    }

    /**
     * Получить имя класса документа
     *
     * @return class name
     */
    public abstract String getClassName();

    /**
     * Получить UUID класса документа
     *
     * @return uuid of class
     */
    public abstract String getClassUUID();

    /**
     * Сохранение документа в JSON объект
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject result = super.toJSON();

        result.put(CLASS_NAME_TAG, getClassName());
        result.put(LOCATION_TAG, mLocation.toJSON());

        return result;
    }

    /**
     * Информация о предупреждениях ФН
     *
     * @return Информация о предупреждениях ФН
     */
    public FnWarnings getFNWarnings() {
        return mFnWarnings;
    }
    
    private static final String FN_LESS_3_DAY = "warning.3days";
    private static final String FN_FULL = "warning.full";
    private static final String FN_LESS_30_DAYS = "warning.30days";

    private static final String AUTOMATE_NUMBER = "automateNumber";
    private static final String SENDER_EMAIL = "sender_email";
    private static final String FNS_URL = "fns_url";

    @Override
    public String onKey(String key) {
    	switch(key) {
        case FN_LESS_3_DAY:
            return bv(getFNWarnings().isReplaceUrgent3Days());
        case FN_LESS_30_DAYS:
            return bv(getFNWarnings().isReplace30Days());
        case FN_FULL:
            return bv(getFNWarnings().isMemoryFull99());
    	
        case AUTOMATE_NUMBER:
        	return getTagString(FZ54Tag.T1036_AUTOMAT_NO);
        case SENDER_EMAIL:	
        	return getTagString(FZ54Tag.T1117_SENDER_EMAIL);
        case FNS_URL:			
        	return getTagString(FZ54Tag.T1060_FNS_URL);
    	}
    	return mSignature.onKey(key);
    }
    
}
