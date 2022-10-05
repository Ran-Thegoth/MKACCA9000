package rs.mkacca.ui.fragments;

import java.math.BigDecimal;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import cs.ui.widgets.DialogSpinner;
import rs.fncore.data.Correction;
import rs.fncore.data.Correction.CorrectionTypeE;
import rs.mkacca.R;
import rs.mkacca.ui.DocumentDialog;
import rs.mkacca.ui.SumItemDialog;
import rs.utils.Utils;

public class CorrectionFragment extends SellOrderFragment  {

	private DialogSpinner _corType;
	private AlertDialog _addType;
	private EditText _corDoc;
	public static CorrectionFragment newInstance(Correction c) {
		CorrectionFragment result = new CorrectionFragment();
		result._order = c;
		return result;
	}
	
	
	@Override
	protected int getLayoutId() {
		return R.layout.correction;
	}
	
	private Correction order() { return (Correction)_order; }
	
	@Override
	protected void bind(View v) {
		AlertDialog.Builder b = new AlertDialog.Builder(getContext());
		ArrayAdapter<String> a = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new String [] { "Предмет расчета","Сумма"});
		b.setAdapter(a, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int what) {
				if(what == 0)
					showFragment(GoodSelectFragment.newInstance(CorrectionFragment.this));
				else {
					new SumItemDialog(getContext()).show(CorrectionFragment.this);
				}
					
			}
		});
		_addType = b.create();
		_corType = v.findViewById(R.id.sp_cor_type);
		_corType.setAdapter(new ArrayAdapter<CorrectionTypeE>(getContext(), android.R.layout.simple_list_item_1,CorrectionTypeE.values()));
		_corType.setSelectedItem(order().getCorrectionType());
		_corDoc = v.findViewById(R.id.sp_cor_doc);
		_corDoc.setInputType(InputType.TYPE_NULL);
		_corDoc.setOnClickListener(this);
		updateBaseDoc();
		super.bind(v);
	}
	
	private void updateBaseDoc() { 
		String s = order().getBaseDocumentNumber();
		if(!s.isEmpty()) {
			if(order().getBaseDocumentDate() > 0)
				s += " от "+Utils.formatDate(order().getBaseDocumentDate());
		}
		_corDoc.setText(s);
	}
	
	@Override
	protected boolean commit() {
		if(order().getBaseDocumentNumber().isEmpty() || order().getBaseDocumentDate() == 0) {
			Toast.makeText(getContext(), "Не указан документ-основание", Toast.LENGTH_SHORT).show();
			return false;
		}
		order().setType((CorrectionTypeE)_corType.getSelectedItem());
		
		return true;
	}
	public CorrectionFragment() {
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.sp_cor_doc: 
			new DocumentDialog(getContext()).show(order(), new Runnable() {
				@Override
				public void run() {
					updateBaseDoc();
				}
			});
			break;
		case R.id.iv_add:
			if(_order.getTotalSum().compareTo(BigDecimal.ZERO) > 0)
				super.onClick(v);
			else
				_addType.show();
			break;
		default:
			super.onClick(v);
		} 
	}

}
