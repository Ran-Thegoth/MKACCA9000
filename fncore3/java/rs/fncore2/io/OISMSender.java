package rs.fncore2.io;

import static rs.utils.Utils.readUint16LE;

import android.content.Context;
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
import rs.fncore.data.OismStatistic;
import rs.fncore2.FNCore;
import rs.fncore2.core.Settings;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.FNBaseI;
import rs.fncore2.utils.BufferFactory;
import rs.fncore2.utils.UtilsCore;
import rs.fncore2.utils.WakeLockPower;
import rs.utils.Utils;
import rs.log.Logger;

public class OISMSender extends BaseThread implements PropertyChangeListener {

	protected static final short MSG_FLAGS = (1 << 4) | (1 << 2);
	private static final byte[] SIGNATURE_OISM = { (byte) 0xDD, (byte) 0x80, (byte) 0xCA, (byte) 0xA1 };
	private static final byte[] PROTOCOL_VERSION_OISM_S = { (byte) 0x82, (byte) 0xA2 };
	private static final byte[] PROTOCOL_VERSION_OISM_P = { (byte) 0x00, (byte) 0x01 };
	private static final boolean LOG_PRINT = true;
	private static final long SEND_TIMEOUT_MS = 5 * 60 * 1000;

	private volatile boolean mProgress;
	private volatile DocServerSettings mOismSettings;
	private volatile boolean mSendNow;

	private static final Object mSendNowSync = new Object();
	private FNManager mFNManager = FNManager.getInstance();
	private static Context mContext;

	public OISMSender(Settings settings, Context context) {
		mContext = context;
		setName("OISMSender");
		mOismSettings = settings.getOISMServer();
		settings.addOFDChangedListener(this);

		mFNManager.addFNChangedListener(this);
		mFNManager.addFNReadyListener(this);
		start();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(Settings.OISM_CHANGED)) {
			mOismSettings = (DocServerSettings) evt.getNewValue();
			Logger.i("new OFD in OISMSender %s", mOismSettings.getServerAddress());
			unblockWait();
		} else if (evt.getPropertyName().equals(FNManager.FN_READY)) {
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

	protected void unblockWait() {
		synchronized (mSendNowSync) {
			mSendNowSync.notifyAll();
		}
	}

	public static byte[] getOISMMarkingResp(byte[] requestData, DocServerSettings oismServer) {
		if (!UtilsCore.isConnected(mContext))
			return null;
		String FNSN = FNManager.getInstance().getFN().getKKMInfo().getFNNumber();
		ByteBuffer tmpDataBuffer = ByteBuffer.allocate(BufferFactory.DOCUMENT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
		try {
			byte[] dataOFD = prepareOISMDocument(tmpDataBuffer, requestData, FNSN);
			return sendPackToOISM(tmpDataBuffer, dataOFD, FNSN, oismServer);
		} finally {
			BufferFactory.release(tmpDataBuffer);
		}
	}

	private void notifyProgress(int count) {
		if (count != 0) {
			mProgress = true;
		} else {
			mProgress = false;
		}
		Intent intent = new Intent(Const.OFD_SENT_ACTION);
		intent.putExtra(Const.OISM_DOCUMENTS, count);
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		FNCore.getInstance().sendBroadcast(intent);
	}

	@Override
	public void run() {
		Logger.i("Запущен обмен с ОИСМ");
		OismStatistic s = new OismStatistic();
		WakeLockPower mWakeLockPower = new WakeLockPower(getName());
		ByteBuffer tmpBuffer = ByteBuffer.allocate(BufferFactory.DOCUMENT_SIZE).order(ByteOrder.LITTLE_ENDIAN);

		while (!isStopped) {
			try {
				Thread.sleep(60000);
				mFNManager.waitFNReady();
				synchronized (mSendNowSync) {
					if (mOismSettings.getServerAddress().isEmpty() || mSendNow) {
						mSendNowSync.wait(1000);
						continue;
					}
				}

				FNBaseI fN = mFNManager.getFN();
				String FnSN = fN.getKKMInfo().getFNNumber();
				if (fN.updateOISMStatus(s) != Errors.NO_ERROR)
					continue;
				while (!isStopped && s.haveUnsentDocuments()) {
					mSendNow = true;
					try {
						mWakeLockPower.acquireWakeLock();
						notifyProgress(s.getUnsentDocumentCount());

						byte[] dataFN = fN.readOISMDocument(tmpBuffer);
						if (dataFN == null) {
							break;
						}

						byte[] dataOFD = prepareOISMDocument(tmpBuffer, dataFN, FnSN);

						long startTime = System.currentTimeMillis();
						int failCnt = 0;
						while (!isStopped && System.currentTimeMillis() < startTime + SEND_TIMEOUT_MS) {
							byte[] reply = sendPackToOISM(tmpBuffer, dataOFD, FnSN, mOismSettings);
							if (reply != null) {
								if (!mFNManager.getFN().writeOFDReplyOISM(tmpBuffer, s.getFirstUnsentNumber(), reply)) {
									Logger.e("Ошибка записи ответа ОИСМ");
								}
								break;
							}
							if (++failCnt > 10)
								break;
							Thread.sleep(500);
						}
					} finally {
						mWakeLockPower.releaseWakeLock();
						fN.updateOISMStatus(s);
					}
				}

				mSendNow = false;
			} catch (InterruptedException ie) {
				break;
			} catch (Exception e) {
				Logger.e(e, "Ошибка выполнения");
			}
		}
		BufferFactory.release(tmpBuffer);
		mFNManager.removeFNChangedListener(this);
		mFNManager.removeFNReadyListener(this);
		Logger.i("Отправка в ОИСМ остановлена");
	}

	private static byte[] prepareOISMDocument(ByteBuffer tmpBuffer, byte[] dataFN, String FNSN) {
		tmpBuffer.clear();
		tmpBuffer.put(SIGNATURE_OISM);
		tmpBuffer.put(PROTOCOL_VERSION_OISM_S);
		tmpBuffer.put(PROTOCOL_VERSION_OISM_P);

		tmpBuffer.put(FNSN.getBytes(Const.ENCODING));
		tmpBuffer.putShort((short) (dataFN.length & 0xFFFF));
		tmpBuffer.putShort(MSG_FLAGS);
		tmpBuffer.putShort((short) 0);
		tmpBuffer.put(dataFN);

		byte[] outData = new byte[tmpBuffer.position()];
		System.arraycopy(tmpBuffer.array(), 0, outData, 0, outData.length);
		return outData;
	}

	public static byte[] sendPackToOISM(ByteBuffer tmpBuffer, byte[] pack, String FNSN,
			DocServerSettings mOFDSettings) {
		try {
			Logger.i("Соединение с ОФД ОИСМ %s : %s ", mOFDSettings.getServerAddress(), mOFDSettings.getServerPort());

			try (Socket socket = new Socket(mOFDSettings.getServerAddress(), mOFDSettings.getServerPort())) {
				socket.setSoTimeout(mOFDSettings.getServerTimeout() * 1000);
				OutputStream os = socket.getOutputStream();
				InputStream is = socket.getInputStream();
				os.write(pack);
				os.flush();

				if (LOG_PRINT) {
					Logger.i(">> OFD OISM %s", Utils.dump(pack));
				}

				int read = is.read(tmpBuffer.array());
				if (read > (SIGNATURE_OISM.length + PROTOCOL_VERSION_OISM_S.length)) {
					if (LOG_PRINT) {
						Logger.i("OFD OISM << \n %s", Utils.dump(tmpBuffer.array(), 0, read));
					}

					tmpBuffer.limit(read);
					tmpBuffer.position(0);

					for (byte b : SIGNATURE_OISM)
						if (tmpBuffer.get() != b)
							return null;

					for (byte b : PROTOCOL_VERSION_OISM_S)
						if (tmpBuffer.get() != b)
							return null;

					tmpBuffer.getShort();
					byte[] fn = new byte[16];
					tmpBuffer.get(fn);

					if (!new String(fn, Const.ENCODING).equals(FNSN))
						return null;

					int size = readUint16LE(tmpBuffer);
					tmpBuffer.getInt();
					byte[] answer = new byte[size];
					tmpBuffer.get(answer);

					Logger.i("Документ передан в ОФД ОИСМ");
					return answer;
				} else {
					Logger.e("ОФД ОИСМ не принял документ, ошибка: %s", read);
				}
				return null;

			}

		} catch (IOException ioe) {
			Logger.e(ioe, "Ошибка ОФД ОИСМ");
			return null;
		}
	}
}
