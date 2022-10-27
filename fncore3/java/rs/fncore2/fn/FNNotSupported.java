package rs.fncore2.fn;

import java.nio.ByteBuffer;

import android.content.Context;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.ArchiveReport;
import rs.fncore.data.CheckNeedUpdateKeysE;
import rs.fncore.data.Correction;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.KKMInfo.FiscalReasonE;
import rs.fncore.data.OU;
import rs.fncore.data.OismStatistic;
import rs.fncore.data.ParcelableStrings;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore2.FNCore;
import rs.fncore2.fn.common.FNBase;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.common.ShiftExBase;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.log.Logger;

public class FNNotSupported extends FNBase {

	public FNNotSupported(StorageI storage) {
		super(storage);
		Logger.e("!! FNCORE Не поддерживает ФФД < 1.1 !!");
	}

	@Override
	public int readKKMInfo(KKMInfo result) {
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public int loadKKMInfo(int number) {
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public KKMInfoExBase getKKMInfo() {
		return new KKMInfoExBase() {
			{
		        mShift = new ShiftExBase(this);
		        add(FZ54Tag.T1013_KKT_SERIAL_NO, FNCore.getInstance().getDeviceSerial());
			}
		};
	}

	@Override
	public int exportMarking(String file) {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public int updateOISMStatus(OismStatistic s) {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public int updateOISMStatus(OismStatistic s, Transaction transaction, ByteBuffer bb) {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public byte[] readOISMDocument(ByteBuffer bb) {
		// TODO Auto-generated method stub
		return new byte [0];
	}

	@Override
	public boolean writeOFDReplyOISM(ByteBuffer mDataBuffer, long docNo, byte[] answer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FNCounters getFnCounters(Transaction transaction, boolean isTotal) {
		return new FNCounters(isTotal);
	}

	@Override
	public int storeOfflineMarkNotify() {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public doPrintResult doArchive(OU operator, ArchiveReport report, String template) {
		return new doPrintResult();
	}

	@Override
	public doPrintResult doFiscalization(FiscalReasonE reason, OU operator, KKMInfo info, KKMInfo signed,
			String template) {
		return new doPrintResult();
	}

	@Override
	public doCorrectionResult doCorrection(Correction cor, OU operator, Correction signed, String template) {
		// TODO Auto-generated method stub
		return new doCorrectionResult();
	}

	@Override
	public doCorrectionResult doCorrection2(Correction cor, OU operator, Correction signed, String header, String item,
			String footer, String footerEx) {
		// TODO Auto-generated method stub
		return new doCorrectionResult();
	}

	@Override
	public doSellOrderResult doSellOrder(SellOrder order, OU operator, SellOrder signed, boolean doPrint, String header,
			String item, String footer, String footerEx, boolean cashControlFlag) {
		// TODO Auto-generated method stub
		return new doSellOrderResult();
	}

	@Override
	public doPrintResult requestFiscalReport(OU operator, FiscalReport report, String template) {
		// TODO Auto-generated method stub
		return new doPrintResult();
	}

	@Override
	public doShiftResult openShift(OU operator, Shift shift, String template, Context ctx) {
		// TODO Auto-generated method stub
		return new doShiftResult();
	}

	@Override
	public doShiftResult closeShift(OU operator, Shift shift, String template) {
		// TODO Auto-generated method stub
		return new doShiftResult();
	}

	@Override
	public int checkMarkingItem(SellItem item, DocServerSettings oismSettings) {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public int confirmMarkingItem(SellItem item, boolean accepted) {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public int checkMarkingItemLocalFN(SellItem item) {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public int checkMarkingItemOnlineOISM(SellItem item, DocServerSettings oismSettings) {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

	@Override
	public void releaseMarkCodes() {
		// TODO Auto-generated method stub

	}

	@Override
	public CheckNeedUpdateKeysE isNeedUpdateOkpKeys() {
		// TODO Auto-generated method stub
		return CheckNeedUpdateKeysE.UNKNOWN;
	}

	@Override
	public String getPrintDoc(String type, byte[] bb, ParcelableStrings templates) {
		// TODO Auto-generated method stub
		return Const.EMPTY_STRING;
	}

	@Override
	public Tag getDoc(String type, byte[] bb) {
		// TODO Auto-generated method stub
		return new Tag();
	}

	@Override
	protected int cancelDocument(Transaction transaction) {
		// TODO Auto-generated method stub
		return Errors.FN_VERSION_TOO_OLD;
	}

}
