package com.aeromaintenance.ai.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;

import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;

/** Shared drawing helpers for the Canvas charts. */
abstract class ChartView extends View {

    protected final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final float density;

    ChartView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        fill.setStyle(Paint.Style.FILL);
        text.setTypeface(Type.font(context, R.font.barlow_regular));
        text.setTextSize(sp(11));
        text.setColor(Palette.TEXT_FAINT);
    }

    protected float dp(float v) {
        return v * density;
    }

    protected float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    protected DashPathEffect dash() {
        return new DashPathEffect(new float[]{dp(6), dp(5)}, 0);
    }

    protected void strokeLine(Canvas c, float x0, float y0, float x1, float y1, int color, float widthDp, boolean dashed) {
        line.setColor(color);
        line.setStrokeWidth(dp(widthDp));
        line.setPathEffect(dashed ? dash() : null);
        c.drawLine(x0, y0, x1, y1, line);
        line.setPathEffect(null);
    }

    /**
     * Draws a label whose top-left corner is at (x, top), or whose top-right corner
     * is there when {@code alignRight}. Labels are clamped inside the view.
     */
    protected void label(Canvas c, String s, float x, float top, int color, boolean alignRight) {
        label(c, s, x, top, color, alignRight, text);
    }

    protected void label(Canvas c, String s, float x, float top, int color, boolean alignRight, Paint p) {
        p.setColor(color);
        float w = p.measureText(s);
        Paint.FontMetrics fm = p.getFontMetrics();
        float h = fm.descent - fm.ascent;
        float left = alignRight ? x - w : x;
        left = Math.max(0f, Math.min(left, Math.max(0f, getWidth() - w)));
        float t = Math.max(0f, Math.min(top, Math.max(0f, getHeight() - h)));
        c.drawText(s, left, t - fm.ascent, p);
    }
}
