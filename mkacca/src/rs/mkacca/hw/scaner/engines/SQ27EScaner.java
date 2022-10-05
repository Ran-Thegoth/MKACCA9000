package rs.mkacca.hw.scaner.engines;

import com.google.zxing.BarcodeFormat;
import com.imagealgorithm.scansdk.BarcodePicker;
import com.imagealgorithm.scansdk.DecodeCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import rs.data.BarcodeValue;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.BarcodeReceiver;
import rs.mkacca.hw.scaner.BarcodeScaner;
import rs.log.Logger;

public class SQ27EScaner extends BarcodeScaner implements View.OnClickListener, DecodeCallback, OnDismissListener {

	
	private AlertDialog _dlg;
	private BarcodePicker _bc;
	private FrameLayout _holder;
	private class MovableFloatingActionButton extends ImageView implements View.OnTouchListener {

	    private final static float CLICK_DRAG_TOLERANCE = 10; // Often, there will be a slight, unintentional, drag when the user taps the FAB, so we need to account for this.

	    private float downRawX, downRawY;
	    private float dX, dY;

	    public MovableFloatingActionButton(Context context) {
	        super(context);
	        init();
	    }

	    private void init() {
	    	setBackgroundResource(R.drawable.circle_red);
	    	setScaleType(ScaleType.CENTER_INSIDE);
	    	setImageResource(R.drawable.ic_menu_barcode_w);
	        setOnTouchListener(this);
	    }

	    @Override
	    public boolean onTouch(View view, MotionEvent motionEvent){

	        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)view.getLayoutParams();

	        int action = motionEvent.getAction();
	        if (action == MotionEvent.ACTION_DOWN) {

	            downRawX = motionEvent.getRawX();
	            downRawY = motionEvent.getRawY();
	            dX = view.getX() - downRawX;
	            dY = view.getY() - downRawY;

	            return true; // Consumed

	        }
	        else if (action == MotionEvent.ACTION_MOVE) {

	            int viewWidth = view.getWidth();
	            int viewHeight = view.getHeight();

	            View viewParent = (View)view.getParent();
	            int parentWidth = viewParent.getWidth();
	            int parentHeight = viewParent.getHeight();

	            float newX = motionEvent.getRawX() + dX;
	            newX = Math.max(layoutParams.leftMargin, newX); // Don't allow the FAB past the left hand side of the parent
	            newX = Math.min(parentWidth - viewWidth - layoutParams.rightMargin, newX); // Don't allow the FAB past the right hand side of the parent

	            float newY = motionEvent.getRawY() + dY;
	            newY = Math.max(layoutParams.topMargin, newY); // Don't allow the FAB past the top of the parent
	            newY = Math.min(parentHeight - viewHeight - layoutParams.bottomMargin, newY); // Don't allow the FAB past the bottom of the parent

	            view.animate()
	                    .x(newX)
	                    .y(newY)
	                    .setDuration(0)
	                    .start();

	            return true; // Consumed

	        }
	        else if (action == MotionEvent.ACTION_UP) {

	            float upRawX = motionEvent.getRawX();
	            float upRawY = motionEvent.getRawY();

	            float upDX = upRawX - downRawX;
	            float upDY = upRawY - downRawY;

	            if (Math.abs(upDX) < CLICK_DRAG_TOLERANCE && Math.abs(upDY) < CLICK_DRAG_TOLERANCE) { // A click
	                return performClick();
	            }
	            else { // A drag
	                return true; // Consumed
	            }

	        }
	        else {
	            return super.onTouchEvent(motionEvent);
	        }

	    }

	}
	
//	private MovableFloatingActionButton _button;
	public static final String ENGINE_NAME = "SQ27E Камера";
	public SQ27EScaner() {
	}

	@Override
	public void start(Context context, BarcodeReceiver rcvr) {
		start(context,rcvr,false);
	}

	private BarcodeReceiver _rcvr;
	private boolean _once;
	@Override
	public void stop() {
		if(_dlg != null)
			_dlg.dismiss();
/*		if(_button != null)
			((ViewGroup)_button.getParent()).removeView(_button);
		_button = null; */
	}

	private void start(Context context, BarcodeReceiver rcvr, boolean once) {
		_rcvr = rcvr;
		_once = once;
/*		if(_button == null && _rcvr != null) {
			_button = new MovableFloatingActionButton(context);
			_button.setOnClickListener(this);
			FrameLayout l = ((Activity)context).findViewById(android.R.id.content);
			int p = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64,context.getResources().getDisplayMetrics());
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(p,p);
			lp.gravity = Gravity.END | Gravity.BOTTOM;
			lp.bottomMargin = 32; lp.rightMargin = 16;
			l.addView(_button,lp);
		} */
		
	}
	@Override
	public void scanOnce(Context context, BarcodeReceiver rcvr) {
		start(context, rcvr,true);
	}

	@Override
	public void onClick(View v) {
		_bc = new BarcodePicker(v.getContext());
		_bc.setDecodeCallback(this);
		AlertDialog.Builder b = new AlertDialog.Builder(v.getContext());
		_holder = (FrameLayout)LayoutInflater.from(v.getContext()).inflate(R.layout.sq27e_scaner, new LinearLayout(v.getContext()),false);
		try {
			_bc.openCamera(0,BarcodePicker.PrewviewResolution.Resolution_640x480);
			_holder.addView(_bc.getCameraPreview());
			b.setView(_holder);
			_dlg = b.create();
			_dlg.setOnDismissListener(this);
			_dlg.show();
			v.setVisibility(View.GONE);
			_bc.startDecode();
		} catch(Exception e) {
			Logger.e(e,"Ошибка инициализации сканера");
		}
		
	}

	@Override
	public void onDecodeComplete(String code, int size, byte[] value) {
		_dlg.dismiss();
		final BarcodeValue bv = new BarcodeValue(new String(value), BarcodeFormat.AZTEC);
		Core.getInstance().invokeOnUI(new Runnable() {
			@Override
			public void run() {
				_rcvr.onBarcode(bv);
				if(_once)
					_rcvr = null;
			}
		});
	}

	@Override
	public void onError(int arg0) {
	}

	@Override
	public void onDismiss(DialogInterface arg0) {
		_holder.removeAllViews();
		_bc.stopDecode();
		_bc.releaseCamera();
//		_button.setVisibility(View.VISIBLE);
		
	}

}
