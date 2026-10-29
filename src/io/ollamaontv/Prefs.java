package io.ollamaontv;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private static Context app;

    private Prefs() {}

    static void init(Context context) {
        app = context.getApplicationContext();
    }

    private static SharedPreferences sp() {
        return app.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    static String endpoint() {
        String value = sp().getString("endpoint", "https://ollama.com/api");
        return value == null ? "https://ollama.com/api" : value.trim();
    }

    static void setEndpoint(String value) {
        sp().edit().putString("endpoint", value == null ? "" : value.trim()).apply();
    }

    static String apiKey() {
        String value = sp().getString("apiKey", "");
        return value == null ? "" : value.trim();
    }

    static void setApiKey(String value) {
        sp().edit().putString("apiKey", value == null ? "" : value.trim()).apply();
    }

    static String model() {
        String value = sp().getString("model", "");
        return value == null ? "" : value.trim();
    }

    static void setModel(String value) {
        sp().edit().putString("model", value == null ? "" : value.trim()).apply();
    }

    static boolean speak() {
        return sp().getBoolean("speak", true);
    }

    static void setSpeak(boolean value) {
        sp().edit().putBoolean("speak", value).apply();
    }

    static String sttUrl() {
        String value = sp().getString("sttUrl", "");
        return value == null ? "" : value.trim();
    }

    static void setSttUrl(String value) {
        sp().edit().putString("sttUrl", value == null ? "" : value.trim()).apply();
    }

    static boolean sounds() {
        return sp().getBoolean("sounds", true);
    }

    static void setSounds(boolean value) {
        sp().edit().putBoolean("sounds", value).apply();
    }

    static String saved() {
        String value = sp().getString("saved", "[]");
        return value == null ? "[]" : value;
    }

    static void setSaved(String value) {
        sp().edit().putString("saved", value == null ? "[]" : value).apply();
    }

    static String ttsUrl() {
        String value = sp().getString("ttsUrl", "");
        return value == null ? "" : value.trim();
    }

    static void setTtsUrl(String value) {
        sp().edit().putString("ttsUrl", value == null ? "" : value.trim()).apply();
    }

    static String ttsVoice() {
        String value = sp().getString("ttsVoice", "en-US-AriaNeural");
        return value == null ? "" : value.trim();
    }

    static void setTtsVoice(String value) {
        sp().edit().putString("ttsVoice", value == null ? "" : value.trim()).apply();
    }

    static String prompt() {
        String value = sp().getString("prompt", "");
        return value == null ? "" : value.trim();
    }

    static void setPrompt(String value) {
        sp().edit().putString("prompt", value == null ? "" : value.trim()).apply();
    }
}
