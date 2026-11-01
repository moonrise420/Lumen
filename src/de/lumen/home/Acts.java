package de.lumen.home;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.provider.Settings;

/** Actions shared by home, menus, the remote service and the phone page. */
final class Acts {
    private Acts() {}

    // ---- sources ----
    static void hdmi(Context c) {
        applyZoomScene(c, "hdmi");
        if (!L.comp(c, "com.softwinner.awlivetv", "com.softwinner.awlivetv.MainActivity"))
            L.comp(c, "com.softwinner.awsource", "com.softwinner.awsource.MainActivity");
    }

    // ---- per-input picture size ----
    // Lumen keeps a size for apps (the launcher view) and one for HDMI, and applies it live when switching.
    static void saveZoomScene(Context c, String key, int g) { L.p(c).edit().putInt("zoomG_" + key, g).apply(); }

    static boolean hasZoomScene(Context c, String key) { return L.p(c).contains("zoomG_" + key); }

    static void applyZoomScene(Context c, String key) {
        if (!Sys.on()) return;
        int g = L.p(c).getInt("zoomG_" + key, Integer.MIN_VALUE);
        if (g == Integer.MIN_VALUE) return;
        int[] base = ScreenSizeActivity.baseline(c);
        int[] v = new int[8];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) { v[i] = Math.max(0, base[i] - g); sb.append(i > 0 ? "," : "").append(v[i]); }
        final String prop = sb.toString();
        L.IO.execute(() -> L.sh("setprop persist.sys.zoom.value " + prop));
        Sys.zoomLive(v);
    }

    // ---- one-tap scenes (Modus) ----
    static final String[] SCENE_NAMES = {"Standard", "Film", "Spiele", "Heller Raum"};

    static String[] sceneNames() {
        return new String[]{"Standard", Lang.t("Movie", "Film"), Lang.t("Games", "Spiele"), Lang.t("Bright room", "Heller Raum")};
    }

    static void applyScene(Context c, int which) {
        AudioManager au = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        int max = au.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        switch (which) {
            case 1: // Film
                au.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(max * 0.6f), 0);
                setAnim(c, 1); setOff(c, 5);
                if (nm.isNotificationPolicyAccessGranted()) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                L.toast(c, "Film-Modus: Ton 60 %, keine Störungen, kein Bildschirmschoner");
                break;
            case 2: // Spiele
                au.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(max * 0.7f), 0);
                setAnim(c, 1); setOff(c, 5);
                if (nm.isNotificationPolicyAccessGranted()) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                L.toast(c, "Spiele-Modus: Ton 70 %, schnelle Animationen, kein Bildschirmschoner");
                break;
            case 3: // Heller Raum -> max brightness + open stock picture settings
                try { android.provider.Settings.System.putInt(c.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, 255); } catch (Exception ignored) {}
                L.comp(c, "com.android.tv.settings", "com.android.tv.settings.device.displaysound.aw_displaysound.PictureSettingsActivity");
                L.toast(c, "Heller Raum: Helligkeit hoch – Feineinstellung im Bildmenü");
                break;
            default: // Standard
                au.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(max * 0.5f), 0);
                setAnim(c, 2); setOff(c, 2);
                if (nm.isNotificationPolicyAccessGranted()) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                L.toast(c, "Standard-Modus");
        }
    }

    static void usb(Context c) { L.start(c, new Intent(c, FilesActivity.class)); }

    static void mirror(Context c) { L.comp(c, "com.softwinner.miracastReceiver", "com.softwinner.miracastReceiver.Miracast"); }

    // ---- memory ----
    /** Asks Android to drop every background app except what's needed. Returns MB gained. */
    static long freeMemory(Context c) {
        long before = Status.ramFreeMb(c);
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = c.getPackageManager();
        for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
            if (ai.packageName.equals(c.getPackageName())) continue;
            if (ai.packageName.startsWith("com.google.android.gms") || ai.packageName.equals("android")) continue;
            am.killBackgroundProcesses(ai.packageName);
        }
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        return Math.max(0, Status.ramFreeMb(c) - before);
    }

    // ---- sleep timer ----
    static final int[] SLEEP_MIN = {0, 15, 30, 45, 60, 90, 120};
    static final String[] SLEEP_NAMES = {"Aus", "15 min", "30 min", "45 min", "1 Std", "1,5 Std", "2 Std"};

    static void setSleep(Context c, int minutes) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, new Intent(c, SleepReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if (minutes <= 0) { L.p(c).edit().remove("sleepAt").apply(); return; }
        long at = System.currentTimeMillis() + minutes * 60_000L;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        L.p(c).edit().putLong("sleepAt", at).apply();
    }

    static long sleepLeftMin(Context c) {
        long at = L.p(c).getLong("sleepAt", 0);
        long left = at - System.currentTimeMillis();
        return left > 0 ? (left + 59_999) / 60_000 : 0;
    }

    static int sleepIndex(Context c) {
        long left = sleepLeftMin(c);
        if (left == 0) return 0;
        int best = 1;
        for (int i = 1; i < SLEEP_MIN.length; i++) if (SLEEP_MIN[i] >= left) { best = i; break; }
        return best;
    }

    static void standby(Context c) {
        if (Sys.standby(c)) return;
        if (RemoteService.inst != null) RemoteService.inst.sleepNow();
        else L.toast(c, Lang.t("Please enable the Lumen key helper (Advanced › Remote)", "Bitte den Lumen-Tastenassistenten aktivieren (Erweitert › Fernbedienung)"));
    }

    // ---- cinema mode ----
    static boolean cinema(Context c) { return L.p(c).getBoolean("cinema", false); }

    static void setCinema(Context c, boolean on) {
        AudioManager au = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        int max = au.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (on) {
            L.p(c).edit().putBoolean("cinema", true)
                    .putInt("cinemaPrevVol", au.getStreamVolume(AudioManager.STREAM_MUSIC))
                    .putInt("cinemaPrevOff", Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1200000))
                    .apply();
            int pct = L.p(c).getInt("cinemaVol", 60);
            au.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(max * pct / 100f), AudioManager.FLAG_SHOW_UI);
            if (nm.isNotificationPolicyAccessGranted()) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            try { Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 4 * 3600_000); } catch (Exception ignored) {}
            L.IO.execute(() -> freeMemory(c));
            L.toast(c, Lang.t("Cinema mode on: sound " + pct + " %, interruptions off, screensaver off", "Kino-Modus an: Ton " + pct + " %, Störungen aus, Bildschirmschoner aus"));
        } else {
            au.setStreamVolume(AudioManager.STREAM_MUSIC, L.p(c).getInt("cinemaPrevVol", max / 2), AudioManager.FLAG_SHOW_UI);
            if (nm.isNotificationPolicyAccessGranted()) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            try { Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, L.p(c).getInt("cinemaPrevOff", 1200000)); } catch (Exception ignored) {}
            L.p(c).edit().putBoolean("cinema", false).apply();
            L.toast(c, "Kino-Modus aus");
        }
    }

    // ---- ad-blocking DNS (Android "Privates DNS") ----
    static final String[] DNS_NAMES = {"Aus", "AdGuard (Werbung + Tracker)", "Mullvad (Werbung + Tracker)", "Cloudflare (nur schnell)", "Quad9 (Schutz vor Schadseiten)"};
    static final String[] DNS_HOSTS = {null, "dns.adguard-dns.com", "adblock.dns.mullvad.net", "one.one.one.one", "dns.quad9.net"};

    static int dnsIndex(Context c) {
        String mode = Settings.Global.getString(c.getContentResolver(), "private_dns_mode");
        String host = Settings.Global.getString(c.getContentResolver(), "private_dns_specifier");
        if (!"hostname".equals(mode) || host == null) return 0;
        for (int i = 1; i < DNS_HOSTS.length; i++) if (DNS_HOSTS[i].equals(host)) return i;
        return 0;
    }

    static void setDns(Context c, int i) {
        try {
            if (i == 0) Settings.Global.putString(c.getContentResolver(), "private_dns_mode", "opportunistic");
            else {
                Settings.Global.putString(c.getContentResolver(), "private_dns_specifier", DNS_HOSTS[i]);
                Settings.Global.putString(c.getContentResolver(), "private_dns_mode", "hostname");
            }
        } catch (SecurityException e) { L.toast(c, "Fehlende Berechtigung (WRITE_SECURE_SETTINGS)"); }
    }

    // ---- animations ----
    static final String[] ANIM_NAMES = {"Aus", "Turbo 0,5×", "Normal 1×", "Gemütlich 1,5×"};
    static final float[] ANIM_VALS = {0f, 0.5f, 1f, 1.5f};

    static int animIndex(Context c) {
        float v = Settings.Global.getFloat(c.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        for (int i = 0; i < ANIM_VALS.length; i++) if (Math.abs(ANIM_VALS[i] - v) < 0.01) return i;
        return 2;
    }

    static void setAnim(Context c, int i) {
        try {
            float v = ANIM_VALS[i];
            Settings.Global.putFloat(c.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, v);
            Settings.Global.putFloat(c.getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, v);
            Settings.Global.putFloat(c.getContentResolver(), Settings.Global.WINDOW_ANIMATION_SCALE, v);
        } catch (SecurityException e) { L.toast(c, "Fehlende Berechtigung (WRITE_SECURE_SETTINGS)"); }
    }

    // ---- screensaver: off, or show after N ----
    static final String[] OFF_NAMES = {"Aus", "5 min", "10 min", "20 min", "30 min", "1 Std", "2 Std"};
    static final int[] OFF_VALS = {0, 300000, 600000, 1200000, 1800000, 3600000, 7200000}; // 0 = screensaver off

    static int offIndex(Context c) {
        try { if (Settings.Secure.getInt(c.getContentResolver(), "screensaver_enabled", 1) == 0) return 0; } catch (Exception ignored) {}
        int v = Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1200000);
        for (int i = 1; i < OFF_VALS.length; i++) if (OFF_VALS[i] >= v) return i;
        return OFF_VALS.length - 1;
    }

    static void setOff(Context c, int i) {
        try {
            if (OFF_VALS[i] == 0) { Settings.Secure.putInt(c.getContentResolver(), "screensaver_enabled", 0); }
            else {
                Settings.Secure.putInt(c.getContentResolver(), "screensaver_enabled", 1);
                Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, OFF_VALS[i]);
            }
        } catch (Exception e) { L.toast(c, Lang.t("Missing permission", "Fehlende Berechtigung")); }
    }

    // ---- units: temperature (°C / °F / K) and speed (Mbit/s / MB/s) ----
    static final String[] TEMP_NAMES = {"Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"};
    static final String[] SPEED_NAMES = {"Megabit (Mbit/s)", "Megabyte (MB/s)"};

    private static java.util.Locale loc(Context c) { return Lang.EN ? java.util.Locale.ENGLISH : java.util.Locale.GERMAN; }

    static int tempUnit(Context c) { return L.p(c).getInt("tempUnit", 0); }     // 0=C 1=F 2=K
    static String tempUnitName(Context c) { return new String[]{"°C", "°F", "K"}[tempUnit(c)]; }

    /** A Celsius reading formatted in the chosen unit, e.g. "72 °C" / "162 °F" / "345 K". -1 means "no reading". */
    static String fmtTemp(Context c, int cel) {
        if (cel < 0) return "–";
        int u = tempUnit(c);
        int v = u == 1 ? Math.round(cel * 9f / 5f + 32) : u == 2 ? cel + 273 : cel;
        return v + " " + tempUnitName(c);
    }

    static boolean speedBits(Context c) { return L.p(c).getInt("speedUnit", 0) == 0; } // 0=bits (default) 1=bytes

    /** One measured throughput (bytes/s) in the chosen unit, with its unit suffix. */
    static String fmtRate(Context c, long bps) {
        if (bps < 0) bps = 0;
        if (speedBits(c)) {
            double bits = bps * 8.0;
            if (bits < 1_000_000) return Math.round(bits / 1000) + " kbit/s";
            return String.format(loc(c), "%.1f Mbit/s", bits / 1_000_000);
        }
        if (bps < 1024 * 1024) return (bps / 1024) + " KB/s";
        return String.format(loc(c), "%.1f MB/s", bps / 1048576.0);
    }

    /** Down/up throughput (bytes/s) as one compact pill string: "↓1.2 ↑0.3 Mbit/s" or "↓14 ↑28 KB/s". */
    static String ratePair(Context c, long d, long u) {
        if (d < 0) d = 0; if (u < 0) u = 0;
        if (speedBits(c))
            return String.format(loc(c), "↓%.1f ↑%.1f Mbit/s", d * 8.0 / 1_000_000, u * 8.0 / 1_000_000);
        return "↓" + (d / 1024) + " ↑" + (u / 1024) + " KB/s";
    }

    /** A link rate reported in Mbit (e.g. Wi-Fi getLinkSpeed) formatted in the chosen unit. */
    static String fmtLink(Context c, int mbit) {
        if (mbit <= 0) return "–";
        if (speedBits(c)) return mbit + " Mbit/s";
        return String.format(loc(c), "%.1f MB/s", mbit / 8.0);
    }

    // ---- UI density (applies after restart) ----
    static final String[] DENS_NAMES = {"Kompakt (200)", "Klein (220)", "Standard (240)", "Groß (260)", "Sehr groß (280)"};
    static final int[] DENS_VALS = {200, 220, 240, 260, 280};

    static int densIndex(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), "display_density_forced");
        int v = 240;
        try { if (s != null && !s.isEmpty()) v = Integer.parseInt(s); } catch (Exception ignored) {}
        for (int i = 0; i < DENS_VALS.length; i++) if (DENS_VALS[i] == v) return i;
        return 2;
    }

    static void setDens(Context c, int i) {
        try {
            if (DENS_VALS[i] == 240) Settings.Secure.putString(c.getContentResolver(), "display_density_forced", "");
            else Settings.Secure.putString(c.getContentResolver(), "display_density_forced", String.valueOf(DENS_VALS[i]));
            L.toast(c, "Wird nach einem Neustart aktiv");
        } catch (SecurityException e) { L.toast(c, "Fehlende Berechtigung (WRITE_SECURE_SETTINGS)"); }
    }

    // ---- readability ----
    static boolean highContrast(Context c) {
        return Settings.Secure.getInt(c.getContentResolver(), "high_text_contrast_enabled", 0) == 1;
    }

    static void setHighContrast(Context c, boolean on) {
        try { Settings.Secure.putInt(c.getContentResolver(), "high_text_contrast_enabled", on ? 1 : 0); }
        catch (Exception e) { L.toast(c, "Fehlende Berechtigung"); }
    }

    // ---- captions / subtitles (system-wide) ----
    static boolean captionsOn(Context c) {
        return Settings.Secure.getInt(c.getContentResolver(), "accessibility_captioning_enabled", 0) == 1;
    }

    static void setCaptions(Context c, boolean on) {
        try { Settings.Secure.putInt(c.getContentResolver(), "accessibility_captioning_enabled", on ? 1 : 0); }
        catch (Exception e) { L.toast(c, "Fehlende Berechtigung"); }
    }

    static final String[] CAP_NAMES = {"Klein", "Normal", "Groß", "Sehr groß"};
    static final float[] CAP_VALS = {0.75f, 1.0f, 1.5f, 2.0f};

    static int capIndex(Context c) {
        float v = Settings.Secure.getFloat(c.getContentResolver(), "accessibility_captioning_font_scale", 1f);
        int best = 1;
        float bd = 99;
        for (int i = 0; i < CAP_VALS.length; i++) if (Math.abs(CAP_VALS[i] - v) < bd) { bd = Math.abs(CAP_VALS[i] - v); best = i; }
        return best;
    }

    static void setCap(Context c, int i) {
        try { Settings.Secure.putFloat(c.getContentResolver(), "accessibility_captioning_font_scale", CAP_VALS[i]); }
        catch (Exception e) { L.toast(c, "Fehlende Berechtigung"); }
    }

    // ---- mouse pointer speed (Settings.System.POINTER_SPEED, -7..7) ----
    static final String[] PTR_NAMES = {"Langsam", "Normal", "Schnell", "Sehr schnell"};
    static final int[] PTR_VALS = {-3, 0, 3, 6};

    static int ptrIndex(Context c) {
        int v = Settings.System.getInt(c.getContentResolver(), "pointer_speed", 0);
        int best = 1, bd = 99;
        for (int i = 0; i < PTR_VALS.length; i++) if (Math.abs(PTR_VALS[i] - v) < bd) { bd = Math.abs(PTR_VALS[i] - v); best = i; }
        return best;
    }

    static void setPtr(Context c, int i) {
        try { Settings.System.putInt(c.getContentResolver(), "pointer_speed", PTR_VALS[i]); }
        catch (Exception e) { L.toast(c, "Fehlende Berechtigung"); }
    }

    // ---- set Lumen as the screensaver ----
    static void useLumenDream(Context c) {
        try {
            Settings.Secure.putString(c.getContentResolver(), "screensaver_components", "de.lumen.home/.Dream");
            Settings.Secure.putString(c.getContentResolver(), "screensaver_default_component", "de.lumen.home/.Dream");
            Settings.Secure.putInt(c.getContentResolver(), "screensaver_enabled", 1);
            Settings.Secure.putInt(c.getContentResolver(), "screensaver_activate_on_sleep", 1);
            Settings.Secure.putInt(c.getContentResolver(), "screensaver_activate_on_dock", 1);
        } catch (SecurityException ignored) {}
    }

    static boolean lumenDreamOn(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), "screensaver_components");
        return s != null && s.contains("de.lumen.home");
    }

    // ---- favourites + settings backup ----
    static java.io.File backupFile(Context c, java.io.File dir) {
        return new java.io.File(dir, "lumen-backup.json");
    }

    static boolean export(Context c, java.io.File dst) {
        try {
            org.json.JSONObject o = new org.json.JSONObject();
            for (java.util.Map.Entry<String, ?> e : L.p(c).getAll().entrySet()) {
                Object v = e.getValue();
                if (v == null) continue;
                // skip volatile caches
                if (e.getKey().startsWith("wx") || e.getKey().equals("photoFetch")) continue;
                o.put(e.getKey(), v instanceof Integer ? "i:" + v : v instanceof Boolean ? "b:" + v
                        : v instanceof Long ? "l:" + v : v instanceof Float ? "f:" + v : "s:" + v);
            }
            dst.getParentFile().mkdirs();
            try (java.io.OutputStream os = new java.io.FileOutputStream(dst)) { os.write(o.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            return true;
        } catch (Throwable t) { return false; }
    }

    static boolean importFrom(Context c, java.io.File src) {
        try {
            byte[] b = java.nio.file.Files.readAllBytes(src.toPath());
            org.json.JSONObject o = new org.json.JSONObject(new String(b, java.nio.charset.StandardCharsets.UTF_8));
            android.content.SharedPreferences.Editor ed = L.p(c).edit();
            java.util.Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                String v = o.getString(k);
                if (v.startsWith("i:")) ed.putInt(k, Integer.parseInt(v.substring(2)));
                else if (v.startsWith("b:")) ed.putBoolean(k, Boolean.parseBoolean(v.substring(2)));
                else if (v.startsWith("l:")) ed.putLong(k, Long.parseLong(v.substring(2)));
                else if (v.startsWith("f:")) ed.putFloat(k, Float.parseFloat(v.substring(2)));
                else ed.putString(k, v.substring(2));
            }
            ed.apply();
            return true;
        } catch (Throwable t) { return false; }
    }
}
