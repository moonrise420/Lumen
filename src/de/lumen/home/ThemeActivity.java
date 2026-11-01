package de.lumen.home;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Pick a preset (live preview on focus), then mix font, focus, glass, dim, corners and accent. */
public class ThemeActivity extends Base {
    private FrameLayout preview;
    private String previewId;

    @Override void build() {
        TextView title = Ui.one(this, "Themes & Mischen", 30, t.head(), t.text);
        title.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        root.addView(title, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 56, 36));
        TextView sub = Ui.one(this, "Auswählen zeigt die Vorschau  ·  OK übernimmt  ·  unten frei mischen", 15, t.body(), t.dim);
        sub.setShadowLayer(L.s(6), 0, L.s(1), 0x88000000);
        root.addView(sub, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 56, 80));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(L.s(8), L.s(8), L.s(8), L.s(40));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> list.invalidate());
        root.addView(sv, Ui.at(L.s(520), Ui.MATCH, Gravity.TOP | Gravity.LEFT, 48, 112));

        String cur = L.p(this).getString("theme", "glass");
        View curRow = null;
        list.addView(Ui.header(this, t, "Looks"));
        for (Theme th : Theme.ALL) {
            Ui.Row r = Ui.action(this, t, "palette", th.name, th.mood, () -> th.id.equals(L.p(this).getString("theme", "glass")) ? "✓ aktiv" : "", () -> {
                L.p(this).edit().putString("theme", th.id).apply();
                recreate();
            });
            View.OnFocusChangeListener base = r.getOnFocusChangeListener();
            r.setOnFocusChangeListener((v, has) -> { base.onFocusChange(v, has); if (has) showPreview(th.id); });
            add(list, r);
            if (th.id.equals(cur)) curRow = r;
        }
        list.addView(Ui.header(this, t, "Mischen (gilt für jedes Theme)"));
        mix(list, "look", "Schrift", Theme.FONT_NAMES, "mixFont");
        mix(list, "look", "Fokus-Effekt", Theme.FOCUS_NAMES, "mixFocus");
        mix(list, "look", "Glas-Stärke", Theme.GLASS_NAMES, "mixGlass");
        mix(list, "photo", "Foto abdunkeln", Theme.DIM_NAMES, "mixDim");
        mix(list, "look", "Ecken", Theme.CORNER_NAMES, "mixCorner");
        mix(list, "palette", "Akzentfarbe", Theme.ACCENT_NAMES, "mixAccent");
        add(list, Ui.action(this, t, "home", "Mischung zurücksetzen", "Alles wieder wie im Theme", () -> "›", () -> {
            L.p(this).edit().remove("mixFont").remove("mixFocus").remove("mixGlass").remove("mixDim").remove("mixCorner").remove("mixAccent").apply();
            recreate();
        }));

        preview = new FrameLayout(this);
        root.addView(preview, Ui.at(L.s(608), L.s(342), Gravity.TOP | Gravity.RIGHT, 56, 150));
        showPreview(cur);
        if (curRow != null) curRow.requestFocus();
    }

    private void add(LinearLayout l, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(7);
        l.addView(v, lp);
    }

    private void mix(LinearLayout l, String ico, String name, String[] opts, String key) {
        Ui.Row r = Ui.choice(this, t, ico, name, null, opts, () -> L.p(this).getInt(key, 0), i -> {
            L.p(this).edit().putInt(key, i).apply();
            previewId = null;
            showPreview(L.p(this).getString("theme", "glass"));
        });
        View.OnFocusChangeListener base = r.getOnFocusChangeListener();
        r.setOnFocusChangeListener((v, has) -> { base.onFocusChange(v, has); if (has) showPreview(L.p(this).getString("theme", "glass")); });
        add(l, r);
    }

    /** A miniature home screen drawn in the candidate look (half scale). */
    private void showPreview(String id) {
        if (id.equals(previewId)) return;
        previewId = id;
        Theme p = Theme.build(this, id);
        preview.removeAllViews();
        Ui.clip(preview, L.s(16));
        preview.addView(new Backdrop.View0(this, p), new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        preview.setForeground(Ui.round(0, L.s(16)));

        TextView clock = Ui.one(this, "20:45", 40, p.head(), p.text);
        clock.setShadowLayer(L.s(8), 0, L.s(1), 0x66000000);
        preview.addView(clock, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 26, 20));
        TextView date = Ui.one(this, Lang.t("Saturday, 31 October", "Samstag, 31. Oktober"), 12, p.bold(), p.text);
        preview.addView(date, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 28, 70));

        LinearLayout pills = new LinearLayout(this);
        String[][] ps = {{"wifi", "86 Mbit/s"}, {"ram", "460 MB"}, {"temp", "62 °C"}};
        for (int i = 0; i < ps.length; i++) {
            TextView tx = Ui.one(this, ps[i][1], 10, p.bold(), p.text);
            LinearLayout pl = Ui.pill(this, p, ps[i][0], tx);
            pl.setPadding(L.s(8), L.s(4), L.s(10), L.s(4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            if (i > 0) lp.leftMargin = L.s(6);
            pills.addView(pl, lp);
        }
        preview.addView(pills, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.RIGHT, 20, 22));

        LinearLayout row = new LinearLayout(this);
        String[] names = {"Netflix", "YouTube", "Prime", "Twitch"};
        for (int i = 0; i < names.length; i++) {
            FrameLayout tile = new FrameLayout(this);
            Ui.glass(tile, p, Theme.alpha(p.panel, Math.min(255, (p.panel >>> 24) + 30)), L.s(p.radius * 0.6f));
            TextView n = Ui.one(this, names[i], 12, p.bold(), p.text);
            tile.addView(n, new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.CENTER));
            if (i == 0) {
                tile.setForeground(Ui.ring(p, L.s(p.radius * 0.6f)));
                tile.setScaleX(1.07f);
                tile.setScaleY(1.07f);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(L.s(116), L.s(66));
            if (i > 0) lp.leftMargin = L.s(12);
            row.addView(tile, lp);
        }
        preview.addView(row, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 28, 160));

        LinearLayout chips = new LinearLayout(this);
        String[][] cs = {{"hdmi", "HDMI"}, {"usb", "USB"}, {"film", Lang.t("Cinema", "Kino")}, {"gear", "Erweitert"}};
        for (int i = 0; i < cs.length; i++) {
            TextView tx = Ui.one(this, cs[i][1], 11, p.bold(), p.text);
            LinearLayout c = Ui.pill(this, p, cs[i][0], tx);
            c.setBackground(new Ui.Glass(c, p.panel, p.border, L.s(Math.min(p.radius, 16) * 0.6f)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            if (i > 0) lp.leftMargin = L.s(8);
            chips.addView(c, lp);
        }
        preview.addView(chips, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM | Gravity.LEFT, 28, 20));

        TextView nm = Ui.one(this, Theme.byId(id).name, 13, p.bold(), p.accent);
        preview.addView(nm, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM | Gravity.RIGHT, 22, 24));
    }
}
