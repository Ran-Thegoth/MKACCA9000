package rs.mkacca.ui.widgets.pages;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cs.ui.widgets.DialogSpinner;
import rs.fncore.data.DocServerSettings;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OU;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.DocServer;
import rs.mkacca.ui.widgets.ItemCard;
import rs.utils.Utils;

public class NetworkInfo extends LinearLayout
		implements ItemCard<KKMInfo>, View.OnClickListener, OnItemSelectedListener {

	private DocServerSettings ofd;
	private DocServerSettings osim;
	private DocServerSettings okp;
	private DocServer ofd_screen, osim_screen, okp_screen;
	private DialogSpinner ofd_select;
	private KKMInfo _info;
	private TextView name, inn, fns;

	private class OFDInfo {
		public String INN, NAME, SERVER, OISM_SERVER;
		public int PORT, OISM_PORT;

		OFDInfo() {
		}

		OFDInfo(String line) {
			String[] v = line.split("\t");
			INN = v[0].trim();
			NAME = v[1].trim();
			SERVER = v[2].trim();
			PORT = Integer.parseInt(v[3].trim());
			OISM_SERVER = v[4].trim();
			OISM_PORT = Integer.parseInt(v[5].trim());
		}

		@Override
		public String toString() {
			return NAME;
		}
	}

	private class OFDAdapter extends ArrayAdapter<OFDInfo> {
		public OFDAdapter() {
			super(NetworkInfo.this.getContext(), android.R.layout.simple_list_item_1);
		}

		public int find(String inn) {
			for (int i = 0; i < getCount(); i++)
				if (inn.equals(getItem(i).INN)) {
					return i;
				}
			return 1;
		}

	}

	private OFDAdapter adapter;

	public NetworkInfo(Context context) {
		super(context);
	}

	public NetworkInfo(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public NetworkInfo(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		ofd_screen = findViewById(R.id.v_ofd_server);


		findViewById(R.id.lbl_ofd).setOnClickListener(this);
		osim_screen = findViewById(R.id.v_osim_server);
		osim_screen.setOFDMode(false);
		findViewById(R.id.lbl_osim).setOnClickListener(this);
		okp_screen = findViewById(R.id.v_okp_server);
		okp_screen.setOFDMode(false);
		findViewById(R.id.lbl_okp).setOnClickListener(this);
		adapter = new OFDAdapter();
		OFDInfo ofd = new OFDInfo();
		ofd.INN = OU.EMPTY_INN_FULL;
		ofd.NAME = "Не используется";
		adapter.add(ofd);
		ofd = new OFDInfo();
		ofd.NAME = "Другой ОФД";
		adapter.add(ofd);

		try (LineNumberReader lnr = new LineNumberReader(
				new InputStreamReader(getContext().getResources().getAssets().open("ofd.csv")))) {
			String line;
			while ((line = lnr.readLine()) != null) {
				adapter.add(new OFDInfo(line));
			}
		} catch (IOException ioe) {
		}
		ofd_select = findViewById(R.id.sp_ofd_list);
		ofd_select.setAdapter(adapter);
		name = findViewById(R.id.ed_ofd_name);
		inn = findViewById(R.id.ed_ofd_inn);
		fns = findViewById(R.id.ed_fns_url);
	}

	public NetworkInfo(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void setItem(KKMInfo item) {
		_info = item;
		inn.setText(_info.ofd().getINN());
		name.setText(_info.ofd().getName());
		fns.setText(_info.getFNSUrl());
		if (_info.isOfflineMode()) {
			ofd_select.setSelection(0);
		} else
			ofd_select.setSelection(adapter.find(_info.ofd().getINN()));
		ofd_select.setOnItemSelectedListener(this);
		DocServerSettings[] ss = (DocServerSettings[]) _info.attachment();
		if(ss == null) return;
		ofd = ss[0];
		osim = ss[1];
		okp = ss[2];
		ofd_screen.setItem(ofd);
		osim_screen.setItem(osim);
		okp_screen.setItem(okp);
	}
	public void setServers(DocServerSettings[] ss) {
		ofd = ss[0];
		osim = ss[1];
		okp = ss[2];
		ofd_screen.setItem(ofd);
		osim_screen.setItem(osim);
		okp_screen.setItem(okp);
	}

	public void disableOfdSelection() {
		inn.setEnabled(false);
		name.setEnabled(false);
		ofd_select.setEnabled(false);
		fns.setEnabled(false);

	}

	@Override
	public boolean obtain() {
		if (!ofd_screen.obtain())
			return false;
		osim_screen.obtain();
		okp_screen.obtain();
		if(!Utils.checkINN(inn.getText().toString())) {
			Toast.makeText(getContext(), "Неверно указан ИНН ОФД", Toast.LENGTH_SHORT).show();
			return false;
		}
		_info.ofd().setINN(inn.getText().toString());
		_info.ofd().setName(name.getText().toString());
		_info.setFNSUrl(fns.getText().toString());
		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.lbl_ofd:
			ofd_screen.setVisibility(ofd_screen.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
			break;
		case R.id.lbl_osim:
			osim_screen.setVisibility(osim_screen.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
			break;
		case R.id.lbl_okp:
			okp_screen.setVisibility(okp_screen.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
			break;
		}

	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int p, long arg3) {
		OFDInfo ofd = adapter.getItem(p);
		ofd_screen.setEnabled(true);
		osim_screen.setEnabled(true);
		switch (p) {
		case 1:
			name.setEnabled(true);
			inn.setEnabled(true);
			break;
		case 0:
			ofd_screen.clear();
			ofd_screen.setEnabled(false);
			osim_screen.clear();
			osim_screen.setEnabled(false);
			name.setEnabled(false);
			inn.setEnabled(false);
			name.setText(ofd.NAME);
			inn.setText(ofd.INN);
			break;
		default:
			name.setEnabled(false);
			inn.setEnabled(false);
			name.setText(ofd.NAME);
			inn.setText(ofd.INN);
			ofd_screen.set(ofd.SERVER, ofd.PORT);
			if (ofd.OISM_PORT > 0)
				osim_screen.set(ofd.OISM_SERVER, ofd.OISM_PORT);
			break;
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public View getView() {
		return this;
	}

}
