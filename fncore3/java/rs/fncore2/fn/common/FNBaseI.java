package rs.fncore2.fn.common;

import android.content.Context;

import java.nio.ByteBuffer;

import rs.fncore.Errors;
import rs.fncore.data.ArchiveReport;
import rs.fncore.data.CheckNeedUpdateKeysE;
import rs.fncore.data.Correction;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OfdStatistic;
import rs.fncore.data.OismStatistic;
import rs.fncore.data.OU;
import rs.fncore.data.ParcelableStrings;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;

public interface FNBaseI {
    int readKKMInfo(KKMInfo result);
    int loadKKMInfo(int number);
    KKMInfoExBase getKKMInfo();

    void setConnectionMode(KKMInfo.FNConnectionModeE mode);

    int cancelDocument();
    //region Transaction
    int openTransaction();
    int readB(int id, byte[] data, int offset, int len);
    int writeB(int id, byte[] data, int offset, int len);
    void closeTransaction(int id);
    //endregion

    int resetFN();
    boolean isMGM();

    //region OFD
    int updateOFDStatus(OfdStatistic s);
    boolean writeOFDReply(ByteBuffer mDataBuffer, long docNo, byte[] answer);
    byte[] readOFDDocument(ByteBuffer mDataBuffer) throws Exception;
    boolean setOFDExchangeMode(boolean on);

    int exportMarking(String file);
    int updateOISMStatus(OismStatistic s);
    int updateOISMStatus(OismStatistic s, Transaction transaction, ByteBuffer bb);
    byte[] readOISMDocument(ByteBuffer bb);
    boolean writeOFDReplyOISM(ByteBuffer mDataBuffer, long docNo, byte[] answer);

    FNCounters getFnCounters(Transaction transaction, boolean isTotal);

    int storeOfflineMarkNotify();
    //endregion
    int readDocumentFromTLV(long documentNumber, Tag document);

    //region documents
    doPrintResult doArchive(OU operator, ArchiveReport report, String template);
    doPrintResult doFiscalization(KKMInfo.FiscalReasonE reason, OU operator, KKMInfo info, KKMInfo signed,
                                  String template);
    doCorrectionResult doCorrection(Correction cor, OU operator, Correction signed, String template);
    doCorrectionResult doCorrection2(Correction cor, OU operator, Correction signed, String header, String item,
                                    String footer, String footerEx);
    doSellOrderResult doSellOrder(SellOrder order, OU operator,
                                           SellOrder signed, boolean doPrint, String header, String item,
                                           String footer, String footerEx, boolean cashControlFlag);
    doPrintResult requestFiscalReport(OU operator, FiscalReport report, String template);
    doShiftResult openShift(OU operator, Shift shift, String template, Context ctx);
    doShiftResult closeShift(OU operator, Shift shift, String template);

    int checkMarkingItem(SellItem item, DocServerSettings oismSettings);
    int confirmMarkingItem(SellItem item, boolean accepted);

    int checkMarkingItemLocalFN(SellItem item);
    int checkMarkingItemOnlineOISM(SellItem item, DocServerSettings oismSettings);
    void releaseMarkCodes();
    CheckNeedUpdateKeysE isNeedUpdateOkpKeys();


    String getPrintDoc(String type, byte [] bb, ParcelableStrings templates);
    Tag getDoc(String type, byte [] bb);

    class doPrintResult {
        public int code = Errors.NO_ERROR;
        public String print;
        public boolean check(){
            return code==Errors.NO_ERROR;
        }
    }

    class doCorrectionResult extends doPrintResult{
        public Correction cor;
    }

    class doSellOrderResult extends doPrintResult{
        public SellOrder sell;
    }

    class doShiftResult extends doPrintResult{
        public Shift shift;
        public boolean alreadyDone;
    }

    void Destroy();
    StorageI getStorage();
	String getNumber();
}
