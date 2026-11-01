package de.lumen.home;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** About Lumen + a plain-language privacy page: what it uses, and what it never does. */
public class AboutActivity extends Base {

    private String[][] topics() {
        return new String[][]{
                {"info", "Lumen",
                        Lang.t("Lumen is a free launcher by Soulride. Not for sale, no ads, no account.\nVersion " + L.VERSION,
                                "Lumen ist ein kostenloser Launcher von Soulride. Kein Verkauf, keine Werbung, kein Konto.\nVersion " + L.VERSION)},
                {"lock", Lang.t("No data collection", "Keine Datensammlung"),
                        Lang.t("Lumen has no server of its own and no tracking. It never sends your data to us or to anyone. "
                                        + "Your settings, favourites and the phone-remote PIN stay only on this device.",
                                "Lumen hat keinen eigenen Server und kein Tracking. Es sendet niemals deine Daten an uns oder an andere. "
                                        + "Deine Einstellungen, Favoriten und die Handy-PIN bleiben nur auf diesem Gerät.")},
                {"net", Lang.t("Internet services it uses", "Genutzte Internet-Dienste"),
                        Lang.t("Only when a feature actually needs it:\n"
                                        + "• Weather: open-meteo.com (and ip-api.com for a rough location if you set no city)\n"
                                        + "• Web wallpapers: the bing.com image archive — only if the background source is 'Web'\n"
                                        + "• Speed test: cloudflare.com — only when you start it\n"
                                        + "• Updates: only an address you enter yourself\n"
                                        + "No keys, no accounts, nothing that identifies you.",
                                "Nur wenn eine Funktion es wirklich braucht:\n"
                                        + "• Wetter: open-meteo.com (und ip-api.com für einen groben Standort, wenn du keine Stadt einträgst)\n"
                                        + "• Web-Hintergründe: das Bild-Archiv von bing.com — nur wenn die Hintergrund-Quelle 'Web' ist\n"
                                        + "• Speed-Test: cloudflare.com — nur wenn du ihn startest\n"
                                        + "• Updates: nur eine Adresse, die du selbst einträgst\n"
                                        + "Keine Schlüssel, keine Konten, nichts Persönliches.")},
                {"remote", Lang.t("Phone remote", "Handy-Fernbedienung"),
                        Lang.t("Runs only on your local Wi-Fi and is protected by a PIN. No cloud — nothing leaves your network.",
                                "Läuft nur in deinem lokalen WLAN und ist mit einer PIN geschützt. Keine Cloud — nichts verlässt dein Netzwerk.")},
                {"palette", Lang.t("Thanks to", "Danke an"),
                        Lang.t("Questrial font (SIL Open Font License). Weather by Open-Meteo. Built on Android open source.",
                                "Questrial-Schrift (SIL Open Font License). Wetter von Open-Meteo. Gebaut auf Android Open Source.")},
                {"info", "Soulride",
                        Lang.t("More from Soulride: scan the QR code on the home screen (Explore Soulride).",
                                "Mehr von Soulride: QR-Code auf der Startseite scannen (Explore Soulride).")},
        };
    }

    @Override void build() {
        TextView title = Ui.one(this, Lang.t("About & privacy", "Über & Datenschutz"), 30, t.head(), t.text);
        title.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        root.addView(title, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 36));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(L.s(56), L.s(8), L.s(56), L.s(40));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> list.invalidate());
        root.addView(sv, Ui.at(Ui.MATCH, Ui.MATCH, Gravity.TOP | Gravity.LEFT, 0, 92));

        for (String[] tp : topics()) {
            Ui.Row r = new Ui.Row(this, t, tp[0], tp[1], tp[2], null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
            lp.topMargin = L.s(9);
            list.addView(r, lp);
        }
        list.getChildAt(0).requestFocus();
    }
}
