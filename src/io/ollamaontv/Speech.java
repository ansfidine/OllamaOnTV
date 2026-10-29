package io.ollamaontv;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

final class Speech {
    interface Listener {
        void onPartial(String text);
        void onResult(String text);
        void onError(String message);
    }

    private SpeechRecognizer recognizer;
    private Listener listener;

    boolean available(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    void start(Context context, Listener target) {
        stop();
        listener = target;
        if (!available(context)) {
            target.onError("Speech recognition is unavailable on this TV.");
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int eventType, Bundle params) {}

            @Override
            public void onError(int error) {
                AppLog.w("speech: error " + error);
                if (listener != null) listener.onError(message(error));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> texts = results == null ? null
                        : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (texts != null && !texts.isEmpty()) {
                    AppLog.w("speech: " + texts.get(0));
                    if (listener != null) listener.onResult(texts.get(0));
                } else if (listener != null) {
                    listener.onError("I didn't catch that.");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> texts = partialResults == null ? null
                        : partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (texts != null && !texts.isEmpty() && listener != null) listener.onPartial(texts.get(0));
            }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        try {
            recognizer.startListening(intent);
        } catch (Exception e) {
            AppLog.w("speech: start failed: " + e.getMessage());
            target.onError("Speech recognition could not start: " + e.getMessage());
        }
    }

    void stop() {
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception ignored) {
            }
            recognizer = null;
        }
        listener = null;
    }

    private static String message(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Microphone error.";
            case SpeechRecognizer.ERROR_CLIENT: return "Speech client error.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Microphone permission is missing.";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Speech recognition needs a network.";
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "I didn't catch that.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Speech recognizer is busy.";
            case SpeechRecognizer.ERROR_SERVER: return "Speech server error.";
            default: return "Speech error (" + error + ").";
        }
    }
}
