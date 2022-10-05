package rs.utils;

import java.math.BigDecimal;
import java.util.ArrayList;

import android.content.Context;
import android.os.RemoteException;
import android.widget.Toast;
import cs.U;
import rs.fncore.Const;
import rs.fncore.FiscalStorage;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.Shift;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.hw.payment.EPayment;
import rs.mkacca.hw.payment.EPayment.OperationType;
import rs.mkacca.ui.Main;

public class ShiftUtils extends AsyncFNTask {
	public static void closeShift(Context ctx) {
		new ShiftUtils(ctx, ACTION_CHECK_RESTS | ACTION_CLOSE_SHIFT | ACTION_SETTLEMENT).execute();
	}

	public static void openShift(Context ctx) {
		new ShiftUtils(ctx, ACTION_OPEN_SHIFT).execute();
	}
	public static void shiftReport(Context ctx) {
		new ShiftUtils(ctx, ACTION_SHIFT_REPORT).execute();
	}
	private static final int ACTION_CLOSE_SHIFT = 1;
	private static final int ACTION_OPEN_SHIFT = 2;
	private static final int ACTION_CHECK_RESTS = 4;
	public static final int ACTION_WITHDRAW = 8;
	
	private static final int ACTION_SHIFT_REPORT = 0x10;
	public static final int ACTION_DEPOSIT = 0x20;
	public static final int ACTION_SETTLEMENT = 0x40;

	private volatile int _lStatus;
	private int _action;
	private Object[] _args;
	private Context _ctx;

	public ShiftUtils(Context ctx, int action, Object... args) {
		_action = action;
		_args = args;
		_ctx = ctx;
		Main.lock();
	}

	@Override
	protected int execute(FiscalStorage fs) throws RemoteException {
		if ((_action & ACTION_CHECK_RESTS) != 0) {
			_args = new Object[] { fs.getCashRest() };
			return 0;
		}
		if((_action & ACTION_WITHDRAW) != 0) {
			int r = fs.putOrWithdrawCash(-((Double)_args[0]).doubleValue(),Core.getInstance().user().toOU(),Const.EMPTY_STRING);
			if(r == 0 ) Core.getInstance().updateRests();
			return r;
		}
		if((_action & ACTION_DEPOSIT) != 0) {
			int r =  fs.putOrWithdrawCash(((Double)_args[0]).doubleValue(),Core.getInstance().user().toOU(),Const.EMPTY_STRING);
			if(r == 0 ) Core.getInstance().updateRests();
			return r;
		}
		if((_action & ACTION_SETTLEMENT) != 0) {
			for(final String e : EPayment.knownEngines().keySet()) {
				EPayment p =  EPayment.knownEngines().get(e);
				if(p.isEnabled()) {
					_lStatus = 0;
					p.requestSettlement(_ctx, new EPayment.EPaymentListener() {
						
						@Override
						public void onOperationSuccess(EPayment engine, OperationType type, String rrn, BigDecimal sum) {
							_lStatus = 1;
							
						}
						@Override
						public void onOperationFail(EPayment engine, OperationType type, Exception e) {
							Toast.makeText(_ctx, "Сверка итогов для '"+e+"' выполнена с ошибкой", Toast.LENGTH_SHORT).show();
							_lStatus = 2;
						}
					});
					while(_lStatus == 0 ) try { Thread.sleep(100); } catch(InterruptedException ie) { return 0; }
				}
			}
			return 0;
		}
		if((_action & ACTION_CLOSE_SHIFT) != 0) {
			Shift shft = new Shift(); 
			int r = fs.closeShift(Core.getInstance().user().toOU(), shft, Const.EMPTY_STRING);
			if(r ==0 ) Core.getInstance().updateInfo();
			return r;
		}
		if((_action & ACTION_OPEN_SHIFT)!=0) {
			Shift shft = new Shift(); 
			int r = fs.openShift(Core.getInstance().user().toOU(), shft, Const.EMPTY_STRING);
			if(r ==0 ) Core.getInstance().updateInfo();
			return r;
		}
		if((_action & ACTION_SHIFT_REPORT) != 0) {
			FiscalReport rep = new FiscalReport();
			return fs.requestFiscalReport(Core.getInstance().user().toOU(), rep, Const.EMPTY_STRING);
		}
			
		
		return 0;
	}

	@Override
	protected void postExecute(int result, Object results) {
		if(result != 0) {
			return;
		}
		do {
			if ((_action & ACTION_CHECK_RESTS) != 0) {
				_action &= ~ACTION_CHECK_RESTS;
				Double rest = (Double) _args[0];
				if (rest.doubleValue() > 0.009) {
					U.confirm(_ctx,
							"Изять остаток денежных средств (" + String.format("%.2f", rest.doubleValue()) + ")?",
							new Runnable() {
								@Override
								public void run() {
									execute();
								}
							}, new Runnable() {

								@Override
								public void run() {
									_action &= ~ ACTION_WITHDRAW;
									execute();
								}
							});
					return;
				}
				break;
			}
			if((_action & ACTION_SETTLEMENT) != 0) {
				_action &= ~ACTION_SETTLEMENT;
				break;
			}
			if((_action & ACTION_WITHDRAW) != 0) {
				_action &= ~ ACTION_WITHDRAW;
				break;
			}
			if((_action & ACTION_CLOSE_SHIFT) != 0) {
				_action &= ~ ACTION_CLOSE_SHIFT;
				break;
			}
			if((_action & ACTION_OPEN_SHIFT)!=0) {
				_action &= ~ ACTION_OPEN_SHIFT;
				break;
			}
			if((_action & ACTION_SHIFT_REPORT) != 0) {
				_action &= ~ ACTION_SHIFT_REPORT;
				break;
			}
			if((_action & ACTION_DEPOSIT) != 0) {
				_action &= ~ ACTION_DEPOSIT;
				break;
			}
			
		} while (false);

		if (_action != 0)
			execute();
		else {
			Main.unlock();
			Toast.makeText(_ctx, "Операция успешно выполнена", Toast.LENGTH_SHORT).show();
		}
	}
}
