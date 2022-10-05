package rs.mkacca.hw.payment.engines;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import rs.fncore.Const;
import rs.mkacca.hw.payment.EPayment;
import rs.mkacca.hw.payment.EPayment.EPaymentListener;

public class CardEmulator extends EPayment {

	public static final String ENGINE_NAME = "Эмуляция";
	private AtomicInteger _rrn = new AtomicInteger(100);
	public CardEmulator() {
		if(!_settings.has("Enabled")) try {
			_settings.put("Enabled", false);
		} catch(JSONException jse)  { }
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
	@Override
	public void doPayment(Context ctx, final BigDecimal sum, final EPaymentListener listener) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		b.setTitle("К оплате");
		TextView tv = new TextView(ctx);
		tv.setGravity(Gravity.CENTER_HORIZONTAL);
		tv.setTextSize(24);
		tv.setText(String.format("%.2f", sum));
		b.setView(tv);
		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				listener.onOperationSuccess(CardEmulator.this, OperationType.PAYMENT, String.format("%05d", _rrn.getAndIncrement()), sum);
			}
		});
		b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				listener.onOperationFail(CardEmulator.this, OperationType.PAYMENT, new CanceledByUserException());
			}
		});
		AlertDialog dlg = b.create();
		dlg.setCancelable(false);
		dlg.setCanceledOnTouchOutside(false);
		dlg.show();
		
	}

	@Override
	public void doRefund(Context ctx, BigDecimal sum, String rrn, EPaymentListener listener) {
		listener.onOperationSuccess(this, OperationType.REFUND, String.format("%05d", _rrn.getAndIncrement()), sum);

	}

	@Override
	public void doCancel(Context ctx, String rrn, EPaymentListener listener) {
		listener.onOperationSuccess(this, OperationType.CANCEL, String.format("%05d", _rrn.getAndIncrement()), BigDecimal.ZERO);
	}

	@Override
	public void setup(LinearLayout holder) {
		TextView tv = new TextView(holder.getContext());
		tv.setText("Этот модуль не требует настройки");
		holder.addView(tv);
	}

	@Override
	public boolean applySetup(LinearLayout holder) {
		return true;

	}

	@Override
	public void setEnabled(boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRefunded() {
		return true;
	}

	@Override
	public void requestSettlement(Context ctx,EPaymentListener listener) {
		listener.onOperationSuccess(this,OperationType.SETTLEMENT, Const.EMPTY_STRING, BigDecimal.ZERO);
	}
	
	@Override
	public String toString() {
		return ENGINE_NAME;
	}

}
