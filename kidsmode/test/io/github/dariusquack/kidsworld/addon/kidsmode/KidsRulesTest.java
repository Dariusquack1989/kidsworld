package io.github.dariusquack.kidsworld.addon.kidsmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class KidsRulesTest {
    @Test
    public void bedtimeAcrossMidnight() {
        assertTrue(KidsTime.inBedtime(20 * 60, 19 * 60, 7 * 60));
        assertTrue(KidsTime.inBedtime(6 * 60, 19 * 60, 7 * 60));
        assertFalse(KidsTime.inBedtime(12 * 60, 19 * 60, 7 * 60));
        assertFalse(KidsTime.inBedtime(12 * 60, -1, -1));
    }

    @Test
    public void blockOrder() {
        KidsProfile p = new KidsProfile("p", "A", 0);
        assertEquals(KidsTime.Block.NONE, KidsTime.block(false, 600, p, -1));
        assertEquals(KidsTime.Block.LOCKED, KidsTime.block(true, 600, p, -1));
        assertEquals(KidsTime.Block.LIMIT, KidsTime.block(false, 600, p, 0));
        p.bedFrom = 590;
        p.bedTo = 610;
        assertEquals(KidsTime.Block.BEDTIME, KidsTime.block(false, 600, p, 5));
    }
}
