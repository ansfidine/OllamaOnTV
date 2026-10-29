package io.ollamaontv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AssistActivity extends Activity {
    private static final int MIC_REQUEST = 42;

    private final Speech speech = new Speech();
    private TextView status;
    private TextView you;
    private TextView reply;
    private ScrollView scroll;
    private Button again;
    private boolean pendingListen;
    private boolean testOnly;
    private String sttUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        testOnly = getIntent().getBooleanExtra("test_only", false);
        sttUrl = Prefs.sttUrl();
        setContentView(build());
        if (!testOnly && Prefs.model().isEmpty()) {
            status.setText("No model selected");
            reply.setText("Open Settings, add your Ollama API key and choose a model.");
            again.setText("Open settings");
            again.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
            });
            return;
        }
        if (!testOnly && Prefs.endpoint().toLowerCase().contains("ollama.com") && Prefs.apiKey().isEmpty()) {
            status.setText("API key missing");
            reply.setText("Add your Ollama Cloud API key in Settings.");
            again.setText("Open settings");
            again.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
            });
            return;
        }
        if ((sttUrl == null || sttUrl.isEmpty()) && !speech.available(this)) {
            status.setText("Speech recognition unavailable");
            reply.setText("This TV did not expose a recognition service. You can still type in the main screen.");
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingListen = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
        } else {
            status.postDelayed(this::startListening, 400);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        startListening();
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(UiKit.dp(this, 72), UiKit.dp(this, 44), UiKit.dp(this, 72), UiKit.dp(this, 36));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{UiKit.BG, UiKit.BG2}));

        TextView brand = UiKit.text(this, "OllamaOnTV", 20, 0xFF7DD3FC, true);
        root.addView(brand);

        status = UiKit.text(this, "Listening...", 34, 0xFFF8FAFC, true);
        status.setGravity(Gravity.CENTER);
        root.addView(status, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 14, this));

        you = UiKit.text(this, "", 20, 0xFF94A3B8, false);
        you.setGravity(Gravity.CENTER);
        root.addView(you, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 8, this));

        scroll = new ScrollView(this);
        scroll.setBackground(UiKit.glass(this, 24, 0x18FFFFFF, 0x2EFFFFFF));
        scroll.setPadding(UiKit.dp(this, 26), UiKit.dp(this, 20), UiKit.dp(this, 26), UiKit.dp(this, 20));
        scroll.setFocusable(false);
        reply = UiKit.text(this, "", 26, 0xFFF8FAFC, false);
        reply.setLineSpacing(UiKit.dp(this, 5), 1f);
        reply.setFocusable(false);
        scroll.addView(reply);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = UiKit.dp(this, 18);
        root.addView(scroll, scrollParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        again = UiKit.button(this, "Talk again", true);
        again.setOnClickListener(v -> startListening());
        buttons.addView(again, new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f));
        Button chat = UiKit.button(this, "Open chat", false);
        chat.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        LinearLayout.LayoutParams chatParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f);
        chatParams.leftMargin = UiKit.dp(this, 12);
        buttons.addView(chat, chatParams);
        Button close = UiKit.button(this, "Close", false);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f);
        closeParams.leftMargin = UiKit.dp(this, 12);
        buttons.addView(close, closeParams);
        root.addView(buttons, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 16, this));

        TextView hint = UiKit.text(this,
                "Press the Google Assistant button again, or use Back to close.", 14, 0xFF64748B, false);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 10, this));

        again.requestFocus();
        return root;
    }

    private void startListening() {
        if (!testOnly && Prefs.model().isEmpty()) return;
        speech.stop();
        Speaker.stop();
        status.setText("Listening...");
        you.setText("");
        reply.setText("");
        again.setEnabled(false);
        if (!testOnly && sttUrl != null && !sttUrl.isEmpty()) {
            startStt();
        } else {
            startAndroidSpeech();
        }
    }

    private void startAndroidSpeech() {
        speech.start(this, new Speech.Listener() {
            @Override
            public void onPartial(String text) {
                you.setText(text);
            }

            @Override
            public void onResult(String text) {
                you.setText(text);
                if (testOnly) {
                    status.setText("Microphone works");
                    reply.setText("I heard: " + text);
                    if (Prefs.speak()) Speaker.speak("I heard " + text);
                    again.setEnabled(true);
                    again.requestFocus();
                } else {
                    ask(text);
                }
            }

            @Override
            public void onError(String message) {
                status.setText(message);
                again.setEnabled(true);
                again.requestFocus();
            }
        });
    }

    private void startStt() {
        status.setText("Listening... speak now");
        VoiceRecorder.record(6, new VoiceRecorder.Callback() {
            @Override
            public void onDone(byte[] wav, int peak, int rms) {
                AppLog.w("voice: peak=" + peak + " rms=" + rms + " bytes=" + wav.length);
                if (peak < 120) {
                    runOnUiThread(() -> {
                        status.setText("I didn't catch that");
                        reply.setText("The microphone captured almost no audio (peak " + peak
                                + "). Try again, closer or louder.");
                        again.setEnabled(true);
                        again.requestFocus();
                    });
                    return;
                }
                runOnUiThread(() -> status.setText("Transcribing..."));
                new Thread(() -> {
                    try {
                        String text = SttClient.transcribe(sttUrl, wav);
                        runOnUiThread(() -> {
                            if (text.isEmpty()) {
                                status.setText("I didn't catch that");
                                reply.setText("Try again, a little slower.");
                                again.setEnabled(true);
                                again.requestFocus();
                            } else {
                                you.setText(text);
                                ask(text);
                            }
                        });
                    } catch (Exception e) {
                        String message = e.getMessage() == null ? e.toString() : e.getMessage();
                        AppLog.w("stt: " + message);
                        runOnUiThread(() -> {
                            status.setText("STT error");
                            reply.setText(message);
                            again.setEnabled(true);
                            again.requestFocus();
                        });
                    }
                }, "stt").start();
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    status.setText("Microphone error");
                    reply.setText(message);
                    again.setEnabled(true);
                    again.requestFocus();
                });
            }
        });
    }

    private void ask(String text) {
        status.setText("Thinking...");
        speech.stop();
        Assistant.ask(this, text, new Assistant.Callback() {
            @Override
            public void onStatus(String message) {
                status.setText(message);
            }

            @Override
            public void onReply(String response) {
                status.setText("Ready");
                reply.setText(response);
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
                if (Prefs.speak()) Speaker.speak(response);
                again.setEnabled(true);
                again.setText("Talk again");
                again.requestFocus();
            }

            @Override
            public void onError(String message) {
                status.setText("Something went wrong");
                reply.setText(message);
                again.setEnabled(true);
                again.requestFocus();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingListen) startListening();
            } else {
                status.setText("Microphone permission denied");
                reply.setText("Allow the microphone in the TV's app settings, then press Talk again.");
            }
            pendingListen = false;
        }
    }

    @Override
    protected void onDestroy() {
        speech.stop();
        super.onDestroy();
    }
}
