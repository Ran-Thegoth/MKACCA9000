package rs.mkacca;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.SharedPreferences;
import android.os.RemoteException;
import cs.U;
import cs.orm.ORMHelper;
import cs.ui.fragments.BaseFragment;
import rs.data.AppSettings;
import rs.data.ShiftSells;
import rs.data.User;
import rs.data.WeightBarcodeParser;
import rs.data.goods.Barcode;
import rs.data.goods.Good;
import rs.data.goods.GoodGroup;
import rs.data.goods.Variant;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OfdStatistic;
import rs.mkacca.hw.scaner.BarcodeScaner;
import rs.mkacca.ui.fragments.LoginFragment;
import rs.mkacca.ui.fragments.MainMenu;
import rs.mkacca.ui.fragments.Splash;
import rs.utils.app.AppCore;
import rs.log.Logger;

public class Core extends AppCore {

	public static final long ONE_DAY = 24 * 60 * 60 * 1000L;
	public static final int EVT_INFO_UPDATED = 1000;
	public static final int EVT_REST_CHANGED = 1001;
	private static Core _instance;
	private User _user;
	private BaseFragment _activeFragment;
	private KKMInfo _kkmInfo = new KKMInfo();
	private double _cashRest;
	private DB _db;
	private AppSettings _settings;
	private static boolean _fnPresent;

	public static boolean isFNPresent() {
		return _fnPresent;
	}

	public static void setFNOK() {
		_fnPresent = true;
	}

	public class LastFNInfo {
		public String number = Const.EMPTY_STRING;
		public String owner = Const.EMPTY_STRING;
		public int status;

		private void store() {
			SharedPreferences sp = getSharedPreferences("fn.info", MODE_PRIVATE);
			sp.edit().putString("NUMBER", number).putInt("MODE", status).putString("INN", owner).commit();
		}
		private void load() {
			SharedPreferences sp = getSharedPreferences("fn.info", MODE_PRIVATE);
			number = sp.getString("NUMBER", Const.EMPTY_STRING);
			status = sp.getInt("MODE", 0);
			owner = sp.getString("INN",Const.EMPTY_STRING);
		}
	}

	private LastFNInfo _lastFn = new LastFNInfo();

	public static Core getInstance() {
		return _instance;
	}

	@Override
	public void onCreate() {
		_instance = this;
		_activeFragment = new Splash();
		super.onCreate();
	}

	public BaseFragment getActiveFragment() {
		return _activeFragment;
	}

	public void setActiveFragment(BaseFragment f) {
		_activeFragment = f;
	}

	public boolean init(ProgressNotifier nf) {
		nf.updateProgress(10, "Открытие базы данных");
		db();
		Logger.init(this);
		Logger.enableExceptionHandler(this);
		Logger.i("=================== МКАССА ======================");
		_lastFn.load();
		_settings = ORMHelper.load(AppSettings.class);
		if (_settings == null) {
			_settings = new AppSettings();
			_settings.store();
		}
		if (ORMHelper.load(User.class) == null) {
			nf.updateProgress(20, "Инициализация данных");
			adduser(User.ADMIN_NAME, User.ROLE_ALL);
			adduser(User.CASHIER_NAME, User.ROLE_SELL);
		}
		GoodGroup.load();
		WeightBarcodeParser.load();
		ORMHelper.load(GoodGroup.class, U.pair(DB.ID_FLD, 0));
		ORMHelper.load(Barcode.class, U.pair(DB.ID_FLD, 0));
		ORMHelper.load(Variant.class, U.pair(DB.ID_FLD, 0));
		ORMHelper.load(Good.class, U.pair(DB.ID_FLD, 0));
		ORMHelper.load(ShiftSells.class,U.pair(DB.DB_FILE, 0));
		nf.updateProgress(50, "Подключение к ФН");
		return initialize(5000);
	}

	private void adduser(String name, int role) {
		User u = new User(name);
		u.setRoles(role);
		u.setPIN("0000");
		u.store();
	}

	public DB db() {
		if (_db == null)
			_db = new DB(this);
		return _db;
	}

	public String getLastUserName() {
		return null;
	}

	public void setUser(User u) {
		_user = u;
		if (_user == null)
			_activeFragment = new LoginFragment();
		else
			_activeFragment = new MainMenu();
	}

	public User user() {
		return _user;
	}

	@Override
	public FiscalStorage getStorage() {
		return super.getStorage();
	}

	private OfdStatistic _ofdInfo = new OfdStatistic();
	private AtomicInteger __id = new AtomicInteger(9000);

	public int nextId() {
		return __id.getAndIncrement();
	}

	public KKMInfo kkmInfo() {
		return _kkmInfo;
	}

	public void updateRests() throws RemoteException {
		if (getStorage() != null) {
			_cashRest = getStorage().getCashRest();
			sendMessage(EVT_REST_CHANGED, _kkmInfo);
		}

	}

	public OfdStatistic ofdInfo() {
		return _ofdInfo;
	}

	public void updateLastFNStatus() {
		_lastFn.number = _kkmInfo.getFNNumber();
		_lastFn.status = _kkmInfo.getFNState().bVal;
		_lastFn.owner = _kkmInfo.getOwner().getINN();
		_lastFn.store();
	}
	public int updateInfo() throws RemoteException {
		if (getStorage() == null)
			return Errors.DEVICE_ABSEND;
		try {
			int r = getStorage().readKKMInfo(_kkmInfo);
			if (r == Errors.NO_ERROR) {
				setFNOK();
				getStorage().updateOfdStatistic(_ofdInfo);
				_cashRest = getStorage().getCashRest();
			}
			return r;
		} finally {
			sendMessage(EVT_INFO_UPDATED, _kkmInfo);
		}
	}

	public double cashRest() {
		return _cashRest;
	}

	public AppSettings appSettings() {
		return _settings;
	}

	public BarcodeScaner scaner() {
		return _settings.scaner();
	}

	public OfdStatistic updateOfd() {
		if (getStorage() != null)
			try {
				getStorage().updateOfdStatistic(_ofdInfo);
			} catch (RemoteException re) {
			}
		return _ofdInfo;
	}

}
