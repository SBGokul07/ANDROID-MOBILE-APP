package com.aeromaintenance.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.aeromaintenance.ai.notify.AlertNotifier;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.screens.AboutScreen;
import com.aeromaintenance.ai.ui.screens.AircraftDetailScreen;
import com.aeromaintenance.ai.ui.screens.AlertsScreen;
import com.aeromaintenance.ai.ui.screens.AnalysisScreen;
import com.aeromaintenance.ai.ui.screens.BaseScreen;
import com.aeromaintenance.ai.ui.screens.DashboardScreen;
import com.aeromaintenance.ai.ui.screens.FacultyDemoScreen;
import com.aeromaintenance.ai.ui.screens.FleetScreen;
import com.aeromaintenance.ai.ui.screens.InsightsScreen;
import com.aeromaintenance.ai.ui.screens.MaintenanceScreen;
import com.aeromaintenance.ai.ui.screens.MonitoringScreen;
import com.aeromaintenance.ai.ui.screens.ReportsScreen;
import com.aeromaintenance.ai.ui.shell.BottomNav;
import com.aeromaintenance.ai.ui.shell.DemoPanel;
import com.aeromaintenance.ai.ui.shell.NavDrawer;
import com.aeromaintenance.ai.ui.shell.Snackbar;
import com.aeromaintenance.ai.ui.shell.TopBar;

/**
 * The only activity. It builds the app shell (top bar, content area, demo panel,
 * bottom navigation, drawer), shows the screen for the current destination, and
 * forwards state changes from {@link AppState} to that screen.
 */
public class MainActivity extends Activity implements AppState.Listener, AppState.MessageListener {

    private static final int REQUEST_NOTIFICATIONS = 1;

    private AppState state;
    private TopBar topBar;
    private BottomNav bottomNav;
    private DemoPanel demoPanel;
    private NavDrawer drawer;
    private Snackbar snackbar;
    private FrameLayout contentHost;

    private BaseScreen screen;
    private View screenView;
    private Destination shown;

    public AppState state() {
        return state;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Palette.NIGHT);
        w.setNavigationBarColor(Palette.PANEL);
        AlertNotifier.createChannel(this);
        state = AppState.get(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Palette.NIGHT);

        LinearLayout main = Ui.column(this);
        topBar = new TopBar(this, new TopBar.Actions() {
            @Override
            public void onMenu() {
                drawer.openDrawer();
            }

            @Override
            public void onBack() {
                state.back();
            }

            @Override
            public void onAlerts() {
                state.navigate(Screen.ALERTS);
            }
        });
        main.addView(topBar, Ui.matchWrap());

        contentHost = new FrameLayout(this);
        snackbar = new Snackbar(this);
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        int m = Ui.dp(this, 12);
        slp.setMargins(m, m, m, m);
        contentHost.addView(snackbar, slp);
        main.addView(contentHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        demoPanel = new DemoPanel(this, state, new DemoPanel.Actions() {
            @Override
            public void onGenerateReport() {
                state.exitDemo();
                state.navigate(Screen.REPORTS);
                state.generateReport();
            }

            @Override
            public void onReplay() {
                requestNotifications();
                state.startDemo();
            }
        });
        main.addView(demoPanel, Ui.matchWrap());
        main.addView(Ui.divider(this, Palette.HAIRLINE));
        bottomNav = new BottomNav(this, state);
        main.addView(bottomNav, Ui.matchWrap());
        root.addView(main, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        drawer = new NavDrawer(this, state, () -> {
            requestNotifications();
            state.startDemo();
        });
        root.addView(drawer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        state.addListener(this);
        state.setMessageListener(this);
        showScreen(state.current(), false);
        refreshChrome();
        demoPanel.bind();
        if (savedInstanceState == null) handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        LaunchOptions o = LaunchOptions.from(intent);
        if (o == null) return;
        if (o.startDemo) requestNotifications();
        state.applyLaunchOptions(o);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screen != null) screen.setActive(true);
    }

    @Override
    protected void onPause() {
        if (screen != null) screen.setActive(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        state.removeListener(this);
        state.setMessageListener(null);
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (drawer.isOpen()) {
            drawer.close();
            return;
        }
        if (!state.back()) super.onBackPressed();
    }

    // ------------------------------------------------------------------ state

    @Override
    public void onStateChanged(AppState.Change change) {
        switch (change) {
            case NAVIGATION:
                showScreen(state.current(), true);
                refreshChrome();
                return;
            case DEMO_PROGRESS:
                demoPanel.updateProgress();
                return;
            case DEMO:
                demoPanel.bind();
                break;
            case ANALYSIS:
                demoPanel.bind();
                break;
            case ALERTS:
            case MAINTENANCE_TAB:
                refreshChrome();
                break;
            default:
                break;
        }
        if (screen != null) screen.onStateChanged(change);
    }

    @Override
    public void onMessage(String message) {
        snackbar.show(message);
    }

    private void refreshChrome() {
        Destination d = state.current();
        topBar.bind(d, state.canGoBack(), state.unacknowledgedCount());
        bottomNav.select(d);
        drawer.refresh();
    }

    private void showScreen(Destination d, boolean animate) {
        if (d.equals(shown) && screen != null) return;
        if (screen != null) screen.setActive(false);
        BaseScreen next = createScreen(d);
        View view = next.createView();
        final View old = screenView;
        contentHost.addView(view, contentHost.indexOfChild(snackbar),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (old != null) {
            if (animate) {
                view.setAlpha(0f);
                view.animate().alpha(1f).setDuration(220).start();
                old.animate().alpha(0f).setDuration(180).withEndAction(() -> contentHost.removeView(old)).start();
            } else {
                contentHost.removeView(old);
            }
        }
        screen = next;
        screenView = view;
        shown = d;
        next.setActive(true);
        next.onShown();
    }

    private BaseScreen createScreen(Destination d) {
        switch (d.screen) {
            case FLEET:
                return new FleetScreen(this);
            case AIRCRAFT_DETAIL:
                return new AircraftDetailScreen(this, d.aircraftId);
            case MONITORING:
                return new MonitoringScreen(this);
            case ANALYSIS:
                return new AnalysisScreen(this);
            case INSIGHTS:
                return new InsightsScreen(this);
            case MAINTENANCE:
                return new MaintenanceScreen(this);
            case ALERTS:
                return new AlertsScreen(this);
            case REPORTS:
                return new ReportsScreen(this);
            case FACULTY_DEMO:
                return new FacultyDemoScreen(this);
            case ABOUT:
                return new AboutScreen(this);
            case DASHBOARD:
            default:
                return new DashboardScreen(this);
        }
    }

    // --------------------------------------------------------------- services

    /** Asks for the notification permission (Android 13+) before the app first raises an alert. */
    public void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !AlertNotifier.canPost(this)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    /** Opens the Android share sheet with plain text. */
    public void shareText(String subject, String text) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Share report"));
    }
}
