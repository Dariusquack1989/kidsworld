package io.github.dariusquack.kidsworld.addon.kidsmode;

/** Wann Mediennutzung erlaubt ist. Reine Logik, testbar. */
public final class KidsTime {
    /**
     * LIMIT sperrt nur zählende Apps. WINDOW (außerhalb des Zeitfensters oder fernsehfreier Tag) sperrt alle Apps, die
     * Aufgaben bleiben erreichbar. BEDTIME und LOCKED sperren die ganze Startseite.
     */
    public enum Block { NONE, LIMIT, WINDOW, BEDTIME, LOCKED }

    private KidsTime() {}

    /** Liegt die Uhrzeit (Minute des Tages) in der Ruhezeit? Sie darf über Mitternacht gehen; from &lt; 0 = aus. */
    public static boolean inBedtime(int minute, int from, int to) {
        if (from < 0 || to < 0 || from == to) return false;
        return from < to ? minute >= from && minute < to : minute >= from || minute < to;
    }

    /** Gesperrt geht vor Ruhezeit, Ruhezeit vor aufgebrauchter Zeit. {@code left}: Restminuten, -1 = kein Konto. */
    public static Block block(boolean locked, int minute, KidsProfile p, int left) {
        if (locked) return Block.LOCKED;
        if (inBedtime(minute, p.bedFrom, p.bedTo)) return Block.BEDTIME;
        if (!p.tvOn || minute < p.tvFrom || minute >= p.tvTo) return Block.WINDOW;
        return left == 0 ? Block.LIMIT : Block.NONE;
    }
}
