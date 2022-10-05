package rs.fncore2.fn.storage.virtual;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import rs.fncore.Const;
import rs.fncore.data.Tag;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.fn12.FN2;
import rs.fncore2.fn.storage.StorageI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.utils.BufferFactory;
import rs.utils.Utils;
import rs.log.Logger;

class VTransaction extends Transaction {

    private final VStorage vStorage;
    private Tag mDocument;
    private final ByteBuffer mResult;
    private int mReadOffset;

    VTransaction(VStorage vStorage, StorageI s) {
        super(s);
        this.vStorage = vStorage;
        mResult = ByteBuffer.allocate(BufferFactory.DOCUMENT_SIZE);
        mResult.order(ByteOrder.LITTLE_ENDIAN);
    }

    private void putCounterOperationsInfo(ByteBuffer bb){
        bb.putInt(0);
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
    }

    private void putCounterCorrectionInfo(ByteBuffer bb){
        bb.putInt(0);

        bb.putInt(0);
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.putInt(0);
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.putInt(0);
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
        bb.putInt(0);
        bb.put(new byte[]{0, 0, 0, 0, 0, 0});
    }

    @Override
    public int writeB(byte[] buff, int offset, int size) {
        mResult.clear();
        mReadOffset = 0;
        mResult.put(VStorage.FN_PREFIX);
        mResult.putShort((byte) 0);
        if (buff[offset] == VStorage.FN_PREFIX) {
            ByteBuffer bb = BufferFactory.allocateRecord();
            try {
                bb.put(buff, offset, size);
                bb.flip();
                bb.position(3);
                int commandCode = bb.get() & 0xFF;
                FNCommandsE command = FNCommandsE.fromInt(commandCode);
                Logger.i("VFN processing command: " + command);
                switch (command) {
                case GET_FN_NUMBER:
                	mResult.put(VStorage.NO_ERRORS);
                	mResult.put(VStorage.V_FN_NO.getBytes());
                	break;
                    case CANCEL_DOCUMENT:
                        mResult.put(VStorage.NO_ERRORS);
                        break;
                    case MARKING_CODE_ADD_REQUEST_DATA:
                        bb.get();
                    case ADD_DOCUMENT_DATA:
                        if (mDocument != null) {
                        	bb.limit(bb.limit()-2);
                            mDocument.addAll(bb);
                            mResult.put(VStorage.NO_ERRORS);
                        } else {
                            mResult.put((byte) 2);
                        }
                        break;
                    case GET_FN_STATUS:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put(vStorage.mFnInfo.mState);
                        mResult.put((byte) 0);
                        mResult.putShort((short) 0);
                        mResult.put((byte) 0);
                        mResult.put(Utils.encodeDate(vStorage.mFnInfo.mLastDocumentDate));
                        mResult.put(VStorage.VFN_SERIAL);
                        mResult.putShort((short) (vStorage.mFnInfo.mLastDocumentNo & 0xFFFF));
                        mResult.putShort((short) 0);
                        mResult.put((byte) 0);
                        break;
                    case GET_FISCALIZATION_RESULT:
                        if (vStorage.mFnInfo.mFiscalization == null) {
                            mResult.put((byte) 0x02);
                        } else {
                            mResult.put(VStorage.NO_ERRORS);
                            mResult.put(Utils.encodeDate(vStorage.mFnInfo.mFTime));
                            mResult.put(vStorage.mFnInfo.mINN);
                            mResult.put(vStorage.mFnInfo.mKkmNO);
                            mResult.put(vStorage.mFnInfo.mTaxModes);
                            mResult.put(vStorage.mFnInfo.mWorkModes);
                        }
                        break;
                    case START_OPEN_SHIFT:
                        if (!vStorage.mFnInfo.mShiftOpen) {
                            mResult.put(VStorage.NO_ERRORS);
                            mDocument = new Tag();
                        } else
                            mResult.put((byte) 2);
                        break;
                    case START_CLOSE_SHIFT:
                        if (vStorage.mFnInfo.mShiftOpen) {
                            mResult.put(VStorage.NO_ERRORS);
                            mDocument = new Tag();
                        } else
                            mResult.put((byte) 2);
                        break;
                    case COMMIT_CLOSE_SHIFT:
                        int id = vStorage.mDB.storeDocument(mDocument);
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.putShort((short) vStorage.mFnInfo.mShiftNumber);
                        mResult.putInt(id);
                        mResult.putInt(0);
                        vStorage.mFnInfo.mShiftOpen = false;
                        vStorage.mFnInfo.store();
                        break;
                    case COMMIT_OPEN_SHIFT:
                        id = vStorage.mDB.storeDocument(mDocument);
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.putShort((short) ++vStorage.mFnInfo.mShiftNumber);
                        mResult.putInt(id);
                        mResult.putInt(0);
                        vStorage.mFnInfo.mNCheck = 0;
                        vStorage.mFnInfo.mShiftOpen = true;
                        vStorage.mFnInfo.store();
                        break;

                    case GET_SHIFT_STATUS:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put((byte) (vStorage.mFnInfo.mShiftOpen ? 1 : 0));
                        mResult.putShort((short) vStorage.mFnInfo.mShiftNumber);
                        mResult.putShort((short) vStorage.mFnInfo.mLastDocumentNo);
                        break;
                    case GET_FISCALIZATION_ARG:
                        if (vStorage.mFnInfo.mFiscalization != null) {
                            Tag tag = vStorage.mFnInfo.mFiscalization.getTag(bb.getShort());
                            if (tag != null) {
                                mResult.put(VStorage.NO_ERRORS);
                                mResult.put(tag.pack(true));
                            } else
                                mResult.put((byte) 3);
                        } else
                            mResult.put((byte) 2);
                        break;
                    case RESET_MGM:
                        vStorage.clear();
                        mResult.put(VStorage.NO_ERRORS);
                        break;
                    case GET_OFD_STATUS:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.putShort((short) 0);
                        mResult.putShort((short) 0);
                        mResult.putInt(0);
                        mResult.put(new byte[]{0, 0, 0, 0, 0});
                        break;
                    case START_FISCAL_REPORT:
                    case START_CLOSE_FISCAL_MODE:
                        mDocument = new Tag();
                        mResult.put(VStorage.NO_ERRORS);
                        break;
                    case COMMIT_CLOSE_FISCAL_MODE:
                        id = vStorage.mDB.storeDocument(mDocument);
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.putInt(id);
                        mResult.putInt(0);
                        vStorage.mFnInfo.mState = 4;
                        vStorage.mFnInfo.store();
                        break;
                    case START_BILL:
                    case START_CORRECTION_BILL:
                        if (!vStorage.mFnInfo.mShiftOpen)
                            mResult.put((byte) 2);
                        else {
                            mDocument = new Tag();
                            mResult.put(VStorage.NO_ERRORS);
                        }
                        break;
                    case COMMIT_BILL:
                        id = vStorage.mDB.storeDocument(mDocument);
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.putShort((short) ++vStorage.mFnInfo.mNCheck);
                        vStorage.mFnInfo.store();
                        mResult.putInt(id);
                        mResult.putInt(0);
                        break;
                    case COMMIT_FISCAL_REPORT:
                        id = vStorage.mDB.storeDocument(mDocument);
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.putInt(id);
                        mResult.putInt(0);
                        break;
                    case START_FISCALIZATION_1_2:
                    case START_FISCALIZATION:
                        mDocument = new Tag();
                        mResult.put(VStorage.NO_ERRORS);
                        break;
                    case COMMIT_FISCALIZATION_1_2:
                    case COMMIT_FISCALIZATION:
                        id = vStorage.mDB.storeDocument(mDocument);
                        if (id > 0) {
                            bb.position(bb.position() + 5);
                            bb.get(vStorage.mFnInfo.mINN);
                            bb.get(vStorage.mFnInfo.mKkmNO);
                            vStorage.mFnInfo.mTaxModes = bb.get();
                            vStorage.mFnInfo.mWorkModes = bb.get();
                            if (vStorage.mFnInfo.mID == 0)
                                vStorage.mFnInfo.mFTime.setTimeInMillis(System.currentTimeMillis());
                            vStorage.mFnInfo.mID = id;
                            vStorage.mFnInfo.mFiscalization = mDocument;
                            vStorage.mFnInfo.mLastDocumentDate.setTimeInMillis(System.currentTimeMillis());
                            vStorage.mFnInfo.mLastDocumentNo = id;
                            vStorage.mFnInfo.mState = 0x3;
                            vStorage.mFnInfo.store();
                            mResult.put((byte) 0);
                            mResult.putInt(id);
                            mResult.putInt(0);
                        } else
                            mResult.put((byte) 2);
                        break;
                    case GET_OISM_STATUS:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put((byte)0);
                        mResult.putShort((short) 0);
                        mResult.putInt(0);
                        mResult.put(new byte[]{0, 0, 0, 0, 0});
                        mResult.put((byte)0);
                        break;
                    case GET_FN_FFD:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put((byte) 0x4);
                        mResult.put((byte) 0x4);
                        break;
                    case GET_FN_VERSION:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put("DEMO VERSION 123".getBytes());
                        mResult.put((byte) 0x0);
                        break;
                    case SET_FN_SPEED:
                        mResult.put(VStorage.NO_ERRORS);
                        break;
                    case MARKING_CODE_TO_FN:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put((byte) 0x3);
                        mResult.put((byte) 0x1);
                        break;
                    case MARKING_CODE_CHECK_SAVE:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put((byte) 0xF);
                        break;
                    case GET_FISCAL_DOC_IN_TLV_INFO:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.putShort((short)0x1);
                        mResult.putShort((short)0x0);
                        break;
                    case GET_KEYS_SERVER: {
                        mResult.put(VStorage.NO_ERRORS);
                        FN2.GetKeysServerParamE reason = FN2.GetKeysServerParamE.fromByte(bb.get());
                        switch (reason) {
                            case GET_SERVER_URI:
                                mResult.put("test.rs.com".getBytes());
                                break;
                            case CHECK_NEED_UPDATE_KEYS:
                                mResult.put((byte) 0x0);
                                break;
                        }
                    }
                        break;
                    case GET_FN_EXPIRE_INFO:
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put(new byte[]{99, 1, 1});
                        mResult.put(new byte[]{99, 1, 1});
                        mResult.put((byte) 0xf0);
                        mResult.put((byte) 0xf0);
                        break;
                    case GET_FN_COUNTERS: {
//                        FnCountersEx2.FnCountersParamE reason = FnCountersEx2.FnCountersParamE.fromByte(bb.get());
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.putShort((short) vStorage.mFnInfo.mShiftNumber);
                        mResult.putInt(0);

                        putCounterOperationsInfo(mResult);
                        putCounterOperationsInfo(mResult);
                        putCounterOperationsInfo(mResult);
                        putCounterOperationsInfo(mResult);
                        putCounterCorrectionInfo(mResult);
                    }
                        break;
                    case GET_FN_MARKING_STATUS:{
                        mResult.put(VStorage.NO_ERRORS);
                        mResult.put((byte) 0x00);
                        mResult.put((byte) 0x00);
                        mResult.put((byte) 0xFF);
                        mResult.put((byte) 0x00);
                        mResult.put((byte) 0x00);
                        mResult.put((byte) 0x04);
                        mResult.putShort((short)0x0);
                    }
                        break;
                    default:
                        Logger.i("VFN unknown command: " + command + ", code: " + commandCode);
                        mResult.put((byte) 3);
                        break;
                }
                mResult.putShort(1, (short) (mResult.position() - 3));
                mResult.putShort(Utils.CRC16(mResult.array(), 1, mResult.position() - 1, Const.CCIT_POLY));
            } finally {
                BufferFactory.release(bb);
            }
        }
        return size;
    }

    @Override
    public void flushReadBuffer(byte[] buff) {

    }

    @Override
    public int readB(byte[] buff, int offset, int size, long timeout) {
        int read = Math.min(mResult.position() - mReadOffset, size);
        System.arraycopy(mResult.array(), mReadOffset, buff, offset, read);
        mReadOffset += read;
        return read;
    }
}
