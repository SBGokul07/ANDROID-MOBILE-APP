package com.aeromaintenance.ai.ui.shell;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.Destination;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.Screen;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/** Slide-in navigation drawer listing every section, grouped like an operations console. */
public class NavDrawer extends FrameLayout {

    public interface Actions {
        void onStartDemo();
    }

    private static final class Entry {
        final View row;
        final ImageView icon;
        final TextView label;
        final TextView badge;
        final Screen screen;
        final int tab;

        Entry(View row, ImageView icon, TextView label, TextView badge, Screen screen, int tab) {
            this.row = row;
            this.icon = icon;
            this.label = label;
            this.badge = badge;
            this.screen = screen;
            this.tab = tab;
        }
    }

    private final AppState state;
    private final View scrim;
    private final LinearLayout panel;
    private final LinearLayout list;
    private final List<Entry> entries = new ArrayList<>();
    private boolean open;

    public NavDrawer(Context c, final AppState state, final Actions actions) {
        super(c);
        this.state = state;
        scrim = new View(c);
        scrim.setBackgroundColor(0x99000000);
        scrim.setOnClickListener(v -> close());
        addView(scrim, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        panel = Ui.column(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Palette.PANEL);
        float r = Ui.dp(c, 16);
        bg.setCornerRadii(new float[]{0, 0, r, r, r, r, 0, 0});
        panel.setBackground(bg);
        panel.setClickable(true);
        panel.setElevation(Ui.dp(c, 12));
        ScrollView scroll = new ScrollView(c);
        list = Ui.column(c);
        list.setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 12), Ui.dp(c, 24));
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(panel, new LayoutParams(Ui.dp(c, 304), LayoutParams.MATCH_PARENT, Gravity.START));

        // Header
        LinearLayout header = Ui.row(c);
        header.setPadding(Ui.dp(c, 8), Ui.dp(c, 20), 0, Ui.dp(c, 16));
        ImageView logo = new ImageView(c);
        logo.setImageResource(R.drawable.ic_launcher_foreground);
        logo.setBackground(Ui.rounded(c, Palette.NIGHT, 12, 0, 0));
        logo.setClipToOutline(true);
        header.addView(logo, Ui.size(c, 44, 44));
        header.addView(Ui.hspace(c, 12));
        LinearLayout titles = Ui.column(c);
        titles.addView(Ui.text(c, "AeroMaintenance AI", Type.Style.TITLE_LARGE, Palette.TEXT));
        titles.addView(Ui.text(c, "Research prototype, simulated data", Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
        header.addView(titles, Ui.weight(1f));
        list.addView(header, Ui.matchWrap());
        list.addView(Ui.divider(c, Palette.HAIRLINE));

        group("Operations");
        entry("Dashboard", R.drawable.ic_dashboard, Screen.DASHBOARD, -1);
        entry("Aircraft", R.drawable.ic_flight, Screen.FLEET, -1);
        entry("Sensor monitoring", R.drawable.ic_monitor_heart, Screen.MONITORING, -1);
        entry("AI prediction", R.drawable.ic_psychology, Screen.ANALYSIS, -1);
        entry("Alerts", R.drawable.ic_notifications, Screen.ALERTS, -1);
        group("Maintenance");
        entry("Predictive maintenance", R.drawable.ic_timeline, Screen.MAINTENANCE, 0);
        entry("Maintenance planning", R.drawable.ic_build, Screen.MAINTENANCE, 1);
        entry("Reports", R.drawable.ic_description, Screen.REPORTS, -1);
        group("Explain");
        entry("AI insights", R.drawable.ic_insights, Screen.INSIGHTS, -1);
        entry("Faculty demo mode", R.drawable.ic_school, Screen.FACULTY_DEMO, -1);
        entry("About and disclaimer", R.drawable.ic_info, Screen.ABOUT, -1);

        View start = Ui.primary(c, "Start demo", R.drawable.ic_play, v -> {
            close();
            actions.onStartDemo();
        });
        Ui.add(list, start, 16);

        setVisibility(GONE);
    }

    private void group(String title) {
        Context c = getContext();
        TextView t = Ui.text(c, title, Type.Style.LABEL_MEDIUM, Palette.TEXT_FAINT);
        t.setPadding(Ui.dp(c, 16), Ui.dp(c, 18), 0, Ui.dp(c, 6));
        list.addView(t, Ui.matchWrap());
    }

    private void entry(String label, int icon, final Screen screen, final int tab) {
        Context c = getContext();
        LinearLayout row = Ui.row(c);
        row.setPadding(Ui.dp(c, 16), 0, Ui.dp(c, 16), 0);
        row.setMinimumHeight(Ui.dp(c, 48));
        ImageView i = Ui.icon(c, icon, Palette.TEXT_MUTED, 24);
        row.addView(i);
        row.addView(Ui.hspace(c, 12));
        TextView l = Ui.text(c, label, Type.Style.TITLE_SMALL, Palette.TEXT);
        row.addView(l, Ui.weight(1f));
        TextView badge = Ui.text(c, "", Type.Style.TAG, Palette.RED);
        row.addView(badge);
        Ui.tappable(row, 24, v -> {
            close();
            if (tab >= 0) state.openMaintenance(tab);
            else state.navigate(screen);
        });
        list.addView(row, Ui.matchWrap());
        entries.add(new Entry(row, i, l, badge, screen, tab));
    }

    /** Updates the selected entry and the alert badge. */
    public void refresh() {
        Destination current = state.current();
        for (Entry e : entries) {
            boolean selected = current.screen == e.screen && (e.tab < 0 || e.tab == state.maintenanceTab());
            int color = selected ? Palette.CYAN : Palette.TEXT_MUTED;
            e.icon.setImageTintList(ColorStateList.valueOf(color));
            e.label.setTextColor(selected ? Palette.CYAN : Palette.TEXT);
            e.row.setBackground(selected ? Ui.rounded(getContext(), Palette.alpha(Palette.CYAN, 0.14f), 24, 0, 0) : null);
            int count = e.screen == Screen.ALERTS ? state.unacknowledgedCount() : 0;
            e.badge.setText(count > 0 ? Integer.toString(count) : "");
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void openDrawer() {
        if (open) return;
        open = true;
        refresh();
        setVisibility(VISIBLE);
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(200).start();
        panel.setTranslationX(-Ui.dp(getContext(), 320));
        panel.animate().translationX(0).setDuration(240).start();
    }

    public void close() {
        if (!open) return;
        open = false;
        scrim.animate().alpha(0f).setDuration(180).start();
        panel.animate().translationX(-Ui.dp(getContext(), 320)).setDuration(200)
                .withEndAction(() -> {
                    if (!open) setVisibility(GONE);
                }).start();
    }
}
