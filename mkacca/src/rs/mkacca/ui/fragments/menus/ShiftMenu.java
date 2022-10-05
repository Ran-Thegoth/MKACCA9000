package rs.mkacca.ui.fragments.menus;

import java.math.BigDecimal;

import android.os.Message;
import android.os.RemoteException;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import cs.U;
import rs.data.ShiftSells;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.SellReport;
import rs.mkacca.ui.widgets.CashOperationDialog;
import rs.mkacca.ui.widgets.CashOperationDialog.CashOperationListener;
import rs.utils.ShiftUtils;

public class ShiftMenu extends MenuFragment implements CashOperationListener {

	private TextView _rest, _inOut;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.string.shift_open_close:
			if (Core.getInstance().kkmInfo().getShift().isOpen()) {
				U.confirm(getContext(), "Закрыть текущую смену?", new Runnable() {
					@Override
					public void run() {
						ShiftUtils.closeShift(getContext());

					}
				});
			} else {
				U.confirm(getContext(), "Открыть новую смену?", new Runnable() {
					@Override
					public void run() {
						final int shift = Core.getInstance().kkmInfo().getShift().getNumber();
						if (shift > 0)
							U.confirm(getContext(), "Удалить данные о продажах за смену № " + shift + "?",
									new Runnable() {
										@Override
										public void run() {
											ShiftSells.clearSells(shift);
										}
									});
						ShiftUtils.openShift(getContext());
					}
				});
			}
			break;
		case R.string.shift_rest:
			U.confirm(getContext(), "Запросить отчет о состоянии расчетов?", new Runnable() {
				@Override
				public void run() {
					ShiftUtils.shiftReport(getContext());
				}
			});

			break;
		case R.string.dep_with: {
			new CashOperationDialog(getContext()).show(BigDecimal.ZERO, true, this);
		}
			break;
		case R.string.sell_report:
			getMainActivity().showFragment(new SellReport());

		}
	}

	@Override
	protected void buildMenu() {
		_rest = new TextView(getContext());
		_rest.setGravity(Gravity.CENTER);
		_rest.setText("Наличных в кассе");
		getMenuHolder().addView(_rest, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		_rest = new TextView(getContext());
		_rest.setGravity(Gravity.CENTER);
		_rest.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24,
				getContext().getResources().getDisplayMetrics()));
		_rest.setText(String.format("%.2f", Core.getInstance().cashRest()));
		getMenuHolder().addView(_rest, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		addMenuItem(R.string.shift_open_close);
		_inOut = addMenuItem(R.string.dep_with);
		addMenuItem(R.string.shift_rest);
		addMenuItem(R.string.sell_report);
		updateShiftButton();

	}

	private void updateShiftButton() {
		TextView tv = getMenuHolder().findViewById(R.string.shift_open_close);
		if (Core.getInstance().kkmInfo().getShift().isOpen()) {
			tv.setText("Закрыть смену");
			getMenuHolder().findViewById(R.string.dep_with).setEnabled(true);
		} else {
			tv.setText("Открыть смену");
			getMenuHolder().findViewById(R.string.dep_with).setEnabled(false);
		}
		try {
			if(Core.getInstance().getStorage().isCashControlEnabled())  {
				_rest.setText(String.format("%.2f", Core.getInstance().cashRest()));
				_inOut.setEnabled(true);
			} else {
				_rest.setText("ОТКЛЮЧЕНО");
				_inOut.setEnabled(false);
			}
		} catch(RemoteException re) { }
		
		

	}

	@Override
	public boolean onMessage(Message msg) {
		switch (msg.what) {
		case Core.EVT_INFO_UPDATED:
			updateShiftButton();
			break;
		case Core.EVT_REST_CHANGED:
			_rest.setText(String.format("%.2f", Core.getInstance().cashRest()));
			break;
		}
		return false;
	}

	@Override
	public void onOperation(BigDecimal sum, int opType) {
		new ShiftUtils(getContext(), opType, sum.doubleValue()).execute();

	}

}
