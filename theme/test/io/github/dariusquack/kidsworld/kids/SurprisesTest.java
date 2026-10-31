package io.github.dariusquack.kidsworld.kids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

public class SurprisesTest {
    @Test
    public void nothingAtStart() {
        assertEquals(-1f, new Surprises(new Random(1)).progress(0), 0f);
    }

    @Test
    public void runsAfterAWhileThenPausesLong() {
        Surprises s = new Surprises(new Random(1));
        s.progress(0);
        long now = 0;
        while (s.progress(now) < 0 && now < 60_000) now += 100;
        assertTrue("kommt innerhalb der ersten Minute", now < 60_000);
        long end = now;
        while (s.progress(end) >= 0) end += 100;
        assertTrue(end - now <= Surprises.DURATION_MS + 100);
        // Danach mindestens 25 Sekunden Ruhe.
        assertEquals(-1f, s.progress(end + 24_000), 0f);
    }

    @Test
    public void progressStaysInRange() {
        Surprises s = new Surprises(new Random(2));
        for (long t = 0; t < 300_000; t += 50) {
            float p = s.progress(t);
            assertTrue(p == -1f || (p >= 0f && p < 1f));
        }
    }
}
