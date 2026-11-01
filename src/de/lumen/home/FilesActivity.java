package de.lumen.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * File browser in Lumen's style. Internal storage + USB sticks, D-pad friendly, German, with open / open-with /
 * copy / move / rename / delete / details. As a system app it can read every volume.
 */
public class FilesActivity extends Base {
    private static File clip;      // copy/move source
    private static boolean clipMove;

    private LinearLayout places, list;
    private ScrollView listScroll;
    private TextView pathTv, freeTv, hint;
    private File dir;
    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN);

    @Override void build() {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build()); // allow file:// to other apps (we are system)

        // ---- left: places ----
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(L.s(20), L.s(28), L.s(16), L.s(18));
        Ui.glass(left, t, Theme.alpha(t.panel, Math.max(0xC8, t.panel >>> 24)), 0);
        TextView h = Ui.one(this, "Dateien", 28, t.head(), t.text);
        h.setPadding(L.s(10), 0, 0, L.s(14));
        left.addView(h);
        ScrollView ps = new ScrollView(this);
        ps.setVerticalScrollBarEnabled(false);
        places = new LinearLayout(this);
        places.setOrientation(LinearLayout.VERTICAL);
        ps.addView(places);
        left.addView(ps, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));
        root.addView(left, Ui.at(L.s(300), Ui.MATCH, Gravity.TOP | Gravity.LEFT, 0, 0));

        // ---- right: header + list ----
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        pathTv = Ui.one(this, "", 22, t.head(), t.text);
        pathTv.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        right.addView(pathTv);
        freeTv = Ui.one(this, "", 14, t.body(), t.dim);
        freeTv.setPadding(0, L.s(2), 0, L.s(8));
        right.addView(freeTv);
        listScroll = new ScrollView(this);
        listScroll.setVerticalScrollBarEnabled(false);
        listScroll.setClipToPadding(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(list);
        listScroll.setOnScrollChangeListener((v, x, y, ox, oy) -> list.invalidate());
        right.addView(listScroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));
        hint = Ui.one(this, "OK öffnen  ·  Menü-Taste oder lange OK: Optionen  ·  Zurück: Ordner hoch", 14, t.body(), t.dim);
        hint.setPadding(0, L.s(8), 0, 0);
        hint.setShadowLayer(L.s(6), 0, L.s(1), 0x88000000);
        right.addView(hint);
        FrameLayout.LayoutParams rp = Ui.at(Ui.MATCH, Ui.MATCH, Gravity.TOP | Gravity.LEFT, 332, 30);
        rp.rightMargin = L.s(44);
        rp.bottomMargin = L.s(24);
        root.addView(right, rp);

        buildPlaces();
        open(Environment.getExternalStorageDirectory());
    }

    // ------------------------------------------------------------------ places
    private void buildPlaces() {
        places.removeAllViews();
        place("storage", "Interner Speicher", Environment.getExternalStorageDirectory());
        for (File v : usbVolumes()) place("usb", v.getName().length() > 14 ? Lang.t("USB stick", "USB-Stick") : v.getName(), v);
        places.addView(Ui.header(this, t, "Ordner"));
        File ext = Environment.getExternalStorageDirectory();
        quick("Download", new File(ext, "Download"));
        quick("Filme", new File(ext, "Movies"));
        quick("Bilder", new File(ext, "Pictures"));
        quick("Musik", new File(ext, "Music"));
        quick("DCIM", new File(ext, "DCIM"));
    }

    private List<File> usbVolumes() {
        List<File> out = new ArrayList<>();
        for (File root : new File[]{new File("/storage"), new File("/mnt/media_rw")}) {
            File[] fs = root.listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                String n = f.getName();
                if (n.equals("emulated") || n.equals("self") || n.equals("enc_emulated")) continue;
                if (f.isDirectory() && f.canRead() && (f.listFiles() != null)) out.add(f);
            }
        }
        return out;
    }

    private void place(String ico, String label, File f) {
        Ui.Row r = Ui.action(this, t, ico, label, null, () -> {
            try { return Formatter.formatShortFileSize(this, new StatFs(f.getPath()).getAvailableBytes()) + Lang.t(" free", " frei"); }
            catch (Exception e) { return ""; }
        }, () -> open(f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(5);
        places.addView(r, lp);
    }

    private void quick(String label, File f) {
        if (!f.isDirectory()) return;
        Ui.Row r = Ui.action(this, t, "storage", label, null, () -> "", () -> open(f));
        r.setBackground(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(3);
        places.addView(r, lp);
    }

    // ------------------------------------------------------------------ listing
    private void open(File d) {
        if (d == null || !d.isDirectory() || !d.canRead()) { L.toast(this, "Ordner nicht lesbar"); return; }
        dir = d;
        pathTv.setText(pretty(d));
        try { freeTv.setText(Formatter.formatShortFileSize(this, new StatFs(d.getPath()).getAvailableBytes()) + Lang.t(" free", " frei")); } catch (Exception e) { freeTv.setText(""); }
        list.removeAllViews();
        File[] raw = d.listFiles();
        if (raw == null) raw = new File[0];
        List<File> dirs = new ArrayList<>(), files = new ArrayList<>();
        for (File f : raw) { if (f.isHidden()) continue; (f.isDirectory() ? dirs : files).add(f); }
        dirs.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        files.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        if (clip != null) list.addView(pasteRow());
        if (dirs.isEmpty() && files.isEmpty()) {
            TextView e = Ui.one(this, "Leer", 16, t.body(), t.dim);
            e.setPadding(L.s(6), L.s(16), 0, 0);
            list.addView(e);
        }
        for (File f : dirs) list.addView(entry(f));
        for (File f : files) list.addView(entry(f));
        if (list.getChildCount() > 0) list.getChildAt(0).requestFocus();
        listScroll.scrollTo(0, 0);
    }

    private View pasteRow() {
        Ui.Row r = Ui.action(this, t, clipMove ? "size" : "plus", (clipMove ? "Hierher verschieben: " : "Hierher kopieren: ") + clip.getName(),
                "OK einfügen  ·  Menü: abbrechen", () -> "", this::paste);
        r.setOnKeyListener((v, code, e) -> {
            if (code == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP) { clip = null; open(dir); return true; }
            return false;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.bottomMargin = L.s(6);
        r.setLayoutParams(lp);
        return r;
    }

    private View entry(File f) {
        boolean d = f.isDirectory();
        String sub = d ? childCount(f) : Formatter.formatShortFileSize(this, f.length()) + "  ·  " + df.format(new Date(f.lastModified()));
        Ui.Row r = new Ui.Row(this, t, iconFor(f), f.getName(), sub, () -> d ? "›" : ext(f).toUpperCase());
        r.setOnClickListener(v -> { if (d) open(f); else openFile(f); });
        r.setOnLongClickListener(v -> { options(f); return true; });
        r.setOnKeyListener((v, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_UP) return false;
            if (code == KeyEvent.KEYCODE_MENU) { options(f); return true; }
            return false;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(6);
        r.setLayoutParams(lp);
        return r;
    }

    private String childCount(File f) {
        String[] c = f.list();
        return c == null ? Lang.t("Folder", "Ordner") : c.length + (c.length == 1 ? Lang.t(" entry", " Eintrag") : Lang.t(" entries", " Einträge"));
    }

    @Override void back() {
        if (overlayOpen()) { super.back(); return; }
        File p = dir == null ? null : dir.getParentFile();
        if (p != null && p.canRead() && !dir.getPath().equals("/storage") && dir.getParentFile() != null
                && !dir.getPath().equals(Environment.getExternalStorageDirectory().getPath())) open(p);
        else finish();
    }

    // ------------------------------------------------------------------ open
    private void openFile(File f) {
        String e = ext(f);
        if (e.equals("apk")) { L.start(this, new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive")); return; }
        String mime = mimeOf(e);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.fromFile(f), mime);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(i); } catch (Exception ex) { chooseApp(f, mime); }
    }

    private void chooseApp(File f, String mime) {
        Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(f), mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(Intent.createChooser(i, Lang.t("Open with", "Öffnen mit"))); } catch (Exception e) { L.toast(this, "Keine App dafür"); }
    }

    private void options(File f) {
        boolean d = f.isDirectory();
        List<String> items = new ArrayList<>();
        if (!d) { items.add("Öffnen"); items.add("Öffnen mit …"); }
        items.add("Kopieren");
        items.add("Verschieben");
        items.add("Umbenennen");
        items.add("Löschen");
        items.add("Details");
        menu(f.getName(), items.toArray(new String[0]), idx -> {
            String a = items.get(idx);
            switch (a) {
                case "Öffnen": openFile(f); break;
                case "Öffnen mit …": chooseApp(f, mimeOf(ext(f))); break;
                case "Kopieren": clip = f; clipMove = false; L.toast(this, "Kopiert – Zielordner öffnen, dann einfügen"); open(dir); break;
                case "Verschieben": clip = f; clipMove = true; L.toast(this, "Zum Verschieben – Zielordner öffnen, dann einfügen"); open(dir); break;
                case "Umbenennen": rename(f); break;
                case "Löschen": confirmDelete(f); break;
                case "Details": details(f); break;
            }
        });
    }

    private void confirmDelete(File f) {
        menu("Löschen?", new String[]{Lang.t("Yes, delete '" + f.getName() + "'", "Ja, '" + f.getName() + "' löschen"), "Abbrechen"}, i -> {
            if (i != 0) return;
            L.IO.execute(() -> {
                boolean ok = deleteRec(f);
                L.UI.post(() -> { L.toast(this, ok ? "Gelöscht" : "Konnte nicht löschen"); open(dir); });
            });
        });
    }

    private void rename(File f) {
        Keyboard.ask(this, t, "Neuer Name", f.getName(), name -> {
            if (name == null || name.trim().isEmpty()) return;
            File dst = new File(f.getParentFile(), name.trim());
            boolean ok = f.renameTo(dst);
            L.toast(this, ok ? "Umbenannt" : "Konnte nicht umbenennen");
            open(dir);
        });
    }

    private void paste() {
        final File src = clip;
        final boolean move = clipMove;
        final File dst = new File(dir, src.getName());
        clip = null;
        if (dst.exists()) { L.toast(this, "Gibt es schon"); open(dir); return; }
        L.toast(this, move ? "Verschiebe …" : "Kopiere …");
        L.IO.execute(() -> {
            boolean ok;
            if (move && src.renameTo(dst)) ok = true;
            else { ok = copyRec(src, dst); if (ok && move) deleteRec(src); }
            L.UI.post(() -> { L.toast(this, ok ? Lang.t("Done", "Fertig") : Lang.t("Failed", "Fehlgeschlagen")); open(dir); });
        });
    }

    private void details(File f) {
        long size = f.isDirectory() ? dirSize(f) : f.length();
        String s = Lang.t("Path: ", "Pfad: ") + f.getPath() + Lang.t("\nSize: ", "\nGröße: ") + Formatter.formatShortFileSize(this, size)
                + Lang.t("\nModified: ", "\nGeändert: ") + df.format(new Date(f.lastModified()))
                + (f.isDirectory() ? Lang.t("\nContents: ", "\nInhalt: ") + childCount(f) : Lang.t("\nType: ", "\nTyp: ") + ext(f).toUpperCase());
        menu(f.getName(), new String[]{s, "Schließen"}, i -> {});
    }

    // ------------------------------------------------------------------ fs helpers
    private static boolean deleteRec(File f) {
        if (f.isDirectory()) { File[] c = f.listFiles(); if (c != null) for (File x : c) deleteRec(x); }
        return f.delete();
    }

    private static boolean copyRec(File src, File dst) {
        try {
            if (src.isDirectory()) {
                if (!dst.exists() && !dst.mkdirs()) return false;
                File[] c = src.listFiles();
                if (c != null) for (File x : c) if (!copyRec(x, new File(dst, x.getName()))) return false;
                return true;
            }
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[262144];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            return true;
        } catch (Exception e) { return false; }
    }

    private static long dirSize(File f) {
        long s = 0;
        File[] c = f.listFiles();
        if (c != null) for (File x : c) s += x.isDirectory() ? dirSize(x) : x.length();
        return s;
    }

    private String pretty(File f) {
        String p = f.getAbsolutePath();
        String ext = Environment.getExternalStorageDirectory().getPath();
        if (p.equals(ext)) return Lang.t("Internal storage", "Interner Speicher");
        if (p.startsWith(ext + "/")) return Lang.t("Internal storage", "Interner Speicher") + p.substring(ext.length());
        return p;
    }

    private static String ext(File f) {
        String n = f.getName();
        int d = n.lastIndexOf('.');
        return d > 0 && d < n.length() - 1 ? n.substring(d + 1).toLowerCase() : "";
    }

    private static String iconFor(File f) {
        if (f.isDirectory()) return "storage";
        switch (type(ext(f))) {
            case 1: return "film";
            case 2: return "sound";
            case 3: return "photo";
            case 4: return "apps";
            default: return "info";
        }
    }

    private static int type(String e) {
        if (Arrays.asList("mp4,mkv,avi,mov,webm,ts,m2ts,flv,wmv,m4v,3gp".split(",")).contains(e)) return 1;
        if (Arrays.asList("mp3,flac,wav,aac,ogg,m4a,opus,wma".split(",")).contains(e)) return 2;
        if (Arrays.asList("jpg,jpeg,png,gif,bmp,webp,heic".split(",")).contains(e)) return 3;
        if (e.equals("apk")) return 4;
        return 0;
    }

    private static String mimeOf(String e) {
        switch (type(e)) {
            case 1: return "video/*";
            case 2: return "audio/*";
            case 3: return "image/*";
            default:
                if (Arrays.asList("txt,log,md,ini,cfg,json,xml".split(",")).contains(e)) return "text/plain";
                if (e.equals("pdf")) return "application/pdf";
                return "*/*";
        }
    }
}
