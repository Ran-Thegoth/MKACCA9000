package rs.fncore2.fn.common;

import android.os.Parcel;

import rs.fncore.data.FiscalReport;
import rs.fncore.data.KKMInfo;
import rs.fncore2.PrintHelper;
import rs.utils.Utils;

public class FiscalReportExBase extends FiscalReport {


	protected KKMInfo mKKMInfo;
	protected FiscalReportExBase(Parcel p) {
		super();
		readFromParcel(p);
	}

	public FiscalReportExBase(KKMInfo info) {
		mKKMInfo = info;
		mShiftNumber = info.getShift().getNumber();
		mIsOffline = info.isOfflineMode();
		mIsShiftOpen = info.getShift().isOpen();
	}

	public void cloneTo(FiscalReport dest) {
		Utils.readFromParcel(dest, Utils.writeToParcel(this));
	}

	public String getPF(String template, KKMInfo info) {
		return getPF(template);
	}

	public String getPF(String template) {
		return PrintHelper.processTemplate(PrintHelper.loadTemplate(template, "fiscalreport"), this);
	}

}
