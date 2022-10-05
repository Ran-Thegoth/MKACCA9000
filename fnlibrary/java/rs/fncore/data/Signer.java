package rs.fncore.data;

import android.os.Parcel;
import android.os.Parcelable;
import rs.fncore.data.KKMInfo.FFDVersionE;
import rs.utils.Utils;

/**
 * Оборудование выполнившее подпись
 *
 * @author nick
 */
public class Signer implements Parcelable {
    protected String mKkmNumber;
    protected String mFnNumber;
    protected String mDeviceSerial = Utils.getDeviceSerial();
    protected OU mOwner = new OU();
	protected FFDVersionE mFFDVersion = FFDVersionE.VER_12;
    protected Location mLocation = new Location();

    public String DeviceSerial() {
        return mDeviceSerial;
    }

    /**
     * Заводской номер ККТ
     *
     * @return Заводской номер ККТ
     */
    public String KKMNumber() {
        return mKkmNumber;
    }

    /**
     * Серийный номер фискального накопителя
     *
     * @return Серийный номер фискального накопителя
     */
    public String FNNumber() {
        return mFnNumber;
    }

    /**
     * Владелец ККТ
     *
     * @return Владелец ККТ
     */
    public OU owner() {
        return mOwner;
    }

    /**
     * Адрес и место расчетов
     *
     * @return Адрес и место расчетов
     */
    public Location getLocation() {
        return mLocation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(mDeviceSerial);
        p.writeString(mFnNumber);
        p.writeString(mKkmNumber);
        p.writeInt(mFFDVersion.ordinal());
        mOwner.writeToParcel(p, flags);
    }

    public void readFromParcel(Parcel p) {
        mDeviceSerial = p.readString();
        mFnNumber = p.readString();
        mKkmNumber = p.readString();
        mFFDVersion = FFDVersionE.values()[p.readInt()];
        mOwner.readFromParcel(p);
    }
}
