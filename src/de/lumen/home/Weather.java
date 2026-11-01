package de.lumen.home;

import android.content.Context;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/** Current weather with no API key (open-meteo), location from IP once. Cached ~1 h. */
final class Weather {
    private Weather() {}

    static String city = "";
    static int temp = -999;
    static int code = -1;

    static boolean has() { return temp > -999; }

    static void refresh(Context c) {
        long last = L.p(c).getLong("wxAt", 0);
        if (has() && System.currentTimeMillis() - last < 3600_000) return;
        // use cached last values immediately
        temp = L.p(c).getInt("wxTemp", -999);
        code = L.p(c).getInt("wxCode", -1);
        city = L.p(c).getString("wxCity", "");
        L.IO.execute(() -> {
            try {
                double lat, lon;
                String setCity = L.p(c).getString("wxSetCity", "");
                if (!setCity.isEmpty()) {
                    JSONObject g = new JSONObject(new String(Backdrop.get("https://geocoding-api.open-meteo.com/v1/search?count=1&name=" + android.net.Uri.encode(setCity)), StandardCharsets.UTF_8));
                    JSONObject r = g.getJSONArray("results").getJSONObject(0);
                    lat = r.getDouble("latitude"); lon = r.getDouble("longitude");
                    city = r.getString("name");
                } else {
                    JSONObject ip = new JSONObject(new String(Backdrop.get("http://ip-api.com/json/?fields=city,lat,lon"), StandardCharsets.UTF_8));
                    lat = ip.getDouble("lat"); lon = ip.getDouble("lon");
                    city = ip.optString("city", "");
                }
                JSONObject w = new JSONObject(new String(Backdrop.get("https://api.open-meteo.com/v1/forecast?current_weather=true&latitude=" + lat + "&longitude=" + lon), StandardCharsets.UTF_8));
                JSONObject cw = w.getJSONObject("current_weather");
                temp = (int) Math.round(cw.getDouble("temperature"));
                code = cw.getInt("weathercode");
                L.p(c).edit().putInt("wxTemp", temp).putInt("wxCode", code).putString("wxCity", city).putLong("wxAt", System.currentTimeMillis()).apply();
                L.UI.post(Backdrop::ping);
            } catch (Throwable ignored) {}
        });
    }

    static String text() {
        if (!has()) return "";
        return temp + "°  " + desc(code);
    }

    static String icon() {
        if (code < 0) return "temp";
        if (code == 0) return "sun";
        if (code <= 3) return "cloud";
        if (code >= 45 && code <= 48) return "cloud";
        if (code >= 51 && code <= 67) return "rain";
        if (code >= 71 && code <= 77) return "snow";
        if (code >= 80 && code <= 82) return "rain";
        if (code >= 85 && code <= 86) return "snow";
        if (code >= 95) return "storm";
        return "cloud";
    }

    static String desc(int c) {
        if (c == 0) return Lang.t("clear", "klar");
        if (c <= 2) return Lang.t("fair", "heiter");
        if (c == 3) return Lang.t("cloudy", "bewölkt");
        if (c <= 48) return Lang.t("fog", "Nebel");
        if (c <= 57) return Lang.t("drizzle", "Niesel");
        if (c <= 67) return Lang.t("rain", "Regen");
        if (c <= 77) return Lang.t("snow", "Schnee");
        if (c <= 82) return Lang.t("showers", "Schauer");
        if (c <= 86) return Lang.t("snow showers", "Schneeschauer");
        return Lang.t("thunderstorm", "Gewitter");
    }
}
