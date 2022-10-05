package rs.fncore2.data;

import android.annotation.SuppressLint;

import java.math.BigDecimal;

import rs.fncore.Const;
import rs.fncore.data.Document;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.Location;
import rs.fncore.data.OU;
import rs.fncore.data.TemplateProcessor;
import rs.fncore2.PrintHelper;
import rs.utils.Utils;

@SuppressLint("DefaultLocale")
public class CashWithdraw implements TemplateProcessor {
    private OU mOperator;
    private OU mOwner;
    private BigDecimal mValue;
    private Location mLocation;
    private long mDate;

    public CashWithdraw(BigDecimal value, KKMInfo info, OU operator) {
        mLocation = info.getLocation();
        mDate = System.currentTimeMillis();
        mOwner = info.getOwner();
        mValue = value;
        if (operator == null || operator.getName().isEmpty())
            operator = info.signature().operator();
        mOperator = operator;
    }

    public Location getLocation() {
        return mLocation;
    }

    public OU getOperator() {
        return mOperator;
    }

    public OU getOwner() {
        return mOwner;
    }

    public BigDecimal getValue() {
        return mValue.abs();
    }

    public boolean isIncome() {
        return mValue.compareTo(BigDecimal.ZERO) == 1;
    }

    public long getDate() {
        return mDate;
    }

    public String getPF() {
        return PrintHelper.processTemplate(PrintHelper.loadTemplate(null, "cash"), this);
    }

    private static final String DATE = "Date";
    private static final String IS_INCOME = "isIncome";
    private static final String SUM = "sum";
    private static final String CASIER_NAME = "operator.Name";
    private static final String CASIER_INN = "operator.INN";
    private static final String OWNER_NAME = "owner.Name";
    private static final String OWNER_INN = "owner.INN";
    private static final String ADDRESS = "Address";
    private static final String LOCATION = "Location";

    @Override
    public String onKey(String key) {
        if (OWNER_INN.equals(key)) return getOwner().getINNtrimZ();
        if (OWNER_NAME.equals(key)) return getOwner().getName();
        if (ADDRESS.equals(key)) return getLocation().getAddress();
        if (LOCATION.equals(key)) return getLocation().getPlace();
        if (DATE.equals(key)) return Utils.formatDate(getDate());
        if (CASIER_INN.equals(key)) return getOperator().getINNtrimZ();
        if (CASIER_NAME.equals(key)) return getOperator().getName();
        if (IS_INCOME.equals(key)) return Document.bv(isIncome());
        if (SUM.equals(key)) return String.format("%.2f", getValue());
        return Const.EMPTY_STRING;
    }

}
