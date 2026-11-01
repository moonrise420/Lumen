package de.lumen.home;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every launchable app as a grid. With extra "pick": choose one to add to favourites. */
public class AppsActivity extends Base {
    private boolean pick;
    private TextView hint;

    @Override void build() {
        pick = getIntent().getBooleanExtra("pick", false);
        TextView title = Ui.one(this, pick ? "App zu Favoriten hinzufügen" : "Alle Apps", 30, t.head(), t.text);
        title.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        root.addView(title, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 40));
        hint = Ui.one(this, pick ? "OK: hinzufügen" : "OK öffnen  ·  lange OK: Favorit, App-Info, Deinstallieren", 15, t.body(), t.dim);
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

        Map<String, String> apps = apps();
        List<String> favs = L.favs(this);
        LinearLayout row = null;
        int n = 0;
        for (Map.Entry<String, String> e : apps.entrySet()) {
            if (pick && favs.contains(e.getKey())) continue;
            if (n % 6 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
                rp.bottomMargin = L.s(18);
                grid.addView(row, rp);
            }
            View tile = tile(e.getKey(), e.getValue());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(L.s(176), L.s(99));
            if (n % 6 != 0) lp.leftMargin = L.s(16);
            row.addView(tile, lp);
            n++;
        }
        if (grid.getChildCount() > 0) ((LinearLayout) grid.getChildAt(0)).getChildAt(0).requestFocus();
    }

    /** package -> label, sorted by label; TV apps and phone apps both. */
    private Map<String, String> apps() {
        PackageManager pm = getPackageManager();
        Map<String, String> m = new java.util.TreeMap<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if (p.equals(getPackageName())) continue;
                m.put(p, ri.loadLabel(pm).toString());
            }
        }
        List<Map.Entry<String, String>> l = new ArrayList<>(m.entrySet());
        l.sort((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : l) out.put(e.getKey(), e.getValue());
        return out;
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
        Ui.focus(tile, t, 1.08f, r, has -> { if (has) hint.setText(label + (pick ? Lang.t("  ·  OK: add", "  ·  OK: hinzufügen") : Lang.t("  ·  OK to open  ·  hold OK: options", "  ·  OK öffnen  ·  lange OK: Optionen"))); });
        tile.setOnClickListener(v -> {
            if (pick) {
                List<Fav> cats = Fav.load(this);
                int ci = Math.max(0, Math.min(getIntent().getIntExtra("cat", 0), cats.size() - 1));
                if (!cats.get(ci).pkgs.contains(pkg)) cats.get(ci).pkgs.add(pkg);
                Fav.save(this, cats);
                L.toast(this, label + " → " + cats.get(ci).name);
                finish();
            } else L.launch(this, pkg);
        });
        tile.setOnLongClickListener(v -> { options(pkg, label); return true; });
        tile.setOnKeyListener((v, code, e) -> {
            if (code == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP) { options(pkg, label); return true; }
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

    private void options(String pkg, String label) {
        List<Fav> cats = Fav.load(this);
        boolean fav = Fav.contains(cats, pkg);
        String[] items = {fav ? "Aus Favoriten entfernen" : "Zu Favoriten hinzufügen", "App-Info (Stopp, Speicher, Rechte)", "Deinstallieren"};
        menu(label, items, i -> {
            if (i == 0) {
                if (fav) { for (Fav f : cats) f.pkgs.remove(pkg); } else cats.get(0).pkgs.add(pkg);
                Fav.save(this, cats);
                L.toast(this, fav ? Lang.t("Removed", "Entfernt") : Lang.t("Added to ", "Hinzugefügt zu ") + cats.get(0).name);
            } else if (i == 1) L.start(this, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
            else L.start(this, new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
        });
    }
}
