package rs.fncore2.example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.Payment;
import rs.fncore.data.Payment.PaymentTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.VatE;
import rs.fncore.data.SellItem.ItemPaymentTypeE;
import rs.fncore.fncoresample.R;
import rs.fncore.data.SellOrder.OrderTypeE;

public class SellOrderActivity extends Activity implements OnItemSelectedListener, View.OnClickListener {

	public static final String ORDER_EXTRA = "ORDER";
	protected SellOrder ORDER;
	private TextView total;
	private View do_pay;
	private CheckBox use_mark;

	
	private List<String> MARK_CODES = new ArrayList<>();

	private class SellItemAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return ORDER.getItems().size();
		}

		@Override
		public SellItem getItem(int p) {
			return ORDER.getItems().get(p);
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int p, View v, ViewGroup vg) {
			if (v == null) {
				v = getLayoutInflater().inflate(android.R.layout.two_line_list_item, vg, false);
				((TextView) v.findViewById(android.R.id.text1)).setTypeface(Typeface.DEFAULT_BOLD);
				((TextView) v.findViewById(android.R.id.text2)).setGravity(Gravity.END);
			}
			TextView name = v.findViewById(android.R.id.text1);
			TextView details = v.findViewById(android.R.id.text2);
			SellItem item = getItem(p);
			String s = Const.EMPTY_STRING;
			if(!item.getMarkingCode().isEmpty()) 
				s += item.getMarkCheckResult().getMarkTag()+" ";
			s+= item.getItemName();
			name.setText(s);
			details.setText(String.format(Locale.ROOT, "%.3f %s x %.2f = %.2f", item.getQTTY(),
					item.getMeasure().toString(), item.getPrice(), item.getSum()));
			return v;
		}

	}

	private SellItemAdapter ITEMS_ADAPTER;

	public SellOrderActivity() {
		// TODO Auto-generated constructor stub
	}

	protected void setupView() { 
		
	}
	protected boolean commit() {
		return true;
	}
	
	protected SellOrder newOrder() { 
		return new SellOrder(OrderTypeE.INCOME, TaxModeE.COMMON); 
	}
	protected int getViewId() {
		return R.layout.sell_order;
	}
	protected SellOrder getOrder() { return ORDER; }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ORDER = newOrder();
		setContentView(getViewId());
		setTitle("Чек");
		Spinner sp = findViewById(R.id.check_type);
		sp.setAdapter(new ArrayAdapter<OrderTypeE>(this, R.layout.log_row, OrderTypeE.values()));
		sp.setOnItemSelectedListener(this);
		sp.setSelection(OrderTypeE.indexOf(ORDER.getType()));
		sp = findViewById(R.id.tax_mode);
		sp.setAdapter(new ArrayAdapter<TaxModeE>(this, R.layout.log_row, TaxModeE.values()));
		sp.setOnItemSelectedListener(this);
		sp.setSelection(TaxModeE.indexOf(ORDER.getTaxMode()));
		total = findViewById(R.id.total_sum);
		do_pay = findViewById(R.id.v_pay);
		do_pay.setOnClickListener(this);
		findViewById(R.id.add_item).setOnClickListener(this);
		ITEMS_ADAPTER = new SellItemAdapter();
		((ListView) findViewById(R.id.items_list)).setAdapter(ITEMS_ADAPTER);
		use_mark = findViewById(R.id.use_mark);
		setupView();
		
		// Список тестовых кодов маркировки
		MARK_CODES.add(
				"010123456789012321XHe\"ImQ>*A&jOL\u001d91808B\u001d92BCBr3YRDprM1AAWPkjE/RatPM7XyltEtqOTV4Y9bOtnegQLzeh1OVuOZHMfQDSMqTnXjIcM8Yb20qLr4d+Ykfg==");
		MARK_CODES.add(
				"010123456789012321M,7aL0JDGbJCWa\u001d91808B\u001d92CuE2b4wBhPv9XeoBQDEux9wOKeNR4vf4I+q/QbhqzhRGyYQymkkpgtAZUtPHlfp0THGVN6i+D8ZxZQcbTnvEMg==");
		MARK_CODES.add(
				"010123456789012321h=z'e=jOWuUyBI\u001d91808B\u001d92bRRLZuegUFPGAmRC/XERWw4RenYR6pCvEupy+u57JKppK9xQpHbHjQ7PL7waqIt9xk8Rs5OHBwnV1+0iJhQLcw==");
		MARK_CODES.add(
				"010123456789012321UKa%d3DC9ExJj9\u001d91808B\u001d92UPUZ9/JW23zfOEK+EOC5HNTVdL4JhabUwLhP9BPz+Pn1EX1XzAis/FIztHIj0TZfz3iUIGSvJAXy+aX/0hkWjA==");
		MARK_CODES.add(
				"010123456789012321Ng/kkYnMKctFYb\u001d91808B\u001d922LgwirizH3VrlKLE/SuFbeYpXqe8SoIKkGc4uJ+RgnoZElVnaGtgzOoqwqcv0qHd/23GiUQBNGUz1+eyKNEpnw==");
		updateCheckSum();

	}

	@Override
	public void onItemSelected(AdapterView<?> aview, View arg1, int p, long arg3) {
		if (aview.getId() == R.id.tax_mode)
			ORDER.setTaxMode(TaxModeE.values()[p]);
		else
			ORDER.setType(OrderTypeE.values()[p]);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

	private void updateCheckSum() {
		BigDecimal s = ORDER.getTotalSum();
		total.setText(String.format("%.2f", s));
		do_pay.setEnabled(!ORDER.getItems().isEmpty());
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.v_pay: {
			if(!commit()) return;
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setAdapter(
					new ArrayAdapter<PaymentTypeE>(this, android.R.layout.simple_list_item_1, PaymentTypeE.values()),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface arg0, int p) {
							ORDER.addPayment(new Payment(PaymentTypeE.values()[p], ORDER.getTotalSum()));
							setResult(RESULT_OK, new Intent().putExtra(ORDER_EXTRA, ORDER));
							finish();

						}
					});
			b.setTitle("Укажите способ оплаты");
			b.show();
		}
			break;
		case R.id.add_item: {
			SellItem item = new SellItem("Предмет расчета", BigDecimal.ONE, BigDecimal.valueOf(100), VatE.VAT_20);
			item.setPaymentType(ItemPaymentTypeE.FULL);
			item.setMeasure(MeasureTypeE.PIECE);
			if (use_mark.isChecked()) {
				try {

					item.setMarkingCode(MARK_CODES.remove(0), ORDER.getType());
					if (Errors.isOK(Core.getInstance().storage().checkMarkingItem(item))) {
						Toast.makeText(this, "Код маркировки проверен", Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(this, "Неверный код маркировки", Toast.LENGTH_LONG).show();
					Core.getInstance().storage().confirmMarkingItem(item, true);
				} catch (RemoteException re) {

				}
				
				if (MARK_CODES.isEmpty()) {
					use_mark.setChecked(false);
					use_mark.setEnabled(false);
				}
			}
			ORDER.addItem(item);
			ITEMS_ADAPTER.notifyDataSetChanged();
			updateCheckSum();
		}

			break;
		}
	}
	

}
