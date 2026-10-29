package io.ollamaontv;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

public class App extends Application {
    private static App app;

    static App get() { return app; }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        Prefs.init(this);
        Speaker.init(this);
        ConfigIO.importIfPresent(this);
        try {
            Intent service = new Intent(this, AssistantService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
        } catch (Exception e) {
            AppLog.w("service: " + e.getMessage());
        }
    }
}
