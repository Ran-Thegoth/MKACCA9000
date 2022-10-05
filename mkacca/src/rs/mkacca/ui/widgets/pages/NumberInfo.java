package rs.mkacca.ui.widgets.pages;

import java.util.Random;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import rs.fncore.data.KKMInfo;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.ItemCard;
import rs.utils.Utils;

public class NumberInfo extends LinearLayout implements  ItemCard<KKMInfo>, View.OnClickListener {

	private KKMInfo _info;
	private View _generate;
	private TextView _number;
	public NumberInfo(Context context) {
		super(context);
	}

	public NumberInfo(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NumberInfo(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public NumberInfo(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		_number = findViewById(R.id.ed_kkm_number);
		_generate = findViewById(R.id.iv_generate);
		_generate.setOnClickListener(this);
	}
	@Override
	public void setItem(KKMInfo item) {
		_info = item;
		_number.setText(_info.getKKMNumber());
		((TextView)findViewById(R.id.lbl_device_serial)).setText(_info.getKKMSerial());
		_generate.setVisibility(_info.getFNNumber().startsWith("9999") ? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean obtain() {
		if(!Utils.checkRegNo(_number.getText().toString(), _info.getOwner().getINN(), _info.getKKMSerial())) {
			Toast.makeText(getContext(), "Неверно указан регистрационный номер ККТ", Toast.LENGTH_LONG).show();
			_number.requestFocus();
			return false;
		}
		_info.setKKMNumber(_number.getText().toString());
		return true;
	}

	@Override
	public void onClick(View arg0) {
		Random r = new Random(System.currentTimeMillis());
		String num = String.valueOf(r.nextInt(10000));
		while (num.length() < 10)
			num = "0" + num;
		String inn = _info.getOwner().getINN();
		String device = _info.getKKMSerial();
		while (inn.length() < 12)
			inn = "0" + inn;
		while (device.length() < 20)
			device = "0" + device;

		byte[] b = (num + inn + device).getBytes();
		int sCRC = Utils.CRC16(b, 0, b.length, (short) 0x1021) & 0xFFFF;
		_number.setText(num + String.format("%06d", sCRC));
	}
	@Override
	public View getView() {
		return this;
	}

}
