package rs.data;

import cs.U;
import cs.orm.DBField;
import cs.orm.DBObject;
import cs.orm.DBTable;
import cs.orm.ORMHelper;
import cs.ui.annotations.BindTo;
import rs.fncore.Const;
import rs.fncore.data.OU;
import rs.mkacca.DB;
import rs.mkacca.R;

@DBTable(name=DB.USER_TABLE,unique={"NAME"},indeces = {})
public class User extends DBObject  {
	public static final String NAME_FLD = "NAME";
	public static final String ADMIN_NAME = "Администратор";
	public static final String CASHIER_NAME = "Кассир";

	public static final int PREBUILD        = 0x0001;
	public static final int MANAGE_DEVICE   = 0x0002;
	public static final int MANAGE_USERS    = 0x0004;
	public static final int MANAGE_GOODS    = 0x0008;
	public static final int MANAGE_DISCOUNT = 0x0010;
	public static final int MANAGE_SHIFT    = 0x0020;
	public static final int MANAGE_CASH     = 0x0040;
	public static final int MANAGE_SELL     = 0x0080;
	public static final int MANAGE_REFUND   = 0x0100;
	public static final int DO_CHECKS = MANAGE_SELL | MANAGE_REFUND;     
	
	
	public static final int ROLE_ALL = 0xFFF;
	public static final int ROLE_SELL = MANAGE_SELL | MANAGE_REFUND;  
	@BindTo(ui=R.id.ed_user_name,required=true)
	@DBField(name=NAME_FLD)
	private String _name;
	@DBField(name="PIN")
	private String _pin = Const.EMPTY_STRING;
	@BindTo(ui=R.id.cb_is_active)
	@DBField(name="ENABLED")
	private boolean _enabled = true;
	@DBField(name="ROLES")
	private int _roles;
	@DBField(name="MAXD")
	@BindTo(ui=R.id.ed_max_discount,format="%.1f",ignore="0,0")
	private double _maxDiscount;
	@DBField(name="START_SCREEN")
	@BindTo(ui=R.id.sp_screen)
	private int _startScreen = 0;
	
	public User() {
		_enabled = true;
	}
	public User(String name) {
		_name = name;
		_enabled = true;
	}
	public String getPIN() { return _pin; }
	public void setPIN(String pin) {
		_pin = pin;
	}
	
	public static User load(String name) {
		try {
			return ORMHelper.load(User.class,U.pair(NAME_FLD, name));
		} catch(Exception e) {
			return null;
		}
	}
	
	public boolean can(int what) {
		return (_roles & what) != 0;
	}
	public int getRoles() { return _roles; }
	public void setRoles(int value) { _roles = value; } 

	@Override
	public String toString() {
		return _name;
	}
	public double getMaxDicsount() { return _maxDiscount; }
	public String pin() {
		return _pin;
	}
	public String name() {
		return _name;
	}
	public int startScreen() {
		return _startScreen;
	}
	public OU toOU() {
		return new OU(_name);
	}
}
