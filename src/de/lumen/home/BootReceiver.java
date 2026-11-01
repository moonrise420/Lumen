package de.lumen.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After boot: phone keyboard back on, stale sleep timer and cinema mode cleared. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        L.p(c).edit().remove("sleepAt").putBoolean("cinema", false).apply();
        if (L.p(c).getBoolean("phone", true)) PhoneServer.start(c);
    }
}
