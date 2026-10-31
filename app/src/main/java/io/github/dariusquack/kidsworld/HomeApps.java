package io.github.dariusquack.kidsworld;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gemeinsame Helfer der Themes: Reihenfolge der Apps, „vor X Minuten“, Einstellungen öffnen. */
public final class HomeApps {
    private HomeApps() {}

    /** Zuletzt benutzte Apps zuerst, dann alle anderen in der bisherigen (alphabetischen) Reihenfolge. */
    public static List<AppEntry> recentFirst(Context ctx, List<AppEntry> apps) {
        Map<String, AppEntry> byPkg = new LinkedHashMap<>();
        for (AppEntry a : apps) byPkg.put(a.pkg, a);
        List<AppEntry> out = new ArrayList<>();
        for (String pkg : RecentApps.list(ctx)) {
            AppEntry a = byPkg.remove(pkg);
            if (a != null) out.add(a);
        }
        out.addAll(byPkg.values());
        return out;
    }

    /** „vor 5 Minuten“ seit dem letzten Öffnen über Kids World, oder null. */
    public static String lastOpened(Context ctx, String pkg) {
        long when = RecentApps.time(ctx, pkg);
        if (when <= 0) return null;
        long now = System.currentTimeMillis();
        // Unter einer Minute sagt Android „vor 0 Minuten“; „vor 1 Minute“ liest sich besser.
        return DateUtils.getRelativeTimeSpanString(Math.min(when, now - DateUtils.MINUTE_IN_MILLIS), now,
                DateUtils.MINUTE_IN_MILLIS).toString();
    }

    public static void openSettings(Context ctx) {
        try {
            ctx.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ignored) {
            // Kein Einstellungen-Handler auf diesem Gerät: lieber nichts tun als abstürzen.
        }
    }
}
