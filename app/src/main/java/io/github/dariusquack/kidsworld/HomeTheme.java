package io.github.dariusquack.kidsworld;

import android.view.View;

import java.util.List;

/**
 * Ein Kids World-Theme: baut seine eigene Oberfläche. Daten (Apps, später Add-ons) kommen von
 * {@link HomeActivity}; das Theme entscheidet nur, wie sie aussehen.
 */
public interface HomeTheme {
    View create(HomeActivity host);

    /** Neue App-Liste (Start, Installation, Deinstallation). */
    void showApps(List<AppEntry> apps);

    /** Home ist sichtbar: Animationen starten, „Zuletzt geöffnet“ auffrischen. */
    void onResume();

    void onPause();

    /** Home-Taste auf dem Startbildschirm: zurück zur Ausgangsposition. */
    void onHomePressed();

    /** Zurück-Taste auf dem Startbildschirm; ohne eigene Bedeutung wie Home. */
    default void onBackPressed() {
        onHomePressed();
    }
}
