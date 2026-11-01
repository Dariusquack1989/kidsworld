package io.github.dariusquack.kidsworld.kids;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gathin.tvhome.R;

import java.util.List;

import io.github.dariusquack.kidsworld.addon.kidsmode.KidsDb;
import io.github.dariusquack.kidsworld.addon.kidsmode.TaskCatalog;

/**
 * „Meine Aufgaben“: oben die Medienzeit, darunter die Aufgaben als große Karten. OK meldet eine Aufgabe als fertig; die
 * Minuten gibt es erst, wenn die Eltern bestätigen. Das Kind sieht hier nur, ändern kann es nichts außer dem Melden.
 */
final class TaskView extends FrameLayout {
    interface Listener {
        void onMark(KidsDb.Task task);
    }

    private static final int[] CATEGORIES = {R.string.sc_task_cat_school, R.string.sc_task_cat_room, R.string.sc_task_cat_home,
            R.string.sc_task_cat_special};

    private final Typeface font;
    private final Listener listener;
    private final LinearLayout list;
    private final LinearLayout summary;

    TaskView(Context ctx, Typeface font, Listener listener) {
        super(ctx);
        this.font = font;
        this.listener = listener;
        setBackgroundColor(0xF2120C33);

        LinearLayout column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(dp(60), dp(26), dp(60), dp(16));
        addView(column, new LayoutParams(-1, -1));

        column.addView(text(ctx.getString(R.string.sc_kids_tasks), 32, Color.WHITE));
        summary = new LinearLayout(ctx);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setPadding(0, dp(6), 0, dp(10));
        column.addView(summary);

        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        column.addView(scroll, new LinearLayout.LayoutParams(dp(620), 0, 1));
        TextView back = text(ctx.getString(R.string.sc_kids_tasks_back), 14, 0xB3FFFFFF);
        back.setPadding(0, dp(8), 0, 0);
        column.addView(back);
    }

    /** Zeigt den Stand; die Karte an Position {@code focus} bekommt den Fokus. */
    void show(boolean timeOn, int available, int used, int left, int earned, List<KidsDb.Task> tasks, int focus) {
        summary.removeAllViews();
        if (timeOn) {
            fact(R.string.sc_kids_time_left, left);
            fact(R.string.sc_kids_time_used, used);
            fact(R.string.sc_kids_time_earned, earned);
        }
        list.removeAllViews();
        if (tasks.isEmpty()) {
            TextView none = text(getContext().getString(R.string.sc_kids_tasks_none), 22, Color.WHITE);
            none.setFocusable(true);
            list.addView(none, new LinearLayout.LayoutParams(-1, dp(100)));
            none.requestFocus();
            return;
        }
        for (KidsDb.Task t : tasks) list.addView(card(t));
        View target = list.getChildAt(Math.max(0, Math.min(focus, list.getChildCount() - 1)));
        target.requestFocus();
    }

    int focused() {
        for (int i = 0; i < list.getChildCount(); i++) {
            if (list.getChildAt(i).isFocused()) return i;
        }
        return 0;
    }

    private void fact(int label, int minutes) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.addView(text(getContext().getString(R.string.sc_kids_minutes, minutes), 26, 0xFFFFD36E));
        box.addView(text(getContext().getString(label), 15, 0xCCFFFFFF));
        summary.addView(box, new LinearLayout.LayoutParams(dp(190), -2));
    }

    private View card(KidsDb.Task t) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.sc_kids_card);
        row.setPadding(dp(18), dp(10), dp(18), dp(10));
        row.setFocusable(true);
        row.setClickable(true);

        LinearLayout words = new LinearLayout(getContext());
        words.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(t.title, 22, Color.WHITE);
        title.setGravity(Gravity.START);
        words.addView(title);
        TextView kind = text(TaskCatalog.symbol(t.category) + " " + getContext().getString(CATEGORIES[t.category]), 14, 0xCCFFFFFF);
        kind.setGravity(Gravity.START);
        words.addView(kind);
        String detail = t.reason != null && !t.reason.isEmpty() ? t.reason : t.description;
        if (detail != null && !detail.isEmpty()) {
            TextView desc = text(detail, 15, 0xCCFFFFFF);
            desc.setGravity(Gravity.START);
            words.addView(desc);
        }
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout side = new LinearLayout(getContext());
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.END);
        side.addView(text(getContext().getString(R.string.sc_kids_plus, t.minutes), 20, 0xFFFFD36E));
        side.addView(text(getContext().getString(t.today ? statusText(t.status) : R.string.sc_kids_task_free), 15, 0xFFFFFFFF));
        row.addView(side);
        if (t.today) row.setOnClickListener(v -> listener.onMark(t));
        else row.setAlpha(0.6f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        row.setLayoutParams(lp);
        return row;
    }

    private static int statusText(String status) {
        switch (status) {
            case KidsDb.WAITING:
                return R.string.sc_kids_task_waiting;
            case KidsDb.DONE:
                return R.string.sc_kids_task_done;
            case KidsDb.REJECTED:
                return R.string.sc_kids_task_again;
            default:
                return R.string.sc_kids_task_open;
        }
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTypeface(font);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
