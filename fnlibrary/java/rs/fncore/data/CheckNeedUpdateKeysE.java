package rs.fncore.data;

import java.security.InvalidParameterException;

/**
 * Статус необходимости обновления ключей ОКП.
 * @author nick
 *
 */
public enum CheckNeedUpdateKeysE {
	NO_NEED_UPDATE(0), NEED_UPDATE_DELAY_15_60_DAYS(1), NEED_UPDATE_DELAY_MORE_60_DAYS(2), UNKNOWN(100),
	UPDATED_NOW(101),;

	public final byte bVal;

	private CheckNeedUpdateKeysE(int val) {
		this.bVal = (byte) val;
	}

	public static CheckNeedUpdateKeysE fromByte(byte number) {
		for (CheckNeedUpdateKeysE val : values()) {
			if (val.bVal == number) {
				return val;
			}
		}
		throw new InvalidParameterException("unknown value");
	}

	public String getDays() {
		switch (this) {
		case UPDATED_NOW:
		case NO_NEED_UPDATE:
			return ">70";
		case NEED_UPDATE_DELAY_15_60_DAYS:
			return "15-60";
		case NEED_UPDATE_DELAY_MORE_60_DAYS:
			return ">60";
		default:
			return "unk";
		}
	}
}
