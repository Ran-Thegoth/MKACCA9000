package rs.fncore.data;

import static rs.utils.Utils.formatInn;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import rs.fncore.FZ54Tag;

/**
 * Данные агента
 *
 * @author nick
 */
public class AgentData extends Tag implements IReableFromParcel {

    /**
     * Поля тега 1223 (для внутреннего использования)
     */
    public static final SparseArray<String> TAGS_1223 = new SparseArray<>();
    static {
        TAGS_1223.append(FZ54Tag.T1005_TRANSFER_OPERATOR_ADDR, "Адрес оператора перевода");
        TAGS_1223.append(FZ54Tag.T1016_TRANSFER_OPERATOR_INN, "ИНН оператора перевода");
        TAGS_1223.append(FZ54Tag.T1026_TRANSFER_OPERATOR_NAME, "Наименование оператора перевода");
        TAGS_1223.append(FZ54Tag.T1044_TRANSFER_OPERATOR_ACTION, "Операция платежного агента");
        TAGS_1223.append(FZ54Tag.T1073_PAYMENT_AGENT_PHONE, "Телефон платежного агента");
        TAGS_1223.append(FZ54Tag.T1074_PAYMENT_OPERATOR_PHONE, "Телефон оператора по приему платежей");
        TAGS_1223.append(FZ54Tag.T1075_TRANSFER_OPERATOR_PHONE, "Телефон оператора перевода");
    }

    /**
     * Поля тега 1224 (для внутреннего использования)
     */
    public static final SparseArray<String> TAGS_1224 = new SparseArray<>();
    static{
        TAGS_1224.append(FZ54Tag.T1171_SUPPLIER_PHONE,"Телефон поставщика");
        TAGS_1224.append(FZ54Tag.T1225_SUPPLIER_NAME,"Наименование поставщика");
        TAGS_1224.append(FZ54Tag.T1226_SUPPLIER_INN,"ИНН поставщика");
    }

    private AgentTypeE mType = AgentTypeE.NONE;

    public static boolean isAgentTag(int tag) {
    	for(int i=0;i<TAGS_1223.size();i++)
    		if(TAGS_1223.keyAt(i) == tag) return true;
    	for(int i=0;i<TAGS_1224.size();i++)
    		if(TAGS_1224.keyAt(i) == tag) return true;
    	return false;
    }
    
    
    public AgentData() {
    }

    /**
     * Получить тип агентских услуг
     *
     * @return тип агентских услуг или null если не определена
     */
    public AgentTypeE getType() {
        return mType;
    }

    /**
     * Установить тип агентских услуг
     *
     * @param type тип агентских услуг
     */
    public void setType(AgentTypeE type) {
        mType = type;

    }

    /**
     * Указать телефон поставщика (тег 1171)
     *
     * @param s телефон поставщика (тег 1171)
     */
    public void setProviderPhone(String s) {
        if (s != null && !s.isEmpty()) {
            add(FZ54Tag.T1171_SUPPLIER_PHONE, s);
        } else {
            remove(FZ54Tag.T1171_SUPPLIER_PHONE);
        }
    }

    /**
     * Получить телефон поставщика (тег 1171)
     *
     * @return телефон поставщика (тег 1171)
     */
    public String getProviderPhone() {
        return getTagString(FZ54Tag.T1171_SUPPLIER_PHONE);
    }

    /**
     * Установить наименование поставщика (тег 1225)
     *
     * @param s наименование поставщика (тег 1225)
     */
    public void setProviderName(String s) {
        if (s != null && !s.isEmpty()) {
            add(FZ54Tag.T1225_SUPPLIER_NAME, s);
        } else {
            remove(FZ54Tag.T1225_SUPPLIER_NAME);
        }
    }

    /**
     * Получить наименование поставщика (тег 1225)
     *
     * @return наименование поставщика (тег 1225)
     */
    public String getProviderName() {
        return getTagString(FZ54Tag.T1225_SUPPLIER_NAME);
    }

    /**
     * Установить ИНН поставщика (тег 1226)
     *
     * @param s ИНН поставщика (тег 1226)
     */
    public void setProviderINN(String s) {
        if (s != null && !s.isEmpty()) {
            s=formatInn(s);
            add(FZ54Tag.T1226_SUPPLIER_INN, s);
        } else {
            remove(FZ54Tag.T1226_SUPPLIER_INN);
        }
    }

    /**
     * Получить ИНН поставщика (тег 1226)
     *
     * @return ИНН поставщика (тег 1226)
     */
    public String getProviderINN() {
        return getTagString(FZ54Tag.T1226_SUPPLIER_INN);
    }

    /**
     * Указать телефон агента (тег 1073)
     *
     * @param s телефон агента (тег 1073)
     */
    public void setAgentPhone(String s) {
        if (s != null && !s.isEmpty()) {
            add(FZ54Tag.T1073_PAYMENT_AGENT_PHONE, s);
        } else {
            remove(FZ54Tag.T1073_PAYMENT_AGENT_PHONE);
        }
    }

    /**
     * Получить телефон агента (тег 1073)
     *
     * @return телефон агента (тег 1073)
     */
    public String getAgentPhone() {
        return getTagString(FZ54Tag.T1073_PAYMENT_AGENT_PHONE);
    }

    /**
     * Указать имя агента (тег 1044)
     *
     * @param s имя агента (тег 1044)
     */
    public void setAgentOperation(String s) {
        if (s != null && !s.isEmpty()) {
            add(FZ54Tag.T1044_TRANSFER_OPERATOR_ACTION,s);
        } else {
            remove(FZ54Tag.T1044_TRANSFER_OPERATOR_ACTION);
        }
    }

    /**
     * Получить имя агента (тег 1044)
     *
     * @return имя агента (тег 1044)
     */
    public String getAgentOperation() {
        return getTagString(FZ54Tag.T1044_TRANSFER_OPERATOR_ACTION);
    }


    /**
     * Установить ИНН оператора перевода (тег 1016)
     *
     * @param s ИНН оператора перевода (тег 1016)
     */
    public void setOperatorINN(String s) {
        if (s != null && !s.isEmpty()) {
            s = formatInn(s);
            add(FZ54Tag.T1016_TRANSFER_OPERATOR_INN, s);
        } else {
            remove(FZ54Tag.T1016_TRANSFER_OPERATOR_INN);
        }
    }

    /**
     * Получить ИНН оператора перевода (тег 1016)
     *
     * @return ИНН оператора перевода (тег 1016)
     */
    public String getOperatorINN() {
        return getTagString(FZ54Tag.T1016_TRANSFER_OPERATOR_INN);
    }


    /**
     * Установить адрес оператора перевода (тег 1005)
     *
     * @param s адрес оператора перевода (тег 1005)
     */
    public void setOperatorAddress(String s) {
        if (s != null && !s.isEmpty()) {
            add(FZ54Tag.T1005_TRANSFER_OPERATOR_ADDR, s);
        } else {
            remove(FZ54Tag.T1005_TRANSFER_OPERATOR_ADDR);
        }
    }

    /**
     * Получить адрес оператора перевода (тег 1005)
     *
     * @return адрес оператора перевода (тег 1005)
     */
    public String getOperatorAddress() {
        return getTagString(FZ54Tag.T1005_TRANSFER_OPERATOR_ADDR);
    }

    /**
     * Установить наименование оператора перевода (тег 1026)
     *
     * @param s наименование оператора перевода (тег 1026)
     */
    public void setOperatorName(String s) {
        if (s != null && !s.isEmpty()) {
            add(FZ54Tag.T1026_TRANSFER_OPERATOR_NAME, s);
        } else {
            remove(FZ54Tag.T1026_TRANSFER_OPERATOR_NAME);
        }
    }

    /**
     * Получить наименование оператора перевода (тег 1026)
     *
     * @return наименование оператора перевода (тег 1026)
     */
    public String getOperatorName() {
        return getTagString(FZ54Tag.T1026_TRANSFER_OPERATOR_NAME);
    }

    /**
     * Установить телефон оператора перевода (тег 1075)
     *
     * @param s телефон оператора перевода (тег 1075)
     */
    public void setOperatorPhone(String s) {
        if (s != null && !s.isEmpty()) {
            add(FZ54Tag.T1075_TRANSFER_OPERATOR_PHONE, s);
        } else {
            remove(FZ54Tag.T1075_TRANSFER_OPERATOR_PHONE);
        }
    }

    /**
     * Получить телефон оператора перевода (тег 1075)
     *
     * @return телефон оператора перевода (тег 1075)
     */
    public String getOperatorPhone() {
        return getTagString(FZ54Tag.T1075_TRANSFER_OPERATOR_PHONE);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Tag packAgent() {
        if (getType() == AgentTypeE.NONE) return null;
        Tag tag = new Tag(FZ54Tag.T1223_AGENT_DATA_TLV);

        for(int i = 0; i < TAGS_1223.size(); i++) {
            int tagId=TAGS_1223.keyAt(i);
            tag.add(getTag(tagId));
        }
        if (tag.getChilds().isEmpty()) return null;
        return tag;
    }

    public Tag packSupplier() {
        if (getType() == AgentTypeE.NONE) return null;
        Tag tag = new Tag(FZ54Tag.T1224_SUPPLIER_DATA_TLV);

        for(int i = 0; i < TAGS_1224.size(); i++) {
            int tagId=TAGS_1224.keyAt(i);
            tag.add(getTag(tagId));
        }

        if (tag.size() == 0) return null;
        return tag;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        super.writeToParcel(p, flags);
        p.writeByte(mType.bVal);
    }

    @Override
    public void readFromParcel(Parcel p) {
        super.readFromParcel(p);
        byte t = p.readByte();
        mType = AgentTypeE.fromByte(t);
    }

    public static final Parcelable.Creator<AgentData> CREATOR = new Parcelable.Creator<AgentData>() {
        @Override
        public AgentData createFromParcel(Parcel p) {
            AgentData result = new AgentData();
            result.readFromParcel(p);
            return result;
        }

        @Override
        public AgentData[] newArray(int size) {
            return new AgentData[size];
        }
    };
}
