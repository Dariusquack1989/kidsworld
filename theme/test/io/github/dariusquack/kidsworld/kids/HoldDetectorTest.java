package io.github.dariusquack.kidsworld.kids;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HoldDetectorTest {
    @Test
    public void shortPressLaunches() {
        HoldDetector d = new HoldDetector(5000);
        assertEquals(HoldDetector.Action.NONE, d.down(100, 100, 0));
        assertEquals(HoldDetector.Action.PRESS, d.up());
    }

    @Test
    public void holdingFiresOnceAndSuppressesPress() {
        HoldDetector d = new HoldDetector(5000);
        d.down(100, 100, 0);
        assertEquals(HoldDetector.Action.NONE, d.down(100, 3000, 20));
        assertEquals(HoldDetector.Action.HOLD, d.down(100, 5100, 40));
        assertEquals(HoldDetector.Action.NONE, d.down(100, 6000, 50));
        assertEquals(HoldDetector.Action.NONE, d.up());
    }

    @Test
    public void releaseWithoutDownDoesNothing() {
        assertEquals(HoldDetector.Action.NONE, new HoldDetector(5000).up());
    }
}
