package rs.mkacca.hw.payment.engines;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cs.U;
import ibox.pro.sdk.external.PaymentContext;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentController.Currency;
import ibox.pro.sdk.external.PaymentController.PaymentError;
import ibox.pro.sdk.external.PaymentController.PaymentInputType;
import ibox.pro.sdk.external.PaymentController.ReaderEvent;
import ibox.pro.sdk.external.PaymentController.ReaderType;
import ibox.pro.sdk.external.PaymentControllerListener;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.hardware.reader.ITtkReaderHandler.Result;
import ibox.pro.sdk.external.hardware.reader.ReaderInfo;
import ibox.pro.sdk.external.hardware.reader.ReaderListener;
import ibox.pro.sdk.external.hardware.reader.ttk.TtkClient;
import rs.fncore.Const;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.payment.EPayment;
import rs.mkacca.hw.payment.EPayment.OperationType;

public class TTK extends EPayment implements ReaderListener, Handler.Callback {

	public static final String ENGINE_NAME = "iBox";
	private static final int MSG_OK = 1;
	private static final int MSG_FAIL = 2;
	private Handler _h;
	private String _lVersion;
	private OperationType _op;
	private EPaymentListener _listener;
	private BigDecimal _opSum;
	private TtkClient _ttk;

	private void sendFail(Exception e) {
		Message msg = _h.obtainMessage(MSG_FAIL);
		msg.obj = e;
		_h.sendMessage(msg);
	}

	public TTK() {
		_h = new Handler(Core.getInstance().getMainLooper(), this);
		try {
			PackageInfo pi = Core.getInstance().getPackageManager().getPackageInfo("com.devreactor.ibox.ttk", 0);
			_ttk = TtkClient.getInstance(Core.getInstance(), this);
			if (isEnabled())
				_ttk.start();

			_lVersion = pi.versionName;
		} catch (NameNotFoundException nfe) {

		}
	}

	@Override
	public void doPayment(Context ctx, BigDecimal sum, EPaymentListener listener) {
		_listener = listener;
		_op = OperationType.PAYMENT;
		_opSum = sum;
		if (_ttk == null) {
			sendFail(new Exception("Служба недоступна"));
			return;
		}
		try {
			_ttk.setTransactionData("1", 0, "100", "", "", sum, "RUB", "");
			Result r = _ttk.sale();
			if (r != null) {
				if (r.isSuccess()) {
					Message msg = _h.obtainMessage(MSG_OK);
					msg.obj = r.getErrorCode();
					_h.sendMessage(msg);
				} else
					sendFail(new Exception(r.getErrorMsg()));
			}
		} catch (Exception e) {

		}
	}

	@Override
	public void doRefund(Context ctx, BigDecimal sum, String rrn, EPaymentListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doCancel(Context ctx, String rrn, EPaymentListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setup(LinearLayout holder) {
		View v = LayoutInflater.from(holder.getContext()).inflate(R.layout.ibox, holder, false);
		TextView tv = v.findViewById(R.id.lb_ttk_version);
		if (_lVersion == null) {
			tv.setText("Не установлен");
			v.findViewById(R.id.v_settlement).setEnabled(false);
		} else {
			tv.setText(_lVersion);
			v.findViewById(R.id.v_settlement).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					requestSettlement(v.getContext(), new EPayment.EPaymentListener() {
						@Override
						public void onOperationSuccess(EPayment engine, OperationType type, String rrn,
								BigDecimal sum) {
							Toast.makeText(v.getContext(), "Выполнено успешно", Toast.LENGTH_SHORT).show();
						}

						@Override
						public void onOperationFail(EPayment engine, OperationType type, Exception e) {
							U.notify(v.getContext(), "Ошибка сверки итогов:\n" + e.getLocalizedMessage());
						}
					});
				}
			});
		}
		holder.addView(v, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	@Override
	public void requestSettlement(Context ctx, EPaymentListener listener) {
		_listener = listener;
		_op = OperationType.SETTLEMENT;
		_opSum = BigDecimal.ZERO;

		if (_ttk == null) {
			sendFail(new Exception("Служба недоступна"));
			return;
		}
		new Thread() {
			public void run() {
				try {
					Result r = _ttk.settlement("");
					if (r != null) {
						if (r.isSuccess()) {
							Message msg = _h.obtainMessage(MSG_OK);
							msg.obj = r.getErrorCode();
							_h.sendMessage(msg);
						} else
							sendFail(new Exception(r.getErrorMsg()));
					}
				} catch (Exception e) {
					Log.e("ibox", "requestSettlement", e);
					sendFail(e);
				}
			}
		}.start();

	}

	@Override
	public boolean applySetup(LinearLayout holder) {

		return true;
	}

	@Override
	public boolean isRefunded() {
		return true;
	}

	@Override
	public int getIconId() {
		return R.drawable.ibox;
	}

	@Override
	public boolean handleMessage(Message msg) {
		if (msg.what == MSG_OK)
			_listener.onOperationSuccess(this, _op, msg.obj.toString(), _opSum);
		else
			_listener.onOperationFail(null, _op, (Exception) msg.obj);
		return true;
	}

	@Override
	public void onAutoConfigError() {
		Log.d("ibox", "onAutoConfigError");
		// TODO Auto-generated method stub

	}

	@Override
	public void onAutoConfigFinished(String arg0, boolean arg1) {
		Log.d("ibox", "onAutoConfigFinished " + arg0 + " " + arg1);

	}

	@Override
	public void onAutoConfigProgress(double arg0) {
		Log.d("ibox", "onAutoConfigProgress " + arg0);

	}

	@Override
	public void onBatteryStatus(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCardSwiped(String arg0) {
		Log.d("ibox", "onCardSwiped " + arg0);

	}

	@Override
	public void onConnectionChanged(boolean arg0) {
		Log.d("ibox", "onConnectionChanged " + arg0);

	}

	@Override
	public void onEMVFinished(TransactionResult r, String s) {
		Log.d("ibox", "onEMVFinished " + r + " " + s);

	}

	@Override
	public void onEMVInserted() {
		Log.d("ibox", "onEMVInserted");

	}

	@Override
	public void onError() {
		Log.d("ibox", "onError");

	}

	@Override
	public void onEvent(ReaderEvent e) {
		Log.d("ibox", "onEvent " + e);

	}

	@Override
	public void onFinishMifareCardResult(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onHashPanReceived(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNFCDetected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOperateMifareCardResult(Hashtable<String, String> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPanReceived(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPinEntered(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPinPadPressed(PinPadKeyCode arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPinRequest() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPinTimeout() {
		Log.d("ibox", "onPinTimeout");

	}

	@Override
	public String onProcessOnline(String arg0) {
		Log.d("ibox", "onProcessOnline " + arg0);
		return null;
	}

	@Override
	public void onReadMifareCardResult(Hashtable<String, String> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReturnNFCApduResult(boolean arg0, String arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReturnPowerOffNFCResult(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReturnPowerOnNFCResult(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSearchMifareCardResult(Hashtable<String, String> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public int onSelectApplication(List<String> arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void onStartInit() {
		Log.d("ibox", "onStartInit");

	}

	@Override
	public void onStopInit(boolean v, ReaderInfo i) {
		Log.d("ibox", "onStopInit " + v + " " + i);

	}

	@Override
	public void onTransferMifareData(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onVerifyMifareCardResult(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onWaitingForCard() {
		Log.d("ibox", "onWaitingForCard");

	}

	@Override
	public void onWaitingForCardCanceled() {
		Log.d("ibox", "onWaitingForCardCanceled");

	}

	@Override
	public void onWriteMifareCardResult(boolean arg0) {
		// TODO Auto-generated method stub

	}
}
