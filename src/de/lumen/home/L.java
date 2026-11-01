package de.lumen.home;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared helpers: prefs, scaling, threads, launching, shell. */
final class L {
    private L() {}

    static final String VERSION = "1.1"; // shown as "LUMEN V1.1" top-left
    static boolean BOLD = false; // readability: bolder text everywhere

    /** Everything is laid out for a 1280x720 screen and scaled by F. */
    static float F = 1f;
    static int W = 1280, H = 720;

    static final ExecutorService IO = Executors.newFixedThreadPool(2);
    static final Handler UI = new Handler(Looper.getMainLooper());

    static void init(Activity a) {
        DisplayMetrics m = new DisplayMetrics();
        a.getWindowManager().getDefaultDisplay().getRealMetrics(m);
        W = Math.max(m.widthPixels, m.heightPixels);
        H = Math.min(m.widthPixels, m.heightPixels);
        F = W / 1280f;
    }

    static int s(float v) { return Math.round(v * F); }

    static SharedPreferences p(Context c) { return c.getSharedPreferences("lumen", Context.MODE_PRIVATE); }

    // ---- bundled brand assets ----
    private static android.graphics.Typeface geo;
    private static final java.util.Map<String, android.graphics.Bitmap> assets = new java.util.HashMap<>();

    /** Geometric light font (Questrial), close to the Soulride wordmark. */
    static android.graphics.Typeface geo(Context c) {
        if (geo == null) {
            try { geo = android.graphics.Typeface.createFromAsset(c.getAssets(), "fonts/geo.ttf"); }
            catch (Throwable t) { geo = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL); }
        }
        return geo;
    }

    /** A bundled PNG asset (soulride_logo, soulride_mark, qr_soulride), cached. */
    static android.graphics.Bitmap asset(Context c, String name) {
        android.graphics.Bitmap b = assets.get(name);
        if (b != null) return b;
        try (java.io.InputStream in = c.getAssets().open(name + ".png")) {
            b = android.graphics.BitmapFactory.decodeStream(in);
            assets.put(name, b);
        } catch (Throwable ignored) {}
        return b;
    }

    static boolean anim(Context c) { return p(c).getBoolean("anim", true); }

    // ---- favourites ----
    static final String[] DEFAULT_FAVS = {
            "com.netflix.mediaclient", "com.google.android.youtube.tv", "com.amazon.amazonvideo.livingroom",
            "tv.twitch.android.app", "com.fgl27.twitch", "org.jellyfin.androidtv", "com.valvesoftware.steamlink",
            "com.zappntv", "com.brave.browser", "org.mozilla.firefox", "com.phlox.tvwebbrowser", "com.android.vending"};

    static List<String> favs(Context c) {
        String s = p(c).getString("favs", null);
        List<String> out = new ArrayList<>();
        PackageManager pm = c.getPackageManager();
        for (String pkg : s == null ? Arrays.asList(DEFAULT_FAVS) : Arrays.asList(s.split(","))) {
            if (pkg.isEmpty()) continue;
            if (launchIntent(c, pkg) != null || pkg.startsWith("@")) out.add(pkg);
        }
        return out;
    }

    static void saveFavs(Context c, List<String> l) {
        p(c).edit().putString("favs", String.join(",", l)).apply();
    }

    // ---- launching ----
    static Intent launchIntent(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        return i;
    }

    static boolean launch(Context c, String pkg) {
        Intent i = launchIntent(c, pkg);
        if (i == null) { toast(c, "App nicht gefunden"); return false; }
        return start(c, i);
    }

    static boolean start(Context c, Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            toast(c, "Lässt sich nicht öffnen");
            return false;
        }
    }

    static boolean comp(Context c, String pkg, String cls) {
        if (pkg.contains("htcsettings")) RemoteService.allowStock(); // we're opening it on purpose, don't bounce it to Advanced
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(new ComponentName(pkg, cls));
        return start(c, i);
    }

    static boolean action(Context c, String action) { return start(c, new Intent(action)); }

    static void toast(Context c, String s) { Toast.makeText(c.getApplicationContext(), Lang.EN ? Tr.t(s) : s, Toast.LENGTH_SHORT).show(); }

    // ---- system ----
    static String sh(String cmd) {
        StringBuilder sb = new StringBuilder();
        try {
            Process pr = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
            }
            pr.waitFor();
        } catch (Exception ignored) {}
        return sb.toString().trim();
    }

    static String prop(String key) { return sh("getprop " + key); }

    // ---- key helper keep-alive ----
    // This firmware clears the enabled accessibility services whenever another app starts. Lumen may write
    // secure settings, so it puts its helper back (unless the user switched keep-alive off).
    static final String HELPER = "de.lumen.home/de.lumen.home.RemoteService";
    private static android.database.ContentObserver helperWatch;

    static void ensureHelper(Context c) {
        if (!p(c).getBoolean("helperKeep", true)) return;
        android.content.ContentResolver cr = c.getContentResolver();
        String cur = android.provider.Settings.Secure.getString(cr, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (cur != null && (cur.contains(HELPER) || cur.contains("de.lumen.home/.RemoteService"))) return;
        try {
            android.provider.Settings.Secure.putString(cr, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    cur == null || cur.isEmpty() ? HELPER : cur + ":" + HELPER);
            android.provider.Settings.Secure.putInt(cr, android.provider.Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (SecurityException ignored) {}
    }

    static void watchHelper(Context c) {
        if (helperWatch != null) return;
        Context app = c.getApplicationContext();
        helperWatch = new android.database.ContentObserver(UI) {
            @Override public void onChange(boolean self) {
                UI.removeCallbacks(fix);
                UI.postDelayed(fix, 2000);
            }
            final Runnable fix = () -> ensureHelper(app);
        };
        app.getContentResolver().registerContentObserver(
                android.provider.Settings.Secure.getUriFor(android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, helperWatch);
        ensureHelper(app);
    }

    static String readFile(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(new File(path)))) {
            String l = r.readLine();
            return l == null ? "" : l.trim();
        } catch (Exception e) { return ""; }
    }
}
