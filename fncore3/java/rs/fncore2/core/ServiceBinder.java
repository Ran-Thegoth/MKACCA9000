package rs.fncore2.core;

import android.os.Build;
import android.os.RemoteException;
import android.os.StrictMode;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.ArchiveReport;
import rs.fncore.data.Correction;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OfdStatistic;
import rs.fncore.data.OismStatistic;
import rs.fncore.data.OU;
import rs.fncore.data.ParcelableBytes;
import rs.fncore.data.ParcelableStrings;
import rs.fncore.data.PrintSettings;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore2.FNCore;
import rs.fncore2.JsonCommands;
import rs.fncore2.fn.FNManager;
import rs.log.Logger;
import rs.utils.Utils;
import rs.fncore2.utils.DocumentUtils;
import rs.fncore2.utils.UrovoUtils;

public class ServiceBinder extends FiscalStorage.Stub {

    private final boolean logCalls=true;

    private final ServiceBase mMainService;
    private final JsonCommands mJsonCommands;

    void logMethodName(){
        if (logCalls) {
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            Logger.i("call %s", methodName);
        }
    }

    public ServiceBinder(ServiceBase mainService) {
        logMethodName();
        this.mMainService = mainService;
        mJsonCommands = new JsonCommands(this);
    }

    @Override
    public synchronized int cancelDocument() {
        try {
            logMethodName();
            return mMainService.cancelDocument();
        }
        catch (Exception e){
            Logger.e(e, "Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized void closeTransaction(int id) {
        try {
            logMethodName();
            mMainService.closeTransaction(id);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public synchronized int doArchive(OU operator, ArchiveReport report, String template) {
        try {
            logMethodName();
            return mMainService.doArchive(operator, report, template);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int doFiscalization(KKMInfo.FiscalReasonE reason, OU operator, KKMInfo info, KKMInfo signed, String template) {
        try {
            logMethodName();
            if(!Utils.checkINN(info.getOwner().getINN())) return Errors.WRONG_INN;
            if(!Utils.checkRegNo(info.getKKMNumber(), info.getOwner().getINN(), FNCore.getInstance().getDeviceSerial()))
            	return Errors.DATA_ERROR;
            return mMainService.doFiscalization(reason, operator, info, signed, template);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int doCorrection(Correction cor, OU operator,
                                         Correction signed, String template) {
        try {
            logMethodName();
            return mMainService.doCorrection(cor, operator, signed, template);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int doCorrection2(Correction cor, OU operator,
                                         Correction signed, String header, String item,
                                         String footer, String footerEx) {
        try {
            logMethodName();
            return mMainService.doCorrection2(cor, operator, signed, header, item, footer, footerEx);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int doSellOrder(SellOrder order, OU operator,
                                        SellOrder signed, boolean doPrint, String header, String item,
                                        String footer, String footerEx) {
        try {
            logMethodName();
            return mMainService.doSellOrder(order, operator, signed, doPrint, header, item, footer,
                    footerEx);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized void doPrint(String s) {
        try {
            logMethodName();
            mMainService.doPrint(s);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }


    @Override
    public synchronized double getCashRest() {
        try {
            logMethodName();
            return mMainService.getCashRest();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return -1;
    }

    @Override
    public synchronized DocServerSettings getOFDSettings() {
        try {
            logMethodName();
            return mMainService.mSettings.getOFDServer();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return null;
    }

    @Override
    public synchronized void setOFDSettings(DocServerSettings s) {
        try {
            logMethodName();
            mMainService.setOFDSettings(s);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public synchronized DocServerSettings getOismSettings() {
        try {
            logMethodName();
            return mMainService.mSettings.getOISMServer();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return null;
    }

    @Override
    public synchronized void setOismSettings(DocServerSettings s) {
        try {
            logMethodName();
            mMainService.setOismSettings(s);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public synchronized DocServerSettings getOKPSettings() {
        try {
            logMethodName();
            return Settings.getInstance().getOKPServer();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return null;
    }

    @Override
    public synchronized void setOKPSettings(DocServerSettings s) {
        try {
            logMethodName();
            mMainService.setOKPSettings(s);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public synchronized PrintSettings getPrintSettings() {
        try {
            logMethodName();
            return mMainService.mSettings.getPrintSettings();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return null;
    }

    @Override
    public synchronized void setPrintSettings(PrintSettings s) {
        try {
            logMethodName();
            mMainService.setPrintSettings(s);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public boolean isMGM() {
        try {
            if (FNManager.getInstance().getFN()!=null) {
                return FNManager.getInstance().getFN().isMGM();
            }
            else return false;
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return false;
    }

    @Override
    public String execute(String action, String argsJson) {
        try {logMethodName();
            return mJsonCommands.execute(action, argsJson);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return null;
    }

    @Override
    public synchronized int waitFnReady(long waitFNtimeoutMs) {
        try {
            logMethodName();
            return mMainService.waitFnReady(waitFNtimeoutMs);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized boolean isCashControlEnabled() {
        try {
            logMethodName();
            return mMainService.mSettings.isCashControlFlag();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return false;
    }

    @Override
    public synchronized KKMInfo.FNConnectionModeE getConnectionMode() {
        try {
            logMethodName();
            return mMainService.mSettings.getConnectionMode();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return KKMInfo.FNConnectionModeE.UNKNOWN;
    }

    @Override
    public synchronized void setConnectionMode(KKMInfo.FNConnectionModeE mode) {
        try {
            logMethodName();
            mMainService.setConnectionMode(mode);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public synchronized boolean isKeepRest() {
        try {
            logMethodName();
            return mMainService.mSettings.isKeepRestFlag();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return false;
    }

    @Override
    public synchronized void setKeepRest(boolean value) {
        try {
            logMethodName();
            Logger.i("Set keep rest flag: %s", value);
            mMainService.mSettings.setKeepRestFlag(value);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public synchronized int openTransaction() {
        try {
            logMethodName();
            return mMainService.openTransaction();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int printExistingDocument(int docNo, ParcelableStrings templates, boolean doprint,
                                                  ParcelableBytes image) {
        try {
            logMethodName();
            return mMainService.printExistingDocument(docNo, templates, doprint, image);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int getExistingDocument(int docNo, Tag doc) {
        try {
            logMethodName();
            return mMainService.getExistingDocument(docNo, doc);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized void pushDocuments() {
        try {
            logMethodName();
            mMainService.mOfdSender.sendNowDocs();
            mMainService.mOismSender.sendNowDocs();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public synchronized int putOrWithdrawCash(double value, OU operator, String template) {
        try {
            logMethodName();
            return mMainService.putOrWithdrawCash(value, operator, template);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int readB(int id, byte[] data, int offset, int len) {
        try {
            logMethodName();
            return mMainService.readB(id, data, offset, len);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int readKKMInfo(KKMInfo result) {
        try {
            logMethodName();
            return mMainService.readKKMInfo(result);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int requestFiscalReport(OU operator, FiscalReport report, String template) {
        try {
            logMethodName();
            return mMainService.requestFiscalReport(operator, report, template);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int resetFN() {
        try {
            logMethodName();
            return mMainService.resetFN();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int restartCore() {
        try {
            logMethodName();
            return mMainService.restartCore();
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized void setCashControl(boolean value) {
        try {
            logMethodName();
            Logger.i("Set cash control flag: %s", value);
            mMainService.mSettings.setCashControlFlag(value);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
    }

    @Override
    public synchronized int openShift(OU operator, Shift shift, String template) {
        try {
            logMethodName();
            return mMainService.openShift(operator, shift, template);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int closeShift(OU operator, Shift shift, String template) {
        try {
            logMethodName();
            return mMainService.closeShift(operator, shift, template);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int toggleShift(OU operator, Shift shift, String template) {
        try {
            logMethodName();
            return mMainService.toggleShift(operator, shift, template);
        }
        catch (Exception e){
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int writeB(int id, byte[] data, int offset, int len) {
        try {
            logMethodName();
            return mMainService.writeB(id, data, offset, len);
        }
        catch (Exception e){
            Logger.e(e, "Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int exchangeB(byte [] inData, byte [] outData, long waitTimeoutMs) {
        try {
            logMethodName();
            return mMainService.exchangeB(inData, outData, waitTimeoutMs);
        }
        catch (Exception e){
            Logger.e(e, "Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int updateOfdStatistic(OfdStatistic s) {
        try {
            logMethodName();
            return mMainService.updateOfdStatistic(s);
        }
        catch (Exception e){
            Logger.e(e, "Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public synchronized int updateOismStatistic(OismStatistic s) {
        try {
            logMethodName();
            return mMainService.updateOismStatistic(s);
        }
        catch (Exception e){
            Logger.e(e, "Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public int checkMarkingItem(SellItem item){
        try {
            //TODO: fix in future for thread safe operation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
            return mMainService.checkMarkingItem(item);
        } catch (Exception e) {
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public int checkMarkingItemLocalFN(SellItem item){
        try {
            return mMainService.checkMarkingItemLocalFN(item);
        } catch (Exception e) {
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public int checkMarkingItemOnlineOISM(SellItem item){
        try {
            return mMainService.checkMarkingItemOnlineOISM(item);
        } catch (Exception e) {
            Logger.e(e,"Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }

    @Override
    public int confirmMarkingItem(SellItem item, boolean accepted){
        try {
            return mMainService.confirmMarkingItem(item, accepted);
        } catch (Exception e) {
            Logger.e(e, "Unhandled exception");
        }
        return Errors.UNHANDLED_EXCEPTION;
    }



	@Override
	public String getCheckSum() throws RemoteException {
		return FNCore.getInstance().checksum();
	}

	
	@Override
	public String getPF(Tag tag) throws RemoteException {
		return DocumentUtils.getPrintForm(tag);
	}

	@Override
	public int getFNCounters(FNCounters doc, boolean isShiftCounters) throws RemoteException {
		return mMainService.getFNCounters(doc,isShiftCounters);
	}

	@Override
	public int exportMarking(String file) throws RemoteException {
		return mMainService.exportMarking(file);
	}

	@Override
	public long getPaperConsume() throws RemoteException {
		return mMainService.getPaperConsume();
	}

	@Override
	public void resetPaperCounter() throws RemoteException {
		mMainService.resetPaperCounter();
		
	}


	@Override
	public void setUSBMonitorMode(boolean enabled) throws RemoteException {
		mMainService.USB_MONITOR = enabled;
		if(enabled &&UrovoUtils.isUSBFN())
			rs.fncore.UrovoUtils.switchOTG(true);
	}
}
