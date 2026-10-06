package com.aeromaintenance.ai.ui.screens;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.DemoScript;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.Screen;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.PanelView;

/** Faculty demo mode: problem, solution, pipeline, benefits, future scope and the guided demo. */
public class FacultyDemoScreen extends BaseScreen {

    public FacultyDemoScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return false;
    }

    @Override
    protected void build() {
        PanelView problem = new PanelView(ctx).setAccent(Palette.RED);
        problem.addView(Ui.text(ctx, "Problem", Type.Style.LABEL_LARGE, Palette.RED));
        Ui.add(problem, Ui.text(ctx, "Unexpected aircraft component degradation can result in maintenance delays, "
                + "downtime, and operational risk.", Type.Style.TITLE_LARGE, Palette.TEXT), 4);
        Ui.add(problem, Ui.text(ctx, "Fixed-interval maintenance either replaces parts that still have life left or "
                + "misses a part that wears faster than expected.", Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED), 6);
        add(problem);

        PanelView solution = new PanelView(ctx).setAccent(Palette.GREEN);
        solution.addView(Ui.text(ctx, "Proposed solution", Type.Style.LABEL_LARGE, Palette.GREEN));
        Ui.add(solution, Ui.text(ctx, "AI-assisted predictive maintenance using aircraft telemetry.",
                Type.Style.TITLE_LARGE, Palette.TEXT), 4);
        Ui.add(solution, Ui.text(ctx, "The app watches sensor trends, scores how abnormal they are, names the component "
                + "at risk, estimates how many operating hours remain, and turns that into an alert and a work order.",
                Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED), 6);
        add(solution);

        PanelView pipeline = new PanelView(ctx);
        pipeline.addView(Ui.text(ctx, "AI pipeline", Type.Style.TITLE_MEDIUM, Palette.TEXT));
        LinearLayout stages = Ui.column(ctx);
        stages.setGravity(Gravity.CENTER_HORIZONTAL);
        String[] names = {"Telemetry", "Preprocessing", "Anomaly detection", "Risk prediction", "RUL",
                "Maintenance recommendation"};
        for (int i = 0; i < names.length; i++) {
            TextView chip = Ui.text(ctx, names[i], Type.Style.LABEL_MEDIUM, Palette.TEXT);
            chip.setBackground(Ui.rounded(ctx, Palette.PANEL_RAISED, 8, Palette.HAIRLINE, 1));
            chip.setPadding(dp(10), dp(6), dp(10), dp(6));
            stages.addView(chip, Ui.wrap());
            if (i < names.length - 1) stages.addView(Ui.text(ctx, "↓", Type.Style.TITLE_MEDIUM, Palette.CYAN), Ui.wrap());
        }
        Ui.add(pipeline, stages, 10);
        add(pipeline);

        PanelView benefits = new PanelView(ctx);
        benefits.addView(Ui.text(ctx, "Real-world benefits", Type.Style.TITLE_MEDIUM, Palette.TEXT));
        benefits.addView(Ui.vspace(ctx, 6));
        for (String b : new String[]{"Early fault detection", "Reduced unexpected downtime", "Better maintenance planning",
                "Component health monitoring", "Data-driven maintenance decisions"}) {
            benefits.addView(Ui.bullet(ctx, b, Palette.GREEN));
        }
        add(benefits);

        PanelView future = new PanelView(ctx);
        future.addView(Ui.text(ctx, "Future scope", Type.Style.TITLE_MEDIUM, Palette.TEXT));
        future.addView(Ui.vspace(ctx, 6));
        for (String f : new String[]{"Real aircraft IoT/sensor integration", "Edge AI", "Real-time streaming",
                "Digital twins", "Federated learning", "Explainable AI", "Integration with maintenance management systems"}) {
            future.addView(Ui.bullet(ctx, f, Palette.MAGENTA));
        }
        add(future);

        PanelView script = new PanelView(ctx);
        script.addView(Ui.text(ctx, "Guided demo, about 70 seconds", Type.Style.TITLE_MEDIUM, Palette.TEXT));
        script.addView(Ui.text(ctx, "Pause at any step to talk; the narration appears above the navigation bar.",
                Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
        script.addView(Ui.vspace(ctx, 10));
        for (int i = 0; i < DemoScript.STEPS.size(); i++) {
            LinearLayout row = new LinearLayout(ctx);
            row.setPadding(0, dp(3), 0, dp(3));
            row.addView(Ui.text(ctx, Integer.toString(i + 1), Type.Style.NUMERIC, Palette.CYAN), Ui.width(ctx, 24));
            row.addView(Ui.text(ctx, DemoScript.STEPS.get(i).title, Type.Style.BODY_MEDIUM, Palette.TEXT), Ui.weight(1f));
            script.addView(row, Ui.matchWrap());
        }
        Ui.add(script, Ui.primary(ctx, "Start demo", R.drawable.ic_play, v -> {
            activity.requestNotifications();
            state.startDemo();
        }), 12);
        Ui.add(script, Ui.secondary(ctx, "How the AI works", R.drawable.ic_insights,
                v -> state.navigate(Screen.INSIGHTS)), 8);
        add(script);
    }
}
