package com.aeromaintenance.ai.ui.screens;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.ui.Ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Base class for every screen. A screen builds its views from the current
 * {@link AppState} in {@link #build()}; when the state changes it is simply built
 * again (keeping the scroll position), which keeps the code easy to follow.
 * Live parts (dials, the running pipeline) update in place instead.
 */
public abstract class BaseScreen {

    protected final MainActivity activity;
    protected final AppState state;
    protected final Context ctx;

    private ScrollView scroll;
    protected LinearLayout content;

    /** Animation keys already played on this screen, so a rebuild does not replay them. */
    private final Set<String> played = new HashSet<>();
    /** Views the guided demo can scroll to, by focus key. */
    private final Map<String, View> focusTargets = new HashMap<>();
    private String renderedFocus;
    private boolean active;

    protected BaseScreen(MainActivity activity) {
        this.activity = activity;
        this.state = activity.state();
        this.ctx = activity;
    }

    /** Creates the screen's root view. */
    public final View createView() {
        LinearLayout root = Ui.column(ctx);
        View header = buildHeader();
        if (header != null) root.addView(header, Ui.matchWrap());
        scroll = new ScrollView(ctx);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        content = Ui.column(ctx);
        content.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 24));
        scroll.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        render();
        return root;
    }

    /** Optional fixed (non-scrolling) header, e.g. the Maintenance tabs. */
    protected View buildHeader() {
        return null;
    }

    /** Adds this screen's content to {@link #content}. */
    protected abstract void build();

    /**
     * Rebuilds the content. Keeps the scroll position, unless the guided demo has
     * moved on to a new part of the screen, in which case it scrolls there.
     */
    public final void render() {
        final int y = scroll.getScrollY();
        boolean firstRender = !rendered;
        String previousFocus = renderedFocus;
        focusTargets.clear();
        content.removeAllViews();
        renderedFocus = state.demoFocus();
        build();
        rendered = true;
        boolean focusMoved = !firstRender && renderedFocus != null && !renderedFocus.equals(previousFocus);
        if (focusMoved) scrollToFocus();
        else if (y > 0) scroll.post(() -> scroll.scrollTo(0, y));
    }

    private boolean rendered;

    /** Which state changes require this screen to rebuild. */
    protected boolean rendersOn(AppState.Change change) {
        switch (change) {
            case SELECTION:
            case ANALYSIS:
            case ALERTS:
            case TASKS:
            case REPORT:
            case MAINTENANCE_TAB:
                return true;
            default:
                return false;
        }
    }

    public void onStateChanged(AppState.Change change) {
        if (change == AppState.Change.DEMO) {
            // render() scrolls to the new focus itself.
            if (!Objects.equals(renderedFocus, state.demoFocus())) render();
            return;
        }
        if (rendersOn(change)) render();
    }

    /** Called once the screen is on screen. */
    public void onShown() {
        if (state.demoFocus() != null) scrollToFocus();
    }

    /** Called when the screen becomes visible or hidden (activity paused, navigated away). */
    public void setActive(boolean active) {
        this.active = active;
    }

    protected boolean isActive() {
        return active;
    }

    // ------------------------------------------------------------- helpers

    /** Adds an item to the screen with the standard 12 dp gap. */
    protected <T extends View> T add(T view) {
        return add(view, 12);
    }

    protected <T extends View> T add(T view, float gapDp) {
        Ui.add(content, view, content.getChildCount() == 0 ? 0 : gapDp);
        return view;
    }

    /** True the first time a key is seen on this screen: play the entry animation. */
    protected boolean firstTime(String key) {
        return played.add(key);
    }

    /** Registers a direct child of the content as a demo scroll target. */
    protected <T extends View> T focusTarget(String key, T view) {
        focusTargets.put(key, view);
        return view;
    }

    protected boolean isFocused(String key) {
        return key.equals(state.demoFocus());
    }

    protected int dp(float v) {
        return Ui.dp(ctx, v);
    }

    /** Scrolls so the view the demo is talking about is at the top. */
    public void scrollToFocus() {
        scroll.postDelayed(() -> {
            String key = state.demoFocus();
            View v = key == null ? null : focusTargets.get(key);
            if (v != null && v.getParent() == content) {
                scroll.smoothScrollTo(0, Math.max(0, v.getTop() - dp(8)));
            }
        }, 150);
    }
}
