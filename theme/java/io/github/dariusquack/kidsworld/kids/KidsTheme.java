package io.github.dariusquack.kidsworld.kids;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.gathin.tvhome.R;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dariusquack.kidsworld.AppEntry;
import io.github.dariusquack.kidsworld.HomeActivity;
import io.github.dariusquack.kidsworld.HomeApps;
import io.github.dariusquack.kidsworld.HomeTheme;
import io.github.dariusquack.kidsworld.IconArt;
import io.github.dariusquack.kidsworld.IconColors;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsArt;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsDb;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsGate;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsLanguage;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsLock;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsProfile;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsProfiles;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsTime;
import io.github.dariusquack.kidsworld.addon.kidsmode.ParentActivity;

/**
 * Theme „Kids World“: große, schwingende App-Kacheln vor vier Welten, die Hoch/Runter wechselt. Bewusst ohne
 * Einstellungen-Kachel.
 *
 * <p>Das Theme zeigt nur an. Profile, Freigaben, PIN und Wächter gehören dem Add-on Kids Mode (Addons/KidsMode):
 * Das Kind sieht die Apps seines Profils, OK 5 Sekunden öffnet den Elternbereich.
 */
public final class KidsTheme implements HomeTheme {
    private HomeActivity host;
    private FrameLayout root;
    private KidsView stage;
    private KidsLock lock;
    private KidsProfiles profiles;
    private TextView worldTitle, empty, profileName;
    private ImageView avatar;
    private final Map<World, Bitmap> symbols = new EnumMap<>(World.class);
    private static final long TOAST_MS = 3500, GATE_CHECK_MS = 5_000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable gateCheck = this::checkGate;
    private final Runnable hideToast = this::hideToast;
    private KidsGate gate;
    private String shownProfile, shownRow, shownLanguage;
    private View toast, block, hint;
    private ImageView toastArt;
    private TextView toastText, blockTitle, blockText;
    private View chip;
    private TextView timePill;
    private KidsDb db;
    private TaskView taskView;
    private long seenCredit = -1;
    private LinearLayout dots;
    private ProfilePicker picker;
    private Typeface font;
    private List<AppEntry> apps = new ArrayList<>();
    private final List<AppEntry> shown = new ArrayList<>();
    private final IconColors colors = new IconColors(0xFFFF9F43);

    @Override
    public View create(HomeActivity host) {
        this.host = host;
        KidsLanguage.apply(host);
        shownLanguage = KidsLanguage.shown(host);
        lock = new KidsLock(host);
        profiles = new KidsProfiles(host);
        gate = new KidsGate(host);
        db = KidsDb.get(host);
        font = host.getResources().getFont(R.font.baloo2_bold);
        root = (FrameLayout) LayoutInflater.from(host).inflate(R.layout.sc_kids_home, null);
        stage = new KidsView(host, font);
        root.addView(stage, 0, new FrameLayout.LayoutParams(-1, -1));
        worldTitle = root.findViewById(R.id.scWorldTitle);
        dots = root.findViewById(R.id.scWorldDots);
        empty = root.findViewById(R.id.scKidsEmpty);
        chip = root.findViewById(R.id.scProfileChip);
        avatar = root.findViewById(R.id.scProfileAvatar);
        profileName = root.findViewById(R.id.scProfileName);
        toast = root.findViewById(R.id.scKidsToast);
        toastArt = root.findViewById(R.id.scKidsToastArt);
        toastText = root.findViewById(R.id.scKidsToastText);
        block = root.findViewById(R.id.scKidsBlock);
        blockTitle = root.findViewById(R.id.scKidsBlockTitle);
        blockText = root.findViewById(R.id.scKidsBlockText);
        hint = root.findViewById(R.id.scKidsHint);
        timePill = root.findViewById(R.id.scKidsTime);
        stage.setListener(new KidsView.Listener() {
            @Override
            public void onLaunch(int index) {
                if (index >= shown.size()) showTasks();
                else if (gate.mayRun(shown.get(index).pkg)) host.launch(shown.get(index));
                else if (gate.block() == KidsTime.Block.LIMIT) showToast(host.getString(R.string.sc_kids_limit_text), false);
                else checkGate();
            }

            @Override
            public void onWorldChanged(World world) {
                KidsProfile p = profiles.active();
                p.world = world.name();
                profiles.save(p);
                shownProfile = stateKey();
                showWorld(world);
            }

            @Override
            public void onParentRequest() {
                openParents();
            }
        });
        TextClock clock = root.findViewById(R.id.scClock);
        // Immer 24 Stunden, auch wenn das Gerät auf 12 Stunden steht.
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        return root;
    }

    private void hideToast() {
        toast.setVisibility(View.GONE);
    }

    /** Kurzer Hinweis unten; das Maskottchen freut sich auf Wunsch mit. */
    private void showToast(String text, boolean cheer) {
        toastArt.setVisibility(View.GONE);
        toastText.setText(text);
        toast.setVisibility(View.VISIBLE);
        if (cheer) stage.cheer();
        handler.removeCallbacks(hideToast);
        handler.postDelayed(hideToast, TOAST_MS);
    }

    /** Zeigt „Zeit ist um“, solange Tageslimit, Schlafenszeit oder Sperre gelten. Hält OK öffnet weiter die Eltern. */
    private void checkGate() {
        // Eltern können am Handy ändern, während die Startseite offen ist.
        if (!stateKey().equals(shownProfile) && picker == null) applyProfile();
        stage.setCalm(lock.calmMotion());
        KidsTime.Block b = gate.block();
        boolean blocked = gate.fullBlock(), was = blocked();
        showTime();
        showCredit();
        block.setVisibility(blocked ? View.VISIBLE : View.GONE);
        hint.setVisibility(blocked ? View.GONE : View.VISIBLE);
        stage.setLocked(blocked);
        if (blocked) {
            if (picker != null) closePicker();
            if (taskView != null) closeTasks();
        } else if (taskView != null) {
            refreshTasks();
        } else if (was && profiles.needsPick()) {
            showPicker();
        }
        if (b == KidsTime.Block.BEDTIME) {
            blockTitle.setText(R.string.sc_kids_bed_title);
            blockText.setText(R.string.sc_kids_bed_text);
        } else if (b == KidsTime.Block.LOCKED) {
            blockTitle.setText(R.string.sc_kids_lock_title);
            blockText.setText(R.string.sc_kids_lock_text);
        }
        handler.removeCallbacks(gateCheck);
        handler.postDelayed(gateCheck, GATE_CHECK_MS);
    }

    /** Restzeit oben rechts; ohne Zeitkonto nichts. */
    private void showTime() {
        KidsProfile p = profiles.active();
        int left = gate.minutesLeft(p);
        timePill.setVisibility(left < 0 ? View.GONE : View.VISIBLE);
        if (left > 0) timePill.setText(host.getString(R.string.sc_kids_time_pill, left));
        else if (left == 0) timePill.setText(R.string.sc_kids_time_over);
    }

    /** Neue Gutschrift der Eltern: Hinweis, und das Maskottchen freut sich. */
    private void showCredit() {
        KidsDb.Entry e = db.lastCredit(profiles.active().id);
        long id = e != null ? e.id : 0;
        if (seenCredit >= 0 && id > seenCredit && e.minutes > 0) {
            showToast(host.getString(R.string.sc_kids_credit, e.minutes, e.note), true);
        }
        seenCredit = id;
    }

    private void showTasks() {
        taskView = new TaskView(host, font, this::markTask);
        root.addView(taskView, new FrameLayout.LayoutParams(-1, -1));
        refreshTasks();
    }

    private void refreshTasks() {
        KidsProfile p = profiles.active();
        KidsDb.Account a = db.account(p);
        int focus = taskView.focused();
        taskView.show(p.timeOn, gate.available(p), (int) (a.usedMs / 60_000), gate.minutesLeft(p), a.extra, db.tasks(p.id), focus);
    }

    private void markTask(KidsDb.Task t) {
        if (db.markDone(profiles.active(), t.id)) refreshTasks();
    }

    private void closeTasks() {
        root.removeView(taskView);
        taskView = null;
        stage.requestFocus();
    }

    private boolean blocked() {
        return block.getVisibility() == View.VISIBLE;
    }

    /** Alles, was das Handy ändern kann und die Startseite betrifft. */
    private String stateKey() {
        return profiles.active().encode() + profiles.list().size() + "/" + openTasks();
    }

    private int openTasks() {
        int n = 0;
        for (KidsDb.Task t : db.tasks(profiles.active().id)) {
            if (t.today && (KidsDb.OPEN.equals(t.status) || KidsDb.REJECTED.equals(t.status))) n++;
        }
        return n;
    }

    /** Die Kachel „Meine Aufgaben“ steht am Ende der Reihe. */
    private KidsTile.Data taskTile() {
        Drawable d = host.getDrawable(R.drawable.sc_kids_tasks);
        int open = openTasks();
        String label = open > 0 ? host.getString(R.string.sc_kids_tasks_count, open) : host.getString(R.string.sc_kids_tasks);
        return new KidsTile.Data(d, IconArt.analyze(d), label, 0xFFFFC233);
    }

    private void openParents() {
        host.startActivity(new Intent(host, ParentActivity.class));
    }

    private void showWorld(World world) {
        worldTitle.setText(world.title);
        dots.removeAllViews();
        int density = Math.round(host.getResources().getDisplayMetrics().density);
        for (World w : World.values()) {
            // Welt-Symbole: die aktuelle groß und klar, die anderen klein und gedämpft.
            ImageView d = new ImageView(host);
            d.setImageBitmap(symbol(w));
            d.setAlpha(w == world ? 1f : 0.6f);
            int size = (w == world ? 38 : 24) * density;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.gravity = Gravity.CENTER_VERTICAL;
            lp.setMargins(5 * density, 0, 5 * density, 0);
            dots.addView(d, lp);
        }
    }

    private Bitmap symbol(World w) {
        Bitmap b = symbols.get(w);
        if (b == null) {
            b = WorldPainter.symbol(host, w);
            if (b != null) symbols.put(w, b);
        }
        return b;
    }

    /** Zeigt das aktive Profil: Avatar und Name, seine Welt, seine Apps. */
    private void applyProfile() {
        KidsProfile p = profiles.active();
        avatar.setImageBitmap(KidsArt.avatar(host, p));
        profileName.setText(p.name);
        shownProfile = stateKey();
        seenCredit = -1;
        World w = World.named(p.world);
        stage.setCalm(lock.calmMotion());
        stage.setWorld(w);
        showWorld(w);
        rebuild();
    }

    @Override
    public void showApps(List<AppEntry> list) {
        apps = list;
        rebuild();
    }

    @Override
    public void onResume() {
        // Zurück auf der Kinder-Startseite: eine Wächter-Pause aus dem Elternbereich endet hier.
        lock.resumeGuard();
        if (!shownLanguage.equals(KidsLanguage.shown(host))) {
            host.recreate();
            return;
        }
        applyProfile();
        stage.setRunning(true);
        KidsGate.setListener(() -> handler.post(this::checkGate));
        checkGate();
        if (blocked()) return;
        if (profiles.needsPick()) {
            showPicker();
        } else if (picker == null) {
            stage.requestFocus();
        }
    }

    @Override
    public void onPause() {
        stage.setRunning(false);
        KidsGate.setListener(null);
        handler.removeCallbacks(gateCheck);
    }

    @Override
    public void onHomePressed() {
        if (picker != null || blocked()) return;
        if (taskView != null) closeTasks();
        stage.goHome();
    }

    @Override
    public void onBackPressed() {
        if (blocked()) return;
        if (picker != null) {
            // Zurück schließt die Auswahl, außer ein Kind muss erst gewählt werden.
            if (!profiles.needsPick()) {
                closePicker();
                stage.requestFocus();
            }
            return;
        }
        if (taskView != null) {
            closeTasks();
            return;
        }
        // Zurück: erst zur ersten App, von dort zur Profilauswahl.
        if (stage.selected() == 0) showPicker();
        else stage.goHome();
    }

    private void showPicker() {
        if (picker != null) return;
        picker = new ProfilePicker(host, font, profiles.list(), new ProfilePicker.Listener() {
            @Override
            public void onPick(KidsProfile p) {
                profiles.setActive(p);
                closePicker();
                applyProfile();
                checkGate();
            }

            @Override
            public void onParents() {
                closePicker();
                openParents();
            }
        });
        root.addView(picker, new FrameLayout.LayoutParams(-1, -1));
        picker.focusFirst();
    }

    private void closePicker() {
        root.removeView(picker);
        picker = null;
        stage.requestFocus();
    }

    /** Apps des aktiven Profils: Favoriten zuerst, dann zuletzt benutzte. */
    private void rebuild() {
        if (host == null) return;
        KidsProfile p = profiles.active();
        Set<String> allowed = p.allowed;
        allowed.removeAll(KidsLock.settingsPackages(host));
        String keep = stage.selected() < shown.size() ? shown.get(stage.selected()).pkg : null;
        shown.clear();
        List<AppEntry> rest = new ArrayList<>();
        for (AppEntry a : HomeApps.recentFirst(host, apps)) {
            if (!allowed.contains(a.pkg)) continue;
            if (p.favorites.contains(a.pkg)) shown.add(a); else rest.add(a);
        }
        shown.addAll(rest);
        List<KidsTile.Data> tiles = new ArrayList<>();
        for (AppEntry a : shown) {
            Drawable icon = a.icon != null ? a.icon.mutate() : null;
            tiles.add(new KidsTile.Data(icon, a.iconShape, a.label, colors.of(a)));
        }
        tiles.add(taskTile());
        // Nur neu aufbauen, wenn sich die Reihe wirklich ändert; die gewählte App bleibt gewählt.
        StringBuilder key = new StringBuilder(String.valueOf(openTasks()));
        int keepIndex = 0;
        for (int i = 0; i < shown.size(); i++) {
            key.append(shown.get(i).pkg).append(',');
            if (shown.get(i).pkg.equals(keep)) keepIndex = i;
        }
        if (!key.toString().equals(shownRow)) {
            shownRow = key.toString();
            stage.setTiles(tiles, keepIndex);
        }
        empty.setVisibility(shown.isEmpty() && !apps.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
