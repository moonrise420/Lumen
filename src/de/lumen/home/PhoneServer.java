package de.lumen.home;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.KeyEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Tiny LAN web page (port 8300, PIN-protected) that turns a phone into keyboard + remote. */
public class PhoneServer extends Service {
    static final int PORT = 8300;
    private ServerSocket server;
    private volatile boolean running;
    private int wrong;
    private long lockedUntil;

    static void start(Context c) {
        try { c.startForegroundService(new Intent(c, PhoneServer.class)); } catch (Exception ignored) {}
    }

    static String pin(Context c) {
        String p = L.p(c).getString("phonePin", null);
        if (p == null) {
            p = String.format("%04d", new SecureRandom().nextInt(10000));
            L.p(c).edit().putString("phonePin", p).apply();
        }
        return p;
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("phone", Lang.t("Phone keyboard", "Handy-Tastatur"), NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "phone").setContentTitle(Lang.t("Lumen phone keyboard", "Lumen Handy-Tastatur"))
                .setSmallIcon(android.R.drawable.ic_menu_edit).build();
        startForeground(7, n);
        L.watchHelper(this);
    }

    @Override public int onStartCommand(Intent i, int f, int id) {
        if (!running) {
            running = true;
            new Thread(this::serve, "lumen-http").start();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    private void serve() {
        try {
            server = new ServerSocket(PORT);
            while (running) {
                Socket s = server.accept();
                L.IO.execute(() -> handle(s));
            }
        } catch (Exception ignored) {
            running = false;
        }
    }

    private void handle(Socket s) {
        try (Socket sock = s) {
            sock.setSoTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
            String line = in.readLine();
            if (line == null) return;
            while (true) { String h = in.readLine(); if (h == null || h.isEmpty()) break; }
            String[] parts = line.split(" ");
            String target = parts.length > 1 ? parts[1] : "/";
            String path = target, query = "";
            int q = target.indexOf('?');
            if (q >= 0) { path = target.substring(0, q); query = target.substring(q + 1); }
            OutputStream out = sock.getOutputStream();
            if (path.equals("/api")) reply(out, "application/json", api(params(query)));
            else if (path.equals("/logo.png")) replyAsset(out, "soulride_mark.png", "image/png");
            else reply(out, "text/html; charset=utf-8", page());
        } catch (Exception ignored) {}
    }

    private static Map<String, String> params(String q) throws Exception {
        Map<String, String> m = new HashMap<>();
        for (String kv : q.split("&")) {
            int e = kv.indexOf('=');
            if (e > 0) m.put(kv.substring(0, e), URLDecoder.decode(kv.substring(e + 1), "UTF-8"));
        }
        return m;
    }

    private void replyAsset(OutputStream out, String name, String type) throws Exception {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        try (java.io.InputStream is = getAssets().open(name)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
        }
        byte[] b = bo.toByteArray();
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: " + type + "\r\nContent-Length: " + b.length
                + "\r\nCache-Control: max-age=86400\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(b);
        out.flush();
    }

    private static void reply(OutputStream out, String type, String body) throws Exception {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: " + type + "\r\nContent-Length: " + b.length
                + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(b);
        out.flush();
    }

    private synchronized String api(Map<String, String> p) {
        if (SystemClock.elapsedRealtime() < lockedUntil) return "{\"ok\":false,\"msg\":\"" + Lang.t("Too many wrong PINs. Please wait 5 minutes.", "Zu viele falsche PINs. Bitte 5 Minuten warten.") + "\"}";
        if (!pin(this).equals(p.get("pin"))) {
            if (++wrong >= 8) { lockedUntil = SystemClock.elapsedRealtime() + 300_000; wrong = 0; }
            return "{\"ok\":false,\"msg\":\"" + badPin() + "\"}";
        }
        wrong = 0;
        String a = p.containsKey("a") ? p.get("a") : "";
        String text = p.containsKey("t") ? p.get("t") : "";
        AudioManager au = (AudioManager) getSystemService(AUDIO_SERVICE);
        switch (a) {
            case "ping": return "{\"ok\":true}";
            case "vup": au.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); return ok();
            case "vdown": au.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); return ok();
            case "mute": au.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI); return ok();
            case "play": media(au, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE); return ok();
            case "next": media(au, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD); return ok();
            case "prev": media(au, KeyEvent.KEYCODE_MEDIA_REWIND); return ok();
            case "kino": L.UI.post(() -> Acts.setCinema(this, !Acts.cinema(this))); return ok();
            case "scene": { int si = 0; try { si = Integer.parseInt(p.get("s")); } catch (Exception ignored) {} final int sf = si; L.UI.post(() -> Acts.applyScene(this, sf)); return ok(); }
        }
        RemoteService r = RemoteService.inst;
        if (r == null) return "{\"ok\":false,\"msg\":\"" + Lang.t("The key helper on the projector is off", "Tastenassistent am Beamer ist aus") + "\"}";
        AtomicBoolean res = new AtomicBoolean(true);
        CountDownLatch done = new CountDownLatch(1);
        L.UI.post(() -> {
            switch (a) {
                case "type": res.set(r.type(text, false)); break;
                case "replace": res.set(r.type(text, true)); break;
                case "del": res.set(r.backspace()); break;
                case "enter": res.set(r.enter()); break;
                case "back": r.back(); break;
                case "home": r.home(); break;
                case "menu": r.openAdvanced(); break;
                case "sleep": r.sleepNow(); break;
                case "mon": r.mouseOn(); break;
                case "moff": r.mouseOff(); break;
                case "mv": r.moveBy(f(p, "dx"), f(p, "dy")); break;
                case "mclick": res.set(r.click()); break;
                case "mlong": res.set(r.rightClick()); break;
                case "mscroll": r.scroll("up".equals(p.get("d"))); break;
                case "up": r.key(KeyEvent.KEYCODE_DPAD_UP); break;
                case "down": r.key(KeyEvent.KEYCODE_DPAD_DOWN); break;
                case "left": r.key(KeyEvent.KEYCODE_DPAD_LEFT); break;
                case "right": r.key(KeyEvent.KEYCODE_DPAD_RIGHT); break;
                case "ok": r.key(KeyEvent.KEYCODE_DPAD_CENTER); break;
                default: res.set(false);
            }
            done.countDown();
        });
        try { done.await(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return res.get() ? ok() : "{\"ok\":false,\"msg\":\"" + Lang.t("No input field is selected on the projector", "Am Beamer ist kein Eingabefeld ausgewählt") + "\"}";
    }

    private static String ok() { return "{\"ok\":true}"; }

    private static float f(Map<String, String> p, String k) {
        try { return Float.parseFloat(p.get(k)); } catch (Exception e) { return 0; }
    }

    private static void media(AudioManager au, int code) {
        au.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        au.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private static String badPin() { return Lang.t("Wrong PIN", "PIN falsch"); }

    private static String page() {
        return "<!doctype html><html lang=\"" + (Lang.EN ? "en" : "de") + "\"><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\"><title>Lumen</title><style>"
            + ":root{--bg:#0b1416;--card:rgba(255,255,255,.055);--card2:rgba(255,255,255,.09);--line:rgba(255,255,255,.12);--tx:#eef5f2;--dim:#9fb6b0;--ac:#7ee0c6;--acd:#0b1416}"
            + "*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}"
            + "body{margin:0;min-height:100vh;background:radial-gradient(120% 80% at 50% -10%,#163233 0%,var(--bg) 58%) fixed;color:var(--tx);font:16px/1.4 -apple-system,system-ui,'Segoe UI',Roboto,sans-serif;padding:max(22px,env(safe-area-inset-top)) 16px 46px;max-width:580px;margin:auto}"
            + ".brand{display:flex;align-items:center;gap:12px}.logo{width:46px;height:46px;object-fit:contain;flex:0 0 auto}"
            + "h1{font-weight:300;font-size:26px;margin:0}.sub{color:var(--dim);margin:4px 0 18px;font-size:13.5px}"
            + ".card{background:var(--card);border:1px solid var(--line);border-radius:20px;padding:16px;margin:14px 0;backdrop-filter:blur(10px);-webkit-backdrop-filter:blur(10px)}"
            + "h2{font-size:12px;letter-spacing:.16em;text-transform:uppercase;color:var(--ac);margin:0 2px 12px;font-weight:700}"
            + "textarea{width:100%;background:rgba(0,0,0,.25);color:var(--tx);border:1px solid var(--line);border-radius:14px;padding:14px;font-size:17px;height:92px;resize:none}textarea::placeholder{color:var(--dim)}"
            + "input{width:100%;background:rgba(0,0,0,.25);color:var(--tx);border:1px solid var(--line);border-radius:14px;padding:16px;font-size:20px;text-align:center;letter-spacing:.35em}input::placeholder{letter-spacing:.2em}"
            + ".row{display:grid;gap:10px;margin-top:10px}.r2{grid-template-columns:1fr 1fr}.r3{grid-template-columns:1fr 1fr 1fr}.r4{grid-template-columns:repeat(4,1fr)}"
            + "button{appearance:none;background:var(--card2);color:var(--tx);border:1px solid var(--line);border-radius:14px;padding:0 8px;min-height:56px;font-size:16px;font-weight:600;transition:transform .05s ease,background .15s}"
            + "button:active{transform:scale(.97);background:rgba(255,255,255,.17)}.pri{background:var(--ac);color:var(--acd);border-color:transparent;font-weight:700}.ghost{background:transparent;color:var(--dim);min-height:48px}"
            + "#pad{height:46vh;min-height:270px;margin-top:4px;border-radius:18px;border:1px solid var(--line);background:repeating-linear-gradient(0deg,rgba(255,255,255,.028) 0 1px,transparent 1px 24px),repeating-linear-gradient(90deg,rgba(255,255,255,.028) 0 1px,transparent 1px 24px),rgba(0,0,0,.22);touch-action:none;display:grid;place-items:center;text-align:center;color:var(--dim);font-size:13.5px;padding:18px;user-select:none}#pad.on{border-color:var(--ac);box-shadow:0 0 0 1px var(--ac) inset}"
            + "#mousePanel{display:none}#msg{min-height:22px;margin:14px 2px 0;color:var(--dim);font-size:14px;text-align:center}#login{display:none}"
            + ".dpad{display:grid;grid-template-columns:repeat(3,1fr);grid-auto-rows:54px;gap:10px;max-width:260px;margin:12px auto 0}.dpad button{min-height:0;font-size:22px}"
            + ".du{grid-column:2;grid-row:1}.dl{grid-column:1;grid-row:2}.dok{grid-column:2;grid-row:2}.dr{grid-column:3;grid-row:2}.dd{grid-column:2;grid-row:3}"
            + "#app h2{cursor:pointer;display:flex;align-items:center;justify-content:space-between;gap:8px;user-select:none}#app h2::after{content:'▾';font-size:13px;opacity:.6;font-weight:400;transition:transform .2s}.card.col h2{margin-bottom:0}.card.col h2::after{transform:rotate(-90deg)}.card.col>:not(h2){display:none!important}"
            + "</style></head><body>"
            + "<div class=\"brand\"><img class=\"logo\" src=\"/logo.png\" alt=\"\"><h1>Lumen</h1></div>"
            + "<div class=\"sub\">" + Lang.t("by Soulride  ·  keyboard, mouse & remote", "by Soulride  ·  Tastatur, Maus & Fernbedienung") + "</div>"
            + "<div id=\"login\" class=\"card\"><h2>PIN</h2><input id=\"pin\" inputmode=\"numeric\" maxlength=\"4\" placeholder=\"····\">"
            + "<div class=\"row\"><button class=\"pri\" onclick=\"savePin()\">" + Lang.t("Connect", "Verbinden") + "</button></div>"
            + "<div class=\"sub\" style=\"margin:12px 2px 0\">" + Lang.t("The PIN is on the projector under Advanced › Phone as keyboard", "PIN steht am Beamer unter Erweitert › Handy als Tastatur") + "</div></div>"
            + "<div id=\"app\">"
            + "<div class=\"card\"><h2>Text</h2><textarea id=\"t\" placeholder=\"" + Lang.t("Type here … lands in the selected field on the projector", "Hier tippen … landet im ausgewählten Feld am Beamer") + "\"></textarea>"
            + "<div class=\"row r2\"><button class=\"pri\" onclick=\"send('type')\">" + Lang.t("Send", "Senden") + "</button><button onclick=\"send('replace')\">" + Lang.t("Replace field", "Feld ersetzen") + "</button></div>"
            + "<div class=\"row r2\"><button onclick=\"go('del')\">⌫ " + Lang.t("Delete", "Löschen") + "</button><button onclick=\"go('enter')\">Enter ⏎</button></div></div>"
            + "<div class=\"card\"><h2>" + Lang.t("Mouse", "Maus") + "</h2><div class=\"row\"><button id=\"mBtn\" onclick=\"toggleMouse()\">🖱 " + Lang.t("Mouse on", "Maus ein") + "</button></div>"
            + "<div id=\"mousePanel\"><div id=\"pad\">" + Lang.t("Drag = move the pointer · tap = click", "Finger ziehen = Zeiger bewegen · tippen = klick") + "</div>"
            + "<div class=\"row r2\"><button onclick=\"go('mclick')\">" + Lang.t("Click", "Klick") + "</button><button onclick=\"go('mlong')\">" + Lang.t("Right-click", "Rechtsklick") + "</button></div>"
            + "<div class=\"row r2\"><button onclick=\"api({a:'mscroll',d:'up'})\">" + Lang.t("Scroll ↑", "Scroll ↑") + "</button><button onclick=\"api({a:'mscroll',d:'down'})\">" + Lang.t("Scroll ↓", "Scroll ↓") + "</button></div></div></div>"
            + "<div class=\"card\"><h2>" + Lang.t("Controls", "Steuerung") + "</h2>"
            + "<div class=\"row\"><button class=\"pri\" onclick=\"go('menu')\">☰ " + Lang.t("Lumen menu", "Lumen-Menü") + "</button></div>"
            + "<div class=\"row r3\"><button onclick=\"go('back')\">" + Lang.t("Back", "Zurück") + "</button><button onclick=\"go('home')\">Home</button><button onclick=\"go('play')\">Play / Pause</button></div>"
            + "<div class=\"row r4\"><button onclick=\"go('prev')\">⏪</button><button onclick=\"go('vdown')\">" + Lang.t("Vol -", "Leiser") + "</button><button onclick=\"go('vup')\">" + Lang.t("Vol +", "Lauter") + "</button><button onclick=\"go('next')\">⏩</button></div>"
            + "<div class=\"row r3\"><button onclick=\"go('mute')\">" + Lang.t("Mute", "Stumm") + "</button><button onclick=\"go('kino')\">" + Lang.t("Cinema", "Kino") + "</button><button onclick=\"go('sleep')\">Standby</button></div>"
            + "<div class=\"dpad\"><button class=\"du\" onclick=\"go('up')\">↑</button><button class=\"dl\" onclick=\"go('left')\">←</button><button class=\"dok pri\" onclick=\"go('ok')\">OK</button><button class=\"dr\" onclick=\"go('right')\">→</button><button class=\"dd\" onclick=\"go('down')\">↓</button></div></div>"
            + "<div class=\"card\"><h2>" + Lang.t("Picture & mode", "Bild & Modus") + "</h2>"
            + "<div class=\"row r2\"><button onclick=\"api({a:'scene',s:0})\">Standard</button><button onclick=\"api({a:'scene',s:3})\">" + Lang.t("Bright room", "Heller Raum") + "</button></div>"
            + "<div class=\"row r2\"><button onclick=\"api({a:'scene',s:1})\">" + Lang.t("Movie", "Film") + "</button><button onclick=\"api({a:'scene',s:2})\">" + Lang.t("Games", "Spiele") + "</button></div></div>"
            + "<div class=\"row\"><button class=\"ghost\" onclick=\"logout()\">" + Lang.t("Change PIN", "PIN ändern") + "</button></div></div><div id=\"msg\"></div>"
            + "<script>let P=localStorage.getItem('lumenPin')||'';const $=i=>document.getElementById(i);"
            + "const BADPIN='" + badPin() + "',UNREACH='" + Lang.t("Projector not reachable", "Beamer nicht erreichbar") + "',MON='🖱 " + Lang.t("Mouse on", "Maus ein") + "',MOFF='🖱 " + Lang.t("Mouse off", "Maus aus") + "',SENS=1.7;"
            + "function view(){$('login').style.display=P?'none':'block';$('app').style.display=P?'block':'none'}"
            + "async function api(o){const q=new URLSearchParams(Object.assign({pin:P},o));try{const r=await fetch('/api?'+q);const j=await r.json();"
            + "if(o.a!=='mv'&&o.a!=='ping')$('msg').textContent=j.ok?'✓':(j.msg||'');if(j.msg===BADPIN)logout();return j.ok}catch(e){if(o.a!=='mv')$('msg').textContent=UNREACH;return false}}"
            + "function go(a){api({a:a})}async function send(a){if(await api({a:a,t:$('t').value}))$('t').value=''}"
            + "async function savePin(){P=$('pin').value;localStorage.setItem('lumenPin',P);if(await api({a:'ping'}))view();else{P='';localStorage.removeItem('lumenPin')}}"
            + "let mon=false;function toggleMouse(){mon=!mon;$('mousePanel').style.display=mon?'block':'none';$('mBtn').classList.toggle('pri',mon);$('mBtn').textContent=mon?MOFF:MON;$('pad').classList.toggle('on',mon);api({a:mon?'mon':'moff'})}"
            + "function logout(){if(mon)toggleMouse();P='';localStorage.removeItem('lumenPin');view()}"
            + "const pad=$('pad');let lx,ly,moved,dt,ax=0,ay=0,last=0;"
            + "pad.addEventListener('touchstart',e=>{e.preventDefault();const t=e.touches[0];lx=t.clientX;ly=t.clientY;moved=0;dt=Date.now()},{passive:false});"
            + "pad.addEventListener('touchmove',e=>{e.preventDefault();const t=e.touches[0];const dx=t.clientX-lx,dy=t.clientY-ly;lx=t.clientX;ly=t.clientY;moved+=Math.abs(dx)+Math.abs(dy);ax+=dx*SENS;ay+=dy*SENS;const n=Date.now();if(n-last>45){const sx=Math.round(ax),sy=Math.round(ay);ax=0;ay=0;last=n;if(sx||sy)api({a:'mv',dx:sx,dy:sy})}},{passive:false});"
            + "pad.addEventListener('touchend',e=>{e.preventDefault();if(moved<9&&Date.now()-dt<260)api({a:'mclick'})},{passive:false});"
            + "document.querySelectorAll('#app .card h2').forEach((h,i)=>{const k='lc'+i;if(localStorage.getItem(k)==='1')h.parentElement.classList.add('col');h.addEventListener('click',()=>{const c=h.parentElement.classList.toggle('col');localStorage.setItem(k,c?'1':'0')})});"
            + "view()</script></body></html>";
    }
}
