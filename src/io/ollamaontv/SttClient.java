package io.ollamaontv;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class SttClient {
    private SttClient() {}

    static String transcribe(String url, byte[] wav) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(wav.length);
            connection.setRequestProperty("Content-Type", "audio/wav");
            connection.setRequestProperty("Connection", "close");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(wav);
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String body = read(stream);
            if (code < 200 || code >= 300) throw new IOException("STT HTTP " + code + ": " + body);
            return new JSONObject(body).optString("text", "").trim();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            for (int n; (n = reader.read(buffer)) > 0; ) out.append(buffer, 0, n);
        }
        return out.toString();
    }
}
