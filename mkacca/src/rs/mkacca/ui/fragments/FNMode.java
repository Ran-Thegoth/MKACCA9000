package rs.mkacca.ui.fragments;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import cs.U;
import cs.orm.ORMHelper;
import cs.ui.BackHandler;
import cs.ui.fragments.BaseFragment;
import rs.data.User;
import rs.fncore.FiscalStorage;
import rs.fncore.data.KKMInfo.FNConnectionModeE;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.PinConfirmDialog;

public class FNMode extends BaseFragment implements BackHandler, View.OnClickListener {

	private boolean _isDemo, _isCash;
	private Switch _mode, _cache;
	private View _content;

	public FNMode() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_content == null) {
			_content = inflater.inflate(R.layout.fn_mode, container, false);
			_mode = _content.findViewById(R.id.sw_demo_mode);
			_cache = _content.findViewById(R.id.sw_cash_control);
			_isDemo = Core.getInstance().kkmInfo().getFNNumber().startsWith("9999999999");
			try {
				_isCash = Core.getInstance().getStorage().isCashControlEnabled();
				_cache.setChecked(_isCash);
			} catch(RemoteException re) { }
			_mode.setChecked(_isDemo);
		}
		return _content;
	}

	@Override
	public boolean onBackPressed() {
		if (_isDemo != _mode.isChecked()) {
			onClick(null);
			return false;
		}
		return true;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Режим работы ФН");
		setupButtons(this, R.id.iv_save);
	}

	private void changeFnMode() {
		_isDemo = _mode.isChecked();
		_isCash = _cache.isChecked();  
		Main.lock();
		new AsyncFNTask() {
			@Override
			protected int execute(FiscalStorage fs) throws RemoteException {
				fs.setCashControl(_isCash);
				if( _isDemo == (fs.getConnectionMode() ==FNConnectionModeE.VIRTUAL )) return 0;
				fs.setConnectionMode(_isDemo ? FNConnectionModeE.VIRTUAL : FNConnectionModeE.UART);
				fs.restartCore();
				Core.getInstance().updateInfo();
				Core.getInstance().updateRests();
				return 0;
			}

			@Override
			protected void postExecute(int result, Object results) {
				Main.unlock();
				getFragmentManager().popBackStack();
			}
		}.execute();
	}
	@Override
	public void onClick(View v) {
		U.confirm(getContext(), "Установить новый режим работы?", new Runnable() {
			@Override
			public void run() {
				if(_mode.isChecked()) {
					User u = ORMHelper.load(User.class, U.pair(User.NAME_FLD, User.ADMIN_NAME));
					new PinConfirmDialog(getContext()).show(u.getPIN(), new Runnable() {
						@Override
						public void run() {
							changeFnMode();
						}
					});
				} else
					changeFnMode();
			}
		}, new Runnable() {
			@Override
			public void run() {
				getFragmentManager().popBackStack();

			}
		});

	}

}
