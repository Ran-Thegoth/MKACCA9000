package rs.mkacca.ui.fragments.menus;

import android.os.Message;
import android.view.View;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.GoodListFragment;
import rs.mkacca.ui.fragments.WeightBarcodesList;

public class GoodsMenu extends MenuFragment {

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.string.weight_barcodes:
			getMainActivity().showFragment(new WeightBarcodesList());
			break;
		case R.string.do_goods:
			getMainActivity().showFragment(new GoodListFragment());
			break;
			
		}

	}

	@Override
	public boolean onMessage(Message arg0) {
		return false;
	}

	@Override
	protected void buildMenu() {
		addMenuItem(R.string.do_goods);
		addMenuItem(R.string.weight_barcodes);

	}

}
