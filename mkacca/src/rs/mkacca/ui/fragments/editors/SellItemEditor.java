package rs.mkacca.ui.fragments.editors;

import java.math.BigDecimal;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cs.U;
import cs.cashier.ui.widgets.NumberEdit;
import cs.ui.widgets.DialogSpinner;
import rs.data.BarcodeValue;
import rs.data.goods.Good.MarkTypeE;
import rs.data.goods.ISellable;
import rs.fncore.FZ54Tag;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellItem.ItemPaymentTypeE;
import rs.fncore.data.SellItem.SellItemTypeE;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.BarcodeReceiver;
import rs.mkacca.ui.AgentDialog;
import rs.mkacca.ui.IndustryInfoDlg;

public class SellItemEditor extends BaseEditor<SellItem> implements NumberEdit.OnValueChangeListener,BarcodeReceiver  {
	private NumberEdit _price,_qtty,_sum, _excise;
	private enum CodeInputMode {
		AsText,
		AsHex
	}
	private CodeInputMode _iM = CodeInputMode.AsText;
	private TextView _name, _pricename,_item_type,_mark_code, _mt,_add;
	private View _markBox;
	private DialogSpinner _pay_type;
	public static SellItemEditor newInstance(SellItem item, OnValueChangedListener<SellItem> l) {
		SellItemEditor result = new SellItemEditor() ;
		result.setItem(item);
		result.setOnChangedListener(l);
		return result;
	}
	
	public SellItemEditor() {
		setEditorLayout(R.layout.sell_item_editor);
	}
	@Override
	protected void beforeDataBind(View v) {
		super.beforeDataBind(v);
		if(_price == null) {
			_item_type = v.findViewById(R.id.lbl_item_type); 
			_price = v.findViewById(R.id.ed_price);
			_mt = v.findViewById(R.id.lbl_mt);
			_price.setOnValueChangeListener(this);
			_qtty = v.findViewById(R.id.ed_qtty);
			_qtty.setOnValueChangeListener(this);
			_qtty.setDecimalDigits(3);
			_sum = v.findViewById(R.id.ed_sum);
			_sum.setOnValueChangeListener(this);
			_pay_type = v.findViewById(R.id.sp_pay_type);
			_name = v.findViewById(R.id.lbl_name);
			_pricename = v.findViewById(R.id.lbl_price_name);
			_pay_type.setAdapter(new ArrayAdapter<ItemPaymentTypeE>(getContext(), android.R.layout.simple_list_item_1,ItemPaymentTypeE.values()));
			_markBox = v.findViewById(R.id.v_mark_box);
			_mark_code = v.findViewById(R.id.ed_mark_code);
			_add = v.findViewById(R.id.ed_additional);
			v.findViewById(R.id.v_item_rq).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					new IndustryInfoDlg(getContext()).show(getEditItem(), FZ54Tag.T1260_INDUSTRY_ITEM_REQUISIT);
				}
			});
			v.findViewById(R.id.iv_mk_more).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					AlertDialog.Builder b = new AlertDialog.Builder(getContext());
					ArrayAdapter<String> a = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new String [] { "Текстовый","Шестнадцатиричный"});
					b.setTitle("Режим ввода");
					b.setAdapter(a, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface arg0, int w) {
							if(w == 0 ) setTextMode();
							else setHexMode();
						}
					});
					b.show();
				}
			});
			_excise = v.findViewById(R.id.ed_excise);
			v.findViewById(R.id.v_add).setOnClickListener(this);
			v.findViewById(R.id.v_agent).setOnClickListener(this);
		}
	}
	@Override
	protected void afterDataBind(View v) {
		_add.setText(getEditItem().getTagString(FZ54Tag.T1191_EXTRA_ITEM_FIELD));
		_item_type.setText(getEditItem().getType().pName);
		_pay_type.setSelectedItem(getEditItem().getPaymentType());
		_name.setText(getEditItem().getName());
		_pricename.setText("Цена за "+getEditItem().getMeasure().pName);
		_qtty.setText(String.format("%.3f", getEditItem().getQTTY().doubleValue()));
		_price.setText(String.format("%.2f", getEditItem().getPrice().doubleValue()));
		_sum.setText(String.format("%.2f", getEditItem().getSum().doubleValue()));
		ISellable sellable = (ISellable)getEditItem().attachment();
		_markBox.setVisibility(sellable.good().getMarkType() != MarkTypeE.NONE ? View.VISIBLE : View.GONE);
		_mark_code.setText(getEditItem().getMarkingCode().getCode());
		View a = v.findViewById(R.id.v_excises);
		a.setVisibility(View.GONE);
		_excise.setText("0.00");
		if(getEditItem().getType() == SellItemTypeE.EXCISES_GOOD || getEditItem().getType() == SellItemTypeE.EXCISABLE_MARK_GOODS_MARK || 
				getEditItem().getType() == SellItemTypeE.EXCISABLE_MARK_GOODS_NO_MARK) {
			if(Core.getInstance().kkmInfo().isExcisesMode()) { 
				a.setVisibility(View.VISIBLE);
				if(getEditItem().hasTag(FZ54Tag.T1229_EXCISE_SUM))
				_excise.setText(String.format("%.2f", getEditItem().getTag(FZ54Tag.T1229_EXCISE_SUM).asDouble()));
			}
		}
		
		
	}
	@Override
	protected void doSave() {
		ISellable sellable = (ISellable)getEditItem().attachment();
		double qtty = _qtty.doubleValue();
		if(sellable.maxQtty() > 0 && qtty > sellable.maxQtty()) {
			Toast.makeText(getContext(), "Превышено максимальное количество", Toast.LENGTH_SHORT).show();
			_qtty.setText(String.format("%.3f", sellable.maxQtty()));
			return;
		}
		if(_add.getText().toString().isEmpty())
			getEditItem().remove(FZ54Tag.T1191_EXTRA_ITEM_FIELD);
		else
			getEditItem().add(FZ54Tag.T1191_EXTRA_ITEM_FIELD,_add.getText().toString());
		getEditItem().setQtty(BigDecimal.valueOf(qtty));
		getEditItem().setPrice(BigDecimal.valueOf(_price.doubleValue()));
		getEditItem().setPaymentType((ItemPaymentTypeE)_pay_type.getSelectedItem());
		getEditItem().remove(FZ54Tag.T1229_EXCISE_SUM);
		if(_excise.doubleValue() > 0) 
			getEditItem().add(FZ54Tag.T1229_EXCISE_SUM, BigDecimal.valueOf(_excise.doubleValue()));
		if(sellable.good().getMarkType() != MarkTypeE.NONE) {
			if(!updateMark(sellable.good().getMarkType())) return;
		}
		super.doSave();
	}

	private boolean updateMark(MarkTypeE type) {
		if(type == MarkTypeE.NONE) return true;
		if(_mark_code.getText().toString().trim().isEmpty()) {
			Toast.makeText(getContext(), "Не указан код маркировки!", Toast.LENGTH_SHORT).show();
			_mark_code.requestFocus();
			return false;
		}
		String mk = _mark_code.getText().toString();
		if(_iM == CodeInputMode.AsHex ) {
			mk= fromHex(mk);
			if(mk == null) {
				Toast.makeText(getContext(), "Не верное значение кода маркировки",Toast.LENGTH_SHORT).show();
				_mark_code.requestFocus();
				return false;
			}
		}
		int p = mk.indexOf("\\u001d");
		while(p > -1) {
			mk = mk.replace("\\u001d", ""+(char)(0x1d));
			p = mk.indexOf("\\u001d");
		}
			
		Log.d("fncore2", "Mark code is "+mk);
		
		getEditItem().setMarkingCode(mk.trim(), OrderTypeE.INCOME);
		return true;
	}
	
	@Override
	public boolean onNewValue(NumberEdit sender, double value) {
		if(value < 0.01) {
			Toast.makeText(getContext(), "Значение не может быть нулем или меньше",Toast.LENGTH_LONG).show();
			return true;
		}
		if(sender == _qtty) {
			_sum.setText(String.format("%.2f", value*_price.doubleValue()));
		} 
		else if(sender == _sum) {
			ISellable sellable = (ISellable)getEditItem().attachment();
			if(sellable.price() > 0) {  
				double pc =  (sellable.price() * _qtty.doubleValue()) / 100.0;
				double dif = (sellable.price() * _qtty.doubleValue()) - value;
				if(dif < 0) {
					Toast.makeText(getContext(), "Вы не можете продавать выше цены по каталогу", Toast.LENGTH_LONG).show();
					return false;
				}
				if(Core.getInstance().user().getMaxDicsount() > 0) {
					if(dif / pc > Core.getInstance().user().getMaxDicsount()) {
						Toast.makeText(getContext(), "Превышен допустимый размер скидки",Toast.LENGTH_LONG).show();
						return false;
					}
				}
			}
			_price.setText(String.format("%.2f", value / _qtty.doubleValue()));
		}
		else if(sender == _price) {
			ISellable sellable = (ISellable)getEditItem().attachment();
			if(sellable.price() > 0) {  
				double pc =  sellable.price() / 100.0;
				double dif = sellable.price()  - value;
				if(dif < 0) {
					Toast.makeText(getContext(), "Вы не можете продавать выше цены по каталогу", Toast.LENGTH_LONG).show();
					return false;
				}
				
				if(Core.getInstance().user().getMaxDicsount() > 0) {
					if(dif / pc > Core.getInstance().user().getMaxDicsount()) {
						Toast.makeText(getContext(), "Превышен допустимый размер скидки",Toast.LENGTH_LONG).show();
						return false;
					}
				}
			}
			_sum.setText(String.format("%.2f", value * _qtty.doubleValue()));
			return true;
		}
		
		return true;
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Предмет расчета");
		if(_markBox.getVisibility() == View.VISIBLE)
			Core.getInstance().scaner().start(getContext(), this);
		setupButtons(this, R.id.iv_save,R.id.iv_del);
	}
	@Override
	public void onStop() {
		Core.getInstance().scaner().stop();
		super.onStop();
	}
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.iv_del:
			U.confirm(getContext(), "Удалить предмет расчета из чека?", new Runnable() {
				@Override
				public void run() {
					doDelete();
				}
			});
			break;
		case R.id.v_add: {
				View ed = getActivity().getLayoutInflater().inflate(R.layout.additional, new LinearLayout(getContext()),false);
				final TextView cc = ed.findViewById(R.id.ed_cc);
				final TextView custom = ed.findViewById(R.id.ed_customer_code);
				cc.setText(getEditItem().getTagString(FZ54Tag.T1230_COUNTRY_ORIGIN));
				custom.setText(getEditItem().getTagString(FZ54Tag.T1231_CUSTOMS_DECLARATION_NO));
				AlertDialog.Builder b = new AlertDialog.Builder(getContext());
				b.setView(ed);
				b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						if(cc.getText().toString().isEmpty())
							getEditItem().remove(FZ54Tag.T1230_COUNTRY_ORIGIN);
						else 
							getEditItem().add(FZ54Tag.T1230_COUNTRY_ORIGIN, cc.getText().toString());
						if(custom.getText().toString().isEmpty())
							getEditItem().remove(FZ54Tag.T1231_CUSTOMS_DECLARATION_NO);
						else 
							getEditItem().add(FZ54Tag.T1231_CUSTOMS_DECLARATION_NO, custom.getText().toString());
					}
					
				});
				b.setNegativeButton(android.R.string.cancel, null);
				b.show();
			}
			break;
		case R.id.v_agent: {
			new AgentDialog(getContext()).show(getEditItem().getAgentData());
		}
			break;
		default:	
			super.onClick(v);
		}
	}
	
	
	private String fromHex(String hex) {
		try {
			String mk = "";
			if(hex.length() % 2 != 0) 
			throw new Exception();
			while(hex.length() > 0 ) {
				String hd = hex.substring(0,2);
				char c = (char)Byte.parseByte(hd,16);
				hex =hex.substring(2);
				mk += c;
			}
			return mk;
		} catch(Exception e) {
			return null;
		}
		
	}
	private String toHex(String mk) {
		String hex = "";
		for(int i=0;i<mk.length();i++)
			hex += String.format("%02X", (byte)mk.charAt(i));
		return hex;
	}
	private void setTextMode() {
		if(_iM == CodeInputMode.AsText) return;
		String mk = fromHex(_mark_code.getText().toString());
		if(mk != null) {
			_mark_code.setText(mk);
			_iM = CodeInputMode.AsText;
			_mt.setText("Код маркировки");
		} else
			Toast.makeText(getContext(), "Ошибка в коде маркировки", Toast.LENGTH_SHORT).show();
	}

	private void setHexMode() {
		if(_iM == CodeInputMode.AsHex) return;
		_mark_code.setText(toHex(_mark_code.getText().toString()));
		_iM = CodeInputMode.AsHex;
		_mt.setText("Код маркировки (HEX)");
	}

	@Override
	public void onBarcode(BarcodeValue code) {
		if(_iM == CodeInputMode.AsHex)
			_mark_code.setText(toHex(code.CODE));
		else 
			_mark_code.setText(code.CODE);
		
	}

	
	

}
