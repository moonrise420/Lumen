package de.lumen.home;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Consumer;

/** Small text-input overlay that uses the system on-screen keyboard. */
final class Keyboard {
    private Keyboard() {}

    static void ask(Base a, Theme t, String title, String preset, Consumer<String> done) {
        FrameLayout scrim = new FrameLayout(a);
        scrim.setBackgroundColor(0x99000000);
        scrim.setClickable(true);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(L.s(26), L.s(22), L.s(26), L.s(22));
        Ui.glass(box, t, Theme.alpha(t.panel, Math.max(0xD0, t.panel >>> 24)), L.s(t.radius + 4));
        TextView h = Ui.one(a, title, 22, t.head(), t.text);
        h.setPadding(L.s(4), 0, 0, L.s(12));
        box.addView(h);

        EditText in = new EditText(a);
        in.setText(preset == null ? "" : preset);
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT);
        in.setTextColor(t.text);
        in.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, L.s(20));
        in.setBackground(Ui.round(Theme.alpha(t.text, 0x22), L.s(10)));
        in.setPadding(L.s(16), L.s(12), L.s(16), L.s(12));
        in.setSelectAllOnFocus(true);
        box.addView(in, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        LinearLayout btns = new LinearLayout(a);
        btns.setPadding(0, L.s(14), 0, 0);
        Runnable close = () -> { a.root.removeView(scrim); a.restoreBackgroundFocus(); };
        LinearLayout ok = Ui.chip(a, t, "size", "Übernehmen", v -> { close.run(); done.accept(in.getText().toString()); });
        LinearLayout cancel = Ui.chip(a, t, "home", "Abbrechen", v -> close.run());
        btns.addView(ok);
        View sp = new View(a);
        sp.setLayoutParams(new LinearLayout.LayoutParams(L.s(12), 1));
        btns.addView(sp);
        btns.addView(cancel);
        box.addView(btns);

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(L.s(620), Ui.WRAP, Gravity.CENTER);
        scrim.addView(box, bp);
        a.root.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        a.blockBackgroundFocus(scrim);

        in.requestFocus();
        in.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(in, InputMethodManager.SHOW_IMPLICIT);
        }, 150);
        in.setOnEditorActionListener((v, actionId, e) -> { close.run(); done.accept(in.getText().toString()); return true; });
    }
}
