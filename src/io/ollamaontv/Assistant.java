package io.ollamaontv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class Assistant {
    interface Callback {
        void onStatus(String status);
        void onReply(String reply);
        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final List<JSONObject> history = new ArrayList<>();

    private Assistant() {}

    static void reset() {
        history.clear();
    }

    static List<String> recentUserMessages() {
        List<String> out = new ArrayList<>();
        synchronized (history) {
            for (int i = history.size() - 1; i >= 0 && out.size() < 6; i--) {
                JSONObject item = history.get(i);
                if ("user".equals(item.optString("role"))) {
                    String text = item.optString("content", "").trim();
                    if (!text.isEmpty()) out.add(text);
                }
            }
        }
        return out;
    }

    static void ask(Context context, String userText, Callback callback) {
        IO.execute(() -> {
            try {
                String endpoint = Prefs.endpoint();
                String key = Prefs.apiKey();
                String model = Prefs.model();
                if (model.isEmpty()) {
                    throw new Exception("Choose a model in Settings.");
                }
                if (endpoint.toLowerCase(Locale.ROOT).contains("ollama.com") && key.isEmpty()) {
                    throw new Exception("Add your Ollama Cloud API key in Settings.");
                }
                JSONArray tools = tools();
                JSONArray messages = new JSONArray();
                messages.put(message("system", systemPrompt(context)));
                synchronized (history) {
                    for (JSONObject item : history) messages.put(item);
                }
                messages.put(message("user", userText));

                String reply = null;
                List<String> actions = new ArrayList<>();
                boolean useTools = true;
                for (int turn = 0; turn < 4; turn++) {
                    final String statusText = turn == 0 ? "Thinking..." : "Working...";
                    MAIN.post(() -> callback.onStatus(statusText));
                    JSONObject answer;
                    try {
                        answer = OllamaClient.chat(endpoint, key, model, messages, useTools ? tools : null);
                    } catch (Exception e) {
                        if (useTools && String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT).contains("tool")) {
                            useTools = false;
                            messages = new JSONArray();
                            messages.put(message("system", systemPrompt(context)));
                            synchronized (history) {
                                for (JSONObject item : history) messages.put(item);
                            }
                            messages.put(message("user", userText));
                            answer = OllamaClient.chat(endpoint, key, model, messages, null);
                        } else {
                            throw e;
                        }
                    }
                    String content = answer.optString("content", "");
                    JSONArray calls = answer.optJSONArray("tool_calls");
                    if (calls != null && calls.length() > 0) {
                        JSONObject assistant = new JSONObject();
                        assistant.put("role", "assistant");
                        assistant.put("content", content);
                        assistant.put("tool_calls", calls);
                        messages.put(assistant);
                        for (int i = 0; i < calls.length(); i++) {
                            JSONObject call = calls.optJSONObject(i);
                            if (call == null) continue;
                            JSONObject function = call.optJSONObject("function");
                            if (function == null) continue;
                            String name = function.optString("name", "");
                            JSONObject args = arguments(function.opt("arguments"));
                            String result;
                            if ("web_search".equals(name)) {
                                result = webSearch(endpoint, key, args.optString("query", ""));
                            } else {
                                result = TvControl.execute(context, name, args,
                                        status -> MAIN.post(() -> callback.onStatus(status)));
                            }
                            actions.add(result);
                            JSONObject tool = new JSONObject();
                            tool.put("role", "tool");
                            tool.put("tool_name", name);
                            tool.put("content", result);
                            messages.put(tool);
                        }
                        continue;
                    }
                    reply = content;
                    break;
                }
                if (reply == null || reply.trim().isEmpty()) {
                    reply = actions.isEmpty() ? "Done." : join(actions);
                }
                JSONObject userMessage = message("user", userText);
                JSONObject assistantMessage = message("assistant", reply);
                synchronized (history) {
                    history.add(userMessage);
                    history.add(assistantMessage);
                    while (history.size() > 16) history.remove(0);
                }
                final String out = reply;
                MAIN.post(() -> callback.onReply(out));
            } catch (Exception e) {
                String error = e.getMessage() == null ? e.toString() : e.getMessage();
                AppLog.w("assistant: " + error);
                final String friendly = Net.friendly(context, Prefs.endpoint(), error);
                MAIN.post(() -> callback.onError(friendly));
            }
        });
    }

    private static JSONObject arguments(Object value) {
        if (value instanceof JSONObject) return (JSONObject) value;
        try {
            return new JSONObject(String.valueOf(value));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static String join(List<String> actions) {
        StringBuilder out = new StringBuilder();
        for (String action : actions) {
            if (out.length() > 0) out.append(' ');
            out.append(action);
        }
        return out.toString();
    }

    private static JSONObject message(String role, String content) {
        JSONObject object = new JSONObject();
        try {
            object.put("role", role);
            object.put("content", content == null ? "" : content);
        } catch (Exception ignored) {
        }
        return object;
    }

    private static String systemPrompt(Context context) {
        String apps = TvControl.appSummary(context);
        if (apps.length() > 1800) apps = apps.substring(0, 1800) + "...";
        String time = new SimpleDateFormat("EEEE, MMMM d, yyyy, HH:mm", Locale.getDefault()).format(new Date());
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are OllamaOnTV, a full general-purpose AI assistant running on an Anker Nebula Android TV projector. ")
                .append("OllamaOnTV is an app developed by Ansfidine Youssouf as a fun project for his TV. ")
                .append("If the user asks who developed, made or created OllamaOnTV, or who the developer is, always answer that OllamaOnTV was developed by Ansfidine Youssouf as a fun project for his TV. ")
                .append("The AI models are served by Ollama Cloud; the app around them is his. ")
                .append("Answer questions on any topic using your knowledge; be helpful, accurate and complete. ")
                .append("Never refuse a general question and never say \"no data\" if you can answer it. ")
                .append("Use web_search for current events, live data such as news, weather, prices or sports, or facts you are unsure about. ")
                .append("Cite the source briefly when you use web results. ")
                .append("Use launch_app, press_key, set_volume, open_settings and search_youtube only when the user asks to control the TV. ")
                .append("Answers are spoken aloud: keep them clear and concise (usually 1 to 4 sentences), but give the full answer needed. ")
                .append("Reply in the same language as the user. Never claim a TV action succeeded unless a tool result confirms it. ")
                .append("Installed apps: ").append(apps.isEmpty() ? "(none found)" : apps).append(". ")
                .append("Current time: ").append(time).append('.');
        String extra = Prefs.prompt();
        if (!extra.isEmpty()) prompt.append(' ').append(extra);
        return prompt.toString();
    }

    private static JSONArray tools() throws Exception {
        JSONArray tools = new JSONArray();

        JSONObject launch = tool("launch_app",
                "Open an installed app on this TV. The name may be the app label or its package name.");
        JSONObject launchProps = new JSONObject();
        launchProps.put("name", string("The app name, for example Netflix, YouTube, Prime Video"));
        launch.put("parameters", schema(launchProps, new String[]{"name"}));
        tools.put(launch);

        JSONObject key = tool("press_key", "Press a remote-control key on the TV.");
        JSONObject keyProps = new JSONObject();
        keyProps.put("key", new JSONObject().put("type", "string").put("description",
                "home, back, up, down, left, right, ok, play_pause, volume_up, volume_down, mute, menu, search"));
        key.put("parameters", schema(keyProps, new String[]{"key"}));
        tools.put(key);

        JSONObject volume = tool("set_volume", "Set the TV media volume to a level from 0 to 100.");
        JSONObject volumeProps = new JSONObject();
        volumeProps.put("level", new JSONObject().put("type", "integer").put("description", "Volume level from 0 to 100"));
        volume.put("parameters", schema(volumeProps, new String[]{"level"}));
        tools.put(volume);

        JSONObject settings = tool("open_settings", "Open the TV's own settings app.");
        settings.put("parameters", schema(new JSONObject(), new String[]{}));
        tools.put(settings);

        JSONObject web = tool("web_search",
                "Search the web for current or live information: news, weather, prices, sports, or facts you are unsure about.");
        JSONObject webProps = new JSONObject();
        webProps.put("query", string("What to search the web for"));
        web.put("parameters", schema(webProps, new String[]{"query"}));
        tools.put(web);

        JSONObject search = tool("search_youtube", "Open a YouTube search for a query.");
        JSONObject searchProps = new JSONObject();
        searchProps.put("query", string("What to search for"));
        search.put("parameters", schema(searchProps, new String[]{"query"}));
        tools.put(search);

        return tools;
    }

    private static String webSearch(String endpoint, String key, String query) throws Exception {
        JSONObject json = OllamaClient.webSearch(endpoint, key, query);
        JSONArray results = json.optJSONArray("results");
        if (results == null || results.length() == 0) return "No web results.";
        StringBuilder out = new StringBuilder();
        int count = Math.min(5, results.length());
        for (int i = 0; i < count; i++) {
            JSONObject result = results.optJSONObject(i);
            if (result == null) continue;
            String title = result.optString("title", "");
            String url = result.optString("url", "");
            String content = result.optString("content", "");
            if (content.length() > 700) content = content.substring(0, 700) + "...";
            out.append(i + 1).append(". ").append(title).append('\n')
                    .append(url).append('\n').append(content).append("\n\n");
        }
        return out.length() == 0 ? "No web results." : out.toString();
    }

    private static JSONObject tool(String name, String description) throws Exception {
        JSONObject function = new JSONObject();
        function.put("name", name);
        function.put("description", description);
        JSONObject wrapper = new JSONObject();
        wrapper.put("type", "function");
        wrapper.put("function", function);
        return wrapper;
    }

    private static JSONObject string(String description) throws Exception {
        return new JSONObject().put("type", "string").put("description", description);
    }

    private static JSONObject schema(JSONObject properties, String[] required) throws Exception {
        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        JSONArray array = new JSONArray();
        for (String name : required) array.put(name);
        parameters.put("required", array);
        return parameters;
    }
}
