package com.aeromaintenance.ai.ui.screens;

import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.data.Aircraft;
import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.engine.AnalysisResult;
import com.aeromaintenance.ai.engine.ComponentAssessment;
import com.aeromaintenance.ai.engine.EvidenceRow;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.engine.MaintenanceReport;
import com.aeromaintenance.ai.engine.ReportGenerator;
import com.aeromaintenance.ai.ui.Controls;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.ButtonView;
import com.aeromaintenance.ai.ui.widget.PanelView;

/** "Generate Maintenance Report": builds and shows a formatted, shareable report. */
public class ReportsScreen extends BaseScreen {

    public ReportsScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return change == AppState.Change.SELECTION || change == AppState.Change.REPORT;
    }

    @Override
    protected void build() {
        final String id = state.selectedId();
        DataCondition cond = state.conditionOf(id);
        AppState.ReportPhase phase = state.reportPhase();

        LinearLayout controls = Ui.column(ctx);
        controls.addView(Controls.aircraftSelector(ctx, state), Ui.matchWrap());
        Ui.add(controls, Controls.conditionToggle(ctx, cond, c -> state.setCondition(id, c)), 10);
        boolean ready = phase == AppState.ReportPhase.READY;
        ButtonView generate = Ui.primary(ctx, ready ? "Generate again" : "Generate maintenance report",
                ready ? R.drawable.ic_refresh : R.drawable.ic_description, v -> state.generateReport());
        generate.setEnabled(phase != AppState.ReportPhase.GENERATING);
        Ui.add(controls, generate, 10);
        add(controls);

        if (phase == AppState.ReportPhase.IDLE) {
            PanelView p = new PanelView(ctx);
            p.addView(Ui.text(ctx, "No report yet", Type.Style.TITLE_MEDIUM, Palette.TEXT));
            Ui.add(p, Ui.text(ctx, "The report combines the aircraft record, the AI finding, the sensor evidence and the "
                    + "recommended work package for " + id + ". It can be shared from the phone as text.",
                    Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED), 4);
            add(p);
        } else if (phase == AppState.ReportPhase.GENERATING) {
            PanelView p = new PanelView(ctx);
            for (int i = 0; i < AppState.REPORT_STEPS.size(); i++) {
                LinearLayout row = Ui.row(ctx);
                row.setPadding(0, dp(6), 0, dp(6));
                FrameLayout mark = new FrameLayout(ctx);
                int step = state.reportStep();
                if (i < step) {
                    mark.addView(Ui.icon(ctx, R.drawable.ic_check, Palette.GREEN, 20),
                            new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
                } else if (i == step) {
                    ProgressBar spinner = new ProgressBar(ctx);
                    spinner.setIndeterminate(true);
                    spinner.setIndeterminateTintList(ColorStateList.valueOf(Palette.CYAN));
                    mark.addView(spinner, new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
                } else {
                    mark.addView(Ui.dot(ctx, Palette.HAIRLINE, 8), new FrameLayout.LayoutParams(dp(8), dp(8), Gravity.CENTER));
                }
                row.addView(mark, Ui.size(ctx, 22, 22));
                row.addView(Ui.hspace(ctx, 12));
                row.addView(Ui.text(ctx, AppState.REPORT_STEPS.get(i), Type.Style.BODY_MEDIUM,
                        i <= step ? Palette.TEXT : Palette.TEXT_FAINT));
                p.addView(row, Ui.matchWrap());
            }
            add(p);
        } else {
            final MaintenanceReport report = state.report();
            add(paper(report));
            add(Ui.secondary(ctx, "Share report", R.drawable.ic_share, v -> activity.shareText(
                    "Maintenance report " + report.reportNumber, ReportGenerator.toPlainText(report))));
        }
    }

    /** The report laid out like a printed document. */
    private View paper(MaintenanceReport r) {
        AnalysisResult res = r.result;
        ComponentAssessment p = res.primary;
        Aircraft a = r.aircraft;
        boolean low = res.risk == RiskLevel.LOW;
        int riskColor = Palette.paperRisk(res.risk);

        LinearLayout doc = Ui.column(ctx);
        doc.setBackground(Ui.rounded(ctx, Palette.PAPER, 6, 0, 0));
        doc.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout head = new LinearLayout(ctx);
        LinearLayout titles = Ui.column(ctx);
        titles.addView(Ui.text(ctx, "AeroMaintenance AI", Type.Style.LABEL_MEDIUM, Palette.INK_MUTED));
        titles.addView(Ui.text(ctx, "Predictive Maintenance Report", Type.Style.HEADLINE_SMALL, Palette.INK));
        head.addView(titles, Ui.weight(1f));
        TextView stamp = Ui.text(ctx, res.risk.label + " RISK", Type.Style.TAG, riskColor);
        stamp.setBackground(Ui.rounded(ctx, 0x00000000, 4, riskColor, 1.5f));
        stamp.setPadding(dp(8), dp(4), dp(8), dp(4));
        head.addView(stamp, Ui.wrap());
        doc.addView(head, Ui.matchWrap());
        Ui.add(doc, Ui.text(ctx, "Report " + r.reportNumber + ", generated " + r.generatedAt, Type.Style.BODY_SMALL,
                Palette.INK_MUTED), 4);
        doc.addView(Ui.text(ctx, "AI-assisted prediction from simulated sensor data. Prototype, not for operational use.",
                Type.Style.BODY_SMALL, Palette.PAPER_ORANGE));

        section(doc, "Aircraft");
        paperRow(doc, "Aircraft", a.id, Palette.INK, false);
        paperRow(doc, "Model", a.model, Palette.INK, false);
        paperRow(doc, "Flight hours", Format.number(a.flightHours, 0), Palette.INK, false);
        paperRow(doc, "Flight cycles", Format.number(a.flightCycles, 0), Palette.INK, false);
        paperRow(doc, "Last maintenance", a.lastMaintenance, Palette.INK, false);
        paperRow(doc, "Data analysed", res.condition.label + ", " + res.samplesAnalysed + " samples", Palette.INK, false);

        section(doc, "AI finding");
        paperRow(doc, "Detected component", low ? "None" : p.component.label, Palette.INK, true);
        paperRow(doc, "Risk", res.risk.label, riskColor, true);
        paperRow(doc, "Anomaly score", Format.number(res.anomalyScore, 2), Palette.INK, false);
        paperRow(doc, "Estimated RUL", InferenceEngine.rulText(p.rulHours) + " (operating hours)", Palette.INK, false);
        paperRow(doc, "Confidence", Format.number(res.confidence * 100, 1) + "%", Palette.INK, false);
        paperRow(doc, "Predicted condition", p.condition, Palette.INK, false);
        Ui.add(doc, Ui.text(ctx, r.summary, Type.Style.BODY_MEDIUM, Palette.INK), 6);

        section(doc, "AI recommendation");
        doc.addView(Ui.text(ctx, p.recommendation, Type.Style.TITLE_MEDIUM, Palette.INK));
        for (int i = 0; i < p.workPackage.size(); i++) {
            TextView t = Ui.text(ctx, (i + 1) + ". " + p.workPackage.get(i), Type.Style.BODY_MEDIUM, Palette.INK);
            t.setPadding(0, dp(2), 0, dp(2));
            Ui.add(doc, t, i == 0 ? 4 : 0);
        }

        section(doc, "Sensor evidence");
        LinearLayout ehead = new LinearLayout(ctx);
        ehead.setPadding(0, dp(4), 0, dp(4));
        paperHead(ehead, "Channel", 1.5f);
        paperHead(ehead, "Baseline", 1f);
        paperHead(ehead, "Current", 1f);
        paperHead(ehead, "Trend", 1.1f);
        doc.addView(ehead, Ui.matchWrap());
        for (EvidenceRow e : r.evidence) {
            doc.addView(Ui.divider(ctx, Palette.PAPER_RULE));
            LinearLayout row = new LinearLayout(ctx);
            row.setPadding(0, dp(5), 0, dp(5));
            int c = e.flagged ? Palette.PAPER_RED : Palette.INK;
            row.addView(evidenceCell(e.sensor.shortLabel + " (" + e.sensor.unit + ")", c, e.flagged), Ui.weight(1.5f));
            row.addView(evidenceCell(e.baseline, Palette.INK_MUTED, false), Ui.weight(1f));
            row.addView(evidenceCell(e.current, c, e.flagged), Ui.weight(1f));
            row.addView(evidenceCell(e.trend, Palette.INK_MUTED, false), Ui.weight(1.1f));
            doc.addView(row, Ui.matchWrap());
        }
        Ui.add(doc, Ui.text(ctx, "Red rows deviate more than 3σ from the fleet baseline.", Type.Style.LABEL_SMALL,
                Palette.INK_MUTED), 4);

        section(doc, "Explanation");
        doc.addView(Ui.text(ctx, res.explanation, Type.Style.BODY_MEDIUM, Palette.INK));

        section(doc, "Component assessment");
        for (ComponentAssessment c : res.components) {
            paperRow(doc, c.component.label, c.health + "%, " + c.risk.label + ", RUL " + InferenceEngine.rulText(c.rulHours),
                    Palette.paperRisk(c.risk), false);
        }

        section(doc, "Engineering review");
        Ui.add(doc, Ui.text(ctx, "Reviewed by (licensed engineer): ______________________", Type.Style.BODY_MEDIUM,
                Palette.INK), 4);
        Ui.add(doc, Ui.text(ctx, "Date: ____________   Decision: accept / reject / defer", Type.Style.BODY_MEDIUM,
                Palette.INK), 10);
        Ui.add(doc, Ui.divider(ctx, Palette.PAPER_RULE), 14);
        Ui.add(doc, Ui.text(ctx, "Academic research prototype using simulated/synthetic data. Not certified for real-world "
                + "aviation safety, maintenance or flight-critical decision-making.", Type.Style.LABEL_SMALL, Palette.INK_MUTED), 8);
        return doc;
    }

    private void section(LinearLayout doc, String title) {
        Ui.add(doc, Ui.text(ctx, title, Type.Style.TITLE_MEDIUM, Palette.INK), 16);
        View rule = new View(ctx);
        rule.setBackgroundColor(Palette.INK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1.5f)));
        lp.topMargin = dp(2);
        lp.bottomMargin = dp(6);
        doc.addView(rule, lp);
    }

    private void paperRow(LinearLayout doc, String label, String value, int valueColor, boolean bold) {
        LinearLayout row = new LinearLayout(ctx);
        row.setPadding(0, dp(3), 0, dp(3));
        row.addView(Ui.text(ctx, label, Type.Style.BODY_MEDIUM, Palette.INK_MUTED), Ui.weight(1f));
        TextView v = Ui.text(ctx, value, Type.Style.BODY_MEDIUM, valueColor);
        if (bold) v.setTypeface(Type.font(ctx, R.font.barlow_semibold));
        row.addView(v, Ui.weight(1.3f));
        doc.addView(row, Ui.matchWrap());
    }

    private void paperHead(LinearLayout row, String text, float weight) {
        row.addView(Ui.text(ctx, text, Type.Style.LABEL_SMALL, Palette.INK_MUTED), Ui.weight(weight));
    }

    private TextView evidenceCell(String text, int color, boolean bold) {
        TextView t = Ui.text(ctx, text, Type.Style.BODY_SMALL, color);
        if (bold) t.setTypeface(Type.font(ctx, R.font.barlow_semibold));
        return t;
    }
}
