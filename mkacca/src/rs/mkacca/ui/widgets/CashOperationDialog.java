package rs.mkacca.ui.widgets;

import java.math.BigDecimal;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.utils.ShiftUtils;

public class CashOperationDialog implements OnShowListener, View.OnClickListener, TextWatcher {
	private RadioGroup _opType;
	private TextView _available,_sum;
	private View _ok;
	private AlertDialog _dialog;
	private double _cash, _s;
	private CashOperationListener _l;

	public static interface CashOperationListener {
		public void onOperation(BigDecimal sum, int opType);
	}
	
	public CashOperationDialog(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		b.setTitle(R.string.dep_with);
		View v = LayoutInflater.from(ctx).inflate(R.layout.cash_operation, new LinearLayout(ctx),false);
		_opType = v.findViewById(R.id.op_type);
		_sum = v.findViewById(R.id.op_sum);
		_sum.addTextChangedListener(this);
		_available = v.findViewById(R.id.available_cash);
		b.setView(v);
		b.setPositiveButton(android.R.string.ok, null);
		b.setNegativeButton(android.R.string.cancel, null);
		_dialog = b.create();
		_dialog.setOnShowListener(this);
		
	}
	@Override
	public void afterTextChanged(Editable arg0) {
		if(_ok != null) try {
			_s = Double.parseDouble(_sum.getText().toString());
			if(_opType.getCheckedRadioButtonId() == R.id.do_withdraw)
				_ok.setEnabled(_s <= _cash);
			else 
				_ok.setEnabled(_s > 0);
		} catch(NumberFormatException nfe) {
			_ok.setEnabled(false);
		}
			
		
	}
	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}
	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}
	@Override
	public void onClick(View v) {
		_dialog.dismiss();
		_l.onOperation(BigDecimal.valueOf(_s), _opType.getCheckedRadioButtonId() == R.id.do_deposit ? ShiftUtils.ACTION_DEPOSIT :
			ShiftUtils.ACTION_WITHDRAW);
	}
	@Override
	public void onShow(DialogInterface arg0) {
		_ok = _dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		_ok.setEnabled(_s > 0);
		_ok.setOnClickListener(this);
	}
	public void show(BigDecimal sum, boolean awailableDeposit, CashOperationListener l) {
		_l = l;
		try {
			_cash = Core.getInstance().getStorage().getCashRest();
			_s = sum.doubleValue();
			_sum.setText(String.format(Locale.ROOT, "%.2f",_s));
			_available.setText(String.format(Locale.ROOT, "%.2f",_cash));
			if(awailableDeposit)
				_opType.check(R.id.do_deposit);
			else 
				_opType.check(R.id.do_withdraw);
			for(int i=0;i<_opType.getChildCount();i++)
				_opType.getChildAt(i).setEnabled(awailableDeposit);
			_dialog.show();
		} catch(RemoteException re) {
			
		}
	}
}
