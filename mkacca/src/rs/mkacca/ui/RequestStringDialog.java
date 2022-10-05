package rs.mkacca.ui;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;

public class RequestStringDialog implements DialogInterface.OnShowListener, TextWatcher, View.OnClickListener {

	private View _ok;
	private AlertDialog _dialog;
	private EditText _sum;
	private OnValueConfirmListener _l;
	public static interface OnValueConfirmListener {
		public void onConfirm(String  r);
	}
	public RequestStringDialog(int title, Context ctx, String s, OnValueConfirmListener l) {
		this(ctx.getString(title),ctx,s,l);
	}
	public RequestStringDialog(String title, Context ctx, String s, OnValueConfirmListener l) {
		_l = l;
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		b.setPositiveButton(android.R.string.ok,null);
		b.setNegativeButton(android.R.string.cancel,null);
		_sum = new EditText(ctx);
		_sum.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		_sum.setLines(2);
		_sum.setSelectAllOnFocus(true);
		_sum.setGravity(Gravity.TOP);
		_sum.setText(s);
		_sum.addTextChangedListener(this);
		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(6, 6, 6, 6);
		ll.addView(_sum,new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		b.setView(ll);
		b.setTitle(title);
		_dialog = b.create();
		_dialog.setOnShowListener(this);
		_dialog.show();
	}
	@Override
	public void onShow(DialogInterface arg0) {
		_ok = _dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		_ok.setOnClickListener(this);
		afterTextChanged(null);
	}
	@Override
	public void afterTextChanged(Editable arg0) {
		_ok.setEnabled(_sum.getText().length() > 0);
	}
	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
	}
	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onClick(View arg0) {
		_dialog.dismiss();
		_l.onConfirm(_sum.getText().toString());
	}

}
