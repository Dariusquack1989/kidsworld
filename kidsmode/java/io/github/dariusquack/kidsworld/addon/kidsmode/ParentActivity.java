package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gathin.tvhome.R;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import io.github.dariusquack.kidsworld.AppEntry;
import io.github.dariusquack.kidsworld.AppRepository;
import io.github.dariusquack.kidsworld.HomeApps;

/**
 * Elternbereich von Kids Mode: erst die PIN (beim ersten Mal festlegen), dann Kinder verwalten (Name, Farbe, Apps,
 * Favoriten), Einstellungen öffnen, PIN ändern. Schließt sich nach zwei Minuten ohne Eingabe von selbst, damit ein offen gelassener
 * Elternbereich nicht beim Kind landet; beim Verlassen ist er sofort wieder zu (noHistory).
 */
public final class ParentActivity extends Activity {
    private static final long IDLE_MS = 120_000;

    private enum Mode { CHECK, NEW, REPEAT }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable idle = this::finish;
    private KidsLock lock;
    private KidsProfiles profiles;
    private KidsGate gate;
    private KidsDb db;
    private LinearLayout root;
    private Typeface regular, medium;
    /** Position des Sprach-Knopfs im Menü: Er behält den Fokus, damit man weiterschalten kann. */
    private int languageRow;

    // PIN-Eingabe
    private Mode mode;
    private final StringBuilder entered = new StringBuilder();
    private String firstPin;
    private TextView pinTitle, pinDots, pinError;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        KidsLanguage.apply(this);
        lock = new KidsLock(this);
        profiles = new KidsProfiles(this);
        gate = new KidsGate(this);
        db = KidsDb.get(this);
        regular = getResources().getFont(R.font.outfit_regular);
        medium = getResources().getFont(R.font.outfit_medium);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setMinimumHeight(getResources().getDisplayMetrics().heightPixels);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(64), dp(40), dp(64), dp(32));
        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        // Das Bild gehört zur Seite, nicht zum Inhalt: Es bleibt stehen, wenn lange Menüs scrollen.
        page.setBackgroundColor(0xFF14122B);
        android.graphics.Bitmap back = KidsArt.load(this, "hintergruende/hintergrund_elternbereich.jpg", 1);
        if (back != null) page.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{
                new android.graphics.drawable.BitmapDrawable(getResources(), back),
                new android.graphics.drawable.ColorDrawable(0xB314122B)}));
        page.addView(root);
        setContentView(page);
        showPin(lock.hasPin() ? Mode.CHECK : Mode.NEW);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onUserInteraction();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(idle);
        super.onPause();
    }

    @Override
    public void onUserInteraction() {
        handler.removeCallbacks(idle);
        handler.postDelayed(idle, IDLE_MS);
    }

    // ---------- PIN ----------

    private void showPin(Mode m) {
        mode = m;
        entered.setLength(0);
        root.removeAllViews();
        root.addView(text(getString(R.string.sc_parent_title), 30, medium, Color.WHITE));
        pinTitle = text(getString(m == Mode.CHECK ? R.string.sc_parent_pin_enter
                : m == Mode.NEW ? R.string.sc_parent_pin_new : R.string.sc_parent_pin_repeat), 18, regular, 0xCCFFFFFF);
        pinTitle.setPadding(0, dp(8), 0, 0);
        root.addView(pinTitle);
        pinDots = text("", 40, medium, Color.WHITE);
        pinDots.setPadding(0, dp(12), 0, 0);
        root.addView(pinDots);
        pinError = text("", 15, regular, 0xFFFF8A8A);
        root.addView(pinError);

        GridLayout pad = new GridLayout(this);
        pad.setColumnCount(3);
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", ""};
        for (String k : keys) {
            TextView b = button(k, 26);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(96);
            lp.height = dp(64);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            if (k.isEmpty()) {
                b.setFocusable(false);
                b.setBackground(null);
            } else if (k.equals("⌫")) {
                b.setContentDescription(getString(R.string.sc_parent_delete));
                b.setOnClickListener(v -> deleteDigit());
            } else {
                b.setOnClickListener(v -> addDigit(k.charAt(0)));
            }
            pad.addView(b, lp);
        }
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-2, -2);
        plp.topMargin = dp(12);
        root.addView(pad, plp);
        updateDots();
        pad.getChildAt(4).requestFocus();
        long locked = lock.lockedFor();
        if (m == Mode.CHECK && locked > 0) showLocked(locked);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Zifferntasten der Fernbedienung gehen direkt in die PIN.
        if (mode != null && pinDots != null && pinDots.isAttachedToWindow()) {
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                addDigit((char) ('0' + keyCode - KeyEvent.KEYCODE_0));
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                deleteDigit();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void addDigit(char c) {
        if (entered.length() >= PinHash.LENGTH) return;
        entered.append(c);
        pinError.setText("");
        updateDots();
        if (entered.length() == PinHash.LENGTH) handler.postDelayed(this::submitPin, 150);
    }

    private void deleteDigit() {
        if (entered.length() > 0) entered.setLength(entered.length() - 1);
        updateDots();
    }

    private void updateDots() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < PinHash.LENGTH; i++) s.append(i < entered.length() ? "●" : "○").append(i < 3 ? "  " : "");
        pinDots.setText(s);
    }

    private void submitPin() {
        String pin = entered.toString();
        entered.setLength(0);
        switch (mode) {
            case CHECK:
                KidsLock.Result r = lock.check(pin);
                if (r == KidsLock.Result.OK) {
                    showMenu();
                    return;
                }
                if (r == KidsLock.Result.LOCKED) {
                    showLocked(lock.lockedFor());
                } else {
                    pinError.setText(R.string.sc_parent_pin_wrong);
                }
                break;
            case NEW:
                firstPin = pin;
                showPin(Mode.REPEAT);
                return;
            case REPEAT:
                if (pin.equals(firstPin)) {
                    lock.setPin(pin);
                    firstPin = null;
                    showMenu();
                    return;
                }
                showPin(Mode.NEW);
                pinError.setText(R.string.sc_parent_pin_mismatch);
                return;
        }
        updateDots();
    }

    private void showLocked(long ms) {
        pinError.setText(getString(R.string.sc_parent_pin_locked, (int) Math.ceil(ms / 1000.0)));
    }

    // ---------- Menü ----------

    private void showMenu() {
        mode = null;
        root.removeAllViews();
        root.addView(text(getString(R.string.sc_parent_title), 30, medium, Color.WHITE));
        boolean guard = lock.guardEnabled();
        TextView status = text(getString(guard ? R.string.sc_parent_guard_on : R.string.sc_parent_guard_off), 15,
                regular, guard ? 0xFF7CF5B0 : 0xFFFFB37A);
        status.setPadding(0, dp(6), 0, dp(4));
        root.addView(status);
        String address = guard ? KidsRemote.address() : null;
        TextView phone = text(address != null ? getString(R.string.sc_parent_phone, address)
                : getString(R.string.sc_parent_phone_off), 15, regular, 0xCCFFFFFF);
        phone.setPadding(0, 0, 0, dp(14));
        root.addView(phone);

        int waiting = db.waiting().size();
        View first = addButton(getString(R.string.sc_parent_pending, waiting), v -> showPending());
        menuButton(R.string.sc_parent_menu_apps, v -> showAppChildren());
        menuButton(R.string.sc_parent_children, v -> showChildren());
        menuButton(lock.calmMotion() ? R.string.sc_parent_calm_on : R.string.sc_parent_calm_off, v -> {
            lock.setCalmMotion(!lock.calmMotion());
            showMenu();
        });
        String language = lock.language();
        View languageButton = menuButton(KidsLanguage.GERMAN.equals(language) ? R.string.sc_parent_lang_de
                : KidsLanguage.ENGLISH.equals(language) ? R.string.sc_parent_lang_en : R.string.sc_parent_lang_auto, v -> {
            lock.setLanguage(KidsLanguage.next(lock.language()));
            KidsLanguage.apply(this);
            showMenu();
            root.getChildAt(languageRow).requestFocus();
        });
        languageRow = root.indexOfChild(languageButton);
        menuButton(R.string.sc_parent_settings, v -> {
            lock.pauseGuard();
            HomeApps.openSettings(this);
        });
        menuButton(R.string.sc_parent_change_pin, v -> showPin(Mode.NEW));
        menuButton(R.string.sc_parent_forget_phones, v -> KidsRemote.forgetDevices(this));
        menuButton(R.string.sc_parent_done, v -> finish());
        first.requestFocus();
    }

    private View menuButton(int label, View.OnClickListener click) {
        TextView b = button(getString(label), 20);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(520), dp(52));
        lp.topMargin = dp(8);
        root.addView(b, lp);
        return b;
    }

    // ---------- Bestätigungen ----------

    private void showPending() {
        root.removeAllViews();
        root.addView(text(getString(R.string.sc_parent_title_pending), 28, medium, Color.WHITE));
        View first = null;
        for (KidsDb.Task t : db.waiting()) {
            KidsProfile p = profiles.byId(t.kid);
            String who = (p != null ? p.name : "?") + ": " + t.title + " (+" + t.minutes + ")";
            View b = addButton(getString(R.string.sc_parent_confirm_task, who), v -> {
                db.confirm(profiles, t.runId);
                showPending();
            });
            addButton(getString(R.string.sc_parent_reject_task), v -> {
                db.reject(t.runId, "");
                showPending();
            });
            if (first == null) first = b;
        }
        View done = addButton(getString(R.string.sc_parent_done), v -> showMenu());
        (first != null ? first : done).requestFocus();
    }

    // ---------- Kinder ----------

    private void showChildren() {
        root.removeAllViews();
        root.addView(text(getString(R.string.sc_parent_children), 30, medium, Color.WHITE));
        View first = null;
        for (KidsProfile p : profiles.list()) {
            View b = addButton(p.name, v -> showChild(p.id));
            if (first == null) first = b;
        }
        addButton(getString(R.string.sc_parent_new_child), v -> askName());
        addButton(getString(R.string.sc_parent_done), v -> showMenu());
        if (first != null) first.requestFocus();
    }

    /** Direkter Weg zu den Apps: Bei einem Kind sofort die Liste, sonst erst das Kind wählen. */
    private void showAppChildren() {
        List<KidsProfile> kids = profiles.list();
        if (kids.size() == 1) {
            showApps(kids.get(0).id, this::showMenu);
            return;
        }
        root.removeAllViews();
        root.addView(text(getString(R.string.sc_parent_menu_apps), 30, medium, Color.WHITE));
        View first = null;
        for (KidsProfile p : kids) {
            View b = addButton(p.name, v -> showApps(p.id, this::showAppChildren));
            if (first == null) first = b;
        }
        addButton(getString(R.string.sc_parent_done), v -> showMenu());
        first.requestFocus();
    }

    private void askName() {
        EditText name = new EditText(this);
        name.setSingleLine(true);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.sc_parent_child_name)
                .setView(name)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (!n.isEmpty()) showChild(profiles.add(n).id);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private KidsProfile child(String id) {
        for (KidsProfile p : profiles.list()) {
            if (p.id.equals(id)) return p;
        }
        return profiles.active();
    }

    private void showChild(String id) {
        KidsProfile p = child(id);
        root.removeAllViews();
        ImageView face = new ImageView(this);
        face.setImageBitmap(KidsArt.avatar(this, p));
        root.addView(face, new LinearLayout.LayoutParams(dp(84), dp(84)));
        root.addView(text(p.name, 26, medium, Color.WHITE));
        root.addView(text(getString(R.string.sc_parent_used, (int) (db.account(p).usedMs / 60_000)), 14, regular, 0xCCFFFFFF));
        View first = addButton(getString(R.string.sc_parent_child_apps), v -> showApps(p.id, () -> showChild(p.id)));
        addButton(getString(R.string.sc_parent_child_avatar), v -> {
            KidsProfile c = child(p.id);
            c.avatar = (c.avatar + 1) % KidsProfile.AVATARS.length;
            profiles.save(c);
            showChild(p.id);
        });
        addButton(limitLabel(p), v -> {
            KidsProfile c = child(p.id);
            c.baseMin = next(LIMITS, c.baseMin);
            c.timeOn = c.baseMin > 0;
            if (c.maxTotalMin > 0 && c.maxTotalMin < c.baseMin) c.maxTotalMin = c.baseMin;
            profiles.save(c);
            showChild(p.id);
        });
        addButton(bedLabel(p), v -> {
            KidsProfile c = child(p.id);
            c.bedFrom = next(BEDTIMES, c.bedFrom);
            c.bedTo = c.bedFrom < 0 ? -1 : BED_END;
            profiles.save(c);
            showChild(p.id);
        });
        addButton(getString(R.string.sc_parent_bonus), v -> {
            db.correct(child(p.id), BONUS_MIN, getString(R.string.sc_parent_bonus));
            showChild(p.id);
        });
        if (profiles.list().size() > 1) {
            addButton(getString(R.string.sc_parent_child_delete), v -> {
                db.deleteTasksOf(p.id);
                profiles.remove(child(p.id));
                showChildren();
            });
        }
        addButton(getString(R.string.sc_parent_done), v -> showChildren());
        first.requestFocus();
    }

    private static final int[] LIMITS = {0, 30, 60, 90, 120, 180};
    private static final int[] BEDTIMES = {-1, 18 * 60, 19 * 60, 19 * 60 + 30, 20 * 60, 20 * 60 + 30, 21 * 60};
    private static final int BED_END = 7 * 60;
    private static final int BONUS_MIN = 30;

    private static int next(int[] options, int current) {
        for (int i = 0; i < options.length; i++) {
            if (options[i] == current) return options[(i + 1) % options.length];
        }
        return options[0];
    }

    private String limitLabel(KidsProfile p) {
        return p.timeOn ? getString(R.string.sc_parent_limit, p.baseMin) : getString(R.string.sc_parent_limit_off);
    }

    private String bedLabel(KidsProfile p) {
        if (p.bedFrom < 0) return getString(R.string.sc_parent_bed_off);
        return getString(R.string.sc_parent_bed, p.bedFrom / 60, p.bedFrom % 60, p.bedTo / 60, p.bedTo % 60);
    }

    private View addButton(String label, View.OnClickListener click) {
        TextView b = button(label, 18);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(520), dp(44));
        lp.topMargin = dp(6);
        root.addView(b, lp);
        return b;
    }

    // ---------- Apps eines Kindes ----------

    private void showApps(String id, Runnable back) {
        root.removeAllViews();
        root.addView(text(child(id).name + ": " + getString(R.string.sc_parent_apps), 30, medium, Color.WHITE));
        TextView hint = text(getString(R.string.sc_parent_apps_hint3), 15, regular, 0xB3FFFFFF);
        hint.setPadding(0, dp(4), 0, dp(12));
        root.addView(hint);
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(dp(620), dp(330)));
        TextView done = button(getString(R.string.sc_parent_done), 20);
        done.setOnClickListener(v -> back.run());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(300), dp(56));
        dlp.topMargin = dp(12);
        root.addView(done, dlp);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<AppEntry> apps = AppRepository.load(getApplicationContext());
            runOnUiThread(() -> {
                if (isFinishing()) return;
                Set<String> settings = KidsLock.settingsPackages(this);
                for (AppEntry a : apps) {
                    if (!settings.contains(a.pkg)) list.addView(appRow(a, id));
                }
                if (list.getChildCount() > 0) list.getChildAt(0).requestFocus();
            });
        });
    }

    private View appRow(AppEntry app, String id) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.sc_parent_button);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));
        row.setFocusable(true);
        row.setClickable(true);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView name = text(app.label, 18, regular, Color.WHITE);
        name.setGravity(Gravity.START);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, -2, 1);
        nlp.setMarginStart(dp(16));
        row.addView(name, nlp);
        TextView state = text("", 16, medium, Color.WHITE);
        row.addView(state);
        Runnable paint = () -> {
            KidsProfile p = child(id);
            boolean f = row.isFocused(), on = p.allowed.contains(app.pkg), fav = p.favorites.contains(app.pkg);
            name.setTextColor(f ? 0xFF14122B : Color.WHITE);
            String label = getString(on ? R.string.sc_parent_allowed : R.string.sc_parent_blocked);
            if (fav) label += " · " + getString(R.string.sc_parent_favorite);
            if (p.free.contains(app.pkg)) label += " · " + getString(R.string.sc_parent_free);
            state.setText(label);
            state.setTextColor(on ? (f ? 0xFF0E8A4F : 0xFF7CF5B0) : (f ? 0xFFB0413E : 0xFFFF8A8A));
        };
        row.setOnFocusChangeListener((v, f) -> paint.run());
        row.setOnClickListener(v -> {
            KidsProfile p = child(id);
            if (p.allowed.remove(app.pkg)) p.favorites.remove(app.pkg);
            else p.allowed.add(app.pkg);
            profiles.save(p);
            paint.run();
        });
        row.setOnKeyListener((v, code, event) -> {
            if (code != KeyEvent.KEYCODE_DPAD_RIGHT && code != KeyEvent.KEYCODE_DPAD_LEFT) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                KidsProfile p = child(id);
                if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (!p.free.remove(app.pkg)) p.free.add(app.pkg);
                } else if (!p.favorites.remove(app.pkg)) {
                    p.favorites.add(app.pkg);
                    p.allowed.add(app.pkg);
                }
                profiles.save(p);
                paint.run();
            }
            return true;
        });
        paint.run();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(60));
        lp.topMargin = dp(6);
        row.setLayoutParams(lp);
        return row;
    }

    // ---------- Bausteine ----------

    private TextView text(String s, int sp, Typeface tf, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView button(String s, int sp) {
        TextView b = text(s, sp, medium, Color.WHITE);
        b.setBackgroundResource(R.drawable.sc_parent_button);
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((v, f) -> b.setTextColor(f ? 0xFF14122B : Color.WHITE));
        b.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
