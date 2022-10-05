package rs.fncore2;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.google.zxing.BarcodeFormat;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import rs.fncore.data.PrintSettings;
import rs.formatter.Formatter;
import rs.formatter.Formatter.IPrinter;
import rs.formatter.Formatter.PostProcess;
import rs.formatter.Formatter.PrintParams;
import rs.formatter.Formatter.Style;
import rs.utils.Utils;
import rs.log.Logger;


@SuppressLint("DefaultLocale")
public class ImagePrinter implements IPrinter {

    public final int PAPER_WIDTH=384;
    private Map<String, Typeface> FONTS = new HashMap<String, Typeface>();
    private Bitmap BITMAP;
    private Canvas CANVAS;
    private int mY = 0;
    private PrintSettings mSettings;
    private Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public ImagePrinter(PrintSettings s) {
        mSettings = s;
        FONTS.put(PrintSettings.DEFAULT_FONT.toLowerCase(), Typeface.MONOSPACE);
        FONTS.put("SansSerif".toLowerCase(), Typeface.SANS_SERIF);
        FONTS.put("Sans Serif".toLowerCase(), Typeface.SANS_SERIF);
        FONTS.put("Sans-Serif".toLowerCase(), Typeface.SANS_SERIF);
        FONTS.put("Sans".toLowerCase(), Typeface.SANS_SERIF);
        FONTS.put("Serif".toLowerCase(), Typeface.SERIF);
        BITMAP = Bitmap.createBitmap(PAPER_WIDTH, 600, Config.ARGB_8888);
        CANVAS = new Canvas(BITMAP);
        CANVAS.drawColor(Color.WHITE);
    }

    public byte[] print(String s) {
        try {
            Formatter f = new Formatter(this);
            f.init(PAPER_WIDTH, -1,
                    mSettings.getDefaultFontName(), mSettings.getDefaultFontSize());

            f.format(s);
            f.page().print(this, 0, 0);
        } catch (Exception e) {
            Logger.e(e, "print exc");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BITMAP.compress(CompressFormat.PNG, 100, bos);
        return bos.toByteArray();
    }

    private void growInNeed(int y) {
        if (BITMAP.getHeight() < y) {
            Bitmap b = Bitmap.createBitmap(PAPER_WIDTH, BITMAP.getHeight() + 600, Config.ARGB_8888);
            CANVAS = new Canvas(b);
            CANVAS.drawBitmap(BITMAP, 0, 0, PAINT);
            BITMAP.recycle();
            BITMAP = b;
        }
    }

    private void applyStyle(Style s) {
        Typeface tf = FONTS.get(s.fontName.toLowerCase());
        if (tf == null) tf = FONTS.get(mSettings.getDefaultFontName().toLowerCase());
        if ((s.flags & 0xF) != 0) tf = Typeface.create(tf, s.flags & 0xF);

        PAINT.setTypeface(tf);
        PAINT.setTextSize(s.fontSize);
    }

    @Override
    public int getHeight(Style style, String string) {
        applyStyle(style);
        return (int) (-PAINT.ascent() + PAINT.descent());
    }

    @Override
    public int getWidth(Style style, String string) {
        applyStyle(style);
        return (int) PAINT.measureText(string);
    }

    @Override
    public int printString(int x, int y, Style s, String string) {
        growInNeed(y + s.fontSize);
        applyStyle(s);
        CANVAS.drawText(string, x, y + s.fontSize, PAINT);
        mY = Math.max(mY, y + s.fontSize);
        return s.fontSize;
    }

    @Override
    public int printBarcode(int x, int y, int w, int h, BarcodeFormat fmt, String content, int roatation) {
        return printImage(x, y, w, h, Utils.encodeAsBitmap(content, w, h, fmt, roatation));
    }

    @Override
    public int drawLine(int x1, int y1, int x2, int y2, int w) {
        growInNeed(Math.max(y1, y2));
        mY = Math.max(mY, Math.max(y1, y2));
        PAINT.setStrokeWidth(w);
        CANVAS.drawLine(x1, y1, x2, y2, PAINT);
        return Math.max(y1, y2);
    }

    @Override
    public int printImage(int x, int y, int w, int h, Bitmap b) {
        growInNeed(y + h);
        CANVAS.drawBitmap(b, x, y, PAINT);
        mY = Math.max(mY, y + h);
        return h;
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
	public void postPrint(int y,PostProcess arg0) {
	}

	@Override
	public void prePrint(PrintParams arg0) {
	}
}
