package io.github.dariusquack.kidsworld;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;

/** Leuchtfarbe pro App: die kräftigste Farbe im App-Icon, aufgehellt; graue Icons bekommen die Ersatzfarbe. */
public final class IconColors {
    private final Map<String, Integer> cache = new HashMap<>();
    private final int fallback;

    public IconColors(int fallback) {
        this.fallback = fallback;
    }

    public int of(AppEntry a) {
        Integer cached = cache.get(a.pkg);
        if (cached != null) return cached;
        int color = a.icon != null ? strongest(a.icon, fallback) : fallback;
        cache.put(a.pkg, color);
        return color;
    }

    private static int strongest(Drawable icon, int fallback) {
        int color = fallback;
        Bitmap b = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888);
        // Eigene Kopie zeichnen, damit Bounds und Alpha des angezeigten Icons unberührt bleiben.
        Drawable d = icon.getConstantState() != null ? icon.getConstantState().newDrawable() : icon;
        d.setBounds(0, 0, 12, 12);
        d.draw(new Canvas(b));
        float[] hsv = new float[3];
        float best = 0.25f;
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) {
                int px = b.getPixel(x, y);
                if (Color.alpha(px) < 128) continue;
                Color.colorToHSV(px, hsv);
                float score = hsv[1] * hsv[2];
                if (score > best) {
                    best = score;
                    color = Color.HSVToColor(new float[]{hsv[0], Math.max(0.55f, hsv[1]), 1f});
                }
            }
        }
        b.recycle();
        return color;
    }
}
