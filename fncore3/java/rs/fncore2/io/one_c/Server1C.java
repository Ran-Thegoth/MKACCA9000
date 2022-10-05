package rs.fncore2.io.one_c;

import static rs.fncore.FZ54Tag.T2105_PROCESS_REQUEST_CODE;
import static rs.fncore.FZ54Tag.T2109_OISM_ANSWER_ITEM_STATUS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


import org.apache.commons.lang3.StringEscapeUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.AgentTypeE;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.OU;
import rs.fncore.data.OfdStatistic;
import rs.fncore.data.ParcelableBytes;
import rs.fncore.data.ParcelableStrings;
import rs.fncore.data.Payment;
import rs.fncore.data.Payment.PaymentTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.fncore.data.Shift;
import rs.fncore.data.TaxModeE;
import rs.fncore2.core.ServiceBinder;
import rs.fncore2.io.BaseThread;
import rs.fncore2.io.Rests;
import rs.log.Logger;

@SuppressLint("DefaultLocale")
@SuppressWarnings("deprecation")
public class Server1C extends BaseThread {

    private static final int SERVER_PORT = 19841;
    private final KKMInfo mKkmInfo = new KKMInfo();

    private static final long DAY_1 = 24 * 60 * 60 * 1000L;
    private static final long DAYS_2 = 2 * 24 * 60 * 60 * 1000L;

    private ServerSocket mSocket;
    @SuppressLint("SimpleDateFormat")
    private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private FNcoreActions getAction(int value) {
        for (FNcoreActions action : FNcoreActions.values()) {
            if (action.ordinal() == value) return action;
        }
        throw new InvalidParameterException("unknown value");
    }

    private final ServiceBinder mBinder;
    private final Rests mRests;

    public Server1C(ServiceBinder b, Context ctx) {
        setName("Server1C");
        mBinder = b;
        SharedPreferences prefs = ctx.getSharedPreferences("rests", Context.MODE_PRIVATE);
        mRests = new Rests(prefs, mKkmInfo);
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        try {
            mSocket = new ServerSocket(SERVER_PORT);
        } catch (IOException ioe) {
            Logger.e(ioe, "Ошибка запуска сервера приложений 1С: ");
            return;
        }

        Logger.i("Сервер приложений 1С запущен на порту %s", SERVER_PORT);

        byte[] cmdBuffer = new byte[2], rBuffer = new byte[3];
        List<String> args = new ArrayList<>();
        List<String> replies = new ArrayList<>();

        while (!isStopped && (mSocket != null) && !mSocket.isClosed()) {
            try {
                args.clear();
                replies.clear();

                Socket s;
                try { // to avoid stacktraces on interrupt server
                    s = mSocket.accept();
                } catch (IOException e) {
                    Logger.e(e, "error socket exception");
                    continue;
                }

                s.setSoTimeout(5000);
                InputStream is = s.getInputStream();
                OutputStream os = s.getOutputStream();
                int read = is.read(cmdBuffer);
                int result = Errors.NO_ERROR;

                if (read == cmdBuffer.length) {
                    int commandInt = (cmdBuffer[0] & 0xff);
                    int nPayloads = (cmdBuffer[1] & 0xff);
                    while (nPayloads-- > 0) {
                        is.read(cmdBuffer);
                        int size = ((cmdBuffer[0] & 0xFF) << 8)
                                | (cmdBuffer[1] & 0xFF);
                        byte[] payload = new byte[size];
                        int offset = 0;
                        while (offset < size) {
                            offset += is.read(payload, offset, size - offset);
                        }
                        args.add(new String(payload));
                    }
                    try {
                        result = mBinder.readKKMInfo(mKkmInfo);

                        Logger.i("Command: %s", commandInt);
                        for (String ss : args) Logger.i("arg: %s", ss);

                        FNcoreActions command = getAction(commandInt);
                        switch (command) {
                            case ACTION_GET_KKT_INFO:
                                replies.add(serializeInfo(mKkmInfo));
                                break;
                            case ACTION_DO_TEST:
                                result = Errors.NO_ERROR;
                                break;

                            case ACTION_OPEN_SHIFT:
                            case ACTION_CLOSE_SHIFT:
                                Shift shift = new Shift();

                                OU operator = null;
                                if (args.size() > 0) operator = deserializeOU(args.get(0));
                                if (operator == null) operator = new OU();

                                if (command == FNcoreActions.ACTION_OPEN_SHIFT) {
                                    result = mBinder.openShift(operator, shift, null);
                                } else {
                                    result = mBinder.closeShift(operator, shift, null);
                                }

                                if (result == Errors.NO_ERROR) {
                                    if (shift.isOpen()) mRests.clear();
                                    replies.add(serializeShift(shift));
                                    replies.add(String.valueOf(shift.getNumber()));
                                    replies.add(String.valueOf(shift.signature().getFdNumber()));
                                }
                                break;

                            case ACTION_DO_CASH:
                                if (mKkmInfo.getShift().isOpen()) {
                                    BigDecimal sum = BigDecimal.ZERO;

                                    operator = null;
                                    if (args.size() > 0) operator = deserializeOU(args.get(0));
                                    if (operator == null) operator = new OU();

                                    if (args.size() > 1) {
                                        try {
                                            sum = new BigDecimal(args.get(1).replace(",", "."));
                                        } catch (NumberFormatException nfe) {
                                            Logger.e(nfe, "deserializeOU exc: ");
                                        }
                                    }
                                    result = mBinder.putOrWithdrawCash(sum.doubleValue(), operator, null);
                                } else
                                    result = Errors.INVALID_SHIFT_STATE;
                                break;

                            case ACTION_GET_STATUS:
                                OfdStatistic stat = new OfdStatistic();
                                result = mBinder.updateOfdStatistic(stat);

                                if (result == Errors.NO_ERROR) {
                                    replies.add(serializeOfdStatistic(stat));
                                    replies.add(String.valueOf(mKkmInfo
                                            .getLastDocument().getNumber()));
                                    replies.add(String.valueOf(mKkmInfo.getShift()
                                            .getNumber()));
                                    if (mKkmInfo.getShift().isOpen()
                                            && System.currentTimeMillis()
                                            - mKkmInfo.getShift().getWhenOpen() >= DAY_1)
                                        replies.add("3");
                                    else
                                        replies.add(String.valueOf((mKkmInfo.getShift()
                                                .isOpen() ? 2 : 1)));
                                }
                                break;

                            case ACTION_DO_X_REPORT: {
                            	FNCounters c = new FNCounters();
                                result =  mBinder.getFNCounters(c, true);
                                if(result == Errors.NO_ERROR)
                                	mBinder.doPrint(mBinder.getPF(c));
                            }
                                break;

                            case ACTION_CHANGE_KKT_SETTINGS:
                            case ACTION_REGISTER_KKT:
                            case ACTION_ARCHIVE_KKT:
                                result = -6;
                                break;

                            case ACTION_DO_REPORT:
                                operator = null;
                                if (args.size() > 0) operator = deserializeOU(args.get(0));
                                if (operator == null) operator = new OU();

                                FiscalReport rep = new FiscalReport();
                                result = mBinder.requestFiscalReport(operator, rep, null);
                                if (result == Errors.NO_ERROR) replies.add(serializeReport(rep));
                                break;

                            case ACTION_DO_CHECK:
                            case ACTION_DO_CHECK_PRINT:
                                SellOrder1C order = SellOrder1C.decode(args.get(0), mBinder);
                                result = mBinder.doSellOrder(order, order.casier,
                                        order,
                                        command == FNcoreActions.ACTION_DO_CHECK_PRINT,
                                        null, null, null, order.footer);
                                if (result == Errors.NO_ERROR) {
                                    //TODO: remove
                                    for (PaymentTypeE p : PaymentTypeE.values()) {
                                        Payment pm = order.getPaymentByType(p);
                                        if (pm != null) {
                                            if (order.getType() == OrderTypeE.INCOME || order.getType() == OrderTypeE.RETURN_OUTCOME) {
                                                mRests.INCOME[p.ordinal()] = mRests.INCOME[p.ordinal()].add(pm.getValue());
                                                if (p == PaymentTypeE.CASH) {
                                                    mRests.INCOME[p.ordinal()] = mRests.INCOME[p.ordinal()].add(order.getRefund());
                                                    mRests.OUTCOME[p.ordinal()] = mRests.OUTCOME[p.ordinal()].add(order.getRefund());
                                                }
                                            } else {
                                                mRests.OUTCOME[p.ordinal()] = mRests.OUTCOME[p.ordinal()].add(pm.getValue());
                                                if (p == PaymentTypeE.CASH) {
                                                    mRests.OUTCOME[p.ordinal()] = mRests.OUTCOME[p.ordinal()].add(order.getRefund());
                                                    mRests.INCOME[p.ordinal()] = mRests.INCOME[p.ordinal()].add(order.getRefund());
                                                }
                                            }
                                        }
                                    }
                                    mRests.store();
                                    //TODO: remove
                                    replies.add(String.valueOf(order.signature().getFdNumber()));
                                    replies.add(String.valueOf(order.signature().getFpd()));
                                    replies.add(String.valueOf(mKkmInfo.getShift().getNumber()));
                                    replies.add(order.getFnsUrl());
                                }

                                break;
                            case ACTION_DO_CORRECTION:
                                Correction1C cor = Correction1C.decode(args.get(0));
                                result = mBinder.doCorrection(cor, cor.casier,
                                        cor, null);
                                if (result == Errors.NO_ERROR) {
                                    //TODO: remove
                                    for (PaymentTypeE p : PaymentTypeE.values()) {
                                        Payment pm = cor.getPaymentByType(p);
                                        if (pm != null) {
                                            if (cor.getOrderType() == OrderTypeE.INCOME || cor.getOrderType() == OrderTypeE.RETURN_OUTCOME)
                                                mRests.INCOME[p.ordinal()] = mRests.INCOME[p.ordinal()].add(pm.getValue());
                                            else
                                                mRests.OUTCOME[p.ordinal()] = mRests.OUTCOME[p.ordinal()].add(pm.getValue());
                                        }
                                    }
                                    mRests.store();
                                    //TODO: remove
                                    replies.add(String.valueOf(cor.signature().getFdNumber()));
                                    replies.add(String.valueOf(cor.signature().getFpd()));
                                    replies.add(String.valueOf(mKkmInfo.getShift().getNumber()));
                                    replies.add(cor.getTag(FZ54Tag.T1060_FNS_URL).asString());
                                }
                                break;
                            case ACTION_DO_CHECK_COPY:
                            case ACTION_PRINT_AGAIN:
                                result = mBinder.printExistingDocument(
                                        Integer.parseInt(args.get(0)),
                                        new ParcelableStrings(), true,
                                        new ParcelableBytes());
                                break;
                            case ACTION_DO_PRINT:
                                mBinder.doPrint(decodeText(args.get(0)));
                                break;
                            case ACTION_OPEN_SESSION_REGISTRATION_KMN:
                            case ACTION_CLOSE_SESSION_REGISTRATION_KMN:
                                result = Errors.NO_ERROR;
                                break;
                            case ACTION_GET_PROCESSING_KM_RESULT:    
                            case ACTION_REQUEST_KM: {
                                SellItem markItem = SellItem1C.decodeMarkedItem(args.get(0)); //TODO - store GUID
                                result = mBinder.checkMarkingItem(markItem);
                                replies.add(serializeRequestKM(markItem));
                                break;
                            }
                            case ACTION_CONFIRM_KM: 
                            	result = mBinder.confirmMarkingItem(SellItem1C.decodeMarkedItem(args.get(0)), true);
                                break;
                            default: {
                                String params = "";
                                if (args.size() > 0) params = args.get(0);
                                Logger.e("unknown 1C action: %s, params: %s", command, params);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Logger.e(e, "Exec command exc, command=%s", commandInt);
                        result = Errors.SYSTEM_ERROR;
                    }

                    if (result != 0) {
                        Logger.e("Exec command error: %s", Integer.toHexString(result));
                    }

                    rBuffer[0] = (byte) (commandInt & 0xff);
                    rBuffer[1] = (byte) (result & 0xff);
                    rBuffer[2] = (byte) (replies.size() & 0xff);
                    os.write(rBuffer);

                    for (String reply : replies) {
                        Logger.d("reply: %s", reply);
                        byte[] payload = reply.getBytes();
                        cmdBuffer[0] = (byte) ((payload.length >> 8) & 0xFF);
                        cmdBuffer[1] = (byte) (payload.length & 0xFF);
                        os.write(cmdBuffer);
                        os.write(payload);
                    }
                    os.flush();
                }
                s.close();
            } catch (IOException ioe) {
                Logger.e(ioe, "1C IO exc");
                break;
            }
        }
        Logger.d("Сервер 1С приложений остановлен");
    }

    protected void unblockWait() {
    }

    @Override
    public void interrupt() {
        if (mSocket != null)
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException ioe) {
                Logger.e(ioe, "Close server socket exc: ");
            }
        super.interrupt();
    }

    private String serializeInfo(KKMInfo info) {
        String result = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><Parameters ";
        result += "KKTNumber=\"" + info.getKKMNumber() + "\" ";
        result += "KKTSerialNumber=\"" + info.getKKMSerial() + "\" ";
        result += "Fiscal=\"" + (info.isFNActive() ? "true" : "false") + "\" ";

        result += "FFDVersionFN=\"" + info.getFFDProtocolVersion().name + "\" ";
        result += "FFDVersionKKT=\"" + info.getFFDProtocolVersion().name + "\" ";
        result += "FNSerialNumber=\"" + info.getFNNumber() + "\" ";
        if (info.isFNActive()) {
            result += "DocumentNumber=\"" + info.signature().getFdNumber() + "\" ";
            result += "DateTime=\"" + DF.format(new Date(info.signature().signDate())) + "\" ";
        }
        result += "OrganizationName=\"" + StringEscapeUtils.escapeXml(info.getOwner().getName()) + "\" ";
        result += "VATIN=\"" + info.getOwner().getINNtrimZ() + "\" ";
        result += "AddressSettle=\"" + StringEscapeUtils.escapeXml(info.getLocation().getAddress()) + "\" ";
        result += "PlaceSettle=\"" + StringEscapeUtils.escapeXml(info.getLocation().getPlace()) + "\" ";
        result += "DataEncryption=\"" + (info.isEncryptionMode() ? "true" : "false") + "\" ";
        result += "SaleExcisableGoods=\"" + (info.isExcisesMode() ? "true" : "false") + "\" ";
        result += "SignOfGambling=\"" + (info.isGamblingMode() ? "true" : "false") + "\" ";
        result += "SignOfLottery=\"" + (info.isLotteryMode() ? "true" : "false") + "\" ";

        StringBuilder sTm = new StringBuilder();
        for (AgentTypeE type : info.getAgentType()) {
            if (sTm.length() > 0)
                sTm.append(",");
            sTm.append(String.valueOf(type.bVal));
        }

        if (sTm.length() > 0)
            result += "SignOfAgent=\"" + sTm + "\" ";
        result += "FNSWebSite=\"" + info.getFNSUrl() + "\" ";
        result += "SenderEmail=\"" + StringEscapeUtils.escapeXml(info.getSenderEmail()) + "\" ";

        sTm = new StringBuilder();
        for (TaxModeE m : info.getTaxModes()) {
            if (sTm.length() > 0)
                sTm.append(",");
            sTm.append(String.valueOf(m.bVal));
        }
        if (sTm.length() > 0)
            result += "TaxVariant=\"" + sTm + "\" ";
        result += "OfflineMode=\"" + (info.isOfflineMode() ? "true" : "false") + "\" ";
        result += "ServiceSign=\"" + (info.isServiceMode() ? "true" : "false") + "\" ";
        result += "BSOSing=\"" + (info.isBSOMode() ? "true" : "false") + "\" ";
        result += "CalcOnlineSign=\"" + (info.isInternetMode() ? "true" : "false") + "\" ";
        result += "AutomaticMode=\"" + (info.isAutomatedMode() ? "true" : "false") + "\" ";
        if (info.isAutomatedMode())
            result += "AutomaticNumber=\"" + info.getAutomateNumber() + "\" ";
        if (!info.isOfflineMode()) {
            result += "OFDOrganizationName=\"" + StringEscapeUtils.escapeXml(info.ofd().getName()) + "\" ";
            result += "OFDVATIN=\"" + info.ofd().getINNtrimZ() + "\" ";
        }
        result += "/>";
        return result;
    }

    private OU deserializeOU(String str) {
        OU ou = new OU();
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new InputStreamReader(new ByteArrayInputStream(str.getBytes())));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    if ("Parameters".equals(parser.getName())) {
                        String s = parser.getAttributeValue(null, "CashierName");
                        if (s != null)
                            ou.setName(StringEscapeUtils.unescapeXml(s));
                        s = parser.getAttributeValue(null, "CashierVATIN");
                        if (s != null && !s.isEmpty())
                            ou.setINN(s);
                    }

                }
                event = parser.next();
            }
        } catch (Exception e) {
            Logger.e(e, "deserializeOU exc");
        }
        return ou;
    }

    private String serializeShift(Shift wd) {
        String result = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><OutputParameters> <Parameters ";
        result += "MemoryOverflowFN=\"" + wd.getFNWarnings().isMemoryFull99() + "\" ";
        result += "UrgentReplacementFN=\"" + wd.getFNWarnings().isReplaceUrgent3Days() + "\" ";
        result += "ResourcesExhaustionFN=\"" + wd.getFNWarnings().isReplace30Days() + "\" ";
        result += "BacklogDocumentsCounter=\"" + wd.getOFDStatistic().getUnsentDocumentCount() + "\" ";
        if (wd.getOFDStatistic().getUnsentDocumentCount() > 0) {
            result += "OFDtimeout=\""
                    + (System.currentTimeMillis() - wd.getOFDStatistic().getFirstUnsentDate() >= DAYS_2) + "\" ";
            Date d = new Date(wd.getOFDStatistic().getFirstUnsentDate());
            result += "BacklogDocumentFirstDateTime=\"" + DF.format(d) + "\" ";
            result += "BacklogDocumentFirstNumber=\"" + wd.getOFDStatistic().getFirstUnsentNumber() + "\" ";
        } else
            result += "OFDtimeout=\"false\"";
        result += "/></OutputParameters>";
        return result;
    }

    private String serializeOfdStatistic(OfdStatistic s) {
        String result = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><OutputParameters>";
        result += "<Parameters BacklogDocumentsCounter=\"" + s.getUnsentDocumentCount() + "\" ";
        if (s.getUnsentDocumentCount() > 0) {
            result += "BacklogDocumentFirstNumber=\"" + s.getFirstUnsentNumber() + "\" ";
            result += "BacklogDocumentFirstDateTime=\"" + DF.format(s.getFirstUnsentDate()) + "\" ";

        }
        result += " ShiftState=\"";
        if (mKkmInfo.getShift().isOpen()
                && System.currentTimeMillis()
                - mKkmInfo.getShift().getWhenOpen() >= DAY_1)
            result += "3";
        else
            result += String.valueOf((mKkmInfo.getShift()
                    .isOpen() ? 2 : 1));
        result += "\" ";

        result += "/></OutputParameters>";
        return result;
    }

    private String serializeReport(FiscalReport rep) {
        String result = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><OutputParameters> <Parameters ";
        int warnings = 0; // TODO !!!!
        result += "BacklogDocumentsCounter=\"" + rep.getOFDStatistic().getUnsentDocumentCount() + "\" ";
        if (rep.getOFDStatistic().getUnsentDocumentCount() > 0) {
            result += "BacklogDocumentFirstNumber=\"" + rep.getOFDStatistic().getFirstUnsentNumber() + "\" ";
            result += "BacklogDocumentFirstDateTime=\"" + DF.format(rep.getOFDStatistic().getFirstUnsentDate()) + "\" ";
        }
        result += "MemoryOverflowFN=\"" + ((warnings & 1) == 1) + "\" ";
        result += "UrgentReplacementFN=\"" + ((warnings & 4) == 4) + "\" ";
        result += "ResourcesExhaustionFN=\"" + ((warnings & 2) == 2) + "\" ";
        if (rep.getOFDStatistic().getUnsentDocumentCount() > 0)
            result += "OFDtimeout=\""
                    + (System.currentTimeMillis() - rep.getOFDStatistic().getFirstUnsentDate() >= DAYS_2) + "\" ";
        else
            result += "OFDtimeout=\"false\"";
        result += "/></OutputParameters>";
        return result;
    }

    private String serializeRequestKM(SellItem item) {
        String result = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><OutputParameters>";
        result += "<RequestKMResult Checking=\"" + item.getMarkingCode().getCheckResult().codeProcessed + "\" ";
        result += "CheckingResult=\"" + item.getMarkingCode().getCheckResult().codeChecked + "\" ";
        result += "/></OutputParameters>";
        return result;
    }

    private String serializeProcessingKMResult(SellItem item) {
        String result = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><OutputParameters>";
        result += "<ProcessingKMResult GUID=\"" + item.getMarkingCode().getCheckResult().codeProcessed + "\" ";
        result += "Result=\"" + item.getMarkingCode().getCheckResult().bVal + "\" ";
        result += "StatusInfo=\"" + item.getTagString(T2105_PROCESS_REQUEST_CODE) + "\" ";
        result += "HandleCode=\"" + item.getTagString(T2109_OISM_ANSWER_ITEM_STATUS) + "\" ";
        result += "/></OutputParameters>";
        return result;
    }

    private String decodeText(String str) {
        String result = "";
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new InputStreamReader(new ByteArrayInputStream(str.getBytes())));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    if ("TextString".equals(parser.getName())) {
                        String s = parser.getAttributeValue(null, "Text");
                        if (s != null) {
                            if (!result.isEmpty())
                                result += "\n";
                            result += StringEscapeUtils.unescapeXml(s);
                        }
                    } else if ("Barcode".equals(parser.getName())) {
                        String s = parser.getAttributeValue(null, "Barcode");
                        String type = parser.getAttributeValue(null, "BarcodeType");
                        if (type == null)
                            type = "ean13";
                        else
                            type = type.toLowerCase();
                        if (s != null) {
                            if (!result.isEmpty())
                                result += "\n";
                            result += "{\\barcode width:90%;height:80;type:" + type + "\\" + s + "}";
                        }
                    }

                }
                event = parser.next();
            }

        } catch (Exception e) {
            Logger.e(e, "decodeText exc");
        }
        return result;
    }

    private String decodeRequestKM(String str) {
        String result = "";
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new InputStreamReader(new ByteArrayInputStream(str.getBytes())));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    if ("TextString".equals(parser.getName())) {
                        String s = parser.getAttributeValue(null, "Text");
                        if (s != null) {
                            if (!result.isEmpty())
                                result += "\n";
                            result += StringEscapeUtils.unescapeXml(s);
                        }
                    } else if ("Barcode".equals(parser.getName())) {
                        String s = parser.getAttributeValue(null, "Barcode");
                        String type = parser.getAttributeValue(null, "BarcodeType");
                        if (type == null)
                            type = "ean13";
                        else
                            type = type.toLowerCase();
                        if (s != null) {
                            if (!result.isEmpty())
                                result += "\n";
                            result += "{\\barcode width:90%;height:80;type:" + type + "\\" + s + "}";
                        }
                    }

                }
                event = parser.next();
            }

        } catch (Exception e) {
            Logger.e(e, "decodeText exc");
        }
        return result;
    }

}
