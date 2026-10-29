package io.ollamaontv;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class Speaker {
    private static Context app;
    private static TextToSpeech tts;
    private static boolean ready;
    private static MediaPlayer player;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Speaker() {}

    static void init(Context context) {
        app = context.getApplicationContext();
        if (tts != null) return;
        tts = new TextToSpeech(app, status -> {
            ready = status == TextToSpeech.SUCCESS;
            if (ready) {
                try {
                    tts.setLanguage(Locale.getDefault());
                } catch (Exception ignored) {
                }
            }
        });
    }

    static void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        stop();
        String url = Prefs.ttsUrl();
        if (url != null && !url.isEmpty() && app != null) {
            speakRemote(text, url);
        } else {
            speakAndroid(text);
        }
    }

    private static void speakRemote(String text, String url) {
        IO.execute(() -> {
            try {
                byte[] audio = TtsClient.synthesize(url, text, Prefs.ttsVoice());
                File file = File.createTempFile("tts", ".mp3", app.getCacheDir());
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(audio);
                }
                MAIN.post(() -> play(file, text));
            } catch (Exception e) {
                AppLog.w("tts server: " + e.getMessage());
                MAIN.post(() -> speakAndroid(text));
            }
        });
    }

    private static void play(File file, String text) {
        stopPlayer();
        try {
            MediaPlayer media = new MediaPlayer();
            media.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            media.setDataSource(file.getAbsolutePath());
            media.setOnCompletionListener(p -> {
                p.release();
                if (player == p) player = null;
                file.delete();
            });
            media.prepare();
            media.start();
            player = media;
        } catch (Exception e) {
            AppLog.w("tta play: " + e.getMessage());
            file.delete();
            speakAndroid(text);
        }
    }

    private static void speakAndroid(String text) {
        if (tts == null || !ready) return;
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ollamaontv");
        } catch (Exception e) {
            AppLog.w("tts: " + e.getMessage());
        }
    }

    private static void stopPlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    static void stop() {
        stopPlayer();
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) {
            }
        }
    }
}
