package org.lanter.lan4gate.Implementation.Messages.Requests.Operations.ServiceOperations;

import org.lanter.lan4gate.Messages.Fields.RequestFieldsList;
import org.lanter.lan4gate.Messages.OperationsList;
import org.lanter.lan4gate.Implementation.Messages.Requests.Request;

public class Settlement extends Request {
    public Settlement() {
        setOperationCode(OperationsList.Settlement);
        addOptionalFields(RequestFieldsList.EcrMerchantNumber);
    }
}
