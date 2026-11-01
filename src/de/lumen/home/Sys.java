package de.lumen.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;

import java.lang.reflect.Method;

/**
 * Powers that only work when Lumen runs as a system app (platform-signed, sharedUserId android.uid.system).
 * On a normal build every call here is a no-op that returns false, and the UI falls back to the stock way.
 */
final class Sys {
    private Sys() {}

    /** True when this build runs with system privileges. */
    static boolean on() { return Process.myUid() == Process.SYSTEM_UID; }

    /**
     * Apply the picture size live, the way the stock settings app does: 8 corner values (each 0..1000) sent to
     * SurfaceFlinger as floats via binder call 1050 on interface android.ui.ISurfaceComposer. Only a system/
     * graphics caller is allowed, so this returns false on a normal build.
     */
    static boolean zoomLive(int[] v8) {
        if (!on() || v8.length != 8) return false;
        Parcel data = null;
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method get = sm.getMethod("getService", String.class);
            IBinder sf = (IBinder) get.invoke(null, "SurfaceFlinger");
            if (sf == null) return false;
            data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            for (int x : v8) data.writeFloat(x * 0.001f);
            sf.transact(1050, data, null, 0);
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (data != null) data.recycle();
        }
    }

    /** Restart the projector. Needs REBOOT permission (system). Returns false on a normal build. */
    static boolean reboot(Context c) {
        if (!on()) return false;
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            pm.reboot(null);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Turn the screen/picture off (standby) at the system level. */
    static boolean standby(Context c) {
        if (!on()) return false;
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            Method m = PowerManager.class.getMethod("goToSleep", long.class);
            m.invoke(pm, android.os.SystemClock.uptimeMillis());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** True when Lumen is already the current default home screen. */
    static boolean isHome(Context c) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo ri = c.getPackageManager().resolveActivity(i, 0);
            return ri != null && ri.activityInfo != null && c.getPackageName().equals(ri.activityInfo.packageName);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Make Lumen the default home screen. As a system app we set it silently via IPackageManager.setHomeActivity;
     * otherwise (or if that fails) we open the system's "default home" picker so the user can choose. Returns true
     * only when it was set silently.
     */
    static boolean makeHome(Context c) {
        ComponentName cn = new ComponentName(c, HomeActivity.class);
        if (on()) {
            try {
                Class<?> smC = Class.forName("android.os.ServiceManager");
                IBinder b = (IBinder) smC.getMethod("getService", String.class).invoke(null, "package");
                Object ipm = Class.forName("android.content.pm.IPackageManager$Stub")
                        .getMethod("asInterface", IBinder.class).invoke(null, b);
                int uid = 0;
                try { uid = (int) Class.forName("android.os.UserHandle").getMethod("myUserId").invoke(null); } catch (Throwable ignored) {}
                ipm.getClass().getMethod("setHomeActivity", ComponentName.class, int.class).invoke(ipm, cn, uid);
                return true;
            } catch (Throwable ignored) {}
        }
        try {
            c.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable t1) {
            try { c.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Throwable ignored) {}
        }
        return false;
    }
}
