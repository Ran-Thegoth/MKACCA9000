package rs.mkacca.hw.payment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import rs.data.PayInfo;
import rs.mkacca.Core;
import rs.mkacca.hw.payment.engines.CardEmulator;
import rs.mkacca.hw.payment.engines.Lanter;
import rs.mkacca.hw.payment.engines.SPBPayment;

public abstract class EPayment {

	public enum OperationType {
		PAYMENT,
		REFUND,
		CANCEL,
		SETTLEMENT
	}
	public static class CanceledByUserException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public CanceledByUserException() {
			super("Операция отменена");
		}
		
	}
	private static Map<String, EPayment> ENGINES = new HashMap<>();
	public static Map<String, EPayment> knownEngines() { return ENGINES; }
	static {
		ENGINES.put(SPBPayment.ENGINE_NAME, new SPBPayment());
//		ENGINES.put(CardEmulator.ENGINE_NAME,new CardEmulator());
		ENGINES.put(Lanter.ENGINE_NAME,Lanter.SHARER_INSTANCE);
//		ENGINES.put(TTK.ENGINE_NAME,new TTK());
	}
	public static interface EPaymentListener {
		public void onOperationSuccess(EPayment engine, OperationType type, PayInfo info, BigDecimal sum);
		public void onOperationFail(EPayment engine, OperationType type, Exception e);
	}

	protected JSONObject _settings;
	private SharedPreferences _pref;

	public EPayment() {
		_pref = Core.getInstance().getSharedPreferences("engines.pref", Context.MODE_PRIVATE);
		try {
		_settings = new JSONObject(_pref.getString(getClass().getName(), "{}"));
		} catch(JSONException jse) { }
	}
	protected void store() {
		_pref.edit().putString(getClass().getName(), _settings.toString()).commit();
	}
	
	public abstract void doPayment(Context ctx, BigDecimal sum, EPaymentListener listener);
	public abstract void doRefund(Context ctx,BigDecimal sum, PayInfo info, EPaymentListener listener);
	public abstract void doCancel(Context ctx,PayInfo info, EPaymentListener listener);
	public abstract void setup(LinearLayout holder);
	public abstract void requestSettlement(Context ctx,EPaymentListener listener);
	public abstract boolean applySetup(LinearLayout holder);
	public abstract boolean isRefunded();
	
	public int getIconId() { return 0; }
	public boolean isEnabled() {
		try {
			return _settings.getBoolean("Enabled");
		} catch(JSONException jse) {
			return false;
		}
	}

	public void setEnabled(boolean value) {
		try {
			_settings.put("Enabled", value);
			store();
		} catch(JSONException jse) { }
		
	}


}
