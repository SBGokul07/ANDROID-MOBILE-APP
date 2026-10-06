package com.aeromaintenance.ai.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;

/**
 * Filled (primary) or outlined (secondary) button with an optional icon.
 * Built from platform views so the app needs no support libraries.
 */
public class ButtonView extends LinearLayout {

    private final boolean primary;
    private final int color;
    private final ImageView icon;
    private final TextView label;
    private final GradientDrawable shape = new GradientDrawable();

    public ButtonView(Context context, String text, int iconRes, boolean primary, int color) {
        super(context);
        this.primary = primary;
        this.color = color;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        int padH = Ui.dp(context, primary ? 18 : 14);
        setPadding(padH, 0, padH, 0);
        setMinimumHeight(Ui.dp(context, primary ? 52 : 48));
        setClickable(true);
        setFocusable(true);

        shape.setCornerRadius(Ui.dp(context, 12));
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(Ui.dp(context, 12));
        int rippleColor = primary ? Palette.alpha(Palette.NIGHT, 0.18f) : Palette.alpha(color, 0.18f);
        setBackground(new RippleDrawable(ColorStateList.valueOf(rippleColor), shape, mask));

        icon = new ImageView(context);
        if (iconRes != 0) {
            icon.setImageResource(iconRes);
            int size = Ui.dp(context, primary ? 20 : 18);
            LayoutParams lp = new LayoutParams(size, size);
            lp.rightMargin = Ui.dp(context, primary ? 8 : 6);
            addView(icon, lp);
        }
        label = new TextView(context);
        Type.apply(label, Type.Style.LABEL_LARGE);
        if (primary) label.setTypeface(Type.font(context, com.aeromaintenance.ai.R.font.barlow_bold));
        label.setText(text);
        label.setMaxLines(1);
        label.setGravity(Gravity.CENTER);
        addView(label, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        applyState();
    }

    public void setText(String text) {
        label.setText(text);
    }

    public void setIcon(int iconRes) {
        icon.setImageResource(iconRes);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        applyState();
    }

    private void applyState() {
        boolean on = isEnabled();
        int content;
        if (primary) {
            shape.setColor(on ? color : Palette.HAIRLINE);
            shape.setStroke(0, 0);
            content = on ? Palette.NIGHT : Palette.TEXT_FAINT;
        } else {
            shape.setColor(0x00000000);
            shape.setStroke(Ui.dp(getContext(), 1), on ? Palette.alpha(color, 0.6f) : Palette.HAIRLINE);
            content = on ? color : Palette.TEXT_FAINT;
        }
        label.setTextColor(content);
        icon.setImageTintList(ColorStateList.valueOf(content));
    }
}
