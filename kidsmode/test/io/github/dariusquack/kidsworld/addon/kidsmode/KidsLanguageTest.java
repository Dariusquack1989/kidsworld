package io.github.dariusquack.kidsworld.addon.kidsmode;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KidsLanguageTest {
    @Test
    public void followsTheTvUnlessChosen() {
        assertEquals("de", KidsLanguage.shown(KidsLanguage.AUTO, "de"));
        assertEquals("en", KidsLanguage.shown(KidsLanguage.AUTO, "fr"));
        assertEquals("en", KidsLanguage.shown(KidsLanguage.ENGLISH, "de"));
        assertEquals("de", KidsLanguage.shown(KidsLanguage.GERMAN, "en"));
    }

    @Test
    public void choiceGoesRound() {
        assertEquals(KidsLanguage.GERMAN, KidsLanguage.next(KidsLanguage.AUTO));
        assertEquals(KidsLanguage.ENGLISH, KidsLanguage.next(KidsLanguage.GERMAN));
        assertEquals(KidsLanguage.AUTO, KidsLanguage.next(KidsLanguage.ENGLISH));
    }
}
