package rs.mkacca.ui.fragments.menus;

import android.os.Message;
import android.view.View;
import rs.data.User;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.FileJournal;
import rs.mkacca.ui.fragments.JournalFragment;

public class JournalMenu extends MenuFragment {

	public JournalMenu() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.string.journal:
			getMainActivity().showFragment(new JournalFragment());
			break;
		case R.string.evt_journal:
			getMainActivity().showFragment(FileJournal.newInstance("MKACCA.log"));
			break;
		case R.string.io_journal:	
			getMainActivity().showFragment(FileJournal.newInstance("FNIO.log"));
			break;
			
		}

	}

	@Override
	public boolean onMessage(Message arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void buildMenu() {
		addMenuItem(R.string.journal);
		addMenuItem(R.string.evt_journal);
		if(Core.getInstance().user().can(User.MANAGE_DEVICE))
			addMenuItem(R.string.io_journal);


	}

}
