package de.lumen.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

/** A look: colours, glass, type, corners and focus effect. Presets plus the user's mix-overrides. */
final class Theme {
    static final int RING = 0, GLOW = 1, LIFT = 2;

    final String id, name, mood;
    int panel, text, dim, accent, focus, border, bgTop, bgBottom;
    float radius, photoDim;
    String headFont, bodyFont;
    int focusStyle;
    boolean light;

    Theme(String id, String name, String mood, int panel, int text, int dim, int accent, int focus, int border,
          float radius, float photoDim, String head, String body, int focusStyle, int bgTop, int bgBottom) {
        this.id = id; this.name = name; this.mood = mood; this.panel = panel; this.text = text; this.dim = dim;
        this.accent = accent; this.focus = focus; this.border = border; this.radius = radius; this.photoDim = photoDim;
        this.headFont = head; this.bodyFont = body; this.focusStyle = focusStyle; this.bgTop = bgTop; this.bgBottom = bgBottom;
        this.light = brightness(panel) > 150;
    }

    Theme copy() {
        return new Theme(id, name, mood, panel, text, dim, accent, focus, border, radius, photoDim,
                headFont, bodyFont, focusStyle, bgTop, bgBottom);
    }

    Typeface head() { return Typeface.create(headFont, Typeface.NORMAL); }
    Typeface body() { return Typeface.create(bodyFont, Typeface.NORMAL); }
    Typeface bold() { return Typeface.create(bodyFont.equals("sans-serif-light") ? "sans-serif-medium" : bodyFont, Typeface.BOLD); }

    static final String LIGHT = "sans-serif-light", SANS = "sans-serif", MED = "sans-serif-medium",
            SERIF = "serif", COND = "sans-serif-condensed";

    static final Theme[] ALL = {
            new Theme("glass", "Glass Shelf", "Klar, modern, Milchglas",
                    0x6E0F1A1A, 0xFFFFFFFF, 0xD9FFFFFF, 0xFF8FD3C1, 0xFFFFFFFF, 0x38FFFFFF,
                    18, 0.12f, LIGHT, SANS, RING, 0xFF22383A, 0xFF0E1A1B),
            new Theme("midnight", "Mitternacht", "Dark Mode, ruhig, kontrastreich",
                    0xD90B0D10, 0xFFE8EAED, 0xFFA8AEB5, 0xFF8AB4F8, 0xFF8AB4F8, 0x24FFFFFF,
                    14, 0.45f, LIGHT, SANS, GLOW, 0xFF14171C, 0xFF050607),
            new Theme("saga", "Alte Saga", "Pergament, Gold, Fantasy-Epos",
                    0xBF1C140C, 0xFFF3E6C4, 0xFFD2BF92, 0xFFD4AF37, 0xFFD4AF37, 0x66D4AF37,
                    6, 0.25f, SERIF, SERIF, RING, 0xFF3A2A16, 0xFF120C06),
            new Theme("pub", "Irish Pub", "Dunkles Holz, Messing, gemütlich",
                    0xC20F2A1E, 0xFFF5EBD7, 0xFFD6C9AC, 0xFFC9A24A, 0xFFC9A24A, 0x70C9A24A,
                    10, 0.30f, SERIF, SANS, GLOW, 0xFF2B1A0E, 0xFF0B140F),
            new Theme("landhaus", "Landhaus", "Leinen, Salbei, hell und warm",
                    0xE0F4EEE2, 0xFF3B2F25, 0xFF66574A, 0xFF6F8C62, 0xFF6F8C62, 0x403B2F25,
                    16, 0.0f, SERIF, SANS, RING, 0xFFD9CBB0, 0xFFA89676),
            new Theme("glow", "Leuchtwald", "Biolumineszenz, Türkis-Glühen",
                    0xA6061A2A, 0xFFE6FBFF, 0xFFA9DDEC, 0xFF4FE3FF, 0xFF4FE3FF, 0x704FE3FF,
                    22, 0.35f, LIGHT, SANS, GLOW, 0xFF07263A, 0xFF02080F),
            new Theme("nature", "Natur", "Wald, Moos, frische Luft",
                    0x8C1E3320, 0xFFF4F7EE, 0xFFD3DFC8, 0xFFA8C686, 0xFFF4F7EE, 0x40F4F7EE,
                    20, 0.10f, LIGHT, SANS, RING, 0xFF2E4A2F, 0xFF101C11),
            new Theme("aurora", "Nordlicht", "Eisblau mit Polarlicht-Akzent",
                    0x85122033, 0xFFFFFFFF, 0xFFCBD7EA, 0xFF7CF5C4, 0xFF7CF5C4, 0x507CF5C4,
                    24, 0.20f, COND, COND, GLOW, 0xFF16263E, 0xFF060C16),
            new Theme("cinema", "Kinosaal", "Samtrot, Gold, Premiere",
                    0xE00A0505, 0xFFF7EFE6, 0xFFC7B3AA, 0xFFB3121B, 0xFFE3B341, 0x55E3B341,
                    8, 0.50f, SERIF, SANS, LIFT, 0xFF2A0606, 0xFF060202),
            new Theme("zen", "Zen", "Reispapier, Tusche, minimal",
                    0xEBFAFAF7, 0xFF1E1E1C, 0xFF5A5A55, 0xFFC0392B, 0xFF1E1E1C, 0x301E1E1C,
                    2, 0.0f, LIGHT, SANS, RING, 0xFFE9E6DF, 0xFFBDB8AC),
    };

    // Mix options (index 0 = keep the preset's own value)
    static final String[] FONT_NAMES = {"Wie Theme", "Modern", "Klassisch (Serif)", "Schmal", "Kräftig"};
    static final String[] FONT_VALS = {null, LIGHT, SERIF, COND, MED};
    static final String[] FOCUS_NAMES = {"Wie Theme", "Ring", "Leuchten", "Anheben"};
    static final String[] GLASS_NAMES = {"Wie Theme", "Fast klar", "Sehr transparent", "Transparent", "Leicht", "Mittel", "Kräftig", "Satt", "Fast voll"};
    static final int[] GLASS_ALPHA = {-1, 0x1C, 0x34, 0x50, 0x70, 0x9A, 0xBE, 0xDA, 0xF2};
    static final String[] DIM_NAMES = {"Wie Theme", "Kein", "Leicht", "Mittel", "Stark"};
    static final float[] DIM_VALS = {-1, 0f, 0.15f, 0.35f, 0.55f};
    static final String[] CORNER_NAMES = {"Wie Theme", "Eckig", "Sanft", "Rund", "Sehr rund"};
    static final float[] CORNER_VALS = {-1, 2, 10, 18, 28};
    static final String[] ACCENT_NAMES = {"Wie Theme", "Mint", "Gold", "Türkis", "Rubin", "Lavendel", "Bernstein", "Weiß"};
    static final int[] ACCENT_VALS = {0, 0xFF8FD3C1, 0xFFD4AF37, 0xFF4FE3FF, 0xFFE0475A, 0xFFB9A3FF, 0xFFF0A040, 0xFFFFFFFF};

    static Theme byId(String id) {
        for (Theme t : ALL) if (t.id.equals(id)) return t;
        return ALL[0];
    }

    static Theme current(Context c) { return build(c, L.p(c).getString("theme", "glass")); }

    /** Preset + mix overrides from prefs. */
    static Theme build(Context c, String id) {
        SharedPreferences p = L.p(c);
        Theme t = byId(id).copy();
        int f = p.getInt("mixFont", 0);
        if (f > 0) { t.headFont = FONT_VALS[f]; t.bodyFont = f == 1 ? SANS : FONT_VALS[f]; }
        int fo = p.getInt("mixFocus", 0);
        if (fo > 0) t.focusStyle = fo - 1;
        int g = p.getInt("mixGlass", 0);
        if (g > 0) t.panel = (GLASS_ALPHA[g] << 24) | (t.panel & 0xFFFFFF);
        int d = p.getInt("mixDim", 0);
        if (d > 0) t.photoDim = DIM_VALS[d];
        int r = p.getInt("mixCorner", 0);
        if (r > 0) t.radius = CORNER_VALS[r];
        int a = p.getInt("mixAccent", 0);
        if (a > 0) { t.accent = ACCENT_VALS[a]; if (t.focus != t.text) t.focus = ACCENT_VALS[a]; }
        return t;
    }

    /** Signature that changes whenever the look changes (activities recreate on change). */
    static String signature(Context c) {
        SharedPreferences p = L.p(c);
        return p.getString("theme", "glass") + p.getInt("mixFont", 0) + p.getInt("mixFocus", 0) + p.getInt("mixGlass", 0)
                + p.getInt("mixDim", 0) + p.getInt("mixCorner", 0) + p.getInt("mixAccent", 0);
    }

    static int brightness(int c) {
        return (((c >> 16) & 0xFF) * 299 + ((c >> 8) & 0xFF) * 587 + (c & 0xFF) * 114) / 1000;
    }

    static int alpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }
}
