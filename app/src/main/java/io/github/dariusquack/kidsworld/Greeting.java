package io.github.dariusquack.kidsworld;

import com.gathin.tvhome.R;

/** Begrüßung nach Tageszeit. */
public final class Greeting {
    private Greeting() {}

    /** String-Ressource für die Stunde 0–23. */
    public static int forHour(int hour) {
        if (hour >= 5 && hour < 11) return R.string.sc_greeting_morning;
        if (hour >= 11 && hour < 17) return R.string.sc_greeting_day;
        if (hour >= 17 && hour < 22) return R.string.sc_greeting_evening;
        return R.string.sc_greeting_night;
    }
}
