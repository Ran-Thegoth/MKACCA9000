package rs.fncore2.fn.fn1_05;


import android.content.Context;


import java.nio.ByteBuffer;

import rs.fncore.Errors;
import rs.fncore.data.ArchiveReport;
import rs.fncore.data.CheckNeedUpdateKeysE;
import rs.fncore.data.Correction;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.Document;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OismStatistic;
import rs.fncore.data.OU;
import rs.fncore.data.ParcelableStrings;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore2.fn.common.FNBase;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.fn12.FN2Commands;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;
import rs.log.Logger;

public class FN extends FNBase {

    public FN(StorageI storage) {
        super(storage);
        mKKMInfo = new KKMInfoEx();
        Logger.i("creating FN 1.1");
    }

    public final KKMInfoEx getKKMInfo() {
        return (KKMInfoEx)mKKMInfo;
    }

    protected int cancelDocument(Transaction transaction) {
        ByteBuffer bb = BufferFactory.allocateDocument();
        try {
            int res=mKKMInfo.readBase(transaction, bb);
            if (res!= Errors.NO_ERROR) return res;

            if (mKKMInfo.hasUnfinished()) {
                if (!transaction.write(FNCommandsE.CANCEL_DOCUMENT).check()) {
                    return transaction.getLastError();
                }
            }
            return Errors.NO_ERROR;
        } finally {
            BufferFactory.release(bb);
        }
    }

    public doPrintResult doArchive(OU operator, ArchiveReport report, String template) {
        Logger.i("Начата архивация");
        try (Transaction transaction = mStorage.open()) {
            doPrintResult res = new doPrintResult();

            ArchiveReportEx helper = new ArchiveReportEx(getKKMInfo());
            res.code = helper.write(transaction, operator);
            Logger.i("Архивация завершена, результат 0x%02X", res.code);

            if (res.code == Errors.NO_ERROR) {
                mKKMInfo.read(transaction);

                if (report != null) helper.cloneTo(report);
                res.print = helper.getPF(template);
            } else {
                cancelDocument(transaction);
            }
            return res;
        }
    }

    public doPrintResult doFiscalization(KKMInfo.FiscalReasonE reason, OU operator, KKMInfo info, KKMInfo signed,
                                         String template) {
        Logger.i("Начата фискализация");

        try (Transaction transaction = mStorage.open()) {
            info.setFFDProtocolVersion(KKMInfo.FFDVersionE.VER_105);
            doPrintResult res = new doPrintResult();

            KKMInfoEx newInfo;
            if (reason != KKMInfo.FiscalReasonE.CHANGE_INN) {
                newInfo = new KKMInfoEx(info);
                mKKMInfo.cloneSavedStatus(newInfo);
                res.code = newInfo.write(reason, transaction, operator);
                Logger.i("Фискализация завершена, результат 0x%02X", res.code);
            } else {
                newInfo = new KKMInfoEx(mKKMInfo);
                String currInn = mKKMInfo.getOwner().getINN();
                if (template.length() == 10) {
                    // move to entity user
                    if (currInn.length() != 12 ||
                            !currInn.startsWith("00") ||
                            !currInn.substring(2, 12).equals(template)) {
                        res.code = Errors.WRONG_INN;
                    } else {
                        template = template + "  ";
                    }
                } else if (template.length() == 12) {
                    if (currInn.trim().length() != 10 ||
                            !template.startsWith("00") ||
                            !template.substring(2, 12).equals(currInn)) {
                        res.code = Errors.WRONG_INN;
                    }
                } else {
                    res.code = Errors.WRONG_INN;
                }
                newInfo.getOwner().setINN(template);
            }

            if (res.code == Errors.NO_ERROR) {
                mKKMInfo.cloneSavedStatus(newInfo);
                newInfo.cloneTo(mKKMInfo);
                if (signed != null) mKKMInfo.cloneTo(signed);
                res.print = newInfo.getPF(template);
            } else {
                if (signed != null) newInfo.cloneTo(signed);
                cancelDocument(transaction);
            }
            return res;
        }
        finally{
        }
    }

    public doCorrectionResult doCorrection(Correction cor, OU operator, Correction signed, String template) {
        Logger.i("Начата коррекция");
        try (Transaction transaction = mStorage.open()) {
            doCorrectionResult res = new doCorrectionResult();
            CorrectionEx helper = new CorrectionEx(getKKMInfo(), cor);
            res.code = helper.write(transaction, operator);
            Logger.i("Коррекция завершена, результат 0x%02X", res.code);

            if (res.code == Errors.NO_ERROR) {
                if (signed != null) helper.cloneTo(signed);

                res.print = helper.getPF(template);
                res.cor = helper;
            } else {
                cancelDocument(transaction);
            }

            return res;

        }
    }

    public doCorrectionResult doCorrection2(Correction cor, OU operator, Correction signed, String header, String item,
                                           String footer, String footerEx) {
        throw new RuntimeException("no supported for FFD 1.05");
    }

    public doSellOrderResult doSellOrder(SellOrder order, OU operator,
                                         SellOrder signed, boolean doPrint, String header, String item,
                                         String footer, String footerEx, boolean cashControlFlag) {

        Logger.i("Начат чек продаж");

        try (Transaction transaction = mStorage.open()) {
            doSellOrderResult res = new doSellOrderResult();

            SellOrderEx helper = new SellOrderEx(getKKMInfo(), order);
            res.code = helper.write(transaction, operator,
                    cashControlFlag);
            Logger.i("Чек продаж завершен, результат 0x%02X", res.code);

            if (res.code == Errors.NO_ERROR) {
                if (signed != null) helper.cloneTo(signed);
                if (doPrint) res.print = helper.getPF(header, item, footer, footerEx);
                res.sell = helper;
            } else {
                cancelDocument(transaction);
            }
            return res;

        }
    }

    public int checkMarkingItem(SellItem item, DocServerSettings oismSettings){
        return Errors.NO_ERROR;
    }

    public int checkMarkingItemLocalFN(SellItem item){
        return Errors.NO_ERROR;
    }

    public int checkMarkingItemOnlineOISM(SellItem item, DocServerSettings oismSettings){
        return Errors.NO_ERROR;
    }

    public int confirmMarkingItem(SellItem item, boolean accepted){
        return Errors.NO_ERROR;
    }

    public doPrintResult requestFiscalReport(OU operator, FiscalReport report, String template) {
        Logger.i("Запрос отчета о состоянии расчетов");

        try (Transaction transaction = mStorage.open()) {
            doPrintResult res = new doPrintResult();

            FiscalReportEx signed = new FiscalReportEx(mKKMInfo);
            res.code = signed.write(transaction, operator);
            Logger.i("Запрос выполнен, результат 0x%02X", res.code);

            if (res.code == Errors.NO_ERROR) {
                if (report != null) signed.cloneTo(report);
                res.print = signed.getPF(template);
            } else {
                cancelDocument(transaction);
            }
            return res;
        }
    }


    public doShiftResult openShift(OU operator, Shift shift, String template, Context ctx) {
        doShiftResult res = new doShiftResult();

        if (mKKMInfo.getShift().isOpen()) {
            Logger.w("Shift already opened: " + mKKMInfo.getShift().getNumber());
            if (shift != null) getKKMInfo().getShift().cloneTo(shift);

            res.code = Errors.NO_ERROR;
            res.alreadyDone = true;
            return res;
        }

        Logger.i("Opening shift...");

        try (Transaction transaction = mStorage.open()) {
            res.code = getKKMInfo().getShift().writeOpenShift(transaction, operator);

            if (res.code == Errors.NO_ERROR) {
                Logger.i("Done opening shift: " + mKKMInfo.getShift().getNumber());


                if (shift != null) getKKMInfo().getShift().cloneTo(shift);
                res.print = getKKMInfo().getShift().getPF(template);
                res.shift = mKKMInfo.getShift();
            } else {
                Logger.i("Error opening shift: 0x%02X", res.code);
                cancelDocument(transaction);
            }
            return res;
        }
    }


    public doShiftResult closeShift(OU operator, Shift shift, String template) {
        doShiftResult res = new doShiftResult();

        if (!mKKMInfo.getShift().isOpen()) {
            Logger.w("Shift already closed: " + mKKMInfo.getShift().getNumber());
            if (shift != null) getKKMInfo().getShift().cloneTo(shift);

            res.code = Errors.NO_ERROR;
            res.alreadyDone = true;
            return res;
        }

        Logger.i("Closing shift...");

        try (Transaction transaction = mStorage.open()) {
            res.code = getKKMInfo().getShift().writeCloseShift(transaction, operator);
            if (res.code == Errors.NO_ERROR) {
                Logger.i("Done closing shift: " + mKKMInfo.getShift().getNumber());

                if (shift != null) getKKMInfo().getShift().cloneTo(shift);
                res.print = getKKMInfo().getShift().getPF(template);
                res.shift = mKKMInfo.getShift();

            } else {
                Logger.i("Error closing shift: 0x%02X", res.code);
                cancelDocument(transaction);
            }
        }
        return res;
    }


    public String getPrintDoc(String type, byte[] bb, ParcelableStrings templates) {
        String res = "";
        if (KKMInfo.CLASS_NAME.equals(type)) {
            res = Utils.deserialize(bb,
                    KKMInfoEx.CREATOR).getPF(templates.get(0));

        } else if (Shift.CLASS_NAME.equals(type)) {
            res = Utils.deserialize(bb,
                    ShiftEx.CREATOR).getPF(templates.get(0), mKKMInfo);

        } else if (ArchiveReport.CLASS_NAME.equals(type)) {
            res = Utils.deserialize(bb,
                    ArchiveReportEx.CREATOR).getPF(templates.get(0), mKKMInfo);

        } else if (Correction.CLASS_NAME.equals(type)) {
            res = Utils.deserialize(bb,
                    CorrectionEx.CREATOR).getPF(templates.get(0), mKKMInfo);

        } else if (SellOrder.CLASS_NAME.equals(type)) {
            res = Utils.deserialize(bb,
                    SellOrderEx.CREATOR).getPF(templates.get(0), templates.get(1), templates.get(2),
                    templates.get(3), mKKMInfo);

        } else if (FiscalReport.CLASS_NAME.equals(type)) {
            res = Utils.deserialize(bb,
                    FiscalReportEx.CREATOR).getPF(templates.get(0), mKKMInfo);
        }
        return res;
    }

    public Tag getDoc(String type, byte [] bb){
        Document doc=null;
        if (KKMInfo.CLASS_NAME.equals(type)) {
            doc=Utils.deserialize(bb, KKMInfoEx.CREATOR);

        } else if (Shift.CLASS_NAME.equals(type)) {
            doc=Utils.deserialize(bb, ShiftEx.CREATOR);

        } else if (ArchiveReport.CLASS_NAME.equals(type)) {
            doc=Utils.deserialize(bb, ArchiveReportEx.CREATOR);

        } else if (Correction.CLASS_NAME.equals(type)) {
            doc=Utils.deserialize(bb, CorrectionEx.CREATOR);

        } else if (SellOrder.CLASS_NAME.equals(type)) {
            doc=Utils.deserialize(bb, SellOrderEx.CREATOR);

        } else if (FiscalReport.CLASS_NAME.equals(type)) {
            doc=Utils.deserialize(bb, FiscalReportEx.CREATOR);
        }
        return doc;
    }

    public int readKKMInfo(KKMInfo result) {
        try (Transaction transaction = mStorage.open()) {
            mKKMInfo.update(transaction);
        }
        if (result != null) {
            mKKMInfo.cloneTo(result);
        }
        return Errors.NO_ERROR;
    }


    public int loadKKMInfo(int docNumber) {
        if(mKKMInfo == null) mKKMInfo = new KKMInfoEx();
		ByteBuffer bb = BufferFactory.allocateDocument();
		Tag doc = new Tag();
		try (Transaction transaction = mStorage.open()) {
			if (docNumber > 0) {
				if (readDocumentFromTLV(docNumber, doc,transaction) == Errors.NO_ERROR) {
					Document mInfo = doc.createInstance();
					if (mInfo instanceof KKMInfo) {
						mKKMInfo = new KKMInfoEx((KKMInfo) mInfo);
						mKKMInfo.update(transaction);
						return Errors.NO_ERROR;
					}
				}
			}
			mKKMInfo.update(transaction);
			for(long i =  mKKMInfo.getLastFNDocNumber();i>0;i--) {
				if(transaction.write(FNCommandsE.GET_FISCAL_DOC_IN_TLV_INFO, (int)i).getLastError() == Errors.NO_ERROR) {
					transaction.read(bb);
					int type = bb.getShort(); 
					if( type == 1 || type == 11) {
						if (readDocumentFromTLV(i, doc,transaction) == Errors.NO_ERROR) {
							Document mInfo = doc.createInstance();
							if (mInfo instanceof KKMInfo) {
								mKKMInfo = new KKMInfoEx((KKMInfo) mInfo);
								mKKMInfo.update(transaction);
								return Errors.NO_ERROR;
							}
						}
						break;
					}
				}
			}
			return Errors.NEW_FN;
		} finally {
			BufferFactory.release(bb);
			Logger.i("FN MGM status: %s", isMGM());
		}
    }


    public int updateOISMStatus(OismStatistic s) {
        return Errors.NO_ERROR;
    }

    public int updateOISMStatus(OismStatistic s, Transaction transaction, ByteBuffer b) {
        return Errors.NO_ERROR;
    }

    public byte[] readOISMDocument(ByteBuffer bb){
        return null;
    }

    public boolean writeOFDReplyOISM(ByteBuffer mDataBuffer, long docNo, byte[] answer){
        return true;
    }

    public int storeOfflineMarkNotify(){
        return Errors.NO_ERROR;
    }

    public void updateOKPServerSettings(DocServerSettings okpServer){
    }


    public CheckNeedUpdateKeysE isNeedUpdateOkpKeys(){
        return CheckNeedUpdateKeysE.NO_NEED_UPDATE;
    }

    @Override
    public FNCounters getFnCounters(Transaction transaction, boolean isTotal){
    	return FN2Commands.getFNCounters(transaction, isTotal);
    }

	@Override
	public int exportMarking(String file) {
		return Errors.NOT_IMPLEMENTED;
	}

	@Override
	public void releaseMarkCodes() {
		// TODO Auto-generated method stub
		
	} 



}
