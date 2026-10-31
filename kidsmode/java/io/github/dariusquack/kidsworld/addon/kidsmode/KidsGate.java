package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/** Was gerade erlaubt ist: Medienzeit, Ruhezeit und die Sofort-Sperre. Die Zahlen stehen in {@link KidsDb}. */
public final class KidsGate {
    private static final String PREFS = "sc_kids";
    private static final String KEY_LOCKED = "locked_now";

    /** Wird gerufen, wenn Eltern am Handy etwas ändern; die Startseite zeigt es dann sofort. */
    private static volatile Runnable listener;

    private final SharedPreferences prefs;
    private final KidsProfiles profiles;
    private final KidsDb db;

    public KidsGate(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        profiles = new KidsProfiles(ctx);
        db = KidsDb.get(ctx);
    }

    public static void setListener(Runnable r) {
        listener = r;
    }

    static void notifyChanged() {
        Runnable r = listener;
        if (r != null) r.run();
    }

    private static int minuteNow() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    /** Restminuten des Kindes, -1 ohne Zeitkonto. */
    public int minutesLeft(KidsProfile p) {
        KidsDb.Account a = db.account(p);
        return MediaTime.left(p, a.sum, a.usedMs);
    }

    public int available(KidsProfile p) {
        return MediaTime.available(p, db.account(p).sum);
    }

    public boolean lockedNow() {
        return prefs.getBoolean(KEY_LOCKED, false);
    }

    public void setLockedNow(boolean on) {
        prefs.edit().putBoolean(KEY_LOCKED, on).apply();
    }

    /** Warum das aktive Kind gerade nichts Zählendes starten darf. */
    public KidsTime.Block block() {
        KidsProfile p = profiles.active();
        return KidsTime.block(lockedNow(), minuteNow(), p, minutesLeft(p));
    }

    /** Darf diese App jetzt laufen? Apps ohne Medienzeit sind nur von Sperre und Ruhezeit betroffen. */
    public boolean mayRun(String pkg) {
        KidsTime.Block b = block();
        return b == KidsTime.Block.NONE || (b == KidsTime.Block.LIMIT && profiles.active().free.contains(pkg));
    }

    /** Sperre oder Ruhezeit: Dann ist die ganze Startseite zu, Aufgaben bleiben bei aufgebrauchter Zeit erreichbar. */
    public boolean fullBlock() {
        KidsTime.Block b = block();
        return b == KidsTime.Block.LOCKED || b == KidsTime.Block.BEDTIME;
    }
}
