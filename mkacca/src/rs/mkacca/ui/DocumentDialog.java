package rs.mkacca.ui;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import rs.fncore.data.Correction;
import rs.mkacca.R;

public class DocumentDialog implements DialogInterface.OnShowListener,  View.OnClickListener  {

	private AlertDialog _dlg;
	private Correction _cor;
	private DatePicker _dp;
	private EditText _num;
	private Runnable _onOk;
	private View _ok;
	private Calendar _cal = Calendar.getInstance();
	public DocumentDialog(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.cor_doc, new LinearLayout(ctx),false);
		_dp = v.findViewById(R.id.cal_doc_date);
		_num = v.findViewById(R.id.ed_doc_no);
		b.setView(v);
		b.setNegativeButton(android.R.string.cancel, null);
		b.setPositiveButton(android.R.string.ok, null);
		_dlg = b.create();
		_dlg.setOnShowListener(this);
	}
	
	public void show(Correction cor,Runnable ok) {
		_cor = cor;
		_onOk  =ok;
		if(_cor.getBaseDocumentDate() > 0)
			_cal.setTimeInMillis(_cor.getBaseDocumentDate());
		_dp.updateDate(_cal.get(Calendar.YEAR), _cal.get(Calendar.MONTH),_cal.get(Calendar.DAY_OF_MONTH));
		_num.setText(_cor.getBaseDocumentNumber());
		_dlg.show();
	}

	@Override
	public void onClick(View v) {
		if(_num.getText().toString().trim().isEmpty()) {
			Toast.makeText(v.getContext(), "Укажите номер документа", Toast.LENGTH_SHORT).show();
			return;
		}
		_cal.set(Calendar.YEAR, _dp.getYear());
		_cal.set(Calendar.MONTH, _dp.getMonth());
		_cal.set(Calendar.DAY_OF_MONTH, _dp.getDayOfMonth());
		_cor.setBaseDocumentDate(_cal.getTimeInMillis());
		_cor.setBaseDocumentNumber(_num.getText().toString());
		_dlg.dismiss();
		if(_onOk != null) _onOk.run();
	}

	@Override
	public void onShow(DialogInterface arg0) {
		_ok = _dlg.getButton(DialogInterface.BUTTON_POSITIVE);
		_ok.setOnClickListener(this);
		
	}


}
