package rs.fncore2.fn.fn12;

import static rs.fncore.Errors.CHECK_MARKING_CODE;
import static rs.fncore.Errors.MARKING_CHECK_FAILED;
import static rs.utils.Utils.readUint16LE;

import android.os.Parcel;
import android.util.Log;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Calendar;

import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.MarkingCode;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.ParcelableList;
import rs.fncore.data.SellItem;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.KKMInfoExBase;
import rs.fncore2.fn.fn12.marking.MarkingCodeParam;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.io.OISMSender;
import rs.utils.Utils;
import rs.log.Logger;

class MarkingCodeEx2 extends MarkingCode {

	private static int MAX_UNKNOWN_CODE_LEN = 255;
	private KKMInfoExBase mKKMInfo;
	private SellItemEx2 mSellItem;
	private MarkingCodeParam mCodeParam;

	public static final int[] MARK_REQUEST_TAGS = { FZ54Tag.T2003_PLANNED_ITEM_STATE, FZ54Tag.T2102_MARKING_CODE_REGIME,
			FZ54Tag.T1023_QUANTITY, FZ54Tag.T2108_MEASURE_AMOUNT_ITEM, FZ54Tag.T1291_MARKING_FRACTIONAL_ITEM };

	public static final int[] MARK_REQUEST_TAGS_2007 = { FZ54Tag.T2000_MARKING_CODE, FZ54Tag.T2100_MARKING_CODE_TYPE,
			FZ54Tag.T2101_ITEM_IDENTIFICATOR, FZ54Tag.T2110_ASSIGNED_ITEM_STATUS, };

	public static final int[] MARKING_EXTENDED_TAGS = { FZ54Tag.T1261_INDUSTRY_CHECK_REQUISIT, FZ54Tag.T1228_CLIENT_INN,
			FZ54Tag.T1009_TRANSACTION_ADDR, FZ54Tag.T1055_USED_TAX_SYSTEM, };

	public enum addRequestDataCommandParamE {
		Notification(1), Data(2), ExtendedData(3);

		public final byte bVal;

		private addRequestDataCommandParamE(int val) {
			this.bVal = (byte) val;
		}
	}

	public enum markingCodeCheckSaveParamE {
		NotSaveResult(0), SaveResult(1);

		public final byte bVal;

		private markingCodeCheckSaveParamE(int val) {
			this.bVal = (byte) val;
		}
	}

	public static class CodeCheckResult_2004 {
		public final byte bValResult;
		public final byte bErrorResult;
		public Result2004E result;
		public ErrorE error;

		public enum Result2004E {
			FnNotChecked, FnCheckNegative, Unknown, FnCheckPositive
		}

		public enum ErrorE {
			CheckedNoErrors, CodeNotforFnCheck, FnNotHaveKeyForCheck, WrongGs91Offset, FnCheckInternalError
		}

		public CodeCheckResult_2004(ByteBuffer bb) {
			bValResult = bb.get();
			bErrorResult = bb.get();
			result = Result2004E.values()[bValResult];
			error = ErrorE.values()[bErrorResult];
		}

		public CodeCheckResult_2004(int data) {
			bValResult = (byte) (data & 0xFF);
			bErrorResult = (byte) (data >> 16 & 0xFF);
			result = Result2004E.values()[bValResult];
			error = ErrorE.values()[bErrorResult];
		}

		public short toInt() {
			return (short) (bValResult | bErrorResult << 16);
		}

		public String getMarkTag() {
			if (result == Result2004E.FnCheckNegative) {
				return "[M-]";
			} else if (result == Result2004E.FnCheckPositive) {
				return "[M+]";
			} else {
				return "[M]";
			}
		}

		public boolean isPositiveChecked() {
			return (result == Result2004E.FnCheckPositive);
		}

		public byte convertTo2106() {
			return (byte) ((bValResult | (1 << 3)));
		}
	}

	public MarkingCodeEx2(KKMInfoExBase info, MarkingCode srcClass, SellItemEx2 sellItem) {
		this.mKKMInfo = info;
		this.mSellItem = sellItem;
		Utils.readFromParcel(this, Utils.writeToParcel(srcClass));

		if (!srcClass.isEmpty()) {
			if (getPlannedItemStatus() == PlannedItemStatusE.UNKNOWN) {
				if (mSellItem.getMeasure() == MeasureTypeE.PIECE) {
					setPlannedItemStatus(PlannedItemStatusE.PIECE_ITEM_SELL);
				} else {
					setPlannedItemStatus(PlannedItemStatusE.MEASURED_ITEM_SELL);
				}
			}
			processNewMarkingCode();
		}
	}

	public void setItemIdentificator(String identificator) {
		if (identificator != null) {
			add(FZ54Tag.T2101_ITEM_IDENTIFICATOR, identificator);
		}
	}

	public String getItemIdentificator() {
		return getTagString(FZ54Tag.T2101_ITEM_IDENTIFICATOR);
	}

	private void processNewMarkingCode() {
		mCodeParam = new MarkingCodeParam(getCode());
		MarkingCodeParam.CodeTypesParamE type = mCodeParam.getType();
		MarkingCode.CodeTypesE fnMarkingCodeType = CodeTypesE.UNKNOWN;
		switch (type) {
		case EAN_8:
		case EAN_13:
		case ITF_14:
		case MI:
		case EGAIS_2:
		case EGAIS_3:
			break;
		case GS1_0:
			if (mCodeParam.getGS1MarkingCryptoTail() != null && mCodeParam.getGS1MarkingCryptoTail().length() == 4) {
				fnMarkingCodeType = CodeTypesE.RESP_4_NO_FN_CHECK;
			}
			break;
		case GS1_M:
			if (mCodeParam.getGS1MarkingCheckCode() != null && mCodeParam.getGS1MarkingCheckCode().length() == 44) {
				if (mCodeParam.isCodeForFNCheck()) {
					fnMarkingCodeType = CodeTypesE.RESP_44_FN_CHECK;
				} else {
					fnMarkingCodeType = CodeTypesE.RESP_44_NO_FN_CHECK;
				}
			} else if (mCodeParam.getGS1MarkingCheckCode() != null
					&& mCodeParam.getGS1MarkingCheckCode().length() == 88) {
				fnMarkingCodeType = CodeTypesE.RESP_88;
			}
			break;
		case KMK:
			fnMarkingCodeType = CodeTypesE.SHORT;
			break;
		case UNKNOWN:
		default:
			Logger.w("unknown code type %s", getCode());

			fnMarkingCodeType = CodeTypesE.UNKNOWN;
			int maxCodeLen = Math.min(MAX_UNKNOWN_CODE_LEN, getCode().length());
			setCode(getCode().substring(0, maxCodeLen));
			break;
		}

		setItemIdentificator(mCodeParam.getIdentificator());
		setCodeType(fnMarkingCodeType);
	}

	public MarkingCodeEx2(Parcel p) {
		readFromParcel(p);
	}

	public MarkingCodeParam.CodeTypesParamE getCodeTypeParam() {
		return mCodeParam.getType();
	}

	public CodeTypesE getCodeType() {
		return CodeTypesE.fromByte(getTag(FZ54Tag.T2100_MARKING_CODE_TYPE).asByte());
	}

	public void setCodeType(CodeTypesE codeType) {
		add(FZ54Tag.T2100_MARKING_CODE_TYPE, codeType.bVal);

		if (codeType.needGSOffset && (mCodeParam.getGS1MarkingCheckCodeOffset() == null
				|| mCodeParam.getGS1MarkingCheckKeyOffset() == null)) {
			throw new InvalidParameterException("need to set GS1 offsets for this type of code");
		}
	}

	byte[] getDataForOISMRequect(ByteBuffer bb) {
		int size = readUint16LE(bb);
		byte[] payload = new byte[size];
		System.arraycopy(bb.array(), 0, payload, 0, size);
		return payload;
	}

	public int checkMarkingItemLocalFN(Transaction transaction, ByteBuffer bb) {
		Logger.i("Проверка маркировки на ФН...");
		if (mSellItem.getMarkingCode().isEmpty())
			return Errors.NO_MARKING_CODE_IN_ITEM;
		if (mSellItem.getMarkingCode().getCheckResult().codeProcessed)
			return Errors.NO_ERROR;
		if (getCodeType().needGSOffset) {
			transaction.write(FNCommandsE.MARKING_CODE_TO_FN, getCodeType().bVal, (byte) getCode().length(), getCode(),
					mCodeParam.getGS1MarkingCheckKeyOffset(), mCodeParam.getGS1MarkingCheckCodeOffset());
		} else {
			transaction.write(FNCommandsE.MARKING_CODE_TO_FN, getCodeType().bVal, (byte) getCode().length(), getCode());
		}
		if (transaction.read(bb) != Errors.NO_ERROR)
			return transaction.getLastError();
		CodeCheckResult_2004 codeCheckResult = new CodeCheckResult_2004(bb);
		mMarkingCheckResult = new ItemCheckResult2106(codeCheckResult.convertTo2106());
		Logger.i("Проверка выполнена с результатом " + codeCheckResult.result + " (" + codeCheckResult.bValResult
				+ ") : " + codeCheckResult.error);
		return Errors.NO_ERROR;
	}

	public int checkMarkingItemOnlineOISM(Transaction transaction, ByteBuffer bb, DocServerSettings oismSettings) {
		if (mSellItem.getMarkingCode().isEmpty())
			return Errors.NO_MARKING_CODE_IN_ITEM;

		int res;
		Calendar now = Calendar.getInstance();
		transaction.write(FNCommandsE.MARKING_CODE_REQUEST_CREATE, now, packToTlvList(MARK_REQUEST_TAGS));
		if (transaction.read(bb) != Errors.NO_ERROR) {
			res = transaction.getLastError();
			dumpFNMarkingState(transaction, bb);
			return res;
		}

		byte[] request = getDataForOISMRequect(bb);
		byte[] resp = OISMSender.getOISMMarkingResp(request, oismSettings);

		if (resp == null) {
			Logger.e("no responce from OISM");
			return Errors.MARKING_CHECK_FAILED;
		}

		transaction.write(FNCommandsE.MARKING_CODE_REQUEST_STORE, resp);
		res = transaction.read(bb);
		if (res != Errors.NO_ERROR) {
			if (res == 0x20) {
				byte errDesc = bb.get();
				Logger.e("error marking marking response %s", errDesc);
				return CHECK_MARKING_CODE;
			}
			return transaction.getLastError();
		}

		mMarkingCheckResult = new ItemCheckResult2106(bb.get());

		// TODO: check tags, that parse ok
		Tag tlvs = new Tag();
		tlvs.unpackFromTlvList(bb);
		String s = mCodeParam.getGS1MarkingCheckCodeCRC();
		String r = Const.EMPTY_STRING;
		for (int i = s.length() - 1; i >= 0; i--) {
			if (r.length() == 4)
				break;
			r = s.charAt(i) + r;
		}
		mSellItem.add(2115, r);
		/*
		 * if (tlvs.hasTag(T2105_PROCESS_REQUEST_CODE)) {
		 * mSellItem.add(T2105_PROCESS_REQUEST_CODE,
		 * tlvs.getTag(T2105_PROCESS_REQUEST_CODE).asByte()); } if
		 * (tlvs.hasTag(T2109_OISM_ANSWER_ITEM_STATUS)) {
		 * mSellItem.add(T2109_OISM_ANSWER_ITEM_STATUS,
		 * tlvs.getTag(T2109_OISM_ANSWER_ITEM_STATUS).asByte()); }
		 */

		if (!mMarkingCheckResult.codeOISMChecked) {
			Logger.e("error check code online %d", mMarkingCheckResult.bVal);
			mKKMInfo.setFailedMarkingCode(true);
			return MARKING_CHECK_FAILED;
		}
		return Errors.NO_ERROR;
	}

	public int checkMarkingItem(Transaction transaction, ByteBuffer bb, DocServerSettings oismSettings) {

		int res = checkMarkingItemLocalFN(transaction, bb);
		if (res != Errors.NO_ERROR)
			return res;
		if (mKKMInfo.isOfflineMode()) {
			if (!mMarkingCheckResult.isPositiveChecked()) {
				mKKMInfo.setFailedMarkingCode(true);
				return MARKING_CHECK_FAILED;
			}
			return Errors.NO_ERROR;
		}

		res = checkMarkingItemOnlineOISM(transaction, bb, oismSettings);
		if (res != Errors.NO_ERROR) {
			if (mMarkingCheckResult.isPositiveChecked())
				return Errors.NO_ERROR;
		}
		return res;
	}

	public int confirmMarkingItem(Transaction transaction, ByteBuffer bb, boolean accepted) {
		if (mMarkingCheckResult.autonomousMode)
			transaction.write(FNCommandsE.MARKING_CODE_REQUEST_CREATE, Calendar.getInstance(),
					packToTlvList(MARK_REQUEST_TAGS));

		transaction.write(FNCommandsE.MARKING_CODE_CHECK_SAVE,
				accepted ? markingCodeCheckSaveParamE.SaveResult.bVal : markingCodeCheckSaveParamE.NotSaveResult.bVal);
		int res = transaction.read(bb);
		if (res != Errors.NO_ERROR) {
			dumpFNMarkingState(transaction, bb);
			return res;
		}

		if (accepted) {
			mMarkingCheckResult = new ItemCheckResult2106(bb.get());
			if (mMarkingCheckResult.autonomousMode) {
				if (!mMarkingCheckResult.codeChecked) {
					Logger.i("error save check marking code offline: %s", mMarkingCheckResult.bVal);
					mKKMInfo.setForcedFailedMarkingCode(true);
				}
			} else {
				if (!mMarkingCheckResult.codeOISMChecked) {
					Logger.i("error save check marking code online: %s", mMarkingCheckResult.bVal);
					mKKMInfo.setForcedFailedMarkingCode(true);
				}
			}

		} else
			mMarkingCheckResult = new ItemCheckResult2106((byte) 0);
		mSellItem.setMarkResult(mMarkingCheckResult);

		return Errors.NO_ERROR;
	}

	private int dumpFNMarkingState(Transaction transaction, ByteBuffer bb) {
		transaction.write(FNCommandsE.GET_FN_MARKING_STATUS);
		int res = transaction.read(bb);
		if (res != Errors.NO_ERROR) {
			Logger.e("error getFN Marking status %s", res);
		} else {
			Logger.i("KM check status: 0x%s", Integer.toHexString(bb.get()));
			Logger.i("Notify item generate status: 0x%s", Integer.toHexString(bb.get()));
			Logger.i("Flags KM work: 0x%s", Integer.toHexString(bb.get()));
			Logger.i("Number KM check: %s", bb.get());
			Logger.i("Number KM in notify: %s", bb.get());
			Logger.i("Warning about full: 0x%s", Integer.toHexString(bb.get()));
			Logger.i("Number Notify in Queue: %s", bb.getShort());
		}
		return res;
	}

	public int read(Transaction transaction, ByteBuffer bb) {
		transaction.write(FNCommandsE.GET_SHIFT_STATUS);
		int res = transaction.read(bb);
		if (res != Errors.NO_ERROR)
			return res;
		return res;
	}

	public void cloneTo(Shift dest) {
		Utils.readFromParcel(dest, Utils.writeToParcel(this));
	}

	public static final Creator<MarkingCodeEx2> CREATOR = new Creator<MarkingCodeEx2>() {

		@Override
		public MarkingCodeEx2 createFromParcel(Parcel p) {
			p.readString();
			return new MarkingCodeEx2(p);
		}

		@Override
		public MarkingCodeEx2[] newArray(int arg0) {
			return null;
		}
	};

	public static boolean isAllMarkingItemsPositiveChecked(ParcelableList<SellItem> items) {
		for (SellItem item : items) {
			if (!item.getMarkingCode().isEmpty()) {
				if (!item.getMarkingCode().getCheckResult().isPositiveChecked()) {
					return false;
				}
			}
		}
		return true;
	}
}
