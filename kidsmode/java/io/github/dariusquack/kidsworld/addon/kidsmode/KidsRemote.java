package io.github.dariusquack.kidsworld.addon.kidsmode;

import android.content.Context;
import android.content.SharedPreferences;

import com.gathin.tvhome.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.dariusquack.kidsworld.AppEntry;
import io.github.dariusquack.kidsworld.AppRepository;

/**
 * Handy-Verwaltung im Heimnetz: Der TV stellt eine Seite bereit, die man im Handy-Browser öffnet. Nur gekoppelte Handys
 * kommen hinein: Beim ersten Mal braucht es die PIN und einen Code, der auf dem TV erscheint, danach reicht die PIN.
 * Ändernde Aktionen laufen nur per POST, der Zugangsschlüssel steht im Kopf der Anfrage.
 *
 * <p>Die Verbindung ist unverschlüsselt (HTTP): Im eigenen WLAN vertretbar, die PIN wäre aber im Netz mitlesbar. Der
 * Server läuft nur, solange der Wächter läuft.
 */
public final class KidsRemote {
    public static final int PORT = 8686;
    private static final long SESSION_MS = 60 * 60_000L;
    private static final long APPS_MS = 5 * 60_000L;
    private static final long CODE_MS = 5 * 60_000L;
    private static final int CODE_TRIES = 5;
    private static final int TIMEOUT_MS = 5000;
    private static final int MAX_REQUEST = 8192;
    private static final int MAX_MESSAGE = 200, MAX_NAME = 20, MAX_TITLE = 60, MINUTES_PER_DAY = 1440;
    private static final int LOG_LINES = 40, USAGE_DAYS = 7, DEVICE_ID = 12;
    private static final String DEVICES = "sc_kids_devices", KEY_DEVICES = "devices", DEVICE_SALT = "kidsworld-device";

    /** Was der Wächter für die Handy-Verwaltung tut; läuft nicht auf dem Haupt-Thread. */
    public interface Listener {
        void onChange();

        void onMessage(String text);

        void onOpenSettings();
    }

    /** Name und Icon, wenn die Seite auf den Startbildschirm des Handys gelegt wird. */
    private static final String MANIFEST = "{\"name\":\"Kids World\",\"short_name\":\"Kids World\",\"start_url\":\"/\","
            + "\"scope\":\"/\",\"display\":\"standalone\",\"background_color\":\"#14122b\",\"theme_color\":\"#14122b\","
            + "\"icons\":[{\"src\":\"/icon-192.png\",\"sizes\":\"192x192\",\"type\":\"image/png\"},"
            + "{\"src\":\"/icon-512.png\",\"sizes\":\"512x512\",\"type\":\"image/png\"}]}";

    private static final class Request {
        String method, path, token, host, origin, agent, ip;
        Map<String, String> params = new HashMap<>();
    }

    private final Context ctx;
    private final Listener listener;
    private final KidsLock lock;
    private final KidsProfiles profiles;
    private final KidsGate gate;
    private final KidsDb db;
    private final SharedPreferences devices;
    private final SecureRandom random = new SecureRandom();
    private static final Map<String, Long> sessions = new HashMap<>();
    /** Welches Gerät zu einer Sitzung gehört; beide Karten stehen unter dem Schloss von {@code sessions}. */
    private static final Map<String, String> sessionDevice = new HashMap<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(6);
    private List<AppEntry> apps = new ArrayList<>();
    private long appsAt;
    private String pairCode;
    private long pairUntil;
    private int pairTries;
    private volatile ServerSocket server;

    public KidsRemote(Context ctx, Listener listener) {
        this.ctx = ctx.getApplicationContext();
        this.listener = listener;
        lock = new KidsLock(ctx);
        profiles = new KidsProfiles(ctx);
        gate = new KidsGate(ctx);
        db = KidsDb.get(ctx);
        devices = this.ctx.getSharedPreferences(DEVICES, Context.MODE_PRIVATE);
    }

    public void start() {
        Thread t = new Thread(this::serve, "kids-remote");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        ServerSocket s = server;
        server = null;
        workers.shutdownNow();
        try {
            if (s != null) s.close();
        } catch (IOException ignored) {
            // Beim Beenden egal.
        }
    }

    private void serve() {
        try (ServerSocket s = new ServerSocket(PORT)) {
            server = s;
            while (server != null) {
                try {
                    Socket c = s.accept();
                    // Eigene Bearbeitung je Verbindung: Browser halten oft eine leere Reserveverbindung offen.
                    workers.execute(() -> serve(c));
                } catch (IOException | RuntimeException e) {
                    if (server == null) return;
                }
            }
        } catch (IOException e) {
            // Port belegt oder Netz fehlt: ohne Handy-Verwaltung weiterlaufen.
        }
    }

    private void serve(Socket socket) {
        try (Socket c = socket) {
            c.setSoTimeout(TIMEOUT_MS);
            handle(c);
        } catch (IOException | RuntimeException ignored) {
            // Verbindung abgebrochen: Das Handy fragt neu an.
        }
    }

    private void handle(Socket c) throws IOException {
        OutputStream out = c.getOutputStream();
        Request r = read(c.getInputStream());
        if (r != null) r.ip = c.getInetAddress().getHostAddress().replace("::ffff:", "");
        KidsLanguage.apply(ctx);
        if (r == null) {
            send(out, "400 Bad Request", "text/plain", new byte[0]);
        } else if (!RemoteText.localRequest(r.host, r.origin)) {
            send(out, "403 Forbidden", "text/plain", new byte[0]);
        } else if (r.method.equals("GET")) {
            if (r.path.equals("/")) {
                String page = new String(readAll(ctx.getResources().openRawResource(R.raw.sc_remote)), StandardCharsets.UTF_8);
                send(out, "200 OK", "text/html; charset=utf-8",
                        page.replace("%LANG%", KidsLanguage.shown(ctx)).getBytes(StandardCharsets.UTF_8));
            } else if (r.path.equals("/manifest.webmanifest")) {
                send(out, "200 OK", "application/manifest+json", MANIFEST.getBytes(StandardCharsets.UTF_8));
            } else if (r.path.matches("/icon-(180|192|512)\\.png")) {
                art(out, "handy/icon_" + r.path.substring(6, r.path.length() - 4) + ".png");
            } else if (r.path.startsWith("/art/") && RemoteText.safeArt(r.path.substring(5))) {
                art(out, r.path.substring(5));
            } else {
                send(out, "404 Not Found", "text/plain", new byte[0]);
            }
        } else if (!r.method.equals("POST")) {
            send(out, "405 Method Not Allowed", "text/plain", new byte[0]);
        } else if (r.path.equals("/api/login")) {
            login(out, r);
        } else if (r.path.equals("/api/pair")) {
            pair(out, r);
        } else if (!authorized(r.token)) {
            sendJson(out, "401 Unauthorized", "{\"error\":\"login\"}");
        } else if (r.path.equals("/api/state")) {
            sendJson(out, "200 OK", state(r.token));
        } else if (r.path.equals("/api/do")) {
            String error = act(r.params);
            listener.onChange();
            if (error != null) sendJson(out, "400 Bad Request", "{\"error\":" + RemoteText.str(error) + "}");
            else sendJson(out, "200 OK", state(r.token));
        } else {
            send(out, "404 Not Found", "text/plain", new byte[0]);
        }
    }

    /** Liest Kopf und Inhalt einer Anfrage; null wenn sie kaputt oder zu groß ist. */
    private static Request read(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int ch, end = -1;
        while (end < 0 && (ch = in.read()) >= 0) {
            b.write(ch);
            if (b.size() > MAX_REQUEST) return null;
            if (ch == '\n' && b.toString("ISO-8859-1").endsWith("\r\n\r\n")) end = b.size();
        }
        if (end < 0) return null;
        String[] lines = b.toString("ISO-8859-1").split("\r\n");
        String[] first = lines[0].split(" ");
        if (first.length != 3) return null;
        Request r = new Request();
        r.method = first[0];
        r.path = RemoteText.path(first[1]);
        int length = 0;
        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String name = line.substring(0, colon).trim().toLowerCase(), value = line.substring(colon + 1).trim();
            if (name.equals("content-length")) length = RemoteText.number(value, 0);
            if (name.equals("x-token")) r.token = value;
            if (name.equals("host")) r.host = value;
            if (name.equals("origin")) r.origin = value;
            if (name.equals("user-agent")) r.agent = value;
        }
        if (length < 0 || length > MAX_REQUEST) return null;
        byte[] body = new byte[length];
        int got = 0, n;
        while (got < length && (n = in.read(body, got, length - got)) > 0) got += n;
        if (got < length) return null;
        r.params = RemoteText.query("?" + new String(body, StandardCharsets.UTF_8));
        return r;
    }

    private static void send(OutputStream out, String status, String type, byte[] body) throws IOException {
        String head = "HTTP/1.1 " + status + "\r\nContent-Type: " + type + "\r\nContent-Length: " + body.length
                + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
    }

    private static void sendJson(OutputStream out, String status, String json) throws IOException {
        send(out, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream s = in) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = s.read(buf)) > 0) b.write(buf, 0, n);
            return b.toByteArray();
        }
    }

    private void art(OutputStream out, String file) throws IOException {
        try {
            send(out, "200 OK", "image/png", readAll(ctx.getAssets().open(file)));
        } catch (IOException e) {
            send(out, "404 Not Found", "text/plain", new byte[0]);
        }
    }

    // ---------- Anmeldung ----------

    private String randomHex(int bytes) {
        byte[] raw = new byte[bytes];
        random.nextBytes(raw);
        StringBuilder t = new StringBuilder();
        for (byte x : raw) t.append(String.format("%02x", x));
        return t.toString();
    }

    private Set<String> knownDevices() {
        return new HashSet<>(devices.getStringSet(KEY_DEVICES, new HashSet<>()));
    }

    private static String deviceHash(String device) {
        return PinHash.hash(DEVICE_SALT, device);
    }

    /** Prüft die PIN; sendet bei einem Fehler die Antwort und ergibt false. */
    private boolean pinOk(OutputStream out, String pin) throws IOException {
        if (!lock.hasPin()) {
            sendJson(out, "403 Forbidden", "{\"error\":\"nopin\"}");
            return false;
        }
        KidsLock.Result r = PinHash.valid(pin) ? lock.check(pin) : KidsLock.Result.WRONG;
        if (r == KidsLock.Result.OK) return true;
        boolean locked = r == KidsLock.Result.LOCKED;
        sendJson(out, locked ? "429 Too Many Requests" : "403 Forbidden",
                "{\"error\":\"" + (locked ? "locked" : "wrong") + "\"}");
        return false;
    }

    private String newSession(String device) {
        String t = randomHex(16);
        synchronized (sessions) {
            sessions.put(t, System.currentTimeMillis() + SESSION_MS);
            sessionDevice.put(t, device);
        }
        return t;
    }

    /** Merkt sich Name und letzte Nutzung; einen Namen von den Eltern überschreibt die Browser-Kennung nicht. */
    private void touchDevice(String hash, Request r, boolean paired) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor e = devices.edit().putLong("seen_" + hash, now).putString("agent_" + hash, RemoteText.deviceName(r.agent))
                .putString("ip_" + hash, r.ip);
        if (paired) e.putLong("paired_" + hash, now);
        e.apply();
    }

    private String deviceHashOf(String shortId) {
        for (String h : knownDevices()) {
            if (h.startsWith(shortId)) return h;
        }
        return null;
    }

    /** Entfernt ein Gerät und beendet seine Sitzungen. */
    private void forgetDevice(String hash) {
        Set<String> known = knownDevices();
        known.remove(hash);
        devices.edit().putStringSet(KEY_DEVICES, known).remove("seen_" + hash).remove("agent_" + hash).remove("paired_" + hash).remove("ip_" + hash)
                .remove("name_" + hash).apply();
        synchronized (sessions) {
            Iterator<Map.Entry<String, String>> it = sessionDevice.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> s = it.next();
                if (hash.equals(s.getValue())) {
                    sessions.remove(s.getKey());
                    it.remove();
                }
            }
        }
    }

    private void login(OutputStream out, Request r) throws IOException {
        if (!pinOk(out, r.params.get("pin"))) return;
        String device = r.params.get("device");
        if (device != null && knownDevices().contains(deviceHash(device))) {
            touchDevice(deviceHash(device), r, false);
            sendJson(out, "200 OK", "{\"token\":\"" + newSession(deviceHash(device)) + "\"}");
            return;
        }
        // Unbekanntes Handy: Code auf dem TV zeigen. Wer nur vor dem TV sitzt, kennt die PIN nicht.
        String code;
        synchronized (this) {
            pairCode = String.format("%06d", random.nextInt(1_000_000));
            pairUntil = System.currentTimeMillis() + CODE_MS;
            pairTries = 0;
            code = pairCode;
        }
        listener.onMessage(ctx.getString(R.string.sc_remote_pair_code, code));
        sendJson(out, "200 OK", "{\"pair\":true}");
    }

    private void pair(OutputStream out, Request r) throws IOException {
        if (!pinOk(out, r.params.get("pin"))) return;
        boolean ok;
        synchronized (this) {
            ok = pairCode != null && System.currentTimeMillis() < pairUntil && pairCode.equals(r.params.get("code"));
            if (ok || ++pairTries >= CODE_TRIES) pairCode = null;
        }
        if (!ok) {
            sendJson(out, "403 Forbidden", "{\"error\":\"code\"}");
            return;
        }
        String device = randomHex(24);
        Set<String> known = knownDevices();
        known.add(deviceHash(device));
        devices.edit().putStringSet(KEY_DEVICES, known).apply();
        touchDevice(deviceHash(device), r, true);
        sendJson(out, "200 OK", "{\"token\":\"" + newSession(deviceHash(device)) + "\",\"device\":\"" + device + "\"}");
    }

    private boolean authorized(String token) {
        if (token == null) return false;
        synchronized (sessions) {
            Long until = sessions.get(token);
            return until != null && until > System.currentTimeMillis();
        }
    }

    /** Alle gekoppelten Handys entfernen. */
    public static void forgetDevices(Context ctx) {
        ctx.getSharedPreferences(DEVICES, Context.MODE_PRIVATE).edit().clear().apply();
        endSessions();
    }

    /** Beendet alle offenen Anmeldungen, etwa nach einer neuen PIN. */
    public static void endSessions() {
        synchronized (sessions) {
            sessions.clear();
            sessionDevice.clear();
        }
    }

    // ---------- Daten ----------

    private synchronized List<AppEntry> apps() {
        if (apps.isEmpty() || System.currentTimeMillis() - appsAt > APPS_MS) {
            apps = new ArrayList<>(AppRepository.load(ctx));
            Collections.sort(apps, (x, y) -> x.label.compareToIgnoreCase(y.label));
            appsAt = System.currentTimeMillis();
        }
        return apps;
    }

    private static void field(StringBuilder b, String name, Object value) {
        b.append('"').append(name).append("\":");
        if (value instanceof String) b.append(RemoteText.str((String) value));
        else b.append(value);
        b.append(',');
    }

    private static void close(StringBuilder b, char end) {
        if (b.charAt(b.length() - 1) == ',') b.setLength(b.length() - 1);
        b.append(end).append(',');
    }

    private synchronized String state(String token) {
        String me;
        synchronized (sessions) {
            me = sessionDevice.get(token);
        }
        StringBuilder b = new StringBuilder("{");
        field(b, "locked", gate.lockedNow());
        field(b, "calm", lock.calmMotion());
        b.append("\"devices\":[");
        for (String h : knownDevices()) {
            String given = devices.getString("name_" + h, "");
            b.append('{');
            field(b, "id", h.substring(0, DEVICE_ID));
            field(b, "name", given.isEmpty() ? devices.getString("agent_" + h, "") : given);
            field(b, "paired", devices.getLong("paired_" + h, 0));
            field(b, "seen", devices.getLong("seen_" + h, 0));
            field(b, "ip", devices.getString("ip_" + h, ""));
            field(b, "me", h.equals(me));
            close(b, '}');
        }
        close(b, ']');
        field(b, "active", profiles.active().id);
        b.append("\"pending\":[");
        for (KidsDb.Task t : db.waiting()) {
            KidsProfile p = profiles.byId(t.kid);
            b.append('{');
            field(b, "run", t.runId);
            field(b, "kid", p != null ? p.name : "?");
            field(b, "avatar", p != null ? p.avatarName() : "fuchs");
            field(b, "title", t.title);
            field(b, "minutes", t.minutes);
            field(b, "marked", t.markedAt);
            field(b, "cat", t.category);
            close(b, '}');
        }
        close(b, ']');
        String language = KidsLanguage.shown(ctx);
        b.append("\"catalog\":[");
        for (TaskCatalog.Entry e : TaskCatalog.all()) {
            b.append('{');
            field(b, "title", e.title(language));
            field(b, "cat", e.category);
            field(b, "ages", e.ages);
            field(b, "minutes", e.minutes);
            close(b, '}');
        }
        close(b, ']');
        b.append("\"children\":[");
        for (KidsProfile p : profiles.list()) child(b, p);
        close(b, ']');
        b.append("\"log\":[");
        for (KidsDb.Entry e : db.log(null, LOG_LINES)) {
            KidsProfile p = profiles.byId(e.kid);
            b.append('{');
            field(b, "time", e.time);
            field(b, "kid", p != null ? p.name : "");
            field(b, "minutes", e.minutes);
            field(b, "kind", e.kind);
            field(b, "note", e.note != null ? e.note : "");
            close(b, '}');
        }
        close(b, ']');
        close(b, '}');
        b.setLength(b.length() - 1);
        return b.toString();
    }

    private void child(StringBuilder b, KidsProfile p) {
        KidsDb.Account a = db.account(p);
        KidsProfile today = gate.rulesToday(p);
        b.append('{');
        field(b, "id", p.id);
        field(b, "name", p.name);
        field(b, "avatar", p.avatarName());
        field(b, "timeOn", p.timeOn);
        field(b, "carry", p.carryOver);
        field(b, "age", p.age);
        field(b, "group", TaskCatalog.ageGroup(p.age));
        field(b, "available", MediaTime.available(today, a.sum));
        field(b, "usedMin", a.usedMs / 60_000);
        field(b, "left", MediaTime.left(today, a.sum, a.usedMs));
        field(b, "extra", a.extra);
        b.append("\"week\":[");
        for (KidsProfile.Day d : p.week()) {
            b.append('{');
            field(b, "on", d.on);
            field(b, "base", d.base);
            field(b, "from", d.from);
            field(b, "to", d.to);
            field(b, "extra", d.extra);
            close(b, '}');
        }
        close(b, ']');
        b.append("\"days\":[");
        for (int m : db.usageDays(p.id, USAGE_DAYS)) b.append(m).append(',');
        close(b, ']');
        b.append("\"tasks\":[");
        for (KidsDb.Task t : db.tasks(p.id, true)) {
            b.append('{');
            field(b, "id", t.id);
            field(b, "title", t.title);
            field(b, "desc", t.description != null ? t.description : "");
            field(b, "minutes", t.minutes);
            field(b, "days", t.days);
            field(b, "until", t.until);
            field(b, "confirm", t.confirm);
            field(b, "status", t.status);
            field(b, "today", t.today);
            field(b, "cat", t.category);
            field(b, "ages", t.ages);
            field(b, "rep", t.repeat);
            field(b, "weekend", t.weekend);
            field(b, "active", t.active);
            field(b, "reason", t.reason != null ? t.reason : "");
            close(b, '}');
        }
        close(b, ']');
        b.append("\"apps\":[");
        Set<String> hidden = KidsLock.settingsPackages(ctx);
        for (AppEntry e : apps()) {
            if (hidden.contains(e.pkg)) continue;
            b.append('{');
            field(b, "pkg", e.pkg);
            field(b, "label", e.label);
            field(b, "state", p.favorites.contains(e.pkg) ? "fav" : p.allowed.contains(e.pkg) ? "on" : "off");
            field(b, "free", p.free.contains(e.pkg));
            close(b, '}');
        }
        close(b, ']');
        close(b, '}');
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(v, max));
    }

    /** Führt eine Aktion aus; ergibt einen Fehlertext für das Handy oder null. */
    private synchronized String act(Map<String, String> q) {
        String a = q.get("a");
        if (a == null) return ctx.getString(R.string.sc_err_action);
        switch (a) {
            case "lock":
                gate.setLockedNow("1".equals(q.get("v")));
                return null;
            case "calm":
                lock.setCalmMotion("1".equals(q.get("v")));
                return null;
            case "msg":
                String text = q.get("text");
                if (text == null || text.trim().isEmpty()) return ctx.getString(R.string.sc_err_no_text);
                listener.onMessage(RemoteText.limited(text.trim(), MAX_MESSAGE));
                return null;
            case "settings":
                listener.onOpenSettings();
                return null;
            case "pin":
                if (!PinHash.valid(q.get("v"))) return ctx.getString(R.string.sc_err_pin);
                lock.setPin(q.get("v"));
                endSessions();
                return null;
            case "devices":
                forgetDevices(ctx);
                return null;
            case "devname":
            case "devdel":
                String hash = deviceHashOf(String.valueOf(q.get("dev")));
                if (hash == null) return ctx.getString(R.string.sc_err_no_device);
                if (a.equals("devdel")) forgetDevice(hash);
                else devices.edit().putString("name_" + hash, RemoteText.limited(String.valueOf(q.get("name")).trim(), MAX_NAME)).apply();
                return null;
            case "add":
                String kidName = q.get("name");
                if (kidName == null || kidName.trim().isEmpty()) return ctx.getString(R.string.sc_err_no_name);
                profiles.add(RemoteText.limited(kidName.trim(), MAX_NAME));
                return null;
            case "confirm":
                return db.confirm(profiles, RemoteText.number(q.get("run"), 0)) < 0 ? ctx.getString(R.string.sc_err_decided) : null;
            case "reject":
                String reason = RemoteText.limited(q.containsKey("reason") ? q.get("reason") : "", MAX_MESSAGE);
                return db.reject(RemoteText.number(q.get("run"), 0), reason) ? null : ctx.getString(R.string.sc_err_decided);
            case "taskactive":
                db.setTaskActive(RemoteText.number(q.get("task"), 0), "1".equals(q.get("v")));
                return null;
            case "taskdel":
                db.deleteTask(RemoteText.number(q.get("task"), 0));
                return null;
            default:
                KidsProfile p = profiles.byId(q.get("id"));
                return p == null ? ctx.getString(R.string.sc_err_no_child) : childAct(a, p, q);
        }
    }

    private String childAct(String a, KidsProfile p, Map<String, String> q) {
        int v = RemoteText.number(q.get("v"), 0);
        switch (a) {
            case "app":
                String pkg = q.get("pkg");
                if (pkg == null || KidsLock.settingsPackages(ctx).contains(pkg)) return ctx.getString(R.string.sc_err_app_denied);
                if (!p.allowed.contains(pkg)) {
                    p.allowed.add(pkg);
                } else if (!p.favorites.contains(pkg)) {
                    p.favorites.add(pkg);
                } else {
                    p.allowed.remove(pkg);
                    p.favorites.remove(pkg);
                }
                break;
            case "free":
                String freePkg = q.get("pkg");
                if (freePkg == null) return ctx.getString(R.string.sc_err_no_app);
                if (!p.free.remove(freePkg)) p.free.add(freePkg);
                break;
            case "time":
                p.timeOn = "1".equals(q.get("on"));
                p.carryOver = "1".equals(q.get("carry"));
                if (q.containsKey("base")) {
                    // Ein Wert für die ganze Woche.
                    p.baseMin = clamp(RemoteText.number(q.get("base"), 0), MINUTES_PER_DAY);
                    p.maxExtraMin = clamp(RemoteText.number(q.get("extra"), 0), MINUTES_PER_DAY);
                    p.maxTotalMin = clamp(RemoteText.number(q.get("total"), 0), MINUTES_PER_DAY);
                    p.resetWeek();
                }
                break;
            case "week":
                int from = clamp(RemoteText.number(q.get("from"), 0), MINUTES_PER_DAY);
                int to = clamp(RemoteText.number(q.get("to"), MINUTES_PER_DAY), MINUTES_PER_DAY);
                if (to <= from) return ctx.getString(R.string.sc_err_window);
                KidsProfile.Day d = p.week()[clamp(v, 6)];
                d.on = "1".equals(q.get("on"));
                d.base = clamp(RemoteText.number(q.get("base"), 0), MINUTES_PER_DAY);
                d.extra = clamp(RemoteText.number(q.get("extra"), 0), MINUTES_PER_DAY);
                d.from = from;
                d.to = to;
                break;
            case "daycopy":
                p.copyDay(clamp(v, 6), RemoteText.number(q.get("to"), 0));
                break;
            case "kidcopy":
                KidsProfile target = profiles.byId(q.get("target"));
                if (target == null) return ctx.getString(R.string.sc_err_no_child);
                target.copyWeek(p);
                profiles.save(target);
                return null;
            case "age":
                p.age = clamp(v, 18);
                break;
            case "correct":
                String note = q.get("note");
                db.correct(p, v, RemoteText.limited(note != null ? note : ctx.getString(R.string.sc_log_correction), MAX_TITLE));
                return null;
            case "task":
                return saveTask(p, q);
            case "name":
                String name = q.get("name");
                if (name == null || name.trim().isEmpty()) return ctx.getString(R.string.sc_err_no_name);
                p.name = RemoteText.limited(name.trim(), MAX_NAME);
                break;
            case "avatar":
                p.avatar = Math.floorMod(p.avatar + 1, KidsProfile.AVATARS.length);
                break;
            case "delete":
                db.deleteTasksOf(p.id);
                profiles.remove(p);
                return null;
            default:
                return ctx.getString(R.string.sc_err_action);
        }
        profiles.save(p);
        return null;
    }

    private String saveTask(KidsProfile p, Map<String, String> q) {
        String title = q.get("title");
        if (title == null || title.trim().isEmpty()) return ctx.getString(R.string.sc_err_task_name);
        String desc = q.get("desc");
        KidsDb.Task t = new KidsDb.Task();
        t.id = RemoteText.number(q.get("task"), 0);
        t.kid = p.id;
        t.title = RemoteText.limited(title.trim(), MAX_TITLE);
        t.description = RemoteText.limited(desc != null ? desc.trim() : "", MAX_MESSAGE);
        t.minutes = clamp(RemoteText.number(q.get("minutes"), 0), MINUTES_PER_DAY);
        t.days = clamp(RemoteText.number(q.get("days"), 0), 127);
        t.repeat = q.containsKey("rep") ? clamp(RemoteText.number(q.get("rep"), 0), KidsCalendar.ON_DEMAND)
                : t.days == 0 ? KidsCalendar.ONCE : t.days == 127 ? KidsCalendar.DAILY : KidsCalendar.WEEKDAYS;
        t.category = clamp(RemoteText.number(q.get("cat"), TaskCatalog.SPECIAL), TaskCatalog.SPECIAL);
        t.ages = clamp(RemoteText.number(q.get("ages"), TaskCatalog.AGE_ALL), TaskCatalog.AGE_ALL);
        t.weekend = !"0".equals(q.get("weekend"));
        t.active = !"0".equals(q.get("active"));
        t.until = Math.max(0, RemoteText.number(q.get("until"), 0));
        t.confirm = !"0".equals(q.get("confirm"));
        db.saveTask(t);
        // Eine neue Aufgabe für mehrere Kinder wird je Kind eine eigene Aufgabe.
        String others = q.get("kids");
        if (t.id == 0 && others != null) {
            for (String id : others.split(",")) {
                KidsProfile other = profiles.byId(id);
                if (other == null || other.id.equals(p.id)) continue;
                t.kid = other.id;
                db.saveTask(t);
            }
        }
        return null;
    }

    /** Adresse für den Elternbereich, zum Beispiel http://192.168.1.50:8686; null ohne Netz. */
    public static String address() {
        try {
            for (NetworkInterface n : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!n.isUp() || n.isLoopback()) continue;
                for (InetAddress a : Collections.list(n.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        return "http://" + a.getHostAddress() + ":" + PORT;
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }
}
