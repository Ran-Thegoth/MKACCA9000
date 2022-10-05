package rs.fncore2.io;

import static rs.utils.Utils.readUint16LE;

import android.content.Intent;
import android.util.Log;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.KKMInfo.FFDVersionE;
import rs.fncore.data.OfdStatistic;
import rs.fncore2.FNCore;
import rs.fncore2.core.Settings;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.FNBaseI;
import rs.fncore2.utils.BufferFactory;
import rs.fncore2.utils.WakeLockPower;
import rs.utils.Utils;
import rs.log.Logger;

public class OFDSender extends BaseThread implements PropertyChangeListener {

    protected static final short MSG_FLAGS = (1 << 4) | (1 << 2);
    private static final byte[] SIGNATURE_OFD = {0x2A, 0x08, 0x41, 0x0A};
    private static final byte[] PROTOCOL_VERSION_OFD_S = {(byte) 0x81, (byte) 0xA2};
    private static final long SEND_TIMEOUT_MS=5*60*1000;

    private static final boolean LOG_PRINT = true;

    private volatile boolean mProgress;
    private volatile DocServerSettings mOFDSettings;
    private volatile boolean mSendNow;
    private volatile boolean mHasErrors = false;
    private final Object mSendNowSync = new Object();
    private FNManager mFNManager = FNManager.getInstance();

    public OFDSender(Settings settings) {
        setName("OFDSender");
        mOFDSettings = settings.getOFDServer();
        settings.addOFDChangedListener(this);

        mFNManager.addFNReadyListener(this);
        mFNManager.addFNChangedListener(this);

        start();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Settings.OFD_CHANGED)) {
        	mHasErrors = false;
            mOFDSettings = (DocServerSettings)evt.getNewValue();
            Logger.i("new OFD in OFDSender %s", mOFDSettings.getServerAddress());
            unblockWait();
        }
        else if (evt.getPropertyName().equals(FNManager.FN_READY)){
            unblockWait();
        }
    }

    public boolean isProgress() {
        return mProgress;
    }

    public void sendNowDocs() {
        mSendNow = true;
        unblockWait();
    }

    private void notifyProgress(int count){
        if (count!=0){
            mProgress = true;
        }
        else{
            mProgress = false;
        }
        Intent intent = new Intent(Const.OFD_SENT_ACTION);
        intent.putExtra(Const.OFD_DOCUMENTS, count);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        FNCore.getInstance().sendBroadcast(intent);
    }

    protected void unblockWait(){
        synchronized (mSendNowSync) {
            mSendNowSync.notifyAll();
        }
    }

    @Override
    public void run() {
    	Logger.i("Запущен обмен с ОФД");
        OfdStatistic s = new OfdStatistic();
        WakeLockPower mWakeLockPower =  new WakeLockPower(getName());
        ByteBuffer tmpBuffer = ByteBuffer.allocate(BufferFactory.DOCUMENT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        
        int failCnt = 0;
        while (!isStopped) {
            try {
            	try { Thread.sleep(60000); } catch(InterruptedException ie) { break; }
            	if(mHasErrors) continue;
                mFNManager.waitFNReady();
                
                synchronized (mSendNowSync) {
                    if (        mOFDSettings.getServerAddress().isEmpty()
                            || !(mOFDSettings.getImmediatelyMode() || mSendNow)
                            || mFNManager.getFN().getKKMInfo().isOfflineMode()) {
                        mSendNowSync.wait(1000);
                        continue;
                    }
                }

                FNBaseI fN=mFNManager.getFN();
                String FnSN = fN.getKKMInfo().getFNNumber();
                FFDVersionE verFFD = fN.getKKMInfo().getFFDProtocolVersion();
                if(fN.updateOFDStatus(s) != Errors.NO_ERROR) 
                	continue;
                while (!isStopped && s.getUnsentDocumentCount() > 0){
                    try {
                        mWakeLockPower.acquireWakeLock();
                        notifyProgress(s.getUnsentDocumentCount());
                        fN.setOFDExchangeMode(true);

                        byte[] dataFN = fN.readOFDDocument(tmpBuffer);
                        if (dataFN == null) {
                            Logger.e("Readed null document for send to OFD ???");
                            break;
                        }
                        Log.d("OFD", " To OFD : "+Utils.dump(dataFN));

                        byte[] dataOFD = prepareOFDDocument(tmpBuffer, dataFN, FnSN, verFFD);

                        long startTime = System.currentTimeMillis();
                        failCnt = 0;
                        while (!isStopped && System.currentTimeMillis() < startTime + SEND_TIMEOUT_MS ){
                            byte[] reply = sendPackToOFD(tmpBuffer, dataOFD, FnSN);
                            if (reply!=null){
                            	Log.d("OFD", " From OFD : "+Utils.dump(reply));
                                if (!mFNManager.getFN().writeOFDReply(tmpBuffer,
                                        s.getFirstUnsentNumber(), reply)){
                                	mHasErrors = true;
                                    Logger.e("error write OFD reply");
                                }
                                break;
                            } else {
                            	Log.d("OFD", " OFD send no anser");
                            	if(++failCnt > 10) {
                            		mHasErrors = true;
                            		break;
                            	}
                            	Thread.sleep(500);
                            }
                        }
                        if(failCnt > 10) break;
                    } finally {
                        mFNManager.getFN().setOFDExchangeMode(false);
                        fN.updateOFDStatus(s);
                        mWakeLockPower.releaseWakeLock();
                    }
                }
                
                mSendNow = false;
                notifyProgress(s.getUnsentDocumentCount());
            } catch (InterruptedException ie) {
                break;
            } catch (Exception e) {
                Logger.e(e, "Execute error");
            }
        }
        BufferFactory.release(tmpBuffer);
        mFNManager.removeFNChangedListener(this);
        mFNManager.removeFNReadyListener(this);
        Logger.i("Ended");
    }

    private byte [] prepareOFDDocument(ByteBuffer tmpBuffer, byte [] dataFN, String FNSN,
                                       KKMInfo.FFDVersionE verFFD) throws Exception {
        tmpBuffer.clear();
        tmpBuffer.put(SIGNATURE_OFD);
        tmpBuffer.put(PROTOCOL_VERSION_OFD_S);

        switch (verFFD){
            case VER_10:
            case VER_105:
                tmpBuffer.put((byte) 0x01);
                tmpBuffer.put((byte) 0x05);
                break;
            case VER_11:
                tmpBuffer.put((byte) 0x01);
                tmpBuffer.put((byte) 0x10);
                break;
            case VER_12:
                tmpBuffer.put((byte) 0x01);
                tmpBuffer.put((byte) 0x20);
                break;
            default:
                throw new Exception("unknown FFD version: "+verFFD);
        }

        tmpBuffer.put(FNSN.getBytes(Const.ENCODING));
        tmpBuffer.putShort((short) (dataFN.length & 0xFFFF));
        tmpBuffer.putShort(MSG_FLAGS);
        tmpBuffer.putShort((short) 0);
        tmpBuffer.put(dataFN);

        byte[] outData = new byte[tmpBuffer.position()];
        System.arraycopy(tmpBuffer.array(), 0, outData, 0, outData.length);
        return outData;
    }

    private byte[] sendPackToOFD(ByteBuffer tmpBuffer, byte[] pack, String FNSN) {
        try {
            Logger.i("Соединение с ОФД %s : %s", mOFDSettings.getServerAddress(), mOFDSettings.getServerPort());

            try (Socket socket = new Socket(mOFDSettings.getServerAddress(), mOFDSettings.getServerPort())) {
                socket.setSoTimeout(mOFDSettings.getServerTimeout() * 1000);
                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream();

                os.write(pack);
                os.flush();

                if (LOG_PRINT) {
                    Logger.i(">> OFD %s", Utils.dump(pack));
                }

                int read = is.read(tmpBuffer.array());
                if (read > (SIGNATURE_OFD.length + PROTOCOL_VERSION_OFD_S.length)) {
                    if (LOG_PRINT) {
                        Logger.i("OFD << %s", Utils.dump(tmpBuffer.array(), 0, read));
                    }

                    tmpBuffer.limit(read);
                    tmpBuffer.position(0);

                    for (byte b : SIGNATURE_OFD)
                        if (tmpBuffer.get() != b)
                            return null;

                    for (byte b : PROTOCOL_VERSION_OFD_S)
                        if (tmpBuffer.get() != b)
                            return null;

                    tmpBuffer.getShort();
                    byte[] fn = new byte[16];
                    tmpBuffer.get(fn);

                    if (!new String(fn, Const.ENCODING).equals(FNSN)) return null;

                    int size = readUint16LE(tmpBuffer);
                    tmpBuffer.getInt();
                    byte[] answer = new byte[size];
                    tmpBuffer.get(answer);

                    Logger.i("Документ передан в ОФД");
                    return answer;

                } else {
                    Logger.e("ОФД не принял документ, ошибка: %s", read);
                }
                return null;

            }

        } catch (IOException ioe) {
            Logger.e(ioe,"Ошибка ОФД");
            return null;
        }
    }
}
