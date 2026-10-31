package io.github.dariusquack.kidsworld.addon.kidsmode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PinHashTest {
    @Test
    public void pinHashMatchesOnlyTheRightPin() {
        String salt = PinHash.newSalt();
        String stored = PinHash.hash(salt, "4711");
        assertTrue(PinHash.matches(salt, "4711", stored));
        assertFalse(PinHash.matches(salt, "4712", stored));
        assertFalse(PinHash.matches(salt, "4711", null));
        assertNotEquals(stored, PinHash.hash(PinHash.newSalt(), "4711"));
    }

    @Test
    public void pinMustBeFourDigits() {
        assertTrue(PinHash.valid("0000"));
        assertFalse(PinHash.valid("123"));
        assertFalse(PinHash.valid("12a4"));
        assertFalse(PinHash.valid(null));
    }
}
