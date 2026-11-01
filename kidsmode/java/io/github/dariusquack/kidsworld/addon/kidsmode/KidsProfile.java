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

    /** Ältere Profile ohne Grenze für Zusatzzeit bekommen diesen Wert: so viel, dass er nie greift. */
    static final int NO_EXTRA_LIMIT = 1440;
    static final int WHOLE_DAY = 1440;
    public static final int WHOLE_DAY_END = WHOLE_DAY;

    /** Regeln eines Wochentags. Zeiten sind Minuten des Tages, {@code to} 1440 heißt bis Mitternacht. */
    public static final class Day {
        public boolean on = true;
        public int base, from, to = WHOLE_DAY, extra;

        void set(Day o) {
            on = o.on;
            base = o.base;
            from = o.from;
            to = o.to;
            extra = o.extra;
        }
    }

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
    /** Alter in Jahren für den Vorschlag der Altersgruppe; 0 = nicht angegeben. */
    public int age;
    /** Zeitfenster dieses Tages; nur in der Tagesansicht aus {@link #onDay} gesetzt. */
    public boolean tvOn = true;
    public int tvFrom, tvTo = WHOLE_DAY;
    private Day[] week;
    public final Set<String> allowed = new HashSet<>();
    public final Set<String> favorites = new HashSet<>();
    /** Freigegebene Apps, die keine Medienzeit kosten, zum Beispiel Lern-Apps. */
    public final Set<String> free = new HashSet<>();

    public KidsProfile(String id, String name, int avatar) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
    }

    /** Die sieben Tage, Montag zuerst. Ältere Profile bekommen sie aus ihren bisherigen Regeln. */
    public Day[] week() {
        if (week == null) {
            week = new Day[7];
            int extra = maxTotalMin > 0 ? Math.max(0, maxTotalMin - baseMin) : NO_EXTRA_LIMIT;
            if (maxExtraMin > 0) extra = Math.min(extra, maxExtraMin);
            boolean window = bedFrom > bedTo && bedTo >= 0;
            for (int i = 0; i < 7; i++) {
                Day d = new Day();
                d.base = baseMin;
                d.extra = extra;
                if (window) {
                    d.from = bedTo;
                    d.to = bedFrom;
                }
                week[i] = d;
            }
        }
        return week;
    }

    /** Verwirft die Wochentage, sie entstehen neu aus den allgemeinen Regeln. */
    public void resetWeek() {
        week = null;
    }

    /** Überträgt einen Tag auf andere; {@code targets} ist eine Bitmaske, Bit 0 = Montag. Werte, keine Verknüpfung. */
    public void copyDay(int from, int targets) {
        Day[] w = week();
        for (int i = 0; i < 7; i++) {
            if ((targets & (1 << i)) != 0 && i != from) w[i].set(w[from]);
        }
    }

    /** Übernimmt die Wochenzeiten eines anderen Kindes als eigene Werte. */
    public void copyWeek(KidsProfile other) {
        Day[] mine = week(), theirs = other.week();
        for (int i = 0; i < 7; i++) mine[i].set(theirs[i]);
    }

    /**
     * Die Regeln an einem Wochentag (0 = Montag) als eigene Ansicht, mit der {@link MediaTime} und {@link KidsTime}
     * rechnen. Das Maximum ist Grundzeit plus Zusatzzeit dieses Tages.
     */
    public KidsProfile onDay(int weekday) {
        Day d = week()[Math.floorMod(weekday, 7)];
        KidsProfile s = new KidsProfile(id, name, avatar);
        s.world = world;
        s.timeOn = timeOn;
        s.carryOver = carryOver;
        s.baseMin = d.base;
        s.maxExtraMin = d.extra;
        s.maxTotalMin = d.base + d.extra;
        s.tvOn = d.on;
        s.tvFrom = d.from;
        s.tvTo = d.to;
        s.allowed.addAll(allowed);
        s.favorites.addAll(favorites);
        s.free.addAll(free);
        return s;
    }

    private String weekText() {
        StringBuilder b = new StringBuilder();
        for (Day d : week()) {
            b.append(b.length() > 0 ? ";" : "").append(d.on ? 1 : 0).append(',').append(d.base).append(',')
                    .append(d.from).append(',').append(d.to).append(',').append(d.extra);
        }
        return b.toString();
    }

    private static Day[] parseWeek(String text) {
        String[] days = text.split(";");
        if (days.length != 7) throw new IllegalArgumentException(text);
        Day[] out = new Day[7];
        for (int i = 0; i < 7; i++) {
            String[] v = days[i].split(",");
            Day d = new Day();
            d.on = "1".equals(v[0]);
            d.base = Integer.parseInt(v[1]);
            d.from = Integer.parseInt(v[2]);
            d.to = Integer.parseInt(v[3]);
            d.extra = Integer.parseInt(v[4]);
            out[i] = d;
        }
        return out;
    }

    public String avatarName() {
        return AVATARS[Math.floorMod(avatar, AVATARS.length)];
    }

    /** Eine Zeile zum Speichern. */
    public String encode() {
        return String.join("|", enc(id), enc(name), String.valueOf(avatar), enc(world), joined(allowed), joined(favorites),
                String.valueOf(baseMin), String.valueOf(bedFrom), String.valueOf(bedTo), timeOn ? "1" : "0",
                String.valueOf(maxExtraMin), String.valueOf(maxTotalMin), carryOver ? "1" : "0", joined(free),
                String.valueOf(age), weekText());
    }

    /** Gegenstück zu {@link #encode}; null bei einer kaputten Zeile. */
    public static KidsProfile decode(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length != 6 && f.length != 9 && f.length != 14 && f.length != 16) return null;
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
            if (f.length >= 14) {
                p.timeOn = "1".equals(f[9]);
                p.maxExtraMin = Integer.parseInt(f[10]);
                p.maxTotalMin = Integer.parseInt(f[11]);
                p.carryOver = "1".equals(f[12]);
                p.free.addAll(split(f[13]));
            }
            if (f.length == 16) {
                p.age = Integer.parseInt(f[14]);
                p.week = parseWeek(f[15]);
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
