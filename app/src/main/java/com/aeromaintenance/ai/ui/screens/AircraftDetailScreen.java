package com.aeromaintenance.ai.ui.screens;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.Screen;
import com.aeromaintenance.ai.data.Aircraft;
import com.aeromaintenance.ai.data.ComponentHealthRecord;
import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.SensorType;
import com.aeromaintenance.ai.data.TelemetryWindow;
import com.aeromaintenance.ai.engine.PreprocessedWindow;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.BarView;
import com.aeromaintenance.ai.ui.widget.DialView;
import com.aeromaintenance.ai.ui.widget.PanelView;
import com.aeromaintenance.ai.ui.widget.TrendChartView;

/** Health page of one aircraft: information, component health and its vibration trend. */
public class AircraftDetailScreen extends BaseScreen {

    private final String id;

    public AircraftDetailScreen(MainActivity activity, String id) {
        super(activity);
        this.id = id;
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return change == AppState.Change.SELECTION;
    }

    @Override
    protected void build() {
        Aircraft a = state.aircraft(id);
        DataCondition cond = state.conditionOf(id);
        PreprocessedWindow pre = state.preprocessed(id, cond);
        TelemetryWindow window = state.telemetry(id, cond);

        // ---- Aircraft information -------------------------------------------
        PanelView header = new PanelView(ctx).setAccent(Palette.fleetStatus(a.status));
        LinearLayout top = Ui.row(ctx);
        LinearLayout info = Ui.column(ctx);
        info.addView(Ui.text(ctx, "Aircraft information", Type.Style.LABEL_MEDIUM, Palette.TEXT_MUTED));
        info.addView(Ui.text(ctx, a.id, Type.Style.DISPLAY_SMALL, Palette.TEXT));
        info.addView(Ui.text(ctx, a.model + ", " + a.base, Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED));
        LinearLayout tags = Ui.row(ctx);
        tags.addView(Ui.tag(ctx, a.status.label.toUpperCase(java.util.Locale.ROOT), Palette.fleetStatus(a.status)));
        tags.addView(Ui.hspace(ctx, 6));
        tags.addView(Ui.tag(ctx, a.risk.label + " RISK", Palette.risk(a.risk)));
        LinearLayout.LayoutParams tlp = Ui.wrap();
        tlp.topMargin = dp(8);
        info.addView(tags, tlp);
        top.addView(info, Ui.weight(1f));

        DialView dial = new DialView(ctx).setRange(0, 100).setBands(DialView.healthBands()).setStrokeDp(7).setTicks(4);
        dial.setValue(a.healthScore, firstTime("health-dial"));
        LinearLayout readout = Ui.column(ctx);
        readout.setGravity(Gravity.CENTER_HORIZONTAL);
        readout.addView(Ui.text(ctx, Integer.toString(a.healthScore), Type.Style.READOUT_SMALL, Palette.health(a.healthScore)));
        readout.addView(Ui.text(ctx, "health", Type.Style.LABEL_SMALL, Palette.TEXT_MUTED));
        top.addView(Ui.dialBox(ctx, dial, readout, 0), Ui.size(ctx, 112, 112));
        header.addView(top, Ui.matchWrap());

        LinearLayout facts = Ui.row(ctx);
        facts.setGravity(Gravity.TOP);
        facts.addView(Ui.keyValue(ctx, "Flight hours", Format.number(a.flightHours, 0), Palette.TEXT), Ui.weight(1f));
        facts.addView(Ui.keyValue(ctx, "Flight cycles", Format.number(a.flightCycles, 0), Palette.TEXT), Ui.weight(1f));
        facts.addView(Ui.keyValue(ctx, "Last maintenance", a.lastMaintenance, Palette.TEXT), Ui.weight(1.3f));
        LinearLayout.LayoutParams flp = Ui.matchWrap();
        flp.topMargin = dp(14);
        header.addView(facts, flp);
        add(header);

        // ---- Component health ------------------------------------------------
        add(Ui.sectionHeader(ctx, "Component health", "Last condition assessment"));
        PanelView comps = focusTarget("components", new PanelView(ctx));
        comps.setHighlighted(isFocused("components"));
        boolean animate = firstTime("component-bars");
        for (int i = 0; i < a.components.size(); i++) {
            ComponentHealthRecord c = a.components.get(i);
            int color = Palette.health(c.health);
            LinearLayout row = Ui.row(ctx);
            row.addView(Ui.dot(ctx, color, 8));
            row.addView(Ui.hspace(ctx, 10));
            LinearLayout col = Ui.column(ctx);
            col.addView(Ui.text(ctx, c.name, Type.Style.TITLE_SMALL, Palette.TEXT));
            col.addView(Ui.text(ctx, c.note, Type.Style.BODY_SMALL, Palette.TEXT_FAINT));
            row.addView(col, Ui.weight(1f));
            LinearLayout right = Ui.column(ctx);
            right.setGravity(Gravity.END);
            right.addView(Ui.text(ctx, c.health + "%", Type.Style.READOUT_SMALL, color));
            right.addView(Ui.text(ctx, c.status, Type.Style.TAG, color));
            row.addView(right);
            Ui.add(comps, row, i == 0 ? 0 : 14);
            BarView bar = new BarView(ctx, 8).setColor(color).setFraction(c.health / 100f, animate);
            Ui.add(comps, bar, 8);
        }
        add(comps, 4);

        // ---- Vibration trend -------------------------------------------------
        add(Ui.sectionHeader(ctx, "Vibration, last 120 operating hours",
                cond.label + ". Open sensor monitoring for every channel."));
        PanelView chartPanel = new PanelView(ctx, 12);
        TrendChartView chart = new TrendChartView(ctx, 130)
                .setData(SensorType.VIBRATION, window.channel(SensorType.VIBRATION), pre.smoothed(SensorType.VIBRATION),
                        pre.spikes(SensorType.VIBRATION), null);
        chart.animateReveal(firstTime("vib-" + cond));
        chartPanel.addView(chart, Ui.matchWrap());
        add(chartPanel, 4);

        // ---- Actions ---------------------------------------------------------
        add(Ui.primary(ctx, "Run AI analysis", R.drawable.ic_psychology, v -> {
            state.selectAircraft(id);
            state.navigate(Screen.ANALYSIS);
            state.runAnalysis();
        }));
        View sensors = Ui.secondary(ctx, "Sensor data", R.drawable.ic_monitor_heart, v -> {
            state.selectAircraft(id);
            state.navigate(Screen.MONITORING);
        });
        View report = Ui.secondary(ctx, "Report", R.drawable.ic_description, v -> {
            state.selectAircraft(id);
            state.navigate(Screen.REPORTS);
        });
        add(Ui.equalRow(ctx, 10, sensors, report), 10);
    }
}
