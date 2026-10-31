package io.github.dariusquack.kidsworld.addon.kidsmode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/** Textarbeit der Handy-Steuerung: Adressen zerlegen, JSON-Text schützen. Reine Logik, testbar. */
final class RemoteText {
    private RemoteText() {}

    static String path(String target) {
        int q = target.indexOf('?');
        return q < 0 ? target : target.substring(0, q);
    }

    static Map<String, String> query(String target) {
        Map<String, String> out = new HashMap<>();
        int q = target.indexOf('?');
        if (q < 0) return out;
        for (String part : target.substring(q + 1).split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            try {
                out.put(URLDecoder.decode(part.substring(0, eq), "UTF-8"), URLDecoder.decode(part.substring(eq + 1), "UTF-8"));
            } catch (UnsupportedEncodingException | IllegalArgumentException ignored) {
                // Kaputter Teil: auslassen.
            }
        }
        return out;
    }

    /** Text als JSON-Zeichenkette mit Anführungszeichen. */
    static String str(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }

    /**
     * Kommt die Anfrage über die Adresse des TVs im Heimnetz? Ein Hostname deutet auf DNS-Rebinding hin, ein fremdes
     * Origin auf eine andere Webseite.
     */
    static boolean localRequest(String host, String origin) {
        if (host == null || !host.matches("((\\d{1,3}\\.){3}\\d{1,3}|localhost)(:\\d+)?")) return false;
        return origin == null || origin.equals("http://" + host);
    }

    static String limited(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    static int number(String s, int fallback) {
        try {
            return s == null ? fallback : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Nur Bilder aus diesen Ordnern dürfen ausgeliefert werden, keine Pfade nach außen. */
    static boolean safeArt(String file) {
        return file.matches("avatare/[a-z_]+\\.png");
    }
}
