package com.aeromaintenance.ai.ui.screens;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.Screen;
import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.data.SensorType;
import com.aeromaintenance.ai.data.TelemetryWindow;
import com.aeromaintenance.ai.data.Zone;
import com.aeromaintenance.ai.engine.AnalysisResult;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.engine.PreprocessedWindow;
import com.aeromaintenance.ai.engine.SensorFeatures;
import com.aeromaintenance.ai.ui.Controls;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.DialView;
import com.aeromaintenance.ai.ui.widget.PanelView;
import com.aeromaintenance.ai.ui.widget.PulseDotView;
import com.aeromaintenance.ai.ui.widget.TrendChartView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Live sensor monitoring: six instrument dials that refresh every second, the
 * NORMAL / ABNORMAL data toggle and trend charts that make degradation obvious.
 */
public class MonitoringScreen extends BaseScreen {

    private static final SensorType[] CHART_ORDER = {
            SensorType.ENGINE_TEMP, SensorType.VIBRATION, SensorType.HYDRAULIC_PRESSURE,
            SensorType.OIL_PRESSURE, SensorType.RPM, SensorType.FUEL_FLOW};
    private static final String[] CHART_TITLES = {
            "Temperature trend", "Vibration trend", "Hydraulic pressure trend",
            "Oil pressure trend", "RPM trend", "Fuel flow trend"};

    /** One live instrument tile. */
    private static final class Tile {
        final SensorType sensor;
        final DialView dial;
        final TextView value;

        Tile(SensorType sensor, DialView dial, TextView value) {
            this.sensor = sensor;
            this.dial = dial;
            this.value = value;
        }
    }

    private final List<Tile> tiles = new ArrayList<>();
    /** Last value shown per dial, so a rebuild continues from it instead of jumping. */
    private final Map<SensorType, Float> lastValues = new EnumMap<>(SensorType.class);
    private final Map<SensorType, Double> baseValues = new EnumMap<>(SensorType.class);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int tick;
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick++;
            updateTiles(true);
            handler.postDelayed(this, 1000);
        }
    };

    public MonitoringScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return change == AppState.Change.SELECTION;
    }

    /** Colour ranges for a channel's dial, built from its caution and warning limits. */
    static List<DialView.Band> sensorBands(SensorType s) {
        float lo = (float) s.chartMin;
        float hi = (float) s.chartMax;
        List<DialView.Band> bands = new ArrayList<>();
        float greenLo = (float) (s.warnLow != null ? s.warnLow : s.chartMin);
        float greenHi = (float) (s.warnHigh != null ? s.warnHigh : s.chartMax);
        if (s.alarmLow != null) bands.add(new DialView.Band(lo, s.alarmLow.floatValue(), Palette.RED));
        if (s.warnLow != null) {
            float from = (float) (s.alarmLow != null ? s.alarmLow : s.chartMin);
            bands.add(new DialView.Band(from, s.warnLow.floatValue(), Palette.AMBER));
        }
        bands.add(new DialView.Band(greenLo, greenHi, Palette.GREEN));
        if (s.warnHigh != null) {
            float to = (float) (s.alarmHigh != null ? s.alarmHigh : s.chartMax);
            bands.add(new DialView.Band(s.warnHigh.floatValue(), to, Palette.AMBER));
        }
        if (s.alarmHigh != null) bands.add(new DialView.Band(s.alarmHigh.floatValue(), hi, Palette.RED));
        return bands;
    }

    static String zoneWord(Zone z) {
        switch (z) {
            case NORMAL:
                return "NORMAL";
            case WARN:
                return "CAUTION";
            default:
                return "WARNING";
        }
    }

    @Override
    protected void build() {
        final String id = state.selectedId();
        DataCondition cond = state.conditionOf(id);
        TelemetryWindow window = state.telemetry(id, cond);
        PreprocessedWindow pre = state.preprocessed(id, cond);
        AnalysisResult result = state.resultFor(id, cond);
        Integer onsetIndex = result.onsetHoursAgo != null ? window.size() - 1 - result.onsetHoursAgo : null;
        java.util.Set<SensorType> onsetSensors = InferenceEngine.COMPONENT_SIGNATURES.get(result.primary.component).keySet();

        // ---- Controls ---------------------------------------------------------
        LinearLayout controls = Ui.column(ctx);
        controls.addView(Controls.aircraftSelector(ctx, state), Ui.matchWrap());
        Ui.add(controls, Controls.conditionToggle(ctx, cond, c -> state.setCondition(id, c)), 10);
        LinearLayout live = Ui.row(ctx);
        live.setPadding(dp(4), 0, dp(4), 0);
        live.addView(new PulseDotView(ctx, cond == DataCondition.NORMAL ? Palette.GREEN : Palette.RED), Ui.size(ctx, 8, 8));
        live.addView(Ui.hspace(ctx, 8));
        live.addView(Ui.text(ctx, "Live simulated feed, 1 sample per operating hour, read-outs refresh every second",
                Type.Style.BODY_SMALL, Palette.TEXT_MUTED), Ui.weight(1f));
        Ui.add(controls, live, 10);
        add(controls);

        // ---- Instrument tiles ---------------------------------------------------
        tiles.clear();
        baseValues.clear();
        for (SensorFeatures f : result.features) baseValues.put(f.sensor, f.current);
        PanelView tilePanel = focusTarget("tiles", new PanelView(ctx, 10));
        tilePanel.setHighlighted(isFocused("tiles"));
        SensorType[] all = SensorType.values();
        boolean sweep = firstTime("tiles");
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(ctx);
            for (int k = 0; k < 3; k++) {
                SensorType s = all[r * 3 + k];
                row.addView(instrumentTile(s, sweep), Ui.weight(1f));
            }
            tilePanel.addView(row, Ui.matchWrap());
        }
        add(tilePanel);
        updateTiles(false);

        add(Ui.primary(ctx, "Run AI analysis", R.drawable.ic_psychology, v -> {
            state.navigate(Screen.ANALYSIS);
            state.runAnalysis();
        }));

        // ---- Trend charts -------------------------------------------------------
        String supporting = cond == DataCondition.ABNORMAL
                ? "Abnormal data set: degradation injected from about "
                + (result.onsetHoursAgo != null ? result.onsetHoursAgo : 90) + " h ago"
                : "Normal data set: noise around the fleet baseline";
        add(Ui.sectionHeader(ctx, "Telemetry history", supporting));
        boolean reveal = firstTime("charts-" + id + "-" + cond);
        for (int i = 0; i < CHART_ORDER.length; i++) {
            SensorType sensor = CHART_ORDER[i];
            SensorFeatures f = result.feature(sensor);
            Zone zone = sensor.zoneOf(f.current);
            PanelView panel = new PanelView(ctx, 12);
            if (i == 0) focusTarget("charts", panel);
            panel.setHighlighted(isFocused("charts") && i < 2);

            LinearLayout head = Ui.row(ctx);
            LinearLayout titles = Ui.column(ctx);
            titles.addView(Ui.text(ctx, CHART_TITLES[i], Type.Style.TITLE_SMALL, Palette.TEXT));
            titles.addView(Ui.text(ctx, sensor.label, Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
            head.addView(titles, Ui.weight(1f));
            head.addView(Ui.text(ctx, sensor.format(f.current) + " " + sensor.unit, Type.Style.READOUT_SMALL, Palette.zone(zone)));
            head.addView(Ui.hspace(ctx, 8));
            head.addView(Ui.tag(ctx, zoneWord(zone), Palette.zone(zone)));
            panel.addView(head, Ui.matchWrap());

            TrendChartView chart = new TrendChartView(ctx, 150).setData(sensor, window.channel(sensor),
                    pre.smoothed(sensor), pre.spikes(sensor), onsetSensors.contains(sensor) ? onsetIndex : null);
            chart.animateReveal(reveal);
            Ui.add(panel, chart, 8);

            String trend = "Trend over the last 24 h: " + Format.signed(f.slopePerHour, sensor.decimals + 2) + " "
                    + sensor.unit + " per hour"
                    + (f.volatilityRatio > 1.6 ? ", fluctuation " + Format.number(f.volatilityRatio, 1) + "× normal" : "");
            Ui.add(panel, Ui.text(ctx, trend, Type.Style.BODY_SMALL, Palette.TEXT_MUTED), 4);
            add(panel, i == 0 ? 4 : 12);
        }

        add(Ui.prototypeNote(ctx, "Grey: raw samples. Coloured: cleaned and smoothed signal, green inside limits, amber in "
                + "the caution band, red past the warning limit. Rings: sensor spikes removed during preprocessing. "
                + "All telemetry is simulated on the phone; no aircraft data source is connected."));
        add(Ui.primary(ctx, "Run AI analysis", R.drawable.ic_psychology, v -> {
            state.navigate(Screen.ANALYSIS);
            state.runAnalysis();
        }));
    }

    private View instrumentTile(SensorType s, boolean sweep) {
        LinearLayout col = Ui.column(ctx);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(4), dp(4), dp(4), dp(6));
        DialView dial = new DialView(ctx).setRange((float) s.chartMin, (float) s.chartMax)
                .setBands(sensorBands(s)).setStrokeDp(5).setTicks(4);
        Float last = lastValues.get(s);
        if (last != null) dial.setValue(last, false);
        else if (!sweep) dial.setValue((float) s.baselineMean, false);
        LinearLayout.LayoutParams dlp = Ui.matchWrap();
        dlp.leftMargin = dp(6);
        dlp.rightMargin = dp(6);
        col.addView(dial, dlp);
        TextView value = Ui.text(ctx, "", Type.Style.READOUT_SMALL, Palette.GREEN);
        value.setGravity(Gravity.CENTER);
        value.setPadding(dp(8), dp(2), dp(8), dp(2));
        LinearLayout.LayoutParams vlp = Ui.wrap();
        vlp.topMargin = dp(2);
        col.addView(value, vlp);
        TextView label = Ui.singleLine(ctx, s.shortLabel + " (" + s.unit + ")", Type.Style.LABEL_SMALL, Palette.TEXT_MUTED);
        label.setGravity(Gravity.CENTER);
        col.addView(label, Ui.matchWrap());
        tiles.add(new Tile(s, dial, value));
        return col;
    }

    /** Small, deterministic jitter around the current value so the read-outs look live. */
    private void updateTiles(boolean animate) {
        for (Tile t : tiles) {
            Double base = baseValues.get(t.sensor);
            if (base == null) continue;
            double jitter = Math.sin(tick * 1.7 + t.sensor.ordinal() * 2.1) * t.sensor.baselineStd * 0.35;
            double v = base + jitter;
            Zone zone = t.sensor.zoneOf(v);
            int color = Palette.zone(zone);
            t.value.setText(t.sensor.format(v));
            t.value.setTextColor(color);
            t.value.setBackground(Ui.rounded(ctx, 0x00000000, 6, Palette.alpha(color, 0.6f), 1));
            t.dial.animateTo((float) v);
            lastValues.put(t.sensor, (float) v);
        }
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        handler.removeCallbacks(ticker);
        if (active) handler.postDelayed(ticker, 1000);
    }
}
