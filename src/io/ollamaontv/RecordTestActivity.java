package io.ollamaontv;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class RecordTestActivity extends Activity {
    private static final int MIC_REQUEST = 43;
    private static final int SECONDS = 5;

    private TextView status;
    private TextView result;
    private Button start;
    private Button play;
    private File file;
    private boolean recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(build());
        file = new File(getExternalFilesDir(null), "mic.wav");
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
        }
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(UiKit.dp(this, 72), UiKit.dp(this, 48), UiKit.dp(this, 72), UiKit.dp(this, 40));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0B1220, 0xFF10233A}));

        root.addView(UiKit.text(this, "Microphone test", 32, 0xFFF8FAFC, true));
        status = UiKit.text(this, "Press Start, then speak for 5 seconds.", 20, 0xFF94A3B8, false);
        status.setGravity(Gravity.CENTER);
        root.addView(status, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 14, this));

        result = UiKit.text(this, "", 18, 0xFF7DD3FC, false);
        result.setGravity(Gravity.CENTER);
        root.addView(result, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 18, this));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        start = UiKit.button(this, "Start recording", true);
        start.setOnClickListener(v -> record());
        buttons.addView(start, new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f));
        play = UiKit.button(this, "Play", false);
        play.setEnabled(false);
        play.setOnClickListener(v -> play());
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f);
        playParams.leftMargin = UiKit.dp(this, 12);
        buttons.addView(play, playParams);
        Button close = UiKit.button(this, "Close", false);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(0, UiKit.dp(this, 56), 1f);
        closeParams.leftMargin = UiKit.dp(this, 12);
        buttons.addView(close, closeParams);
        root.addView(buttons, UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 22, this));

        root.addView(UiKit.text(this,
                "A peak near 0 means Android is not giving the app microphone audio. "
                        + "The WAV is saved in the app's files folder for diagnosis.",
                13, 0xFF64748B, false), UiKit.params(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 16, this));

        start.requestFocus();
        return root;
    }

    private void record() {
        if (recording) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
            return;
        }
        recording = true;
        start.setEnabled(false);
        play.setEnabled(false);
        result.setText("");
        status.setText("Recording... speak now");
        new Thread(() -> {
            int rate = 16000;
            int min = AudioRecord.getMinBufferSize(rate, android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT);
            int bufferBytes = Math.max(min, rate * 2);
            short[] buffer = new short[Math.max(1, bufferBytes / 2)];
            AudioRecord recorder = null;
            try {
                recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
                        android.media.AudioFormat.CHANNEL_IN_MONO,
                        android.media.AudioFormat.ENCODING_PCM_16BIT, bufferBytes * 2);
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    throw new IllegalStateException("AudioRecord did not initialize");
                }
                recorder.startRecording();
                ByteArrayOutputStream pcm = new ByteArrayOutputStream();
                long end = System.currentTimeMillis() + SECONDS * 1000L;
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
                writeWav(pcm.toByteArray(), rate, 1, 16);
                final long max = peak;
                final long rms = count == 0 ? 0 : Math.round(Math.sqrt(sum / count));
                AppLog.w("mic test: peak=" + max + " rms=" + rms + " file=" + file.getAbsolutePath());
                runOnUiThread(() -> {
                    status.setText("Done");
                    result.setText("Peak " + max + " / 32767\nRMS " + rms
                            + (max < 200 ? "\nVery low: the app is not getting microphone audio." : "\nAudio captured."));
                    start.setEnabled(true);
                    play.setEnabled(true);
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                AppLog.w("mic test failed: " + message);
                runOnUiThread(() -> {
                    status.setText("Record failed");
                    result.setText(message);
                    start.setEnabled(true);
                });
            } finally {
                if (recorder != null) {
                    try {
                        recorder.release();
                    } catch (Exception ignored) {
                    }
                }
                recording = false;
            }
        }, "mic-test").start();
    }

    private void writeWav(byte[] pcm, int rate, int channels, int bits) throws Exception {
        int dataSize = pcm.length;
        int byteRate = rate * channels * bits / 8;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{'R', 'I', 'F', 'F'});
        out.write(le32(36 + dataSize));
        out.write(new byte[]{'W', 'A', 'V', 'E'});
        out.write(new byte[]{'f', 'm', 't', ' '});
        out.write(le32(16));
        out.write(le16(1));
        out.write(le16(channels));
        out.write(le32(rate));
        out.write(le32(byteRate));
        out.write(le16(channels * bits / 8));
        out.write(le16(bits));
        out.write(new byte[]{'d', 'a', 't', 'a'});
        out.write(le32(dataSize));
        out.write(pcm);
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(out.toByteArray());
        }
    }

    private static byte[] le32(int value) {
        return new byte[]{(byte) value, (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24)};
    }

    private static byte[] le16(int value) {
        return new byte[]{(byte) value, (byte) (value >> 8)};
    }

    private void play() {
        if (!file.isFile()) return;
        try {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.prepare();
            player.start();
            Toast.makeText(this, "Playing the recording", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            result.setText("Play failed: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            status.setText("Microphone granted. Press Start.");
        }
    }
}
