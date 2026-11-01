package de.lumen.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Lumen-styled Wi-Fi picker: scan, connect (with password), show the current network. System app = can connect. */
public class WifiActivity extends Base {
    private WifiManager wm;
    private LinearLayout list;
    private TextView status;
    private final BroadcastReceiver scanDone = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { fill(); }
    };

    @Override void build() {
        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        TextView title = Ui.one(this, Lang.t("Wi-Fi", "WLAN"), 30, t.head(), t.text);
        title.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        root.addView(title, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 36));
        status = Ui.one(this, "", 15, t.body(), t.dim);
        status.setShadowLayer(L.s(6), 0, L.s(1), 0x88000000);
        root.addView(status, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 82));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(L.s(56), L.s(8), L.s(56), L.s(40));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> list.invalidate());
        root.addView(sv, Ui.at(Ui.MATCH, Ui.MATCH, Gravity.TOP | Gravity.LEFT, 0, 120));

        if (!wm.isWifiEnabled()) wm.setWifiEnabled(true);
        fill();
        rescan();
    }

    private void rescan() {
        status.setText(Lang.t("Scanning …", "Suche Netzwerke …"));
        try { wm.startScan(); } catch (Exception ignored) {}
    }

    private void fill() {
        list.removeAllViews();
        WifiInfo cur = wm.getConnectionInfo();
        String curSsid = cur == null ? null : cur.getSSID() == null ? null : cur.getSSID().replace("\"", "");
        status.setText(curSsid != null && cur.getNetworkId() != -1
                ? Lang.t("Connected: ", "Verbunden: ") + curSsid + "  ·  " + cur.getLinkSpeed() + " Mbit/s"
                : Lang.t("Not connected", "Nicht verbunden"));

        List<ScanResult> results = new ArrayList<>();
        try { results = wm.getScanResults(); } catch (Exception ignored) {}
        // dedupe by SSID, keep strongest
        java.util.LinkedHashMap<String, ScanResult> best = new java.util.LinkedHashMap<>();
        results.sort((a, b) -> b.level - a.level);
        for (ScanResult r : results) {
            if (r.SSID == null || r.SSID.isEmpty()) continue;
            if (!best.containsKey(r.SSID)) best.put(r.SSID, r);
        }
        add(Ui.action(this, t, "wifi", Lang.t("Rescan", "Neu suchen"), null, () -> "", this::rescan));
        if (curSsid != null) add(row(curSsid, cur.getRssi(), true, true));
        for (ScanResult r : best.values()) {
            if (r.SSID.equals(curSsid)) continue;
            boolean secured = r.capabilities.contains("WPA") || r.capabilities.contains("WEP");
            add(row(r.SSID, r.level, secured, false));
        }
        if (list.getChildCount() > 0) list.getChildAt(0).requestFocus();
    }

    private Ui.Row row(String ssid, int level, boolean secured, boolean connected) {
        int bars = WifiManager.calculateSignalLevel(level, 5);
        String sub = (connected ? Lang.t("connected", "verbunden") + "  ·  " : "") + (secured ? "🔒 " : "") + bars + "/4";
        Ui.Row r = Ui.action(this, t, "wifi", ssid, sub, () -> connected ? "✓" : "›", () -> {
            if (connected) { menu(ssid, new String[]{Lang.t("Disconnect / forget", "Trennen / vergessen"), Lang.t("Cancel", "Abbrechen")}, i -> { if (i == 0) forget(ssid); }); return; }
            if (secured) Keyboard.ask(this, t, Lang.t("Password for ", "Passwort für ") + ssid, "", pw -> connect(ssid, pw, true));
            else connect(ssid, null, false);
        });
        return r;
    }

    private void connect(String ssid, String pass, boolean secured) {
        status.setText(Lang.t("Connecting to ", "Verbinde mit ") + ssid + " …");
        L.IO.execute(() -> {
            boolean ok = false;
            try {
                WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = "\"" + ssid + "\"";
                if (secured && pass != null) {
                    conf.preSharedKey = "\"" + pass + "\"";
                } else {
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }
                int id = wm.addNetwork(conf);
                if (id != -1) {
                    wm.disconnect();
                    wm.enableNetwork(id, true);
                    ok = wm.reconnect();
                }
            } catch (Throwable ignored) {}
            final boolean fok = ok;
            L.UI.postDelayed(() -> { fill(); if (!fok) status.setText(Lang.t("Could not connect", "Verbindung fehlgeschlagen")); }, 2500);
        });
    }

    private void forget(String ssid) {
        try {
            for (WifiConfiguration c : wm.getConfiguredNetworks()) {
                if (c.SSID != null && c.SSID.replace("\"", "").equals(ssid)) { wm.removeNetwork(c.networkId); }
            }
            wm.disconnect();
        } catch (Throwable ignored) {}
        L.UI.postDelayed(this::fill, 1200);
    }

    private void add(Ui.Row r) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(7);
        list.addView(r, lp);
    }

    @Override protected void onResume() {
        super.onResume();
        try { registerReceiver(scanDone, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); } catch (Exception ignored) {}
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(scanDone); } catch (Exception ignored) {}
    }
}
