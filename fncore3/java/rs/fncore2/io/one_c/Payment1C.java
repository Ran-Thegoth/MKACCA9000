package rs.fncore2.io.one_c;

import org.xmlpull.v1.XmlPullParser;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import rs.fncore.data.Payment;

public class Payment1C extends Payment {

    public static Collection<Payment> decode(XmlPullParser parser) throws Exception {
        Map<Payment.PaymentTypeE, Payment> payments = new HashMap<>();
        Payment p = null;
        String s = parser.getAttributeValue(null, "Cash");
        if (s != null) {
            BigDecimal v = new BigDecimal(s.replace(",", "."));
            if (v.compareTo(BigDecimal.ZERO) > 0) {
                p = new Payment(Payment.PaymentTypeE.CASH, v);
                payments.put(p.getType(), p);
            }
        }
        s = parser.getAttributeValue(null, "ElectronicPayment");
        if (s != null) {
            BigDecimal v = new BigDecimal(s.replace(",", "."));
            if (v.compareTo(BigDecimal.ZERO) > 0) {
                p = new Payment(Payment.PaymentTypeE.CARD, v);
                payments.put(p.getType(), p);
            }
        }
        s = parser.getAttributeValue(null, "AdvancePayment");
        if (s != null) {
            BigDecimal v = new BigDecimal(s.replace(",", "."));
            if (v.compareTo(BigDecimal.ZERO) > 0) {
                p = new Payment(Payment.PaymentTypeE.PREPAYMENT, v);
                payments.put(p.getType(), p);
            }
        }
        s = parser.getAttributeValue(null, "Credit");
        if (s != null) {
            BigDecimal v = new BigDecimal(s.replace(",", "."));
            if (v.compareTo(BigDecimal.ZERO) > 0) {
                p = new Payment(Payment.PaymentTypeE.CREDIT, v);
                payments.put(p.getType(), p);
            }
        }
        s = parser.getAttributeValue(null, "CashProvision");
        if (s != null) {
            BigDecimal v = new BigDecimal(s.replace(",", "."));
            if (v.compareTo(BigDecimal.ZERO) > 0) {
                p = new Payment(Payment.PaymentTypeE.AHEAD, v);
                payments.put(p.getType(), p);
            }
        }
        return payments.values();
    }
}
