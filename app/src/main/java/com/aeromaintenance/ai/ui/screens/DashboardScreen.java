package com.aeromaintenance.ai.ui.screens;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.Destination;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.Screen;
import com.aeromaintenance.ai.data.Aircraft;
import com.aeromaintenance.ai.data.Alert;
import com.aeromaintenance.ai.data.FleetRepository;
import com.aeromaintenance.ai.data.FleetStatus;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.data.Severity;
import com.aeromaintenance.ai.engine.AnalysisResult;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.DialView;
import com.aeromaintenance.ai.ui.widget.PanelView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fleet overview: health dial, status counts, active alerts, quick actions, the AI pipeline. */
public class DashboardScreen extends BaseScreen {

    /** The end-to-end workflow from the brief (§2), in order. */
    static final String[] WORKFLOW = {
            "Sensor data", "Preprocessing", "AI/ML analysis", "Anomaly detection", "Health assessment",
            "Failure risk", "RUL estimate", "Recommendation", "Mobile alert"};

    public DashboardScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return change == AppState.Change.ALERTS;
    }

    static String healthWord(int h) {
        if (h >= 80) return "GOOD";
        if (h >= 60) return "FAIR";
        return "POOR";
    }

    static String severityTag(Severity s) {
        switch (s) {
            case CRITICAL:
                return "HIGH RISK";
            case WARNING:
                return "WARNING";
            default:
                return "MONITOR";
        }
    }

    @Override
    protected void build() {
        int health = FleetRepository.fleetHealthScore();

        // ---- Hero: overall fleet health -----------------------------------
        PanelView hero = new PanelView(ctx, 18);
        hero.addView(Ui.text(ctx, "Overall fleet health", Type.Style.TITLE_MEDIUM, Palette.TEXT_MUTED));
        DialView dial = new DialView(ctx).setRange(0, 100).setBands(DialView.healthBands()).setStrokeDp(12);
        dial.setValue(health, firstTime("fleet-dial"));
        LinearLayout readout = Ui.column(ctx);
        readout.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout numbers = Ui.bottomRow(ctx);
        numbers.addView(Ui.text(ctx, Integer.toString(health), Type.Style.READOUT, Palette.TEXT));
        TextView of = Ui.text(ctx, " / 100", Type.Style.READOUT_SMALL, Palette.TEXT_MUTED);
        of.setPadding(0, 0, 0, dp(6));
        numbers.addView(of);
        readout.addView(numbers);
        int hc = health >= 80 ? Palette.GREEN : health >= 60 ? Palette.ORANGE : Palette.RED;
        LinearLayout.LayoutParams tlp = Ui.wrap();
        tlp.topMargin = dp(2);
        readout.addView(Ui.tag(ctx, healthWord(health), hc), tlp);
        FrameLayout box = Ui.dialBox(ctx, dial, readout, 6);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(230), LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.CENTER_HORIZONTAL;
        hero.addView(box, blp);
        hero.addView(Ui.vspace(ctx, 14));
        hero.addView(Ui.equalRow(ctx, 8,
                fleetCount("Total", state.fleet().size(), Palette.CYAN),
                fleetCount("Healthy", FleetRepository.countByStatus(FleetStatus.HEALTHY), Palette.GREEN),
                fleetCount("Warning", FleetRepository.countByStatus(FleetStatus.WARNING), Palette.ORANGE),
                fleetCount("Critical", FleetRepository.countByStatus(FleetStatus.CRITICAL), Palette.RED)));
        add(hero);

        // ---- Demo buttons ---------------------------------------------------
        LinearLayout demoRow = Ui.row(ctx);
        View start = Ui.primary(ctx, "Start demo", R.drawable.ic_play, v -> {
            activity.requestNotifications();
            state.startDemo();
        });
        View faculty = Ui.secondary(ctx, "Faculty mode", R.drawable.ic_school, v -> state.navigate(Screen.FACULTY_DEMO));
        demoRow.addView(start, new LinearLayout.LayoutParams(0, dp(52), 1.2f));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        flp.leftMargin = dp(10);
        demoRow.addView(faculty, flp);
        add(demoRow);

        // ---- Active alerts --------------------------------------------------
        add(Ui.sectionHeader(ctx, "Active alerts", null, "View all", v -> state.navigate(Screen.ALERTS)));
        List<Alert> active = new ArrayList<>();
        for (Alert a : state.alerts()) {
            if (!a.acknowledged) active.add(a);
        }
        Collections.sort(active, (a, b) -> Integer.compare(b.severity.rank, a.severity.rank));
        PanelView alertsPanel = new PanelView(ctx, 0);
        if (active.isEmpty()) {
            TextView none = Ui.text(ctx, "No open alerts. Every alert has been acknowledged.", Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED);
            none.setPadding(dp(16), dp(16), dp(16), dp(16));
            alertsPanel.addView(none);
        }
        for (int i = 0; i < Math.min(3, active.size()); i++) {
            final Alert a = active.get(i);
            if (i > 0) alertsPanel.addView(Ui.divider(ctx, Palette.HAIRLINE));
            LinearLayout row = Ui.row(ctx);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            View bar = new View(ctx);
            bar.setBackground(Ui.rounded(ctx, Palette.severity(a.severity), 2, 0, 0));
            row.addView(bar, Ui.size(ctx, 4, 36));
            row.addView(Ui.hspace(ctx, 12));
            LinearLayout col = Ui.column(ctx);
            col.addView(Ui.text(ctx, a.component, Type.Style.TITLE_SMALL, Palette.TEXT));
            col.addView(Ui.singleLine(ctx, a.aircraftId + ", " + Ui.sentence(a.title), Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
            row.addView(col, Ui.weight(1f));
            row.addView(Ui.hspace(ctx, 8));
            row.addView(Ui.tag(ctx, severityTag(a.severity), Palette.severity(a.severity)));
            Ui.tappable(row, 0, v -> {
                state.setExpandedAlert(a.id);
                state.navigate(Screen.ALERTS);
            });
            alertsPanel.addView(row, Ui.matchWrap());
        }
        add(alertsPanel, 4);

        // ---- Quick actions --------------------------------------------------
        add(Ui.sectionHeader(ctx, "Quick actions", null));
        add(Ui.equalRow(ctx, 10,
                quickAction("Aircraft", R.drawable.ic_flight, 0, v -> state.navigate(Screen.FLEET)),
                quickAction("Sensor monitoring", R.drawable.ic_monitor_heart, 0, v -> state.navigate(Screen.MONITORING)),
                quickAction("AI prediction", R.drawable.ic_psychology, 0, v -> state.navigate(Screen.ANALYSIS))), 4);
        add(Ui.equalRow(ctx, 10,
                quickAction("Maintenance", R.drawable.ic_build, 0, v -> state.openMaintenance(0)),
                quickAction("Alerts", R.drawable.ic_notifications, state.unacknowledgedCount(), v -> state.navigate(Screen.ALERTS)),
                quickAction("Reports", R.drawable.ic_description, 0, v -> state.navigate(Screen.REPORTS))), 10);

        // ---- Aircraft needing attention ------------------------------------
        add(Ui.sectionHeader(ctx, "Aircraft needing attention", "Latest AI assessment of each tail"));
        List<AnalysisResult> flagged = new ArrayList<>();
        for (AnalysisResult r : state.fleetPredictions()) {
            if (r.risk != RiskLevel.LOW) flagged.add(r);
        }
        Collections.sort(flagged, (a, b) -> a.risk.rank != b.risk.rank
                ? Integer.compare(b.risk.rank, a.risk.rank) : Integer.compare(a.primary.rulHours, b.primary.rulHours));
        PanelView attention = new PanelView(ctx, 0);
        for (int i = 0; i < flagged.size(); i++) {
            final AnalysisResult r = flagged.get(i);
            final Aircraft a = state.aircraft(r.aircraftId);
            if (i > 0) attention.addView(Ui.divider(ctx, Palette.HAIRLINE));
            LinearLayout row = Ui.row(ctx);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout col = Ui.column(ctx);
            col.addView(Ui.text(ctx, a.id + "  " + a.model, Type.Style.TITLE_SMALL, Palette.TEXT));
            col.addView(Ui.text(ctx, r.primary.component.label, Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
            row.addView(col, Ui.weight(1f));
            LinearLayout right = Ui.column(ctx);
            right.setGravity(Gravity.END);
            right.addView(Ui.text(ctx, "RUL " + InferenceEngine.rulText(r.primary.rulHours), Type.Style.NUMERIC, Palette.risk(r.risk)));
            LinearLayout.LayoutParams rtlp = Ui.wrap();
            rtlp.topMargin = dp(3);
            right.addView(Ui.tag(ctx, r.risk.label, Palette.risk(r.risk)), rtlp);
            row.addView(right);
            Ui.tappable(row, 0, v -> {
                state.selectAircraft(a.id);
                state.navigate(Destination.detail(a.id));
            });
            attention.addView(row, Ui.matchWrap());
        }
        add(attention, 4);

        // ---- Workflow -------------------------------------------------------
        add(Ui.sectionHeader(ctx, "How the AI pipeline works", "Telemetry to maintenance action in nine stages",
                "Details", v -> state.navigate(Screen.INSIGHTS)));
        PanelView workflow = new PanelView(ctx, 12);
        workflow.addView(workflowStrip());
        workflow.onTap(v -> state.navigate(Screen.INSIGHTS));
        add(workflow, 4);
    }

    private View fleetCount(String label, int value, int color) {
        LinearLayout col = Ui.column(ctx);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setBackground(Ui.rounded(ctx, Palette.NIGHT, 10, Palette.HAIRLINE, 1));
        col.setPadding(dp(4), dp(10), dp(4), dp(10));
        col.addView(Ui.text(ctx, Integer.toString(value), Type.Style.READOUT_MEDIUM, color));
        TextView l = Ui.singleLine(ctx, label, Type.Style.LABEL_SMALL, Palette.TEXT_MUTED);
        l.setGravity(Gravity.CENTER);
        col.addView(l);
        Ui.tappable(col, 10, v -> state.navigate(Screen.FLEET));
        return col;
    }

    private View quickAction(String label, int icon, int badge, View.OnClickListener onClick) {
        LinearLayout col = Ui.column(ctx);
        col.setGravity(Gravity.CENTER);
        col.setBackground(Ui.rounded(ctx, Palette.PANEL, 12, Palette.HAIRLINE, 1));
        col.setPadding(dp(8), dp(10), dp(8), dp(10));
        col.setMinimumHeight(dp(92));
        FrameLayout iconBox = new FrameLayout(ctx);
        iconBox.addView(Ui.icon(ctx, icon, Palette.CYAN, 26), new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        if (badge > 0) {
            TextView b = Ui.text(ctx, Integer.toString(badge), Type.Style.TAG, Palette.NIGHT);
            b.setGravity(Gravity.CENTER);
            b.setBackground(Ui.rounded(ctx, Palette.RED, 8, 0, 0));
            b.setMinWidth(dp(16));
            b.setPadding(dp(3), dp(1), dp(3), dp(1));
            iconBox.addView(b, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END));
        }
        col.addView(iconBox, Ui.size(ctx, 40, 28));
        TextView l = Ui.text(ctx, label, Type.Style.LABEL_MEDIUM, Palette.TEXT);
        l.setGravity(Gravity.CENTER);
        l.setMaxLines(2);
        LinearLayout.LayoutParams llp = Ui.wrap();
        llp.topMargin = dp(8);
        col.addView(l, llp);
        Ui.tappable(col, 12, onClick);
        return col;
    }

    private View workflowStrip() {
        HorizontalScrollView scroll = new HorizontalScrollView(ctx);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.topRow(ctx);
        row.setPadding(0, dp(4), 0, dp(4));
        for (int i = 0; i < WORKFLOW.length; i++) {
            LinearLayout item = Ui.column(ctx);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView num = Ui.text(ctx, Integer.toString(i + 1), Type.Style.TAG, Palette.CYAN);
            num.setGravity(Gravity.CENTER);
            boolean last = i == WORKFLOW.length - 1;
            android.graphics.drawable.GradientDrawable circle = Ui.circle(last ? Palette.alpha(Palette.CYAN, 0.2f) : Palette.PANEL_RAISED);
            circle.setStroke(dp(1), Palette.alpha(Palette.CYAN, 0.6f));
            num.setBackground(circle);
            item.addView(num, Ui.size(ctx, 28, 28));
            TextView label = Ui.text(ctx, WORKFLOW[i], Type.Style.LABEL_SMALL, Palette.TEXT_MUTED);
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            label.setLines(2);
            LinearLayout.LayoutParams llp = Ui.matchWrap();
            llp.topMargin = dp(6);
            item.addView(label, llp);
            row.addView(item, new LinearLayout.LayoutParams(dp(78), LinearLayout.LayoutParams.WRAP_CONTENT));
            if (!last) {
                View connector = new View(ctx);
                connector.setBackgroundColor(Palette.HAIRLINE_STRONG);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(14), Math.max(1, dp(1)));
                clp.topMargin = dp(14);
                row.addView(connector, clp);
            }
        }
        scroll.addView(row);
        return scroll;
    }
}
