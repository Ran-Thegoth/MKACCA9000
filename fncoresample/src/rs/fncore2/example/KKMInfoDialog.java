package rs.fncore2.example;

import android.content.Context;
import android.view.View;
import rs.fncore.Const;
import rs.fncore.data.KKMInfo;
import rs.fncore.data.TaxModeE;

public class KKMInfoDialog extends DocumentInfoDialog {

	public KKMInfoDialog(Context ctx, KKMInfo doc) {
		super(ctx,doc);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected View buildUserView() {
		TableBuilder b = new TableBuilder();
		b.addRow("Зав. № ККТ", getDocument().getKKMSerial());
		String status = "Не установлен";
		if(getDocument().isFNPresent())
			status = "Не фискализирован";
		if(getDocument().isFNActive())
			status = "Готов к работе";
		if(getDocument().isFNArchived())
			status = "Постфискальный режим";
		
		b.addRow("Статус ФН", status);
		if(getDocument().isFNPresent()) {
			b.addRow("№ ФН",getDocument().getFNNumber());
			if(getDocument().isFNActive() || getDocument().isFNArchived()) {
				b.addRow("Рег №",getDocument().getKKMNumber());
				b.addRow("Владелец",getDocument().getOwner().getName());
				b.addRow("ИНН",getDocument().getOwner().getINN());
				String s = Const.EMPTY_STRING;
				for(TaxModeE tax : 	getDocument().getTaxModes()) {
					if(!s.isEmpty()) s+=",";
					s += tax.toString();
				}
				b.addRow("СНО",s);
				if(getDocument().isFNActive())
					b.addRow("Смена",String.valueOf(getDocument().getShift().getNumber()));
			}
		}
		
		return b.getView();
	}
	@Override
	protected KKMInfo getDocument() {
		return (KKMInfo)super.getDocument();
	}

}
