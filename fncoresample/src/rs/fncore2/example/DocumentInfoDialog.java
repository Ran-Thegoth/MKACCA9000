package rs.fncore2.example;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import rs.fncore.data.Document;
import rs.fncore.data.Tag;
import rs.fncore.fncoresample.R;
import rs.utils.Utils;

@SuppressWarnings("deprecation")
public abstract class DocumentInfoDialog implements TabContentFactory {

	private class TagAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return _doc.getChilds().size();
		}

		@Override
		public Tag getItem(int p) {
			return _doc.getChilds().get(p);
		}

		@Override
		public long getItemId(int p) {
			return getItem(p).getId();
		}

		@Override
		public View getView(int p, View v, ViewGroup vg) {
			if(v == null) {
				v = new TextView(getContext());
				((TextView)v).setTextSize(18);
			}
			Tag tag = getItem(p);
			String label = String.valueOf(tag.getId())+"\t"+Utils.dump(tag.asRaw());
			((TextView)v).setText(label);
			return v;
		}
		
	}
	private Document _doc;
	private AlertDialog _dialog;
	
	protected class TableBuilder {
		private TableLayout _table;
		TableBuilder() {
			_table = new TableLayout(getContext());
			_table.setOrientation(TableLayout.VERTICAL);
			_table.setPadding(4, 4, 4, 4);
		}
		public void addRow(String caption, String value) {
			TableRow tr = new TableRow(getContext());
			tr.setOrientation(TableRow.HORIZONTAL);
			
			TextView tv = new TextView(getContext());
			tv.setText(caption);
			tr.addView(tv,new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT));
			
			tv = new TextView(getContext());
			tv.setGravity(Gravity.END);
			tv.setText(value);
			tv.setTypeface(Typeface.DEFAULT_BOLD);
			
			TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT);
			lp.width = 0;
			lp.weight = 1.0f;
			lp.leftMargin = 6;
			tr.addView(tv,lp);
			_table.addView(tr,new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.WRAP_CONTENT));
		}
		public View getView() { return _table; }
	}
	
	private static final String [] VMODE = { "USER","TAGS"};
	private  Context _ctx; 
	
	public DocumentInfoDialog(Context ctx, Document doc) {
		_ctx = ctx;
		_doc = doc;
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.document_view, new LinearLayout(ctx),false);
		TabHost th = v.findViewById(android.R.id.tabhost);
		th.setup();
		TabSpec ts = th.newTabSpec(VMODE[0]);
		ts.setIndicator("Инфо");
		ts.setContent(this);
		b.setView(v);
		th.addTab(ts);
		ts = th.newTabSpec(VMODE[1]);
		ts.setContent(this);
		ts.setIndicator("В тегах");
		th.addTab(ts);
		b.setPositiveButton(android.R.string.ok, null);
		b.setView(v);
		_dialog = b.create();
	}
	
	protected Context getContext() { return _ctx; }
	
	public void show() { 
		_dialog.show();
	}
	protected Document getDocument() {
		return _doc;
	}
	protected abstract View buildUserView();
	@Override
	public View createTabContent(String tag) {
		if(VMODE[0].equals(tag)) {
			ScrollView sw = new ScrollView(_ctx);
			sw.addView(buildUserView(), new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			return sw;
		} else {
			ListView lv = new ListView(_ctx);
			lv.setAdapter(new TagAdapter());
			return lv;
		}
	}

}
