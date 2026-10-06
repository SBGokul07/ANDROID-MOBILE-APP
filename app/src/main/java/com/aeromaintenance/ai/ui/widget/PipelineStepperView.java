package com.aeromaintenance.ai.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical AI pipeline used on the AI prediction screen. Completed stages show a
 * green tick (and their detail line when details are given); the active stage pulses.
 */
public class PipelineStepperView extends LinearLayout {

    private final List<String> titles;
    private final List<String> details;
    private final List<StepCircle> circles = new ArrayList<>();
    private final List<View> connectors = new ArrayList<>();
    private final List<TextView> titleViews = new ArrayList<>();
    private final List<TextView> detailViews = new ArrayList<>();
    private int completed = -1;
    private float pulse = 1f;
    private ValueAnimator animator;

    public PipelineStepperView(Context context, List<String> titles, List<String> details) {
        super(context);
        this.titles = titles;
        this.details = details;
        setOrientation(VERTICAL);
        for (int i = 0; i < titles.size(); i++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);

            LinearLayout rail = new LinearLayout(context);
            rail.setOrientation(VERTICAL);
            rail.setGravity(Gravity.CENTER_HORIZONTAL);
            StepCircle circle = new StepCircle(context, i + 1);
            rail.addView(circle, new LayoutParams(Ui.dp(context, 22), Ui.dp(context, 22)));
            View connector = new View(context);
            if (i < titles.size() - 1) {
                rail.addView(connector, new LayoutParams(Ui.dp(context, 1.5f), Ui.dp(context, 14)));
            }
            row.addView(rail, new LayoutParams(Ui.dp(context, 28), LayoutParams.MATCH_PARENT));

            LinearLayout text = new LinearLayout(context);
            text.setOrientation(VERTICAL);
            text.setPadding(Ui.dp(context, 10), Ui.dp(context, 1), 0, 0);
            TextView title = Ui.text(context, titles.get(i), Type.Style.TITLE_SMALL, Palette.TEXT_FAINT);
            text.addView(title);
            TextView detail = Ui.text(context, details != null && i < details.size() ? details.get(i) : "",
                    Type.Style.BODY_SMALL, Palette.TEXT_MUTED);
            detail.setMaxLines(2);
            detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
            detail.setVisibility(GONE);
            text.addView(detail);
            row.addView(text, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            circles.add(circle);
            connectors.add(connector);
            titleViews.add(title);
            detailViews.add(detail);
        }
        setCompleted(-1);
    }

    /** Stages before {@code completed} are done; the stage at {@code completed} is active. */
    public void setCompleted(int completed) {
        this.completed = completed;
        for (int i = 0; i < titles.size(); i++) {
            boolean done = i < completed;
            boolean active = i == completed;
            circles.get(i).set(done, active);
            TextView t = titleViews.get(i);
            t.setTextColor(done ? Palette.TEXT : active ? Palette.CYAN : Palette.TEXT_FAINT);
            boolean showDetail = done && details != null && i < details.size();
            detailViews.get(i).setVisibility(showDetail ? VISIBLE : GONE);
            View connector = connectors.get(i);
            connector.setBackgroundColor(done ? Palette.alpha(Palette.GREEN, 0.5f) : Palette.HAIRLINE);
            LayoutParams lp = (LayoutParams) connector.getLayoutParams();
            if (lp != null) {
                lp.height = Ui.dp(getContext(), done && details != null ? 34 : 14);
                connector.setLayoutParams(lp);
            }
        }
        applyPulse();
        boolean anyActive = completed >= 0 && completed < titles.size();
        if (anyActive) startPulse();
        else stopPulse();
    }

    private void applyPulse() {
        for (int i = 0; i < titles.size(); i++) {
            boolean active = i == completed;
            circles.get(i).setPulse(active ? pulse : 1f);
            titleViews.get(i).setAlpha(active ? 0.6f + 0.4f * pulse : 1f);
        }
    }

    private void startPulse() {
        if (animator != null && animator.isRunning()) return;
        animator = ValueAnimator.ofFloat(0.35f, 1f);
        animator.setDuration(600);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(a -> {
            pulse = (Float) a.getAnimatedValue();
            applyPulse();
        });
        if (isAttachedToWindow()) animator.start();
    }

    private void stopPulse() {
        if (animator != null) animator.cancel();
        animator = null;
        pulse = 1f;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isStarted()) animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    /** Numbered circle that turns into a green tick when the stage is done. */
    static final class StepCircle extends View {
        private final String number;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path check = new Path();
        private boolean done;
        private boolean active;
        private float pulse = 1f;

        StepCircle(Context context, int number) {
            super(context);
            this.number = Integer.toString(number);
            paint.setTypeface(Type.font(context, R.font.barlow_semicondensed_bold));
            paint.setTextAlign(Paint.Align.CENTER);
        }

        void set(boolean done, boolean active) {
            this.done = done;
            this.active = active;
            invalidate();
        }

        void setPulse(float p) {
            pulse = p;
            if (active) invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = getResources().getDisplayMetrics().density;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 1.5f * d / 2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(done ? Palette.alpha(Palette.GREEN, 0.18f) : Palette.NIGHT);
            c.drawCircle(cx, cy, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f * d);
            paint.setColor(done ? Palette.GREEN : active ? Palette.alpha(Palette.CYAN, pulse) : Palette.HAIRLINE);
            c.drawCircle(cx, cy, r, paint);
            if (done) {
                check.reset();
                check.moveTo(cx - r * 0.42f, cy + r * 0.02f);
                check.lineTo(cx - r * 0.12f, cy + r * 0.32f);
                check.lineTo(cx + r * 0.45f, cy - r * 0.30f);
                paint.setStrokeWidth(2f * d);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setColor(Palette.GREEN);
                c.drawPath(check, paint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 12,
                        getResources().getDisplayMetrics()));
                paint.setColor(active ? Palette.CYAN : Palette.TEXT_FAINT);
                Paint.FontMetrics fm = paint.getFontMetrics();
                c.drawText(number, cx, cy - (fm.ascent + fm.descent) / 2f, paint);
            }
        }
    }
}
