package rs.mkacca.ui.fragments.menus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.utils.app.MessageQueue.MessageHandler;

public abstract class MenuFragment extends Fragment  implements  View.OnClickListener,MessageHandler {
	private LinearLayout _menu;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_menu == null) {
			_menu = new LinearLayout(getContext());
			_menu.setOrientation(LinearLayout.VERTICAL);
			int p = (int)getContext().getResources().getDimension(R.dimen.tooltip_margin);
			_menu.setPadding(p,p,p,p);
			buildMenu();
			Core.getInstance().registerHandler(this);
		}
		return _menu;
			
	}
	@Override
	public void onStart() {
		super.onStart();
	}
	@Override
	public void onDestroy() {
		Core.getInstance().removeHandler(this);
		super.onDestroy();
	}
	@Override
	public void onStop() {
		super.onStop();
	}
	protected void showMenu(MenuFragment f) {
		getParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.menu_content, f).addToBackStack(f.getClass().getName()).commit();
	}
	protected abstract void buildMenu();
	
	protected <T extends View> T addMenuItem(int strId) {
		String s = strId != 0 ? getContext().getString(strId) : null; 
		return addMenuItem(strId,s);
	}
	@SuppressWarnings({ "deprecation", "unchecked" })
	protected <T extends View> T addMenuItem(int id, String name) {
		TextView v = new TextView(getContext());
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		int p = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics());
		if(id != 0) {
			v.setBackgroundResource(R.drawable.blue_button);
			v.setTextColor(getContext().getResources().getColorStateList(R.drawable.blue_button_text));
			v.setGravity(Gravity.CENTER);
			v.setOnClickListener(this);
			v.setId(id);
			v.setText(name);
			v.setPadding(p, p, p, p);
			v.setMinHeight(48);
		} else {
			lp.height = 1;
			v.setBackgroundColor(getContext().getResources().getColor(android.R.color.darker_gray));
		}
		
		lp.topMargin = p;
		_menu.addView(v,lp);
		return (T)v;
	}
	
	
	public Main getMainActivity() {
		return (Main)getActivity();
	}
	protected LinearLayout getMenuHolder() { return _menu; }
}
