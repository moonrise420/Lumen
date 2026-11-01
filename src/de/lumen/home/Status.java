package de.lumen.home;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Live system numbers for the status bar and System-Info. All cheap, no root. */
final class Status {
    private Status() {}

    static WifiInfo wifi(Context c) {
        WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm == null || !wm.isWifiEnabled()) return null;
        WifiInfo i = wm.getConnectionInfo();
        return i == null || i.getNetworkId() == -1 ? null : i;
    }

    static boolean ethernet(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network n = cm.getActiveNetwork();
        NetworkCapabilities nc = n == null ? null : cm.getNetworkCapabilities(n);
        return nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    static String wifiShort(Context c) {
        if (ethernet(c)) return "LAN";
        WifiInfo i = wifi(c);
        if (i == null) return "Offline";
        return Acts.fmtLink(c, i.getLinkSpeed());
    }

    static int wifiBars(Context c) {
        WifiInfo i = wifi(c);
        return i == null ? 0 : WifiManager.calculateSignalLevel(i.getRssi(), 5);
    }

    static String ssid(Context c) {
        WifiInfo i = wifi(c);
        if (i == null) return "-";
        String s = i.getSSID();
        if (s == null || s.contains("unknown")) return Lang.t("connected", "verbunden");
        return s.replace("\"", "");
    }

    static String wifiDetail(Context c) {
        WifiInfo i = wifi(c);
        if (i == null) return ethernet(c) ? Lang.t("Wired LAN", "Kabel-LAN") : Lang.t("not connected", "nicht verbunden");
        int f = i.getFrequency();
        return ssid(c) + " · " + (f > 4900 ? "5 GHz" : Lang.t("2.4 GHz", "2,4 GHz")) + " · " + i.getRssi() + " dBm · " + i.getLinkSpeed() + " Mbit/s";
    }

    static String ip(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network n = cm.getActiveNetwork();
        LinkProperties lp = n == null ? null : cm.getLinkProperties(n);
        if (lp == null) return "-";
        for (LinkAddress a : lp.getLinkAddresses())
            if (a.getAddress() instanceof Inet4Address) return a.getAddress().getHostAddress();
        return "-";
    }

    /** Names of connected Bluetooth devices, or "An" / "Aus". */
    static String bt(Context c) {
        BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
        if (a == null) return "–";
        if (!a.isEnabled()) return Lang.t("Off", "Aus");
        List<String> names = new ArrayList<>();
        try {
            Method m = BluetoothDevice.class.getMethod("isConnected");
            for (BluetoothDevice d : a.getBondedDevices()) {
                if ((Boolean) m.invoke(d)) names.add(d.getName() == null ? Lang.t("Device", "Gerät") : d.getName());
            }
        } catch (Throwable ignored) {}
        if (names.isEmpty()) return Lang.t("On", "An");
        return names.size() == 1 ? names.get(0) : names.get(0) + " +" + (names.size() - 1);
    }

    static long ramFreeMb(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem / (1024 * 1024);
    }

    static long ramTotalMb(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem / (1024 * 1024);
    }

    /** CPU temperature in °C, or -1. */
    static int cpuTemp() { return zone(0); }

    static int gpuTemp() { return zone(1); }

    static int zone(int n) {
        String v = L.readFile("/sys/class/thermal/thermal_zone" + n + "/temp");
        try {
            int x = Integer.parseInt(v);
            return x > 1000 ? x / 1000 : x;
        } catch (Exception e) { return -1; }
    }

    static String cpuFreq() {
        String cur = L.readFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
        String max = L.readFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
        try {
            return (Integer.parseInt(cur) / 1000) + " / " + (Integer.parseInt(max) / 1000) + " MHz";
        } catch (Exception e) { return "-"; }
    }

    static String storage() {
        StatFs s = new StatFs(Environment.getDataDirectory().getPath());
        double free = s.getAvailableBytes() / 1e9, total = s.getTotalBytes() / 1e9;
        return String.format(Lang.EN ? Locale.ENGLISH : Locale.GERMAN, Lang.t("%.1f GB free of %.1f GB", "%.1f GB frei von %.1f GB"), free, total);
    }

    static double storageFreeGb() {
        return new StatFs(Environment.getDataDirectory().getPath()).getAvailableBytes() / 1e9;
    }

    static String uptime() {
        long m = SystemClock.elapsedRealtime() / 60000;
        return (m / 60) + " h " + (m % 60) + " min";
    }
}
