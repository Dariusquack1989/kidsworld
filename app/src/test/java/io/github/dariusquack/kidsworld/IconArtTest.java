package io.github.dariusquack.kidsworld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IconArtTest {
    private static final int N = 48;
    private static final int OPAQUE = 0xFFFF0000;

    @Test
    public void fullSquareIsFilled() {
        int[] px = new int[N * N];
        java.util.Arrays.fill(px, OPAQUE);
        IconArt.Shape s = IconArt.fromAlpha(px, N);
        assertTrue(s.filled);
        assertEquals(0f, s.left, 1e-6);
        assertEquals(1f, s.right, 1e-6);
    }

    @Test
    public void insetSquareIsFilledAndKnowsItsBounds() {
        int[] px = new int[N * N];
        for (int y = 6; y < 42; y++) for (int x = 6; x < 42; x++) px[y * N + x] = OPAQUE;
        IconArt.Shape s = IconArt.fromAlpha(px, N);
        assertTrue(s.filled);
        assertEquals(6f / N, s.left, 1e-6);
        assertEquals(42f / N, s.bottom, 1e-6);
    }

    @Test
    public void triangleLogoIsNotFilled() {
        int[] px = new int[N * N];
        for (int y = 0; y < N; y++) for (int x = 0; x <= y; x++) px[y * N + x] = OPAQUE;
        assertFalse(IconArt.fromAlpha(px, N).filled);
    }

    @Test
    public void wideBarIsNotFilled() {
        int[] px = new int[N * N];
        for (int y = 18; y < 30; y++) for (int x = 0; x < N; x++) px[y * N + x] = OPAQUE;
        assertFalse(IconArt.fromAlpha(px, N).filled);
    }

    @Test
    public void emptyIconIsUnknown() {
        assertFalse(IconArt.fromAlpha(new int[N * N], N).filled);
    }
}
