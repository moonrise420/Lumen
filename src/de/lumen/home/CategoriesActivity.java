package de.lumen.home;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Manage favourite categories: add, rename, delete, reorder. */
public class CategoriesActivity extends Base {
    private List<Fav> cats;
    private LinearLayout list;

    @Override void build() {
        TextView title = Ui.one(this, "Kategorien", 30, t.head(), t.text);
        title.setShadowLayer(L.s(8), 0, L.s(1), 0x88000000);
        root.addView(title, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 36));
        TextView sub = Ui.one(this, "OK: Optionen (umbenennen, nach oben/unten, löschen)", 15, t.body(), t.dim);
        sub.setShadowLayer(L.s(6), 0, L.s(1), 0x88000000);
        root.addView(sub, Ui.at(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT, 64, 80));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setPadding(L.s(56), L.s(8), L.s(56), L.s(40));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> list.invalidate());
        root.addView(sv, Ui.at(Ui.MATCH, Ui.MATCH, Gravity.TOP | Gravity.LEFT, 0, 120));

        rebuild(0);
    }

    private void rebuild(int focus) {
        cats = Fav.load(this);
        list.removeAllViews();
        for (int i = 0; i < cats.size(); i++) {
            final int ci = i;
            Fav c = cats.get(i);
            Ui.Row r = Ui.action(this, t, "palette", c.name, c.pkgs.size() + (c.pkgs.size() == 1 ? " App" : " Apps"), () -> "", () -> edit(ci));
            add(r);
        }
        add(Ui.action(this, t, "plus", "Neue Kategorie", null, () -> "", () -> Keyboard.ask(this, t, "Name der Kategorie", "", name -> {
            if (name == null || name.trim().isEmpty()) return;
            cats.add(new Fav(name.trim(), new java.util.ArrayList<>()));
            Fav.save(this, cats);
            rebuild(cats.size() - 1);
        })));
        int f = Math.max(0, Math.min(focus, list.getChildCount() - 1));
        if (list.getChildCount() > 0) list.getChildAt(f).requestFocus();
    }

    private void add(Ui.Row r) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.topMargin = L.s(7);
        list.addView(r, lp);
    }

    private void edit(int i) {
        Fav c = cats.get(i);
        menu(c.name, new String[]{"Umbenennen", "Nach oben", "Nach unten", "Löschen (Apps gehen in die erste Kategorie)"}, sel -> {
            switch (sel) {
                case 0:
                    Keyboard.ask(this, t, "Neuer Name", c.name, name -> {
                        if (name == null || name.trim().isEmpty()) return;
                        cats.set(i, new Fav(name.trim(), c.pkgs));
                        Fav.save(this, cats);
                        rebuild(i);
                    });
                    break;
                case 1:
                    if (i > 0) { cats.remove(i); cats.add(i - 1, c); Fav.save(this, cats); rebuild(i - 1); }
                    break;
                case 2:
                    if (i < cats.size() - 1) { cats.remove(i); cats.add(i + 1, c); Fav.save(this, cats); rebuild(i + 1); }
                    break;
                case 3:
                    if (cats.size() <= 1) { L.toast(this, "Mindestens eine Kategorie muss bleiben"); return; }
                    cats.remove(i);
                    int target = i == 0 ? 0 : 0;
                    for (String p : c.pkgs) if (!cats.get(target).pkgs.contains(p)) cats.get(target).pkgs.add(p);
                    Fav.save(this, cats);
                    rebuild(Math.max(0, i - 1));
                    break;
            }
        });
    }
}
