package io.ollamaontv;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FocusCard extends FrameLayout {
    private final TextView titleView;
    private final TextView metaView;
    private final ImageView iconView;
    private final GradientDrawable background;
    private Runnable click;
    private final int radius;
    private final int colorA;
    private final int colorB;

    public FocusCard(Context context, String title, String meta, int colorA, int colorB) {
        this(context, title, meta, colorA, colorB, 22);
    }

    public FocusCard(Context context, String title, String meta, int colorA, int colorB, int radius) {
        super(context);
        this.colorA = colorA;
        this.colorB = colorB;
        this.radius = radius;
        setFocusable(true);
        setClickable(true);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        UiKit.dp(getContext(), FocusCard.this.radius));
            }
        });
        background = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{colorA, colorB});
        background.setCornerRadius(UiKit.dp(context, radius));
        setBackground(background);

        iconView = new ImageView(context);
        LayoutParams iconParams = new LayoutParams(UiKit.dp(context, 44), UiKit.dp(context, 44), Gravity.TOP | Gravity.START);
        iconParams.leftMargin = UiKit.dp(context, 18);
        iconParams.topMargin = UiKit.dp(context, 18);
        iconView.setVisibility(GONE);
        addView(iconView, iconParams);

        LinearLayout text = new LinearLayout(context);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.BOTTOM);
        titleView = UiKit.text(context, title, 19, UiKit.INK, true);
        titleView.setMaxLines(2);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        metaView = UiKit.text(context, meta, 12, 0xCCFFFFFF, false);
        metaView.setMaxLines(2);
        metaView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.addView(titleView);
        text.addView(metaView, UiKit.params(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 4, context));
        LayoutParams textParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        textParams.leftMargin = UiKit.dp(context, 18);
        textParams.rightMargin = UiKit.dp(context, 18);
        textParams.bottomMargin = UiKit.dp(context, 16);
        addView(text, textParams);

        setOnClickListener(v -> {
            SoundFx.select();
            if (click != null) click.run();
        });
    }

    public FocusCard icon(Drawable drawable) {
        iconView.setImageDrawable(drawable);
        iconView.setVisibility(drawable == null ? GONE : VISIBLE);
        return this;
    }

    public FocusCard onCardClick(Runnable action) {
        click = action;
        return this;
    }

    public FocusCard size(int widthDp, int heightDp) {
        setLayoutParams(new LinearLayout.LayoutParams(UiKit.dp(getContext(), widthDp), UiKit.dp(getContext(), heightDp)));
        return this;
    }

    public void focus(boolean focused) {
        background.setStroke(UiKit.dp(getContext(), focused ? 2 : 0),
                focused ? UiKit.CYAN : 0x00000000);
        if (UiKit.motion(getContext())) {
            animate()
                    .scaleX(focused ? 1.06f : 1f)
                    .scaleY(focused ? 1.06f : 1f)
                    .translationZ(focused ? UiKit.dp(getContext(), 18) : 0)
                    .setDuration(180)
                    .start();
        }
        setElevation(focused ? UiKit.dp(getContext(), 12) : 0);
    }

    public TextView titleView() {
        return titleView;
    }
}
