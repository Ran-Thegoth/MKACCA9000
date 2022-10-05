package rs.mkacca.ui.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import cs.ui.widgets.PinPad;
import rs.mkacca.R;

public class PasswordSetDialog implements OnShowListener, OnClickListener, PinPad.OnPinChangedListener {
	private AlertDialog _dialog;
	private PinPad _pinpad;
	private View _ok;
	private String _pin,_newPin;
	private int _mode;
	public static interface PasswordSetListener {
		public void onPasswordSet(String pin);
	}
	private PasswordSetListener _l;
	public PasswordSetDialog(Context ctx, PasswordSetListener l) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		_l = l;
		b.setTitle(R.string.enter_cur_pin);
		LinearLayout ll = new LinearLayout(ctx);
		ll.setGravity(Gravity.CENTER);
		_pinpad = (PinPad)LayoutInflater.from(ctx).inflate(R.layout.pinpad, ll,false);
		_pinpad.setOnPinChangedListener(this);
		ll.addView(_pinpad);
		b.setView(ll);
		b.setNegativeButton(android.R.string.cancel, null);
		b.setPositiveButton(android.R.string.ok, null);
		_dialog = b.create();
		_dialog.setOnShowListener(this);
	}

	@Override
	public void onPinChanged(String pin) {
		switch(_mode) {
		case 0:
			if(_pin.equals(pin)) {
				_dialog.setTitle(R.string.enter_new_pin);
				_pinpad.clear();
				_mode = 1;
			}
			break;
		case 1:
			_ok.setEnabled( pin.length() > 3);
			_newPin = pin;
			break;
		case 2:
			_ok.setEnabled(_newPin.equals(pin));
			break;
		}
		
	}
	@Override
	public void onClick(View arg0) {
		switch(_mode) {
		case 1: 
			_dialog.setTitle(R.string.confirm_new_pin);
			_pinpad.clear();
			_mode = 2;
			break;
		case 2:
			_l.onPasswordSet(_newPin);
			_dialog.dismiss();
			break;
		}
		
	}
	@Override
	public void onShow(DialogInterface arg0) {
		_ok = _dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		_ok.setOnClickListener(this);
		_ok.setEnabled(false);
	}
	public void show(String pin) {
		
		_pin = pin;
		_newPin = "";		
		if(_pin != null && !_pin.isEmpty()) {
			_dialog.setTitle(R.string.enter_cur_pin);
			_mode = 0;
		} else {
			_dialog.setTitle(R.string.enter_new_pin);
			_mode = 1;
		}
		
		_pinpad.clear();
		_dialog.show();
	}
}
