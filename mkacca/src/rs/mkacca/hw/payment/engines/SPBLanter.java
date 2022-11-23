package rs.mkacca.hw.payment.engines;

import java.math.BigDecimal;

import org.lanter.lan4gate.IRequest;
import org.lanter.lan4gate.Lan4Gate;
import org.lanter.lan4gate.Messages.OperationsList;

import android.content.Context;

public class SPBLanter extends Lanter {
	public static final  String ENGINE_NAME = "Лантер: СБП";
	
	public SPBLanter() {
		super();
	}
	@Override
	public void doPayment(Context ctx, BigDecimal sum, EPaymentListener listener) {
		_op = OperationType.PAYMENT;
		_listener = listener;
		_sum = sum;
		IRequest rq = Lan4Gate.getPreparedRequest(OperationsList.QuickPayment);
		rq.setCurrencyCode(643);
		rq.setEcrMerchantNumber(1);
		rq.setAmount(sum.multiply(BigDecimal.valueOf(100)).longValue());
		startOperation(ctx, "Оплата СБП", rq,Lan4Gate.getPreparedRequest(OperationsList.FinalizeTransaction));
	}
	@Override
	public String toString() {
		return ENGINE_NAME;
	}

	
}
