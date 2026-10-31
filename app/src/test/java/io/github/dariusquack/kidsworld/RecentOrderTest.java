package io.github.dariusquack.kidsworld;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class RecentOrderTest {
    @Test
    public void parseSkipsEmptyAndDuplicates() {
        assertEquals(Arrays.asList("a", "b"), RecentOrder.parse("a,,b,a"));
        assertEquals(Collections.emptyList(), RecentOrder.parse(""));
        assertEquals(Collections.emptyList(), RecentOrder.parse(null));
    }

    @Test
    public void pushMovesToFrontAndTrims() {
        assertEquals(Arrays.asList("c", "a", "b"), RecentOrder.push(Arrays.asList("a", "b", "c"), "c", 5));
        assertEquals(Arrays.asList("d", "a"), RecentOrder.push(Arrays.asList("a", "b", "c"), "d", 2));
    }

    @Test
    public void joinRoundTrips() {
        assertEquals("x,y", RecentOrder.join(RecentOrder.parse("x,y")));
    }
}
