package de.lumen.home;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Lumen self-update. By default it checks this project's latest GitHub release and installs the matching APK
 * (the "system" asset on a system build, the normal one otherwise). A custom updateUrl (JSON {version, apk})
 * overrides GitHub. The GitHub check needs the repo to be public (no token is shipped). The APK must be signed
 * with the same key as the installed app, or the system rejects it; as a system app Lumen installs it silently.
 */
final class Updater {
    private Updater() {}

    /** This project's releases. The latest one is checked when no custom updateUrl is set. */
    static final String GITHUB_API = "https://api.github.com/repos/moonrise420/Lumen/releases/latest";

    static String url(Context c) { return L.p(c).getString("updateUrl", ""); }

    interface Cb { void done(String msg); }

    static void check(Context c, boolean manual, Cb cb) {
        String custom = url(c);
        L.IO.execute(() -> {
            try {
                String ver, apk;
                if (!custom.isEmpty()) {                        // your own version.json {version, apk}
                    JSONObject o = new JSONObject(new String(get(custom), StandardCharsets.UTF_8));
                    ver = o.getString("version");
                    apk = o.getString("apk");
                } else {                                         // latest GitHub release (needs a public repo)
                    JSONObject o = new JSONObject(new String(get(GITHUB_API), StandardCharsets.UTF_8));
                    String tag = o.optString("tag_name", "");
                    ver = tag.startsWith("v") ? tag.substring(1) : tag;
                    apk = pickAsset(o.optJSONArray("assets"), Sys.on());
                    if (ver.isEmpty() || apk == null) { L.UI.post(() -> cb.done(Lang.t("No release found", "Keine Version gefunden"))); return; }
                }
                if (!newer(ver, L.VERSION)) { L.UI.post(() -> cb.done(Lang.t("Lumen is up to date (v" + L.VERSION + ")", "Lumen ist aktuell (v" + L.VERSION + ")"))); return; }
                final String fver = ver, fapk = apk;
                L.UI.post(() -> cb.done(Lang.t("Downloading v" + fver + " …", "Lade v" + fver + " …")));
                byte[] data = get(fapk);
                File f = new File(c.getCacheDir(), "update.apk");
                try (OutputStream os = new FileOutputStream(f)) { os.write(data); }
                install(c, f, fver, cb);
            } catch (Throwable t) {
                L.UI.post(() -> cb.done(Lang.t("Update check failed", "Update fehlgeschlagen")));
            }
        });
    }

    /** From a GitHub release's assets, pick the right APK: the "system" one on a system build, otherwise the normal one. */
    private static String pickAsset(JSONArray assets, boolean system) {
        if (assets == null) return null;
        String match = null, anyApk = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.optJSONObject(i);
            if (a == null) continue;
            String name = a.optString("name", "").toLowerCase();
            String dl = a.optString("browser_download_url", "");
            if (!name.endsWith(".apk") || dl.isEmpty()) continue;
            if (anyApk == null) anyApk = dl;
            if (name.contains("system") == system && match == null) match = dl;
        }
        return match != null ? match : anyApk;
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
