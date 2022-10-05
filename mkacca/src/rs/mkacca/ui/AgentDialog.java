package rs.mkacca.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import cs.ui.widgets.DialogSpinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import rs.fncore.data.AgentData;
import rs.fncore.data.AgentTypeE;
import rs.mkacca.R;

public class AgentDialog implements OnCheckedChangeListener, OnItemSelectedListener, DialogInterface.OnClickListener {

	
	
	private AlertDialog _dialog;
	private LinearLayout _holder;
	private DialogSpinner _type;
	private AgentData _data;
	
	public AgentDialog(Context ctx) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);
		View v = LayoutInflater.from(ctx).inflate(R.layout.agent_data, new LinearLayout(ctx),false);
		b.setView(v);
		_holder = v.findViewById(R.id.v_agent_data);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT); 
		for(int i=0;i<AgentData.TAGS_1223.size();i++) {
			CheckBox cb = new CheckBox(ctx);
			cb.setTag(AgentData.TAGS_1223.keyAt(i));
			cb.setText(AgentData.TAGS_1223.valueAt(i));
			cb.setOnCheckedChangeListener(this);
			_holder.addView(cb,lp);
			EditText e = new EditText(ctx);
			e.setInputType(InputType.TYPE_CLASS_TEXT);
			_holder.addView(e,lp);
		}
		for(int i=0;i<AgentData.TAGS_1224.size();i++) {
			CheckBox cb = new CheckBox(ctx);
			cb.setTag(AgentData.TAGS_1224.keyAt(i));
			cb.setText(AgentData.TAGS_1224.valueAt(i));
			cb.setOnCheckedChangeListener(this);
			_holder.addView(cb,lp);
			EditText e = new EditText(ctx);
			e.setInputType(InputType.TYPE_CLASS_TEXT);
			_holder.addView(e,lp);
		}
		
		_type = v.findViewById(R.id.sp_agent_type);
		_type.setOnItemSelectedListener(this);
		_type.setAdapter(new ArrayAdapter<AgentTypeE>(ctx, android.R.layout.simple_list_item_1,AgentTypeE.values()));
		b.setPositiveButton(android.R.string.ok, this);
		b.setNegativeButton(android.R.string.cancel, null);
		_dialog = b.create();
	}
	
	public void show(AgentData data) {
		_data = data;
		_type.setSelectedItem(data.getType());
		for(int i=0;i<_holder.getChildCount();i+=2) {
			CheckBox cb = (CheckBox)_holder.getChildAt(i);
			EditText e = (EditText)_holder.getChildAt(i+1);
			int tagId = ((Number)cb.getTag()).intValue();
			cb.setChecked(data.hasTag(tagId));
			if(cb.isChecked())
				e.setText(data.getTagString(tagId));
			else 
				e.setText(null);
			onCheckedChanged(cb, cb.isChecked());
		}
		_dialog.show();
	}

	@Override
	public void onCheckedChanged(CompoundButton v, boolean checked) {
		int idx = _holder.indexOfChild(v);
		_holder.getChildAt(idx+1).setEnabled(checked);
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		_data.getChilds().clear();
		_data.setType((AgentTypeE)_type.getSelectedItem());
		for(int i=0;i<_holder.getChildCount();i+=2) {
			CheckBox cb = (CheckBox)_holder.getChildAt(i);
			int tagId = ((Number)cb.getTag()).intValue();
			EditText e = (EditText)_holder.getChildAt(i+1);
			if(cb.isChecked())
				_data.add(tagId,e.getText().toString());
		}
		
	}
	

}
