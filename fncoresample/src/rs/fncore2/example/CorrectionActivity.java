package rs.fncore2.example;

import java.text.SimpleDateFormat;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import rs.fncore.data.Correction;
import rs.fncore.data.Correction.CorrectionTypeE;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.fncore.fncoresample.R;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.SellOrder;

public class CorrectionActivity extends SellOrderActivity {

	private static final SimpleDateFormat DF = new SimpleDateFormat("dd/mm/yyyy");
	private TextView docNumber, docDate;
	private AdapterView.OnItemSelectedListener CHANGE_COR_TYPE = new AdapterView.OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int p, long arg3) {
			getOrder().setType(CorrectionTypeE.values()[p]);
			
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
		
	};
	public CorrectionActivity() {
		// TODO Auto-generated constructor stub
	}
	@Override
	protected Correction getOrder() {
		return (Correction)super.getOrder();
	}
	@Override
	protected void setupView() {
		Spinner sp = findViewById(R.id.cor_type);
		sp.setAdapter(new ArrayAdapter<CorrectionTypeE>(this, R.layout.log_row,CorrectionTypeE.values()));
		sp.setOnItemSelectedListener(CHANGE_COR_TYPE);
		docNumber = findViewById(R.id.doc_number);
		docNumber.setText("1");
		docDate = findViewById(R.id.doc_date);
		docDate.setText(DF.format(System.currentTimeMillis()));
	}
	@Override
	protected boolean commit() {
		try {
		getOrder().setBaseDocumentDate(DF.parse(docDate.getText().toString()));
		getOrder().setBaseDocumentNumber(docNumber.getText().toString());
		} catch(Exception e) {
			Toast.makeText(this, "Неверные данные",Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	@Override
	protected SellOrder newOrder() {
		return new Correction(CorrectionTypeE.BY_OWN, OrderTypeE.OUTCOME, TaxModeE.COMMON);
	}
	@Override
	protected int getViewId() {
		return R.layout.correction;
	}
}
