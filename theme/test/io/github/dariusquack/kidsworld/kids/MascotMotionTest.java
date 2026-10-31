package io.github.dariusquack.kidsworld.kids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

public class MascotMotionTest {
    private final MascotMotion m = new MascotMotion(new Random(1));

    @Test
    public void standsStillWithoutEvents() {
        assertEquals(0f, m.lift(1000), 0f);
        assertEquals(0f, m.hidden(1000), 0f);
        assertEquals(1f, m.scale(1000), 0f);
    }

    @Test
    public void entersFromBelow() {
        m.enter(1000);
        assertTrue(m.hidden(1000) > 0.9f);
        assertEquals(0f, m.hidden(1000 + MascotMotion.ENTER_MS), 0f);
    }

    @Test
    public void nodsOnlyOnceInAWhile() {
        m.nod(1000);
        assertTrue(m.lift(1000 + MascotMotion.NOD_MS / 2) > 0f);
        m.nod(1100);
        assertEquals(0f, m.lift(1100 + MascotMotion.NOD_MS), 0f);
    }

    @Test
    public void cheerEnds() {
        m.cheer(1000);
        assertTrue(m.lift(1000 + MascotMotion.CHEER_MS / 6) > 0.05f);
        assertEquals(1f, m.scale(1000 + MascotMotion.CHEER_MS), 0f);
        assertEquals(0f, m.lift(1000 + MascotMotion.CHEER_MS), 0f);
    }

    @Test
    public void calmSkipsNodAndWiggle() {
        m.setCalm(true);
        m.nod(1000);
        assertEquals(0f, m.lift(1000 + MascotMotion.NOD_MS / 2), 0f);
        m.tilt(0);
        assertEquals(0f, m.tilt(100_000), 0f);
    }
}
