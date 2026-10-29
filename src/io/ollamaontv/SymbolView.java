package io.ollamaontv;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

public class SymbolView extends View {
    private String name;
    private int color = UiKit.INK;
    private float size = 24;

    public SymbolView(Context context, String name) {
        super(context);
        this.name = name;
    }

    public void setSymbolColor(int color) {
        this.color = color;
        invalidate();
    }

    public void setSymbolSize(float size) {
        this.size = size;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Symbols.draw(getContext(), canvas, name, getWidth() / 2f, getHeight() / 2f, size, color);
    }
}
