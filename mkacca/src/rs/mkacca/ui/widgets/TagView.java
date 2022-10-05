package rs.mkacca.ui.widgets;

import java.text.DecimalFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import rs.fncore.data.Tag;
import rs.mkacca.R;
import rs.utils.Utils;

public class TagView extends LinearLayout implements OnClickListener, DialogInterface.OnClickListener {
	private static final DecimalFormat DF = new DecimalFormat("#");
	private Tag _tag;
	private TextView _dataView, _tagView;
	public TagView(Context ctx, Tag tag) {
		super(ctx);
		setPadding(6, 6, 6, 6);
		setOnClickListener(this);
		_tag = tag;
		setOrientation(VERTICAL);
		LinearLayout hdr = new LinearLayout(ctx);
		hdr.setOrientation(HORIZONTAL);
		_tagView = new TextView(ctx);
		_tagView.setCompoundDrawablePadding(2);
		_tagView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.custom_dt_raw, 0, 0, 0);
		_tagView.setTypeface(Typeface.DEFAULT_BOLD);
		_tagView.setText(String.valueOf(tag.getId()));
		hdr.addView(_tagView,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
		_dataView = new TextView(ctx);
		_dataView.setGravity(Gravity.END);
		_dataView.setTypeface(Typeface.MONOSPACE);
		_dataView.setText(tag.asHex());
		LayoutParams lp = new LayoutParams(0,LayoutParams.WRAP_CONTENT);
		lp.leftMargin = 5;
		lp.weight = 1.0f;
		hdr.addView(_dataView,lp);
		addView(hdr,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
	}
	@Override
	public void onClick(View v) {
		AlertDialog.Builder b = new AlertDialog.Builder(getContext());
		b.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,getContext().getResources().getStringArray(R.array.data_type_names)), this);
		b.show();
	}
	@Override
	public void onClick(DialogInterface arg0, int p) {
		while(getChildCount() > 1)
			removeView(getChildAt(1));
		switch(p) {
		case 0: 
			_tagView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.custom_dt_raw, 0, 0, 0);
			_dataView.setText(_tag.asHex());
			break;
		case 1:
			_tagView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.custom_dt_string, 0, 0, 0);
			_dataView.setText(_tag.asString());
			break;
		case 2:	
			_tagView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.custom_dt_number, 0, 0, 0);
			_dataView.setText(String.valueOf(_tag.asUInt()));
			break;
		case 3:
			_tagView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.custom_dt_decimal, 0, 0, 0);
			_dataView.setText(DF.format(_tag.asDouble()));
			break;		
		case 4:	
			_tagView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.custom_dt_bool, 0, 0, 0);
			_dataView.setText(_tag.asBoolean() ? "Да" : "Нет"); 
			break;
		case 5:	
			_tagView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.custom_dt_date, 0, 0, 0);
			_dataView.setText(Utils.formatDate(_tag.asTimeStamp()));
			break;
		case 6:
			_tag.unpackSTLV();
			if(!_tag.getChilds().isEmpty()) {
				_dataView.setText(null);
				for(Tag tag : _tag.getChilds()) {
					TagView v = new TagView(getContext(),tag);
					v.setPadding(getPaddingStart()+8, 6, 6, 6);
					addView(v,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
				}
			}
			break;
		case 7: {
			String bits = "";
			String bVal = "";
			String res = "";
			byte [] bytes = _tag.asRaw();
			int bCnt = bytes.length*8-1;;
			int maxs = (String.valueOf(bCnt)+" ").length();
			for(int i=0;i<bytes.length;i++) {
				for(int j=0;j<8;j++) {
					String bv = String.valueOf(bCnt--)+" "; 
					while(bv.length() < maxs) bv += " ";
					bits += bv;
					if((bytes[i] & (1 << j)) != 0) 
						bv = "1";
					else
						bv= "0";
					while(bv.length() < maxs) bv += " ";
					bVal+=bv;
				}
				if(!res.isEmpty()) res += "\n";
				res += bits+"\n"+bVal;
				bits = "";
				bVal = "";
			}
			_dataView.setText(res);
		}
		break;
		}
	}
}
