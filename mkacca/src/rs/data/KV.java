package rs.data;

import rs.fncore.Const;

public class KV {

	public String k = Const.EMPTY_STRING, v = Const.EMPTY_STRING;
	public KV() { }
	public KV(String k, String v) {
		this.k = k; this.v = v;
	}
	@Override
	public String toString() {
		return k+"="+v;
	}

}
