package io.github.dariusquack.kidsworld.addon.kidsmode;

/** Tage und Wiederholungen, ohne Android. Ein Tag ist die Zahl der Tage seit dem 1. Januar 1970. Testbar. */
public final class KidsCalendar {
    public static final long DAY_MS = 86_400_000L;
    /** Ab dieser Abweichung zwischen Uhr und Laufzeit gilt die Uhr als verstellt. */
    private static final long JUMP_MS = 5 * 60_000L;

    private KidsCalendar() {}

    /** 0 = Montag bis 6 = Sonntag; der 1. Januar 1970 war ein Donnerstag. */
    public static int weekday(int day) {
        return Math.floorMod(day + 3, 7);
    }

    /** Der Tag läuft nie rückwärts: Eine zurückgestellte Uhr bringt keinen frischen oder alten Tag. */
    public static int logicalDay(int highest, int wallDay) {
        return Math.max(highest, wallDay);
    }

    /**
     * Wurde die Uhr seit der letzten Messung verstellt? Ergibt die Abweichung in ms, sonst 0. Nach einem Neustart
     * (Laufzeit kleiner als zuvor) lässt sich das nicht vergleichen.
     */
    public static long clockJump(long lastWall, long lastElapsed, long wall, long elapsed) {
        if (lastWall <= 0 || elapsed < lastElapsed) return 0;
        long diff = (wall - lastWall) - (elapsed - lastElapsed);
        return Math.abs(diff) >= JUMP_MS ? diff : 0;
    }

    /**
     * Ist eine Aufgabe an diesem Tag fällig?
     *
     * @param days  Bitmaske der Wochentage (Bit 0 = Montag); 0 = einmalig
     * @param until letzter gültiger Tag, 0 = unbegrenzt
     */
    public static boolean due(int days, int createdDay, int until, int day) {
        return active(createdDay, until, day) && (days == 0 || (days & (1 << weekday(day))) != 0);
    }

    /** Gilt eine Aufgabe an diesem Tag noch? Sie steht dann in der Liste, auch wenn ihr Wochentag nicht dran ist. */
    public static boolean active(int createdDay, int until, int day) {
        return day >= createdDay && (until <= 0 || day <= until);
    }
}
