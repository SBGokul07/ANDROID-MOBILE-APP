package com.aeromaintenance.ai.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.ui.widget.ButtonView;
import com.aeromaintenance.ai.ui.widget.DialView;

import java.util.List;

/**
 * Small UI toolkit: every screen is built from these helpers so the look stays
 * consistent. Everything uses plain Android SDK views; no support libraries.
 */
public final class Ui {

    private Ui() {
    }

    // ------------------------------------------------------------------ units

    public static int dp(Context c, float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics()));
    }

    // ----------------------------------------------------------------- layout

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    /** Horizontal row with vertically centred children. */
    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** Horizontal weight inside a row. */
    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    public static LinearLayout.LayoutParams size(Context c, float wDp, float hDp) {
        return new LinearLayout.LayoutParams(dp(c, wDp), dp(c, hDp));
    }

    /** Fixed width, natural height. */
    public static LinearLayout.LayoutParams width(Context c, float wDp) {
        return new LinearLayout.LayoutParams(dp(c, wDp), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static View vspace(Context c, float dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dp)));
        return v;
    }

    public static View hspace(Context c, float dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, dp), 1));
        return v;
    }

    /** Flexible gap that pushes the following views to the end of a row. */
    public static View fill(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    /** Adds {@code child} full-width with a top margin. */
    public static void add(LinearLayout parent, View child, float topDp) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(parent.getContext(), topDp);
        parent.addView(child, lp);
    }

    /** Adds the views to a row with a gap between them, each taking an equal share. */
    public static LinearLayout equalRow(Context c, float gapDp, View... views) {
        LinearLayout row = row(c);
        for (int i = 0; i < views.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = dp(c, gapDp);
            row.addView(views[i], lp);
        }
        return row;
    }

    // ------------------------------------------------------------------ shapes

    public static GradientDrawable rounded(Context c, int fill, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) d.setStroke(dp(c, strokeDp), stroke);
        return d;
    }

    public static GradientDrawable circle(int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        return d;
    }

    /** Background with a ripple on top of {@code content}. */
    public static RippleDrawable ripple(Context c, Drawable content, float radiusDp) {
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(dp(c, radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(Palette.alpha(Palette.TEXT, 0.10f)), content, mask);
    }

    /** Makes a view tappable with a ripple drawn over it. */
    public static <T extends View> T tappable(T v, float radiusDp, View.OnClickListener l) {
        v.setOnClickListener(l);
        v.setClickable(true);
        v.setForeground(ripple(v.getContext(), null, radiusDp));
        return v;
    }

    // -------------------------------------------------------------------- text

    public static TextView text(Context c, CharSequence s, Type.Style style, int color) {
        TextView t = new TextView(c);
        Type.apply(t, style);
        t.setTextColor(color);
        t.setText(s);
        return t;
    }

    public static TextView singleLine(Context c, CharSequence s, Type.Style style, int color) {
        TextView t = text(c, s, style, color);
        t.setMaxLines(1);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    /** Status tag in the brief's vocabulary (HIGH, WARNING, NORMAL…). */
    public static TextView tag(Context c, String s, int color, boolean filled) {
        TextView t = text(c, s, Type.Style.TAG, filled ? Palette.NIGHT : color);
        t.setMaxLines(1);
        t.setBackground(rounded(c, filled ? color : Palette.alpha(color, 0.14f), 6,
                Palette.alpha(color, filled ? 1f : 0.45f), 1));
        t.setPadding(dp(c, 8), dp(c, 3), dp(c, 8), dp(c, 3));
        t.setLayoutParams(wrap());
        return t;
    }

    public static TextView tag(Context c, String s, int color) {
        return tag(c, s, color, false);
    }

    public static View dot(Context c, int color, float sizeDp) {
        View v = new View(c);
        v.setBackground(circle(color));
        v.setLayoutParams(size(c, sizeDp, sizeDp));
        return v;
    }

    public static ImageView icon(Context c, int res, int color, float sizeDp) {
        ImageView i = new ImageView(c);
        i.setImageResource(res);
        i.setImageTintList(ColorStateList.valueOf(color));
        i.setLayoutParams(size(c, sizeDp, sizeDp));
        return i;
    }

    public static View divider(Context c, int color) {
        View v = new View(c);
        v.setBackgroundColor(color);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1))));
        return v;
    }

    // ----------------------------------------------------------------- buttons

    public static ButtonView primary(Context c, String s, int icon, View.OnClickListener l) {
        return primary(c, s, icon, Palette.CYAN, l);
    }

    public static ButtonView primary(Context c, String s, int icon, int color, View.OnClickListener l) {
        ButtonView b = new ButtonView(c, s, icon, true, color);
        b.setOnClickListener(l);
        return b;
    }

    public static ButtonView secondary(Context c, String s, int icon, View.OnClickListener l) {
        ButtonView b = new ButtonView(c, s, icon, false, Palette.CYAN);
        b.setOnClickListener(l);
        return b;
    }

    /** Square icon button for toolbars (48 dp touch target). */
    public static ImageView iconButton(Context c, int res, int color, String description, View.OnClickListener l) {
        ImageView i = new ImageView(c);
        i.setImageResource(res);
        i.setImageTintList(ColorStateList.valueOf(color));
        i.setScaleType(ImageView.ScaleType.CENTER);
        i.setContentDescription(description);
        i.setLayoutParams(size(c, 48, 48));
        GradientDrawable mask = circle(0xFFFFFFFF);
        i.setBackground(new RippleDrawable(ColorStateList.valueOf(Palette.alpha(Palette.TEXT, 0.12f)), null, mask));
        i.setOnClickListener(l);
        return i;
    }

    // --------------------------------------------------------------- composites

    public static View sectionHeader(Context c, String title, String supporting, String action, View.OnClickListener onAction) {
        LinearLayout row = row(c);
        row.setPadding(0, dp(c, 8), 0, dp(c, 2));
        LinearLayout col = column(c);
        col.addView(text(c, title, Type.Style.TITLE_MEDIUM, Palette.TEXT));
        if (supporting != null) col.addView(text(c, supporting, Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
        row.addView(col, weight(1f));
        if (action != null && onAction != null) {
            TextView a = text(c, action, Type.Style.LABEL_LARGE, Palette.CYAN);
            a.setPadding(dp(c, 8), dp(c, 6), dp(c, 8), dp(c, 6));
            tappable(a, 8, onAction);
            row.addView(a);
        }
        return row;
    }

    public static View sectionHeader(Context c, String title, String supporting) {
        return sectionHeader(c, title, supporting, null, null);
    }

    public static View keyValue(Context c, String label, String value, int valueColor) {
        LinearLayout col = column(c);
        col.addView(text(c, label, Type.Style.LABEL_SMALL, Palette.TEXT_MUTED));
        col.addView(vspace(c, 2));
        col.addView(singleLine(c, value, Type.Style.READOUT_SMALL, valueColor));
        return col;
    }

    public static View bullet(Context c, String s, int dotColor, int textColor) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(c, 3), 0, dp(c, 3));
        View dot = dot(c, dotColor, 6);
        LinearLayout.LayoutParams dlp = size(c, 6, 6);
        dlp.topMargin = dp(c, 8);
        dlp.rightMargin = dp(c, 10);
        row.addView(dot, dlp);
        row.addView(text(c, s, Type.Style.BODY_MEDIUM, textColor), weight(1f));
        return row;
    }

    public static View bullet(Context c, String s, int dotColor) {
        return bullet(c, s, dotColor, Palette.TEXT);
    }

    /** Honest labelling, shown wherever the AI produces an output. */
    public static View prototypeNote(Context c, String s) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(c, 6), 0, dp(c, 6));
        ImageView i = icon(c, R.drawable.ic_info, Palette.TEXT_FAINT, 16);
        LinearLayout.LayoutParams ilp = size(c, 16, 16);
        ilp.topMargin = dp(c, 1);
        ilp.rightMargin = dp(c, 8);
        row.addView(i, ilp);
        row.addView(text(c, s, Type.Style.BODY_SMALL, Palette.TEXT_FAINT), weight(1f));
        return row;
    }

    /** Rounded filter chip ("All 12", "Critical 1"…). */
    public static TextView filterPill(Context c, String s, boolean selected, int color, View.OnClickListener l) {
        TextView t = text(c, s, Type.Style.LABEL_LARGE, selected ? color : Palette.TEXT_MUTED);
        t.setBackground(rounded(c, selected ? Palette.alpha(color, 0.18f) : Palette.PANEL, 20,
                selected ? color : Palette.HAIRLINE, 1));
        t.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        tappable(t, 20, l);
        return t;
    }

    /** Horizontally scrolling row of filter chips. */
    public static HorizontalScrollView pillRow(Context c, List<? extends View> pills) {
        HorizontalScrollView scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout row = row(c);
        for (int i = 0; i < pills.size(); i++) {
            LinearLayout.LayoutParams lp = wrap();
            if (i > 0) lp.leftMargin = dp(c, 8);
            row.addView(pills.get(i), lp);
        }
        scroll.addView(row);
        return scroll;
    }

    /**
     * A dial with a read-out placed on top of it, centred near the bottom of the
     * dial (where a cockpit gauge shows its digital value).
     */
    public static FrameLayout dialBox(Context c, DialView dial, View overlay, float overlayBottomDp) {
        FrameLayout box = new FrameLayout(c);
        box.addView(dial, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (overlay != null) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.bottomMargin = dp(c, overlayBottomDp);
            box.addView(overlay, lp);
        }
        return box;
    }

    /** Lower-case a sentence and capitalise its first letter. */
    public static String sentence(String s) {
        if (s == null || s.isEmpty()) return s;
        String lower = s.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
