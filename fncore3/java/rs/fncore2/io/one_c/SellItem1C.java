package rs.fncore2.io.one_c;

import static rs.fncore2.io.one_c.SellOrder1C.decodeMarkingCode;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import rs.fncore.data.MarkingCode;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.VatE;
import rs.log.Logger;

public class SellItem1C extends SellItem {
    public String GUID;
    public boolean waitForResult;

    public SellItem1C(String name, BigDecimal qtty, MeasureTypeE measure, BigDecimal price, VatE vat) {
        super(name, qtty, measure, price, vat);
    }

    public static SellItem1C decodeMarkedItem(String str) throws Exception {
        SellItem1C result=null;
        try {

            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new InputStreamReader(new ByteArrayInputStream(str.getBytes())));
            int event = parser.getEventType();

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    switch (parser.getName()){
                        case "RequestKM":
                            String gUID = parser.getAttributeValue(null, "GUID");
                            String waitForResult = parser.getAttributeValue(null, "WaitForResult");
                            String markingCode = parser.getAttributeValue(null, "MarkingCode");
                            String plannedStatus = parser.getAttributeValue(null, "PlannedStatus");
                            String quantity = parser.getAttributeValue(null, "Quantity");
                            String measureOfQuantity = parser.getAttributeValue(null, "MeasureOfQuantity");

                            result.GUID = gUID;
                            result.waitForResult = Boolean.parseBoolean(waitForResult);

                            MarkingCode.PlannedItemStatusE status = rs.fncore.data.MarkingCode.PlannedItemStatusE.UNKNOWN;
                            if (plannedStatus != null) {
                                status = MarkingCode.PlannedItemStatusE.fromByte((byte)Integer.parseInt(plannedStatus));
                            }
                            BigDecimal qtty = BigDecimal.ONE;
                            if (quantity!=null) {
                                qtty = new BigDecimal(quantity.replace(",", "."));
                            }

                            MeasureTypeE mType = MeasureTypeE.PIECE;
                            if (measureOfQuantity!=null){
                                mType=MeasureTypeE.fromByte((byte)Integer.parseInt(measureOfQuantity));
                            }

                            String decodedMarkCode = decodeMarkingCode(markingCode);
                            MarkingCode code = new MarkingCode(decodedMarkCode, status);

                            result = new SellItem1C("test", qtty, mType, BigDecimal.ONE, VatE.VAT_NONE);
                            result.setMarkingCode(code);

                            break;
                        case "FractionalQuantity":{
                            String numerator = parser.getAttributeValue(null, "Numerator");
                            String denominator = parser.getAttributeValue(null, "Denominator");

                            int num=0;
                            if (numerator!=null){
                                num = Integer.parseInt(numerator);
                            }

                            int den=0;
                            if (denominator!=null && !denominator.isEmpty()){
                                den = Integer.parseInt(denominator);
                            }
                            result.setMarkingFractionalAmount(num, den);
                        }
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
