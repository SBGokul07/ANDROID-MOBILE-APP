package com.aeromaintenance.ai.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.PathInterpolator;

import com.aeromaintenance.ai.ui.Palette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Engine-instrument style dial, modelled on glass-cockpit EGT / N1 gauges: a 240°
 * scale with coloured range arcs on the outside, the filled value arc, tick marks
 * and a needle. Read-outs are placed on top of it by the caller.
 */
public class DialView extends View {

    /** A coloured range on the dial scale (green arc, amber arc, red arc…). */
    public static final class Band {
        public final float from;
        public final float to;
        public final int color;

        public Band(float from, float to, int color) {
            this.from = from;
            this.to = to;
            this.color = color;
        }
    }

    /** Default range arcs for a 0–100 health scale. */
    public static List<Band> healthBands() {
        return Arrays.asList(
                new Band(0f, 60f, Palette.RED),
                new Band(60f, 80f, Palette.ORANGE),
                new Band(80f, 100f, Palette.GREEN));
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float density;

    private float min = 0f;
    private float max = 100f;
    private List<Band> bands = new ArrayList<>();
    private float strokeDp = 10f;
    private float sweep = 240f;
    private int ticks = 10;
    private float current;
    private ValueAnimator animator;

    public DialView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
    }

    public DialView setRange(float min, float max) {
        this.min = min;
        this.max = max;
        this.current = min;
        return this;
    }

    public DialView setBands(List<Band> bands) {
        this.bands = bands;
        invalidate();
        return this;
    }

    public DialView setStrokeDp(float dp) {
        this.strokeDp = dp;
        invalidate();
        return this;
    }

    public DialView setTicks(int ticks) {
        this.ticks = ticks;
        return this;
    }

    /** Shows {@code value}; when {@code sweepFromMin} the needle sweeps up from the bottom of the scale. */
    public DialView setValue(float value, boolean sweepFromMin) {
        float target = clamp(value);
        if (sweepFromMin) {
            current = min;
            animateTo(target);
        } else {
            if (animator != null) animator.cancel();
            current = target;
            invalidate();
        }
        return this;
    }

    /** Animates the needle from where it is now to {@code value}. */
    public void animateTo(float value) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(current, clamp(value));
        animator.setDuration(1100);
        animator.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
        animator.addUpdateListener(a -> {
            current = (Float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private float clamp(float v) {
        return Math.max(min, Math.min(max, v));
    }

    private float angleOf(float v) {
        float start = 90f + (360f - sweep) / 2f;
        float f = (v - min) / (max - min);
        return start + sweep * Math.max(0f, Math.min(1f, f));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) w = Math.round(120 * density);
        int size = w;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            size = Math.min(w, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(w, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float sw = strokeDp * density;
        float bandW = sw * 0.42f;
        float gap = sw * 0.55f;
        float outer = Math.min(getWidth(), getHeight()) / 2f;
        float bandR = outer - bandW / 2f;
        float r = bandR - bandW / 2f - gap - sw / 2f;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float start = 90f + (360f - sweep) / 2f;

        int valueColor = Palette.CYAN;
        for (Band b : bands) {
            if (current >= b.from && current <= b.to) {
                valueColor = b.color;
                break;
            }
        }

        // Range arcs (outer ring)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(bandW);
        oval.set(cx - bandR, cy - bandR, cx + bandR, cy + bandR);
        for (Band b : bands) {
            float a0 = angleOf(b.from);
            float a1 = angleOf(b.to);
            paint.setColor(Palette.alpha(b.color, 0.85f));
            canvas.drawArc(oval, a0, a1 - a0, false, paint);
        }

        // Scale track and value arc
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(sw);
        oval.set(cx - r, cy - r, cx + r, cy + r);
        paint.setColor(Palette.HAIRLINE);
        canvas.drawArc(oval, start, sweep, false, paint);
        float va = angleOf(current);
        if (va > start + 0.5f) {
            paint.setColor(valueColor);
            canvas.drawArc(oval, start, va - start, false, paint);
        }

        // Ticks
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(Palette.TEXT_FAINT);
        for (int i = 0; i <= ticks; i++) {
            double a = Math.toRadians(start + sweep * i / ticks);
            float r0 = r - sw / 2f - sw * 0.5f;
            float r1 = r0 - (i % 5 == 0 ? sw * 0.9f : sw * 0.45f);
            canvas.drawLine(cx + (float) (r0 * Math.cos(a)), cy + (float) (r0 * Math.sin(a)),
                    cx + (float) (r1 * Math.cos(a)), cy + (float) (r1 * Math.sin(a)), paint);
        }

        // Needle
        double na = Math.toRadians(va);
        float nLen = r - sw * 1.6f;
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(3f * density);
        paint.setColor(Palette.TEXT);
        canvas.drawLine(cx - (float) (sw * 1.2f * Math.cos(na)), cy - (float) (sw * 1.2f * Math.sin(na)),
                cx + (float) (nLen * Math.cos(na)), cy + (float) (nLen * Math.sin(na)), paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, sw * 0.55f, paint);
        paint.setColor(Palette.NIGHT);
        canvas.drawCircle(cx, cy, sw * 0.25f, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}
