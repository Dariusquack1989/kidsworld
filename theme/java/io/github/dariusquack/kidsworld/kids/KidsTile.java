package io.github.dariusquack.kidsworld.kids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import io.github.dariusquack.kidsworld.IconArt;

/**
 * Eine App-Kachel als eigene View. Sie wird einmal in Fokusgröße in einen Hardware-Layer gemalt und danach nur noch
 * verschoben, gedreht und skaliert; so kostet die Schwingbewegung auf dem TV fast nichts. Neu gemalt wird nur, wenn
 * sich der Fokus ändert (Rahmenfarbe).
 */
final class KidsTile extends View {
    /** Inhalt einer Kachel. */
    static final class Data {
        final Drawable icon;
        final IconArt.Shape shape;
        final String label;
        final int color;

        Data(Drawable icon, IconArt.Shape shape, String label, int color) {
            this.icon = icon;
            this.shape = shape;
            this.label = label;
            this.color = color;
        }
    }

    private final Data data;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();
    private final int square;
    private boolean focusedTile;

    KidsTile(Context ctx, Data data, int square, Typeface font) {
        super(ctx);
        this.data = data;
        this.square = square;
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setContentDescription(data.label);
        labelPaint.setTypeface(font);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(square * 0.145f);
        labelPaint.setShadowLayer(square * 0.02f, 0, square * 0.008f, 0x73000000);
    }

    void setFocusedTile(boolean on) {
        if (on == focusedTile) return;
        focusedTile = on;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float inset = square * 0.05f, s = square - 2 * inset, radius = s * 0.24f;
        float l = (getWidth() - s) / 2f, t = inset;
        // Tiefe: ein weicher, nach unten versetzter Schatten
        rect.set(l, t + s * 0.05f, l + s, t + s * 1.05f);
        paint.setColor(0x38000000);
        c.drawRoundRect(rect, radius, radius, paint);
        // Rahmen: gewählt weiß, sonst in der Farbe der App
        rect.set(l, t, l + s, t + s);
        paint.setColor(focusedTile ? Color.WHITE : data.color);
        c.drawRoundRect(rect, radius, radius, paint);
        float border = s * 0.05f;
        rect.inset(border, border);
        float inner = radius - border;
        paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, mix(data.color, Color.WHITE, 0.8f),
                mix(data.color, Color.WHITE, 0.52f), Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, inner, inner, paint);
        paint.setShader(null);
        if (data.icon != null) {
            c.save();
            clip.reset();
            clip.addRoundRect(rect, inner, inner, Path.Direction.CW);
            c.clipPath(clip);
            data.icon.setAlpha(255);
            if (data.shape.filled) {
                IconArt.fill(c, data.icon, data.shape, new RectF(rect));
            } else {
                IconArt.center(c, data.icon, data.shape, rect.centerX(), rect.centerY(), s * 0.64f);
            }
            c.restore();
        }
        // Name auf einer dunklen Pille, damit er auf jedem Hintergrund lesbar bleibt.
        String name = TextUtils.ellipsize(data.label, labelPaint, getWidth() * 0.86f, TextUtils.TruncateAt.END).toString();
        float textW = labelPaint.measureText(name), baseline = t + s + square * 0.2f, pad = square * 0.05f;
        float mid = getWidth() / 2f, size = labelPaint.getTextSize();
        rect.set(mid - textW / 2f - pad * 1.6f, baseline - size * 0.98f, mid + textW / 2f + pad * 1.6f, baseline + size * 0.34f);
        paint.setColor(0x66000000);
        c.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, paint);
        c.drawText(name, mid, baseline, labelPaint);
    }

    static int mix(int a, int b, float t) {
        return Color.rgb(Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
}
