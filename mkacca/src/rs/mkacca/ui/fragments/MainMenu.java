package rs.mkacca.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import cs.ui.BackHandler;
import cs.ui.fragments.BaseFragment;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.KKMInfo;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.fragments.menus.Common;
import rs.mkacca.ui.widgets.Launcher;
import rs.utils.app.MessageQueue.MessageHandler;

public class MainMenu extends BaseFragment
		implements View.OnTouchListener, View.OnClickListener, BackHandler, MessageHandler, OnBackStackChangedListener {

	private View _content, kkm_Info;

	private TextView fn_status, fn_sn, kkm_sn, reg_no, shift_state, ffd_fn, mark_allow, ofd_await, v_do_send;
	private Launcher launcher;
	private List<String> _messages = new ArrayList<>();
	private AsyncFNTask FN_CHECKER = new AsyncFNTask() {
		@Override
		protected int execute(FiscalStorage fs) throws RemoteException {
			int r = Core.getInstance().updateInfo();
			Core.getInstance().updateRests();
			return r;
		}

		@Override
		protected void postExecute(int result, Object results) {
			updateInfoScreen();
		};
	};

	private void updateInfoScreen() {
		KKMInfo info = Core.getInstance().kkmInfo();
		ofd_await.setText("-");
		v_do_send.setVisibility(View.GONE);
		shift_state.setText(null);
		shift_state.setTextColor(Color.BLACK);
		fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		fn_sn.setText(info.getFNNumber());
		kkm_sn.setText(info.getKKMSerial());
		reg_no.setText(info.getKKMNumber());
		mark_allow.setText(null);
		_messages.clear();
//		List<String> messages = new ArrayList<>();
		if (info.isFNPresent()) {
			ffd_fn.setText(info.getFFDProtocolVersion().toString());
		} else {
			ffd_fn.setText("-");
		}
		if (info.isFNArchived()) {
			fn_status.setText("Постфискальный режим");
			shift_state.setText("Закрыта");
			if (!info.isOfflineMode())
				ofd_await.setText(String.valueOf(Core.getInstance().ofdInfo().getUnsentDocumentCount()));
			v_do_send.setVisibility(
					Core.getInstance().ofdInfo().getUnsentDocumentCount() > 0 ? View.VISIBLE : View.GONE);
		} else if (info.isFNActive()) {
			fn_status.setText("Готов к работе");
			v_do_send.setVisibility(
					Core.getInstance().ofdInfo().getUnsentDocumentCount() > 0 ? View.VISIBLE : View.GONE);
			mark_allow.setText(info.isMarkingGoods() ? "Да" : "Нет");
			if (info.getFNWarnings().iVal == 0)
				fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_notify_ok, 0);
			else {
				if (info.getFNWarnings().isFNCriticalError() || info.getFNWarnings().isMemoryFull99()
						|| info.getFNWarnings().isFailureFormat() || info.getFNWarnings().isOFDCanceled()) {
					fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_notify_stop, 0);
					if (info.getFNWarnings().isFNCriticalError())
						_messages.add("Критическая ошибка ФН");
					if (info.getFNWarnings().isMemoryFull99())
						_messages.add("Память ФН переполнена, требуется замена");
					if (info.getFNWarnings().isFailureFormat())
						_messages.add("Ошибка фискальных данных");
					if (info.getFNWarnings().isOFDCanceled())
						_messages.add("Работа прекращена по требованию ОФД");
					if (info.getFNWarnings().isOFDTimeout())
						_messages.add("Превышен интервал ожидания ответа от ФН");
				} else {
					fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_notify_warn, 0);
					_messages.add(String.format("Предупреждения ФН %02X", info.getFNWarnings().iVal));
				}
			}
			if (!info.getShift().isOpen())
				shift_state.setText("Закрыта");
			else {
				String s = String.valueOf(info.getShift().getNumber());
				long durationInH = (System.currentTimeMillis() - info.getShift().getWhenOpen()) / (60 * 60 * 1000L);
				if (durationInH >= 24) {
					shift_state.setTextColor(Color.RED);
					_messages.add("Смена открыта более 24 часов!");
					fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_notify_warn, 0);
				} else if (durationInH > 12)
					shift_state.setTextColor(Color.argb(0xFF, 255, 85, 0));
				else
					shift_state.setTextColor(Color.GREEN);
				shift_state.setText(s);

			}
			if (!info.isOfflineMode()) {
				try {
					DocServerSettings ds = Core.getInstance().getStorage().getOFDSettings();
					if (ds.getServerAddress().isEmpty()) {
						_messages.add("Не настроен сервер ОФД");
						fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_notify_warn, 0);
					}
					if (info.isMarkingGoods()) {
						ds = Core.getInstance().getStorage().getOismSettings();
						if (ds.getServerAddress().isEmpty()) {
							_messages.add("Не настроен сервер ОИСМ");
							fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_notify_warn, 0);
						}
					}

				} catch (RemoteException re) {

				}
				ofd_await.setText(String.valueOf(Core.getInstance().ofdInfo().getUnsentDocumentCount()));
			}

		} else if (info.isFNPresent()) {
			fn_status.setText("Новый ФН");
			_messages.add("Проведите фискализацию");
			fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_notify_warn, 0);
		} else {
			fn_status.setText("ФН не установлен");
			_messages.add("Установите ФН");
			fn_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_notify_stop, 0);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_content == null) {
			_content = inflater.inflate(R.layout.mmenu, container, false);
			fn_status = _content.findViewById(R.id.fn_status);
			fn_status.setOnClickListener(this);
			fn_sn = _content.findViewById(R.id.fn_sn);
			kkm_sn = _content.findViewById(R.id.kkm_no);
			ffd_fn = _content.findViewById(R.id.ffd_fn);
			reg_no = _content.findViewById(R.id.kkm_reg_no);
			kkm_Info = _content.findViewById(R.id.kkm_info);
			ofd_await = _content.findViewById(R.id.ofd_await);
			shift_state = _content.findViewById(R.id.shift_state);
			mark_allow = _content.findViewById(R.id.mark_allow);
			v_do_send = _content.findViewById(R.id.v_do_send);
			v_do_send.setPaintFlags(v_do_send.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			v_do_send.setOnClickListener(this);
			_content.findViewById(R.id.kkm_info_button).setOnClickListener(this);
			_content.setOnTouchListener(this);
			_content.findViewById(R.id.app_button).setOnClickListener(this);
			launcher = _content.findViewById(R.id.v_launcher);
			getChildFragmentManager().addOnBackStackChangedListener(this);
			getChildFragmentManager().beginTransaction().replace(R.id.menu_content, new Common()).commit();
		}
		return _content;
	}

	@Override
	public void onStop() {
		Core.getInstance().removeHandler(this);
		super.onStop();
	}

	@Override
	public void onStart() {
		super.onStart();
		fn_status.setText("Обновление...");
		getActivity().setTitle(R.string.app_name);
		Core.getInstance().registerHandler(this);
		FN_CHECKER.execute();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		if (kkm_Info.getVisibility() != View.GONE)
			kkm_Info.setVisibility(View.GONE);
		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.fn_status:
			if (!_messages.isEmpty()) {
				String s = "";
				for (String m : _messages) {
					if (!s.isEmpty())
						s += "\n";
					s += m;
				}
				Toast.makeText(getContext(), s, Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.v_do_send:
			Main.pushDocuments(getContext());
			break;
		case R.id.kkm_info_button:

			kkm_Info.setVisibility(kkm_Info.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
			break;
		case R.id.app_button:
			launcher.setVisibility(launcher.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
			break;
		}
	}

	@Override
	public boolean onBackPressed() {
		if (getChildFragmentManager().getBackStackEntryCount() > 0) {
			getChildFragmentManager().popBackStackImmediate();
			return false;
		}
		if (launcher.getVisibility() == View.VISIBLE) {
			launcher.setVisibility(View.GONE);
			return false;
		}
		return true;
	}

	@Override
	public boolean onMessage(Message msg) {
		if (msg.what == Core.EVT_INFO_UPDATED)
			updateInfoScreen();
		return false;
	}

	@Override
	public void onBackStackChanged() {
		((Main) getActivity()).showBackButton(getChildFragmentManager().getBackStackEntryCount() > 0);

	}
}
