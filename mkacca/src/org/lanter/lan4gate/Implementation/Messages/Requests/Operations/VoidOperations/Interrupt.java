package org.lanter.lan4gate.Implementation.Messages.Requests.Operations.VoidOperations;

import org.lanter.lan4gate.Implementation.Messages.Requests.Request;
import org.lanter.lan4gate.Messages.OperationsList;

public class Interrupt extends Request  {

	public Interrupt() {
		setOperationCode(OperationsList.Interrupt);
	}

}
