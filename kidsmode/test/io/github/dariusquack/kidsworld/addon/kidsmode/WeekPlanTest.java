package io.github.dariusquack.kidsworld.addon.kidsmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.TimeZone;

/** Aufgabenkatalog, Wiederholung und Wochenzeiten. Tag 4 ist ein Montag, Tag 9 ein Samstag, Tag 10 ein Sonntag. */
public class WeekPlanTest {
    private static final int MON = 4, SAT = 9, SUN = 10;

    private static KidsProfile child() {
        KidsProfile p = new KidsProfile("p", "A", 0);
        p.timeOn = true;
        for (int i = 0; i < 7; i++) {
            p.week()[i].base = 30;
            p.week()[i].extra = 30;
            p.week()[i].from = 16 * 60;
            p.week()[i].to = 19 * 60;
        }
        return p;
    }

    @Test
    public void catalogHasAllTemplates() {
        assertEquals(40, TaskCatalog.all().size());
        int[] count = new int[4];
        for (TaskCatalog.Entry e : TaskCatalog.all()) count[e.category]++;
        assertEquals(10, count[TaskCatalog.SCHOOL]);
        assertEquals(10, count[TaskCatalog.ROOM]);
        assertEquals(12, count[TaskCatalog.HOME]);
        assertEquals(8, count[TaskCatalog.SPECIAL]);
    }

    @Test
    public void ageGroupIsASuggestion() {
        assertEquals(TaskCatalog.AGE_YOUNG, TaskCatalog.ageGroup(7));
        assertEquals(TaskCatalog.AGE_MIDDLE, TaskCatalog.ageGroup(10));
        assertEquals(TaskCatalog.AGE_OLD, TaskCatalog.ageGroup(13));
        assertEquals(0, TaskCatalog.ageGroup(0));
    }

    @Test
    public void weekendRule() {
        assertTrue(KidsCalendar.available(KidsCalendar.DAILY, 0, true, 0, 0, SAT));
        assertTrue(KidsCalendar.available(KidsCalendar.DAILY, 0, true, 0, 0, SUN));
        assertFalse(KidsCalendar.available(KidsCalendar.DAILY, 0, false, 0, 0, SAT));
        assertFalse(KidsCalendar.available(KidsCalendar.DAILY, 0, false, 0, 0, SUN));
        assertTrue(KidsCalendar.available(KidsCalendar.DAILY, 0, false, 0, 0, MON));
    }

    @Test
    public void chosenWeekdaysOnly() {
        int monday = 1;
        assertTrue(KidsCalendar.available(KidsCalendar.WEEKDAYS, monday, true, 0, 0, MON));
        assertFalse(KidsCalendar.available(KidsCalendar.WEEKDAYS, monday, true, 0, 0, MON + 1));
    }

    @Test
    public void onePeriodOneCredit() {
        assertEquals(0, KidsCalendar.period(KidsCalendar.ONCE, SAT));
        assertEquals(SAT, KidsCalendar.period(KidsCalendar.DAILY, SAT));
        assertEquals(SAT, KidsCalendar.period(KidsCalendar.ON_DEMAND, SAT));
        assertEquals(KidsCalendar.period(KidsCalendar.WEEKLY, MON), KidsCalendar.period(KidsCalendar.WEEKLY, SUN));
        assertEquals(KidsCalendar.period(KidsCalendar.WEEKLY, MON) + 7, KidsCalendar.period(KidsCalendar.WEEKLY, SUN + 1));
    }

    @Test
    public void everyChildHasItsOwnWeek() {
        KidsProfile a = child(), b = child();
        a.week()[5].base = 90;
        assertEquals(30, b.week()[5].base);
    }

    @Test
    public void mondayAndSaturdayDiffer() {
        KidsProfile p = child();
        p.week()[5].base = 90;
        assertEquals(30, MediaTime.available(p.onDay(0), 0));
        assertEquals(90, MediaTime.available(p.onDay(5), 0));
    }

    @Test
    public void timeWindow() {
        KidsProfile rules = child().onDay(0);
        assertEquals(KidsTime.Block.WINDOW, KidsTime.block(false, 15 * 60 + 59, rules, 30));
        assertEquals(KidsTime.Block.NONE, KidsTime.block(false, 16 * 60, rules, 30));
        assertEquals(KidsTime.Block.NONE, KidsTime.block(false, 19 * 60 - 1, rules, 30));
        // Die Endzeit greift, auch wenn noch Minuten übrig sind; der Wächter prüft das auch bei laufenden Apps.
        assertEquals(KidsTime.Block.WINDOW, KidsTime.block(false, 19 * 60, rules, 30));
    }

    @Test
    public void dayCanBeScreenFree() {
        KidsProfile p = child();
        p.week()[2].on = false;
        assertEquals(KidsTime.Block.WINDOW, KidsTime.block(false, 17 * 60, p.onDay(2), 30));
    }

    @Test
    public void baseAndExtraAdd() {
        KidsProfile p = child();
        p.week()[5].base = 90;
        KidsProfile sat = p.onDay(5);
        assertEquals(120, sat.maxTotalMin);
        assertEquals(15, MediaTime.creditable(sat, 15, 0, 0));
        assertEquals(105, MediaTime.available(sat, 15));
    }

    @Test
    public void manyTasksCannotPassTheLimit() {
        KidsProfile sat = child().onDay(5);
        int extra = 0, sum = 0;
        for (int minutes : new int[]{15, 10, 5, 5}) {
            int real = MediaTime.creditable(sat, minutes, extra, sum);
            extra += real;
            sum += real;
        }
        assertEquals(30, extra);
        assertEquals(0, MediaTime.correctable(sat, 10, sum));
    }

    @Test
    public void noExtraMeansNoExtra() {
        KidsProfile p = child();
        p.week()[0].extra = 0;
        assertEquals(0, MediaTime.creditable(p.onDay(0), 15, 0, 0));
    }

    @Test
    public void weekSurvivesSaving() {
        KidsProfile p = child();
        p.age = 9;
        p.week()[6].on = false;
        p.week()[6].base = 77;
        KidsProfile back = KidsProfile.decode(p.encode());
        assertNotNull(back);
        assertEquals(9, back.age);
        assertFalse(back.week()[6].on);
        assertEquals(77, back.week()[6].base);
        assertEquals(16 * 60, back.week()[0].from);
    }

    @Test
    public void oldProfilesKeepTheirRules() {
        KidsProfile old = new KidsProfile("p", "A", 0);
        old.timeOn = true;
        old.baseMin = 45;
        old.maxTotalMin = 60;
        old.bedFrom = 19 * 60;
        old.bedTo = 7 * 60;
        for (KidsProfile.Day d : old.week()) {
            assertEquals(45, d.base);
            assertEquals(15, d.extra);
            assertEquals(7 * 60, d.from);
            assertEquals(19 * 60, d.to);
        }
    }

    @Test
    public void dayCopyIsNoLink() {
        KidsProfile p = child();
        p.week()[5].base = 90;
        p.copyDay(5, 1 << 6);
        assertEquals(90, p.week()[6].base);
        p.week()[5].base = 10;
        assertEquals(90, p.week()[6].base);
    }

    @Test
    public void childCopyIsNoLink() {
        KidsProfile a = child(), b = child();
        a.week()[1].base = 50;
        b.copyWeek(a);
        assertEquals(50, b.week()[1].base);
        a.week()[1].base = 20;
        assertEquals(50, b.week()[1].base);
    }

    @Test
    public void clockBackGivesNoNewDay() {
        assertEquals(SAT, KidsCalendar.logicalDay(SAT, SAT - 2));
    }

    @Test
    public void dayChangeAcrossSummerTime() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");
        // 29. März 2026: Umstellung um 2 Uhr. 23:30 und 00:30 Ortszeit liegen auf verschiedenen Tagen.
        long before = 1_774_737_000_000L, after = before + 3_600_000L;
        assertEquals(KidsCalendar.localDay(before, berlin) + 1, KidsCalendar.localDay(after, berlin));
        // Der Sonntag der Umstellung hat 23 Stunden: 00:30 plus 23 Stunden ist schon der nächste Tag.
        assertEquals(KidsCalendar.localDay(after, berlin) + 1, KidsCalendar.localDay(after + 23 * 3_600_000L, berlin));
    }

    @Test
    public void dayChangeAcrossWinterTime() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");
        // 25. Oktober 2026: Der Tag hat 25 Stunden. 00:30 Ortszeit plus 24 Stunden ist noch derselbe Tag.
        long start = 1_792_881_000_000L;
        assertEquals(KidsCalendar.localDay(start, berlin), KidsCalendar.localDay(start + 24 * 3_600_000L, berlin));
        assertEquals(KidsCalendar.localDay(start, berlin) + 1, KidsCalendar.localDay(start + 25 * 3_600_000L, berlin));
    }
}
