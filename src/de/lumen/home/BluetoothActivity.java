package de.lumen.home;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Lumen-styled Bluetooth: paired devices (connect / unpair) + scan & pair new ones. */
public class BluetoothActivity extends Base {
    private BluetoothAdapter bt;
    private LinearLayout list;
    private TextView status;
    private final Set<String> found = new LinkedHashSet<>();
    private BluetoothA2dp a2dp;

    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(a)) {
                BluetoothDevice d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null && d.getName() != null) { found.add(d.getAddress()); fill(); }
            } else fill();
        }
    };

    @Override void build() {
        bt = BluetoothAdapter.getDefaultAdapter();
        TextView title = Ui.one(this, "Bluetooth", 30, t.head(), t.text);
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

        if (bt == null) { status.setText(Lang.t("No Bluetooth", "Kein Bluetooth")); return; }
        if (!bt.isEnabled()) bt.enable();
        try { bt.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override public void onServiceConnected(int p, BluetoothProfile proxy) { if (p == BluetoothProfile.A2DP) a2dp = (BluetoothA2dp) proxy; fill(); }
            @Override public void onServiceDisconnected(int p) { a2dp = null; }
        }, BluetoothProfile.A2DP); } catch (Exception ignored) {}
        fill();
        rescan();
    }

    private void rescan() {
        found.clear();
        if (bt == null) return;
        try { if (bt.isDiscovering()) bt.cancelDiscovery(); bt.startDiscovery(); } catch (Exception ignored) {}
        status.setText(Lang.t("Searching …", "Suche Geräte …"));
    }

    private boolean connected(BluetoothDevice d) {
        try {
            Method m = BluetoothDevice.class.getMethod("isConnected");
            return (Boolean) m.invoke(d);
        } catch (Throwable t) { return false; }
    }

    private void fill() {
        if (bt == null) return;
        list.removeAllViews();
        status.setText(Lang.t("Tap a device to connect or pair", "Gerät antippen zum Verbinden oder Koppeln"));
        add(Ui.action(this, t, "bt", Lang.t("Search again", "Neu suchen"), null, () -> "", this::rescan));

        List<BluetoothDevice> paired = new ArrayList<>(bt.getBondedDevices());
        if (!paired.isEmpty()) list.addView(Ui.header(this, t, Lang.t("Paired", "Gekoppelt")));
        for (BluetoothDevice d : paired) add(deviceRow(d, true));

        List<BluetoothDevice> near = new ArrayList<>();
        for (String addr : found) {
            BluetoothDevice d = bt.getRemoteDevice(addr);
            if (d.getBondState() != BluetoothDevice.BOND_BONDED && d.getName() != null) near.add(d);
        }
        if (!near.isEmpty()) list.addView(Ui.header(this, t, Lang.t("Nearby", "In der Nähe")));
        for (BluetoothDevice d : near) add(deviceRow(d, false));
        if (list.getChildCount() > 0) list.getChildAt(0).requestFocus();
    }

    private Ui.Row deviceRow(BluetoothDevice d, boolean paired) {
        String name = d.getName() == null ? d.getAddress() : d.getName();
        boolean con = paired && connected(d);
        Ui.Row r = Ui.action(this, t, "bt", name, paired ? (con ? Lang.t("connected", "verbunden") : Lang.t("paired", "gekoppelt")) : d.getAddress(),
                () -> con ? "✓" : "›", () -> {
            try { bt.cancelDiscovery(); } catch (Exception ignored) {}
            if (!paired) { status.setText(Lang.t("Pairing …", "Koppeln …")); d.createBond(); return; }
            menu(name, new String[]{con ? Lang.t("Disconnect", "Trennen") : Lang.t("Connect", "Verbinden"), Lang.t("Unpair", "Entkoppeln"), Lang.t("Cancel", "Abbrechen")}, i -> {
                if (i == 0) { if (con) a2dpCall("disconnect", d); else a2dpCall("connect", d); }
                else if (i == 1) removeBond(d);
                L.UI.postDelayed(this::fill, 1500);
            });
        });
        return r;
    }

    private void a2dpCall(String method, BluetoothDevice d) {
        if (a2dp == null) { status.setText(Lang.t("Audio profile not ready", "Audio-Profil nicht bereit")); return; }
        try { BluetoothA2dp.class.getMethod(method, BluetoothDevice.class).invoke(a2dp, d); }
        catch (Throwable t) { status.setText(Lang.t("Could not change connection", "Verbindung nicht möglich")); }
    }

    private void removeBond(BluetoothDevice d) {
        try { BluetoothDevice.class.getMethod("removeBond").invoke(d); } catch (Throwable ignored) {}
    }

    private void add(Ui.Row r) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(7);
        list.addView(r, lp);
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothDevice.ACTION_FOUND);
        f.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        try { registerReceiver(rx, f); } catch (Exception ignored) {}
    }

    @Override protected void onPause() {
        super.onPause();
        try { unregisterReceiver(rx); } catch (Exception ignored) {}
        try { if (bt != null) bt.cancelDiscovery(); } catch (Exception ignored) {}
    }
}
