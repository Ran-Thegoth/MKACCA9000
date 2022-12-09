package rs.formatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Base64;

public class Formatter {

	public static final int E_FORMAT_OK = 0;
	public static final int E_FORMAT_FAIL = 0x1000000;

	public static final int UNDERLINE = 0x10;
	public static final int STRIKEOUT = 0x20;
	public static final int INVERSE = 0x40;
	

	public static final int ALIGN_LEFT = 0x1;
	public static final int ALIGN_CENTER = 0x2;
	public static final int ALIGN_RIGHT = 0x4;

	public static final int VALIGN_TOP = 0x10;
	public static final int VALIGN_CENTER = 0x20;
	public static final int VALIGN_BOTTOM = 0x40;
	public static final int PERCENT_MASK = 0x10000;

	private static final String BLOCK_TAG = "p";
	private static final String STYLE_TAG = "s";
	private static final String BARCODE_TAG = "barcode";
	private static final String TABLE_TAG = "table";
	private static final String TR_TAG = "tr";
	private static final String TD_TAG = "td";
	private static final String IMAGE_TAG = "image";
	private static final String FIELD_TAG = "fd";
	private static final String PARAM_TAG = "pp";
	private static final String CUT = "cut";
	private static final String FF = "ff";

	public long WHEN;

	private Map<String, String> ARGS = new HashMap<>();
	private static final Pattern EQUAL_P = Pattern.compile("(.+)=(.+)");
	private static final Pattern NOT_EQUAL_P = Pattern.compile("(.+)!=(.+)");
	private static final Pattern LESS_P = Pattern.compile("(.+)<(.+)");
	private static final Pattern LESS_EQ_P = Pattern.compile("(.+)<=(.+)");
	private static final Pattern GREAT_P = Pattern.compile("(.+)>(.+)");
	private static final Pattern GREAT_EQ_P = Pattern.compile("(.+)>=(.+)");

	private enum CCheck {
		equal, not_equal, less, less_eq, great, great_eq
	}

	public class PrintParams {
		public int W, H = -1;
		public float DPI = 203f;
		public int DENSITY = 1;
		private String parse(Formatter f, String line) {
			String s = "";
			while (!line.isEmpty() && line.charAt(0) != '\\' && line.charAt(0) != '\n') {
				s += line.charAt(0);
				line = line.substring(1);
			}
			if (line.startsWith("\\"))
				line = line.substring(1);
			for (String p : s.trim().split(";")) {
				if (p.isEmpty())
					continue;
				String[] kv = p.split(":");
				if (kv.length > 1)
					try {
						kv[0] = kv[0].trim();
						kv[1] = kv[1].trim();
						if("width".equals(kv[0])) try {
							W = Integer.parseInt(kv[1]);
						} catch(NumberFormatException nfe) { }
						else if("height".equals(kv[0])) try {
							H = Integer.parseInt(kv[1]);
						} catch(NumberFormatException nfe) { }
						else if("dpi".equals(kv[0])) try {
							DPI = Float.parseFloat(kv[1]);
						} catch(NumberFormatException nfe) { }
						else if("density".equals(kv[0])) try {
							DENSITY = Integer.parseInt(kv[1]);
						} catch(NumberFormatException nfe) { }
						
					} catch (Exception e) {
					}
			}
			return line;
		}
	}
	
	public class PostProcess {
		public boolean doCut;
		public boolean doFF;
	}
	
	private class Condition {
		private String lval = null, rval = null;
		private CCheck check = null;

		public boolean check(Map<String, String> args) {
			if (lval == null || rval == null || check == null) {
				return true;
			}
			if (args.containsKey(lval))
				lval = args.get(lval);
			if (args.containsKey(rval))
				rval = args.get(rval);
			switch (check) {
			case equal:
				return lval.equals(rval);
			case not_equal:
				return !lval.equals(rval);
			case less:
				try {
					return Double.parseDouble(lval.replace(",", ".")) < Double.parseDouble(rval.replace(",", "."));
				} catch (Exception nfe) {
					return false;
				}
			case less_eq:
				try {
					return Double.parseDouble(lval.replace(",", ".")) <= Double.parseDouble(rval.replace(",", "."));
				} catch (Exception nfe) {
					return false;
				}
			case great:
				try {
					return Double.parseDouble(lval.replace(",", ".")) > Double.parseDouble(rval.replace(",", "."));
				} catch (Exception nfe) {
					return false;
				}
			case great_eq:
				try {
					return Double.parseDouble(lval.replace(",", ".")) >= Double.parseDouble(rval.replace(",", "."));
				} catch (Exception nfe) {
					return false;
				}
			}
			return true;
		}

		public void parse(String s) {
			if (s == null || s.isEmpty())
				return;
			Matcher m = null;
			try {
				m = NOT_EQUAL_P.matcher(s);
				if (m.find()) {
					check = CCheck.not_equal;
					return;
				}
				m = LESS_EQ_P.matcher(s);
				if (m.find()) {
					check = CCheck.less_eq;
					return;
				}
				m = GREAT_EQ_P.matcher(s);
				if (m.find()) {
					check = CCheck.great_eq;
					return;
				}
				m = GREAT_P.matcher(s);
				if (m.find()) {
					check = CCheck.great;
					return;
				}
				m = LESS_P.matcher(s);
				if (m.find()) {
					check = CCheck.less;
					return;
				}
				m = EQUAL_P.matcher(s);
				if (m.find()) {
					check = CCheck.equal;
					return;
				}
				m = null;
			} finally {
				if (m != null) {
					lval = m.group(1);
					rval = m.group(2);
					if (lval != null)
						lval = lval.replaceAll("\"", "");
					if (rval != null)
						rval = rval.replaceAll("\"", "");
				}
			}

		}
	}

	public interface IPrinter {
		public int getHeight(Style style, String string);

		public int getWidth(Style style, String string);

		public int printString(int x, int y, Style s, String string);

		public int printBarcode(int x, int y, int w, int h, BarcodeFormat fmt, String content, int rotation);

		public int drawLine(int x1, int y1, int x2, int y2, int w);

		public int printImage(int x, int y, int w, int h, Bitmap b);
		public int barcodeHeight(BarcodeFormat fmt, int w, int h);
		public void fill(int x1, int y1, int x2, int y2);

		public void clip(int X, int Y, int X1, int Y1);
		public void clearClip();
		public void prePrint(PrintParams params);
		public void postPrint(int y, PostProcess pp);

	}

	public interface IPrintable {
		public void dump(String padding);

		public void measure(IPrinter printer) throws Exception;

		public int getHeight();

		public int getWidth();

		public int print(IPrinter printer, int x, int y);
	}

	public interface IStyleable {
		public Condition getContidion();

		public void parse(String key, String value);

		public IStyleable getParent();

		public Style getStyle();
	}

	private String parseStyleableArgs(String line, IStyleable object) {
		String s = "";
		while (!line.isEmpty() && line.charAt(0) != '\\' && line.charAt(0) != '\n') {
			s += line.charAt(0);
			line = line.substring(1);
		}
		if (line.startsWith("\\"))
			line = line.substring(1);
		for (String p : s.trim().split(";")) {
			if (p.isEmpty())
				continue;
			String[] kv = p.split(":");
			if (kv.length > 1)
				try {
					object.parse(kv[0].trim(), kv[1].trim());
				} catch (Exception e) {
				}
		}
		return line;
	}

	private void parseBlock(String key, String value, ContentBlock block) {
		if ("padding".equals(key)) {
			String[] v = value.split(",");
			for (int i = 0; i < v.length; i++)
				try {
					block.padding[i] = Integer.parseInt(v[i]);
				} catch (NumberFormatException nfe) {
				}
		} else if ("border".equals(key)) {
			String[] v = value.split(",");
			for (int i = 0; i < v.length; i++)
				try {
					block.border[i] = Integer.parseInt(v[i]);
				} catch (NumberFormatException nfe) {
				}
		} else if ("if".equals(key)) {
			if (block.getContidion() != null)
				block.getContidion().parse(value);
		} else if ("width".equals(key))
			try {
				block.W = Integer.parseInt(value.replace("%", ""));
				if (value.contains("%"))
					block.W |= PERCENT_MASK;
			} catch (NumberFormatException nfe) {

			}
		else if ("height".equals(key))
			try {
				block.H = Integer.parseInt(value);
			} catch (NumberFormatException nfe) {

			}
		else if ("align".equals(key)) {
			block.align &= ~0xF;
			if ("left".equals(value))
				block.align |= ALIGN_LEFT;
			if ("center".equals(value))
				block.align |= ALIGN_CENTER;
			if ("right".equals(value))
				block.align |= ALIGN_RIGHT;
		} else if ("valign".equals(key)) {
			block.align &= ~0xF0;
			if ("top".equals(value))
				block.align |= VALIGN_TOP;
			if ("center".equals(value))
				block.align |= VALIGN_CENTER;
			if ("bottom".equals(value))
				block.align |= VALIGN_BOTTOM;
		} else if("xo".equals(key)) try {
			block.XOffset = Integer.parseInt(value);
		} catch(NumberFormatException nfe) {
			
		} else if("yo".equals(key)) try {
			block.YOffset = Integer.parseInt(value);
		} catch(NumberFormatException nfe) {
			
		} 
		else
			parseStyle(key, value, block);
	}

	private void parseStyle(String key, String value, IStyleable stylable) {
		if ("fontSize".equals(key))
			try {
				stylable.getStyle().fontSize = Integer.parseInt(value.replace("%", ""));
				if (value.contains("%"))
					stylable.getStyle().fontSize = (int) (stylable.getParent().getStyle().fontSize / 100f
							* stylable.getStyle().fontSize);
			} catch (NumberFormatException nfe) {
			}
		else if ("inverse".equals(key)) {
			if("yes".equals(value) || "on".equals(value))
				stylable.getStyle().flags |= INVERSE;
			else 
				stylable.getStyle().flags &= ~INVERSE;
		}
		else if ("fontName".equals(key)) {
			stylable.getStyle().fontName = value;
		} else if ("nobreak".equals(key)) {
			stylable.getStyle().NB = "true".equals(value);
		} else if ("rotation".equals(key)) {
			try {
				stylable.getStyle().rotation = Integer.parseInt(value);
			} catch (NumberFormatException nfe) {
			}
		} else if ("if".equals(key)) {
			if (stylable.getContidion() != null)
				stylable.getContidion().parse(value);
		} else if ("style".equals(key)) {
			if (value.contains("normal"))
				stylable.getStyle().flags = 0;
			else {
				if (value.contains("bold"))
					stylable.getStyle().flags |= Typeface.BOLD;
				if (value.contains("italic"))
					stylable.getStyle().flags |= Typeface.ITALIC;
				if (value.contains("strikeout"))
					stylable.getStyle().flags |= STRIKEOUT;
				if (value.contains("underline"))
					stylable.getStyle().flags |= UNDERLINE;
			}
		}

	}

	public class Style {
		public int fontSize;
		public int flags;
		public int rotation;
		boolean NB;
		public String fontName;

		public Style(String fontName, int fontSize) {
			this.fontName = fontName;
			this.fontSize = fontSize;
		}

		public Style(Style parent) {
			this.fontName = parent.fontName;
			this.fontSize = parent.fontSize;
			this.flags = parent.flags;
			this.rotation = parent.rotation;
		}
	}

	
		
	public class Text implements IStyleable, IPrintable {
		private Style STYLE;
		private Condition con = new Condition();
		protected int W, H;
		protected ContentBlock OWNER;
		protected String TEXT;

		@Override
		public Condition getContidion() {
			return con;
		}

		public Text() {

		}

		public Text(ContentBlock owner) {
			STYLE = new Style(owner.CURRENT_STYLE);
			owner.STYLE_STACK.add(owner.CURRENT_STYLE);
			owner.CURRENT_STYLE = STYLE;
			OWNER = owner;
		}

		public void addChar(char ch) {
			if (TEXT == null) {
				OWNER.CURRENT_LINE.add(this);
				TEXT = String.valueOf(ch);
			} else
				TEXT += ch;
		}

		public Text clone() {
			Text result = new Text();
			result.OWNER = OWNER;
			result.STYLE = OWNER.CURRENT_STYLE;
			return result;
		}

		@Override
		public void parse(String key, String value) {
			parseStyle(key, value, this);
		}

		@Override
		public Style getStyle() {
			return STYLE;
		}

		@Override
		public void dump(String padding) {
		}

		@Override
		public void measure(IPrinter printer) {
			if(getContidion()!=null && !getContidion().check(ARGS)) return;
			W = printer.getWidth(STYLE, TEXT);
			H = printer.getHeight(STYLE, TEXT);
		}

		@Override
		public int getHeight() {
			return H;
		}

		@Override
		public int getWidth() {
			return W;
		}

		@Override
		public int print(IPrinter printer, int x, int y) {
			if(getContidion()!=null && !getContidion().check(ARGS)) return 0;
			return printer.printString(x, y, STYLE, TEXT);
		}

		@Override
		public IStyleable getParent() {
			return OWNER;
		}
	}

	
	public class Barcode extends Text {
		public BarcodeFormat FORMAT = BarcodeFormat.CODE_128;

		public Barcode(ContentBlock owner) {
			super(owner);
			W = 300;
			H = -1;
		}

		@Override
		public Text clone() {
			Text t = new Text();
			t.OWNER = OWNER;
			t.STYLE = OWNER.CURRENT_STYLE;
			return t;
		}

		@Override
		public void parse(String key, String value) {
			if ("type".equals(key)) {
				if ("ean13".equals(value))
					FORMAT = BarcodeFormat.EAN_13;
				else if ("ean8".equals(value))
					FORMAT = BarcodeFormat.EAN_8;
				else if ("code39".equals(value))
					FORMAT = BarcodeFormat.CODE_39;
				else if ("code93".equals(value))
					FORMAT = BarcodeFormat.CODE_93;
				else if ("qr".equals(value))
					FORMAT = BarcodeFormat.QR_CODE;
				else if ("dm".equals(value))
					FORMAT = BarcodeFormat.DATA_MATRIX;
			} else if ("width".equals(key))
				try {
					W = Integer.parseInt(value.replace("%", ""));
					if (value.endsWith("%"))
						W *= -1;
				} catch (NumberFormatException nfe) {
				}
			else if ("height".equals(key))
				try {
					H = Integer.parseInt(value.replace("%", ""));
					if (value.endsWith("%"))
						H *= -1;
				} catch (NumberFormatException nfe) {
				}
			else
				super.parse(key, value);
		}

		@Override
		public void dump(String padding) {
			
		}

		@Override
		public void measure(IPrinter printer) {
			if (W < 0 && OWNER.W > 0)
				W = (int) (OWNER.W / 100f * (W * -1));
			else if (H < 0 && OWNER.H > 0)
				H = (int) (OWNER.H / 100f * (H * -1));
			if ((W > 0 || H > 0) && (FORMAT == BarcodeFormat.DATA_MATRIX || FORMAT == BarcodeFormat.QR_CODE)) {
				W = Math.max(H, W);
				H = W;
			}
			H = printer.barcodeHeight(FORMAT, W, H);
		}

		@Override
		public int print(IPrinter printer, int x, int y) {
			if(!getContidion().check(null)) return 0;
			if (FORMAT == BarcodeFormat.EAN_13) {
				while (TEXT.length() < 13)
					TEXT = "0" + TEXT;
			}
			if(TEXT != null) {
				printer.printBarcode(x, y, W, H, FORMAT, TEXT,getStyle().rotation);
				return H;
			}
			return 0;
		}
	}

	public class Image extends Text {
		private Bitmap B;
		private int _scale = 0;
		public Image(ContentBlock owner) {
			super(owner);
			W = H = -1;
		}

		@Override
		public Text clone() {
			Text t = new Text();
			t.OWNER = OWNER;
			t.STYLE = OWNER.CURRENT_STYLE;
			return t;
		}

		@Override
		public void parse(String key, String value) {
			if ("width".equals(key)) {
				W = Integer.parseInt(value.replace("%", ""));
				if (value.contains("%"))
					W |= PERCENT_MASK;
			} else if ("height".equals(key)) {
				H = Integer.parseInt(value.replace("%", ""));
			}
			else if("scale".equals(key)) {
				if("none".equals(value))
					_scale = 0;
				else if("fitX".equals(value))
					_scale = 1;
				else if("fitY".equals(value))
					_scale = 2;
				else if("fitXY".equals(value))
					_scale = 3;
				else if("center".equals(value)) {
					_scale = 4;
				}
				
				
			} else
				super.parse(key, value);
		}

		@Override
		public void measure(IPrinter printer) {
			Bitmap b = getBitmap();
			if (b == null) {
				H = W = 0;
				return;
			}
			if (W == -1)
				W = OWNER.W;
			else if ((W & PERCENT_MASK) == PERCENT_MASK) {
				W &= ~PERCENT_MASK;
				W = (int) (OWNER.W / 100 * (float) W);
			}
			if (H == -1) {
				float scale = W / (float) b.getWidth();
				H = Math.min((int) (b.getHeight() * scale),OWNER.H);
			}
			if (getStyle().rotation == 90 || getStyle().rotation == 270) {
				int t = H;
				H = W;
				W = t;
			}
		}

		Bitmap getBitmap() {
			return B;
		}

		@Override
		public void dump(String padding) {
		}

		@Override
		public int print(IPrinter printer, int x, int y) {
			
			if (B != null && getContidion().check(null)) {
				Bitmap res = B;
				Matrix M = null;
				switch(_scale) {
				case 1:
					M = new Matrix();
					M.postScale((float)W/(float)B.getWidth(), 1);
					break;
				case 2:
					M = new Matrix();
					M.postScale(1, (float)H/(float)B.getHeight());
					break;
				case 3:
					M = new Matrix();
					M.postScale((float)W/(float)B.getWidth(), (float)H/(float)B.getHeight());
					break;
				case 4:
					M = new Matrix();
					float minH = Math.min(W, H);
					float minB = Math.min(B.getWidth(), B.getHeight());
					float sc =minH/minB;
					M.postScale(sc, sc);
					if(B.getWidth()*sc < W ) 
						M.postTranslate((W - B.getWidth()*sc)/2, 0);
					else 
						M.postTranslate(0,(H - B.getHeight()*sc)/2);
					break;
				}
				if(M != null) {
					res = Bitmap.createBitmap(W, H, Config.ARGB_8888);
					Canvas c = new Canvas(res);
					c.drawBitmap(B, M, new Paint(Paint.FILTER_BITMAP_FLAG));
				}
				printer.printImage(x, y, W, H, res);
				int h = res.getHeight();
				if(res != B)
					res.recycle();
				return h;
			}
			return 0;
		}

		public String load(String line) {
			String l = parseStyleableArgs(line, this);
			while (l.startsWith("\n") || l.startsWith("\r") || l.startsWith(" "))
				l = l.substring(1);
			int end = l.indexOf("}");
			if (end > -1) {
				try {
					byte[] b = Base64.decode(l.substring(0, end), Base64.DEFAULT);
					B = BitmapFactory.decodeByteArray(b, 0, b.length);
				} catch (Exception e) {
					e.printStackTrace();
				}
				OWNER.CURRENT_LINE.add(this);
				l = l.substring(end);
			}
			return l;
		}

	}

	public class Line extends ArrayList<Text> implements IPrintable {

		private static final long serialVersionUID = 1L;
		private int H = 0;
		private ContentBlock OWNER;

		public Line(ContentBlock owner) {
			owner.addLine(this);
			OWNER = owner;
		}

		public Line(ContentBlock owner, boolean noadd) {
			OWNER = owner;
		}

		@Override
		public void dump(String padding) {
			for(Text s: this) 
				s.dump(padding+" ");
			
		}

		@Override
		public void measure(IPrinter printer) {
			int h = 0;
			for (Text text : this) {
				text.measure(printer);
				h = Math.max(h, text.H);
			}
			H = h;
		}

		@Override
		public int getHeight() {
			return H;
		}

		@Override
		public int getWidth() {
			return OWNER.W;
		}

		@Override
		public int print(IPrinter printer, int x, int y) {
			int h = 0;
			if ((OWNER.align & ALIGN_CENTER) == ALIGN_CENTER) {
				int w = 0;
				for (Text t : this)
					w += t.getWidth();
				x += (OWNER.W - w) / 2;
			}
			if ((OWNER.align & ALIGN_CENTER) == ALIGN_CENTER || (OWNER.align & ALIGN_LEFT) == ALIGN_LEFT) {
				for (Text t : this) {
					h += t.print(printer, x, y + (H - t.H));
					x += t.getWidth();
				}
			} else {
				int w = x + OWNER.W - OWNER.padding[2];
				for (int i = size() - 1; i >= 0; i--) {
					Text t = get(i);
					w -= t.getWidth();
					h += t.print(printer, w, y + (H - t.H));
				}
			}
			return h;
		}

	}

	private Style DEFAULT_STYLE;

	public class ContentBlock implements IPrintable, IStyleable {
		public int W = -1;
		public int H = -1;
		public int X = 0, Y = 0;
		public int XOffset = 0, YOffset = 0;
		public int align = ALIGN_LEFT | VALIGN_TOP;
		protected List<IPrintable> LINES = new ArrayList<>();
		protected Style CURRENT_STYLE;
		private Stack<Style> STYLE_STACK = new Stack<>();
		protected ContentBlock PARENT;
		private Line CURRENT_LINE = new Line(this);
		private Text CURRENT_TEXT;
		protected int[] padding = { 0, 0, 0, 0 };
		protected int[] border = { 0, 0, 0, 0 };
		private Condition cond = new Condition();

		private boolean firstParse = true;

		@Override
		public Condition getContidion() {
			return cond;
		}

		public ContentBlock(ContentBlock parent) {
			CURRENT_STYLE = new Style(parent == null ? DEFAULT_STYLE : parent.CURRENT_STYLE);
			PARENT = parent;
			if (PARENT != null)
				PARENT.LINES.add(this);
			CURRENT_TEXT = new Text();
			CURRENT_TEXT.STYLE = CURRENT_STYLE;
			CURRENT_TEXT.OWNER = this;
		}

		public ContentBlock close() {
			if (STYLE_STACK.isEmpty())
				return PARENT == null ? this : PARENT;
			else {
				CURRENT_STYLE = STYLE_STACK.pop();
				CURRENT_TEXT = CURRENT_TEXT.clone();
				return this;
			}
		}

		@Override
		public void dump(String padding) {
//			Log.d("FMT", padding+" AT "+X+","+Y+":");
			for(IPrintable s: LINES) 
				s.dump(padding);
		}

		public void addLine(Line l) {
			LINES.add(l);
		}

		public void addChar(char ch) {
			switch (ch) {
			case '\r':
				break;
			case '\n':
				if(this instanceof Row) return;
				CURRENT_LINE = new Line(this);
				CURRENT_TEXT = CURRENT_TEXT.clone();
				break;
			default:
				CURRENT_TEXT.addChar(ch);
				break;
			}
		}

		public String parseArgs(String line) {
			if (firstParse) {
				String l = parseStyleableArgs(line, this);
				firstParse = false;
				while (l.startsWith("\n") || l.startsWith("\r"))
					l = l.substring(1);
				return l;
			} else if (CURRENT_TEXT instanceof Image)
				return ((Image) CURRENT_TEXT).load(line);

			else
				return parseStyleableArgs(line, CURRENT_TEXT);
		}

		@Override
		public void measure(IPrinter printer) throws Exception {
			if (getContidion() != null && !getContidion().check(ARGS))
				return;
			if (W == -1)
				W = PARENT.W;
			else if ((W & PERCENT_MASK) == PERCENT_MASK) {
				W &= ~PERCENT_MASK;
				W = (int) (PARENT.W / 100f * (float) W);
			}
			int h = 0;
			if (W == 0)
				return; // throw new
						// Exception("Width of "+getClass().getSimpleName()+" is 0");
			if (getStyle().rotation == 90 || getStyle().rotation == 270) {
				int t = W;
				W = H;
				H = t;
				if (W == -1) {
					ContentBlock c = PARENT;
					while (c != null) {
						if (c.H > 0) {
							W = c.H;
							break;
						}
						c = c.PARENT;
					}
				}
			}
			try {
				for (int i = 0; i < LINES.size(); i++) {
					IPrintable p = LINES.get(i);
					if (p instanceof Line) {
						Line line = (Line) p;
						int w = 0;
						for (int ii = 0; ii < line.size(); ii++) {
							Text t = line.get(ii);
							t.measure(printer);
							line.H = Math.max(line.H, t.getHeight());
							if (w + t.getWidth() > W - padding[0] - padding[2]) {
								if (t instanceof Barcode || t instanceof Image) {
									t.W = W - padding[0] - padding[2];

								} else {
									Line nLine = new Line(this, true);
									LINES.add(i + 1, nLine);
									String s = t.TEXT;

									if (!t.STYLE.NB) {
										if (s.length() < 2)
											continue;
										int m = s.length() - 1;
										for (; m >= 0; m--) {
											char c = s.charAt(m);
											if (" .!?-,=;+/".indexOf(c) > -1) {
												if (printer.getWidth(t.STYLE, s.substring(0, m + 1)) <= W - padding[0]
														- padding[2]) {
													s = s.substring(0, m + 1);
													break;
												}
											}
										}
										if (s.equals(t.TEXT))
											while (s.length() > 0
													&& w + printer.getWidth(t.STYLE, s) > W - padding[0] - padding[2]) {
												s = s.substring(0, s.length() - 1);
											}
										String left = t.TEXT.substring(s.length());
										t.TEXT = s;
										Text t1 = t.clone();
										t1.STYLE = t.STYLE;
										t1.TEXT = left;
										nLine.add(t1);
									} else {
										while (s.length() > 0
												&& w + printer.getWidth(t.STYLE, s) > W - padding[0] - padding[2]) {
											s = s.substring(0, s.length() - 1);
										}
										t.TEXT = s;

									}
									for (int jj = line.size() - 1; jj > ii; jj--) {
										nLine.add(1, line.remove(jj));
									}
									measure(printer);
									return;
								}
							}
							w += t.getWidth();
						}
					} else
						p.measure(printer);
					h += p.getHeight();
				}
				if (H == -1)
					H = h;
			} finally {
				if (getStyle().rotation == 90 || getStyle().rotation == 270) {
					int t = W;
					H = t;
				}
			}
		}

		@Override
		public int getHeight() {
			return H;
		}

		@Override
		public int getWidth() {
			return W;
		}

		@Override
		public int print(IPrinter printer, int x, int y) {
			if (getContidion() != null && !getContidion().check(ARGS))
				return 0;
			if (W == 0)
				return 0;
			int ly = y + padding[1], lx = x + padding[0], lh = H;
			if ((align & VALIGN_CENTER) == VALIGN_CENTER) {
				int h = 0;
				for (IPrintable p : LINES)
					h += p.getHeight();
				ly += (lh - h) / 2f;
			}
			int h = 0;
			if ((align & VALIGN_CENTER) == VALIGN_CENTER || (align & VALIGN_TOP) == VALIGN_TOP) {
				for (IPrintable p : LINES) {
					int h0 = p.print(printer, lx+XOffset, ly+YOffset); 
					h+= h0;
					ly += h0;
				}
			} else {
				ly += H - padding[3];
				for (int i = LINES.size() - 1; i >= 0; i--) {
					IPrintable p = LINES.get(i);
					ly -= p.getHeight();
					h+=p.print(printer, lx+XOffset, ly+YOffset);
				}
			}
			return h;
		}

		@Override
		public void parse(String key, String value) {
			parseBlock(key, value, this);
		}

		@Override
		public Style getStyle() {
			return CURRENT_STYLE;
		}

		@Override
		public IStyleable getParent() {
			return PARENT;
		}
	}

	public class Table extends ContentBlock implements IStyleable {
		public Table(ContentBlock parent) {
			super(parent);
		}

		@Override
		public void addLine(Line l) {
		}

		@Override
		public void addChar(char ch) {
		}

		@Override
		public void dump(String padding) {
		}

		@Override
		public int print(IPrinter printer, int x, int y) {
			if (getContidion() != null && !getContidion().check(ARGS))
				return 0;
			int h = 0;
			for (IPrintable p : LINES) {
				h += p.print(printer, x, y+h);
			}
			if (border[0] > 0)
				printer.drawLine(x, y, x, y + h+1, border[0]);
			if (border[1] > 0)
				printer.drawLine(x, y, x + W+1, y, border[1]);
			if (border[2] > 0)
				printer.drawLine(x + W-1, y, x + W-1, y + h, border[2]);
			if (border[3] > 0)
				printer.drawLine(x, y + h, x + W, y + h, border[3]);
			return h;
		}

		@Override
		public void measure(IPrinter printer) throws Exception {
			super.measure(printer);
			if(border[1] > 0) H +=  border[1];
			if(border[3] > 0) H +=  border[3];
		}

	}

	public class Row extends ContentBlock {
		public Row(ContentBlock parent) {
			super(parent);
		}

		@Override
		public void addLine(Line l) {
		}

		@Override
		public void addChar(char ch) {
		}
		
		@Override
		public void dump(String padding) {
		}

		@Override
		public void measure(IPrinter printer) throws Exception {
			if (getContidion() != null && !getContidion().check(ARGS))
				return;
			W = PARENT.W;
			int w = W;
			int h = 0;
			for (IPrintable cell : LINES) {
				cell.measure(printer);
				W -= cell.getWidth();
				h = Math.max(h, cell.getHeight());
			}
			H = h;
			for (IPrintable cell : LINES) {
				if (cell instanceof Cell)
					((Cell) cell).H = H;
			}
			W = w;
			if(border[1] > 0) H +=  border[1];
			if(border[3] > 0) H +=  border[3];
		}

		@Override
		public int print(IPrinter printer, int x, int y) {
			if (getContidion() != null && !getContidion().check(ARGS))
				return 0;
			int w = x;
			int h = 0;
			for (IPrintable cell : LINES) {
				h = Math.max(h,cell.print(printer, w, y));
				w += cell.getWidth();
			}
			if (border[0] > 0)
				printer.drawLine(x, y, x, y + H + 1, border[0]);
			if (border[1] > 0)
				printer.drawLine(x, y, x + W, y, border[1]);
			if (border[2] > 0)
				printer.drawLine(x + W-1, y, x + W-1, y + H + 1, border[2]);
			if (border[3] > 0)
				printer.drawLine(x, y + H, x + W, y + H + 1, border[3]);
			return h;
		};
	}

	public class Field extends ContentBlock {
		private int X,Y;
		public Field(ContentBlock owner) {
			super(owner);
		}
		public int print(IPrinter printer, int x, int y) {
			
			printer.clip(X, Y, X+W, Y+H);
			int h = super.print(printer, X, Y);
			printer.clearClip();
			if (border[0] > 0)
				printer.drawLine(X, Y, X, Y + H, border[0]);
			if (border[1] > 0)
				printer.drawLine(X, Y, X + W, Y, border[0]);
			if (border[2] > 0)
				printer.drawLine(X + W-1, Y, X + W-1, Y + H, border[0]);
			if (border[3] > 0)
				printer.drawLine(X, Y + H, X + W, Y + H, border[0]);
			return h;
		}
		@Override
		public void measure(IPrinter printer) throws Exception {
			if(W < 0) W = 0;
			if(H < 0) H = 0;
			for(IPrintable line : LINES) {
				line.measure(printer);
			}
		}
		@Override
		public void parse(String key, String value) {
			if("x".equals(key)) try {
				X = Integer.parseInt(value);
			} catch(NumberFormatException nfe) {
				
			}
			else if("y".equals(key)) try {
				Y = Integer.parseInt(value);
			} catch(NumberFormatException nfe) {
				
			}
			else 
				super.parse(key, value);
		}
	}
	public class Cell extends ContentBlock {
		public Cell(ContentBlock parent) {
			super(parent);
			padding = new int[] { 2, 2, 2, 2 };
		}

		@Override
		public void dump(String padding) {
			super.dump(padding+" ");
		}

		@Override
		public int print(IPrinter printer, int x, int y) {
			if (getContidion() != null && !getContidion().check(ARGS))
				return 0;
			if((getStyle().flags & INVERSE) == INVERSE)
				printer.fill(x,y,x+W,y+H);
			int h = super.print(printer, x, y);
			if (border[0] > 0)
				printer.drawLine(x, y, x, y + H, border[0]);
			if (border[1] > 0)
				printer.drawLine(x, y, x + W, y, border[0]);
			if (border[2] > 0)
				printer.drawLine(x + W-1, y, x + W-1, y + H, border[0]);
			if (border[3] > 0)
				printer.drawLine(x, y + H, x + W, y + H, border[0]);
			return h;
		}
		@Override
		public void measure(IPrinter printer) throws Exception {
			// TODO Auto-generated method stub
			super.measure(printer);
			H += LINES.size()*3;
		}

	}

	private ContentBlock ROOT;
	private ContentBlock CURRENT;

	private final IPrinter PRINTER;

	public Formatter(IPrinter printer) {
		PRINTER = printer;
	}

	public void init(int pageWidth, int pageHeight, String fontname, int fontSize) {
		WHEN = System.currentTimeMillis();
		_params = new PrintParams();
		DEFAULT_STYLE = new Style(fontname, fontSize);
		ROOT = new ContentBlock(null) {
			public int print(IPrinter printer, int x, int y) {
				printer.prePrint(_params);
				int h = super.print(printer, x, y);
				printer.postPrint (H, _postProcess);
				return h;
			};
		};
		ROOT.W = pageWidth;
		ROOT.H = pageHeight;
		ROOT.parseArgs("");
		CURRENT = ROOT;

	}

	private PrintParams _params = new PrintParams();
	private PostProcess _postProcess = new PostProcess();
	public int format(String line) throws Exception {
		return format(line, null);
	}

	
	
	public int format(String line, Map<String, String> args) throws Exception {
		if (args != null)
			ARGS = args;
		_params.W = 0;
		_params.H = -1;
		_params.DPI = 203f;
		_postProcess = new PostProcess();
		while (!line.isEmpty()) {
			char e = line.charAt(0);
			line = line.substring(1);
			switch (e) {
			case '\r':
				continue;
			case '{':
				if (line.startsWith("\\")) {
					line = line.substring(1);
					if(line.startsWith(CUT)) {
						line = line.substring(CUT.length());
						_postProcess.doCut = true;
					}
					else if(line.startsWith(FF)) {
						line = line.substring(FF.length());
						_postProcess.doFF= true;
					}
					
					else if (line.startsWith(PARAM_TAG)) {
						line = line.substring(PARAM_TAG.length());
						line = _params.parse(this,line);
					}
					else if (line.startsWith(STYLE_TAG)) {
						line = line.substring(STYLE_TAG.length());
						CURRENT.CURRENT_TEXT = new Text(CURRENT);
						line = CURRENT.parseArgs(line);
					} else if (line.startsWith(BARCODE_TAG)) {
						line = line.substring(BARCODE_TAG.length());
						CURRENT.CURRENT_TEXT = new Barcode(CURRENT);
						line = CURRENT.parseArgs(line);
					} else if (line.startsWith(IMAGE_TAG)) {
						line = line.substring(IMAGE_TAG.length());
						CURRENT.CURRENT_TEXT = new Image(CURRENT);
						line = CURRENT.parseArgs(line);
					} else if (line.startsWith(TABLE_TAG)) {
						CURRENT = new Table(CURRENT);
						line = line.substring(TABLE_TAG.length());
						line = CURRENT.parseArgs(line);
					}
					else if (line.startsWith(FIELD_TAG)) {
						CURRENT = new Field(CURRENT);
						line = line.substring(FIELD_TAG.length());
						line = CURRENT.parseArgs(line);
					} else if (line.startsWith(BLOCK_TAG)) {
						CURRENT = new ContentBlock(CURRENT);
						line = line.substring(BLOCK_TAG.length());
						line = CURRENT.parseArgs(line);
					}
					else if (line.startsWith(TR_TAG)) {
						line = line.substring(TR_TAG.length());
						CURRENT = new Row(CURRENT);
						line = CURRENT.parseArgs(line);
					} else if (line.startsWith(TD_TAG)) {
						line = line.substring(TD_TAG.length());
						CURRENT = new Cell(CURRENT);
						line = CURRENT.parseArgs(line);
					}
				} else
					CURRENT.addChar(e);
				break;
			case '}':
				CURRENT = CURRENT.close();
				break;
			case '\\':
				if (line.startsWith("}"))
					CURRENT.addChar('}');
				else
					CURRENT.addChar(e);
				break;
			default:
				CURRENT.addChar(e);
				break;
			}
		}
		ROOT.measure(PRINTER);
		return 0;
	}

	public ContentBlock page() {
		return ROOT;
	}

	public void release() {
		CURRENT = ROOT = null;
		System.gc();
	}
	
	public static String hDump(byte [] bb) {
		String s = "";
		if(bb == null) return s;
		for(byte b : bb) {
			s+=String.format("%02X ", b);
		}
		return s; 
	}
	public PrintParams getParams() { return _params; }

	public static Bitmap encodeAsBitmap(String content, int w, int h, BarcodeFormat fmt, int rotate) {
		Hashtable<EncodeHintType, Object> hints = null;
		hints = new Hashtable<EncodeHintType, Object>();
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		MultiFormatWriter writer = new MultiFormatWriter();
		int bw = w, bh = h;
		if(rotate == 90 || rotate == -90) {
			bw = h; bh = w;
		}
		BitMatrix result;
		try {
			result = writer.encode(content, fmt, bw, bh, hints);
		} catch (Exception iae) {
			iae.printStackTrace();
			return null;
		}
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		int po = 0;
		switch(rotate) {
		default:
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					pixels[po++] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
				}
			}
			break;
		case 90:
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					pixels[po++] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
				}
			}
			break;
		case -90:
			for (int x = width-1; x >=0 ; x--) {
				for (int y = 0; y < height; y++) {
					pixels[po++] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
				}
			}
			break;
			
		}
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
		return bitmap;

	}

	

}
