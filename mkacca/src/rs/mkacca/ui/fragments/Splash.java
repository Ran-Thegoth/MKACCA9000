package rs.mkacca.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import cs.U;
import cs.ui.fragments.BaseFragment;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.mkacca.Core;
import rs.mkacca.ProgressNotifier;

public class Splash extends BaseFragment implements Runnable, ProgressNotifier, Handler.Callback, View.OnClickListener {
	private final int MSG_PROGRESS = 0;
	private final int MSG_INIT_OK = 1;
	private final int MSG_INIT_FAIL = 2;
	private View _content;
	private Handler _h;
	private ProgressBar _progress;
	private TextView _progressMessage;
	private View _check;

	private Runnable COUNTDOWN = new Runnable() {
		private int _seconds = 5;
		@Override
		public void run() {
			if(_seconds == 0) {
				BaseFragment f = new LoginFragment();
				Core.getInstance().setActiveFragment(f);
				setFragment(f);
				return;
			}
			_progress.setProgress(_seconds * 20);
			_progressMessage.setText("Запуск через "+_seconds+" сек");
			_seconds--;
			_h.postDelayed(this, 950);
		}
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_content == null) {
			_content = inflater.inflate(R.layout.splash, container, false);
			_progress = _content.findViewById(R.id.progress);
			_progressMessage = _content.findViewById(R.id.progress_info);
			_check = _content.findViewById(R.id.do_check);
			_check.setOnClickListener(this);
			_h = new Handler(getContext().getMainLooper(), this);
			new Thread(this).start();
		}
		return _content;
	}

	@Override
	public void run() {
		if (Core.getInstance().init(this))
			_h.sendEmptyMessage(MSG_INIT_OK);
		else
			_h.sendEmptyMessage(MSG_INIT_FAIL);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		((Main)getActivity()).disableSideMenu();
	}
	@Override
	public void onStop() {
		_h.removeCallbacks(COUNTDOWN);
		super.onStop();
	}

	@Override
	public void updateProgress(int progress, String message) {
		Message msg = _h.obtainMessage(MSG_PROGRESS);
		msg.arg1 = progress;
		msg.obj = message;
		_h.sendMessage(msg);
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_PROGRESS:
			_progress.setProgress(msg.arg1);
			_progressMessage.setText(msg.obj.toString());
			break;
		case MSG_INIT_OK:
			_check.setEnabled(true);
			COUNTDOWN.run();
			break;
		case MSG_INIT_FAIL:
			U.notify(getContext(), "Ошибка подключения к фискальному накопителю", new Runnable() {
				@Override
				public void run() {
					Runtime.getRuntime().exit(5);
				}
			});
			break;
		}
		return true;
	}

	@Override
	public void onClick(View arg0) {
		_h.removeCallbacks(COUNTDOWN);
		Core.getInstance().setActiveFragment(new LoginFragment());
		setFragment(new LoginFragment());
		showFragment(new DeviceTestFragment());
		
	}
}
