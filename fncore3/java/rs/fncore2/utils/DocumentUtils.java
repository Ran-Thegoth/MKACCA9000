package rs.fncore2.utils;

import android.util.Log;
import rs.fncore.data.ArchiveReport;
import rs.fncore.data.Correction;
import rs.fncore.data.Document;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.Shift;
import rs.fncore.data.Tag;
import rs.fncore2.PrintHelper;

public class DocumentUtils {
	public static String getPrintForm(Tag tag) {
		Document doc;
		if(tag instanceof Document)
			doc = (Document)tag;
		else
			doc = tag.createInstance();
		if(doc == null)	{
			Log.d("Web", "no document");
			return null;
			
		}
		Log.d("Web", "Class "+doc.getClass().getName());
		if(KKMInfo.class.isAssignableFrom(doc.getClass()))  {
			return PrintHelper.processTemplate(PrintHelper.loadTemplate(null, "registration"),
	                doc);
		} else if(Shift.class.isAssignableFrom(doc.getClass())) {
			return PrintHelper.processTemplate(PrintHelper.loadTemplate(null, "shift"),
	                doc);
		}
		 else if(FiscalReport.class.isAssignableFrom(doc.getClass())) {
			 return PrintHelper.processTemplate(PrintHelper.loadTemplate(null, "fiscalreport"),
		                doc);
		 }
		 else if(ArchiveReport.class.isAssignableFrom(doc.getClass())) {
			 return PrintHelper.processTemplate(PrintHelper.loadTemplate(null, "archive"),
		                doc);
		 }
		 else if(Correction.class.isAssignableFrom(doc.getClass())) {
		        String header = PrintHelper.loadTemplate(null, "correction2_header");
		        String item = PrintHelper.loadTemplate(null, "sale_item");
		        String footer = PrintHelper.loadTemplate(null, "correction2_footer");
		        String s = PrintHelper.processTemplate(header, doc);
		        for (SellItem i : ((Correction)doc).getItems())
		            s += PrintHelper.processTemplate(item, i);
		        s += PrintHelper.processTemplate(footer, doc);
		        return s;
		 }
		 else if(SellOrder.class.isAssignableFrom(doc.getClass())) {
		        String header = PrintHelper.loadTemplate(null, "sale_header");
		        String item = PrintHelper.loadTemplate(null, "sale_item");
		        String footer = PrintHelper.loadTemplate(null, "sale_footer");
		        String s = PrintHelper.processTemplate(header, doc);
		        for (SellItem i : ((SellOrder)doc).getItems())
		            s += PrintHelper.processTemplate(item, i);
		        s += PrintHelper.processTemplate(footer, doc);
		        return s;
			 
		 } else if(FNCounters.class.isAssignableFrom(doc.getClass()) ) {
			 return  PrintHelper.processTemplate(PrintHelper.loadTemplate(null, "fn_counters"),doc);
		 }
		return null;
		
	}

}
