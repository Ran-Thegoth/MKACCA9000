package rs.mkacca.ui.fragments.menus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import cs.U;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.ArchiveReport;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.DB;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.fragments.EPaymentsFragment;
import rs.mkacca.ui.fragments.FNMode;
import rs.mkacca.ui.fragments.NetworkSettingsFragment;
import rs.mkacca.ui.fragments.PrnSettings;
import rs.mkacca.ui.fragments.ScanerSettings;

public class Settings extends MenuFragment implements DialogSelectionListener {

	private TextView export;

	public Settings() {
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.string.fn_mode:
			getMainActivity().showFragment(new FNMode());
			break;
		case R.string.do_reset:
			U.confirm(getContext(), "Выполнить сброс МГМ?", new Runnable() {
				@Override
				public void run() {
					Main.lock();
					new AsyncFNTask() {

						@Override
						protected int execute(FiscalStorage fs) throws RemoteException {
							if (fs.resetFN() == Errors.NO_ERROR) {
								fs.restartCore();
								Core.getInstance().updateInfo();
								Core.getInstance().updateRests();
							}
							return 0;
						}

						@Override
						protected void postExecute(int result, Object results) {
							Main.unlock();
						}
					}.execute();
					;
				}
			});
			break;
		case R.string.scan_mode:
			getMainActivity().showFragment(new ScanerSettings());
			break;
		case R.string.card_services:
			getMainActivity().showFragment(new EPaymentsFragment());
			break;
		case R.string.servers:
			getMainActivity().showFragment(new NetworkSettingsFragment());
			break;
		case R.string.do_android_settinsg:
			getActivity().startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
			break;
		case R.string.do_archive:
			if (Core.getInstance().kkmInfo().getShift().isOpen()) {
				Toast.makeText(getContext(), "Открыта смена, проведение операции не возможно", Toast.LENGTH_LONG)
						.show();
				return;
			}
			U.confirm(getContext(), "Вы уверены, что хотите перевести ККТ в постфискальный режим?", new Runnable() {
				@Override
				public void run() {
					U.confirm(getContext(), "Данная операция не обратима. Продолжить?", new Runnable() {
						@Override
						public void run() {
							Main.lock();
							new AsyncFNTask() {
								File _f;

								@SuppressWarnings("deprecation")
								@Override
								protected int execute(FiscalStorage fs) throws RemoteException {
									ArchiveReport ar = new ArchiveReport();
									int r = fs.doArchive(Core.getInstance().user().toOU(), ar, getTag());
									JSONObject o = null;
									try {
										o = Core.getInstance().kkmInfo().toJSON();
									} catch (JSONException jse) {
										o = null;
									}
									if (r == Errors.NO_ERROR) {
										Core.getInstance().updateInfo();
										Core.getInstance().updateLastFNStatus();
										if (o != null) {
											File f = new File(Environment.getExternalStorageDirectory(), "MKACCA");
											_f = new File(f,
													Core.getInstance().kkmInfo().getOwner().getINN() + ".json");
											try (FileOutputStream fos = new FileOutputStream(_f)) {
												fos.write(o.toString(1).getBytes());
											} catch (Exception ioe) {
											}
										}
									}
									return r;
								}

								protected void postExecute(int result, Object results) {
									Main.unlock();
									if (result == 0) {
										Main.pushDocuments(getContext());
										if (_f != null)
											Toast.makeText(getContext(),
													"Операция выполнена, реквизиты ККТ сохранены в файл "
															+ _f.getName(),
													Toast.LENGTH_LONG).show();
										else
											Toast.makeText(getContext(),
													"Операция выполнена, не удалось сохранить реквизиты ККТ",
													Toast.LENGTH_LONG).show();
									} else
										U.notify(getContext(),
												"Ошибка выполнения операции:\n" + Errors.getErrorDescr(result));
								};
							}.execute();
							;

						}
					});

				}
			});
			break;
		case R.string.export_db: {
			Core.getInstance().db().close();
			@SuppressWarnings("deprecation")
			File f = new File(Environment.getExternalStorageDirectory(), "MKACCA");
			if (!f.exists())
				f.mkdir();
			f = new File(f, DB.DB_FILE);
			try {
				FileInputStream is = new FileInputStream(getContext().getDatabasePath(DB.DB_FILE));
				FileOutputStream os = new FileOutputStream(f);
				byte[] b = new byte[8192];
				int read;
				while ((read = is.read(b)) > 0) {
					os.write(b, 0, read);
				}
				os.close();
				is.close();
				Runtime.getRuntime().exit(0);
			} catch (IOException ioe) {
			}
		}
			break;
		case R.string.do_export_marks: {
			DialogProperties p = new DialogProperties();
			p.save_mode = true;
			p.root = new File(Environment.getExternalStorageDirectory(), "MKACCA");
			FilePickerDialog dlg = new FilePickerDialog(getContext(), p);
			dlg.show(this);
		}
			break;
		case R.string.prn_mode:
			getMainActivity().showFragment(new PrnSettings());
			break;
		}
	}

	@Override
	public boolean onMessage(Message msg) {
		if (msg.what == Core.EVT_INFO_UPDATED)
			export.setEnabled(
					Core.getInstance().kkmInfo().isFNActive() && Core.getInstance().kkmInfo().isOfflineMode());
		return false;
	}

	@Override
	protected void buildMenu() {
		addMenuItem(R.string.fn_mode);
		export = addMenuItem(R.string.do_export_marks);
		export.setEnabled(Core.getInstance().kkmInfo().isFNActive() && Core.getInstance().kkmInfo().isOfflineMode());
		addMenuItem(R.string.servers);
		if (Core.getInstance().kkmInfo().getFNNumber().startsWith("9999")) {
			addMenuItem(R.string.do_reset);
		}
		addMenuItem(R.string.prn_mode);
		addMenuItem(R.string.scan_mode);
		addMenuItem(R.string.card_services);
		addMenuItem(R.string.do_android_settinsg);
		addMenuItem(R.string.do_archive).setEnabled(!Core.getInstance().kkmInfo().isFNArchived());
		// addMenuItem(R.string.export_db);

	}

	@Override
	public void onSelectedFilePaths(FilePickerDialog dlg, final String[] files) {
		Main.lock();
		new AsyncFNTask() {
			
			@Override
			protected int execute(FiscalStorage fs) throws RemoteException {
				return fs.exportMarking(files[0]);
			}
			protected void postExecute(int result, Object results) {
				Main.unlock();
				if(Errors.isOK(result)) 
					Toast.makeText(getContext(), "Операция выполнена успешно.\nФайл "+files[0]+" сохранен", Toast.LENGTH_LONG).show();
				else 
					Toast.makeText(getContext(), "Ошибка выполнения:\n"+Errors.getErrorDescr(result),Toast.LENGTH_LONG).show();
			};
		}.execute();;

	}

}
