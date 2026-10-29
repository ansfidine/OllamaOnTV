package io.ollamaontv;

import android.media.AudioManager;
import android.media.ToneGenerator;

final class SoundFx {
    private static ToneGenerator tone;

    private SoundFx() {}

    private static ToneGenerator tone() {
        if (tone == null) {
            try {
                tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 24);
            } catch (Exception ignored) {
            }
        }
        return tone;
    }

    static void nav() {
        if (!Prefs.sounds()) return;
        try {
            tone().startTone(ToneGenerator.TONE_PROP_BEEP, 45);
        } catch (Exception ignored) {
        }
    }

    static void select() {
        if (!Prefs.sounds()) return;
        try {
            tone().startTone(ToneGenerator.TONE_PROP_ACK, 70);
        } catch (Exception ignored) {
        }
    }

    static void back() {
        if (!Prefs.sounds()) return;
        try {
            tone().startTone(ToneGenerator.TONE_PROP_NACK, 60);
        } catch (Exception ignored) {
        }
    }

    static void shutdown() {
        if (tone != null) {
            try {
                tone.release();
            } catch (Exception ignored) {
            }
            tone = null;
        }
    }
}
