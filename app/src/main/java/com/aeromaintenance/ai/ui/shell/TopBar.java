package com.aeromaintenance.ai.ui.shell;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.Destination;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.Screen;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;

/** App bar: menu or back button, screen title, and the alerts bell with its badge. */
public class TopBar extends LinearLayout {

    public interface Actions {
        void onMenu();

        void onBack();

        void onAlerts();
    }

    private final ImageView navButton;
    private final TextView title;
    private final TextView subtitle;
    private final TextView badge;
    private boolean showingBack;

    public TopBar(Context c, final Actions actions) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundColor(Palette.NIGHT);
        setPadding(Ui.dp(c, 4), 0, Ui.dp(c, 4), 0);
        setMinimumHeight(Ui.dp(c, 64));

        navButton = Ui.iconButton(c, R.drawable.ic_menu, Palette.TEXT, "Open menu", v -> {
            if (showingBack) actions.onBack();
            else actions.onMenu();
        });
        addView(navButton);

        LinearLayout titles = Ui.column(c);
        titles.setPadding(Ui.dp(c, 4), 0, Ui.dp(c, 4), 0);
        title = Ui.singleLine(c, "", Type.Style.TITLE_LARGE, Palette.TEXT);
        subtitle = Ui.singleLine(c, "Predictive maintenance prototype, simulated data", Type.Style.BODY_SMALL, Palette.TEXT_MUTED);
        titles.addView(title);
        titles.addView(subtitle);
        addView(titles, Ui.weight(1f));

        FrameLayout bell = new FrameLayout(c);
        ImageView bellIcon = Ui.iconButton(c, R.drawable.ic_notifications, Palette.TEXT, "Alerts", v -> actions.onAlerts());
        bell.addView(bellIcon, new FrameLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
        badge = Ui.text(c, "", Type.Style.TAG, Palette.NIGHT);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.rounded(c, Palette.RED, 9, 0, 0));
        badge.setMinWidth(Ui.dp(c, 18));
        badge.setPadding(Ui.dp(c, 4), Ui.dp(c, 2), Ui.dp(c, 4), Ui.dp(c, 2));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        blp.topMargin = Ui.dp(c, 6);
        blp.rightMargin = Ui.dp(c, 6);
        badge.setClickable(false);
        bell.addView(badge, blp);
        addView(bell);
    }

    public void bind(Destination current, boolean canGoBack, int alertCount) {
        showingBack = canGoBack && !current.screen.isTab;
        navButton.setImageResource(showingBack ? R.drawable.ic_arrow_back : R.drawable.ic_menu);
        navButton.setContentDescription(showingBack ? "Back" : "Open menu");
        title.setText(current.title());
        subtitle.setVisibility(current.screen == Screen.DASHBOARD ? View.VISIBLE : View.GONE);
        setAlertCount(alertCount);
    }

    public void setAlertCount(int count) {
        badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        badge.setText(Integer.toString(count));
    }
}
