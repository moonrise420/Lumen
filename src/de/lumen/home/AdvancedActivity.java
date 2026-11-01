package de.lumen.home;

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Two-pane expert menu: sections on the left, rows on the right. */
public class AdvancedActivity extends Base {
    private LinearLayout sections, content;
    private ScrollView contentScroll;
    private View currentSection;
    private TextView contentTitle;
    private int shown = -1;
    private int helpIdx = -1;
    private final List<Object[]> secs = new ArrayList<>();

    @Override void build() {
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(L.s(22), L.s(30), L.s(22), L.s(20));
        Ui.glass(left, t, Theme.alpha(t.panel, Math.max(0xC8, t.panel >>> 24)), 0);
        TextView h = Ui.one(this, "Erweitert", 30, t.head(), t.text);
        h.setPadding(L.s(12), 0, 0, L.s(16));
        left.addView(h);
        ScrollView ls = new ScrollView(this);
        ls.setVerticalScrollBarEnabled(false);
        ls.setClipToPadding(false);
        sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.VERTICAL);
        ls.addView(sections);
        left.addView(ls, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));
        root.addView(left, Ui.at(L.s(312), Ui.MATCH, Gravity.TOP | Gravity.LEFT, 0, 0));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        contentTitle = Ui.one(this, "", 26, t.head(), t.text);
        contentTitle.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        contentTitle.setPadding(L.s(6), 0, 0, L.s(10));
        right.addView(contentTitle);
        contentScroll = new ScrollView(this);
        contentScroll.setVerticalScrollBarEnabled(false);
        contentScroll.setClipToPadding(false);
        contentScroll.setPadding(0, L.s(4), 0, L.s(40));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        contentScroll.addView(content);
        contentScroll.setOnScrollChangeListener((v, x, y, ox, oy) -> content.invalidate());
        right.addView(contentScroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));
        root.addView(right, Ui.at(Ui.MATCH, Ui.MATCH, Gravity.TOP | Gravity.LEFT, 356, 34));
        ((android.widget.FrameLayout.LayoutParams) right.getLayoutParams()).rightMargin = L.s(48);

        sec("size", "Bild & Beamer", this::picture);
        sec("flask", "Tweak-Labor", this::tweaks);
        sec("net", "Netzwerk & Privatsphäre", this::network);
        sec("sound", "Bluetooth & Ton", this::sound);
        sec("storage", "Apps & Speicher", this::apps);
        sec("moon", "Energie & Schlaf", this::power);
        sec("remote", "Fernbedienung", this::remote);
        sec("phone", "Handy als Tastatur", this::phone);
        sec("palette", "Aussehen & Fotos", this::look);
        sec("eye", "Versteckte Menüs", this::hidden);
        sec("storage", "Sichern & Wiederherstellen", this::backup);
        sec("info", "System-Info", this::info);
        helpIdx = secs.size();
        sec("help", "Hilfe", c -> { startActivity(new Intent(this, HelpActivity.class)); });

        int start = getIntent().getIntExtra("section", 0);
        show(start);
        sections.getChildAt(start).requestFocus();
    }

    private void sec(String ico, String name, Consumer<LinearLayout> fill) {
        int idx = secs.size();
        secs.add(new Object[]{name, fill});
        Ui.Row r = new Ui.Row(this, t, ico, name, null, null);
        r.setBackground(null);
        r.setOnFocusChangeListener((v, has) -> {
            v.setBackground(has ? Ui.round(Theme.alpha(t.text, 0x22), L.s(Math.min(t.radius, 12))) : null);
            v.setForeground(has ? Ui.ring(t, L.s(Math.min(t.radius, 12))) : null);
            if (has) { currentSection = v; if (idx != helpIdx) show(idx); }
        });
        r.setOnClickListener(v -> {
            if (idx == helpIdx) { startActivity(new Intent(this, HelpActivity.class)); return; }
            if (content.getChildCount() > 0) firstFocusable(content).requestFocus();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(3);
        sections.addView(r, lp);
    }

    private View firstFocusable(LinearLayout l) {
        for (int i = 0; i < l.getChildCount(); i++) if (l.getChildAt(i).isFocusable()) return l.getChildAt(i);
        return l;
    }

    @SuppressWarnings("unchecked")
    private void show(int idx) {
        if (idx == shown) return;
        shown = idx;
        content.removeAllViews();
        contentTitle.setText(Lang.EN ? Tr.t((String) secs.get(idx)[0]) : (String) secs.get(idx)[0]);
        ((Consumer<LinearLayout>) secs.get(idx)[1]).accept(content);
        contentScroll.scrollTo(0, 0);
    }

    @Override void back() {
        View f = getCurrentFocus();
        if (f != null && currentSection != null && isInside(f, content)) currentSection.requestFocus();
        else finish();
    }

    private static boolean isInside(View v, View parent) {
        for (Object p = v.getParent(); p instanceof View; p = ((View) p).getParent()) if (p == parent) return true;
        return false;
    }

    // ------------------------------------------------------------------ helpers
    private void add(LinearLayout l, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(7);
        l.addView(v, lp);
    }

    private void head(LinearLayout l, String s) { l.addView(Ui.header(this, t, s)); }

    private Ui.Row open(LinearLayout l, String ico, String title, String sub, Runnable r) {
        Ui.Row row = Ui.action(this, t, ico, title, sub, () -> "›", r);
        add(l, row);
        return row;
    }

    private Ui.Row info(LinearLayout l, String ico, String title, java.util.function.Supplier<String> v) {
        Ui.Row row = Ui.action(this, t, ico, title, null, v, null);
        add(l, row);
        return row;
    }

    private Runnable comp(String pkg, String cls) { return () -> L.comp(this, pkg, cls); }

    private Runnable act(String action) { return () -> L.action(this, action); }

    // ------------------------------------------------------------------ sections
    private void picture(LinearLayout l) {
        head(l, "Bild");
        open(l, "size", "Bildgröße (Werk)", "Zoom des Beamers, innerhalb der Werksgrenzen", comp("com.htc.htcsettings", "com.htc.activity.ProjectionActivity"));
        open(l, "flask", "Bildgröße frei (über Werksgrenzen)", Sys.on() ? "Größer oder kleiner als das Werk erlaubt, sofort" : "Größer oder kleiner als das Werk erlaubt, wirkt nach Neustart", () -> startActivity(new Intent(this, ScreenSizeActivity.class)));
        open(l, "look", "Trapez & Projektion", "Werks-Menü: Trapezkorrektur, Spiegeln, Decke/Rückprojektion", comp("com.htc.htcsettings", "com.htc.activity.ProjectionActivity"));
        open(l, "photo", "Bildeinstellungen", "Helligkeit, Kontrast, Farbe, Bildmodus", comp("com.android.tv.settings", "com.android.tv.settings.device.displaysound.aw_displaysound.PictureSettingsActivity"));
        open(l, "gear", "Werks-Beamer-Menü", "Das Original-Einstellungsmenü des HY300", comp("com.htc.htcsettings", "com.htc.activity.HomeActivity"));
        head(l, "HDMI");
        add(l, Ui.toggle(this, t, "hdmi", "Automatisch auf HDMI", "Beim Einstecken eines Geräts umschalten (experimentell, nur von der Startseite aus)", () -> L.p(this).getBoolean("autoHdmi", false), v -> L.p(this).edit().putBoolean("autoHdmi", v).apply()));
        head(l, "Bildschirmschoner");
        add(l, Ui.choice(this, t, "clock", "Bildschirmschoner nach", "Zeit ohne Tastendruck", Acts.OFF_NAMES, () -> Acts.offIndex(this), i -> Acts.setOff(this, i)));
        head(l, "Untertitel");
        add(l, Ui.toggle(this, t, "info", "Untertitel (systemweit)", "Captions in Apps anzeigen, wenn verfügbar", () -> Acts.captionsOn(this), v -> Acts.setCaptions(this, v)));
        add(l, Ui.choice(this, t, "info", "Untertitel-Größe", null, Acts.CAP_NAMES, () -> Acts.capIndex(this), i -> Acts.setCap(this, i)));
        open(l, "gear", "Untertitel-Stil (System)", "Farbe, Rand, Schrift", act(Settings.ACTION_CAPTIONING_SETTINGS));
    }

    private void tweaks(LinearLayout l) {
        head(l, "Live");
        Ui.Row ram = info(l, "ram", "Freier Arbeitsspeicher", () -> Status.ramFreeMb(this) + " / " + Status.ramTotalMb(this) + " MB");
        info(l, "temp", "Temperatur CPU / GPU", () -> Acts.fmtTemp(this, Status.cpuTemp()) + " / " + Acts.fmtTemp(this, Status.gpuTemp()));
        info(l, "speed", "CPU-Takt (aktuell / max)", Status::cpuFreq);
        head(l, "Tempo");
        add(l, Ui.choice(this, t, "speed", "Animationen (ganzes System)", "Turbo = Menüs fühlen sich schneller an", Acts.ANIM_NAMES, () -> Acts.animIndex(this), i -> Acts.setAnim(this, i)));
        Ui.Row free = Ui.action(this, t, "ram", "Speicher jetzt freigeben", "Schließt Apps, die gerade nicht laufen", () -> "Start", null);
        free.setOnClickListener(v -> {
            free.value.setText("…");
            L.IO.execute(() -> {
                long mb = Acts.freeMemory(this);
                L.UI.post(() -> { free.value.setText("+" + mb + " MB"); ram.refresh(); });
            });
        });
        add(l, free);
        add(l, Ui.toggle(this, t, "look", "Lumen-Animationen", "Zoom-Effekt beim Auswählen", () -> L.anim(this), v -> L.p(this).edit().putBoolean("anim", v).apply()));
        add(l, Ui.choice(this, t, "photo", "Fotos laden nach Start", "Später = Startseite ist schneller da", new String[]{"0,5 s", "1,5 s", "3 s", "6 s"},
                () -> idx(new int[]{500, 1500, 3000, 6000}, L.p(this).getInt("photoDelay", 1500)),
                i -> L.p(this).edit().putInt("photoDelay", new int[]{500, 1500, 3000, 6000}[i]).apply()));
        head(l, "Experimente");
        add(l, Ui.choice(this, t, "look", "Oberflächen-Größe (Dichte)", "Wirkt nach Neustart. Standard = 240", Acts.DENS_NAMES, () -> Acts.densIndex(this), i -> Acts.setDens(this, i)));
        int[] warnC = {75, 80, 82, 85, 90};
        String[] warnNames = new String[warnC.length];
        for (int wi = 0; wi < warnC.length; wi++) warnNames[wi] = Acts.fmtTemp(this, warnC[wi]);
        add(l, Ui.choice(this, t, "temp", "Temperatur-Warnung ab", "Status-Anzeige wird orange", warnNames,
                () -> idx(warnC, L.p(this).getInt("tempWarn", 82)),
                i -> L.p(this).edit().putInt("tempWarn", warnC[i]).apply()));
        add(l, Ui.choice(this, t, "film", "Kino-Modus Lautstärke", "Wird beim Einschalten gesetzt", new String[]{"30 %", "40 %", "50 %", "60 %", "70 %", "80 %"},
                () -> idx(new int[]{30, 40, 50, 60, 70, 80}, L.p(this).getInt("cinemaVol", 60)),
                i -> L.p(this).edit().putInt("cinemaVol", new int[]{30, 40, 50, 60, 70, 80}[i]).apply()));
        open(l, "flask", "Entwickleroptionen", "Alle Android-Profi-Schalter", act(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
    }

    private void network(LinearLayout l) {
        head(l, "Verbindung");
        info(l, "wifi", "WLAN", () -> Status.wifiDetail(this));
        info(l, "net", "IP-Adresse", () -> Status.ip(this));
        Ui.Row sp = Ui.action(this, t, "speed", "Internet-Geschwindigkeit testen", "Lädt 25 MB von Cloudflare", () -> "Start", null);
        sp.setOnClickListener(v -> speedTest(sp));
        add(l, sp);
        open(l, "net", "Netzwerk-Dashboard", "Live-Datenrate, wer ins Internet funkt, Daten sperren", () -> startActivity(new Intent(this, NetworkActivity.class)));
        open(l, "wifi", "WLAN wählen & verbinden", "Lumen-Netzwerkliste", () -> startActivity(new Intent(this, WifiActivity.class)));
        open(l, "wifi", "WLAN-Einstellungen (System)", null, comp("com.android.tv.settings", "com.android.tv.settings.connectivity.NetworkActivity"));
        head(l, "Privatsphäre");
        add(l, Ui.choice(this, t, "lock", "Werbe- & Tracker-Blocker", "Sperrt Werbung und Tracker für ALLE Apps (Privates DNS)", Acts.DNS_NAMES, () -> Acts.dnsIndex(this), i -> Acts.setDns(this, i)));
        open(l, "lock", "Standort", null, comp("com.android.tv.settings", "com.android.tv.settings.system.LocationActivity"));
        open(l, "lock", "Sicherheit & Einschränkungen", null, comp("com.android.tv.settings", "com.android.tv.settings.system.SecurityActivity"));
        info(l, "remote", "ADB über WLAN (Port 5555)", () -> L.prop("service.adb.tcp.port").equals("5555") || L.prop("persist.adb.tcp.port").equals("5555") ? "An" : "Aus / unbekannt");
    }

    private void sound(LinearLayout l) {
        head(l, "Bluetooth");
        info(l, "bt", "Verbunden", () -> Status.bt(this));
        open(l, "bt", "Bluetooth-Geräte verbinden", "Lumen: koppeln & verbinden", () -> startActivity(new Intent(this, BluetoothActivity.class)));
        open(l, "bt", "Bluetooth-Einstellungen (System)", null, act(Settings.ACTION_BLUETOOTH_SETTINGS));
        head(l, "Ton");
        AudioManager au = (AudioManager) getSystemService(AUDIO_SERVICE);
        int max = au.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        String[] vols = new String[max + 1];
        for (int i = 0; i <= max; i++) vols[i] = (i * 100 / max) + " %";
        add(l, Ui.choice(this, t, "sound", "Lautstärke", "Links / rechts zum Ändern", vols, () -> au.getStreamVolume(AudioManager.STREAM_MUSIC), i -> au.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0)));
        open(l, "sound", "Ton-Einstellungen", "Ausgabe, Format, Effekte", comp("com.android.tv.settings", "com.android.tv.settings.device.displaysound.aw_displaysound.audio.AudioSettingsActivity"));
    }

    private void apps(LinearLayout l) {
        head(l, "Standard");
        open(l, "apps", "Standard-Apps", "Browser, Startbildschirm und mehr festlegen", act(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        open(l, "home", "Startbildschirm-App wählen", null, act(Settings.ACTION_HOME_SETTINGS));
        open(l, "apps", "Alle Apps verwalten", "Stoppen, Speicher leeren, Rechte", comp("com.android.tv.settings", "com.android.tv.settings.device.apps.AppsActivity"));
        open(l, "lock", "Unbekannte Quellen", "Welche App darf APKs installieren", comp("com.android.tv.settings", "com.android.tv.settings.device.apps.specialaccess.ExternalSourcesActivity"));
        head(l, "Speicher");
        info(l, "storage", "Interner Speicher", Status::storage);
        File pd = Backdrop.dir(this);
        Ui.Row pc = Ui.action(this, t, "photo", "Foto-Cache von Lumen leeren", null, () -> String.format(Locale.GERMAN, "%.0f MB", size(pd) / 1e6), null);
        pc.setOnClickListener(v -> {
            File[] fs = pd.listFiles();
            if (fs != null) for (File f : fs) if (!f.getName().equals(Backdrop.currentFile)) f.delete();
            L.p(this).edit().putLong("photoFetch", 0).apply();
            pc.refresh();
        });
        add(l, pc);
        Ui.Row an = Ui.action(this, t, "storage", "Speicher-Analyse", "Größte Apps anzeigen (OK auf eine App = App-Info)", () -> "Start", null);
        an.setOnClickListener(v -> { an.value.setText("…"); L.IO.execute(() -> { List<Object[]> top = topApps(); L.UI.post(() -> showTop(l, an, top)); }); });
        add(l, an);
    }

    private void power(LinearLayout l) {
        head(l, "Schlaf");
        add(l, Ui.choice(this, t, "moon", "Schlaf-Timer", "Beamer geht danach in Standby", Acts.SLEEP_NAMES, () -> Acts.sleepIndex(this), i -> Acts.setSleep(this, Acts.SLEEP_MIN[i])));
        add(l, Ui.toggle(this, t, "film", "Kino-Modus", "Ton-Preset, keine Störungen, kein Bildschirmschoner", () -> Acts.cinema(this), v -> Acts.setCinema(this, v)));
        head(l, "Jetzt");
        open(l, "moon", "Standby jetzt", "Bild aus, Beamer schläft", () -> Acts.standby(this));
        if (Sys.on()) open(l, "power", "Neu starten", "Beamer sofort neu starten", () -> Sys.reboot(this));
        open(l, "power", "Ein/Aus-Menü", "Neustart oder Ausschalten", () -> { if (RemoteService.inst != null) RemoteService.inst.powerMenu(); else L.toast(this, "Tastenassistent ist aus"); });
    }

    private void remote(LinearLayout l) {
        head(l, "Tastenassistent");
        info(l, "remote", "Lumen-Tastenassistent", () -> RemoteService.inst != null ? "Aktiv" : "Aus");
        add(l, Ui.toggle(this, t, "lock", "Assistent aktiv halten", "Die Firmware schaltet ihn beim App-Start ab; Lumen schaltet ihn wieder an",
                () -> L.p(this).getBoolean("helperKeep", true), v -> { L.p(this).edit().putBoolean("helperKeep", v).apply(); if (v) L.ensureHelper(this); }));
        open(l, "gear", "Bedienungshilfen öffnen", "Hier lässt sich der Assistent an/aus schalten", act(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        head(l, "Eigene Tasten");
        info(l, "gear", "Home 2× drücken", () -> "Öffnet dieses Menü (Erweitert)");
        add(l, Ui.toggle(this, t, "gear", Lang.t("Menu key (☰) → Advanced", "Menü-Taste (☰) → Erweitert"), Lang.t("Open this menu instead of the factory menu", "Statt des Werks-Menüs dieses Menü öffnen"), () -> L.p(this).getBoolean("menuToAdvanced", true), v -> L.p(this).edit().putBoolean("menuToAdvanced", v).apply()));
        head(l, "Maus");
        add(l, Ui.choice(this, t, "remote", "Mauszeiger-Tempo", "Schneller fühlt sich oft flüssiger an", Acts.PTR_NAMES, () -> Acts.ptrIndex(this), i -> Acts.setPtr(this, i)));
        head(l, "Gut zu wissen");
        info(l, "remote", "Maus-Taste", () -> "eingebaut in der Fernbedienung");
        info(l, "remote", Lang.t("Hold OK", "OK lang drücken"), () -> Lang.t("Options (favourite, app info)", "Optionen (Favorit, App-Info)"));
        info(l, "remote", "Zurück in Erweitert", () -> "springt zur Liste links");
    }

    private void phone(LinearLayout l) {
        head(l, "Mit dem Handy tippen");
        add(l, Ui.toggle(this, t, "phone", "Handy-Tastatur", "Webseite im WLAN, kein App-Download nötig", () -> L.p(this).getBoolean("phone", true), v -> {
            L.p(this).edit().putBoolean("phone", v).apply();
            if (v) PhoneServer.start(this); else stopService(new Intent(this, PhoneServer.class));
        }));
        info(l, "net", "Am Handy öffnen", () -> "http://" + Status.ip(this) + ":" + PhoneServer.PORT);
        info(l, "lock", "PIN", () -> PhoneServer.pin(this));
        Ui.Row np = Ui.action(this, t, "lock", "Neue PIN erzeugen", null, () -> "›", null);
        np.setOnClickListener(v -> { L.p(this).edit().remove("phonePin").apply(); shown = -1; show(7); });
        add(l, np);
        // Live QR of the current address — regenerated every time this section opens, so a changed IP is always right.
        String ip = Status.ip(this);
        if (!ip.equals("-")) {
            android.graphics.Bitmap q = Qr.bitmap("http://" + ip + ":" + PhoneServer.PORT + "/", L.s(6), 4, 0xFF111417, 0xFFF2EDE4);
            if (q != null) {
                ImageView qv = new ImageView(this);
                qv.setImageBitmap(q);
                int pad = L.s(10);
                qv.setBackground(Ui.round(0xFFF2EDE4, L.s(12)));
                qv.setPadding(pad, pad, pad, pad);
                LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
                qp.topMargin = L.s(16);
                qp.leftMargin = L.s(8);
                l.addView(qv, qp);
                TextView qh = Ui.one(this, Lang.t("Point your phone camera at this", "Handy-Kamera draufhalten"), 14, t.body(), t.dim);
                qh.setPadding(L.s(10), L.s(8), 0, 0);
                l.addView(qh);
            }
        }
        TextView tip = Ui.tv(this, "So geht's: Handy im selben WLAN, Adresse oben im Browser öffnen, PIN eingeben. Dann einfach tippen: der Text landet im gerade ausgewählten Eingabefeld (Suche, Passwort, Browser). Dazu gibt es Tasten für Zurück, Home, Play/Pause und Lautstärke.", 15, t.body(), t.dim);
        tip.setPadding(L.s(8), L.s(14), L.s(8), 0);
        tip.setShadowLayer(L.s(6), 0, L.s(1), 0x88000000);
        l.addView(tip);
    }

    private void look(LinearLayout l) {
        head(l, "Sprache / Language");
        add(l, Ui.choice(this, t, "net", "Sprache / Language", "Automatisch folgt der Android-Sprache", Lang.MODE_NAMES,
                () -> Lang.modeIndex(this), i -> { L.p(this).edit().putString("lang", Lang.MODE_VALS[i]).apply(); recreate(); }));
        head(l, Lang.t("Units", "Einheiten"));
        add(l, Ui.choice(this, t, "temp", Lang.t("Temperature", "Temperatur"), Lang.t("Unit in the status bar", "Einheit in der Statusleiste"), Acts.TEMP_NAMES,
                () -> Acts.tempUnit(this), i -> L.p(this).edit().putInt("tempUnit", i).apply()));
        add(l, Ui.choice(this, t, "speed", Lang.t("Speed", "Geschwindigkeit"), Lang.t("Network speed: bits or bytes", "Netzwerk-Tempo: Bit oder Byte"), Acts.SPEED_NAMES,
                () -> L.p(this).getInt("speedUnit", 0), i -> L.p(this).edit().putInt("speedUnit", i).apply()));
        head(l, Lang.t("Readability", "Lesbarkeit"));
        add(l, Ui.toggle(this, t, "info", Lang.t("Bold text (Lumen)", "Fette Schrift (Lumen)"), Lang.t("Thicker text everywhere in Lumen", "Dickere Schrift überall in Lumen"),
                () -> L.p(this).getBoolean("boldText", false), v -> { L.p(this).edit().putBoolean("boldText", v).apply(); recreate(); }));
        add(l, Ui.toggle(this, t, "info", Lang.t("High contrast (system)", "Hoher Kontrast (System)"), Lang.t("Outlines text in apps for weak displays", "Umrandet Text in Apps für schwache Displays"),
                () -> Acts.highContrast(this), v -> Acts.setHighContrast(this, v)));
        add(l, Ui.choice(this, t, "size", Lang.t("UI size", "Oberflächen-Größe"), Lang.t("Bigger menus & text (after restart)", "Größere Menüs & Schrift (nach Neustart)"), Acts.DENS_NAMES, () -> Acts.densIndex(this), i -> Acts.setDens(this, i)));
        head(l, Lang.t("Appearance", "Aussehen"));
        open(l, "palette", "Themes & Mischen", "10 Looks, dazu Schrift, Glas, Ecken, Akzent frei mischen", () -> startActivity(new Intent(this, ThemeActivity.class)));
        head(l, "Hintergrund-Fotos");
        add(l, Ui.choice(this, t, "photo", "Neues Foto alle", null, new String[]{"1 min", "3 min", "5 min", "10 min", "20 min", "60 min"},
                () -> idx(new int[]{1, 3, 5, 10, 20, 60}, L.p(this).getInt("photoMin", 5)),
                i -> L.p(this).edit().putInt("photoMin", new int[]{1, 3, 5, 10, 20, 60}[i]).apply()));
        add(l, Ui.toggle(this, t, "photo", "Fotos in Full-HD", "Schärfer, braucht etwas mehr Speicher", () -> L.p(this).getBoolean("photoHd", false), v -> L.p(this).edit().putBoolean("photoHd", v).putLong("photoFetch", 0).apply()));
        open(l, "photo", "Nächstes Foto jetzt", null, () -> Backdrop.next(this));
        info(l, "photo", "Fotos im Speicher", () -> String.valueOf(Backdrop.cached(this).length));
        head(l, "Hintergrund-Quelle");
        String[] srcNames = {"Soulride", "Web-Fotos (Natur)", "Eigene (USB)"};
        String[] srcVals = {"soulride", "web", "custom"};
        add(l, Ui.choice(this, t, "photo", "Hintergründe", "Soulride = deine Bilder · Web = Natur aus dem Netz · Eigene = dein USB-Ordner", srcNames,
                () -> { String s = L.p(this).getString("bgSource", "soulride"); for (int i = 0; i < srcVals.length; i++) if (srcVals[i].equals(s)) return i; return 0; },
                i -> { L.p(this).edit().putString("bgSource", srcVals[i]).apply(); Backdrop.next(this); }));
        TextView ct = Ui.tv(this, "Eigene (USB): kopiere Bilder in den Ordner Pictures/Lumen/custom auf dem Beamer (per USB-Stick + Dateien, oder am PC). JPG oder PNG.", 14, t.body(), t.dim);
        ct.setPadding(L.s(8), L.s(10), L.s(8), 0);
        l.addView(ct);
        head(l, "Bildschirmschoner & Wetter");
        add(l, Ui.toggle(this, t, "moon", "Lumen-Bildschirmschoner", "Foto + Uhr statt Werks-Schoner", () -> Acts.lumenDreamOn(this), v -> { if (v) Acts.useLumenDream(this); else { try { android.provider.Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 0); } catch (Exception ignored) {} } }));
        add(l, Ui.toggle(this, t, "cloud", Lang.t("Weather in the status bar", "Wetter in der Statusleiste"), Lang.t("Show the weather at the top of the home screen", "Wetter oben am Startbildschirm zeigen"), () -> L.p(this).getBoolean("weatherOn", true), v -> { L.p(this).edit().putBoolean("weatherOn", v).apply(); if (v) Weather.refresh(this); }));
        add(l, Ui.toggle(this, t, "photo", Lang.t("App previews in Recent", "App-Vorschau in „Zuletzt“"), Lang.t("Show each recent app's last screen instead of its icon", "Letztes Bild der App statt des Symbols zeigen"), () -> L.p(this).getBoolean("thumbs", true), v -> { L.p(this).edit().putBoolean("thumbs", v).apply(); if (!v) RemoteService.disableThumbs(this); }));
        Ui.Row wx = Ui.action(this, t, "cloud", "Wetter-Ort", "Leer = automatisch per Internet", () -> L.p(this).getString("wxSetCity", Lang.t("automatic", "automatisch")), null);
        wx.setOnClickListener(v -> Keyboard.ask(this, t, "Stadt (leer = automatisch)", L.p(this).getString("wxSetCity", ""), p -> { L.p(this).edit().putString("wxSetCity", p == null ? "" : p.trim()).putLong("wxAt", 0).apply(); Weather.refresh(this); wx.refresh(); }));
        add(l, wx);
    }

    private void hidden(LinearLayout l) {
        head(l, "Direkt");
        open(l, "flask", "Entwickleroptionen", null, act(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        open(l, "gear", "Android-Einstellungen (komplett)", null, act(Settings.ACTION_SETTINGS));
        open(l, "gear", "Allwinner-Manager", "Versteckte Chip-Einstellungen", comp("com.softwinner.awmanager", "com.softwinner.awmanager.AwManager"));
        open(l, "info", "Geräte-Status", "Seriennummer, Laufzeit, Adressen", comp("com.android.tv.settings", "com.android.tv.settings.about.StatusActivity"));
        open(l, "look", "Bedienungshilfen", null, act(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        open(l, "clock", "Datum & Uhrzeit", null, comp("com.android.tv.settings", "com.android.tv.settings.system.DateTimeActivity"));
        open(l, "net", "Sprache", null, comp("com.android.tv.settings", "com.android.tv.settings.system.LanguageActivity"));
        open(l, "remote", "Tastatur", null, comp("com.android.tv.settings", "com.android.tv.settings.inputmethod.KeyboardActivity"));
        head(l, "Alle Einstellungs-Seiten (Experte)");
        PackageManager pm = getPackageManager();
        for (String pkg : new String[]{"com.android.tv.settings", "com.htc.htcsettings", "com.softwinner.awmanager", "com.softwinner.awlogsettings", "com.softwinner.settingsassist", "com.softwinner.vis"}) {
            try {
                PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
                if (pi.activities == null) continue;
                for (ActivityInfo ai : pi.activities) {
                    if (!ai.exported || ai.permission != null) continue;
                    String nm = ai.name.substring(ai.name.lastIndexOf('.') + 1).replace("Activity", "");
                    if (nm.isEmpty()) continue;
                    String lbl = ai.loadLabel(pm).toString();
                    open(l, "eye", nm, lbl + "  ·  " + pkg, () -> {
                        Intent i = new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(ai.packageName, ai.name));
                        L.start(this, i);
                    });
                }
            } catch (Exception ignored) {}
        }
    }

    private void backup(LinearLayout l) {
        head(l, "Sichern");
        TextView tip = Ui.tv(this, "Speichert deine Favoriten, das Theme und alle Lumen-Einstellungen in eine Datei. Praktisch vor einem Update (ein Neuinstallieren setzt Lumen sonst zurück).", 15, t.body(), t.dim);
        tip.setPadding(L.s(8), 0, L.s(8), L.s(6));
        l.addView(tip);
        File internal = new File(Environment.getExternalStorageDirectory(), "Lumen");
        open(l, "storage", "In internen Speicher sichern", "/Lumen/lumen-backup.json", () -> {
            boolean ok = Acts.export(this, Acts.backupFile(this, internal));
            L.toast(this, ok ? "Gesichert nach /Lumen/" : "Fehlgeschlagen");
        });
        for (File usb : usbList()) {
            open(l, "usb", "Auf USB sichern: " + usb.getName(), usb.getPath(), () -> {
                boolean ok = Acts.export(this, Acts.backupFile(this, usb));
                L.toast(this, ok ? "Auf USB gesichert" : "Fehlgeschlagen");
            });
        }
        head(l, "Wiederherstellen");
        List<File> found = new ArrayList<>();
        File fi = Acts.backupFile(this, internal);
        if (fi.exists()) found.add(fi);
        for (File usb : usbList()) { File f = Acts.backupFile(this, usb); if (f.exists()) found.add(f); }
        if (found.isEmpty()) { TextView n = Ui.one(this, "Keine Sicherung gefunden", 15, t.body(), t.dim); n.setPadding(L.s(8), L.s(8), 0, 0); l.addView(n); }
        for (File f : found) {
            open(l, "storage", "Wiederherstellen", f.getPath(), () -> menu("Wiederherstellen?", new String[]{"Ja, Einstellungen laden", "Abbrechen"}, i -> {
                if (i != 0) return;
                if (Acts.importFrom(this, f)) { L.toast(this, "Geladen – wird angewendet"); recreate(); } else L.toast(this, "Fehlgeschlagen");
            }));
        }
    }

    private List<File> usbList() {
        List<File> out = new ArrayList<>();
        File[] fs = new File("/storage").listFiles();
        if (fs != null) for (File f : fs) {
            String n = f.getName();
            if (!n.equals("emulated") && !n.equals("self") && !n.equals("enc_emulated") && f.isDirectory() && f.canWrite()) out.add(f);
        }
        return out;
    }

    private void info(LinearLayout l) {
        head(l, "Datenschutz & Info");
        open(l, "lock", "Über Lumen & Datenschutz", "Was Lumen nutzt · keine Datensammlung · kein Server", () -> startActivity(new Intent(this, AboutActivity.class)));
        head(l, "Lumen & Updates");
        info(l, "info", "Lumen-Version", () -> "v" + L.VERSION + "  ·  by Soulride");
        Ui.Row up = Ui.action(this, t, "net", "Nach Updates suchen", Lang.t("Newest version from GitHub", "Neueste Version von GitHub"), () -> "›", null);
        up.setOnClickListener(v -> { up.value.setText("…"); Updater.check(this, true, msg -> up.value.setText(msg)); });
        add(l, up);
        Ui.Row uu = Ui.action(this, t, "gear", "Update-Adresse", Lang.t("Optional: your own version.json instead of GitHub", "Optional: eigene version.json statt GitHub"), () -> Updater.url(this).isEmpty() ? "nicht gesetzt" : "gesetzt", null);
        uu.setOnClickListener(v -> Keyboard.ask(this, t, "Update-URL (JSON)", Updater.url(this), p -> { L.p(this).edit().putString("updateUrl", p == null ? "" : p.trim()).apply(); uu.refresh(); }));
        add(l, uu);
        head(l, "Gerät");
        info(l, "info", "Modell", () -> "HY300 (" + Lang.t("reports as ", "meldet sich als ") + Build.MODEL + ")");
        info(l, "info", "Firmware", () -> Build.DISPLAY);
        info(l, "info", "Android", () -> Build.VERSION.RELEASE + Lang.t("  ·  Security patch ", "  ·  Sicherheits-Patch ") + Build.VERSION.SECURITY_PATCH);
        info(l, "lock", "Lumen-Systemrechte", () -> Sys.on() ? "Aktiv (Live-Zoom, Neustart)" : "Normale App");
        info(l, "info", "Chip", () -> "Allwinner " + Build.HARDWARE + " / " + L.prop("ro.board.platform"));
        info(l, "clock", "Läuft seit", Status::uptime);
        head(l, "Leistung");
        info(l, "temp", "CPU / GPU", () -> Status.cpuTemp() + " °C / " + Status.gpuTemp() + " °C");
        info(l, "ram", "Arbeitsspeicher", () -> Status.ramFreeMb(this) + Lang.t(" MB free of ", " MB frei von ") + Status.ramTotalMb(this) + " MB");
        info(l, "storage", "Speicher", Status::storage);
        head(l, "Netz");
        info(l, "wifi", "WLAN", () -> Status.wifiDetail(this));
        info(l, "net", "IP", () -> Status.ip(this));
        info(l, "bt", "Bluetooth", () -> Status.bt(this));
    }

    // ------------------------------------------------------------------ jobs
    private void speedTest(Ui.Row row) {
        row.value.setText(Lang.t("Running …", "Läuft …"));
        L.IO.execute(() -> {
            String res;
            try {
                long t0 = System.nanoTime(), bytes = 0;
                HttpURLConnection h = (HttpURLConnection) new URL("https://speed.cloudflare.com/__down?bytes=25000000").openConnection();
                h.setConnectTimeout(8000);
                h.setReadTimeout(20000);
                try (InputStream in = h.getInputStream()) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) { bytes += n; if (System.nanoTime() - t0 > 15_000_000_000L) break; }
                }
                double sec = (System.nanoTime() - t0) / 1e9;
                res = Acts.fmtLink(this, (int) Math.round(bytes * 8 / sec / 1e6));
            } catch (Exception e) { res = Lang.t("Error", "Fehler"); }
            final String r = res;
            L.UI.post(() -> row.value.setText(r));
        });
    }

    private List<Object[]> topApps() {
        List<Object[]> out = new ArrayList<>();
        try {
            StorageStatsManager ssm = (StorageStatsManager) getSystemService(STORAGE_STATS_SERVICE);
            PackageManager pm = getPackageManager();
            for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
                try {
                    StorageStats st = ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, ai.packageName, Process.myUserHandle());
                    long total = st.getAppBytes() + st.getDataBytes();
                    out.add(new Object[]{ai.packageName, pm.getApplicationLabel(ai).toString(), total, st.getCacheBytes()});
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        out.sort((a, b) -> Long.compare((Long) b[2], (Long) a[2]));
        return out.size() > 12 ? out.subList(0, 12) : out;
    }

    private void showTop(LinearLayout l, Ui.Row anchor, List<Object[]> top) {
        anchor.value.setText(top.isEmpty() ? Lang.t("No permission", "Keine Berechtigung") : Lang.t("done", "fertig"));
        int at = l.indexOfChild(anchor) + 1;
        for (Object[] o : top) {
            String pkg = (String) o[0];
            Ui.Row r = Ui.action(this, t, "apps", (String) o[1], String.format(Locale.GERMAN, "davon Cache %.0f MB", (Long) o[3] / 1e6),
                    () -> String.format(Locale.GERMAN, "%.0f MB", (Long) o[2] / 1e6),
                    () -> L.start(this, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
            lp.topMargin = L.s(7);
            lp.leftMargin = L.s(28);
            l.addView(r, at++, lp);
        }
    }

    private static long size(File d) {
        long s = 0;
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) s += f.length();
        return s;
    }

    private static int idx(int[] vals, int v) {
        for (int i = 0; i < vals.length; i++) if (vals[i] == v) return i;
        return 0;
    }
}
