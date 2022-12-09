package rs.mkacca.hw.payment.engines;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.BarcodeFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import rs.data.PayInfo;
import rs.fncore.Const;
import rs.mkacca.R;
import rs.mkacca.hw.payment.EPayment;
import rs.mkacca.ui.Main;
import rs.utils.Utils;
import rs.log.Logger;

public class SPBPayment extends EPayment {

	public static final String ENGINE_NAME = "Система быстрых платежей";
	private String _key = Const.EMPTY_STRING, _url = "https://api-stage.mapcard.pro/";
	private String _pass = "123";
	private boolean _test;

	private static final int MSG_SUCCESS = 1;
	private static final int MSG_CANCELED = 2;
	private static final int MSG_ERROR = 3;
	private static final String SUCCESS_TAG = "Success";
	private OperationType _opType;
	private EPaymentListener _listener;
	private BigDecimal _sum;

	private class NoValidReplyException extends Exception {
		private static final long serialVersionUID = 1L;

		public NoValidReplyException() {
			super("Нет ответа от сервера");
		}
	}

	private abstract class SPBTask extends Thread implements Handler.Callback {
		private Context _ctx;
		private Handler _handler;
		private JSONObject _request;
		private String _ep;
		private boolean _singleRequest;

		public SPBTask(Context ctx, JSONObject request, String ep, boolean repetable) {
			_ctx = ctx;
			_handler = new Handler(ctx.getMainLooper(), this);
			_request = request;
			_ep = ep;
			_singleRequest = !repetable;
		}

		protected Context getContext() {
			return _ctx;
		}

		protected void sendMessage(int what, Object payload) {
			Message msg = _handler.obtainMessage(what);
			msg.obj = payload;
			_handler.sendMessage(msg);
		}

		public boolean handleMessage(Message msg) {
			Main.unlock();
			switch (msg.what) {
			case MSG_CANCELED:
				_listener.onOperationFail(SPBPayment.this, _opType, new CanceledByUserException());
				break;
			case MSG_ERROR:
				_listener.onOperationFail(SPBPayment.this, _opType, (Exception) msg.obj);
				break;
			}
			return true;
		}

		@Override
		public void run() {
			try {
				if (!_url.endsWith("/"))
					_url += "/";
				URL url = new URL(_url + _ep);
				_request.put("key", _key);
				while (!isInterrupted()) {
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					try {
						con.setConnectTimeout(5000);
						con.setRequestMethod("POST");
						con.addRequestProperty("Content-type", "application/json");
						con.setDoInput(true);
						con.setDoOutput(true);
						OutputStream os = con.getOutputStream();
						os.write(_request.toString().getBytes());
						os.flush();
						int r = con.getResponseCode();
						if (r == 200) {
							byte[] b = new byte[con.getContentLength()];
							InputStream is = con.getInputStream();
							int read = is.read(b);
							JSONObject rep = new JSONObject(new String(b, 0, read)); 
							if (parseReply(rep))
								return;
						} else {
							onError(new Exception("Ошибка сервера " + r));
							return;
						}
					} finally {
						con.disconnect();
					}
					if (_singleRequest) {
						onError(new NoValidReplyException());
						return;
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ie) {
						break;
					}
				}
				sendMessage(MSG_CANCELED, null);
			} catch (Exception e) {
				Logger.e(e,"СБП: Ошибка "+e.getLocalizedMessage());
				onError(e);
			}
		}

		public abstract boolean parseReply(JSONObject reply) throws JSONException;

		public abstract void onError(Exception e);
	}

	private class QRDialog extends SPBTask implements DialogInterface.OnClickListener, View.OnClickListener {
		private AlertDialog _dialog;
		private String _id;

		public QRDialog(Context ctx, JSONObject request, String qr) {
			super(ctx, request, "getState", true);
			try {
				_id = request.getString("map_order_id");
				AlertDialog.Builder b = new AlertDialog.Builder(ctx);
				View v = LayoutInflater.from(ctx).inflate(R.layout.spb_payment, new LinearLayout(ctx), false);
				b.setView(v);
				b.setNegativeButton(android.R.string.cancel, this);

				if (_test) {
					View btn = v.findViewById(R.id.v_test_payment);
					btn.setVisibility(View.VISIBLE);
					btn.setOnClickListener(this);
				}
				int w = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.6);
				Bitmap bQr = Utils.encodeAsBitmap(qr, w, w, BarcodeFormat.QR_CODE, 0);
				((ImageView) v.findViewById(R.id.iv_qr)).setImageBitmap(bQr);
				_dialog = b.create();
				_dialog.setCancelable(false);
				_dialog.setCanceledOnTouchOutside(false);
				_dialog.show();
			} catch (JSONException jse) {
			}
		}

		@Override
		public boolean parseReply(JSONObject reply) throws JSONException {
			if (reply.getBoolean(SUCCESS_TAG)) {
				if ("paid".equalsIgnoreCase(reply.getString("State"))) {
					sendMessage(MSG_SUCCESS, null);
					return true;
				}
			}
			return false;
		}

		@Override
		public void onError(Exception e) {
			sendMessage(MSG_ERROR, e);

		}

		@Override
		public boolean handleMessage(Message msg) {
			_dialog.dismiss();
			if (msg.what == MSG_SUCCESS) 
				_listener.onOperationSuccess(SPBPayment.this, _opType, new PayInfo(String.valueOf(_id), String.valueOf(_id)), _sum);
			return super.handleMessage(msg);
		}

		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			interrupt();
		}

		@Override
		public void onClick(View v) {
			v.setEnabled(false);
			try {
				JSONObject rq = new JSONObject();
				rq.put("map_order_id", _id);
				rq.put("testpay_fail",false);
				rq.put("credential",new JSONObject("{\"terminal_password\":\""+_pass+"\"}"));
				new SPBTask(getContext(), rq, "testQRPayment", false) {
					@Override
					public boolean parseReply(JSONObject reply) throws JSONException {
						return true;
					}

					@Override
					public void onError(Exception e) {
					}
				}.start();
			} catch (JSONException jse) {
			}

		}

	}

	private class RequestQR extends SPBTask {
		public RequestQR(Context ctx, JSONObject request) {
			super(ctx, request, "generateQR", false);
			Main.lock();
		}

		@Override
		public boolean handleMessage(Message msg) {

			if (msg.what == MSG_SUCCESS) {
				JSONObject reply = (JSONObject) msg.obj;

				JSONObject request = new JSONObject();
				try {
					request.put("map_order_id", reply.get("MapOrderID"));
					new QRDialog(getContext(), request, reply.getString("QRData")).start();
					return true;
				} catch (JSONException jse) {
					onError(jse);
				}
			}
			Main.unlock();
			return super.handleMessage(msg);
		}

		@Override
		public boolean parseReply(JSONObject reply) throws JSONException {
			if (reply.getBoolean(SUCCESS_TAG)) {
				sendMessage(MSG_SUCCESS, reply);
			} else {
				String err = "Неизвестная ошибка";
				if (reply.has("ErrMessage"))
					err = reply.getString("ErrMessage");
				if (reply.has("ErrCode"))
					err += " код " + reply.getString("ErrCode");
				onError(new Exception(err));
			}
			return true;
		}

		@Override
		public void onError(Exception e) {
			sendMessage(MSG_ERROR, e);

		}

	}

	public SPBPayment() {
		super();
		try {
			if (_settings.has("mId"))
				_key = _settings.getString("mId");
			if (_settings.has("url"))
				_url = _settings.getString("url");
			if (_settings.has("isTest"))
				_test = _settings.getBoolean("isTest");
			if(_settings.has("psk"))
				_pass = _settings.getString("psk");
		} catch (JSONException jse) {
		}
	}

	@Override
	public void doPayment(Context ctx, BigDecimal sum, EPaymentListener listener) {
		JSONObject request = new JSONObject();
		try {
			request.put("key", _key);
			request.put("qr_type", "dynamic");
			request.put("amount", sum.multiply(BigDecimal.valueOf(100.0)).longValue());
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			_opType = OperationType.PAYMENT;
			_sum = sum;
			_listener = listener;
			request.put("merchant_order_id", String.valueOf(System.currentTimeMillis() - cal.getTimeInMillis()));
			new RequestQR(ctx, request).start();
		} catch (JSONException jse) {
			listener.onOperationFail(this, OperationType.PAYMENT, jse);
		}
	}

	@Override
	public void doRefund(Context ctx, BigDecimal sum, PayInfo payment, EPaymentListener listener) {
	}

	@Override
	public void doCancel(Context ctx, PayInfo payment, EPaymentListener listener) {
	}

	@Override
	public void setup(LinearLayout holder) {
		View v = LayoutInflater.from(holder.getContext()).inflate(R.layout.spb_setup, holder, false);
		((TextView) v.findViewById(R.id.ed_mid)).setText(_key);
		((TextView) v.findViewById(R.id.ed_spb_url)).setText(_url);
		((TextView) v.findViewById(R.id.ed_spb_psk)).setText(_pass);
		((CheckBox) v.findViewById(R.id.cb_test)).setChecked(_test);
		holder.addView(v);
	}

	@Override
	public boolean applySetup(LinearLayout holder) {
		TextView id = holder.findViewById(R.id.ed_mid);
		if (id.getText().toString().isEmpty()) {
			Toast.makeText(holder.getContext(), "Идентификатор продавца должен быть указан", Toast.LENGTH_SHORT).show();
			id.requestFocus();
			return false;
		}
		TextView url = holder.findViewById(R.id.ed_spb_url);
		if (url.getText().toString().isEmpty()) {
			Toast.makeText(holder.getContext(), "Адрес для отправки запроса должен быть указан", Toast.LENGTH_SHORT)
					.show();
			url.requestFocus();
			return false;
		}
		_key = id.getText().toString();
		_url = url.getText().toString();
		_test = ((CheckBox) holder.findViewById(R.id.cb_test)).isChecked();
		_pass = ((TextView)holder.findViewById(R.id.ed_spb_psk)).getText().toString();
		try {
			_settings.put("mId", _key);
			_settings.put("url", _url);
			_settings.put("isTest", _test);
			_settings.put("psk",_pass);
			store();
			return true;
		} catch (JSONException jse) {
			Toast.makeText(holder.getContext(), "Ошибка при сохранении параметров", Toast.LENGTH_SHORT).show();
			return false;
		}
	}

	@Override
	public boolean isRefunded() {
		return false;
	}

	@Override
	public void requestSettlement(Context ctx, EPaymentListener listener) {
		listener.onOperationSuccess(this,OperationType.SETTLEMENT, null, BigDecimal.ZERO);
		
	}
	@Override
	public int getIconId() {
		return R.drawable.ic_sbp;
	}
	@Override
	public String toString() {
		return ENGINE_NAME;
	}

}
