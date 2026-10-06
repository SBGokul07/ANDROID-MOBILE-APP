package com.aeromaintenance.ai.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.LinearLayout;

import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Ui;

/**
 * A display unit: dark panel with a hairline frame. A highlighted panel pulses in
 * cyan (the guided demo uses this to point at what it is talking about); an accent
 * colours the frame (e.g. red for a high-risk result).
 */
public class PanelView extends LinearLayout {

    private final GradientDrawable background = new GradientDrawable();
    private ValueAnimator pulse;
    private boolean highlighted;
    private Integer accent;

    public PanelView(Context context) {
        this(context, 16);
    }

    public PanelView(Context context, int paddingDp) {
        super(context);
        setOrientation(VERTICAL);
        background.setColor(Palette.PANEL);
        background.setCornerRadius(Ui.dp(context, 14));
        setBackground(background);
        setContentPadding(paddingDp);
        applyStroke(1f);
    }

    public PanelView setContentPadding(int dp) {
        int p = Ui.dp(getContext(), dp);
        setPadding(p, p, p, p);
        return this;
    }

    /** Frame tinted with this colour (e.g. the risk colour). */
    public PanelView setAccent(Integer color) {
        accent = color;
        applyStroke(1f);
        return this;
    }

    /** Cyan pulsing frame, used by the guided demo. */
    public PanelView setHighlighted(boolean on) {
        if (highlighted == on) return this;
        highlighted = on;
        if (on) startPulse();
        else stopPulse();
        applyStroke(1f);
        return this;
    }

    public PanelView setPanelColor(int color) {
        background.setColor(color);
        return this;
    }

    /** Makes the whole panel tappable, with a ripple. */
    public PanelView onTap(View.OnClickListener listener) {
        setOnClickListener(listener);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(Ui.dp(getContext(), 14));
        setForeground(new RippleDrawable(ColorStateList.valueOf(Palette.alpha(Palette.TEXT, 0.10f)), null, mask));
        return this;
    }

    private void applyStroke(float pulseAlpha) {
        int color;
        float width;
        if (highlighted) {
            color = Palette.alpha(Palette.CYAN, pulseAlpha);
            width = 2f;
        } else if (accent != null) {
            color = Palette.alpha(accent, 0.55f);
            width = 1f;
        } else {
            color = Palette.HAIRLINE;
            width = 1f;
        }
        background.setStroke(Ui.dp(getContext(), width), color);
    }

    private void startPulse() {
        stopPulse();
        pulse = ValueAnimator.ofFloat(0.45f, 1f);
        pulse.setDuration(900);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.addUpdateListener(a -> applyStroke((Float) a.getAnimatedValue()));
        if (isAttachedToWindow()) pulse.start();
    }

    private void stopPulse() {
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (highlighted && pulse != null && !pulse.isStarted()) pulse.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (pulse != null) pulse.cancel();
        super.onDetachedFromWindow();
    }
}
