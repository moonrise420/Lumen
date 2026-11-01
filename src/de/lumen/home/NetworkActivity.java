package de.lumen.home;

import android.content.Intent;
import android.net.TrafficStats;
import android.net.Uri;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Live network dashboard: total bandwidth, which apps are talking to the internet and where, per-app data. */
public class NetworkActivity extends Base {
    private TextView speed;
    private LinearLayout list;
    private long lastRx, lastTx, lastT;
    private final Runnable loop = new Runnable() {
        @Override public void run() { tickSpeed(); L.UI.postDelayed(this, 1000); }
    };
    private final Runnable refresh = new Runnable() {
        @Override public void run() { fill(); L.UI.postDelayed(this, 4000); }
    };

    @Override void build() {
        TextView title = Ui.one(this, Lang.t("Network", "Netzwerk"), 30, t.head(), t.text);
        title.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        root.addView(title, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 34));
        speed = Ui.one(this, "", 18, t.bold(), t.text);
        speed.setShadowLayer(L.s(6), 0, L.s(1), 0x88000000);
        root.addView(speed, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 80));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(L.s(56), L.s(8), L.s(56), L.s(40));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> list.invalidate());
        root.addView(sv, Ui.at(Ui.MATCH, Ui.MATCH, Gravity.TOP | Gravity.LEFT, 0, 120));

        lastRx = TrafficStats.getTotalRxBytes();
        lastTx = TrafficStats.getTotalTxBytes();
        lastT = System.currentTimeMillis();
        fill();
    }

    private void tickSpeed() {
        long rx = TrafficStats.getTotalRxBytes(), tx = TrafficStats.getTotalTxBytes(), now = System.currentTimeMillis();
        double dt = Math.max(1, now - lastT) / 1000.0;
        long down = (long) ((rx - lastRx) / dt), up = (long) ((tx - lastTx) / dt);
        lastRx = rx; lastTx = tx; lastT = now;
        speed.setText("↓ " + Acts.fmtRate(this, Math.max(0, down)) + "      ↑ " + Acts.fmtRate(this, Math.max(0, up)));
    }

    private void fill() {
        View focused = getCurrentFocus();
        list.removeAllViews();

        // ---- apps talking to the internet now: host(s) + data used, one row each ----
        Map<Integer, List<String>> byUid = connections();
        list.addView(Ui.header(this, t, Lang.t("Apps online now", "Apps online")));
        if (byUid.isEmpty()) {
            TextView e = Ui.one(this, Lang.t("none right now", "gerade keine"), 15, t.body(), t.dim);
            e.setPadding(L.s(6), L.s(8), 0, 0);
            list.addView(e);
        }
        for (Map.Entry<Integer, List<String>> e : byUid.entrySet()) {
            int uid = e.getKey();
            String app = appName(uid);
            List<String> hosts = e.getValue();
            String sub = android.text.TextUtils.join("   ", hosts.subList(0, Math.min(3, hosts.size()))) + (hosts.size() > 3 ? "  +" + (hosts.size() - 3) : "");
            long bytes = uidBytes(uid);
            Ui.Row r = Ui.action(this, t, "net", app, sub, () -> bytes > 0 ? Formatter.formatShortFileSize(this, bytes) : hosts.size() + " ×", () -> appMenu(uid, app));
            add(r);
        }
        if (focused == null && list.getChildCount() > 1) list.getChildAt(1).requestFocus();
    }

    /** uid -> list of "ip:port" for established outbound TCP connections. */
    private Map<Integer, List<String>> connections() {
        Map<Integer, List<String>> out = new LinkedHashMap<>();
        for (String path : new String[]{"/proc/net/tcp", "/proc/net/tcp6"}) {
            try (BufferedReader r = new BufferedReader(new FileReader(path))) {
                String line = r.readLine(); // header
                while ((line = r.readLine()) != null) {
                    String[] f = line.trim().split("\\s+");
                    if (f.length < 8) continue;
                    if (!f[3].equals("01")) continue; // ESTABLISHED only
                    String rem = f[2];
                    int uid;
                    try { uid = Integer.parseInt(f[7]); } catch (Exception e) { continue; }
                    if (uid < 10000) continue; // system/root noise
                    String hp = remote(rem);
                    if (hp == null) continue;
                    out.computeIfAbsent(uid, k -> new ArrayList<>());
                    if (!out.get(uid).contains(hp)) out.get(uid).add(hp);
                }
            } catch (Exception ignored) {}
        }
        return out;
    }

    private static String remote(String rem) {
        int c = rem.lastIndexOf(':');
        if (c < 0) return null;
        String ipHex = rem.substring(0, c), portHex = rem.substring(c + 1);
        int port;
        try { port = Integer.parseInt(portHex, 16); } catch (Exception e) { return null; }
        String ip;
        if (ipHex.length() == 8) {
            long v = Long.parseLong(ipHex, 16);
            int b0 = (int) (v & 0xFF), b1 = (int) ((v >> 8) & 0xFF), b2 = (int) ((v >> 16) & 0xFF), b3 = (int) ((v >> 24) & 0xFF);
            if (b0 == 0 || b0 == 127) return null;
            ip = b0 + "." + b1 + "." + b2 + "." + b3;
        } else {
            ip = "[IPv6]";
        }
        return ip + ":" + port;
    }

    /** Total rx+tx bytes for one uid since boot, or 0 if the OS doesn't expose it. */
    private long uidBytes(int uid) {
        try {
            long rx = TrafficStats.getUidRxBytes(uid), tx = TrafficStats.getUidTxBytes(uid);
            if (rx == TrafficStats.UNSUPPORTED || rx < 0) return 0;
            return rx + tx;
        } catch (Exception e) { return 0; }
    }

    private String appName(int uid) {
        String[] pkgs = getPackageManager().getPackagesForUid(uid);
        if (pkgs == null || pkgs.length == 0) return "uid " + uid;
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkgs[0], 0)).toString(); }
        catch (Exception e) { return pkgs[0]; }
    }

    private void appMenu(int uid, String app) {
        String[] pkgs = getPackageManager().getPackagesForUid(uid);
        if (pkgs == null || pkgs.length == 0) return;
        String pkg = pkgs[0];
        menu(app, new String[]{Lang.t("App info (stop, permissions)", "App-Info (stoppen, Rechte)"), Lang.t("Close in background", "Im Hintergrund schließen"), Lang.t("Block data (restrict background)", "Daten sperren (Hintergrund)"), Lang.t("Cancel", "Abbrechen")}, i -> {
            if (i == 0) L.start(this, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
            else if (i == 1) { ((android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE)).killBackgroundProcesses(pkg); L.toast(this, Lang.t("Closed", "Geschlossen")); }
            else if (i == 2) restrict(uid);
        });
    }

    /** Best-effort: restrict background data for the uid (NetworkPolicyManager). Full per-app firewall needs root. */
    private void restrict(int uid) {
        try {
            Object npm = getSystemService("netpolicy");
            if (npm == null) { L.toast(this, Lang.t("Not available", "Nicht verfügbar")); return; }
            java.lang.reflect.Method m = npm.getClass().getMethod("setUidPolicy", int.class, int.class);
            m.invoke(npm, uid, 1); // POLICY_REJECT_METERED_BACKGROUND = 1
            L.toast(this, Lang.t("Background data restricted", "Hintergrunddaten gesperrt"));
        } catch (Throwable t) { L.toast(this, Lang.t("Needs root for a full block", "Voller Block braucht Root")); }
    }

    private void add(Ui.Row r) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(7);
        list.addView(r, lp);
    }

    @Override protected void onResume() {
        super.onResume();
        L.UI.post(loop);
        L.UI.postDelayed(refresh, 4000);
    }

    @Override protected void onPause() {
        super.onPause();
        L.UI.removeCallbacks(loop);
        L.UI.removeCallbacks(refresh);
    }
}
