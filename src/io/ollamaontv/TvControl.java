package io.ollamaontv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TvControl {
    interface Status {
        void say(String message);
    }

    static final class AppEntry {
        final String label;
        final String packageName;

        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    private TvControl() {}

    static String execute(Context context, String name, JSONObject args, Status status) throws Exception {
        if ("launch_app".equals(name)) return launch(context, args.optString("name", ""), status);
        if ("press_key".equals(name)) return key(context, args.optString("key", ""), status);
        if ("set_volume".equals(name)) return volume(context, args.optInt("level", 20), status);
        if ("open_settings".equals(name)) return settings(context, status);
        if ("search_youtube".equals(name)) return search(context, args.optString("query", ""), status);
        return "Unknown tool: " + name;
    }

    static List<AppEntry> apps(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, AppEntry> byPackage = new LinkedHashMap<>();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Intent leanback = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (Intent intent : new Intent[]{launcher, leanback}) {
            List<ResolveInfo> found = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo info : found) {
                if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
                String pkg = info.activityInfo.packageName;
                if (byPackage.containsKey(pkg)) continue;
                CharSequence label = info.loadLabel(pm);
                byPackage.put(pkg, new AppEntry(label == null ? pkg : label.toString().trim(), pkg));
            }
        }
        List<AppEntry> out = new ArrayList<>(byPackage.values());
        Collections.sort(out, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return out;
    }

    static String appSummary(Context context) {
        StringBuilder out = new StringBuilder();
        for (AppEntry app : apps(context)) {
            if (out.length() > 0) out.append(", ");
            out.append(app.label).append(" (").append(app.packageName).append(')');
        }
        return out.toString();
    }

    static String launch(Context context, String requested, Status status) throws Exception {
        String wanted = requested == null ? "" : requested.trim();
        if (wanted.isEmpty()) return "No app name was given.";
        List<AppEntry> apps = apps(context);
        AppEntry found = null;
        for (AppEntry app : apps) {
            if (app.label.equalsIgnoreCase(wanted) || app.packageName.equalsIgnoreCase(wanted)) {
                found = app;
                break;
            }
        }
        if (found == null) {
            for (AppEntry app : apps) {
                String label = app.label.toLowerCase(Locale.ROOT);
                String pkg = app.packageName.toLowerCase(Locale.ROOT);
                String query = wanted.toLowerCase(Locale.ROOT);
                if (label.contains(query) || pkg.contains(query)) {
                    found = app;
                    break;
                }
            }
        }
        if (found == null) {
            StringBuilder names = new StringBuilder();
            for (AppEntry app : apps) {
                if (names.length() > 0) names.append(", ");
                names.append(app.label);
            }
            return "No app matched \"" + wanted + "\". Installed apps: " + names;
        }
        PackageManager pm = context.getPackageManager();
        String component = null;
        Intent launch = pm.getLaunchIntentForPackage(found.packageName);
        if (launch == null) launch = pm.getLeanbackLaunchIntentForPackage(found.packageName);
        if (launch != null && launch.getComponent() != null) {
            component = launch.getComponent().flattenToShortString();
        }
        String output;
        if (component != null) {
            output = shell(context, "am start -n " + component, status);
        } else {
            output = shell(context, "monkey -p " + found.packageName
                    + " -c android.intent.category.LEANBACK_LAUNCHER 1", status);
        }
        if (output.toLowerCase(Locale.ROOT).contains("error")) {
            return "Couldn't open " + found.label + ": " + output.trim();
        }
        return "Opened " + found.label + ".";
    }

    static String key(Context context, String name, Status status) throws Exception {
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        int code;
        switch (key) {
            case "home": code = 3; break;
            case "back": code = 4; break;
            case "up": code = 19; break;
            case "down": code = 20; break;
            case "left": code = 21; break;
            case "right": code = 22; break;
            case "ok":
            case "enter":
            case "select": code = 23; break;
            case "play_pause":
            case "play": code = 85; break;
            case "volume_up": code = 24; break;
            case "volume_down": code = 25; break;
            case "mute": code = 164; break;
            case "menu": code = 82; break;
            case "search": code = 84; break;
            default: return "Unknown key: " + name;
        }
        String output = shell(context, "input keyevent " + code, status);
        if (output.toLowerCase(Locale.ROOT).contains("error")) return "Key failed: " + output.trim();
        return "Pressed " + key + ".";
    }

    static String volume(Context context, int level, Status status) throws Exception {
        int value = Math.max(0, Math.min(100, level));
        String output = shell(context, "cmd media_session volume --stream 3 --set " + value, status);
        if (output.toLowerCase(Locale.ROOT).contains("error")) return "Volume failed: " + output.trim();
        return "Volume set to " + value + ".";
    }

    static String settings(Context context, Status status) throws Exception {
        String output = shell(context, "am start -a android.settings.SETTINGS", status);
        if (output.toLowerCase(Locale.ROOT).contains("error")) return "Settings failed: " + output.trim();
        return "Opened the TV settings.";
    }

    static String search(Context context, String query, Status status) throws Exception {
        String text = query == null ? "" : query.trim();
        if (text.isEmpty()) return "No search query was given.";
        String url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(text, "UTF-8");
        String output = shell(context, "am start -a android.intent.action.VIEW -d " + url, status);
        if (output.toLowerCase(Locale.ROOT).contains("error")) return "Search failed: " + output.trim();
        return "Opened a YouTube search for " + text + ".";
    }

    private static String shell(Context context, String command, Status status) throws Exception {
        try (Adb adb = Adb.connect(context, 60_000, new Adb.Approval() {
            @Override
            public void waiting() {
                if (status != null) status.say("Approve the USB debugging prompt on the TV");
            }
        })) {
            return adb.shell(command);
        }
    }
}
