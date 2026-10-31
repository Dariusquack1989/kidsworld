package io.github.dariusquack.kidsworld;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** App-Kacheln für Reihen und Raster: Bild, Fokusrahmen, Vergrößern bei Fokus, OK startet die App. */
public final class TileAdapter extends RecyclerView.Adapter<TileAdapter.Holder> {
    public interface OnFocus {
        void onFocus(AppEntry app);
    }

    private static final long FOCUS_MS = 150;

    private final HomeActivity host;
    private final int width, height, ring;
    private final float radius, scale, lift;
    private final IconColors colors;
    private final OnFocus onFocus;
    private List<AppEntry> apps = new ArrayList<>();

    /**
     * @param width  Kachelbreite in px, oder -1 für die Spaltenbreite des Rasters
     * @param ring   Fokusrahmen (Drawable mit state_focused)
     * @param lift   Anheben bei Fokus in px
     */
    public TileAdapter(HomeActivity host, int width, int height, float radius, int ring, float scale, float lift,
                       IconColors colors, OnFocus onFocus) {
        this.host = host;
        this.width = width;
        this.height = height;
        this.radius = radius;
        this.ring = ring;
        this.scale = scale;
        this.lift = lift;
        this.colors = colors;
        this.onFocus = onFocus;
    }

    /** Fokus auf die erste Kachel; wartet, bis die Reihe sie angelegt hat. */
    public static void focusFirst(RecyclerView row) {
        focusFirst(row, 20);
    }

    private static void focusFirst(RecyclerView row, int tries) {
        row.postDelayed(() -> {
            RecyclerView.ViewHolder vh = row.findViewHolderForAdapterPosition(0);
            if (vh != null) vh.itemView.requestFocus();
            else if (tries > 0) focusFirst(row, tries - 1);
        }, 30);
    }

    public void setApps(List<AppEntry> list) {
        if (list.equals(apps)) return;
        apps = list;
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int type) {
        FrameLayout box = new FrameLayout(parent.getContext());
        box.setLayoutParams(new RecyclerView.LayoutParams(width, height));
        box.setFocusable(true);
        box.setClickable(true);
        AppArtView art = new AppArtView(parent.getContext(), radius);
        box.addView(art, new FrameLayout.LayoutParams(-1, -1));
        Drawable fg = parent.getContext().getDrawable(ring);
        box.setForeground(fg);
        return new Holder(box, art);
    }

    @Override
    public void onBindViewHolder(Holder h, int pos) {
        h.bind(apps.get(pos));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final AppArtView art;
        private AppEntry app;

        Holder(View v, AppArtView art) {
            super(v);
            this.art = art;
            v.setOnClickListener(x -> {
                if (app != null) host.launch(app);
            });
            v.setOnFocusChangeListener((x, f) -> {
                x.animate().scaleX(f ? scale : 1f).scaleY(f ? scale : 1f).translationY(f ? -lift : 0f)
                        .translationZ(f ? lift + 8 : 0f).setDuration(FOCUS_MS).start();
                if (f && app != null) onFocus.onFocus(app);
            });
        }

        void bind(AppEntry a) {
            app = a;
            art.show(a, colors.of(a));
        }
    }
}
