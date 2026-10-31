package io.github.dariusquack.kidsworld.kids;

import java.util.Random;

/**
 * Wann in einer Welt eine kleine Überraschung vorbeikommt: zufällig und selten, immer nur eine. Reine Logik;
 * gezeichnet wird in {@link WorldLife}, unter den Kacheln und damit nie über der App-Auswahl.
 */
final class Surprises {
    static final long DURATION_MS = 6000;
    private static final long FIRST_MS = 8000;
    private static final long MIN_GAP_MS = 25_000;
    private static final long MAX_GAP_MS = 70_000;

    private final Random random;
    private long startAt = -1;
    private int variant;

    Surprises(Random random) {
        this.random = random;
    }

    /** Fortschritt 0..1 der laufenden Überraschung, sonst -1. Plant beim ersten Aufruf und nach jedem Ende. */
    float progress(long now) {
        if (startAt < 0) startAt = now + FIRST_MS + (long) (random.nextFloat() * FIRST_MS);
        if (now < startAt) return -1f;
        if (now >= startAt + DURATION_MS) {
            startAt = now + MIN_GAP_MS + (long) (random.nextFloat() * (MAX_GAP_MS - MIN_GAP_MS));
            variant = random.nextInt(2);
            return -1f;
        }
        return (now - startAt) / (float) DURATION_MS;
    }

    /** Welche von zwei Überraschungen der Welt gerade dran ist (0 oder 1). */
    int variant() {
        return variant;
    }
}
