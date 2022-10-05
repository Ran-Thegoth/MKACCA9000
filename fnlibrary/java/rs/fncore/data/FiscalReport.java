package rs.fncore.data;

import android.os.Parcel;
import android.os.Parcelable;
import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.utils.Utils;

/**
 * Отчет о состоянии расчетов
 *
 * @author nick
 */
public class FiscalReport extends Document {

    public static final String CLASS_NAME="FiscalReport";
    public static final String CLASS_UUID="bf308fbc-ebae-11eb-9a03-0242ac130003";

    protected OfdStatistic mOfdStatistic = new OfdStatistic();
    protected boolean mIsOffline;
    protected int mNumUnsentMarkNotify;
    protected int mShiftNumber;
    protected boolean mIsShiftOpen;
    protected FNCounters mTotalCounters;


    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        mOfdStatistic.writeToParcel(p, flags);
        p.writeInt(mShiftNumber);
        p.writeInt(mIsShiftOpen ? 1 :0);
        mSignature.operator().writeToParcel(p, flags);
        p.writeInt(mNumUnsentMarkNotify);
        if(mTotalCounters != null) {
        	p.writeInt(1);
        	mTotalCounters.writeToParcel(p, flags);
        } else 
        	p.writeInt(0);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        mOfdStatistic.readFromParcel(p);
        mShiftNumber = p.readInt();
        mIsShiftOpen = p.readInt() != 0;
        mSignature.operator().readFromParcel(p);
        mNumUnsentMarkNotify=p.readInt();
        if(p.readInt() != 0) {
        	mTotalCounters = new FNCounters(true);
        	mTotalCounters.readFromParcel(p);
        }
    }

    /**
     * Информация по документам для отправки в ОФД
     *
     * @return Информация по документам для отправки в ОФД
     */
    public OfdStatistic getOFDStatistic() {
        return mOfdStatistic;
    }

    /**
     * Данные о неотправленных уведомлений по маркировке
     *
     * @return количество неотправленных уведомлений по маркировке
     */
    public int getUnsentMarkingNotify() {
        return mNumUnsentMarkNotify;
    }

    public static final Parcelable.Creator<FiscalReport> CREATOR = new Parcelable.Creator<FiscalReport>() {
        @Override
        public FiscalReport createFromParcel(Parcel p) {
            FiscalReport result = new FiscalReport();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public FiscalReport[] newArray(int size) {
            return new FiscalReport[size];
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

	private static final String NO_DATE = "-";
	private static final String NUM_UNSENTS = "ofd.NumUnsent";
	private static final String DATE_UNSENTS = "ofd.DateUnsent";
	private static final String NUM_FIRST_UNSENT = "ofd.FirstUnsentNo";
	private static final String SHIFT_NUMBER = "shift.Number";
	private static final String IS_OPEN = "shift.IsOpen";
	private static final String OFFLINE = "offline";
	private static final String NUM_OISM_UNSENT = "oism.NumUnsent";
	private static final String TOTAL_COUNTERS = "counters.total";

	@Override
	protected boolean parseTag(Tag t) {
		switch(t.getId()) {
		case FZ54Tag.T1002_AUTONOMOUS_MODE:
			mIsOffline = t.asBoolean();
			break;
		case FZ54Tag.T1157_FN_TOTALS_TLV:
			mTotalCounters = new FNCounters(true);
			mTotalCounters.parseTag(t);
			break;
		case FZ54Tag.T1038_SHIFT_NO:
			mShiftNumber = t.asInt();
			mIsShiftOpen = true;
			break;
		case FZ54Tag.T1097_OFD_UNSENT_COUNT:
		case FZ54Tag.T1098_OFD_UNSENT_DATE:
		case FZ54Tag.T1116_OFD_UNSENT_NO:
			mOfdStatistic.parseTag(t);
			return true;
		default:
			return super.parseTag(t);
		}
		return true;
	}
	@Override
	public String onKey(String key) {
		switch (key) {
		case TOTAL_COUNTERS:
			if(mTotalCounters == null) return Const.EMPTY_STRING;
			return Utils.printCounters(mTotalCounters);
		case NUM_OISM_UNSENT:
			if (mIsOffline)
				return "0";
			return String.valueOf(mNumUnsentMarkNotify);
		case NUM_FIRST_UNSENT:
			return String.valueOf(getOFDStatistic().getFirstUnsentNumber());
		case NUM_UNSENTS:
			return String.valueOf(getOFDStatistic().getUnsentDocumentCount());
		case DATE_UNSENTS:
			if (getOFDStatistic().getUnsentDocumentCount() == 0)
				return NO_DATE;
			return Utils.formatDate(getOFDStatistic().getFirstUnsentDate());
		case SHIFT_NUMBER:
			return String.valueOf(mShiftNumber);
		case IS_OPEN:
			return bv(mIsShiftOpen);
		case OFFLINE:
			return bv(mIsOffline);
		default:
			return super.onKey(key);
		}
	}
}
