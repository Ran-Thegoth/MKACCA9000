package rs.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import com.google.zxing.BarcodeFormat;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import rs.fncore.data.PrintSettings;
import rs.formatter.Formatter;
import rs.formatter.Formatter.IPrinter;
import rs.formatter.Formatter.PostProcess;
import rs.formatter.Formatter.PrintParams;
import rs.formatter.Formatter.Style;

public class ImagePrinter  implements IPrinter {

	private static final int GROW_SIZE = 600;
	private Paint P = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
	private Canvas C;
	private Bitmap B;
	private int _MY =-1, pw = 384;
	private void growIfNeeded(int y) {
		_MY = Math.max(_MY, y);
		if (B.getHeight() < y) {
			Bitmap b1 = Bitmap.createBitmap(B.getWidth(), B.getHeight() + GROW_SIZE,Config.ARGB_8888);
			Canvas c = new Canvas(b1);
			c.drawColor(Color.WHITE);
			c.drawBitmap(B, 0, 0,P);
//			int oldH = B.getHeight();
			B.recycle();
			B = b1;
			C = new Canvas(B);
/*			Paint p = new Paint();
			p.setStyle(android.graphics.Paint.Style.FILL_AND_STROKE);
			p.setColor(Color.WHITE);
			C.drawRect(0, oldH, B.getWidth(), B.getHeight(), p); */
		}
	}



	private void applyStyle(Style style) {
		P.setTypeface(Typeface.create(Typeface.MONOSPACE, style.flags & 0x3));
		P.setTextSize(style.fontSize);
		if ((style.flags & Formatter.INVERSE) == Formatter.INVERSE)
			P.setColor(Color.WHITE);
		else
			P.setColor(Color.BLACK);

	}

	@Override
	public int getHeight(Style style, String string) {
		applyStyle(style);
		return (int) (-P.ascent() + P.descent());
	}

	@Override
	public int getWidth(Style style, String string) {
		applyStyle(style);
		return (int) P.measureText(string);
	}

	@Override
	public int printString(int x, int y, Style s, String string) {
		applyStyle(s);
		growIfNeeded((int) (y + P.getTextSize()));
		int w = (int)P.measureText(string);
		C.drawText(string, x, y + P.getTextSize(), P);
		if((s.flags & Formatter.UNDERLINE) == Formatter.UNDERLINE) {
			P.setStrokeWidth(1);
			C.drawLine(x,y+P.getTextSize()+1,
					x+w,y+P.getTextSize()+1,P);
					
		}
		if((s.flags & Formatter.STRIKEOUT) == Formatter.STRIKEOUT) {
			P.setStrokeWidth(2);
			C.drawLine(x,y+(int)(P.getTextSize()/1.3),
					x+w,y+(int)(P.getTextSize()/1.3),P);
			
		}
		return (int) (-P.ascent() + P.descent());
	}

	@Override
	public int printBarcode(int x, int y, int w, int h, BarcodeFormat fmt, String content, int rotation) {
		Bitmap b = Utils.encodeAsBitmap(content, w, h, fmt, rotation);
		if (b != null)
			try {
				return printImage(x, y, w, h, b);
			} finally {
				b.recycle();
			}
		return 0;
	}

	@Override
	public int drawLine(int x1, int y1, int x2, int y2, int w) {
		growIfNeeded(Math.max(y1, y2));
		P.setStrokeWidth(w);
		C.drawLine(Math.min(x1, x2), Math.min(y1, y2),
				Math.max(x1, x2), Math.max(y1, y2), P);
		return Math.abs(y2 - y1);
	}

	@Override
	public int printImage(int x, int y, int w, int h, Bitmap b) {
		growIfNeeded(y +  h);
		Matrix m = new Matrix();
		m.postScale(w / (float) b.getWidth(), h / (float) b.getHeight());
		m.postTranslate(x , y);
		C.drawBitmap(b, m, P);
		return h;
	}

	@Override
	public void fill(int x1, int y1, int x2, int y2) {
		growIfNeeded(Math.max(y1, y2));
		android.graphics.Paint.Style s = P.getStyle();
		P.setStyle(android.graphics.Paint.Style.FILL_AND_STROKE);
		C.drawRect(Math.min(x1, x2), Math.min(y1, y2),
				Math.max(x1, x2), Math.max(y1, y2), P);
		P.setStyle(s);
	}

	@Override
	public void clip(int x, int y, int x1, int y1) {
		C.save();
		C.clipRect(new Rect(x, y, x1, y1));
	}

	@Override
	public void clearClip() {
		if (C.getSaveCount() > 0)
			C.restore();
	}

	@Override
	public void prePrint(PrintParams params) {
	}

	@Override
	public void postPrint(PostProcess pp) {
	}

	public void reset(int W) {
		if(B != null) B.recycle();
		B = Bitmap.createBitmap(W, GROW_SIZE, Config.ARGB_8888);
		_MY = 0;
		C = new Canvas(B);
		C.drawColor(Color.WHITE);
	}
	public Bitmap toBitmap() {
		if(B.getHeight() > _MY) {
			if(_MY == 0) _MY = 1;
			Bitmap B1 = Bitmap.createBitmap(B.getWidth(), _MY,Config.ARGB_8888);
			Canvas c = new Canvas(B1);
			c.drawBitmap(B, 0, 0,new Paint(Paint.FILTER_BITMAP_FLAG));
			B.recycle();
			B = B1;
		}
		return B;
	}



	@Override
	public int barcodeHeight(BarcodeFormat arg0, int arg1, int h) {
		return h;
	}



	public ImagePrinter print(String printline, PrintSettings s) {
		reset(384-s.getMargins()[0] - s.getMargins()[1]);
		Formatter fmt = new Formatter(this);
		fmt.init(B.getWidth(), -1, s.getDefaultFontName(), s.getDefaultFontSize());
		try {
			fmt.format(printline);
			fmt.page().print(this, 0,0);
		} catch(Exception e) {
			
		}
		return this;
	}
	

}
