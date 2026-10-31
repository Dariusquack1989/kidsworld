package io.github.dariusquack.kidsworld;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/**
 * Damit App-Logos in den Themes sauber aussehen: Viele Icons sind selbst schon eine farbige Fläche (Quadrat,
 * Kreis). Die füllen die Kachel oder Kugel ganz aus, statt als Kasten im Kasten zu stehen. Freistehende Logos
 * (transparent um das Motiv) werden mittig auf ruhigem Grund gezeigt. Ausschneiden (clip) macht der Aufrufer.
 */
public final class IconArt {
    private static final int PROBE = 48;

    /** Wo im Icon das Motiv liegt (Anteile 0..1) und ob es eine geschlossene Fläche ist. */
    public static final class Shape {
        public final float left, top, right, bottom;
        public final boolean filled;
        final boolean adaptive;

        Shape(float left, float top, float right, float bottom, boolean filled, boolean adaptive) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.filled = filled;
            this.adaptive = adaptive;
        }

        float width() {
            return right - left;
        }

        float height() {
            return bottom - top;
        }

        float centerX() {
            return (left + right) / 2f;
        }

        float centerY() {
            return (top + bottom) / 2f;
        }
    }

    static final Shape UNKNOWN = new Shape(0, 0, 1, 1, false, false);

    private IconArt() {}

    /** Einmal pro App beim Laden (Hintergrund-Thread). */
    public static Shape analyze(Drawable d) {
        if (d == null) return UNKNOWN;
        if (d instanceof AdaptiveIconDrawable) return new Shape(0, 0, 1, 1, true, true);
        Bitmap b = Bitmap.createBitmap(PROBE, PROBE, Bitmap.Config.ARGB_8888);
        Drawable copy = d.getConstantState() != null ? d.getConstantState().newDrawable().mutate() : d;
        copy.setBounds(0, 0, PROBE, PROBE);
        copy.draw(new Canvas(b));
        int[] px = new int[PROBE * PROBE];
        b.getPixels(px, 0, PROBE, 0, 0, PROBE, PROBE);
        b.recycle();
        return fromAlpha(px, PROBE);
    }

    /** Reine Rechnung auf den Pixeln (ARGB), testbar ohne Android. */
    static Shape fromAlpha(int[] px, int size) {
        int l = size, t = size, r = -1, bt = -1, solid = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if ((px[y * size + x] >>> 24) < 40) continue;
                l = Math.min(l, x);
                t = Math.min(t, y);
                r = Math.max(r, x);
                bt = Math.max(bt, y);
            }
        }
        if (r < 0) return UNKNOWN;
        for (int y = t; y <= bt; y++) {
            for (int x = l; x <= r; x++) {
                if ((px[y * size + x] >>> 24) >= 200) solid++;
            }
        }
        int w = r - l + 1, h = bt - t + 1;
        float coverage = solid / (float) (w * h);
        // Eine Fläche: deckt ihren Rahmen fast ganz (runde Ecken kosten ~10 %) und ist nicht winzig.
        boolean filled = coverage >= 0.82f && w >= size * 0.55f && h >= size * 0.55f
                && Math.abs(w - h) <= size * 0.12f;
        return new Shape(l / (float) size, t / (float) size, (r + 1) / (float) size, (bt + 1) / (float) size,
                filled, false);
    }

    /** Zeichnet ein Flächen-Icon so, dass sein Motiv {@code target} ganz ausfüllt. */
    public static void fill(Canvas c, Drawable d, Shape s, RectF target) {
        if (s.adaptive) {
            // Adaptive Icons: Ebenen sind 108 dp groß, sichtbar sind die inneren 72 dp; die Maske des Systems
            // lassen wir weg, die Form gibt das Theme vor.
            AdaptiveIconDrawable a = (AdaptiveIconDrawable) d;
            float grow = target.width() * 18f / 72f;
            Rect r = new Rect(Math.round(target.left - grow), Math.round(target.top - grow),
                    Math.round(target.right + grow), Math.round(target.bottom + grow));
            for (Drawable layer : new Drawable[]{a.getBackground(), a.getForeground()}) {
                if (layer == null) continue;
                layer.setBounds(r);
                layer.draw(c);
            }
            return;
        }
        // Etwas überziehen, damit die abgerundeten Ecken des Icons im Ausschnitt verschwinden.
        float sx = target.width() / s.width() * 1.08f, sy = target.height() / s.height() * 1.08f;
        float cx = target.centerX() - s.centerX() * sx, cy = target.centerY() - s.centerY() * sy;
        d.setBounds(Math.round(cx), Math.round(cy), Math.round(cx + sx), Math.round(cy + sy));
        d.draw(c);
    }

    /** Zeichnet ein freistehendes Logo so, dass sein Motiv höchstens {@code size} groß mittig bei (x, y) steht. */
    public static void center(Canvas c, Drawable d, Shape s, float x, float y, float size) {
        if (s.adaptive) {
            fill(c, d, s, new RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2));
            return;
        }
        float k = size / Math.max(s.width(), s.height());
        float left = x - s.centerX() * k, top = y - s.centerY() * k;
        d.setBounds(Math.round(left), Math.round(top), Math.round(left + k), Math.round(top + k));
        d.draw(c);
    }
}
