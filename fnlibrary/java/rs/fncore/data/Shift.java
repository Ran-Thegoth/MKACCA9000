package rs.fncore.data;

import android.os.Parcel;
import android.os.Parcelable;
import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.utils.Utils;

/**
 * Рабочая смена
 *
 * @author nick
 */
public class Shift extends Document {

	public static final int INCORRECT_MARKING_CODE_FLAG = 1 << 1;
	public static final int INCORRECT_MARKING_CODE_IN_FISCAL_FLAG = 1 << 2;

	public static final String CLASS_NAME = "Shift";
	public static final String CLASS_UUID = "d4d8fcdc-ebae-11eb-9a03-0242ac130003";
	protected int mShiftNumber = 0;
	protected long mWhenOpen = 0;
	protected boolean mIsOpen = false;
	protected long mLastDocNumber = 0;
	protected long mLastCheckNumber = 0;
	protected long mFnRemainedDays = 0;
	protected OfdStatistic mOFDStat = new OfdStatistic();
	protected int mNumUnsentMarkNotify;
	protected FNCounters mTotalCounters;
	protected FNCounters mShiftCounters;

	protected String mOKPUpdateResult = Const.EMPTY_STRING;
	public Shift() {
	}
	Shift(boolean isOpen) {
		mIsOpen = isOpen;
	}


	@Override
	public void writeToParcel(Parcel p, int flags) {
		super.writeToParcel(p, flags);
		p.writeInt(mShiftNumber);
		p.writeLong(mWhenOpen);
		p.writeLong(mLastDocNumber);
		p.writeLong(mLastCheckNumber);
		p.writeInt(mIsOpen ? 1 : 0);
		mOFDStat.writeToParcel(p, flags);
		p.writeInt(mNumUnsentMarkNotify);
		p.writeLong(mFnRemainedDays);
		p.writeString(mOKPUpdateResult);
		if (mTotalCounters != null) {
			p.writeInt(1);
			mTotalCounters.writeToParcel(p, flags);
		} else
			p.writeInt(0);
		if (mShiftCounters != null) {
			p.writeInt(1);
			mShiftCounters.writeToParcel(p, flags);
		} else
			p.writeInt(0);

	}

	@Override
	public void readFromParcel(Parcel p) {
		super.readFromParcel(p);
		mShiftNumber = p.readInt();
		mWhenOpen = p.readLong();
		mLastDocNumber = p.readLong();
		mLastCheckNumber = p.readLong();
		mIsOpen = p.readInt() != 0;
		mOFDStat.readFromParcel(p);
		mNumUnsentMarkNotify = p.readInt();
		mFnRemainedDays = p.readLong();
        mOKPUpdateResult = p.readString();
        
		if (p.readInt() == 1) {
			mTotalCounters = new FNCounters(false);
			mTotalCounters.readFromParcel(p);
		} else
			mTotalCounters = null;
		if (p.readInt() == 1) {
			mShiftCounters = new FNCounters(false);
			mShiftCounters.readFromParcel(p);
		} else
			mShiftCounters = null;

	}

	/**
	 * @return Данные о неотправленных в ОФД документах
	 */
	public OfdStatistic getOFDStatistic() {
		return mOFDStat;
	}

	/**
	 * @return Данные о неотправленных уведомлений по маркировке
	 */
	public int getUnsentMarkingNotify() {
		return mNumUnsentMarkNotify;
	}

	/**
	 * @return Открыта ли смена (актуален после фискализации)
	 */
	public boolean isOpen() {
		return mIsOpen;
	}

	/**
	 * @return Номер смены (доступен после фискализации)
	 */
	public int getNumber() {
		return mShiftNumber;
	}

	/**
	 * @return Номер последнего документа в смене если смена открыта, количество
	 *         документов за смену если закрыта (доступен после фискализации)
	 */
	public long getLastDocumentNumber() {
		return mLastDocNumber;
	}

	/**
	 * Номер последнего чека за смену (доступен после фискализации)
	 * @return Номер последнего чека за смену
	 */
	public long getLastCheckNumber() {
		return mLastCheckNumber;
	}

	/**
	 * @return дата, Когда открыта смена  (доступен после фискализации)
	 */
	public long getWhenOpen() {
		return mWhenOpen;
	}

	protected void setWhenOpen(long when) {
		mWhenOpen = when;
	}

	public static final Parcelable.Creator<Shift> CREATOR = new Parcelable.Creator<Shift>() {
		@Override
		public Shift createFromParcel(Parcel p) {
			Shift result = new Shift();
			result.readFromParcel(p);
			if (p.dataAvail() > 0)
				result.mSignature.mOperator.readFromParcel(p);
			return result;
		}

		@Override
		public Shift[] newArray(int size) {
			return new Shift[size];
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

	@Override
	protected boolean parseTag(Tag t) {
		switch (t.getId()) {
		case FZ54Tag.T1038_SHIFT_NO:
			mShiftNumber = t.asInt();
			break;
		case FZ54Tag.T1118_SHIFT_BILL_COUNT:
			mLastCheckNumber = t.asUInt();
			break;
		case FZ54Tag.T1111_SHIFT_DOCUMENT_COUNT:
			mLastDocNumber = t.asUInt();
			break;
		case FZ54Tag.T1213_FN_KEYS_EXPIRE_DAYS:
			mFnRemainedDays = t.asUInt();
			break;
		case FZ54Tag.T1097_OFD_UNSENT_COUNT:
		case FZ54Tag.T1098_OFD_UNSENT_DATE:
		case FZ54Tag.T1116_OFD_UNSENT_NO:
			mOFDStat.parseTag(t);
			return true;
		case FZ54Tag.T1157_FN_TOTALS_TLV:
			mTotalCounters = new FNCounters(true);
			mTotalCounters.parseTag(t);
			break;
		case FZ54Tag.T1194_TOTAL_SHIFT_SUM_TLV:
			mShiftCounters = new FNCounters(false);
			mShiftCounters.parseTag(t);
			break;
		default:
			return super.parseTag(t);
		}
		return true;
	}

	public FNCounters getShiftCounters() { return mShiftCounters; }
	public FNCounters getTotalCounters() { return mTotalCounters; }
	
	private static final String SHIFT_DOCUMENTS = "shift.NumDocuments";
	private static final String SHIFT_NUMBER = "shift.Number";
	private static final String IS_OPEN = "shift.IsOpen";
	private static final String SHIFT_CHECKS = "shift.NumChecks";
	private static final String NUM_UNSENTS = "ofd.NumUnsent";
	private static final String DATE_UNSENTS = "ofd.DateUnsent";
	private static final String NUM_FIRST_UNSENT = "ofd.FirstUnsentNo";
	private static final String NUM_OISM_UNSENT = "oism.NumUnsent";
	private static final String INCORRECT_MARKING_CODE = "mark.Incorrect";
	private static final String INCORRECT_MARKING_CODE_IN_FISCAL = "mark.Incorrect.InFiscal";
	private static final String TOTAL_COUNTERS = "counters.total";
	private static final String SHIFT_COUNTERS = "counters.shift";

	private static final String TOTAL_COUNTERS_HAS = "is.counters.total";
	private static final String SHIFT_COUNTERS_HAS = "is.counters.shift";
	
	protected static final String UPDATE_OKP_KEYS_RESULT= "OKP.Update.Result";

	protected static final String KEYS_DAYS_REMAINING = "Keys.Days.Remaning";

	@Override
	public String onKey(String key) {
		switch (key) {
		case TOTAL_COUNTERS_HAS:
			return bv(mTotalCounters != null);
		case SHIFT_COUNTERS_HAS:
			return bv(mShiftCounters != null);
		case TOTAL_COUNTERS:
			if(mTotalCounters == null) return Const.EMPTY_STRING;
			return Utils.printCounters(mTotalCounters);
		case SHIFT_COUNTERS:
			if(mShiftCounters == null) return Const.EMPTY_STRING;
			return Utils.printCounters(mShiftCounters);
		case SHIFT_NUMBER:
			return String.valueOf(getNumber());
		case IS_OPEN:
			return bv(isOpen());
		case SHIFT_DOCUMENTS:
			return String.valueOf(mLastDocNumber);
		case SHIFT_CHECKS:
			return String.valueOf(mLastCheckNumber);
		case NUM_UNSENTS:
			return String.valueOf(getOFDStatistic().getUnsentDocumentCount());
		case DATE_UNSENTS:
			if (getOFDStatistic().getUnsentDocumentCount() == 0)
				return "-";
			return Utils.formatDate(getOFDStatistic().getFirstUnsentDate());
		case NUM_FIRST_UNSENT:
			return String.valueOf(getOFDStatistic().getFirstUnsentNumber());
		case KEYS_DAYS_REMAINING:
			return String.valueOf(mFnRemainedDays); // mKKMInfo.getFnRemainedDays();
		case NUM_OISM_UNSENT:
			if (isOpen())
				return "0";
			return String.valueOf(mNumUnsentMarkNotify);
		case INCORRECT_MARKING_CODE:
			if (isOpen())
				return "";
			if (getTag(FZ54Tag.T2112_MARKING_CODE_INCORRECT) != null) {
				if ((getTag(FZ54Tag.T2112_MARKING_CODE_INCORRECT).asInt() & INCORRECT_MARKING_CODE_FLAG) > 0) {
					return bv(true);
				}
			}
			return bv(false);
		case INCORRECT_MARKING_CODE_IN_FISCAL:
			if (isOpen())
				return "";
			if (getTag(FZ54Tag.T2112_MARKING_CODE_INCORRECT) != null) {
				if ((getTag(FZ54Tag.T2112_MARKING_CODE_INCORRECT).asInt()
						& INCORRECT_MARKING_CODE_IN_FISCAL_FLAG) > 0) {
					return bv(true);
				}
			}
			return bv(false);
        case UPDATE_OKP_KEYS_RESULT: {
        	return mOKPUpdateResult;
        }
		default:
			return super.onKey(key);
		}
	}

}
