package de.lumen.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.function.IntConsumer;

/** Every screen: photo backdrop, theme, overlay menus, recreate when the look changes. */
abstract class Base extends Activity {
    Theme t;
    FrameLayout root;
    private String sig;
    private String langSig;
    private View overlay;
    private View focusBeforeOverlay;
    private final ArrayList<ViewGroup> blocked = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        L.init(this);
        Lang.init(this);
        L.BOLD = L.p(this).getBoolean("boldText", false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        t = Theme.current(this);
        sig = Theme.signature(this);
        langSig = L.p(this).getString("lang", "auto");
        root = new FrameLayout(this);
        root.addView(new Backdrop.View0(this, t), new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        setContentView(root);
        build();
        if (Backdrop.photo == null && !(this instanceof HomeActivity)) Backdrop.next(this);
    }

    abstract void build();

    @Override protected void onResume() {
        super.onResume();
        if (!sig.equals(Theme.signature(this)) || !langSig.equals(L.p(this).getString("lang", "auto"))
                || L.BOLD != L.p(this).getBoolean("boldText", false)) recreate();
    }

    @Override public void onBackPressed() {
        if (overlay != null) closeOverlay(); else back();
    }

    void back() { super.onBackPressed(); }

    boolean overlayOpen() { return overlay != null; }

    void closeOverlay() {
        if (overlay == null) return;
        root.removeView(overlay);
        overlay = null;
        restoreBackgroundFocus();
        if (focusBeforeOverlay != null) focusBeforeOverlay.requestFocus();
    }

    /** Confine D-pad focus to the overlay so up/down can't escape into the list behind it. */
    void blockBackgroundFocus(View keep) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View c = root.getChildAt(i);
            if (c == keep || !(c instanceof ViewGroup)) continue;
            ViewGroup g = (ViewGroup) c;
            if (g.getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS) continue;
            g.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            blocked.add(g);
        }
    }

    void restoreBackgroundFocus() {
        for (ViewGroup g : blocked) g.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        blocked.clear();
    }

    /** Centered glass menu. */
    void menu(String title, String[] items, IntConsumer pick) {
        closeOverlay();
        focusBeforeOverlay = getCurrentFocus();
        FrameLayout scrim = new FrameLayout(this);
        scrim.setBackgroundColor(0x99000000);
        scrim.setClickable(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(L.s(26), L.s(24), L.s(26), L.s(24));
        Ui.glass(box, t, Theme.alpha(t.panel, Math.max(0xD0, t.panel >>> 24)), L.s(t.radius + 4));
        TextView h = Ui.one(this, title, 24, t.head(), t.text);
        h.setPadding(L.s(4), 0, 0, L.s(14));
        box.addView(h);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            Ui.Row r = Ui.action(this, t, null, items[i], null, null, () -> { closeOverlay(); pick.accept(idx); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
            lp.topMargin = L.s(6);
            list.addView(r, lp);
        }
        ScrollView sv = new ScrollView(this);
        sv.setClipChildren(false);
        sv.setClipToPadding(false);
        sv.addView(list);
        box.addView(sv, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(L.s(520), Ui.WRAP, Gravity.CENTER);
        bp.topMargin = L.s(40);
        bp.bottomMargin = L.s(40);
        scrim.addView(box, bp);
        root.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        overlay = scrim;
        blockBackgroundFocus(scrim);
        if (list.getChildCount() > 0) list.getChildAt(0).requestFocus();
    }
}
