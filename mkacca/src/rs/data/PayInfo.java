package rs.data;

import org.json.JSONException;
import org.json.JSONObject;

import rs.fncore.Const;

public class PayInfo {
	private String _number = Const.EMPTY_STRING;
	private String _rrn = Const.EMPTY_STRING;
	private void parse(JSONObject o) {
		try {
			_number = o.getString("no");
			_rrn = o.getString("rrn");
		} catch(JSONException jse) { }
	}
	public PayInfo(String s) {
		try {
			parse(new JSONObject(s));
		} catch(JSONException jse) { }
	}
	public PayInfo(String no, String rrn) {
		_number = no;
		_rrn = rrn;
	}
	public String number() { return _number; }
	public String rrn() { return _rrn; }
	@Override
	public String toString() {
		JSONObject o = new JSONObject();
		try {
			o.put("no", _number);
			o.put("rrn", _rrn);
		} catch(JSONException jse) { }
		return o.toString();
	}
}
