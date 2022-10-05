package rs.mkacca.ui.fragments.menus;

import android.os.Message;
import android.view.View;
import cs.U;
import rs.data.User;
import rs.fncore.data.KKMInfo;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.AboutFragment;
import rs.mkacca.ui.fragments.Fiscalization;
import rs.mkacca.ui.fragments.LoginFragment;
import rs.mkacca.ui.fragments.UserListFragment;

public class Common extends MenuFragment {

	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.string.do_users:
			getMainActivity().showFragment(new UserListFragment());
			break;
		case R.string.do_shift_op:
			showMenu(new ShiftMenu());
			break;
		case R.string.do_cash_op:
			showMenu(new SellMenu());
			break;
		case R.string.do_goods:
			showMenu(new GoodsMenu());
			break;
		case R.string.journals:
			showMenu(new JournalMenu());
			break;
		case R.string.do_reg:
			getMainActivity().showFragment(new Fiscalization());
			break;
		case R.string.do_settings:	
			showMenu(new rs.mkacca.ui.fragments.menus.Settings());
			break;
		case R.string.do_about:
			getMainActivity().showFragment(new AboutFragment());
			break;
		case R.string.change_user:
			U.confirm(getContext(), "Сменить текущего пользователя?", new Runnable() {
				@Override
				public void run() {
					Core.getInstance().setUser(null);
					getMainActivity().setFragment(Core.getInstance().getActiveFragment());
				}
			});
			break;
		}
		
	}

	@Override
	protected void buildMenu() {
		if(Core.getInstance().user().can(User.MANAGE_DEVICE))
			addMenuItem(R.string.do_reg);
		if(Core.getInstance().user().can(User.MANAGE_SHIFT))
			addMenuItem(R.string.do_shift_op);
		if(Core.getInstance().user().can(User.MANAGE_SELL))
			addMenuItem(R.string.do_cash_op);
		if(Core.getInstance().user().can(User.MANAGE_GOODS))
			addMenuItem(R.string.do_goods);
		addMenuItem(0);
		if(Core.getInstance().user().can(User.MANAGE_USERS))
			addMenuItem(R.string.do_users);
		if(Core.getInstance().user().can(User.MANAGE_DEVICE)) {
			addMenuItem(R.string.do_settings);
		}
		addMenuItem(R.string.journals);
		addMenuItem(0);
		addMenuItem(R.string.do_about);
		addMenuItem(R.string.change_user);
	}
	@Override
	public void onStart() {
		super.onStart();
		updateButtons();
	}

	private void updateButtons() {
		KKMInfo info = Core.getInstance().kkmInfo(); 
		View v = getMenuHolder().findViewById(R.string.do_reg);
		if(v != null) {
			if(info.isFNArchived())
				v.setEnabled(false);
			else if(info.isFNActive()) {
				v.setEnabled(!info.getShift().isOpen());
			} else 
				v.setEnabled(info.isFNPresent());
		}
		v = getMenuHolder().findViewById(R.string.do_shift_op);
		if(v != null) v.setEnabled(!info.isFNArchived() && info.isFNActive());
		v = getMenuHolder().findViewById(R.string.do_cash_op);
		if(v  != null) {
			v.setEnabled(false);
			if(!info.isFNArchived() && info.isFNActive()) {
				if(info.getShift().isOpen() && System.currentTimeMillis() -  info.getShift().getWhenOpen() <  Core.ONE_DAY)
					v.setEnabled(true);
			}
		}
	}
	
	@Override
	public boolean onMessage(Message msg) {
		if(msg.what == Core.EVT_INFO_UPDATED) {
			updateButtons();
		}
		return false;
	}

}
