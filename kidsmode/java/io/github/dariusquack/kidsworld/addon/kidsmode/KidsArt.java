package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

/** Lädt die aufbereiteten Grafiken aus den Assets der App, verkleinert um {@code sample}; fehlt eine, kommt null. */
public final class KidsArt {
    private KidsArt() {}

    public static Bitmap load(Context ctx, String path, int sample) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        try (InputStream in = ctx.getAssets().open(path)) {
            return BitmapFactory.decodeStream(in, null, o);
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap avatar(Context ctx, KidsProfile p) {
        return load(ctx, "avatare/avatar_" + p.avatarName() + ".png", 2);
    }
}
