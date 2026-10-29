package io.ollamaontv;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChatActivity extends Activity {
    private static final int MIC_REQUEST = 41;

    private TextView status;
    private TextView conversation;
    private TextView hint;
    private EditText input;
    private ScrollView scroll;
    private Button talk;
    private boolean busy;

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
        String ask = getIntent().getStringExtra("ask");
        if (ask == null) ask = getIntent().getStringExtra("prompt");
        if (ask != null && !ask.trim().isEmpty()) {
            final String prompt = ask;
            input.setText(prompt);
            input.postDelayed(this::send, 600);
        }
        if (conversation.getText().length() == 0) {
            conversation.setText("Welcome to OllamaOnTV.\n\n"
                    + "1. Open Settings and add your Ollama Cloud API key.\n"
                    + "2. Pick a model.\n"
                    + "3. Press Talk, or the Google Assistant button on the remote, and speak.\n\n"
                    + "You can also type below. Replies appear here and can be spoken aloud.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChrome();
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(UiKit.dp(this, 56), UiKit.dp(this, 32), UiKit.dp(this, 56), UiKit.dp(this, 28));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{UiKit.BG, UiKit.BG2}));

        TextView title = UiKit.text(this, "OllamaOnTV", 36, 0xFFF8FAFC, true);
        root.addView(title);

        TextView subtitle = UiKit.text(this, "by Ansfidine Youssouf  ·  AI chat", 16, UiKit.MUTED, false);
        root.addView(subtitle, UiKit.params(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 6, this));

        status = UiKit.text(this, "", 16, 0xFF7DD3FC, false);
        root.addView(status, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 10, this));

        scroll = new ScrollView(this);
        scroll.setBackground(UiKit.glass(this, 18, 0x14FFFFFF, 0x24FFFFFF));
        scroll.setPadding(UiKit.dp(this, 22), UiKit.dp(this, 18), UiKit.dp(this, 22), UiKit.dp(this, 18));
        scroll.setFocusable(false);
        conversation = UiKit.text(this, "", 20, 0xFFE2E8F0, false);
        conversation.setLineSpacing(UiKit.dp(this, 5), 1f);
        conversation.setFocusable(false);
        scroll.addView(conversation);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = UiKit.dp(this, 16);
        root.addView(scroll, scrollParams);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this);
        input.setHint("Type a message for your TV assistant");
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setTextColor(0xFFF8FAFC);
        input.setHintTextColor(0xFF64748B);
        input.setPadding(UiKit.dp(this, 18), 0, UiKit.dp(this, 18), 0);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        UiKit.focusable(input, this, UiKit.GLASS_STRONG, 0x33FFFFFF);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f);
        inputParams.rightMargin = UiKit.dp(this, 12);
        inputRow.addView(input, inputParams);
        Button send = UiKit.button(this, "Send", true);
        send.setOnClickListener(v -> send());
        inputRow.addView(send, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this, 56)));
        root.addView(inputRow, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 14, this));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        talk = UiKit.button(this, "Talk", true);
        talk.setOnClickListener(v -> startActivity(new Intent(this, AssistActivity.class)));
        buttons.addView(talk, new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f));
        Button settings = UiKit.button(this, "Settings", false);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f);
        settingsParams.leftMargin = UiKit.dp(this, 12);
        buttons.addView(settings, settingsParams);
        Button clear = UiKit.button(this, "Clear", false);
        clear.setOnClickListener(v -> {
            Assistant.reset();
            conversation.setText("");
            refreshChrome();
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f);
        clearParams.leftMargin = UiKit.dp(this, 12);
        buttons.addView(clear, clearParams);
        root.addView(buttons, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 12, this));

        hint = UiKit.text(this, "", 14, 0xFF64748B, false);
        root.addView(hint, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 12, this));

        talk.requestFocus();
        return root;
    }

    private void refreshChrome() {
        String model = Prefs.model();
        String endpoint = Prefs.endpoint();
        status.setText((model.isEmpty() ? "No model selected" : model) + "  ·  " + OllamaClient.base(endpoint));
        boolean listener = KeyService.isEnabled(this);
        boolean assistant = false;
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            assistant = roles != null && roles.isRoleHeld("android.app.role.ASSISTANT");
        }
        hint.setText(listener || assistant
                ? "Press the Google Assistant button on the remote to talk."
                : "Enable the button listener in Settings to use the Google Assistant button. Talk also works.");
    }

    private void send() {
        if (busy) return;
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        if (Prefs.model().isEmpty()) {
            append("System", "Choose a model in Settings first.");
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        input.setText("");
        append("You", text);
        busy = true;
        talk.setEnabled(false);
        status.setText("Thinking...");
        Assistant.ask(this, text, new Assistant.Callback() {
            @Override
            public void onStatus(String message) {
                status.setText(message);
            }

            @Override
            public void onReply(String reply) {
                append("AI", reply);
                if (Prefs.speak()) Speaker.speak(reply);
                busy = false;
                talk.setEnabled(true);
                refreshChrome();
            }

            @Override
            public void onError(String message) {
                append("Error", message);
                busy = false;
                talk.setEnabled(true);
                refreshChrome();
            }
        });
    }

    private void append(String who, String text) {
        if (conversation.getText().length() > 0) conversation.append("\n\n");
        conversation.append(who + "\n" + text);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void requestMicrophone() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            status.setText("Microphone ready.");
        }
    }
}
