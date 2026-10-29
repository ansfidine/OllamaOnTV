package io.ollamaontv;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class UiKit {
    static final int BG = 0xFF05060A;
    static final int BG2 = 0xFF0B1026;
    static final int BG3 = 0xFF121833;
    static final int INK = 0xFFF8FAFC;
    static final int MUTED = 0xFF94A3B8;
    static final int FAINT = 0xFF64748B;
    static final int PURPLE = 0xFF8B5CF6;
    static final int BLUE = 0xFF3B82F6;
    static final int CYAN = 0xFF22D3EE;
    static final int ORANGE = 0xFFFB923C;
    static final int GLASS = 0x14FFFFFF;
    static final int GLASS_STRONG = 0x24FFFFFF;
    static final int BORDER = 0x2EFFFFFF;

    private static Typeface base;

    private UiKit() {}

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static Typeface font(Context context, int style) {
        if (base == null) {
            try {
                base = Typeface.createFromAsset(context.getAssets(), "fonts/InterVariable.ttf");
            } catch (Exception e) {
                base = Typeface.create("sans-serif", Typeface.NORMAL);
            }
        }
        return Typeface.create(base, style);
    }

    static boolean motion(Context context) {
        try {
            float scale = Settings.Global.getFloat(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            return scale > 0.01f;
        } catch (Exception e) {
            return true;
        }
    }

    static GradientDrawable box(Context context, int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static GradientDrawable gradient(Context context, int radiusDp, int topColor, int bottomColor) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{topColor, bottomColor});
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static GradientDrawable glass(Context context, int radiusDp, int fill, int border) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{fill, 0x08FFFFFF});
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), border);
        return drawable;
    }

    static TextView text(Context context, String text, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(font(context, bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    static void focusable(View view, Context context, int normal, int focused) {
        GradientDrawable normalDrawable = box(context, normal, 14);
        GradientDrawable focusedDrawable = box(context, focused, 14);
        view.setBackground(normalDrawable);
        view.setFocusable(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackground(hasFocus ? focusedDrawable : normalDrawable);
            if (hasFocus) SoundFx.nav();
            if (motion(context)) {
                v.animate()
                        .scaleX(hasFocus ? 1.02f : 1f)
                        .scaleY(hasFocus ? 1.02f : 1f)
                        .translationZ(hasFocus ? dp(context, 6) : 0)
                        .setDuration(170)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        });
    }

    static Button button(Context context, String label, boolean accent) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(17);
        button.setTypeface(font(context, Typeface.BOLD));
        button.setTextColor(0xFFFFFFFF);
        button.setMinHeight(dp(context, 54));
        button.setPadding(dp(context, 22), 0, dp(context, 22), 0);
        focusable(button, context,
                accent ? 0xFF6D28D9 : GLASS_STRONG,
                accent ? 0xFF8B5CF6 : 0x33FFFFFF);
        return button;
    }

    static LinearLayout.LayoutParams params(int width, int height, int topDp, Context context) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = dp(context, topDp);
        return params;
    }
}
