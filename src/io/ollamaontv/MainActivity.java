package io.ollamaontv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int MIC_REQUEST = 41;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private ScrollView scroll;
    private LinearLayout feed;
    private FrameLayout quickPanel, searchPanel, detailPanel;
    private LinearLayout searchResults;
    private EditText searchInput;
    private LinearLayout detailActions;
    private TextView detailChip, detailTitle, detailMeta, detailDescription;
    private HomeRow continueRow, modelsRow, appsRow, promptsRow, savedRow;
    private final List<OllamaClient.Model> models = new ArrayList<>();
    private final List<TvControl.AppEntry> apps = new ArrayList<>();

    private static final String[] PROMPTS = {
            "What is the weather in Shanghai today?",
            "Who developed OllamaOnTV?",
            "Explain quantum computing like I'm 10.",
            "Open Netflix",
            "Recommend a movie for tonight",
            "What can you do?"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(build());
        requestMicrophone();
        loadApps();
        loadModels();
        loadContinue();
        loadPrompts();
        loadSaved();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (models.isEmpty()) loadModels();
        if (apps.isEmpty()) loadApps();
        if (continueRow != null) loadContinue();
        loadSaved();
    }

    private View build() {
        root = new FrameLayout(this);
        root.addView(new AuroraView(this), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.addView(topBar());

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        feed = new LinearLayout(this);
        feed.setOrientation(LinearLayout.VERTICAL);
        feed.setPadding(0, 0, 0, UiKit.dp(this, 60));
        feed.addView(hero());
        continueRow = new HomeRow(this, "Continue the conversation");
        modelsRow = new HomeRow(this, "Models");
        appsRow = new HomeRow(this, "TV Apps");
        promptsRow = new HomeRow(this, "Prompt ideas");
        savedRow = new HomeRow(this, "Saved");
        feed.addView(continueRow, rowParams());
        feed.addView(modelsRow, rowParams());
        feed.addView(appsRow, rowParams());
        feed.addView(promptsRow, rowParams());
        feed.addView(savedRow, rowParams());
        AppLog.w("home feed children=" + feed.getChildCount());
        continueRow.post(() -> AppLog.w("rows: continue h=" + continueRow.getHeight()
                + " top=" + continueRow.getTop() + " models top=" + modelsRow.getTop()
                + " feed h=" + feed.getHeight() + " scroll h=" + scroll.getHeight()));
        scroll.addView(feed);
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(page, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        quickPanel = quickSettings();
        searchPanel = searchOverlay();
        detailPanel = detailsOverlay();
        root.addView(quickPanel, overlayParams());
        root.addView(searchPanel, overlayParams());
        root.addView(detailPanel, overlayParams());
        return root;
    }

    private LinearLayout.LayoutParams rowParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private FrameLayout.LayoutParams overlayParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(UiKit.dp(this, 48), UiKit.dp(this, 20), UiKit.dp(this, 48), UiKit.dp(this, 14));
        bar.setClipChildren(false);
        bar.setClipToPadding(false);

        TextView brand = UiKit.text(this, "OLLAMA", 26, UiKit.INK, true);
        brand.setLetterSpacing(0.06f);
        TextView brand2 = UiKit.text(this, "ON TV", 26, UiKit.CYAN, true);
        brand2.setLetterSpacing(0.06f);
        bar.addView(brand);
        bar.addView(brand2);

        View spacer = new View(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        bar.addView(nav("Home", this::scrollTop, true));
        bar.addView(nav("Chat", () -> openChat(null), false));
        bar.addView(nav("Models", () -> scrollTo(modelsRow), false));
        bar.addView(nav("TV", () -> scrollTo(appsRow), false));
        bar.addView(nav("Saved", () -> scrollTo(savedRow), false));

        View spacer2 = new View(this);
        bar.addView(spacer2, new LinearLayout.LayoutParams(0, 1, 1f));

        bar.addView(pill("Search", this::showSearch));
        bar.addView(pill("Voice", this::openAssist));
        bar.addView(pill("Settings", this::showQuick));
        bar.addView(avatar());
        return bar;
    }

    private TextView nav(String label, Runnable action, boolean active) {
        TextView view = UiKit.text(this, label, 15, active ? UiKit.INK : UiKit.MUTED, true);
        view.setPadding(UiKit.dp(this, 15), UiKit.dp(this, 9), UiKit.dp(this, 15), UiKit.dp(this, 9));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        navParams.leftMargin = UiKit.dp(this, 4);
        navParams.rightMargin = UiKit.dp(this, 4);
        view.setLayoutParams(navParams);
        view.setGravity(Gravity.CENTER);
        GradientDrawable normal = UiKit.box(this, 0x00000000, 12);
        GradientDrawable focused = UiKit.glass(this, 12, 0x1FFFFFFF, 0x33FFFFFF);
        view.setBackground(normal);
        view.setFocusable(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackground(hasFocus ? focused : normal);
            if (hasFocus) SoundFx.nav();
            if (UiKit.motion(this)) {
                v.animate().scaleX(hasFocus ? 1.06f : 1f).scaleY(hasFocus ? 1.06f : 1f)
                        .setDuration(160).start();
            }
        });
        view.setOnClickListener(v -> {
            SoundFx.select();
            action.run();
        });
        return view;
    }

    private TextView pill(String label, Runnable action) {
        TextView view = UiKit.text(this, label, 12, UiKit.INK, true);
        view.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 8), UiKit.dp(this, 14), UiKit.dp(this, 8));
        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pillParams.leftMargin = UiKit.dp(this, 6);
        view.setLayoutParams(pillParams);
        view.setGravity(Gravity.CENTER);
        GradientDrawable normal = UiKit.glass(this, 99, 0x14FFFFFF, 0x24FFFFFF);
        GradientDrawable focused = UiKit.glass(this, 99, 0x33FFFFFF, 0x66FFFFFF);
        view.setBackground(normal);
        view.setFocusable(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackground(hasFocus ? focused : normal);
            if (hasFocus) SoundFx.nav();
        });
        view.setOnClickListener(v -> {
            SoundFx.select();
            action.run();
        });
        return view;
    }

    private TextView avatar() {
        TextView view = UiKit.text(this, "AY", 13, 0xFFFFFFFF, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(UiKit.gradient(this, 99, UiKit.PURPLE, UiKit.BLUE));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                UiKit.dp(this, 40), UiKit.dp(this, 40));
        avatarParams.leftMargin = UiKit.dp(this, 12);
        view.setLayoutParams(avatarParams);
        view.setFocusable(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) SoundFx.nav();
            if (UiKit.motion(this)) v.animate().scaleX(hasFocus ? 1.12f : 1f).scaleY(hasFocus ? 1.12f : 1f).setDuration(150).start();
        });
        view.setOnClickListener(v -> {
            SoundFx.select();
            showAbout();
        });
        return view;
    }


    private View hero() {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(UiKit.gradient(this, 28, 0xFF3B0F7A, 0xFF0B2A6F));
        hero.setClipToOutline(true);
        hero.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), UiKit.dp(MainActivity.this, 28));
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 410));
        params.leftMargin = UiKit.dp(this, 44);
        params.rightMargin = UiKit.dp(this, 44);
        params.topMargin = UiKit.dp(this, 14);
        hero.setLayoutParams(params);

        SymbolView spark = new SymbolView(this, "sparkles");
        spark.setSymbolColor(0x22FFFFFF);
        spark.setSymbolSize(UiKit.dp(this, 280));
        FrameLayout.LayoutParams sparkParams = new FrameLayout.LayoutParams(
                UiKit.dp(this, 360), UiKit.dp(this, 360), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        sparkParams.rightMargin = UiKit.dp(this, 30);
        hero.addView(spark, sparkParams);

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xE605060A, 0xB305060A, 0x3305060A, 0x00000000}));
        hero.addView(shade, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(UiKit.dp(this, 48), UiKit.dp(this, 28), UiKit.dp(this, 48), UiKit.dp(this, 28));

        TextView chip = UiKit.text(this, greeting() + "  ·  AI  ·  VOICE  ·  TV", 13, UiKit.CYAN, true);
        chip.setLetterSpacing(0.14f);
        TextView title = UiKit.text(this, "Your television,\nwith a mind of its own.", 42, UiKit.INK, true);
        title.setLineSpacing(UiKit.dp(this, 3), 1f);
        TextView description = UiKit.text(this,
                "Talk, ask, search and control your TV — a cinematic AI console built for the big screen.",
                17, 0xD9FFFFFF, false);
        description.setMaxWidth(UiKit.dp(this, 560));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setClipChildren(false);
        actions.setClipToPadding(false);
        Button talk = UiKit.button(this, "Talk to AI", true);
        talk.setOnClickListener(v -> {
            SoundFx.select();
            openAssist();
        });
        Button chat = UiKit.button(this, "Open chat", false);
        chat.setOnClickListener(v -> {
            SoundFx.select();
            openChat(null);
        });
        Button explore = UiKit.button(this, "Explore models", false);
        explore.setOnClickListener(v -> {
            SoundFx.select();
            scrollTo(modelsRow);
        });
        actions.addView(talk);
        LinearLayout.LayoutParams chatParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 54));
        chatParams.leftMargin = UiKit.dp(this, 12);
        actions.addView(chat, chatParams);
        LinearLayout.LayoutParams exploreParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 54));
        exploreParams.leftMargin = UiKit.dp(this, 12);
        actions.addView(explore, exploreParams);

        content.addView(chip);
        content.addView(title, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 10, this));
        content.addView(description, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 12, this));
        content.addView(actions, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 22, this));
        hero.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        talk.post(talk::requestFocus);
        return hero;
    }

    private String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "GOOD MORNING";
        if (hour < 18) return "GOOD AFTERNOON";
        return "GOOD EVENING";
    }

    private void loadContinue() {
        continueRow.removeAllViews();
        List<String> recent = Assistant.recentUserMessages();
        if (recent.isEmpty()) {
            FocusCard card = new FocusCard(this, "Start a conversation", "Ask anything, by voice or text",
                    UiKit.PURPLE, 0xFF1E1B4B);
            card.onCardClick(() -> openChat(null));
            continueRow.add(card);
            return;
        }
        for (String text : recent) {
            FocusCard card = new FocusCard(this, text, "Continue", UiKit.PURPLE, 0xFF1E1B4B);
            card.onCardClick(() -> openChat(null));
            continueRow.add(card);
        }
    }

    private void loadPrompts() {
        promptsRow.removeAllViews();
        int[] colors = {0xFF0EA5E9, 0xFF7C3AED, 0xFF0891B2, 0xFFEA580C, 0xFFDB2777, 0xFF2563EB};
        for (int i = 0; i < PROMPTS.length; i++) {
            String prompt = PROMPTS[i];
            FocusCard card = new FocusCard(this, prompt, "Ask OllamaOnTV", colors[i % colors.length], 0xFF0B1026);
            card.onCardClick(() -> openChat(prompt));
            promptsRow.add(card);
        }
    }

    private void loadModels() {
        new Thread(() -> {
            try {
                List<OllamaClient.Model> list = OllamaClient.models(Prefs.endpoint(), Prefs.apiKey());
                handler.post(() -> {
                    models.clear();
                    models.addAll(list);
                    renderModels();
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                AppLog.w("home models: " + message);
                final String friendly = Net.friendly(this, Prefs.endpoint(), message);
                handler.post(() -> {
                    modelsRow.removeAllViews();
                    FocusCard error = new FocusCard(this, "Couldn't load models",
                            Net.local(Prefs.endpoint()) ? "Start the relay / tap to retry" : "Check connection / tap to retry",
                            0xFF9A3412, 0xFF0B1026);
                    error.onCardClick(this::loadModels);
                    modelsRow.add(error);
                });
            }
        }, "home-models").start();
    }

    private void renderModels() {
        modelsRow.removeAllViews();
        String current = Prefs.model();
        int[] colors = {0xFF4C1D95, 0xFF1D4ED8, 0xFF0E7490, 0xFF9A3412, 0xFF831843, 0xFF1E40AF};
        for (int i = 0; i < models.size() && i < 10; i++) {
            OllamaClient.Model model = models.get(i);
            String meta = model.parameters.isEmpty() ? caps(model) : model.parameters + "  ·  " + caps(model);
            FocusCard card = new FocusCard(this, model.name, meta, colors[i % colors.length], 0xFF0B1026);
            if (model.name.equals(current)) {
                card.titleView().setText("[active] " + model.name);
            }
            card.onCardClick(() -> showModelDetails(model));
            modelsRow.add(card);
        }
    }

    private static String caps(OllamaClient.Model model) {
        if (model.capabilities.isEmpty()) return "model";
        StringBuilder out = new StringBuilder();
        for (String cap : model.capabilities) {
            if (out.length() > 0) out.append(" · ");
            out.append(cap);
        }
        return out.toString();
    }

    private void loadApps() {
        new Thread(() -> {
            List<TvControl.AppEntry> list = TvControl.apps(this);
            handler.post(() -> {
                apps.clear();
                apps.addAll(list);
                renderApps();
            });
        }, "home-apps").start();
    }

    private void renderApps() {
        appsRow.removeAllViews();
        PackageManager manager = getPackageManager();
        int[] colors = {0xFF1D4ED8, 0xFF7C3AED, 0xFF0F766E, 0xFF9A3412, 0xFFBE123C};
        for (int i = 0; i < apps.size() && i < 12; i++) {
            TvControl.AppEntry app = apps.get(i);
            Drawable icon = null;
            try {
                icon = manager.getApplicationIcon(app.packageName);
            } catch (Exception ignored) {
            }
            FocusCard card = new FocusCard(this, app.label, "Open app", colors[i % colors.length], 0xFF0B1026);
            if (icon != null) card.icon(icon);
            card.onCardClick(() -> launchApp(app));
            appsRow.add(card);
        }
    }

    private void launchApp(TvControl.AppEntry app) {
        new Thread(() -> {
            try {
                JSONObject args = new JSONObject();
                args.put("name", app.label);
                TvControl.execute(this, "launch_app", args, s -> {
                });
                handler.post(() -> toast("Opening " + app.label));
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                handler.post(() -> toast(message));
            }
        }, "launch-app").start();
    }

    private void loadSaved() {
        savedRow.removeAllViews();
        List<JSONObject> items = savedItems();
        if (items.isEmpty()) {
            FocusCard card = new FocusCard(this, "Nothing saved yet", "Use Save on a model, app or prompt", UiKit.CYAN, 0xFF0B3B45);
            card.onCardClick(() -> openChat("What can you do?"));
            savedRow.add(card);
            return;
        }
        String[] colors = {"0xFF0E7490", "0xFF4C1D95"};
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.get(i);
            String type = item.optString("type", "item");
            String title = item.optString("title", "");
            String value = item.optString("value", "");
            FocusCard card = new FocusCard(this, title, type.toUpperCase(Locale.ROOT) + "  ·  saved",
                    i % 2 == 0 ? 0xFF0E7490 : 0xFF4C1D95, 0xFF0B1026);
            card.onCardClick(() -> openSaved(type, title, value));
            savedRow.add(card);
        }
    }

    private void openSaved(String type, String title, String value) {
        if ("prompt".equals(type)) {
            showDetails("SAVED PROMPT", title, "Saved prompt", value, "Ask this", () -> openChat(value));
        } else if ("app".equals(type)) {
            TvControl.AppEntry found = null;
            for (TvControl.AppEntry app : apps) {
                if (app.label.equals(title) || app.packageName.equals(value)) {
                    found = app;
                    break;
                }
            }
            final TvControl.AppEntry app = found;
            showDetails("SAVED APP", title, value, "Open this app from your collection.", "Open app",
                    app == null ? () -> toast("App not found") : () -> launchApp(app));
        } else {
            showDetails("SAVED MODEL", title, value, "Use this model for your next conversation.", "Use model",
                    () -> selectModel(title));
        }
    }

    private void showModelDetails(OllamaClient.Model model) {
        String meta = (model.parameters.isEmpty() ? "" : model.parameters + "  ·  ") + caps(model);
        showDetails("MODEL", model.name, meta,
                "Ask anything with this model. It can use web search and control your TV.",
                "Use this model", () -> selectModel(model.name));
    }

    private void selectModel(String name) {
        Prefs.setModel(name);
        SoundFx.select();
        closeOverlay(detailPanel);
        renderModels();
        toast("Model: " + name);
    }

    private void openChat(String prompt) {
        Intent intent = new Intent(this, ChatActivity.class);
        if (prompt != null) intent.putExtra("prompt", prompt);
        startActivity(intent);
    }

    private void openAssist() {
        startActivity(new Intent(this, AssistActivity.class));
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void scrollTop() {
        scroll.smoothScrollTo(0, 0);
    }

    private void scrollTo(View row) {
        scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, row.getTop() - UiKit.dp(this, 30))));
    }

    private FrameLayout quickSettings() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x99000000);
        overlay.setVisibility(View.GONE);
        overlay.setOnClickListener(v -> closeOverlay(overlay));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(UiKit.dp(this, 30), UiKit.dp(this, 34), UiKit.dp(this, 30), UiKit.dp(this, 30));
        panel.setBackground(UiKit.glass(this, 0, 0xF20B1026, 0x33FFFFFF));
        panel.setOnClickListener(v -> {
        });
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                UiKit.dp(this, 420), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
        overlay.addView(panel, panelParams);

        TextView title = UiKit.text(this, "Quick settings", 28, UiKit.INK, true);
        panel.addView(title);
        panel.addView(UiKit.text(this, "Your console at a glance", 14, UiKit.MUTED, false),
                UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 4, this));

        panel.addView(section("Model"));
        panel.addView(UiKit.text(this, Prefs.model().isEmpty() ? "No model selected" : Prefs.model(),
                18, UiKit.CYAN, false));
        Button changeModel = UiKit.button(this, "Change model", false);
        changeModel.setOnClickListener(v -> {
            closeOverlay(overlay);
            openSettings();
        });
        panel.addView(changeModel, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 48), 8, this));

        panel.addView(section("Voice"));
        panel.addView(UiKit.text(this, Prefs.ttsVoice(), 18, UiKit.CYAN, false));
        Button changeVoice = UiKit.button(this, "Change voice", false);
        changeVoice.setOnClickListener(v -> {
            closeOverlay(overlay);
            openSettings();
        });
        panel.addView(changeVoice, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 48), 8, this));

        CheckBox sounds = new CheckBox(this);
        sounds.setText("Navigation sounds");
        sounds.setTextColor(UiKit.INK);
        sounds.setTextSize(16);
        sounds.setChecked(Prefs.sounds());
        sounds.setOnCheckedChangeListener((v, checked) -> Prefs.setSounds(checked));
        panel.addView(sounds, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 16, this));

        panel.addView(section("Connections"));
        panel.addView(UiKit.text(this, "STT  " + (Prefs.sttUrl().isEmpty() ? "TV" : "Vosk · 11437"),
                13, UiKit.MUTED, false));
        panel.addView(UiKit.text(this, "TTS  " + (Prefs.ttsUrl().isEmpty() ? "TV voice" : "Neural · 11438"),
                13, UiKit.MUTED, false), UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 2, this));

        View spacer = new View(this);
        panel.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));
        Button full = UiKit.button(this, "Open full settings", true);
        full.setOnClickListener(v -> {
            SoundFx.select();
            closeOverlay(overlay);
            openSettings();
        });
        panel.addView(full, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 52), 16, this));
        Button close = UiKit.button(this, "Close", false);
        close.setOnClickListener(v -> {
            SoundFx.back();
            closeOverlay(overlay);
        });
        panel.addView(close, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 52), 8, this));
        return overlay;
    }

    private TextView section(String text) {
        TextView view = UiKit.text(this, text.toUpperCase(Locale.ROOT), 12, UiKit.CYAN, true);
        view.setLetterSpacing(0.16f);
        view.setPadding(0, UiKit.dp(this, 24), 0, UiKit.dp(this, 4));
        return view;
    }

    private FrameLayout searchOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xE605060A);
        overlay.setVisibility(View.GONE);
        overlay.setOnClickListener(v -> closeOverlay(overlay));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(UiKit.dp(this, 88), UiKit.dp(this, 50), UiKit.dp(this, 88), UiKit.dp(this, 40));
        overlay.addView(panel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView title = UiKit.text(this, "Search", 40, UiKit.INK, true);
        TextView subtitle = UiKit.text(this, "Models, apps and prompts — all in one place.", 16, UiKit.MUTED, false);
        panel.addView(title);
        panel.addView(subtitle, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 4, this));

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Search your TV universe…");
        searchInput.setTextSize(20);
        searchInput.setTextColor(UiKit.INK);
        searchInput.setHintTextColor(UiKit.FAINT);
        searchInput.setBackground(UiKit.glass(this, 16, 0x1FFFFFFF, 0x33FFFFFF));
        searchInput.setPadding(UiKit.dp(this, 20), UiKit.dp(this, 10), UiKit.dp(this, 20), UiKit.dp(this, 10));
        searchInput.setFocusable(true);
        panel.addView(searchInput, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 64), 20, this));

        searchResults = new LinearLayout(this);
        searchResults.setOrientation(LinearLayout.VERTICAL);
        panel.addView(searchResults, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 20, this));

        Button close = UiKit.button(this, "Close search", false);
        close.setOnClickListener(v -> closeOverlay(overlay));
        panel.addView(close, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 48), 20, this));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                renderSearch(s.toString());
            }
        });
        return overlay;
    }

    private void renderSearch(String query) {
        searchResults.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            TextView hint = UiKit.text(this, "Try “Netflix”, “deepseek”, “weather” or “who developed”.",
                    15, UiKit.MUTED, false);
            searchResults.addView(hint);
            return;
        }
        int shown = 0;
        for (OllamaClient.Model model : models) {
            if (!model.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            addSearchRow("MODEL", model.name, caps(model), () -> {
                closeOverlay(searchPanel);
                showModelDetails(model);
            });
            if (++shown >= 8) return;
        }
        for (TvControl.AppEntry app : apps) {
            if (!app.label.toLowerCase(Locale.ROOT).contains(q)
                    && !app.packageName.toLowerCase(Locale.ROOT).contains(q)) continue;
            addSearchRow("APP", app.label, "Open app", () -> {
                closeOverlay(searchPanel);
                launchApp(app);
            });
            if (++shown >= 12) return;
        }
        for (String prompt : PROMPTS) {
            if (!prompt.toLowerCase(Locale.ROOT).contains(q)) continue;
            addSearchRow("PROMPT", prompt, "Ask this", () -> {
                closeOverlay(searchPanel);
                openChat(prompt);
            });
            if (++shown >= 16) return;
        }
        if (shown == 0) {
            TextView empty = UiKit.text(this, "No results. Try another search.", 16, UiKit.MUTED, false);
            searchResults.addView(empty);
        }
    }

    private void addSearchRow(String type, String title, String meta, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(this, 16), UiKit.dp(this, 12), UiKit.dp(this, 16), UiKit.dp(this, 12));
        row.setBackground(UiKit.glass(this, 14, 0x12FFFFFF, 0x22FFFFFF));
        row.setFocusable(true);
        row.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackground(hasFocus ? UiKit.glass(this, 14, 0x2BFFFFFF, 0x55FFFFFF)
                    : UiKit.glass(this, 14, 0x12FFFFFF, 0x22FFFFFF));
            if (hasFocus) SoundFx.nav();
        });
        row.setOnClickListener(v -> {
            SoundFx.select();
            action.run();
        });
        TextView badge = UiKit.text(this, type, 10, UiKit.CYAN, true);
        badge.setLetterSpacing(0.12f);
        badge.setPadding(0, 0, UiKit.dp(this, 16), 0);
        TextView main = UiKit.text(this, title, 16, UiKit.INK, true);
        TextView sub = UiKit.text(this, meta, 12, UiKit.MUTED, false);
        row.addView(badge);
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(main);
        text.addView(sub);
        row.addView(text);
        searchResults.addView(row, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 8, this));
    }

    private FrameLayout detailsOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xCC05060A);
        overlay.setVisibility(View.GONE);
        overlay.setOnClickListener(v -> closeOverlay(overlay));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(UiKit.dp(this, 42), UiKit.dp(this, 34), UiKit.dp(this, 42), UiKit.dp(this, 34));
        panel.setBackground(UiKit.glass(this, 26, 0xF20B1026, 0x33FFFFFF));
        panel.setOnClickListener(v -> {
        });
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                UiKit.dp(this, 760), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        overlay.addView(panel, panelParams);

        detailChip = UiKit.text(this, "", 11, UiKit.CYAN, true);
        detailChip.setLetterSpacing(0.16f);
        detailTitle = UiKit.text(this, "", 34, UiKit.INK, true);
        detailMeta = UiKit.text(this, "", 14, UiKit.MUTED, false);
        detailDescription = UiKit.text(this, "", 16, 0xD9FFFFFF, false);
        detailDescription.setLineSpacing(UiKit.dp(this, 3), 1f);
        detailActions = new LinearLayout(this);
        detailActions.setOrientation(LinearLayout.HORIZONTAL);

        panel.addView(detailChip);
        panel.addView(detailTitle, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 8, this));
        panel.addView(detailMeta, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 4, this));
        panel.addView(detailDescription, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 16, this));
        panel.addView(detailActions, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 22, this));
        return overlay;
    }

    private void showDetails(String chip, String title, String meta, String description,
                             String actionLabel, Runnable action) {
        detailChip.setText(chip);
        detailTitle.setText(title);
        detailMeta.setText(meta);
        detailDescription.setText(description);
        detailActions.removeAllViews();
        Button primary = UiKit.button(this, actionLabel, true);
        primary.setOnClickListener(v -> {
            SoundFx.select();
            action.run();
        });
        Button save = UiKit.button(this, "Save", false);
        save.setOnClickListener(v -> {
            SoundFx.select();
            saveItem(chip.toLowerCase(Locale.ROOT), title, title,
                    meta == null ? "" : meta);
            toast("Saved to your collection");
        });
        Button close = UiKit.button(this, "Close", false);
        close.setOnClickListener(v -> {
            SoundFx.back();
            closeOverlay(detailPanel);
        });
        detailActions.addView(primary);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 54));
        saveParams.leftMargin = UiKit.dp(this, 10);
        detailActions.addView(save, saveParams);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 54));
        closeParams.leftMargin = UiKit.dp(this, 10);
        detailActions.addView(close, closeParams);
        showOverlay(detailPanel);
        primary.requestFocus();
    }

    private List<JSONObject> savedItems() {
        List<JSONObject> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(Prefs.saved());
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) out.add(item);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void saveItem(String type, String title, String value, String meta) {
        List<JSONObject> items = savedItems();
        for (JSONObject item : items) {
            if (title.equals(item.optString("title"))) return;
        }
        try {
            JSONObject item = new JSONObject();
            item.put("type", type);
            item.put("title", title);
            item.put("value", value);
            item.put("meta", meta);
            items.add(0, item);
            JSONArray array = new JSONArray();
            for (int i = 0; i < items.size() && i < 20; i++) array.put(items.get(i));
            Prefs.setSaved(array.toString());
            loadSaved();
        } catch (Exception ignored) {
        }
    }

    private void showAbout() {
        showDetails("DEVELOPER", "Ansfidine Youssouf",
                "OllamaOnTV · a fun project for his TV",
                "OllamaOnTV was developed by Ansfidine Youssouf as a fun project for his TV. "
                        + "The AI models are served by Ollama Cloud; the app, voice console and design are his.",
                "Open settings", this::openSettings);
    }

    private void showSearch() {
        showOverlay(searchPanel);
        renderSearch("");
        searchInput.requestFocus();
    }

    private void showQuick() {
        showOverlay(quickPanel);
        quickPanel.requestFocus();
    }

    private void showOverlay(View overlay) {
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(UiKit.motion(this) ? 180 : 0).start();
    }

    private void closeOverlay(View overlay) {
        overlay.animate().alpha(0f).setDuration(UiKit.motion(this) ? 140 : 0).withEndAction(() -> {
            overlay.setVisibility(View.GONE);
            overlay.setAlpha(1f);
        }).start();
    }

    private boolean overlayShowing(View overlay) {
        return overlay != null && overlay.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onBackPressed() {
        if (overlayShowing(detailPanel)) {
            closeOverlay(detailPanel);
            SoundFx.back();
        } else if (overlayShowing(searchPanel)) {
            closeOverlay(searchPanel);
            SoundFx.back();
        } else if (overlayShowing(quickPanel)) {
            closeOverlay(quickPanel);
            SoundFx.back();
        } else {
            super.onBackPressed();
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message == null ? "" : message, Toast.LENGTH_SHORT).show();
    }

    private void requestMicrophone() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
