package rs.mkacca.ui.fragments.editors;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import cs.orm.ORMHelper;
import rs.data.App;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.BaseListFragment;
import rs.mkacca.ui.widgets.Launcher;

public class AppsEditorFragment extends BaseListFragment<App> {
	private List<App> AVAIL_APPS = new ArrayList<>();
	public static AppsEditorFragment newInstance(Launcher l) {
		AppsEditorFragment result = new AppsEditorFragment();
		result._l = l;
		return result;
	}
	private Launcher _l;
	public AppsEditorFragment() {
		setListItems(ORMHelper.loadAll(App.class));
		PackageManager pm = Core.getInstance().getPackageManager();
		List<PackageInfo> all =  pm.getInstalledPackages(0);
		for(PackageInfo info : all) {
			if(pm.getLaunchIntentForPackage(info.packageName) != null &&  !Core.getInstance().getPackageName().equals(info.packageName)) {
				boolean isInstalled = false;
				for(App app : getItems())
					if(app.packageName().equals(info.packageName)) {
						isInstalled = true;
						break;
					}
				if(!isInstalled)
					AVAIL_APPS.add(new App(info));
			}
		}
		
	}
	
	@Override
	protected boolean onDeleteItem(App item) {
		if(item.delete()) {
			getItems().remove(item);
			AVAIL_APPS.add(item);
			return true;
		} 
		return false;
	}
	
	public static class AppAdapter extends BaseAdapter {

		private List<App> APPS;
		private Context _ctx;
		public AppAdapter(List<App> apps, Context ctx) {
			APPS = apps;
			_ctx = ctx;
		}
		@Override
		public int getCount() {
			return APPS.size();
		}

		@Override
		public App getItem(int p) {
			return APPS.get(p);
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int p, View v, ViewGroup vg) {
			if(v == null) {
				v = ((Activity)_ctx).getLayoutInflater().inflate(android.R.layout.simple_list_item_1,vg,false);
				((TextView)v).setCompoundDrawablePadding(8);
				((TextView)v).setGravity(Gravity.CENTER_VERTICAL);
			}
			TextView tv = (TextView)v;
			App app = getItem(p);
			tv.setText(app.getName());
			tv.setCompoundDrawablesRelativeWithIntrinsicBounds(app.getIcon(), null, null, null);
			return v;
		}
		
	}
	private AppAdapter _adapter;
	@Override
	protected void onNewItem() {
		if(_adapter == null)
			_adapter = new AppAdapter(AVAIL_APPS, getContext());
		AlertDialog.Builder b = new AlertDialog.Builder(getContext());
		b.setAdapter(_adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int p) {
				App item = AVAIL_APPS.remove(p);
				item.store();
				getItems().add(item);
				update();
			}
		});
		b.show();
	}
	
	@Override
	protected void setPopupMenuItems(App item, Menu menu) {
		menu.add(0, R.id.mi_delete, -1, R.string.do_delete);
	}
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Доступные приложения");
	}
	@Override
	public void onDestroy() {
		if(_l != null) _l.reload();
		super.onDestroy();
	}
}
