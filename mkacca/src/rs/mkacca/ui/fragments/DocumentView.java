package rs.mkacca.ui.fragments;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.Toast;
import cs.U;
import cs.ui.fragments.BaseFragment;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.PrintSettings;
import rs.fncore.data.Tag;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.widgets.TagView;
import rs.utils.ImagePrinter;

public class DocumentView extends BaseFragment  implements TabContentFactory, View.OnClickListener{

	private int _id;
	private Tag _document = new Tag();
	private String _pf = Const.EMPTY_STRING;
	private ImagePrinter _ip = new ImagePrinter();
	public static DocumentView newInstance(int id) {
		DocumentView result = new DocumentView();
		result._id = id;
		return result;
	}
	public DocumentView() {
	}
	private class DocumentReader extends AsyncFNTask {
		DocumentReader() {
			Main.lock();
		}
		@Override
		protected int execute(FiscalStorage fs) throws RemoteException {
			int r = fs.getExistingDocument(_id, _document);
			if(r == Errors.NO_ERROR) {
				_pf = fs.getPF(_document);
				if(_pf == null) _pf = Const.EMPTY_STRING;
				_ip.print(_pf, new PrintSettings());
				return Errors.NO_ERROR;
			}
			return r;
		}
		@Override
		protected void postExecute(int result, Object results) {
			Main.unlock();
			if(result != Errors.NO_ERROR) {
				U.notify(getContext(), "Документ не найден", new Runnable() {
					@Override
					public void run() {
						getFragmentManager().popBackStack();
					}
				});
			} else
				setupTabs();
		}
		
	}
	
	private void setupTabs() {
		_tabs.setup();
		for(String s  : TAGS) {
			TabHost.TabSpec spec = _tabs.newTabSpec(s);
			spec.setIndicator(s);
			spec.setContent(this);
			_tabs.addTab(spec);
		}
	}
	private TabHost _tabs;
	private static final String [] TAGS = {"Документ","В тегах"};
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_tabs == null) {
			_tabs = (TabHost)inflater.inflate(R.layout.document_view, container,false);
		}
		return _tabs;
	}

	@Override
	public void onStart() {
		super.onStart();
		if(_document.getChilds().isEmpty())
			new DocumentReader().execute();
		setCustomButtom(R.drawable.ic_menu_do_fiscal, this);
	}
	@Override
	public void onClick(View arg0) {
		if(!_pf.isEmpty()) try {
			Core.getInstance().getStorage().doPrint(_pf);
			Toast.makeText(getContext(), "Документ отправлен на печать",Toast.LENGTH_SHORT).show();
		} catch(RemoteException re) {
			Toast.makeText(getContext(), "Ошибка печати", Toast.LENGTH_LONG).show();
		}
		
	}
	@Override
	public View createTabContent(String tag) {
		ScrollView v = new ScrollView(getContext());
		v.setBackgroundColor(Color.WHITE);
		if(TAGS[0].equals(tag)) {
			final SubsamplingScaleImageView iv = new SubsamplingScaleImageView(getContext());
			iv.setPadding(0, 10, 0, 0);
			iv.setImage(ImageSource.bitmap(_ip.toBitmap()));
			v.addView(iv,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		} else {
			LinearLayout ll = new LinearLayout(getContext());
			ll.setOrientation(LinearLayout.VERTICAL);
			LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
			for(Tag t : _document.getChilds()) 
				ll.addView(new TagView(getContext(), t),lp);
			v.addView(ll,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		}
		
		return v;
	}
	

}
