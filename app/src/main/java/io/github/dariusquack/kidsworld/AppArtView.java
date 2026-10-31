package io.github.dariusquack.kidsworld;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Das Bild einer App in abgerundeter Form, für alle Themes gleich sauber: das Banner ganz sichtbar auf seiner eigenen
 * Randfarbe (Logos werden nie abgeschnitten), sonst das Icon (Flächen-Icons füllen alles, freistehende Logos stehen
 * mittig auf der Farbe der App).
 */
public final class AppArtView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private final float radius;
    private AppEntry app;
    private int color = 0xFF2A2F3A;
    private int edgeTop, edgeBottom;

    public AppArtView(Context ctx, float radiusPx) {
        super(ctx);
        radius = radiusPx;
    }

    public void show(AppEntry a, int appColor) {
        app = a;
        color = appColor;
        if (a != null && a.banner != null) readEdges(a.banner);
        setContentDescription(a != null ? a.label : null);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        rect.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        if (app != null && app.banner != null) {
            paint.setColor(edgeTop);
            c.drawRect(0, 0, w, h / 2f, paint);
            paint.setColor(edgeBottom);
            c.drawRect(0, h / 2f, w, h, paint);
        } else {
            paint.setColor(darker(color));
            c.drawRect(rect, paint);
        }
        if (app != null) {
            if (app.banner != null) {
                drawContained(c, app.banner, w, h);
            } else if (app.icon != null && app.iconShape.filled) {
                float s = Math.min(w, h) * 0.8f;
                rect.set((w - s) / 2f, (h - s) / 2f, (w + s) / 2f, (h + s) / 2f);
                IconArt.fill(c, app.icon, app.iconShape, rect);
            } else if (app.icon != null) {
                IconArt.center(c, app.icon, app.iconShape, w / 2f, h / 2f, Math.min(w, h) * 0.62f);
            }
        }
        c.restore();
    }

    /** Das ganze Bild, so groß wie es passt; die Fläche drumherum trägt seine Randfarbe. */
    private static void drawContained(Canvas c, Drawable d, int w, int h) {
        int iw = Math.max(1, d.getIntrinsicWidth()), ih = Math.max(1, d.getIntrinsicHeight());
        float k = Math.min(w / (float) iw, h / (float) ih);
        int dw = Math.round(iw * k), dh = Math.round(ih * k), l = (w - dw) / 2, t = (h - dh) / 2;
        d.setBounds(l, t, l + dw, t + dh);
        d.draw(c);
    }

    /** Farben am oberen und unteren Rand des Banners, für die Flächen darüber und darunter. */
    private void readEdges(Drawable d) {
        Bitmap b = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        Drawable copy = d.getConstantState() != null ? d.getConstantState().newDrawable() : d;
        copy.setBounds(0, 0, 8, 8);
        copy.draw(new Canvas(b));
        edgeTop = solid(b.getPixel(1, 0));
        edgeBottom = solid(b.getPixel(1, 7));
        b.recycle();
    }

    private static int solid(int px) {
        return Color.alpha(px) < 128 ? 0xFF2A2F3A : px | 0xFF000000;
    }

    private static int darker(int c) {
        return Color.rgb(Color.red(c) * 6 / 10, Color.green(c) * 6 / 10, Color.blue(c) * 6 / 10);
    }
}
