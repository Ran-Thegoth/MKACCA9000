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
import rs.data.PayInfo;
import rs.fncore.Const;
import rs.mkacca.hw.payment.EPayment;
import rs.mkacca.hw.payment.EPayment.EPaymentListener;

public class CardEmulator extends EPayment {

	public static final String ENGINE_NAME = "Эмуляция";
	private AtomicInteger _rrn = new AtomicInteger(100);
	private AtomicInteger _number = new AtomicInteger(1);
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
	public void doPayment(final Context ctx, final BigDecimal sum, final EPaymentListener listener) {
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
				doPayment(ctx, sum, listener);
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
	public void doRefund(Context ctx, BigDecimal sum, PayInfo info, EPaymentListener listener) {
		PayInfo i = new PayInfo(String.valueOf(_number.getAndIncrement()), String.format("%05d", _rrn.getAndIncrement()));
		listener.onOperationSuccess(this, OperationType.REFUND, i, sum);

	}

	@Override
	public void doCancel(Context ctx, PayInfo info, EPaymentListener listener) {
		PayInfo i = new PayInfo(String.valueOf(_number.getAndIncrement()), String.format("%05d", _rrn.getAndIncrement()));
		listener.onOperationSuccess(this, OperationType.REFUND, i, BigDecimal.ZERO);
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
		listener.onOperationSuccess(this,OperationType.SETTLEMENT, null, BigDecimal.ZERO);
	}
	
	@Override
	public String toString() {
		return ENGINE_NAME;
	}

}
