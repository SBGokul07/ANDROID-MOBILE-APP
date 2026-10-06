package com.aeromaintenance.ai.ui.shell;

import android.content.Context;
import android.widget.TextView;

import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;

/** Short confirmation message at the bottom of the screen. */
public class Snackbar extends TextView {

    private final Runnable hide = () -> animate().alpha(0f).translationY(Ui.dp(getContext(), 12)).setDuration(200)
            .withEndAction(() -> setVisibility(GONE)).start();

    public Snackbar(Context c) {
        super(c);
        Type.apply(this, Type.Style.BODY_MEDIUM);
        setTextColor(Palette.TEXT);
        setBackground(Ui.rounded(c, Palette.PANEL_RAISED, 10, Palette.HAIRLINE_STRONG, 1));
        int h = Ui.dp(c, 16);
        int v = Ui.dp(c, 14);
        setPadding(h, v, h, v);
        setElevation(Ui.dp(c, 6));
        setVisibility(GONE);
    }

    public void show(String message) {
        removeCallbacks(hide);
        setText(message);
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(Ui.dp(getContext(), 12));
        animate().alpha(1f).translationY(0).setDuration(200).start();
        postDelayed(hide, 2800);
    }
}
