package rs.fncore2.io.one_c;


import android.util.Base64;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import rs.fncore.Const;
import rs.fncore.FZ54Tag;
import rs.fncore.data.AgentTypeE;
import rs.fncore.data.MarkingCode;
import rs.fncore.data.OU;
import rs.fncore.data.Payment;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.VatE;
import rs.fncore2.core.ServiceBinder;
import rs.log.Logger;

public class SellOrder1C extends SellOrder {
    OU casier = new OU();
    String footer = Const.EMPTY_STRING;

    public SellOrder1C(OrderTypeE type, TaxModeE sno) {
        super(type, sno);
    }

    public static String decodeMarkingCode(String encodedString) {
        byte[] decodedBytes = Base64.decode(encodedString, Base64.DEFAULT);
        String decodedString = new String(decodedBytes);
        decodedString = decodedString.replaceAll("\\\\x1d", new String(new byte[]{(byte) 0x1d}));
        return decodedString;
    }

    public static SellOrder1C decode(String str, ServiceBinder binder) {
        SellOrder1C result = null;
        SellItem curItem = null;
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new InputStreamReader(new ByteArrayInputStream(str.getBytes())));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    switch(parser.getName()){
                        case "Parameters": {
                            String s = parser.getAttributeValue(null, "PaymentType");
                            if (s == null) {
                                return null;
                            }
                            OrderTypeE type = OrderTypeE.fromByte((byte) Integer.parseInt(s));
                            TaxModeE taxMode = TaxModeE.COMMON;
                            s = parser.getAttributeValue(null, "TaxVariant");
                            if (s != null)
                                try {
                                    taxMode = TaxModeE.fromByte((byte) Integer.parseInt(s));
                                } catch (NumberFormatException nfe) {
                                    Logger.e(nfe, "TaxVariant exc: ");
                                }
                            result = new SellOrder1C(type, taxMode);
                            s = parser.getAttributeValue(null, "CashierName");
                            if (s != null)
                                result.casier.setName(StringEscapeUtils.unescapeXml(s));
                            s = parser.getAttributeValue(null, "CashierVATIN");
                            if (s != null)
                                result.casier.setINN(s);

                            s = parser.getAttributeValue(null, "CustomerInfo");
                            if (s != null && !s.isEmpty())
                                result.add(FZ54Tag.T1227_CLIENT_NAME, StringEscapeUtils.unescapeXml(s));

                            s = parser.getAttributeValue(null, "CustomerINN");
                            if (s != null && !s.isEmpty())
                                result.add(FZ54Tag.T1228_CLIENT_INN, StringEscapeUtils.unescapeXml(s));

                            s = parser.getAttributeValue(null, "CustomerEmail");
                            if (s != null && !s.isEmpty())
                                result.add(FZ54Tag.T1008_BUYER_PHONE_EMAIL, StringEscapeUtils.unescapeXml(s));
                            s = parser.getAttributeValue(null, "CustomerPhone");
                            if (s != null && !s.isEmpty())
                                result.add(FZ54Tag.T1008_BUYER_PHONE_EMAIL, StringEscapeUtils.unescapeXml(s));
                            s = parser.getAttributeValue(null, "AdditionalAttribute");
                            if (s != null) {
                                if (s.length() > 16)
                                    s = s.substring(0, 15);
                                result.add(FZ54Tag.T1192_EXTRA_BILL_FIELD, s);
                            }
                            s = parser.getAttributeValue(null, "PlaceSettle");
                            if (s != null)
                                result.getLocation().setPlace(StringEscapeUtils.unescapeXml(s));
                            s = parser.getAttributeValue(null, "AddressSettle");
                            if (s != null)
                                result.getLocation().setAddress(StringEscapeUtils.unescapeXml(s));
                            break;
                        }
                        case "FiscalString": {
                            String s = parser.getAttributeValue(null, "SignCalculationObject");
                            SellItem.SellItemTypeE itemType = SellItem.SellItemTypeE.GOOD;
                            if (s != null)
                                try {
                                    itemType = SellItem.SellItemTypeE.fromByte((byte) Integer.parseInt(s));
                                } catch (NumberFormatException nfe) {
                                    Logger.e(nfe, "SignCalculationObject exc: ");
                                }
                            String name = "Товар по свободной цене";
                            BigDecimal qtty = BigDecimal.ONE;
                            BigDecimal price = BigDecimal.ZERO;
                            VatE vat = VatE.VAT_20;
                            SellItem.ItemPaymentTypeE pType = SellItem.ItemPaymentTypeE.FULL;
                            s = parser.getAttributeValue(null, "Name");
                            if (s != null)
                                name = StringEscapeUtils.unescapeXml(s);
                            s = parser.getAttributeValue(null, "Quantity");
                            if (s != null)
                                qtty = new BigDecimal(s.replace(",", "."));
                            s = parser.getAttributeValue(null, "PriceWithDiscount");
                            if (s != null)
                                price = new BigDecimal(s.replace(",", "."));
                            s = parser.getAttributeValue(null, "Tax");
                            switch (s) {
                                case "none":
                                    vat = VatE.VAT_NONE;
                                    break;
                                case "0":
                                    vat = VatE.VAT_0;
                                    break;
                                case "20":
                                    vat = VatE.VAT_20;
                                    break;
                                case "20/120":
                                    vat = VatE.VAT_20_120;
                                    break;
                                case "10":
                                    vat = VatE.VAT_10;
                                    break;
                                case "10/110":
                                    vat = VatE.VAT_10_110;
                                    break;
                                default:
                                    Logger.e("unknown VAT %s", s);
                                    break;
                            }
                            s = parser.getAttributeValue(null, "SignMethodCalculation");
                            if (s != null)
                                try {
                                    pType = SellItem.ItemPaymentTypeE.fromByte((byte) Integer.parseInt(s));
                                } catch (NumberFormatException nfe) {
                                    Logger.e(nfe, "SignMethodCalculation exc: ");
                                }
                            curItem = new SellItem(itemType, pType, name, qtty, null, price, vat);
                            s = parser.getAttributeValue(null, "CustomsDeclaration");
                            if (s != null) {
                                curItem.add(FZ54Tag.T1231_CUSTOMS_DECLARATION_NO, StringEscapeUtils.unescapeXml(s));
                            }
                            s = parser.getAttributeValue(null, "CountryOfOrigin");
                            if (s != null) {
                                curItem.add(FZ54Tag.T1230_COUNTRY_ORIGIN, StringEscapeUtils.unescapeXml(s));
                            }
                            s = parser.getAttributeValue(null, "SignSubjectCalculationAgent");
                            if (s != null) {
                                try {
                                    AgentTypeE aType = AgentTypeE.values()[Integer.parseInt(s)];
                                    curItem.getAgentData().setType(aType);
                                } catch (NumberFormatException nfe) {
                                    Logger.e(nfe, "SignMethodCalculation exc: %s", s);
                                }
                            }
                            result.addItem(curItem);
                            break;
                        }
                        case "AgentData": {
                            if (curItem != null && curItem.getAgentData().getType() != AgentTypeE.NONE) {
                                String s = parser.getAttributeValue(null, "PayingAgentOperation");
                                if (s != null) {
                                    curItem.getAgentData().setAgentOperation(s);
                                }

                                s = parser.getAttributeValue(null, "PayingAgentPhone");
                                if (s != null) {
                                    curItem.getAgentData().setAgentPhone(s);
                                }

                                s = parser.getAttributeValue(null, "MoneyTransferOperatorName");
                                if (s != null) {
                                    curItem.getAgentData().setOperatorName(s);
                                }

                                s = parser.getAttributeValue(null, "MoneyTransferOperatorPhone");
                                if (s != null) {
                                    curItem.getAgentData().setOperatorPhone(s);
                                }

                                s = parser.getAttributeValue(null, "MoneyTransferOperatorAddress");
                                if (s != null) {
                                    curItem.getAgentData().setOperatorAddress(s);
                                }

                                s = parser.getAttributeValue(null, "MoneyTransferOperatorVATIN");
                                if (s != null) {
                                    curItem.getAgentData().setOperatorINN(s);
                                }
                            }
                            break;
                        }
                        case "PurveyorData": {
                            if (curItem != null && curItem.getAgentData().getType() != AgentTypeE.NONE) {
                                String s = parser.getAttributeValue(null, "PurveyorPhone");
                                if (s != null) {
                                    curItem.getAgentData().setProviderPhone(s);
                                }

                                s = parser.getAttributeValue(null, "PurveyorName");
                                if (s != null) {
                                    curItem.getAgentData().setProviderName(s);
                                }

                                s = parser.getAttributeValue(null, "PurveyorVATIN");
                                if (s != null) {
                                    curItem.getAgentData().setProviderINN(s);
                                }
                            }
                            break;
                        }
                        case "GoodCodeData": {
                            if (curItem != null) {
                                short hdr = 0x444d;
                                ByteBuffer b1162 = ByteBuffer.allocate(256);
                                String markTag = parser.getAttributeValue(null, "Stamp");
                                if (markTag == null) {
                                    String s = parser.getAttributeValue(null, "StampType");
                                    if ("1520".equals(s))
                                        markTag = "[O]";
                                    else if ("05".equals(s))
                                        markTag = "[T]";
                                    else if ("02".equals(s)) {
                                        hdr = 0x5246;
                                        markTag = "[M]";
                                    } else
                                        markTag = "[A]";
                                }
                                b1162.putShort(hdr);
                                String GTIN = parser.getAttributeValue(null, "GTIN");
                                if (GTIN != null) {
                                    BigInteger bin = new BigInteger(GTIN);
                                    byte[] bbin = bin.toByteArray();
                                    for (int i = bbin.length; i < 6; i++)
                                        b1162.put((byte) 0);
                                    b1162.put(bbin);
                                }
                                String SN = parser.getAttributeValue(null, "SerialNumber");
                                if (SN != null) {
                                    b1162.put(SN.getBytes(Charset.forName("CP866")));
                                }
                                byte[] a1162 = new byte[b1162.position()];
                                System.arraycopy(b1162.array(), 0, a1162, 0, a1162.length);
                                curItem.add(FZ54Tag.T1162_COMMODITY_NUMBER, a1162);

                                String markCode = parser.getAttributeValue(null, "MarkingCodeBase64");
                                if (markCode != null) {
                                    String decodedMarkCode = decodeMarkingCode(markCode);
                                    MarkingCode code = new MarkingCode(decodedMarkCode,
                                            MarkingCode.PlannedItemStatusE.UNKNOWN);
                                    curItem.setMarkingCode(code);
                                } else {
                                    String markingCode = parser.getAttributeValue(null, "MarkingCode");
                                    if (markingCode != null) {
                                        markingCode = decodeMarkingCode(markingCode);
                                        curItem.add(FZ54Tag.T2101_ITEM_IDENTIFICATOR, markingCode);
                                    }
                                }
                            }
                            break;
                        }
                        case "Payments": {
                            for (Payment p : Payment1C.decode(parser)) {
                                result.addPayment(p);
                            }
                            break;
                        }
                        case "TextString": {
                            String s = parser.getAttributeValue(null, "Text");
                            if (s != null) {
                                if (!result.footer.isEmpty()) {
                                    result.footer += "\n";
                                }
                                result.footer += StringEscapeUtils.unescapeXml(s);
                            }
                            break;
                        }
                        case "Barcode": {
                            String s = parser.getAttributeValue(null, "Barcode");
                            String type = parser.getAttributeValue(null, "BarcodeType");
                            if (type == null) {
                                type = "ean13";
                            } else {
                                type = type.toLowerCase();
                            }
                            if (s != null) {
                                if (!result.footer.isEmpty()) {
                                    result.footer += "\n";
                                }
                                result.footer += "{\\barcode width:90%;height:80;type:" + type + "\\" + s + "}";
                            }
                            break;
                        }
                        default:
                            Logger.e("Unknown field in SellOrder1C: %s", parser.getName());
                            break;
                    }
                }
                event = parser.next();
            }
            return result;
        } catch (Exception e) {
            Logger.e(e, "decodeOrder exc");
            return null;
        }
    }
}
