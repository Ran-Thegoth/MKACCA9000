package rs.mkacca.ui.fragments;

import java.math.BigDecimal;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import cs.U;
import cs.orm.ORMHelper;
import cs.ui.BackHandler;
import cs.ui.fragments.BaseFragment;
import cs.ui.widgets.DialogSpinner;
import rs.data.goods.ISellable;
import rs.data.goods.Variant;
import rs.data.BarcodeValue;
import rs.data.CheckStorage;
import rs.data.PayInfo;
import rs.data.WeigthService;
import rs.data.goods.Good;
import rs.data.goods.Good.MarkTypeE;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.FZ54Tag;
import rs.fncore.FiscalStorage;
import rs.fncore.data.AgentTypeE;
import rs.fncore.data.SellItem;
import rs.fncore.data.SellOrder;
import rs.fncore.data.TaxModeE;
import rs.fncore.data.SellOrder.OrderTypeE;
import rs.mkacca.AsyncFNTask;
import rs.mkacca.Core;
import rs.mkacca.DB;
import rs.mkacca.R;
import rs.mkacca.hw.BarcodeReceiver;
import rs.mkacca.ui.fragments.editors.SellItemEditor;
import rs.utils.SellOrderUtils.OnSellItemClickListener;
import rs.utils.SellOrderUtils.SellItemsAdapter;
import rs.mkacca.ui.AgentDialog;
import rs.mkacca.ui.CustomerInfoDlg;
import rs.mkacca.ui.IndustryInfoDlg;
import rs.mkacca.ui.Main;
import rs.mkacca.ui.OperationInfoDlg;
import rs.mkacca.ui.RequestStringDialog;
import rs.mkacca.ui.fragments.editors.BaseEditor.OnValueChangedListener;

public class SellOrderFragment extends BaseFragment
		implements View.OnClickListener, BackHandler, OnValueChangedListener<SellItem>, BarcodeReceiver, OnSellItemClickListener {

	private DialogSpinner type, mode;
	private RecyclerView items;
	private View _content;
	private TextView sum, done;
//	private List<SellItem> _items;
	private Handler _h;
	protected PayInfo _pInfo;
	public static SellOrderFragment newInstance(SellOrder order) {
		SellOrderFragment result = new SellOrderFragment();
		result._order = order;
		return result;
	}

/*	private class SellItemCard extends ViewHolder implements View.OnClickListener {
		private SellItem _item;
		private TextView _name, _type, _sellType, _price;

		public SellItemCard(View itemView) {
			super(itemView);
			_name = itemView.findViewById(R.id.lbl_name);
			_type = itemView.findViewById(R.id.lbl_item_type);
			_sellType = itemView.findViewById(R.id.lbl_item_sell_type);
			_price = itemView.findViewById(R.id.lbl_price);
			itemView.setOnClickListener(this);
		}

		public void update(SellItem item) {
			_item = item;
			_name.setText(_item.getName());
			_type.setText(_item.getType().toString());
			_sellType.setText(_item.getPaymentType().pName);
			_price.setText(String.format("%.3f %s x %.2f = %.2f", _item.getQTTY().doubleValue(),
					item.getMeasure().pName, item.getPrice().doubleValue(), item.getSum().doubleValue()));
		}

		@Override
		public void onClick(View arg0) {
			showFragment(SellItemEditor.newInstance(_item, SellOrderFragment.this));

		}

	}

	private class SellItemsAdapter extends Adapter<SellItemCard> {

		@Override
		public int getItemCount() {
			return _items.size();
		}

		@Override
		public void onBindViewHolder(SellItemCard h, int p) {
			h.update(_items.get(p));
		}

		@Override
		public SellItemCard onCreateViewHolder(ViewGroup vg, int arg1) {
			// TODO Auto-generated method stub
			return new SellItemCard(getActivity().getLayoutInflater().inflate(R.layout.sell_item_card, vg, false));
		}

	} */

	private SellItemsAdapter _adapter;
	protected SellOrder _order;

	public SellOrderFragment() {
	}

	protected int getLayoutId() {
		return R.layout.sell_order;
	}

	protected void bind(View v) {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (_content == null) {
			_h = new Handler(getContext().getMainLooper());
			_content = inflater.inflate(getLayoutId(), container, false);
			type = _content.findViewById(R.id.sp_order_type);
			mode = _content.findViewById(R.id.sp_sno_type);
			type.setAdapter(new ArrayAdapter<OrderTypeE>(getContext(), android.R.layout.simple_list_item_1,
					OrderTypeE.values()));
			type.setSelectedItem(_order.getType());
			mode.setAdapter(
					new ArrayAdapter<TaxModeE>(getContext(), android.R.layout.simple_list_item_1, TaxModeE.values()));
			mode.setSelectedItem(_order.getTaxMode());
			sum = _content.findViewById(R.id.lb_sum);
			done = _content.findViewById(R.id.do_pay);
			done.setOnClickListener(this);
			items = _content.findViewById(R.id.v_items);
			items.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
			_adapter = new SellItemsAdapter(getContext(),_order.getItems(),this);
			items.setAdapter(_adapter);
			bind(_content);
			updateSum();
		}
		return _content;
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().setTitle("Кассовая операция");
		setupButtons(this, R.id.iv_add);
		setCustomButtom(R.drawable.ic_menu_more, this);
		Core.getInstance().scaner().start(getContext(), this);
	}

	@Override
	public void onStop() {
		Core.getInstance().scaner().stop();
		super.onStop();
	}

	protected boolean commit() {
		_order.setTaxMode((TaxModeE)mode.getSelectedItem());
		_order.setType((OrderTypeE)type.getSelectedItem());
		String sINN = Const.EMPTY_STRING;
		for(SellItem i : _order.getItems()) {
			if(!i.getAgentData().getProviderINN().isEmpty()) {
				if(sINN.isEmpty()) 
					sINN = i.getAgentData().getProviderINN();
				else
					if(!sINN.equals(i.getAgentData().getProviderINN())) {
						U.notify(getContext(), "ИНН поставщика не совпадает в предметах расчета");
						return false;
					}
			}
		}
		return true;
	}

	protected void onAddItem() {
		showFragment(GoodSelectFragment.newInstance(this));
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_add:
			onAddItem();
			break;
		case R.id.iv_custom: {
			AlertDialog.Builder b = new AlertDialog.Builder(getContext());
			ArrayAdapter<String> a = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,
					getResources().getStringArray(R.array.sell_item_more));
			b.setAdapter(
					a,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int p) {
							switch (p) {
							case 0:
								new AgentDialog(getContext()).show(_order.getAgentData());
								break;
							case 1:
								new CustomerInfoDlg(getContext()).show(_order);
								break;
							case 2:
								new RequestStringDialog("Дополнительно", getContext(), _order.getTagString(FZ54Tag.T1192_EXTRA_BILL_FIELD), new RequestStringDialog.OnValueConfirmListener() {
									@Override
									public void onConfirm(String r) {
										if(r.isEmpty())
											_order.remove(FZ54Tag.T1192_EXTRA_BILL_FIELD);
										else
											_order.add(FZ54Tag.T1192_EXTRA_BILL_FIELD,r);
									}
								});
								break;
							case 3:
								new OperationInfoDlg(getContext()).show(_order);
								break;
							case 4:
								new IndustryInfoDlg(getContext()).show(_order, FZ54Tag.T1261_INDUSTRY_CHECK_REQUISIT);
								break;
							}
						}
					});
			b.show();
		}
			break;
		case R.id.do_pay:
			if (commit())
				showFragment(PaymentFragment.newInstance(_order,_pInfo));
			break;
		}
	}

	@Override
	public boolean onBackPressed() {
		U.confirm(getContext(), "Сохранить текущий чек?", new Runnable() {
			@Override
			public void run() {
				CheckStorage cs = ORMHelper.load(CheckStorage.class, U.pair(CheckStorage.CHECK_TYPE_FLD, _order.getType().ordinal()));
				if(cs == null ) cs = new CheckStorage();
				cs.set(_order);
				cs.store();
				getFragmentManager().popBackStack();
			}
		}, new Runnable() {

			@Override
			public void run() {
				getFragmentManager().popBackStack();
			}
		});
		return false;
	}

	private void updateSum() {
		sum.setText(String.format("%.2f", _order.getTotalSum().doubleValue()));
		done.setEnabled(_order.getTotalSum().compareTo(BigDecimal.ZERO) > 0);
	}

	@Override
	public void onChanged(final SellItem value) {
		ISellable selable = (ISellable) value.attachment();
		if (selable.good().getMarkType() != MarkTypeE.NONE) {
			if (!Core.getInstance().kkmInfo().isMarkingGoods()) {
				Toast.makeText(getContext(), "Вы не можете работать с маркированным товаром", Toast.LENGTH_LONG).show();
				return;
			}
		}
		if (!_order.getItems().contains(value)) {
			boolean needAdd = true;
			if (value.getPrice().compareTo(BigDecimal.ZERO) == 0
					|| (selable.good().getMarkType() != MarkTypeE.NONE && value.getMarkingCode().isEmpty())) {

				_h.postDelayed(new Runnable() {
					@Override
					public void run() {
						showFragment(SellItemEditor.newInstance(value, SellOrderFragment.this));
					}
				}, 400);
				return;
			}
			for (SellItem i : _order.getItems()) {
				if (i.attachment().equals(value.attachment()) && selable.good().getMarkType() == MarkTypeE.NONE) {
					needAdd = false;
					i.setQtty(i.getQTTY().add(value.getQTTY()));
					break;
				}
			}
			if (needAdd) {
				if (selable.good().getMarkType() != MarkTypeE.NONE) {
					new AsyncFNTask() {
						{
							Main.lock();
						}
						@Override
						protected int execute(FiscalStorage fs) throws RemoteException {
							if (Core.getInstance().kkmInfo().isOfflineMode()
									|| Core.getInstance().kkmInfo().isDemoMode())
								return Core.getInstance().getStorage().checkMarkingItemLocalFN(value);
							else
								return Core.getInstance().getStorage().checkMarkingItem(value);
						}
						@Override
						protected void postExecute(int result, Object results) {
							Main.unlock();
							if (result != Errors.NO_ERROR) {
								U.confirm(getContext(), "Товар не прошел проверку маркировки. Включить его в чек?",
										new Runnable() {
											@Override
											public void run() {
												try {
													Core.getInstance().getStorage().confirmMarkingItem(value, true);
													_order.addItem(value);
													items.getAdapter().notifyDataSetChanged();
													updateSum();
												} catch (RemoteException re) {

												}
											};
										});
							} else try {
								Core.getInstance().getStorage().confirmMarkingItem(value, true);
								_order.addItem(value);
								items.getAdapter().notifyDataSetChanged();
								updateSum();
							} catch(RemoteException re) { }

						}
					}.execute();
					return;
				}
				_order.addItem(value);
			}
		}
		items.getAdapter().notifyDataSetChanged();
		updateSum();
	}

	@Override
	public void onDeleted(SellItem value) {
		_order.removeItem(value);
		items.getAdapter().notifyDataSetChanged();
		updateSum();
	}

	@Override
	public void onBarcode(BarcodeValue code) {
		String SQL = "SELECT A._id,0 FROM  BARCODES C INNER JOIN GOODS A ON A._id = C.OWNER_ID WHERE C.CODE = '"
				+ code.GOOD_CODE + "' AND C.OWNER_TYPE = 0 " + "UNION ALL "
				+ "SELECT A._id, B._id FROM  BARCODES C INNER JOIN VARIANTS B on B._id = C.OWNER_ID "
				+ "INNER JOIN GOODS A on A._id = B.GOOD_ID " + "WHERE C.CODE = '" + code.GOOD_CODE
				+ "' AND C.OWNER_TYPE = 1";
		Cursor c = Core.getInstance().db().getReadableDatabase().rawQuery(SQL, null);
		ISellable result = null;
		if (c.moveToFirst()) {
			Good g = ORMHelper.load(Good.class, U.pair(DB.ID_FLD, c.getLong(0)));
			result = g;
			if (c.getLong(1) > 0)
				for (Variant v : g.variants())
					if (v.id() == c.getLong(1)) {
						result = v;
						break;
					}
		}
		c.close();
		if (result != null) {
			SellItem item = result.createSellItem();
			item.setQtty(WeigthService.getQtty(code.CODE, result.measure()));
			if (result.good().getMarkType() != MarkTypeE.NONE) {
				item.setMarkingCode(code.MARK_CODE, _order.getType());
			}
			onChanged(item);
		}

	}

	@Override
	public void onClick(SellItem item) {
		showFragment(SellItemEditor.newInstance(item, SellOrderFragment.this));
		
	}

}
