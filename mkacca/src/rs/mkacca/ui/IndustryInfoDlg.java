package rs.mkacca.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import rs.fncore.FZ54Tag;
import rs.fncore.data.Tag;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.IndustryValuesList;

public class IndustryInfoDlg implements DialogInterface.OnShowListener, View.OnClickListener {

	private EditText _code, _date,_number;
	private  int _infoId;
	private  Tag _tag, _info;
	private IndustryValuesList _values;
	
	private AlertDialog _dialog;
	public IndustryInfoDlg(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.industry_info, new LinearLayout(ctx),false);
		_code = v.findViewById(R.id.ed_foiv);
		_date = v.findViewById(R.id.ed_doc_date);
		_number = v.findViewById(R.id.ed_doc_number);
		_values = v.findViewById(R.id.v_rq_values);
		b.setView(v);
		b.setPositiveButton(android.R.string.ok, null);
		b.setNegativeButton(android.R.string.cancel, null);
		_dialog = b.create();
		_dialog.setOnShowListener(this);
	}
	
	
	public void show(Tag tag, int infoId) {
		_infoId = infoId;
		_tag = tag;
		if(_tag.hasTag(infoId)) 
			_info = _tag.getTag(infoId);
		else 
			_info = new Tag();
		_code.setText(_info.getTagString(FZ54Tag.T1262_FOIV_ID));
		_date.setText(_info.getTagString(FZ54Tag.T1263_DOC_BASE_DATE));
		_number.setText(_info.getTagString(FZ54Tag.T1264_DOC_BASE_NO));
		_values.setValue(_info.getTagString(FZ54Tag.T1265_INDUSTRY_REQUISIT_VALUE));
		_dialog.show();
	}

	@Override
	public void onClick(View v) {
		_info = new Tag(_infoId);
		_tag.remove(_infoId);
		String sCode = _code.getText().toString().trim();
		String sDate = _date.getText().toString().trim();
		String sNo = _number.getText().toString().trim();
		if(sCode.isEmpty()) {
			_dialog.dismiss();
			return;
		}
		String val = _values.getValue();
		if(sDate.isEmpty() ||  sNo.isEmpty() || val.isEmpty()) {
			Toast.makeText(v.getContext(), "Не указано значение реквизита", Toast.LENGTH_LONG).show();
			return;
		}
		_info.add(FZ54Tag.T1262_FOIV_ID,sCode);
		_info.add(FZ54Tag.T1263_DOC_BASE_DATE,sDate);
		_info.add(FZ54Tag.T1264_DOC_BASE_NO,sNo);
		_info.add(FZ54Tag.T1265_INDUSTRY_REQUISIT_VALUE,val);
		_tag.getChilds().add(_info);
		_dialog.dismiss();
	}

	@Override
	public void onShow(DialogInterface arg0) {
		_dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
		
	}

}
