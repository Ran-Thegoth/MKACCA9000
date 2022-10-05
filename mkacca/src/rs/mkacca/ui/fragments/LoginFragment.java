package rs.mkacca.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import cs.U;
import cs.orm.ORMHelper;
import cs.ui.fragments.BaseFragment;
import cs.ui.widgets.DialogSpinner;
import cs.ui.widgets.PinPad;
import cs.ui.widgets.PinPad.OnPinChangedListener;
import rs.data.User;
import rs.mkacca.Core;
import rs.mkacca.R;

public class LoginFragment extends BaseFragment implements OnItemSelectedListener, OnPinChangedListener {

	private static final String USER_INDEX = "usedIndex";
	private DialogSpinner _users;
	private User _u;
	private PinPad _pinpad;

	public LoginFragment() {
	}

	private int indexOf(String name) {
		for (int i = 0; i < _users.getAdapter().getCount(); i++) {
			User u = (User) _users.getAdapter().getItem(i);
			if (u.toString().equals(name))
				return i;
		}
		return _users.getAdapter().getCount() > 0 ? 0 : -1;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.login, container, false);
		_users = v.findViewById(R.id.sp_users);
		_pinpad = v.findViewById(R.id.v_pinpad);
		_pinpad.setOnPinChangedListener(this);
		_users.setOnItemSelectedListener(this);
		_users.setAdapter(new ArrayAdapter<User>(getContext(), android.R.layout.simple_list_item_1,
				ORMHelper.loadAll(User.class, U.pair("ENABLED", 1))));
		if (savedInstanceState != null)
			_users.setSelection(savedInstanceState.getInt(USER_INDEX));
		else
			_users.setSelection(indexOf(Core.getInstance().getLastUserName()));
		_u = (User) _users.getSelectedItem();
		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		setupButtons(null);
		getActivity().setTitle(R.string.app_name);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(USER_INDEX, _users.getSelectedItemPosition());
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
		_u = (User) _users.getSelectedItem();
		_pinpad.clear();
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		_u = null;
		_pinpad.clear();
	}

	@Override
	public void onPinChanged(String pin) {
		if (_u == null)
			return;
		if (_u.getPIN().equals(pin)) {
			_pinpad.setPinOK();
			enableViews(getView().findViewById(R.id.v_pinpad), false);
			Core.getInstance().setUser(_u);
			new Handler(getContext().getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					setFragment(Core.getInstance().getActiveFragment());
				}
			}, 800);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}
}
