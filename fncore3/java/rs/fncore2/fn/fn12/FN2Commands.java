package rs.fncore2.fn.fn12;

import static rs.utils.Utils.readUint16LE;

import android.content.Context;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Date;

import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.data.CheckNeedUpdateKeysE;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.Document;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.OismStatistic;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore2.FNCore;
import rs.fncore2.FNDocumentsE;
import rs.fncore2.core.Settings;
import rs.fncore2.fn.common.FNBase;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.common.OISMStatisticExBase;
import rs.fncore2.fn.fn12.marking.FnMarkingStatus;
import rs.fncore2.fn.fn12.utils.FnOperationsCounters;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;
import rs.log.Logger;

public abstract class FN2Commands extends FNBase {

	public enum GetKeysServerParamE {
		CHECK_NEED_UPDATE_KEYS(0), GET_SERVER_URI(1);

		public final byte bVal;

		private GetKeysServerParamE(int val) {
			this.bVal = (byte) val;
		}

		public static GetKeysServerParamE fromByte(byte number) {
			for (GetKeysServerParamE val : values()) {
				if (val.bVal == number) {
					return val;
				}
			}
			throw new InvalidParameterException("unknown value");
		}
	}

	public static class FnExpiriationInfo {
		public final long expireDate;
		public final short remainedNumRegBills;
		public final short numRegBills;

		public FnExpiriationInfo(ByteBuffer bb) {
			expireDate = Utils.readDate3(bb);
			remainedNumRegBills = Utils.readUint8LE(bb);
			numRegBills = Utils.readUint8LE(bb);
		}
	}

	public FN2Commands(StorageI storage) {
		super(storage);
	}

	protected static boolean clearMarkingResults(Transaction transaction) {
		return transaction.write(FNCommandsE.MARKING_CODE_CHECK_CLEAR).check();
	}


	protected static int updateOISMStatus(StorageI storage, KKMInfoEx2 kkmInfo, OismStatistic s) {
		if (s == null)
			return Errors.NO_ERROR;
		if (!kkmInfo.isMarkingGoods())
			return Errors.NO_ERROR;

		ByteBuffer bb = BufferFactory.allocateRecord();
		try (Transaction transaction = storage.open()) {
			return updateOISMStatus(kkmInfo, s, transaction, bb);
		} finally {
			BufferFactory.release(bb);
		}
	}

	protected static int updateOISMStatus(KKMInfoEx2 kkmInfo, OismStatistic s, Transaction transaction, ByteBuffer bb) {
		if (s == null)
			return Errors.NO_ERROR;
		if (!kkmInfo.isMarkingGoods())
			return Errors.NO_ERROR;

		if (!kkmInfo.isOfflineMode()) {
			transaction.write(FNCommandsE.GET_OISM_STATUS);
			int result = transaction.read(bb);
			if (result == Errors.NO_ERROR)
				s.update(bb);
			return result;
		} else {
			FnMarkingStatus info = new FnMarkingStatus(kkmInfo);
			info.read(transaction, bb);
			OISMStatisticExBase temp = new OISMStatisticExBase(s);
			temp.setUnsentDocumentCount(info.mNumMarkingNotifyUnsent);
			temp.cloneTo(s);
			return Errors.NO_ERROR;
		}
	}

	public static byte[] readOISMDocument(StorageI storage, ByteBuffer bb) {
		try (Transaction transaction = storage.open()) {
			transaction.write(FNCommandsE.START_READ_OISM_MESSAGE);
			if (transaction.read(bb) != Errors.NO_ERROR)
				return null;

			int size = readUint16LE(bb);
			byte[] payload = new byte[size];
			int offset = 0;

			try {
				while (offset < size) {
					int block = Math.min(size - offset, MAX_FN_MESSAGE_DATA_LEN);
					transaction.write(FNCommandsE.READ_OISM_MESSAGE_BLOCK, (short) offset, (short) block);
					if (transaction.read(bb) != Errors.NO_ERROR)
						return null;
					System.arraycopy(bb.array(), 0, payload, offset, block);
					offset += block;
				}
			} finally {
				transaction.write(FNCommandsE.COMMIT_READ_OISM_MESSAGE).check();
			}
			return payload;
		}
	}

	protected static boolean writeOFDReplyOISM(StorageI storage, ByteBuffer dataBuffer, long docNo, byte[] answer) {
		try (Transaction transaction = storage.open()) {
			transaction.write(FNCommandsE.STORE_OFD_OISM_RECEIPT, answer);

			if (transaction.read(dataBuffer) == Errors.NO_ERROR) {
				byte[] reply = new byte[dataBuffer.remaining()];
				dataBuffer.get(reply);
				FNCore.getInstance().getDB().storeOISMReply(docNo, reply);
				return true;
			} else {
				return false;
			}
		}
	}

	protected static int getFnVersion(Transaction transaction, ByteBuffer bb, KKMInfoEx2 info) {
		transaction.write(FNCommandsE.GET_FN_VERSION);
		if (transaction.read(bb) != Errors.NO_ERROR)
			return transaction.getLastError();
		byte[] bVersion = new byte[16];
		bb.get(bVersion);
		info.mFnVersion = new String(bVersion).trim();
		Logger.i("FNFw version : %s, mIsMGM: %s", info.mFnVersion, info.isMGM());
		return Errors.NO_ERROR;
	}

	protected static int cancelDocument(Transaction transaction, KKMInfoExBase kkmInfo) {
		ByteBuffer bb = BufferFactory.allocateDocument();
		try {
			int res = kkmInfo.readBase(transaction, bb);
			if (res != Errors.NO_ERROR)
				return res;

			if (kkmInfo.hasUnfinished()) {
				if (!transaction.write(FNCommandsE.CANCEL_DOCUMENT).check()) {
					return transaction.getLastError();
				}
			}
			return Errors.NO_ERROR;
		} finally {
			BufferFactory.release(bb);
		}
	}

	protected static int getOKPServer(Transaction transaction, ByteBuffer bb, KKMInfoEx2 info) {
		try {
			transaction.write(FNCommandsE.GET_KEYS_SERVER, FN2.GetKeysServerParamE.GET_SERVER_URI.bVal);
			int res = transaction.read(bb);
			if (res != Errors.NO_ERROR) {
				DocServerSettings okp = Settings.getInstance().getOKPServer();
				if (okp.getServerAddress() != null && !okp.getServerAddress().isEmpty() && okp.getServerPort() != 0) {
					info.mSupportOKP = true;
					return Errors.NO_ERROR;
				}

				Logger.i("not supported OKP %s", res);
				info.mSupportOKP = false;
				return res;
			} else {
				int bytesNumber = bb.limit();
				byte[] bUrl = new byte[bb.limit()];
				bb.get(bUrl);
				Logger.i("OKP byte url %s: %s", bytesNumber, Utils.dump(bUrl));

				int lastZeroIndex;
				{
					for (lastZeroIndex = 0; lastZeroIndex < bUrl.length; lastZeroIndex++) {
						if (bUrl[lastZeroIndex] == 0) {
							break;
						}
					}
				}

				String url = new String(bUrl, 0, lastZeroIndex).trim();
				Logger.i("OKP url: %s", url);

				url = url.replaceAll("[^\\x00-\\x7F]", "");
				Logger.i("OKP url processed: %s", url);

				DocServerSettings okp = Settings.getInstance().getOKPServer();
				try {
					URI uri = new URI(url);

					okp.setServerAddress(uri.getHost());
					okp.setServerPort(uri.getPort());
					Settings.getInstance().setOKPServer(okp);
				} catch (URISyntaxException e) {
					if (okp.getServerAddress() != null && !okp.getServerAddress().isEmpty()
							&& okp.getServerPort() != 0) {
						info.mSupportOKP = true;
						return Errors.NO_ERROR;
					}
					info.mSupportOKP = false;
					return Errors.DATA_ERROR;
				}
				info.mSupportOKP = true;
			}
		} catch (Exception e) {
			Logger.e(e, "get OKP server");
		}
		return Errors.NO_ERROR;
	}

	protected static CheckNeedUpdateKeysE isNeedUpdateOkpKeys(Transaction transaction, ByteBuffer bb,
			KKMInfoEx2 kkmInfo) {
		CheckNeedUpdateKeysE result = CheckNeedUpdateKeysE.NO_NEED_UPDATE;
		if (!kkmInfo.isMarkingGoods())
			return result;
		try {
			transaction.write(FNCommandsE.GET_KEYS_SERVER, FN2.GetKeysServerParamE.CHECK_NEED_UPDATE_KEYS.bVal);
			if (transaction.read(bb) == Errors.NO_ERROR) {
				result = CheckNeedUpdateKeysE.fromByte(bb.get());
				return result;
			}
			long diff = (new Date()).getTime(); // - kkmInfo.lastUpdateOKPTime;
			int days = (int) (diff / (1000 * 60 * 60 * 24));
			Logger.w("Последнее обновление ключей %s, %d дней назад", new Date(kkmInfo.lastUpdateOKPTime), days);

			if (days < 15) {
				return result;
			} else if (days <= 60) {
				result = CheckNeedUpdateKeysE.NEED_UPDATE_DELAY_15_60_DAYS;

			} else {
				result = CheckNeedUpdateKeysE.NEED_UPDATE_DELAY_MORE_60_DAYS;
			}
			return result;
		} finally {
			Logger.i("Необходимость обновления ключей: %s", result);

		}
	}

	private static final String NO_NEEDED = "Обновление ключей не требуется";
	private static final String SUCCESS = "Обновление ключей выполнено успешно";
	private static final String SERVER_INACCESSIBLE = "Обновление ключей не выполнено: Нет доступа к серверу ОКП";
	private static final String MARKING_DISABLED = "Обновление ключей не выполнено: Запрещена работа с маркированными товарами";
	private static final String INCORRECT_FN_STATE = "Обновление ключей не выполнено: Неверное состояние ФН";
	private static final String CRC_ERROR = "Обновление ключей не выполнено: Проблема КС при обработке	запроса";
	private static final String DEFAULT_ERROR = "Обновление ключей не выполнено: ошибка %02X. Обратитесь в службу техподдержки";
	private static final String HANDLE_ERROR = "Обновление ключей не выполнено: ФН не смог обработать ответ сервиса";
	private static final String FN_FAIL = "Обновление ключей не выполнено: Отказ ФН";
	private static final String NO_SERVER = "Обновление ключей не выполнено:  Невозможно получить адрес сервера ОКП";
	private static final String INCOREECT_RQ = "Обновление ключей не выполнено:  Неверный фискальный признак";
	private static final String INCOREECT_RSP = "Обновление ключей не выполнено:  Неверный формат ответа";
	private static final String INCOREECT_RQ_NO = "Обновление ключей не выполнено:  Неверный номер запроса в ответе";
	private static final String INCOREECT_FN_NO = "Обновление ключей не выполнено:  Неверный номер ФН";
	private static final String INCOREECT_LENGTH = "Обновление ключей не выполнено: Неверная длина ответа";
	private static final String ERR_20 = "Обновление ключей не выполнено: Ошибка 0x20";
	protected static String updateOKPKeys(StorageI storage, KKMInfoEx2 kkmInfo, Context ctx) {

		if (!kkmInfo.isMarkingGoods())
			return NO_NEEDED;

		int res = Errors.NO_ERROR;

		Calendar now = Calendar.getInstance();

		ByteBuffer bb = BufferFactory.allocateRecord();
		try (Transaction transaction = storage.open()) {
			if (getOKPServer(transaction, bb, kkmInfo) != Errors.NO_ERROR)
				return NO_SERVER;
			do {
				transaction.write(FNCommandsE.START_REQUST_UPDATE_OKP, now);
				res = transaction.read(bb);
				switch (res) {
				case 0:
					break;
				case 0x03:
					return FN_FAIL;
				case 0x32:
					return MARKING_DISABLED;
				case 0x02:
					return INCORRECT_FN_STATE;
				case 0x04:
					return CRC_ERROR;
				default:
					return String.format(DEFAULT_ERROR, res);
				}
				byte[] data = new byte[bb.limit()];
				System.arraycopy(bb.array(), 0, data, 0, bb.limit());
				byte[] resp = OKPSender.getOkpResp(ctx, data, Settings.getInstance().getOKPServer());
				if (resp == null)
					return SERVER_INACCESSIBLE;
				transaction.write(FNCommandsE.WRITE_RESP_UPDATE_OKP, resp);
				res = transaction.read(bb);
				switch (res) {
				case 0x30:
					break;
				case 0x20:
					if(bb.limit() > 0) {
						res = bb.get() & 0xFF;
						switch(res) {
						case 1: return INCOREECT_RQ;
						case 2: return INCOREECT_RSP;
						case 3: return INCOREECT_RQ_NO;
						case 4: return INCOREECT_FN_NO;
						case 5: return CRC_ERROR;
						case 7: return INCOREECT_LENGTH;
						default:
							return ERR_20 + String.format(" код %02x", res);
						}
					}
					return ERR_20;
				case 0x0:
					kkmInfo.lastUpdateOKPTime = new Date().getTime();
					Settings.getInstance().setLastUpdateOKPTime(kkmInfo.getFNNumber(), kkmInfo.lastUpdateOKPTime);
					return SUCCESS;
				case 0x24:
					return HANDLE_ERROR;
				case 0x32:
					return MARKING_DISABLED;
				case 0x02:
					return INCORRECT_FN_STATE;
				case 0x03:	
					return FN_FAIL;
				case 0x04:
					return CRC_ERROR;
				case 0x23: {
					short code = 0xFF;
					String r = "ошибка 0x23 обратитесь в техническую поддержку";
					if(bb.limit() > 1) {
						code = bb.getShort();
						r = String.format("код ошибки %02X",code);
					}
					if(bb.limit() > 4) {	
						short len = bb.getShort();
						byte[] buf = new byte[len];
						bb.get(buf);
						r = new String(buf, Const.ENCODING);
						
					}
					return "Обновление ключей не выполнено: " + r;
				}
				default:
					return String.format(DEFAULT_ERROR, res);
				
				}
			} while (true);
		} finally {
			BufferFactory.release(bb);
		}
	}

	protected static FnOperationsCounters getFnOperationsCounters(StorageI storage) {
		ByteBuffer bb = BufferFactory.allocateRecord();
		try (Transaction transaction = storage.open()) {
			transaction.write(FNCommandsE.GET_FN_COUNTERS_OPERATIONS,
					FnOperationsCounters.FnOperationCountersParamE.TotalFnCounters.bVal);
			if (transaction.read(bb) == Errors.NO_ERROR) {
				return new FnOperationsCounters(bb);
			}
		} finally {
			BufferFactory.release(bb);
		}
		return null;
	}

	public static FNCounters getFNCounters(Transaction transaction, boolean isTotal) {
		ByteBuffer bb = BufferFactory.allocateRecord();
		try {
			transaction.write(FNCommandsE.GET_FN_COUNTERS,
					isTotal ? FnCountersEx2.FnCountersParamE.TOTAL_FN_COUNTERS.bVal
							: FnCountersEx2.FnCountersParamE.CURRENT_SHIFT.bVal);
			if (transaction.read(bb) == Errors.NO_ERROR) {
				return new FnCountersEx2(bb, isTotal);
			}
		} finally {
			BufferFactory.release(bb);
		}
		return new FNCounters(isTotal);
	}


	protected static Document readDocumentFromTLV(StorageI storage, KKMInfoExBase kkmInfo, long documentNumber) {
		ByteBuffer bb = BufferFactory.allocateDocument();
		Document resDoc = null;
		int docType;
		try (Transaction transaction = storage.open()) {
			if (transaction.write(FNCommandsE.GET_FISCAL_DOC_IN_TLV_INFO, (int) documentNumber)
					.getLastError() == Errors.NO_ERROR) {
				if (transaction.read(bb) != Errors.NO_ERROR)
					return null;
				docType = readUint16LE(bb);
				int docLen = readUint16LE(bb);

				if (docType == FNDocumentsE.KKT_REGISTRATION_REPORT.value) {
					resDoc = new KKMInfoEx2();
					kkmInfo.cloneTo((KKMInfoEx2) resDoc);
					return resDoc;
				} else if (docType == FNDocumentsE.OPEN_SHIFT_REPORT.value) {
					resDoc = new Shift();
					// throw new Exception("unsupported document" + docType);
				} else if (docType == FNDocumentsE.FISCAL_ORDER.value) {
					resDoc = new FiscalReport();
					throw new Exception("unsupported document" + docType);
				} else if (docType == FNDocumentsE.CLOSE_SHIFT_REPORT.value) {
					throw new Exception("unsupported document" + docType);
				} else if (docType == FNDocumentsE.REPORT_OF_CURRENT_STATUS.value) {

					throw new Exception("unsupported document" + docType);
				} else if (docType == 55555555) {

				} else {
					throw new Exception("uknown document" + docType);
				}

				byte[] docBytes = new byte[docLen];
				int offset = 0;
				while (docLen > 0) {
					if (transaction.write(FNCommandsE.GET_FISCAL_DOC_IN_TLV_DATA).getLastError() == Errors.NO_ERROR) {
						if (transaction.read(bb) != Errors.NO_ERROR)
							break;
						int numBytesRead = bb.remaining() - 4;
						bb.get(docBytes, offset, numBytesRead);

						bb.position(0);
						int tag = readUint16LE(bb);
						Tag newTag = new Tag(tag, bb);

						// TODO: add all tags with types
						if (resDoc != null) {
							resDoc.add(tag, newTag.asString());
						}
						offset += numBytesRead;
						docLen -= numBytesRead;
					}
				}
			}
		} catch (Exception e) {
			Logger.e(e, "Transaction error doc: %s", documentNumber);
		} finally {
			BufferFactory.release(bb);
		}
		return resDoc;
	}
}

