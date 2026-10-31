package io.github.dariusquack.kidsworld.addon.kidsmode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Ein Kinderprofil: Name, Avatar, Lieblingswelt, Apps, Favoriten und die Regeln des Medienzeitkontos. Reine Daten. */
public final class KidsProfile {
    /** Avatar-Tiere; {@link #avatar} ist der Index. */
    public static final String[] AVATARS = {"fuchs", "panda", "loewe", "baer", "katze", "hase", "gecko", "alien"};

    public final String id;
    public String name;
    public int avatar;
    /** Name der Lieblingswelt, wie das Theme sie nennt; leer = keine. Das Add-on kennt keine Welten. */
    public String world = "";
    /** Zeitkonto an: Ohne bleibt die Medienzeit unbegrenzt. */
    public boolean timeOn;
    /** Grundzeit, höchste Zusatzzeit und Tagesmaximum in Minuten; 0 bei den Maxima = keine Grenze. */
    public int baseMin, maxExtraMin, maxTotalMin;
    /** Unverbrauchte Zusatzminuten gehen auf den nächsten Tag über. */
    public boolean carryOver;
    /** Schlafenszeit als Minute des Tages, -1 = keine. */
    public int bedFrom = -1, bedTo = -1;
    public final Set<String> allowed = new HashSet<>();
    public final Set<String> favorites = new HashSet<>();
    /** Freigegebene Apps, die keine Medienzeit kosten, zum Beispiel Lern-Apps. */
    public final Set<String> free = new HashSet<>();

    public KidsProfile(String id, String name, int avatar) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
    }

    public String avatarName() {
        return AVATARS[Math.floorMod(avatar, AVATARS.length)];
    }

    /** Eine Zeile zum Speichern. */
    public String encode() {
        return String.join("|", enc(id), enc(name), String.valueOf(avatar), enc(world), joined(allowed), joined(favorites),
                String.valueOf(baseMin), String.valueOf(bedFrom), String.valueOf(bedTo), timeOn ? "1" : "0",
                String.valueOf(maxExtraMin), String.valueOf(maxTotalMin), carryOver ? "1" : "0", joined(free));
    }

    /** Gegenstück zu {@link #encode}; null bei einer kaputten Zeile. */
    public static KidsProfile decode(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length != 6 && f.length != 9 && f.length != 14) return null;
        try {
            KidsProfile p = new KidsProfile(dec(f[0]), dec(f[1]), Integer.parseInt(f[2]));
            p.world = dec(f[3]);
            p.allowed.addAll(split(f[4]));
            p.favorites.addAll(split(f[5]));
            if (f.length >= 9) {
                p.baseMin = Integer.parseInt(f[6]);
                p.bedFrom = Integer.parseInt(f[7]);
                p.bedTo = Integer.parseInt(f[8]);
                // Ältere Zeilen: Das Tageslimit wird zur Grundzeit, ohne Zusatzzeit.
                p.timeOn = p.baseMin > 0;
                p.maxTotalMin = p.baseMin;
            }
            if (f.length == 14) {
                p.timeOn = "1".equals(f[9]);
                p.maxExtraMin = Integer.parseInt(f[10]);
                p.maxTotalMin = Integer.parseInt(f[11]);
                p.carryOver = "1".equals(f[12]);
                p.free.addAll(split(f[13]));
            }
            return p;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String joined(Set<String> s) {
        StringBuilder b = new StringBuilder();
        for (String x : s) b.append(b.length() > 0 ? "," : "").append(enc(x));
        return b.toString();
    }

    private static Set<String> split(String s) {
        Set<String> out = new HashSet<>();
        if (s.isEmpty()) return out;
        for (String x : Arrays.asList(s.split(","))) out.add(dec(x));
        return out;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String dec(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
