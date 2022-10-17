package rs.mkacca.ui;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import cs.ui.MainActivity;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.KKMInfo;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;

public class Main extends MainActivity {
	private static View _lock;
	
	private BroadcastReceiver SCREEN_UNLOCK_SENCE = new BroadcastReceiver() {
		KKMInfo i = new KKMInfo();
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			try {
				if(Core.isFNPresent() && Core.getInstance().getStorage().readKKMInfo(i) == Errors.DEVICE_ABSEND) {
					Main.lock();
					new AsyncFNTask() {
						@Override
						protected int execute(FiscalStorage fs) throws RemoteException {
							fs.restartCore();
							int cnt = 0;
							do {
								if(fs.readKKMInfo(i) != Errors.DEVICE_ABSEND) break;
								try { Thread.sleep(2000); } catch(InterruptedException ie) { return 0; }
							} while(cnt++ <  8);
							Core.getInstance().updateInfo();
							return 0;
						}
						protected void postExecute(int result, Object results) {
							Main.unlock();
						};
					}.execute();
				}
			} catch(RemoteException re) { }
		}
	};
	
	private BroadcastReceiver FNREADY = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			Main.lock();
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
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return true;
			}
		});
//		registerReceiver(SCREEN_UNLOCK_SENCE, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(FNREADY,new IntentFilter("fncore.ready"));
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT);
		fl.addView(_lock,lp);
		
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
		unregisterReceiver(SCREEN_UNLOCK_SENCE);
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
}
