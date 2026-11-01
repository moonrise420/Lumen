package de.lumen.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Last-frame thumbnails for the recent-apps row on the home screen. This firmware is a TV and low-RAM
 * device, so Android keeps no task snapshots (getTaskSnapshot is always null). Instead the accessibility
 * helper screenshots the foreground app and stores the latest frame here, one small JPEG per package,
 * in the app's private files dir. Nothing leaves the device and nothing is kept for apps you don't open.
 */
final class Thumbs {
    private Thumbs() {}

    private static final int WIDTH = 480;   // downscaled; the tile is ~240dp wide
    private static final int KEEP = 8;       // most-recent apps to keep a thumbnail for; older ones are pruned

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "thumbs");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static File file(Context c, String pkg) {
        return new File(dir(c), pkg.replace('/', '_') + ".jpg");
    }

    static void save(Context c, String pkg, Bitmap src) {
        if (src == null) return;
        Bitmap scaled = null;
        try {
            int w = Math.min(WIDTH, src.getWidth());
            int h = Math.max(1, Math.round(src.getHeight() * (w / (float) src.getWidth())));
            scaled = Bitmap.createScaledBitmap(src, w, h, true);
            File tmp = new File(dir(c), pkg.replace('/', '_') + ".tmp");
            try (FileOutputStream o = new FileOutputStream(tmp)) { scaled.compress(Bitmap.CompressFormat.JPEG, 80, o); }
            File f = file(c, pkg);
            if (f.exists()) f.delete();
            tmp.renameTo(f);                      // atomic swap so the home screen never reads a half-written file
            prune(c);
        } catch (Throwable ignored) {
        } finally {
            if (scaled != null && scaled != src) scaled.recycle();
            src.recycle();
        }
    }

    /** Keep only the KEEP most-recently-updated thumbnails, so the folder never grows without bound. */
    private static void prune(Context c) {
        try {
            File[] fs = dir(c).listFiles((d, n) -> n.endsWith(".jpg"));
            if (fs == null || fs.length <= KEEP) return;
            java.util.Arrays.sort(fs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (int i = KEEP; i < fs.length; i++) fs[i].delete();
        } catch (Throwable ignored) {}
    }

    static Bitmap load(Context c, String pkg) {
        try {
            File f = file(c, pkg);
            if (!f.exists()) return null;
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Throwable t) {
            return null;
        }
    }

    /** Drop thumbnails for apps no longer worth keeping (e.g. when the feature is switched off). */
    static void clear(Context c) {
        try {
            File[] fs = dir(c).listFiles();
            if (fs != null) for (File f : fs) f.delete();
        } catch (Throwable ignored) {}
    }
}
