package rs.fncore;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.KKMInfo.FiscalReasonE;
import rs.fncore.data.KKMInfo.FNConnectionModeE;
import rs.fncore.data.PrintFormTypeE;
import rs.fncore.data.OU;
import rs.fncore.data.Location;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore.data.OfdStatistic;
import rs.fncore.data.OismStatistic;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.ArchiveReport;
import rs.fncore.data.SellOrder;
import rs.fncore.data.SellItem;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.PrintSettings;
import rs.fncore.data.Correction;
import rs.fncore.data.ParcelableStrings;
import rs.fncore.data.ParcelableBytes;
import rs.fncore.data.FNCounters;

interface FiscalStorage  {
    int waitFnReady(in long waitFNtimeoutMs);
    int readKKMInfo(out KKMInfo info);
    int resetFN();

    int cancelDocument();
    int doFiscalization(in KKMInfo.FiscalReasonE reason, in OU operator, in KKMInfo info, out
    KKMInfo signed, String template);
    int toggleShift(in OU operator, out Shift shift, String template);
    int updateOfdStatistic(out OfdStatistic statistic);
    int requestFiscalReport(in OU operator, out FiscalReport report, String template);
    int doSellOrder(in SellOrder order, in OU operator, out SellOrder signed, boolean doPrint,
        String header, String item, String footer, String footerEx);
    int doArchive(in OU operator, out ArchiveReport report, String template);
    int doCorrection(in Correction correction, in OU operator, out Correction signed,
        String template);

    DocServerSettings getOFDSettings();
    void setOFDSettings(in DocServerSettings settings);

    PrintSettings getPrintSettings();
    void setPrintSettings(in PrintSettings settings);
    void doPrint(String text);

    void pushDocuments();

    int openTransaction();
    int writeB(int transaction, in byte [] data, int offset, int size);
    int readB(int transacrion, out byte [] data, int offset, int size);
    void closeTransaction(int transaction);

    int printExistingDocument(int number,in ParcelableStrings template,boolean doPrint, out ParcelableBytes result);
    double getCashRest();
    int putOrWithdrawCash(double v, in OU operator, String template);
    void setCashControl(boolean  val);
    boolean isCashControlEnabled();

    boolean isKeepRest();
    void setKeepRest(boolean val);
    int restartCore();
    void setConnectionMode(in KKMInfo.FNConnectionModeE val);
    KKMInfo.FNConnectionModeE getConnectionMode();
    int openShift(in OU operator, out Shift shift, String template);
    int closeShift(in OU operator, out Shift shift, String template);

    boolean isMGM();
    String execute(String action, String argsJson);

    int updateOismStatistic(out OismStatistic statistic);
    DocServerSettings getOismSettings();
    void setOismSettings(in DocServerSettings settings);

    DocServerSettings getOKPSettings();
    void setOKPSettings(in DocServerSettings settings);

    int exchangeB(in byte [] inData, out byte [] outData, long waitTimeoutMs);


    int checkMarkingItem(inout SellItem item);
    int confirmMarkingItem(inout SellItem item, boolean accepted);

    int checkMarkingItemLocalFN(inout SellItem item);
    int checkMarkingItemOnlineOISM(inout SellItem item);

    int getExistingDocument(int docNo, out rs.fncore.data.Tag doc);

    int doCorrection2(in Correction correction, in OU operator, out Correction signed,
                    String header, String item, String footer, String footerEx);
    String getCheckSum();
    String getPF(in Tag tag);
    int getFNCounters(out FNCounters counters, boolean shiftCounters);
    int exportMarking(String file);

    long getPaperConsume();
    void resetPaperCounter();
    void setUSBMonitorMode(boolean enabled);
    void setDocumentPayload(int number, in byte [] payload);
    byte [] getDocumentPayload(int number);
	boolean isFNOK();

}
