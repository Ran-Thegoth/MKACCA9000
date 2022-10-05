package rs.mkacca.ui;

import java.math.BigDecimal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import cs.cashier.ui.widgets.NumberEdit;
import gnu.trove.map.TIntObjectMap;
import rs.fncore.data.Payment.PaymentTypeE;
import rs.mkacca.R;

public class OtherPaymentsDialog implements DialogInterface.OnClickListener {

	private NumberEdit _ahread,_barter,_credit;
	private AlertDialog _dialog;
	private TIntObjectMap<BigDecimal> _payments;
	private Runnable _ok;
	
	public OtherPaymentsDialog(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.other_payments, new LinearLayout(ctx),false);
		_ahread = v.findViewById(R.id.e_ahead);
		_barter = v.findViewById(R.id.e_barter);
		_credit = v.findViewById(R.id.e_credit);
		b.setView(v);
		b.setPositiveButton(android.R.string.ok, this);
		b.setNegativeButton(android.R.string.cancel, null);
		_dialog = b.create();
	}
	
	public void show(TIntObjectMap<BigDecimal> values, Runnable onOk) {
		_ok= onOk;
		_payments = values;
		_ahread.setText(String.format("%.2f", _payments.get(PaymentTypeE.PREPAYMENT.ordinal())));
		_barter.setText(String.format("%.2f", _payments.get(PaymentTypeE.AHEAD.ordinal())));
		_credit.setText(String.format("%.2f", _payments.get(PaymentTypeE.CREDIT.ordinal())));
		_dialog.show();
	}
	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		_payments.put(PaymentTypeE.PREPAYMENT.ordinal(), BigDecimal.valueOf(_ahread.doubleValue()));
		_payments.put(PaymentTypeE.CREDIT.ordinal(), BigDecimal.valueOf(_credit.doubleValue()));
		_payments.put(PaymentTypeE.AHEAD.ordinal(), BigDecimal.valueOf(_barter.doubleValue()));
		_ok.run();
	}


}
