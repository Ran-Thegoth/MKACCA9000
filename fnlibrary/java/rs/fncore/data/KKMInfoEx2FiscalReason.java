package rs.fncore.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Причины перерегистрации ККМ согласно ФФД 1.1+
 * 
 * @author nick
 *
 */
public class KKMInfoEx2FiscalReason {
    public static long getExtendedFiscalReasonChange(KKMInfo.FiscalReasonE reason, KKMInfo oldInfo, KKMInfo newInfo) {
        long res = 0;

        if (reason == KKMInfo.FiscalReasonE.REPLACE_FN) {
            return  FiscalReasonExtE.REPLACE_FN.val;
        } else if (reason == KKMInfo.FiscalReasonE.CHANGE_OFD) {
            res |= FiscalReasonExtE.CHANGE_OFD.val;
        }

        if (!oldInfo.ofd().equals(newInfo.ofd())) {
            res |= FiscalReasonExtE.CHANGE_OFD.val;
        }

        if (!oldInfo.getOwner().equals(newInfo.getOwner())) {
            res |= FiscalReasonExtE.CHANGE_USER.val;
        }

        if (!oldInfo.getLocation().equals(newInfo.getLocation())) {
            res |= FiscalReasonExtE.CHANGE_KKT_LOCATION.val;
        }

        if (oldInfo.isOfflineMode() && !newInfo.isOfflineMode()) {
            res |= FiscalReasonExtE.CHANGE_KKM_TO_ONLINE.val;
        }

        if (!oldInfo.isOfflineMode() && newInfo.isOfflineMode()) {
            res |= FiscalReasonExtE.CHANGE_KKM_TO_OFFLINE.val;
        }

/*        if (!oldInfo.getServiceVersion().equals(newInfo.getServiceVersion())) {
            res |= FiscalReasonExtE.CHANGE_KKM_VER.val;
        } */

        if (TaxModeE.toByteArray(oldInfo.getTaxModes()) != TaxModeE.toByteArray(newInfo.getTaxModes())) {
            res |= FiscalReasonExtE.CHANGE_TAX.val;
        }

        if (!oldInfo.getAutomateNumber().equals(newInfo.getAutomateNumber())) {
            res |= FiscalReasonExtE.CHANGE_AUTO_NUM.val;
        }

        if (oldInfo.isAutomatedMode() && !newInfo.isAutomatedMode()) {
            res |= FiscalReasonExtE.CHANGE_TO_MANUAL.val;
        }

        if (!oldInfo.isAutomatedMode() && newInfo.isAutomatedMode()) {
            res |= FiscalReasonExtE.CHANGE_TO_AUTO.val;
        }

        if (!oldInfo.isBSOMode() && newInfo.isBSOMode()) {
            res |= FiscalReasonExtE.CHANGE_TO_BSO.val;
        }

        if (oldInfo.isBSOMode() && !newInfo.isBSOMode()) {
            res |= FiscalReasonExtE.CHANGE_TO_NON_BSO.val;
        }

        if (!oldInfo.isInternetMode() && newInfo.isInternetMode()) {
            res |= FiscalReasonExtE.CHANGE_BSO_TO_INTERNET.val;
        }

        if (oldInfo.isInternetMode() && !newInfo.isInternetMode()) {
            res |= FiscalReasonExtE.CHANGE_INTERNET_TO_BSO.val;
        }

        if (oldInfo.isAgent() && !newInfo.isAgent()) {
            res |= FiscalReasonExtE.CHANGE_TO_NON_AGENT.val;
        }

        if (!oldInfo.isAgent() && newInfo.isAgent()) {
            res |= FiscalReasonExtE.CHANGE_TO_AGENT.val;
        }

        if (oldInfo.isGamblingMode() && !newInfo.isGamblingMode()) {
            res |= FiscalReasonExtE.CHANGE_TO_NON_GAMBLING.val;
        }

        if (!oldInfo.isGamblingMode() && newInfo.isGamblingMode()) {
            res |= FiscalReasonExtE.CHANGE_TO_GAMBLING.val;
        }

        if (oldInfo.isLotteryMode() && !newInfo.isLotteryMode()) {
            res |= FiscalReasonExtE.CHANGE_TO_NON_LOTTERY.val;
        }

        if (!oldInfo.isLotteryMode() && newInfo.isLotteryMode()) {
            res |= FiscalReasonExtE.CHANGE_TO_LOTTERY.val;
        }

        if (oldInfo.getFFDProtocolVersion() != newInfo.getFFDProtocolVersion()) {
            res |= FiscalReasonExtE.CHANGE_FFD.val;
        }

        if (res == 0) {
            res |= FiscalReasonExtE.CHANGE_OTHERS.val;
        }
        return res;
    }

    /**
     * Причина регистрации
     */
    public enum FiscalReasonExtE {
        REPLACE_FN(0, "1", "Замена фискального накопителя"),
        CHANGE_OFD(1, "2", "Замена оператора фискальных данных"),
        CHANGE_USER(2, "3", "Изменение наименования пользователя контрольно-" +
                "кассовой техники"),
        CHANGE_KKT_LOCATION(3, "4", "Изменение адреса и (или) места установки " +
                "(применения) контрольно-кассовой техники"),

        CHANGE_KKM_TO_ONLINE(4, "5", "Перевод ККТ из автономного режима в режим " +
                "передачи данных"),

        CHANGE_KKM_TO_OFFLINE(5, "6", "Перевод ККТ из режима передачи данных в " +
                "автономный режим"),

        CHANGE_KKM_VER(6, "7", "Изменение версии модели ККТ"),

        CHANGE_TAX(7, "8", "Изменение перечня систем налогообложения," +
                "применяемых при осуществлении расчетов"),

        CHANGE_AUTO_NUM(8, "9", "Изменение номера автоматического устройства для" +
                "расчетов, в составе которого применяется ККТ"),

        CHANGE_TO_MANUAL(9, "10", "Перевод ККТ из автоматического режима в" +
                "неавтоматический режим (осуществление расчетов кассиром)"),

        CHANGE_TO_AUTO(10, "11", "Перевод ККТ из неавтоматического режима" +
                "(осуществление расчетов кассиром) в автоматический режим"),

        CHANGE_TO_BSO(11, "12", "Перевод ККТ из режима, не позволяющего формировать" +
                "БСО, в режим, позволяющий формировать БСО"),

        CHANGE_TO_NON_BSO(12, "13", "Перевод ККТ из режима, позволяющего формировать" +
                " БСО,\n" +
                "в режим, не позволяющий формировать БСО"),

        CHANGE_INTERNET_TO_BSO(13, "14", "Перевод ККТ из режима расчетов в сети " +
                "Интернет (позволяющего не печатать кассовый чек и БСО) в режим," +
                "позволяющий печатать кассовый чек и БСО"),

        CHANGE_BSO_TO_INTERNET(14, "15", "Перевод ККТ из режима, позволяющего " +
                "печатать кассовый чек и БСО, в режим расчетов в сети Интернет" +
                "(позволяющего не печатать кассовый чек и БСО)"),

        CHANGE_TO_NON_AGENT(15, "16", "Перевод ККТ из режима, позволяющего оказывать" +
                " услуги" +
                "платежного агента (субагента) или банковского" +
                "платежного агента, в режим, не позволяющий оказывать" +
                "услуги платежного агента (субагента) или банковского" +
                "платежного агента"),

        CHANGE_TO_AGENT(16, "17", "Перевод ККТ из режима, не позволяющего оказывать" +
                "услуги платежного агента (субагента) или банковского" +
                "платежного агента в режим, позволяющий оказывать" +
                "услуги платежного агента (субагента) или банковского" +
                "платежного агента"),

        CHANGE_TO_NON_GAMBLING(17, "18", "Перевод ККТ из режима, позволяющего " +
                "применять " +
                "ККТ" +
                "при приеме ставок и выплате денежных средств в виде" +
                "выигрыша при осуществлении деятельности по" +
                "проведению азартных игр, в режим, не позволяющий" +
                "применять ККТ при приеме ставок и выплате денежных 18средств в виде выигрыша при осуществлении деятельности" +
                "по проведению азартных игр"),

        CHANGE_TO_GAMBLING(18, "19", "Перевод ККТ из режима, не позволяющего " +
                "применять ККТ" +
                "при приеме ставок и выплате денежных средств в виде" +
                "выигрыша при осуществлении деятельности по" +
                "проведению азартных игр, в режим, позволяющий" +
                "применять ККТ при приеме ставок и выплате денежных" +
                "средств в виде выигрыша при осуществлении деятельности" +
                "по проведению азартных игр"),

        CHANGE_TO_NON_LOTTERY(19, "20", "Перевод ККТ из режима, позволяющего " +
                "применять " +
                "ККТ" +
                "при приеме денежных средств при реализации лотерейных" +
                "билетов, электронных лотерейных билетов, приеме" +
                "лотерейных ставок и выплате денежных средств в виде" +
                "выигрыша при осуществлении деятельности по" +
                "проведению лотерей, в режим, не позволяющий применять" +
                "ККТ при приеме денежных средств при реализации" +
                "лотерейных билетов, электронных лотерейных билетов," +
                "приеме лотерейных ставок и выплате денежных средств в" +
                "виде выигрыша при осуществлении деятельности по" +
                "проведению лотерей"),

        CHANGE_TO_LOTTERY(20, "21", "Перевод ККТ из режима, не позволяющего " +
                "применять ККТ\n" +
                "при приеме денежных средств при реализации лотерейных\n" +
                "билетов, электронных лотерейных билетов, приеме\n" +
                "лотерейных ставок и выплате денежных средств в виде\n" +
                "выигрыша при осуществлении деятельности по\n" +
                "проведению лотерей, в режим, позволяющий применять\n" +
                "ККТ при приеме денежных средств при реализации\n" +
                "лотерейных билетов, электронных лотерейных билетов,\n" +
                "приеме лотерейных ставок и выплате денежных средств в\n" +
                "виде выигрыша при осуществлении деятельности по\n" +
                "проведению лотерей"),

        /**
         * Изменение версии ФФД
         */
        CHANGE_FFD(21, "22", "Изменение версии ФФД"),

        /**
         * Иные причины
         */
        CHANGE_OTHERS(31, "32", "Иные причины"),

        ;

        public final long val;
        public final String pName;
        public final String desc;

        private FiscalReasonExtE(int val, String pName, String desc) {
            this.val = (1L << val);
            this.pName = pName;
            this.desc = desc;
        }

        public static Set<FiscalReasonExtE> fromLongArray(long val) {
            HashSet<FiscalReasonExtE> result = new HashSet<>();
            for (FiscalReasonExtE a : values()) {
                if ((val & a.val) == a.val)
                    result.add(a);
            }
            return result;
        }

        public static String fromLongArrayStr(long val){
            Set<FiscalReasonExtE> vals=fromLongArray(val);
            StringBuilder res= new StringBuilder();
            for (FiscalReasonExtE reason : vals){
                if (res.length() > 0) res.append(",");
                res.append(reason.pName);
            }
            return res.toString();
        }
        public static String fromLongArrayDescr(long val){
            Set<FiscalReasonExtE> vals=fromLongArray(val);
            StringBuilder res= new StringBuilder();
            for (FiscalReasonExtE reason : vals){
                if (res.length() > 0) res.append("\n");
                res.append(reason.desc);
            }
            return res.toString();
        }
        
    }
}
