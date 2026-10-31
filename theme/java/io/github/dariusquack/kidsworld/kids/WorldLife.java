package io.github.dariusquack.kidsworld.kids;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * Was sich in einer Welt bewegt: wenige Dinge gleichzeitig (Fische, Pflanzen, Blätter, Inseln, Lichtpunkte) und selten
 * eine Überraschung. Liegt unter den Kacheln, verdeckt die App-Auswahl also nie. Läuft mit 30 Bildern pro Sekunde und
 * nur, solange die Startseite sichtbar ist.
 *
 * <p>Liegt ein Bild im Ordner Bilder, entfällt alles Gezeichnete, was neben dem gemalten Bild billig wirkt (Kulisse,
 * Tiere): Es bleiben Lichtpunkte, Blasen und Lichtspuren. Das Maskottchen ist eine eigene Ebene ({@link KidsView}).
 */
final class WorldLife extends View {
    private static final long FRAME_MS = 33;
    private static final int POINTS = 9;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final float[][] points = new float[POINTS][4];
    private final Surprises surprises = new Surprises(new Random());
    private World world = World.SPACE;
    private boolean running;
    private boolean calm;
    private boolean hasImage;

    WorldLife(Context ctx) {
        super(ctx);
        Random r = new Random(5);
        for (float[] p : points) {
            p[0] = r.nextFloat();
            p[1] = r.nextFloat();
            p[2] = r.nextFloat() * 6.28f;
            p[3] = 0.6f + r.nextFloat() * 0.8f;
        }
    }

    void setWorld(World w, boolean hasImage) {
        world = w;
        this.hasImage = hasImage;
        invalidate();
    }

    void setCalm(boolean on) {
        calm = on;
    }

    void setRunning(boolean on) {
        running = on;
        if (on) invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        long now = SystemClock.uptimeMillis();
        float t = now / 1000f;
        switch (world) {
            case SPACE:
                twinkles(c, w, h, t);
                break;
            case SEA:
                if (!hasImage) plants(c, w, h, t);
                bubbles(c, w, h, t);
                if (!calm && !hasImage) fish(c, (t * 0.022f + 0.3f) % 1.3f * w - 0.15f * w, h * 0.2f, h * 0.05f, 0xFFFFD23F, false);
                break;
            case JUNGLE:
                if (!hasImage) leaves(c, w, h, t);
                if (!calm && !hasImage) butterfly(c, w * (0.5f + 0.42f * (float) Math.sin(t * 0.13f)), h * (0.17f + 0.05f * (float) Math.sin(t * 0.5f)), h * 0.035f, t);
                break;
            default:
                if (!hasImage) islands(c, w, h, t);
                sparkles(c, w, h, t);
        }
        if (!calm) {
            float s = surprises.progress(now);
            if (s >= 0) surprise(c, w, h, s, surprises.variant());
        }
        if (running) postInvalidateDelayed(FRAME_MS);
    }

    private void twinkles(Canvas c, int w, int h, float t) {
        for (float[] p : points) {
            float a = 0.2f + 0.8f * Math.abs((float) Math.sin(t * p[3] + p[2]));
            paint.setColor(Color.argb(Math.round(255 * a), 255, 255, 230));
            c.drawCircle(p[0] * w, p[1] * h * 0.26f + (p[1] > 0.5f ? h * 0.66f : 0), h * 0.0042f * p[3], paint);
        }
    }

    private void bubbles(Canvas c, int w, int h, float t) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.0024f);
        paint.setColor(0x99FFFFFF);
        for (float[] p : points) {
            float rise = (t * h * 0.035f * p[3] + p[1] * h) % (h * 1.1f);
            c.drawCircle(p[0] * w + (float) Math.sin(t + p[2]) * h * 0.012f, h * 1.05f - rise, h * 0.009f * p[3], paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void plants(Canvas c, int w, int h, float t) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(h * 0.024f);
        float[][] weeds = {{0.04f, 0.5f}, {0.08f, 0.38f}, {0.12f, 0.46f}, {0.88f, 0.42f}, {0.93f, 0.52f}, {0.97f, 0.36f}};
        for (int i = 0; i < weeds.length; i++) {
            float base = w * weeds[i][0], top = h * weeds[i][1];
            path.reset();
            path.moveTo(base, h);
            for (float y = h; y > top; y -= h * 0.05f) {
                float grow = (h - y) / (h - top);
                float sway = (float) Math.sin(y / h * 12 + t * 0.9f + i) * h * 0.022f * grow;
                path.lineTo(base + sway, y);
            }
            paint.setColor(i % 2 == 0 ? 0xFF22B573 : 0xFF13875A);
            c.drawPath(path, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void leaves(Canvas c, int w, int h, float t) {
        leaf(c, w * 0.02f, h * 0.05f, h * 0.34f, 35 + sway(t, 0), 0xFF1F8F4E);
        leaf(c, w * 0.08f, h * -0.02f, h * 0.27f, 65 + sway(t, 1), 0xFF2FAE60);
        leaf(c, w * 0.98f, h * 0.98f, h * 0.32f, 215 + sway(t, 2), 0xFF1F8F4E);
        leaf(c, 0, h * 0.9f, h * 0.25f, -40 + sway(t, 3), 0xFF2FAE60);
        leaf(c, w * 0.99f, h * 0.02f, h * 0.2f, 140 + sway(t, 4), 0xFF27A057);
    }

    private static float sway(float t, int i) {
        return (float) Math.sin(t * 0.8f + i * 1.3f) * 3.5f;
    }

    private void leaf(Canvas c, float x, float y, float len, float deg, int color) {
        c.save();
        c.rotate(deg, x, y);
        path.reset();
        path.moveTo(x, y);
        path.quadTo(x + len * 0.5f, y - len * 0.32f, x + len, y);
        path.quadTo(x + len * 0.5f, y + len * 0.32f, x, y);
        paint.setColor(color);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(len * 0.015f);
        paint.setColor(0x55FFFFFF);
        c.drawLine(x, y, x + len * 0.95f, y, paint);
        paint.setStyle(Paint.Style.FILL);
        c.restore();
    }

    private void islands(Canvas c, int w, int h, float t) {
        island(c, w * 0.09f, h * (0.3f + 0.012f * (float) Math.sin(t * 0.5f)), h * 0.13f, 0xFF6FD39A, 0xFF8C6BB0);
        island(c, w * 0.9f, h * (0.27f + 0.012f * (float) Math.sin(t * 0.42f + 2)), h * 0.15f, 0xFF7ADBA6, 0xFF9476BC);
        paint.setColor(0x38FFFFFF);
        float drift = (t * 0.008f) % 1.4f - 0.2f;
        cloud(c, drift * w, h * 0.68f, h * 0.2f);
        cloud(c, ((drift + 0.7f) % 1.4f - 0.2f) * w, h * 0.16f, h * 0.15f);
    }

    private void island(Canvas c, float x, float y, float r, int grass, int rock) {
        paint.setColor(rock);
        path.reset();
        path.moveTo(x - r * 0.8f, y);
        path.lineTo(x + r * 0.8f, y);
        path.lineTo(x + r * 0.1f, y + r * 0.95f);
        path.lineTo(x - r * 0.1f, y + r * 0.7f);
        path.close();
        c.drawPath(path, paint);
        paint.setColor(grass);
        oval.set(x - r, y - r * 0.2f, x + r, y + r * 0.2f);
        c.drawOval(oval, paint);
    }

    private void cloud(Canvas c, float x, float y, float r) {
        c.drawCircle(x, y, r * 0.45f, paint);
        c.drawCircle(x + r * 0.5f, y + r * 0.08f, r * 0.34f, paint);
        c.drawCircle(x - r * 0.5f, y + r * 0.1f, r * 0.3f, paint);
    }

    private void sparkles(Canvas c, int w, int h, float t) {
        for (float[] p : points) {
            float a = Math.abs((float) Math.sin(t * 0.8f * p[3] + p[2]));
            float x = p[0] * w + (float) Math.sin(t * 0.3f + p[2]) * h * 0.03f;
            float y = (p[1] * 0.3f + (p[1] > 0.5f ? 0.55f : 0.02f)) * h;
            // Ein kleiner Stern aus zwei Lichtstrichen
            float r = h * 0.007f * a;
            paint.setColor(Color.argb(Math.round(240 * a), 255, 248, 225));
            paint.setStrokeWidth(h * 0.0022f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine(x - r, y, x + r, y, paint);
            c.drawLine(x, y - r, x, y + r, paint);
        }
    }

    private void fish(Canvas c, float x, float y, float r, int color, boolean leftward) {
        c.save();
        if (leftward) c.scale(-1, 1, x, y);
        paint.setColor(color);
        oval.set(x - r, y - r * 0.55f, x + r, y + r * 0.55f);
        c.drawOval(oval, paint);
        path.reset();
        path.moveTo(x - r * 0.8f, y);
        path.lineTo(x - r * 1.5f, y - r * 0.5f);
        path.lineTo(x - r * 1.5f, y + r * 0.5f);
        path.close();
        c.drawPath(path, paint);
        paint.setColor(Color.WHITE);
        c.drawCircle(x + r * 0.5f, y - r * 0.12f, r * 0.15f, paint);
        paint.setColor(0xFF1B2A4A);
        c.drawCircle(x + r * 0.54f, y - r * 0.12f, r * 0.08f, paint);
        c.restore();
    }

    private void butterfly(Canvas c, float x, float y, float r, float t) {
        float flap = 0.45f + 0.55f * Math.abs((float) Math.sin(t * 7));
        paint.setColor(0xFFFF6FA5);
        oval.set(x - r * flap, y - r * 0.8f, x, y + r * 0.2f);
        c.drawOval(oval, paint);
        oval.set(x, y - r * 0.8f, x + r * flap, y + r * 0.2f);
        c.drawOval(oval, paint);
        paint.setColor(0xFFFFB547);
        oval.set(x - r * 0.7f * flap, y, x, y + r * 0.7f);
        c.drawOval(oval, paint);
        oval.set(x, y, x + r * 0.7f * flap, y + r * 0.7f);
        c.drawOval(oval, paint);
        paint.setColor(0xFF3B2A1E);
        oval.set(x - r * 0.08f, y - r * 0.5f, x + r * 0.08f, y + r * 0.6f);
        c.drawOval(oval, paint);
    }

    /** Die seltene Überraschung; bleibt im oberen oder unteren Band des Bildes. */
    private void surprise(Canvas c, int w, int h, float s, int variant) {
        if (hasImage) {
            // Neben einem gemalten Bild nur Licht, keine gezeichneten Tiere.
            switch (world) {
                case SPACE:
                    shootingStar(c, w, h, s);
                    break;
                case SEA:
                    bubbleSwarm(c, w, h, s);
                    break;
                case JUNGLE:
                    fireflySwarm(c, w, h, s);
                    break;
                default:
                    lightTrail(c, w, h, s);
            }
            return;
        }
        switch (world) {
            case SPACE:
                if (variant == 0) shootingStar(c, w, h, s); else spaceship(c, s * 1.3f * w - 0.15f * w, h * 0.17f, h * 0.05f);
                break;
            case SEA:
                fish(c, (1.15f - s * 1.3f) * w, h * 0.86f, h * 0.08f, variant == 0 ? 0xFFFF9F43 : 0xFFFF6FA5, true);
                break;
            case JUNGLE:
                if (variant == 0) monkey(c, w, h, s); else butterfly(c, s * 1.2f * w - 0.1f * w, h * 0.85f, h * 0.06f, SystemClock.uptimeMillis() / 1000f);
                break;
            default:
                lightTrail(c, w, h, s);
        }
    }

    /** Ein Schwarm Blasen steigt rechts neben den Kacheln auf. */
    private void bubbleSwarm(Canvas c, int w, int h, float s) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(h * 0.003f);
        for (int i = 0; i < 12; i++) {
            float k = s * 1.5f - i * 0.04f;
            if (k < 0 || k > 1) continue;
            float x = w * (0.9f + 0.05f * (float) Math.sin(i * 2.1f + k * 6));
            paint.setColor(Color.argb(Math.round(200 * (1 - k)), 255, 255, 255));
            c.drawCircle(x, h * (1.02f - k * 1.05f), h * (0.008f + 0.004f * (i % 3)), paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** Ein Band aus Glühwürmchen zieht unten durchs Bild. */
    private void fireflySwarm(Canvas c, int w, int h, float s) {
        for (int i = 0; i < 12; i++) {
            float x = (s * 1.3f - 0.15f - i * 0.018f) * w;
            float y = h * (0.9f + 0.035f * (float) Math.sin(i * 1.7f + s * 12));
            float a = 0.5f + 0.5f * Math.abs((float) Math.sin(i + s * 30));
            paint.setColor(Color.argb(Math.round(70 * a), 255, 240, 130));
            c.drawCircle(x, y, h * 0.012f, paint);
            paint.setColor(Color.argb(Math.round(250 * a), 255, 250, 200));
            c.drawCircle(x, y, h * 0.0042f, paint);
        }
    }

    private void shootingStar(Canvas c, int w, int h, float s) {
        float p = Math.min(1f, s * 2.4f), x = w * (0.2f + p * 0.6f), y = h * (0.05f + p * 0.14f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(h * 0.004f);
        for (int i = 0; i < 4; i++) {
            float k = i / 4f;
            paint.setColor(Color.argb(Math.round(200 * (1 - k) * (1 - p)), 255, 255, 255));
            c.drawLine(x - k * w * 0.09f, y - k * h * 0.02f, x - (k + 0.25f) * w * 0.09f, y - (k + 0.25f) * h * 0.02f, paint);
        }
    }

    private void spaceship(Canvas c, float x, float y, float r) {
        paint.setColor(0xFFFF6B6B);
        path.reset();
        path.moveTo(x - r * 0.7f, y - r * 0.4f);
        path.lineTo(x - r * 1.2f, y - r * 0.95f);
        path.lineTo(x - r * 0.3f, y - r * 0.4f);
        path.moveTo(x - r * 0.7f, y + r * 0.4f);
        path.lineTo(x - r * 1.2f, y + r * 0.95f);
        path.lineTo(x - r * 0.3f, y + r * 0.4f);
        c.drawPath(path, paint);
        paint.setColor(0xFFE9E6F5);
        oval.set(x - r * 1.2f, y - r * 0.45f, x + r * 1.2f, y + r * 0.45f);
        c.drawOval(oval, paint);
        paint.setColor(0xFF5CC8FF);
        c.drawCircle(x + r * 0.45f, y, r * 0.24f, paint);
        paint.setColor(0xCCFFB547);
        c.drawCircle(x - r * 1.35f, y, r * 0.2f, paint);
    }

    private void monkey(Canvas c, int w, int h, float s) {
        // Schaut unten links kurz über den Rand und verschwindet wieder.
        float up = (float) Math.sin(Math.PI * s), r = h * 0.075f, x = w * 0.12f, y = h + r * 1.1f - up * r * 2.2f;
        paint.setColor(0xFF8A5A36);
        c.drawCircle(x - r * 1.05f, y, r * 0.45f, paint);
        c.drawCircle(x + r * 1.05f, y, r * 0.45f, paint);
        c.drawCircle(x, y, r, paint);
        paint.setColor(0xFFE8C49B);
        oval.set(x - r * 0.65f, y - r * 0.1f, x + r * 0.65f, y + r * 0.75f);
        c.drawOval(oval, paint);
        paint.setColor(0xFF2B1A10);
        c.drawCircle(x - r * 0.35f, y - r * 0.28f, r * 0.13f, paint);
        c.drawCircle(x + r * 0.35f, y - r * 0.28f, r * 0.13f, paint);
    }

    private void lightTrail(Canvas c, int w, int h, float s) {
        for (int i = 0; i < 14; i++) {
            float k = s * 1.2f - i * 0.022f;
            if (k < 0 || k > 1) continue;
            float x = w * (0.08f + k * 0.84f), y = h * (0.2f - 0.11f * (float) Math.sin(Math.PI * k));
            paint.setColor(Color.argb(Math.round(230 * (1 - i / 14f)), 255, 240, 190));
            c.drawCircle(x, y, h * 0.0062f * (1 - i / 22f), paint);
        }
    }
}
