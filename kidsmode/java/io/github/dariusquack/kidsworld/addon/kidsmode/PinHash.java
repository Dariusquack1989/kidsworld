package io.github.dariusquack.kidsworld.addon.kidsmode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** Die Eltern-PIN wird nie im Klartext gespeichert, nur als SHA-256 aus Salz und PIN. */
public final class PinHash {
    public static final int LENGTH = 4;

    private PinHash() {}

    public static String newSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return hex(b);
    }

    public static String hash(String salt, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest((salt + ":" + pin).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Vergleich in konstanter Zeit. */
    public static boolean matches(String salt, String pin, String expected) {
        return expected != null && MessageDigest.isEqual(
                hash(salt, pin).getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII));
    }

    /** Genau LENGTH Ziffern. */
    public static boolean valid(String pin) {
        return pin != null && pin.length() == LENGTH && pin.chars().allMatch(Character::isDigit);
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
