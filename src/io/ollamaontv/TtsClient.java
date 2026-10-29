package io.ollamaontv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class TtsClient {
    private TtsClient() {}

    static final class Voice {
        final String name;
        final String gender;
        final String locale;

        Voice(String name, String gender, String locale) {
            this.name = name;
            this.gender = gender;
            this.locale = locale;
        }

        String label() {
            return name + "  ·  " + gender + "  ·  " + locale;
        }
    }

    static List<Voice> voices(String ttsUrl) throws Exception {
        String url = ttsUrl == null ? "" : ttsUrl.trim();
        int at = url.lastIndexOf("/tts");
        if (at >= 0) url = url.substring(0, at) + "/voices";
        else if (!url.endsWith("/voices")) url = url + "/voices";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Connection", "close");
            int code = connection.getResponseCode();
            InputStream in = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            StringBuilder text = new StringBuilder();
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    char[] buffer = new char[8192];
                    for (int n; (n = reader.read(buffer)) > 0; ) text.append(buffer, 0, n);
                }
            }
            if (code < 200 || code >= 300) throw new IOException("Voices HTTP " + code + ": " + text);
            JSONArray array = new JSONObject(text.toString()).optJSONArray("voices");
            List<Voice> out = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item == null) continue;
                    String locale = item.optString("Locale", "");
                    if (!(locale.startsWith("en-US") || locale.startsWith("en-GB") || locale.startsWith("zh-CN"))) continue;
                    out.add(new Voice(item.optString("ShortName", ""), item.optString("Gender", ""), locale));
                }
            }
            Collections.sort(out, new Comparator<Voice>() {
                @Override
                public int compare(Voice a, Voice b) {
                    return a.label().compareToIgnoreCase(b.label());
                }
            });
            return out;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    static byte[] synthesize(String url, String text, String voice) throws Exception {
        JSONObject body = new JSONObject();
        body.put("text", text);
        if (voice != null && !voice.trim().isEmpty()) body.put("voice", voice.trim());
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(60000);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Connection", "close");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }
            int code = connection.getResponseCode();
            InputStream in = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (in != null) {
                byte[] buffer = new byte[8192];
                for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
            }
            if (code < 200 || code >= 300) {
                throw new IOException("TTS HTTP " + code + ": " + out.toString("UTF-8"));
            }
            return out.toByteArray();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
