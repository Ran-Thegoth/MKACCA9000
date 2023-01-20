package rs.fncore2.core;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.TimeZone;

import rs.fncore.Errors;
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
import rs.fncore3.R;
import rs.fncore2.core.utils.NotificationsHelper;
import rs.fncore2.data.CashWithdraw;
import rs.fncore2.fn.FNFactory;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.FNNotSupported;
import rs.fncore2.fn.NotSupportedFFDException;
import rs.fncore2.fn.common.FNBaseI;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.fn.storage.serial.SStorage;
import rs.fncore2.fn.storage.virtual.VStorage;
import rs.fncore2.io.DocumentsRestore;
import rs.fncore2.io.OFDSender;
import rs.fncore2.io.OISMSender;
import rs.fncore2.io.Printing;
import rs.fncore2.io.WebServer;
import rs.fncore2.utils.UrovoUtils;
import rs.fncore2.utils.WakeLockPower;
import rs.utils.Utils;
import rs.log.Logger;

class ServiceBase extends Service {

	protected OFDSender mOfdSender;
	protected OISMSender mOismSender;
	private DocumentsRestore mDocRestore;

	protected Settings mSettings;
	protected Printing mPrinter;
	protected ServiceBinder mBinder;
	protected WakeLockPower mWakelockPower;
	public boolean USB_MONITOR = true;

	public final FNManager mFNManager = FNManager.getInstance();
	private WebServer server;

	@Override
	public void onCreate() {
		super.onCreate();

		// android.os.Debug.waitForDebugger(); //TODO: remove at release

		mWakelockPower = new WakeLockPower("ServiceBase");
		mWakelockPower.acquireWakeLock();

		SharedPreferences mSp = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
		mSettings = new Settings(mSp);
		mPrinter = new Printing(this);
		mBinder = new ServiceBinder(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		mWakelockPower.releaseWakeLock();
		NotificationsHelper.clear(this);
		super.onDestroy();
	}

	private void stopServerThread(Thread thread) {
		if (thread == null)
			return;

		try {
			thread.interrupt();
			Logger.i("Stopping thread: %s", thread.getName());
			thread.join();
		} catch (Exception | Error e) {
			Logger.e(e, "%s destroy exc: ", thread.getName());
		}
	}

	protected void destroyStorage() {
		Logger.i("Destroy storage");

		if(server != null)
			server.stop();
		server = null;
		stopServerThread(mOismSender);
		mOismSender = null;
		stopServerThread(mDocRestore);
		mDocRestore = null;
		stopServerThread(mOfdSender);
		mOfdSender = null;

		Logger.i("Destroy storage: " + mFNManager.getStorageName());
		mFNManager.emptyFN();
		Logger.i("Storage destroyed");
	}

	protected int buildStorage() {
		Logger.i("Build storage");
		StorageI storage;
		switch (mSettings.getConnectionMode()) {
		case CLOUD:
		case VIRTUAL:
			storage = new VStorage(this);
			break;
		case USB:
		case UART:
			storage = new SStorage();
			break;
		default:
			Logger.e("unknown connection mode: %s", mSettings.getConnectionMode());
			return Errors.UNKNOWN_CONNECTION_MODE;
		}
		try {
			if (storage.isReady()) {
				FNBaseI newFN = FNFactory.getFN(storage);
				if (newFN != null)
					try {
						if (newFN.loadKKMInfo(mSettings.getKKMInfoNumber(newFN)) == Errors.NO_ERROR)
							mSettings.updateKKMInfo(newFN.getKKMInfo().signature().getFdNumber(), newFN);
						newFN.setConnectionMode(mSettings.getConnectionMode());
					} catch (NotSupportedFFDException e) {
						newFN = new FNNotSupported(storage);
					}
				int res = mFNManager.setFN(newFN);

				if (res == Errors.NO_ERROR) {
					server = new WebServer();
					server.start();
					mOfdSender = new OFDSender(mSettings);
					mDocRestore = new DocumentsRestore();
					mOismSender = new OISMSender(mSettings, this);

					Logger.i("buildStorage end");
				} else {
					Logger.i("buildStorage failed " + res);
					mFNManager.emptyFN();
					return res;
				}
				Logger.i("Storage built: " + storage);
			} else {
				Logger.e("Storage built error, storage not ready ");
				return Errors.DEVICE_ABSEND;
			}
			int r = waitFnReady(100);
			return r;
		} finally {
			sendBroadcast(new Intent("fncore.ready"));
		}

	}

	protected int waitFnReady(long waitFNtimeoutMs) {
		Logger.d("checkFN start %s", waitFNtimeoutMs);
		try {
			if (!mFNManager.waitFNReady(waitFNtimeoutMs)) {
				Logger.i("checkFN empty");
				return Errors.DEVICE_ABSEND;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Logger.d("checkFN end");
		return Errors.NO_ERROR;
	}

	protected int setConnectionMode(KKMInfo.FNConnectionModeE connMode) {
		Logger.i("Переключение режима подключения ФН");
		destroyStorage();

		mSettings.setConnectionMode(connMode);
		switch (mSettings.getConnectionMode()) {
		case CLOUD:
		case VIRTUAL:
			mSettings.setFnTAG(VStorage.class.getSimpleName());
			VStorage.clear(this);
			break;
		case USB:
		case UART:
			mSettings.setFnTAG(SStorage.class.getSimpleName());
			break;
		default:
			return Errors.UNKNOWN_CONNECTION_MODE;
		}
		return buildStorage();
	}

	protected int restartCore() {
		if (UrovoUtils.getUART() == 2)
			rs.fncore.UrovoUtils.switchOTG(true);
		destroyStorage();
		return buildStorage();
	}

	protected int doArchive(OU operator, ArchiveReport report, String template) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		FNBaseI.doPrintResult res = mFNManager.getFN().doArchive(operator, report, template);
		if (res.code == Errors.NO_ERROR) {
			doPrint(res.print);
		}
		return res.code;
	}

	/*
	 * protected void printFnCounters(boolean total){ if (!total &&
	 * mFNManager.getFN().getKKMInfo().getFFDProtocolVersion().bVal<
	 * KKMInfo.FFDVersionE.VER_12.bVal) { Logger.i("Print XReport: " + mXReport);
	 * doPrint(mXReport.getPF(null)); return; }
	 * 
	 * FnCountersExBase fnCounters=mFNManager.getFN().getFnCounters(total); if
	 * (fnCounters!=null) { doPrint(fnCounters.getPF(null)); } }
	 */

	protected int doFiscalization(KKMInfo.FiscalReasonE reason, OU operator, KKMInfo info, KKMInfo signed,
			String template) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;
//        boolean wasActiveBefore = mFNManager.getFN().getKKMInfo().isFNActive();

		FNBaseI.doPrintResult res = mFNManager.getFN().doFiscalization(reason, operator, info, signed, template);
		if (res.code == Errors.NO_ERROR) {
			mSettings.updateKKMInfo(signed.signature().getFdNumber(), mFNManager.getFN());
			mFNManager.notifyFNReady();
			doPrint(res.print);

			/*
			 * if (wasActiveBefore && reason!=KKMInfo.FiscalReasonE.REPLACE_FN) {
			 * printFnCounters(true); }
			 */

			forceSendOFD();
		}
		return res.code;
	}

	protected void doPrint(String s) {
		mPrinter.queue(s);
	}

	public String convertTimeWithTimeZome(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(time);
		return (cal.get(Calendar.YEAR) + " " + (cal.get(Calendar.MONTH) + 1) + " " + cal.get(Calendar.DAY_OF_MONTH)
				+ " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));

	}

	protected int doCorrection(Correction cor, OU operator, Correction signed, String template) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		FNBaseI.doCorrectionResult res = mFNManager.getFN().doCorrection(cor, operator, signed, template);
		if (res.code == Errors.NO_ERROR) {
			doPrint(res.print);
		}
		return res.code;
	}

	protected int doCorrection2(Correction cor, OU operator, Correction signed, String header, String item,
			String footer, String footerEx) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		FNBaseI.doCorrectionResult res = mFNManager.getFN().doCorrection2(cor, operator, signed, header, item, footer,
				footerEx);
		if (res.code == Errors.NO_ERROR) {
			doPrint(res.print);
		}
		return res.code;
	}

	protected int doSellOrder(SellOrder order, OU operator, SellOrder signed, boolean is_doPrint, String header,
			String item, String footer, String footerEx) {
		final long DAY = 24 * 60 * 60000L;

		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;
		if (!mFNManager.getFN().getKKMInfo().getShift().isOpen())
			return Errors.INVALID_SHIFT_STATE;

		long openShiftTime = System.currentTimeMillis() - mFNManager.getFN().getKKMInfo().getShift().getWhenOpen();
		if (openShiftTime > DAY) {
			Logger.e("Shift opened too much time: %s",
					convertTimeWithTimeZome(mFNManager.getFN().getKKMInfo().getShift().getWhenOpen()));
			return Errors.INVALID_SHIFT_STATE;
		}

		FNBaseI.doSellOrderResult res = mFNManager.getFN().doSellOrder(order, operator, signed, is_doPrint, header,
				item, footer, footerEx, mSettings.isCashControlFlag());
		if (res.code == Errors.NO_ERROR) {
			if (is_doPrint)
				doPrint(res.print);
		}
		return res.code;
	}

	public int checkMarkingItem(SellItem item) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		int res = mFNManager.getFN().checkMarkingItem(item, mSettings.getOISMServer());
		return res;
	}

	public int checkMarkingItemLocalFN(SellItem item) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		int res = mFNManager.getFN().checkMarkingItemLocalFN(item);
		return res;
	}

	public int checkMarkingItemOnlineOISM(SellItem item) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		int res = mFNManager.getFN().checkMarkingItemOnlineOISM(item, mSettings.getOISMServer());
		return res;
	}

	public int confirmMarkingItem(SellItem item, boolean accepted) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		int res = mFNManager.getFN().confirmMarkingItem(item, accepted);
		return res;
	}

	protected int requestFiscalReport(OU operator, FiscalReport report, String template) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		FNBaseI.doPrintResult res = mFNManager.getFN().requestFiscalReport(operator, report, template);
		if (res.code == Errors.NO_ERROR) {
			doPrint(res.print);
		}
		return res.code;
	}

	protected double getCashRest() {
		return mSettings.getCashRest(mFNManager.getFN().getNumber());
	}

	protected void setOFDSettings(DocServerSettings s) {
		if (s == null)
			return;
		try {
			mSettings.setOFDServer(Utils.deserialize(Utils.serialize(s), DocServerSettings.CREATOR));
		} catch (Exception e) {
			Logger.e(e, "Ошибка сохранения настроек ОФД");
		}
	}

	protected void setOismSettings(DocServerSettings s) {
		if (s == null)
			return;
		try {
			mSettings.setOISMServer(Utils.deserialize(Utils.serialize(s), DocServerSettings.CREATOR));
		} catch (Exception e) {
			Logger.e(e, "Ошибка сохранения настроек ОИСМ");
		}
	}

	protected void setOKPSettings(DocServerSettings s) {
		if (s == null)
			return;
		try {
			mSettings.setOKPServer(s);
		} catch (Exception e) {
			Logger.e(e, "Ошибка сохранения настроек ОКП");
		}
	}

	protected void setPrintSettings(PrintSettings s) {
		if (s == null)
			return;
		try {
			mSettings.setPrintSettings(Utils.deserialize(Utils.serialize(s), PrintSettings.CREATOR));
		} catch (Exception e) {
			Logger.e(e, "Ошибка сохранения настроек печати");
		}
	}

	protected int cancelDocument() {
		if (mFNManager.getFN() == null)
			return 0;
		return mFNManager.getFN().cancelDocument();
	}

	protected int openTransaction() {
		return mFNManager.getFN().openTransaction();
	}

	protected void closeTransaction(int id) {
		mFNManager.getFN().closeTransaction(id);
	}

	protected int readB(int id, byte[] data, int offset, int len) {
		return mFNManager.getFN().readB(id, data, offset, len);
	}

	protected int writeB(int id, byte[] data, int offset, int len) {
		return mFNManager.getFN().writeB(id, data, offset, len);
	}

	protected int exchangeB(byte[] inData, byte[] outData, long waitTimeoutMs) throws InterruptedException {
		int transaction = mFNManager.getFN().openTransaction();
		if (transaction == 0)
			return Errors.DEVICE_ABSEND;

		try {
			int res = mFNManager.getFN().writeB(transaction, inData, 0, inData.length);
			if (res != inData.length)
				return Errors.DEVICE_ABSEND;

			Thread.sleep(waitTimeoutMs);

			return mFNManager.getFN().readB(transaction, outData, 0, outData.length);
		} finally {
			mFNManager.getFN().closeTransaction(transaction);
		}
	}

	protected int readKKMInfo(KKMInfo result) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		return mFNManager.getFN().readKKMInfo(result);
	}

	protected int printExistingDocument(int docNo, ParcelableStrings templates, boolean doprint,
			ParcelableBytes image) {
		return Errors.NOT_SUPPORTED;
	}

	protected int getExistingDocument(int docNo, Tag returnDoc) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;
		return mFNManager.getFN().readDocumentFromTLV(docNo, returnDoc);
	}

	protected int resetFN() {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		Logger.i("Clear documents database");
		FNCore.getInstance().getDB().clear();

		int res = mFNManager.getFN().resetFN();

		mSettings.updateKKMInfo(0, mFNManager.getFN());
		restartCore();

		Logger.i("Done reset MGM fully");
		return res;
	}

	int openShift(OU operator, Shift shift, String template) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		FNBaseI.doShiftResult res = mFNManager.getFN().openShift(operator, shift, template, this);
		if (res.code == Errors.NO_ERROR) {
			if (res.alreadyDone)
				return res.code;

			doPrint(res.print);

		}
		return res.code;
	}

	private void forceSendOFD() {
		mOfdSender.sendNowDocs();
		mOismSender.sendNowDocs();
	}

	protected int closeShift(OU operator, Shift shift, String template) throws RemoteException {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		if (mFNManager.getFN().getKKMInfo().getShift().isOpen()) {
			if (!mSettings.isKeepRestFlag()) {
				double cashboxValue = mSettings.getCashRest(mFNManager.getFN().getNumber());
				if (Math.abs(cashboxValue) >= 0.01)
					putOrWithdrawCash(-cashboxValue, operator, null);
			}
		}

		FNBaseI.doShiftResult res = mFNManager.getFN().closeShift(operator, shift, template);
		if (res.code == Errors.NO_ERROR) {
			if (res.alreadyDone)
				return res.code;
			doPrint(res.print);

			forceSendOFD();
		}
		return res.code;
	}

	protected int toggleShift(OU operator, Shift shift, String template) throws RemoteException {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		if (mFNManager.getFN().getKKMInfo().getShift().isOpen())
			return closeShift(operator, shift, template);
		return openShift(operator, shift, template);
	}

	protected int putOrWithdrawCash(double value, OU operator, String template) throws RemoteException {
		if (Math.abs(value) < 0.01)
			return Errors.SUM_MISMATCH;

		double cashBoxValue = mSettings.getCashRest(mFNManager.getFN().getNumber());
		if ((mSettings.isCashControlFlag()) && ((value + cashBoxValue) < 0)) {
			return Errors.NO_CASH;
		}
		cashBoxValue += value;
		mSettings.setCashRest(mFNManager.getFN().getNumber(), cashBoxValue);

		doPrint(new CashWithdraw(new BigDecimal(value), mFNManager.getFN().getKKMInfo(), operator).getPF());

		return Errors.NO_ERROR;
	}

	protected int updateOfdStatistic(OfdStatistic s) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		return mFNManager.getFN().updateOFDStatus(s);
	}

	protected int updateOismStatistic(OismStatistic s) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;

		return mFNManager.getFN().updateOISMStatus(s);
	}

	protected int storeOfflineMarkNotify() throws FileNotFoundException {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;
		return mFNManager.getFN().storeOfflineMarkNotify();
	}

	public int getFNCounters(FNCounters doc, boolean isShiftCounters) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;
		try (Transaction transaction = mFNManager.getFN().getStorage().open()) {
			FNCounters result = mFNManager.readCounters(transaction, !isShiftCounters);
			if (result == null)
				return Errors.DATA_ERROR;
			Utils.readFromParcel(doc, Utils.writeToParcel(result));
			return Errors.NO_ERROR;
		}

	}

	public int exportMarking(String file) {
		if (!mFNManager.isFNReady())
			return Errors.DEVICE_ABSEND;
		return mFNManager.getFN().exportMarking(file);
	}

	public long getPaperConsume() {
		return mPrinter.getPaperCount();
	}

	public void resetPaperCounter() {
		mPrinter.resetPrinterCounter();

	}

	public boolean isFNOK() {
		return mFNManager.getFN() != null;
	}

}
