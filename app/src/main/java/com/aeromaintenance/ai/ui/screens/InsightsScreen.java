package com.aeromaintenance.ai.ui.screens;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.PanelView;

/** "AI insights": what the prototype actually computes, and what a production model would use. */
public class InsightsScreen extends BaseScreen {

    /** name, concept, how this prototype implements it */
    private static final String[][] ARCHITECTURE = {
            {"Sensor data", "Engine and system telemetry recorded in flight.",
                    "Six simulated channels, one sample per operating hour, 120 hours per aircraft. Normal and abnormal data sets."},
            {"Preprocessing", "Remove sensor glitches, smooth noise, put channels on a common scale.",
                    "Hampel filter (median ± 4.5 MAD) replaces spikes, 5-point moving average, z-score against the fleet baseline."},
            {"Feature extraction", "Summarise each window with numbers that describe behaviour.",
                    "Per channel over the last 24 h: level (z-score), trend (least-squares slope and R²), volatility "
                            + "(fluctuation vs normal). 18 features."},
            {"Time-series model", "Learn what normal looks like and measure how far today's data is from it.",
                    "Normal-behaviour model: weighted root-mean-square of each channel's deviation from the learned baseline, in σ."},
            {"Anomaly detection", "Turn the deviation into a score and decide whether it is abnormal.",
                    "Logistic function maps deviation to a 0–1 score (0.5 at " + Format.number(InferenceEngine.ANOMALY_MIDPOINT, 2)
                            + "σ). Alert above " + Format.number(InferenceEngine.ALERT_THRESHOLD, 2) + ". CUSUM finds when it started."},
            {"Risk prediction", "Attribute the anomaly to a component and rate the failure risk.",
                    "Fault-signature matrix links channels to components. Health = 100 × (1 − margin used^1.6). "
                            + "HIGH if RUL < 72 h or health < 65 %."},
            {"RUL estimation", "Estimate operating hours until the component reaches its limit.",
                    "Linear extrapolation of the health-indicator trend to its limit (for example vibration to 7.0 mm/s), rounded down."},
    };

    private static final String[][] PRODUCTION_MODELS = {
            {"LSTM", "Long short-term memory network; learns long temporal patterns in sensor sequences. Common baseline for RUL regression."},
            {"GRU", "Gated recurrent unit; a lighter LSTM with similar accuracy and faster training, suited to on-device inference."},
            {"BiLSTM", "Bidirectional LSTM; reads each window forwards and backwards for richer context when classifying fault types."},
            {"1D CNN", "Convolutions along time; fast at spotting local shapes such as vibration bursts or pressure ripple."},
            {"CNN-LSTM", "CNN layers extract local features, LSTM layers model how they evolve; strong on multi-sensor RUL benchmarks."},
            {"Transformer", "Self-attention over the whole window; captures long-range interactions between channels."},
            {"Autoencoder", "Trained only on healthy data; a high reconstruction error flags an anomaly without needing labelled failures."},
    };

    public InsightsScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return false;
    }

    @Override
    protected void build() {
        PanelView label = new PanelView(ctx).setAccent(Palette.MAGENTA);
        label.addView(Ui.tag(ctx, InferenceEngine.MODEL_TAG, Palette.MAGENTA));
        Ui.add(label, Ui.text(ctx, "What this prototype actually does", Type.Style.TITLE_LARGE, Palette.TEXT), 10);
        Ui.add(label, Ui.text(ctx, "The app shows the full predictive-maintenance workflow on simulated telemetry. The "
                + "\"neural network\" label names the role the model plays in a production system; inside this prototype "
                + "each stage is a transparent statistical method, so every number on screen can be traced and explained. "
                + "No trained aviation model exists in the app, and its outputs are not real aircraft predictions.",
                Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED), 6);
        add(label);

        add(Ui.sectionHeader(ctx, "Architecture", "Grey: the concept. Cyan: how this prototype implements it."));
        PanelView arch = new PanelView(ctx, 14);
        for (int i = 0; i < ARCHITECTURE.length; i++) {
            LinearLayout row = Ui.topRow(ctx);
            LinearLayout rail = Ui.column(ctx);
            rail.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView num = Ui.text(ctx, Integer.toString(i + 1), Type.Style.TAG, Palette.CYAN);
            num.setBackground(Ui.rounded(ctx, Palette.alpha(Palette.CYAN, 0.14f), 8, Palette.alpha(Palette.CYAN, 0.5f), 1));
            num.setPadding(dp(7), dp(3), dp(7), dp(3));
            rail.addView(num, Ui.wrap());
            if (i < ARCHITECTURE.length - 1) {
                View line = new View(ctx);
                line.setBackgroundColor(Palette.HAIRLINE_STRONG);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(Math.max(1, dp(1.5f)), 0, 1f);
                llp.topMargin = dp(4);
                rail.addView(line, llp);
            }
            row.addView(rail, new LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.MATCH_PARENT));
            row.addView(Ui.hspace(ctx, 12));
            LinearLayout col = Ui.column(ctx);
            col.setPadding(0, 0, 0, dp(14));
            col.addView(Ui.text(ctx, ARCHITECTURE[i][0], Type.Style.TITLE_MEDIUM, Palette.TEXT));
            col.addView(Ui.text(ctx, ARCHITECTURE[i][1], Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
            Ui.add(col, Ui.text(ctx, ARCHITECTURE[i][2], Type.Style.BODY_SMALL, Palette.CYAN), 4);
            row.addView(col, Ui.weight(1f));
            arch.addView(row, Ui.matchWrap());
        }
        add(arch, 4);

        add(Ui.sectionHeader(ctx, "Models for a production version",
                "Would replace the statistical model once real fleet data is available"));
        PanelView models = new PanelView(ctx, 0);
        for (int i = 0; i < PRODUCTION_MODELS.length; i++) {
            if (i > 0) models.addView(Ui.divider(ctx, Palette.HAIRLINE));
            LinearLayout row = Ui.topRow(ctx);
            row.setPadding(dp(16), dp(12), dp(16), dp(12));
            row.addView(Ui.text(ctx, PRODUCTION_MODELS[i][0], Type.Style.TITLE_SMALL, Palette.MAGENTA), Ui.width(ctx, 96));
            row.addView(Ui.text(ctx, PRODUCTION_MODELS[i][1], Type.Style.BODY_SMALL, Palette.TEXT_MUTED), Ui.weight(1f));
            models.addView(row, Ui.matchWrap());
        }
        add(models, 4);

        add(Ui.sectionHeader(ctx, "Training a real model would need", null));
        PanelView data = new PanelView(ctx);
        data.addView(Ui.bullet(ctx, "Labelled run-to-failure histories, for example the NASA C-MAPSS turbofan degradation "
                + "data set, for a first benchmark.", Palette.CYAN));
        data.addView(Ui.bullet(ctx, "Operator fleet data with maintenance records to label when components were actually "
                + "replaced.", Palette.CYAN));
        data.addView(Ui.bullet(ctx, "Validation against engineering limits from the maintenance manual and sign-off by "
                + "licensed engineers.", Palette.CYAN));
        add(data, 4);

        PanelView params = new PanelView(ctx);
        params.addView(Ui.text(ctx, "Prototype parameters", Type.Style.TITLE_MEDIUM, Palette.TEXT));
        params.addView(Ui.vspace(ctx, 8));
        param(params, "Window", "120 operating hours, 6 channels");
        param(params, "Recent feature window", InferenceEngine.RECENT + " h");
        param(params, "Anomaly alert threshold", Format.number(InferenceEngine.ALERT_THRESHOLD, 2));
        param(params, "High-risk threshold", Format.number(InferenceEngine.HIGH_THRESHOLD, 2));
        param(params, "Detection sensitivity", Format.number(InferenceEngine.detectionSigma(), 1) + "σ");
        param(params, "Bearing vibration limit", Format.number(InferenceEngine.BEARING_LIMIT, 1) + " mm/s");
        param(params, "EGT limit", Format.number(InferenceEngine.EGT_LIMIT, 0) + " °C");
        add(params);
    }

    private void param(LinearLayout panel, String k, String v) {
        LinearLayout row = Ui.topRow(ctx);
        row.setPadding(0, dp(4), 0, dp(4));
        row.addView(Ui.text(ctx, k, Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED), Ui.weight(1f));
        row.addView(Ui.text(ctx, v, Type.Style.NUMERIC, Palette.TEXT));
        panel.addView(row, Ui.matchWrap());
    }
}
