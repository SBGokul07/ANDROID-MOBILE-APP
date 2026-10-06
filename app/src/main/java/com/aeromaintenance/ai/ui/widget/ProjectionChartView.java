package com.aeromaintenance.ai.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.animation.PathInterpolator;

import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.ui.Palette;

/**
 * Remaining-useful-life chart: the health indicator's history (cyan), its nominal
 * level, the failure limit (red) and the AI's projection from now to the limit
 * (magenta, dashed). The horizontal length of the projection is the RUL.
 */
public class ProjectionChartView extends ChartView {

    private double[] history = new double[0];
    private double current;
    private double nominal;
    private double limit;
    private int rulHours;
    private int decimals;
    private String unit = "";
    private float grow = 1f;
    private ValueAnimator animator;
    private final Path path = new Path();

    public ProjectionChartView(Context context) {
        super(context);
    }

    public ProjectionChartView setData(double[] history, double current, double nominal, double limit,
                                       int rulHours, int decimals, String unit) {
        this.history = history;
        this.current = current;
        this.nominal = nominal;
        this.limit = limit;
        this.rulHours = rulHours;
        this.decimals = decimals;
        this.unit = unit;
        invalidate();
        return this;
    }

    public ProjectionChartView animateGrow(boolean animate) {
        if (animator != null) animator.cancel();
        if (!animate) {
            grow = 1f;
            invalidate();
            return this;
        }
        grow = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1400);
        animator.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
        animator.addUpdateListener(a -> {
            grow = (Float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.round(dp(170)));
    }

    @Override
    protected void onDraw(Canvas c) {
        int n = history.length;
        if (n < 2) return;
        int ahead = Math.min(rulHours, 240);
        int total = n + Math.max(ahead, 24) + 8;
        float left = dp(40);
        float right = dp(6);
        float top = dp(10);
        float bottom = dp(18);
        float w = getWidth() - left - right;
        float h = getHeight() - top - bottom;
        double minH = Double.MAX_VALUE;
        double maxH = -Double.MAX_VALUE;
        for (double v : history) {
            minH = Math.min(minH, v);
            maxH = Math.max(maxH, v);
        }
        double lo = Math.min(minH, nominal);
        double hi = Math.max(limit, maxH);
        double pad = (hi - lo) * 0.08;
        final double y0 = lo - pad;
        final double y1 = hi + pad;

        // Limit and nominal
        float limitY = yOf(limit, top, h, y0, y1);
        strokeLine(c, left, limitY, left + w, limitY, Palette.alpha(Palette.RED, 0.85f), 1.4f, false);
        label(c, "limit " + Format.number(limit, decimals) + " " + unit, left + dp(4), limitY - dp(15), Palette.RED, false);
        float nomY = yOf(nominal, top, h, y0, y1);
        strokeLine(c, left, nomY, left + w, nomY, Palette.alpha(Palette.CYAN, 0.3f), 1f, true);
        label(c, Format.number(y1, decimals), 0, top - dp(4), Palette.TEXT_FAINT, false);
        label(c, Format.number(y0, decimals), 0, top + h - dp(12), Palette.TEXT_FAINT, false);
        strokeLine(c, left, top + h, left + w, top + h, Palette.HAIRLINE, 1f, false);

        // History
        path.reset();
        for (int i = 0; i < n; i++) {
            float px = left + w * i / (total - 1);
            float py = yOf(history[i], top, h, y0, y1);
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        line.setColor(Palette.CYAN);
        line.setStrokeWidth(dp(2.4f));
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setPathEffect(null);
        c.drawPath(path, line);

        // Now marker
        float nowX = left + w * (n - 1) / (total - 1);
        strokeLine(c, nowX, top, nowX, top + h, Palette.HAIRLINE_STRONG, 1f, false);
        label(c, "−" + (n - 1) + " h", left, top + h + dp(3), Palette.TEXT_FAINT, false);
        label(c, "now", nowX, top + h + dp(3), Palette.TEXT_FAINT, true);

        // Projection
        double endIndex = (n - 1) + ahead * grow;
        double endValue = current + (limit - current) * (ahead / (double) Math.max(rulHours, 1)) * grow;
        float sx = nowX;
        float sy = yOf(current, top, h, y0, y1);
        float ex = left + (float) (w * endIndex / (total - 1));
        float ey = yOf(endValue, top, h, y0, y1);
        strokeLine(c, sx, sy, ex, ey, Palette.MAGENTA, 2.6f, true);
        fill.setColor(Palette.MAGENTA);
        c.drawCircle(sx, sy, dp(4.5f), fill);
        if (grow >= 1f) {
            c.drawCircle(ex, ey, dp(5), fill);
            String t = rulHours > ahead ? "+" + ahead + " h (limit beyond chart)" : "+" + rulHours + " h";
            label(c, t, ex, top + h + dp(3), Palette.MAGENTA, true);
        }
    }

    private static float yOf(double v, float top, float h, double y0, double y1) {
        return top + (float) (h * (1 - (v - y0) / (y1 - y0)));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        grow = 1f;
        super.onDetachedFromWindow();
    }
}
