package de.lumen.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Picture-size lab. The live zoom can only be changed by system apps (SurfaceFlinger call 1050), so this
 * writes the stored zoom (persist.sys.zoom.value, 8 corner values in 1/1000) that the projector applies when it
 * recalculates keystone, e.g. after a restart. Smaller values = bigger picture. The first values seen are kept
 * as "Standard" for reset.
 */
public class ScreenSizeActivity extends Base {
    static final String PROP = "persist.sys.zoom.value";
    static final int G_MIN = -200;

    private int[] base;
    private int g, gMax;
    private Bar bar;
    private TextView value, numbers;
    private final Runnable apply = this::applyNow;

    @Override void build() {
        base = baseline(this);
        gMax = 9999;
        for (int b : base) gMax = Math.min(gMax, b);
        g = L.p(this).getInt("zoomG", 0);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(L.s(40), L.s(32), L.s(40), L.s(30));
        Ui.glass(box, t, Theme.alpha(t.panel, Math.max(0xC0, t.panel >>> 24)), L.s(t.radius + 4));
        box.addView(Ui.one(this, "Bildgröße-Labor", 30, t.head(), t.text));
        String how = Sys.on()
                ? "Das Bild ändert sich sofort (System-Rechte aktiv). Links / rechts: kleiner / größer, gedrückt halten = schneller."
                : "Lumen speichert die Größe, der Beamer übernimmt sie beim nächsten Neustart. Links / rechts: kleiner / größer, gedrückt halten = schneller.";
        TextView sub = Ui.tv(this, how, 15, t.body(), t.dim);
        sub.setPadding(0, L.s(6), 0, L.s(22));
        box.addView(sub);

        value = Ui.one(this, "", 22, t.bold(), t.text);
        box.addView(value);
        bar = new Bar(this);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.MATCH, L.s(96));
        bp.topMargin = L.s(10);
        box.addView(bar, bp);
        numbers = Ui.one(this, "", 13, t.body(), t.dim);
        numbers.setPadding(0, L.s(4), 0, L.s(16));
        box.addView(numbers);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.addView(Ui.chip(this, t, "home", "Zurücksetzen", v -> { g = 0; changed(); }));
        btns.addView(space());
        btns.addView(Ui.chip(this, t, "power", "Neu starten", v -> {
            if (Sys.reboot(this)) return;
            if (RemoteService.inst != null) RemoteService.inst.powerMenu(); else L.toast(this, "Bitte über die Ein/Aus-Taste neu starten");
        }));
        btns.addView(space());
        btns.addView(Ui.chip(this, t, "gear", "Werks-Zoom", v -> L.comp(this, "com.htc.htcsettings", "com.htc.activity.ProjectionActivity")));
        btns.addView(space());
        btns.addView(Ui.chip(this, t, "flask", "Werk-Min merken", v -> learn("zoomMinG")));
        btns.addView(space());
        btns.addView(Ui.chip(this, t, "flask", "Werk-Max merken", v -> learn("zoomMaxG")));
        box.addView(btns);

        if (Sys.on()) {
            LinearLayout scenes = new LinearLayout(this);
            scenes.setOrientation(LinearLayout.HORIZONTAL);
            scenes.setPadding(0, L.s(10), 0, 0);
            scenes.addView(Ui.chip(this, t, "apps", "Diese Größe für Apps", v -> { Acts.saveZoomScene(this, "apps", g); L.toast(this, "Für Apps/Launcher gemerkt"); }));
            scenes.addView(space());
            scenes.addView(Ui.chip(this, t, "hdmi", "Diese Größe für HDMI", v -> { Acts.saveZoomScene(this, "hdmi", g); L.toast(this, "Für HDMI gemerkt – wird beim Umschalten gesetzt"); }));
            box.addView(scenes);
        }

        TextView tip = Ui.tv(this, "Werksgrenzen lernen: im Werks-Zoom ganz klein stellen, zurück, 'Werk-Min merken'. Dann ganz groß, 'Werk-Max merken'. Danach zeigt der Balken beide Grenzen.", 13, t.body(), t.dim);
        tip.setPadding(0, L.s(18), 0, 0);
        box.addView(tip);

        root.addView(box, Ui.at(L.s(1040), Ui.WRAP, Gravity.CENTER, 0, 0));
        update();
        bar.requestFocus();
    }

    private View space() { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(L.s(10), 1)); return v; }

    private static int[] parse(String s) {
        int[] b = new int[8];
        String[] p = s.split(",");
        for (int i = 0; i < 8; i++) {
            try { b[i] = Integer.parseInt(p[i].trim()); } catch (Exception e) { b[i] = 167; }
        }
        return b;
    }

    static int[] baseline(Context c) {
        String s = L.p(c).getString("zoomBase", null);
        if (s == null) {
            s = L.prop(PROP);
            if (s.split(",").length != 8) s = "167,166,167,167,167,167,167,166";
            L.p(c).edit().putString("zoomBase", s).apply();
        }
        return parse(s);
    }

    private void learn(String key) {
        int[] cur = parse(L.prop(PROP));
        int sum = 0;
        for (int i = 0; i < 8; i++) sum += base[i] - cur[i];
        int lg = Math.round(sum / 8f);
        L.p(this).edit().putInt(key, lg).putInt("zoomG", lg).apply();
        g = lg;
        L.toast(this, Lang.t("Saved: ", "Gemerkt: ") + label(lg));
        update();
    }

    private void changed() {
        g = Math.max(G_MIN, Math.min(gMax, g));
        L.p(this).edit().putInt("zoomG", g).apply();
        update();
        L.UI.removeCallbacks(apply);
        L.UI.postDelayed(apply, 200);
    }

    private String joined() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(i > 0 ? "," : "").append(Math.max(0, base[i] - g));
        return sb.toString();
    }

    private void applyNow() {
        final String v = joined();
        final int[] vals = new int[8];
        for (int i = 0; i < 8; i++) vals[i] = Math.max(0, base[i] - g);
        L.IO.execute(() -> {
            L.sh("setprop " + PROP + " " + v);           // persists for next boot
            if (Sys.on()) L.UI.post(() -> Sys.zoomLive(vals)); // and apply immediately
        });
    }

    private String label(int v) {
        if (v == 0) return "Standard";
        return (v > 0 ? "+" : "") + v + (v > 0 ? Lang.t("  (bigger)", "  (größer)") : Lang.t("  (smaller)", "  (kleiner)"));
    }

    private void update() {
        int min = L.p(this).getInt("zoomMinG", Integer.MIN_VALUE), max = L.p(this).getInt("zoomMaxG", Integer.MIN_VALUE);
        boolean beyond = (min != Integer.MIN_VALUE && g < min) || (max != Integer.MIN_VALUE && g > max);
        value.setText(Lang.t("Size: ", "Größe: ") + label(g) + (beyond ? Lang.t("   ·   beyond factory limits", "   ·   außerhalb der Werksgrenzen") : ""));
        value.setTextColor(beyond ? 0xFFFFB74D : t.text);
        numbers.setText(Lang.t("Saved for next start: ", "Gespeichert für den nächsten Start: ") + joined() + Lang.t("   (default: ", "   (Standard: ") + L.p(this).getString("zoomBase", "") + ")");
        bar.invalidate();
    }

    /** The slider: track, stock zone, standard mark, knob. */
    final class Bar extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF r = new RectF();

        Bar(Context c) {
            super(c);
            setFocusable(true);
            setOnFocusChangeListener((v, has) -> invalidate());
        }

        float x(int v) { return L.s(20) + (v - G_MIN) / (float) (gMax - G_MIN) * (getWidth() - L.s(40)); }

        @Override protected void onDraw(Canvas c) {
            float cy = getHeight() * 0.42f, th = L.s(10);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Theme.alpha(t.text, 0x30));
            r.set(x(G_MIN), cy - th / 2, x(gMax), cy + th / 2);
            c.drawRoundRect(r, th, th, p);
            int min = L.p(getContext()).getInt("zoomMinG", Integer.MIN_VALUE), max = L.p(getContext()).getInt("zoomMaxG", Integer.MIN_VALUE);
            if (min != Integer.MIN_VALUE && max != Integer.MIN_VALUE) {
                p.setColor(Theme.alpha(t.accent, 0x90));
                r.set(x(Math.min(min, max)), cy - th / 2, x(Math.max(min, max)), cy + th / 2);
                c.drawRoundRect(r, th, th, p);
            }
            p.setTextSize(L.s(13));
            p.setTypeface(t.bold());
            p.setTextAlign(Paint.Align.CENTER);
            mark(c, 0, cy, "Standard");
            if (min != Integer.MIN_VALUE) mark(c, min, cy, Lang.t("Factory min", "Werk-Min"));
            if (max != Integer.MIN_VALUE) mark(c, max, cy, Lang.t("Factory max", "Werk-Max"));
            p.setColor(t.dim);
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText(Lang.t("smaller", "kleiner"), x(G_MIN), getHeight() - L.s(4), p);
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(Lang.t("bigger", "größer"), x(gMax), getHeight() - L.s(4), p);
            float kx = x(g);
            p.setColor(isFocused() ? t.focus : t.text);
            c.drawCircle(kx, cy, L.s(isFocused() ? 16 : 13), p);
            p.setColor(0x55000000);
            c.drawCircle(kx, cy, L.s(5), p);
        }

        void mark(Canvas c, int v, float cy, String s) {
            float mx = x(v);
            p.setColor(t.text);
            c.drawRect(mx - L.s(1.5f), cy - L.s(18), mx + L.s(1.5f), cy + L.s(18), p);
            c.drawText(s, mx, cy + L.s(36), p);
        }

        @Override public boolean onKeyDown(int code, KeyEvent e) {
            int step = e.getRepeatCount() > 8 ? 6 : e.getRepeatCount() > 2 ? 3 : 1;
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT) { g += step; changed(); return true; }
            if (code == KeyEvent.KEYCODE_DPAD_LEFT) { g -= step; changed(); return true; }
            return super.onKeyDown(code, e);
        }
    }
}
