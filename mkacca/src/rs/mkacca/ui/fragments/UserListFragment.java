package rs.mkacca.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import cs.orm.ORMHelper;
import rs.data.User;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;
import rs.mkacca.ui.fragments.editors.UserEditor;

public class UserListFragment extends BaseListFragment<User> implements OnValueChangedListener<User> {

	public UserListFragment() {
		setListItems(ORMHelper.loadAll(User.class));
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle(R.string.do_users);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	@Override
	protected void onItemSelected(User item) {
		onEditItem(item);
	}
	@Override
	protected void onEditItem(User item) {
		showFragment(UserEditor.newInstance(item,this));
	}

	@Override
	protected void onNewItem() {
		onEditItem(new User());
	}
	@Override
	protected boolean onDeleteItem(User item) {
		if(item.can(User.PREBUILD)) {
			Toast.makeText(getContext(), R.string.can_t_remove_prebuild_user, Toast.LENGTH_LONG).show();
			return false;
		}
		if(item.id() == Core.getInstance().user().id()) {
			Toast.makeText(getContext(), R.string.can_t_remove_current_user, Toast.LENGTH_LONG).show();
			return false;
		}
		if(item.delete()) {
			getItems().remove(item);
			return true;
		}
		return false;
	}
	@Override
	public void onChanged(User value) {
		if(getItems().indexOf(value) == -1)
			getItems().add(value);
		update();
	}
	@Override
	public void onDeleted(User value) {
		// TODO Auto-generated method stub
		
	}
}
