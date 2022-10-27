package rs.fncore2.fn.common;

import static rs.utils.Utils.readUint16LE;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;
import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OfdStatistic;
import rs.fncore.data.Tag;
import rs.fncore2.FNCore;
import rs.fncore2.fn.NotSupportedFFDException;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.log.Logger;

public abstract class FNBase implements FNBaseI {

	public static final int MAX_FN_MESSAGE_DATA_LEN = 1024;
	private static final byte RESET_MGM_SUB_CODE = 22;

	protected volatile StorageI mStorage;
	private volatile Transaction mTransaction;
	protected KKMInfoExBase mKKMInfo;
	private String mFNNumber = "9999999999999999";


	public boolean isMGM() {
		return getKKMInfo().getFNNumber().startsWith("9999");
	}

	public FNBase(StorageI storage) {
		mStorage = storage;
		try (Transaction transaction = mStorage.open()) {
			if (transaction.write(FNCommandsE.GET_FN_NUMBER).getLastError() == Errors.NO_ERROR) {
				ByteBuffer bb = BufferFactory.allocateRecord();
				transaction.read(bb);
				byte[] fnNo = new byte[16];
				bb.get(fnNo);
				mFNNumber = new String(fnNo);
				Logger.i("Номер ФН: %s", mFNNumber);
				BufferFactory.release(bb);
			}

		}
	}

	public void Destroy() {
		mStorage = null;
	}

	public StorageI getStorage() {
		return mStorage;
	}

	public void setConnectionMode(KKMInfo.FNConnectionModeE mode) {
		getKKMInfo().setConnectionMode(mode);
	}

	protected abstract int cancelDocument(Transaction transaction);

	public int cancelDocument() {
		try (Transaction transaction = mStorage.open()) {
			return cancelDocument(transaction);
		}
	}

	public int openTransaction() {
		if (mTransaction != null)
			return mTransaction.hashCode();
		mTransaction = mStorage.open();
		return mTransaction.hashCode();
	}

	public void closeTransaction(int id) {
		if (mTransaction != null) {
			mTransaction.close();
			mTransaction = null;
		}
	}

	public int readB(int id, byte[] data, int offset, int len) {
		if (mTransaction == null)
			return -1;
		return mTransaction.readB(data, offset, len, 5000);
	}

	public int writeB(int id, byte[] data, int offset, int len) {
		if (mTransaction == null)
			return -1;
		return mTransaction.writeB(data, offset, len);
	}

	public int resetFN() {
		if (!isMGM()) {
			Logger.e("can't reset FN - not an MGM");
			return Errors.NOT_IMPLEMENTED;
		}

		Logger.i("Reset MGM");
		try (Transaction transaction = mStorage.open()) {
			transaction.write(FNCommandsE.RESET_MGM, RESET_MGM_SUB_CODE).check(1000 * 20);
		}
		mKKMInfo = null;
		try { loadKKMInfo(0); } catch(NotSupportedFFDException e) { }
		Logger.i("Done reset MGM");
		return Errors.NO_ERROR;
	}

	public boolean setOFDExchangeMode(boolean on) {
		try (Transaction transaction = mStorage.open()) {
			return transaction.write(FNCommandsE.SET_OFD_CONNECTION_STATUS, on ? 1 : 0).check();
		}
	}

	public int updateOFDStatus(OfdStatistic s) {
		if (s == null)
			return Errors.NO_ERROR;

		ByteBuffer bb = BufferFactory.allocateRecord();
		try (Transaction transaction = mStorage.open()) {
			transaction.write(FNCommandsE.GET_OFD_STATUS);
			int result = transaction.read(bb);
			if (result == Errors.NO_ERROR)
				s.update(bb);
			return result;
		} finally {
			BufferFactory.release(bb);
		}
	}

	public byte[] readOFDDocument(ByteBuffer bb) {
		try (Transaction transaction = mStorage.open()) {
			transaction.write(FNCommandsE.START_READ_OFD_MESSAGE);
			if (transaction.read(bb) != Errors.NO_ERROR)
				return null;

			int size = readUint16LE(bb);
			byte[] payload = new byte[size];
			int offset = 0;

			try {
				while (offset < size) {
					int block = Math.min(size - offset, MAX_FN_MESSAGE_DATA_LEN);
					transaction.write(FNCommandsE.READ_OFD_MESSAGE_BLOCK, (short) offset, (short) block);
					if (transaction.read(bb) != Errors.NO_ERROR)
						return null;
					System.arraycopy(bb.array(), 0, payload, offset, block);
					offset += block;
				}
			} finally {
				transaction.write(FNCommandsE.COMMIT_READ_OFD_MESSAGE).check();
			}
			return payload;
		}
	}

	public boolean writeOFDReply(ByteBuffer mDataBuffer, long docNo, byte[] answer) {
		try (Transaction transaction = mStorage.open()) {
			if (!transaction.write(FNCommandsE.STORE_OFD_RECEIPT, answer).check()) {
				return false;
			}

			transaction.write(FNCommandsE.FIND_ARCHIVE_RECEIPT, (int) docNo);
			if (transaction.read(mDataBuffer) == Errors.NO_ERROR) {
				byte[] reply = new byte[mDataBuffer.remaining()];
				mDataBuffer.get(reply);
				FNCore.getInstance().getDB().storeOFDReply(docNo, reply);
				return true;
			} else {
				return false;
			}

		}
	}

	public static KKMInfo.FFDVersionE readFFDVersionTag(StorageI storage) {
		try (Transaction transaction = storage.open()) {
			KKMInfo info = new KKMInfo();
			int ffdVer = Integer.parseInt(
					KKMInfoExBase.readTag(transaction, FZ54Tag.T1209_FFD_VERSION, null, byte.class).toString());
			int kktFFDVer = Integer.parseInt(
					KKMInfoExBase.readTag(transaction, FZ54Tag.T1189_KKT_FFD_VERSION, null, byte.class).toString());
			info.add(FZ54Tag.T1209_FFD_VERSION, ffdVer);
			info.add(FZ54Tag.T1189_KKT_FFD_VERSION, kktFFDVer);
			return info.getFFDProtocolVersion();
		} catch (Exception e) {
			return KKMInfo.FFDVersionE.UNKNOWN;
		}
	}

	@Override
	public int readDocumentFromTLV(long documentNumber, Tag document) {
		document.getChilds().clear();
		try (Transaction transaction = mStorage.open()) {
			return readDocumentFromTLV(documentNumber, document,transaction);
		}
	}

	public int readDocumentFromTLV(long documentNumber, Tag document, Transaction transaction) {
		document.getChilds().clear();
		ByteBuffer bb = ByteBuffer.allocate(32767);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		if (transaction.write(FNCommandsE.GET_FISCAL_DOC_IN_TLV_INFO, (int) documentNumber)
				.getLastError() != Errors.NO_ERROR || transaction.read(bb) != Errors.NO_ERROR)
			return transaction.getLastError();
		if(bb.limit() == 0) return Errors.NO_MORE_DATA;
		short type = bb.getShort();
		Log.d("fncore2", "Doc type is " + type);
		document.add(new Tag(FZ54Tag.T1000_DOCUMENT_NAME, type));
		while (transaction.write(FNCommandsE.GET_FISCAL_DOC_IN_TLV_DATA).getLastError() == Errors.NO_ERROR) {
			if (transaction.read(bb) == Errors.NO_ERROR && bb.limit() > 0) {
				document.getChilds().add(new Tag(bb));
			} else
				break;
		}
		if (transaction.write(FNCommandsE.FIND_ARCHIVE_RECEIPT, (int) documentNumber)
				.getLastError() == Errors.NO_ERROR) {
			transaction.read(bb);
			if(bb.limit() > 0) {
				byte[] b = new byte[bb.limit()];
				bb.get(b);
				document.getChilds().add(new Tag(FZ54Tag.T1068_OPERATOR_MESSAGE_TLV, b));
			}
		}
		return 0;
	}

	@Override
	public String getNumber() {
		return mFNNumber;
	}

}
