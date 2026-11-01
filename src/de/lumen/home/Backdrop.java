package de.lumen.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Nature photo engine. Photos are cached on disk; one is decoded at a time (RGB_565) and a tiny blurred copy
 * feeds every glass panel. Nothing loads until the launcher has drawn its first frame.
 */
final class Backdrop {
    private Backdrop() {}

    static Bitmap photo, previous, blur;
    static String caption = "";
    static String currentFile = "";
    static long changedAt = 0;
    private static final List<WeakReference<View>> watchers = new ArrayList<>();
    private static final Random rnd = new Random();
    private static boolean loading;

    static void watch(View v) { watchers.add(new WeakReference<>(v)); }

    /** Redraw all watchers (e.g. after weather updates). */
    static void ping() { notifyAll0(); }

    // Background sources
    static final String SOULRIDE_DIR = "/storage/emulated/0/Pictures/Lumen/soulride";
    static final String CUSTOM_DIR = "/storage/emulated/0/Pictures/Lumen/custom";

    static File[] listImages(String dir) {
        File[] f = new File(dir).listFiles((x, n) -> { String s = n.toLowerCase(); return s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png") || s.endsWith(".webp"); });
        if (f != null) java.util.Arrays.sort(f, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return f == null ? new File[0] : f;
    }

    /** The current background source: "soulride" (default), "web" (Bing nature), "custom" (USB folder). */
    static File[] pool(Context c) {
        String src = L.p(c).getString("bgSource", "soulride");
        if ("web".equals(src)) { File[] p = natural(c); if (p.length == 0) { fetch(c); p = natural(c); } return p; }
        if ("custom".equals(src)) { File[] p = listImages(CUSTOM_DIR); return p.length > 0 ? p : natural(c); }
        File[] p = listImages(SOULRIDE_DIR);
        return p.length > 0 ? p : natural(c);
    }

    private static void notifyAll0() {
        Iterator<WeakReference<View>> it = watchers.iterator();
        while (it.hasNext()) {
            View v = it.next().get();
            if (v == null) it.remove(); else v.invalidate();
        }
    }

    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "photos");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    static File[] cached(Context c) {
        File[] f = dir(c).listFiles((d, n) -> n.endsWith(".jpg"));
        return f == null ? new File[0] : f;
    }

    /** Bing also shows cities and buildings; anything whose caption names one is skipped. */
    private static final java.util.regex.Pattern NOT_NATURE = java.util.regex.Pattern.compile(
            "(?i)stadt|kathedrale|kirche|dom\\b|münster|basilika|kloster|burg|schloss|palast|palais|brücke|turm|hafen|venedig|paris|london|new york|"
                    + "\\brom\\b|berlin|frankfurt|gebäude|museum|festival|markt|platz|straße|bahnhof|tempel|moschee|skyline|leuchtturm|hotel|dorf|kanal|canal|"
                    + "feuerwerk|karneval|laterne|statue|denkmal|bibliothek|universität|rathaus|gasse|viertel|bauwerk|architektur|stadion|"
                    + "mühle|windmühle|ruine|kapelle|villa|riesenrad|pagode|bridge|castle|church|cathedral|minster|city|town|street|tower|building|square|harbour|harbor");

    static boolean nature(String caption) { return caption == null || !NOT_NATURE.matcher(caption).find(); }

    private static String captionOf(File jpg) {
        try {
            File t = new File(jpg.getPath().replace(".jpg", ".txt"));
            return t.exists() ? new String(Files.readAllBytes(t.toPath()), StandardCharsets.UTF_8).trim() : "";
        } catch (Exception e) { return ""; }
    }

    /** Cached photos that pass the nature filter; non-nature ones are deleted. */
    static File[] natural(Context c) {
        List<File> ok = new ArrayList<>();
        for (File f : cached(c)) {
            if (nature(captionOf(f))) ok.add(f);
            else { f.delete(); new File(f.getPath().replace(".jpg", ".txt")).delete(); }
        }
        return ok.toArray(new File[0]);
    }

    /** Show the next random photo. Safe to call often. */
    static void next(Context c) {
        if (loading) return;
        loading = true;
        Context app = c.getApplicationContext();
        L.IO.execute(() -> {
            try {
                File[] all = pool(app);
                if (all.length == 0) return;
                File pick;
                if (currentFile.isEmpty()) pick = all[0]; // first load: the default (O Berrón sorts first)
                else {
                    pick = all[rnd.nextInt(all.length)];
                    if (all.length > 1 && pick.getName().equals(currentFile)) pick = all[(Arrays.asList(all).indexOf(pick) + 1) % all.length];
                }
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(pick.getPath(), o);
                int sample = 1;
                while (o.outWidth / (sample * 2) >= L.W && o.outHeight / (sample * 2) >= L.H) sample *= 2;
                o = new BitmapFactory.Options();
                o.inSampleSize = sample;
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap b = BitmapFactory.decodeFile(pick.getPath(), o);
                if (b == null) { pick.delete(); return; }
                Bitmap bl = makeBlur(b);
                String cap = "";
                File t = new File(pick.getPath().replace(".jpg", ".txt"));
                if (t.exists()) cap = new String(Files.readAllBytes(t.toPath()), StandardCharsets.UTF_8).trim();
                final String fcap = cap, fname = pick.getName();
                L.UI.post(() -> {
                    if (previous != null && previous != photo) previous.recycle();
                    previous = photo;
                    photo = b;
                    blur = bl;
                    caption = fcap;
                    currentFile = fname;
                    changedAt = System.currentTimeMillis();
                    notifyAll0();
                });
            } catch (Throwable ignored) {
            } finally {
                loading = false;
            }
        });
    }

    /** Pulls the latest photo list (Bing daily images: mostly landscapes and wildlife) and caches new ones. */
    static void fetch(Context c) {
        long last = L.p(c).getLong("photoFetch", 0);
        if (System.currentTimeMillis() - last < 20L * 3600_000 && cached(c).length >= 6) return;
        String size = L.p(c).getBoolean("photoHd", false) ? "_1920x1080.jpg" : "_1366x768.jpg";
        int got = 0;
        for (int idx : new int[]{0, 8}) {
            try {
                String js = new String(get("https://www.bing.com/HPImageArchive.aspx?format=js&idx=" + idx + "&n=8&mkt=de-DE"), StandardCharsets.UTF_8);
                JSONArray arr = new JSONObject(js).getJSONArray("images");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject im = arr.getJSONObject(i);
                    String base = im.getString("urlbase");
                    if (!nature(im.optString("copyright", "") + " " + im.optString("title", ""))) continue;
                    String name = base.replaceAll("[^A-Za-z0-9_.-]", "_");
                    File f = new File(dir(c), name + ".jpg");
                    if (f.exists()) continue;
                    byte[] data = get("https://www.bing.com" + base + size);
                    if (data.length < 20000) continue;
                    File tmp = new File(dir(c), name + ".tmp");
                    try (OutputStream o = new FileOutputStream(tmp)) { o.write(data); }
                    tmp.renameTo(f);
                    String cap = im.optString("copyright", im.optString("title", ""));
                    try (OutputStream o = new FileOutputStream(new File(dir(c), name + ".txt"))) { o.write(cap.getBytes(StandardCharsets.UTF_8)); }
                    got++;
                }
            } catch (Throwable ignored) {}
        }
        L.p(c).edit().putLong("photoFetch", System.currentTimeMillis()).apply();
        // keep the newest 80
        File[] all = cached(c);
        if (all.length > 80) {
            Arrays.sort(all, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < all.length - 80; i++) {
                all[i].delete();
                new File(all[i].getPath().replace(".jpg", ".txt")).delete();
            }
        }
    }

    static byte[] get(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(8000);
        h.setReadTimeout(15000);
        h.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11) Lumen");
        try (InputStream in = h.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toByteArray();
        } finally {
            h.disconnect();
        }
    }

    /** 128x72 copy, box-blurred 3 times. Drawn scaled up with filtering = frosted glass. */
    static Bitmap makeBlur(Bitmap src) {
        int w = 128, h = 72;
        Bitmap s = Bitmap.createScaledBitmap(src, w, h, true).copy(Bitmap.Config.ARGB_8888, true);
        int[] px = new int[w * h];
        s.getPixels(px, 0, w, 0, 0, w, h);
        for (int k = 0; k < 3; k++) { boxH(px, w, h, 3); boxV(px, w, h, 3); }
        s.setPixels(px, 0, w, 0, 0, w, h);
        return s;
    }

    private static void boxH(int[] p, int w, int h, int r) {
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            int o = y * w;
            for (int x = 0; x < w; x++) {
                int rs = 0, gs = 0, bs = 0, n = 0;
                for (int k = -r; k <= r; k++) {
                    int xx = Math.min(w - 1, Math.max(0, x + k));
                    int c = p[o + xx];
                    rs += (c >> 16) & 0xFF; gs += (c >> 8) & 0xFF; bs += c & 0xFF; n++;
                }
                row[x] = 0xFF000000 | ((rs / n) << 16) | ((gs / n) << 8) | (bs / n);
            }
            System.arraycopy(row, 0, p, o, w);
        }
    }

    private static void boxV(int[] p, int w, int h, int r) {
        int[] col = new int[h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rs = 0, gs = 0, bs = 0, n = 0;
                for (int k = -r; k <= r; k++) {
                    int yy = Math.min(h - 1, Math.max(0, y + k));
                    int c = p[yy * w + x];
                    rs += (c >> 16) & 0xFF; gs += (c >> 8) & 0xFF; bs += c & 0xFF; n++;
                }
                col[y] = 0xFF000000 | ((rs / n) << 16) | ((gs / n) << 8) | (bs / n);
            }
            for (int y = 0; y < h; y++) p[y * w + x] = col[y];
        }
    }

    /** Full-screen photo with crossfade, dim layer and a themed fallback gradient. */
    static final class View0 extends View {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint fill = new Paint();
        private final Matrix m = new Matrix();
        private final Theme t;

        View0(Context c, Theme t) {
            super(c);
            this.t = t;
            watch(this);
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            fill.setShader(new LinearGradient(0, 0, 0, h, t.bgTop, t.bgBottom, Shader.TileMode.CLAMP));
        }

        @Override protected void onDraw(Canvas c) {
            c.drawRect(0, 0, getWidth(), getHeight(), fill);
            long age = System.currentTimeMillis() - changedAt;
            float a = L.anim(getContext()) ? Math.min(1f, age / 1400f) : 1f;
            if (previous != null && !previous.isRecycled() && a < 1f) { paint.setAlpha(255); drawCrop(c, previous); }
            if (photo != null && !photo.isRecycled()) { paint.setAlpha((int) (255 * a)); drawCrop(c, photo); }
            if (t.photoDim > 0) c.drawColor((int) (t.photoDim * 255) << 24);
            if (a < 1f) postInvalidateOnAnimation();
        }

        private void drawCrop(Canvas c, Bitmap b) {
            float sc = Math.max(getWidth() / (float) b.getWidth(), getHeight() / (float) b.getHeight());
            m.setScale(sc, sc);
            m.postTranslate((getWidth() - b.getWidth() * sc) / 2f, (getHeight() - b.getHeight() * sc) / 2f);
            c.drawBitmap(b, m, paint);
        }
    }
}
