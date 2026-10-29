package io.ollamaontv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

public class AuroraView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] stars = new float[64 * 4];
    private final long started = SystemClock.uptimeMillis();
    private final boolean motion;
    private boolean attached;

    public AuroraView(Context context) {
        super(context);
        motion = UiKit.motion(context);
        Random random = new Random(19);
        for (int i = 0; i < 64; i++) {
            stars[i * 4] = random.nextFloat();
            stars[i * 4 + 1] = random.nextFloat();
            stars[i * 4 + 2] = 0.6f + random.nextFloat() * 1.8f;
            stars[i * 4 + 3] = random.nextFloat() * 6.283f;
        }
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        long t = motion ? SystemClock.uptimeMillis() - started : 0;

        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{UiKit.BG, UiKit.BG2, 0xFF060814, UiKit.BG},
                new float[]{0f, 0.45f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        glow(canvas, w * 0.16f + wave(t, 9000) * w * 0.06f, h * 0.18f, w * 0.62f, UiKit.PURPLE, 92);
        glow(canvas, w * 0.86f + wave(t, 12000) * w * 0.05f, h * 0.38f, w * 0.7f, UiKit.BLUE, 80);
        glow(canvas, w * 0.5f + wave(t, 15000) * w * 0.09f, h * 1.02f, w * 0.75f, UiKit.CYAN, 38);

        starPaint.setColor(0xFFFFFFFF);
        for (int i = 0; i < 64; i++) {
            float x = stars[i * 4] * w;
            float y = stars[i * 4 + 1] * h;
            float radius = stars[i * 4 + 2];
            float twinkle = motion ? 0.25f + 0.75f * (0.5f + 0.5f * (float) Math.sin(t / 1700f + stars[i * 4 + 3])) : 0.6f;
            starPaint.setAlpha(Math.round(70 * twinkle));
            canvas.drawCircle(x, y, radius, starPaint);
        }

        paint.setShader(new RadialGradient(w * 0.5f, h * 0.5f, Math.max(w, h) * 0.8f,
                new int[]{0x00000000, 0x22000000, 0x77000000}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        if (attached && motion) postInvalidateOnAnimation();
    }

    private static float wave(long t, float period) {
        return (float) Math.sin(t / period * 6.283f);
    }

    private void glow(Canvas canvas, float x, float y, float radius, int color, int alpha) {
        paint.setShader(new RadialGradient(x, y, radius,
                new int[]{Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                        Color.argb(alpha / 3, Color.red(color), Color.green(color), Color.blue(color)),
                        0x00000000},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        super.onDetachedFromWindow();
    }
}
