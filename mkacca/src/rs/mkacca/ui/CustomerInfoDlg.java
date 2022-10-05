package rs.mkacca.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import cs.ui.widgets.DialogSpinner;
import rs.fncore.FZ54Tag;
import rs.fncore.data.IdentifyDocumentCodeE;
import rs.fncore.data.Tag;
import rs.mkacca.R;

public class CustomerInfoDlg implements View.OnClickListener, DialogInterface.OnShowListener {

	private static final SimpleDateFormat DF = new SimpleDateFormat("dd.MM.yyyy");
	private AlertDialog _dialog;
	private Tag _tag;
	private EditText _name, _inn,_email,_phone, _birthday,_citisen,_passData,_address;
	private DialogSpinner _passCode;
	public CustomerInfoDlg(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.customer_data, new LinearLayout(ctx),false);
		_name = v.findViewById(R.id.ed_customer_name);
		_inn = v.findViewById(R.id.ed_customer_inn);
		_phone = v.findViewById(R.id.ed_customer_phone);
		_email = v.findViewById(R.id.ed_customer_email);
		_birthday = v.findViewById(R.id.ed_birthdate);
		_citisen = v.findViewById(R.id.ed_citisen);
		_passCode = v.findViewById(R.id.sp_pass_code);
		_passCode.setAdapter(new ArrayAdapter<IdentifyDocumentCodeE>(ctx, android.R.layout.simple_list_item_1,IdentifyDocumentCodeE.values()));
		_passData = v.findViewById(R.id.ed_pass_data);
		_address = v.findViewById(R.id.ed_address);
		b.setView(v);
		b.setNegativeButton(android.R.string.cancel, null);
		b.setPositiveButton(android.R.string.ok, null);
		_dialog = b.create();
		_dialog.setOnShowListener(this);
	}
	
	public void show(Tag tag) {
		_tag = tag;
		String s = _tag.getTagString(FZ54Tag.T1008_BUYER_PHONE_EMAIL);
		_name.setText(null);
		_inn.setText(null);
		_email.setText(null);
		_phone.setText(null);
		if(!s.isEmpty()) {
			if(s.contains("@"))
				_email.setText(s);
			else 
				_phone.setText(s);
		}
		_name.setText(_tag.getTagString(FZ54Tag.T1227_CLIENT_NAME));
		_inn.setText(_tag.getTagString(FZ54Tag.T1228_CLIENT_INN));
		_birthday.setText(_tag.getTagString(FZ54Tag.T1243_CLIENT_BIRTHDAY));
		_citisen.setText(_tag.getTagString(FZ54Tag.T1244_CLIENT_CITIZENSHIP));
		_passCode.setSelectedItem(IdentifyDocumentCodeE.fromCode(_tag.getTagString(FZ54Tag.T1245_CLIENT_IDENTITY_DOC_CODE)));
		_passData.setText(_tag.getTagString(FZ54Tag.T1246_CLIENT_IDENTITY_DOC_DATA));
		_address.setText(_tag.getTagString(FZ54Tag.T1254_CLIENT_ADDRESS));
		_dialog.show();
	}

	@Override
	public void onClick(View v) {
		_tag.remove(FZ54Tag.T1008_BUYER_PHONE_EMAIL);
		_tag.remove(FZ54Tag.T1227_CLIENT_NAME);
		_tag.remove(FZ54Tag.T1228_CLIENT_INN);
		_tag.remove(FZ54Tag.T1243_CLIENT_BIRTHDAY);
		_tag.remove(FZ54Tag.T1244_CLIENT_CITIZENSHIP);
		
		_tag.remove(FZ54Tag.T1245_CLIENT_IDENTITY_DOC_CODE);
		_tag.remove(FZ54Tag.T1246_CLIENT_IDENTITY_DOC_DATA);
		_tag.remove(FZ54Tag.T1254_CLIENT_ADDRESS);
		
		String s = _name.getText().toString().trim();
		if(!s.isEmpty()) _tag.add(FZ54Tag.T1227_CLIENT_NAME,s);
		s = _inn.getText().toString().trim();
		if(!s.isEmpty()) _tag.add(FZ54Tag.T1228_CLIENT_INN,s);
		s = _phone.getText().toString().trim();
		if(!s.isEmpty()) _tag.add(FZ54Tag.T1008_BUYER_PHONE_EMAIL,s);
		s = _email.getText().toString().trim(); 
		if(!s.isEmpty()) {
			_tag.remove(FZ54Tag.T1008_BUYER_PHONE_EMAIL);
			_tag.add(FZ54Tag.T1008_BUYER_PHONE_EMAIL,s);
		}
		s = _birthday.getText().toString().trim();
		if(!s.isEmpty()) try {
			if(DF.parse(s).getTime() >= System.currentTimeMillis()) 
				throw new ParseException(s, 0);
			_tag.add(FZ54Tag.T1243_CLIENT_BIRTHDAY,s);
		} catch(ParseException pe) {
			Toast.makeText(v.getContext(), "Неверно указана дата рождения", Toast.LENGTH_LONG).show();
			return;
		}
		s = _citisen.getText().toString().trim();
		if(!s.isEmpty()) _tag.add(FZ54Tag.T1244_CLIENT_CITIZENSHIP,s);
		IdentifyDocumentCodeE dCode = (IdentifyDocumentCodeE)_passCode.getSelectedItem();
		if(dCode != IdentifyDocumentCodeE.NONE) { 
			_tag.add(FZ54Tag.T1245_CLIENT_IDENTITY_DOC_CODE,dCode.code);
			s = _passData.getText().toString().trim();
			if(!s.isEmpty()) 
				_tag.add(FZ54Tag.T1246_CLIENT_IDENTITY_DOC_DATA,s);
			else  {
				Toast.makeText(v.getContext(), "Укажите данные документа удостоверяющего личность", Toast.LENGTH_LONG).show();
			}
				
		}
		s = _address.getText().toString().trim();
		if(!s.isEmpty()) _tag.add(FZ54Tag.T1254_CLIENT_ADDRESS,s);
		_dialog.dismiss();
	}

	@Override
	public void onShow(DialogInterface arg0) {
		_dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);
		
	}


}
