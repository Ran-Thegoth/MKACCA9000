package rs.utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import cs.U;
import cs.orm.ORMHelper;
import rs.data.goods.Variant;
import rs.data.goods.Barcode;
import rs.data.goods.Good;
import rs.data.goods.Good.MarkTypeE;
import rs.fncore.Const;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.VatE;
import rs.fncore.data.SellItem.SellItemTypeE;
import rs.mkacca.Core;
import rs.mkacca.DB;
import rs.mkacca.ui.Main;

@SuppressWarnings("deprecation")
public class Importer implements Runnable, Handler.Callback, DialogInterface.OnClickListener {

	private InputStream _stream;
	private ImportListener _listener;
	private static final int MSG_PROGRESS = 100;
	private static final int MSG_DONE = 101;
	private Handler h;
	private volatile boolean _work = true;
	
	private ProgressDialog _dialog;
	private Importer(Context ctx, InputStream s, ImportListener l) {
		_stream = s;
		_listener = l;
		h = new Handler(Core.getInstance().getMainLooper(),this);
		_dialog = new ProgressDialog(ctx);
		_dialog.setCancelable(false);
		_dialog.setCanceledOnTouchOutside(false);
		_dialog.setIndeterminate(true);
		_dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Прервать", this);
		_dialog.setMessage("Подготовка...");
		Main.lock();
		_dialog.show();
	}

	public static void doImport(Context ctx, InputStream s, ImportListener l) {
		SimpleTaskExecutor.getInstance().execute(new Importer(ctx, s, l));
	}

	public static interface ImportListener {
		public void onImportDone(Exception e);
	}

	private static final String [] ID_FIELD = { DB.ID_FLD };
	private static final String WHERE_UUID = DB.UUID_FLD+"=?";
	private static final String WHERE_ID = DB.ID_FLD +"=?";
	private static final String [] PARAMS = { Const.EMPTY_STRING };
	
	@Override
	public void run() {
		SQLiteDatabase db = Core.getInstance().db().getWritableDatabase();
		Exception result = null;
		Map<String, MeasureTypeE> measures = new HashMap<String, MeasureTypeE>();
		try {
			XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
			parser.setInput(_stream, "UTF-8");
			MeasureTypeE mu = MeasureTypeE.OTHER;
			int evt = XmlPullParser.START_DOCUMENT;
			long goodId = 0, varId = 0;
			double goodPrice = 0;
			boolean hasPacks = false;
			String packPrefix = Const.EMPTY_STRING;
			int counter = 0;
			while (evt != XmlPullParser.END_DOCUMENT) {
				if(!_work) break;
				switch (evt) {
				case XmlPullParser.START_TAG:
					if ("Unit".equals(parser.getName())) {
						String uuid = parser.getAttributeValue(null, "UUID");
						String code = parser.getAttributeValue(null, "Code");
						String name = parser.getAttributeValue(null, "Name");
						mu = MeasureTypeE.byOKEI(Integer.parseInt(code.trim())); 
						measures.put(uuid, mu);
					} else if ("Good".equals(parser.getName())) {
						hasPacks = false;
						String code = parser.getAttributeValue(null, "Article");
						String uuid = parser.getAttributeValue(null, "UUID");
						String name = parser.getAttributeValue(null, "Name");
						String val = parser.getAttributeValue(null, "TaxRate");
						String unitId = parser.getAttributeValue(null, "UnitUUID");
						String price = parser.getAttributeValue(null, "Price");
						if(price == null || price.isEmpty()) price = "0";
						goodPrice = Double.parseDouble(price.trim().replace(",", "."));
						VatE vat = VatE.VAT_20;
						switch (val) {
						case "none":
							vat = VatE.VAT_NONE;
							break;
						case "20":
						case "18":
							vat = VatE.VAT_20;
							break;
						case "10":
							vat = VatE.VAT_10;
							break;
						case "0":
							vat = VatE.VAT_0;
							break;

						}
						val = parser.getAttributeValue(null, "CalculationSubject");
						if (val == null || val.isEmpty())
							val = "1";
						SellItemTypeE type = SellItemTypeE.values()[Integer.parseInt(val.trim()) - 1];
						val = parser.getAttributeValue(null, "MarkedGoodTypeCode");
						if (val == null || val.isEmpty())
							val = "0";
						MarkTypeE markType = MarkTypeE.values()[Integer.parseInt(val.trim())];
						mu = measures.get(unitId);
						if(mu == null) mu = MeasureTypeE.PIECE;
						ContentValues cv = new ContentValues();
						cv.put(DB.UUID_FLD, uuid);
						
						cv.put("CODE",code);
						cv.put("VAT",vat.ordinal());
						cv.put("MARK_TYPE",markType.ordinal());
						cv.put("ITEM_TYPE",type.ordinal());
						cv.put("MU", mu.ordinal());
						cv.put("PRICE", goodPrice);
						PARAMS[0] = uuid;
						
						Cursor c = db.query(DB.GOODS, ID_FIELD,WHERE_UUID,PARAMS,null,null,null);
						if(c.moveToFirst()) {
							goodId = c.getLong(0);
							PARAMS[0] = String.valueOf(goodId);
							db.update(DB.GOODS, cv, WHERE_ID, PARAMS);
						} else {
							cv.put("NAME",name);
							cv.put("IS_FAV",1);
							cv.put("USED",1);
							goodId = db.insert(DB.GOODS, null, cv);
						}
						c.close();
					} else if ("Characteristic".equals(parser.getName())) {
						if (goodId == 0)
							throw new Exception("Неожиданный тег \"Characteristic\"");
						hasPacks = "true".equalsIgnoreCase(parser.getAttributeValue(null, "HasPacks"));
						packPrefix = parser.getAttributeValue(null, "Name");
						if (!hasPacks) {
							String uuid = parser.getAttributeValue(null, "UUID");
							String price = parser.getAttributeValue(null, "Price");
							if (price == null || price.isEmpty())
								price = "0";
							ContentValues cv = new ContentValues();
							cv.put("NAME", packPrefix);
							cv.put(Good.GOOD_ID_FLD,goodId);
							cv.put("MU",mu.ordinal());
							cv.put("QTTY",1.0);
							cv.put("UUID",uuid);
							cv.put("PRICE",Double.parseDouble(price.trim().replace(",", ".")));
							varId =  db.insert(DB.VARIANTS, null, cv);

						}
					} else if ("Pack".equals(parser.getName())) {
						if (goodId == 0)
							throw new Exception("Неожиданный тег \"Pack\"");
						String uuid = parser.getAttributeValue(null, "UUID");
						String price = parser.getAttributeValue(null, "Price");
						String name = parser.getAttributeValue(null, "Name");
						if(!packPrefix.isEmpty()) name = ","+name;
						String count = parser.getAttributeValue(null, "UnitsPerPack");
						if (count == null || count.isEmpty())
							count = "1";
						if (price == null || price.isEmpty())
							price = "0";
						ContentValues cv = new ContentValues();
						cv.put("NAME", packPrefix+name);
						cv.put("USED",1);
						cv.put("UUID",uuid);
						cv.put(Good.GOOD_ID_FLD,goodId);
						cv.put("MU",mu.ordinal());
						cv.put("QTTY",Double.parseDouble(count.trim().replace(",", ".")));
						cv.put("PRICE",Double.parseDouble(price.trim().replace(",", ".")));
						varId =  db.insert(DB.VARIANTS, null, cv);

					} else if("Barcode".equals(parser.getName())) {
						if (goodId == 0)
							throw new Exception("Неожиданный тег \"Barcode\"");
						String code = parser.getAttributeValue(null, "Value");
						ContentValues cv = new ContentValues();
						if(varId != 0) {
							cv.put(Barcode.OWNER_FLD,  varId);
							cv.put(Barcode.OWNER_TYPE_FLD,1);
						} else  {
							cv.put(Barcode.OWNER_FLD,  goodId);
							cv.put(Barcode.OWNER_TYPE_FLD,0);
						}
						cv.put("CODE",code);
						db.insert(DB.BARCODES, null, cv);
					}
					break;
				case XmlPullParser.END_TAG:
					if ("Good".equals(parser.getName())) {
						counter++;
						if(counter % 10 == 0 ) {
							Message m = h.obtainMessage(MSG_PROGRESS);
							m.arg1 = counter;
							m.arg2 = parser.getLineNumber();
							h.sendMessage(m);
						}
						varId =  goodId = 0;
						packPrefix = Const.EMPTY_STRING;
					}
					break;
				}
				evt = parser.next();
			}
		} catch (Exception e) {
			result = e;
		}
		Message m = h.obtainMessage(MSG_DONE);
		m.obj = result;
		h.sendMessage(m);
		
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_DONE:
			Main.unlock();
			_dialog.dismiss();
			_listener.onImportDone((Exception)msg.obj);
			break;
		case MSG_PROGRESS:
			_dialog.setMessage("Импортировано "+msg.arg1+" номенклатур, "+msg.arg2+" строк");
			break;
		}
		return true;
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		_work = false;
		
	}
}

