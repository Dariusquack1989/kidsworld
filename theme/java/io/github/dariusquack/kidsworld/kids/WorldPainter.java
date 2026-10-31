package io.github.dariusquack.kidsworld.kids;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import io.github.dariusquack.kidsworld.addon.kidsmode.KidsArt;

/**
 * Die stehenden Hintergründe der Welten. Liegt ein aufbereitetes Bild zur Welt vor (Themes/KidsWorld/Bilder/Aufbereitet), wird es benutzt;
 * sonst malt der Code einen Hintergrund in halber Auflösung.
 */
final class WorldPainter {
    private WorldPainter() {}

    /** Hintergrundbild, auf {@code width} Pixel Breite verkleinert; null wenn es keins gibt. */
    static Bitmap image(Context ctx, World world, int width) {
        try (InputStream in = ctx.getAssets().open("hintergruende/hintergrund_" + world.image + ".jpg")) {
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap b = BitmapFactory.decodeStream(in, null, d);
            if (b == null || b.getWidth() <= width) return b;
            Bitmap small = Bitmap.createScaledBitmap(b, width, Math.round(b.getHeight() * (width / (float) b.getWidth())), true);
            if (small != b) b.recycle();
            return small;
        } catch (IOException e) {
            return null;
        }
    }

    /** Maskottchen der Welt, halbe Größe reicht für den TV; null, wenn es keins gibt. */
    static Bitmap mascot(Context ctx, World world) {
        return KidsArt.load(ctx, "maskottchen/maskottchen_" + world.image + ".png", 2);
    }

    /** Welt-Symbol für den Wechsler oben. */
    static Bitmap symbol(Context ctx, World world) {
        return KidsArt.load(ctx, "welt_symbole/welt_" + world.image + ".png", 4);
    }

    /** Gemalter Hintergrund, in halber Auflösung. */
    static Bitmap drawn(World world, int screenW, int screenH) {
        int w = Math.max(1, screenW / 2), h = Math.max(1, screenH / 2);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        switch (world) {
            case SPACE:
                drawSpace(c, p, w, h);
                break;
            case SEA:
                drawSea(c, p, w, h);
                break;
            case JUNGLE:
                drawJungle(c, p, w, h);
                break;
            default:
                drawMagic(c, p, w, h);
        }
        return b;
    }

    private static void drawMagic(Canvas c, Paint p, int w, int h) {
        vertical(c, p, w, h, 0xFF2B1E66, 0xFF6A45B8, 0xFFF4B6C8);
        Random r = new Random(4);
        for (int i = 0; i < 70; i++) {
            p.setColor(Color.argb(70 + r.nextInt(150), 255, 248, 225));
            c.drawCircle(r.nextFloat() * w, r.nextFloat() * h * 0.55f, r.nextFloat() < 0.15f ? 1.4f : 0.7f, p);
        }
        // Eine Wolkenbank am unteren Rand
        p.setColor(0x4DFFFFFF);
        c.drawOval(new RectF(-w * 0.1f, h * 0.84f, w * 0.55f, h * 1.12f), p);
        c.drawOval(new RectF(w * 0.4f, h * 0.87f, w * 1.1f, h * 1.14f), p);
        p.setColor(0x33FFFFFF);
        c.drawOval(new RectF(w * 0.1f, h * 0.78f, w * 0.8f, h * 1.05f), p);
    }

    private static void drawSpace(Canvas c, Paint p, int w, int h) {
        vertical(c, p, w, h, 0xFF141A5C, 0xFF3A2A8C, 0xFF6A3FB0);
        Random r = new Random(1);
        for (int i = 0; i < 140; i++) {
            p.setColor(Color.argb(80 + r.nextInt(150), 255, 255, 255));
            c.drawCircle(r.nextFloat() * w, r.nextFloat() * h, r.nextFloat() < 0.1f ? 1.4f : 0.7f, p);
        }
        // Großer Ringplanet unten links
        float px = w * 0.07f, py = h * 0.95f, pr = h * 0.25f;
        p.setShader(new RadialGradient(px - pr * 0.3f, py - pr * 0.4f, pr * 1.3f, 0xFFFFC371, 0xFFFF6B3D,
                Shader.TileMode.CLAMP));
        c.drawCircle(px, py, pr, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(h * 0.018f);
        p.setColor(0xCCFFE29A);
        c.save();
        c.rotate(-14, px, py);
        c.drawOval(new RectF(px - pr * 1.7f, py - pr * 0.35f, px + pr * 1.7f, py + pr * 0.35f), p);
        c.restore();
        p.setStyle(Paint.Style.FILL);
        // Kleiner türkiser Planet und ein Mond
        p.setShader(new RadialGradient(w * 0.86f, h * 0.2f, h * 0.09f, 0xFF7CF5DD, 0xFF16A89A,
                Shader.TileMode.CLAMP));
        c.drawCircle(w * 0.88f, h * 0.22f, h * 0.07f, p);
        p.setShader(null);
        p.setColor(0xFFE9E6F5);
        c.drawCircle(w * 0.68f, h * 0.1f, h * 0.035f, p);
        p.setColor(0xFFC9C3E0);
        c.drawCircle(w * 0.675f, h * 0.095f, h * 0.008f, p);
        c.drawCircle(w * 0.688f, h * 0.112f, h * 0.005f, p);
    }

    private static void drawSea(Canvas c, Paint p, int w, int h) {
        vertical(c, p, w, h, 0xFF3FD3F0, 0xFF1180C0, 0xFF0A3E78);
        // Lichtstrahlen von oben
        Random r = new Random(2);
        for (int i = 0; i < 6; i++) {
            float x = w * (0.1f + i * 0.16f + r.nextFloat() * 0.05f);
            Path ray = new Path();
            ray.moveTo(x, 0);
            ray.lineTo(x + w * 0.05f, 0);
            ray.lineTo(x + w * 0.16f, h * 0.85f);
            ray.lineTo(x + w * 0.07f, h * 0.85f);
            ray.close();
            p.setShader(new LinearGradient(0, 0, 0, h * 0.85f, 0x33FFFFFF, Color.TRANSPARENT,
                    Shader.TileMode.CLAMP));
            c.drawPath(ray, p);
        }
        p.setShader(null);
        // Sandboden
        Path sand = new Path();
        sand.moveTo(0, h);
        sand.lineTo(0, h * 0.9f);
        sand.cubicTo(w * 0.3f, h * 0.84f, w * 0.6f, h * 0.95f, w, h * 0.87f);
        sand.lineTo(w, h);
        sand.close();
        p.setColor(0xFFF3D497);
        c.drawPath(sand, p);
        // Muschel und Seestern
        p.setColor(0xFFFF8FA3);
        c.drawCircle(w * 0.22f, h * 0.93f, h * 0.018f, p);
        star(c, p, w * 0.78f, h * 0.92f, h * 0.03f, 0xFFFF9F43);
    }

    private static void drawJungle(Canvas c, Paint p, int w, int h) {
        vertical(c, p, w, h, 0xFFB8F0A0, 0xFF4CC46E, 0xFF1C7A45);
        // Sonne
        p.setShader(new RadialGradient(w * 0.82f, h * 0.15f, h * 0.2f, 0xCCFFF3A0, Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        c.drawCircle(w * 0.82f, h * 0.15f, h * 0.2f, p);
        p.setShader(null);
        p.setColor(0xFFFFE066);
        c.drawCircle(w * 0.82f, h * 0.15f, h * 0.06f, p);
        // Hügel in drei Grüntönen
        hill(c, p, w, h, 0.74f, 0xFF3DA862);
        hill(c, p, w, h, 0.84f, 0xFF2A8A4E);
        hill(c, p, w, h, 0.93f, 0xFF1B6B3B);
        // Ein paar Blumen
        flower(c, p, w * 0.30f, h * 0.9f, h * 0.018f, 0xFFFF6FA5);
        flower(c, p, w * 0.62f, h * 0.95f, h * 0.016f, 0xFFFFD23F);
    }

    private static void vertical(Canvas c, Paint p, int w, int h, int top, int mid, int bottom) {
        p.setShader(new LinearGradient(0, 0, 0, h, new int[]{top, mid, bottom}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
    }

    private static void hill(Canvas c, Paint p, int w, int h, float base, int color) {
        Path hill = new Path();
        hill.moveTo(0, h);
        hill.lineTo(0, h * base);
        hill.cubicTo(w * 0.25f, h * (base - 0.08f), w * 0.55f, h * (base + 0.05f), w, h * (base - 0.04f));
        hill.lineTo(w, h);
        hill.close();
        p.setColor(color);
        c.drawPath(hill, p);
    }

    private static void flower(Canvas c, Paint p, float x, float y, float r, int color) {
        p.setColor(color);
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5;
            c.drawCircle(x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r, r * 0.8f, p);
        }
        p.setColor(0xFFFFF3B0);
        c.drawCircle(x, y, r * 0.6f, p);
    }

    private static void star(Canvas c, Paint p, float x, float y, float r, int color) {
        Path s = new Path();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float rr = i % 2 == 0 ? r : r * 0.45f;
            float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
            if (i == 0) s.moveTo(px, py); else s.lineTo(px, py);
        }
        s.close();
        p.setColor(color);
        c.drawPath(s, p);
    }
}
