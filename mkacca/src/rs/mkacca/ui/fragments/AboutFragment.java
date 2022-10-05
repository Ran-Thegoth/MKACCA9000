package rs.mkacca.ui.fragments;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cs.ui.fragments.BaseFragment;
import rs.fncore.Const;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.utils.FileComparator;

public class AboutFragment extends BaseFragment {

	private View _content;
	public AboutFragment() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("О приложении");
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_content == null) {
			_content = inflater.inflate(R.layout.about, container,false);
			try {
				PackageInfo pi = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
				((TextView)_content.findViewById(R.id.lbl_mkacca)).setText(pi.versionName);
			} catch(NameNotFoundException nfe) { }
			try {
				PackageInfo pi = getContext().getPackageManager().getPackageInfo(Const.CORE_PACKAGE, 0);
				((TextView)_content.findViewById(R.id.lbl_fncore)).setText(pi.versionName);
			} catch(NameNotFoundException nfe) { 
				((TextView)_content.findViewById(R.id.lbl_fncore)).setText("-");
			}
			try {
				String s  = Core.getInstance().getStorage().getCheckSum();
				String s1 = ((TextView)_content.findViewById(R.id.lb_desided_cs)).getText().toString();
				((TextView)_content.findViewById(R.id.lbl_crc)).setText(s);
				TextView cr = _content.findViewById(R.id.lb_check_result);
				if(s.equals(s1)) {
					cr.setText("Совпадают");
					cr.setTextColor(0xFF005500);
				} else {
					cr.setText("Ошибка контрольной суммы");
					cr.setTextColor(0xFFff0000);
				}
				
			} catch(Exception re) {
				((TextView)_content.findViewById(R.id.lbl_crc)).setText("-");
			}
			try {
				((TextView)_content.findViewById(R.id.lb_paper_counter)).setText(String.valueOf(Core.getInstance().getStorage().getPaperConsume()));
				
			} catch(RemoteException re) {
				((TextView)_content.findViewById(R.id.lb_paper_counter)).setText("-");
			}
			_content.findViewById(R.id.v_paper_reset).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					try {
						Core.getInstance().getStorage().resetPaperCounter();
						((TextView)_content.findViewById(R.id.lb_paper_counter)).setText("0");
					} catch(RemoteException re) { }
					
				}
			});
			_content.findViewById(R.id.do_check).setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					showFragment(new DeviceTestFragment());
				}
			});
			_content.findViewById(R.id.do_file_check).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					DialogProperties dp = new DialogProperties();
					dp.root = Environment.getExternalStorageDirectory();
					dp.extensions = new String [] { "apk" };
					FilePickerDialog dlg = new FilePickerDialog(getContext(),dp);
					dlg.show(new DialogSelectionListener() {
						@Override
						public void onSelectedFilePaths(FilePickerDialog dlg, String[] files) {
							new FileComparator(getContext(), files[0]);
						}
					});
					
				}
			});
		}
		return _content;
	}

}
