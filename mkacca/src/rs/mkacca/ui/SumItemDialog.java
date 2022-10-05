package rs.mkacca.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import rs.data.goods.Good;
import rs.fncore.data.SellItem;
import rs.fncore.data.VatE;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;

public class SumItemDialog implements DialogInterface.OnShowListener, View.OnClickListener {

	
	private AlertDialog _dlg;
	private Spinner _sp;
	private EditText _sum;
	private OnValueChangedListener<SellItem> _l;
	public SumItemDialog(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.sum_item, new LinearLayout(ctx),false);
		_sp = v.findViewById(R.id.sp_tax);
		_sp.setAdapter(new ArrayAdapter<VatE>(ctx, android.R.layout.simple_list_item_1,VatE.values()));
		_sum = v.findViewById(R.id.ed_sum);
		_sum.setText(String.format("%.2f", 0.0));
		b.setView(v);
		b.setPositiveButton(android.R.string.ok, null);
		b.setNegativeButton(android.R.string.cancel, null);
		_dlg = b.create();
		_dlg.setOnShowListener(this);
	}
	public void show(OnValueChangedListener<SellItem> l) {
		_l = l;
		_dlg.show();
	}
	@Override
	public void onClick(View v) {
		double sum = 0;
		try {
			sum = Double.parseDouble(_sum.getText().toString().replace(",", "."));
			if(sum <= 0.01) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
			Toast.makeText(v.getContext(), "Неверно указана сумма", Toast.LENGTH_SHORT).show();
			return;
		}
		Good g = new Good("Сумма",sum,(VatE)_sp.getSelectedItem());
		_dlg.dismiss();
		if(_l != null)
			_l.onChanged(g.createSellItem());
		
	}
	@Override
	public void onShow(DialogInterface arg0) {
		_dlg.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
		
	}

}
