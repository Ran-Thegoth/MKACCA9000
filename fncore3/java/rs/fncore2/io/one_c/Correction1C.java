package rs.fncore2.io.one_c;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Calendar;

import rs.fncore.FZ54Tag;
import rs.fncore.data.Correction;
import rs.fncore.data.OU;
import rs.fncore.data.Payment;
import rs.fncore.data.SellOrder;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.VatE;
import rs.log.Logger;

class Correction1C extends Correction {
    OU casier = new OU();

    public Correction1C(CorrectionTypeE type, SellOrder.OrderTypeE checkType,
                        BigDecimal sum, VatE vat, TaxModeE taxMode) {
        super(type, checkType, taxMode);
        setSum(sum);
        setVATMode(vat);
    }

    public static Correction1C decode(String str) {
        Correction1C result = null;
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new InputStreamReader(new ByteArrayInputStream(str.getBytes())));
            int event = parser.getEventType();

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    switch (parser.getName()) {
                        case "CheckCorrectionPackage":
                            break;
                        case "Parameters":
                            CorrectionTypeE cType = CorrectionTypeE.BY_ARBITARITY;
                            SellOrder.OrderTypeE oType = SellOrder.OrderTypeE.OUTCOME;
                            TaxModeE taxMode = TaxModeE.COMMON;
                            VatE vat = VatE.VAT_20;
                            BigDecimal sum = BigDecimal.ZERO;
                            String s = parser.getAttributeValue(null, "CorrectionType");
                            if (s != null)
                                try {
                                    cType = Integer.parseInt(s) == 0 ? CorrectionTypeE.BY_OWN
                                            : CorrectionTypeE.BY_ARBITARITY;
                                } catch (Exception e) {
                                    Logger.e(e, "CorrectionType exc: ");
                                }
                            s = parser.getAttributeValue(null, "PaymentType");
                            if (s != null)
                                try {
                                    oType = SellOrder.OrderTypeE.fromByte((byte) (Integer.parseInt(s) - 1));
                                } catch (Exception e) {
                                    Logger.e(e, "PaymentType exc: ");
                                }
                            s = parser.getAttributeValue(null, "TaxVariant");
                            if (s != null)
                                try {
                                    taxMode = TaxModeE.fromByte((byte) Integer.parseInt(s));
                                } catch (Exception e) {
                                    Logger.e(e, "TaxVariant exc: ");
                                }
                            s = parser.getAttributeValue(null, "Sum");
                            if (s != null)
                                try {
                                    sum = new BigDecimal(s.replace(",", "."));
                                } catch (NumberFormatException nfe) {
                                    Logger.e(nfe, "Sum exc: ");
                                }
                            s = parser.getAttributeValue(null, "SumTAX18");
                            if (s != null && Double.parseDouble(s.replace(",", ".")) > 0)
                                vat = VatE.VAT_20;
                            s = parser.getAttributeValue(null, "SumTAX20");
                            if (s != null && Double.parseDouble(s.replace(",", ".")) > 0)
                                vat = VatE.VAT_20;
                            s = parser.getAttributeValue(null, "SumTAX10");
                            if (s != null && Double.parseDouble(s.replace(",", ".")) > 0)
                                vat = VatE.VAT_10;
                            s = parser.getAttributeValue(null, "SumTAX0");
                            if (s != null && Double.parseDouble(s.replace(",", ".")) > 0)
                                vat = VatE.VAT_0;
                            s = parser.getAttributeValue(null, "SumTAXNone");
                            if (s != null && Double.parseDouble(s.replace(",", ".")) > 0)
                                vat = VatE.VAT_NONE;
                            s = parser.getAttributeValue(null, "SumTAX118");
                            if (s != null && Double.parseDouble(s.replace(",", ".")) > 0)
                                vat = VatE.VAT_20_120;
                            s = parser.getAttributeValue(null, "SumTAX120");
                            if (s != null && Double.parseDouble(s.replace(",", ".")) > 0)
                                vat = VatE.VAT_20_120;
                            s = parser.getAttributeValue(null, "SumTAX110");
                            if (s != null && Double.parseDouble(s.replace(",", ".")) > 0)
                                vat = VatE.VAT_10_110;
                            result = new Correction1C(cType, oType, sum, vat, taxMode);
                            s = parser.getAttributeValue(null, "CashierName");
                            if (s != null)
                                result.casier.setName(s);
                            s = parser.getAttributeValue(null, "CashierVATIN");
                            if (s != null)
                                result.casier.setINN(s);
                            s = parser.getAttributeValue(null, "AdditionalAttribute");
                            if (s != null) {
                                if (s.length() > 16)
                                    s = s.substring(0, 15);
                                result.add(FZ54Tag.T1192_EXTRA_BILL_FIELD, s);
                            }
                            String docInfo = "";
                            s = parser.getAttributeValue(null, "CorrectionBaseName");
                            if (s != null) {
                                if (!docInfo.isEmpty())
                                    docInfo += " ";
                                docInfo += s;
                            }
                            s = parser.getAttributeValue(null, "CorrectionBaseNumber");
                            if (s != null) {
                                if (!docInfo.isEmpty())
                                    docInfo += " № ";
                                docInfo += s;
                            }
                            result.setBaseDocumentNumber(docInfo);

                            s = parser.getAttributeValue(null, "СorrectionBaseDate");
                            if (s != null) {
                                String[] dd = s.split("T");
                                String[] ymd = dd[0].split("-");
                                Calendar c = Calendar.getInstance();
                                c.set(Calendar.YEAR, Integer.parseInt(ymd[0]));
                                c.set(Calendar.MONTH, Integer.parseInt(ymd[1]) - 1);
                                c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(ymd[2]));
                                if (dd.length > 1) {
                                    ymd = dd[1].split(":");
                                    c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(ymd[0]));
                                    c.set(Calendar.MINUTE, Integer.parseInt(ymd[1]));
                                }
                                result.setBaseDocumentDate(c.getTimeInMillis());
                            }

                            break;
                        case "Payments":
                            for (Payment p : Payment1C.decode(parser)) {
                                result.addPayment(p);
                            }
                            break;
                        default:
                            Logger.e("Unknown field in Correction1C: %s", parser.getName());
                            break;
                    }
                }
                event = parser.next();
            }

        } catch (Exception e) {
            Logger.e(e, "deserializeCorrection exc");
        }
        return result;
    }
}
