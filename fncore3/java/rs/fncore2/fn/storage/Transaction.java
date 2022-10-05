package rs.fncore2.fn.storage;

import static rs.fncore2.utils.BufferFactory.RECORD_SIZE;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.data.Tag;
import rs.fncore2.fn.common.FNCommandsE;
import rs.fncore2.fn.common.FNErrorsE;
import rs.fncore2.utils.BufferFactory;
import rs.fncore2.utils.WakeLockPower;
import rs.utils.Utils;
import rs.log.Logger;

public abstract class Transaction implements AutoCloseable {

    private boolean mPrintLogs = true;

    private static final byte FN_CMD_PREFIX = 0x04;
    private static final int SIZE_POSITION = 1;
    private static final int FN_REPLY_HEADER_SIZE = 3;
    private static final int ANSWER_SIZE=1;
    public static final int CRC_LENGTH = 2;

    private static final long READ_TIMEOUT_MS = 3000;

    private ByteBuffer mBufferRead;
    private ByteBuffer mBufferWrite;
    static private byte[] mBufferFlush = new byte[BufferFactory.DOCUMENT_SIZE];
    private StorageI mStorage;
    private volatile int mError;
    private volatile FNCommandsE mLastCommand = FNCommandsE.UNKNOWN;
    private long mWrongBytesCounter=0;

    static volatile int mCount =0;
    private WakeLockPower mWakeLockPower;

    public Transaction(StorageI storage) {
        mWakeLockPower = new WakeLockPower(this.getClass().getSimpleName()+ " "+mCount++);
        mWakeLockPower.acquireWakeLock();
        mStorage = storage;
        mBufferRead = ByteBuffer.allocate(BufferFactory.DOCUMENT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        mBufferWrite = ByteBuffer.allocate(BufferFactory.DOCUMENT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void setLog(boolean logEnable){
        mPrintLogs = logEnable;
    }

    public void close() {
        mStorage.release(this);
        mWakeLockPower.releaseWakeLock();
    }

    public static Calendar readDate(ByteBuffer bb) {
        Calendar now = Calendar.getInstance();
        int year = bb.get() + 2000;
        int month = bb.get() - 1;
        int day = bb.get();
        int hour = bb.get();
        int minute = bb.get();
        now.set(year, month, day, hour, minute);
        return now;
    }

    public Transaction write(FNCommandsE command, Object... payload) {
        mError = Errors.DEVICE_ABSEND;
        mLastCommand = command;

        mBufferWrite.clear();
        mBufferWrite.put(FN_CMD_PREFIX);
        mBufferWrite.putShort((short) 0);
        mBufferWrite.put((byte) (command.value & 0xFF));
        short size = 1;

        for (Object o : payload) {
            if (o == null)
                continue;
            if (o instanceof Tag) {
                byte[] b = ((Tag) o).pack(true);
                size += b.length;
                mBufferWrite.put(b);
            } else if (byte[].class.isAssignableFrom(o.getClass())) {
                byte[] b = (byte[]) o;
                size += b.length;
                mBufferWrite.put(b);
            } else if (o instanceof Byte) {
                ++size;
                mBufferWrite.put(((Number) o).byteValue());
            } else if (o instanceof Boolean) {
                ++size;
                mBufferWrite.put((byte) ((Boolean) o ? 1 : 0));
            } else if (o instanceof Short) {
                size += 2;
                mBufferWrite.putShort(((Number) o).shortValue());
            } else if (o instanceof Integer) {
                size += 4;
                mBufferWrite.putInt(((Number) o).intValue());
            } else if (o instanceof String) {
                byte[] b = o.toString().getBytes(Const.ENCODING);
                if (b == null || b.length == 0)
                    b = new byte[]{0};
                size += b.length;
                mBufferWrite.put(b);
            } else if (o instanceof Calendar) {
                size += 5;
                Calendar c = (Calendar) o;
                byte b = (byte) ((c.get(Calendar.YEAR) - 2000) & 0xFF);
                mBufferWrite.put(b);
                b = (byte) ((c.get(Calendar.MONTH) + 1) & 0xFF);
                mBufferWrite.put(b);
                b = (byte) (c.get(Calendar.DAY_OF_MONTH) & 0xFF);
                mBufferWrite.put(b);
                b = (byte) (c.get(Calendar.HOUR_OF_DAY) & 0xFF);
                mBufferWrite.put(b);
                b = (byte) (c.get(Calendar.MINUTE) & 0xFF);
                mBufferWrite.put(b);
            } else if (o instanceof Float) {
                int v = (int) (Utils.round2((Float) o, 2) * 100);
                mBufferWrite.putInt(v);
                mBufferWrite.put((byte) 0);
                size += 5;
            } else if (o instanceof Double) {
                int v = (int) (Utils.round2((Double) o, 2) * 100);
                mBufferWrite.putInt(v);
                mBufferWrite.put((byte) 0);
                size += 5;
            } else if (o instanceof BigDecimal) {
                int v = Utils.round2((BigDecimal) o, 2).multiply(new BigDecimal(100), MathContext.DECIMAL128).intValue();
                mBufferWrite.putInt(v);
                mBufferWrite.put((byte) 0);
                size += 5;
            } else {
                Logger.e("write: unsupported data type");
            }
        }

        mBufferWrite.putShort(SIZE_POSITION, (short) (size & 0xffff));
        mBufferWrite.putShort((short) (Utils.CRC16(mBufferWrite.array(), 1, size + 2, Const.CCIT_POLY) & 0xffff));

        mError = Errors.DATA_ERROR;
        if (mPrintLogs) 
            Logger.logIO(">> %s", Utils.dump(mBufferWrite.array(), 0, mBufferWrite.position()));

        flushReadBuffer(mBufferFlush);
        
        if (writeB(mBufferWrite.array(), 0, mBufferWrite.position()) == mBufferWrite.position())
            mError = Errors.NO_ERROR;

        return this;
    }

    public int getLastError() {
        return mError;
    }

    public void clearLastError() {
        mError=Errors.NO_ERROR;
    }

    public boolean check() {
        return check(READ_TIMEOUT_MS);
    }

    public boolean check(long timeoutMs) {
        return read(null, timeoutMs) == Errors.NO_ERROR;
    }

    public long getWrongBytesCounter(){
        return mWrongBytesCounter;
    }

    private int readRaw(ByteBuffer bb, final long timeoutMs) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        int offset=0;
        bb.clear();
        byte[] dataArr = bb.array();
        {
            while (offset<FN_REPLY_HEADER_SIZE && System.currentTimeMillis() < startTime + timeoutMs) {
                int readBytes=readB(dataArr, offset, 1);
                if (readBytes==1){
                    switch (offset){
                        case 0:
                            if (dataArr[0] == FN_CMD_PREFIX) {
                                ++offset;
                            }
                            else{
                                ++mWrongBytesCounter;
                            }
                            continue;
                        case 1:
                        case 2:
                            ++offset;
                            continue;
                        default:
                            ++mWrongBytesCounter;
                            break;
                    }
                }
                Thread.sleep(5);
            }
            bb.position(0);
        }

        if (offset!=FN_REPLY_HEADER_SIZE){
            Logger.e("not received cmd header in time: %s, received only: %s",timeoutMs,offset);
            return -1;
        }

        if (bb.get() != FN_CMD_PREFIX){
            Logger.e("wrong cmd prefix");
            return -1;
        }
        int payloadSize = Utils.readUint16LE(bb);
        int bs = payloadSize + CRC_LENGTH;

        {
            int endOffset=bs + offset;
            while (offset < endOffset && System.currentTimeMillis() < startTime + timeoutMs) {
                int readBytes=readB(dataArr, offset, bs);

                if (readBytes>0) {
                    offset += readBytes;
                }

                if (offset<endOffset){
                    Thread.sleep(5);
                }
            }

            if (offset!=endOffset){
                Logger.e("not received cmd data in time: %s, received only: %s, from: %s", timeoutMs, offset,endOffset);
                return -1;
            }
            bb.position(0);
        }

        return offset;
    }

    public int read(ByteBuffer bb){
        return read(bb, READ_TIMEOUT_MS);
    }

    public int read(ByteBuffer bb, final long timeoutMs) {
        int dataSize=0;
        try {
            mError = Errors.NO_ERROR;

            int readBytes=readRaw(mBufferRead, timeoutMs);
            if (readBytes<=0){
                mError = Errors.DATA_ERROR;
                return mError;
            }

            if (mBufferRead.get() != FN_CMD_PREFIX){
                mError = Errors.DATA_ERROR;
                return mError;
            }
            int payloadSize = Utils.readUint16LE(mBufferRead);
            dataSize=payloadSize-ANSWER_SIZE;
            int bs = payloadSize + CRC_LENGTH;

            mBufferRead.limit(FN_REPLY_HEADER_SIZE + bs);
            if (mPrintLogs) {
                Logger.logIO("<< %s", Utils.dump(mBufferRead.array(), 0, mBufferRead.limit()));
            }
            int calcCRC = (Utils.CRC16(mBufferRead.array(), ANSWER_SIZE, mBufferRead.limit() - FN_REPLY_HEADER_SIZE,
                    Const.CCIT_POLY) & 0xFFFF);
            int rcvdCRC = (mBufferRead.getShort(mBufferRead.limit() - CRC_LENGTH) & 0xFFFF);

            if (calcCRC != rcvdCRC) {
                mError = Errors.CRC_ERROR;
                return mError;
            }

            int answer = (mBufferRead.get() & ~0x80) & 0xFF;
            if (answer!=0){
                mError=answer;
            }

            if (bb != null) {
                bb.clear();
                bb.put(mBufferRead.array(), FN_REPLY_HEADER_SIZE + ANSWER_SIZE, dataSize);
                bb.flip();
            }
            return mError;

        } catch (InterruptedException e) {
            mError = Errors.OPERATION_ABORTED;
            return mError;
        } finally {
            if (mError != Errors.NO_ERROR && mError != Errors.NO_MORE_DATA) {
                Logger.e("Command write: %20s data: 0x%s", mLastCommand,
                        Utils.dump(mBufferWrite));
                Logger.e("Command read : %20s, dataSize=%d, " +
                                "data: 0x%s, ERROR: %s\nERROR stack: %s",
                        mLastCommand, dataSize,
                        Utils.dump(mBufferRead.array(), FN_REPLY_HEADER_SIZE+1, dataSize),
                        FNErrorsE.fromInt(mError), Utils.getStackTrace());
                if (mBufferRead.limit()<RECORD_SIZE*2) {
                    Logger.e("Command read raw data: 0x%s", Utils.dump(mBufferRead.array(), 0, mBufferRead.limit()));
                }
                else{
                    Logger.e("Command read raw data: too long : %d", mBufferRead.limit());
                }
            }
        }
    }

    public int readB(byte[] data, int offset, int size) {
        return readB(data, offset, size, READ_TIMEOUT_MS);
    }

    public abstract int writeB(byte[] data, int offset, int size);

    public abstract void flushReadBuffer(byte[] data);

    public abstract int readB(byte[] data, int offset, int size, long timeout);
}
