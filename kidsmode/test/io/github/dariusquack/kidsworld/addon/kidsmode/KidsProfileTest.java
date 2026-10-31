package io.github.dariusquack.kidsworld.addon.kidsmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KidsProfileTest {
    @Test
    public void encodeDecodeKeepsEverything() {
        KidsProfile p = new KidsProfile("p2", "Mia | Sophie", 3);
        p.world = "MAGIC";
        p.allowed.add("com.example.a");
        p.allowed.add("com.example.b");
        p.favorites.add("com.example.a");
        KidsProfile q = KidsProfile.decode(p.encode());
        assertEquals("p2", q.id);
        assertEquals("Mia | Sophie", q.name);
        assertEquals(3, q.avatar);
        assertEquals("MAGIC", q.world);
        assertEquals(p.allowed, q.allowed);
        assertEquals(p.favorites, q.favorites);
    }

    @Test
    public void emptyListsSurvive() {
        KidsProfile q = KidsProfile.decode(new KidsProfile("p1", "Kind", 0).encode());
        assertTrue(q.allowed.isEmpty());
        assertTrue(q.favorites.isEmpty());
    }

    @Test
    public void brokenLineIsNull() {
        assertNull(KidsProfile.decode("kaputt"));
        assertNull(KidsProfile.decode("a|b|keine-zahl|d|e|f"));
    }
}
