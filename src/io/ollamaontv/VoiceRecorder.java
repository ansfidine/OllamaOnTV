package io.ollamaontv;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.ByteArrayOutputStream;

final class VoiceRecorder {
    interface Callback {
        void onDone(byte[] wav, int peak, int rms);
        void onError(String message);
    }

    private VoiceRecorder() {}

    static void record(int seconds, Callback callback) {
        new Thread(() -> {
            int rate = 16000;
            int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int bufferBytes = Math.max(min, rate * 2);
            short[] buffer = new short[Math.max(1, bufferBytes / 2)];
            AudioRecord recorder = null;
            try {
                recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes * 2);
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    throw new IllegalStateException("AudioRecord did not initialize");
                }
                recorder.startRecording();
                ByteArrayOutputStream pcm = new ByteArrayOutputStream();
                long end = System.currentTimeMillis() + seconds * 1000L;
                long peak = 0;
                double sum = 0;
                long count = 0;
                while (System.currentTimeMillis() < end) {
                    int n = recorder.read(buffer, 0, buffer.length);
                    if (n <= 0) continue;
                    for (int i = 0; i < n; i++) {
                        int value = buffer[i];
                        if (Math.abs(value) > peak) peak = Math.abs(value);
                        sum += (double) value * value;
                        count++;
                    }
                    for (int i = 0; i < n; i++) {
                        pcm.write(buffer[i] & 0xFF);
                        pcm.write((buffer[i] >> 8) & 0xFF);
                    }
                }
                recorder.stop();
                byte[] wav = wav(pcm.toByteArray(), rate, 1, 16);
                callback.onDone(wav, (int) peak,
                        (int) Math.round(Math.sqrt(sum / Math.max(1, count))));
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? e.toString() : e.getMessage());
            } finally {
                if (recorder != null) {
                    try {
                        recorder.release();
                    } catch (Exception ignored) {
                    }
                }
            }
        }, "voice-recorder").start();
    }

    private static byte[] wav(byte[] pcm, int rate, int channels, int bits) {
        int dataSize = pcm.length;
        int byteRate = rate * channels * bits / 8;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{'R', 'I', 'F', 'F'});
        out.writeBytes(le32(36 + dataSize));
        out.writeBytes(new byte[]{'W', 'A', 'V', 'E'});
        out.writeBytes(new byte[]{'f', 'm', 't', ' '});
        out.writeBytes(le32(16));
        out.writeBytes(le16(1));
        out.writeBytes(le16(channels));
        out.writeBytes(le32(rate));
        out.writeBytes(le32(byteRate));
        out.writeBytes(le16(channels * bits / 8));
        out.writeBytes(le16(bits));
        out.writeBytes(new byte[]{'d', 'a', 't', 'a'});
        out.writeBytes(le32(dataSize));
        out.writeBytes(pcm);
        return out.toByteArray();
    }

    private static byte[] le32(int value) {
        return new byte[]{(byte) value, (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24)};
    }

    private static byte[] le16(int value) {
        return new byte[]{(byte) value, (byte) (value >> 8)};
    }
}
