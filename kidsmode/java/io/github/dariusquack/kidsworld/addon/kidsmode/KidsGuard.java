package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gathin.tvhome.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.github.dariusquack.kidsworld.HomeApps;

/**
 * Der Wächter von Kids World (Bedienungshilfe). Er schluckt die Fernbedienungstasten, die aus der Kinderwelt
 * herausführen (Einstellungen, Eingangswahl, Alle Apps). Öffnet sich trotzdem eine Activity einer App, die nicht
 * freigegeben ist, schickt er den TV zurück auf die Kinder-Startseite. Er zählt auch die Nutzungszeit und beendet
 * die laufende App, wenn Tageslimit oder Schlafenszeit erreicht sind (die Android-Plattform bietet dafür keine
 * systemweite Sperre; ohne Wächter greifen die Zeiten nicht).
 *
 * <p>Grenzen: Er sieht nur Activities, keine Inhalte innerhalb einer freigegebenen App; Werksreset, abgesicherter
 * Modus oder das Abschalten der Bedienungshilfe in den Einstellungen (die er ohne PIN nicht öffnen lässt) kann er
 * nicht verhindern. Er ist eine Hürde für Kinder, keine Garantie.
 */
public final class KidsGuard extends AccessibilityService {
    /** Tasten, die ein Kind nicht drücken soll. */
    private static final Set<Integer> BLOCKED_KEYS = new HashSet<>(Arrays.asList(
            KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_TV_INPUT, KeyEvent.KEYCODE_ALL_APPS));

    /**
     * Systemhelfer, die eine freigegebene App kurz einblenden darf (Konto-Auswahl, Berechtigungsfragen).
     * Nur Activities, die eine App selbst aufruft; sie führen nicht in andere Apps.
     */
    private static final Set<String> HELPERS = new HashSet<>(Arrays.asList(
            "android", "com.android.systemui", "com.google.android.gms",
            "com.android.permissioncontroller", "com.google.android.permissioncontroller"));

    /** Kürzerer Takt, wenn die Zeit bald um ist. */
    private static final long LAST_TICK_MS = 10_000;
    private static final int LAST_MINUTES = 5;

    /** Solange gesperrt ist, kommen nur diese Tasten durch: OK (Elternbereich) sowie Lautstärke und Ein/Aus. */
    private static final Set<Integer> FREE_KEYS = new HashSet<>(Arrays.asList(
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_TV_POWER));

    /** So oft wird die Nutzungszeit fortgeschrieben; lange Abstände sparen Rechenzeit. */
    private static final long TICK_MS = 30_000;

    private KidsLock lock;
    private KidsProfiles profiles;
    private KidsGate gate;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tick;
    /** Die freigegebene App, die gerade läuft; null auf der Kinder-Startseite. */
    private String foreground;
    private long countedAt;
    private boolean screenOn = true;
    /** Das Kind, dem die laufende App gerade Zeit kostet; bleibt bis zur Abrechnung fest. */
    private KidsProfile payer;
    private KidsDb db;
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            count();
            screenOn = Intent.ACTION_SCREEN_ON.equals(i.getAction());
            countedAt = SystemClock.elapsedRealtime();
        }
    };
    private KidsRemote remote;
    /** Die Kinder-Startseite ist vorn (nicht der Elternbereich). */
    private boolean atHome;
    private View message;
    private Set<String> settings = new HashSet<>();

    @Override
    protected void onServiceConnected() {
        lock = new KidsLock(this);
        profiles = new KidsProfiles(this);
        gate = new KidsGate(this);
        db = KidsDb.get(this);
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screen, f);
        PowerManager power = getSystemService(PowerManager.class);
        screenOn = power == null || power.isInteractive();
        handler.postDelayed(tick, TICK_MS);
        remote = new KidsRemote(this, new KidsRemote.Listener() {
            @Override
            public void onChange() {
                handler.post(() -> {
                    enforce();
                    KidsGate.notifyChanged();
                });
            }

            @Override
            public void onMessage(String text) {
                handler.post(() -> showMessage(text));
            }

            @Override
            public void onOpenSettings() {
                handler.post(() -> {
                    lock.pauseGuard();
                    HomeApps.openSettings(KidsGuard.this);
                });
            }
        });
        remote.start();
        settings = KidsLock.settingsPackages(this);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (lock == null || lock.guardPaused()) return false;
        if (BLOCKED_KEYS.contains(event.getKeyCode())) return true;
        return atHome && gate.fullBlock() && !FREE_KEYS.contains(event.getKeyCode());
    }

    /** Solange eine Nachricht steht: OK oder Zurück schließen sie, sonst kommt nur die Lautstärke durch. */
    private boolean messageKey(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_VOLUME_UP || key == KeyEvent.KEYCODE_VOLUME_DOWN
                || key == KeyEvent.KEYCODE_VOLUME_MUTE || key == KeyEvent.KEYCODE_POWER) {
            return false;
        }
        boolean close = key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_DPAD_CENTER
                || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (close && event.getAction() == KeyEvent.ACTION_UP) hideMessage();
        return true;
    }

    /** Große Nachricht über allem, auch über laufenden Apps. Eine neue ersetzt die alte. */
    private void showMessage(String text) {
        hideMessage();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(0xF0120C33);
        // Das Fenster nimmt die Tasten selbst an, solange es steht.
        box.setFocusable(true);
        box.setFocusableInTouchMode(true);
        box.setOnKeyListener((v, code, event) -> messageKey(event));
        int pad = Math.round(64 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);
        box.addView(label(getString(R.string.sc_message_title), 22, 0xFFFFD36E));
        TextView body = label(text, text.length() > 60 ? 42 : 64, Color.WHITE);
        body.setPadding(0, pad / 3, 0, pad / 3);
        box.addView(body);
        box.addView(label(getString(R.string.sc_message_hint), 20, 0xCCFFFFFF));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        getSystemService(WindowManager.class).addView(box, lp);
        message = box;
        box.requestFocus();
    }

    private TextView label(String text, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(getResources().getFont(R.font.outfit_medium));
        return t;
    }

    private void hideMessage() {
        if (message == null) return;
        getSystemService(WindowManager.class).removeView(message);
        message = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (lock == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkgCs = event.getPackageName(), clsCs = event.getClassName();
        if (pkgCs == null || clsCs == null) return;
        String pkg = pkgCs.toString();
        if (pkg.equals(getPackageName())) {
            atHome = clsCs.toString().endsWith("HomeActivity");
            setForeground(null);
            return;
        }
        atHome = false;
        if (HELPERS.contains(pkg) || isKeyboard(pkg)) return;
        if (lock.guardPaused()) return;
        // Nur echte Activities zählen; Einblendungen (Lautstärke, Toasts, Dialoge) lässt er in Ruhe.
        if (!isActivity(pkg, clsCs.toString())) return;
        boolean allowed = profiles.active().allowed.contains(pkg) && !settings.contains(pkg);
        if (allowed && gate.mayRun(pkg)) {
            setForeground(pkg);
        } else {
            setForeground(null);
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    }

    private void setForeground(String pkg) {
        count();
        foreground = pkg;
        payer = pkg != null ? profiles.active() : null;
    }

    /**
     * Rechnet die Zeit seit dem letzten Mal ab, wenn eine zählende App lief und der Bildschirm an war. Die Laufzeituhr
     * lässt sich nicht verstellen. Sie läuft aber im Standby weiter; deshalb zählt ein Takt höchstens doppelt, falls das
     * Ausschalten nicht gemeldet wurde.
     */
    private void count() {
        long now = SystemClock.elapsedRealtime();
        if (foreground != null && payer != null && countedAt > 0 && screenOn && !payer.free.contains(foreground)) {
            db.addUsage(payer.id, Math.min(now - countedAt, 2 * TICK_MS));
        }
        countedAt = now;
    }

    private void tick() {
        count();
        enforce();
        int left = foreground != null ? gate.minutesLeft(profiles.active()) : -1;
        handler.postDelayed(tick, left >= 0 && left <= LAST_MINUTES ? LAST_TICK_MS : TICK_MS);
    }

    /** Beendet die laufende App, wenn gerade nichts erlaubt ist. */
    private void enforce() {
        if (foreground != null && !lock.guardPaused() && !gate.mayRun(foreground)) {
            foreground = null;
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        if (db != null) unregisterReceiver(screen);
        if (remote != null) remote.stop();
        hideMessage();
        super.onDestroy();
    }

    private boolean isActivity(String pkg, String cls) {
        try {
            getPackageManager().getActivityInfo(new ComponentName(pkg, cls), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isKeyboard(String pkg) {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm == null) return false;
        for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
            if (pkg.equals(i.getPackageName())) return true;
        }
        return false;
    }

    @Override
    public void onInterrupt() {
    }
}
