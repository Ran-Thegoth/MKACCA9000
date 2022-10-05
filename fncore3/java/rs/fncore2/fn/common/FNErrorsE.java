package rs.fncore2.fn.common;

public enum FNErrorsE {
    UNKNOWN(                                        -1),
    NO_ERROR(                                       0x0),
    UNKNOWN_CMD_OR_WRONG_PARAM(                     0x01),
    ANOTHER_FN_STATE(                               0x02),
    FN_FAILURE(                                     0x03),
    KC_FAILURE(                                     0x04),
    INVALID_COMMAND_FOR_FN_LIFE(                    0x05),

    INCORRECT_DATE_TIME(                            0x07),
    NO_REQUESTED_DATA(                              0x08),
    INCORRECT_COMMAND_PARAM(                        0x09),
    INCORRECT_COMMAND(                              0x0A),
    DISALLOWED_REQUISITES(                          0x0B),
    DUPLICATE_DATA(                                 0x0C),
    NO_DATA_FOR_FN_ACCOUNTING(                      0x0D),
    TOO_MUCH_DOC_POS(                               0x0E),
    TOO_MUCH_TLV_DATA(                              0x10),
    NO_TRANSPORT_CONNECTION(                        0x11),
    FN_EXAUSTED(                                    0x12),
    FN_OUT_OF_RESOURCE(                             0x14),
    TOO_LONG_SHIFT(                                 0x16),
    WRONG_DOC_TIME_INTERVAL(                        0x17),
    INCORRECT_REQUISIT_FROM_KKT(                    0x18),
    INCORRECT_REQUISIT_FROM_REG(                    0x19),
    ERROR_PROCESSING_ANSWER_FN(                     0x20),
    ERROR_KEY_KM_SERVICE(                           0x23),
    UNKN_ANSWER_KEY_SERVICE(                        0x24),
    REQUEST_UPDATE_KEY_KM(                          0x30),
    MARKING_ITEMS_PROHIBITED(                       0x32),
    INVALID_SEQUENCE_COMMANDS_Bxh(                  0x33),
    MARKING_ITEMS_BLOCKED(                          0x34),
    OVERLOAD_TABLE_MARKING(                         0x35),
    TLV_REQUISITES_EMPTY(                           0x3C),
    MARKING_CODE_NOT_EXIST_IN_CHECK_TABLE(          0x3E)
    ;

    public final byte bVal;
    private FNErrorsE(int value) {
        this.bVal = (byte)value;
    }

    public static FNErrorsE fromInt(int state){
        byte cmpVal=(byte)state;
        for (FNErrorsE val:values()){
            if (val.bVal == cmpVal){
                return val;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return "0x"+Integer.toHexString(bVal)+", "+super.toString();
    }
}
