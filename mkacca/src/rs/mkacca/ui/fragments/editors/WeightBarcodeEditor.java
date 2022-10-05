package rs.mkacca.ui.fragments.editors;

import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import cs.ui.widgets.DialogSpinner;
import rs.data.WeightBarcodeParser;
import rs.fncore.Const;
import rs.fncore.data.MeasureTypeE;
import rs.mkacca.R;

public class WeightBarcodeEditor extends BaseEditor<WeightBarcodeParser> implements TextWatcher {
	private View _mPrefix,_mCode,_mWeight;
	private Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
	private TextView _sample, _ePrefix,_eCode,_eWeigth;
	public static WeightBarcodeEditor newInstance(WeightBarcodeParser item, OnValueChangedListener<WeightBarcodeParser> l) {
		WeightBarcodeEditor result = new WeightBarcodeEditor();
		result.setItem(item);
		result.setOnChangedListener(l);
		return result;
	}
	public WeightBarcodeEditor() {
		setEditorLayout(R.layout.weight_barcode_editor);
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Весовой штрихкод");
	}
	@Override
	protected void beforeDataBind(View v) {
		_sample = v.findViewById(R.id.lbl_sample);
		_mPrefix = v.findViewById(R.id.v_prefix);
		_mCode = v.findViewById(R.id.v_code);
		_mWeight = v.findViewById(R.id.v_weight);
		_ePrefix = v.findViewById(R.id.ed_prefix);
		_eCode = v.findViewById(R.id.ed_code_len);
		_eWeigth = v.findViewById(R.id.ed_weight_len);
		P.setTypeface(_sample.getTypeface());
		P.setTextSize(_sample.getTextSize());
		DialogSpinner sp = v.findViewById(R.id.sp_mu);
		sp.setAdapter(new ArrayAdapter<MeasureTypeE>(getContext(), android.R.layout.simple_list_item_1,MeasureTypeE.values()));
	}
	@Override
	protected void afterDataBind(View v) {
		_ePrefix.addTextChangedListener(this);
		_eCode.addTextChangedListener(this);
		_eWeigth.addTextChangedListener(this);
		afterTextChanged(null);
	}
	@Override
	public void afterTextChanged(Editable arg0) {
		String code = _ePrefix.getText().toString();
		if(code.isEmpty()) code = "40";
		LayoutParams lp = _mPrefix.getLayoutParams();
		lp.width = (int)P.measureText(code);
		_mPrefix.setLayoutParams(lp);
		int cLen, wLen;
		try {
			cLen = Integer.parseInt(_eCode.getText().toString());
		} catch(NumberFormatException nfe) {
			cLen = 0;
		}
		try {
			wLen = Integer.parseInt(_eWeigth.getText().toString());
		} catch(NumberFormatException nfe) {
			wLen = 0;
		}
		
		if(_eCode.isFocused()) {
			if(code.length() + cLen +wLen != 12 ) {
				wLen = 12 - code.length() - cLen;
				_eWeigth.setText(String.valueOf(wLen));
			}
		}
		if(_eWeigth.isFocused()) {
			if(code.length() + cLen +wLen != 12 ) {
				cLen = 12 - code.length() - wLen;
				_eCode.setText(String.valueOf(cLen));
			}
		}
		String s = Const.EMPTY_STRING;
		for(int i =0;i<cLen;i++)
			s += "C";
		lp = _mCode.getLayoutParams();
		lp.width = (int)P.measureText(s);
		_mCode.setLayoutParams(lp);
		code += s;
		s = Const.EMPTY_STRING;
		for(int i =0;i<wLen;i++)
			s += "W";
		lp = _mWeight.getLayoutParams();
		lp.width = (int)P.measureText(s);
		_mWeight.setLayoutParams(lp);
		code += s;
		code += "*";
		
		_sample.setText(code);
			
		
	}
	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
	}
	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}
	@Override
	protected boolean checkSaveConditions(WeightBarcodeParser item) {
		String code = _ePrefix.getText().toString();
		int cLen,wLen;
		try {
			cLen = Integer.parseInt(_eCode.getText().toString());
		} catch(NumberFormatException nfe) {
			notify(R.string.field_not_set);
			_eCode.requestFocus();
			return false;
		}
		try {
			wLen = Integer.parseInt(_eWeigth.getText().toString());
		} catch(NumberFormatException nfe) {
			notify(R.string.field_not_set);
			_eWeigth.requestFocus();
			return false;
		}
		if(code.length() + cLen + wLen != 12) {
			notify("Слишком длиный баркод");
			return false;
		}
		return true;
	}
	@Override
	protected boolean storeItem(WeightBarcodeParser item) {
		return item.store();
	}
}
