package com.aeromaintenance.ai.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.animation.PathInterpolator;

import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.engine.ComponentAssessment;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Risk chart: remaining useful life per component, coloured by risk. The two
 * dashed lines are the risk rules (under 72 h = HIGH, under 250 h = MEDIUM), so
 * the chart shows <i>why</i> each component got its rating.
 */
public class RiskChartView extends ChartView {

    private static final int MAX_HOURS = 600;
    private List<ComponentAssessment> components = new ArrayList<>();
    private float grow = 1f;
    private ValueAnimator animator;
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public RiskChartView(Context context) {
        super(context);
        namePaint.setTypeface(Type.font(context, R.font.barlow_medium));
        namePaint.setTextSize(sp(13));
        valuePaint.setTypeface(Type.font(context, R.font.barlow_semibold));
        valuePaint.setTextSize(sp(12));
        valuePaint.setFontFeatureSettings("tnum");
    }

    public RiskChartView setComponents(List<ComponentAssessment> components, boolean animate) {
        this.components = components;
        requestLayout();
        if (animator != null) animator.cancel();
        if (animate) {
            grow = 0f;
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1000);
            animator.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
            animator.addUpdateListener(a -> {
                grow = (Float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            grow = 1f;
            invalidate();
        }
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.round(dp(44) * components.size() + dp(26)));
    }

    private float xOf(int hours, float left, float w) {
        return left + w * (Math.min(hours, MAX_HOURS) / (float) MAX_HOURS);
    }

    @Override
    protected void onDraw(Canvas c) {
        float left = dp(112);
        float right = dp(8);
        float w = getWidth() - left - right;
        float rh = dp(44);
        float chartH = rh * components.size();

        int[] rules = {72, 250};
        int[] ruleColors = {Palette.RED, Palette.ORANGE};
        for (int k = 0; k < rules.length; k++) {
            float x = xOf(rules[k], left, w);
            line.setColor(Palette.alpha(ruleColors[k], 0.7f));
            line.setStrokeWidth(dp(1));
            line.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(5), dp(4)}, 0));
            c.drawLine(x, 0, x, chartH, line);
            line.setPathEffect(null);
            label(c, rules[k] + " h", x, chartH + dp(6), ruleColors[k], false);
        }
        label(c, MAX_HOURS + "+ h", left + w, chartH + dp(6), Palette.TEXT_FAINT, true);

        float barH = dp(14);
        float radius = dp(4);
        for (int i = 0; i < components.size(); i++) {
            ComponentAssessment ca = components.get(i);
            float cy = rh * i + rh / 2f;
            int color = Palette.risk(ca.risk);
            label(c, ca.component.label, 0, cy - dp(9), Palette.TEXT, false, namePaint);
            fill.setColor(Palette.HAIRLINE);
            rect.set(left, cy - barH / 2, left + w, cy + barH / 2);
            c.drawRoundRect(rect, radius, radius, fill);
            float bw = (xOf(ca.rulHours, left, w) - left) * grow;
            fill.setColor(color);
            rect.set(left, cy - barH / 2, left + Math.max(bw, dp(3)), cy + barH / 2);
            c.drawRoundRect(rect, radius, radius, fill);

            String t = InferenceEngine.rulText(ca.rulHours) + "  " + ca.risk.label;
            float tw = valuePaint.measureText(t);
            float tx = left + bw + dp(6) + tw < getWidth() ? left + bw + dp(6) : left + bw - tw - dp(6);
            int textColor = tx < left + bw ? Palette.NIGHT : color;
            label(c, t, tx, cy - dp(8), textColor, false, valuePaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        grow = 1f;
        super.onDetachedFromWindow();
    }
}
