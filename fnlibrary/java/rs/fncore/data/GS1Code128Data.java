package rs.fncore.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

/**
 * Структура GS1 баркода
 * @author nick
 *
 */
public class GS1Code128Data {
	public static final String GS1_DIV = "" + (char) 0x1d;
    public enum AIListE{

        UNKNOWN("0",0,0),
        SERIAL_SHIPPING_CONTAINER_CODE_SSCC("00",18),
        GLOBAL_TRADE_ITEM_NUMBER_GTIN("01",14),
        GTIN_OF_CONTAINED_TRADE_ITEMS("02",14),
        BATCH_LOT_NUMBER("10",1,20),
        PRODUCTION_DATE("11",6),
        DUE_DATE("12",6),
        PACKAGING_DATE("13",6),
        BEST_BEFORE_DATE_YYMMDD("15",6),
        EXPIRATION_DATE("17",6),
        PRODUCT_VARIANT("20",2),
        SERIAL_NUMBER("21",1,20),
        SECONDARY_DATA_FIELDS("22",1,29),
        LOT_NUMBER_N("23",2,20),
        ADDITIONAL_PRODUCT_IDENTIFICATION("240",1,30),
        CUSTOMER_PART_NUMBER("241",1,30),
        MADE_TO_ORDER_VARIATION_NUMBER("242",1,6),
        PACKAGING_COMPONENT_NUMBER("243",1,20),
        SECONDARY_SERIAL_NUMBER("250",1,30),
        REFERENCE_TO_SOURCE_ENTITY("251",1,30),
        GLOBAL_DOCUMENT_TYPE_IDENTIFIER("253",13,17),
        GLN_EXTENSION_COMPONENT("254",1,20),
        GLOBAL_COUPON_NUMBER_GCN("255",13,25),
        COUNT_OF_ITEMS("30",1,8),
        // TODO: continue according to http://en.wikipedia.org/wiki/GS1-128
        PRODUCT_NET_WEIGHT_IN_KG("310",1,30),
        PRODUCT_LENGTH_1ST_DIMENSION_IN_METERS("311",1,30),
        PRODUCT_WIDTH_DIAMETER_2ND_DIMENSION_IN_METERS("312",1,30),
        PRODUCT_DEPTH_THICKNESS_HEIGHT_3RD_DIMENSION_IN_METERS("313",1,30),
        PRODUCT_AREA_IN_SQUARE_METERS("314",1,30),
        PRODUCT_NET_VOLUME_IN_LITERS("315",1,30),
        PRODUCT_NET_VOLUME_IN_CUBIC_METERS("316",1,30),
        PRODUCT_NET_WEIGHT_IN_POUNDS("320",1,30),
        PRODUCT_LENGTH_1ST_DIMENSION_IN_INCHES("321",1,30),
        PRODUCT_LENGTH_1ST_DIMENSION_IN_FEET("322",1,30),
        PRODUCT_LENGTH_1ST_DIMENSION_IN_YARDS("323",1,30),
        PRODUCT_WIDTH_DIAMETER_2ND_DIMENSION_IN_INCHES("324",1,30),
        PRODUCT_WIDTH_DIAMETER_2ND_DIMENSION_IN_FEET("325",1,30),
        PRODUCT_WIDTH_DIAMETER_2ND_DIMENSION_IN_YARDS("326",1,30),
        PRODUCT_DEPTH_THICKNESS_HEIGHT_3RD_DIMENSION_IN_INCHES("327",1,30),
        PRODUCT_DEPTH_THICKNESS_HEIGHT_3RD_DIMENSION_IN_FEET("328",1,30),
        PRODUCT_DEPTH_THICKNESS_3RD_DIMENSION_IN_YARDS("329",1,30),
        CONTAINER_GROSS_WEIGHT_KG("330",1,30),
        CONTAINER_LENGTH_1ST_DIMENSION_METERS("331",1,30),
        CONTAINER_WIDTH_DIAMETER_2ND_DIMENSION_METERS("332",1,30),
        CONTAINER_DEPTH_THICKNESS_3RD_DIMENSION_METERS("333",1,30),
        CONTAINER_AREA_SQUARE_METERS("334",1,30),
        CONTAINER_GROSS_VOLUME_LITERS("335",1,30),
        CONTAINER_GROSS_VOLUME_CUBIC_METERS("336",1,30),
        CONTAINER_GROSS_WEIGHT_POUNDS("340",1,30),
        CONTAINER_LENGTH_1ST_DIMENSION_IN_INCHES("341",1,30),
        CONTAINER_LENGTH_1ST_DIMENSION_IN_FEET("342",1,30),
        CONTAINER_LENGTH_1ST_DIMENSION_IN_IN_YARDS("343",1,30),
        CONTAINER_WIDTH_DIAMETER_2ND_DIMENSION_IN_INCHES("344",1,30),
        CONTAINER_WIDTH_DIAMETER_2ND_DIMENSION_IN_FEET("345",1,30),
        CONTAINER_WIDTH_DIAMETER_2ND_DIMENSION_IN_YARDS("346",1,30),
        CONTAINER_DEPTH_THICKNESS_HEIGHT_3RD_DIMENSION_IN_INCHES("347",1,30),
        CONTAINER_DEPTH_THICKNESS_HEIGHT_3RD_DIMENSION_IN_FEET("348",1,30),
        CONTAINER_DEPTH_THICKNESS_HEIGHT_3RD_DIMENSION_IN_YARDS("349",1,30),
        PRODUCT_AREA_SQUARE_INCHES("350",1,30),
        PRODUCT_AREA_SQUARE_FEET("351",1,30),
        PRODUCT_AREA_SQUARE_YARDS("352",1,30),
        CONTAINER_AREA_SQUARE_INCHES("353",1,30),
        CONTAINER_AREA_SQUARE_FEET("354",1,30),
        CONTAINER_AREA_SQUARE_YARDS("355",1,30),
        NET_WEIGHT_TROY_OUNCES("356",1,30),
        NET_WEIGHT_VOLUME_OUNCES("357",1,30),
        PRODUCT_VOLUME_QUARTS("360",1,30),
        PRODUCT_VOLUME_GALLONS("361",1,30),
        CONTAINER_GROSS_VOLUME_QUARTS("362",1,30),
        CONTAINER_GROSS_VOLUME_U_S_GALLONS("363",1,30),
        PRODUCT_VOLUME_CUBIC_INCHES("364",1,30),
        PRODUCT_VOLUME_CUBIC_FEET("365",1,30),
        PRODUCT_VOLUME_CUBIC_YARDS("366",1,30),
        CONTAINER_GROSS_VOLUME_CUBIC_INCHES("367",1,30),
        CONTAINER_GROSS_VOLUME_CUBIC_FEET("368",1,30),
        CONTAINER_GROSS_VOLUME_CUBIC_YARDS("369",1,30),
        NUMBER_OF_UNITS_CONTAINED("37",1,30),
        AMOUNT_PAYABLE_LOCAL_CURRENCY("390",1,30),
        AMOUNT_PAYABLE_WITH_ISO_CURRENCY_CODE("391",1,30),
        AMOUNT_PAYABLE_PER_SINGLE_ITEM_LOCAL_CURRENCY("392",1,30),
        AMOUNT_PAYABLE_PER_SINGLE_ITEM_WITH_ISO_CURRENCY_CODE("393",1,30),
        CUSTOMER_PURCHASE_ORDER_NUMBER("400",1,30),
        CONSIGNMENT_NUMBER("401",1,30),
        BILL_OF_LADING_NUMBER("402",1,30),
        ROUTING_CODE("403",1,30),
        SHIP_TO_DELIVER_TO_LOCATION_CODE_GLOBAL_LOCATION_NUMBER("410",1,30),
        BILL_TO_INVOICE_LOCATION_CODE_GLOBAL_LOCATION_NUMBER("411",1,30),
        PURCHASE_FROM_LOCATION_CODE_GLOBAL_LOCATION_NUMBER("412",1,30),
        SHIP_FOR_DELIVER_FOR_OR_FORWARD_TO_LOCATION_CODE_GLOBAL_LOCATION_NUMBER("413",1,30),
        IDENTIFICATION_OF_A_PHYSICAL_LOCATION_GLOBAL_LOCATION_NUMBER("414",1,30),
        SHIP_TO_DELIVER_TO_POSTAL_CODE_SINGLE_POSTAL_AUTHORITY("420",1,30),
        SHIP_TO_DELIVER_TO_POSTAL_CODE_WITH_ISO_COUNTRY_CODE("421",1,30),
        COUNTRY_OF_ORIGIN_ISO_COUNTRY_CODE("422",1,30),
        COUNTRY_OR_COUNTRIES_OF_INITIAL_PROCESSING("423",1,30),
        COUNTRY_OF_PROCESSING("424",1,30),
        COUNTRY_OF_DISASSEMBLY("425",1,30),
        COUNTRY_OF_FULL_PROCESS_CHAIN("426",1,30),
        NATO_STOCK_NUMBER_NSN("7001",1,30),
        UN_ECE_MEAT_CARCASSES_AND_CUTS_CLASSIFICATION("7002",1,30),
        EXPIRATION_DATE_AND_TIME("7003",1,30),
        ACTIVE_POTENCY("7004",1,30),
        PROCESSOR_APPROVAL_WITH_ISO_COUNTRY_CODE("703",1,30),
        ROLL_PRODUCTS__WIDTH_LENGTH_CORE_DIAMETER_DIRECTION_SPLICES("8001",1,30),
        MOBILE_PHONE_IDENTIFIER("8002",1,30),
        GLOBAL_RETURNABLE_ASSET_IDENTIFIER("8003",1,30),
        GLOBAL_INDIVIDUAL_ASSET_IDENTIFIER("8004",1,30),
        PRICE_PER_UNIT_OF_MEASURE("8005",1,30),
        IDENTIFICATION_OF_THE_COMPONENTS_OF_AN_ITEM("8006",1,30),
        INTERNATIONAL_BANK_ACCOUNT_NUMBER("8007",1,30),
        DATE_TIME_OF_PRODUCTION("8008",1,30),
        GLOBAL_SERVICE_RELATIONSHIP_NUMBER("8018",1,30),
        PAYMENT_SLIP_REFERENCE_NUMBER("8020",1,30),
        COUPON_EXTENDED_CODE__NUMBER_SYSTEM_AND_OFFER("8100",1,30),
        COUPON_EXTENDED_CODE__NUMBER_SYSTEM_OFFER_END_OF_OFFER("8101",1,30),
        COUPON_EXTENDED_CODE__NUMBER_SYSTEM_PRECEDED_BY_0("8102",1,30),
        COUPON_CODE_ID_NORTH_AMERICA("8110",1,30),
        EXTENDED_PACKAGING_URL("8200",1,30),
        MUTUALLY_AGREED_BETWEEN_TRADING_PARTNERS("90",1,30),
        MARKING_CHECK_KEY("91",4),
        MARKING_CHECK_CODE("92",44,88),
        MARKING_CRYPTO_TAIL("93",4),
        INTERNAL_COMPANY_CODES4("94",1,90),
        INTERNAL_COMPANY_CODES5("95",1,90),
        INTERNAL_COMPANY_CODES6("96",1,90),
        INTERNAL_COMPANY_CODES7("97",1,90),
        INTERNAL_COMPANY_CODES8("98",1,90),
        INTERNAL_COMPANY_CODES9("99",1,90),
        ;

        public final String id;
        public final int maxLength;
        public final int minLength;

        private AIListE(String id, int minLength, int maxLength) {
            this.id = id;
            this.maxLength=maxLength;
            this.minLength=minLength;
        }

        private AIListE(String id, int length) {
            this(id, length, length);
        }

        public static AIListE fromID(String id){
            for (AIListE val:values()){
                if (val.id.equals(id)){
                    return val;
                }
            }
            return UNKNOWN;
        }
    }

    /** Maps the AI to the corresponding data from the barcode. */
    private final Map<AIListE, AIData> mData = new HashMap<>();

    public static class AIData{
        public final int offset;
        public final String data;
        public AIData(int offset, String data){
            this.offset=offset;
            this.data=data;
        }
    }

    public GS1Code128Data(String s) {
    	this(s,GS1_DIV);
    }
    
    public GS1Code128Data(String s, String dividers) {
        StringBuilder ai = new StringBuilder();
        int index = 0;
        while(index < s.length()) {
            char newChar=s.charAt(index++);
            if (dividers.indexOf(newChar)!=-1) continue;

            ai.append(newChar);
            AIListE info = AIListE.fromID(ai.toString());
            if (info != AIListE.UNKNOWN) {
                int dataOffset=index;
                StringBuilder value = new StringBuilder();
                for (int i = 0; i < info.maxLength && index < s.length(); i++) {
                    char c = s.charAt(index++);
                    if (dividers.indexOf(c)!=-1) break;

                    value.append(c);
                }
                if (value.length() < info.minLength) {
                    throw new IllegalArgumentException("Short field for AI \"" + ai + "\": \"" + value + "\".");
                }
                Log.d("fncore2", "GS1 "+info+" : '"+value.toString()+"' offset "+dataOffset);
                mData.put(info, new AIData(dataOffset-ai.length(), value.toString()));
                ai.setLength(0);
            }
        }
        if (ai.length() > 0) {
            //throw new IllegalArgumentException("Unknown AI \"" + ai + "\".");
        }
    }

    public AIData getSerial(){
        return mData.get(AIListE.SERIAL_NUMBER);
    }

    public AIData getGTIN(){
        return mData.get(AIListE.GLOBAL_TRADE_ITEM_NUMBER_GTIN);
    }

    public AIData getMarkingCheckKey(){
        return mData.get(AIListE.MARKING_CHECK_KEY);
    }

    public boolean hasMarkingCheckKey(){
        return mData.get(AIListE.MARKING_CHECK_KEY)!=null && !mData.get(AIListE.MARKING_CHECK_KEY).data.isEmpty();
    }

    public boolean hasMarkingCheckCode(){
        return mData.get(AIListE.MARKING_CHECK_CODE)!=null && !mData.get(AIListE.MARKING_CHECK_CODE).data.isEmpty();
    }

    public boolean hasMarkingCryptoTail(){
        return mData.get(AIListE.MARKING_CRYPTO_TAIL)!=null && !mData.get(AIListE.MARKING_CRYPTO_TAIL).data.isEmpty();
    }

    public AIData getMarkingCheckCode(){
        return mData.get(AIListE.MARKING_CHECK_CODE);
    }

    public AIData getMarkingCryptoTail(){
        return mData.get(AIListE.MARKING_CRYPTO_TAIL);
    }

    private static Date asDate(String s) throws ParseException {
        if (s == null) return null;
        return new SimpleDateFormat("yyMMdd").parse(s);
    }

    public Date getDueDate() throws ParseException {
        return asDate(mData.get(AIListE.DUE_DATE).data);
    }
}
