package rs.fncore.data;

import java.util.Calendar;

import android.os.Parcel;
import android.os.Parcelable;
import rs.fncore.FZ54Tag;
import rs.fncore.data.KKMInfo.FFDVersionE;
import rs.utils.Utils;

/**
 * Подпись фискального документа
 *
 * @author nick
 */
public class Signature implements Parcelable, TemplateProcessor {

	private static final String SIGN_DATE = "signature.Date";
	private static final String SIGN_NUMBER = "signature.Number";
	private static final String SIGN_SIGN = "signature.sign";
	private static final String DEVICE_NUMBER = "device.Number";
	private static final String DEVICE_REGNO = "device.regNo";
	private static final String DEVICE_FN = "device.FN";
	private static final String FFD_KKT = "FFD.KKT";
	private static final String FFD_FN = "FFD.FN";
	private static final String FFD_VER = "FFD.VER";
	private static final String DEVICE_VERSION = "device.Version";
	private static final String OPERATOR_NAME = "operator.Name";
	private static final String OPERATOR_INN = "operator.INN";
	private static final String ADDRESS = "Address";
	private static final String LOCATION = "Location";
	private static final String OWNER_NAME = "owner.Name";
	private static final String OWNER_INN = "owner.INN";

	protected int mFdNumber;
	protected long mFPD;
	protected long mSignDate;
	protected Signer mSigner;
	protected OU mOperator = new OU();
	@SuppressWarnings("unused")
	private Document mOwner;

	public Signature(Document owner) {
		mOwner = owner;
	}

	public Signature(Document owner, Signer signer, long signTime) {
		mOwner = owner;
		mSigner = signer;
		mSignDate = signTime;
	}

	/**
	 * Фискальный номер документа
	 *
	 * @return Фискальный номер документа
	 */
	public int getFdNumber() {
		return mFdNumber;
	}

	/**
	 * Фискальная подпись документа
	 *
	 * @return Фискальная подпись документа
	 */
	public long getFpd() {
		return mFPD;
	}

	/**
	 * Дата/время подписи
	 *
	 * @return Дата/время подписи
	 */
	public long signDate() {
		return mSignDate;
	}

	/**
	 * Информация об оборудовании выполнившем фискальную операцию
	 *
	 * @return Информация об оборудовании выполнившем фискальную операцию
	 */
	public Signer signer() {
		return mSigner;
	}

	/**
	 * Оператор (кассир), выполнивший фискализацию
	 *
	 * @return Оператор (кассир), выполнивший фискализацию
	 */
	public OU operator() {
		return mOperator;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		p.writeInt(mFdNumber);
		p.writeLong(mFPD);
		p.writeLong(mSignDate);
		if (mSigner != null) {
			p.writeInt(1);
			mSigner.writeToParcel(p, flags);
		} else
			p.writeInt(0);
	}

	public void readFromParcel(Parcel p) {
		mFdNumber = p.readInt();
		mFPD = p.readLong();
		mSignDate = p.readLong();
		if (p.readInt() != 0) {
			mSigner = new Signer();
			mSigner.readFromParcel(p);
		}
	}

	public boolean parseTag(Tag t) {
		switch (t.getId()) {
		case FZ54Tag.T1012_DATE_TIME:
			mSignDate = t.asTimeStamp() - Calendar.getInstance().getTimeZone().getRawOffset();
			break;
		case FZ54Tag.T1013_KKT_SERIAL_NO:
			if (mSigner == null)
				mSigner = new Signer();
			mSigner.mDeviceSerial = t.asString().trim();
			break;
		case FZ54Tag.T1018_OWNER_INN:
			if (mSigner == null)
				mSigner = new Signer();
			mSigner.mOwner.setINN(t.asString());
			break;
		case FZ54Tag.T1021_CASHIER_NAME:
			mOperator.setName(t.asString());
			break;
		case FZ54Tag.T1037_KKT_REG_NO:
			if (mSigner == null)
				mSigner = new Signer();
			mSigner.mKkmNumber = t.asString().trim();
			break;
		case FZ54Tag.T1040_DOCUMENT_NO:
			mFdNumber = (int)(t.asUInt() & 0xFFFFFFFF);
			break;
		case FZ54Tag.T1041_FN_NO:
			if (mSigner == null)
				mSigner = new Signer();
			mSigner.mFnNumber = t.asString();
			break;
		case FZ54Tag.T1209_FFD_VERSION:
			if (mSigner == null)
				mSigner = new Signer();
			mSigner.mFFDVersion = FFDVersionE.fromByte(t.asByte());
			break;
		case FZ54Tag.T1048_OWNER_NAME:
			if (mSigner == null)
				mSigner = new Signer();
			mSigner.mOwner.setName(t.asString());
			break;
		case FZ54Tag.T1077_DOCUMENT_FISCAL_SIGN:
			mFPD = t.asUInt();
			break;
		case FZ54Tag.T1009_TRANSACTION_ADDR:
			if (mSigner == null)
				mSigner = new Signer();
			mSigner.mLocation.setAddress(t.asString());
			break;
		case FZ54Tag.T1187_TRANSACTION_PLACE:
			if (mSigner == null)
				mSigner = new Signer();
			mSigner.mLocation.setPlace(t.asString());
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	public String onKey(String key) {
		switch (key) {
		case OWNER_INN:
			return signer().mOwner.getINNtrimZ();
		case OWNER_NAME:
			return signer().mOwner.getName();
		case OPERATOR_NAME:
			return operator().getName();
		case OPERATOR_INN:
			return operator().getINNtrimZ();
		case DEVICE_NUMBER:
			return signer().DeviceSerial();
		case DEVICE_REGNO:
			return signer().KKMNumber();
		case DEVICE_FN:
			return signer().FNNumber();
		case DEVICE_VERSION:
			return KKMInfo.KKT_VERSION;
		case FFD_KKT:
			return KKMInfo.FFDVersionE.VER_12.name;
		case FFD_FN:
			return signer().mFFDVersion.name;
		case FFD_VER:
			return String.valueOf(signer().mFFDVersion.bVal);
		case SIGN_NUMBER:
			return String.valueOf(getFdNumber());
		case SIGN_DATE:
			return Utils.formatDate(signDate());
		case SIGN_SIGN:
			return String.valueOf(getFpd());
		case ADDRESS:
			return signer().getLocation().getAddress();
		case LOCATION:
			return signer().getLocation().getPlace();
		}
		return null;
	}
}
