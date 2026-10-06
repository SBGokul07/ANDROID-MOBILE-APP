package com.aeromaintenance.ai.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.animation.LinearInterpolator;

import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.SensorType;
import com.aeromaintenance.ai.ui.Palette;

import java.util.Collections;
import java.util.List;

/**
 * Telemetry trend chart. Draws the caution and warning bands of the channel, the
 * raw samples (faint), the cleaned and smoothed trace coloured by zone, the sensor
 * spikes removed by preprocessing (rings), the detected onset of degradation and
 * a pulsing "live" point at the latest sample.
 */
public class TrendChartView extends ChartView {

    private SensorType sensor;
    private double[] raw = new double[0];
    private double[] smooth = new double[0];
    private List<Integer> spikes = Collections.emptyList();
    private Integer onsetIndex;
    private final int heightDp;
    private float reveal = 1f;
    private ValueAnimator revealAnimator;
    private final Path path = new Path();
    private long pulseStart = System.currentTimeMillis();

    public TrendChartView(Context context, int heightDp) {
        super(context);
        this.heightDp = heightDp;
    }

    public TrendChartView setData(SensorType sensor, double[] raw, double[] smooth, List<Integer> spikes, Integer onsetIndex) {
        this.sensor = sensor;
        this.raw = raw;
        this.smooth = smooth;
        this.spikes = spikes == null ? Collections.<Integer>emptyList() : spikes;
        this.onsetIndex = onsetIndex;
        invalidate();
        return this;
    }

    /** Draws the trace from left to right (used when the data set changes). */
    public TrendChartView animateReveal(boolean animate) {
        if (revealAnimator != null) revealAnimator.cancel();
        if (!animate) {
            reveal = 1f;
            invalidate();
            return this;
        }
        reveal = 0f;
        revealAnimator = ValueAnimator.ofFloat(0f, 1f);
        revealAnimator.setDuration(1400);
        revealAnimator.setInterpolator(new LinearInterpolator());
        revealAnimator.addUpdateListener(a -> {
            reveal = (Float) a.getAnimatedValue();
            invalidate();
        });
        revealAnimator.start();
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.round(dp(heightDp)));
    }

    private float left, top, w, h;
    private int total;
    private double lo, hi;

    private float x(double i) {
        return left + (float) (w * i / (total - 1));
    }

    private float y(double v) {
        double f = (v - lo) / (hi - lo);
        f = Math.max(-0.02, Math.min(1.02, f));
        return top + (float) (h * (1 - f));
    }

    private void band(Canvas c, double a, double b, int color) {
        float y0 = y(Math.max(a, b));
        float y1 = y(Math.min(a, b));
        fill.setColor(color);
        c.drawRect(left, y0, left + w, y1, fill);
    }

    private void hLine(Canvas c, double v, int color) {
        strokeLine(c, left, y(v), left + w, y(v), color, 1f, true);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (sensor == null || smooth.length < 2) return;
        int n = smooth.length;
        total = n;
        left = dp(40);
        float right = dp(6);
        top = dp(6);
        float bottom = dp(18);
        w = getWidth() - left - right;
        h = getHeight() - top - bottom;
        lo = sensor.chartMin;
        hi = sensor.chartMax;

        // Zone bands
        if (sensor.warnHigh != null) band(c, sensor.warnHigh, sensor.alarmHigh != null ? sensor.alarmHigh : hi, Palette.alpha(Palette.AMBER, 0.07f));
        if (sensor.alarmHigh != null) band(c, sensor.alarmHigh, hi, Palette.alpha(Palette.RED, 0.10f));
        if (sensor.warnLow != null) band(c, sensor.warnLow, sensor.alarmLow != null ? sensor.alarmLow : lo, Palette.alpha(Palette.AMBER, 0.07f));
        if (sensor.alarmLow != null) band(c, lo, sensor.alarmLow, Palette.alpha(Palette.RED, 0.10f));

        if (sensor.warnHigh != null) hLine(c, sensor.warnHigh, Palette.alpha(Palette.AMBER, 0.6f));
        if (sensor.alarmHigh != null) hLine(c, sensor.alarmHigh, Palette.alpha(Palette.RED, 0.7f));
        if (sensor.warnLow != null) hLine(c, sensor.warnLow, Palette.alpha(Palette.AMBER, 0.6f));
        if (sensor.alarmLow != null) hLine(c, sensor.alarmLow, Palette.alpha(Palette.RED, 0.7f));
        hLine(c, sensor.baselineMean, Palette.alpha(Palette.CYAN, 0.25f));

        // Frame and axis labels
        strokeLine(c, left, top + h, left + w, top + h, Palette.HAIRLINE, 1f, false);
        label(c, Format.number(hi, sensor.decimals), 0, top - dp(2), Palette.TEXT_FAINT, false);
        label(c, Format.number(lo, sensor.decimals), 0, top + h - dp(12), Palette.TEXT_FAINT, false);
        label(c, "−" + (n - 1) + " h", left, top + h + dp(3), Palette.TEXT_FAINT, false);
        label(c, "now", x(n - 1), top + h + dp(3), Palette.TEXT_FAINT, true);

        // Onset marker
        if (onsetIndex != null && reveal > onsetIndex / (float) n) {
            float ox = x(onsetIndex);
            strokeLine(c, ox, top, ox, top + h, Palette.alpha(Palette.CYAN, 0.8f), 1.2f, true);
            label(c, "onset −" + (n - 1 - onsetIndex) + " h", ox + dp(4), top + dp(2), Palette.CYAN, false);
        }

        int visible = Math.max(2, (int) (n * reveal));

        // Raw samples
        path.reset();
        int rawCount = Math.min(visible, raw.length);
        for (int i = 0; i < rawCount; i++) {
            if (i == 0) path.moveTo(x(i), y(raw[i]));
            else path.lineTo(x(i), y(raw[i]));
        }
        line.setColor(Palette.alpha(Palette.TEXT_FAINT, 0.55f));
        line.setStrokeWidth(dp(1));
        line.setStrokeJoin(Paint.Join.ROUND);
        c.drawPath(path, line);

        // Smoothed trace, coloured by zone
        line.setStrokeWidth(dp(2.6f));
        for (int i = 1; i < Math.min(visible, n); i++) {
            line.setColor(Palette.zone(sensor.zoneOf(smooth[i])));
            c.drawLine(x(i - 1), y(smooth[i - 1]), x(i), y(smooth[i]), line);
        }

        // Removed spikes
        line.setColor(Palette.AMBER);
        line.setStrokeWidth(dp(1.5f));
        for (int i : spikes) {
            if (i < visible && i < raw.length) c.drawCircle(x(i), y(raw[i]), dp(4.5f), line);
        }

        // Live point
        if (reveal >= 1f) {
            float lx = x(n - 1);
            float ly = y(smooth[n - 1]);
            int color = Palette.zone(sensor.zoneOf(smooth[n - 1]));
            float t = ((System.currentTimeMillis() - pulseStart) % 1300L) / 1300f;
            fill.setColor(Palette.alpha(color, (1f - t) * 0.5f));
            c.drawCircle(lx, ly, dp(4) + dp(10) * t, fill);
            fill.setColor(color);
            c.drawCircle(lx, ly, dp(4), fill);
            // Keep the live point pulsing at ~15 fps while the chart is on screen
            // (at most one pending redraw, however often the chart is drawn).
            if (!pulseScheduled) {
                pulseScheduled = true;
                postDelayed(pulseTick, 66);
            }
        }
    }

    private boolean pulseScheduled;
    private final Runnable pulseTick = () -> {
        pulseScheduled = false;
        invalidate();
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        pulseStart = System.currentTimeMillis();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (revealAnimator != null) revealAnimator.cancel();
        reveal = 1f;
        removeCallbacks(pulseTick);
        pulseScheduled = false;
        super.onDetachedFromWindow();
    }
}
