package rs.fncore2.fn.common;

public enum FNCommandsE {
    UNKNOWN(-1),
    START_FISCALIZATION(        0x02),
    COMMIT_FISCALIZATION(       0x03),
    START_CLOSE_FISCAL_MODE(    0x04),
    COMMIT_CLOSE_FISCAL_MODE(   0x05),

    CANCEL_DOCUMENT(            0x06),
    ADD_DOCUMENT_DATA(          0x07),

    GET_SHIFT_STATUS(           0x10),
    START_OPEN_SHIFT(           0x11),
    COMMIT_OPEN_SHIFT(          0x12),
    START_CLOSE_SHIFT(          0x13),
    COMMIT_CLOSE_SHIFT(         0x14),

    START_BILL(                 0x15),
    COMMIT_BILL(                0x16),
    START_CORRECTION_BILL(      0x17),

    START_FISCAL_REPORT(        0x18),
    COMMIT_FISCAL_REPORT(       0x19),

    GET_OFD_STATUS(             0x20),
    SET_OFD_CONNECTION_STATUS(  0x21),
    START_READ_OFD_MESSAGE(     0x22),
    READ_OFD_MESSAGE_BLOCK(     0x23),
    CANCEL_READ_OFD_MESSAGE(    0x24),
    COMMIT_READ_OFD_MESSAGE(    0x25),
    STORE_OFD_RECEIPT(          0x26),

    GET_FN_STATUS(              0x30),
    GET_FN_NUMBER(              0x31),
    GET_FN_EXPIRE_INFO(         0x32),
    GET_FN_VERSION(             0x33),
    GET_FN_HARD_ERRORS(         0x35),

    GET_FN_COUNTERS(            0x36), //1.2
    GET_FN_COUNTERS_OPERATIONS( 0x37), //1.2
    GET_FN_COUNTERS_TYPE(       0x38), //1.2
    GET_FN_COUNTERS_NON_SEND_DOCS(0x39), //1.2
    GET_FN_FFD(                 0x3A),
    GET_FN_REMAINING_TIME(      0x3B), //1.2
    GET_FN_REMAINING_SPACE(     0x3D), //1.2

    FIND_ARCHIVE_BILL(          0x40),
    FIND_ARCHIVE_RECEIPT(       0x41),
    GET_OFD_UNCONFIRMED_COUNT(  0x42),
    GET_FISCALIZATION_RESULT(   0x43),
    GET_FISCALIZATION_ARG(      0x44),
    GET_FISCAL_DOC_IN_TLV_INFO( 0x45),
    GET_FISCAL_DOC_IN_TLV_DATA( 0x46),

    RESET_MGM(0x60),

    SET_FN_SPEED(0xAB), //1.2

    START_FISCALIZATION_1_2(0xA2), //1.2
    COMMIT_FISCALIZATION_1_2(0xA3),//1.2

    GET_FN_MARKING_STATUS(0xB0),//1.2
    MARKING_CODE_TO_FN(0xB1),//1.2
    MARKING_CODE_CHECK_SAVE(0xB2),//1.2
    MARKING_CODE_CHECK_CLEAR(0xB3),//1.2
    MARKING_CODE_REQUEST_CREATE(0xB5),//1.2
    MARKING_CODE_REQUEST_STORE(0xB6),//1.2
    MARKING_CODE_ADD_REQUEST_DATA(0xB7),//1.2

    GET_OISM_STATUS(0xBA),//1.2
    START_READ_OISM_MESSAGE(0xBB),
    READ_OISM_MESSAGE_BLOCK(0xBC),
    CANCEL_READ_OISM_MESSAGE(0xBD),
    COMMIT_READ_OISM_MESSAGE(0xBE),
    STORE_OFD_OISM_RECEIPT(0xBF),

    START_REQUST_UPDATE_OKP(0xD0),
    WRITE_RESP_UPDATE_OKP(0xD1),
    START_DOWNLOAD_MARKING_NOTIFY(0xD3),
    SETUP_MARKING_NOTIFY(0xD4),
    READ_MARKING_NOTIFY(0xD5), 
    CONFIRM_MARKING_NOTIFY(0xD6),
    GET_KEYS_SERVER(0xD7),
    ;

    public final int value;
    private FNCommandsE(int value) {
        this.value = value;
    }

    public static FNCommandsE fromInt(int state){
        for (FNCommandsE val:values()){
            if (val.value == state){
                return val;
            }
        }
        return UNKNOWN;
    }
}
