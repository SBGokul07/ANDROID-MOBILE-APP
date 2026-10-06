package com.aeromaintenance.ai.ui.screens;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.Destination;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.data.Aircraft;
import com.aeromaintenance.ai.data.FleetRepository;
import com.aeromaintenance.ai.data.FleetStatus;
import com.aeromaintenance.ai.data.Format;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.DialView;
import com.aeromaintenance.ai.ui.widget.PanelView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** All twelve aircraft with model, hours, cycles, health, risk and last maintenance. */
public class FleetScreen extends BaseScreen {

    /** Null = all aircraft. */
    private FleetStatus filter;

    public FleetScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return false;
    }

    @Override
    protected void build() {
        List<View> pills = new ArrayList<>();
        pills.add(Ui.filterPill(ctx, "All " + state.fleet().size(), filter == null, Palette.CYAN, v -> setFilter(null)));
        FleetStatus[] order = {FleetStatus.CRITICAL, FleetStatus.WARNING, FleetStatus.HEALTHY};
        for (final FleetStatus s : order) {
            pills.add(Ui.filterPill(ctx, s.label + " " + FleetRepository.countByStatus(s), filter == s,
                    Palette.fleetStatus(s), v -> setFilter(s)));
        }
        add(Ui.pillRow(ctx, pills));

        List<Aircraft> shown = new ArrayList<>();
        for (Aircraft a : state.fleet()) {
            if (filter == null || a.status == filter) shown.add(a);
        }
        Collections.sort(shown, (a, b) -> a.risk.rank != b.risk.rank
                ? Integer.compare(b.risk.rank, a.risk.rank) : Integer.compare(a.healthScore, b.healthScore));
        for (Aircraft a : shown) add(aircraftRow(a), 10);
    }

    private void setFilter(FleetStatus s) {
        filter = s;
        render();
    }

    private View aircraftRow(final Aircraft a) {
        PanelView panel = new PanelView(ctx, 14);
        LinearLayout row = Ui.row(ctx);

        DialView dial = new DialView(ctx).setRange(0, 100).setBands(DialView.healthBands()).setStrokeDp(5).setTicks(4);
        dial.setValue(a.healthScore, firstTime("dial-" + a.id));
        row.addView(Ui.dialBox(ctx, dial, Ui.text(ctx, Integer.toString(a.healthScore), Type.Style.NUMERIC,
                Palette.health(a.healthScore)), 0), Ui.size(ctx, 64, 64));
        row.addView(Ui.hspace(ctx, 14));

        LinearLayout col = Ui.column(ctx);
        LinearLayout title = Ui.row(ctx);
        title.setGravity(Gravity.BOTTOM);
        title.addView(Ui.text(ctx, a.id, Type.Style.TITLE_MEDIUM, Palette.TEXT));
        title.addView(Ui.hspace(ctx, 8));
        title.addView(Ui.singleLine(ctx, a.model, Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED));
        col.addView(title);
        col.addView(Ui.text(ctx, Format.number(a.flightHours, 0) + " flight hours, " + Format.number(a.flightCycles, 0)
                + " cycles", Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
        col.addView(Ui.text(ctx, "Last maintenance " + a.lastMaintenance, Type.Style.BODY_SMALL, Palette.TEXT_FAINT));
        row.addView(col, Ui.weight(1f));

        LinearLayout right = Ui.column(ctx);
        right.setGravity(Gravity.END);
        right.addView(Ui.tag(ctx, a.risk.label, Palette.risk(a.risk)));
        LinearLayout.LayoutParams clp = Ui.size(ctx, 24, 24);
        clp.topMargin = dp(10);
        right.addView(Ui.icon(ctx, R.drawable.ic_chevron_right, Palette.TEXT_FAINT, 24), clp);
        row.addView(right);

        panel.addView(row, Ui.matchWrap());
        panel.onTap(v -> {
            state.selectAircraft(a.id);
            state.navigate(Destination.detail(a.id));
        });
        panel.setContentDescription(a.id + ", " + a.model + ", health " + a.healthScore + ", risk " + a.risk.label);
        return panel;
    }
}
