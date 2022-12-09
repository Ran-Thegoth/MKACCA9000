package rs.mkacca.hw.payment.engines;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.lanter.lan4gate.ICommunicationCallback;
import org.lanter.lan4gate.IErrorCallback;
import org.lanter.lan4gate.INotification;
import org.lanter.lan4gate.INotificationCallback;
import org.lanter.lan4gate.IRequest;
import org.lanter.lan4gate.IResponse;
import org.lanter.lan4gate.IResponseCallback;
import org.lanter.lan4gate.Lan4Gate;
import org.lanter.lan4gate.Implementation.Messages.Requests.Operations.SaleOperations.QuickPayment;
import org.lanter.lan4gate.Implementation.Messages.Requests.Operations.VoidOperations.Interrupt;
import org.lanter.lan4gate.Messages.OperationsList;
import org.lanter.lan4gate.Messages.Fields.NotificationsList;
import org.lanter.lan4gate.Messages.Fields.StatusList;

import com.google.zxing.BarcodeFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix.ScaleToFit;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import cs.U;
import rs.data.PayInfo;
import rs.fncore.Const;
import rs.fncore.data.Payment;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.payment.EPayment;
import rs.utils.Utils;
import rs.log.Logger;

public class Lanter extends EPayment
		implements ICommunicationCallback, IResponseCallback, IErrorCallback, INotificationCallback, Handler.Callback {

	public static final Lanter SHARER_INSTANCE = new Lanter();
	public static final String ENGINE_NAME = "Лантер";
	private Lan4Gate _gate;
	private Handler _h;
	protected EPaymentListener _listener;
	private IResponse _lastResponse;
	private Exception _lastException;
	protected OperationType _op;
	private AtomicInteger RRN = new AtomicInteger(1);
	private PayInfo _pResult;
	protected BigDecimal _sum;

	private static final int MSG_HIDE_DIALOG = 1;
	private static final int MSG_SET_MESSAGE = 2;
	private static final int MSG_SET_QR = 3;
	private static final int MSG_SELECT_PAYMENT = 4;
	private static final int MSG_WAIT_USER = 5;
	private static final int MSG_HIDE_WAIT_DIALOG = 6;
	private static final int MSG_BUILD_DIALOG = 7;

	
	private int _paymentType = 0;
	
	private static final String MERCH_ID = "mId";
	private static final String USE_SBP = "sbp";
	private static final String NUM_SLIPS = "slips";
	private static final String SLIP_PAUSE = "sPause";
	private static final String USE_PING = "ping";
	

	private enum ConnectionState {
		NOT_CONNECTED, CONNECTING, CONNECTED, CANCELED
	};

	private enum TransactionState {
		PROCESSING, FAIL, DONE,CANCELED
	}

	private String _lVersion;

	private volatile ConnectionState _cState = ConnectionState.CONNECTING;
	private volatile TransactionState _rState;
	private AlertDialog _dialog;
	private TextView _message;
	private ImageView _qr;

	public Lanter() {
		super();
		_h = new Handler(Core.getInstance().getMainLooper(), this);
		try {
			PackageInfo nfo = Core.getInstance().getPackageManager().getPackageInfo("org.lanter.hits", 0);
			_lVersion = nfo.versionName;
			Logger.e("LanterPOS: Версия службы " + _lVersion);
		} catch (NameNotFoundException nfe) {
			Logger.e("LanterPOS: Приложение не установлено");
			_lVersion = null;
			return;
		}
		_gate = new Lan4Gate(1);
		_gate.addResponseCallback(Lanter.this);
		_gate.addErrorCallback(Lanter.this);
		_gate.addNotificationCallback(Lanter.this);
		_gate.addCommunicationCallback(Lanter.this);
		_gate.setPort(20501);
	}

	private int getMerchantId() {
		try {
			return _settings.getInt(MERCH_ID);
		} catch(JSONException jse) {
			return 1;
		}
	}
	private int getNumSlips() {
		try {
			return _settings.getInt(NUM_SLIPS);
		} catch(JSONException jse) {
			return 1;
		}
		
	}
	private boolean isSBPUsed() {
		try {
			return _settings.getBoolean(USE_SBP);
		} catch(JSONException jse) {
			return false;
		}
	}
	private boolean pauseAfterSlip() {
		try {
			return _settings.getBoolean(SLIP_PAUSE);
		} catch(JSONException jse) {
			return true;
		}
		
	}
	private boolean usePing() {
		try {
			return _settings.getBoolean(USE_PING);
		} catch(JSONException jse) {
			return true;
		}
		
	}
	
	private Queue<IRequest> _queue = new LinkedList<>();
	
	
	private class WaitRef {
		volatile boolean value;
		AlertDialog _dialog;
		public WaitRef(boolean val) { value = val; }
	}
	private class PROCESSOR extends Thread {
		
		private void awaitUser() {
			long opend = System.currentTimeMillis() + 10000L;
			Message msg = _h.obtainMessage(MSG_WAIT_USER);
			WaitRef confirm = new WaitRef(false); 
			msg.obj = confirm;
			_h.sendMessage(msg);
			while(!confirm.value && System.currentTimeMillis() < opend) 
				try { Thread.sleep(100); } catch(InterruptedException ie) { }
			msg = _h.obtainMessage(MSG_HIDE_WAIT_DIALOG);
			msg.obj = confirm;
			_h.sendMessage(msg);
		}
		
		private boolean awaitConnection() {
			_lastException = new Exception("Истекло время ожидания соединения с сервисом");
			long opend = System.currentTimeMillis() + 180000L;
			_cState = ConnectionState.CONNECTING;
			_gate.start();
			while(_cState == ConnectionState.CONNECTING && System.currentTimeMillis() < opend) 
				try { Thread.sleep(100); } catch(InterruptedException ie) { return false; }
			return _cState == ConnectionState.CONNECTED;
		}
		private boolean awaitOperation(IRequest rq) {
			if(_cState != ConnectionState.CONNECTED) return false;
			long opend = System.currentTimeMillis() + 120000L;
			if(rq instanceof Interrupt) opend = System.currentTimeMillis() +3000L;
			if(rq instanceof QuickPayment) opend += 60000L;
			_rState = TransactionState.PROCESSING;
			rq.setEcrNumber(RRN.getAndIncrement());
			_gate.sendRequest(rq);
			while(_rState == TransactionState.PROCESSING && System.currentTimeMillis() < opend) 
				try { Thread.sleep(100); } catch(InterruptedException ie) { return false; }
			return _rState == TransactionState.DONE;
		}
		
		@Override
		public void run() {
			
			try {
				if(!awaitConnection()) return;
				showMessage("Выполнение операции");
				while(!_queue.isEmpty()) {
					if(!awaitOperation(_queue.poll())) break;
				}
				if(_rState != TransactionState.DONE)
					awaitOperation(Lan4Gate.getPreparedRequest(OperationsList.Interrupt));
			} finally {
				_h.sendEmptyMessage(MSG_HIDE_DIALOG);
				if(_lastException != null)
					_h.post(new Runnable() {
						@Override
						public void run() {
							_listener.onOperationFail(Lanter.this, _op, _lastException);
						}
					});
				else {
					if(_op != OperationType.SETTLEMENT) {
							if(pauseAfterSlip())
								awaitUser();
							for(int i=1;i<getNumSlips();i++) {
								awaitOperation(Lan4Gate.getPreparedRequest(OperationsList.PrintLastReceipt));
								if(pauseAfterSlip())
									awaitUser();
							}
					}
					_h.post(new Runnable() {
						@Override
						public void run() {
							_listener.onOperationSuccess(Lanter.this, _op, _pResult, _sum);
						}
					});
				}
				_gate.stop();
			}
		}
	};
	
	protected void startOperation(final Context ctx, IRequest... requests) {
		ctx.sendBroadcast(new Intent("org.lanter.START_SERVICE"));
		_queue.clear();
		for(IRequest rq : requests) _queue.add(rq);
		Message m = _h.obtainMessage(MSG_BUILD_DIALOG);
		m.obj = ctx;
		_h.sendMessage(m);
		new PROCESSOR().start();
	}

	@Override
	public void communicationStarted(Lan4Gate initiator) {
	}

	@Override
	public void connected(Lan4Gate initiator) {
		if(_cState != ConnectionState.CONNECTING) return;
		if(usePing())
			showMessage("Проверка готовности сервиса");
		else
			_cState = ConnectionState.CONNECTED;
	}

	@Override
	public void communicationStopped(Lan4Gate initiator) {
	}

	@Override
	public void disconnected(Lan4Gate initiator) {
		_cState = ConnectionState.NOT_CONNECTED;
	}

	@Override
	public void errorException(Exception exception, Lan4Gate initiator) {
		_lastException = exception;
		_cState = ConnectionState.NOT_CONNECTED;
	}

	@Override
	public void newNotificationMessage(INotification notification, Lan4Gate initiator) {
		Message msg;
		if(notification.getNotificationCode() == NotificationsList.QrShowing) {
			msg = _h.obtainMessage(MSG_SET_QR);
		} else
			msg = _h.obtainMessage(MSG_SET_MESSAGE);
		msg.obj = notification.getMessage();
		_h.sendMessage(msg);
	}

	@Override
	public void newResponseMessage(IResponse response, Lan4Gate initiator) {
		_lastException = null;
		_lastResponse = response;
		_rState = response.getStatus() == StatusList.Success ? TransactionState.DONE : TransactionState.FAIL;
		if(_rState == TransactionState.DONE) {
			switch(response.getOperationCode()) {
			case QuickPayment:
			case Sale:
			case Refund:
			case Void:
				_pResult = new PayInfo(response.getReceiptReference(), response.getRRN());
				break;
			default:
				break;
			}
		}
		if(_rState == TransactionState.FAIL) {
			String s = response.getResponseText();
			if(s == null || s.isEmpty())
				s = "Ошибка транзакции: "+response.getStatus().getNumber();
			_lastException = new Exception(s);
		}
		
	}

	@Override
	public void errorMessage(String error, Lan4Gate initiator) {
		Log.i("fncore2", "errorMessage() " + error);
		_lastException = new Exception(error);
		_rState = TransactionState.FAIL;
	}

	@Override
	public boolean handleMessage(final Message msg) {
		switch (msg.what) {
		case MSG_BUILD_DIALOG: {
			AlertDialog.Builder b = new AlertDialog.Builder((Context)msg.obj);
			View v = LayoutInflater.from((Context)msg.obj).inflate(R.layout.lanter_op, new LinearLayout((Context)msg.obj),false);
			_message = v.findViewById(R.id.l_hint);
			_qr = v.findViewById(R.id.l_qr);
			_message.setText("Соединение с сервисом...");
			b.setView(v);
			b.setNegativeButton(android.R.string.cancel, null);
			_dialog = b.create();
			_dialog.setCancelable(false);
			_dialog.setCanceledOnTouchOutside(false);
			_dialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface arg0) {
					final View ok = _dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
					ok.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View arg0) {
							ok.setEnabled( false );
							_message.setText("Отмена операции");
							if(_cState == ConnectionState.CONNECTING)
								_cState = ConnectionState.CANCELED;
							if(_rState == TransactionState.PROCESSING)
								_rState = TransactionState.CANCELED;
						}
					});
					
				}
			});
			_dialog.show();
		}
			break;
		case MSG_HIDE_DIALOG:
			if(_dialog != null)
				_dialog.dismiss();
			_dialog = null;
			break;
		case MSG_WAIT_USER: {
			final WaitRef ref = (WaitRef)msg.obj;
			AlertDialog.Builder b = new AlertDialog.Builder(_message.getContext());
			b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					ref.value = true;
				}
			});
			b.setMessage("Оторвите чек");
			ref._dialog = b.create();
			ref._dialog.setCancelable(false);
			ref._dialog.setCanceledOnTouchOutside(false);
			ref._dialog.show();
		}
		break;
		case MSG_HIDE_WAIT_DIALOG: {
			WaitRef ref = (WaitRef)msg.obj;
			if(ref._dialog.isShowing())
				ref._dialog.dismiss();
		}
		break;
		case MSG_SELECT_PAYMENT: {
			ArrayAdapter<String> a = new ArrayAdapter<>((Context)msg.obj, android.R.layout.simple_list_item_1);
			a.add("Банковская карта");
			a.add("Система быстрых платежей");
			AlertDialog.Builder b = new AlertDialog.Builder((Context)msg.obj);
			b.setAdapter(a, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int p) {
					_paymentType = p +1;
				}
			});
			b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					_paymentType = -1;
				}
			});
			b.setTitle("Способ оплаты");
			AlertDialog dlg = b.create();
			dlg.setCancelable(false);
			dlg.setCanceledOnTouchOutside(false);
			dlg.show();
		}
			break;
		case MSG_SET_QR: 
			_qr.setImageBitmap(Utils.encodeAsBitmap(msg.obj.toString(), 500, 500, BarcodeFormat.QR_CODE, 0));
			_qr.setVisibility(View.VISIBLE);
			_message.setText("Сосканируйте код оплаты");
			break;
		case MSG_SET_MESSAGE:
			_message.setText(msg.obj.toString());
			break;
		}
		return true;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_lanter;
	}

	@Override
	public String toString() {
		return ENGINE_NAME;
	}

	@Override
	public void doPayment(final Context ctx, final BigDecimal sum, final EPaymentListener listener) {
		if(isSBPUsed()) {
			new Thread() { 
				public void run() {
					Message msg = _h.obtainMessage(MSG_SELECT_PAYMENT);
					_paymentType = 0;
					msg.obj = ctx;
					_h.sendMessage(msg);
					long end = System.currentTimeMillis() + 15000;
					while(_paymentType == 0 && System.currentTimeMillis() < end) try { Thread.sleep(100); } catch(InterruptedException ie) { }
					if(_paymentType <= 0) {
						_h.post(new Runnable() {
							@Override
							public void run() {
								listener.onOperationFail(Lanter.this, OperationType.PAYMENT, new Exception("Операция отменена"));
							}
						});
						return;
					}
					_h.post(new Runnable()  {
						@Override
						public void run() {
							doPayment(Lan4Gate.getPreparedRequest(_paymentType == 1 ? OperationsList.Sale : OperationsList.QuickPayment),ctx,sum,listener);
						}
					});
				};
			}.start();
		} else
			doPayment(Lan4Gate.getPreparedRequest(OperationsList.Sale), ctx, sum,listener);
	}
	
	public void doPayment(IRequest rq, Context ctx, BigDecimal sum, EPaymentListener listener) {
		rq.setCurrencyCode(643);
		rq.setEcrMerchantNumber(getMerchantId());
		rq.setAmount(sum.multiply(BigDecimal.valueOf(100)).longValue());
		_listener = listener;
		_op = OperationType.PAYMENT;
		_sum = sum;
		startOperation(ctx, rq,Lan4Gate.getPreparedRequest(OperationsList.FinalizeTransaction));
	}

	@Override
	public void doRefund(Context ctx, BigDecimal sum, PayInfo pay, EPaymentListener listener) {
		IRequest rq =  Lan4Gate.getPreparedRequest(OperationsList.Refund); 
		rq.setCurrencyCode(643);
		rq.setEcrMerchantNumber(getMerchantId());
		rq.setAmount(sum.multiply(BigDecimal.valueOf(100)).longValue());
		rq.setRRN(pay.rrn());
		_listener = listener;
		_op = OperationType.REFUND;
		_sum = sum;
		startOperation(ctx, rq,Lan4Gate.getPreparedRequest(OperationsList.FinalizeTransaction));

	}

	@Override
	public void doCancel(Context ctx, PayInfo pay, EPaymentListener listener) {
		IRequest rq = Lan4Gate.getPreparedRequest(OperationsList.Void); rq.setEcrNumber(1);
		rq.setReceiptReference(pay.number()); 
		rq.setEcrMerchantNumber(getMerchantId());
		_listener = listener;
		_op = OperationType.CANCEL;
		_sum = BigDecimal.ZERO;
		startOperation(ctx, rq,Lan4Gate.getPreparedRequest(OperationsList.FinalizeTransaction));
	}

	@Override
	public void setup(LinearLayout holder) {
		View v = LayoutInflater.from(holder.getContext()).inflate(R.layout.lanter, holder, false);
		TextView tv = v.findViewById(R.id.lb_ttk_version);
		Switch sw = v.findViewById(R.id.l_pause);
		sw.setChecked(pauseAfterSlip());
		sw = v.findViewById(R.id.l_sbp);
		sw.setChecked(isSBPUsed());
		sw = v.findViewById(R.id.l_ping);
		sw.setChecked(usePing());
		EditText e = v.findViewById(R.id.l_merch_id);
		e.setText(String.valueOf(getMerchantId()));
		e = v.findViewById(R.id.l_slip_numbers);
		e.setText(String.valueOf(getNumSlips()));
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
						public void onOperationSuccess(EPayment engine, OperationType type, PayInfo pay,
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
		_pResult = null;
		_listener = listener;
		_sum = BigDecimal.ZERO;
		_op = OperationType.SETTLEMENT;
		startOperation(ctx,  Lan4Gate.getPreparedRequest(OperationsList.Settlement));
	}

	@Override
	public boolean applySetup(LinearLayout holder) {
		EditText e =  holder.findViewById(R.id.l_merch_id);
		try {
			_settings.put(MERCH_ID, Integer.parseInt(e.getText().toString()));
			e =  holder.findViewById(R.id.l_slip_numbers);
			_settings.put(NUM_SLIPS, Integer.parseInt(e.getText().toString()));
			Switch sw = holder.findViewById(R.id.l_sbp);
			_settings.put(USE_SBP,sw.isChecked());
			sw = holder.findViewById(R.id.l_pause);
			_settings.put(SLIP_PAUSE,sw.isChecked());
			sw = holder.findViewById(R.id.l_ping);
			_settings.put(USE_PING,sw.isChecked());
			store();
			return true;
		} catch(Exception err) {
			return false;
		}
	}

	@Override
	public boolean isRefunded() {
		return true;
	}

	@Override
	public void newControlMessage(Lan4Gate initiator) {
		_cState = ConnectionState.CONNECTED;
	}
	
	private void showMessage(String m) {
		Message msg = _h.obtainMessage(MSG_SET_MESSAGE);
		msg.obj = m;
		_h.sendMessage(msg);
	}
}
