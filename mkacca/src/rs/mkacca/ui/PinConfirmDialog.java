package rs.mkacca.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import cs.ui.widgets.PinPad;
import rs.mkacca.R;

public class PinConfirmDialog implements PinPad.OnPinChangedListener {
	private AlertDialog _dialog;
	private PinPad _pinpad;
	private String _pinOK;
	private Runnable _r;

	public PinConfirmDialog(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		b.setTitle("Введите ПИН Администратора");
		LinearLayout ll = new LinearLayout(ctx);
		ll.setGravity(Gravity.CENTER);
		_pinpad = (PinPad)LayoutInflater.from(ctx).inflate(R.layout.pinpad, ll,false);
		_pinpad.setOnPinChangedListener(this);
		ll.addView(_pinpad);
		b.setView(ll);
		b.setNegativeButton(android.R.string.cancel, null);
		_dialog = b.create();
	}

	@Override
	public void onPinChanged(String pin) {
		if(pin.equals(_pinOK)) {
			_dialog.dismiss();
			_r.run();
		}
	}
	public void show(String pin, Runnable r) {
		_r = r;
		_pinOK = pin;
		_dialog.show();
	}
	
	
}
