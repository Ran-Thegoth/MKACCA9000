package rs.fncore2.io;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.util.Base64;


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;

import rs.fncore.Const;
import rs.fncore.data.IReableFromParcel;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.Payment;
import rs.fncore.data.TemplateProcessor;
import rs.fncore2.PrintHelper;
import rs.utils.Utils;
import rs.log.Logger;

public class Rests implements IReableFromParcel, TemplateProcessor {
    private static final String REST_TAG = "Rests";
    private final SharedPreferences mSp;
    private final KKMInfo mKkmInfo;

    public  BigDecimal[] INCOME;
    public  BigDecimal[] OUTCOME;

    public Rests(SharedPreferences prefs, KKMInfo kkmInfo) {
        mSp=prefs;
        mKkmInfo=kkmInfo;
        INCOME = new BigDecimal[Payment.PaymentTypeE.values().length];
        Arrays.fill(INCOME, BigDecimal.ZERO);

        OUTCOME = new BigDecimal[Payment.PaymentTypeE.values().length];
        Arrays.fill(OUTCOME, BigDecimal.ZERO);

        String s = mSp.getString(REST_TAG, null);
        if (s != null) {
            try {
                Parcel p = Parcel.obtain();
                byte[] b = Base64.decode(s, Base64.NO_WRAP);
                p.unmarshall(b, 0, b.length);
                readFromParcel(p);
                p.recycle();
            } catch (Exception e) {
                Logger.e(e,"Ошибка восстановления остатков");
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int arg1) {
        for (BigDecimal bigDecimal : INCOME) p.writeString(bigDecimal.toString());
        for (BigDecimal bigDecimal : OUTCOME) p.writeString(bigDecimal.toString());
    }

    @Override
    public void readFromParcel(Parcel p) {
        for (int i = 0; i < INCOME.length; i++) {
            String s = p.readString();
            INCOME[i] = (s != null) ? new BigDecimal(s) : BigDecimal.ZERO;
        }
        for (int i = 0; i < OUTCOME.length; i++) {
            String s = p.readString();
            OUTCOME[i] = (s != null) ? new BigDecimal(s) : BigDecimal.ZERO;
        }
    }

    public static final Creator<Rests> CREATOR = new Creator<Rests>() {
        @Override
        public Rests createFromParcel(Parcel p) {
            return null;
        }

        @Override
        public Rests[] newArray(int arg0) {
            return null;
        }
    };

    public void store() {
        Parcel p = Parcel.obtain();
        writeToParcel(p, 0);
        p.setDataPosition(0);

        SharedPreferences.Editor editor = mSp.edit();
        editor.putString(REST_TAG, Base64.encodeToString(p.marshall(), Base64.NO_WRAP));
        editor.commit();

        p.recycle();
    }

    public void clear() {
        Arrays.fill(INCOME, BigDecimal.ZERO);
        Arrays.fill(INCOME, BigDecimal.ZERO);
        store();
    }

    private static final String DATE = "Date";
    private static final String INCOME_TAG = "Income.";
    private static final String OUTCOME_TAG = "Outcome.";
    private static final String REST = "Rest.";
    private static final String SHIFT_NUMBER = "shift.Number";
    private static final String OWNER_NAME = "owner.Name";
    private static final String OWNER_INN = "owner.INN";

    @Override
    public String onKey(String key) {
        if (OWNER_NAME.equals(key)) return mKkmInfo.getOwner().getName();
        if (OWNER_INN.equals(key)) return mKkmInfo.getOwner().getINNtrimZ();
        if (DATE.equals(key)) return Utils.formatDate(System.currentTimeMillis());
        if (SHIFT_NUMBER.equals(key)) return String.valueOf(mKkmInfo.getShift().getNumber());

        if (key.startsWith(INCOME_TAG)) {
            key = key.replace(INCOME_TAG, "");
            return String.format(Locale.ROOT, "%.2f", INCOME[Payment.PaymentTypeE.valueOf(key).ordinal()]);
        }

        if (key.startsWith(OUTCOME_TAG)) {
            key = key.replace(OUTCOME_TAG, "");
            return String.format(Locale.ROOT,"%.2f", OUTCOME[Payment.PaymentTypeE.valueOf(key).ordinal()]);
        }

        if (key.startsWith(REST)) {
            key = key.replace(REST, "");
            return String.format(Locale.ROOT,"%.2f", INCOME[Payment.PaymentTypeE.valueOf(key).ordinal()].subtract(OUTCOME[Payment.PaymentTypeE.valueOf(key).ordinal()]));
        }

        return Const.EMPTY_STRING;
    }

    public String getPF() {
        return PrintHelper.processTemplate(PrintHelper.loadTemplate(null, "xreport"), this);
    }
}
