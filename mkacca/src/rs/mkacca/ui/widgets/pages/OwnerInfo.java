package rs.mkacca.ui.widgets.pages;

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

public class OwnerInfo extends LinearLayout implements ItemCard<KKMInfo> {

	private TextView owner_name, owner_inn,address,place,email;
	private KKMInfo _info;
	public OwnerInfo(Context context) {
		super(context);
	}

	public OwnerInfo(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OwnerInfo(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public OwnerInfo(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		owner_name = findViewById(R.id.ed_owner_name);
		owner_inn = findViewById(R.id.ed_owner_inn);
		address = findViewById(R.id.ed_address);
		place = findViewById(R.id.ed_place);
		email = findViewById(R.id.ed_email);
		
	}
	@Override
	public void setItem(KKMInfo item) {
		_info = item;
		owner_name.setText(_info.getOwner().getName());
		owner_inn.setText(_info.getOwner().getINN());
		address.setText(_info.getLocation().getAddress());
		place.setText(_info.getLocation().getPlace());
		email.setText(_info.getSenderEmail());
	}

	@Override
	public boolean obtain() {
		if(!Utils.checkINN(owner_inn.getText().toString())) {
			Toast.makeText(getContext(), "Неверно указан ИНН", Toast.LENGTH_LONG).show();
			owner_inn.requestFocus();
			return false;
		}
		if(owner_name.getText().toString().toString().isEmpty()) {
			Toast.makeText(getContext(), "Не указано наименование владелца ККТ", Toast.LENGTH_LONG).show();
			owner_name.requestFocus();
			return false;
			
		}
		_info.getOwner().setINN(owner_inn.getText().toString());
		_info.getOwner().setName(owner_name.getText().toString());
		_info.getLocation().setAddress(address.getText().toString());
		_info.getLocation().setPlace(place.getText().toString());
		_info.setSenderEmail(email.getText().toString());
		return true;
	}
	@Override
	public View getView() {
		return this;
	}

}
