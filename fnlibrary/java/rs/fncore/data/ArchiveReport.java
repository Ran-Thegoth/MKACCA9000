package rs.fncore.data;

import android.os.Parcel;
import android.os.Parcelable;
import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.utils.Utils;

/**
 * Отчет о переводе ФН в постфискальный режим
 *
 * @author nick
 */
public class ArchiveReport extends Document {

	protected boolean mIsAutomateMode;
	protected String mAutomateNumber = Const.EMPTY_STRING;
	public static final String CLASS_NAME = "ArchiveReport";
	public static final String CLASS_UUID = "ae948b36-ebae-11eb-9a03-0242ac130003";
	protected int mShiftNumber;
	protected FNCounters mTotalCounters;

	public ArchiveReport() {
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		super.writeToParcel(p, flags);
		p.writeInt(mIsAutomateMode ? 1 : 0);
		p.writeString(mAutomateNumber);
		p.writeInt(mShiftNumber);
		if (mTotalCounters != null) {
			p.writeInt(1);
			mTotalCounters.writeToParcel(p, flags);
		} else
			p.writeInt(0);
	}

	@Override
	public void readFromParcel(Parcel p) {
		super.readFromParcel(p);
		mIsAutomateMode = p.readInt() != 0;
		mAutomateNumber = p.readString();
		mShiftNumber = p.readInt();
		if (p.readInt() != 0) {
			mTotalCounters = new FNCounters(true);
			mTotalCounters.readFromParcel(p);
		}
	}

	/**
	 * Признак "установлен в автомате"
	 *
	 * @return Признак "установлен в автомате"
	 */
	public boolean isAutomatedMode() {
		return mIsAutomateMode;
	}

	/**
	 * Номер автомата
	 *
	 * @return Номер автомата
	 */
	public String getAutomateNumber() {
		return mAutomateNumber;
	}

	/**
	 * Номер последней смены
	 *
	 * @return Номер последней смены
	 */
	public int getShiftNumber() {
		return mShiftNumber;
	}

	public static final Parcelable.Creator<ArchiveReport> CREATOR = new Parcelable.Creator<ArchiveReport>() {
		@Override
		public ArchiveReport createFromParcel(Parcel p) {
			ArchiveReport result = new ArchiveReport();
			result.readFromParcel(p);
			return result;
		}

		@Override
		public ArchiveReport[] newArray(int size) {
			return new ArchiveReport[size];
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
		switch(t.getId()) {
		case FZ54Tag.T1157_FN_TOTALS_TLV:
			mTotalCounters = new FNCounters(true);
			mTotalCounters.parseTag(t);
			break;
		case FZ54Tag.T1038_SHIFT_NO:
			mShiftNumber = (int)t.asInt();
			break;
		case FZ54Tag.T1001_AUTOMATIC_MODE:
			mIsAutomateMode = t.asBoolean();
			break;
		case FZ54Tag.T1036_AUTOMAT_NO:
			mAutomateNumber = t.asString().trim();
			break;
		default:
			return super.parseTag(t);
		}
		return true;
		
	}
	
	private static final String IS_AUTOMATION = "automation";
	private static final String AUTOMATE_NUMBER = "automateNumber";
	private static final String SHIFT_NUMBER = "shift.Number";
	private static final String TOTAL_COUNTERS = "counters.total";

	@Override
	public String onKey(String key) {
		switch (key) {
		case TOTAL_COUNTERS:
			if (mTotalCounters == null)
				return Const.EMPTY_STRING;
			return Utils.printCounters(mTotalCounters);
		case IS_AUTOMATION:
			return bv(isAutomatedMode());
		case AUTOMATE_NUMBER:
			return getAutomateNumber();
		case SHIFT_NUMBER:
			return String.valueOf(getShiftNumber());
		}
		return super.onKey(key);
	}
}
