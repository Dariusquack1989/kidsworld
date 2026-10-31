package io.github.dariusquack.kidsworld;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.gathin.tvhome.R;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Kids World-Startbildschirm (HOME). Lädt die Apps und reicht sie an das Theme. Welches Theme, steht beim Bauen
 * fest: Jeder Ordner unter Themes/ liefert seine {@link SelectedTheme}, und je Theme entsteht eine eigene App.
 */
public class HomeActivity extends Activity {
    /** Nur in Debug-Builds: am start ... --ei sc_hour 7 zeigt das Theme zu dieser Uhrzeit. */
    static final String EXTRA_HOUR = "sc_hour";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private HomeTheme theme;
    private int loadGeneration;
    private int debugHour = -1;

    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            loadApps();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        takeExtras(getIntent());
        theme = SelectedTheme.create();
        setContentView(theme.create(this));
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packages, f);
        loadApps();
    }

    /** Nur in Debug-Builds: übernimmt die vorgetäuschte Uhrzeit (sc_hour). */
    private void takeExtras(Intent intent) {
        if (intent == null) return;
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            debugHour = intent.getIntExtra(EXTRA_HOUR, debugHour);
        }
    }

    /** Aktuelle Stunde 0–23 (in Debug-Builds per Extra überschreibbar). */
    public int hourOfDay() {
        return debugHour >= 0 ? debugHour : Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    private void loadApps() {
        int gen = ++loadGeneration;
        worker.execute(() -> {
            List<AppEntry> apps = AppRepository.load(getApplicationContext());
            main.post(() -> {
                if (gen == loadGeneration && !isDestroyed()) theme.showApps(apps);
            });
        });
    }

    /** Startet eine App und meldet, wenn das nicht geht. */
    public void launch(AppEntry app) {
        if (!AppRepository.launch(this, app)) {
            Toast.makeText(this, getString(R.string.sc_launch_failed, app.label), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        theme.onResume();
    }

    @Override
    protected void onPause() {
        theme.onPause();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        takeExtras(intent);
        if (intent != null && intent.hasCategory(Intent.CATEGORY_HOME)) theme.onHomePressed();
    }

    @Override
    public void onBackPressed() {
        // Home ist die unterste Ebene: Zurück schließt nichts, das Theme entscheidet, was es bedeutet.
        theme.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packages);
        worker.shutdownNow();
        super.onDestroy();
    }
}
