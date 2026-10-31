package io.github.dariusquack.kidsworld.addon.kidsmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MediaTimeTest {
    private static KidsProfile kid() {
        KidsProfile p = new KidsProfile("p", "A", 0);
        p.timeOn = true;
        p.baseMin = 30;
        p.maxExtraMin = 30;
        p.maxTotalMin = 60;
        return p;
    }

    @Test
    public void exampleFromTheBrief() {
        KidsProfile p = kid();
        int first = MediaTime.creditable(p, 15, 0, 0);
        int second = MediaTime.creditable(p, 10, first, first);
        assertEquals(15, first);
        assertEquals(10, second);
        assertEquals(55, MediaTime.available(p, first + second));
    }

    @Test
    public void creditStopsAtExtraLimit() {
        KidsProfile p = kid();
        assertEquals(5, MediaTime.creditable(p, 15, 25, 25));
        assertEquals(0, MediaTime.creditable(p, 15, 30, 30));
    }

    @Test
    public void creditStopsAtDailyMaximum() {
        KidsProfile p = kid();
        p.maxExtraMin = 100;
        p.maxTotalMin = 40;
        assertEquals(10, MediaTime.creditable(p, 15, 0, 0));
    }

    @Test
    public void manualCorrectionNeverPassesMaximum() {
        KidsProfile p = kid();
        assertEquals(30, MediaTime.correctable(p, 100, 0));
        assertEquals(0, MediaTime.correctable(p, 10, 30));
        assertEquals(60, MediaTime.available(p, 500));
    }

    @Test
    public void correctionDownStopsAtZero() {
        KidsProfile p = kid();
        assertEquals(-10, MediaTime.correctable(p, -10, 0));
        assertEquals(-30, MediaTime.correctable(p, -100, 0));
        assertEquals(0, MediaTime.available(p, -100));
    }

    @Test
    public void noMaximumWhenZero() {
        KidsProfile p = kid();
        p.maxTotalMin = 0;
        p.maxExtraMin = 0;
        assertEquals(500, MediaTime.creditable(p, 500, 0, 0));
    }

    @Test
    public void leftRoundsUpAndOffMeansNoAccount() {
        KidsProfile p = kid();
        assertEquals(30, MediaTime.left(p, 0, 0));
        assertEquals(1, MediaTime.left(p, 0, 29 * 60_000L + 1));
        assertEquals(0, MediaTime.left(p, 0, 30 * 60_000L));
        p.timeOn = false;
        assertEquals(-1, MediaTime.left(p, 0, 0));
    }

    @Test
    public void carryOnlyUnusedExtraWhenAllowed() {
        KidsProfile p = kid();
        assertEquals(0, MediaTime.carry(p, 20, 20, 0));
        p.carryOver = true;
        assertEquals(20, MediaTime.carry(p, 20, 20, 0));
        assertEquals(5, MediaTime.carry(p, 20, 20, 45 * 60_000L));
        assertEquals(0, MediaTime.carry(p, 20, 20, 50 * 60_000L));
    }

    @Test
    public void weekdays() {
        assertEquals(3, KidsCalendar.weekday(0));
        assertEquals(0, KidsCalendar.weekday(4));
    }

    @Test
    public void activeOnFreeWeekdays() {
        // Tag 4 ist ein Montag: Eine Aufgabe nur für Dienstag ist nicht dran, gilt aber weiter.
        assertFalse(KidsCalendar.due(2, 0, 0, 4));
        assertTrue(KidsCalendar.active(0, 0, 4));
        assertFalse(KidsCalendar.active(0, 3, 4));
        assertFalse(KidsCalendar.active(5, 0, 4));
    }

    @Test
    public void dueRules() {
        assertTrue(KidsCalendar.due(0, 100, 0, 500));
        assertFalse(KidsCalendar.due(0, 100, 0, 99));
        assertFalse(KidsCalendar.due(0, 100, 150, 151));
        int mondayOnly = 1;
        assertTrue(KidsCalendar.due(mondayOnly, 0, 0, 4));
        assertFalse(KidsCalendar.due(mondayOnly, 0, 0, 5));
    }

    @Test
    public void dayNeverRunsBackwards() {
        assertEquals(100, KidsCalendar.logicalDay(100, 90));
        assertEquals(101, KidsCalendar.logicalDay(100, 101));
    }

    @Test
    public void clockJumpDetected() {
        assertEquals(0, KidsCalendar.clockJump(1000, 1000, 61_000, 61_000));
        assertEquals(0, KidsCalendar.clockJump(1000, 5000, 9000, 100));
        assertEquals(0, KidsCalendar.clockJump(0, 0, 9_000_000, 10));
        assertEquals(3_600_000L, KidsCalendar.clockJump(1000, 1000, 3_601_000 + 1000, 2000 - 1000 + 1000 - 1000 + 1000));
        assertTrue(KidsCalendar.clockJump(10_000_000, 1000, 1000, 2000) < 0);
    }

    @Test
    public void oldProfileLineBecomesAccount() {
        KidsProfile q = KidsProfile.decode("p1|Kind|0|SPACE|||60|1140|420");
        assertTrue(q.timeOn);
        assertEquals(60, q.baseMin);
        assertEquals(60, q.maxTotalMin);
        KidsProfile none = KidsProfile.decode("p1|Kind|0|SPACE|||0|-1|-1");
        assertFalse(none.timeOn);
    }

    @Test
    public void accountSettingsSurviveSaving() {
        KidsProfile p = kid();
        p.carryOver = true;
        p.free.add("com.lernen");
        KidsProfile q = KidsProfile.decode(p.encode());
        assertTrue(q.timeOn);
        assertEquals(30, q.maxExtraMin);
        assertEquals(60, q.maxTotalMin);
        assertTrue(q.carryOver);
        assertTrue(q.free.contains("com.lernen"));
    }
}
