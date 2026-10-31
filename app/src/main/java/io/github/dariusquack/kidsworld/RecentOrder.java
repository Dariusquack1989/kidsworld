package io.github.dariusquack.kidsworld;

import java.util.ArrayList;
import java.util.List;

/** Reine Listenlogik für {@link RecentApps}, ohne Android, damit sie per JVM-Test prüfbar ist. */
final class RecentOrder {
    private RecentOrder() {}

    static List<String> parse(String stored) {
        List<String> out = new ArrayList<>();
        if (stored == null || stored.isEmpty()) return out;
        for (String s : stored.split(",")) if (!s.isEmpty() && !out.contains(s)) out.add(s);
        return out;
    }

    /** Setzt pkg an den Anfang, entfernt Doppelte, kürzt auf max. */
    static List<String> push(List<String> order, String pkg, int max) {
        List<String> out = new ArrayList<>(order);
        out.remove(pkg);
        out.add(0, pkg);
        while (out.size() > max) out.remove(out.size() - 1);
        return out;
    }

    static String join(List<String> order) {
        return String.join(",", order);
    }
}
