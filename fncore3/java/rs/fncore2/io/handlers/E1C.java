package rs.fncore2.io.handlers;

import org.nanohttpd.util.IHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Base64;
import android.util.Log;

import org.nanohttpd.protocols.http.response.IStatus;

import rs.fncore2.core.Settings;
import rs.fncore2.FNCore;
import rs.fncore2.data.CashWithdraw;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.data.AgentTypeE;
import rs.fncore.data.Correction;
import rs.fncore.data.Correction.CorrectionTypeE;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.IAgentOwner;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.OU;
import rs.fncore.data.OfdStatistic;
import rs.fncore.data.Payment;
import rs.fncore.data.Payment.PaymentTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellItem.ItemPaymentTypeE;
import rs.fncore.data.SellItem.SellItemTypeE;
import rs.fncore.data.SellOrder;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.VatE;
import rs.fncore2.fn.FNManager;
import rs.fncore2.fn.common.FNBaseI;
import rs.fncore2.fn.storage.Transaction;
import rs.fncore2.io.Printing;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import rs.fncore2.utils.DocumentUtils;
import rs.utils.Utils;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;

public class E1C implements IHandler<IHTTPSession, Response> {

	private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>";

	private class KMSession {
		Map<String, SellItem> BY_UUID = new HashMap<>();
		Map<String, SellItem> BY_CODE = new HashMap<>();
		Set<SellItem> CONFIRMED = new HashSet<>();
		SellItem getByUUID(String uuid) { return BY_UUID.get(uuid); }
		SellItem getByCode(String code) { return BY_CODE.get(code); }
		void add(SellItem item, String uuid) {
			BY_UUID.put(uuid, item);
			BY_CODE.put(item.getMarkingCode().getCode(), item);
		}
		boolean isConfirmed(SellItem i) { return CONFIRMED.contains(i); }
	}
	private Map<String,KMSession> KM_SESSIONS = new HashMap<>();
	private SellItem _lastChecked;
	
	private class CheckedItem extends SellItem {
		CheckedItem(SellItem src) {
			readFromParcel(Utils.writeToParcel(src));
		}
		void applyCheckResult(SellItem item) {
			setMarkingCode(item.getMarkingCode());
			mMarkResult = item.getMarkCheckResult();
		}
	}
	
	private class E1CResult implements IStatus {
		private int code;
		private String response;
		private String mime = "text/plain;charset=utf8";
		private byte[] bytes;
		private ByteArrayInputStream is;

		E1CResult(int code) {
			this.code = code;
		}

		@Override
		public String getDescription() {
			return "Ошибка " + code;
		}

		@Override
		public int getRequestStatus() {
			return code;
		}

		public InputStream getStream() {
			if (is == null) {
				bytes = response.getBytes();
				is = new ByteArrayInputStream(bytes);
			}
			return is;
		}

		public long size() {
			getStream();
			return bytes.length;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public Response handle(IHTTPSession input) {
		E1CResult result = new E1CResult(500 + Errors.NOT_IMPLEMENTED);
		result.response = "Не реализовано";
		OU cashier = new OU("Администратор");
		if (input.getMethod() == Method.POST) try {
			XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
			byte b[] = new byte[input.getInputStream().available()];
			input.getInputStream().read(b);
			parser.setInput(new ByteArrayInputStream(b), "UTF-8");
			if (input.getUri().endsWith("GetDataKKT")) {
				getDataKKT(result);
			} else if (input.getUri().endsWith("PrintXReport")) {
				doXReports(result);
			} else if (input.getUri().endsWith("PrintCheckCopy")) {
				String no = input.getParms().get("number");
				try {
					printCopy(result, Integer.parseInt(no));
				} catch (NumberFormatException nfe) {
					result.code = 500 + Errors.DATA_ERROR;
					result.response = "Неверный номер документа";
				}
			} else if (input.getUri().endsWith("GetCurrentStatus"))
				getCurrentStatus(result);
			else if (input.getUri().endsWith("CashInOutcome")) {
				try {
					double sum = Double.parseDouble(input.getParms().get("sum"));
					int evt = XmlPullParser.START_DOCUMENT;
					while (evt != XmlPullParser.END_DOCUMENT) {
						if (evt == XmlPullParser.START_TAG && "Parameters".equals(parser.getName()))
							cashier = parseOU(parser);
						evt = parser.next();
					}
					cashInOutcome(result, sum, cashier);
				} catch (NumberFormatException e) {
					
					result.code = 500 + Errors.DATA_ERROR;
					result.response = "Неверный параметр запроса";
				}
			}
			else if(input.getUri().endsWith("PrintDocument")) {
				Tag doc = new Tag();
				result.code = 500 + Errors.NO_MORE_DATA; 
				result.response = "Неверный номер документа";
				long no = Long.parseLong(input.getParms().get("number"));
				if(FNManager.getInstance().getFN().readDocumentFromTLV(no, doc) == Errors.NO_ERROR) {
					result.response = "Ошибка печати документа";
					String pf = DocumentUtils.getPrintForm(doc);
					if(pf != null) {
						
						result.code = 200;
						result.response = "Выполнено успешно";
						Printing.getInstance().queue(pf);
					}
				}
			}
			else if(input.getUri().endsWith("OpenShift")) {
				int evt = XmlPullParser.START_DOCUMENT;
				while (evt != XmlPullParser.END_DOCUMENT) {
					if (evt == XmlPullParser.START_TAG && "Parameters".equals(parser.getName()))
						cashier = parseOU(parser);
					evt = parser.next();
				}
				openShift(result, cashier);
			}
			else if(input.getUri().endsWith("CloseShift")) {
				int evt = XmlPullParser.START_DOCUMENT;
				while (evt != XmlPullParser.END_DOCUMENT) {
					if (evt == XmlPullParser.START_TAG && "Parameters".equals(parser.getName()))
						cashier = parseOU(parser);
					evt = parser.next();
				}
				closeShift(result, cashier);
			} 
			else if(input.getUri().endsWith("ReportCurrentStatusOfSettlements")) {
				requestReport(result, parser);
			}
			else if(input.getUri().endsWith("PrintTextDocument"))
				printTextDocument(result, parser);
			else if(input.getUri().endsWith("RequestKM")) {
				KMSession session = KM_SESSIONS.get(input.getRemoteIpAddress()); 
				if(session == null) {
					result.code = 500+Errors.SYSTEM_ERROR;
					result.response = "Сессия работы с КМ не открыта";
				} else
					requestKM(result, parser,session);
			}
			else if(input.getUri().endsWith("OpenSessionRegistrationKM")) {
				KM_SESSIONS.remove(input.getRemoteIpAddress());
				synchronized(this) {
					_lastChecked = null;
				}
				KM_SESSIONS.put(input.getRemoteIpAddress(),new KMSession());
				result.code = 200;
				result.response = "Операция выполнена успешно";
			}
			
			else if(input.getUri().endsWith("CloseSessionRegistrationKM")) {
				KM_SESSIONS.remove(input.getRemoteIpAddress());
				FNManager.getInstance().getFN().cancelDocument();
				FNManager.getInstance().getFN().releaseMarkCodes();
				synchronized(this) {
					_lastChecked = null;
				}
				result.code = 200;
				result.response = "Операция выполнена успешно";
			}
			else if(input.getUri().endsWith("GetProcessingKMResult")) {
				if(_lastChecked == null) {
					result.code = 500+Errors.SYSTEM_ERROR;
					result.response = "Сессия работы с КМ не открыта или проверка не производилась";
				} else {
					result.mime = "text/xml;charset=utf-8";
					result.code = 200;
					result.response = XML_HEADER+"<ProcessingKMResult";
					result.response+=" GUID=\""+_lastChecked.attachment().toString()+"\"";
					result.response+=" Result=\""+_lastChecked.getMarkCheckResult().isPositiveChecked()+"\"";
					result.response+=" ResultCode=\""+_lastChecked.getMarkCheckResult().bVal+"\"";
					result.response+=" HandleCode=\"0\"";
					result.response+=" />";
				}
			}
			else if(input.getUri().endsWith("ConfirmKM")) {
				KMSession session = KM_SESSIONS.get(input.getRemoteIpAddress());
				SellItem item = session.getByUUID(input.getParms().get("guid"));
				if(item != null) {
					FNManager.getInstance().getFN().confirmMarkingItem(item, "true".equals(input.getParms().get("confirm")));
					session.CONFIRMED.add(item);
					result.code = 200;
					result.response = "Операция выполнена успешно";
				}
				else throw new Exception();
			} 
			else if(input.getUri().endsWith("ProcessCheck")) {
				KMSession session = KM_SESSIONS.get(input.getRemoteIpAddress());
				doSellOrder(result,parser,session, "true".equalsIgnoreCase(input.getParms().get("doPrint")));
			}
			else if (input.getUri().endsWith("ProcessCorrectionCheck")) {
				KMSession session = KM_SESSIONS.get(input.getRemoteIpAddress());
				doCorrection(result, session,parser);
			}
		} catch(Exception e) {
			result.code = 500 + Errors.DATA_ERROR;
			result.response = "Неверный параметр запроса";
			Log.d("Web", "Exception ", e);
		}
		return new Response(result, result.mime, result.getStream(), result.size());
	}


	private interface ParserCallback {
		void onTag(SellOrder order, XmlPullParser parser);
	}
	
	private boolean doSellOrder(E1CResult response, XmlPullParser parser,KMSession km, boolean doPrint) throws XmlPullParserException,IOException {
		response.code = 500;
		response.response = "Неверный формат данных";
		SellOrder order = null;
		String after = Const.EMPTY_STRING;
		OU ou = null;
		int event = parser.getEventType();
		while(event != XmlPullParser.END_DOCUMENT) {
			if(event == XmlPullParser.START_TAG) {
				if("Parameters".equals(parser.getName())) {
					ou = parseOU(parser);
					OrderTypeE type = OrderTypeE.INCOME;
					int v = Integer.parseInt(parser.getAttributeValue(null, "OperationType"));
					switch(v) {
					case 2: type = OrderTypeE.RETURN_INCOME; break;
					case 3: type = OrderTypeE.OUTCOME; break;
					case 4: type = OrderTypeE.RETURN_OUTCOME; break;
					}
					v = Integer.parseInt(parser.getAttributeValue(null, "TaxationSystem"));
					TaxModeE tax = TaxModeE.values()[v];
					order = new SellOrder(type,tax);
					order.attach(after);
					if(!parseOrder(order, parser,km, new ParserCallback() {
						
						@Override
						public void onTag(SellOrder order, XmlPullParser parser) {
							if("TextString".equals(parser.getName())) {
								String after = (String)order.attachment();
								after += "\n";
								after+= parser.getAttributeValue(null, "Text");
								order.attach(after);
							}
							
						}
					})) return false;
					break;
				}
			}
			event = parser.next();
		}
		FNBaseI.doSellOrderResult res  = FNManager.getInstance().getFN().doSellOrder(order, ou, order,doPrint , Const.EMPTY_STRING, Const.EMPTY_STRING, Const.EMPTY_STRING, order.attachment().toString(), true);
		if(Errors.isOK(res.code)) {
			if(doPrint)
				Printing.getInstance().queue(res.print);
			response.code = 200;
			response.mime = "text/xml;charset=utf8";
			response.response = XML_HEADER+"<DocumentOutputParameters><Parameters";
			response.response +=" ShiftNumber=\""+order.getShiftNumber()+"\"";
			response.response +=" CheckNumber=\""+order.getNumber()+"\"";
			response.response +=" ShiftClosingCheckNumber=\""+order.getNumber()+"\"";
			response.response +=" AddressSiteInspections=\""+escape(order.getFnsUrl())+"\"";
			response.response +=" FiscalSign=\""+order.signature().getFdNumber()+"\"";
			response.response +=" DateTime=\""+DF.format(order.signature().signDate())+"\"";
			response.response += " /></DocumentOutputParameters>";
			return true;
		} else {
			response.response = Errors.getErrorDescr(res.code );
			response.code = 500+res.code;
			return false;
		}
			
		
	}
	private boolean doCorrection(E1CResult response, KMSession km, XmlPullParser parser) throws XmlPullParserException,IOException {
		response.code = 500;
		response.response = "Неверный формат данных";
		Correction order = null;
		OU ou = null;
		int event = parser.getEventType();
		while(event != XmlPullParser.END_DOCUMENT) {
			if(event == XmlPullParser.START_TAG) {
				if("Parameters".equals(parser.getName())) {
					ou = parseOU(parser);
					OrderTypeE type = OrderTypeE.INCOME;
					int v = Integer.parseInt(parser.getAttributeValue(null, "OperationType"));
					switch(v) {
					case 2: type = OrderTypeE.RETURN_INCOME; break;
					case 3: type = OrderTypeE.OUTCOME; break;
					case 4: type = OrderTypeE.RETURN_OUTCOME; break;
					}
					v = Integer.parseInt(parser.getAttributeValue(null, "TaxationSystem"));
					TaxModeE tax = TaxModeE.values()[v];
					order = new Correction(CorrectionTypeE.BY_OWN, type, tax);
					if(!parseOrder(order, parser,km,new ParserCallback() {
						@Override
						public void onTag(SellOrder order, XmlPullParser parser) {
							if("CorrectionData".equals(parser.getName())) {
								Correction cor = (Correction)order;
								String s = parser.getAttributeValue(null, "Type");
								if(s != null)
									cor.setType(s.equals("0") ? CorrectionTypeE.BY_OWN : CorrectionTypeE.BY_ARBITARITY);
								s = parser.getAttributeValue(null, "Datе");
								if(s != null && !s.isEmpty()) try {
									cor.setBaseDocumentDate(DF.parse(s));
								} catch(ParseException pe) { }
								s = parser.getAttributeValue(null, "Number");
								if(s != null && !s.isEmpty()) 
									cor.setBaseDocumentNumber(s);
							}
							
						}
						
					})) return false;
					break;
				}
			}
			event = parser.next();
		}
		FNBaseI.doCorrectionResult res  = FNManager.getInstance().getFN().doCorrection(order, ou, order, Const.EMPTY_STRING);
		if(Errors.isOK(res.code)) {
			Printing.getInstance().queue(res.print);
			response.code = 200;
			response.mime = "text/xml;charset=utf8";
			response.response = XML_HEADER+"<DocumentOutputParameters><Parameters";
			response.response +=" ShiftNumber=\""+order.getShiftNumber()+"\"";
			response.response +=" CheckNumber=\""+order.getNumber()+"\"";
			response.response +=" ShiftClosingCheckNumber=\""+order.getNumber()+"\"";
			response.response +=" AddressSiteInspections=\""+escape(order.getFnsUrl())+"\"";
			response.response +=" FiscalSign=\""+order.signature().getFdNumber()+"\"";
			response.response +=" DateTime=\""+DF.format(order.signature().signDate())+"\"";
			response.response += " /></DocumentOutputParameters>";
			return true;
		} else {
			response.response = Errors.getErrorDescr(res.code );
			response.code = 500+res.code;
			return false;
		}
			
		
	}
	
	private boolean parseOrder(SellOrder order,XmlPullParser parser, KMSession km, ParserCallback callback) throws XmlPullParserException,IOException {
		IAgentOwner agentOwner = null;
		Tag attrOwner = null;
		int iTag = 0;
		int event = parser.getEventType();
		while(event != XmlPullParser.END_DOCUMENT) {
			if(event == XmlPullParser.START_TAG) {
				if("Parameters".equals(parser.getName())) {
					
					String s = parser.getAttributeValue(null, "SaleAddress");
					if(s != null && !s.isEmpty()) order.getLocation().setAddress(s);
					s = parser.getAttributeValue(null, "SaleLocation");
					if(s != null && !s.isEmpty()) order.getLocation().setPlace(s);
					s = parser.getAttributeValue(null, "CustomerEmail");
					if(s != null && !s.isEmpty()) order.getClientData().add(FZ54Tag.T1008_BUYER_PHONE_EMAIL, s);
					s = parser.getAttributeValue(null, "CustomerPhone");
					if(s != null && !s.isEmpty()) order.getClientData().add(FZ54Tag.T1008_BUYER_PHONE_EMAIL,s);
					s = parser.getAttributeValue(null, "AgentType");
					if(s !=null && !s.isEmpty()) 
						order.getAgentData().setType(AgentTypeE.values()[Integer.parseInt(s)]);
					s = parser.getAttributeValue(null, "AdditionalAttribute");
					if(s != null && !s.isEmpty())
						order.add(FZ54Tag.T1192_EXTRA_BILL_FIELD,s);
					agentOwner = order;	
					attrOwner = order;
					iTag = FZ54Tag.T1261_INDUSTRY_CHECK_REQUISIT;
				}
				else if("CustomerDetail".equals(parser.getName())) {
					if(order == null) return false;
					String s = parser.getAttributeValue(null, "Info");
					if(s != null && !s.isEmpty())
						order.getAgentData().add(FZ54Tag.T1227_CLIENT_NAME,s);
					s = parser.getAttributeValue(null, "INN");
					if(s != null && !s.isEmpty())
						order.getAgentData().add(FZ54Tag.T1228_CLIENT_INN,s);
					s = parser.getAttributeValue(null, "DateOfBirth");
					if(s != null && !s.isEmpty())
						order.getAgentData().add(FZ54Tag.T1243_CLIENT_BIRTHDAY,s);
					s = parser.getAttributeValue(null, "Citizenship");
					if(s != null && !s.isEmpty())
						order.getAgentData().add(FZ54Tag.T1244_CLIENT_CITIZENSHIP,s);
					s = parser.getAttributeValue(null, "DocumentTypeCode");
					if(s != null && !s.isEmpty())
						order.getAgentData().add(FZ54Tag.T1254_CLIENT_ADDRESS,s);
					s = parser.getAttributeValue(null, "DocumentData");
					if(s != null && !s.isEmpty())
						order.getAgentData().add(FZ54Tag.T1246_CLIENT_IDENTITY_DOC_DATA,s);
					s = parser.getAttributeValue(null, "Address");
					if(s != null && !s.isEmpty())
						order.getAgentData().add(FZ54Tag.T1254_CLIENT_ADDRESS,s);
				}
				else if("AgentData".equals(parser.getName())) {
					if(agentOwner == null) return false;
					if(agentOwner.getAgentData().getType() != AgentTypeE.NONE) {
						String s = parser.getAttributeValue(null, "AgentOperation");
						if(s != null && !s.isEmpty()) agentOwner.getAgentData().setAgentOperation(s);
						s = parser.getAttributeValue(null, "AgentPhone");
						if(s != null && !s.isEmpty()) agentOwner.getAgentData().setAgentPhone(s);
						s = parser.getAttributeValue(null, "PaymentProcessorPhone");
						if(s != null && !s.isEmpty()) agentOwner.getAgentData().setProviderName(s);
						s = parser.getAttributeValue(null, "AcquirerOperatorPhone");
						if(s != null && !s.isEmpty()) agentOwner.getAgentData().setOperatorPhone(s);
						s = parser.getAttributeValue(null, "AcquirerOperatorName");
						if(s != null && !s.isEmpty()) agentOwner.getAgentData().setOperatorName(s);
						s = parser.getAttributeValue(null, "AcquirerOperatorAddress");
						if(s != null && !s.isEmpty()) agentOwner.getAgentData().setOperatorAddress(s);
						s = parser.getAttributeValue(null, "AcquirerOperatorINN");
						if(s != null && !s.isEmpty()) agentOwner.getAgentData().setOperatorINN(s);
					}
				} else if("IndustryAttribute".equals(parser.getName())) {
					if(iTag == 0 || attrOwner == null) return false;
					Tag tag = new Tag(iTag);
					String s= parser.getAttributeValue(null, "IdentifierFOIV");
					if(s != null && !s.isEmpty())
						tag.add(FZ54Tag.T1262_FOIV_ID,s);
					s = parser.getAttributeValue(null, "DocumentDate");
					if(s != null && !s.isEmpty())
						tag.add(FZ54Tag.T1263_DOC_BASE_DATE,s);
					s = parser.getAttributeValue(null, "DocumentNumber");
					if(s != null && !s.isEmpty())
						tag.add(FZ54Tag.T1264_DOC_BASE_NO,s);
					
					s = parser.getAttributeValue(null, "AttributeValue");
					if(s != null && !s.isEmpty())
						tag.add(FZ54Tag.T1265_INDUSTRY_REQUISIT_VALUE,s);
					if(!tag.getChilds().isEmpty())
						attrOwner.add(tag);
					
				}
				else if("VendorData".equals(parser.getName())) {
					
				}
				else if("FiscalString".equals(parser.getName())) {
					SellItem item = null;
					String name = parser.getAttributeValue(null, "Name");
					String s = parser.getAttributeValue(null, "Quantity");
					if (s == null || s.isEmpty()) s = "1.0";
					double qtty  = Double.parseDouble(s.replace(",", "."));
					s = parser.getAttributeValue(null, "PriceWithDiscount");
					if(s == null || s.isEmpty()) s = "0.0";
					double price  = Double.parseDouble(s.replace(",","."));
					VatE vat = VatE.VAT_20;
					s = parser.getAttributeValue(null, "VATRate");
					if("none".equalsIgnoreCase(s)) vat =VatE.VAT_NONE;
					else if("10".equals(s)) vat = VatE.VAT_10;
					else if("20/120".equals(s)) vat = VatE.VAT_20_120;
					else if("10/110".equals(s)) vat = VatE.VAT_10_110;
					else if("0".equals(s)) vat = VatE.VAT_0;
					SellItemTypeE type = SellItemTypeE.GOOD;
					ItemPaymentTypeE payment = ItemPaymentTypeE.FULL;
					s = parser.getAttributeValue(null, "PaymentMethod");
					if(s != null && !s.isEmpty())
						payment = ItemPaymentTypeE.fromByte(Byte.parseByte(s));
					s = parser.getAttributeValue(null, "CalculationSubject");
					if(s != null && !s.isEmpty())
						type = SellItemTypeE.fromByte(Byte.parseByte(s));
					MeasureTypeE measure = MeasureTypeE.PIECE;
					s = parser.getAttributeValue(null, "MeasureOfQuantity");
					if(s != null && !s.isEmpty())
						measure = MeasureTypeE.fromByte((byte)Integer.parseInt(s));
					item = new SellItem(type, payment, name, BigDecimal.valueOf(qtty), measure,BigDecimal.valueOf(price),vat);
					s = parser.getAttributeValue(null,"MarkingCode");
					if(s != null && !s.isEmpty()) {
						boolean needCheck = true;
						s = new String(Base64.decode(s, Base64.DEFAULT));
						while(s.indexOf("\\u001d") != -1)
							s = s.replace("\\u001d", "\u001d");
						if(km != null ) {
							SellItem checked = km.getByCode(s);
							if(checked != null && km.isConfirmed(checked)) {
								needCheck = false;
								item = new CheckedItem(item);
								((CheckedItem)item).applyCheckResult(checked);
							}
						} 
						if(needCheck) {
							item.setMarkingCode(s, order.getType());
							FNManager.getInstance().getFN().checkMarkingItem(item, Settings.getInstance().getOISMServer());
							if(item.getMarkCheckResult().isPositiveChecked())
								FNManager.getInstance().getFN().confirmMarkingItem(item, true);
						}
					}
					
					order.addItem(item);
					s = parser.getAttributeValue(null, "CountryOfOrigin");
					if(s != null && !s.isEmpty()) item.add(FZ54Tag.T1230_COUNTRY_ORIGIN,s);
					s = parser.getAttributeValue(null, "CustomsDeclaration");
					if(s != null && !s.isEmpty()) item.add(FZ54Tag.T1231_CUSTOMS_DECLARATION_NO,s);
					s = parser.getAttributeValue(null, "ExciseAmount");
					if(s != null && !s.isEmpty()) item.add(FZ54Tag.T1229_EXCISE_SUM,BigDecimal.valueOf(Double.parseDouble(s.replace(",", "."))));
					s = parser.getAttributeValue(null, "AdditionalAttribute");
					if(s!=null && !s.isEmpty()) item.add(FZ54Tag.T1191_EXTRA_ITEM_FIELD,s);
					s = parser.getAttributeValue(null, "CalculationAgent");
					if(s!=null && !s.isEmpty()) {
						item.getAgentData().setType(AgentTypeE.values()[Integer.parseInt(s)]);
					}
					agentOwner = item;
					attrOwner = item;
					iTag = FZ54Tag.T1260_INDUSTRY_ITEM_REQUISIT;
				}
				else if("Payments".equals(parser.getName())) {
					String s = parser.getAttributeValue(null, "Cash");
					double val = 0;
					if (s != null && !s.isEmpty()) val = Double.parseDouble(s.replace(",",".")); else val = 0;
					if(val > 0)
						order.addPayment(new Payment(PaymentTypeE.CASH, BigDecimal.valueOf(val)));
					s = parser.getAttributeValue(null, "ElectronicPayment");
					if (s != null && !s.isEmpty()) val = Double.parseDouble(s.replace(",",".")); else val = 0;
					if(val > 0)
						order.addPayment(new Payment(PaymentTypeE.CARD, BigDecimal.valueOf(val)));
					s = parser.getAttributeValue(null, "PrePayment");
					if (s != null && !s.isEmpty()) val = Double.parseDouble(s.replace(",",".")); else val = 0;
					if(val > 0)
						order.addPayment(new Payment(PaymentTypeE.PREPAYMENT, BigDecimal.valueOf(val)));
					s = parser.getAttributeValue(null, "PostPayment");
					if (s != null && !s.isEmpty()) val = Double.parseDouble(s.replace(",",".")); else val = 0;
					if(val > 0)
						order.addPayment(new Payment(PaymentTypeE.CREDIT, BigDecimal.valueOf(val)));
					s = parser.getAttributeValue(null, "Barter");
					if (s != null && !s.isEmpty()) val = Double.parseDouble(s.replace(",",".")); else val = 0;
					if(val > 0)
						order.addPayment(new Payment(PaymentTypeE.AHEAD, BigDecimal.valueOf(val)));
					if(order.getPayments().isEmpty())
						order.addPayment(new Payment(PaymentTypeE.CASH, BigDecimal.valueOf(0)));
				}
				if(callback != null)
					callback.onTag(order, parser);
			}
			event = parser.next();
		}
		return true;
	}
	
	private boolean doCheck(E1CResult response) {
		response.code = 500 + Errors.DEVICE_ABSEND;
		response.response = "ФН не установлен";
		if (!FNManager.getInstance().isFNReady())
			return false;
		if (!FNManager.getInstance().getFN().getKKMInfo().isFNActive()) {
			response.code = 500 + Errors.WRONG_FN_MODE;
			response.response = Errors.getErrorDescr(Errors.WRONG_FN_MODE);
			return false;
		}
		return true;
	}

	private OU parseOU(XmlPullParser parser) {
		OU result = new OU("Администратор");
		if (parser != null) {
			String s = parser.getAttributeValue(null, "CashierName");
			if (s != null)
				result.setName(s);
			s = parser.getAttributeValue(null, "CashierINN");
			if (s != null && Utils.checkINN(s))
				result.setINN(s);
		}
		return result;
	}

	private void printTextDocument(E1CResult response, XmlPullParser parser) throws Exception {
		int evt = parser.getEventType();
		String s = Const.EMPTY_STRING;
		while(evt != XmlPullParser.END_DOCUMENT) {
			if(evt == XmlPullParser.START_TAG) {
				if("TextString".equals(parser.getName())) {
					s += parser.getAttributeValue(null, "Text")+"\n";
				} else if ("Barcode".equals(parser.getName())) {
					s+="{\\barcode padding:12,0,12,0;";
					String type = parser.getAttributeValue(null, "BarcodeType");
					if(type == null || type.isEmpty()) type="code128";
					type = type.toLowerCase(Locale.ROOT);
					s+="type:"+type+";";
					int w = 300, h = 60;
					if(type == "qr") h = 300;
					s += "width:"+w+";height:"+h;
					s+="\\";
					s += parser.getAttributeValue(null, "Barcode")+"}\n";
				}
			}
			evt = parser.next();
		}
		response.code = 200;
		response.response = "Выполнено успешно";
		if(!s.isEmpty()) {
			for(int i=0;i<5;i++)
				s+="\n ";
		}
		Printing.getInstance().queue(s);
	}
	
	private void requestKM(E1CResult response, XmlPullParser parser, KMSession session) throws Exception {
		if (!doCheck(response))
			return;
		if(!FNManager.getInstance().getFN().getKKMInfo().isMarkingGoods()) {
			response.code  = 500 + Errors.NOT_SUPPORTED;
			response.response = "Работа с маркированным товаром не поддерживается";
			return;
		}
		int evt = parser.getEventType();
		while(evt != XmlPullParser.END_DOCUMENT) {
			if(evt == XmlPullParser.START_TAG && "RequestKM".equals(parser.getName())) {
				String uuid = parser.getAttributeValue(null, "GUID");
				String code = new String(Base64.decode(parser.getAttributeValue(null, "MarkingCode"),Base64.DEFAULT));
				String s = parser.getAttributeValue(null, "Quantity");
				double qtty = 1.0;
				if(s != null) qtty = Double.parseDouble(s.replace(",", "."));
				SellItem item = new SellItem("Предмет рассчета", BigDecimal.valueOf(qtty), BigDecimal.ONE, VatE.VAT_NONE);
				while(code.indexOf("\\u001d")!=-1)
					code = code.replace("\\u001d", "\u001d");
				item.setMarkingCode(code, OrderTypeE.INCOME);
				item.attach(uuid);
				response.code =  FNManager.getInstance().getFN().checkMarkingItem(item, Settings.getInstance().getOISMServer());
				response.mime = "text/xml;charset=utf-8";
				response.response = XML_HEADER;
				response.response+="<RequestKMResult";
				if(response.code == Errors.NO_ERROR) {
					Log.d("FNCORE", "------ MARK "+code+" --------------------");
					Log.d("FNCORE","check code : "+String.format("%02X",item.getMarkCheckResult().bVal));
					Log.d("FNCORE","Local only: "+item.getMarkCheckResult().autonomousMode);
					Log.d("FNCORE","local checked: "+item.getMarkCheckResult().codeChecked);
					Log.d("FNCORE","local processed: "+item.getMarkCheckResult().codeProcessed);
					Log.d("FNCORE","OISM checked: "+item.getMarkCheckResult().codeOISMChecked);
					Log.d("FNCORE","OISM processed: "+item.getMarkCheckResult().codeOISMProcessed);
					
					
					response.response+=" Checking=\"true\"";
					response.response+=" CheckingResult=\""+item.getMarkCheckResult().isPositiveChecked()+"\"";
				} else {
					response.response+=" Checking=\"false\"";
					response.response+=" CheckingResult=\"false\"";
				}
				synchronized(this) {
					_lastChecked = item;
				}
				session.add(item,uuid);
				response.response+=" />";
				response.code = 200;
				break;
			}
			evt = parser.next();
		}
	}
	
	private void closeShift(E1CResult response, OU cashier) {
		if (!doCheck(response))
			return;
		if (!FNManager.getInstance().getFN().getKKMInfo().getShift().isOpen()) {
			response.code = 500 + Errors.INVALID_SHIFT_STATE;
			response.response = "Смена уже закрыта";
			return;
		}
		Shift shift = new Shift();
		KKMInfo info = new KKMInfo();
		FNBaseI.doShiftResult r =  FNManager.getInstance().getFN().closeShift(cashier, shift, null);
		if (Errors.isOK(r.code)) {
			response.code = 200;
			response.response = XML_HEADER;
			response.response += "<OutputParameters><Parameters";
			response.response += " ShiftNumber=\"" + shift.getNumber() + "\"";
			response.response += " CheckNumber=\"" + info.getLastFNDocNumber() + "\"";
			response.response += " ShiftClosingCheckNumber=\"" + info.getShift().getLastCheckNumber() + "\"";
			response.response += " ShiftState=\"1\"";
			response.response += " CashBalance=\""
					+ String.format(Locale.ROOT, "%.2f", Settings.getInstance().getCashRest(info.getFNNumber())) + "\"";
			OfdStatistic s = new OfdStatistic();
			FNManager.getInstance().getFN().updateOFDStatus(s);
			response.response += " BacklogDocumentsCounter=\"" + s.getUnsentDocumentCount() + "\"";
			response.response += " BacklogDocumentFirstNumber=\"" + s.getFirstUnsentNumber() + "\"";
			response.response += " BacklogDocumentFirstDateTime=\"" + DF.format(s.getFirstUnsentDate()) + "\"";
			response.response += "/>";
			response.response += "<CountersOperationType1";
			response.response += " CheckCount=\""+shift.getShiftCounters().Income().count+"\"";
			response.response += String.format(Locale.ROOT," TotalChecksAmount=\"%.2f\"",shift.getShiftCounters().Income().totalSum/100.0);
			response.response += " CorrectionCheckCount=\""+shift.getShiftCounters().Corrections().countIncome+"\"";
			response.response += String.format(Locale.ROOT," TotalCorrectionChecksAmount=\"%.2f\"",shift.getShiftCounters().Corrections().incomeTotalSum/100.0);
			response.response += "/>";
			response.response += "<CountersOperationType2";
			response.response += " CheckCount=\""+shift.getShiftCounters().ReturnIncome().count+"\"";
			response.response += String.format(Locale.ROOT," TotalChecksAmount=\"%.2f\"",shift.getShiftCounters().ReturnIncome().totalSum/100.0);
			response.response += " CorrectionCheckCount=\""+shift.getShiftCounters().Corrections().countReturnIncome+"\"";
			response.response += String.format(Locale.ROOT," TotalCorrectionChecksAmount=\"%.2f\"",shift.getShiftCounters().Corrections().returnIncomeTotalSum/100.0);
			response.response += "/>";
			response.response += "<CountersOperationType3";
			response.response += " CheckCount=\""+shift.getShiftCounters().Outcome().count+"\"";
			response.response += String.format(Locale.ROOT," TotalChecksAmount=\"%.2f\"",shift.getShiftCounters().Outcome().totalSum/100.0);
			response.response += " CorrectionCheckCount=\""+shift.getShiftCounters().Corrections().countOutcome+"\"";
			response.response += String.format(Locale.ROOT," TotalCorrectionChecksAmount=\"%.2f\"",shift.getShiftCounters().Corrections().outcomeTotalSum/100.0);
			response.response += "/>";
			response.response += "<CountersOperationType4";
			response.response += " CheckCount=\""+shift.getShiftCounters().ReturnOutcome().count+"\"";
			response.response += String.format(Locale.ROOT," TotalChecksAmount=\"%.2f\"",shift.getShiftCounters().ReturnOutcome().totalSum/100.0);
			response.response += " CorrectionCheckCount=\""+shift.getShiftCounters().Corrections().countReturnOutcome+"\"";
			response.response += String.format(Locale.ROOT," TotalCorrectionChecksAmount=\"%.2f\"",shift.getShiftCounters().Corrections().returnOutcomeTotalSum/100.0);
			response.response += "/>";
			
			response.response += "</OutputParameters>";
			Printing.getInstance().queue(r.print);
		} else {
			response.response = Errors.getErrorDescr(r.code);
			response.code = r.code + 500;
		}
		
	}
	
	private void openShift(E1CResult response, OU cashier) {
		if (!doCheck(response))
			return;
		if (FNManager.getInstance().getFN().getKKMInfo().getShift().isOpen()) {
			response.code = 500 + Errors.INVALID_SHIFT_STATE;
			response.response = "Смена уже открыта";
			return;
		}
		Shift shift = new Shift();
		KKMInfo info = new KKMInfo();
		FNBaseI.doShiftResult r =  FNManager.getInstance().getFN().openShift(cashier, shift, null, FNCore.getInstance());
		if (Errors.isOK(r.code)) {
			response.code = 200;
			response.response = XML_HEADER;
			response.response += "<OutputParameters><Parameters";
			response.response += " ShiftNumber=\"" + shift.getNumber() + "\"";
			response.response += " CheckNumber=\"" + info.getLastFNDocNumber() + "\"";
			response.response += " ShiftClosingCheckNumber=\"" + info.getShift().getLastCheckNumber() + "\"";
			response.response += " ShiftState=\"1\"";
			response.response += " CashBalance=\""
					+ String.format(Locale.ROOT, "%.2f", Settings.getInstance().getCashRest(info.getFNNumber())) + "\"";
			OfdStatistic s = new OfdStatistic();
			FNManager.getInstance().getFN().updateOFDStatus(s);
			response.response += " BacklogDocumentsCounter=\"" + s.getUnsentDocumentCount() + "\"";
			response.response += " BacklogDocumentFirstNumber=\"" + s.getFirstUnsentNumber() + "\"";
			response.response += " BacklogDocumentFirstDateTime=\"" + DF.format(s.getFirstUnsentDate()) + "\"";
			response.response += "/></OutputParameters>";
			Printing.getInstance().queue(r.print);
		} else {
			response.response = Errors.getErrorDescr(r.code);
			response.code = r.code + 500;
		}
		

	}

	private void cashInOutcome(E1CResult response, double sum, OU cashier) {
		if (!doCheck(response))
			return;
		if (!FNManager.getInstance().getFN().getKKMInfo().getShift().isOpen()) {
			response.code = 500 + Errors.INVALID_SHIFT_STATE;
			response.response = Errors.getErrorDescr(Errors.INVALID_SHIFT_STATE);
			return;
		}
		double s = Settings.getInstance().getCashRest(FNManager.getInstance().getFN().getKKMInfo().getFNNumber());
		if (s + sum < 0) {
			response.code = 500 + Errors.DATA_ERROR;
			response.response = "Неверная сумма операции";
			return;
		}
		s += sum;
		Settings.getInstance().setCashRest(FNManager.getInstance().getFN().getKKMInfo().getFNNumber(), s);
		Printing.getInstance().queue(
				new CashWithdraw(new BigDecimal(sum), FNManager.getInstance().getFN().getKKMInfo(), cashier).getPF());
		response.code = 200;
		response.response = "Операция выполнена успешно";
	}

	
	
	private void getCurrentStatus(E1CResult response) {
		if (!doCheck(response))
			return;
		KKMInfo info = new KKMInfo();
		response.code = FNManager.getInstance().getFN().readKKMInfo(info);
		if (!Errors.isOK(response.code)) {
			response.response = Errors.getErrorDescr(response.code);
			response.code += 500;
			return;
		}
		response.response = XML_HEADER;
		response.mime = "text/xml;charset=utf-8";
		response.code = 200;
		response.response += "<OutputParameters><Parameters";
		response.response += " ShiftNumber=\"" + info.getShift().getNumber() + "\"";
		response.response += " CheckNumber=\"" + info.getLastFNDocNumber() + "\"";
		response.response += " ShiftClosingCheckNumber=\"" + info.getShift().getLastCheckNumber() + "\"";
		response.response += " ShiftState=\"" + (info.getShift().isOpen()
				? (System.currentTimeMillis() - info.getShift().getWhenOpen() >= Const.ONE_DAY ? 3 : 2)
				: 1) + "\"";
		response.response += " CashBalance=\""
				+ String.format(Locale.ROOT, "%.2f", Settings.getInstance().getCashRest(info.getFNNumber())) + "\"";
		OfdStatistic s = new OfdStatistic();
		FNManager.getInstance().getFN().updateOFDStatus(s);
		response.response += " BacklogDocumentsCounter=\"" + s.getUnsentDocumentCount() + "\"";
		response.response += " BacklogDocumentFirstNumber=\"" + s.getFirstUnsentNumber() + "\"";
		response.response += " BacklogDocumentFirstDateTime=\"" + DF.format(s.getFirstUnsentDate()) + "\"";
		response.response += "/></OutputParameters>";
	}

	private void printCopy(E1CResult response, int number) {
		if (!doCheck(response))
			return;
		Tag tlv = new Tag();
		response.code = FNManager.getInstance().getFN().readDocumentFromTLV(number, tlv);
		if (response.code != Errors.NO_ERROR) {
			response.response = Errors.getErrorDescr(response.code);
			response.code += 500;
			return;
		}
		response.code = 200;
		response.response = "Выполнено без ошибок";
		String pf = DocumentUtils.getPrintForm(tlv);
		if (pf != null && !pf.isEmpty())
			Printing.getInstance().queue(pf);
	}

	private void requestReport(E1CResult response, XmlPullParser parser) throws IOException, XmlPullParserException {
		if (!doCheck(response))
			return;
		OU ou = new OU("Администратор");
		int evt= parser.getEventType();
		while(evt != XmlPullParser.END_DOCUMENT) {
			if(evt == XmlPullParser.START_TAG && "Parameters".equals(parser.getName())) {
				ou = parseOU(parser);
				break;
			}
			evt = parser.next();
		}
		FiscalReport r = new FiscalReport();
		FNBaseI.doPrintResult rs =  FNManager.getInstance().getFN().requestFiscalReport(ou, r, Const.EMPTY_STRING);
		if(Errors.isOK(rs.code)) {
			KKMInfo info = FNManager.getInstance().getFN().getKKMInfo();
			response.code = 200;
			response.mime = "text/xml;charset=utf8";
			response.response = XML_HEADER;
			response.response += "<OutputParameters><Parameters";
			response.response += " ShiftNumber=\"" + info.getShift().getNumber() + "\"";
			response.response += " CheckNumber=\"" + info.getLastFNDocNumber() + "\"";
			response.response += " ShiftClosingCheckNumber=\"" + info.getShift().getLastCheckNumber() + "\"";
			response.response += " ShiftState=\"" + (info.getShift().isOpen()
					? (System.currentTimeMillis() - info.getShift().getWhenOpen() >= Const.ONE_DAY ? 3 : 2)
					: 1) + "\"";
			response.response += " CashBalance=\""
					+ String.format(Locale.ROOT, "%.2f", Settings.getInstance().getCashRest(info.getFNNumber())) + "\"";
			OfdStatistic s = new OfdStatistic();
			FNManager.getInstance().getFN().updateOFDStatus(s);
			response.response += " BacklogDocumentsCounter=\"" + s.getUnsentDocumentCount() + "\"";
			response.response += " BacklogDocumentFirstNumber=\"" + s.getFirstUnsentNumber() + "\"";
			response.response += " BacklogDocumentFirstDateTime=\"" + DF.format(s.getFirstUnsentDate()) + "\"";
			response.response += "/></OutputParameters>";
			Printing.getInstance().queue(rs.print);
		} else {
			response.code = rs.code + 500;
			response.response = Errors.getErrorDescr(rs.code);
		}
		
	}
	
	private void doXReports(E1CResult response) {
		if (!doCheck(response))
			return;
		FNCounters result;
		try (Transaction transaction = FNManager.getInstance().getFN().getStorage().open()) {
			result = FNManager.getInstance().getFN().getFnCounters(transaction, false);
			if (result == null)
				response.code = 500 + Errors.DATA_ERROR;
			response.code = 200;
		}
		if (response.code != 200)
			response.response = Errors.getErrorDescr(response.code - 500);
		else {
			response.response = "Выполнено без ошибок";
			String pf = DocumentUtils.getPrintForm(result);
			if (pf != null && !pf.isEmpty())
				Printing.getInstance().queue(pf);
		}
	}

	private void getDataKKT(E1CResult response) {
		response.code = 500 + Errors.DEVICE_ABSEND;
		response.response = "ФН не установлен";
		if (!FNManager.getInstance().isFNReady())
			return;
		KKMInfo info = new KKMInfo();
		int r = FNManager.getInstance().getFN().readKKMInfo(info);
		if (Errors.isOK(r)) {
			response.code = 200;
			response.mime = "text/xml;charset=utf-8";
			response.response = XML_HEADER;
			response.response += "<TableParametersKKT";
			response.response += " KKTNumber=\"" + info.getKKMNumber() + "\"";
			response.response += " KKTSerialNumber=\"" + info.getKKMSerial() + "\"";
			response.response += " FirmwareVersion=\"" + KKMInfo.KKT_VERSION + "\"";
			response.response += " Fiscal=\"" + info.isFNActive() + "\"";
			response.response += " FFDVersionKKT=\"1.2\"";
			if (info.isFNActive()) {
				response.response += " FFDVersionFN=\"" + info.getFFDProtocolVersion().name + "\"";

				response.response += " FNSerialNumber=\"" + info.getFNNumber() + "\"";
				response.response += " DocumentNumber=\"" + info.signature().getFdNumber() + "\"";

				response.response += " DateTime=\"" + DF.format(info.signature().signDate()) + "\"";
				response.response += " CompanyName=\"" + escape(info.getOwner().getName()) + "\"";
				Log.d("Web", "Name " + info.getOwner().getName());
				Log.d("Web", "Addr " + info.getLocation().getAddress());
				response.response += " INN=\"" + info.getOwner().getINN() + "\"";
				response.response += " SaleAddress=\"" + escape(info.getLocation().getAddress()) + "\"";
				response.response += " SaleLocation=\"" + escape(info.getLocation().getPlace()) + "\"";
				response.response += " TaxationSystems=\"";
				String s = "";
				for (TaxModeE m : info.getTaxModes()) {
					if (!s.isEmpty())
						s += ",";
					s += m.ordinal();
				}
				response.response += s + "\"";
				response.response += " IsOffline=\"" + info.isOfflineMode() + "\"";
				response.response += " IsEncrypted=\"" + info.isEncryptionMode() + "\"";
				response.response += " IsService=\"" + info.isServiceMode() + "\"";
				response.response += " IsExcisable=\"" + info.isExcisesMode() + "\"";
				response.response += " IsGambling=\"" + info.isGamblingMode() + "\"";
				response.response += " IsLottery=\"" + info.isLotteryMode() + "\"";
				response.response += " BSOSing=\"" + info.isBSOMode() + "\"";
				response.response += " IsOnline=\"" + info.isInternetMode() + "\"";
				response.response += " IsAutomaticPrinter=\"" + info.isAutoPrinter() + "\"";
				response.response += " IsAutomatic=\"" + info.isAutomatedMode() + "\"";
				response.response += " IsMarking=\"" + info.isMarkingGoods() + "\"";
				response.response += " IsPawnshop=\"" + info.isPawnShopActivity() + "\"";
				response.response += " IsAssurance=\"" + info.isInsuranceActivity() + "\"";
				response.response += " AgentTypes=\"";
				s = "";
				for (AgentTypeE m : info.getAgentType()) {
					if (!s.isEmpty())
						s += ",";
					s += m.bVal;
				}
				response.response += s + "\"";
				response.response += " AutomaticNumber=\"" + info.getAutomateNumber() + "\"";
				response.response += " OFDCompany=\"" + escape(info.ofd().getName()) + "\"";
				response.response += " OFDCompanyINN=\"" + info.ofd().getINN() + "\"";
				response.response += " FNSURL=\"" + escape(info.getFNSUrl()) + "\"";
				response.response += " SenderEmail=\"" + escape(info.getSenderEmail()) + "\"";
			}

			response.response += "/>";
		} else {
			response.code = 500 + r;
			response.response = Errors.getErrorDescr(r);
		}
	}

	private static final String escape(String s) {
		String r = Const.EMPTY_STRING;
		if(s == null || s.isEmpty()) return r;
		for (int i = 0; i < s.length(); i++) {
			switch (s.charAt(i)) {
			case '\'':
				r += "&apos;";
				break;
			case '"':
				r += "&quot;";
				break;
			case '&':
				r += "&amp;";
				break;
			case '<':
				r += "&lt;";
				break;
			case '>':
				r += "&gt;";
				break;
			default:
				r += s.charAt(i);
			}
		}
		return r;
	}

}
