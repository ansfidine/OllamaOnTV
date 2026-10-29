package io.ollamaontv;

import android.content.Context;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

final class ConfigIO {
    private ConfigIO() {}

    static void importIfPresent(Context context) {
        File directory = context.getExternalFilesDir(null);
        if (directory == null) return;
        File file = new File(directory, "config.json");
        if (!file.isFile()) return;
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(text);
            if (json.has("endpoint")) Prefs.setEndpoint(json.optString("endpoint"));
            if (json.has("api_key")) Prefs.setApiKey(json.optString("api_key"));
            if (json.has("model")) Prefs.setModel(json.optString("model"));
            if (json.has("speak")) Prefs.setSpeak(json.optBoolean("speak", true));
            if (json.has("system_prompt")) Prefs.setPrompt(json.optString("system_prompt"));
            if (json.has("stt_url")) Prefs.setSttUrl(json.optString("stt_url"));
            if (json.has("tts_url")) Prefs.setTtsUrl(json.optString("tts_url"));
            if (json.has("tts_voice")) Prefs.setTtsVoice(json.optString("tts_voice"));
            if (!file.delete()) AppLog.w("config: imported but file not deleted");
            AppLog.w("config: imported from " + file.getAbsolutePath());
        } catch (Exception e) {
            AppLog.w("config: import failed: " + e.getMessage());
        }
    }
}
