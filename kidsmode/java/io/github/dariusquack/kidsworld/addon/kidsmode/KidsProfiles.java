package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Die Kinderprofile, lokal auf dem TV. Es gibt immer mindestens eins; ältere Installationen übernehmen ihre
 * Freigaben und ihre Welt in ein erstes Profil.
 */
public final class KidsProfiles {
    private static final String PREFS = "sc_kids";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_ACTIVE = "active_profile";
    private static final String KEY_NEXT_ID = "next_profile_id";
    private static final String LEGACY_ALLOWED = "allowed";
    private static final String LEGACY_WORLD = "world";
    private static final String DEFAULT_NAME = "Kind";

    /** Wurde seit dem Start des TVs schon gewählt, wer spielt? */
    private static boolean picked;

    private final SharedPreferences prefs;

    public KidsProfiles(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<KidsProfile> list() {
        List<KidsProfile> out = new ArrayList<>();
        for (String line : prefs.getString(KEY_PROFILES, "").split("\n")) {
            KidsProfile p = line.isEmpty() ? null : KidsProfile.decode(line);
            if (p != null) out.add(p);
        }
        if (out.isEmpty()) out.add(firstProfile());
        return out;
    }

    private KidsProfile firstProfile() {
        KidsProfile p = new KidsProfile("p1", DEFAULT_NAME, 0);
        Set<String> legacy = prefs.getStringSet(LEGACY_ALLOWED, null);
        if (legacy != null) p.allowed.addAll(new HashSet<>(legacy));
        p.world = prefs.getString(LEGACY_WORLD, "");
        write(java.util.Collections.singletonList(p));
        return p;
    }

    /** Profil zur Kennung, oder null. */
    public KidsProfile byId(String id) {
        for (KidsProfile p : list()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    public KidsProfile active() {
        List<KidsProfile> all = list();
        String id = prefs.getString(KEY_ACTIVE, "");
        for (KidsProfile p : all) {
            if (p.id.equals(id)) return p;
        }
        return all.get(0);
    }

    public void setActive(KidsProfile p) {
        prefs.edit().putString(KEY_ACTIVE, p.id).apply();
        picked = true;
    }

    /** Muss die Startseite fragen, wer spielt? Nur bei mehreren Profilen, einmal seit dem TV-Start. */
    public boolean needsPick() {
        return !picked && list().size() > 1;
    }

    public KidsProfile add(String name) {
        int id = prefs.getInt(KEY_NEXT_ID, 2);
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply();
        List<KidsProfile> all = list();
        KidsProfile p = new KidsProfile("p" + id, name, all.size() % KidsProfile.AVATARS.length);
        all.add(p);
        write(all);
        return p;
    }

    /** Geänderte Felder eines Profils speichern. */
    public void save(KidsProfile changed) {
        List<KidsProfile> all = list();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(changed.id)) all.set(i, changed);
        }
        write(all);
    }

    /** Löscht ein Profil; das letzte bleibt immer erhalten. */
    public void remove(KidsProfile p) {
        List<KidsProfile> all = list();
        if (all.size() <= 1) return;
        all.removeIf(x -> x.id.equals(p.id));
        write(all);
    }

    private void write(List<KidsProfile> all) {
        StringBuilder b = new StringBuilder();
        for (KidsProfile p : all) b.append(p.encode()).append('\n');
        prefs.edit().putString(KEY_PROFILES, b.toString()).apply();
    }
}
