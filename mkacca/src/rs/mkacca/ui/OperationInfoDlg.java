package rs.mkacca.ui;

import java.text.ParseException;

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
import rs.utils.Utils;

public class OperationInfoDlg  implements View.OnClickListener, DialogInterface.OnShowListener {

	private Tag _info, _order;
	private AlertDialog _dialog;
	private EditText _opDate, _opCode,_opData;
	public OperationInfoDlg(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.operation_info, new LinearLayout(ctx),false);
		b.setView(v);
		_opCode = v.findViewById(R.id.ed_op_id);
		_opDate = v.findViewById(R.id.ed_op_date);
		_opDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(_opDate.getText().toString().isEmpty())
					_opDate.setText(Utils.formatDate(System.currentTimeMillis()));
				
			}
		}); 
		_opData = v.findViewById(R.id.ed_op_data);
		b.setPositiveButton(android.R.string.ok, null);
		b.setNegativeButton(android.R.string.cancel, null);
		_dialog = b.create();
		_dialog.setOnShowListener(this);
	}
	
	public void show(Tag tag) {
		_order = tag;
		if(tag.hasTag(FZ54Tag.T1270_OPERATION_INFO)) 
			_info = tag.getTag(FZ54Tag.T1270_OPERATION_INFO);
		else 
			_info = new Tag();
		if(_info.hasTag(FZ54Tag.T1271_OPERATION_ID)) 
			_opCode.setText(String.valueOf(_info.getTag(FZ54Tag.T1271_OPERATION_ID).asByte()));
		if(_info.hasTag(FZ54Tag.T1273_OPERATION_DATE)) 
			_opDate.setText(Utils.formatDate(_info.getTag(FZ54Tag.T1273_OPERATION_DATE).asTimeStamp()));
		_opData.setText(_info.getTagString(FZ54Tag.T1272_OPERATION_DATA));
		_dialog.show();
	}


	@Override
	public void onClick(View v) {
		_info = new Tag(FZ54Tag.T1270_OPERATION_INFO);
		String s = _opCode.getText().toString();
		try {
			byte b = Byte.parseByte(s);
			_info.add(FZ54Tag.T1271_OPERATION_ID,b);
		} catch(NumberFormatException nfe) {
			Toast.makeText(v.getContext(), "Неверный идентификатор операции", Toast.LENGTH_LONG).show();
			return;
		}
		s = _opDate.getText().toString();
		
		try {
			_info.add(FZ54Tag.T1273_OPERATION_DATE,Utils.parseDate(s)/1000L);
		} catch(ParseException e) {
			Toast.makeText(v.getContext(), "Неверная дата операции", Toast.LENGTH_LONG).show();
			return;
		}
		s = _opData.getText().toString();
		if(s.isEmpty()) {
			Toast.makeText(v.getContext(), "Не указаны данные операции", Toast.LENGTH_LONG).show();
			return;
		}
		_info.add(FZ54Tag.T1272_OPERATION_DATA,s);
		
		if(!_info.getChilds().isEmpty())
			_order.getChilds().add(_info);
		else
			_order.remove(FZ54Tag.T1270_OPERATION_INFO);
		_dialog.dismiss();
	}

	@Override
	public void onShow(DialogInterface arg0) {
		_dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
		
	}

}
