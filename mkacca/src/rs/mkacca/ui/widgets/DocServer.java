package rs.mkacca.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import rs.fncore.data.DocServerSettings;
import rs.mkacca.R;

public class DocServer extends LinearLayout implements ItemCard<DocServerSettings> {

	private DocServerSettings _settings;
	private TextView _server,_port,_timeout;
	private CheckBox _sendImmediate;
	public DocServer(Context context) {
		super(context);
	}

	public DocServer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DocServer(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public DocServer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		_server = findViewById(R.id.ed_ofd_server_address);
		_port = findViewById(R.id.ed_ofd_port);
		_sendImmediate = findViewById(R.id.sw_send_now);
		_timeout = findViewById(R.id.ed_ofd_timeout);
	}

	@Override
	public void setItem(DocServerSettings item) {
		_settings = item;
		_server.setText(_settings.getServerAddress());
		_timeout.setText(String.valueOf(_settings.getServerTimeout()));
		_port.setText(String.valueOf(_settings.getServerPort()));
		_sendImmediate.setChecked(_settings.getImmediatelyMode());

	}

	@Override
	public boolean obtain() {
		try {
			int p = Integer.parseInt(_port.getText().toString());
			if(p < 0 || p > Short.MAX_VALUE) throw new NumberFormatException();
			int t = Integer.parseInt(_timeout.getText().toString());
			if(t < 0 ) throw new NumberFormatException();
			_settings.setImmediatelyMode(_sendImmediate.isChecked());
			_settings.setServerTimeout(t);
			_settings.setServerPort(p);
			_settings.setServerAddress(_server.getText().toString());
			return true;
		} catch(NumberFormatException nfe) {

			return false;
		}
	}
	public void clear() {
		_server.setText(null);
		_port.setText("0");
		_timeout.setText("60");
	}
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		_server.setEnabled(enabled);
		_port.setEnabled(enabled);
		_timeout.setEnabled(enabled);
		_sendImmediate.setEnabled(enabled);
	}
	public void set(String server, int port) {
		_server.setText(server);
		_port.setText(String.valueOf(port));
	}
	@Override
	public View getView() {
		return this;
	}

	public void setOFDMode(boolean b) {
		_sendImmediate.setVisibility(b ? View.VISIBLE : View.GONE);

	}

}
