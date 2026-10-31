package io.github.dariusquack.kidsworld;

import io.github.dariusquack.kidsworld.kids.KidsTheme;

/** Das Theme dieser App: „Kids World“. Jeder Theme-Ordner liefert diese Klasse; gebaut wird immer genau eins. */
public final class SelectedTheme {
    private SelectedTheme() {}

    public static HomeTheme create() {
        return new KidsTheme();
    }
}
