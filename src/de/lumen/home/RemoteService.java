package de.lumen.home;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for what apps normally can't do: remember the last used app (for Home 2×), standby, power menu
 * and typing into the selected field (phone keyboard). It does not catch keys: this firmware switches off
 * any accessibility service that consumes the Home key.
 */
public class RemoteService extends AccessibilityService {
    static RemoteService inst;

    private String curPkg;
    /** Last app the user was in before the home screen. */
    static String lastApp;

    private TvInputManager tvm;
    private TvInputManager.TvInputCallback tvCb;
    private final Map<String, Integer> tvState = new HashMap<>();

    @Override protected void onServiceConnected() {
        inst = this;
        watchHdmi();
    }

    /** Auto-switch to HDMI when a device is plugged in (best-effort, only while on the launcher). */
    private void watchHdmi() {
        try {
            tvm = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
            if (tvm == null) return;
            for (TvInputInfo i : tvm.getTvInputList()) tvState.put(i.getId(), tvm.getInputState(i.getId()));
            tvCb = new TvInputManager.TvInputCallback() {
                @Override public void onInputStateChanged(String inputId, int state) {
                    Integer prev = tvState.put(inputId, state);
                    boolean becameConnected = state == TvInputManager.INPUT_STATE_CONNECTED
                            && (prev == null || prev != TvInputManager.INPUT_STATE_CONNECTED);
                    if (!becameConnected) return;
                    if (!L.p(RemoteService.this).getBoolean("autoHdmi", false)) return;
                    TvInputInfo info = tvm.getTvInputInfo(inputId);
                    if (info == null || info.getType() != TvInputInfo.TYPE_HDMI) return;
                    // only when sitting on the Lumen launcher, so we never yank the user out of an app
                    if (getPackageName().equals(curPkg)) L.UI.postDelayed(() -> Acts.hdmi(RemoteService.this), 800);
                }
            };
            tvm.registerCallback(tvCb, L.UI);
        } catch (Throwable ignored) {}
    }

    @Override public void onDestroy() {
        inst = null;
        mouseOff();
        try { if (tvm != null && tvCb != null) tvm.unregisterCallback(tvCb); } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @Override public void onInterrupt() {}

    // The ☰ Menu key is grabbed by the firmware to open the stock settings before any app sees it.
    // We can't catch the key, but we can catch the window it opens: when the stock settings appear and
    // we didn't open them on purpose (from our own Advanced menu), it was the Menu key → show Advanced instead.
    static volatile long stockAllowedUntil;
    static volatile long openAdvancedAt;   // set when the Menu key hit; HomeActivity.onResume opens Advanced
    private long lastStockEvent;           // time of the last stock-settings window event (to spot a fresh entry)
    static void allowStock() { stockAllowedUntil = android.os.SystemClock.elapsedRealtime() + 5000; }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null) return;
        if (mouseActive) L.UI.postDelayed(this::ensureCursor, 300);   // a window change can detach the pointer — bring it back
        String pkg = e.getPackageName().toString();
        if (pkg.equals("com.htc.htcsettings")) {   // stock settings appeared
            long now = android.os.SystemClock.elapsedRealtime();
            boolean freshEntry = now - lastStockEvent > 2000;   // a new press, not navigation inside the stock menu
            lastStockEvent = now;
            if (freshEntry && now > stockAllowedUntil && L.p(this).getBoolean("menuToAdvanced", true)) {
                // The ☰ Menu key opened it. This firmware blocks/delays background activity starts even for a system
                // app, so the reliable path is: press Back (closes the stock menu, resumes our launcher) → HomeActivity
                // then opens Advanced. Not instant, but it always lands on our menu.
                openAdvancedAt = now;
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            return;
        }
        trackThumb(pkg);
        if (pkg.equals(curPkg) || pkg.equals("com.android.systemui") || pkg.equals("android")) return;
        if (L.launchIntent(this, pkg) == null && !pkg.equals(getPackageName())) return;
        if (curPkg != null && !curPkg.equals(getPackageName())) lastApp = curPkg;
        curPkg = pkg;
    }

    // ---- last-frame thumbnails for the recents row -------------------------------------------------
    // No task snapshots on this box (TV + low-RAM), so we screenshot the foreground app ourselves and
    // keep the latest frame per package. Refreshes every few seconds while an app is in front, so what
    // the home screen shows is close to the frame you left it on. Off by default of the "thumbs" setting.
    private String thumbPkg;
    private long lastShot;
    private volatile boolean thumbGood;   // saved a real (non-blank) frame during this foreground visit
    private long thumbStart;              // when the current app came to the front (elapsedRealtime)

    private void trackThumb(String pkg) {
        if (pkg.equals(getPackageName())) { stopThumbs(); return; }                // back on the launcher: keep the last frame
        if (pkg.equals("com.android.systemui") || pkg.equals("android")) return;   // transient overlay, leave the timer running
        if (L.launchIntent(this, pkg) == null) return;                             // not a launchable app
        startThumbs(pkg);
    }

    private void startThumbs(String pkg) {
        if (!L.p(this).getBoolean("thumbs", true)) return;
        if (pkg.equals(thumbPkg)) return;
        thumbPkg = pkg;
        thumbGood = false;
        thumbStart = android.os.SystemClock.elapsedRealtime();
        L.UI.removeCallbacks(thumbTick);
        L.UI.postDelayed(thumbTick, 1200);                                         // let the app paint its first real frame
    }

    void stopThumbs() {
        thumbPkg = null;
        L.UI.removeCallbacks(thumbTick);
    }

    static void disableThumbs(android.content.Context c) {
        if (inst != null) inst.stopThumbs();
        Thumbs.clear(c);
    }

    private final Runnable thumbTick = new Runnable() {
        @Override public void run() {
            String p = thumbPkg;
            if (p == null) return;
            snap(p);
            // Apps paint a black splash first, so keep trying quickly until we catch a real frame; once we have one,
            // just refresh now and then. Secure apps (e.g. Prime) stay black — give up the fast retries so we don't hammer.
            long elapsed = android.os.SystemClock.elapsedRealtime() - thumbStart;
            long next = thumbGood ? 30000 : (elapsed < 15000 ? 1500 : 30000);
            L.UI.postDelayed(this, next);
        }
    };

    private void snap(String pkg) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastShot < 1100) return;                                         // platform rate-limits screenshots to ~1/sec
        lastShot = now;
        try {
            takeScreenshot(android.view.Display.DEFAULT_DISPLAY, L.IO, new TakeScreenshotCallback() {
                @Override public void onSuccess(ScreenshotResult res) {
                    try {
                        android.hardware.HardwareBuffer hb = res.getHardwareBuffer();
                        android.graphics.Bitmap hw = android.graphics.Bitmap.wrapHardwareBuffer(hb, res.getColorSpace());
                        android.graphics.Bitmap soft = hw == null ? null : hw.copy(android.graphics.Bitmap.Config.ARGB_8888, false);
                        if (hw != null) hw.recycle();
                        hb.close();
                        if (soft == null) return;
                        if (isBlank(soft)) { soft.recycle(); return; }                  // secure window / black splash: keep the icon
                        Thumbs.save(RemoteService.this, pkg, soft);                     // save() recycles it
                        if (pkg.equals(thumbPkg)) thumbGood = true;
                    } catch (Throwable ignored) {}
                }
                @Override public void onFailure(int err) {}
            });
        } catch (Throwable ignored) {}
    }

    /** True for a pure-black frame (secure windows such as Prime return black) or a flat solid splash — never shown. */
    private static boolean isBlank(android.graphics.Bitmap b) {
        int w = b.getWidth(), h = b.getHeight();
        if (w == 0 || h == 0) return true;
        int cols = 16, rows = 9, max = 0, min = 255;
        for (int yi = 0; yi < rows; yi++) {
            int y = h * (yi * 2 + 1) / (rows * 2);
            for (int xi = 0; xi < cols; xi++) {
                int x = w * (xi * 2 + 1) / (cols * 2);
                int c = b.getPixel(x, y);
                int lum = ((c >> 16 & 0xFF) + (c >> 8 & 0xFF) + (c & 0xFF)) / 3;
                if (lum > max) max = lum;
                if (lum < min) min = lum;
            }
        }
        return max < 24 || (max - min) < 10;
    }

    // ---- used by sleep timer, menus, phone page ----
    void sleepNow() { performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN); }

    void powerMenu() { performGlobalAction(GLOBAL_ACTION_POWER_DIALOG); }

    void back() { performGlobalAction(GLOBAL_ACTION_BACK); }

    void home() { performGlobalAction(GLOBAL_ACTION_HOME); }

    /** Open our Advanced menu from the phone remote. Background apps can't start activities on this firmware,
     *  so we go Home (allowed) and let the launcher open Advanced on resume (HomeActivity.onResume). */
    void openAdvanced() {
        openAdvancedAt = android.os.SystemClock.elapsedRealtime();
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    private AccessibilityNodeInfo field() {
        AccessibilityNodeInfo f = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        return f != null && f.isEditable() ? f : null;
    }

    /** Appends text (or replaces when replace=true). Returns false when no text field is selected. */
    boolean type(String text, boolean replace) {
        AccessibilityNodeInfo f = field();
        if (f == null) return false;
        CharSequence cur = f.getText();
        if (cur != null && f.isShowingHintText()) cur = "";
        String now = replace || cur == null ? text : cur + text;
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, now);
        return f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    boolean backspace() {
        AccessibilityNodeInfo f = field();
        if (f == null || f.getText() == null || f.isShowingHintText()) return false;
        CharSequence cur = f.getText();
        if (cur.length() == 0) return true;
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, cur.subSequence(0, cur.length() - 1));
        return f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    boolean enter() {
        AccessibilityNodeInfo f = field();
        return f != null && f.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
    }

    // ---- virtual mouse (phone trackpad): an overlay cursor + tap/scroll gestures ----
    private WindowManager wm;
    private View cursor;
    private boolean mouseActive;
    private int cx, cy, curSize;

    private int[] disp() {
        WindowManager w = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics m = new DisplayMetrics();
        w.getDefaultDisplay().getRealMetrics(m);
        return new int[]{m.widthPixels, m.heightPixels};
    }

    void mouseOn() {
        mouseActive = true;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (cursor == null) {
            int[] d = disp();
            cx = d[0] / 2; cy = d[1] / 2; curSize = L.s(24);
            View v = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(0xF2FFFFFF);
            g.setStroke(L.s(2), 0xFF101417);
            v.setBackground(g);
            v.setElevation(L.s(5));
            cursor = v;
        }
        ensureCursor();
    }

    void mouseOff() {
        mouseActive = false;
        if (cursor != null) { try { wm.removeView(cursor); } catch (Throwable ignored) {} }
        cursor = null;
    }

    private WindowManager.LayoutParams cursorLp() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(curSize, curSize,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = cx - curSize / 2; lp.y = cy - curSize / 2;
        return lp;
    }

    /** Re-attach the pointer if a window change detached it — that's why the mouse "died" until toggled off/on. */
    private void ensureCursor() {
        if (!mouseActive || cursor == null || wm == null || cursor.isAttachedToWindow()) return;
        try { wm.addView(cursor, cursorLp()); } catch (Throwable ignored) {}
    }

    void moveBy(float dx, float dy) {
        if (!mouseActive) return;
        ensureCursor();
        if (cursor == null || !cursor.isAttachedToWindow()) return;
        int[] d = disp();
        cx = Math.max(0, Math.min(d[0] - 1, cx + Math.round(dx)));
        cy = Math.max(0, Math.min(d[1] - 1, cy + Math.round(dy)));
        try { wm.updateViewLayout(cursor, cursorLp()); } catch (Throwable ignored) {}
    }

    boolean click() { ensureCursor(); return gesture(cx, cy, cx, cy, 55); }

    boolean rightClick() { ensureCursor(); return gesture(cx, cy, cx, cy, 650); }

    boolean scroll(boolean up) {
        ensureCursor();
        int[] d = disp();
        float amt = d[1] * 0.33f;
        float toY = up ? cy + amt : cy - amt;   // "up" reveals content above = finger swipes down
        return gesture(cx, cy, cx, Math.max(0, Math.min(d[1] - 1, toY)), 220);
    }

    private boolean gesture(float x1, float y1, float x2, float y2, long dur) {
        try {
            Path p = new Path();
            p.moveTo(x1, y1);
            if (x1 == x2 && y1 == y2) p.lineTo(x2 + 1, y2); else p.lineTo(x2, y2); // a tap needs a non-empty path
            GestureDescription g = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(p, 0, Math.max(1, dur))).build();
            return dispatchGesture(g, null, null);
        } catch (Throwable t) { return false; }
    }

    // ---- D-pad keys for the phone remote (a system app may inject input events) ----
    void key(int code) {
        long t = android.os.SystemClock.uptimeMillis();
        inject(new android.view.KeyEvent(t, t, android.view.KeyEvent.ACTION_DOWN, code, 0));
        inject(new android.view.KeyEvent(t, t, android.view.KeyEvent.ACTION_UP, code, 0));
    }

    private void inject(android.view.KeyEvent ev) {
        try {
            Object im = getSystemService("input");
            java.lang.reflect.Method m = im.getClass().getMethod("injectInputEvent", android.view.InputEvent.class, int.class);
            m.invoke(im, ev, 0); // INJECT_INPUT_EVENT_MODE_ASYNC
        } catch (Throwable ignored) {}
    }
}
