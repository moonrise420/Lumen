package de.lumen.home;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends Base {
    static final Map<String, Drawable> BANNERS = new HashMap<>();
    static final Map<String, Drawable> ICONS = new HashMap<>();
    static final Map<String, String> NAMES = new HashMap<>();

    private TextView clock, date, hint, hintSub, caption;
    private TextView wifiTx, btTx, netTx, ramTx, tempTx, sleepTx, wxTx;
    private View wxPill;
    private long nRx, nTx, nT;
    private View sleepPill;
    private ScrollView favV;
    private LinearLayout favVBox;
    private List<Fav> cats;
    private TextView cinemaLabel;
    private boolean firstPhotoDone;

    private final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.GERMAN);

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateClock(); }
    };

    private final Runnable statusLoop = new Runnable() {
        @Override public void run() { updateStatus(); L.UI.postDelayed(this, 5000); }
    };

    private final Runnable photoLoop = new Runnable() {
        @Override public void run() {
            Backdrop.next(HomeActivity.this);
            L.UI.postDelayed(this, L.p(HomeActivity.this).getInt("photoMin", 5) * 60_000L);
        }
    };

    @Override void build() {
        // ---- top-left: LUMEN by Soulride  /  clock  /  date ----
        LinearLayout tl = new LinearLayout(this);
        tl.setOrientation(LinearLayout.VERTICAL);
        tl.setClipChildren(false);
        tl.setClipToPadding(false);
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.BOTTOM);
        nameRow.setClipChildren(false);
        nameRow.setClipToPadding(false);
        TextView name = Ui.tv(this, "LUMEN", 22, L.geo(this), t.text);
        name.setLetterSpacing(0.14f);
        name.setIncludeFontPadding(true); // keep full glyph height so the E's top/bottom bars aren't clipped
        name.setPadding(0, L.s(10), L.s(8), L.s(4)); // top room for caps, small right gap to "by Soulride"
        name.setShadowLayer(L.s(8), 0, L.s(1), 0x77000000);
        nameRow.addView(name);
        TextView by = Ui.tv(this, "by Soulride", 15, L.geo(this), t.text);
        by.setLetterSpacing(0.04f);
        by.setAlpha(0.82f);
        by.getPaint().setFakeBoldText(true); // slightly fatter
        by.setEllipsize(null);
        by.setPadding(0, 0, L.s(18), 0); // room so the trailing "e" never clips
        by.setShadowLayer(L.s(6), 0, L.s(1), 0x77000000);
        LinearLayout.LayoutParams byp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        byp.topMargin = L.s(12); // sit lower, on LUMEN's baseline
        nameRow.addView(by, byp);
        nameRow.setGravity(Gravity.BOTTOM);
        tl.addView(nameRow);
        clock = Ui.one(this, "", 72, L.geo(this), t.text);
        clock.setLetterSpacing(0.02f);
        clock.setEllipsize(null);
        clock.setSingleLine(true);
        clock.setMinWidth(L.s(340));
        clock.setPadding(0, 0, L.s(40), 0);
        clock.setShadowLayer(L.s(14), 0, L.s(2), 0x77000000);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        clp.topMargin = L.s(6);
        tl.addView(clock, clp);
        date = Ui.one(this, "", 18, t.bold(), t.text);
        date.setShadowLayer(L.s(8), 0, L.s(1), 0x77000000);
        // date line + the navigation hint beside it (dim), instead of down at the bottom over the tiles
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.BOTTOM);
        dateRow.addView(date);
        hintSub = Ui.one(this, Lang.t("OK to open  ·  hold OK / Menu: move, category, remove", "OK öffnen  ·  lange OK / Menü: verschieben, Kategorie, entfernen"), 12, t.body(), t.dim);
        hintSub.setAlpha(0.75f);
        hintSub.setShadowLayer(L.s(6), 0, L.s(1), 0x99000000);
        LinearLayout.LayoutParams hsp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        hsp.leftMargin = L.s(24);
        hsp.bottomMargin = L.s(1);
        dateRow.addView(hintSub, hsp);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        dp.topMargin = L.s(2);
        tl.addView(dateRow, dp);
        root.addView(tl, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 56, 30));

        // ---- status pills (weather · Wi-Fi [tap→picker] · net speed · Bluetooth [tap→on/off, hold→picker]) ----
        LinearLayout pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        wifiTx = Ui.one(this, "", 15, t.bold(), t.text);
        btTx = Ui.one(this, "", 15, t.bold(), t.text);
        netTx = Ui.one(this, "", 15, t.bold(), t.text);
        ramTx = Ui.one(this, "", 15, t.bold(), t.text);
        tempTx = Ui.one(this, "", 15, t.bold(), t.text);
        sleepTx = Ui.one(this, "", 15, t.bold(), t.text);
        wxTx = Ui.one(this, "", 15, t.bold(), t.text);
        sleepPill = Ui.pill(this, t, "moon", sleepTx);
        wxPill = Ui.pill(this, t, "cloud", wxTx);
        LinearLayout wifiPill = Ui.pill(this, t, "wifi", wifiTx);
        Ui.focus(wifiPill, t, 1.06f, L.s(999), null);
        wifiPill.setOnClickListener(v -> startActivity(new Intent(this, WifiActivity.class)));
        LinearLayout netPill = Ui.pill(this, t, "speed", netTx);
        netPill.setOnClickListener(v -> startActivity(new Intent(this, NetworkActivity.class)));
        Ui.focus(netPill, t, 1.06f, L.s(999), null);
        LinearLayout btPill = Ui.pill(this, t, "bt", btTx);
        Ui.focus(btPill, t, 1.06f, L.s(999), null);
        btPill.setOnClickListener(v -> {
            android.bluetooth.BluetoothAdapter a = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (a != null) { if (a.isEnabled()) a.disable(); else a.enable(); }
            L.UI.postDelayed(this::updateStatus, 800);
        });
        btPill.setOnLongClickListener(v -> { startActivity(new Intent(this, BluetoothActivity.class)); return true; });
        View[] ps = {sleepPill, wxPill, wifiPill, netPill, btPill, Ui.pill(this, t, "ram", ramTx), Ui.pill(this, t, "temp", tempTx)};
        for (int i = 0; i < ps.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            if (i > 0) lp.leftMargin = L.s(9);
            pills.addView(ps[i], lp);
        }
        root.addView(pills, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.RIGHT, 48, 18));
        nRx = android.net.TrafficStats.getTotalRxBytes();
        nTx = android.net.TrafficStats.getTotalTxBytes();
        nT = System.currentTimeMillis();

        caption = Ui.one(this, "", 11, t.body(), t.dim); // photo credit (own photos: empty)

        // ---- QR + "Explore Soulride", top-right on the clock's row ----
        LinearLayout qc = new LinearLayout(this);
        qc.setOrientation(LinearLayout.VERTICAL);
        qc.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView qr = new ImageView(this);
        qr.setImageBitmap(L.asset(this, "qr_soulride"));
        qr.setBackground(Ui.round(0xFFF2EDE4, L.s(8)));
        qr.setPadding(L.s(5), L.s(5), L.s(5), L.s(5));
        qc.addView(qr, new LinearLayout.LayoutParams(L.s(104), L.s(104)));
        TextView link = Ui.one(this, "Explore Soulride", 13, L.geo(this), t.text);
        link.setLetterSpacing(0.08f);
        link.setEllipsize(null);
        link.setPadding(L.s(10), 0, L.s(10), 0); // room so the trailing "e" never clips
        link.getPaint().setFakeBoldText(true); // fatter for readability on the projector
        link.setShadowLayer(L.s(5), 0, L.s(1), 0x99000000);
        LinearLayout.LayoutParams lkP = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        lkP.topMargin = L.s(9);
        qc.addView(link, lkP);
        root.addView(qc, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.RIGHT, 44, 66)); // right edge ~aligned with the status pills

        // ---- favourites, grouped into categories (vertical scroll of rows) ----
        favV = new ScrollView(this);
        favV.setVerticalScrollBarEnabled(false);
        favV.setClipChildren(false);
        favV.setClipToPadding(false);
        favV.setOnScrollChangeListener((v, x, y, ox, oy) -> invalidateDeep(favVBox));
        favVBox = new LinearLayout(this);
        favVBox.setOrientation(LinearLayout.VERTICAL);
        favVBox.setClipChildren(false);
        favV.addView(favVBox);
        root.addView(favV, Ui.at(L.s(1190), L.s(404), Gravity.TOP | Gravity.LEFT, 0, 222)); // lower, so scrolling doesn't bleed into the clock/date


        // ---- bottom chips ----
        HorizontalScrollView cs = new HorizontalScrollView(this);
        cs.setHorizontalScrollBarEnabled(false);
        cs.setClipChildren(false);
        cs.setClipToPadding(false);
        cs.setPadding(L.s(56), L.s(12), L.s(56), L.s(12));
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setClipChildren(false);
        LinearLayout cinema = Ui.chip(this, t, "film", Lang.t("Cinema", "Kino"), v -> { Acts.setCinema(this, !Acts.cinema(this)); updateCinema(); });
        cinemaLabel = (TextView) cinema.findViewWithTag("label");
        View[] cv = {
                Ui.chip(this, t, "hdmi", "HDMI", v -> Acts.hdmi(this)),
                Ui.chip(this, t, "usb", "USB", v -> Acts.usb(this)),
                Ui.chip(this, t, "storage", Lang.t("Files", "Dateien"), v -> startActivity(new Intent(this, FilesActivity.class))),
                Ui.chip(this, t, "cast", Lang.t("Cast", "Spiegeln"), v -> menu(Lang.t("Screen mirroring (Miracast)", "Handy spiegeln (Miracast)"),
                        new String[]{Lang.t("Start – Wi-Fi pauses until you press Back", "Starten – WLAN pausiert, bis du mit Zurück beendest"), Lang.t("Cancel", "Abbrechen")}, i -> { if (i == 0) Acts.mirror(this); })),
                Ui.chip(this, t, "apps", Lang.t("All apps", "Alle Apps"), v -> startActivity(new Intent(this, AppsActivity.class))),
                Ui.chip(this, t, "size", Lang.t("Picture size", "Bildgröße"), v -> startActivity(new Intent(this, ScreenSizeActivity.class))),
                cinema,
                Ui.chip(this, t, "film", Lang.t("Mode", "Modus"), v -> menu(Lang.t("Choose mode", "Modus wählen"), Acts.sceneNames(), i -> Acts.applyScene(this, i))),
                Ui.chip(this, t, "ram", Lang.t("Clean up", "Aufräumen"), v -> {
                    L.toast(this, Lang.t("Closing background apps …", "Schließe Hintergrund-Apps …"));
                    L.IO.execute(() -> { long mb = Acts.freeMemory(this); L.UI.post(() -> { L.toast(this, mb > 0 ? "+" + mb + Lang.t(" MB free", " MB frei") : Lang.t("Cleaned up", "Aufgeräumt")); updateStatus(); }); });
                }),
                Ui.chip(this, t, "moon", Lang.t("Sleep", "Schlaf"), v -> sleepMenu()),
                Ui.chip(this, t, "palette", "Themes", v -> startActivity(new Intent(this, ThemeActivity.class))),
                Ui.chip(this, t, "gear", Lang.t("Advanced", "Erweitert"), v -> startActivity(new Intent(this, AdvancedActivity.class))),
                Ui.chip(this, t, "help", Lang.t("Help", "Hilfe"), v -> startActivity(new Intent(this, HelpActivity.class))),
        };
        for (int i = 0; i < cv.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            if (i > 0) lp.leftMargin = L.s(10);
            chips.addView(cv[i], lp);
        }
        cs.addView(chips);
        root.addView(cs, Ui.at(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM | Gravity.LEFT, 0, 26));

        buildNowBar();

        buildFavs();
        updateClock();
        updateStatus();
        updateCinema();

        // Photos only after the first frame is on screen, so start-up stays instant.
        root.post(() -> L.UI.postDelayed(() -> {
            firstPhotoDone = true;
            L.UI.post(photoLoop);
            L.IO.execute(() -> Backdrop.fetch(getApplicationContext()));
        }, L.p(this).getInt("photoDelay", 1500)));

        Backdrop.watch(caption);
        L.watchHelper(this);
        if (Sys.on() && !L.p(this).getBoolean("dreamSet", false)) { Acts.useLumenDream(this); L.p(this).edit().putBoolean("dreamSet", true).apply(); }
        if (L.p(this).getBoolean("phone", true)) L.UI.postDelayed(() -> PhoneServer.start(this), 4000);
    }

    private static void invalidateDeep(android.view.ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            c.invalidate();
            if (c instanceof android.view.ViewGroup) invalidateDeep((android.view.ViewGroup) c);
        }
    }

    // ------------------------------------------------------------------ favourites (categories)
    void buildFavs() { buildFavs(0, 0); }

    /** Rebuild all category rows; then focus category `fc`, tile `ft`. */
    void buildFavs(int fc, int ft) {
        cats = Fav.load(this);
        favVBox.removeAllViews();

        // ---- "Zuletzt" row: the apps you most recently used, as larger tiles ----
        List<String> recent = recentApps(3);
        if (!recent.isEmpty()) {
            TextView rh = Ui.one(this, Lang.t("RECENT", "ZULETZT"), 15, L.geo(this), t.accent);
            rh.setLetterSpacing(0.22f);
            rh.setEllipsize(null);
            rh.setShadowLayer(L.s(5), 0, L.s(1), 0x88000000);
            rh.setPadding(L.s(58), L.s(4), 0, L.s(8));
            favVBox.addView(rh);
            HorizontalScrollView rhs = new HorizontalScrollView(this);
            rhs.setHorizontalScrollBarEnabled(false);
            rhs.setClipChildren(false);
            rhs.setClipToPadding(false);
            rhs.setPadding(L.s(56), L.s(6), L.s(56), L.s(6));
            LinearLayout rrow = new LinearLayout(this);
            rrow.setOrientation(LinearLayout.HORIZONTAL);
            rrow.setClipChildren(false);
            for (String pkg : recent) rrow.addView(recentTile(pkg, rrow.getChildCount() > 0));
            rhs.addView(rrow);
            favVBox.addView(rhs);
            favTop = 2;
        } else favTop = 0;

        for (int ci = 0; ci < cats.size(); ci++) {
            Fav cat = cats.get(ci);
            final int cIdx = ci;
            TextView hdr = Ui.one(this, (Lang.EN ? Tr.t(cat.name) : cat.name).toUpperCase(), 15, L.geo(this), t.text);
            hdr.setLetterSpacing(0.22f);
            hdr.setEllipsize(null);
            hdr.setShadowLayer(L.s(5), 0, L.s(1), 0x88000000);
            hdr.setPadding(L.s(58), ci == 0 ? L.s(4) : L.s(18), 0, L.s(8));
            favVBox.addView(hdr);

            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            hs.setClipChildren(false);
            hs.setClipToPadding(false);
            hs.setPadding(L.s(56), L.s(6), L.s(56), L.s(6));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setClipChildren(false);
            hs.addView(row);
            for (String pkg : cat.pkgs) row.addView(tile(cIdx, pkg, row.getChildCount() > 0));
            row.addView(addTile(cIdx, row.getChildCount() > 0));
            favVBox.addView(hs);
        }
        favVBox.post(() -> {
            HorizontalScrollView hs;
            if (fc < 0) hs = favTop > 0 ? (HorizontalScrollView) favVBox.getChildAt(1) : rowOf(0); // recent row, else cat 0
            else hs = rowOf(fc);
            if (hs != null) {
                LinearLayout row = (LinearLayout) hs.getChildAt(0);
                View v = row.getChildAt(Math.max(0, Math.min(ft, row.getChildCount() - 1)));
                if (v != null) v.requestFocus();
            }
        });
    }

    private int favTop = 0; // views before the categories (2 when the "Zuletzt" row is shown)

    private HorizontalScrollView rowOf(int cat) {
        int hsIndex = favTop + cat * 2 + 1; // [recent hdr, recent hs,] hdr, hs, hdr, hs, ...
        if (hsIndex < favVBox.getChildCount()) return (HorizontalScrollView) favVBox.getChildAt(hsIndex);
        return null;
    }

    // ---- recently used apps ----
    private List<String> recentApps(int max) {
        List<String> order = new ArrayList<>();
        try {
            UsageStatsManager um = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long end = System.currentTimeMillis();
            UsageEvents ev = um.queryEvents(end - 7L * 24 * 3600_000L, end);
            UsageEvents.Event e = new UsageEvents.Event();
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e);
                if (e.getEventType() != UsageEvents.Event.ACTIVITY_RESUMED) continue;
                String p = e.getPackageName();
                if (p.equals(getPackageName()) || L.launchIntent(this, p) == null) continue;
                order.add(p);
            }
        } catch (Exception ignored) {}
        java.util.LinkedHashSet<String> recent = new java.util.LinkedHashSet<>();
        for (int i = order.size() - 1; i >= 0; i--) recent.add(order.get(i));
        List<String> out = new ArrayList<>(recent);
        return out.size() > max ? out.subList(0, max) : out;
    }

    private FrameLayout recentTile(String pkg, boolean margin) {
        FrameLayout tile = new FrameLayout(this);
        float r = L.s(t.radius);
        Ui.glass(tile, t);
        Ui.clip(tile, r);
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(img, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        LinearLayout fb = new LinearLayout(this);
        fb.setOrientation(LinearLayout.VERTICAL);
        fb.setGravity(Gravity.CENTER);
        ImageView ic = new ImageView(this);
        fb.addView(ic, new LinearLayout.LayoutParams(L.s(60), L.s(60)));
        TextView nm = Ui.one(this, "", 15, t.bold(), t.text);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        np.topMargin = L.s(8);
        fb.addView(nm, np);
        tile.addView(fb, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        Ui.focus(tile, t, 1.07f, r, null);
        tile.setOnClickListener(v -> L.launch(this, pkg));
        tile.setOnLongClickListener(v -> { L.start(this, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))); return true; });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(L.s(240), L.s(135));
        if (margin) lp.leftMargin = L.s(16);
        tile.setLayoutParams(lp);
        // Prefer the app's own last frame (captured by the accessibility helper); fall back to banner/icon.
        L.IO.execute(() -> {
            android.graphics.Bitmap thumb = Thumbs.load(this, pkg);
            if (thumb != null) L.UI.post(() -> { img.setImageBitmap(thumb); fb.setVisibility(View.GONE); });
            else loadArt(pkg, img, ic, nm, fb);
        });
        return tile;
    }

    private FrameLayout addTile(int cat, boolean margin) {
        FrameLayout add = new FrameLayout(this);
        float r = L.s(t.radius);
        Ui.glass(add, t);
        add.addView(Ui.icon(this, "plus", t.text, 38), new FrameLayout.LayoutParams(L.s(38), L.s(38), Gravity.CENTER));
        Ui.focus(add, t, 1.07f, r, null);
        add.setOnClickListener(v -> startActivity(new Intent(this, AppsActivity.class).putExtra("pick", true).putExtra("cat", cat)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(L.s(120), L.s(116));
        if (margin) lp.leftMargin = L.s(16);
        add.setLayoutParams(lp);
        return add;
    }

    private FrameLayout tile(int cat, String pkg, boolean margin) {
        FrameLayout tile = new FrameLayout(this);
        float r = L.s(t.radius);
        Ui.glass(tile, t);
        Ui.clip(tile, r);
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(img, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        LinearLayout fallback = new LinearLayout(this);
        fallback.setOrientation(LinearLayout.VERTICAL);
        fallback.setGravity(Gravity.CENTER);
        ImageView ic = new ImageView(this);
        fallback.addView(ic, new LinearLayout.LayoutParams(L.s(48), L.s(48)));
        TextView nm = Ui.one(this, "", 14, t.bold(), t.text);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        np.topMargin = L.s(6);
        fallback.addView(nm, np);
        tile.addView(fallback, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        Ui.focus(tile, t, 1.08f, r, null);
        tile.setOnClickListener(v -> L.launch(this, pkg));
        tile.setOnLongClickListener(v -> { favMenu(cat, pkg); return true; });  // options: hold OK (MENU is reserved for Advanced)
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(L.s(208), L.s(116));
        if (margin) lp.leftMargin = L.s(16);
        tile.setLayoutParams(lp);
        loadArt(pkg, img, ic, nm, fallback);
        return tile;
    }

    String name(String pkg) {
        String n = NAMES.get(pkg);
        if (n != null) return n;
        try {
            PackageManager pm = getPackageManager();
            n = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) { n = pkg; }
        NAMES.put(pkg, n);
        return n;
    }

    private void loadArt(String pkg, ImageView img, ImageView ic, TextView nm, View fallback) {
        Drawable cached = BANNERS.get(pkg);
        if (cached != null) { img.setImageDrawable(cached); fallback.setVisibility(View.GONE); return; }
        L.IO.execute(() -> {
            PackageManager pm = getPackageManager();
            Drawable banner = null, icon = null;
            try {
                Intent li = L.launchIntent(this, pkg);
                ComponentName cn = li == null ? null : li.getComponent();
                if (cn != null) banner = pm.getActivityBanner(cn);
                if (banner == null) banner = pm.getApplicationBanner(pkg);
                if (banner == null) icon = pm.getApplicationIcon(pkg);
            } catch (Exception ignored) {}
            final Drawable b = banner, i = icon;
            final String n = name(pkg);
            L.UI.post(() -> {
                if (b != null) { BANNERS.put(pkg, b); img.setImageDrawable(b); fallback.setVisibility(View.GONE); }
                else { if (i != null) { ICONS.put(pkg, i); ic.setImageDrawable(i); } nm.setText(n); }
            });
        });
    }

    private void favMenu(int cat, String pkg) {
        List<String> items = new ArrayList<>(Arrays.asList(
                "Nach links", "Nach rechts", "In Kategorie verschieben …", "Aus Favoriten entfernen",
                "App-Info (Stopp, Speicher, Rechte)", "Kategorien verwalten …"));
        menu(name(pkg), items.toArray(new String[0]), i -> {
            List<String> pk = cats.get(cat).pkgs;
            int idx = pk.indexOf(pkg);
            switch (i) {
                case 0: if (idx > 0) { pk.remove(idx); pk.add(idx - 1, pkg); } Fav.save(this, cats); buildFavs(cat, Math.max(0, idx - 1)); break;
                case 1: if (idx >= 0 && idx < pk.size() - 1) { pk.remove(idx); pk.add(idx + 1, pkg); } Fav.save(this, cats); buildFavs(cat, idx + 1); break;
                case 2: moveToCategory(cat, pkg); break;
                case 3: pk.remove(pkg); Fav.save(this, cats); buildFavs(cat, Math.max(0, idx)); break;
                case 4: L.start(this, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))); break;
                case 5: startActivity(new Intent(this, CategoriesActivity.class)); break;
            }
        });
    }

    private void moveToCategory(int from, String pkg) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < cats.size(); i++) if (i != from) names.add(cats.get(i).name);
        if (names.isEmpty()) { L.toast(this, "Keine andere Kategorie – erst eine anlegen (Kategorien verwalten)"); return; }
        menu("Verschieben nach", names.toArray(new String[0]), sel -> {
            int to = sel < from ? sel : sel + 1;
            Fav.move(cats, from, pkg, to);
            Fav.save(this, cats);
            buildFavs(to, Math.max(0, cats.get(to).pkgs.size() - 1));
        });
    }

    private void sleepMenu() {
        long left = Acts.sleepLeftMin(this);
        menu(left > 0 ? Lang.t("Sleep timer (" + left + " min left)", "Schlaf-Timer (noch " + left + " min)") : Lang.t("Sleep timer", "Schlaf-Timer"), Acts.SLEEP_NAMES, i -> {
            Acts.setSleep(this, Acts.SLEEP_MIN[i]);
            String nm = Lang.EN ? Tr.t(Acts.SLEEP_NAMES[i]) : Acts.SLEEP_NAMES[i];
            L.toast(this, i == 0 ? Lang.t("Sleep timer off", "Schlaf-Timer aus") : Lang.t("Projector goes to standby in " + nm, "Beamer geht in " + nm + " in Standby"));
            updateStatus();
        });
    }

    // ------------------------------------------------------------------ live bits
    private void updateClock() {
        Date d = new Date();
        clock.setText(hm.format(d));
        SimpleDateFormat f = Lang.EN ? new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
                                     : new SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.GERMAN);
        date.setText(f.format(d));
    }

    private void updateStatus() {
        wifiTx.setText(Status.wifiShort(this));
        btTx.setText(Status.bt(this));
        long rx = android.net.TrafficStats.getTotalRxBytes(), tx = android.net.TrafficStats.getTotalTxBytes(), now = System.currentTimeMillis();
        double dt = Math.max(1, now - nT) / 1000.0;
        netTx.setText(Acts.ratePair(this, (long) ((rx - nRx) / dt), (long) ((tx - nTx) / dt)));
        nRx = rx; nTx = tx; nT = now;
        ramTx.setText(Status.ramFreeMb(this) + Lang.t(" MB RAM free", " MB RAM frei"));
        int temp = Status.cpuTemp();
        tempTx.setText(Acts.fmtTemp(this, temp));
        tempTx.setTextColor(temp >= L.p(this).getInt("tempWarn", 82) ? 0xFFFFB74D : t.text);
        boolean wxOn = L.p(this).getBoolean("weatherOn", true);
        if (wxOn) Weather.refresh(this);
        wxPill.setVisibility(wxOn && Weather.has() ? View.VISIBLE : View.GONE);
        if (wxOn && Weather.has()) { wxTx.setText(Weather.text()); ((ImageView) ((LinearLayout) wxPill).getChildAt(0)).setImageDrawable(new Ui.Ico(Weather.icon(), t.text)); }
        updateNow();
        long sl = Acts.sleepLeftMin(this);
        sleepPill.setVisibility(sl > 0 ? View.VISIBLE : View.GONE);
        sleepTx.setText(sl >= 60 ? (sl / 60) + ":" + String.format(Locale.GERMAN, "%02d", sl % 60) + " h" : sl + " min");
        String cap = Backdrop.caption;
        caption.setText(cap);
    }

    private void updateCinema() {
        if (cinemaLabel != null) cinemaLabel.setText(Acts.cinema(this) ? Lang.t("Cinema on", "Kino an") : Lang.t("Cinema", "Kino"));
    }

    // ------------------------------------------------------------------ now playing
    private LinearLayout nowBar;
    private TextView nowTitle, nowSub;
    private ImageView nowPlayIc, nowAppIc;
    private MediaController nowMc;

    private void buildNowBar() {
        nowBar = new LinearLayout(this);
        nowBar.setOrientation(LinearLayout.HORIZONTAL);
        nowBar.setGravity(Gravity.CENTER_VERTICAL);
        nowBar.setPadding(L.s(16), L.s(10), L.s(14), L.s(10));
        Ui.glass(nowBar, t, Theme.alpha(t.panel, Math.max(0xC0, t.panel >>> 24)), L.s(Math.min(t.radius, 18)));
        nowBar.setVisibility(View.GONE);
        nowAppIc = new ImageView(this);
        LinearLayout.LayoutParams aip = new LinearLayout.LayoutParams(L.s(34), L.s(34));
        aip.rightMargin = L.s(12);
        nowBar.addView(nowAppIc, aip);
        LinearLayout tcol = new LinearLayout(this);
        tcol.setOrientation(LinearLayout.VERTICAL);
        nowTitle = Ui.one(this, "", 16, t.bold(), t.text);
        nowSub = Ui.one(this, "", 13, t.body(), t.dim);
        tcol.addView(nowTitle);
        tcol.addView(nowSub);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(L.s(300), Ui.WRAP);
        tp.rightMargin = L.s(12);
        nowBar.addView(tcol, tp);
        nowBar.addView(mediaBtn("prev", () -> NowPlaying.prev(nowMc)));
        LinearLayout playBtn = mediaBtn("play", () -> { NowPlaying.toggle(nowMc); L.UI.postDelayed(this::updateNow, 300); });
        nowPlayIc = (ImageView) playBtn.getChildAt(0);
        nowBar.addView(playBtn);
        nowBar.addView(mediaBtn("next", () -> NowPlaying.next(nowMc)));
        FrameLayout.LayoutParams nbp = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        nbp.bottomMargin = L.s(96);
        root.addView(nowBar, nbp);
    }

    private LinearLayout mediaBtn(String ico, Runnable r) {
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER);
        float rad = L.s(999);
        Ui.glass(b, t, Theme.alpha(t.accent, 0x33), rad);
        ImageView iv = Ui.icon(this, ico, t.text, 24);
        b.addView(iv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(L.s(52), L.s(52));
        lp.leftMargin = L.s(8);
        b.setLayoutParams(lp);
        Ui.focus(b, t, 1.08f, rad, null);
        b.setOnClickListener(v -> r.run());
        b.setTag(ico);
        return b;
    }

    private void updateNow() {
        MediaController mc = NowPlaying.find(this);
        nowMc = mc;
        if (mc == null || NowPlaying.title(mc).isEmpty()) { if (nowBar != null) nowBar.setVisibility(View.GONE); return; }
        nowBar.setVisibility(View.VISIBLE);
        nowTitle.setText(NowPlaying.title(mc));
        String sub = NowPlaying.sub(mc);
        if (sub.isEmpty()) sub = name(mc.getPackageName());
        nowSub.setText(sub);
        nowPlayIc.setImageDrawable(new Ui.Ico(NowPlaying.playing(mc) ? "pause" : "play", t.text));
        try { nowAppIc.setImageDrawable(getPackageManager().getApplicationIcon(mc.getPackageName())); } catch (Exception e) { nowAppIc.setImageDrawable(new Ui.Ico("sound", t.text)); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (L.p(this).getInt("thumbsV", 0) < 2) {   // drop any black/stale frames cached by the old capture logic
            Thumbs.clear(this);
            L.p(this).edit().putInt("thumbsV", 2).apply();
        }
        registerReceiver(tick, new IntentFilter(Intent.ACTION_TIME_TICK));
        long adv = RemoteService.openAdvancedAt;   // the ☰ Menu key bounced us here — go straight on to Advanced
        if (adv > 0 && SystemClock.elapsedRealtime() - adv < 8000) {   // generous: the stock menu can be slow to open first
            RemoteService.openAdvancedAt = 0;
            startActivity(new Intent(this, AdvancedActivity.class));
            return;   // skip rebuilding the home screen we're only passing through
        }
        if (!firstRunShown && !L.p(this).getBoolean("firstRunDone", false)) {   // first launch: welcome + set-up
            firstRunShown = true;
            startActivity(new Intent(this, FirstRunActivity.class));
        }
        updateClock();
        L.UI.removeCallbacks(statusLoop);
        L.UI.post(statusLoop);
        if (firstPhotoDone) {
            long age = System.currentTimeMillis() - Backdrop.changedAt;
            long every = L.p(this).getInt("photoMin", 5) * 60_000L;
            L.UI.removeCallbacks(photoLoop);
            L.UI.postDelayed(photoLoop, Math.max(500, every - age));
        }
        if (favVBox != null) buildFavs(-1, 0); // refresh recents + focus the "Zuletzt" row
        updateCinema();
        Acts.applyZoomScene(this, "apps"); // restore the picture size you chose for the launcher/apps
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(tick); } catch (Exception ignored) {}
        L.UI.removeCallbacks(statusLoop);
        L.UI.removeCallbacks(photoLoop);
    }

    // ------------------------------------------------------------------ Home pressed twice
    // Every Home press arrives here as a new intent, so double presses are counted without catching keys.
    private static boolean firstRunShown;
    private boolean visible;
    private long visibleAt;
    private int homeTaps;
    private String lastFavSig = "";

    // Every Home press arrives as a new intent; several can arrive for one press, so we count the burst
    // and act once it settles: 1 press = jump back to the top, 2 presses = our Advanced menu.
    private final Runnable homeBurst = () -> {
        int c = homeTaps;
        homeTaps = 0;
        if (c >= 2) { startActivity(new Intent(this, AdvancedActivity.class)); return; }  // 2× Home = our Advanced menu
        closeOverlay();
        favV.smoothScrollTo(0, 0);
        HorizontalScrollView first = rowOf(0);
        if (first != null) { first.smoothScrollTo(0, 0); LinearLayout r = (LinearLayout) first.getChildAt(0); if (r.getChildCount() > 0) r.getChildAt(0).requestFocus(); }
    };

    @Override protected void onStart() {
        super.onStart();
        visible = true;
        visibleAt = SystemClock.uptimeMillis();
    }

    @Override protected void onStop() {
        super.onStop();
        visible = false;
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        homeTaps++;
        L.UI.removeCallbacks(homeBurst);
        L.UI.postDelayed(homeBurst, 400);
    }

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_MENU) {   // if the ☰ key ever reaches us directly → our Advanced menu
            startActivity(new Intent(this, AdvancedActivity.class));
            return true;
        }
        return super.onKeyDown(code, e);
    }

    private void lastApp() {
        String pkg = RemoteService.lastApp;
        if (pkg == null) pkg = lastFromUsage();
        if (pkg == null || pkg.equals(getPackageName())) { L.toast(this, "Noch keine letzte App"); return; }
        L.launch(this, pkg);
    }

    private String lastFromUsage() {
        UsageStatsManager um = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        UsageEvents ev = um.queryEvents(end - 3 * 3600_000L, end);
        UsageEvents.Event e = new UsageEvents.Event();
        String last = null;
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED && !e.getPackageName().equals(getPackageName())
                    && L.launchIntent(this, e.getPackageName()) != null) last = e.getPackageName();
        }
        return last;
    }

    @Override void back() { /* the home screen stays */ }
}
