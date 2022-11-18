package rs.mkacca.hw.payment.engines;

import java.math.BigDecimal;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONException;
import org.lanter.lan4gate.ICommunicationCallback;
import org.lanter.lan4gate.IErrorCallback;
import org.lanter.lan4gate.INotification;
import org.lanter.lan4gate.INotificationCallback;
import org.lanter.lan4gate.IRequest;
import org.lanter.lan4gate.IResponse;
import org.lanter.lan4gate.IResponseCallback;
import org.lanter.lan4gate.Lan4Gate;
import org.lanter.lan4gate.Messages.OperationsList;
import org.lanter.lan4gate.Messages.Fields.StatusList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import rs.fncore.Const;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.payment.EPayment;
import rs.log.Logger;

public class Lanter extends EPayment
		implements ICommunicationCallback, IResponseCallback, IErrorCallback, INotificationCallback, Handler.Callback {

	public static final String ENGINE_NAME = "Лантер: Сбер";
	private Lan4Gate _gate;
	private Handler _h;
	private EPaymentListener _listener;
	private IResponse _lastResponse;
	private Exception _lastException;
	private OperationType _op;
	private String _rrn;
	private BigDecimal _sum;
	private ProgressDialog _dialog;

	private static final int MSG_CONN_FAIL = 1;
	private static final int MSG_OP_FAIL = 2;
	private static final int MSG_OP_SUCCESS = 3;
	private static final int MSG_SET_MESSAGE = 4;

	private Queue<IRequest> _requests = new ConcurrentLinkedQueue<>();

	private enum ConnectionState {
		NOT_CONNECTED, CONNECTING, CONNECTED
	};

	private enum TransactionState {
		PROCESSING, FAIL, DONE,CANCELED
	}

	private String _lVersion;

	private volatile ConnectionState _cState = ConnectionState.NOT_CONNECTED;
	private volatile TransactionState _rState;

	public Lanter() {
		try {
			PackageInfo nfo = Core.getInstance().getPackageManager().getPackageInfo("org.lanter.hits", 0);
			_lVersion = nfo.versionName;
			Logger.e("LanterPOS: Версия службы " + _lVersion);
		} catch (NameNotFoundException nfe) {
			Logger.e("LanterPOS: Приложение не установлено");
			_lVersion = null;
		}
		_h = new Handler(Core.getInstance().getMainLooper(), this);
		_gate = new Lan4Gate(1);
		_gate.addResponseCallback(Lanter.this);
		_gate.addErrorCallback(Lanter.this);
		_gate.addNotificationCallback(Lanter.this);
		_gate.addCommunicationCallback(Lanter.this);
		_gate.setPort(20501);
		
	}

	private Runnable PROCESSOR = new Runnable() {


		private void showMessage(String msg) {
			Message m = _h.obtainMessage(MSG_SET_MESSAGE);
			m.obj = msg;
			_h.sendMessage(m);
			
		}
		private boolean awaitState(ConnectionState s) {
			long opEnd = System.currentTimeMillis() + 15000L;
			while (_cState != s && System.currentTimeMillis() < opEnd)
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
				}
			return _cState == s;
		}

		private boolean awaitTransactionSuccess() {
			return awaitTransactionSuccess(60000L);
		}
		
		private boolean awaitTransactionSuccess(long time) {
			long opEnd = System.currentTimeMillis() + time;
			while (_rState == TransactionState.PROCESSING && System.currentTimeMillis() < opEnd)
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
				}
			if(_rState == TransactionState.PROCESSING)
				_lastException = new Exception("Истекло время ожидания");
			return _rState == TransactionState.DONE;
		}

		@Override
		public void run() {
			Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE"));
			do {
				showMessage("Подключение к сервису...");
				_cState = ConnectionState.CONNECTING;
				_gate.start();
				if (!awaitState(ConnectionState.CONNECTED))
					break;
				
				Log.d("fncore2", "Processing requests");
				while (!_requests.isEmpty()) {
					if(_rState == TransactionState.CANCELED) break;
					IRequest rq = _requests.poll();
					_rState = TransactionState.PROCESSING;
					_gate.sendRequest(rq);
					if (!awaitTransactionSuccess())
						break;
				}
			} while (false);
			Message msg;
			if (_cState != ConnectionState.CONNECTED)
				msg = _h.obtainMessage(MSG_CONN_FAIL);
			else if (_rState == TransactionState.DONE)
				msg = _h.obtainMessage(MSG_OP_SUCCESS);
			else {
				msg = _h.obtainMessage(MSG_OP_FAIL);
				if(_rState == TransactionState.CANCELED) {
					Log.d("fncore2", "Interrupt operation...");
					showMessage("Отмена операции...");
					_rState = TransactionState.PROCESSING;
					_gate.sendRequest(Lan4Gate.getPreparedRequest(OperationsList.Interrupt));
					awaitTransactionSuccess(3000);
				}
			}
			_h.sendMessage(msg);
			_gate.stop();
		}
	};

	private void startOperation(final Context ctx, final String msg, IRequest... requests) {
		_requests.clear();
		for (IRequest rq : requests)
			_requests.add(rq);
		_h.post(new Runnable() {
			public void run() {
				_dialog = new ProgressDialog(ctx);
				_dialog.setIndeterminate(true);
				_dialog.setMessage(msg);
				_dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Отмена", (DialogInterface.OnClickListener)null);
				_dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface arg0) {
						final View cancel = _dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
						cancel.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View arg0) {
								_rState = TransactionState.CANCELED;
								_lastException = new CanceledByUserException();
								Log.d("fncore2", "Cancel operation now...");
								cancel.setEnabled(false);
							}
						});
						
					}
				});
				_dialog.setCancelable(false);
				_dialog.setCanceledOnTouchOutside(false);

				_dialog.show();
			}
		});
		new Thread(PROCESSOR).start();
	}

	@Override
	public void communicationStarted(Lan4Gate initiator) {
	}

	@Override
	public void connected(Lan4Gate initiator) {
		Log.i("fncore2", "connected()");
		_cState = ConnectionState.CONNECTED;
	}

	@Override
	public void communicationStopped(Lan4Gate initiator) {
		_cState = ConnectionState.NOT_CONNECTED;
	}

	@Override
	public void disconnected(Lan4Gate initiator) {
		Log.i("fncore2", "disconnected()");
		_cState = ConnectionState.NOT_CONNECTED;
		_gate = null;
	}

	@Override
	public void errorException(Exception exception, Lan4Gate initiator) {
		Log.i("fncore2", "errorException() " + exception.getLocalizedMessage(),exception);
		_lastException = exception;
		_cState = ConnectionState.NOT_CONNECTED;
	}

	@Override
	public void newNotificationMessage(INotification notification, Lan4Gate initiator) {
		Log.i("fncore2", "newNotificationMessage() " + notification.getMessage());
		Message msg = _h.obtainMessage(MSG_SET_MESSAGE);
		msg.obj = notification.getMessage();
		_h.sendMessage(msg);
	}

	@Override
	public void newResponseMessage(IResponse response, Lan4Gate initiator) {
		Log.i("fncore2",
				"newResponseMessage() " + response.getClass().getSimpleName() + " status: " + response.getStatus());
		_lastException = null;
		_lastResponse = response;
		_rState = response.getStatus() == StatusList.Success ? TransactionState.DONE : TransactionState.FAIL;
		if(_rState == TransactionState.FAIL)
			_lastException = new Exception(response.getResponseText());
	}

	@Override
	public void errorMessage(String error, Lan4Gate initiator) {
		Log.i("fncore2", "errorMessage() " + error);
		_lastException = new Exception(error);
		_rState = TransactionState.FAIL;
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_CONN_FAIL:
			_lastException = new Exception("Истекло время соединения с сервисом Lan-4TAP");
			_h.post(new Runnable() {
				@Override
				public void run() {
					_listener.onOperationFail(Lanter.this, _op, _lastException);
				}
			});
			break;
		case MSG_SET_MESSAGE:
			if (_dialog != null)
				_dialog.setMessage(msg.obj.toString());
			return true;
		case MSG_OP_FAIL:
			if (_lastException == null)
				_lastException = new Exception("Ошибка обработки транзакции");
			_h.post(new Runnable() {
				@Override
				public void run() {
					_listener.onOperationFail(Lanter.this, _op, _lastException);
				}
			});
			break;
		case MSG_OP_SUCCESS:
			_h.post(new Runnable() {
				@Override
				public void run() {
					_listener.onOperationSuccess(Lanter.this, _op, _rrn, _sum);
				}
			});
			break;

		}
		if (_dialog != null) {
			_dialog.dismiss();
			_dialog = null;
		}

		return true;
	}

	/*
	 * private void connect() { if (_lVersion == null) return;
	 * Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE")); new
	 * Thread() {
	 * 
	 * @Override public void run() { if (_cState != ConnectionState.NOT_CONNECTED)
	 * _gate.stop(); while (_cState != ConnectionState.NOT_CONNECTED) try {
	 * Thread.sleep(100); } catch (InterruptedException ie) { return; } _cState =
	 * ConnectionState.CONNECTING; _gate.setPort(getPort());
	 * Logger.i("LanterPOS: Устанавливается соединение..."); _gate.start(); }
	 * }.start(); }
	 * 
	 * @Override public void doPayment(Context ctx, BigDecimal sum, EPaymentListener
	 * listener) { if (_cState != ConnectionState.CONNECTED) {
	 * listener.onOperationFail(this, OperationType.PAYMENT, new
	 * Exception("Сервис Лантер недоступен")); return; }
	 * Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE")); _op
	 * = OperationType.PAYMENT; _sum = sum; _listener = listener;
	 * startOperation(ctx, "Оплата", new Runnable() {
	 * 
	 * @Override public void run() { IRequest rq =
	 * _gate.getPreparedRequest(OperationsList.Sale); rq.setCurrencyCode(643);
	 * rq.setEcrMerchantNumber(1);
	 * rq.setAmount(_sum.multiply(BigDecimal.valueOf(100)).longValue());
	 * processRequest(rq); _h.sendEmptyMessage(_rState.ordinal()); } });
	 * 
	 * }
	 * 
	 * @Override public void doRefund(Context ctx, BigDecimal sum, final String rrn,
	 * EPaymentListener listener) { if (_cState != ConnectionState.CONNECTED) {
	 * listener.onOperationFail(this, OperationType.REFUND, new
	 * Exception("Сервис Лантер недоступен")); return; } _op =
	 * OperationType.PAYMENT; _sum = sum; _listener = listener;
	 * Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE"));
	 * startOperation(ctx, "Возврат", new Runnable() {
	 * 
	 * @Override public void run() { IRequest rq =
	 * _gate.getPreparedRequest(OperationsList.Refund); if(rrn != null &&
	 * !rrn.isEmpty()) rq.setRRN(rrn); rq.setCurrencyCode(643);
	 * rq.setEcrMerchantNumber(1);
	 * rq.setAmount(_sum.multiply(BigDecimal.valueOf(100)).longValue());
	 * processRequest(rq); _h.sendEmptyMessage(_rState.ordinal()); } }); }
	 * 
	 * @Override public void doCancel(Context ctx, final String rrn,
	 * EPaymentListener listener) { if (_cState != ConnectionState.CONNECTED) {
	 * listener.onOperationFail(this, OperationType.CANCEL, new
	 * Exception("Сервис Лантер недоступен")); return; }
	 * Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE")); _op
	 * = OperationType.PAYMENT; _sum = BigDecimal.ZERO; _listener = listener;
	 * startOperation(ctx, "Оплата", new Runnable() { public void run() { IRequest
	 * rq = _gate.getPreparedRequest(OperationsList.Void); rq.setEcrNumber(1);
	 * rq.setReceiptReference(rrn); rq.setEcrMerchantNumber(1); processRequest(rq);
	 * _h.sendEmptyMessage(_rState.ordinal()); }; });
	 * 
	 * }
	 * 
	 * @SuppressWarnings("deprecation") private void startOperation(Context ctx,
	 * String msg, Runnable r) { _dialog = new ProgressDialog(ctx);
	 * _dialog.setIndeterminate(true); _dialog.setMessage(msg);
	 * _dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Отмена", new
	 * DialogInterface.OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface arg0, int arg1) { if(_rState ==
	 * TransactionState.PROCESSING) { _rState = TransactionState.FAIL;
	 * _lastException = new CanceledByUserException();
	 * _gate.sendRequest(_gate.getPreparedRequest(OperationsList.Interrupt)); } }
	 * }); _dialog.setCancelable(false); _dialog.setCanceledOnTouchOutside(false);
	 * _h.post(new Runnable() { public void run() { _dialog.show(); }}); new
	 * Thread(r).start(); }
	 * 
	 * @Override public void setup(LinearLayout holder) { View v =
	 * LayoutInflater.from(holder.getContext()).inflate(R.layout.lanter, holder,
	 * false); TextView tv = v.findViewById(R.id.lb_ttk_version); if (_lVersion ==
	 * null) { tv.setText("Не установлен");
	 * v.findViewById(R.id.v_settlement).setEnabled(false); } else {
	 * tv.setText(_lVersion);
	 * v.findViewById(R.id.v_settlement).setOnClickListener(new
	 * View.OnClickListener() {
	 * 
	 * @Override public void onClick(final View v) {
	 * requestSettlement(v.getContext(), new EPayment.EPaymentListener() {
	 * 
	 * @Override public void onOperationSuccess(EPayment engine, OperationType type,
	 * String rrn, BigDecimal sum) { Toast.makeText(v.getContext(),
	 * "Выполнено успешно", Toast.LENGTH_SHORT).show(); }
	 * 
	 * @Override public void onOperationFail(EPayment engine, OperationType type,
	 * Exception e) { U.notify(v.getContext(), "Ошибка сверки итогов:\n" +
	 * e.getLocalizedMessage()); } }); } }); } holder.addView(v, new
	 * LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	 * 
	 * }
	 * 
	 * @Override public boolean applySetup(LinearLayout holder) { if (isEnabled())
	 * connect(); return true; }
	 * 
	 * @Override public boolean isRefunded() { return true; }
	 * 
	 * @Override public void newNotificationMessage(final INotification
	 * notification, Lan4Gate initiator) { Core.getInstance().runOnUI(new Runnable()
	 * {
	 * 
	 * @SuppressWarnings("deprecation")
	 * 
	 * @Override public void run() { if(_dialog!=null && _dialog.isShowing())
	 * _dialog.setMessage(notification.getMessage());
	 * 
	 * } });
	 * 
	 * }
	 * 
	 * @Override public void errorMessage(String error, Lan4Gate initiator) {
	 * Logger.e("LanterPOS: Ошибка %s",error); if (_rState ==
	 * TransactionState.PROCESSING) { _lastException = new Exception(error); _rState
	 * = TransactionState.FAIL; }
	 * 
	 * }
	 * 
	 * 
	 */
	@Override
	public int getIconId() {
		return R.drawable.ic_lanter;
	}

	@Override
	public String toString() {
		return ENGINE_NAME;
	}

	@Override
	public void doPayment(Context ctx, BigDecimal sum, EPaymentListener listener) {
		IRequest rq =  Lan4Gate.getPreparedRequest(OperationsList.Sale); 
		rq.setCurrencyCode(643);
		rq.setEcrMerchantNumber(1);
		rq.setAmount(sum.multiply(BigDecimal.valueOf(100)).longValue());
		_listener = listener;
		_op = OperationType.PAYMENT;
		_sum = sum;
		startOperation(ctx, "Оплата...", rq,Lan4Gate.getPreparedRequest(OperationsList.FinalizeTransaction));
	}

	@Override
	public void doRefund(Context ctx, BigDecimal sum, String rrn, EPaymentListener listener) {
		IRequest rq =  Lan4Gate.getPreparedRequest(OperationsList.Refund); 
		rq.setCurrencyCode(643);
		rq.setEcrMerchantNumber(1);
		rq.setAmount(sum.multiply(BigDecimal.valueOf(100)).longValue());
		if(!rrn.isEmpty())
			rq.setRRN(rrn);
		_listener = listener;
		_op = OperationType.REFUND;
		_sum = sum;
		startOperation(ctx, "Возврат...", rq,Lan4Gate.getPreparedRequest(OperationsList.FinalizeTransaction));

	}

	@Override
	public void doCancel(Context ctx, String rrn, EPaymentListener listener) {
		IRequest rq = Lan4Gate.getPreparedRequest(OperationsList.Void); rq.setEcrNumber(1);
		rq.setReceiptReference(rrn); 
		rq.setEcrMerchantNumber(1);
		_listener = listener;
		_op = OperationType.CANCEL;
		_sum = BigDecimal.ZERO;
		startOperation(ctx, "Отмена...", rq,Lan4Gate.getPreparedRequest(OperationsList.FinalizeTransaction));
	}

	@Override
	public void setup(LinearLayout holder) {
		View v = LayoutInflater.from(holder.getContext()).inflate(R.layout.lanter, holder, false);
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
		_sum = BigDecimal.ZERO;
		_op = OperationType.SETTLEMENT;
		startOperation(ctx, "Сверка итогов...", _gate.getPreparedRequest(OperationsList.Settlement));
	}

	@Override
	public boolean applySetup(LinearLayout holder) {
		return true;
	}

	@Override
	public boolean isRefunded() {
		return true;
	}
}
