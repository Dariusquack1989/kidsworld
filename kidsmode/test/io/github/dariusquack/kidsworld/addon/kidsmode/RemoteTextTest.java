package io.github.dariusquack.kidsworld.addon.kidsmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

public class RemoteTextTest {
    @Test
    public void splitsTarget() {
        assertEquals("/api/do", RemoteText.path("/api/do?a=lock&v=1"));
        Map<String, String> q = RemoteText.query("/api/do?a=app&pkg=com.x%2Ey&x&v=");
        assertEquals("app", q.get("a"));
        assertEquals("com.x.y", q.get("pkg"));
        assertEquals("", q.get("v"));
    }

    @Test
    public void brokenEscapeIsSkipped() {
        assertFalse(RemoteText.query("/x?a=%zz").containsKey("a"));
    }

    @Test
    public void escapesJson() {
        assertEquals("\"a\\\"b\\\\c\\u000a\"", RemoteText.str("a\"b\\c\n"));
    }

    @Test
    public void limitsLength() {
        assertEquals("abc", RemoteText.limited("abcdef", 3));
        assertEquals("ab", RemoteText.limited("ab", 3));
    }

    @Test
    public void numbers() {
        assertEquals(5, RemoteText.number("5", 0));
        assertEquals(7, RemoteText.number("x", 7));
        assertEquals(7, RemoteText.number(null, 7));
    }

    @Test
    public void onlySafeArtPaths() {
        assertTrue(RemoteText.safeArt("avatare/avatar_fuchs.png"));
        assertFalse(RemoteText.safeArt("../shared_prefs/sc_kids.xml"));
        assertFalse(RemoteText.safeArt("avatare/../x.png"));
        assertFalse(RemoteText.safeArt("hintergruende/x.png"));
    }

    @Test
    public void onlyLocalRequests() {
        assertTrue(RemoteText.localRequest("192.168.1.50:8686", null));
        assertTrue(RemoteText.localRequest("192.168.1.50:8686", "http://192.168.1.50:8686"));
        assertTrue(RemoteText.localRequest("localhost:8686", null));
        assertFalse(RemoteText.localRequest("evil.example:8686", null));
        assertFalse(RemoteText.localRequest("192.168.1.50:8686", "http://evil.example"));
        assertFalse(RemoteText.localRequest(null, null));
    }
}
