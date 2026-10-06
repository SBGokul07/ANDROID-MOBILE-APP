package com.aeromaintenance.ai.ui.shell;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.DemoScript;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.BarView;

/** Narration and controls for the guided demo; sits above the navigation bar. */
public class DemoPanel extends LinearLayout {

    public interface Actions {
        void onGenerateReport();

        void onReplay();
    }

    private final AppState state;
    private final TextView stepLabel;
    private final TextView title;
    private final TextView narration;
    private final LinearLayout controls;
    private final LinearLayout finishedActions;
    private final BarView progress;
    private final ImageView prev;
    private final ImageView pause;

    public DemoPanel(Context c, final AppState state, final Actions actions) {
        super(c);
        this.state = state;
        setOrientation(VERTICAL);
        setBackgroundColor(Palette.PANEL_RAISED);
        setPadding(Ui.dp(c, 16), Ui.dp(c, 10), Ui.dp(c, 8), Ui.dp(c, 8));
        setClickable(true);

        LinearLayout head = Ui.row(c);
        LinearLayout titles = Ui.column(c);
        stepLabel = Ui.text(c, "", Type.Style.LABEL_MEDIUM, Palette.CYAN);
        title = Ui.singleLine(c, "", Type.Style.TITLE_MEDIUM, Palette.TEXT);
        titles.addView(stepLabel);
        titles.addView(title);
        head.addView(titles, Ui.weight(1f));
        head.addView(Ui.iconButton(c, R.drawable.ic_close, Palette.TEXT_MUTED, "Exit demo", v -> state.exitDemo()));
        addView(head, Ui.matchWrap());

        narration = Ui.text(c, "", Type.Style.BODY_SMALL, Palette.TEXT_MUTED);
        narration.setMaxLines(3);
        narration.setEllipsize(TextUtils.TruncateAt.END);
        narration.setPadding(0, 0, Ui.dp(c, 8), 0);
        addView(narration, Ui.matchWrap());

        controls = Ui.row(c);
        progress = new BarView(c, 3);
        progress.setColor(Palette.CYAN);
        controls.addView(progress, Ui.weight(1f));
        prev = Ui.iconButton(c, R.drawable.ic_skip_previous, Palette.TEXT, "Previous step", v -> state.demoPrevious());
        pause = Ui.iconButton(c, R.drawable.ic_pause, Palette.CYAN, "Pause", v -> state.demoTogglePause());
        ImageView next = Ui.iconButton(c, R.drawable.ic_skip_next, Palette.TEXT, "Next step", v -> state.demoNext());
        controls.addView(prev);
        controls.addView(pause);
        controls.addView(next);
        LayoutParams clp = Ui.matchWrap();
        clp.topMargin = Ui.dp(c, 2);
        addView(controls, clp);

        finishedActions = Ui.row(c);
        finishedActions.setPadding(0, Ui.dp(c, 10), Ui.dp(c, 8), Ui.dp(c, 4));
        View report = Ui.primary(c, "Generate report", R.drawable.ic_description, v -> actions.onGenerateReport());
        View replay = Ui.secondary(c, "Replay", R.drawable.ic_refresh, v -> actions.onReplay());
        finishedActions.addView(report, Ui.weight(1f));
        LayoutParams rlp = new LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(c, 52));
        rlp.leftMargin = Ui.dp(c, 8);
        finishedActions.addView(replay, rlp);
        addView(finishedActions, Ui.matchWrap());
        setVisibility(GONE);
    }

    /** Refreshes everything from the current demo state. */
    public void bind() {
        AppState.Demo d = state.demo();
        if (d == null) {
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);
        if (d.finished) {
            stepLabel.setText("Demo complete");
            title.setText("End-to-end workflow shown");
            narration.setText("Telemetry, anomaly score, risk, remaining useful life, recommendation, alert and work "
                    + "order: all generated from the simulated data.");
            controls.setVisibility(GONE);
            finishedActions.setVisibility(VISIBLE);
            return;
        }
        DemoScript.Step step = DemoScript.STEPS.get(d.step);
        stepLabel.setText("Step " + (d.step + 1) + " of " + DemoScript.STEPS.size());
        title.setText(step.title);
        narration.setText(step.narration.text(state.demoResult()));
        controls.setVisibility(VISIBLE);
        finishedActions.setVisibility(GONE);
        prev.setEnabled(d.step > 0);
        prev.setAlpha(d.step > 0 ? 1f : 0.35f);
        pause.setImageResource(d.paused ? R.drawable.ic_play : R.drawable.ic_pause);
        pause.setContentDescription(d.paused ? "Resume" : "Pause");
        progress.setFraction(d.progress, false);
    }

    /** Cheap update for the 50 ms progress ticks. */
    public void updateProgress() {
        AppState.Demo d = state.demo();
        if (d != null && !d.finished) progress.setFraction(d.progress, false);
    }
}
