package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import java.util.HashSet;
import java.util.Set;

/**
 * Was für alle Kinder gilt: PIN, Wächter-Pause und die Einstellung „ruhige Animationen“. Alles nur lokal auf dem
 * TV (SharedPreferences der App), die PIN nur als Hash. Was je Kind gilt, steht in {@link KidsProfile}.
 */
public final class KidsLock {
    /** Nach so vielen falschen Versuchen wird die PIN-Eingabe kurz gesperrt. */
    static final int MAX_FAILS = 5;
    static final long LOCKOUT_MS = 60_000;
    /** So lange darf ein Elternteil nach „Einstellungen öffnen“ ohne Wächter herumgehen. */
    static final long PAUSE_MS = 10 * 60_000;

    private static final String PREFS = "sc_kids";
    private static final String KEY_CALM = "calm_motion";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_SALT = "pin_salt";
    private static final String KEY_HASH = "pin_hash";
    private static final String KEY_FAILS = "pin_fails";
    private static final String KEY_LOCKED_UNTIL = "pin_locked_until";
    private static final String KEY_PAUSED_UNTIL = "guard_paused_until";

    public enum Result { OK, WRONG, LOCKED }

    private final Context ctx;
    private final SharedPreferences prefs;

    public KidsLock(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Ruhige Animationen: weniger Bewegung und keine Überraschungen. */
    public String language() {
        return prefs.getString(KEY_LANGUAGE, KidsLanguage.AUTO);
    }

    public void setLanguage(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    public boolean calmMotion() {
        return prefs.getBoolean(KEY_CALM, false);
    }

    public void setCalmMotion(boolean on) {
        prefs.edit().putBoolean(KEY_CALM, on).apply();
    }

    public boolean hasPin() {
        return prefs.contains(KEY_HASH);
    }

    public void setPin(String pin) {
        if (!PinHash.valid(pin)) throw new IllegalArgumentException("PIN braucht " + PinHash.LENGTH + " Ziffern");
        String salt = PinHash.newSalt();
        prefs.edit().putString(KEY_SALT, salt).putString(KEY_HASH, PinHash.hash(salt, pin))
                .putInt(KEY_FAILS, 0).remove(KEY_LOCKED_UNTIL).apply();
    }

    public Result check(String pin) {
        long now = System.currentTimeMillis();
        if (lockedFor() > 0) return Result.LOCKED;
        if (PinHash.matches(prefs.getString(KEY_SALT, ""), pin, prefs.getString(KEY_HASH, null))) {
            prefs.edit().putInt(KEY_FAILS, 0).apply();
            return Result.OK;
        }
        int fails = prefs.getInt(KEY_FAILS, 0) + 1;
        SharedPreferences.Editor e = prefs.edit().putInt(KEY_FAILS, fails);
        if (fails >= MAX_FAILS) e.putInt(KEY_FAILS, 0).putLong(KEY_LOCKED_UNTIL, now + LOCKOUT_MS);
        e.apply();
        return fails >= MAX_FAILS ? Result.LOCKED : Result.WRONG;
    }

    /** Restliche Sperrzeit der PIN-Eingabe in ms, 0 wenn frei. */
    public long lockedFor() {
        return Math.max(0, prefs.getLong(KEY_LOCKED_UNTIL, 0) - System.currentTimeMillis());
    }

    public void pauseGuard() {
        prefs.edit().putLong(KEY_PAUSED_UNTIL, System.currentTimeMillis() + PAUSE_MS).apply();
    }

    public void resumeGuard() {
        prefs.edit().remove(KEY_PAUSED_UNTIL).apply();
    }

    public boolean guardPaused() {
        return prefs.getLong(KEY_PAUSED_UNTIL, 0) > System.currentTimeMillis();
    }

    /**
     * Pakete, die die Systemeinstellungen öffnen. Sie sind für Kinder nie freigebbar: Sie tauchen weder im
     * Elternbereich noch auf der Startseite auf, und der Wächter schließt sie außerhalb einer Pause immer.
     */
    public static Set<String> settingsPackages(Context ctx) {
        Set<String> out = new HashSet<>();
        for (ResolveInfo r : ctx.getPackageManager().queryIntentActivities(new Intent(Settings.ACTION_SETTINGS),
                PackageManager.MATCH_ALL)) {
            out.add(r.activityInfo.packageName);
        }
        return out;
    }

    /** Ist der Wächter in den Bedienungshilfen eingeschaltet? Ohne ihn schützt nur die Startseite selbst. */
    public boolean guardEnabled() {
        String list = Settings.Secure.getString(ctx.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (list == null) return false;
        ComponentName me = new ComponentName(ctx, KidsGuard.class);
        for (String s : list.split(":")) {
            ComponentName c = ComponentName.unflattenFromString(s);
            if (me.equals(c)) return true;
        }
        return false;
    }
}
