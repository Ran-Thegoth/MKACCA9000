package rs.mkacca.ui.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import cs.ui.fragments.BaseFragment;
import cs.ui.widgets.DialogSpinner;
import rs.fncore.data.PrintSettings;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.utils.ImagePrinter;

public class PrnSettings extends BaseFragment
		implements OnItemSelectedListener, OnValueChangeListener, View.OnClickListener {

	private View _content;
	private boolean _pDisabled;
	private ImagePrinter _ip;
	private ImageView _preview;
	private String SAMPLE;
	private NumberPicker _lm, _rm, _fs;
	private DialogSpinner _fonts;
	private PrintSettings _ps;

	public PrnSettings() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_content == null) {
			try {
				InputStream fis = getResources().getAssets().open("sample.txt");
				byte[] b = new byte[fis.available()];
				fis.read(b);
				fis.close();
				SAMPLE = new String(b);
			} catch (IOException ioe) {
			}
			_content = inflater.inflate(R.layout.print_settings, container, false);
			_preview = _content.findViewById(R.id.iv_canvas);
			_lm = _content.findViewById(R.id.p_left_m);
			_rm = _content.findViewById(R.id.p_right_m);
			_fs = _content.findViewById(R.id.p_font_size);
			_fonts = _content.findViewById(R.id.sp_fonts);
			_content.findViewById(R.id.lbl_default).setOnClickListener(this);
			_content.findViewById(R.id.lbl_print).setOnClickListener(this);
			ArrayAdapter<String> fonts = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
			try {
				File f = new File("/system/etc/fonts.xml");
				if (f.exists() && f.canRead()) {
					XmlPullParser p = XmlPullParserFactory.newInstance().newPullParser();
					p.setInput(new InputStreamReader(new FileInputStream(f)));
					int e = p.getEventType();
					while (e != XmlPullParser.END_DOCUMENT) {
						if (e == XmlPullParser.START_TAG && "family".equals(p.getName())) {
							String name = p.getAttributeValue(null, "name");
							if (name != null)
								fonts.add(name);
						}
						e = p.next();
					}
				}
			} catch (Exception e) {
			}
			if (fonts.getCount() == 0)
				fonts.add("monospace");
			try {
				_ps = Core.getInstance().getStorage().getPrintSettings();
			} catch (RemoteException re) {
				_ps = new PrintSettings();
			}
			_fonts.setAdapter(fonts);
			_lm.setMaxValue(150);
			_rm.setMaxValue(150);
			_fs.setMaxValue(40);
			_lm.setValue(_ps.getMargins()[0]);
			_rm.setValue(_ps.getMargins()[1]);
			_fs.setValue(_ps.getDefaultFontSize());
			_fonts.setSelectedItem(_ps.getDefaultFontName());
			_ip = new ImagePrinter();
			doPreview();
			_rm.setOnValueChangedListener(this);
			_lm.setOnValueChangedListener(this);
			_fs.setOnValueChangedListener(this);
			_fonts.setOnItemSelectedListener(this);

		}
		return _content;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Настройки печати");
		setupButtons(this, R.id.iv_save);
	}

	@Override
	public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
		doPreview();

	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		doPreview();

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

	private void doPreview() {
		if (_pDisabled)
			return;
		_ps.setMargins(_lm.getValue(), _rm.getValue());
		_ps.setDefaultFontName(_fonts.getSelectedItem().toString());
		_ps.setDefaultFontSize(_fs.getValue());
		Bitmap b = _ip.print(SAMPLE, _ps).toBitmap();
		Bitmap pw = Bitmap.createBitmap(384, b.getHeight(), Config.ARGB_8888);
		Canvas c = new Canvas(pw);
		c.drawColor(Color.BLACK);
		c.drawBitmap(b, _lm.getValue(), 0, new Paint(Paint.FILTER_BITMAP_FLAG));
		b.recycle();
		_preview.setImageBitmap(pw);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_save:
			try {
				_ps.setMargins(_lm.getValue(), _rm.getValue());
				_ps.setDefaultFontName(_fonts.getSelectedItem().toString());
				_ps.setDefaultFontSize(_fs.getValue());
				Core.getInstance().getStorage().setPrintSettings(_ps);
				getFragmentManager().popBackStack();
			} catch (RemoteException re) {

			}
			break;
		case R.id.lbl_default:
			_pDisabled = true;
			_lm.setValue(0);
			_rm.setValue(0);
			_fs.setValue(20);
			_fonts.setSelectedItem("monospace");
			_pDisabled = false;
			doPreview();
			break;
		case R.id.lbl_print:
			try {
				final PrintSettings o = Core.getInstance().getStorage().getPrintSettings();
				_ps.setMargins(_lm.getValue(), _rm.getValue());
				_ps.setDefaultFontName(_fonts.getSelectedItem().toString());
				_ps.setDefaultFontSize(_fs.getValue());
				Core.getInstance().getStorage().setPrintSettings(_ps);
				Core.getInstance().getStorage().doPrint(SAMPLE);
				new Thread() {
					public void run() {
						try {
							Thread.sleep(1200);
							Core.getInstance().getStorage().setPrintSettings(o);
						} catch (Exception ie) {
						}
					};
				}.start();
			} catch (RemoteException re) {

			}
		}

	}

}
