package de.lumen.home;

import java.util.HashMap;
import java.util.Map;

/**
 * German→English dictionary. In English mode, Ui.tv() runs every displayed string through Tr.t(), so static
 * German labels become English without touching call sites. Unknown strings (names, numbers, already-English
 * text) pass through unchanged.
 */
final class Tr {
    private Tr() {}

    static String t(String s) {
        if (s == null) return null;
        String en = MAP.get(s);
        return en != null ? en : s;
    }

    private static final Map<String, String> MAP = new HashMap<>();
    private static void p(String de, String en) { MAP.put(de, en); }
    static {
        // sections
        p("Bild & Beamer", "Picture & projector"); p("Tweak-Labor", "Tweak Lab");
        p("Netzwerk & Privatsphäre", "Network & privacy"); p("Bluetooth & Ton", "Bluetooth & sound");
        p("Apps & Speicher", "Apps & storage"); p("Energie & Schlaf", "Power & sleep");
        p("Fernbedienung", "Remote"); p("Handy als Tastatur", "Phone as keyboard");
        p("Aussehen & Fotos", "Appearance & photos"); p("Versteckte Menüs", "Hidden menus");
        p("Sichern & Wiederherstellen", "Backup & restore"); p("System-Info", "System info");
        p("Hilfe", "Help"); p("Erweitert", "Advanced"); p("Kategorien", "Categories");
        // common
        p("Abbrechen", "Cancel"); p("An", "On"); p("Aus", "Off"); p("Start", "Start");
        p("Neu suchen", "Rescan"); p("Zurücksetzen", "Reset"); p("Standard", "Default");
        p("Öffnen", "Open"); p("Öffnen mit", "Open with"); p("Öffnen mit …", "Open with …");
        p("Löschen", "Delete"); p("Löschen?", "Delete?"); p("Umbenennen", "Rename"); p("Details", "Details");
        p("Verschieben", "Move"); p("Verschieben nach", "Move to"); p("Kopieren", "Copy");
        p("Nach links", "Move left"); p("Nach rechts", "Move right"); p("Nach oben", "Move up"); p("Nach unten", "Move down");
        p("Neuer Name", "New name"); p("Neue Kategorie", "New category"); p("Name der Kategorie", "Category name");
        p("Übernehmen", "Apply"); p("Schließen", "Close"); p("Wiederherstellen", "Restore"); p("Wiederherstellen?", "Restore?");
        p("Fehler", "Error"); p("Fehlgeschlagen", "Failed"); p("fertig", "done"); p("gesetzt", "set"); p("nicht gesetzt", "not set");
        p("Leer", "Empty"); p("Ordner", "Folder"); p("Ordner nicht lesbar", "Folder not readable");
        p("Optionen", "Options"); p("Jetzt", "Now"); p("Direkt", "Direct"); p("Gerät", "Device"); p("Leistung", "Performance"); p("Netz", "Network"); p("Verbindung", "Connection");
        p("Fehlende Berechtigung", "Missing permission");
        p("Fehlende Berechtigung (WRITE_SECURE_SETTINGS)", "Missing permission (WRITE_SECURE_SETTINGS)");
        p("Fehlende Berechtigung (WRITE_SETTINGS)", "Missing permission (WRITE_SETTINGS)");
        p("Keine Berechtigung", "No permission");
        // picture section
        p("Bild", "Picture"); p("Bildgröße", "Picture size"); p("Bildgröße (Werk)", "Picture size (factory)");
        p("Bildgröße frei (über Werksgrenzen)", "Free picture size (beyond factory)"); p("Bildgröße-Labor", "Picture-size lab");
        p("Zoom des Beamers, innerhalb der Werksgrenzen", "Projector zoom, within factory limits");
        p("Größer oder kleiner als das Werk erlaubt, sofort", "Bigger or smaller than factory allows, instantly");
        p("Größer oder kleiner als das Werk erlaubt, wirkt nach Neustart", "Bigger or smaller than factory allows, after a restart");
        p("Trapez & Projektion", "Keystone & projection");
        p("Werks-Menü: Trapezkorrektur, Spiegeln, Decke/Rückprojektion", "Factory menu: keystone, flip, ceiling/rear");
        p("Bildeinstellungen", "Picture settings"); p("Helligkeit, Kontrast, Farbe, Bildmodus", "Brightness, contrast, color, mode");
        p("Werks-Beamer-Menü", "Factory projector menu"); p("Das Original-Einstellungsmenü des HY300", "The original HY300 settings menu");
        p("Bildschirmschoner", "Screensaver"); p("Bildschirmschoner nach", "Screensaver after"); p("Zeit ohne Tastendruck", "Time with no key press");
        p("Untertitel", "Subtitles"); p("Untertitel (systemweit)", "Subtitles (system-wide)");
        p("Captions in Apps anzeigen, wenn verfügbar", "Show captions in apps when available");
        p("Untertitel-Größe", "Subtitle size"); p("Untertitel-Stil (System)", "Subtitle style (system)"); p("Farbe, Rand, Schrift", "Color, outline, font");
        p("Werk-Min", "Factory min"); p("Werk-Max", "Factory max"); p("Werk-Min merken", "Learn factory min"); p("Werk-Max merken", "Learn factory max");
        p("Werks-Zoom", "Factory zoom"); p("Neu starten", "Restart"); p("Beamer sofort neu starten", "Restart the projector now");
        p("Diese Größe für Apps", "This size for apps"); p("Diese Größe für HDMI", "This size for HDMI");
        p("Für Apps/Launcher gemerkt", "Saved for apps/launcher"); p("Für HDMI gemerkt – wird beim Umschalten gesetzt", "Saved for HDMI – applied on switch");
        p("Größe: ", "Size: "); p("größer", "bigger"); p("kleiner", "smaller"); p("Gemerkt: ", "Saved: ");
        p("Automatisch auf HDMI", "Auto-switch to HDMI");
        p("Beim Einstecken eines Geräts umschalten (experimentell, nur von der Startseite aus)", "Switch when a device is plugged in (experimental, from the home screen only)");
        // tweak lab
        p("Live", "Live"); p("Freier Arbeitsspeicher", "Free memory"); p("Temperatur CPU / GPU", "Temperature CPU / GPU");
        p("CPU-Takt (aktuell / max)", "CPU clock (current / max)"); p("Tempo", "Speed");
        p("Animationen (ganzes System)", "Animations (whole system)"); p("Turbo = Menüs fühlen sich schneller an", "Turbo = menus feel faster");
        p("Speicher jetzt freigeben", "Free memory now"); p("Schließt Apps, die gerade nicht laufen", "Closes apps you are not using");
        p("Lumen-Animationen", "Lumen animations"); p("Zoom-Effekt beim Auswählen", "Zoom effect on select");
        p("Fotos laden nach Start", "Load photos after start"); p("Später = Startseite ist schneller da", "Later = home screen appears faster");
        p("Experimente", "Experiments"); p("Oberflächen-Größe (Dichte)", "UI size (density)"); p("Wirkt nach Neustart. Standard = 240", "After restart. Default = 240");
        p("Temperatur-Warnung ab", "Temperature warning at"); p("Status-Anzeige wird orange", "Status turns orange");
        p("Kino-Modus Lautstärke", "Cinema mode volume"); p("Wird beim Einschalten gesetzt", "Set when enabled");
        p("Entwickleroptionen", "Developer options"); p("Alle Android-Profi-Schalter", "All Android pro switches");
        // readability / language
        p("Sprache / Language", "Language"); p("Sprache", "Language"); p("Automatisch folgt der Android-Sprache", "Automatic follows the Android language");
        p("Automatisch (System)", "Automatic (system)"); p("Deutsch", "Deutsch"); p("English", "English");
        p("Lesbarkeit", "Readability"); p("Aussehen", "Appearance");
        p("Fette Schrift (Lumen)", "Bold text (Lumen)"); p("Dickere Schrift überall in Lumen", "Thicker text everywhere in Lumen");
        p("Hoher Kontrast (System)", "High contrast (system)"); p("Umrandet Text in Apps für schwache Displays", "Outlines text in apps for weak displays");
        p("Oberflächen-Größe", "UI size"); p("Größere Menüs & Schrift (nach Neustart)", "Bigger menus & text (after restart)");
        p("Kompakt (200)", "Compact (200)"); p("Klein (220)", "Small (220)"); p("Standard (240)", "Default (240)"); p("Groß (260)", "Large (260)"); p("Sehr groß (280)", "Very large (280)");
        p("Klein", "Small"); p("Normal", "Normal"); p("Groß", "Large"); p("Sehr groß", "Very large");
        p("Langsam", "Slow"); p("Schnell", "Fast"); p("Sehr schnell", "Very fast");
        // network
        p("Netzwerk", "Network"); p("Netzwerk-Dashboard", "Network dashboard");
        p("Live-Datenrate, wer ins Internet funkt, Daten sperren", "Live data rate, who's phoning home, block data");
        p("WLAN", "Wi-Fi"); p("WLAN wählen & verbinden", "Choose & connect Wi-Fi"); p("Lumen-Netzwerkliste", "Lumen network list");
        p("WLAN-Einstellungen (System)", "Wi-Fi settings (system)"); p("IP-Adresse", "IP address");
        p("Internet-Geschwindigkeit testen", "Test internet speed"); p("Lädt 25 MB von Cloudflare", "Downloads 25 MB from Cloudflare");
        p("Privatsphäre", "Privacy"); p("Werbe- & Tracker-Blocker", "Ad & tracker blocker");
        p("Sperrt Werbung und Tracker für ALLE Apps (Privates DNS)", "Blocks ads and trackers for ALL apps (Private DNS)");
        p("Standort", "Location"); p("Sicherheit & Einschränkungen", "Security & restrictions"); p("ADB über WLAN (Port 5555)", "ADB over Wi-Fi (port 5555)");
        p("Aus / unbekannt", "Off / unknown"); p("Apps online", "Apps online"); p("gerade keine", "none right now");
        p("Daten sperren (Hintergrund)", "Block data (background)"); p("Im Hintergrund schließen", "Close in background");
        p("App-Info (stoppen, Rechte)", "App info (stop, permissions)"); p("Geschlossen", "Closed");
        p("Hintergrunddaten gesperrt", "Background data restricted"); p("Nicht verfügbar", "Not available"); p("Voller Block braucht Root", "Full block needs root");
        p("AdGuard (Werbung + Tracker)", "AdGuard (ads + trackers)"); p("Mullvad (Werbung + Tracker)", "Mullvad (ads + trackers)");
        p("Cloudflare (nur schnell)", "Cloudflare (just fast)"); p("Quad9 (Schutz vor Schadseiten)", "Quad9 (malware protection)");
        // wifi/bt
        p("Suche Netzwerke …", "Scanning …"); p("Nicht verbunden", "Not connected"); p("Verbunden: ", "Connected: ");
        p("Passwort für ", "Password for "); p("Verbinde mit ", "Connecting to "); p("Verbindung fehlgeschlagen", "Could not connect");
        p("Trennen / vergessen", "Disconnect / forget"); p("verbunden", "connected");
        p("Bluetooth-Geräte verbinden", "Connect Bluetooth devices"); p("Lumen: koppeln & verbinden", "Lumen: pair & connect");
        p("Bluetooth-Einstellungen (System)", "Bluetooth settings (system)");
        p("Suche Geräte …", "Searching …"); p("Gerät antippen zum Verbinden oder Koppeln", "Tap a device to connect or pair");
        p("Gekoppelt", "Paired"); p("In der Nähe", "Nearby"); p("gekoppelt", "paired"); p("Koppeln …", "Pairing …");
        p("Verbinden", "Connect"); p("Trennen", "Disconnect"); p("Entkoppeln", "Unpair");
        p("Audio-Profil nicht bereit", "Audio profile not ready"); p("Verbindung nicht möglich", "Could not change connection"); p("Kein Bluetooth", "No Bluetooth");
        // bluetooth section
        p("Bluetooth", "Bluetooth"); p("Verbunden", "Connected"); p("Neues Gerät koppeln", "Pair a new device");
        p("Ton", "Sound"); p("Lautstärke", "Volume"); p("Links / rechts zum Ändern", "Left / right to change"); p("Ton-Einstellungen", "Sound settings"); p("Ausgabe, Format, Effekte", "Output, format, effects");
        // apps & storage
        p("Standard-Apps", "Default apps"); p("Browser, Startbildschirm und mehr festlegen", "Set browser, home screen and more");
        p("Startbildschirm-App wählen", "Choose home-screen app"); p("Alle Apps verwalten", "Manage all apps"); p("Stoppen, Speicher leeren, Rechte", "Stop, clear storage, permissions");
        p("Unbekannte Quellen", "Unknown sources"); p("Welche App darf APKs installieren", "Which app may install APKs");
        p("Speicher", "Storage"); p("Interner Speicher", "Internal storage"); p("Foto-Cache von Lumen leeren", "Clear Lumen photo cache");
        p("Speicher-Analyse", "Storage analysis"); p("Größte Apps anzeigen (OK auf eine App = App-Info)", "Show largest apps (OK on an app = app info)");
        // power
        p("Schlaf", "Sleep"); p("Schlaf-Timer", "Sleep timer"); p("Beamer geht danach in Standby", "Projector goes to standby after");
        p("Kino-Modus", "Cinema mode"); p("Ton-Preset, keine Störungen, kein Bildschirmschoner", "Sound preset, no interruptions, no screensaver");
        p("Standby jetzt", "Standby now"); p("Bild aus, Beamer schläft", "Picture off, projector sleeps");
        p("Ein/Aus-Menü", "Power menu"); p("Neustart oder Ausschalten", "Restart or power off"); p("Tastenassistent ist aus", "Key helper is off");
        p("Bitte über die Ein/Aus-Taste neu starten", "Please restart with the power button");
        // remote
        p("Tastenassistent", "Key helper"); p("Lumen-Tastenassistent", "Lumen key helper"); p("Aktiv", "Active"); p("Normale App", "Normal app");
        p("Bedienungshilfen öffnen", "Open accessibility"); p("Hier lässt sich der Assistent an/aus schalten", "Turn the helper on/off here");
        p("Eigene Tasten", "Custom buttons"); p("Home 2× in einer App", "Home 2× inside an app"); p("Springt zur zuletzt benutzten App zurück", "Jumps back to the last app");
        p("Home 2× auf der Startseite", "Home 2× on the home screen"); p("Öffnet dieses Erweitert-Menü", "Opens this Advanced menu");
        p("Home 2× drücken", "Press Home 2×"); p("Öffnet dieses Menü (Erweitert)", "Opens this menu (Advanced)");
        p("Datenschutz & Info", "Privacy & info"); p("Über Lumen & Datenschutz", "About Lumen & privacy");
        p("Was Lumen nutzt · keine Datensammlung · kein Server", "What Lumen uses · no data collection · no server");
        p("Home lang drücken", "Home long press"); p("von der Firmware gesperrt", "blocked by the firmware");
        p("Gut zu wissen", "Good to know"); p("Maus", "Mouse"); p("Maus-Taste", "Mouse button"); p("eingebaut in der Fernbedienung", "built into the remote");
        p("Mauszeiger-Tempo", "Pointer speed"); p("Schneller fühlt sich oft flüssiger an", "Faster often feels smoother");
        p("Menü-Taste auf einer App", "Menu key on an app"); p("Zurück in Erweitert", "Back in Advanced"); p("springt zur Liste links", "jumps to the list on the left");
        p("Assistent aktiv halten", "Keep helper active"); p("Die Firmware schaltet ihn beim App-Start ab; Lumen schaltet ihn wieder an", "The firmware turns it off on app start; Lumen turns it back on");
        // phone keyboard
        p("Mit dem Handy tippen", "Type with your phone"); p("Handy-Tastatur", "Phone keyboard");
        p("Webseite im WLAN, kein App-Download nötig", "A web page on Wi-Fi, no app to install");
        p("Am Handy öffnen", "Open on your phone"); p("Neue PIN erzeugen", "Generate a new PIN");
        // appearance / photos
        p("Hintergrund-Quelle", "Background source"); p("Hintergründe", "Backgrounds");
        p("Soulride = deine Bilder · Web = Natur aus dem Netz · Eigene = dein USB-Ordner", "Soulride = your photos · Web = nature from the net · Custom = your USB folder");
        p("Web-Fotos (Natur)", "Web photos (nature)"); p("Eigene (USB)", "Custom (USB)");
        p("Hintergrund-Fotos", "Background photos"); p("Neues Foto alle", "New photo every"); p("Fotos in Full-HD", "Photos in Full HD");
        p("Schärfer, braucht etwas mehr Speicher", "Sharper, uses a bit more storage"); p("Nächstes Foto jetzt", "Next photo now"); p("Fotos im Speicher", "Photos in storage");
        p("Bildschirmschoner & Wetter", "Screensaver & weather"); p("Lumen-Bildschirmschoner", "Lumen screensaver"); p("Foto + Uhr statt Werks-Schoner", "Photo + clock instead of the factory one");
        p("Wetter-Ort", "Weather location"); p("Leer = automatisch per Internet", "Empty = automatic via internet"); p("Stadt (leer = automatisch)", "City (empty = automatic)");
        // hidden
        p("Android-Einstellungen (komplett)", "Android settings (full)"); p("Allwinner-Manager", "Allwinner manager");
        p("Versteckte Chip-Einstellungen", "Hidden chip settings"); p("Geräte-Status", "Device status"); p("Seriennummer, Laufzeit, Adressen", "Serial, uptime, addresses");
        p("Bedienungshilfen", "Accessibility"); p("Datum & Uhrzeit", "Date & time"); p("Tastatur", "Keyboard");
        p("Alle Einstellungs-Seiten (Experte)", "All settings pages (expert)");
        // backup
        p("Sichern", "Backup"); p("In internen Speicher sichern", "Back up to internal storage"); p("Auf USB sichern: ", "Back up to USB: ");
        p("Keine Sicherung gefunden", "No backup found"); p("Gesichert nach /Lumen/", "Saved to /Lumen/"); p("Auf USB gesichert", "Saved to USB");
        p("Ja, Einstellungen laden", "Yes, load settings"); p("Geladen – wird angewendet", "Loaded – applying");
        // system-info / updates
        p("Lumen & Updates", "Lumen & updates"); p("Lumen-Version", "Lumen version"); p("Nach Updates suchen", "Check for updates");
        p("Lädt aus deiner Update-Quelle", "Downloads from your update source"); p("Update-Adresse", "Update URL");
        p("URL zu version.json, das du selbst hostest", "URL to a version.json you host yourself"); p("Update-URL (JSON)", "Update URL (JSON)");
        p("Erst eine Update-Adresse eintragen", "Set an update URL first"); p("Update fehlgeschlagen", "Update check failed");
        p("Modell", "Model"); p("Arbeitsspeicher", "Memory"); p("Läuft seit", "Up for"); p("Lumen-Systemrechte", "Lumen system rights"); p("Aktiv (Live-Zoom, Neustart)", "Active (live zoom, restart)");
        // home chips / hints / menus
        p("Alle Apps", "All apps"); p("Spiegeln", "Cast"); p("Dateien", "Files"); p("Themes", "Themes"); p("Modus", "Mode"); p("Aufräumen", "Clean up");
        p("Modus wählen", "Choose mode"); p("Film", "Movie"); p("Spiele", "Games"); p("Heller Raum", "Bright room");
        p("Schließe Hintergrund-Apps …", "Closing background apps …"); p("Aufgeräumt", "Cleaned up");
        p("Film-Modus: Ton 60 %, keine Störungen, kein Bildschirmschoner", "Movie mode: sound 60 %, no interruptions, no screensaver");
        p("Spiele-Modus: Ton 70 %, schnelle Animationen, kein Bildschirmschoner", "Game mode: sound 70 %, fast animations, no screensaver");
        p("Heller Raum: Helligkeit hoch – Feineinstellung im Bildmenü", "Bright room: brightness up – fine-tune in the picture menu");
        p("Standard-Modus", "Default mode"); p("Kino-Modus aus", "Cinema mode off"); p("Kino an", "Cinema on");
        p("ZULETZT", "RECENT"); p("Favoriten", "Favourites"); p("Streaming", "Streaming"); p("Spiele Streams", "Game streams"); p("Apps", "Apps");
        p("Handy spiegeln (Miracast)", "Screen mirroring (Miracast)");
        p("Starten – WLAN pausiert, bis du mit Zurück beendest", "Start – Wi-Fi pauses until you press Back");
        p("Aus Favoriten entfernen", "Remove from favourites"); p("In Kategorie verschieben …", "Move to category …");
        p("App-Info (Stopp, Speicher, Rechte)", "App info (stop, storage, permissions)"); p("Kategorien verwalten …", "Manage categories …");
        p("Zu Favoriten hinzufügen", "Add to favourites"); p("Deinstallieren", "Uninstall");
        p("App zu Favoriten hinzufügen", "Add app to favourites"); p("App nicht gefunden", "App not found"); p("Lässt sich nicht öffnen", "Can't open"); p("Keine App dafür", "No app for this");
        p("Alle Apps", "All apps"); p("Entfernt", "Removed"); p("Hinzugefügt zu ", "Added to ");
        p("Noch keine letzte App", "No last app yet"); p("Keine andere Kategorie – erst eine anlegen (Kategorien verwalten)", "No other category – create one first (Manage categories)");
        p("Löschen (Apps gehen in die erste Kategorie)", "Delete (apps move to the first category)"); p("Mindestens eine Kategorie muss bleiben", "At least one category must remain");
        p("OK: Optionen (umbenennen, nach oben/unten, löschen)", "OK: options (rename, up/down, delete)");
        // files
        p("Ordner", "Folders"); p("Download", "Download"); p("Filme", "Movies"); p("Bilder", "Pictures"); p("Musik", "Music");
        p("Hierher kopieren: ", "Copy here: "); p("Hierher verschieben: ", "Move here: "); p("OK einfügen  ·  Menü: abbrechen", "OK to paste  ·  Menu: cancel");
        p("Kopiert – Zielordner öffnen, dann einfügen", "Copied – open the target folder, then paste"); p("Zum Verschieben – Zielordner öffnen, dann einfügen", "To move – open the target folder, then paste");
        p("Kopiere …", "Copying …"); p("Verschiebe …", "Moving …"); p("Gibt es schon", "Already exists");
        p("Konnte nicht löschen", "Could not delete"); p("Konnte nicht umbenennen", "Could not rename"); p("Umbenannt", "Renamed"); p("Gelöscht", "Deleted");
        // themes
        p("Themes & Mischen", "Themes & mixing"); p("10 Looks, dazu Schrift, Glas, Ecken, Akzent frei mischen", "10 looks, plus font, glass, corners, accent to mix freely"); p("Auswählen zeigt die Vorschau  ·  OK übernimmt  ·  unten frei mischen", "Select to preview  ·  OK applies  ·  mix freely below");
        p("Looks", "Looks"); p("Mischen (gilt für jedes Theme)", "Mixing (applies to every theme)"); p("Mischung zurücksetzen", "Reset mix"); p("Alles wieder wie im Theme", "Everything back to the theme");
        p("✓ aktiv", "✓ active"); p("Wie Theme", "Like theme");
        p("Schrift", "Font"); p("Modern", "Modern"); p("Klassisch (Serif)", "Classic (serif)"); p("Schmal", "Condensed"); p("Kräftig", "Bold");
        p("Fokus-Effekt", "Focus effect"); p("Ring", "Ring"); p("Leuchten", "Glow"); p("Anheben", "Lift");
        p("Glas-Stärke", "Glass strength"); p("Fast klar", "Almost clear"); p("Sehr transparent", "Very transparent"); p("Transparent", "Transparent"); p("Leicht", "Light"); p("Mittel", "Medium"); p("Satt", "Rich"); p("Fast voll", "Almost solid");
        p("Foto abdunkeln", "Darken photo"); p("Kein", "None"); p("Stark", "Strong");
        p("Ecken", "Corners"); p("Eckig", "Square"); p("Sanft", "Soft"); p("Rund", "Round"); p("Sehr rund", "Very round");
        p("Akzentfarbe", "Accent color"); p("Mint", "Mint"); p("Gold", "Gold"); p("Türkis", "Teal"); p("Rubin", "Ruby"); p("Lavendel", "Lavender"); p("Bernstein", "Amber"); p("Weiß", "White");
        // theme names + moods
        p("Mitternacht", "Midnight"); p("Alte Saga", "Old Saga"); p("Landhaus", "Country House"); p("Leuchtwald", "Glowing Forest");
        p("Natur", "Nature"); p("Nordlicht", "Northern Lights"); p("Kinosaal", "Cinema"); p("Zen", "Zen");
        p("Klar, modern, Milchglas", "Clean, modern, frosted glass"); p("Dark Mode, ruhig, kontrastreich", "Dark mode, calm, high contrast");
        p("Pergament, Gold, Fantasy-Epos", "Parchment, gold, fantasy epic"); p("Dunkles Holz, Messing, gemütlich", "Dark wood, brass, cozy");
        p("Leinen, Salbei, hell und warm", "Linen, sage, light and warm"); p("Biolumineszenz, Türkis-Glühen", "Bioluminescence, teal glow");
        p("Wald, Moos, frische Luft", "Forest, moss, fresh air"); p("Eisblau mit Polarlicht-Akzent", "Ice blue with aurora accent");
        p("Samtrot, Gold, Premiere", "Velvet red, gold, premiere"); p("Reispapier, Tusche, minimal", "Rice paper, ink, minimal");
        // status
        p("Offline", "Offline"); p("Kabel-LAN", "Wired LAN"); p("nicht verbunden", "not connected"); p("verbunden", "connected");
        p("bewölkt", "cloudy"); p("heiter", "fair"); p("klar", "clear"); p("Nebel", "fog"); p("Niesel", "drizzle"); p("Regen", "rain");
        p("Schnee", "snow"); p("Schauer", "showers"); p("Schneeschauer", "snow showers"); p("Gewitter", "thunderstorm");
        // name arrays (choices)
        p("1 Std", "1 hr"); p("1,5 Std", "1.5 hr"); p("2 Std", "2 hr"); p("Nie (4 Std)", "Never (4 hr)");
        p("Turbo 0,5×", "Turbo 0.5×"); p("Normal 1×", "Normal 1×"); p("Gemütlich 1,5×", "Relaxed 1.5×");
        // hints
        p("OK öffnen  ·  Menü-Taste oder lange OK: Optionen  ·  Zurück: Ordner hoch", "OK to open  ·  Menu key or hold OK: options  ·  Back: up a folder");
        p("OK: hinzufügen", "OK: add");
        p("OK öffnen  ·  lange OK: Favorit, App-Info, Deinstallieren", "OK to open  ·  hold OK: favourite, app info, uninstall");
        // long subtitles
        p("So geht's: Handy im selben WLAN, Adresse oben im Browser öffnen, PIN eingeben. Dann einfach tippen: der Text landet im gerade ausgewählten Eingabefeld (Suche, Passwort, Browser). Dazu gibt es Tasten für Zurück, Home, Play/Pause und Lautstärke.",
                "How it works: phone on the same Wi-Fi, open the address above in the browser, enter the PIN. Then just type: the text lands in the currently selected input field (search, password, browser). There are also buttons for Back, Home, Play/Pause and volume.");
        p("Speichert deine Favoriten, das Theme und alle Lumen-Einstellungen in eine Datei. Praktisch vor einem Update (ein Neuinstallieren setzt Lumen sonst zurück).",
                "Saves your favourites, the theme and all Lumen settings to a file. Handy before an update (a reinstall would otherwise reset Lumen).");
        p("Werksgrenzen lernen: im Werks-Zoom ganz klein stellen, zurück, 'Werk-Min merken'. Dann ganz groß, 'Werk-Max merken'. Danach zeigt der Balken beide Grenzen.",
                "Learn the factory limits: in the factory zoom set it as small as possible, go back, 'Learn factory min'. Then as large as possible, 'Learn factory max'. The bar then shows both limits.");
        p("Das Bild ändert sich sofort (System-Rechte aktiv). Links / rechts: kleiner / größer, gedrückt halten = schneller.",
                "The picture changes instantly (system rights active). Left / right: smaller / bigger, hold = faster.");
        p("Lumen speichert die Größe, der Beamer übernimmt sie beim nächsten Neustart. Links / rechts: kleiner / größer, gedrückt halten = schneller.",
                "Lumen saves the size; the projector applies it on the next restart. Left / right: smaller / bigger, hold = faster.");
        p("Wird nach einem Neustart aktiv", "Active after a restart");
        p("Eigene (USB): kopiere Bilder in den Ordner Pictures/Lumen/custom auf dem Beamer (per USB-Stick + Dateien, oder am PC). JPG oder PNG.",
                "Custom (USB): copy images into the folder Pictures/Lumen/custom on the projector (via USB stick + Files, or on a PC). JPG or PNG.");
    }
}
