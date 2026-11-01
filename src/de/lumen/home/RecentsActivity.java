package de.lumen.home;

import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Recently opened apps, newest first — hold Home to open. OK brings one back, hold OK to close it. */
public class RecentsActivity extends Base {
    private TextView hint;

    @Override void build() {
        TextView title = Ui.one(this, Lang.t("Recent apps", "Zuletzt geöffnet"), 30, t.head(), t.text);
        title.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        root.addView(title, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 40));
        hint = Ui.one(this, Lang.t("OK to open  ·  hold OK: close, app info", "OK öffnen  ·  lange OK: schließen, App-Info"), 15, t.body(), t.dim);
        hint.setShadowLayer(L.s(6), 0, L.s(1), 0x88000000);
        root.addView(hint, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 84));

        ScrollView sv = new ScrollView(this);
        sv.setClipChildren(false);
        sv.setClipToPadding(false);
        sv.setPadding(L.s(64), L.s(16), L.s(64), L.s(40));
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        sv.addView(grid);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> grid.invalidate());
        root.addView(sv, Ui.at(Ui.MATCH, Ui.MATCH, Gravity.TOP | Gravity.LEFT, 0, 112));

        List<String> recents = recents();
        if (recents.isEmpty()) {
            TextView e = Ui.one(this, Lang.t("Nothing open yet", "Noch nichts geöffnet"), 17, t.body(), t.dim);
            e.setPadding(L.s(8), L.s(20), 0, 0);
            grid.addView(e);
            return;
        }
        PackageManager pm = getPackageManager();
        LinearLayout row = null;
        int n = 0;
        for (String pkg : recents) {
            String label;
            try { label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
            catch (Exception ex) { continue; }
            if (n % 6 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
                rp.bottomMargin = L.s(18);
                grid.addView(row, rp);
            }
            View tile = tile(pkg, label);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(L.s(176), L.s(99));
            if (n % 6 != 0) lp.leftMargin = L.s(16);
            row.addView(tile, lp);
            n++;
        }
        if (grid.getChildCount() > 0) {
            View first = ((LinearLayout) grid.getChildAt(0)).getChildAt(0);
            if (first != null) first.requestFocus();
        }
    }

    /** Launchable packages used recently, newest first, deduped. */
    private List<String> recents() {
        Set<String> out = new LinkedHashSet<>();
        try {
            UsageStatsManager um = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long end = System.currentTimeMillis();
            UsageEvents ev = um.queryEvents(end - 12 * 3600_000L, end);
            UsageEvents.Event e = new UsageEvents.Event();
            ArrayList<String> order = new ArrayList<>();
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e);
                if (e.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) order.add(e.getPackageName());
            }
            for (int i = order.size() - 1; i >= 0; i--) {
                String p = order.get(i);
                if (p.equals(getPackageName())) continue;
                if (L.launchIntent(this, p) == null) continue;
                out.add(p);
            }
        } catch (Exception ignored) {}
        return new ArrayList<>(out);
    }

    private View tile(String pkg, String label) {
        FrameLayout tile = new FrameLayout(this);
        float r = L.s(Math.min(t.radius, 16));
        Ui.glass(tile, t);
        Ui.clip(tile, r);
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(img, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        LinearLayout fb = new LinearLayout(this);
        fb.setOrientation(LinearLayout.VERTICAL);
        fb.setGravity(Gravity.CENTER);
        ImageView ic = new ImageView(this);
        fb.addView(ic, new LinearLayout.LayoutParams(L.s(42), L.s(42)));
        TextView nm = Ui.one(this, label, 13, t.bold(), t.text);
        nm.setPadding(L.s(8), L.s(6), L.s(8), 0);
        fb.addView(nm);
        tile.addView(fb, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        Ui.focus(tile, t, 1.08f, r, has -> { if (has) hint.setText(label); });
        tile.setOnClickListener(v -> { L.launch(this, pkg); finish(); });
        tile.setOnLongClickListener(v -> { opts(pkg, label); return true; });
        tile.setOnKeyListener((v, code, e) -> {
            if (code == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP) { opts(pkg, label); return true; }
            return false;
        });
        Drawable cached = HomeActivity.BANNERS.get(pkg);
        if (cached != null) { img.setImageDrawable(cached); fb.setVisibility(View.GONE); }
        else L.IO.execute(() -> {
            PackageManager pm = getPackageManager();
            Drawable b = null, i = null;
            try {
                Intent li = L.launchIntent(this, pkg);
                ComponentName cn = li == null ? null : li.getComponent();
                if (cn != null) b = pm.getActivityBanner(cn);
                if (b == null) b = pm.getApplicationBanner(pkg);
                if (b == null) i = pm.getApplicationIcon(pkg);
            } catch (Exception ignored) {}
            final Drawable fbn = b, fi = i;
            L.UI.post(() -> {
                if (fbn != null) { HomeActivity.BANNERS.put(pkg, fbn); img.setImageDrawable(fbn); fb.setVisibility(View.GONE); }
                else if (fi != null) ic.setImageDrawable(fi);
            });
        });
        return tile;
    }

    private void opts(String pkg, String label) {
        menu(label, new String[]{Lang.t("Close app", "App schließen"), Lang.t("App info", "App-Info"), Lang.t("Cancel", "Abbrechen")}, i -> {
            if (i == 0) {
                ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).killBackgroundProcesses(pkg);
                L.toast(this, Lang.t("Closed", "Geschlossen"));
                recreate();
            } else if (i == 1) {
                L.start(this, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
            }
        });
    }
}
