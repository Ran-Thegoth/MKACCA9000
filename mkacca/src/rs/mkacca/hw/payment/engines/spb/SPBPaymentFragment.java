package rs.mkacca.hw.payment.engines.spb;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import com.google.zxing.BarcodeFormat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import cs.U;
import cs.ui.BackHandler;
import cs.ui.fragments.BaseFragment;
import rs.mkacca.R;
import rs.mkacca.hw.payment.EPayment;
import rs.mkacca.hw.payment.EPayment.CanceledByUserException;
import rs.mkacca.hw.payment.EPayment.EPaymentListener;
import rs.mkacca.hw.payment.EPayment.OperationType;
import rs.mkacca.hw.payment.engines.SPBPayment;
import rs.mkacca.ui.Main;
import rs.utils.Utils;

public class SPBPaymentFragment extends BaseFragment implements BackHandler, Handler.Callback {

	private static final int MSG_OK = 0;
	private static final int MSG_ERROR = 1;
	private static final int MSG_PAYED = 2;
	private EPaymentListener _listener;
	private EPayment _engine;
	private String _url;
	private JSONObject _request;
	private View _content; 
	private TextView _status;
	private ImageView _qr;
	private Handler _h;

	private class QRRequester extends Thread {
		public QRRequester() {
			Main.lock();
		}
		@Override
		public void run() {
			if (!_url.endsWith("/"))
				_url += "/";
			try {
				URL url = new URL(_url + "generateQR");
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				con.setRequestMethod("POST");
				con.setConnectTimeout(5000);
				con.setDoInput(true);
				con.setDoOutput(true);
				con.addRequestProperty("Content-Type", "application/json");
				OutputStream os = con.getOutputStream();
				Log.d("SPB", ">> "+_request.toString(1));
				os.write(_request.toString().getBytes());
				os.flush();
				int r = con.getResponseCode(); 
				
				if( r == 200) {
					int sz  = con.getContentLength();
					byte [] b = new byte[sz];
					InputStream is = con.getInputStream();
					int read = is.read(b); 
					String sReply = new String(b,0,read);
					JSONObject reply = new JSONObject(sReply);
					Log.d("SPB", "<< "+reply.toString(1));
					if(reply.has("Success")) {
						if(reply.getBoolean("Success")) {
							if(reply.has("QRData")) {
								Message msg = _h.obtainMessage(MSG_OK);
								msg.obj = reply.getString("QRData");
								_h.sendMessage(msg);
								return;
							}
						} else {
							String s = "Неизвестно";
							if(reply.has("ErrCode"))
								s = reply.getString("ErrCode");
							throw new Exception("Ошибка платежа: "+s);
						}
					} else 
						throw new Exception("Неожиданный ответ от сервера");
					
				} else
					throw new Exception("Ошибка сервера "+r);
			} catch (Exception e) {
				Message msg = _h.obtainMessage(MSG_ERROR);
				msg.obj = e;
				_h.sendMessage(msg);
			}
		}
	}

	public static SPBPaymentFragment newInstance(JSONObject request, String url, EPaymentListener l) {
		SPBPaymentFragment result = new SPBPaymentFragment();
		result._listener = l;
		result._request = request;
		result._url = url;
		return result;
	}

	public SPBPaymentFragment() {
	}

	@Override
	public boolean onBackPressed() {
		U.confirm(getContext(), "Отменить оплату?", new Runnable() {
			@Override
			public void run() {
				getFragmentManager().popBackStack();
				_listener.onOperationFail(_engine, OperationType.PAYMENT, new CanceledByUserException());
			}
		});
		return false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_content == null) {
			_engine = EPayment.knownEngines().get(SPBPayment.ENGINE_NAME);
			_content = inflater.inflate(R.layout.spb_payment, container,false);
			_h = new Handler(getContext().getMainLooper(), this);
			_qr = _content.findViewById(R.id.iv_qr);
			_status = _content.findViewById(R.id.lb_status);
			new QRRequester().start();
		}
		return _content;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Оплата по QR-коду");
	}

	@Override
	public boolean handleMessage(Message msg) {
		Main.unlock();
		switch(msg.what) {
		case MSG_OK:  {
			int w = (int)(getResources().getDisplayMetrics().widthPixels *0.7);
			_qr.setImageBitmap(Utils.encodeAsBitmap(msg.obj.toString(), w, w, BarcodeFormat.QR_CODE,0));
			_status.setText("Ожидание оплаты");
		}
		break;
		case MSG_ERROR: {
			final Exception e=  (Exception)msg.obj;
			_listener.onOperationFail(_engine, OperationType.PAYMENT, e);
			getFragmentManager().popBackStack();
			}
			break;
		}
		return true;
	}

}
