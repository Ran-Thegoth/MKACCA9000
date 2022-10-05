package rs.mkacca;


import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import rs.fncore.FiscalStorage;
import rs.utils.SimpleTaskExecutor;
import rs.log.Logger;

public abstract class AsyncFNTask implements Runnable, Handler.Callback {
	private Handler _h;
	private final static int MSG_RESULT = 10000;
	protected final static int ERR_EXCEPTION = 0x100;
	public AsyncFNTask() {
		_h = new Handler(Core.getInstance().getMainLooper(),this);
	}
	
	protected abstract int execute(FiscalStorage fs) throws RemoteException;
	protected void postExecute(int result,Object results) { 
		
	}
	@Override
	public void run() {
		Message msg = _h.obtainMessage(MSG_RESULT);
		try {
			msg.arg1 = execute(Core.getInstance().getStorage());
		} catch(Exception e) {
			Logger.e(e,"Ошибка работы с ФН");
			msg.arg1 = ERR_EXCEPTION;
			msg.obj = e;
		}
		_h.sendMessage(msg);
	}
	public void execute() {
		SimpleTaskExecutor.getInstance().execute(this);
	}
	@Override
	public boolean handleMessage(Message msg) {
		if(msg.what == MSG_RESULT)
			postExecute(msg.arg1, msg.obj);
		return true;
	}
	public Handler getHandler() { return _h; }
}
