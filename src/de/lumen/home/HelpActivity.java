package de.lumen.home;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Everything Lumen can do, in plain language (English or German). */
public class HelpActivity extends Base {

    private String[][] topics() {
        return new String[][]{
                {"remote", Lang.t("Remote", "Fernbedienung"),
                        Lang.t("Arrows: move  ·  OK: open  ·  Back: back\n"
                                        + "Hold OK or the Menu key on an app: options (move, remove, app info)\n"
                                        + "Home 1×: home screen  ·  Home 2× inside an app: back to the last app  ·  Home 2× on the home screen: Advanced\n"
                                        + "Mouse key: pointer (built in), handy for Brave",
                                "Pfeile: bewegen  ·  OK: öffnen  ·  Zurück: zurück\n"
                                        + "OK lange drücken oder Menü-Taste auf einer App: Optionen (verschieben, entfernen, App-Info)\n"
                                        + "Home 1×: Startseite  ·  Home 2× in einer App: zurück zur letzten App  ·  Home 2× auf der Startseite: Erweitert\n"
                                        + "Maus-Taste: Mauszeiger (eingebaut), praktisch für Brave")},
                {"home", Lang.t("Home screen", "Startseite"),
                        Lang.t("Top: clock, Wi-Fi speed, Bluetooth, free memory, temperature (orange = warm). A moon shows the sleep timer.\n"
                                        + "Middle: your favourites. Far right, the + adds one.\n"
                                        + "Bottom: HDMI, USB, Cast, All apps, Picture size, Cinema, Sleep, Themes, Advanced, Help.",
                                "Oben: Uhr, WLAN-Tempo, Bluetooth, freier Speicher, Temperatur (orange = warm). Ein Mond zeigt den Schlaf-Timer.\n"
                                        + "Mitte: deine Favoriten. Ganz rechts das + zum Hinzufügen.\n"
                                        + "Unten: HDMI, USB, Spiegeln, Alle Apps, Bildgröße, Kino, Schlaf, Themes, Erweitert, Hilfe.")},
                {"hdmi", Lang.t("Sources", "Quellen"),
                        Lang.t("HDMI: console, laptop, receiver.  USB: movies and photos from a stick.  Cast: an Android phone onto the screen via Miracast/Smart View. While casting the projector's Wi-Fi is paused; press Back to stop.",
                                "HDMI: Konsole, Laptop, Receiver.  USB: Filme und Fotos vom Stick.  Spiegeln: Android-Handy per Miracast/Smart View aufs Bild. Während Spiegeln ist das WLAN des Beamers pausiert; mit Zurück beenden.")},
                {"size", Lang.t("Picture size", "Bildgröße"),
                        Lang.t("The 'Picture size' button opens the projector's factory zoom. Beyond the factory limits: Advanced › Picture › Picture-size lab "
                                        + "(experimental, takes effect after a restart; 'Reset' puts everything back to the start).",
                                "Der Knopf 'Bildgröße' öffnet den Werks-Zoom des Beamers. Über die Werksgrenzen hinaus: Erweitert › Bild › Bildgröße-Labor "
                                        + "(experimentell, wirkt nach einem Neustart; 'Zurücksetzen' stellt alles wie am Anfang).")},
                {"film", Lang.t("Cinema mode", "Kino-Modus"),
                        Lang.t("One press: volume to your cinema preset, no interruptions, no screensaver, memory freed. Press again: everything as before.",
                                "Ein Druck: Lautstärke auf dein Kino-Preset, keine Störungen, kein Bildschirmschoner, Speicher wird frei gemacht. Noch ein Druck: alles wie vorher.")},
                {"moon", Lang.t("Sleep timer", "Schlaf-Timer"),
                        Lang.t("Choose 'Sleep' at the bottom: after 15 minutes to 2 hours the projector goes to standby. The time left shows at the top.",
                                "Unten 'Schlaf' wählen: nach 15 Minuten bis 2 Stunden geht der Beamer in Standby. Restzeit steht oben.")},
                {"phone", Lang.t("Phone as keyboard", "Handy als Tastatur"),
                        Lang.t("In your phone's browser open the address from Advanced › Phone as keyboard, enter the PIN. Typed text lands in the input field on the projector.",
                                "Im Handy-Browser die Adresse aus Erweitert › Handy als Tastatur öffnen, PIN eingeben. Getippter Text landet im Eingabefeld am Beamer.")},
                {"palette", "Themes",
                        Lang.t("10 looks: Glass Shelf, Midnight, Old Saga, Irish Pub, Country House, Glowing Forest, Nature, Northern Lights, Cinema, Zen. "
                                        + "Under 'Mixing' you combine font, focus effect, glass, photo darkening, corners and accent color.",
                                "10 Looks: Glass Shelf, Mitternacht, Alte Saga, Irish Pub, Landhaus, Leuchtwald, Natur, Nordlicht, Kinosaal, Zen. "
                                        + "Unter 'Mischen' kombinierst du Schrift, Fokus-Effekt, Glas, Foto-Abdunklung, Ecken und Akzentfarbe.")},
                {"gear", Lang.t("Advanced", "Erweitert"),
                        Lang.t("Everything in one place: Picture, Tweak Lab (animations, free memory, experiments), Network with ad blocker and speed test, "
                                        + "Bluetooth & sound, Apps & storage, Power, Remote, hidden menus, System info. In Advanced: Back jumps to the left list.",
                                "Alles an einem Ort: Bild, Tweak-Labor (Animationen, Speicher freigeben, Experimente), Netzwerk mit Werbeblocker und Speed-Test, "
                                        + "Bluetooth & Ton, Apps & Speicher, Energie, Fernbedienung, versteckte Menüs, System-Info. In Erweitert: Zurück springt zur linken Liste.")},
                {"net", "Browser",
                        Lang.t("Firefox: the nicest, blocks pop-ups and trackers; in the menu › Extensions add 'uBlock Origin' = the best ad blocker. "
                                        + "Brave: ad blocker built in. Both are phone versions: use the remote's mouse key. TV Bro: built for the remote, has its own cursor.",
                                "Firefox: am schönsten, blockt Pop-ups und Tracker; im Menü › Erweiterungen 'uBlock Origin' hinzufügen = bester Werbeblocker. "
                                        + "Brave: Werbeblocker eingebaut. Beide sind Handy-Versionen: Maus-Taste der Fernbedienung benutzen. TV Bro: für die Fernbedienung gebaut, hat eigenen Cursor.")},
        };
    }

    @Override void build() {
        TextView title = Ui.one(this, Lang.t("Help", "Hilfe"), 30, t.head(), t.text);
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
