package io.github.dariusquack.kidsworld.kids;

import java.util.Random;

/**
 * Wie sich das Maskottchen gerade bewegt, ohne Android: hereinhüpfen, kurz nicken, jubeln, selten eine kleine Einlage.
 * Es reagiert nur knapp und nicht auf jeden Tastendruck, damit es nicht vom Inhalt ablenkt.
 */
final class MascotMotion {
    static final long ENTER_MS = 600, NOD_MS = 380, CHEER_MS = 1500, WIGGLE_MS = 900;
    private static final long NOD_GAP_MS = 1500;
    private static final long IDLE_MIN_MS = 20_000, IDLE_MAX_MS = 45_000;

    private final Random random;
    private long enterAt = Long.MIN_VALUE / 2, nodAt = Long.MIN_VALUE / 2, cheerAt = Long.MIN_VALUE / 2;
    private long wiggleAt = Long.MIN_VALUE / 2, nextIdle = -1;
    private boolean calm;

    MascotMotion(Random random) {
        this.random = random;
    }

    void setCalm(boolean on) {
        calm = on;
    }

    void enter(long now) {
        enterAt = now;
    }

    /** Eine App wurde gewählt: kurzes Nicken, höchstens alle paar Sekunden. */
    void nod(long now) {
        if (calm || now - nodAt < NOD_GAP_MS) return;
        nodAt = now;
    }

    /** Besonderes Ereignis, zum Beispiel eine Belohnung. */
    void cheer(long now) {
        cheerAt = now;
    }

    private static float progress(long now, long at, long ms) {
        long d = now - at;
        return d < 0 || d >= ms ? -1f : d / (float) ms;
    }

    /** Wie weit es noch unter dem Bildrand steckt: 1 = ganz unten, 0 = steht. */
    float hidden(long now) {
        float e = progress(now, enterAt, ENTER_MS);
        return e < 0 ? 0f : (1f - e) * (1f - e);
    }

    /** Anheben als Anteil der Bildhöhe. */
    float lift(long now) {
        float c = progress(now, cheerAt, CHEER_MS);
        if (c >= 0) return 0.08f * Math.abs((float) Math.sin(3 * Math.PI * c)) * (calm ? 0.4f : 1f);
        float e = progress(now, enterAt, ENTER_MS);
        if (e >= 0) return 0.03f * (float) Math.sin(Math.PI * e);
        float n = progress(now, nodAt, NOD_MS);
        return n >= 0 ? 0.02f * (float) Math.sin(Math.PI * n) : 0f;
    }

    /** Neigung in Grad; plant auch die seltene kleine Einlage. */
    float tilt(long now) {
        if (nextIdle < 0) nextIdle = now + IDLE_MIN_MS;
        if (!calm && now >= nextIdle) {
            wiggleAt = now;
            nextIdle = now + IDLE_MIN_MS + (long) (random.nextFloat() * (IDLE_MAX_MS - IDLE_MIN_MS));
        }
        float w = progress(now, wiggleAt, WIGGLE_MS);
        return w >= 0 ? 7f * (float) Math.sin(4 * Math.PI * w) * (1f - w) : 0f;
    }

    /** Größe beim Jubeln. */
    float scale(long now) {
        float c = progress(now, cheerAt, CHEER_MS);
        return c >= 0 ? 1f + 0.08f * (float) Math.sin(Math.PI * c) : 1f;
    }
}
