package rs.fncore2.fn.fn12.marking;

import static rs.fncore.Const.CCIT_POLY;
import static rs.fncore2.fn.common.FNBase.MAX_FN_MESSAGE_DATA_LEN;

import java.nio.ByteBuffer;

import rs.fncore.Errors;
import rs.fncore.data.Tag;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.storage.Transaction;
import rs.utils.Utils;

public class MarkingOfflineDocument {

    private int mSize;
    private long mDocNum;
    private byte [] mData;
    private boolean mConfirmed=false;

    private static final int NO_MORE_DOCS=0x8;

    enum SetupSubCmdE {
        GET_PARAMETERS,
        GO_NEXT_DOC
    }

    enum ConfirmSubCmdE {
        GET_PARAMETERS,
        CONFIRM
    }

    public long getNumber(){
        return mDocNum;
    }

    int read(Transaction transaction, ByteBuffer bb){
        transaction.write(FNCommandsE.SETUP_MARKING_NOTIFY, (byte) SetupSubCmdE.GET_PARAMETERS.ordinal());
        int res=transaction.read(bb);
        if (res != Errors.NO_ERROR) return res;

        mSize= Utils.readUint16LE(bb);
        mDocNum = Utils.readUint32LE(bb);
        mData = new byte[mSize];

        for (int offset=0; offset<mSize;){
            int maxLen = Math.min(MAX_FN_MESSAGE_DATA_LEN, mSize-offset);

            transaction.write(FNCommandsE.READ_MARKING_NOTIFY, (short) offset, (short)maxLen);
            res=transaction.read(bb);
            if (res != Errors.NO_ERROR) return res;

            System.arraycopy(bb.array(), 0, mData, offset, maxLen);
            offset+=maxLen;
        }

        transaction.write(FNCommandsE.SETUP_MARKING_NOTIFY, (byte) SetupSubCmdE.GO_NEXT_DOC.ordinal());
        res=transaction.read(bb);
        if (res==NO_MORE_DOCS) return Errors.NO_ERROR;
        return res;
    }

    short getCRC(){
        return CRC16_mark(mData, 0, mData.length, CCIT_POLY);
    }

    public static short CRC16_mark(byte[] data, int offset, int length, short nPoly) {
        short crc = (short) 0xFFFF;
        for (int j = 0; j < length; j++) {
            if (j==2||j==3) continue;
            byte b = data[offset + j];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit)
                    crc ^= nPoly;
            }
        }
        crc &= 0xffff;
        return crc;
    }

    int confirm(Transaction transaction, ByteBuffer bb){
/*        short CRC16=getCRC();
        short CRC161=CRC16(mData, 2, mData.length-2, CCIT_POLY); */
        transaction.write(FNCommandsE.CONFIRM_MARKING_NOTIFY, (byte) ConfirmSubCmdE.CONFIRM.ordinal(),
                (int)mDocNum, getCRC());
        int res=transaction.read(bb);
        if (res==Errors.NO_ERROR) mConfirmed=true;
        return res;
    }

    public boolean isConfirmed(){
        return mConfirmed;
    }

    public int getSize(){
        return mData.length;
    }

    public byte[] getData(){
    	return new Tag(201,mData).pack(true);
    }
}
