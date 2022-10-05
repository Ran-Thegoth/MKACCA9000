package rs.mkacca.hw.payment.engines;

import java.math.BigDecimal;

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

	private enum ConnectionState {
		NOT_CONNECTED, CONNECTING, CONNECTED
	};

	private enum TransactionState {
		PROCESSING, FAIL, DONE
	}

	private String _lVersion;
	private ConnectionState _cState = ConnectionState.NOT_CONNECTED;
	private volatile TransactionState _rState;

	public Lanter() {

		try {
			PackageInfo nfo = Core.getInstance().getPackageManager().getPackageInfo("org.lanter.hits", 0);
			_lVersion = nfo.versionName;
		} catch (NameNotFoundException nfe) {
			_lVersion = null;
		}

		_h = new Handler(Core.getInstance().getMainLooper(), this);
		_gate = new Lan4Gate(1);
		_gate.addResponseCallback(this);
		_gate.addErrorCallback(this);
		_gate.addNotificationCallback(this);
		_gate.addCommunicationCallback(this);
		if (isEnabled())
			connect();
	}

	private int getPort() {
		return 20501;
	}

	private void connect() {
		if (_lVersion == null)
			return;
		Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE"));
		new Thread() {
			@Override
			public void run() {
				if (_cState != ConnectionState.NOT_CONNECTED)
					_gate.stop();
				while (_cState != ConnectionState.NOT_CONNECTED)
					try {
						Thread.sleep(100);
					} catch (InterruptedException ie) {
						return;
					}
				_cState = ConnectionState.CONNECTED;
				_gate.setPort(getPort());

				_gate.start();
			}
		}.start();
	}

	@Override
	public void doPayment(Context ctx, BigDecimal sum, EPaymentListener listener) {
		if (_cState != ConnectionState.CONNECTED) {
			listener.onOperationFail(this, OperationType.PAYMENT, new Exception("Сервис Лантер недоступен"));
			return;
		}
		Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE"));
		_op = OperationType.PAYMENT;
		_sum = sum;
		_listener = listener;
		startOperation(ctx, "Оплата", new Runnable() {
			@Override
			public void run() {
				IRequest rq = _gate.getPreparedRequest(OperationsList.Sale);
				rq.setCurrencyCode(643);
				rq.setEcrMerchantNumber(1);
				rq.setAmount(_sum.multiply(BigDecimal.valueOf(100)).longValue());
				processRequest(rq);
				_h.sendEmptyMessage(_rState.ordinal());
			}
		});

	}

	@Override
	public void doRefund(Context ctx, BigDecimal sum, final String rrn, EPaymentListener listener) {
		if (_cState != ConnectionState.CONNECTED) {
			listener.onOperationFail(this, OperationType.REFUND, new Exception("Сервис Лантер недоступен"));
			return;
		}
		_op = OperationType.PAYMENT;
		_sum = sum;
		_listener = listener;
		Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE"));
		startOperation(ctx, "Возврат", new Runnable() {
			@Override
			public void run() {
				IRequest rq = _gate.getPreparedRequest(OperationsList.Refund);
				if(rrn != null && !rrn.isEmpty())
					rq.setRRN(rrn);
				rq.setCurrencyCode(643);
				rq.setEcrMerchantNumber(1);
				rq.setAmount(_sum.multiply(BigDecimal.valueOf(100)).longValue());
				processRequest(rq);
				_h.sendEmptyMessage(_rState.ordinal());
			}
		});
	}

	@Override
	public void doCancel(Context ctx, final String rrn, EPaymentListener listener) {
		if (_cState != ConnectionState.CONNECTED) {
			listener.onOperationFail(this, OperationType.CANCEL, new Exception("Сервис Лантер недоступен"));
			return;
		}
		Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE"));
		_op = OperationType.PAYMENT;
		_sum = BigDecimal.ZERO;
		_listener = listener;
		startOperation(ctx, "Оплата", new Runnable() {
			public void run() {
				IRequest rq = _gate.getPreparedRequest(OperationsList.Void);
				rq.setEcrNumber(1);
				rq.setReceiptReference(rrn);
				rq.setEcrMerchantNumber(1);
				processRequest(rq);
				_h.sendEmptyMessage(_rState.ordinal());
			};
		});

	}
	
	@SuppressWarnings("deprecation")
	private void startOperation(Context ctx, String msg, Runnable r) {
		_dialog = new ProgressDialog(ctx);
		_dialog.setIndeterminate(true);
		_dialog.setMessage(msg);
		_dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Отмена", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				if(_rState == TransactionState.PROCESSING) {
					_rState = TransactionState.FAIL;
					_lastException = new CanceledByUserException();
					_gate.sendRequest(_gate.getPreparedRequest(OperationsList.Interrupt));
				}
			}
		});
		_dialog.setCancelable(false);
		_dialog.setCanceledOnTouchOutside(false);
		_dialog.show();
		new Thread(r).start();
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
	public boolean applySetup(LinearLayout holder) {
		if (isEnabled())
			connect();
		return true;
	}

	@Override
	public boolean isRefunded() {
		return true;
	}

	@Override
	public void newNotificationMessage(final INotification notification, Lan4Gate initiator) {
		Core.getInstance().runOnUI(new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				if(_dialog!=null && _dialog.isShowing())
					_dialog.setMessage(notification.getMessage());
				
			}
		});

	}

	@Override
	public void errorMessage(String error, Lan4Gate initiator) {
		Logger.e("LanterPOS: Ошибка %s",error);
		if (_rState == TransactionState.PROCESSING) {
			_lastException = new Exception(error);
			_rState = TransactionState.FAIL;
		}

	}

	@Override
	public void errorException(Exception exception, Lan4Gate initiator) {
		Logger.e(exception,"LanterPOS: Ошибка %s",exception.getLocalizedMessage());
		if (_rState == TransactionState.PROCESSING) {
			_lastException = exception;
			_rState = TransactionState.FAIL;
		}
	}

	@Override
	public void newResponseMessage(IResponse response, Lan4Gate initiator) {
		Logger.d("LanterPOS: Получен ответ от сервиса");
		if (_rState == TransactionState.PROCESSING) {
			_lastResponse = response;
			_rState = TransactionState.DONE;
		}

	}

	@Override
	public void communicationStarted(Lan4Gate initiator) {
		
	}

	@Override
	public void communicationStopped(Lan4Gate initiator) {
		
	}

	@Override
	public void connected(Lan4Gate initiator) {
		_cState = ConnectionState.CONNECTED;
	}

	@Override
	public void disconnected(Lan4Gate initiator) {
		_cState = ConnectionState.NOT_CONNECTED;
	}

	@Override
	public boolean handleMessage(Message msg) {
		if(_dialog != null && _dialog.isShowing()) 
			_dialog.dismiss();
		_dialog = null;
		if (_listener != null) {
			
			if (msg.what == TransactionState.DONE.ordinal()) {
				if(_lastResponse.getStatus() != StatusList.Success) {
					_listener.onOperationFail(this, _op, new Exception(_lastResponse.getResponseText()));
				} else  {
					_gate.sendRequest(_gate.getPreparedRequest(OperationsList.FinalizeTransaction));
					_listener.onOperationSuccess(this,_op, _rrn, _sum);
				}
			} else
				_listener.onOperationFail(this, _op, _lastException);
		}
		return true;
	}

	private IResponse processRequest(IRequest rq) {
		if (_gate == null)
			return null;
		_lastResponse = null;
		try {
			_rState = TransactionState.PROCESSING;
			rq.setEcrMerchantNumber(1);
			long start = System.currentTimeMillis();
			_gate.sendRequest(rq);
			while (_rState == TransactionState.PROCESSING && System.currentTimeMillis() - start < 20000L)
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
					return null;
				}
			if (_rState == TransactionState.DONE)
				return _lastResponse;
			else if (_rState == TransactionState.PROCESSING) {
				_rState = TransactionState.FAIL;
				_lastException = new Exception("Истекло время ожидания");
				_gate.sendRequest(_gate.getPreparedRequest(OperationsList.Interrupt));
			}
			return null;
		} finally {
		}
	}

	@Override
	public void requestSettlement(Context ctx, EPaymentListener listener) {
		if (_cState != ConnectionState.CONNECTED) {
			listener.onOperationFail(this, OperationType.SETTLEMENT, new Exception("Сервис не доступен"));
			return;
		}
		Core.getInstance().sendBroadcast(new Intent("org.lanter.START_SERVICE"));
		_op = OperationType.SETTLEMENT;
		_sum = BigDecimal.ZERO;
		_rrn = Const.EMPTY_STRING;
		_listener = listener;
		startOperation(ctx, "Cверка итогов", new Runnable() {
			public void run() {
				IRequest rq = _gate.getPreparedRequest(OperationsList.Settlement);
				processRequest(rq);
				_h.sendEmptyMessage(_rState.ordinal());
			}
		});
	}
	@Override
	public int getIconId() {
		return R.drawable.ic_lanter;
	}
	@Override
	public String toString() {
		return ENGINE_NAME;
	}

}
