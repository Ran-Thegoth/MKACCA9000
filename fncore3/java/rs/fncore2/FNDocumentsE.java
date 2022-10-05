package rs.fncore2;

public enum FNDocumentsE {
    UNKNOWN(0),
    KKT_REGISTRATION_REPORT(0x01),
    OPEN_SHIFT_REPORT(0x02),

    FISCAL_ORDER(0x03),
    FORM_OF_STRICT_ACCOUNTABILITY(0x04),
    CLOSE_SHIFT_REPORT(0x05),
    FISCAL_MODE_CLOSE_REPORT(0x06),
    CHANGE_OPERATOR_REPORT(0x07),

    KKT_REGISTRATION_CHANGE_REPORT(0xB),
    REPORT_OF_CURRENT_STATUS(0x15),
    FISCAL_ORDER_CORRECTION(0x1F),
    CORRECTION_STRICT_REPORTING_FORM(0x29)
    ;

    public final int value;

    private FNDocumentsE(int value) {
        this.value = value;
    }
}
