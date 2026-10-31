package io.github.dariusquack.kidsworld.kids;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Die Bühne von Kids World, von hinten nach vorn: zwei Bildebenen für den weichen Weltwechsel, die bewegte Welt
 * ({@link WorldLife}) und die Kacheln. Links/Rechts wählt die App, Hoch/Runter wechselt die Welt.
 *
 * <p>Pro Bild ändern sich nur Position, Drehung und Größe der Kacheln (GPU, kein Neumalen). Eine Taste ändert sofort
 * das Ziel, die Bewegung läuft hinterher; so kann eine Animation die Bedienung nie aufhalten.
 */
public final class KidsView extends FrameLayout {
    private static final long PARENT_HOLD_MS = 5000;
    private static final long FADE_MS = 450;
    private static final float CARD = 0.367f;     // Kachel im Fokus, Anteil der Bildhöhe
    private static final float REST = 0.76f;      // Größe ohne Fokus
    private static final float GAP = 0.40f;       // Abstand der Mitten, Anteil der Bildhöhe
    private static final float CENTER_Y = 0.49f;
    private static final float FOLLOW = 13f;      // Wie schnell Bewegungen ihr Ziel erreichen (pro Sekunde)
    private static final long BOUNCE_MS = 260;
    /** Hintergründe werden nicht größer geladen: spart auf dem TV Speicher, das Bild bleibt ruhig genug. */
    private static final int BACK_WIDTH = 1280;
    private static final float MASCOT_HEIGHT = 0.36f;

    public interface Listener {
        void onLaunch(int index);

        void onWorldChanged(World world);

        /** OK wurde 5 Sekunden gehalten: Elternbereich öffnen. */
        void onParentRequest();
    }

    private final ImageView backA, backB;
    private final WorldLife life;
    private final View glow;
    private final Typeface font;
    private final List<KidsTile> tiles = new ArrayList<>();
    private final Map<World, Bitmap> backdrops = new ConcurrentHashMap<>();
    private final Map<World, Boolean> images = new ConcurrentHashMap<>();
    private final Map<World, Bitmap> mascots = new ConcurrentHashMap<>();
    private final MascotMotion motion = new MascotMotion(new Random());
    private final ImageView mascot;
    private final HoldDetector okHold = new HoldDetector(PARENT_HOLD_MS);
    private final Choreographer.FrameCallback frame = this::onFrame;
    private List<KidsTile.Data> pending;
    private float[] focus = new float[0];
    private World world = World.SPACE;
    private Listener listener;
    private float cam;
    private int selected;
    private boolean running;
    private boolean calm;
    private long lastFrame;
    private long bounceAt = -1;
    private ValueAnimator fade;
    private Bitmap shownMascot;
    private int tileWidth;
    private boolean locked;

    public KidsView(Context ctx, Typeface font) {
        super(ctx);
        this.font = font;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClipChildren(false);
        backA = back(ctx);
        backB = back(ctx);
        life = new WorldLife(ctx);
        addView(life, new LayoutParams(-1, -1));
        mascot = new ImageView(ctx);
        mascot.setLayerType(LAYER_TYPE_HARDWARE, null);
        addView(mascot, new LayoutParams(1, 1));
        glow = new View(ctx);
        glow.setAlpha(0f);
        addView(glow, new LayoutParams(1, 1));
    }

    private ImageView back(Context ctx) {
        ImageView v = new ImageView(ctx);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(v, new LayoutParams(-1, -1));
        return v;
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setCalm(boolean on) {
        calm = on;
        life.setCalm(on);
        motion.setCalm(on);
    }

    /** Gesperrt: Nur OK halten (Elternbereich) wirkt noch. */
    public void setLocked(boolean on) {
        locked = on;
    }

    public int selected() {
        return selected;
    }

    /** Neue Kacheln; {@code keep} ist die Kachel, die gewählt sein soll. */
    void setTiles(List<KidsTile.Data> list, int keep) {
        for (KidsTile t : tiles) removeView(t);
        tiles.clear();
        focus = new float[list.size()];
        selected = list.isEmpty() ? 0 : Math.max(0, Math.min(list.size() - 1, keep));
        cam = selected;
        if (focus.length > 0) focus[selected] = 1f;
        // Noch ohne Größe wartet die Liste; eine neuere ersetzt sie, damit nichts doppelt angelegt wird.
        pending = null;
        if (getHeight() > 0) buildTiles(list, getHeight());
        else pending = list;
    }

    private void buildTiles(List<KidsTile.Data> list, int h) {
        int square = Math.round(h * CARD);
        int width = Math.round(square * 1.3f), height = Math.round(square * 1.22f);
        tileWidth = width;
        for (KidsTile.Data d : list) {
            KidsTile t = new KidsTile(getContext(), d, square, font);
            t.setPivotX(width / 2f);
            t.setPivotY(square / 2f);
            tiles.add(t);
            addView(t, new LayoutParams(width, height));
        }
        if (!tiles.isEmpty()) tiles.get(selected).setFocusedTile(true);
        // Sofort auf den Platz, sonst blitzen die Kacheln einen Moment oben links auf.
        place(0f);
    }

    public void setWorld(World w) {
        boolean first = backA.getDrawable() == null;
        if (!first && w == world) return;
        world = w;
        if (getWidth() == 0) return;
        Bitmap b = backdrop(w);
        life.setWorld(w, Boolean.TRUE.equals(images.get(w)));
        showMascot();
        if (first) {
            backA.setImageBitmap(b);
            backB.setAlpha(0f);
            return;
        }
        if (fade != null) fade.cancel();
        backB.setImageBitmap(b);
        backB.setAlpha(0f);
        fade = ValueAnimator.ofFloat(0f, 1f);
        fade.setDuration(FADE_MS);
        fade.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            backB.setAlpha(v);
            if (v >= 1f) {
                backA.setImageBitmap(b);
                backB.setAlpha(0f);
            }
        });
        fade.start();
    }

    private Bitmap backdrop(World w) {
        Bitmap b = backdrops.get(w);
        if (b == null) {
            int width = Math.min(BACK_WIDTH, getWidth());
            b = WorldPainter.image(getContext(), w, width);
            images.put(w, b != null);
            if (b == null) b = WorldPainter.drawn(w, getWidth(), getHeight());
            backdrops.put(w, b);
        }
        return b;
    }

    /** Lädt alle Welten vor, damit Hoch/Runter nicht auf ein Bild warten muss. */
    private void preload() {
        new Thread(() -> {
            for (World w : World.values()) {
                backdrop(w);
                Bitmap m = WorldPainter.mascot(getContext(), w);
                if (m != null) mascots.put(w, m);
            }
            post(this::showMascot);
        }, "kids-backdrops").start();
    }

    /** Maskottchen der aktuellen Welt; hüpft herein, wenn es neu ist. */
    private void showMascot() {
        Bitmap b = mascots.get(world);
        if (b == null || b == shownMascot) return;
        shownMascot = b;
        int h = Math.round(getHeight() * MASCOT_HEIGHT), w = h * b.getWidth() / b.getHeight();
        LayoutParams lp = (LayoutParams) mascot.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mascot.setLayoutParams(lp);
        mascot.setImageBitmap(b);
        mascot.setPivotX(w / 2f);
        mascot.setPivotY(h);
        motion.enter(SystemClock.uptimeMillis());
    }

    /** Besonderes Ereignis: Das Maskottchen freut sich. */
    public void cheer() {
        motion.cheer(SystemClock.uptimeMillis());
    }

    private void placeMascot() {
        int h = getHeight();
        if (h == 0 || shownMascot == null) return;
        long now = SystemClock.uptimeMillis();
        float t = now / 1000f;
        float idle = calm ? 0f : (float) Math.sin(t * 1.3f) * h * 0.005f;
        mascot.setTranslationX(getWidth() * 0.012f);
        mascot.setTranslationY(h * (1.01f - MASCOT_HEIGHT) + motion.hidden(now) * h * MASCOT_HEIGHT
                - motion.lift(now) * h + idle);
        mascot.setRotation(motion.tilt(now));
        float s = motion.scale(now);
        mascot.setScaleX(s);
        mascot.setScaleY(s);
    }

    /** Bewegung an (Startseite sichtbar) oder aus. */
    public void setRunning(boolean on) {
        running = on;
        life.setRunning(on);
        Choreographer ch = Choreographer.getInstance();
        ch.removeFrameCallback(frame);
        if (on) {
            lastFrame = 0;
            ch.postFrameCallback(frame);
        }
    }

    public void goHome() {
        select(0);
    }

    private void select(int index) {
        if (tiles.isEmpty()) return;
        int next = Math.max(0, Math.min(tiles.size() - 1, index));
        if (next == selected) return;
        tiles.get(selected).setFocusedTile(false);
        selected = next;
        tiles.get(selected).setFocusedTile(true);
        motion.nod(SystemClock.uptimeMillis());
    }

    private void onFrame(long nanos) {
        long ms = nanos / 1_000_000L;
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (ms - lastFrame) / 1000f);
        lastFrame = ms;
        float k = 1f - (float) Math.exp(-FOLLOW * dt);
        cam += (selected - cam) * k;
        place(k);
        placeMascot();
        if (running) Choreographer.getInstance().postFrameCallback(frame);
    }

    /** Setzt jede Kachel auf ihren Platz. {@code k}: wie weit die Fokus-Werte diesmal aufholen. */
    private void place(float k) {
        int h = getHeight(), w = getWidth();
        if (h == 0 || tiles.isEmpty()) return;
        float square = h * CARD, gap = h * GAP, t = SystemClock.uptimeMillis() / 1000f;
        float amp = h * (calm ? 0.003f : 0.009f), tilt = calm ? 0.4f : 1.5f;
        float bounce = bounce();
        for (int i = 0; i < tiles.size(); i++) {
            KidsTile v = tiles.get(i);
            float x = w / 2f + (i - cam) * gap;
            boolean show = x > -gap && x < w + gap;
            v.setVisibility(show ? VISIBLE : INVISIBLE);
            focus[i] += ((i == selected ? 1f : 0f) - focus[i]) * k;
            if (!show) continue;
            float f = focus[i];
            float phase = i * 1.9f;
            float bob = (float) Math.sin(t * 1.15f + phase) * amp;
            float rot = (float) Math.sin(t * 0.8f + phase * 1.4f) * tilt * (1f - 0.75f * f);
            float scale = REST + (1f - REST) * f;
            if (i == selected) scale *= bounce;
            v.setTranslationX(x - tileWidth / 2f);
            v.setTranslationY(h * CENTER_Y - square / 2f + bob);
            v.setRotation(rot);
            v.setScaleX(scale);
            v.setScaleY(scale);
            if (i == selected) {
                glow.setTranslationX(x - glow.getWidth() / 2f);
                glow.setTranslationY(h * CENTER_Y + bob - glow.getHeight() / 2f);
                glow.setAlpha(f);
            }
        }
    }

    /** Kurzes Nachfedern beim Drücken von OK. */
    private float bounce() {
        if (bounceAt < 0) return 1f;
        long since = SystemClock.uptimeMillis() - bounceAt;
        if (since >= BOUNCE_MS) {
            bounceAt = -1;
            return 1f;
        }
        return 1f - 0.07f * (float) Math.sin(Math.PI * since / BOUNCE_MS);
    }

    private static boolean isOk(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isOk(keyCode)) {
            if (event.getRepeatCount() == 0) bounceAt = SystemClock.uptimeMillis();
            // Gestartet wird erst beim Loslassen, damit langes Halten nicht vorher eine App öffnet.
            if (okHold.down(event.getDownTime(), event.getEventTime(), event.getRepeatCount())
                    == HoldDetector.Action.HOLD && listener != null) {
                listener.onParentRequest();
            }
            return true;
        }
        if (locked) return true;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                select(selected + 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                select(selected - 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (event.getRepeatCount() > 0) return true;
                setWorld(world.step(keyCode == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1));
                if (listener != null) listener.onWorldChanged(world);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isOk(keyCode)) {
            if (okHold.up() == HoldDetector.Action.PRESS && !locked && listener != null && !tiles.isEmpty()) {
                listener.onLaunch(selected);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w == 0 || h == 0) return;
        post(() -> {
            sizeGlow(h);
            setWorld(world);
            preload();
            if (pending != null) {
                List<KidsTile.Data> list = pending;
                pending = null;
                buildTiles(list, h);
            }
        });
    }

    /** Sanfter Lichtschein hinter der gewählten Kachel. */
    private void sizeGlow(int h) {
        int size = Math.round(h * CARD * 1.55f);
        LayoutParams lp = (LayoutParams) glow.getLayoutParams();
        lp.width = size;
        lp.height = size;
        glow.setLayoutParams(lp);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        d.setGradientRadius(size / 2f);
        d.setColors(new int[]{0x8CFFF0BE, 0x00FFF0BE});
        glow.setBackground(d);
    }
}
