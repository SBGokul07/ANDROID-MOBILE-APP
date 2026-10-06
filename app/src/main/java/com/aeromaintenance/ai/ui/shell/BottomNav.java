package com.aeromaintenance.ai.ui.shell;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.Destination;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.Screen;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;

/** Bottom navigation bar with the five main sections. */
public class BottomNav extends LinearLayout {

    private static final Screen[] TABS = {Screen.DASHBOARD, Screen.FLEET, Screen.MONITORING, Screen.ANALYSIS, Screen.MAINTENANCE};
    private static final String[] LABELS = {"Dashboard", "Aircraft", "Monitor", "AI", "Maint."};
    private static final int[] ICONS = {R.drawable.ic_dashboard, R.drawable.ic_flight, R.drawable.ic_monitor_heart,
            R.drawable.ic_psychology, R.drawable.ic_build};

    private final FrameLayout[] pills = new FrameLayout[TABS.length];
    private final ImageView[] icons = new ImageView[TABS.length];
    private final TextView[] labels = new TextView[TABS.length];

    public BottomNav(Context c, final AppState state) {
        super(c);
        setOrientation(HORIZONTAL);
        setBackgroundColor(Palette.PANEL);
        setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 12));
        for (int i = 0; i < TABS.length; i++) {
            final Screen tab = TABS[i];
            LinearLayout item = Ui.column(c);
            item.setGravity(Gravity.CENTER_HORIZONTAL);

            FrameLayout pill = new FrameLayout(c);
            ImageView icon = new ImageView(c);
            icon.setImageResource(ICONS[i]);
            pill.addView(icon, new FrameLayout.LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24), Gravity.CENTER));
            item.addView(pill, Ui.size(c, 64, 32));

            TextView label = Ui.text(c, LABELS[i], Type.Style.LABEL_SMALL, Palette.TEXT_MUTED);
            LayoutParams llp = Ui.wrap();
            llp.topMargin = Ui.dp(c, 4);
            item.addView(label, llp);
            item.setContentDescription(LABELS[i]);
            Ui.tappable(item, 16, v -> state.navigate(tab));
            addView(item, Ui.weight(1f));

            pills[i] = pill;
            icons[i] = icon;
            labels[i] = label;
        }
    }

    public void select(Destination current) {
        Screen s = current.screen == Screen.AIRCRAFT_DETAIL ? Screen.FLEET : current.screen;
        for (int i = 0; i < TABS.length; i++) {
            boolean selected = TABS[i] == s;
            int color = selected ? Palette.CYAN : Palette.TEXT_MUTED;
            pills[i].setBackground(selected
                    ? Ui.rounded(getContext(), Palette.alpha(Palette.CYAN, 0.16f), 16, 0, 0) : null);
            icons[i].setImageTintList(ColorStateList.valueOf(color));
            labels[i].setTextColor(color);
        }
    }
}
