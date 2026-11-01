package de.lumen.home;

import android.service.dreams.DreamService;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Lumen screensaver: the nature photo with a large soft clock. Replaces the stock screensaver. */
public class Dream extends DreamService {
    private final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.GERMAN);
    private SimpleDateFormat dm;
    private TextView clock, date, caption;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (NowPlaying.anyPlaying(Dream.this)) { finish(); return; } // never cover active playback
            Date d = new Date();
            clock.setText(hm.format(d));
            date.setText(dm.format(d));
            caption.setText(Backdrop.caption);
            L.UI.postDelayed(this, 10_000);
        }
    };
    private Runnable photo;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Lang.init(this);
        if (NowPlaying.anyPlaying(this)) { finish(); return; } // something is playing: don't show the screensaver
        dm = Lang.EN ? new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
                     : new SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.GERMAN);
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        Theme t = Theme.current(this); // L.W/H/F already match this 1280x720 panel

        FrameLayout root = new FrameLayout(this);
        root.addView(new Backdrop.View0(this, t), new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        root.addView(new android.view.View(this) {
            { setBackgroundColor(0x33000000); }
        }, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        clock = Ui.one(this, "", 150, t.head(), 0xFFFFFFFF);
        clock.setEllipsize(null); // never truncate the time
        clock.setShadowLayer(L.s(16), 0, L.s(3), 0x80000000);
        date = Ui.one(this, "", 24, t.bold(), 0xFFFFFFFF);
        date.setShadowLayer(L.s(10), 0, L.s(2), 0x80000000);
        box.addView(clock);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        dp.topMargin = L.s(6);
        box.addView(date, dp);
        root.addView(box, new FrameLayout.LayoutParams(L.s(640), Ui.WRAP, Gravity.CENTER | Gravity.LEFT) {{ leftMargin = L.s(80); }});

        caption = Ui.one(this, "", 14, t.body(), 0xFFFFFFFF);
        caption.setShadowLayer(L.s(6), 0, L.s(1), 0xAA000000);
        root.addView(caption, new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM | Gravity.RIGHT) {{ rightMargin = L.s(44); bottomMargin = L.s(34); }});

        setContentView(root);
        Backdrop.next(this);
        L.UI.post(tick);
        photo = new Runnable() {
            @Override public void run() { Backdrop.next(Dream.this); L.UI.postDelayed(this, 60_000); }
        };
        L.UI.postDelayed(photo, 60_000);
    }

    @Override public void onDetachedFromWindow() {
        L.UI.removeCallbacks(tick);
        if (photo != null) L.UI.removeCallbacks(photo);
        super.onDetachedFromWindow();
    }
}
