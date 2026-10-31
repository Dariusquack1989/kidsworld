package io.github.dariusquack.kidsworld;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

/** Eine startbare TV-App: Paket, Activity, Name und (falls vorhanden) Banner oder Icon. */
public final class AppEntry {
    public final String pkg;
    public final ComponentName component;
    public final String label;
    /** 16:9-Banner der App, oder null wenn sie keins hat. */
    public final Drawable banner;
    public final Drawable icon;
    /** Wie das Icon gezeichnet werden soll (Fläche oder freistehendes Logo), siehe {@link IconArt}. */
    public final IconArt.Shape iconShape;

    AppEntry(String pkg, ComponentName component, String label, Drawable banner, Drawable icon) {
        this.pkg = pkg;
        this.component = component;
        this.label = label;
        this.banner = banner;
        this.icon = icon;
        this.iconShape = IconArt.analyze(icon);
    }
}
