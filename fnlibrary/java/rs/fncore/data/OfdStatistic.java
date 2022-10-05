package rs.fncore.data;


import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;

import rs.fncore.FZ54Tag;
import rs.utils.Utils;

/**
 * Данные о документах к отправке в ОФД
 *
 * @author nick
 */
public class OfdStatistic implements IReableFromParcel {
    private boolean mInProgress;
    private int mExchangeStatus;
    private int mUnsentCount;
    private long mFirstUnsentNumber;
    private long mFirstUnsentDate;

    public OfdStatistic() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Внутренний метод
     *
     * @param bb буффер с данными
     */
    public void update(ByteBuffer bb) {
        mExchangeStatus = bb.get();
        mInProgress = (bb.get() & 0xFF) != 0;
        mUnsentCount = Utils.readUint16LE(bb);
        mFirstUnsentNumber = Utils.readUint32LE(bb);
        mFirstUnsentDate = Utils.readDate5(bb);
    }

    /**
     * Признак установленного транспортного режима
     *
     * @return Признак установленного транспортного режима
     */
    public boolean isInProgress() {
        return mInProgress;
    }

    /**
     * Имеются ли неотправленные документы
     *
     * @return Имеются ли неотправленные документы
     */
    public boolean haveUnsentDocuments() {
        return mUnsentCount > 0;
    }

    /**
     * Количество неотправленных документов
     *
     * @return Количество неотправленных документов
     */
    public int getUnsentDocumentCount() {
        return mUnsentCount;
    }

    /**
     * Номер первого неотправленного документа
     *
     * @return Номер первого неотправленного документа
     */
    public long getFirstUnsentNumber() {
        return mFirstUnsentNumber;
    }

    /**
     * Дата первого неотправленного документа
     *
     * @return Дата первого неотправленного документа
     */
    public long getFirstUnsentDate() {
        return mFirstUnsentDate;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(mExchangeStatus);
        p.writeInt(mInProgress ? 1 : 0);
        p.writeInt(mUnsentCount);
        p.writeLong(mFirstUnsentNumber);
        p.writeLong(mFirstUnsentDate);
    }

    public void readFromParcel(Parcel p) {
        mExchangeStatus = p.readInt();
        mInProgress = p.readInt() != 0;
        mUnsentCount = p.readInt();
        mFirstUnsentNumber = p.readLong();
        mFirstUnsentDate = p.readLong();
    }
    
    public void parseTag(Tag t) {
    	switch(t.getId()) {
    	case FZ54Tag.T1097_OFD_UNSENT_COUNT:
    		mUnsentCount = t.asInt();
    		break;
    	case FZ54Tag.T1098_OFD_UNSENT_DATE:
    		mFirstUnsentDate = t.asTimeStamp();
    		break;
    	case FZ54Tag.T1116_OFD_UNSENT_NO:
    		mFirstUnsentNumber = t.asUInt();
    		break;
    	}
    }

    public static final Parcelable.Creator<OfdStatistic> CREATOR = new Parcelable.Creator<OfdStatistic>() {

        @Override
        public OfdStatistic createFromParcel(Parcel p) {
            OfdStatistic result = new OfdStatistic();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public OfdStatistic[] newArray(int size) {
            return new OfdStatistic[size];
        }

    };

}
