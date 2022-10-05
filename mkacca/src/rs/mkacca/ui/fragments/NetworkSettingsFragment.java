package rs.mkacca.ui.fragments;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ScrollView;
import cs.ui.fragments.BaseFragment;
import rs.fncore.FiscalStorage;
import rs.fncore.data.DocServerSettings;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.pages.NetworkInfo;

public class NetworkSettingsFragment extends BaseFragment implements View.OnClickListener {

	private NetworkInfo _nInfo;
	private ScrollView _sw;
	DocServerSettings[] _servers = new DocServerSettings[3];

	public NetworkSettingsFragment() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_sw == null) {
			_sw = new ScrollView(getContext());
			int p = (int) getContext().getResources().getDimension(R.dimen.tooltip_margin);
			_sw.setPadding(p, p, p, p);
			_nInfo = (NetworkInfo) inflater.inflate(R.layout.reg_ofd_screen, _sw, false);
			_sw.addView(_nInfo, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			_nInfo.setItem(Core.getInstance().kkmInfo());
			_nInfo.disableOfdSelection();
			new AsyncFNTask() {
				@Override
				protected int execute(FiscalStorage fs) throws RemoteException {
					_servers[0] = fs.getOFDSettings();
					_servers[1] = fs.getOismSettings();
					_servers[2] = fs.getOKPSettings();
					return 0;
				}

				@Override
				protected void postExecute(int result, Object results) {
					_nInfo.setServers(_servers);
				}
			}.execute();
			;
		}
		return _sw;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Настройки серверов");
		setupButtons(this, R.id.iv_save);
	}

	@Override
	public void onClick(View arg0) {
		if (_nInfo.obtain())
			new AsyncFNTask() {
				@Override
				protected int execute(FiscalStorage fs) throws RemoteException {
					fs.setOFDSettings(_servers[0]);
					fs.setOismSettings(_servers[1]);
					fs.setOKPSettings(_servers[2]);
					return 0;
				}
				protected void postExecute(int result, Object results) {
					getFragmentManager().popBackStack();
				};

			}.execute();
	}

}
