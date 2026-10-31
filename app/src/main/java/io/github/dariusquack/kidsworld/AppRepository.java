package io.github.dariusquack.kidsworld;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Liest die TV-Apps (LEANBACK_LAUNCHER) vom Gerät. Langsam: nicht auf dem UI-Thread aufrufen. */
public final class AppRepository {
    private AppRepository() {}

    public static List<AppEntry> load(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
        List<AppEntry> apps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : found) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || ctx.getPackageName().equals(ai.packageName) || !seen.add(ai.packageName)) continue;
            Drawable banner = ai.loadBanner(pm);
            if (banner == null) banner = ai.applicationInfo.loadBanner(pm);
            apps.add(new AppEntry(ai.packageName, new ComponentName(ai.packageName, ai.name),
                    String.valueOf(ri.loadLabel(pm)), banner, sharpIcon(pm, ri)));
        }
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> collator.compare(a.label, b.label));
        return apps;
    }

    /**
     * Das Icon in der höchsten Auflösung, die die App mitbringt. Die Themes zeigen Icons viel größer als die
     * normale TV-Dichte vorsieht; das Standard-Icon wäre hochskaliert und unscharf.
     */
    private static Drawable sharpIcon(PackageManager pm, ResolveInfo ri) {
        int res = ri.getIconResource();
        if (res != 0) {
            try {
                Resources r = pm.getResourcesForApplication(ri.activityInfo.applicationInfo);
                Drawable d = r.getDrawableForDensity(res, DisplayMetrics.DENSITY_XXXHIGH, null);
                if (d != null) return d;
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException ignored) {
                // Fällt auf das normale Icon zurück.
            }
        }
        return ri.loadIcon(pm);
    }

    /** Startet die App; false wenn das System sie nicht öffnen kann. */
    static boolean launch(Context ctx, AppEntry app) {
        PackageManager pm = ctx.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(app.pkg);
        if (i == null) i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(app.component);
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            RecentApps.record(ctx, app.pkg);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
