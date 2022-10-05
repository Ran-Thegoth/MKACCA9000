package rs.fncore2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import rs.utils.Utils;

public class Restarter extends BroadcastReceiver {

    public Restarter() {
    }

    @Override
    public void onReceive(Context context, Intent arg1) {
        Utils.startService(context);
    }

}
