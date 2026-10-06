package com.aeromaintenance.ai.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.PathInterpolator;

import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Ui;

/** Rounded horizontal bar: component health, evidence share, progress. */
public class BarView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float fraction;
    private float shown;
    private ValueAnimator animator;
    private final int heightPx;

    public BarView(Context context, int heightDp) {
        super(context);
        heightPx = Ui.dp(context, heightDp);
        track.setColor(Palette.HAIRLINE);
        fill.setColor(Palette.CYAN);
    }

    public BarView setColor(int color) {
        fill.setColor(color);
        invalidate();
        return this;
    }

    public BarView setTrackColor(int color) {
        track.setColor(color);
        invalidate();
        return this;
    }

    /** Sets the filled fraction (0..1), animating from 0 when {@code animate} is true. */
    public BarView setFraction(float value, boolean animate) {
        fraction = Math.max(0f, Math.min(1f, value));
        if (animator != null) animator.cancel();
        if (animate) {
            animator = ValueAnimator.ofFloat(0f, fraction);
            animator.setDuration(900);
            animator.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
            animator.addUpdateListener(a -> {
                shown = (Float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            shown = fraction;
            invalidate();
        }
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), heightPx);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = getHeight() / 2f;
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, r, r, track);
        float w = getWidth() * shown;
        if (w > 0.5f) {
            rect.set(0, 0, Math.max(w, getHeight()), getHeight());
            canvas.drawRoundRect(rect, r, r, fill);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        shown = fraction;
        super.onDetachedFromWindow();
    }
}
