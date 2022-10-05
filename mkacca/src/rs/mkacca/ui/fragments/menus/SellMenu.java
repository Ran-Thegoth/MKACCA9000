package rs.mkacca.ui.fragments.menus;

import android.os.Message;
import android.view.View;
import cs.U;
import cs.orm.ORMHelper;
import rs.data.CheckStorage;
import rs.data.User;
import rs.fncore.data.Correction;
import rs.fncore.data.Correction.CorrectionTypeE;
import rs.fncore.data.SellOrder;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.CorrectionFragment;
import rs.mkacca.ui.fragments.ReturnFragment;
import rs.mkacca.ui.fragments.SellOrderFragment;

public class SellMenu extends MenuFragment {

	@Override
	public void onClick(View v) {
		final TaxModeE mode =  Core.getInstance().kkmInfo().getTaxModes().iterator().next();
		OrderTypeE type = OrderTypeE.INCOME; 
		switch(v.getId()) {
		case R.string.do_outcome:
			type = OrderTypeE.OUTCOME;
			break;
		case R.string.do_refund_op:
			getMainActivity().showFragment(new ReturnFragment());
			return;
		case R.string.do_correction: {
			Correction c = new Correction(CorrectionTypeE.BY_OWN, OrderTypeE.RETURN_INCOME, mode);
			getMainActivity().showFragment(CorrectionFragment.newInstance(c));
		}
			return;
		}
		final OrderTypeE cType = type; 
		final CheckStorage cs = ORMHelper.load(CheckStorage.class, U.pair(CheckStorage.CHECK_TYPE_FLD, type.ordinal()));
		if(cs != null) {
			U.confirm(getContext(), "Есть незакрытый чек. Восстановить?" , new Runnable() {
				
				@Override
				public void run() {
					getMainActivity().showFragment(SellOrderFragment.newInstance(cs.get()));
				}
			}, new Runnable() {
				
				@Override
				public void run() {
					SellOrder order = new SellOrder(cType, mode);
					getMainActivity().showFragment(SellOrderFragment.newInstance(order));
				}
			});
			return;
		}
		SellOrder order = new SellOrder(type, mode);
		getMainActivity().showFragment(SellOrderFragment.newInstance(order));
	}

	@Override
	public boolean onMessage(Message arg0) {
		return false;
	}

	@Override
	protected void buildMenu() {
		if(Core.getInstance().user().can(User.MANAGE_SELL)) {
			addMenuItem(R.string.do_income);
			addMenuItem(R.string.do_outcome);
		}
		if(Core.getInstance().user().can(User.MANAGE_REFUND)) {
			addMenuItem(R.string.do_refund_op);
			
		}
		if(Core.getInstance().user().can(User.MANAGE_SHIFT)) {
			addMenuItem(R.string.do_correction);
		}
	}

}
