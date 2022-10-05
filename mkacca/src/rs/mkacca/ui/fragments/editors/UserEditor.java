package rs.mkacca.ui.fragments.editors;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import cs.ui.widgets.DialogSpinner;
import rs.data.User;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.PasswordSetDialog;
import rs.mkacca.ui.widgets.PasswordSetDialog.PasswordSetListener;

public class UserEditor extends BaseEditor<User> implements PasswordSetListener {

	private View _setPin;
	private ViewGroup _roles;
	private PasswordSetDialog _pinpad;
	private String _pin;
	public static UserEditor newInstance(User u, OnValueChangedListener<User> l) {
		UserEditor result = new UserEditor();
		result.setItem(u);
		result.setOnChangedListener(l);
		return result;
	}
	public UserEditor() {
		setEditorLayout(R.layout.user_editor);
	}
	@Override
	protected void afterDataBind(View v) {
		_roles = v.findViewById(R.id.v_roles);
		_setPin = v.findViewById(R.id.v_set_pin);
		_setPin.setOnClickListener(this);
		String [] roleNames = getResources().getStringArray(R.array.user_roles);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8, getResources().getDisplayMetrics()); 
		for(int i=0;i<roleNames.length;i++) {
			if(!roleNames[i].isEmpty()) {
				CheckBox cb = new CheckBox(getContext());
				cb.setTag((1 << i));
				cb.setTextSize(textSize);
				cb.setText(roleNames[i]);
				_roles.addView(cb,lp);
			}
		}
		_pin = getEditItem().getPIN();
		for(int i=0;i<_roles.getChildCount();i++) {
			CompoundButton b = (CompoundButton)_roles.getChildAt(i);
			int role = ((Integer)b.getTag()).intValue();
			b.setChecked(getEditItem().can(role));
		}
		enableViews(v, !getEditItem().can(User.PREBUILD));
		_setPin.setEnabled(true);
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Пользователь");
		setupButtons(this, R.id.iv_save);
	}
	@Override
	protected boolean checkSaveConditions(User item) {
		if(_pin == null || _pin.isEmpty()) {
			notify(R.string.pin_is_not_set);
			return false;
		}
		return true;
	}
	@Override
	protected boolean storeItem(User item) {
		item.setPIN(_pin);
		int role = 0;
		for(int i = 0;i<_roles.getChildCount();i++) {
			CompoundButton cb = (CompoundButton)_roles.getChildAt(i);
			if(cb.isChecked())
				role |= ((Number)cb.getTag()).intValue();
		}
		item.setRoles(role);
		try {
			return item.store();
		} catch(Exception e) {
			notify(e.getMessage());
			return false;
		}
		
	}
	
	@Override
	protected void beforeDataBind(View v) {
		DialogSpinner sp = v.findViewById(R.id.sp_screen);
		sp.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,getResources().getStringArray(R.array.start_screen_names)));
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.v_set_pin:
			if(_pinpad == null)
				_pinpad = new PasswordSetDialog(getContext(), this);
			_pinpad.show(null);
			break;
		default:
			super.onClick(v);
		}
	}
	
	@Override
	public void onPasswordSet(String pin) {
		_pin = pin;
		
	}
}
