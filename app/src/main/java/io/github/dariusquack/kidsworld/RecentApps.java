package io.github.dariusquack.kidsworld;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Welche Apps über Kids World zuletzt geöffnet wurden, neueste zuerst. Nur lokal, ohne Berechtigung:
 * eine App, die anders gestartet wurde (z. B. Netflix-Taste der Fernbedienung), taucht hier nicht auf.
 */
public final class RecentApps {
    private static final String PREFS = "sc_recent";
    private static final String KEY = "order";
    static final int MAX = 8;

    private RecentApps() {}

    static void record(Context ctx, String pkg) {
        SharedPreferences p = prefs(ctx);
        List<String> order = RecentOrder.push(RecentOrder.parse(p.getString(KEY, "")), pkg, MAX);
        p.edit().putString(KEY, RecentOrder.join(order)).putLong("t:" + pkg, System.currentTimeMillis()).apply();
    }

    /** Paketnamen, neueste zuerst. */
    public static List<String> list(Context ctx) {
        return new ArrayList<>(RecentOrder.parse(prefs(ctx).getString(KEY, "")));
    }

    /** Zeitpunkt in ms seit Epoch, oder 0. */
    public static long time(Context ctx, String pkg) {
        return prefs(ctx).getLong("t:" + pkg, 0L);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
