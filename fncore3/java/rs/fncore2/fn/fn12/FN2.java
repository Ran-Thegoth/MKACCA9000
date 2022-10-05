package rs.fncore2.fn.fn12;

import static rs.fncore.Errors.CHECK_MARK_ITEM_REJECTED;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.fn12.marking.MarkingOfflineReport;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;
import rs.log.Logger;

public class FN2 extends FN2Commands {

	public FN2(StorageI storage) {
		super(storage);
		mKKMInfo = new KKMInfoEx2();
		Logger.i("creating FN 1.2");
	}

	public final KKMInfoEx2 getKKMInfo() {
		return (KKMInfoEx2) mKKMInfo;
	}

	public boolean isMGM() {
		return getKKMInfo().isMGM();
	}

	protected int cancelDocument(Transaction transaction) {
		return cancelDocument(transaction, getKKMInfo());
	}


	public doPrintResult doArchive(OU operator, ArchiveReport report, String template) {
		Logger.i("Начата архивация");
		try (Transaction transaction = mStorage.open()) {
			doPrintResult res = new doPrintResult();

			ArchiveReportEx2 helper = new ArchiveReportEx2(getKKMInfo());
			res.code = helper.write(transaction, operator);
			Logger.i("Архивация завершена, результат 0x%02X", res.code);

			if (res.code == Errors.NO_ERROR) {
				mKKMInfo.read(transaction);

				if (report != null)
					helper.cloneTo(report);
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
			info.setFFDProtocolVersion(KKMInfo.FFDVersionE.VER_12);
			doPrintResult res = new doPrintResult();

			KKMInfoEx2 newInfo;
			if (reason != KKMInfo.FiscalReasonE.CHANGE_INN) {
				newInfo = new KKMInfoEx2(info);
				mKKMInfo.cloneSavedStatus(newInfo);
				res.code = newInfo.write(reason, transaction, operator, mKKMInfo);
				Logger.i("Фискализация завершена, результат 0x%02X", res.code);
			} else {
				newInfo = new KKMInfoEx2(mKKMInfo);
				String currInn = mKKMInfo.getOwner().getINN();
				if (template.length() == 10) {
					// move to entity user
					if (currInn.length() != 12 || !currInn.startsWith("00")
							|| !currInn.substring(2, 12).equals(template)) {
						res.code = Errors.WRONG_INN;
					} else {
						template = template + "  ";
					}
				} else if (template.length() == 12) {
					if (currInn.trim().length() != 10 || !template.startsWith("00")
							|| !template.substring(2, 12).equals(currInn)) {
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
				if (signed != null)
					mKKMInfo.cloneTo(signed);
				res.print = newInfo.getPF(template);
			} else {
				if (signed != null)
					newInfo.cloneTo(signed);
				cancelDocument(transaction);
			}
			return res;
		} finally {
		}
	}

	public doCorrectionResult doCorrection(Correction cor, OU operator, Correction signed, String template) {
		return doCorrection2(cor, operator, signed, template, template, template, template);

	}

	public doCorrectionResult doCorrection2(Correction cor, OU operator, Correction signed, String header, String item,
			String footer, String footerEx) {
		Logger.i("Начата коррекция");
		try (Transaction transaction = mStorage.open()) {
			doCorrectionResult res = new doCorrectionResult();
			CorrectionEx2 helper = new CorrectionEx2(getKKMInfo(), cor);
			res.code = helper.write(transaction, operator);
			Logger.i("Коррекция завершена, результат 0x%02X", res.code);

			if (res.code == Errors.NO_ERROR) {
				if (signed != null)
					helper.cloneTo(signed);

				res.print = helper.getPF(header, item, footer, footerEx);
				res.cor = helper;
			} else {
				cancelDocument(transaction);
				if (cor.haveMarkingItems()) {
					if (!clearMarkingResults(transaction)) {
						Logger.e("error clear marking results %d", transaction.getLastError());
					}
				}
			}
			return res;
		}
	}

	public doSellOrderResult doSellOrder(SellOrder order, OU operator, SellOrder signed, boolean doPrint, String header,
			String item, String footer, String footerEx, boolean cashControlFlag) {
		Logger.i("Начат чек продаж");

		try (Transaction transaction = mStorage.open()) {
			doSellOrderResult res = new doSellOrderResult();

			SellOrderEx2 helper = new SellOrderEx2(getKKMInfo(), order);
			res.code = helper.write(transaction, operator, cashControlFlag);
			Logger.i("Чек продаж завершен, результат 0x%02X", res.code);

			if (res.code == Errors.NO_ERROR) {
				if (signed != null)
					helper.cloneTo(signed);
				if (doPrint)
					res.print = helper.getPF(header, item, footer, footerEx);
				res.sell = helper;
			} else {
				cancelDocument(transaction);
				if (order.haveMarkingItems()) {
					if (!clearMarkingResults(transaction)) {
						Logger.e("error clear marking results %d", transaction.getLastError());
					}
				}
			}
			return res;
		}
	}

	public doPrintResult requestFiscalReport(OU operator, FiscalReport report, String template) {
		Logger.i("Запрос отчета о состоянии расчетов");

		try (Transaction transaction = mStorage.open()) {
			doPrintResult res = new doPrintResult();

			FiscalReportEx2 signed = new FiscalReportEx2(mKKMInfo);
			res.code = signed.write(transaction, operator);
			Logger.i("Запрос выполнен, результат 0x%02X", res.code);

			if (res.code == Errors.NO_ERROR) {
				if (report != null)
					signed.cloneTo(report);
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
			Logger.w("Shift already opened: %s", mKKMInfo.getShift().getNumber());
			if (shift != null)
				getKKMInfo().getShift().cloneTo(shift);

			res.code = Errors.NO_ERROR;
			res.alreadyDone = true;
			return res;
		}

		getKKMInfo().getShift().setUpdateOKPKeysResult("Обновление ключей не требуется");
		CheckNeedUpdateKeysE needUpdateOKPKeys = isNeedUpdateOkpKeys();
		if (needUpdateOKPKeys != CheckNeedUpdateKeysE.NO_NEED_UPDATE) {
			getKKMInfo().getShift().setUpdateOKPKeysResult(updateOKPKeys(mStorage, getKKMInfo(), ctx));
		}
		
		Logger.i("Opening shift...");

		try (Transaction transaction = mStorage.open()) {
			res.code = getKKMInfo().getShift().writeOpenShift(transaction, operator, needUpdateOKPKeys);

			if (res.code == Errors.NO_ERROR) {
				Logger.i("Done opening shift: %s", mKKMInfo.getShift().getNumber());

				if (shift != null)
					getKKMInfo().getShift().cloneTo(shift);
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
			Logger.w("Shift already closed: %s", mKKMInfo.getShift().getNumber());
			if (shift != null)
				getKKMInfo().getShift().cloneTo(shift);

			res.code = Errors.NO_ERROR;
			res.alreadyDone = true;
			return res;
		}

		Logger.i("Closing shift...");

		try (Transaction transaction = mStorage.open()) {
			res.code = getKKMInfo().getShift().writeCloseShift(transaction, operator);

			if (res.code == Errors.NO_ERROR) {
				Logger.i("Done closing shift: %s", mKKMInfo.getShift().getNumber());
				if (shift != null)
					getKKMInfo().getShift().cloneTo(shift);

				res.print = getKKMInfo().getShift().getPF(template);
				res.shift = mKKMInfo.getShift();
			} else {
				Logger.i("Error closing shift: 0x%02X", res.code);
				cancelDocument(transaction);
			}
			return res;
		}
	}

	public String getPrintDoc(String type, byte[] bb, ParcelableStrings templates) {
		String res = "";
		if (KKMInfo.CLASS_NAME.equals(type)) {
			res = Utils.deserialize(bb, KKMInfoEx2.CREATOR).getPF(templates.get(0));

		} else if (Shift.CLASS_NAME.equals(type)) {
			res = Utils.deserialize(bb, ShiftEx2.CREATOR).getPF(templates.get(0), mKKMInfo);

		} else if (ArchiveReport.CLASS_NAME.equals(type)) {
			res = Utils.deserialize(bb, ArchiveReportEx2.CREATOR).getPF(templates.get(0), mKKMInfo);

		} else if (Correction.CLASS_NAME.equals(type)) {
			res = Utils.deserialize(bb, CorrectionEx2.CREATOR).getPF(templates.get(0), templates.get(1),
					templates.get(2), templates.get(3), mKKMInfo);

		} else if (SellOrder.CLASS_NAME.equals(type)) {
			res = Utils.deserialize(bb, SellOrderEx2.CREATOR).getPF(templates.get(0), templates.get(1),
					templates.get(2), templates.get(3), mKKMInfo);

		} else if (FiscalReport.CLASS_NAME.equals(type)) {
			res = Utils.deserialize(bb, FiscalReportEx2.CREATOR).getPF(templates.get(0), mKKMInfo);
		}
		return res;
	}

	public Tag getDoc(String type, byte[] bb) {
		Document doc = null;
		if (KKMInfo.CLASS_NAME.equals(type)) {
			doc = Utils.deserialize(bb, KKMInfoEx2.CREATOR);

		} else if (Shift.CLASS_NAME.equals(type)) {
			doc = Utils.deserialize(bb, ShiftEx2.CREATOR);

		} else if (ArchiveReport.CLASS_NAME.equals(type)) {
			doc = Utils.deserialize(bb, ArchiveReportEx2.CREATOR);

		} else if (Correction.CLASS_NAME.equals(type)) {
			doc = Utils.deserialize(bb, CorrectionEx2.CREATOR);

		} else if (SellOrder.CLASS_NAME.equals(type)) {
			doc = Utils.deserialize(bb, SellOrderEx2.CREATOR);

		} else if (FiscalReport.CLASS_NAME.equals(type)) {
			doc = Utils.deserialize(bb, FiscalReportEx2.CREATOR);
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
		if (mKKMInfo == null)
			mKKMInfo = new KKMInfoEx2();
		ByteBuffer bb = BufferFactory.allocateDocument();
		Tag doc = new Tag();
		try (Transaction transaction = mStorage.open()) {
			if (docNumber > 0) {
				if (readDocumentFromTLV(docNumber, doc,transaction) == Errors.NO_ERROR) {
					Document mInfo = doc.createInstance();
					if (mInfo instanceof KKMInfo) {
						mKKMInfo = new KKMInfoEx2((KKMInfo) mInfo);
						mKKMInfo.read(transaction);
						return Errors.NO_ERROR;
					}
				}
			}
			mKKMInfo.update(transaction);
			for(long i =  mKKMInfo.getLastFNDocNumber();i>0;i--) {
				if(transaction.write(FNCommandsE.GET_FISCAL_DOC_IN_TLV_INFO, (int)i).getLastError() == Errors.NO_ERROR &&
						transaction.read(bb) == Errors.NO_ERROR) {
					
					int type = bb.getShort(); 
					if( type == 1 || type == 11) {
						if (readDocumentFromTLV(i, doc,transaction) == Errors.NO_ERROR) {
							Document mInfo = doc.createInstance();
							if (mInfo instanceof KKMInfo) {
								mKKMInfo = new KKMInfoEx2((KKMInfo) mInfo);
								mKKMInfo.read(transaction);
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

	public int checkMarkingItem(SellItem item, DocServerSettings oismSettings) {
		if (!item.getMarkingCode().isEmpty()) {
			ByteBuffer bb = BufferFactory.allocateDocument();
			try (Transaction transaction = mStorage.open()) {
				SellItemEx2 newItem = new SellItemEx2(mKKMInfo, item);
				int res = newItem.getMarkingCode().checkMarkingItem(transaction, bb, oismSettings);
				newItem.cloneTo(item);
				return res;
			} finally {
				BufferFactory.release(bb);
			}
		}
		return Errors.NO_ERROR;
	}

	public int checkMarkingItemLocalFN(SellItem item) {
		if (!item.getMarkingCode().isEmpty()) {
			ByteBuffer bb = BufferFactory.allocateDocument();
			try (Transaction transaction = mStorage.open()) {
				SellItemEx2 newItem = new SellItemEx2(mKKMInfo, item);
				int res = newItem.getMarkingCode().checkMarkingItemLocalFN(transaction, bb);
				newItem.cloneTo(item);
				return res;
			} finally {
				BufferFactory.release(bb);
			}
		}
		return Errors.NO_ERROR;
	}

	public int checkMarkingItemOnlineOISM(SellItem item, DocServerSettings oismSettings) {
		if (!item.getMarkingCode().isEmpty()) {
			ByteBuffer bb = BufferFactory.allocateDocument();
			try (Transaction transaction = mStorage.open()) {
				SellItemEx2 newItem = new SellItemEx2(mKKMInfo, item);
				int res = newItem.getMarkingCode().checkMarkingItemOnlineOISM(transaction, bb, oismSettings);
				newItem.cloneTo(item);
				return res;
			} finally {
				BufferFactory.release(bb);
			}
		}
		return Errors.NO_ERROR;
	}

	public int confirmMarkingItem(SellItem item, boolean accepted) {
		if (item.getMarkingCode().isEmpty())
			return Errors.NO_MARKING_CODE_IN_ITEM;

		ByteBuffer bb = BufferFactory.allocateDocument();
		try (Transaction transaction = mStorage.open()) {
			SellItemEx2 newItem = new SellItemEx2(mKKMInfo, item);
			int res = newItem.getMarkingCode().confirmMarkingItem(transaction, bb, accepted);
			newItem.cloneTo(item);
			return accepted ? res : CHECK_MARK_ITEM_REJECTED;
		} finally {
			BufferFactory.release(bb);
		}
	}

	public Document readDocumentFromTLV(long documentNumber) {
		return readDocumentFromTLV(mStorage, mKKMInfo, documentNumber);
	}

	public int updateOISMStatus(OismStatistic s) {
		return updateOISMStatus(mStorage, getKKMInfo(), s);
	}

	public int updateOISMStatus(OismStatistic s, Transaction transaction, ByteBuffer bb) {
		return updateOISMStatus(getKKMInfo(), s, transaction, bb);
	}

	public byte[] readOISMDocument(ByteBuffer bb) {
		return readOISMDocument(mStorage, bb);
	}

	public boolean writeOFDReplyOISM(ByteBuffer dataBuffer, long docNo, byte[] answer) {
		return writeOFDReplyOISM(mStorage, dataBuffer, docNo, answer);
	}

	@Override
	public FNCounters getFnCounters(Transaction transaction, boolean isTotal) {
		return FN2Commands.getFNCounters(transaction, isTotal);
	}

	public CheckNeedUpdateKeysE isNeedUpdateOkpKeys() {
		ByteBuffer bb = BufferFactory.allocateRecord();
		try (Transaction transaction = mStorage.open()) {
			return isNeedUpdateOkpKeys(transaction, bb, getKKMInfo());
		} finally {
			BufferFactory.release(bb);
		}
	}

	public int storeOfflineMarkNotify() {
		return 0;
	}

	@Override
	public int exportMarking(String file) {
		try(FileOutputStream fos = new FileOutputStream(file)) {
			MarkingOfflineReport offline = new MarkingOfflineReport(mKKMInfo);
			ByteBuffer bb = ByteBuffer.allocate(65535);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			try (Transaction transaction = mStorage.open()) {
				int res = offline.read(transaction,bb); 
				if(res ==Errors.NO_ERROR ) {
					res = offline.confirm(transaction, bb);
					if(res == Errors.NO_ERROR)
						offline.writeToFile(fos);
					
				}
				return res;
			}
		} catch(IOException ioe) {
			return Errors.WRITE_ERROR;
		}
		
	}

	@Override
	public void releaseMarkCodes() {
		try (Transaction transaction = mStorage.open()) {
			transaction.write(FNCommandsE.MARKING_CODE_CHECK_CLEAR).check();
		}
	}

}
