package de.lumen.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Sleep timer fired: standby. */
public class SleepReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        L.p(c).edit().remove("sleepAt").apply();
        if (Acts.cinema(c)) Acts.setCinema(c, false);
        if (RemoteService.inst != null) RemoteService.inst.sleepNow();
    }
}
