package de.lumen.home;

import android.content.Context;

import java.util.Locale;

/**
 * Lightweight English/German strings. Android does not auto-translate an app's own text, so Lumen ships both
 * and picks one: setting "lang" = "auto" (follow the Android system language), "en" or "de".
 */
final class Lang {
    private Lang() {}

    static boolean EN = true;

    static final String[] MODE_NAMES = {"Automatisch (System)", "English", "Deutsch"};
    static final String[] MODE_VALS = {"auto", "en", "de"};

    static void init(Context c) {
        String m = L.p(c).getString("lang", "auto");
        if (m.equals("en")) EN = true;
        else if (m.equals("de")) EN = false;
        else EN = !Locale.getDefault().getLanguage().equals("de"); // auto: German only when the system is German
    }

    static int modeIndex(Context c) {
        String m = L.p(c).getString("lang", "auto");
        for (int i = 0; i < MODE_VALS.length; i++) if (MODE_VALS[i].equals(m)) return i;
        return 0;
    }

    /** Pick the English or German variant. */
    static String t(String en, String de) { return EN ? en : de; }
}
