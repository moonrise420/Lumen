package de.lumen.home;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Favourites grouped into reorderable categories. Stored as JSON in prefs ("favcats"). */
final class Fav {
    final String name;
    final List<String> pkgs;

    Fav(String name, List<String> pkgs) { this.name = name; this.pkgs = pkgs; }

    static List<Fav> load(Context c) {
        String s = L.p(c).getString("favcats", null);
        List<Fav> out = new ArrayList<>();
        if (s == null) return migrate(c);
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONArray pk = o.getJSONArray("pkgs");
                List<String> pkgs = new ArrayList<>();
                for (int j = 0; j < pk.length(); j++) {
                    String p = pk.getString(j);
                    if (L.launchIntent(c, p) != null) pkgs.add(p);
                }
                out.add(new Fav(o.getString("name"), pkgs));
            }
        } catch (Exception e) { return migrate(c); }
        if (out.isEmpty()) out.add(new Fav("Favoriten", new ArrayList<>()));
        return out;
    }

    static void save(Context c, List<Fav> cats) {
        JSONArray arr = new JSONArray();
        try {
            for (Fav f : cats) {
                JSONObject o = new JSONObject();
                o.put("name", f.name);
                o.put("pkgs", new JSONArray(f.pkgs));
                arr.put(o);
            }
        } catch (Exception ignored) {}
        L.p(c).edit().putString("favcats", arr.toString()).apply();
    }

    /** First run: split the flat favourites into sensible default categories. */
    private static List<Fav> migrate(Context c) {
        List<String> all = L.favs(c);
        List<String> streaming = new ArrayList<>(), gstream = new ArrayList<>(), rest = new ArrayList<>();
        List<String> streamIds = Arrays.asList("com.netflix.mediaclient", "com.google.android.youtube.tv",
                "com.amazon.amazonvideo.livingroom", "org.jellyfin.androidtv", "com.zappntv",
                "com.disney.disneyplus", "com.wdt.spotify", "com.spotify.tv.android", "de.joyn.tv", "com.joyn.app");
        List<String> gameStreamIds = Arrays.asList("tv.twitch.android.app", "com.fgl27.twitch",
                "com.valvesoftware.steamlink", "com.nvidia.geforcenow", "com.limelight");
        for (String p : all) {
            if (gameStreamIds.contains(p)) gstream.add(p);
            else if (streamIds.contains(p)) streaming.add(p);
            else rest.add(p);
        }
        List<Fav> cats = new ArrayList<>();
        cats.add(new Fav("Streaming", streaming));
        cats.add(new Fav("Spiele Streams", gstream));
        cats.add(new Fav("Apps", rest));
        save(c, cats);
        return cats;
    }

    static void move(List<Fav> cats, int cat, String pkg, int toCat) {
        cats.get(cat).pkgs.remove(pkg);
        if (!cats.get(toCat).pkgs.contains(pkg)) cats.get(toCat).pkgs.add(pkg);
    }

    static boolean contains(List<Fav> cats, String pkg) {
        for (Fav f : cats) if (f.pkgs.contains(pkg)) return true;
        return false;
    }
}
