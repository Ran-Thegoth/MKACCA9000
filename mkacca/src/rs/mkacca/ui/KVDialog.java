package rs.mkacca.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import rs.data.KV;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;

public class KVDialog implements DialogInterface.OnShowListener, View.OnClickListener{

	private AlertDialog _dialog;
	private TextView _k, _v;
	private KV _kv;
	
	private OnValueChangedListener<KV> _l;
	public KVDialog(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.kv,new LinearLayout(ctx),false);
		_k = v.findViewById(R.id.ed_id);
		_v = v.findViewById(R.id.ed_value);
		b.setView(v);
		b.setNegativeButton(android.R.string.cancel, null);
		b.setPositiveButton(android.R.string.ok, null);
		_dialog = b.create();
		_dialog.setOnShowListener(this);
	}
	
	public void show(KV kv, OnValueChangedListener<KV> l) {
		_kv = kv;
		_k.setText(_kv.k);
		_v.setText(_kv.v);
		_l = l;
		_dialog.show();
	}
	@Override
	public void onClick(View v) {
		if(_k.getText().toString().isEmpty() || _v.getText().toString().isEmpty()) {
			Toast.makeText(v.getContext(), "Не указано значение", Toast.LENGTH_LONG).show();
			return;
		}
		_kv.k = _k.getText().toString();
		_kv.v = _v.getText().toString();
		_l.onChanged(_kv);
		_dialog.dismiss();
		
	}
	@Override
	public void onShow(DialogInterface arg0) {
		_dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
		
	}

}
