package rs.mkacca.ui.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import cs.U;
import cs.orm.DBObject;
import cs.ui.BackHandler;
import cs.ui.fragments.BaseFragment;
import rs.data.goods.Good;
import rs.data.goods.GoodGroup;
import rs.data.goods.Moveable;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;
import rs.mkacca.ui.fragments.editors.GoodEditor;
import rs.mkacca.ui.fragments.editors.GoodGroupEditor;
import rs.mkacca.ui.widgets.GoodGroupTree;
import rs.mkacca.ui.widgets.GoodGroupTree.OnGroupSelectListener;
import rs.mkacca.ui.widgets.GoodList;
import rs.mkacca.ui.widgets.GoodList.OnItemLongClickListener;
import rs.mkacca.ui.widgets.PopupMenu;
import rs.utils.Exporter;
import rs.utils.Importer;
import rs.utils.Importer.ImportListener;

public class GoodListFragment extends BaseFragment
		implements OnClickListener, OnItemLongClickListener, OnValueChangedListener<GoodGroup>, BackHandler,
		DialogInterface.OnClickListener, OnGroupSelectListener, OnMenuItemClickListener, ImportListener, DialogSelectionListener {

	public GoodListFragment() {
	}

	private GoodList _list;
	private Object _selectedItem;
	private AlertDialog _addDialog, _treeDialog, _extraDialog;

	private OnValueChangedListener<Good> GOOD_LISTENER = new OnValueChangedListener<Good>() {
		@Override
		public void onChanged(Good value) {
		}

		@Override
		public void onDeleted(Good value) {
		}

	};

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Номенклатура");
		setupButtons(this, R.id.iv_add);
		if (_list != null)
			_list.update();
		setCustomButtom(R.drawable.ic_menu_more, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_list == null) {
			_list = (GoodList) inflater.inflate(R.layout.good_list, container, false);
			_list.setOnItemLongClickListener(this);
			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
			b.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,
					getResources().getStringArray(R.array.new_good_menu)), this);
			_addDialog = b.create();
			b = new AlertDialog.Builder(getContext());
			b.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,
					getResources().getStringArray(R.array.good_extra)), this);

			_extraDialog = b.create();
		}
		return _list;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.iv_add) {
			_addDialog.show();
		} else if (v.getId() == R.id.iv_custom) {
			_extraDialog.show();
		}
	}

	@Override
	public void onLongClick(Object item) {
		_selectedItem = item;
		PopupMenu menu = new PopupMenu();
		menu.add(0, R.id.mi_edit, -1, R.string.do_edit);
		menu.add(0, R.id.mi_move, -1, R.string.do_move);
		menu.add(0, R.id.mi_delete, -1, R.string.do_delete);
		String title = null;
		if (item instanceof Good) {
			Good g = (Good) item;
			title = g.name();
			if (g.isUsed())
				menu.add(0, R.id.mi_used, -1, R.string.do_not_use_in_future);
			else
				menu.add(0, R.id.mi_used, -1, R.string.use_in_future);
		} else
			title = ((GoodGroup) item).name();
		menu.show(getActivity(), this, title);

	}

	@Override
	public void onChanged(GoodGroup value) {
		_list.update();
	}

	@Override
	public boolean onBackPressed() {
		return _list.doBack();
	}

	@Override
	public void onDeleted(GoodGroup value) {
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(DialogInterface dialog, int what) {
		if (dialog == _addDialog) {
			switch (what) {
			case 0:
				showFragment(GoodGroupEditor.newInstance(new GoodGroup(_list.getRoot()), this));
				break;
			case 1:
				showFragment(GoodEditor.newInstance(new Good(_list.getRoot()), GOOD_LISTENER));
			}
		} else if (dialog == _extraDialog) {

			switch (what) {
			case 0:
			case 1: {
				DialogProperties dp = new DialogProperties();
				dp.root = new File(Environment.getExternalStorageDirectory(),"MKACCA");
				dp.save_mode = what == 1;
				dp.extensions = new String [] { ".xml" };
				FilePickerDialog fp = new FilePickerDialog(getContext(), dp);
				fp.show(this);
			}
				break;
			case 2:
				try {
					final InputStream is = Core.getInstance().getResources().getAssets().open("ExportData.xml");
					U.confirm(getContext(), "Очистить каталог номенклатур?", new Runnable() {
						@Override
						public void run() {
							Good.clearAll();
							doImport(is);
						}
					}, new Runnable() {
						@Override
						public void run() {
							doImport(is);
						}
					});
				} catch (IOException ioe) {

				}
				break;
			case 3:	
				U.confirm(getContext(), "Очистить каталог номенклатур?", new Runnable() {
					@Override
					public void run() {
						Good.clearAll();
						_list.update();
					}
				});
				break;
			}
		}
	}

	@Override
	public void OnGroupSelect(GoodGroup g) {
		_treeDialog.dismiss();
		if (((Moveable) _selectedItem).moveTo(g))
			_list.update();
	}

	@Override
	public boolean onMenuItemClick(MenuItem mi) {
		switch (mi.getItemId()) {
		case R.id.mi_edit:
			if (_selectedItem instanceof Good)
				showFragment(GoodEditor.newInstance((Good) _selectedItem, GOOD_LISTENER));
			else
				showFragment(GoodGroupEditor.newInstance((GoodGroup) _selectedItem, this));
			break;
		case R.id.mi_move:
			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
			GoodGroupTree tree = new GoodGroupTree(getContext());
			tree.setOnGroupSelectListener(this);
			b.setView(tree);
			b.setNegativeButton(android.R.string.cancel, null);
			_treeDialog = b.create();
			_treeDialog.show();
			break;
		case R.id.mi_delete:
			U.confirm(getContext(), R.string.sure_to_delete_item, new Runnable() {
				@Override
				public void run() {
					if (((DBObject) _selectedItem).delete())
						_list.update();
				}
			});
			break;
		case R.id.mi_used:
			((Good) _selectedItem).setUsed(!((Good) _selectedItem).isUsed());
			if (((Good) _selectedItem).store())
				_list.update();
			break;
		}
		return true;
	}

	private void doImport(InputStream is) {
		Importer.doImport(getContext(),is, this);
	}

	@Override
	public void onImportDone(Exception e) {
		if (e != null)
			U.notify(getContext(), "Ошибка импорта данных: " + e.getLocalizedMessage());
		else 
			U.notify(getContext(), "Импорт успешно завершен");
		_list.update();
	}

	@Override
	public void onSelectedFilePaths(FilePickerDialog dlg, String[] files) {
		if(dlg.getProperties().save_mode) {
			try {
				if(!files[0].endsWith(".xml")) files[0]+=".xml";
				FileOutputStream fos = new FileOutputStream(files[0]);
				Exporter.doExport(getContext(), fos);
			} catch(IOException ioe) {
				U.notify(getContext(), "Ошибка экспорта данных "+ioe.getLocalizedMessage());
			}
		} else {
			try {
				final InputStream is = new FileInputStream(files[0]);
				U.confirm(getContext(), "Очистить каталог номенклатур?", new Runnable() {
					@Override
					public void run() {
						Good.clearAll();
						doImport(is);
					}
				}, new Runnable() {
					@Override
					public void run() {
						doImport(is);
					}
				});
			} catch(IOException ioe) {
				U.notify(getContext(), "Ошибка импорта данных "+ioe.getLocalizedMessage());
			}
		}
		
		
	}

}
