package com.aeromaintenance.ai.ui.screens;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.Destination;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.MaintenanceTask;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.data.TaskStatus;
import com.aeromaintenance.ai.engine.AnalysisResult;
import com.aeromaintenance.ai.engine.ComponentAssessment;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.ui.Controls;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.PanelView;
import com.aeromaintenance.ai.ui.widget.RiskChartView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Maintenance: the predictive-maintenance table and risk chart (tab 0) and the
 * work-order plan with Scheduled / In progress / Completed statuses (tab 1).
 */
public class MaintenanceScreen extends BaseScreen {

    private static final String[] TABS = {"Predictive maintenance", "Work orders"};
    private final TextView[] tabViews = new TextView[TABS.length];
    /** Work-order filter; null = all. */
    private TaskStatus filter;

    public MaintenanceScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return change == AppState.Change.SELECTION || change == AppState.Change.TASKS
                || change == AppState.Change.MAINTENANCE_TAB;
    }

    @Override
    protected View buildHeader() {
        LinearLayout wrap = Ui.column(ctx);
        wrap.setPadding(dp(16), dp(4), dp(16), dp(4));
        LinearLayout tabs = Ui.row(ctx);
        tabs.setBackground(Ui.rounded(ctx, Palette.PANEL, 12, Palette.HAIRLINE, 1));
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int i = 0; i < TABS.length; i++) {
            final int tab = i;
            TextView t = Ui.text(ctx, TABS[i], Type.Style.LABEL_LARGE, Palette.TEXT_MUTED);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(10), 0, dp(10));
            Ui.tappable(t, 9, v -> state.setMaintenanceTab(tab));
            tabs.addView(t, Ui.weight(1f));
            tabViews[i] = t;
        }
        wrap.addView(tabs, Ui.matchWrap());
        return wrap;
    }

    private void styleTabs() {
        for (int i = 0; i < tabViews.length; i++) {
            boolean sel = state.maintenanceTab() == i;
            tabViews[i].setTextColor(sel ? Palette.CYAN : Palette.TEXT_MUTED);
            tabViews[i].setBackground(sel ? Ui.rounded(ctx, Palette.alpha(Palette.CYAN, 0.16f), 9, 0, 0) : null);
        }
    }

    @Override
    protected void build() {
        styleTabs();
        if (state.maintenanceTab() == 0) buildPredictive();
        else buildPlanning();
    }

    // ------------------------------------------------------------ predictive

    private void buildPredictive() {
        final String id = state.selectedId();
        DataCondition cond = state.conditionOf(id);
        AnalysisResult result = state.resultFor(id, cond);
        List<ComponentAssessment> rows = new ArrayList<>(result.components);
        Collections.sort(rows, InferenceEngine.RISK_THEN_RUL);

        LinearLayout sel = Ui.column(ctx);
        sel.addView(Controls.aircraftSelector(ctx, state), Ui.matchWrap());
        Ui.add(sel, Controls.conditionToggle(ctx, cond, c -> state.setCondition(id, c)), 10);
        add(sel);

        add(Ui.sectionHeader(ctx, "Component predictions", id + ", " + cond.label.toLowerCase(Locale.ROOT)
                + ". Anomaly score " + Format.number(result.anomalyScore, 2) + "."));
        PanelView table = new PanelView(ctx, 0);
        LinearLayout head = Ui.row(ctx);
        head.setPadding(dp(14), dp(12), dp(14), dp(6));
        headerCell(head, "Component", 1.5f);
        headerCell(head, "Health", 0.75f);
        headerCell(head, "Risk", 0.95f);
        headerCell(head, "RUL", 0.75f);
        headerCell(head, "Action", 0.8f);
        table.addView(head, Ui.matchWrap());
        for (ComponentAssessment c : rows) {
            table.addView(Ui.divider(ctx, Palette.HAIRLINE));
            LinearLayout row = Ui.row(ctx);
            row.setPadding(dp(14), dp(11), dp(14), dp(11));
            row.addView(Ui.text(ctx, c.component.label, Type.Style.BODY_MEDIUM, Palette.TEXT), Ui.weight(1.5f));
            row.addView(Ui.text(ctx, c.health + "%", Type.Style.NUMERIC, Palette.health(c.health)), Ui.weight(0.75f));
            FrameLayout tagBox = new FrameLayout(ctx);
            tagBox.addView(Ui.tag(ctx, c.risk.label, Palette.risk(c.risk)));
            row.addView(tagBox, Ui.weight(0.95f));
            row.addView(Ui.text(ctx, InferenceEngine.rulText(c.rulHours), Type.Style.NUMERIC, Palette.TEXT), Ui.weight(0.75f));
            row.addView(Ui.text(ctx, c.shortAction, Type.Style.LABEL_LARGE, Palette.risk(c.risk)), Ui.weight(0.8f));
            table.addView(row, Ui.matchWrap());
        }
        add(table, 4);

        add(Ui.sectionHeader(ctx, "Risk chart",
                "Remaining useful life per component. Dashed lines are the HIGH and MEDIUM risk rules."));
        PanelView chartPanel = new PanelView(ctx, 14);
        chartPanel.addView(new RiskChartView(ctx).setComponents(rows, firstTime("risk-" + id + "-" + cond)), Ui.matchWrap());
        add(chartPanel, 4);

        add(Ui.sectionHeader(ctx, "Fleet watchlist",
                "Components at MEDIUM or HIGH risk across all 12 aircraft, soonest first"));
        List<Object[]> watch = new ArrayList<>();
        for (AnalysisResult r : state.fleetPredictions()) {
            for (ComponentAssessment c : r.components) {
                if (c.risk != RiskLevel.LOW) watch.add(new Object[]{r.aircraftId, c});
            }
        }
        Collections.sort(watch, (a, b) -> Integer.compare(((ComponentAssessment) a[1]).rulHours,
                ((ComponentAssessment) b[1]).rulHours));
        PanelView list = new PanelView(ctx, 0);
        for (int i = 0; i < watch.size(); i++) {
            final String aid = (String) watch.get(i)[0];
            ComponentAssessment c = (ComponentAssessment) watch.get(i)[1];
            if (i > 0) list.addView(Ui.divider(ctx, Palette.HAIRLINE));
            LinearLayout row = Ui.row(ctx);
            row.setPadding(dp(14), dp(11), dp(14), dp(11));
            LinearLayout col = Ui.column(ctx);
            col.addView(Ui.text(ctx, aid + "  " + c.component.label, Type.Style.TITLE_SMALL, Palette.TEXT));
            col.addView(Ui.text(ctx, "Health " + c.health + "%, " + c.shortAction.toLowerCase(Locale.ROOT),
                    Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
            row.addView(col, Ui.weight(1f));
            row.addView(Ui.text(ctx, InferenceEngine.rulText(c.rulHours), Type.Style.NUMERIC, Palette.risk(c.risk)));
            row.addView(Ui.hspace(ctx, 10));
            row.addView(Ui.tag(ctx, c.risk.label, Palette.risk(c.risk)));
            Ui.tappable(row, 0, v -> {
                state.selectAircraft(aid);
                state.navigate(Destination.detail(aid));
            });
            list.addView(row, Ui.matchWrap());
        }
        add(list, 4);
    }

    private void headerCell(LinearLayout row, String text, float weight) {
        row.addView(Ui.text(ctx, text, Type.Style.LABEL_SMALL, Palette.TEXT_FAINT), Ui.weight(weight));
    }

    // -------------------------------------------------------------- planning

    private void buildPlanning() {
        List<MaintenanceTask> all = state.tasks();
        List<View> pills = new ArrayList<>();
        pills.add(Ui.filterPill(ctx, "All " + all.size(), filter == null, Palette.CYAN, v -> setFilter(null)));
        for (final TaskStatus s : TaskStatus.values()) {
            int n = 0;
            for (MaintenanceTask t : all) {
                if (t.status == s) n++;
            }
            pills.add(Ui.filterPill(ctx, s.label + " " + n, filter == s, Palette.taskStatus(s), v -> setFilter(s)));
        }
        add(Ui.pillRow(ctx, pills));
        add(Ui.sectionHeader(ctx, "Upcoming maintenance",
                "Due times are operating hours, taken from the AI's remaining-useful-life estimate"));

        List<MaintenanceTask> shown = new ArrayList<>();
        for (MaintenanceTask t : all) {
            if (filter == null || t.status == filter) shown.add(t);
        }
        Collections.sort(shown, (a, b) -> {
            if (a.status != b.status) return Integer.compare(a.status.ordinal(), b.status.ordinal());
            int da = a.status == TaskStatus.COMPLETED ? 0 : a.dueHours;
            int db = b.status == TaskStatus.COMPLETED ? 0 : b.dueHours;
            return Integer.compare(da, db);
        });
        for (MaintenanceTask t : shown) {
            View card = taskCard(t);
            if (t.id.equals(state.highlightedTaskId())) focusTarget("task", card);
            add(card, shown.indexOf(t) == 0 ? 4 : 12);
        }
    }

    private void setFilter(TaskStatus s) {
        filter = s;
        render();
    }

    private View taskCard(final MaintenanceTask t) {
        int statusColor = Palette.taskStatus(t.status);
        PanelView p = new PanelView(ctx).setAccent(t.updatedByAiAt != null ? Palette.MAGENTA : null);
        p.setHighlighted(isFocused("task") && t.id.equals(state.highlightedTaskId()));
        LinearLayout top = new LinearLayout(ctx);
        LinearLayout col = Ui.column(ctx);
        col.addView(Ui.text(ctx, t.aircraftId, Type.Style.LABEL_LARGE, Palette.CYAN));
        col.addView(Ui.text(ctx, t.title, Type.Style.TITLE_MEDIUM, Palette.TEXT));
        col.addView(Ui.text(ctx, t.id + ", " + t.source.toLowerCase(Locale.ROOT), Type.Style.BODY_SMALL, Palette.TEXT_FAINT));
        top.addView(col, Ui.weight(1f));
        top.addView(Ui.tag(ctx, t.status.label.toUpperCase(Locale.ROOT), statusColor));
        p.addView(top, Ui.matchWrap());

        if (t.status != TaskStatus.COMPLETED) {
            LinearLayout due = new LinearLayout(ctx);
            due.setGravity(Gravity.BOTTOM);
            TextView dueIn = Ui.text(ctx, "Due in", Type.Style.BODY_SMALL, Palette.TEXT_MUTED);
            dueIn.setPadding(0, 0, 0, dp(4));
            due.addView(dueIn);
            due.addView(Ui.hspace(ctx, 6));
            due.addView(Ui.text(ctx, Integer.toString(t.dueHours), Type.Style.READOUT_MEDIUM, Palette.dueHours(t.dueHours)));
            due.addView(Ui.hspace(ctx, 6));
            TextView hours = Ui.text(ctx, "operating hours", Type.Style.BODY_SMALL, Palette.TEXT_MUTED);
            hours.setPadding(0, 0, 0, dp(4));
            due.addView(hours);
            Ui.add(p, due, 10);
        } else {
            Ui.add(p, Ui.text(ctx, "Completed and signed off", Type.Style.BODY_MEDIUM, Palette.GREEN), 10);
        }
        if (t.updatedByAiAt != null) {
            p.addView(Ui.text(ctx, "Updated by AI analysis at " + t.updatedByAiAt, Type.Style.BODY_SMALL, Palette.MAGENTA));
        }
        Ui.add(p, Ui.vspace(ctx, 0), 8);
        for (String w : t.workPackage) p.addView(Ui.bullet(ctx, w, statusColor), Ui.matchWrap());
        if (t.status != TaskStatus.COMPLETED) {
            boolean scheduled = t.status == TaskStatus.SCHEDULED;
            View b = Ui.primary(ctx, scheduled ? "Start work" : "Mark completed",
                    scheduled ? R.drawable.ic_play : R.drawable.ic_check_circle,
                    scheduled ? Palette.CYAN : Palette.GREEN, v -> state.advanceTask(t.id));
            b.setMinimumHeight(dp(46));
            Ui.add(p, b, 10);
        }
        return p;
    }
}
