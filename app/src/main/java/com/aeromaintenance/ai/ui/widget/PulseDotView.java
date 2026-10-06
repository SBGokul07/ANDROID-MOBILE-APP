package com.aeromaintenance.ai.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Small blinking dot that signals a live data feed. */
public class PulseDotView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator blink = ValueAnimator.ofFloat(0.25f, 1f);

    public PulseDotView(Context context, int color) {
        super(context);
        paint.setColor(color);
        blink.setDuration(700);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.addUpdateListener(a -> setAlpha((Float) a.getAnimatedValue()));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = Math.min(getWidth(), getHeight()) / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, r, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        blink.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        blink.cancel();
        super.onDetachedFromWindow();
    }
}
