package rs.fncore.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.utils.Utils;

/**
 * Отчет о регистрации (состоянии) ККТ
 *
 * @author amv
 */
public class KKMInfo extends Document {

	public static final String CLASS_NAME = "KKMInfo";
	public static final String CLASS_UUID = "1f1d43e4-ebae-11eb-9a03-0242ac130003";
	public static final String KKT_VERSION = "003";
	public static final String FNS_URL = "www.nalog.ru";

	/**
	 * Битовые флаги состояния ФН
	 */
	public enum FNStateE {
		UNKNOWN(0x0, "Не установлен"), STAGE1(0x1, "Не фискализирован"), STAGE2(0x3, "Фискализирован"),
		STAGE3(0x7, "Постфискальный"), STAGE4(0xF, "Постфискальный");

		public final byte bVal;
		public final String pName;

		private FNStateE(int val, String name) {
			this.bVal = (byte) val;
			this.pName = name;
		}

		public static FNStateE fromByte(byte state) {
			for (FNStateE val : values()) {
				if (val.bVal == state) {
					return val;
				}
			}
			return UNKNOWN;
		}

		/**
		 * Битовые флаги состояния ФН
		 */
		public static final int FN_STATE_READY_BF = 0;
		public static final int FN_STATE_ACTIVE_BF = 1;
		public static final int FN_STATE_ARCHIVED_BF = 2;
		public static final int FN_STATE_NO_MORE_DATA = 3;

		public boolean isActive() {
			return (bVal & (0x01 << FN_STATE_ACTIVE_BF)) != 0;
		}

		public boolean isArchived() {
			return (bVal & (0x01 << FN_STATE_ARCHIVED_BF)) != 0;
		}

		@Override
		public String toString() {
			return pName;
		}
	}

	/**
	 * Режим работы
	 */
	public enum WorkModeE {
		UNKNOWN(0), ENCRYPTION(1 << 0), OFFLINE(1 << 1), AUTO(1 << 2), SERVICE(1 << 3), BSO(1 << 4), INTERNET(1 << 5);

		public final byte bVal;

		private WorkModeE(int val) {
			this.bVal = (byte) val;
		}

		/**
		 * Декодировать битовую маску в список
		 *
		 * @param val байт значение рабочих режимов
		 * @return Set рабочих режимов
		 */
		public static Set<WorkModeE> fromByteArray(byte val) {
			HashSet<WorkModeE> result = new HashSet<>();
			for (WorkModeE a : values()) {
				if (a == UNKNOWN)
					continue;
				if ((val & a.bVal) == a.bVal)
					result.add(a);
			}
			return result;
		}

		/**
		 * Закодировать список в битовую маску
		 *
		 * @param val набор рабочих режимов
		 * @return байт значение рабочих режимов
		 */
		public static byte toByteArray(Iterable<WorkModeE> val) {
			byte result = 0;
			for (WorkModeE a : val)
				result |= a.bVal;
			return result;
		}
	}

	/**
	 * Расширенный режим работы, для ФФД 1.2
	 */
	public enum WorkModeExE {
		UNKNOWN(0), EXCISABLE_GOODS(1 << 0), GAMBLING_GAMES(1 << 1), LOTTERY(1 << 2), AUTO_PRINTER(1 << 3),
		MARKING_GOODS(1 << 4), PAWNSHOP_ACTIVITY(1 << 5), INSURANCE_ACTIVITY(1 << 6);

		public final byte bVal;

		private WorkModeExE(int val) {
			this.bVal = (byte) val;
		}

		/**
		 * Декодировать битовую маску в список
		 *
		 * @param val byte array
		 * @return set
		 */
		public static Set<WorkModeExE> fromByteArray(byte val) {
			HashSet<WorkModeExE> result = new HashSet<>();
			for (WorkModeExE a : values()) {
				if (a == UNKNOWN)
					continue;
				if ((val & a.bVal) == a.bVal)
					result.add(a);
			}
			return result;
		}

		/**
		 * Закодировать список в битовую маску
		 *
		 * @param val набор рабочих режимов
		 * @return байт значение рабочих режимов
		 */
		public static byte toByteArray(Iterable<WorkModeExE> val) {
			byte result = 0;
			for (WorkModeExE a : val)
				result |= a.bVal;
			return result;
		}
	}

	/**
	 * Статус предупреждений ФН
	 */

	/**
	 * Причина регистрации
	 */
	public static enum FiscalReasonE implements Parcelable {
		UNKNOWN(-1, "неизвестно", "неизвестно"),
		/**
		 * Регистрация ФН
		 */
		REGISTER(0, "ОТЧЕТ О РЕГ.", "Регистрация ККТ"),
		/**
		 * Замена ФН
		 */
		REPLACE_FN(1, "ИЗМ. СВЕД. О ККТ", "Замена ФН"),
		/**
		 * Замена ОФД
		 */
		CHANGE_OFD(2, "ИЗМ. СВЕД. О ККТ", "Замена ОФД"),
		/**
		 * Изменение реквизитов
		 */
		CHANGE_SETTINGS(3, "ИЗМ. СВЕД. О ККТ", "Изменение реквизитов пользователя"),
		/**
		 * Изменение настроек ККТ
		 */
		CHANGE_KKT_SETTINGS(4, "ИЗМ. СВЕД. О ККТ", "Изменение настроек ККТ"),
		/**
		 * Изменение ИНН
		 */
		CHANGE_INN(5, "ИЗМ. СВЕД. О ККТ", "Изменение ИНН"),

		/**
		 * Другие изменения
		 */
		CHANGE_OTHERS(6, "ИЗМ. СВЕД. О ККТ", "Другие изменения")

		;

		public final byte bVal;
		public final String pName;
		public final String desc;

		private FiscalReasonE(int val, String pName, String desc) {
			this.bVal = (byte) val;
			this.pName = pName;
			this.desc = desc;
		}

		@Override
		public String toString() {
			return desc;
		}

		public static FiscalReasonE fromByte(byte reason) {
			for (FiscalReasonE val : values()) {
				if (val.bVal == reason) {
					return val;
				}
			}
			return UNKNOWN;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel p, int flags) {
			p.writeByte(bVal);
		}

		public static final Parcelable.Creator<FiscalReasonE> CREATOR = new Parcelable.Creator<FiscalReasonE>() {
			@Override
			public FiscalReasonE createFromParcel(Parcel p) {
				return fromByte(p.readByte());
			}

			@Override
			public FiscalReasonE[] newArray(int size) {
				return new FiscalReasonE[size];
			}
		};
	}

	/**
	 * Версия ФФД
	 */
	public enum FFDVersionE {
		UNKNOWN(0, "-"), VER_10(1, "1.0"), VER_105(2, "1.05"), VER_11(3, "1.1"), VER_12(4, "1.2"),;

		FFDVersionE(int bVal, String name) {
			this.bVal = (byte) bVal;
			this.name = name;
		}

		public final String name;
		public final byte bVal;

		public static FFDVersionE fromByte(byte bVal) {
			for (FFDVersionE v : values())
				if (v.bVal == bVal)
					return v;
			return UNKNOWN;
		}

		public boolean is12_OrMore() {
			return bVal >= VER_12.bVal;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	/**
	 * Тип физического подключения ФН
	 *
	 * @author amv
	 */
	public static enum FNConnectionModeE implements Parcelable {
		UNKNOWN, USB, UART, VIRTUAL, CLOUD;

		public static final Creator<FNConnectionModeE> CREATOR = new Creator<FNConnectionModeE>() {
			@Override
			public FNConnectionModeE createFromParcel(Parcel in) {
				return FNConnectionModeE.fromInt(in.readInt());
			}

			@Override
			public FNConnectionModeE[] newArray(int size) {
				return new FNConnectionModeE[size];
			}
		};

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel p, int flags) {
			p.writeInt(toInt());
		}

		public int toInt() {
			return this.ordinal();
		}

		public static FNConnectionModeE fromInt(int value) {
			return values()[value];
		}
	}

	/**
	 * Информация о последнем проведенном документе
	 *
	 * @author nick
	 */
	public static class LastDocumentInfo {
		private long mNumber;
		private final Calendar mDate = Calendar.getInstance();

		private LastDocumentInfo() {
			mDate.setTimeInMillis(0);
		}

		/**
		 * @return Номер документа
		 */
		public long getNumber() {
			return mNumber;
		}

		/**
		 * @return Дата документа в миллисекундах
		 */
		public long getTimeInMillis() {
			return mDate.getTimeInMillis();
		}
	}

	public static final String KKM_NUMBER_TAG = "KKMNo";
	public static final String OWNER_TAG = "Owner";
	public static final String OFD_TAG = "OFD";
	public static final String LOCATION_TAG = "Location";
	public static final String MODES_TAG = "Mode";
	public static final String AUT_NO_TAG = "AutomateNumber";
	public static final String MODES_EX_TAG = "ModeEx";
	public static final String TAX_MODES_TAG = "TaxModes";
	public static final String AGENT_MODES_TAG = "AgentModes";
	public static final String SUPP_FFD_VER_TAG = "SuppFFDVer";
	public static final String FNS_URL_TAG = "FnsUrl";
	public static final String SENDER_MAIL_TAG = "SenderMail";

	protected String mKkmNumber = "";
	protected String mFnNumber = "";
	protected FNStateE mFnState = FNStateE.UNKNOWN;
	protected int mUnfinishedDocType;
	protected Shift mShift = new Shift();
	protected String mServiceVersion = Const.EMPTY_STRING;
	protected LastDocumentInfo mLastDocument = new LastDocumentInfo();
	protected OU mOwner = new OU();
	protected OU mOfd = new OU();
	protected FFDVersionE mSupppFFDVer = FFDVersionE.UNKNOWN;

	protected Set<TaxModeE> mTaxModes = new HashSet<>();
	protected Set<AgentTypeE> mAgentTypes = new HashSet<>();
	protected Set<WorkModeE> mWorkModes = new HashSet<>();
	protected Set<WorkModeExE> mWorkModesEx = new HashSet<>();

	protected FiscalReasonE mRegistrationReason = FiscalReasonE.UNKNOWN;
	protected long mReRegistrationReason = 0;
	protected FNConnectionModeE mConnectionMode = FNConnectionModeE.UNKNOWN;
	protected long mFnRemainedDays = 0;
	protected FNCounters mTotalCounters;

	public KKMInfo() {
		super();
		add(FZ54Tag.T1193_GAMBLING_FLAG, false);
		add(FZ54Tag.T1126_LOTTERY_FLAG, false);
		add(FZ54Tag.T1221_AUTOMAT_FLAG, false);
		add(FZ54Tag.T1207_EXCISE_GOODS_FLAG, false);
		add(FZ54Tag.T1013_KKT_SERIAL_NO, Const.EMPTY_STRING);
		add(FZ54Tag.T1209_FFD_VERSION, FFDVersionE.UNKNOWN.bVal);
		add(FZ54Tag.T1189_KKT_FFD_VERSION, FFDVersionE.UNKNOWN.bVal);
		add(FZ54Tag.T1188_KKT_VERSION, KKT_VERSION);
		add(FZ54Tag.T1117_SENDER_EMAIL, Const.EMPTY_STRING);
		add(FZ54Tag.T1057_AGENT_FLAG, (byte) 0);
		add(FZ54Tag.T1060_FNS_URL, FNS_URL);
	}

	public KKMInfo(JSONObject json) throws JSONException {
		this();
		fromJSON(json);
	}

	public void fromJSON(JSONObject json) throws JSONException {
		mKkmNumber = json.getString(KKM_NUMBER_TAG);
		if (json.has(OWNER_TAG))
			mOwner = new OU(json.getJSONObject(OWNER_TAG));
		if (json.has(OFD_TAG))
			mOfd = new OU(json.getJSONObject(OFD_TAG));
		if (json.has(LOCATION_TAG))
			mLocation = new Location(json.getJSONObject(LOCATION_TAG));
		mWorkModes = WorkModeE.fromByteArray((byte) json.getInt(MODES_TAG));
		if(isAutomatedMode() && json.has(AUT_NO_TAG))
			setAutomateNumber(json.getString(AUT_NO_TAG));
			
		mWorkModesEx = WorkModeExE.fromByteArray((byte) json.getInt(MODES_EX_TAG));
		mAgentTypes = AgentTypeE.fromByteArray((byte) json.getInt(AGENT_MODES_TAG));
		mTaxModes = TaxModeE.fromByteArray((byte) json.getInt(TAX_MODES_TAG));
		mSupppFFDVer = FFDVersionE.fromByte((byte) json.getInt(SUPP_FFD_VER_TAG));
		setFNSUrl(json.getString(FNS_URL_TAG));
		setSenderEmail(json.getString(SENDER_MAIL_TAG));
	}

	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject result = new JSONObject(); // super.toJSON();
		result.put(KKM_NUMBER_TAG, mKkmNumber);
		result.put(OWNER_TAG, mOwner.toJSON());
		result.put(OFD_TAG, mOfd.toJSON());
		result.put(LOCATION_TAG,mLocation.toJSON());
		result.put(MODES_TAG, WorkModeE.toByteArray(mWorkModes));
		if(isAutomatedMode())
			result.put(AUT_NO_TAG,getAutomateNumber());
			
		result.put(MODES_EX_TAG, WorkModeExE.toByteArray(mWorkModesEx));
		result.put(AGENT_MODES_TAG, AgentTypeE.toByteArray(mAgentTypes));
		result.put(TAX_MODES_TAG, TaxModeE.toByteArray(mTaxModes));
		result.put(SUPP_FFD_VER_TAG, mSupppFFDVer.bVal);
		result.put(FNS_URL_TAG, getFNSUrl());
		result.put(SENDER_MAIL_TAG, getSenderEmail());
		return result;
	}

	/**
	 * Получить заводской номер ККТ
	 *
	 * @return заводской номер ККТ
	 */
	public String getKKMSerial() {
		if(hasTag(FZ54Tag.T1013_KKT_SERIAL_NO))
			return getTag(FZ54Tag.T1013_KKT_SERIAL_NO).asString();
		return Const.EMPTY_STRING;
	}

	/**
	 * Получить регистрационный номер ККТ
	 *
	 * @return регистрационный номер ККТ
	 */
	public String getKKMNumber() {
		return mKkmNumber;
	}

	/**
	 * Установить регистрационный номер ККТ
	 *
	 * @param v регистрационный номер ККТ
	 */
	public void setKKMNumber(String v) {
		mKkmNumber = v;
	}

	/**
	 * Получить номер фискального накопителя
	 *
	 * @return номер фискального накопителя
	 */
	public String getFNNumber() {
		return mFnNumber;
	}
	public FNStateE getFNState() {
		return mFnState;
	}

	/**
	 * Получить текущую или последнюю открытую смену
	 *
	 * @return текущая или последняя открытая смену
	 */
	public Shift getShift() {
		return mShift;
	}

	/**
	 * Владелец ККТ
	 *
	 * @return Владелец ККТ
	 */
	public OU getOwner() {
		return mOwner;
	}

	/**
	 * Получить максимальную поддерживаемую версию протокола ФФД
	 *
	 * @return максимальная поддерживаемая версия протокола ФФД
	 */
	public FFDVersionE getMaxSuppFFDVersion() {
		return mSupppFFDVer;
	}

	/**
	 * Получить текущую версию протокола ФФД
	 *
	 * @return текущая версия протокола ФФД
	 */
	public FFDVersionE getFFDProtocolVersion() {
		byte bVal;
		Tag data = getTag(FZ54Tag.T1209_FFD_VERSION);
		if (data == null)
			data = getTag(FZ54Tag.T1189_KKT_FFD_VERSION);

		if (data != null)
			bVal = data.asByte();
		else
			bVal = FFDVersionE.UNKNOWN.bVal;
		return FFDVersionE.fromByte(bVal);
	}

	/**
	 * Установить версию протокола ФФД
	 *
	 * @param ver версия протокола ФФД
	 */
	public void setFFDProtocolVersion(FFDVersionE ver) {
		add(FZ54Tag.T1209_FFD_VERSION, ver.bVal);
		add(FZ54Tag.T1189_KKT_FFD_VERSION, ver.bVal);
	}

	public FNConnectionModeE getConnectionMode() {
		return mConnectionMode;
	}

	public void setConnectionMode(FNConnectionModeE mode) {
		mConnectionMode = mode;
	}

	/**
	 * ФН подключен к ККТ
	 *
	 * @return ФН подключен к ККТ
	 */
	public boolean isFNPresent() {
		return mFnState != FNStateE.UNKNOWN;
	}
	
	public boolean isDemoMode() {
		return mFnNumber != null && mFnNumber.equals("9999999999999999");
	}

	/**
	 * ФН находится в фискальном режиме
	 *
	 * @return ФН находится в фискальном режиме
	 */
	public boolean isFNActive() {
		return mFnState.isActive();
	}

	/**
	 * ФН находится в постфискальном режиме
	 *
	 * @return ФН находится в постфискальном режиме
	 */
	public boolean isFNArchived() {
		return mFnState.isArchived();
	}

	/**
	 * Получить признак "автономная работа"
	 *
	 * @return признак "автономная работа"
	 */
	public boolean isOfflineMode() {
		return mWorkModes.contains(WorkModeE.OFFLINE);
	}

	/**
	 * Установить признак "автономная работа"
	 * Больше не поддерживается
	 * @param val признак "автономная работа"
	 */
	
	public void setOfflineMode(boolean val) {
		if(val) {
			mWorkModes.add(WorkModeE.OFFLINE);
			mWorkModes.remove(WorkModeE.ENCRYPTION);
		} else  {
			mWorkModes.remove(WorkModeE.OFFLINE);
			
		}
			
	}

	/**
	 * Получить признак "Продажа подакцизного товара"
	 *
	 * @return признак "Продажа подакцизного товара"
	 */
	public boolean isExcisesMode() {
		return mWorkModesEx.contains(WorkModeExE.EXCISABLE_GOODS);
	}

	/**
	 * Установить признак "Продажа подакцизного товара"
	 *
	 * @param val признак "Продажа подакцизного товара"
	 */
	public void setExcisesMode(boolean val) {
		if (val) {
			mWorkModesEx.add(WorkModeExE.EXCISABLE_GOODS);
		} else {
			mWorkModesEx.remove(WorkModeExE.EXCISABLE_GOODS);
		}

		add(FZ54Tag.T1207_EXCISE_GOODS_FLAG, val);
	}

	/**
	 * Получить признак "Проведение лотереи"
	 *
	 * @return признак "Проведение лотереи"
	 */
	public boolean isLotteryMode() {
		return mWorkModesEx.contains(WorkModeExE.LOTTERY);
	}

	/**
	 * Установить признак "Проведение лотереи"
	 *
	 * @param val признак "Проведение лотереи"
	 */
	public void setLotteryMode(boolean val) {
		if (val) {
			mWorkModesEx.add(WorkModeExE.LOTTERY);
		} else {
			mWorkModesEx.remove(WorkModeExE.LOTTERY);
		}
		add(FZ54Tag.T1126_LOTTERY_FLAG, val);
	}

	/**
	 * Получить признак "Установка принтера в автомате"
	 *
	 * @return признак "Установка принтера в автомате"
	 */
	public boolean isAutoPrinter() {
		return mWorkModesEx.contains(WorkModeExE.AUTO_PRINTER);
	}

	/**
	 * Установить признак "Установка принтера в автомате"
	 *
	 * @param val признак "Установка принтера в автомате"
	 */
	public void setAutoPrinter(boolean val) {
		if (val) {
			mWorkModesEx.add(WorkModeExE.AUTO_PRINTER);
		} else {
			mWorkModesEx.remove(WorkModeExE.AUTO_PRINTER);
		}
		add(FZ54Tag.T1221_AUTOMAT_FLAG, val);
	}

	/**
	 * Получить признак "Работы с маркированными товарами"
	 *
	 * @return признак "Работы с маркированными товарами"
	 */
	public boolean isMarkingGoods() {
		return mWorkModesEx.contains(WorkModeExE.MARKING_GOODS);
	}

	/**
	 * Установить признак "Работы с маркированными товарами"
	 *
	 * @param val признак "Работы с маркированными товарами"
	 */
	public void setMarkingGoods(boolean val) {
		if (val) {
			mWorkModesEx.add(WorkModeExE.MARKING_GOODS);
		} else {
			mWorkModesEx.remove(WorkModeExE.MARKING_GOODS);
		}
	}

	/**
	 * Получить признак "Осуществления ломбардной деятельности"
	 *
	 * @return признак "Осуществления ломбардной деятельности"
	 */
	public boolean isPawnShopActivity() {
		return mWorkModesEx.contains(WorkModeExE.PAWNSHOP_ACTIVITY);
	}

	/**
	 * Установить признак "Осуществления ломбардной деятельности"
	 *
	 * @param val признак "Осуществления ломбардной деятельности"
	 */
	public void setPawnShopActivity(boolean val) {
		if (val) {
			mWorkModesEx.add(WorkModeExE.PAWNSHOP_ACTIVITY);
		} else {
			mWorkModesEx.remove(WorkModeExE.PAWNSHOP_ACTIVITY);
		}
	}

	/**
	 * Получить признак "Осуществления страховой деятельности"
	 *
	 * @return признак "Осуществления страховой деятельности"
	 */
	public boolean isInsuranceActivity() {
		return mWorkModesEx.contains(WorkModeExE.INSURANCE_ACTIVITY);
	}

	/**
	 * Установить признак "Осуществления страховой деятельности"
	 *
	 * @param val признак "Осуществления страховой деятельности"
	 */
	public void setInsuranceActivity(boolean val) {
		if (val) {
			mWorkModesEx.add(WorkModeExE.INSURANCE_ACTIVITY);
		} else {
			mWorkModesEx.remove(WorkModeExE.INSURANCE_ACTIVITY);
		}
	}

	/**
	 * Получить признак "Проведение азартных игр"
	 *
	 * @return признак "Проведение азартных игр"
	 */
	public boolean isGamblingMode() {
		return mWorkModesEx.contains(WorkModeExE.GAMBLING_GAMES);
	}

	/**
	 * Установить признак "Проведение азартных игр"
	 *
	 * @param val признак "Проведение азартных игр"
	 */
	public void setGamblingMode(boolean val) {
		if (val) {
			mWorkModesEx.add(WorkModeExE.GAMBLING_GAMES);
		} else {
			mWorkModesEx.remove(WorkModeExE.GAMBLING_GAMES);
		}
		add(FZ54Tag.T1193_GAMBLING_FLAG, val);
	}

	/**
	 * @return установленные режимы агента
	 */
	public Set<AgentTypeE> getAgentType() {
		return mAgentTypes;
	}

	/**
	 * @return установлен ли режим агента
	 */
	public boolean isAgent() {
		return mAgentTypes.size() > 0;
	}

	/**
	 * @return Находится ли ККТ в режиме автомата
	 */
	public boolean isAutomatedMode() {
		return mWorkModes.contains(WorkModeE.AUTO);
	}

	public void setAutomatedMode(boolean val) {
		if (val) {
			mWorkModes.add(WorkModeE.AUTO);
			add(FZ54Tag.T1036_AUTOMAT_NO, String.format("% 20d", 1));
		} else {
			mWorkModes.remove(WorkModeE.AUTO);
			remove(FZ54Tag.T1036_AUTOMAT_NO);
		}
	}

	/**
	 * @return номер автомата
	 */
	public String getAutomateNumber() {
		if (isAutomatedMode()) {
			return getTagString(FZ54Tag.T1036_AUTOMAT_NO);
		}
		return Const.EMPTY_STRING;
	}

	/**
	 * @param v номер автомата
	 */
	public void setAutomateNumber(String v) throws NumberFormatException {
		if (!isAutomatedMode())
			return;
		Integer.parseInt(v.trim());

		StringBuilder vBuilder = new StringBuilder(v);
		while (vBuilder.length() < 10)
			vBuilder.insert(0, " ");
		v = vBuilder.toString();
		add(FZ54Tag.T1036_AUTOMAT_NO, v);
	}

	/**
	 * @return режим налоообложения
	 */
	public Set<TaxModeE> getTaxModes() {
		return mTaxModes;
	}

	/**
	 * Получить e-mail отправителя
	 *
	 * @return e-mail отправителя
	 */
	public String getSenderEmail() {
		return getTagString(FZ54Tag.T1117_SENDER_EMAIL);
	}

	/**
	 * Установить e-mail отправителя
	 *
	 * @param value e-mail отправителя
	 */
	public void setSenderEmail(String value) {
		add(FZ54Tag.T1117_SENDER_EMAIL, value);
	}

	/**
	 * Получить признак "ККТ для Интернет"
	 *
	 * @return признак "ККТ для Интернет"
	 */
	public boolean isInternetMode() {
		return mWorkModes.contains(WorkModeE.INTERNET);
	}

	/**
	 * Установить признак "ККТ для Интернет"
	 *
	 * @param val признак "ККТ для Интернет"
	 */
	public void setInternetMode(boolean val) {
		if (val) {
			mWorkModes.add(WorkModeE.INTERNET);
		} else {
			mWorkModes.remove(WorkModeE.INTERNET);
		}
	}

	/**
	 * Получить признак "режим шифрования"
	 *
	 * @return признак "режим шифрования"
	 */
	public boolean isEncryptionMode() {
		return mWorkModes.contains(WorkModeE.ENCRYPTION);
	}

	/**
	 * Установить признак "режим шифрования"
	 *
	 * @param val признак "режим шифрования"
	 */

	public void setEncryptionMode(boolean val) {
		if (val) {
			mWorkModes.add(WorkModeE.ENCRYPTION);
		} else {
			mWorkModes.remove(WorkModeE.ENCRYPTION);
		}
	}

	/**
	 * Получить признак "Оказание услуг"
	 *
	 * @return признак "Оказание услуг"
	 */
	public boolean isServiceMode() {
		return mWorkModes.contains(WorkModeE.SERVICE);
	}

	/**
	 * Установить признак "Оказание услуг"
	 *
	 * @param val признак "Оказание услуг"
	 */
	public void setServiceMode(boolean val) {
		if (val) {
			mWorkModes.add(WorkModeE.SERVICE);
		} else {
			mWorkModes.remove(WorkModeE.SERVICE);
		}
	}

	/**
	 * Получить признак "Использование БСО"
	 *
	 * @return признак "Использование БСО"
	 */
	public boolean isBSOMode() {
		return mWorkModes.contains(WorkModeE.BSO);
	}

	/**
	 * Установить признак "Использование БСО"
	 *
	 * @param val признак "Использование БСО"
	 */
	public void setBSOMode(boolean val) {
		if (val) {
			mWorkModes.add(WorkModeE.BSO);
		} else {
			mWorkModes.remove(WorkModeE.BSO);
		}
	}

	/**
	 * Получить адрес сайта ФНС
	 *
	 * @return адрес сайта ФНС
	 */
	public String getFNSUrl() {
		return getTagString(FZ54Tag.T1060_FNS_URL);
	}

	/**
	 * Установить адрес сайта ФНС
	 *
	 * @param value адрес сайта ФНС
	 */
	public void setFNSUrl(String value) {
		add(FZ54Tag.T1060_FNS_URL, value);
	}

	/**
	 * Данные ОФД
	 *
	 * @return Данные ОФД
	 */
	public OU ofd() {
		return mOfd;
	}

	/**
	 * Получить причину изменений параметров ККТ/регистрации
	 *
	 * @return причина изменений параметров ККТ/регистрации
	 */
	public FiscalReasonE getRegistrationReason() {
		return mRegistrationReason;
	}

	/**
	 * Получить номер последнего документа
	 *
	 * @return номер последнего документа
	 */
	public long getLastFNDocNumber() {
		return mLastDocument.mNumber;
	}

	@Override
	public void writeToParcel(Parcel p, int flags) {
		super.writeToParcel(p, flags);
		p.writeString(mFnNumber);
		p.writeString(mKkmNumber);
		p.writeString(mServiceVersion);
		p.writeByte(mFnState.bVal);
		p.writeByte(mSupppFFDVer.bVal);
		p.writeInt(mUnfinishedDocType);
		p.writeLong(mLastDocument.mNumber);
		p.writeLong(mLastDocument.getTimeInMillis());
		p.writeInt(mFnWarnings.iVal);
		p.writeInt(mConnectionMode.ordinal());
		p.writeByte(TaxModeE.toByteArray(mTaxModes));
		p.writeByte(AgentTypeE.toByteArray(mAgentTypes));
		p.writeByte(WorkModeE.toByteArray(mWorkModes));
		p.writeByte(WorkModeExE.toByteArray(mWorkModesEx));
		p.writeByte(mRegistrationReason.bVal);
		p.writeLong(mReRegistrationReason);
		mOwner.writeToParcel(p, flags);
		mOfd.writeToParcel(p, flags);
		mShift.writeToParcel(p, 0);
		mSignature.operator().writeToParcel(p, flags);
		if (mTotalCounters == null)
			p.writeInt(0);
		else {
			p.writeInt(1);
			mTotalCounters.writeToParcel(p, flags);
		}
	}

	@Override
	public void readFromParcel(Parcel p) {
		super.readFromParcel(p);
		mFnNumber = p.readString();
		mKkmNumber = p.readString();
		mServiceVersion = p.readString();
		mFnState = FNStateE.fromByte(p.readByte());
		mSupppFFDVer = FFDVersionE.fromByte(p.readByte());
		mUnfinishedDocType = p.readInt();
		mLastDocument.mNumber = p.readLong();
		mLastDocument.mDate.setTimeInMillis(p.readLong());
		mFnWarnings = new FnWarnings(p.readInt());
		mConnectionMode = FNConnectionModeE.values()[p.readInt()];
		mTaxModes = TaxModeE.fromByteArray(p.readByte());
		mAgentTypes = AgentTypeE.fromByteArray(p.readByte());
		mWorkModes = WorkModeE.fromByteArray(p.readByte());
		mWorkModesEx = WorkModeExE.fromByteArray(p.readByte());
		mRegistrationReason = FiscalReasonE.fromByte(p.readByte());
		mReRegistrationReason = p.readLong();
		mOwner.readFromParcel(p);
		mOfd.readFromParcel(p);
		mShift.readFromParcel(p);
		mSignature.operator().readFromParcel(p);
		if (p.readInt() == 1) {
			mTotalCounters = new FNCounters(false);
			mTotalCounters.readFromParcel(p);
		} else
			mTotalCounters = null;
	}

	protected void updateLastDocumentInfo(long date, long number) {
		if (date != -1)
			mLastDocument.mDate.setTimeInMillis(date);
		mLastDocument.mNumber = number;
	}

	/**
	 * Получить информацию о последнем проведенном документе
	 *
	 * @return информация о последнем проведенном документе
	 */
	public LastDocumentInfo getLastDocument() {
		return mLastDocument;
	}

	/**
	 * Получить версию фискального сервиса
	 *
	 * @return версия фискального сервиса
	 */
	public String getServiceVersion() {
		return mServiceVersion;
	}

	protected void updateLastDocumentInfo(Document doc) {
		mLastDocument.mNumber = doc.signature().getFdNumber();
		mLastDocument.mDate.setTimeInMillis(doc.signature().signDate());
	}

	public static final Parcelable.Creator<KKMInfo> CREATOR = new Parcelable.Creator<KKMInfo>() {
		@Override
		public KKMInfo createFromParcel(Parcel p) {
			KKMInfo result = new KKMInfo();
			result.readFromParcel(p);
			return result;
		}

		@Override
		public KKMInfo[] newArray(int size) {
			return new KKMInfo[size];
		}

	};

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	public String getClassUUID() {
		return CLASS_UUID;
	}

	public void resetState() {
		mFnState = FNStateE.UNKNOWN;
	}

	@Override
	public KKMInfo attach(Object o) {
		return (KKMInfo) super.attach(o);
	}

	@Override
	public byte[][] packToFN() {
		byte b = AgentTypeE.toByteArray(mAgentTypes);
		if(b != 0)
			add(FZ54Tag.T1057_AGENT_FLAG,b);
		else 
			remove(FZ54Tag.T1057_AGENT_FLAG);
		return super.packToFN();
	}
	@Override
	protected boolean parseTag(Tag t) {
		switch (t.getId()) {
		case FZ54Tag.T1037_KKT_REG_NO:
			mKkmNumber = t.asString().trim();
			return super.parseTag(t);
		case FZ54Tag.T1001_AUTOMATIC_MODE:
		case FZ54Tag.T1221_AUTOMAT_FLAG:
			setAutomatedMode(t.asBoolean());
			break;
		case FZ54Tag.T1207_EXCISE_GOODS_FLAG:
			setExcisesMode(t.asBoolean());
			break;
		case FZ54Tag.T1193_GAMBLING_FLAG:
			setGamblingMode(t.asBoolean());
			break;
		case FZ54Tag.T1108_INTERNET_SALE_FLAG:
			setInternetMode(t.asBoolean());
			break;
		case FZ54Tag.T1109_SERVICES_FLAG:
			setServiceMode(t.asBoolean());
			break;
		case FZ54Tag.T1290_KKT_CONDITION_USAGE: {
			long val = t.asUInt();
			setAutoPrinter((val & (1 << 1)) != 0); // 1
			setBSOMode((val & (1 << 2)) != 0); // 2
			setInternetMode((val & (1 << 5)) != 0);
			setExcisesMode((val & (1 << 6)) != 0);
			setMarkingGoods((val & (1 << 8)) != 0);
			setServiceMode((val & (1 << 9)) != 0);
			setGamblingMode((val & (1 << 10)) != 0);
			setLotteryMode((val & (1 << 11)) != 0);
			setPawnShopActivity((val & (1 << 12)) != 0);
			setInsuranceActivity((val & (1 << 13)) != 0);
		}
			break;
		case FZ54Tag.T1213_FN_KEYS_EXPIRE_DAYS:
			mFnRemainedDays = t.asUInt();
			break;
		case FZ54Tag.T1002_AUTONOMOUS_MODE:
			setOfflineMode(t.asBoolean());
			break;
		case FZ54Tag.T1013_KKT_SERIAL_NO:
			super.parseTag(t);
			return false;
		case FZ54Tag.T1062_TAX_SYSTEMS:
			mTaxModes.addAll(TaxModeE.fromByteArray(t.asByte()));
			break;
		case FZ54Tag.T1057_AGENT_FLAG:
			mAgentTypes.addAll(AgentTypeE.fromByteArray(t.asByte()));
			break;
		case FZ54Tag.T1060_FNS_URL:
			setFNSUrl(t.asString());
			break;
		case FZ54Tag.T1036_AUTOMAT_NO:
			setAutomateNumber(t.asString());
			break;
		case FZ54Tag.T1056_ENCRYPTION_FLAG:
			setEncryptionMode(t.asBoolean());
			break;
		case FZ54Tag.T1110_STRICT_FORM_ONLY_FLAG:
			setBSOMode(t.asBoolean());
			break;
		case FZ54Tag.T1126_LOTTERY_FLAG:
			setLotteryMode(t.asBoolean());
			break;
		case FZ54Tag.T1222_AGENT_FLAGS:
			break;
		case FZ54Tag.T1018_OWNER_INN:
			mOwner.setINN(t.asString());
			return super.parseTag(t);
		case FZ54Tag.T1048_OWNER_NAME:
			mOwner.setName(t.asString());
			return super.parseTag(t);
		case FZ54Tag.T1017_OFD_INN:
			mOfd.setINN(t.asString());
			break;
		case FZ54Tag.T1189_KKT_FFD_VERSION:
			mSupppFFDVer = FFDVersionE.fromByte(t.asByte());
			return super.parseTag(t);
		case FZ54Tag.T1009_TRANSACTION_ADDR:
			mLocation.setAddress(t.asString());
			return super.parseTag(t);
		case FZ54Tag.T1187_TRANSACTION_PLACE:
			mLocation.setPlace(t.asString());
			return super.parseTag(t);
		case FZ54Tag.T1117_SENDER_EMAIL:
			setSenderEmail(t.asString());
			break;
		case FZ54Tag.T1205_MODIFY_REG_FLAGS:
			mReRegistrationReason = t.asUInt();
			break;
		case FZ54Tag.T1046_OFD_NAME:
			mOfd.setName(t.asString());
			break;
		case FZ54Tag.T1209_FFD_VERSION:
			super.parseTag(t);
			return false; // Что бы добавил в теги
		case FZ54Tag.T1157_FN_TOTALS_TLV:
			mTotalCounters = new FNCounters(true);
			mTotalCounters.parseTag(t);
			break;
		default:
			return super.parseTag(t);
		}
		return true;

	}

	public FNCounters getTotalCounters() {
		return mTotalCounters;
	}

	public long getFnRemainedDays() {
		return mFnRemainedDays;
	}

	private static final String REG_TYPE = "reason.Type";
	private static final String REG_NAME = "reason.Name";
	private static final String BSO = "bso";

	private static final String ENCRYPTION = "encryption";
	private static final String IS_INTERNET_MODE = "isInternetMode";
	private static final String IS_SERVICE_MODE = "isServiceMode";
	private static final String IS_EXCSISES = "isExcisesMode";
	private static final String IS_CASINO_MODE = "isCasinoMode";
	private static final String IS_LOTTERY_MODE = "isLotteryMode";
	private static final String IS_MARKING = "isMarking";
	private static final String IS_PAWNSHOP = "isPawnShop";
	private static final String IS_INSURANCE = "isInsurance";

	private static final String AGENT_NAMES = "AgentType";
	private static final String OFFLINE = "offline";
	private static final String IS_AUTOMATION = "automation";

	private static final String OFD_INN = "ofd.INN";
	private static final String OFD_NAME = "ofd.Name";

	private static final String KEYS_DAYS_REMAINING = "Keys.Days.Remaning";
	private static final String REGISTRATION_REASON = "Registation.Reason";
	private static final String TOTAL_COUNTERS = "counters.total";
	private static final String TOTAL_COUNTERS_HAS = "is.counters.total";

	@Override
	public String onKey(String key) {

		switch (key) {
		case TOTAL_COUNTERS_HAS:
			return bv(mTotalCounters != null);
		case TOTAL_COUNTERS:
			if (mTotalCounters == null)
				return Const.EMPTY_STRING;
			return Utils.printCounters(mTotalCounters);
		case BSO:
			return bv(isBSOMode());
		case OFD_INN:
			return ofd().getINNtrimZ();
		case OFD_NAME:
			return ofd().getName();
		case REG_TYPE:
			if (getFFDProtocolVersion() == FFDVersionE.VER_12)
				return mReRegistrationReason == 0 ? "Отчет о рег." : "ИЗМ. СВЕД. О ККТ:";
			else
				return getRegistrationReason().desc;
		case REG_NAME:
			return KKMInfoEx2FiscalReason.FiscalReasonExtE.fromLongArrayDescr(mReRegistrationReason);
		case REGISTRATION_REASON:
			return KKMInfoEx2FiscalReason.FiscalReasonExtE.fromLongArrayStr(mReRegistrationReason);
		case KEYS_DAYS_REMAINING:
			return String.valueOf(mFnRemainedDays);

		case OFFLINE:
			return bv(isOfflineMode());
		case ENCRYPTION:
			return bv(isEncryptionMode());
		case IS_AUTOMATION:
			return bv(isAutomatedMode());
		case IS_INTERNET_MODE:
			return bv(isInternetMode());
		case IS_SERVICE_MODE:
			return bv(isServiceMode());
		case IS_EXCSISES:
			return bv(isExcisesMode());
		case IS_CASINO_MODE:
			return bv(isGamblingMode());
		case IS_LOTTERY_MODE:
			return bv(isLotteryMode());
		case IS_MARKING:
			return bv(isMarkingGoods());
		case IS_PAWNSHOP:
			return bv(isPawnShopActivity());
		case IS_INSURANCE:
			return bv(isInsuranceActivity());
		case AGENT_NAMES: {
			StringBuilder s = new StringBuilder(Const.EMPTY_STRING);
			for (AgentTypeE agent : getAgentType()) {
				if (s.length() > 0)
					s.append(",");
				s.append(agent.pName);
			}
			return s.toString();
		}

		case TAX_MODES_TAG: {
			StringBuilder s = new StringBuilder();
			for (TaxModeE m : getTaxModes()) {
				if (s.length() > 0)
					s.append(",");
				s.append(m.pName);
			}
			return s.toString();
		}
		default:
			return super.onKey(key);
		}
	}

}
