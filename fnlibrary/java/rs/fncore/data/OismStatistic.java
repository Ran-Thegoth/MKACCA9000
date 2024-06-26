package rs.fncore.data;


import android.os.Parcel;

import java.nio.ByteBuffer;

import rs.utils.Utils;

/**
 * Данные о документах к отправке в ОИСМ
 *
 * @author amv
 */
public class OismStatistic implements IReableFromParcel {
    private ExchangeStatusE mExchangeStatus = ExchangeStatusE.NotActive;
    protected int mUnsentCount;
    protected long mFirstUnsentNumber;
    protected long mFirstUnsentDate;
    protected int mStorageFillPercent;

    /**
     * Статус сообщения с ОИСМ
     *
     * @author amv
     */
    public enum ExchangeStatusE {
        /**
         * нет активного обмен
         */
        NotActive(0, "нет активного обмена"),
        /**
         * начато чтение уведомления
         */
        StartRead(1, "начато чтение уведомления"),
        /**
         * Предоплата
         */
        WaitForReceipt(2, "ожидание квитанции на уведомление"),
        ;

        public final byte bVal;
        public final String name;

        private ExchangeStatusE(int value, String name) {
            this.bVal = (byte)value;
            this.name = name;
        }

        public static ExchangeStatusE fromByte(byte number){
            for (ExchangeStatusE val:values()){
                if (val.bVal == number){
                    return val;
                }
            }
            return NotActive;
        }
    }

    public OismStatistic() {
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
        mExchangeStatus = ExchangeStatusE.fromByte(bb.get());
        mUnsentCount = Utils.readUint16LE(bb);
        mFirstUnsentNumber = Utils.readUint32LE(bb);
        mFirstUnsentDate = Utils.readDate5(bb);
        mStorageFillPercent =bb.get();
    }

    /**
     * Признак установленного транспортного режима
     *
     * @return Признак установленного транспортного режима
     */
    public boolean isInProgress() {
        return mExchangeStatus!=ExchangeStatusE.NotActive;
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

    /**
     * Процент заполнения области хранения уведомлений
     *
     * @return Процент заполнения области хранения уведомлений
     */
    public int getStorageFillPercent() {
        return mStorageFillPercent;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeByte(mExchangeStatus.bVal);
        p.writeInt(mUnsentCount);
        p.writeLong(mFirstUnsentNumber);
        p.writeLong(mFirstUnsentDate);
        p.writeInt(mStorageFillPercent);
    }

    public void readFromParcel(Parcel p) {
        mExchangeStatus = ExchangeStatusE.fromByte(p.readByte());
        mUnsentCount = p.readInt();
        mFirstUnsentNumber = p.readLong();
        mFirstUnsentDate = p.readLong();
        mStorageFillPercent =p.readInt();
    }

    public static final Creator<OismStatistic> CREATOR = new Creator<OismStatistic>() {

        @Override
        public OismStatistic createFromParcel(Parcel p) {
            OismStatistic result = new OismStatistic();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public OismStatistic[] newArray(int size) {
            return new OismStatistic[size];
        }

    };

}
