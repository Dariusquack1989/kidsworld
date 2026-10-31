package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

/**
 * Sprache von Kids World: wie der TV oder fest gewählt. Android 11 kennt keine Sprache je App, deshalb stellt die App
 * ihre Texte selbst um. Der Wert kommt aus dem Elternbereich und gilt auch für die Handy-Seite.
 */
public final class KidsLanguage {
    public static final String AUTO = "", GERMAN = "de", ENGLISH = "en";

    private KidsLanguage() {}

    /** Nach dem Wählen wieder von vorn: wie TV, Deutsch, Englisch. */
    public static String next(String current) {
        return AUTO.equals(current) ? GERMAN : GERMAN.equals(current) ? ENGLISH : AUTO;
    }

    /** Die tatsächlich gezeigte Sprache; alles außer Deutsch wird Englisch. */
    public static String shown(String chosen, String system) {
        if (!AUTO.equals(chosen)) return chosen;
        return GERMAN.equals(system) ? GERMAN : ENGLISH;
    }

    public static String shown(Context ctx) {
        return shown(new KidsLock(ctx).language(), Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage());
    }

    /** Stellt die Texte dieses Kontexts und der App um. */
    public static void apply(Context ctx) {
        Locale locale = new Locale(shown(ctx));
        update(ctx.getResources(), locale);
        update(ctx.getApplicationContext().getResources(), locale);
    }

    @SuppressWarnings("deprecation")
    private static void update(Resources r, Locale locale) {
        Configuration c = new Configuration(r.getConfiguration());
        c.setLocale(locale);
        r.updateConfiguration(c, r.getDisplayMetrics());
    }
}
