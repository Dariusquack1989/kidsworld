package io.github.dariusquack.kidsworld.addon.kidsmode;

/**
 * Rechnung des Medienzeitkontos, alles in Minuten. Reine Logik, testbar. Das Tagesmaximum gilt immer, auch für
 * Korrekturen von Hand. {@code maxTotal} 0 heißt: kein Tagesmaximum.
 */
public final class MediaTime {
    private MediaTime() {}

    /** Tagesmaximum als Zahl, mit der sich rechnen lässt. */
    private static int cap(KidsProfile p) {
        return p.maxTotalMin > 0 ? p.maxTotalMin : Integer.MAX_VALUE / 2;
    }

    /** Verfügbare Zeit heute: Grundzeit plus alle Buchungen, nie über dem Maximum, nie unter null. */
    public static int available(KidsProfile p, int ledgerSum) {
        return Math.max(0, Math.min(cap(p), p.baseMin + ledgerSum));
    }

    /** Restzeit in Minuten (aufgerundet); ohne Zeitkonto -1. */
    public static int left(KidsProfile p, int ledgerSum, long usedMs) {
        if (!p.timeOn) return -1;
        long rest = available(p, ledgerSum) * 60_000L - usedMs;
        return rest <= 0 ? 0 : (int) Math.ceil(rest / 60_000.0);
    }

    /** Wie viele Minuten eine Aufgabe gutschreiben darf: begrenzt durch Zusatzzeit und Tagesmaximum. */
    public static int creditable(KidsProfile p, int wanted, int extraToday, int ledgerSum) {
        int extraRoom = p.maxExtraMin > 0 ? p.maxExtraMin - extraToday : wanted;
        int totalRoom = cap(p) - (p.baseMin + ledgerSum);
        return Math.max(0, Math.min(wanted, Math.min(extraRoom, totalRoom)));
    }

    /** Korrektur von Hand: nach oben nur bis zum Tagesmaximum, nach unten nur bis null. */
    public static int correctable(KidsProfile p, int wanted, int ledgerSum) {
        int now = p.baseMin + ledgerSum;
        if (wanted >= 0) return Math.max(0, Math.min(wanted, cap(p) - now));
        return Math.max(wanted, -Math.max(0, now));
    }

    /** Übertrag für morgen: unverbrauchte Zusatzminuten von heute, wenn das Kind sie behalten darf. */
    public static int carry(KidsProfile p, int extraToday, int ledgerSum, long usedMs) {
        if (!p.carryOver || !p.timeOn) return 0;
        long unused = available(p, ledgerSum) - (long) Math.ceil(usedMs / 60_000.0);
        return (int) Math.max(0, Math.min(unused, extraToday));
    }
}
