package io.ollamaontv;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomeRow extends LinearLayout {
    private final LinearLayout cards;
    private final HorizontalScrollView scroll;

    public HomeRow(Context context, String title) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(0, UiKit.dp(context, 22), 0, 0);
        setClipChildren(false);
        setClipToPadding(false);

        TextView label = UiKit.text(context, title, 21, UiKit.INK, true);
        label.setPadding(UiKit.dp(context, 48), 0, 0, 0);
        addView(label);

        scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        scroll.setPadding(UiKit.dp(context, 48), UiKit.dp(context, 12),
                UiKit.dp(context, 48), UiKit.dp(context, 18));
        cards = new LinearLayout(context);
        cards.setOrientation(HORIZONTAL);
        cards.setClipChildren(false);
        cards.setClipToPadding(false);
        scroll.addView(cards);
        addView(scroll, UiKit.params(LayoutParams.MATCH_PARENT, UiKit.dp(context, 190), 0, context));
    }

    public FocusCard add(FocusCard card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                UiKit.dp(getContext(), 250), UiKit.dp(getContext(), 146));
        params.rightMargin = UiKit.dp(getContext(), 14);
        card.setLayoutParams(params);
        card.setOnFocusChangeListener((v, hasFocus) -> {
            card.focus(hasFocus);
            if (hasFocus) {
                cards.post(() -> {
                    int target = card.getLeft() - UiKit.dp(getContext(), 48);
                    scroll.smoothScrollTo(Math.max(0, target), 0);
                });
            }
        });
        cards.addView(card);
        return card;
    }

    public void addCompact(FocusCard card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                UiKit.dp(getContext(), 210), UiKit.dp(getContext(), 92));
        params.rightMargin = UiKit.dp(getContext(), 12);
        card.setLayoutParams(params);
        cards.addView(card);
    }
}
