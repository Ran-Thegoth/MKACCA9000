package rs.mkacca.ui.fragments;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cs.ui.fragments.BaseFragment;
import rs.fncore.Const;
import rs.mkacca.R;

public class FileJournal extends BaseFragment {

	
	private RecyclerView _list;
	@SuppressWarnings("deprecation")
	public static FileJournal newInstance(String file) {
		FileJournal result = new FileJournal();
		result.f = new File(Environment.getExternalStorageDirectory(),"MKACCA");
		if(!result.f.exists())
			result.f.mkdir();
		result.f = new File(result.f,file);
		return result;
	}
	
	private File f;
	
	private class JournalRecord {
		String when;
		String serventy;
		String event = Const.EMPTY_STRING;
	}
	
	private class FileJournalRow extends ViewHolder {
		private TextView when,serventy,event;
		public FileJournalRow(View v) {
			super(v);
			serventy = v.findViewById(R.id.lb_serventy);
			when = v.findViewById(R.id.lb_when);
			event = v.findViewById(R.id.lb_event);
		}
		public void bind(JournalRecord r) {
			serventy.setVisibility(r.serventy == null ? View.GONE : View.VISIBLE);
			when.setText(r.when);
			serventy.setText(r.serventy);
			event.setText(r.event);
		}
	}
	private class FileJournalAdapter extends Adapter<FileJournalRow> {

		private FileJournalAdapter() {
			JournalRecord r = null;
			try(LineNumberReader lnr = new LineNumberReader(new FileReader(f))) {
				String line;
				while((line = lnr.readLine()) != null) {
					String [] v = line.split("\t");
					if(v[0].isEmpty()) {
						if(r != null) {
							if(!r.event.isEmpty()) r.event += "\n";
							r.event += v[v.length-1];
						}
					} else {
						if(r != null) RECORDS.add(0,r);
						r = new JournalRecord();
						r.when = v[0];
						if(v.length > 2) 
							r.serventy = v[1];
						if(v.length > 3)
							r.serventy+="\n"+v[2];
						r.event = v[v.length-1];
					}
				}
				if(r != null) 
					RECORDS.add(0,r);
			} catch(IOException ioe) { }
		}
		private List<JournalRecord> RECORDS = new ArrayList<>();
		@Override
		public int getItemCount() {
			return RECORDS.size();
		}

		@Override
		public void onBindViewHolder(FileJournalRow vh, int p) {
			vh.bind(RECORDS.get(p));
			
		}

		@Override
		public FileJournalRow onCreateViewHolder(ViewGroup vg, int arg1) {
			// TODO Auto-generated method stub
			return new FileJournalRow(getActivity().getLayoutInflater().inflate(R.layout.file_journal_row, vg,false));
		}
		
	}
	
	public FileJournal() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(_list == null) {
			_list = new RecyclerView(getContext());
			_list.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false));
			_list.setAdapter(new FileJournalAdapter());
		}
		return _list;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Журнал");
	}

}
