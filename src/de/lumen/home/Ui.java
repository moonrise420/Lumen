package de.lumen.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** View builders: frosted glass, icons, focus effects, rows. */
final class Ui {
    private Ui() {}

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    // ------------------------------------------------------------------ glass
    /** Draws the blurred wallpaper behind the view, a tint and a hairline border: real frosted glass. */
    static final class Glass extends Drawable {
        private final View host;
        private final int tint, border;
        private final float r;
        private final Path path = new Path();
        private final RectF rf = new RectF();
        private final Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG),
                sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix m = new Matrix();
        private final int[] loc = new int[2];

        Glass(View host, int tint, int border, float radius) {
            this.host = host; this.tint = tint; this.border = border; this.r = radius;
            tp.setColor(tint);
            sp.setStyle(Paint.Style.STROKE);
            sp.setStrokeWidth(Math.max(1f, 1.2f * L.F));
            sp.setColor(border);
            Backdrop.watch(host);
        }

        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            rf.set(b);
            path.reset();
            path.addRoundRect(rf, r, r, Path.Direction.CW);
            c.save();
            c.clipPath(path);
            Bitmap bl = Backdrop.blur;
            if (bl != null && !bl.isRecycled() && Theme.brightness(tint) < 250) {
                host.getLocationInWindow(loc);
                m.setScale(L.W / (float) bl.getWidth(), L.H / (float) bl.getHeight());
                m.postTranslate(-loc[0], -loc[1]);
                c.drawBitmap(bl, m, bp);
            }
            c.drawRect(b, tp);
            c.restore();
            if ((border >>> 24) != 0) {
                rf.inset(sp.getStrokeWidth() / 2, sp.getStrokeWidth() / 2);
                c.drawRoundRect(rf, r, r, sp);
            }
        }

        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    static void glass(View v, Theme t) { v.setBackground(new Glass(v, t.panel, t.border, L.s(t.radius))); }

    static void glass(View v, Theme t, int tint, float radius) { v.setBackground(new Glass(v, tint, t.border, radius)); }

    static GradientDrawable round(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r);
        return g;
    }

    static void clip(View v, float r) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline o) { o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r); }
        });
        v.setClipToOutline(true);
    }

    // ------------------------------------------------------------------ text
    static TextView tv(Context c, String s, float size, Typeface tf, int color) {
        TextView t = new TextView(c);
        t.setText(Lang.EN ? Tr.t(s) : s); // English mode: translate known German strings at render time
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, L.s(size));
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setSingleLine(false);
        if (L.BOLD) { t.getPaint().setFakeBoldText(true); t.setShadowLayer(L.s(4), 0, L.s(1), 0x66000000); } // readability option
        return t;
    }

    static TextView one(Context c, String s, float size, Typeface tf, int color) {
        TextView t = tv(c, s, size, tf, color);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    // ------------------------------------------------------------------ focus
    static Drawable ring(Theme t, float r) {
        if (t.focusStyle == Theme.GLOW) {
            GradientDrawable outer = new GradientDrawable();
            outer.setCornerRadius(r);
            outer.setStroke(L.s(7), Theme.alpha(t.focus, 0x55));
            GradientDrawable inner = new GradientDrawable();
            inner.setCornerRadius(Math.max(0, r - L.s(3)));
            inner.setStroke(L.s(2.5f), t.focus);
            LayerDrawable ld = new LayerDrawable(new Drawable[]{outer, inner});
            ld.setLayerInset(1, L.s(3), L.s(3), L.s(3), L.s(3));
            return ld;
        }
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(r);
        g.setStroke(t.focusStyle == Theme.LIFT ? L.s(1.5f) : L.s(3.5f), t.focusStyle == Theme.LIFT ? Theme.alpha(t.focus, 0xAA) : t.focus);
        return g;
    }

    static void focus(View v, Theme t, float scale, float radius, Consumer<Boolean> also) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnFocusChangeListener((view, has) -> {
            float s = has ? (t.focusStyle == Theme.LIFT ? scale + 0.05f : scale) : 1f;
            float z = has ? L.s(t.focusStyle == Theme.LIFT ? 18 : 8) : 0;
            if (L.anim(view.getContext())) view.animate().scaleX(s).scaleY(s).translationZ(z).setDuration(140).start();
            else { view.setScaleX(s); view.setScaleY(s); view.setTranslationZ(z); }
            view.setForeground(has ? ring(t, radius) : null);
            if (also != null) also.accept(has);
        });
    }

    // ------------------------------------------------------------------ icons
    private static final Map<String, String> ICONS = new HashMap<>();
    static {
        ICONS.put("wifi", "M2 9 Q12 0 22 9 M5 12.5 Q12 6 19 12.5 M8.5 16 Q12 13 15.5 16 O12 19.5 1");
        ICONS.put("bt", "M7 7 L17 17 L12 22 L12 2 L17 7 L7 17");
        ICONS.put("ram", "R4 6 16 12 2 M8 10 L16 10 M8 14 L13 14");
        ICONS.put("temp", "M10 4 Q10 2 12 2 Q14 2 14 4 L14 14 Q17 16 15.5 19.5 Q12 23 8.5 19.5 Q7 16 10 14 Z O12 17.5 1.5");
        ICONS.put("hdmi", "R3 7 18 10 2 M7 11 L17 11");
        ICONS.put("usb", "M12 3 L12 17 M8 7 L12 3 L16 7 M7 13 L7 15 Q7 17 9 17 L15 17 Q17 17 17 15 L17 13");
        ICONS.put("cast", "R3 4 18 13 2 M8 21 L16 21 M12 17 L12 21");
        ICONS.put("apps", "R3 3 7 7 1.5 R14 3 7 7 1.5 R3 14 7 7 1.5 R14 14 7 7 1.5");
        ICONS.put("size", "M3 9 L3 3 L9 3 M15 3 L21 3 L21 9 M21 15 L21 21 L15 21 M9 21 L3 21 L3 15");
        ICONS.put("film", "R3 5 18 14 2 M7 5 L7 19 M17 5 L17 19 M3 12 L21 12");
        ICONS.put("palette", "M12 3 Q21 3 21 11 Q21 15 17 15 L15 15 Q13 15 14 17 Q15 19 13 21 Q3 21 3 12 Q3 3 12 3 Z O8 10 1 O12 7 1 O16 10 1");
        ICONS.put("gear", "O12 12 3.2 M12 2 L12 5 M12 19 L12 22 M2 12 L5 12 M19 12 L22 12 M4.9 4.9 L7 7 M17 17 L19.1 19.1 M4.9 19.1 L7 17 M17 7 L19.1 4.9");
        ICONS.put("help", "O12 12 9 M9.5 9.5 Q9.5 7 12 7 Q14.5 7 14.5 9.5 Q14.5 11.5 12 12.5 L12 14 O12 17 0.6");
        ICONS.put("moon", "M20 14 Q17 16 14 15 Q8 13 9 6 Q9.5 4.5 10 4 Q4 5 4 12 Q4 20 12 20 Q17 20 20 14 Z");
        ICONS.put("phone", "R7 2 10 20 2 M11 18 L13 18");
        ICONS.put("power", "M12 2 L12 11 M6.3 6.3 Q3 9.5 4 14 Q5.5 20 12 20.5 Q18.5 20 20 14 Q21 9.5 17.7 6.3");
        ICONS.put("info", "O12 12 9 M12 11 L12 17 O12 7.5 0.6");
        ICONS.put("net", "O12 12 9 M3 12 L21 12 M12 3 Q7 12 12 21 M12 3 Q17 12 12 21");
        ICONS.put("sound", "M4 9 L8 9 L13 5 L13 19 L8 15 L4 15 Z M16.5 8.5 Q19 12 16.5 15.5 M19 6 Q23 12 19 18");
        ICONS.put("storage", "R3 4 18 6 1.5 R3 14 18 6 1.5 O7 7 0.6 O7 17 0.6");
        ICONS.put("flask", "M9 3 L15 3 M10 3 L10 9 L4.5 19 Q4 21 6 21 L18 21 Q20 21 19.5 19 L14 9 L14 3 M7 15 L17 15");
        ICONS.put("eye", "M2 12 Q12 3 22 12 Q12 21 2 12 Z O12 12 3");
        ICONS.put("remote", "R7 2 10 20 3 O12 7 1.6 M10 12 L14 12 M10 15 L14 15");
        ICONS.put("home", "M3 11 L12 4 L21 11 L21 20 L15 20 L15 14 L9 14 L9 20 L3 20 Z");
        ICONS.put("plus", "M12 5 L12 19 M5 12 L19 12");
        ICONS.put("lock", "R5 11 14 10 2 M8 11 L8 7 Q8 3 12 3 Q16 3 16 7 L16 11");
        ICONS.put("speed", "M4 18 Q2 10 8 6 Q12 4 16 6 Q22 10 20 18 M12 14 L16 9 O12 15 1");
        ICONS.put("clock", "O12 12 9 M12 7 L12 12 L15.5 14");
        ICONS.put("look", "O12 12 9 M12 3 L12 21 M12 3 Q21 3 21 12 Q21 21 12 21");
        ICONS.put("photo", "R3 5 18 14 2 M3 16 L9 10 L14 15 L17 12 L21 16 O16 9 1.5");
        ICONS.put("sun", "O12 12 4 M12 2 L12 5 M12 19 L12 22 M2 12 L5 12 M19 12 L22 12 M4.9 4.9 L7 7 M17 17 L19.1 19.1 M4.9 19.1 L7 17 M17 7 L19.1 4.9");
        ICONS.put("cloud", "M7 18 Q3 18 3 14 Q3 10 7.5 10 Q8 6 12.5 6 Q17 6 17.5 10.5 Q21 10.5 21 14 Q21 18 17 18 Z");
        ICONS.put("rain", "M7 15 Q3 15 3 11.5 Q3 8 7.5 8 Q8 4.5 12 4.5 Q16 4.5 16.5 8.5 Q20 8.5 20 12 Q20 15 16 15 M8 18 L7 20 M12 18 L11 21 M16 18 L15 20");
        ICONS.put("snow", "M7 15 Q3 15 3 11.5 Q3 8 7.5 8 Q8 4.5 12 4.5 Q16 4.5 16.5 8.5 Q20 8.5 20 12 Q20 15 16 15 O8 19 0.6 O12 20 0.6 O16 19 0.6");
        ICONS.put("storm", "M7 14 Q3 14 3 10.5 Q3 7 7.5 7 Q8 3.5 12 3.5 Q16 3.5 16.5 7.5 Q20 7.5 20 11 Q20 14 16 14 M12 13 L9 18 L12 18 L10 22");
        ICONS.put("tv", "R3 4 18 12 2 M8 20 L16 20 M12 16 L12 20");
        ICONS.put("play", "M7 5 L19 12 L7 19 Z");
        ICONS.put("pause", "R6 5 4 14 1 R14 5 4 14 1");
        ICONS.put("prev", "M18 5 L8 12 L18 19 Z M6 5 L6 19");
        ICONS.put("next", "M6 5 L16 12 L6 19 Z M18 5 L18 19");
    }

    static Path path(String d) {
        Path p = new Path();
        String[] tk = d.replaceAll("([A-Za-z])", " $1 ").trim().split("\\s+");
        int i = 0;
        String cmd = "M";
        while (i < tk.length) {
            String t = tk[i];
            if (Character.isLetter(t.charAt(0))) {
                cmd = t;
                i++;
                if (cmd.equals("Z")) p.close();
                continue;
            }
            float[] v;
            switch (cmd) {
                case "M": v = nums(tk, i, 2); p.moveTo(v[0], v[1]); i += 2; cmd = "L"; break;
                case "L": v = nums(tk, i, 2); p.lineTo(v[0], v[1]); i += 2; break;
                case "Q": v = nums(tk, i, 4); p.quadTo(v[0], v[1], v[2], v[3]); i += 4; break;
                case "C": v = nums(tk, i, 6); p.cubicTo(v[0], v[1], v[2], v[3], v[4], v[5]); i += 6; break;
                case "O": v = nums(tk, i, 3); p.addCircle(v[0], v[1], v[2], Path.Direction.CW); i += 3; break;
                case "R": v = nums(tk, i, 5); p.addRoundRect(new RectF(v[0], v[1], v[0] + v[2], v[1] + v[3]), v[4], v[4], Path.Direction.CW); i += 5; break;
                default: i++;
            }
        }
        return p;
    }

    private static float[] nums(String[] tk, int i, int n) {
        float[] v = new float[n];
        for (int k = 0; k < n && i + k < tk.length; k++) v[k] = Float.parseFloat(tk[i + k]);
        return v;
    }

    static final class Ico extends Drawable {
        private final Path src;
        private final Path scaled = new Path();
        private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix m = new Matrix();

        Ico(String name, int color) {
            src = path(ICONS.containsKey(name) ? ICONS.get(name) : ICONS.get("info"));
            pt.setStyle(Paint.Style.STROKE);
            pt.setStrokeCap(Paint.Cap.ROUND);
            pt.setStrokeJoin(Paint.Join.ROUND);
            pt.setColor(color);
        }

        @Override protected void onBoundsChange(Rect b) {
            float s = b.width() / 24f;
            m.setScale(s, s);
            m.postTranslate(b.left, b.top);
            src.transform(m, scaled);
            pt.setStrokeWidth(Math.max(1.5f, 1.9f * s));
        }

        @Override public void draw(Canvas c) { c.drawPath(scaled, pt); }
        @Override public void setAlpha(int a) { pt.setAlpha(a); }
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    static ImageView icon(Context c, String name, int color, float size) {
        ImageView iv = new ImageView(c);
        iv.setImageDrawable(new Ico(name, color));
        iv.setLayoutParams(new LinearLayout.LayoutParams(L.s(size), L.s(size)));
        return iv;
    }

    // ------------------------------------------------------------------ chips & pills
    static LinearLayout pill(Context c, Theme t, String ico, TextView label) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(L.s(13), L.s(7), L.s(15), L.s(7));
        glass(l, t, t.panel, L.s(999));
        l.addView(icon(c, ico, t.text, 17));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.leftMargin = L.s(7);
        l.addView(label, lp);
        return l;
    }

    static LinearLayout chip(Context c, Theme t, String ico, String text, View.OnClickListener ok) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(L.s(16), L.s(11), L.s(18), L.s(11));
        float r = L.s(Math.min(t.radius, 16));
        glass(l, t, t.panel, r);
        l.addView(icon(c, ico, t.text, 20));
        TextView tv = one(c, text, 16, t.bold(), t.text);
        tv.setTag("label");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.leftMargin = L.s(9);
        l.addView(tv, lp);
        focus(l, t, 1.07f, r, null);
        l.setOnClickListener(ok);
        return l;
    }

    // ------------------------------------------------------------------ list rows
    /** A settings row: title + optional subtitle on the left, live value on the right. */
    static final class Row extends LinearLayout {
        final TextView value;
        Supplier<String> getter;

        Row(Context c, Theme t, String ico, String title, String sub, Supplier<String> getter) {
            super(c);
            this.getter = getter;
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(L.s(18), L.s(12), L.s(18), L.s(12));
            float r = L.s(Math.min(t.radius, 14));
            glass(this, t, Theme.alpha(t.panel, Math.min(255, ((t.panel >>> 24) + 40))), r);
            if (ico != null) {
                ImageView iv = icon(c, ico, t.dim, 22);
                LayoutParams ip = new LayoutParams(L.s(22), L.s(22));
                ip.rightMargin = L.s(14);
                addView(iv, ip);
            }
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(VERTICAL);
            col.addView(one(c, title, 17, t.bold(), t.text));
            if (sub != null && !sub.isEmpty()) {
                TextView s = tv(c, sub, 14, t.body(), t.dim);
                LayoutParams sp = new LayoutParams(WRAP, WRAP);
                sp.topMargin = L.s(3);
                col.addView(s, sp);
            }
            addView(col, new LayoutParams(0, WRAP, 1));
            value = one(c, "", 16, t.bold(), t.text);
            value.setGravity(Gravity.END);
            LayoutParams vp = new LayoutParams(WRAP, WRAP);
            vp.leftMargin = L.s(16);
            addView(value, vp);
            focus(this, t, 1.015f, r, null);
            refresh();
        }

        void refresh() {
            if (getter != null) {
                String v = getter.get();
                value.setText(v == null ? "" : (Lang.EN ? Tr.t(v) : v));
            }
        }
    }

    static Row action(Context c, Theme t, String ico, String title, String sub, Supplier<String> val, Runnable ok) {
        Row r = new Row(c, t, ico, title, sub, val);
        r.setOnClickListener(v -> { if (ok != null) ok.run(); r.refresh(); });
        return r;
    }

    static Row choice(Context c, Theme t, String ico, String title, String sub, String[] names, IntSupplier get, IntConsumer set) {
        Row r = new Row(c, t, ico, title, sub, () -> { String n = names[Math.max(0, Math.min(names.length - 1, get.getAsInt()))]; return "‹  " + (Lang.EN ? Tr.t(n) : n) + "  ›"; });
        Runnable step1 = () -> { set.accept((get.getAsInt() + 1) % names.length); r.refresh(); };
        r.setOnClickListener(v -> step1.run());
        r.setOnKeyListener((v, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT) { step1.run(); return true; }
            if (code == KeyEvent.KEYCODE_DPAD_LEFT) { set.accept((get.getAsInt() - 1 + names.length) % names.length); r.refresh(); return true; }
            return false;
        });
        return r;
    }

    static Row toggle(Context c, Theme t, String ico, String title, String sub, BooleanSupplier get, Consumer<Boolean> set) {
        Row r = new Row(c, t, ico, title, sub, () -> get.getAsBoolean() ? Lang.t("On", "An") : Lang.t("Off", "Aus"));
        r.setOnClickListener(v -> { set.accept(!get.getAsBoolean()); r.refresh(); });
        return r;
    }

    static TextView header(Context c, Theme t, String s) {
        TextView h = one(c, (Lang.EN ? Tr.t(s) : s).toUpperCase(), 13, t.bold(), t.accent);
        h.setLetterSpacing(0.12f);
        h.setPadding(L.s(6), L.s(18), 0, L.s(6));
        return h;
    }

    static FrameLayout.LayoutParams at(int w, int h, int gravity, float x, float y) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h, gravity);
        if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) lp.rightMargin = L.s(x); else lp.leftMargin = L.s(x);
        if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) lp.bottomMargin = L.s(y); else lp.topMargin = L.s(y);
        return lp;
    }
}
