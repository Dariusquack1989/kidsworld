package io.github.dariusquack.kidsworld.kids;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gathin.tvhome.R;

import java.util.List;

import io.github.dariusquack.kidsworld.addon.kidsmode.KidsArt;
import io.github.dariusquack.kidsworld.addon.kidsmode.KidsProfile;

/** „Wer spielt heute?“: große runde Avatare, per Steuerkreuz gewählt. Die Profile selbst verwaltet Kids Mode. */
final class ProfilePicker extends FrameLayout {
    interface Listener {
        void onPick(KidsProfile p);

        void onParents();
    }

    private static final float FOCUS_SCALE = 1.22f;

    private final Typeface font;
    private final LinearLayout row;

    ProfilePicker(Context ctx, Typeface font, List<KidsProfile> profiles, Listener listener) {
        super(ctx);
        this.font = font;
        setBackgroundColor(0xFF1B1450);
        Bitmap back = KidsArt.load(ctx, "hintergruende/hintergrund_profil_auswahl.jpg", 2);
        if (back != null) setBackground(new android.graphics.drawable.BitmapDrawable(getResources(), back));
        setClickable(true);
        setClipChildren(false);

        LinearLayout column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        addView(column, new LayoutParams(-2, -2, Gravity.CENTER));

        TextView title = new TextView(ctx);
        title.setText(R.string.sc_kids_who);
        title.setTypeface(font);
        title.setTextColor(Color.WHITE);
        title.setTextSize(40);
        title.setGravity(Gravity.CENTER);
        column.addView(title);

        row = new LinearLayout(ctx);
        row.setClipChildren(false);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, -2);
        rlp.topMargin = dp(36);
        column.addView(row, rlp);
        column.setClipChildren(false);

        for (KidsProfile p : profiles) {
            row.addView(avatar(p.name, KidsArt.avatar(ctx, p), () -> listener.onPick(p)));
        }
        row.addView(avatar(ctx.getString(R.string.sc_kids_parents), null, listener::onParents));
    }

    private View avatar(String name, Bitmap image, Runnable click) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(26), 0, dp(26), 0);
        box.setClipChildren(false);
        box.setClipToPadding(false);

        ImageView face = new ImageView(getContext());
        if (image != null) face.setImageBitmap(image);
        else {
            face.setImageResource(android.R.drawable.ic_lock_lock);
            face.setColorFilter(Color.WHITE);
            face.setPadding(dp(34), dp(34), dp(34), dp(34));
        }
        if (image != null) face.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0x33FFFFFF);
        bg.setStroke(dp(4), 0x66FFFFFF);
        face.setBackground(bg);
        face.setFocusable(true);
        face.setClickable(true);
        face.setOnClickListener(v -> click.run());
        face.setContentDescription(name);
        face.setOnFocusChangeListener((v, f) -> {
            float s = f ? FOCUS_SCALE : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(140).start();
            bg.setStroke(dp(f ? 6 : 4), f ? Color.WHITE : 0x66FFFFFF);
        });
        box.addView(face, new LinearLayout.LayoutParams(dp(150), dp(150)));

        TextView label = new TextView(getContext());
        label.setText(name);
        label.setTypeface(font);
        label.setTextSize(22);
        label.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = dp(20);
        box.addView(label, lp);
        return box;
    }

    void focusFirst() {
        ((LinearLayout) row.getChildAt(0)).getChildAt(0).requestFocus();
    }

    /** Zurück schließt die Auswahl nicht: Es muss jemand gewählt werden. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return true;
        return super.dispatchKeyEvent(event);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
