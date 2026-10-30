package io.ollamaontv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private EditText endpointField;
    private EditText keyField;
    private EditText promptField;
    private CheckBox speakBox;
    private TextView modelsStatus;
    private TextView selectedModel;
    private Spinner modelSpinner;
    private ArrayAdapter<String> modelAdapter;
    private boolean bindingModels;
    private TextView listenerStatus;
    private TextView controlStatus;
    private EditText sttField;
    private EditText ttsField;
    private EditText ttsVoiceField;
    private LinearLayout voicesContainer;
    private TextView voicesStatus;
    private List<TtsClient.Voice> loadedVoices = new ArrayList<>();

    private Button loadButton;
    private List<OllamaClient.Model> loadedModels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(build());
        loadModels();
        if (getIntent().getBooleanExtra("test_tv", false)) {
            endpointField.post(this::testTvControl);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateListenerStatus();
    }

    @Override
    protected void onPause() {
        save(false);
        super.onPause();
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{UiKit.BG, UiKit.BG2}));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(UiKit.dp(this, 64), UiKit.dp(this, 36), UiKit.dp(this, 64), UiKit.dp(this, 44));
        scroll.addView(root);

        root.addView(UiKit.text(this, "Settings", 36, 0xFFF8FAFC, true));
        root.addView(UiKit.text(this, "Ollama Cloud or a local Ollama server", 16, 0xFF94A3B8, false),
                UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 6, this));

        root.addView(section("Ollama"));
        root.addView(label("Endpoint"));
        endpointField = field(Prefs.endpoint(), "https://ollama.com/api", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI, 1);
        root.addView(endpointField, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 52), 4, this));

        root.addView(label("API key"));
        keyField = field(Prefs.apiKey(), "Ollama Cloud API key (ollama.com/settings/keys)", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD, 1);
        root.addView(keyField, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 52), 4, this));

        LinearLayout loadRow = new LinearLayout(this);
        loadRow.setOrientation(LinearLayout.HORIZONTAL);
        loadRow.setGravity(Gravity.CENTER_VERTICAL);
        loadButton = UiKit.button(this, "Load models", false);
        loadButton.setOnClickListener(v -> loadModels());
        loadRow.addView(loadButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 52)));
        modelsStatus = UiKit.text(this, "", 15, 0xFF94A3B8, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        statusParams.leftMargin = UiKit.dp(this, 16);
        loadRow.addView(modelsStatus, statusParams);
        root.addView(loadRow, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 18, this));

        Button testConnection = UiKit.button(this, "Test connection", false);
        testConnection.setOnClickListener(v -> testConnection());
        root.addView(testConnection, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 48), 8, this));

        selectedModel = UiKit.text(this, selectedText(), 16, 0xFF7DD3FC, false);
        root.addView(selectedModel, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 16, this));

        modelSpinner = new Spinner(this);
        UiKit.focusable(modelSpinner, this, 0xFF1E293B, 0xFF334155);
        modelSpinner.setMinimumHeight(UiKit.dp(this, 56));
        modelSpinner.setPadding(UiKit.dp(this, 14), 0, UiKit.dp(this, 14), 0);
        modelAdapter = new ArrayAdapter<String>(this, 0, new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                return spinnerRow(getItem(position), false);
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                return spinnerRow(getItem(position), true);
            }
        };
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bindingModels || position < 0 || position >= loadedModels.size()) return;
                Prefs.setModel(loadedModels.get(position).name);
                selectedModel.setText("Selected model: " + loadedModels.get(position).name);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        root.addView(modelSpinner, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 56), 8, this));

        root.addView(UiKit.text(this,
                "Tip: from a computer, push config.json to Android/data/io.ollamaontv/files/ with endpoint, api_key, model, stt_url, tts_url and tts_voice.",
                13, 0xFF64748B, false), UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 8, this));

        root.addView(section("Voice"));
        speakBox = new CheckBox(this);
        speakBox.setText("Speak replies aloud (text is always shown)");
        speakBox.setTextSize(17);
        speakBox.setTextColor(0xFFE2E8F0);
        speakBox.setChecked(Prefs.speak());
        speakBox.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF7DD3FC));
        root.addView(speakBox, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 8, this));
        root.addView(label("STT server URL (optional)"));
        sttField = field(Prefs.sttUrl(), "http://127.0.0.1:11437/stt (Vosk) - empty = Android speech",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, 1);
        root.addView(sttField, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 52), 4, this));
        root.addView(label("TTS server URL (optional)"));
        ttsField = field(Prefs.ttsUrl(), "http://127.0.0.1:11438/tts (neural) - empty = TV voice",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, 1);
        root.addView(ttsField, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 52), 4, this));
        root.addView(label("TTS voice"));
        ttsVoiceField = field(Prefs.ttsVoice(), "en-US-AriaNeural, en-US-GuyNeural, zh-CN-XiaoxiaoNeural",
                InputType.TYPE_CLASS_TEXT, 1);
        root.addView(ttsVoiceField, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 52), 4, this));
        LinearLayout voiceActions = new LinearLayout(this);
        voiceActions.setOrientation(LinearLayout.HORIZONTAL);
        voiceActions.setGravity(Gravity.CENTER_VERTICAL);
        Button loadVoices = UiKit.button(this, "Load voices", false);
        loadVoices.setOnClickListener(v -> loadVoices());
        voiceActions.addView(loadVoices, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 52)));
        Button previewVoice = UiKit.button(this, "Preview voice", true);
        previewVoice.setOnClickListener(v -> previewVoice());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 52));
        previewParams.leftMargin = UiKit.dp(this, 12);
        voiceActions.addView(previewVoice, previewParams);
        root.addView(voiceActions, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 8, this));
        voicesStatus = UiKit.text(this, "Aria is selected. Load voices to see the Edge TTS list.",
                13, 0xFF64748B, false);
        root.addView(voicesStatus, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 6, this));
        voicesContainer = new LinearLayout(this);
        voicesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(voicesContainer, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 4, this));
        Button test = UiKit.button(this, "Test voice", false);
        test.setOnClickListener(v -> startActivity(new Intent(this, AssistActivity.class)));
        root.addView(test, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 52), 10, this));
        Button micTest = UiKit.button(this, "Test microphone", false);
        micTest.setOnClickListener(v -> {
            Intent intent = new Intent(this, AssistActivity.class);
            intent.putExtra("test_only", true);
            startActivity(intent);
        });
        root.addView(micTest, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 52), 8, this));
        Button recorder = UiKit.button(this, "Test recorder (5s)", false);
        recorder.setOnClickListener(v -> startActivity(new Intent(this, RecordTestActivity.class)));
        root.addView(recorder, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 52), 8, this));

        root.addView(section("Remote button"));
        listenerStatus = UiKit.text(this, "", 16, 0xFF94A3B8, false);
        root.addView(listenerStatus, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 4, this));
        Button accessibility = UiKit.button(this, "Open accessibility settings", false);
        accessibility.setOnClickListener(v -> openAccessibility());
        root.addView(accessibility, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 52), 8, this));
        root.addView(UiKit.text(this,
                "Enable the service named OllamaOnTV button listener. It catches only the Assistant or voice key on the remote.",
                13, 0xFF64748B, false), UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 6, this));

        root.addView(section("TV control"));
        Button tvTest = UiKit.button(this, "Test TV control (Home)", false);
        tvTest.setOnClickListener(v -> testTvControl());
        root.addView(tvTest, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 52), 8, this));
        controlStatus = UiKit.text(this,
                "The first time, approve the USB debugging prompt on the TV with Always allow.",
                13, 0xFF64748B, false);
        root.addView(controlStatus, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 6, this));

        root.addView(section("About"));
        root.addView(UiKit.text(this,
                "OllamaOnTV was developed by Ansfidine Youssouf as a fun project for his TV.",
                14, 0xFF94A3B8, false), UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 4, this));

        root.addView(section("Extra instructions"));
        promptField = field(Prefs.prompt(), "Optional: tell the assistant how to behave", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 4);
        promptField.setGravity(Gravity.TOP | Gravity.START);
        root.addView(promptField, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this, 120), 4, this));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button save = UiKit.button(this, "Save", true);
        save.setOnClickListener(v -> save(true));
        actions.addView(save, new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f));
        Button close = UiKit.button(this, "Close", false);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f);
        closeParams.leftMargin = UiKit.dp(this, 12);
        actions.addView(close, closeParams);
        root.addView(actions, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 18, this));

        loadButton.requestFocus();
        return scroll;
    }

    private TextView section(String text) {
        TextView view = UiKit.text(this, text, 20, 0xFF7DD3FC, true);
        view.setPadding(0, UiKit.dp(this, 22), 0, UiKit.dp(this, 4));
        return view;
    }

    private TextView label(String text) {
        TextView view = UiKit.text(this, text, 14, 0xFF94A3B8, false);
        view.setPadding(0, UiKit.dp(this, 8), 0, 0);
        return view;
    }

    private EditText field(String value, String hint, int inputType, int lines) {
        EditText view = new EditText(this);
        view.setText(value);
        view.setHint(hint);
        view.setTextSize(16);
        view.setTextColor(0xFFF8FAFC);
        view.setHintTextColor(0xFF64748B);
        view.setInputType(inputType);
        view.setSingleLine(lines == 1);
        if (lines > 1) view.setMinLines(lines);
        view.setPadding(UiKit.dp(this, 14), UiKit.dp(this, 8), UiKit.dp(this, 14), UiKit.dp(this, 8));
        view.setGravity(lines == 1 ? Gravity.CENTER_VERTICAL : Gravity.TOP | Gravity.START);
        UiKit.focusable(view, this, 0xFF1E293B, 0xFF334155);
        return view;
    }

    private String selectedText() {
        String model = Prefs.model();
        return model.isEmpty() ? "No model selected" : "Selected model: " + model;
    }

    private void testConnection() {
        final String url = endpointField.getText().toString();
        final String key = keyField.getText().toString();
        modelsStatus.setText("Testing connection...");
        new Thread(() -> {
            try {
                List<OllamaClient.Model> list = OllamaClient.models(url, key);
                runOnUiThread(() -> modelsStatus.setText("Online — " + list.size() + " models"));
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                final String friendly = Net.friendly(this, url, message);
                runOnUiThread(() -> modelsStatus.setText(friendly));
            }
        }, "test-connection").start();
    }

    private void loadVoices() {
        String url = ttsField.getText().toString().trim();
        if (url.isEmpty()) {
            voicesStatus.setText("Set a TTS server URL first, then load voices.");
            return;
        }
        voicesStatus.setText("Loading voices...");
        new Thread(() -> {
            try {
                List<TtsClient.Voice> voices = TtsClient.voices(url);
                runOnUiThread(() -> {
                    loadedVoices = voices;
                    renderVoices();
                    voicesStatus.setText(voices.size() + " voices. Tap one to select it.");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                AppLog.w("voices: " + message);
                runOnUiThread(() -> voicesStatus.setText("Error: " + message));
            }
        }, "voices").start();
    }

    private void renderVoices() {
        voicesContainer.removeAllViews();
        String selected = Prefs.ttsVoice();
        for (TtsClient.Voice voice : loadedVoices) {
            boolean current = voice.name.equals(selected);
            Button button = new Button(this);
            button.setText((current ? "[selected] " : "") + voice.label());
            button.setAllCaps(false);
            button.setTextSize(14);
            button.setTextColor(current ? 0xFFBAE6FD : 0xFFE2E8F0);
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setPadding(UiKit.dp(this, 14), 0, UiKit.dp(this, 14), 0);
            button.setMinHeight(UiKit.dp(this, 44));
            UiKit.focusable(button, this, current ? 0xFF0C4A6E : 0xFF1E293B, 0xFF334155);
            button.setOnClickListener(v -> {
                ttsVoiceField.setText(voice.name);
                Prefs.setTtsVoice(voice.name);
                renderVoices();
                voicesStatus.setText("Selected " + voice.name + ". Use Preview voice to hear it.");
            });
            voicesContainer.addView(button, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 4, this));
        }
    }

    private void previewVoice() {
        save(false);
        Speaker.speak("Hello. This is the " + Prefs.ttsVoice() + " voice. How do I sound?");
        voicesStatus.setText("Previewing " + Prefs.ttsVoice());
    }

    private void loadModels() {
        loadButton.setEnabled(false);
        modelsStatus.setText("Loading models...");
        final String endpoint = endpointField.getText().toString();
        final String key = keyField.getText().toString();
        new Thread(() -> {
            try {
                List<OllamaClient.Model> models = OllamaClient.models(endpoint, key);
                runOnUiThread(() -> {
                    loadedModels = models;
                    renderModels();
                    modelsStatus.setText(models.size() + " models");
                    loadButton.setEnabled(true);
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                AppLog.w("models: " + message);
                final String friendly = Net.friendly(SettingsActivity.this, endpoint, message);
                runOnUiThread(() -> {
                    modelsStatus.setText(friendly);
                    loadButton.setEnabled(true);
                });
            }
        }, "models").start();
    }

    private View spinnerRow(String label, boolean dropdown) {
        TextView view = UiKit.text(this, label, 15, 0xFFE2E8F0, false);
        view.setSingleLine(true);
        view.setMaxLines(1);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setPadding(UiKit.dp(this, 16), UiKit.dp(this, dropdown ? 10 : 6),
                UiKit.dp(this, 16), UiKit.dp(this, dropdown ? 10 : 6));
        if (dropdown) view.setBackgroundColor(0xFF1E293B);
        return view;
    }

    private void renderModels() {
        selectedModel.setText(selectedText());
        modelAdapter.clear();
        for (OllamaClient.Model model : loadedModels) {
            StringBuilder label = new StringBuilder(model.name);
            if (!model.parameters.isEmpty()) label.append("  ·  ").append(model.parameters);
            if (!model.capabilities.isEmpty()) {
                label.append("  ·  ");
                for (int i = 0; i < model.capabilities.size(); i++) {
                    if (i > 0) label.append(", ");
                    label.append(model.capabilities.get(i));
                }
            }
            modelAdapter.add(label.toString());
        }
        modelAdapter.notifyDataSetChanged();
        String selected = Prefs.model();
        for (int i = 0; i < loadedModels.size(); i++) {
            if (loadedModels.get(i).name.equals(selected)) {
                bindingModels = true;
                modelSpinner.setSelection(i);
                bindingModels = false;
                break;
            }
        }
    }

    private void updateListenerStatus() {
        boolean enabled = KeyService.isEnabled(this);
        listenerStatus.setText(enabled ? "Button listener: On" : "Button listener: Off");
        listenerStatus.setTextColor(enabled ? 0xFF4ADE80 : 0xFFF87171);
    }

    private void testTvControl() {
        controlStatus.setText("Connecting to the TV's ADB...");
        new Thread(() -> {
            try {
                JSONObject args = new JSONObject().put("key", "home");
                String result = TvControl.execute(this, "press_key", args, message ->
                        runOnUiThread(() -> controlStatus.setText(message)));
                runOnUiThread(() -> controlStatus.setText(result));
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                runOnUiThread(() -> controlStatus.setText("Error: " + message));
            }
        }, "tv-test").start();
    }

    private void openAccessibility() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            listenerStatus.setText("Could not open accessibility settings: " + e.getMessage());
        }
    }

    private void save(boolean notify) {
        if (endpointField == null) return;
        String endpoint = endpointField.getText().toString().trim();
        if (endpoint.isEmpty()) endpoint = "https://ollama.com/api";
        Prefs.setEndpoint(endpoint);
        Prefs.setApiKey(keyField.getText().toString());
        Prefs.setPrompt(promptField.getText().toString());
        Prefs.setSpeak(speakBox.isChecked());
        Prefs.setSttUrl(sttField.getText().toString());
        Prefs.setTtsUrl(ttsField.getText().toString());
        Prefs.setTtsVoice(ttsVoiceField.getText().toString());
        if (notify) {
            selectedModel.setText("Saved. " + selectedText());
            selectedModel.setTextColor(0xFF4ADE80);
        }
    }
}
