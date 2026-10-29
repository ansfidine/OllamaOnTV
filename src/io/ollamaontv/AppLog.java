package io.ollamaontv;

import android.util.Log;

final class AppLog {
    private AppLog() {}

    static void w(String message) {
        Log.w("OllamaOnTV", message == null ? "" : message);
    }
}
