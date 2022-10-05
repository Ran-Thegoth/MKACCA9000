package rs.mkacca.ui.widgets;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
// import cs.cashier2.R;
import cs.orm.ORMHelper;
import rs.data.App;
import rs.data.User;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.fragments.editors.AppsEditorFragment;

public class Launcher extends LinearLayout  implements View.OnClickListener {
	private int _onScreen = -1;
	private RecyclerView _list;
	private class AppHolder extends ViewHolder implements View.OnClickListener {
		private App _app;
		public AppHolder(View itemView) {
			super(itemView);
			itemView.setOnClickListener(this);
		}

		public void setApp(App a) {
			_app = a;
			TextView tv = (TextView)itemView;
			tv.setText(_app.getName());
			tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, _app.getIcon(), null, null);
		}
		
		@Override
		public void onClick(View arg0) {
			try {
				Core.getInstance().startActivity(_app.getLauchIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			} catch(Exception | Error e) {
				Toast.makeText(getContext(), "Ошибка запуска приложения", Toast.LENGTH_LONG).show();
			}
		}
		
	}
	private static final String [] DEF_APPS = {
			"com.android.phone","com.android.calculator2"
			
	};
	private class AppAdapter extends Adapter<AppHolder> {

		private List<App> _apps;
		private AppAdapter() {
			_apps = ORMHelper.loadAll(App.class);
			if(_apps.isEmpty()) {
				for(String s : DEF_APPS)
					addApp(s);
				
			}
		}
			
		private void addApp(String pkg) {
			try {
				App app = new App(getContext().getPackageManager().getPackageInfo(pkg, 0));
				if(app.store())
					_apps.add(app);
			} catch(NameNotFoundException nfe) {
				
			}
		}
		@Override
		public int getItemCount() {
			if(_onScreen == -1 || _apps.size() <= _onScreen )
				return _apps.size();
			return _onScreen;
		}

		@Override
		public void onBindViewHolder(AppHolder h, int p) {
			h.setApp(_apps.get(p));
			
		}

		@Override
		public AppHolder onCreateViewHolder(ViewGroup vg, int arg1) {
			TextView v = new TextView(getContext());
			v.setCompoundDrawablePadding(8);
			v.setMaxHeight((getContext().getResources().getDisplayMetrics().widthPixels - 24)/ 3);
			v.setGravity(Gravity.CENTER);
//			v.setTextSize(18);
			v.setEllipsize(TruncateAt.END);
//			v.setBackgroundResource(R.drawable.blue_button);
			v.setPadding(4, 4, 4, 4);
			return new AppHolder(v);
		}

		public void update() {
			_apps = ORMHelper.loadAll(App.class);
			notifyDataSetChanged();
		}
		
	}
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if(visibility == View.VISIBLE) 
			((AppAdapter)_list.getAdapter()).update();
	}
	
	public Launcher(Context context) {
		super(context);
		setupUI();
	}

	public Launcher(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupUI();
	}

	public Launcher(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2);
		setupUI();
	}
	
	private void setupUI() {
		setOrientation(VERTICAL);
		LinearLayout ll = new LinearLayout(getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		TextView tv = new TextView(getContext());
		tv.setText("Приложения");
		tv.setGravity(Gravity.CENTER);
		LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
		lp.weight = 1;
		ll.addView(tv,lp);
		if(Core.getInstance().user().can(User.MANAGE_DEVICE)) {
			lp = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
			lp.rightMargin = 8;
			ImageView iv = new ImageView(getContext());
			iv.setImageResource(R.drawable.ic_stat_notify_settings);
			iv.setOnClickListener(this);
			iv.setId(R.id.iv_setup);
			ll.addView(iv,lp);
			iv = new ImageView(getContext());
			iv.setImageResource(R.drawable.ic_stat_notify_run);
			iv.setOnClickListener(this);
			ll.addView(iv,lp);
			lp.rightMargin = 20;
		}
		
		lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		tv.setTypeface(Typeface.DEFAULT_BOLD);
		lp.bottomMargin = (int)getContext().getResources().getDimension(R.dimen.tooltip_margin);
		addView(ll,lp);
		_list = new RecyclerView(getContext());
		lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
		lp.weight = 1f;
		addView(_list,lp);
		_list.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
		_list.setAdapter(new AppAdapter());
		
	}
	public void setOnScreenApps(int value) {
		_onScreen = value;
		_list.getAdapter().notifyDataSetChanged();
	}
	public void reload() {
		AppAdapter a = new AppAdapter();
		if(a.getItemCount() != _list.getAdapter().getItemCount())
			_list.setAdapter(a);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.iv_setup)
			((Main)getContext()).showFragment(AppsEditorFragment.newInstance(this));
		else {
			List<App> apps = new ArrayList<>();
			PackageManager pm = Core.getInstance().getPackageManager();
			List<PackageInfo> all =  pm.getInstalledPackages(0);
			for(PackageInfo info : all) {
				if(pm.getLaunchIntentForPackage(info.packageName) != null &&  !Core.getInstance().getPackageName().equals(info.packageName)) {
					apps.add(new App(info));
				}
			}
			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
			b.setTitle("Запустить приложение");
			final AppsEditorFragment.AppAdapter adapter = new AppsEditorFragment.AppAdapter(apps,getContext());
			b.setAdapter(adapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int p) {
					getContext().startActivity(adapter.getItem(p).getLauchIntent());
				}
			});
			b.show();
		}
		
	}

}
