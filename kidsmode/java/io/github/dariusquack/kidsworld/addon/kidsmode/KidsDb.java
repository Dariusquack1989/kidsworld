package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;

import com.gathin.tvhome.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Aufgaben, Bestätigungen, Nutzungszeit und Protokoll in einer Datenbank. Gutschriften laufen als Transaktion, und
 * {@code ledger.run_id} ist eindeutig: Dieselbe erledigte Aufgabe lässt sich nicht zweimal buchen. Buchungen werden nie
 * geändert, Korrekturen sind neue Buchungen; so ist das {@code ledger} zugleich das Protokoll.
 */
public final class KidsDb extends SQLiteOpenHelper {
    public static final String OPEN = "OFFEN", WAITING = "WARTET", DONE = "BESTAETIGT", REJECTED = "ABGELEHNT";
    public static final String TASK = "AUFGABE", CORRECTION = "KORREKTUR", CARRY = "UEBERTRAG", CLOCK = "UHR", GUARD = "WAECHTER";

    private static final String PREFS = "sc_kids_clock";
    private static KidsDb instance;

    /** Eine Aufgabe mit ihrem Stand an einem Tag. */
    public static final class Task {
        public long id, runId;
        public String kid, title, description, status = OPEN, reason;
        public int minutes, days, until, createdDay, credited;
        public int category = TaskCatalog.SPECIAL, ages = TaskCatalog.AGE_ALL, repeat = KidsCalendar.DAILY;
        public boolean confirm, weekend = true, active = true;
        public long markedAt;
        /** Heute dran; sonst steht die Aufgabe nur in der Liste und lässt sich nicht melden. */
        public boolean today;
    }

    /** Eine Zeile des Protokolls. */
    public static final class Entry {
        public long id, time;
        public int minutes, day;
        public String kid, kind, note;
    }

    /** Stand eines Kindes heute. */
    public static final class Account {
        public int sum, extra;
        public long usedMs;
    }

    private final SharedPreferences clock;
    private final Context app;

    private KidsDb(Context ctx) {
        super(ctx.getApplicationContext(), "kids.db", null, 2);
        clock = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        app = ctx.getApplicationContext();
    }

    public static synchronized KidsDb get(Context ctx) {
        if (instance == null) instance = new KidsDb(ctx);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE task(id INTEGER PRIMARY KEY, kid TEXT NOT NULL, title TEXT NOT NULL, description TEXT,"
                + " minutes INTEGER NOT NULL, days INTEGER NOT NULL, until INTEGER NOT NULL, created_day INTEGER NOT NULL,"
                + " confirm INTEGER NOT NULL, deleted INTEGER NOT NULL DEFAULT 0, category INTEGER NOT NULL DEFAULT 3,"
                + " ages INTEGER NOT NULL DEFAULT 7, rep INTEGER NOT NULL DEFAULT 1, weekend INTEGER NOT NULL DEFAULT 1,"
                + " active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE task_run(id INTEGER PRIMARY KEY, task_id INTEGER NOT NULL, day INTEGER NOT NULL,"
                + " status TEXT NOT NULL, reason TEXT, marked_at INTEGER, decided_at INTEGER, title_then TEXT, minutes_then INTEGER,"
                + " UNIQUE(task_id, day))");
        db.execSQL("CREATE TABLE ledger(id INTEGER PRIMARY KEY, kid TEXT NOT NULL, day INTEGER NOT NULL,"
                + " minutes INTEGER NOT NULL, kind TEXT NOT NULL, run_id INTEGER UNIQUE, note TEXT, time INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE usage_day(kid TEXT NOT NULL, day INTEGER NOT NULL, ms INTEGER NOT NULL,"
                + " PRIMARY KEY(kid, day))");
        db.execSQL("CREATE INDEX ledger_day ON ledger(kid, day)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int from, int to) {
        if (from < 2) {
            // Nur neue Spalten: Vorhandene Aufgaben, Läufe und Buchungen bleiben, wie sie sind.
            db.execSQL("ALTER TABLE task ADD COLUMN category INTEGER NOT NULL DEFAULT 3");
            db.execSQL("ALTER TABLE task ADD COLUMN ages INTEGER NOT NULL DEFAULT 7");
            db.execSQL("ALTER TABLE task ADD COLUMN rep INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE task ADD COLUMN weekend INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE task ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("UPDATE task SET rep = CASE days WHEN 0 THEN 0 WHEN 127 THEN 1 ELSE 2 END");
            db.execSQL("ALTER TABLE task_run ADD COLUMN title_then TEXT");
            db.execSQL("ALTER TABLE task_run ADD COLUMN minutes_then INTEGER");
        }
    }

    private static int wallDay() {
        return KidsCalendar.localDay(System.currentTimeMillis(), TimeZone.getDefault());
    }

    /** Der heutige Tag; merkt sich den höchsten und protokolliert verstellte Uhren. */
    public synchronized int today() {
        long wall = System.currentTimeMillis(), elapsed = SystemClock.elapsedRealtime();
        long jump = KidsCalendar.clockJump(clock.getLong("wall", 0), clock.getLong("elapsed", 0), wall, elapsed);
        int day = KidsCalendar.logicalDay(clock.getInt("day", 0), wallDay());
        if (jump != 0) log("", day, 0, CLOCK, app.getString(R.string.sc_log_clock, (int) (jump / 60_000)));
        clock.edit().putLong("wall", wall).putLong("elapsed", elapsed).putInt("day", day).apply();
        return day;
    }

    /** Vermerkt im Protokoll, dass der Wächter aus war. */
    public synchronized void logGuard(String note) {
        log("", today(), 0, GUARD, note);
    }

    private void log(String kid, int day, int minutes, String kind, String note) {
        ContentValues v = new ContentValues();
        v.put("kid", kid);
        v.put("day", day);
        v.put("minutes", minutes);
        v.put("kind", kind);
        v.put("note", note);
        v.put("time", System.currentTimeMillis());
        getWritableDatabase().insert("ledger", null, v);
    }

    // ---------- Konto ----------

    public synchronized Account account(KidsProfile p) {
        int day = today();
        applyCarry(p, day);
        return accountOn(p.id, day);
    }

    private Account accountOn(String kid, int day) {
        Account a = new Account();
        String[] args = {kid, String.valueOf(day)};
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(minutes),0), COALESCE(SUM(CASE WHEN kind IN ('"
                + TASK + "','" + CARRY + "') THEN minutes ELSE 0 END),0) FROM ledger WHERE kid=? AND day=?", args)) {
            if (c.moveToFirst()) {
                a.sum = c.getInt(0);
                a.extra = c.getInt(1);
            }
        }
        try (Cursor c = getReadableDatabase().rawQuery("SELECT ms FROM usage_day WHERE kid=? AND day=?", args)) {
            if (c.moveToFirst()) a.usedMs = c.getLong(0);
        }
        return a;
    }

    /** Beim ersten Blick auf einen neuen Tag: nicht verfallene Zusatzminuten von gestern übernehmen, einmal je Tag. */
    private void applyCarry(KidsProfile p, int day) {
        if (!p.carryOver || !p.timeOn) return;
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {p.id, String.valueOf(day), CARRY};
        try (Cursor c = db.rawQuery("SELECT 1 FROM ledger WHERE kid=? AND day=? AND kind=?", args)) {
            if (c.moveToFirst()) return;
        }
        Account y = accountOn(p.id, day - 1);
        int carry = MediaTime.carry(p.onDay(KidsCalendar.weekday(day - 1)), y.extra, y.sum, y.usedMs);
        carry = MediaTime.creditable(p.onDay(KidsCalendar.weekday(day)), carry, 0, 0);
        log(p.id, day, carry, CARRY, app.getString(R.string.sc_log_carry));
    }

    public synchronized void addUsage(String kid, long ms) {
        int day = today();
        SQLiteDatabase db = getWritableDatabase();
        // Zwei Schritte statt ON CONFLICT: Android 8 und 9 bringen ein älteres SQLite mit.
        db.execSQL("INSERT OR IGNORE INTO usage_day(kid, day, ms) VALUES(?,?,0)", new Object[]{kid, day});
        db.execSQL("UPDATE usage_day SET ms = ms + ? WHERE kid=? AND day=?", new Object[]{ms, kid, day});
    }

    /** Korrektur von Hand; gebucht wird, was das Tagesmaximum zulässt. Ergibt die gebuchten Minuten. */
    public synchronized int correct(KidsProfile p, int minutes, String note) {
        Account a = account(p);
        int real = MediaTime.correctable(p.onDay(KidsCalendar.weekday(today())), minutes, a.sum);
        if (real != 0) {
            log(p.id, today(), real, CORRECTION, real == minutes ? note : app.getString(R.string.sc_log_capped, note, minutes));
        }
        return real;
    }

    /** Protokoll, neueste zuerst; {@code kid} null = alle Kinder. */
    public synchronized List<Entry> log(String kid, int max) {
        List<Entry> out = new ArrayList<>();
        String where = kid == null ? "" : " WHERE kid=?";
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id, kid, day, minutes, kind, note, time FROM ledger" + where
                + " ORDER BY id DESC LIMIT " + max, kid == null ? null : new String[]{kid})) {
            while (c.moveToNext()) {
                Entry e = new Entry();
                e.id = c.getLong(0);
                e.kid = c.getString(1);
                e.day = c.getInt(2);
                e.minutes = c.getInt(3);
                e.kind = c.getString(4);
                e.note = c.getString(5);
                e.time = c.getLong(6);
                if (e.minutes != 0 || CLOCK.equals(e.kind) || GUARD.equals(e.kind)) out.add(e);
            }
        }
        return out;
    }

    /** Genutzte Minuten je Tag, heute zuerst. */
    public synchronized int[] usageDays(String kid, int count) {
        int[] out = new int[count];
        int day = today();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT day, ms FROM usage_day WHERE kid=? AND day>?",
                new String[]{kid, String.valueOf(day - count)})) {
            while (c.moveToNext()) out[day - c.getInt(0)] = (int) (c.getLong(1) / 60_000);
        }
        return out;
    }

    // ---------- Aufgaben ----------

    /** Neue Aufgabe ({@code t.id} 0) oder Änderung. */
    public synchronized long saveTask(Task t) {
        ContentValues v = new ContentValues();
        v.put("kid", t.kid);
        v.put("title", t.title);
        v.put("description", t.description);
        v.put("minutes", t.minutes);
        v.put("days", t.days);
        v.put("until", t.until);
        v.put("confirm", t.confirm ? 1 : 0);
        v.put("category", t.category);
        v.put("ages", t.ages);
        v.put("rep", t.repeat);
        v.put("weekend", t.weekend ? 1 : 0);
        v.put("active", t.active ? 1 : 0);
        SQLiteDatabase db = getWritableDatabase();
        if (t.id > 0) {
            db.update("task", v, "id=?", new String[]{String.valueOf(t.id)});
            return t.id;
        }
        v.put("created_day", today());
        return db.insert("task", null, v);
    }

    public synchronized void setTaskActive(long id, boolean on) {
        ContentValues v = new ContentValues();
        v.put("active", on ? 1 : 0);
        getWritableDatabase().update("task", v, "id=?", new String[]{String.valueOf(id)});
    }

    /** Erledigte Aufgaben bleiben fürs Protokoll erhalten, sie werden nur nicht mehr gezeigt. */
    public synchronized void deleteTask(long id) {
        ContentValues v = new ContentValues();
        v.put("deleted", 1);
        getWritableDatabase().update("task", v, "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized void deleteTasksOf(String kid) {
        ContentValues v = new ContentValues();
        v.put("deleted", 1);
        getWritableDatabase().update("task", v, "kid=?", new String[]{kid});
    }

    /** Gültige Aufgaben eines Kindes mit ihrem Stand, auch an freien Wochentagen; einmalige verschwinden am Tag nach der Bestätigung. */
    public List<Task> tasks(String kid) {
        return tasks(kid, false);
    }

    /** Wie {@link #tasks(String)}; mit {@code all} auch die deaktivierten, für die Eltern. */
    public synchronized List<Task> tasks(String kid, boolean all) {
        int day = today();
        List<Task> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT t.id, t.title, t.description, t.minutes, t.days, t.until,"
                + " t.created_day, t.confirm, r.id, r.status, r.reason, r.decided_at, r.day, l.minutes,"
                + " t.category, t.ages, t.rep, t.weekend, t.active"
                + " FROM task t LEFT JOIN task_run r ON r.task_id = t.id"
                + " AND r.day = CASE t.rep WHEN 0 THEN 0 WHEN 3 THEN ? ELSE ? END"
                + " LEFT JOIN ledger l ON l.run_id = r.id"
                + " WHERE t.kid = ? AND t.deleted = 0 ORDER BY t.category, t.id",
                new String[]{String.valueOf(KidsCalendar.period(KidsCalendar.WEEKLY, day)), String.valueOf(day), kid})) {
            while (c.moveToNext()) {
                Task t = new Task();
                t.id = c.getLong(0);
                t.kid = kid;
                t.title = c.getString(1);
                t.description = c.getString(2);
                t.minutes = c.getInt(3);
                t.days = c.getInt(4);
                t.until = c.getInt(5);
                t.createdDay = c.getInt(6);
                t.confirm = c.getInt(7) != 0;
                if (!c.isNull(8)) {
                    t.runId = c.getLong(8);
                    t.status = c.getString(9);
                    t.reason = c.getString(10);
                }
                t.credited = c.isNull(13) ? 0 : c.getInt(13);
                t.category = c.getInt(14);
                t.ages = c.getInt(15);
                t.repeat = c.getInt(16);
                t.weekend = c.getInt(17) != 0;
                t.active = c.getInt(18) != 0;
                if (!t.active && !all) continue;
                t.today = t.active && KidsCalendar.available(t.repeat, t.days, t.weekend, t.createdDay, t.until, day);
                if (!KidsCalendar.active(t.createdDay, t.until, day) && t.runId == 0) continue;
                if (t.repeat == KidsCalendar.ONCE && DONE.equals(t.status) && !c.isNull(11) && dayOf(c.getLong(11)) < day) continue;
                out.add(t);
            }
        }
        return out;
    }

    private static int dayOf(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        long local = time + c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
        return (int) Math.floorDiv(local, KidsCalendar.DAY_MS);
    }

    /** Alle wartenden Bestätigungen. */
    public synchronized List<Task> waiting() {
        List<Task> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT r.id, t.id, t.kid, COALESCE(r.title_then, t.title),"
                + " COALESCE(r.minutes_then, t.minutes), r.marked_at, t.category FROM task_run r"
                + " JOIN task t ON t.id = r.task_id WHERE r.status = ? ORDER BY r.marked_at", new String[]{WAITING})) {
            while (c.moveToNext()) {
                Task t = new Task();
                t.runId = c.getLong(0);
                t.id = c.getLong(1);
                t.kid = c.getString(2);
                t.title = c.getString(3);
                t.minutes = c.getInt(4);
                t.markedAt = c.getLong(5);
                t.category = c.getInt(6);
                t.status = WAITING;
                out.add(t);
            }
        }
        return out;
    }

    /**
     * Das Kind meldet eine Aufgabe als fertig. Nur aus OFFEN oder ABGELEHNT heraus; die Aufgabe muss zum Kind gehören.
     * Ohne nötige Bestätigung (von den Eltern je Aufgabe so eingestellt) wird gleich gebucht.
     */
    public synchronized boolean markDone(KidsProfile p, long taskId) {
        int day = today();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int minutes, repeat;
            boolean confirm;
            String title;
            try (Cursor c = db.rawQuery("SELECT days, minutes, confirm, title, rep, weekend, created_day, until, active"
                    + " FROM task WHERE id=? AND kid=? AND deleted=0",
                    new String[]{String.valueOf(taskId), p.id})) {
                if (!c.moveToFirst()) return false;
                repeat = c.getInt(4);
                if (c.getInt(8) == 0 || !KidsCalendar.available(repeat, c.getInt(0), c.getInt(5) != 0, c.getInt(6), c.getInt(7), day)) {
                    return false;
                }
                minutes = c.getInt(1);
                confirm = c.getInt(2) != 0;
                title = c.getString(3);
            }
            int rd = KidsCalendar.period(repeat, day);
            ContentValues v = new ContentValues();
            v.put("task_id", taskId);
            v.put("day", rd);
            v.put("status", OPEN);
            db.insertWithOnConflict("task_run", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            ContentValues u = new ContentValues();
            u.put("status", WAITING);
            u.put("marked_at", System.currentTimeMillis());
            u.put("title_then", title);
            u.put("minutes_then", minutes);
            u.putNull("reason");
            int changed = db.update("task_run", u, "task_id=? AND day=? AND status IN (?,?)",
                    new String[]{String.valueOf(taskId), String.valueOf(rd), OPEN, REJECTED});
            if (changed != 1) return false;
            if (!confirm) {
                try (Cursor c = db.rawQuery("SELECT id FROM task_run WHERE task_id=? AND day=?",
                        new String[]{String.valueOf(taskId), String.valueOf(rd)})) {
                    c.moveToFirst();
                    finish(db, p, c.getLong(0), minutes, title);
                }
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Bestätigt eine wartende Aufgabe. Ergibt die gebuchten Minuten, oder -1 wenn sie nicht (mehr) wartet. */
    public synchronized int confirm(KidsProfiles profiles, long runId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String kid, title;
            int minutes;
            try (Cursor c = db.rawQuery("SELECT t.kid, COALESCE(r.minutes_then, t.minutes), COALESCE(r.title_then, t.title)"
                    + " FROM task_run r JOIN task t ON t.id = r.task_id"
                    + " WHERE r.id=? AND r.status=?", new String[]{String.valueOf(runId), WAITING})) {
                if (!c.moveToFirst()) return -1;
                kid = c.getString(0);
                minutes = c.getInt(1);
                title = c.getString(2);
            }
            KidsProfile p = profiles.byId(kid);
            if (p == null) return -1;
            int real = finish(db, p, runId, minutes, title);
            db.setTransactionSuccessful();
            return real;
        } finally {
            db.endTransaction();
        }
    }

    /** Statuswechsel und Buchung; die eindeutige {@code run_id} verhindert jede zweite Gutschrift. */
    private int finish(SQLiteDatabase db, KidsProfile p, long runId, int minutes, String title) {
        int day = today();
        ContentValues u = new ContentValues();
        u.put("status", DONE);
        u.put("decided_at", System.currentTimeMillis());
        db.update("task_run", u, "id=?", new String[]{String.valueOf(runId)});
        Account a = accountOn(p.id, day);
        int real = MediaTime.creditable(p.onDay(KidsCalendar.weekday(day)), minutes, a.extra, a.sum);
        ContentValues v = new ContentValues();
        v.put("kid", p.id);
        v.put("day", day);
        v.put("minutes", real);
        v.put("kind", TASK);
        v.put("run_id", runId);
        v.put("note", real == minutes ? title : app.getString(R.string.sc_log_capped, title, minutes));
        v.put("time", System.currentTimeMillis());
        db.insertOrThrow("ledger", null, v);
        return real;
    }

    /** Lehnt eine wartende Aufgabe ab; das Kind sieht sie mit Begründung wieder als offen. */
    public synchronized boolean reject(long runId, String reason) {
        ContentValues u = new ContentValues();
        u.put("status", REJECTED);
        u.put("reason", reason);
        u.put("decided_at", System.currentTimeMillis());
        return getWritableDatabase().update("task_run", u, "id=? AND status=?",
                new String[]{String.valueOf(runId), WAITING}) == 1;
    }

    /** Neueste Aufgaben-Buchung eines Kindes, für den Hinweis am TV; 0 wenn keine. */
    public synchronized Entry lastCredit(String kid) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id, minutes, note FROM ledger WHERE kid=? AND kind=?"
                + " ORDER BY id DESC LIMIT 1", new String[]{kid, TASK})) {
            if (!c.moveToFirst()) return null;
            Entry e = new Entry();
            e.id = c.getLong(0);
            e.minutes = c.getInt(1);
            e.note = c.getString(2);
            return e;
        }
    }
}
