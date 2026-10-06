package com.aeromaintenance.ai.ui.screens;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.Screen;
import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.engine.AnalysisResult;
import com.aeromaintenance.ai.engine.ComponentAssessment;
import com.aeromaintenance.ai.engine.Contribution;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.engine.PipelineStage;
import com.aeromaintenance.ai.engine.PreprocessedWindow;
import com.aeromaintenance.ai.ui.Controls;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.BarView;
import com.aeromaintenance.ai.ui.widget.ButtonView;
import com.aeromaintenance.ai.ui.widget.DialView;
import com.aeromaintenance.ai.ui.widget.PanelView;
import com.aeromaintenance.ai.ui.widget.PipelineStepperView;
import com.aeromaintenance.ai.ui.widget.ProjectionChartView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The AI prediction screen: runs the (simulated) inference pipeline stage by
 * stage, then shows anomaly score, failure risk, component, condition, remaining
 * useful life, confidence, recommendation and the reasoning behind them.
 */
public class AnalysisScreen extends BaseScreen {

    static final List<String> PIPELINE_TITLES = Collections.unmodifiableList(Arrays.asList(
            "Data acquisition", "Preprocessing", "Feature extraction", "Time-series model", "Anomaly detection",
            "Component health", "Failure-risk prediction", "RUL estimation", "Maintenance recommendation"));

    static List<DialView.Band> anomalyBands() {
        return Arrays.asList(
                new DialView.Band(0f, (float) InferenceEngine.ALERT_THRESHOLD, Palette.GREEN),
                new DialView.Band((float) InferenceEngine.ALERT_THRESHOLD, (float) InferenceEngine.HIGH_THRESHOLD, Palette.ORANGE),
                new DialView.Band((float) InferenceEngine.HIGH_THRESHOLD, 1f, Palette.RED));
    }

    private PipelineStepperView runningStepper;
    private BarView runningBar;
    private boolean traceOpen;

    public AnalysisScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return change == AppState.Change.SELECTION || change == AppState.Change.ANALYSIS;
    }

    @Override
    public void onStateChanged(AppState.Change change) {
        if (change == AppState.Change.ANALYSIS_PROGRESS && runningStepper != null) {
            // Update the running pipeline in place instead of rebuilding the screen.
            runningStepper.setCompleted(state.runningStage());
            runningBar.setFraction(state.runningStage() / (float) AppState.PIPELINE_STAGES, false);
            return;
        }
        super.onStateChanged(change);
    }

    @Override
    protected void build() {
        final String id = state.selectedId();
        DataCondition cond = state.conditionOf(id);
        AppState.AnalysisPhase phase = state.analysisPhase();
        runningStepper = null;
        runningBar = null;

        // ---- Model and inputs --------------------------------------------------
        PanelView model = new PanelView(ctx);
        LinearLayout head = Ui.row(ctx);
        FrameLayout badge = new FrameLayout(ctx);
        badge.setBackground(Ui.rounded(ctx, Palette.alpha(Palette.MAGENTA, 0.14f), 10, 0, 0));
        badge.addView(Ui.icon(ctx, R.drawable.ic_psychology, Palette.MAGENTA, 24),
                new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        head.addView(badge, Ui.size(ctx, 40, 40));
        head.addView(Ui.hspace(ctx, 12));
        LinearLayout names = Ui.column(ctx);
        names.addView(Ui.text(ctx, "AI model", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED));
        names.addView(Ui.text(ctx, InferenceEngine.MODEL_NAME, Type.Style.TITLE_MEDIUM, Palette.TEXT));
        head.addView(names, Ui.weight(1f));
        model.addView(head, Ui.matchWrap());
        LinearLayout.LayoutParams tagLp = Ui.wrap();
        tagLp.topMargin = dp(10);
        model.addView(Ui.tag(ctx, InferenceEngine.MODEL_TAG, Palette.MAGENTA), tagLp);
        Ui.add(model, Ui.text(ctx, InferenceEngine.MODEL_VERSION + ". Deterministic research model running on the "
                + "phone; see AI insights for exactly what it computes.", Type.Style.BODY_SMALL, Palette.TEXT_FAINT), 4);
        Ui.add(model, Controls.aircraftSelector(ctx, state), 14);
        Ui.add(model, Controls.conditionToggle(ctx, cond, c -> state.setCondition(id, c)), 10);
        String label;
        int icon;
        if (phase == AppState.AnalysisPhase.DONE) {
            label = "Run AI analysis again";
            icon = R.drawable.ic_refresh;
        } else if (phase == AppState.AnalysisPhase.RUNNING) {
            label = "Analyzing…";
            icon = R.drawable.ic_psychology;
        } else {
            label = "Run AI analysis";
            icon = R.drawable.ic_psychology;
        }
        ButtonView run = Ui.primary(ctx, label, icon, v -> state.runAnalysis());
        run.setEnabled(phase != AppState.AnalysisPhase.RUNNING);
        Ui.add(model, run, 12);
        add(model);

        // ---- Status / results ---------------------------------------------------
        if (phase == AppState.AnalysisPhase.IDLE) {
            PanelView idle = new PanelView(ctx);
            idle.addView(Ui.text(ctx, "Analysis status", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED));
            idle.addView(Ui.text(ctx, "Ready", Type.Style.TITLE_LARGE, Palette.TEXT));
            Ui.add(idle, Ui.text(ctx, "Input: " + id + ", " + cond.label.toLowerCase(Locale.ROOT)
                    + ", 6 channels × 120 operating hours (720 samples). Tap Run AI analysis to send it through the pipeline.",
                    Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED), 6);
            PipelineStepperView stepper = new PipelineStepperView(ctx, PIPELINE_TITLES, null);
            stepper.setCompleted(-1);
            Ui.add(idle, stepper, 12);
            add(focusTarget("pipeline", idle));
        } else if (phase == AppState.AnalysisPhase.RUNNING) {
            PanelView running = focusTarget("pipeline", new PanelView(ctx));
            running.setHighlighted(isFocused("pipeline"));
            running.addView(Ui.text(ctx, "Analysis status", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED));
            running.addView(Ui.text(ctx, "Analyzing telemetry…", Type.Style.TITLE_LARGE, Palette.CYAN));
            runningBar = new BarView(ctx, 4).setColor(Palette.CYAN);
            runningBar.setFraction(state.runningStage() / (float) AppState.PIPELINE_STAGES, false);
            Ui.add(running, runningBar, 8);
            runningStepper = new PipelineStepperView(ctx, PIPELINE_TITLES, null);
            runningStepper.setCompleted(state.runningStage());
            Ui.add(running, runningStepper, 14);
            add(running);
        } else {
            AnalysisResult r = state.analysisResult();
            PreprocessedWindow pre = state.preprocessed(r.aircraftId, r.condition);
            boolean animate = firstTime("result-" + state.analysisRun());
            add(focusTarget("score", scorePanel(r, animate)));
            add(focusTarget("risk", riskPanel(r, animate)));
            add(focusTarget("rul", rulPanel(r, pre, animate)));
            add(focusTarget("recommendation", recommendationPanel(r)));
            add(explanationPanel(r, animate));
            add(componentTable(r));
            add(tracePanel(r));
        }
        add(Ui.prototypeNote(ctx, "Prototype / simulated AI inference. No trained aviation model is running: the pipeline "
                + "applies transparent statistical rules to simulated telemetry. A production system would use a "
                + "time-series neural network (for example an LSTM or an autoencoder) trained on real fleet data."));
    }

    private View scorePanel(AnalysisResult r, boolean animate) {
        PanelView p = new PanelView(ctx).setAccent(Palette.risk(r.risk));
        p.setHighlighted(isFocused("score"));
        p.addView(Ui.text(ctx, "Anomaly score", Type.Style.TITLE_MEDIUM, Palette.TEXT_MUTED));
        LinearLayout row = Ui.row(ctx);
        DialView dial = new DialView(ctx).setRange(0f, 1f).setBands(anomalyBands()).setStrokeDp(10);
        dial.setValue((float) r.anomalyScore, animate);
        LinearLayout readout = Ui.column(ctx);
        readout.setGravity(Gravity.CENTER_HORIZONTAL);
        readout.addView(Ui.text(ctx, Format.number(r.anomalyScore, 2), Type.Style.READOUT_MEDIUM, Palette.risk(r.risk)));
        readout.addView(Ui.text(ctx, "of 1.00", Type.Style.LABEL_SMALL, Palette.TEXT_MUTED));
        row.addView(Ui.dialBox(ctx, dial, readout, 4), Ui.size(ctx, 168, 168));
        row.addView(Ui.hspace(ctx, 14));
        LinearLayout col = Ui.column(ctx);
        String verdict = r.anomalyScore >= InferenceEngine.HIGH_THRESHOLD ? "Strong anomaly"
                : r.anomalyScore >= InferenceEngine.ALERT_THRESHOLD ? "Anomaly detected" : "Within normal behaviour";
        col.addView(Ui.text(ctx, verdict, Type.Style.TITLE_SMALL, Palette.TEXT));
        Ui.add(col, Ui.text(ctx, "Deviation from the learned fleet baseline: " + Format.number(r.deviationIndex, 1)
                + "σ. Alert threshold " + Format.number(InferenceEngine.ALERT_THRESHOLD, 2) + ", high "
                + Format.number(InferenceEngine.HIGH_THRESHOLD, 2) + ".", Type.Style.BODY_SMALL, Palette.TEXT_MUTED), 4);
        Ui.add(col, Ui.text(ctx, r.samplesAnalysed + " samples analysed, " + r.spikesRemoved + " sensor spikes removed",
                Type.Style.BODY_SMALL, Palette.TEXT_FAINT), 6);
        row.addView(col, Ui.weight(1f));
        p.addView(row, Ui.matchWrap());
        return p;
    }

    private View riskPanel(AnalysisResult r, boolean animate) {
        boolean low = r.risk == RiskLevel.LOW;
        int color = Palette.risk(r.risk);
        PanelView p = new PanelView(ctx);
        p.setHighlighted(isFocused("risk"));
        LinearLayout top = new LinearLayout(ctx);
        LinearLayout left = Ui.column(ctx);
        left.addView(Ui.text(ctx, "Failure risk", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED));
        left.addView(Ui.text(ctx, r.risk.label, Type.Style.READOUT, color));
        top.addView(left, Ui.weight(1f));
        LinearLayout right = Ui.column(ctx);
        right.setGravity(Gravity.END);
        right.addView(Ui.text(ctx, "Confidence", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED));
        right.addView(Ui.text(ctx, Format.number(r.confidence * 100, 1) + "%", Type.Style.READOUT_MEDIUM, Palette.TEXT));
        top.addView(right);
        p.addView(top, Ui.matchWrap());
        View divider = Ui.divider(ctx, Palette.HAIRLINE);
        LinearLayout.LayoutParams dlp = (LinearLayout.LayoutParams) divider.getLayoutParams();
        dlp.topMargin = dp(10);
        dlp.bottomMargin = dp(10);
        p.addView(divider, dlp);
        p.addView(Ui.text(ctx, "Component", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED));
        p.addView(Ui.text(ctx, low ? "No component at risk" : r.primary.component.label, Type.Style.HEADLINE_SMALL, Palette.TEXT));
        Ui.add(p, Ui.text(ctx, "Predicted condition", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED), 8);
        p.addView(Ui.text(ctx, r.primary.condition, Type.Style.TITLE_MEDIUM, low ? Palette.GREEN : color));
        if (!low) {
            LinearLayout hrow = Ui.row(ctx);
            hrow.addView(Ui.text(ctx, "Component health", Type.Style.BODY_SMALL, Palette.TEXT_MUTED), Ui.weight(1f));
            hrow.addView(Ui.text(ctx, r.primary.health + "%", Type.Style.NUMERIC, Palette.health(r.primary.health)));
            Ui.add(p, hrow, 10);
            Ui.add(p, new BarView(ctx, 8).setColor(Palette.health(r.primary.health))
                    .setFraction(r.primary.health / 100f, animate), 4);
            if (r.onsetHoursAgo != null) {
                Ui.add(p, Ui.text(ctx, "Degradation onset detected about " + r.onsetHoursAgo
                        + " operating hours ago (CUSUM change-point).", Type.Style.BODY_SMALL, Palette.TEXT_MUTED), 8);
            }
        }
        return p;
    }

    private View rulPanel(AnalysisResult r, PreprocessedWindow pre, boolean animate) {
        ComponentAssessment p = r.primary;
        boolean low = r.risk == RiskLevel.LOW;
        PanelView panel = new PanelView(ctx);
        panel.setHighlighted(isFocused("rul"));
        panel.addView(Ui.text(ctx, "Estimated remaining useful life", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED));
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.BOTTOM);
        row.addView(Ui.text(ctx, p.rulHours >= 999 ? "999+" : Integer.toString(p.rulHours), Type.Style.READOUT, Palette.MAGENTA));
        row.addView(Ui.hspace(ctx, 8));
        TextView unit = Ui.text(ctx, "operating hours", Type.Style.TITLE_MEDIUM, Palette.TEXT);
        unit.setPadding(0, 0, 0, dp(8));
        row.addView(unit);
        panel.addView(row, Ui.matchWrap());
        String text = low
                ? "No degradation trend. Shortest projected margin: " + p.component.label + ", at the minimum wear rate."
                : p.indicatorName + " " + Format.number(p.indicatorValue, p.indicatorDecimals) + " " + p.indicatorUnit
                + ", rising " + Format.number(p.trendPerHour, p.indicatorDecimals + 2) + " " + p.indicatorUnit
                + " per hour (R² " + Format.number(p.trendR2, 2) + "). Projected to reach the "
                + Format.number(p.indicatorLimit, p.indicatorDecimals) + " " + p.indicatorUnit + " limit in "
                + InferenceEngine.rulText(p.rulHours) + ".";
        panel.addView(Ui.text(ctx, text, Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
        if (!low) {
            ProjectionChartView chart = new ProjectionChartView(ctx).setData(
                    InferenceEngine.indicatorHistory(p.component, pre), p.indicatorValue,
                    InferenceEngine.indicatorNominal(p.component), p.indicatorLimit, p.rulHours,
                    p.indicatorDecimals, p.indicatorUnit);
            chart.animateGrow(animate);
            Ui.add(panel, chart, 10);
        }
        return panel;
    }

    private View recommendationPanel(final AnalysisResult r) {
        boolean low = r.risk == RiskLevel.LOW;
        PanelView p = new PanelView(ctx).setAccent(low ? Palette.GREEN : Palette.CYAN);
        p.setHighlighted(isFocused("recommendation"));
        p.addView(Ui.text(ctx, "AI recommendation", Type.Style.LABEL_MEDIUM, Palette.CYAN));
        Ui.add(p, Ui.text(ctx, "“" + r.primary.recommendation + "”", Type.Style.TITLE_LARGE, Palette.TEXT), 4);
        Ui.add(p, Ui.text(ctx, "Work package", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED), 10);
        for (String w : r.primary.workPackage) p.addView(Ui.bullet(ctx, w, Palette.CYAN), Ui.matchWrap());
        if (!low) {
            Ui.add(p, Ui.primary(ctx, "Create alert", R.drawable.ic_notifications_active, Palette.ORANGE, v -> {
                activity.requestNotifications();
                state.createAlert(r, true);
            }), 12);
            View schedule = Ui.secondary(ctx, "Schedule work", R.drawable.ic_build, v -> state.scheduleMaintenance(r));
            View report = Ui.secondary(ctx, "Report", R.drawable.ic_description, v -> {
                state.navigate(Screen.REPORTS);
                state.generateReport();
            });
            Ui.add(p, Ui.equalRow(ctx, 8, schedule, report), 8);
        } else {
            Ui.add(p, Ui.secondary(ctx, "Generate report", R.drawable.ic_description, v -> {
                state.navigate(Screen.REPORTS);
                state.generateReport();
            }), 12);
        }
        return p;
    }

    private View explanationPanel(AnalysisResult r, boolean animate) {
        PanelView p = new PanelView(ctx);
        p.addView(Ui.text(ctx, "Why the model reached this result", Type.Style.TITLE_MEDIUM, Palette.TEXT));
        Ui.add(p, Ui.text(ctx, r.explanation, Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED), 6);
        if (r.risk != RiskLevel.LOW) {
            Ui.add(p, Ui.text(ctx, "Evidence by sensor for " + r.primary.component.label, Type.Style.LABEL_MEDIUM,
                    Palette.TEXT_MUTED), 12);
            for (Contribution c : r.primary.contributions) {
                LinearLayout row = Ui.row(ctx);
                row.setPadding(0, dp(3), 0, dp(3));
                row.addView(Ui.singleLine(ctx, c.sensor.shortLabel, Type.Style.BODY_SMALL, Palette.TEXT), Ui.width(ctx, 96));
                BarView bar = new BarView(ctx, 6).setColor(Palette.CYAN).setFraction((float) c.share, animate);
                row.addView(bar, Ui.weight(1f));
                TextView pct = Ui.text(ctx, (int) (c.share * 100) + "%", Type.Style.NUMERIC, Palette.CYAN);
                pct.setGravity(Gravity.END);
                row.addView(pct, Ui.width(ctx, 48));
                Ui.add(p, row, 0);
            }
        }
        return p;
    }

    private View componentTable(AnalysisResult r) {
        PanelView p = new PanelView(ctx, 0);
        TextView title = Ui.text(ctx, "All components", Type.Style.TITLE_MEDIUM, Palette.TEXT);
        title.setPadding(dp(16), dp(14), dp(16), dp(8));
        p.addView(title);
        LinearLayout head = Ui.row(ctx);
        head.setPadding(dp(16), dp(4), dp(16), dp(4));
        head.addView(Ui.text(ctx, "Component", Type.Style.LABEL_SMALL, Palette.TEXT_FAINT), Ui.weight(1.6f));
        head.addView(Ui.text(ctx, "Health", Type.Style.LABEL_SMALL, Palette.TEXT_FAINT), Ui.weight(0.8f));
        head.addView(Ui.text(ctx, "Risk", Type.Style.LABEL_SMALL, Palette.TEXT_FAINT), Ui.weight(1f));
        head.addView(Ui.text(ctx, "RUL", Type.Style.LABEL_SMALL, Palette.TEXT_FAINT), Ui.weight(0.8f));
        p.addView(head, Ui.matchWrap());
        List<ComponentAssessment> rows = new ArrayList<>(r.components);
        Collections.sort(rows, InferenceEngine.RISK_THEN_RUL);
        for (ComponentAssessment c : rows) {
            p.addView(Ui.divider(ctx, Palette.HAIRLINE));
            LinearLayout row = Ui.row(ctx);
            row.setPadding(dp(16), dp(10), dp(16), dp(10));
            row.addView(Ui.text(ctx, c.component.label, Type.Style.BODY_MEDIUM, Palette.TEXT), Ui.weight(1.6f));
            row.addView(Ui.text(ctx, c.health + "%", Type.Style.NUMERIC, Palette.health(c.health)), Ui.weight(0.8f));
            FrameLayout tagBox = new FrameLayout(ctx);
            tagBox.addView(Ui.tag(ctx, c.risk.label, Palette.risk(c.risk)));
            row.addView(tagBox, Ui.weight(1f));
            row.addView(Ui.text(ctx, InferenceEngine.rulText(c.rulHours), Type.Style.NUMERIC, Palette.TEXT), Ui.weight(0.8f));
            p.addView(row, Ui.matchWrap());
        }
        p.addView(Ui.vspace(ctx, 6));
        return p;
    }

    private View tracePanel(AnalysisResult r) {
        PanelView p = new PanelView(ctx);
        LinearLayout head = Ui.row(ctx);
        LinearLayout col = Ui.column(ctx);
        col.addView(Ui.text(ctx, "Pipeline trace", Type.Style.TITLE_MEDIUM, Palette.TEXT));
        col.addView(Ui.text(ctx, "Completed " + state.analysisCompletedAt() + ", "
                        + (r.condition == DataCondition.NORMAL ? "normal" : "abnormal") + " data",
                Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
        head.addView(col, Ui.weight(1f));
        head.addView(Ui.icon(ctx, traceOpen ? R.drawable.ic_expand_less : R.drawable.ic_expand_more, Palette.CYAN, 24));
        Ui.tappable(head, 8, v -> {
            traceOpen = !traceOpen;
            render();
        });
        p.addView(head, Ui.matchWrap());
        if (traceOpen) {
            List<String> titles = new ArrayList<>();
            List<String> details = new ArrayList<>();
            for (PipelineStage s : r.stages) {
                titles.add(s.title);
                details.add(s.detail);
            }
            PipelineStepperView stepper = new PipelineStepperView(ctx, titles, details);
            stepper.setCompleted(titles.size());
            Ui.add(p, stepper, 12);
        }
        return p;
    }
}
