package io.github.dariusquack.kidsworld.kids;

import com.gathin.tvhome.R;

/** Die Hintergrundwelten von Kids World. {@code image} ist der Dateiname des Hintergrunds (ohne Endung). */
public enum World {
    SPACE(R.string.sc_kids_space, "weltraum"),
    SEA(R.string.sc_kids_sea, "unterwasser"),
    JUNGLE(R.string.sc_kids_jungle, "dschungel"),
    MAGIC(R.string.sc_kids_magic, "zauberwelt");

    public final int title;
    public final String image;

    World(int title, String image) {
        this.title = title;
        this.image = image;
    }

    /** Nächste (dir = 1) oder vorige (dir = -1) Welt, im Kreis. */
    public World step(int dir) {
        World[] all = values();
        return all[Math.floorMod(ordinal() + dir, all.length)];
    }

    /** Welt zum gespeicherten Namen; unbekannt oder leer ergibt den Weltraum. */
    public static World named(String name) {
        for (World w : values()) {
            if (w.name().equals(name)) return w;
        }
        return SPACE;
    }
}
