package rs.fncore.data;

import android.os.Parcel;


import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import rs.fncore.Const;
import rs.utils.Utils;

/**
 * Базовый элемент тага - без сабтагов
 *
 * @author nick
 */
public abstract class BaseTag implements IReableFromParcel{

    private static final String TAG_ID = "i";
    private static final String TYPE_TAG = "t";
    private static final String SIZE_TAG = "s";
    private static final String DATA_TAG = "d";
    private static final int SHORT_SIZE = 2;
    public static final int ROOT_TAG=-1;

    protected int mTagId = ROOT_TAG;
    protected ByteBuffer mData = ByteBuffer.allocate(Const.MAX_TAG_SIZE);
    protected DataTypeE mTagType;

    /**
     * Типы данных, которые может хранить тег
     * @author nick
     *
     */
    public enum DataTypeE {
        B, I_1, I_2, I_4, TLV, D, S, F, F_2, R, U_4, U_2
    }

    protected byte[] pack(boolean printSize, boolean excludeNonFnTags) {
        ByteBuffer bb = ByteBuffer.allocate(32768);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        if (excludeNonFnTags && (mTagId >= 0 && mTagId<=Short.MAX_VALUE)) {
            bb.putShort((short) (mTagId & 0xFFFF));
        }

        int dataSize=mData.position();
        if (printSize) {
            bb.putShort((short) (dataSize & 0xFFFF));
        }
        bb.put(mData.array(), 0, dataSize);
        byte[] result = new byte[bb.position()];
        System.arraycopy(bb.array(), 0, result, 0, bb.position());
        return result;
    }

    protected void unpack(ByteBuffer bb, boolean haveSize) {
        mTagId = Utils.readUint16LE(bb);
        mData.clear();
        if (haveSize) {
            int size = bb.getShort();
            mData.put(bb.get(size));
        }
    }

    protected BaseTag() {
        mTagType = DataTypeE.TLV;
        mData.order(ByteOrder.LITTLE_ENDIAN);
        Arrays.fill(mData.array(), (byte)0);
    }

    protected BaseTag(Parcel p) {
        this();
        readFromParcel(p);
    }

    protected BaseTag(int tagId, byte value) {
        this();
        mTagType = DataTypeE.I_1;
        mTagId=tagId;
        mData.put(value);
    }

    protected BaseTag(int tagId, boolean value) {
        this();
        mTagType = DataTypeE.B;
        mTagId=tagId;
        mData.put((byte) (value ? 1 : 0));
    }

    protected BaseTag(int tagId, short value) {
        this();
        mTagType = DataTypeE.I_2;
        mTagId=tagId;
        mData.putShort(value);
    }

    protected BaseTag(int tagId, int value) {
        this();
        mTagType = DataTypeE.I_4;
        mTagId=tagId;
        mData.putInt(value);
    }

    protected BaseTag(int tagId, long value) {
        this();
        mTagType = DataTypeE.U_4;
        mTagId=tagId;
        mData.putInt((int)value);
    }

    protected BaseTag(int tagId, String value) {
        this();
        mTagType = DataTypeE.S;
        mTagId=tagId;
        mData.put(value.getBytes(Const.ENCODING));
    }

    protected BaseTag(int tagId, BigDecimal value, int digits) {
        this();
        if (digits == 2)
            mTagType = DataTypeE.F_2;
        else
            mTagType = DataTypeE.F;
        mTagId=tagId;
        long v = (Utils.round2(value, digits).multiply(BigDecimal.valueOf(Math.pow(10, digits)), MathContext.DECIMAL128)).intValue();
        if (digits > 2)
            mData.put((byte) (digits & 0xFF));
        if (v < 256)
            mData.put((byte) (v & 0xFF));
        else if (v < 65536)
            mData.putShort((short) (v & 0xFFFF));
        else
            mData.putInt((int) v);
    }

    protected BaseTag(int tagId, Date date) {
        this();
        mTagType = DataTypeE.D;
        mTagId=tagId;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        mData.put((byte) (cal.get(Calendar.YEAR) - 2000));
        mData.put((byte) cal.get(Calendar.MONTH));
        mData.put((byte) cal.get(Calendar.DAY_OF_MONTH));
        mData.put((byte) cal.get(Calendar.HOUR_OF_DAY));
        mData.put((byte) cal.get(Calendar.MINUTE));
    }

    protected BaseTag(int tagId, byte[] raw) {
    	this();
        mTagType = DataTypeE.R;
        mTagId=tagId;
        mData.put(raw);
    }

    protected BaseTag(int tagId, ByteBuffer bb) {
    	this();
        mTagType = DataTypeE.R;
        mTagId=tagId;
        int size=Utils.readUint16LE(bb);
        byte[] raw = new byte[size];
        bb.get(raw);
        
        mData.put(raw);
    }

    protected BaseTag(BaseTag source) {
    	this();
        mTagId=source.mTagId;
        mTagType = source.mTagType;
        mData.put(source.mData.array(), 0, source.mData.position());
    }

    protected BaseTag(JSONObject o) throws JSONException {
        this();
        mTagId=Integer.getInteger(o.getString(TAG_ID));
        mTagType = DataTypeE.valueOf(o.getString(TYPE_TAG));
        mData.put(Utils.hex2bytes(o.getString(DATA_TAG)));
    }

    /**
     * Сериализация в JSON объект
     * @return сериализованный объект
     * @throws JSONException 
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();

        result.put(TAG_ID, mTagId);
        result.put(TYPE_TAG, mTagType.name());
        result.put(SIZE_TAG, mData.position());
        result.put(DATA_TAG, Utils.dump(mData.array(), 0, mData.position()));

        return result;
    }

    /**
     * Значение тега как байт
     * @return байт
     */
    public byte asByte() {
        return mData.array()[0];
    }
    /**
     * Значение тега как double
     * @return double значение
     */
    public double asDouble() {
    	return asDouble(mTagType);
    }
    /**
     * Получить double значение из указанного типа
     * @param type тип хранения double значения
     * @return результат
     */
    public double asDouble(DataTypeE type) {
        double v = 100.0;
        int p = mData.position();
        mData.position(0);
        try {
            if (type == DataTypeE.F)
                v = Math.pow(10, mData.get());
            if (mData.remaining() == 1)
                return (mData.get() & 0xFF) / v;
            if (mData.position() == 2)
                return Utils.readUint16LE(mData) / v;
            return Utils.readUint32LE(mData) / v;
        } finally {
            mData.position(p);
        }
    }
    /**
     * Значение как short
     * @return short значение
     */
    public short asShort() {
        return (short) (((mData.array()[1] << 8) | mData.array()[0]) & 0xFFFF);
    }

    /**
     * Значение как unsigned short (расширяется до int)
     * @return значение расширенное до int
     */
    public int asUShort() {
        return (((mData.array()[1] & 0xFF << 8) | mData.array()[0] & 0xFF) & 0xFFFF);
    }

    /**
     * Значение как int
     * @return int значение
     */
    public int asInt() {
   		return (mData.array()[3] << 24) | (mData.array()[2] << 16) | (mData.array()[1] << 8) | mData.array()[0];
    }

    /**
     * Значение как unsigned int (расширяется до long)
     * @return значение расширенное до long
     */
    public long asUInt() {
    	if(mData.position() == 6)
    		return (((long) mData.array()[2] & 0xff) << 24) | (((long)mData.array()[3]& 0xff) << 16) | (((long)mData.array()[4]& 0xff) << 8) | ((long)mData.array()[5]& 0xff);
        return (((long) mData.array()[3] & 0xff) << 24) | ((mData.array()[2]& 0xff) << 16) | ((mData.array()[1]& 0xff) << 8) | (mData.array()[0]& 0xff);
    }

    /**
     * Значение как строка
     * @return строка
     */
    public String asString() {
        return new String(mData.array(), 0, mData.position(), Const.ENCODING);
    }

    /**
     * Размер данных тега
     * @return длина тела тега в байтах
     */
    public int size() {
        return mData.position() + SHORT_SIZE;
    }

    /**
     * Значение как массив байт
     * @return массив байт
     */
    public byte [] asRaw() {
    	byte [] result = new byte[mData.position()];
    	System.arraycopy(mData.array(), 0, result, 0, result.length);
    	return result;
    }
    /**
     * Значение как количество милиескунд
     * @return long значение
     */
    public long asTimeStamp() {
    	if(mData.position() == 4) return asUInt()*1000L;
        int p = mData.position();
        mData.position(0);
        try {
            return Utils.readDate5(mData);
        } finally {
            mData.position(p);
        }
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(mTagId);
        p.writeInt(mTagType.ordinal());

        byte [] data = new byte[mData.position()];
        p.writeInt(data.length);
        System.arraycopy(mData.array(), 0, data, 0, data.length);
        p.writeByteArray(data);
    }

    public void readFromParcel(Parcel p) {
        mTagId = p.readInt();
        mTagType = DataTypeE.values()[p.readInt()];

        mData.clear();
        int size = p.readInt();
        byte[] data = new byte[size];
        p.readByteArray(data);
        mData.put(data, 0, data.length);
    }

    /**
     * Значение как да/нет
     * @return значение как boolean
     */
    public boolean asBoolean() {
        return mData.array()[0] != 0;
    }

    @Override
    public String toString() {
        switch (mTagType) {
            case B:
                return asBoolean() ? "Да" : "Нет";
            case I_1:
                return String.valueOf(asByte());
            case I_2:
                return String.valueOf(asShort());
            case I_4:
                return String.valueOf(asInt());
            case U_2:
                return String.valueOf(asUShort());
            case U_4:
                return String.valueOf(asUInt());
            case F:
                return String.format(Locale.ROOT, "%.3f", asDouble());
            case F_2:
                return String.format(Locale.ROOT,"%.2f", asDouble());
            case S:
                return asString();
            case D:
                return Utils.formatDate(asTimeStamp());
            case TLV:
            case R:
                return Utils.dump(mData.array(), 0, mData.position());
        }
        return super.toString();
    }
}
