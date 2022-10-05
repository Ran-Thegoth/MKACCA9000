package rs.fncore.data;

import static rs.utils.Utils.checkDate;
import static rs.utils.Utils.checkINN;
import static rs.utils.Utils.checkStringNumbersOrSpaces;
import static rs.utils.Utils.formatInn;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;


import rs.fncore.FZ54Tag;

/**
 * Данные клиента
 *
 * @author amv
 */
public class ClientData extends Tag implements IReableFromParcel {
    public static final int MAX_DOC_DATA_LEN = 64;
    public static final int MAX_CITIZENSHIP_LEN = 3;
    public static final int MAX_CLIENT_ADDRESS_LEN = 256;

    private volatile boolean mExist;

    public static final SparseArray<String> TAGS_1256 = new SparseArray<>();
    static {
        TAGS_1256.append(FZ54Tag.T1227_CLIENT_NAME, "Имя клиента");
        TAGS_1256.append(FZ54Tag.T1228_CLIENT_INN, "ИНН клиента");
        TAGS_1256.append(FZ54Tag.T1243_CLIENT_BIRTHDAY, "Дата рождения");
        TAGS_1256.append(FZ54Tag.T1244_CLIENT_CITIZENSHIP, "Код гражданства");
        TAGS_1256.append(FZ54Tag.T1245_CLIENT_IDENTITY_DOC_CODE, "Код документа");
        TAGS_1256.append(FZ54Tag.T1246_CLIENT_IDENTITY_DOC_DATA, "Данные документа");
        TAGS_1256.append(FZ54Tag.T1254_CLIENT_ADDRESS, "Адрес");
    }

    public boolean isExist() {
        return mExist;
    }

    public ClientData(){
        super();
        mTagId=FZ54Tag.T1256_CLIENT_DATA;
    }


    /**
     * Установить данные клиента
     *
     * @param name            - имя клиента
     * @param inn             - ИНН клиента
     * @param birthday        - день рождения клиента в формате ЦЦ.ЦЦ.ЦЦЦЦ
     * @param citizenship     - гражданство клиента
     * @param identityDocCode - код вида документа, удостоверяющего личность
     * @param identityDocData - данные документа, удостоверяющего личность
     * @param address         - адрес клиента
     * @return - успех или не успех установки данных после проверки валидности
     */

    public boolean setData(String name, String inn, String birthday, String citizenship,
                           IdentifyDocumentCodeE identityDocCode, String identityDocData, String address) {


        mExist = check(inn, birthday, citizenship, identityDocData, address);
        if (mExist) {
            add(FZ54Tag.T1227_CLIENT_NAME, name);
            add(FZ54Tag.T1228_CLIENT_INN, formatInn(inn));
            add(FZ54Tag.T1243_CLIENT_BIRTHDAY, birthday);
            add(FZ54Tag.T1244_CLIENT_CITIZENSHIP, formatCitizenship(citizenship));
            add(FZ54Tag.T1245_CLIENT_IDENTITY_DOC_CODE, identityDocCode.code);
            add(FZ54Tag.T1246_CLIENT_IDENTITY_DOC_DATA, identityDocData);
            add(FZ54Tag.T1254_CLIENT_ADDRESS, address);
        }
        return mExist;
    }

    /**
     * @return имя
     */
    public String getName() {
        return getTagString(FZ54Tag.T1227_CLIENT_NAME);
    }

    /**
     * @return ИНН
     */
    public String getInn() {
        return getTagString(FZ54Tag.T1228_CLIENT_INN);
    }

    /**
     * @return День рождения
     */
    public String getBirthday() {
        return getTagString(FZ54Tag.T1243_CLIENT_BIRTHDAY);
    }

    /**
     * @return гражданство
     */
    public String getCitizenship() {
        return getTagString(FZ54Tag.T1244_CLIENT_CITIZENSHIP);
    }

    /**
     * @return код документа
     */
    public IdentifyDocumentCodeE getDocCode() {
    	return IdentifyDocumentCodeE.fromCode(getTagString(FZ54Tag.T1245_CLIENT_IDENTITY_DOC_CODE));
    }

    /**
     * @return данные документа
     */
    public String getDocData() {
        return getTagString(FZ54Tag.T1246_CLIENT_IDENTITY_DOC_DATA);
    }

    /**
     * @return адрес
     */
    public String getAddress() {
        return getTagString(FZ54Tag.T1254_CLIENT_ADDRESS);
    }

    private static String formatCitizenship(String citizenship) {
        StringBuilder citizenshipBuilder = new StringBuilder(citizenship);
        while (citizenshipBuilder.length() < 3) citizenshipBuilder.append(" ");
        citizenship = citizenshipBuilder.toString();
        return citizenship;
    }

    private static boolean checkCitizenship(String cs) {
        return checkStringNumbersOrSpaces(cs) && cs.length() <= MAX_CITIZENSHIP_LEN;
    }

    public boolean check(String inn, String birthday, String citizenship,
                          String identityDocData, String address) {
        return checkINN(inn) &&
                checkDate(birthday) &&
                checkCitizenship(citizenship) &&
                identityDocData.length() <= MAX_DOC_DATA_LEN &&
                address.length() <= MAX_CLIENT_ADDRESS_LEN;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        p.writeInt(mExist ? 1 : 0);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        mExist = p.readInt() == 1;
    }

    public static final Parcelable.Creator<ClientData> CREATOR = new Parcelable.Creator<ClientData>() {
        @Override
        public ClientData createFromParcel(Parcel p) {
            ClientData result = new ClientData();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public ClientData[] newArray(int size) {
            return new ClientData[size];
        }
    };
}
