package rs.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import cs.U;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.MeasureTypeE;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.VatE;
import rs.mkacca.Core;

public class Exporter {

	public Exporter() {
		// TODO Auto-generated constructor stub
	}

	private static class ExportThread extends Thread implements Handler.Callback, DialogInterface.OnClickListener {
		private static final int MSG_FINISH_OK = 1;
		private static final int MSG_FINISH_ERROR = 2;
		private static final int MSG_PROGRESS = 3;
		private ProgressDialog _dlg;
		private Handler _h;
		private OutputStream _os;

		public ExportThread(Context ctx, OutputStream os) {
			_os = os;
			_h = new Handler(ctx.getMainLooper(), this);
			_dlg = new ProgressDialog(ctx);
			_dlg.setMessage("Подготовка...");
			_dlg.setIndeterminate(true);
			_dlg.setCancelable(false);
			_dlg.setCanceledOnTouchOutside(false);
			_dlg.setButton(ctx.getString(android.R.string.cancel), this);
			_dlg.show();

		}

		private void writeBarcodes(long id, int type) throws IOException {
			String sql = "SELECT CODE FROM BARCODES WHERE OWNER_ID=" + id + " AND OWNER_TYPE=" + type;
			Cursor c1 = Core.getInstance().db().getReadableDatabase().rawQuery(sql, null);
			if (c1.moveToFirst()) {
				_os.write("    <Barcodes>\n".getBytes());
				do {
					_os.write(("     <Barcode Value=\"" + c1.getString(0) + "\"/>\n").getBytes());
				} while (c1.moveToNext());
				_os.write("    </Barcodes>\n".getBytes());
			}
			c1.close();

		}

		@Override
		public void run() {
			try {
				_os.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n".getBytes());
				_os.write(
						"<ExportData xmlns=\"http://www.1c.ru/EquipmentService/3.0.0.3\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" FormatVersion=\"3003\">\n"
								.getBytes());
				_os.write("<Settings>\n".getBytes());
				KKMInfo info = Core.getInstance().kkmInfo();
				if (info.isFNActive()) {
					_os.write((" <CompanyName>" + info.getOwner().getName() + "</CompanyName>\n").getBytes());
					_os.write((" <INN>" + info.getOwner().getINN() + "</INN>\n").getBytes());
					_os.write((" <SenderEmail>" + info.getSenderEmail() + "</SenderEmail>\n").getBytes());
					_os.write(" <TaxationSystems>\n".getBytes());
					for (TaxModeE m : info.getTaxModes())
						_os.write(("  <TaxationSystem>" + m.bVal + "</TaxationSystem>\n").getBytes());
					_os.write(" </TaxationSystems>\n".getBytes());
				}
				_os.write("</Settings>\n".getBytes());
				_os.write("<PriceList>\n".getBytes());
				SQLiteDatabase db = Core.getInstance().db().getReadableDatabase();
				_os.write("<Units>\n".getBytes());
				TIntObjectMap<String> measures = new TIntObjectHashMap<>();
				for(MeasureTypeE mu : MeasureTypeE.values()) {
						String uuid = UUID.randomUUID().toString();
						measures.put(mu.ordinal(), uuid);
						_os.write((" <Unit Code=\"" + mu.OKEI + "\" Name=\"" + mu.name() + "\" UUID=\""
								+ uuid + "\" />\n").getBytes());
					}
					_os.write("</Units>\n".getBytes());
				Cursor c = db.rawQuery(
						"select A.CODE, A.ITEM_TYPE, A.VAT, A.NAME, A.UUID,   A.PRICE, A.MARK_TYPE, A.MU, COUNT(D._id), COUNT(C._id), A._id AS BCODES from GOODS A "
								+ "LEFT OUTER JOIN VARIANTS D on D.GOOD_ID = A._id "
								+ "LEFT OUTER JOIN BARCODES C on C.OWNER_ID = A._id AND C.OWNER_TYPE = 0 WHERE A.USED = 1 GROUP BY A._id",
						null);
				if (c.moveToFirst()) {
					_os.write("<Goods>\n".getBytes());
					do {
						VatE vat = VatE.values()[c.getInt(2)];
						String sVat = "20";
						switch (vat) {
						case VAT_0:
							sVat = "0";
							break;
						case VAT_10_110:
						case VAT_10:
							sVat = "10";
							break;
						case VAT_20:
						case VAT_20_120:
							sVat = "20";
							break;
						case VAT_NONE:
							sVat = "none";
							break;
						}
						long id = c.getLong(10);
						_os.write(("<Good Code=\"" + String.format("%05d", id) + "\" Article=\"" + c.getString(0)
								+ "\" CalculationSubject=\"" + (c.getInt(1) + 1) + "\" " + "TaxRate=\"" + sVat
								+ "\" Name=\"" + c.getString(3) + "\" UUID=\"" + c.getString(4) + "\" ").getBytes());
						if (c.getDouble(5) > 0)
							_os.write(String.format(Locale.ROOT, "Price=\"%.2f\" ", c.getDouble(5)).getBytes());
						if (c.getInt(6) != 0) {
							_os.write("IsMarked=\"true\" ".getBytes());
							_os.write(("MarkedGoodTypeCode=\"" + c.getInt(6) + "\" ").getBytes());
						}
						if (c.getInt(8) > 0)
							_os.write("HasPacks=\"true\" ".getBytes());
						String uuuid = measures.get(c.getInt(7));
						_os.write(("UnitUUID=\"" + uuuid + "\" >\n").getBytes());
						if (c.getInt(9) > 0)
							writeBarcodes(id, 0);
						if (c.getInt(8) > 0) {
							Cursor c1 = db.rawQuery("SELECT _id, NAME, PRICE, QTTY,UUID,MU  FROM  VARIANTS  "+
									"WHERE GOOD_ID=" + id, null);
							if (c1.moveToFirst()) {
								_os.write("<Packs>\n".getBytes());
								do {
									uuuid = measures.get(c1.getInt(5));
									_os.write(("<Pack UUID=\""+c1.getString(4)+"\" Code=\"" + String.format("%05d", c1.getLong(0)) + "\" Name=\""
											+ c1.getString(1) + "\" Price=\""
											+ String.format(Locale.ROOT, "%.2f", c1.getDouble(2)) + "\" UnitsPerPack=\""
											+ String.format(Locale.ROOT, "%.3f", c1.getDouble(3)) + "\" UnitUUID=\"" + uuuid + "\" >\n")
											.getBytes());
									writeBarcodes(c1.getLong(0), 1);
									_os.write("</Pack>\n".getBytes());
								} while (c1.moveToNext());
								_os.write("</Packs>\n".getBytes());
							}
							c1.close();
						}
						_os.write("</Good>\n".getBytes());
					} while (c.moveToNext());
					_os.write("</Goods>\n".getBytes());
				}
				c.close();
				_os.write("</PriceList>\n".getBytes());
				_os.write("</ExportData>".getBytes());
				sendMsg(MSG_FINISH_OK, null);
			} catch (IOException ioe) {
				sendMsg(MSG_FINISH_ERROR, ioe.getLocalizedMessage());
			}

		}

		private void sendMsg(int what, Object o) {
			Message msg = _h.obtainMessage(what);
			msg.obj = o;
			_h.sendMessage(msg);
		}

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_FINISH_OK:
				_dlg.dismiss();
				U.notify(_dlg.getContext(), "Экспорт успешно завершен");
				try {
					_os.close();
				} catch (IOException ioe) {
				}
				break;
			case MSG_FINISH_ERROR:
				_dlg.dismiss();
				U.notify(_dlg.getContext(), "Экспорт завершен с ошибкой: " + msg.obj.toString());
				try {
					_os.close();
				} catch (IOException ioe) {
				}
				break;
			case MSG_PROGRESS:
				_dlg.setMessage("Выполнено " + msg.arg1 + " из " + msg.arg2);
				break;
			}
			return true;
		}

		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			interrupt();
		}
	}

	public static void doExport(Context ctx, OutputStream os) {
		new ExportThread(ctx, os).start();

	}

}
