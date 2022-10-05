package rs.mkacca.ui.widgets.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Toast;
import rs.fncore.data.AgentTypeE;
import rs.fncore.data.KKMInfo;
import rs.mkacca.R;
import rs.mkacca.ui.widgets.ItemCard;

public class MiscInfo extends LinearLayout implements ItemCard<KKMInfo>, OnCheckedChangeListener {

	private CheckBox [] AGENT_SVC;
	private View autoBox;
	private EditText autoNo;
	private CheckBox automate,offline,encryption;
	private List<CheckBox> MODES = new ArrayList<>();
	private KKMInfo _info;
	public MiscInfo(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public MiscInfo(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public MiscInfo(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	public MiscInfo(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setItem(KKMInfo item) {
		_info = item;
		MODES.get(0).setChecked(_info.isInternetMode());
		MODES.get(1).setChecked(_info.isExcisesMode());
		MODES.get(2).setChecked(_info.isBSOMode());
		MODES.get(3).setChecked(_info.isLotteryMode());
		MODES.get(4).setChecked(_info.isGamblingMode());
		MODES.get(5).setChecked(_info.isAutoPrinter() || _info.isAutomatedMode());
		MODES.get(6).setChecked(_info.isEncryptionMode());
		MODES.get(7).setChecked(_info.isMarkingGoods());
		MODES.get(8).setChecked(_info.isPawnShopActivity());
		MODES.get(9).setChecked(_info.isInsuranceActivity());
		offline.setChecked(_info.isOfflineMode());
		Set<AgentTypeE> agents = _info.getAgentType();
		for(int i=0;i<AGENT_SVC.length;i++) 
			AGENT_SVC[i].setChecked(agents.contains(AgentTypeE.values()[i+1]));
		onCheckedChanged(automate, automate.isChecked());
		onCheckedChanged(offline, offline.isChecked());
		autoNo.setText(_info.getAutomateNumber());
	}
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		autoNo = findViewById(R.id.ed_auto_no);
		autoBox = findViewById(R.id.v_auto);
		LinearLayout ll = findViewById(R.id.v_agent_types);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		lp.bottomMargin = 6;
		AGENT_SVC = new CheckBox[AgentTypeE.values().length-1];
		for(int i=0;i<AGENT_SVC.length;i++) {
			AGENT_SVC[i] = new CheckBox(getContext());
			AGENT_SVC[i].setText(AgentTypeE.values()[i+1].desc);
			ll.addView(AGENT_SVC[i],lp);
		}
		MODES.add((CheckBox)findViewById(R.id.sw_internet));
		MODES.add((CheckBox)findViewById(R.id.sw_excises));
		MODES.add((CheckBox)findViewById(R.id.sw_bso));
		MODES.add((CheckBox)findViewById(R.id.sw_lottery));
		MODES.add((CheckBox)findViewById(R.id.sw_gambling));
		
		automate = findViewById(R.id.sw_auto_printer);
		automate.setOnCheckedChangeListener(this);
		MODES.add(automate);
		
		encryption = findViewById(R.id.sw_encrypt);
		MODES.add(encryption);
		
		MODES.add((CheckBox)findViewById(R.id.sw_marking_goods));
		MODES.add((CheckBox)findViewById(R.id.sw_pawnshop_activity));
		MODES.add((CheckBox)findViewById(R.id.sw_insurance_activity));
		offline = findViewById(R.id.sw_offline);
		offline.setOnCheckedChangeListener(this);
		MODES.add(offline);
	}

	@Override
	public boolean obtain() {
		if(MODES.get(5).isChecked()) {
			if(autoNo.getText().toString().trim().isEmpty()) {
				Toast.makeText(getContext(), "Не указан номер автомата", Toast.LENGTH_LONG).show();
				autoNo.requestFocus();
				return false;
			}
		}
		_info.setInternetMode(MODES.get(0).isChecked());
		_info.setExcisesMode(MODES.get(1).isChecked());
		_info.setBSOMode(MODES.get(2).isChecked());
		_info.setLotteryMode(MODES.get(3).isChecked());
		_info.setGamblingMode(MODES.get(4).isChecked());
		_info.setAutoPrinter(MODES.get(5).isChecked());
		_info.setAutomatedMode(MODES.get(5).isChecked());
		if(_info.isAutoPrinter()) 
			_info.setAutomateNumber(autoNo.getText().toString());
		_info.setEncryptionMode(MODES.get(6).isChecked());
		_info.setMarkingGoods(MODES.get(7).isChecked());
		_info.setPawnShopActivity(MODES.get(8).isChecked());
		_info.setInsuranceActivity(MODES.get(9).isChecked());
		_info.setOfflineMode(offline.isChecked());
		_info.getAgentType().clear();
		for(int i=0;i<AGENT_SVC.length;i++) {
			if(AGENT_SVC[i].isChecked())
				_info.getAgentType().add(AgentTypeE.values()[i+1]);
		}
		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton v, boolean val) {
		switch(v.getId()) {
		case R.id.sw_auto_printer:
			autoBox.setVisibility(val ? View.VISIBLE : View.GONE);
			break;
		case R.id.sw_offline:
			encryption.setEnabled(!val);
			if(val) encryption.setChecked(false);
			break;
		}
		
	}
	@Override
	public View getView() {
		return this;
	}

}

