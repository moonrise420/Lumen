package de.lumen.home;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Lumen self-update from a URL the owner controls. The URL returns JSON {"version":"1.1","apk":"https://…/Lumen.apk","notes":"…"}.
 * The APK must be signed with the same platform key, or the system rejects the update. As a system app Lumen installs it silently.
 */
final class Updater {
    private Updater() {}

    static String url(Context c) { return L.p(c).getString("updateUrl", ""); }

    interface Cb { void done(String msg); }

    static void check(Context c, boolean manual, Cb cb) {
        String u = url(c);
        if (u.isEmpty()) { cb.done(Lang.t("Set an update URL first", "Erst eine Update-Adresse eintragen")); return; }
        L.IO.execute(() -> {
            try {
                String js = new String(get(u), StandardCharsets.UTF_8);
                JSONObject o = new JSONObject(js);
                String ver = o.getString("version");
                String apk = o.getString("apk");
                if (!newer(ver, L.VERSION)) { L.UI.post(() -> cb.done(Lang.t("Lumen is up to date (v" + L.VERSION + ")", "Lumen ist aktuell (v" + L.VERSION + ")"))); return; }
                L.UI.post(() -> cb.done(Lang.t("Downloading v" + ver + " …", "Lade v" + ver + " …")));
                byte[] data = get(apk);
                File f = new File(c.getCacheDir(), "update.apk");
                try (OutputStream os = new FileOutputStream(f)) { os.write(data); }
                install(c, f, ver, cb);
            } catch (Throwable t) {
                L.UI.post(() -> cb.done(Lang.t("Update check failed", "Update fehlgeschlagen")));
            }
        });
    }

    /** Silent install via PackageInstaller (works because Lumen is a system app with INSTALL_PACKAGES). */
    private static void install(Context c, File apk, String ver, Cb cb) {
        try {
            PackageInstaller pi = c.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int id = pi.createSession(p);
            try (PackageInstaller.Session s = pi.openSession(id)) {
                try (InputStream in = new java.io.FileInputStream(apk); OutputStream out = s.openWrite("lumen", 0, apk.length())) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    s.fsync(out);
                }
                Intent cb2 = new Intent("de.lumen.home.INSTALL_DONE");
                PendingIntent pe = PendingIntent.getBroadcast(c, 0, cb2, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                s.commit(pe.getIntentSender());
            }
            L.UI.post(() -> cb.done(Lang.t("Installing v" + ver + " …", "Installiere v" + ver + " …")));
        } catch (Throwable t) {
            // fall back to the normal installer UI
            L.UI.post(() -> {
                cb.done(Lang.t("Opening installer …", "Öffne Installer …"));
                L.start(c, new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive"));
            });
        }
    }

    private static boolean newer(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? parse(x[i]) : 0, yi = i < y.length ? parse(y[i]) : 0;
            if (xi != yi) return xi > yi;
        }
        return false;
    }

    private static int parse(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }

    private static byte[] get(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(10000);
        h.setReadTimeout(30000);
        h.setInstanceFollowRedirects(true);
        h.setRequestProperty("User-Agent", "Lumen");
        try (InputStream in = h.getInputStream()) {
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toByteArray();
        } finally { h.disconnect(); }
    }
}
