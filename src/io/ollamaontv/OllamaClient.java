package io.ollamaontv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class OllamaClient {
    private OllamaClient() {}

    static final class Model {
        final String name;
        final String parameters;
        final String quantization;
        final List<String> capabilities;

        Model(String name, String parameters, String quantization, List<String> capabilities) {
            this.name = name;
            this.parameters = parameters;
            this.quantization = quantization;
            this.capabilities = capabilities;
        }
    }

    static String base(String endpoint) {
        String value = endpoint == null ? "" : endpoint.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        int api = value.indexOf("/api/");
        if (api >= 0) value = value.substring(0, api + 4);
        if (value.endsWith("/v1")) value = value.substring(0, value.length() - 3);
        if (value.isEmpty()) value = "https://ollama.com/api";
        if (!value.endsWith("/api")) value = value + "/api";
        return value;
    }

    static List<Model> models(String endpoint, String key) throws Exception {
        JSONObject json = call(base(endpoint) + "/tags", "GET", key, null, 20_000, 40_000);
        List<Model> out = new ArrayList<>();
        JSONArray array = json.optJSONArray("models");
        if (array == null) return out;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            JSONObject details = item.optJSONObject("details");
            List<String> capabilities = new ArrayList<>();
            JSONArray caps = item.optJSONArray("capabilities");
            if (caps != null) {
                for (int j = 0; j < caps.length(); j++) capabilities.add(caps.optString(j));
            }
            out.add(new Model(
                    item.optString("name", item.optString("model", "")),
                    details == null ? "" : details.optString("parameter_size", ""),
                    details == null ? "" : details.optString("quantization_level", ""),
                    capabilities));
        }
        return out;
    }

    static JSONObject chat(String endpoint, String key, String model, JSONArray messages, JSONArray tools)
            throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        if (tools != null && tools.length() > 0) body.put("tools", tools);
        JSONObject json = call(base(endpoint) + "/chat", "POST", key, body, 20_000, 180_000);
        JSONObject message = json.optJSONObject("message");
        if (message == null) throw new IOException("Ollama returned no message");
        return message;
    }

    static JSONObject webSearch(String endpoint, String key, String query) throws Exception {
        JSONObject body = new JSONObject();
        body.put("query", query);
        body.put("max_results", 5);
        return call(base(endpoint) + "/web_search", "POST", key, body, 20_000, 60_000);
    }

    private static JSONObject call(String url, String method, String key, JSONObject body, int connectMs, int readMs)
            throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(connectMs);
            connection.setReadTimeout(readMs);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Connection", "close");
            if (key != null && !key.trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + key.trim());
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String text = read(stream);
            if (code < 200 || code >= 300) {
                String message = text;
                try {
                    message = new JSONObject(text).optString("error", text);
                } catch (Exception ignored) {
                }
                throw new IOException("HTTP " + code + ": " + message);
            }
            return new JSONObject(text);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192];
            for (int n; (n = reader.read(buffer)) > 0; ) out.append(buffer, 0, n);
        }
        return out.toString();
    }
}
