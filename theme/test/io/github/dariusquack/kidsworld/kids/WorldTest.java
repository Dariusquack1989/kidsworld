package io.github.dariusquack.kidsworld.kids;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WorldTest {
    @Test
    public void stepCyclesBothWays() {
        assertEquals(World.SEA, World.SPACE.step(1));
        assertEquals(World.MAGIC, World.JUNGLE.step(1));
        assertEquals(World.SPACE, World.MAGIC.step(1));
        assertEquals(World.MAGIC, World.SPACE.step(-1));
    }

    @Test
    public void unknownNameFallsBackToSpace() {
        assertEquals(World.SEA, World.named("SEA"));
        assertEquals(World.SPACE, World.named(""));
        assertEquals(World.SPACE, World.named(null));
    }
}
