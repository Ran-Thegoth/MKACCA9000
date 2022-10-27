package rs.fncore2.example;

import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import rs.fncore.Const;
import rs.fncore.Errors;
import rs.fncore.data.AgentTypeE;
import rs.fncore.data.ArchiveReport;
import rs.fncore.data.Correction;
import rs.fncore.data.FNCounters;
import rs.fncore.data.FiscalReport;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.KKMInfo.FiscalReasonE;
import rs.fncore.data.OU;
import rs.fncore.data.SellOrder;
import rs.fncore.data.Shift;
import rs.fncore.data.TaxModeE;
import rs.fncore.fncoresample.R;
import rs.utils.Utils;
import rs.utils.app.MessageQueue.MessageHandler;

public class Main extends Activity implements MessageHandler, View.OnClickListener {

	private ArrayAdapter<String> LOG_RECORDS;
	private ListView LOG_VIEW;
	private static final int MSG_ERROR = 999;
	private static final int MSG_INITIALIZE_CORE = 1000;
	private static final int MSG_KKM_INFO = 1001;
	private static final int MSG_CHECK_SHIFT_STATE = 1002;
	private static final int MSG_SELLORDER = 2001;
	private static final int MSG_CORRECTION = 2002;

	public Main() {

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		findViewById(R.id.v_kkminfo).setOnClickListener(this);
		findViewById(R.id.v_fiscal).setOnClickListener(this);
		findViewById(R.id.v_shift_op).setOnClickListener(this);
		findViewById(R.id.v_report).setOnClickListener(this);
		findViewById(R.id.v_check).setOnClickListener(this);
		findViewById(R.id.v_corr).setOnClickListener(this);
		findViewById(R.id.v_reset).setOnClickListener(this);
		findViewById(R.id.v_postfiscal).setOnClickListener(this);
		findViewById(R.id.v_off).setOnClickListener(this);
		findViewById(R.id.v_on).setOnClickListener(this);

		LOG_VIEW = findViewById(R.id.log_view);
		LOG_RECORDS = new ArrayAdapter<>(this, R.layout.log_row);
		LOG_VIEW.setAdapter(LOG_RECORDS);
		log("Подключение к фискальному ядру");
		Core.getInstance().registerHandler(this); // Что бы не плодить Handle для обработки событий в UI будем
													// использовать MessageQueue
		new Thread() {
			public void run() {
				Core.getInstance().sendMessage(MSG_INITIALIZE_CORE, Core.getInstance().initialize());
			};
		}.start();
	}

	private void checkShiftIsOpen(final Runnable task) {
		new Thread() {
			@Override
			public void run() {
				try {
					log("Проверка открыта ли смена");
					KKMInfo info = new KKMInfo();
					int r = Core.getInstance().storage().readKKMInfo(info);
					if (Errors.isOK(r)) {
						if (info.getShift().isOpen()) {
							log("Смена открыта");
							Core.getInstance().sendMessage(MSG_CHECK_SHIFT_STATE, task);
							return;
						}
						log("Смена закрыта");
					} else
						Core.getInstance().sendMessage(MSG_ERROR, Errors.getErrorDescr(r));
				} catch (Exception e) {
					Core.getInstance().sendMessage(MSG_ERROR, e.getLocalizedMessage());
				}
			}
		}.start();
	}

	private void log(final String line) {
		Core.getInstance().runOnUI(new Runnable() {
			@Override
			public void run() {
				LOG_RECORDS.add(line);
				LOG_RECORDS.notifyDataSetChanged();
				LOG_VIEW.smoothScrollToPosition(LOG_RECORDS.getCount() - 1);
			}
		});

	}

	@Override
	protected void onDestroy() {
		Core.getInstance().removeHandler(this);
		super.onDestroy();
	}

	private void toggleShift() {
		new Thread() {
			@Override
			public void run() {
				try {
					log("Открытие/закрытие смены...");
					Shift shift = new Shift();
					int r = Core.getInstance().storage().toggleShift(new OU("Администратор"), shift,
							Const.EMPTY_STRING);
					if (Errors.isOK(r)) {
						log("Выполнено, смена открыта: " + shift.isOpen() + ", номер смены " + shift.getNumber()
								+ ", номер ФД " + shift.signature().getFdNumber());
						FNCounters c = shift.getShiftCounters();
						if(c != null) {
							log("Операций за смену "+c.getTotalOperationCounter());
							log("Приход за смену "+String.format("%.2f", c.Income().totalSum / 100.0));
							
						}
						
					} else
						Core.getInstance().sendMessage(MSG_ERROR, Errors.getErrorDescr(r));
				} catch (Exception e) {
					Core.getInstance().sendMessage(MSG_ERROR, e.getLocalizedMessage());
				}

			}
		}.start();
	}

	/**
	 * Фискализация/изменение параметров ККТ
	 */
	private void doFiscalization() {
		log("Подготовка данных для фискализации");
		final KKMInfo fInfo = new KKMInfo();
		fInfo.getOwner().setINN("7716776723");
		fInfo.getOwner().setName("ООО Райтскан");
		fInfo.getLocation().setAddress("Москва, ул. малая Семеновская, 11/2 стр. 4");
		fInfo.getLocation().setPlace("Офис продаж");
		fInfo.getTaxModes().add(TaxModeE.COMMON); // Добавление Общей системы налогообложения
		fInfo.setMarkingGoods(true); // Работа с маркированными товарами
		fInfo.setOfflineMode(true); // Режим ККТ оффлайн
		fInfo.ofd().setINN(OU.EMPTY_INN);
		log("Начало фискализации");
		new Thread() {
			public void run() {
				try {

					KKMInfo info = new KKMInfo();
					Core.getInstance().storage().readKKMInfo(info);
					FiscalReasonE reason = FiscalReasonE.REGISTER; // Регистрация
					if (info.isFNActive()) {
						reason = FiscalReasonE.CHANGE_KKT_SETTINGS; // Изменение настроек ККТ
						fInfo.setKKMNumber(info.getKKMNumber()); // Устанавливаем известный номер ККМ
					} else { // Генерация номера
						Random r = new Random(System.currentTimeMillis());
						String num = String.valueOf(r.nextInt(10000));
						while (num.length() < 10)
							num = "0" + num;
						String inn = info.getOwner().getINN();
						String device = info.getKKMSerial();
						while (inn.length() < 12)
							inn = "0" + inn;
						while (device.length() < 20)
							device = "0" + device;

						byte[] b = (num + inn + device).getBytes();
						int sCRC = Utils.CRC16(b, 0, b.length, (short) 0x1021) & 0xFFFF;
						fInfo.setKKMNumber(num + String.format("%06d", sCRC));
					}
					log("Определение типа операции: " + reason.toString());
					int r = Core.getInstance().storage().doFiscalization(reason, new OU("Администратор"), fInfo, fInfo,
							Const.EMPTY_STRING);
					if (Errors.isOK(r))
						log("Выполнено, номер ФД " + fInfo.signature().getFdNumber());
					else
						Core.getInstance().sendMessage(MSG_ERROR, Errors.getErrorDescr(r));
				} catch (Exception e) {
					Core.getInstance().sendMessage(MSG_ERROR, e.getLocalizedMessage());
				}
			};
		}.start();
	}
	
	
	private void doPostfiscal() {
		new Thread() {
			@Override
			public void run() {
				try {
					log("Подготовка к переводу в постфискальный режим");
					KKMInfo info = new KKMInfo();
					if(!Errors.isOK(Core.getInstance().storage().readKKMInfo(info))) throw new Exception("Ошибка чтения информации о ККТ");
					if(info.getShift().isOpen()) {
						log("Закрытие смены");
						Shift shift = new Shift();
						if(!Errors.isOK(Core.getInstance().storage().closeShift(new OU("Администратор"), shift , Const.EMPTY_STRING))) throw new Exception("Ошибка закрытия смены");
						log("Смена закрыта, ФД "+shift.signature().getFdNumber());
					}
					ArchiveReport archive = new ArchiveReport();
					if(!Errors.isOK(Core.getInstance().storage().doArchive(new OU("Администратор"), archive, Const.EMPTY_STRING))) throw new Exception("Ошибка перевола в постфискальный режим");
					log("Операция выполнена, ККТ в постфискальном режиме, ФД "+archive.signature().getFdNumber());
				} catch (Exception e) {
					Core.getInstance().sendMessage(MSG_ERROR, e.getLocalizedMessage());
				}
			}
		}.start();
	}

	@Override
	public boolean onMessage(Message msg) {
		switch (msg.what) {
		case MSG_INITIALIZE_CORE:
			log("Результат подключения: " + msg.obj.toString());
			break;
		case MSG_ERROR:
			log("Ошибка выполнения: " + msg.obj.toString());
			Toast.makeText(this, "Ошибка выполнения: " + msg.obj.toString(), Toast.LENGTH_LONG).show();
			break;
		case MSG_KKM_INFO:
			log("Информация о ККТ получена");
			new KKMInfoDialog(this, (KKMInfo) msg.obj).show();
			break;
		case MSG_CHECK_SHIFT_STATE:
			try {
				((Runnable) msg.obj).run();
			} catch (Exception e) {
				Log.e("FNCS", "Error", e);

			}
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.v_kkminfo:
			log("Получение информации о ККТ...");
			new Thread() {
				public void run() {
					try {
						KKMInfo kkmInfo = new KKMInfo();
						int r = Core.getInstance().storage().readKKMInfo(kkmInfo);
						if (Errors.isOK(r))
							Core.getInstance().sendMessage(MSG_KKM_INFO, kkmInfo);
						else
							Core.getInstance().sendMessage(MSG_ERROR, Errors.getErrorDescr(r));
					} catch (RemoteException re) {
						Core.getInstance().sendMessage(MSG_ERROR, re.getLocalizedMessage());
					}
				};
			}.start();
			break;
		case R.id.v_fiscal:
			doFiscalization();
			break;
		case R.id.v_shift_op:
			toggleShift();
			break;
		case R.id.v_report:
			new Thread() {
				public void run() {
					try {
						log("Запрос отчета о состоянии расчетов");
						FiscalReport report = new FiscalReport();
						int r = Core.getInstance().storage().requestFiscalReport(new OU("Администратор"), report,
								Const.EMPTY_STRING);
						if (Errors.isOK(r))
							log("Выполнено, номер ФД " + report.signature().getFdNumber());
						else
							Core.getInstance().sendMessage(MSG_ERROR, Errors.getErrorDescr(r));
					} catch (Exception e) {
						Core.getInstance().sendMessage(MSG_ERROR, e.getLocalizedMessage());
					}
				};
			}.start();
			break;
		case R.id.v_check:
			checkShiftIsOpen(new Runnable() {
				@Override
				public void run() {
					startActivityForResult(new Intent(Main.this, SellOrderActivity.class), MSG_SELLORDER);
				}
			});
			break;
		case R.id.v_corr:
			checkShiftIsOpen(new Runnable() {
				@Override
				public void run() {
					startActivityForResult(new Intent(Main.this, CorrectionActivity.class), MSG_CORRECTION);
				}
			});
			break;

		case R.id.v_postfiscal: {
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setMessage("Перевести ФН в постфискальный режим?");
			b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					doPostfiscal();
				}
			});
			b.setNegativeButton(android.R.string.cancel, null);
			b.show();
			
		}
			break;
		case R.id.v_reset:
			new Thread() {
				public void run() {
					log("Получение счетчиков смены...");
					try {
						FNCounters c = new FNCounters();
						 if(Errors.isOK(Core.getInstance().storage().getFNCounters(c, true))) {
							 log("Операций за смену "+c.getTotalOperationCounter());
							 log("Чеков прихода "+c.Income().count);
							 log(String.format("Сумма прихода %.2f",c.Income().totalSum/100.0));
							 log(String.format("Наличными %.2f",c.Income().totalSumCash/100.0));
							 log(String.format("Безналичными %.2f",c.Income().totalSumCard/100.0));
						 }
					} catch(Exception e) {
						Core.getInstance().sendMessage(MSG_ERROR, e.getLocalizedMessage());
					}
					
				};
			}.start();
			break;
		case R.id.v_off:
		case R.id.v_on:	
			try {
				Core.getInstance().storage().setUSBMonitorMode(v.getId() == R.id.v_on);
			} catch(Exception e) {
				
			}
			break;
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case MSG_SELLORDER: {
				final SellOrder order = data.getParcelableExtra(SellOrderActivity.ORDER_EXTRA);
				new Thread() {
					public void run() {
						try {
/*							order.getAgentData().setType(AgentTypeE.OTHER);
							order.getAgentData().setAgentPhone("+7 999 999 9889"); */
							log("Проведение чека");
							int r = Core.getInstance().storage().doSellOrder(order, new OU("Кассир"), order, true,
									Const.EMPTY_STRING, Const.EMPTY_STRING, Const.EMPTY_STRING, Const.EMPTY_STRING);
							if (Errors.isOK(r))
								log("Выполнено, чек № " + order.getNumber() + ", ФД "
										+ order.signature().getFdNumber());
							else
								Core.getInstance().sendMessage(MSG_ERROR, Errors.getErrorDescr(r));

						} catch (Exception e) {
							Core.getInstance().sendMessage(MSG_ERROR, e.getLocalizedMessage());
						}
					};
				}.start();
			}
			break;
			case MSG_CORRECTION: {
				final Correction cor = data.getParcelableExtra(SellOrderActivity.ORDER_EXTRA);
				new Thread() {
					public void run() {
						try {
							log("Проведение чека");
							int r = Core.getInstance().storage().doCorrection(cor, new OU("Кассир"), cor, Const.EMPTY_STRING);
							if (Errors.isOK(r))
								log("Выполнено, чек коррекции № " + cor.getNumber() + ", ФД "
										+ cor.signature().getFdNumber());
							else
								Core.getInstance().sendMessage(MSG_ERROR, Errors.getErrorDescr(r));

						} catch (Exception e) {
							Core.getInstance().sendMessage(MSG_ERROR, e.getLocalizedMessage());
						}
					};
				}.start();
			}
			break;
			}
		}
	}

}
