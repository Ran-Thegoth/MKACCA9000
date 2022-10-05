package rs.fncore2;

import org.json.JSONObject;

import rs.fncore.Errors;
import rs.fncore2.core.ServiceBinder;
import rs.log.Logger;


public class JsonCommands {

    public static final String ACTION_INIT = "init";

    public static final String FIELD_RESULT = "result";

    private final ServiceBinder mServiceBinder;

    public JsonCommands(ServiceBinder binder) {
        mServiceBinder = binder;
    }

    public String execute(String action, String argsJson) {
        try {
/*            JSONObject argsObj = ((argsJson == null) || argsJson.isEmpty())
                ? new JSONObject() : new JSONObject(argsJson); */
            JSONObject replyObj = new JSONObject();
            int result = Errors.NOT_IMPLEMENTED;

            if (ACTION_INIT.equals(action)) {
                result = mServiceBinder.waitFnReady(1000);
            }

            replyObj.put(FIELD_RESULT, result);
            return replyObj.toString();

        } catch (Exception e) {
            Logger.w("JsonCmd exc: " + e);
        }
        return "{}";
    }
}
