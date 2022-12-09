package rs.mkacca.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import cs.ui.MainActivity;
import rs.fncore.Const;
import rs.fncore.FiscalStorage;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.LoginFragment;

public class Main extends MainActivity {
	private static View _lock;
	private BroadcastReceiver LOCKER = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent e) {
			try {
				if(Core.getInstance().getStorage().isFNOK()) return;
			} catch(RemoteException re) { }
			Main.lock();
			ctx.registerReceiver(FNREADY, new IntentFilter("fncore.ready"));
		}
	};

	private BroadcastReceiver FNREADY = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context ctx, Intent arg1) {
			ctx.unregisterReceiver(this);
			new AsyncFNTask() {

				@Override
				protected int execute(FiscalStorage fs) throws RemoteException {
					try {
						Core.getInstance().updateInfo();
					} catch(Exception e) { }
					return 0;
				}
				protected void postExecute(int result, Object results) {
					Main.unlock();
				};
				
			}.execute();
		}
	};
	
	@Override
	protected void onCreate(Bundle saved) {
		super.onCreate(saved);
		FrameLayout fl = findViewById(android.R.id.content);
		_lock = LayoutInflater.from(this).inflate(R.layout.lock,new LinearLayout(this),false);
		_lock.setVisibility(View.GONE);
		_lock.setOnTouchListener(new View.OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return true;
			}
		});
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT);
		fl.addView(_lock,lp);
		
	}
	
	private boolean hasFragment(Class<? extends Fragment> clazz) {
		for(Fragment f : getSupportFragmentManager().getFragments())
			if(f.getClass()== clazz) return true;
		return false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(Core.getInstance().user() != null && !hasFragment(LoginFragment.class)) {
			showFragment(LoginFragment.lockMode());
		}
		
	}
	@Override
	protected void onNewInstance() {
		setFragment(Core.getInstance().getActiveFragment());
	}

	@Override
	public void disableSideMenu() {
		super.disableSideMenu();
	}
	
	public static void lock() {
		if(_lock != null)
			_lock.setVisibility(View.VISIBLE);
	}
	public static void unlock() {
		if(_lock != null)
			_lock.setVisibility(View.GONE);
		
	}
	@Override
	protected void onDestroy() {
		try {
			unregisterReceiver(LOCKER);
		} catch(Exception | Error e) { }
		super.onDestroy();
	}
	@SuppressWarnings("deprecation")
	public static void pushDocuments(final Context ctx) {
		final ProgressDialog dlg = new ProgressDialog(ctx);
		dlg.setMessage("Отправка документов...");
		dlg.setIndeterminate(true);
		dlg.setButton(DialogInterface.BUTTON_NEUTRAL, "В фоне",(DialogInterface.OnClickListener)null);
		final BroadcastReceiver RCVR = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent e) {
				int cnt = e.getIntExtra(Const.OFD_DOCUMENTS, 0);
				if(cnt == 0 ) try {
					dlg.dismiss();
					Core.getInstance().updateInfo();
				} catch(RemoteException re) { }
				else
					dlg.setMessage("Осталось "+cnt+" документов");
			}
		};
		dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface arg0) {
				ctx.unregisterReceiver(RCVR);
			}
		});
		try {
			dlg.show();
			Core.getInstance().getStorage().pushDocuments();
			ctx.registerReceiver(RCVR, new IntentFilter(Const.OFD_SENT_ACTION));
		} catch(RemoteException re) {
			dlg.dismiss();
			Toast.makeText(ctx, "Ошибка при отправке документов", Toast.LENGTH_SHORT).show();
		}
		
	}
	public void enableWorkMode() {
		setFragment(Core.getInstance().getActiveFragment());
		registerReceiver(LOCKER,new IntentFilter(Intent.ACTION_SCREEN_ON));
	}

	public void unlockUser() {
		getSupportFragmentManager().popBackStack();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}
}
