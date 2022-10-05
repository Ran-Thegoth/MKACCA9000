package rs.fncore2.io;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.device.PrinterManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import rs.fncore.data.PrintSettings;
import rs.fncore2.core.Settings;
import rs.formatter.Formatter;
import rs.formatter.Formatter.IPrinter;
import rs.formatter.Formatter.PostProcess;
import rs.formatter.Formatter.PrintParams;
import rs.formatter.Formatter.Style;
import rs.utils.Utils;
import rs.log.Logger;

@SuppressLint("DefaultLocale")
public class Printing extends BaseThread {
    public final int PAPER_WIDTH=384;

    public static final String PREFS_PAPER = "paper";
    public static final String PREFS_KEY_COUNT_MM = "count_mm";

    private static Printing SHARED_INSTANCE;
    public static Printing getInstance() { return SHARED_INSTANCE; }
    private class PrinterTask implements IPrinter {
        private volatile int mAskState;

        @SuppressLint("NewApi")
        private class DialogNotifier implements Runnable,
        DialogInterface.OnClickListener {
            private String mMessage;

            @SuppressWarnings("deprecation")
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext,
                    android.R.style.Theme_Holo_Light_Dialog);
                builder.setTitle("Ошибка печати документа");
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(mMessage);
                builder.setNegativeButton("Отмена", this);
                builder.setPositiveButton("Повтор", this);
                AlertDialog dialog = builder.create();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    dialog.getWindow()
                        .setType(
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                else
                    dialog.getWindow().setType(
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }

            public DialogNotifier(String message) {
                mMessage = message;
            }

            @Override
            public void onClick(DialogInterface arg0, int button) {
                mAskState = button;
            }
        }

        private Paint mPaint = new Paint();
        private String mText;

        public PrinterTask(String text) {
            mText = text;
        }

        public void start() {
            while (!isStopped) {
                if (mPrinterStatus == 0) {
                    mPrinter.open();
                    try {
                        mPrinter.setupPage(PAPER_WIDTH, -1);

                        try {
                        	Log.d("fncore2", "Font size is "+mSettings.getDefaultFontSize());
                            Formatter fmt = new Formatter(this);
                            fmt.init(PAPER_WIDTH - mSettings.getMargins()[0] - mSettings.getMargins()[1],
                                    -1, mSettings.getDefaultFontName(),
                                    mSettings.getDefaultFontSize());
                            fmt.format(mText);
                            fmt.page().print(this, 0, 0);
                            int paperConsumeMm = fmt.page().getHeight() * 254 / 2000; // 200dpi
                            mPrinter.printPage(0);

                            Logger.i("Paper consume: " + paperConsumeMm + "mm");
                            addPaperCount(paperConsumeMm);

                        } catch (Exception e) {
                            Logger.e(e, "Ошибка печати");
                        }
                    } finally {
                        mPrinter.close();
                    }
                    return;

                } else {
                    DialogNotifier dn;

                    switch (mPrinterStatus) {
                    case PrinterManager.PRNSTS_OUT_OF_PAPER:
                        dn = new DialogNotifier("Вставьте бумагу и проверьте крышку принтера.");
                        break;
                    case PrinterManager.PRNSTS_OVER_HEAT:
                        dn = new DialogNotifier("Принтер перегрелся. Повторите попытку через некоторое время.");
                        break;
                    default:
                        dn = new DialogNotifier("Зарядите устройство и повторите печать.");
                        break;
                    }

                    mAskState = 0;
                    mHnd.post(dn);

                    while (!isStopped && mAskState == 0) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            return;
                        }
                    }

                    if (mAskState == DialogInterface.BUTTON_NEGATIVE) return;
                    mPrinterStatus=mPrinter.getStatus();
                }
            }
        }

        private Typeface getTypeface(String s) {
            Object o = FONTS.get(s.toLowerCase());
            if (o instanceof Typeface) return (Typeface) o;
            if (o == null) {
                Typeface oldtypeface=getTypeface(mSettings.getDefaultFontName().toLowerCase());
                if (oldtypeface==null){
                    Logger.e("no typeface found, requested: %s, settings: %s",
                            s,mSettings.getDefaultFontName().toLowerCase());
                }
            }

            s = o.toString();
            o = FONTS.get(s);
            if (o == null) {
                Typeface tf = Typeface.createFromFile(s);
                FONTS.put(s, tf);
                return tf;
            }

            return (Typeface) o;
        }

        private String getFontName(String s) {
            Object o = FONTS.get(s.toLowerCase());
            if (o instanceof Typeface)
                return s;

            return o != null ? o.toString() : getFontName(mSettings
                .getDefaultFontName().toLowerCase());
        }

        private void applyStyle(Style s) {
            Typeface tf = getTypeface(s.fontName);
            if ((s.flags & 0xF) != 0)
                tf = Typeface.create(tf, (int)(s.flags & 0xF));
            mPaint.setTypeface(tf);
            mPaint.setTextSize(s.fontSize);
        }

        @Override
        public int getHeight(Style style, String string) {
            applyStyle(style);
            return (int) (-mPaint.ascent() + mPaint.descent());
        }

        @Override
        public int getWidth(Style style, String string) {
            applyStyle(style);
            return (int) mPaint.measureText(string);
        }

        @Override
        public int printString(int x, int y, Style s, String string) {
            String fn = getFontName(s.fontName);
//            Log.d("fncore2", ""+y+" '"+string+"'");
            mPrinter.drawText(string, mSettings.getMargins()[0] + x, y,
                fn, s.fontSize, (s.flags & Typeface.BOLD) == Typeface.BOLD,
                (s.flags & Typeface.ITALIC) == Typeface.ITALIC, 0);
            return getHeight(s, string);
        }

        @Override
        public int printBarcode(int x, int y, int w, int h, BarcodeFormat fmt,
            String content, int rotation) {
            Bitmap b = Utils.encodeAsBitmap(content, w, h, fmt, rotation);
            if (b != null)
                try {
                    return mPrinter.drawBitmap(b,
                        mSettings.getMargins()[0] + x, y);
                } finally {
                    b.recycle();
                }
            return 0;
        }

        @Override
        public int drawLine(int x1, int y1, int x2, int y2, int w) {
            mPrinter.drawLine(mSettings.getMargins()[0] + x1, y1,
                mSettings.getMargins()[0] + x2, y2, w);
            return Math.max(Math.abs(y1 - y2), w);
        }

        @Override
        public int printImage(int x, int y, int w, int h, Bitmap b) {
            return mPrinter.drawBitmap(b, mSettings.getMargins()[0] + x, y);
        }


        @Override
        public void clip(int X, int Y, int X1, int Y1) {
        }

        @Override
        public void clearClip() {
        }

		@Override
		public int barcodeHeight(BarcodeFormat bf, int w, int h) {
			return h;
		}

		@Override
		public void fill(int arg0, int arg1, int arg2, int arg3) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void postPrint(int y, PostProcess pp) {
			if(pp.doFF) {
				for(int i=0;i<5;i++)
					y+=mPrinter.drawText(" ", y, 0, "monospace",20,false,false,0);
			}
				
		}

		@Override
		public void prePrint(PrintParams arg0) {
			// TODO Auto-generated method stub
			
		}
    }

    private PrinterManager mPrinter;
    private LinkedBlockingQueue<PrinterTask> TASKS = new LinkedBlockingQueue<>();
    private Map<String, Object> FONTS = new HashMap<String, Object>();
    private Context mContext;
    private PrintSettings mSettings;
    private Handler mHnd;
    private SharedPreferences mPreferences;

    private volatile int mPrinterStatus = PrinterManager.PRNSTS_ERR;
    private volatile long mPaperCounter = 0;

    @SuppressLint("DefaultLocale")
    public Printing(Context ctx) {
    	SHARED_INSTANCE = this;
        mContext = ctx;
        mHnd = new Handler(mContext.getMainLooper());
        mPrinter = new PrinterManager();

        mPreferences = ctx.getSharedPreferences(PREFS_PAPER, Context.MODE_PRIVATE);
        mPaperCounter = mPreferences.getLong(PREFS_KEY_COUNT_MM, 0);
        FONTS.put(PrintSettings.DEFAULT_FONT.toLowerCase(), Typeface.MONOSPACE);
        FONTS.put("SansSerif".toLowerCase(), Typeface.SANS_SERIF);
        FONTS.put("Sans Serif".toLowerCase(), Typeface.SANS_SERIF);
        FONTS.put("Sans-Serif".toLowerCase(), Typeface.SANS_SERIF);
        FONTS.put("Sans".toLowerCase(), Typeface.SANS_SERIF);
        FONTS.put("Serif".toLowerCase(), Typeface.SERIF);

        start();
    }


    protected void unblockWait(){}

    @Override
    public void run() {
        while (!isStopped) {
            try {
                PrinterTask newTask = TASKS.poll(1, TimeUnit.SECONDS);

                if (newTask!=null) {
                    mSettings = Settings.getInstance().getPrintSettings();
                    int newPrinterStatus = mPrinter.getStatus();
                    if ((mPrinterStatus != newPrinterStatus) && (newPrinterStatus != 0)) {
                        Logger.w("Printer error: " + newPrinterStatus);
                    }
                    mPrinterStatus = newPrinterStatus;
                    newTask.start();
                }
            } catch (InterruptedException e) {
                break;
            }catch (Exception e) {
                Logger.e(e,"Printing execute error");
            }
        }
    }

    public void queue(String content) {
        if (content == null || content.trim().isEmpty()) return;
        TASKS.add(new PrinterTask(content));
    }

    public int getStatus() {
        return mPrinterStatus;
    }

    public long getPaperCount() {
        return  mPaperCounter;
    }


    private void addPaperCount(long paperCountMm) {
        mPaperCounter += paperCountMm;
		mPreferences.edit().putLong(PREFS_KEY_COUNT_MM, mPaperCounter).commit();
    }


	public void resetPrinterCounter() {
		mPreferences.edit().putLong(PREFS_KEY_COUNT_MM, 0).commit();
		mPaperCounter = 0;
		
	}
}
