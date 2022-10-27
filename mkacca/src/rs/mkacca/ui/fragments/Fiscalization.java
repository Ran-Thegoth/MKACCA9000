package rs.mkacca.ui.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.json.JSONObject;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import cs.U;
import cs.ui.fragments.BaseFragment;
import cs.ui.widgets.DialogSpinner;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.KKMInfo.FFDVersionE;
import rs.fncore.data.KKMInfo.FiscalReasonE;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.widgets.ItemCard;

public class Fiscalization extends BaseFragment implements View.OnClickListener {

	private View _content;
	private ViewPager _pages;
	private View _left, _right;
	private DialogSpinner _pageName, reason; // ,_ffd;
	private KKMInfo regInfo = new KKMInfo();

	private static final String[] PAGES_NAME = { "Владелец ККМ", "Регистрационные данные", "Налогообложение", "Прочее",
			"ОФД, ОСИМ, ОКП" };
	private static final int[] PAGES_ID = { R.layout.reg_owner_screen, R.layout.reg_number_screen,
			R.layout.reg_tax_mode_screen, R.layout.reg_misc_screen, R.layout.reg_ofd_screen };

	private class PageAdapter extends PagerAdapter
			implements OnPageChangeListener, View.OnClickListener, OnItemSelectedListener {

		private ItemCard<KKMInfo> active;
		private ItemCard<KKMInfo>[] PAGES;
		private ScrollView [] VIEWS;

		@SuppressWarnings("unchecked")
		PageAdapter() {
			PAGES = new ItemCard[PAGES_ID.length];
			VIEWS = new ScrollView[PAGES_ID.length];
			for (int i = 0; i < PAGES.length; i++) {
				PAGES[i] = (ItemCard<KKMInfo>) getActivity().getLayoutInflater().inflate(PAGES_ID[i], _pages, false);
			}
			active = PAGES[0];
		}

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.iv_left)
				_pages.setCurrentItem(_pages.getCurrentItem() - 1);
			else
				_pages.setCurrentItem(_pages.getCurrentItem() + 1);
		}

		@Override
		public int getItemPosition(Object object) {
			for (int i = 0; i < PAGES.length; i++)
				if (PAGES[i] == object)
					return i;
			return -1;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if (PAGES[position].getView().getParent() == null) {
				VIEWS[position] = new ScrollView(getContext());
				VIEWS[position].addView(PAGES[position].getView(),new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
				container.addView(VIEWS[position]);
			}
			return VIEWS[position];
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int page) {
			if (active != PAGES[page]) {
				if (active != null) {
					active.obtain();
				}
				active = PAGES[page];
				active.setItem(regInfo);
			}
			_left.setEnabled(page > 0);
			_right.setEnabled(page < getCount() - 1);
			_pageName.setSelection(page);
		}

		@Override
		public int getCount() {
			return PAGES.length;
		}

		@Override
		public boolean isViewFromObject(View v, Object o) {
			return v == o;
		}

		public void bind() {
			for (int i = 0; i < PAGES.length; i++)
				PAGES[i].setItem(regInfo);
		}

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int p, long arg3) {
			if (_pages.getCurrentItem() != p)
				_pages.setCurrentItem(p);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}

	private PageAdapter _pAdapter;

	@SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_content == null) {
			_content = inflater.inflate(R.layout.reg_screen, container, false);
			reason = _content.findViewById(R.id.sp_reason);
			ArrayAdapter<FiscalReasonE> a = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
			for (int i = 1; i < FiscalReasonE.values().length; i++)
				a.add(FiscalReasonE.values()[i]);
			reason.setAdapter(a);
//			_ffd = _content.findViewById(R.id.sp_ffd);
//			_ffd.setAdapter(new ArrayAdapter<FFDVersionE>(getContext(), android.R.layout.simple_list_item_1,FFDVersionE.values()));
			_pages = _content.findViewById(R.id.v_pages);
			_left = _content.findViewById(R.id.iv_left);
			_right = _content.findViewById(R.id.iv_right);
			_pageName = _content.findViewById(R.id.lbl_page_name);
			_pageName.setAdapter(
					new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, PAGES_NAME));
			_pAdapter = new PageAdapter();
			_pageName.setOnItemSelectedListener(_pAdapter);
			_left.setOnClickListener(_pAdapter);
			_right.setOnClickListener(_pAdapter);
			_pages.setOffscreenPageLimit(_pAdapter.getCount());
			_pages.setOnPageChangeListener(_pAdapter);
			_pages.setAdapter(_pAdapter);
			Main.lock();
			new AsyncFNTask() {
				@Override
				protected int execute(FiscalStorage fs) throws RemoteException {
					DocServerSettings [] ss = new DocServerSettings[3];
					ss[0] = fs.getOFDSettings();
					ss[1] = fs.getOismSettings();
					ss[2] = fs.getOKPSettings();
					int r = fs.readKKMInfo(regInfo);
					regInfo.attach(ss);
					return r;
				}

				@Override
				protected void postExecute(int result, Object results) {
					_pAdapter.bind();
					if (result == Errors.NO_ERROR) {
						if (regInfo.isFNActive())
							reason.setSelectedItem(FiscalReasonE.CHANGE_KKT_SETTINGS);
						else if (regInfo.isFNPresent())
							reason.setSelectedItem(FiscalReasonE.REGISTER);
					} else if (result == Errors.NEW_FN)
						reason.setSelectedItem(FiscalReasonE.REPLACE_FN);
//					_ffd.setSelectedItem(regInfo.getFFDProtocolVersion());
					Main.unlock();

				}
			}.execute();

		}
		return _content;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Фискализация");
		setupButtons(this, R.id.iv_save);
		setCustomButtom(R.drawable.ic_menu_do_fiscal, this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_custom:
			U.confirm(getContext(), "Выполнить процедуру регистрации/изменения параметров?", new Runnable() {
				@Override
				public void run() {
/*					FFDVersionE ffd = (FFDVersionE)_ffd.getSelectedItem();
					if(ffd.ordinal() < regInfo.getFFDProtocolVersion().ordinal() && regInfo.isFNActive()) {
						U.notify(getContext(), "Нельзя указывать меньшую версию ФФД, чем была устрановлена");
						return;
					} */
					for (int i = 0; i < _pAdapter.getCount(); i++) {
						if (!_pAdapter.PAGES[i].obtain()) {
							_pages.setCurrentItem(i);
							return;
						}
					}
					Main.lock();
					new AsyncFNTask() {

						@Override
						protected int execute(FiscalStorage fs) throws RemoteException {
							DocServerSettings [] ss = (DocServerSettings[])regInfo.attachment();
							fs.setOFDSettings(ss[0]);
							fs.setOismSettings(ss[1]);
							fs.setOKPSettings(ss[2]);
							int r = fs.doFiscalization((FiscalReasonE) reason.getSelectedItem(),
									Core.getInstance().user().toOU(), regInfo, regInfo, Const.EMPTY_STRING);
							regInfo.attach(ss);
							return r;
						}

						@Override
						protected void postExecute(int result, Object results) {
							Main.unlock();
							if (result == Errors.NO_ERROR) {
								Core.getInstance().updateLastFNStatus();
								Toast.makeText(getContext(), "Операция выполнена успешно", Toast.LENGTH_SHORT).show();
								getFragmentManager().popBackStack();
							} else
								U.notify(getContext(), "Ошибка фискализации:\n"+Errors.getErrorDescr(result));
						}
					}.execute();
					;
				}
			}, new Runnable() {
				@Override
				public void run() {
					getFragmentManager().popBackStack();

				}
			});
			break;
		case R.id.iv_save: {
			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
			b.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,getResources().getStringArray(R.array.fiscal_disk_ops)), new DialogInterface.OnClickListener() {
				private static final int SAVE_MODE = 1;
				@SuppressWarnings("deprecation")
				@Override
				public void onClick(DialogInterface arg0, final int mode) {
					DialogProperties dp = new DialogProperties();
					dp.save_mode = mode == SAVE_MODE;
					dp.root = new File(Environment.getExternalStorageDirectory(),"MKACCA");
					new FilePickerDialog(getContext(), dp).show(new DialogSelectionListener() {
						@Override
						public void onSelectedFilePaths(FilePickerDialog dlg,String[] files) {
							if(mode == SAVE_MODE) try {
								JSONObject o = new JSONObject();
								for (int i = 0; i < _pAdapter.getCount(); i++) 
									_pAdapter.PAGES[i].obtain();
								o.put("KKM", regInfo.toJSON());
								DocServerSettings [] ss = (DocServerSettings[])regInfo.attachment();
								o.put("OFD", ss[0].toJSON());
								o.put("OISM", ss[1].toJSON());
								o.put("OKP", ss[2].toJSON());
								try(FileOutputStream fos = new FileOutputStream(files[0])) {
									fos.write(o.toString(1).getBytes());
								} 
								Toast.makeText(getContext(), "Файл успешно сохранен!", Toast.LENGTH_SHORT).show();
							} catch(Exception e) {
								U.notify(getContext(), "Ошибка сохранения файла настроек:\n"+e.getLocalizedMessage());
							}
							else try {
								JSONObject o = null;
								try(FileInputStream fis = new FileInputStream(files[0])) {
									byte [] b = new byte[fis.available()];
									fis.read(b);
									o = new JSONObject(new String(b));
								}
								if(o != null) {
									JSONObject j = o.getJSONObject("KKM");
									regInfo.fromJSON(j);
									DocServerSettings [] ss = (DocServerSettings[])regInfo.attachment();
									j = o.getJSONObject("OFD");
									ss[0].fromJSON(j);
									j = o.getJSONObject("OISM");
									ss[1].fromJSON(j);
									j = o.getJSONObject("OKP");
									ss[2].fromJSON(j);
									_pAdapter.bind();
									Toast.makeText(getContext(), "Данные фискализации загружены",Toast.LENGTH_SHORT).show();
								}
							} catch(Exception e) {
								U.notify(getContext(), "Ошибка чтения файла настроек:\n"+e.getLocalizedMessage());
							}
							
						}
					});
				}
			});
			b.show();
		}
		break;
			
		}
	}
}
