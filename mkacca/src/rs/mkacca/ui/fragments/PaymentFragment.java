package rs.mkacca.ui.fragments;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import cs.U;
import cs.cashier.ui.widgets.Banknotes;
import cs.cashier.ui.widgets.Banknotes.BanknoteListener;
import cs.orm.ORMHelper;
import cs.cashier.ui.widgets.NumberEdit;
import cs.ui.fragments.BaseFragment;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import rs.data.CheckStorage;
import rs.data.PayInfo;
import rs.data.ShiftSells;
import rs.data.goods.GoodGroup.GoodCategorySetter;
import rs.data.goods.ISellable;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FiscalStorage;
import rs.fncore.data.Correction;
import rs.fncore.data.Payment;
import rs.fncore.data.Payment.PaymentTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.fncore.data.SellOrder;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.R;
import rs.mkacca.hw.payment.EPayment;
import rs.mkacca.hw.payment.EPayment.EPaymentListener;
import rs.mkacca.hw.payment.EPayment.OperationType;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.OtherPaymentsDialog;

public class PaymentFragment extends BaseFragment implements BanknoteListener, View.OnClickListener, Runnable, EPaymentListener {

	private BigDecimal refund = BigDecimal.ZERO;
	private SellOrder _order;
	private View _content, _done;
	private NumberEdit _cash;
	private Banknotes _bn;
	private TextView _refund, _other, _card;
	private Switch _print;
	private OtherPaymentsDialog _others;
	private TIntObjectMap<BigDecimal> _payed = new TIntObjectHashMap<>();
	private Map<String, EPayment> _pEngines = new HashMap<>();
	private EPayment _engine;

	private PayInfo _pInfo;
	
	public static PaymentFragment newInstance(SellOrder order,PayInfo pay) {
		PaymentFragment result = new PaymentFragment();
		result._order = order;
		result._pInfo = pay;
		return result;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_content == null) {
			_others = new OtherPaymentsDialog(getContext());
			_content = inflater.inflate(R.layout.payment, container, false);
			_bn = _content.findViewById(R.id.v_baknotes);
			_content.findViewById(R.id.v_no_refund).setOnClickListener(this);
			_cash = _content.findViewById(R.id.ed_cash);
			_refund = _content.findViewById(R.id.lb_refund);
			_other = _content.findViewById(R.id.v_others);
			_other.setOnClickListener(this);
			_card = _content.findViewById(R.id.v_card);
			_card.setOnClickListener(this);
			_done = _content.findViewById(R.id.v_done);
			_done.setOnClickListener(this);
			_print = _content.findViewById(R.id.sw_print);
			_bn.setup(this, 1, 2, 5, 10, 50, 100, 200, 500, 1000, 2000, 5000, -1);
			((TextView) _content.findViewById(R.id.lbl_sum))
					.setText(String.format("%.2f", _order.getTotalSum().doubleValue()));
			for (PaymentTypeE p : PaymentTypeE.values())
				_payed.put(p.ordinal(), BigDecimal.ZERO);
			for (Map.Entry<String, EPayment> e : EPayment.knownEngines().entrySet()) {
				if (e.getValue().isEnabled()) {
					if(_order.getType() == OrderTypeE.RETURN_INCOME || _order.getType() == OrderTypeE.OUTCOME)
						if(!e.getValue().isRefunded()) continue;
					_pEngines.put(e.getKey(), e.getValue());
				}
			}
			update();
		}
		return _content;
	}

	public PaymentFragment() {
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Оплата");
	}

	@Override
	public void onValueChanged(Banknotes sender) {
		_payed.put(PaymentTypeE.CASH.ordinal(), sender.sum());
		update();
	}

	private void doEPayment(EPayment engine) {
		_engine = engine;
		if(_order.getType() == OrderTypeE.OUTCOME || _order.getType() == OrderTypeE.RETURN_INCOME) {
			if(_pInfo == null) return;
			if(_order.getShiftNumber() == Core.getInstance().kkmInfo().getShift().getNumber()) {
				engine.doCancel(getContext(), _pInfo, this);
			} else
				engine.doRefund(getContext(), _payed.get(PaymentTypeE.CARD.ordinal()), _pInfo, this);
		} else 
			engine.doPayment(getContext(), _payed.get(PaymentTypeE.CARD.ordinal()), this);
	}
	private void doEPaymentRollback() {
		if(_engine != null && _pInfo != null) {
			_engine.doCancel(getContext(), _pInfo, this);
		}
	}

	private class SellInfo {
		ISellable item;
		double qtty, price;
		SellInfo(SellItem item) {
			this.item = (ISellable)item.attachment();
			qtty = item.getQTTY().doubleValue();
			price = item.getPrice().doubleValue();
		}
		
	}

	private void doPayment() {
		Main.lock();
		_order.clearPayments();
		_order.setRefund(refund);
		_payed.forEachEntry(new TIntObjectProcedure<BigDecimal>() {

			@Override
			public boolean execute(int p, BigDecimal v) {
				if (v.compareTo(BigDecimal.ZERO) > 0) {
					_order.addPayment(new Payment(PaymentTypeE.values()[p], v));
				}
				return true;
			}
		});
		new AsyncFNTask() {
			private boolean _printCheck;
			{
				_printCheck = _print.isChecked();
			}
			private List<SellInfo> _selled = new ArrayList<>();
			 
			@Override
			protected int execute(FiscalStorage fs) throws RemoteException {
				if(_order.getType() == OrderTypeE.INCOME) {
					for(SellItem i : _order.getItems())
						_selled.add(new SellInfo(i));
				}
				if (_order instanceof Correction) {
					Correction result = new Correction();
					return fs.doCorrection((Correction) _order, Core.getInstance().user().toOU(), result,
							Const.EMPTY_STRING);
				}
				SellOrder result = new SellOrder();
				int r = fs.doSellOrder(_order, Core.getInstance().user().toOU(), result, _printCheck, Const.EMPTY_STRING,
						Const.EMPTY_STRING, Const.EMPTY_STRING, Const.EMPTY_STRING);
				if(Errors.isOK(r)) {
					_order = result;
					if(_pInfo != null) 
						fs.setDocumentPayload(_order.signature().getFdNumber(), _pInfo.toString().getBytes());
				}
					
				return r;
			}

			@Override
			protected void postExecute(int result, Object results) {
				Main.unlock();
				if (result == 0) {
					if(!_selled.isEmpty()) {
						for(SellInfo i : _selled) {
							ShiftSells.updateSell(_order.getShiftNumber(), i.item, i.price, i.qtty);
							
						}
					}
					Toast.makeText(getContext(),
							"Операция выполнена успешно, Документ № " + _order.signature().getFdNumber(),
							Toast.LENGTH_LONG).show();
					FragmentManager fm = getFragmentManager();
					CheckStorage cs = ORMHelper.load(CheckStorage.class, U.pair(CheckStorage.CHECK_TYPE_FLD, _order.getType().ordinal()));
					if(cs != null) cs.delete();
					fm.popBackStackImmediate();
					fm.popBackStackImmediate();
				} else {
					U.notify(getContext(), Errors.getErrorDescr(result));
					doEPaymentRollback();
				}
			}
		}.execute();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.v_no_refund:
			_bn.clear();
			_payed.put(PaymentTypeE.CASH.ordinal(),BigDecimal.ZERO);
			_payed.put(PaymentTypeE.CARD.ordinal(),BigDecimal.ZERO);
			BigDecimal r = BigDecimal.ZERO;
			for(BigDecimal val : _payed.valueCollection())
				r = r.add(val);
			_payed.put(PaymentTypeE.CASH.ordinal(),_order.getTotalSum().subtract(r));
			update();
			break;
		case R.id.v_done:
			doPayment();
			break;
		case R.id.v_card:
			if(_pEngines.size() == 1) {
				doEPayment(_pEngines.values().iterator().next());
			} else {
				AlertDialog.Builder b = new AlertDialog.Builder(getContext());
				b.setTitle("Способ безналичной оплаты");
				final ArrayAdapter<EPayment> a = new ArrayAdapter<EPayment>(getContext(), android.R.layout.simple_list_item_1) {
					@Override
					public View getView(int position, View convertView, ViewGroup parent) {
						convertView =  super.getView(position, convertView, parent);
						((TextView)convertView).setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, getItem(position).getIconId(), 0);
						return convertView;
					}
				};
				a.addAll(_pEngines.values());
				b.setAdapter(a, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int w) {
						doEPayment(a.getItem(w));
					}
				});
				b.show();
			}
			break;
		case R.id.v_others:
			_others.show(_payed, this);
			break;

		}

	}

	private void update() {
		BigDecimal cash = _payed.get(PaymentTypeE.CASH.ordinal());
		_cash.setText(String.format("%.2f", cash.doubleValue()));
		BigDecimal others = _payed.get(PaymentTypeE.AHEAD.ordinal())
				.add(_payed.get(PaymentTypeE.CREDIT.ordinal()).add(_payed.get(PaymentTypeE.PREPAYMENT.ordinal())));
		_other.setText(String.format("Другое\n%.2f", others.doubleValue()));
		try {
			if (others.compareTo(_order.getTotalSum()) > 0) {
				refund = BigDecimal.ZERO;
				_done.setEnabled(false);
				_card.setEnabled(false);
				return;
			}

			BigDecimal total = others.add(cash);
			if (total.compareTo(_order.getTotalSum()) >= 0  || _pEngines.isEmpty()) {
				refund = total.subtract(_order.getTotalSum());
				_card.setText(String.format("Электронными\n%.2f", 0.0));
				_card.setEnabled(false);
				_payed.put(PaymentTypeE.CARD.ordinal(), BigDecimal.ZERO);
			} else {
				refund = BigDecimal.ZERO;
				_done.setEnabled(false);
				BigDecimal card = _order.getTotalSum().subtract(total);
				_card.setText(String.format("Электронными\n%.2f", card.doubleValue()));
				_payed.put(PaymentTypeE.CARD.ordinal(), card);
				_card.setEnabled(true);
			}

			_done.setEnabled(total.compareTo(_order.getTotalSum()) >= 0);
		} finally {
			if(refund.compareTo(BigDecimal.ZERO) < 0) refund = BigDecimal.ZERO;
			_refund.setText(String.format("%.2f", refund));
		}

	}

	@Override
	public void run() {
		update();
	}

	@Override
	public void onOperationSuccess(EPayment engine, OperationType type, PayInfo pay, BigDecimal sum) {
		switch(type) {
		case PAYMENT:
		case REFUND:
		case CANCEL:
			_pInfo = pay;
			doPayment();
			break;
		}
	}

	@Override
	public void onOperationFail(EPayment engine, OperationType type, Exception e) {
		U.notify(getContext(), e.getLocalizedMessage());
	}

}
