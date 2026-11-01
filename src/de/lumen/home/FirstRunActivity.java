package de.lumen.home;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shown once, on the very first launch: pick a language, then offer to make Lumen the home screen. */
public class FirstRunActivity extends Base {

    @Override void build() {
        L.p(this).edit().putBoolean("firstRunDone", true).apply(); // never auto-show again, even if interrupted

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(L.s(56), L.s(36), L.s(56), L.s(40));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        sv.addView(col);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> col.invalidate());
        root.addView(sv, Ui.at(Ui.MATCH, Ui.MATCH, Gravity.CENTER, 0, 0));

        TextView title = Ui.one(this, Lang.t("Welcome to Lumen", "Willkommen bei Lumen"), 34, t.head(), t.text);
        title.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        col.addView(title, top(0));
        TextView sub = Ui.one(this, "by Soulride", 15, t.body(), t.dim);
        col.addView(sub, top(2));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(L.s(560), Ui.WRAP);
        bp.topMargin = L.s(22);
        col.addView(box, bp);

        // ---- language ----
        box.addView(Ui.header(this, t, Lang.t("Language", "Sprache")));
        lang(box, "en", "English");
        lang(box, "de", "Deutsch");
        lang(box, "auto", Lang.t("Automatic (system)", "Automatisch (System)"));

        // ---- home screen ----
        box.addView(Ui.header(this, t, Lang.t("Home screen", "Startbildschirm")));
        TextView info = Ui.tv(this, Lang.t(
                "Make Lumen the screen you see when you turn the projector on.",
                "Mach Lumen zu dem Bildschirm, den du beim Einschalten des Beamers siehst."), 14, t.body(), t.dim);
        info.setPadding(L.s(6), L.s(2), L.s(6), L.s(8));
        box.addView(info);
        Ui.Row home = Ui.action(this, t, "home",
                Lang.t("Make Lumen the home screen", "Lumen als Startbildschirm festlegen"), null,
                () -> Sys.isHome(this) ? "✓" : "›", null);
        home.setOnClickListener(v -> {
            boolean ok = Sys.makeHome(this);
            home.refresh();
            L.toast(this, ok || Sys.isHome(this) ? Lang.t("Done", "Erledigt")
                    : Lang.t("Pick Lumen in the list that opened", "Wähle Lumen in der geöffneten Liste"));
        });
        add(box, home);

        // ---- finish ----
        Ui.Row done = Ui.action(this, t, "info", Lang.t("Finish", "Fertig"), null, () -> "›", this::finish);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        dp.topMargin = L.s(18);
        box.addView(done, dp);

        box.getChildAt(1).requestFocus(); // first language row
    }

    private void lang(LinearLayout box, String val, String label) {
        Ui.Row r = Ui.action(this, t, "net", label, null,
                () -> val.equals(L.p(this).getString("lang", "auto")) ? "✓" : "",
                () -> { L.p(this).edit().putString("lang", val).apply(); recreate(); });
        add(box, r);
    }

    private void add(LinearLayout l, View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(7);
        l.addView(v, lp);
    }

    private LinearLayout.LayoutParams top(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        lp.topMargin = L.s(topDp);
        return lp;
    }
}
