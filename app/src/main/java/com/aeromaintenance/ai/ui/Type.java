package com.aeromaintenance.ai.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.SparseArray;
import android.util.TypedValue;
import android.widget.TextView;

import com.aeromaintenance.ai.R;

/**
 * Typography. Barlow is a DIN-like grotesque with roots in highway and transport
 * signage; the semi-condensed cut is used for titles and instrument read-outs.
 */
public final class Type {

    private Type() {
    }

    /** The type scale (sizes in sp, letter spacing in em). */
    public enum Style {
        DISPLAY_SMALL(R.font.barlow_semicondensed_semibold, 34, 38, 0f, false),
        HEADLINE_MEDIUM(R.font.barlow_semicondensed_semibold, 28, 32, 0f, false),
        HEADLINE_SMALL(R.font.barlow_semicondensed_semibold, 24, 28, 0f, false),
        TITLE_LARGE(R.font.barlow_semicondensed_semibold, 21, 26, 0f, false),
        TITLE_MEDIUM(R.font.barlow_semicondensed_semibold, 18, 22, 0f, false),
        TITLE_SMALL(R.font.barlow_semibold, 15, 20, 0f, false),
        BODY_LARGE(R.font.barlow_regular, 16, 23, 0f, false),
        BODY_MEDIUM(R.font.barlow_regular, 15, 21, 0f, false),
        BODY_SMALL(R.font.barlow_regular, 13, 18, 0f, false),
        LABEL_LARGE(R.font.barlow_semibold, 15, 20, 0f, false),
        LABEL_MEDIUM(R.font.barlow_medium, 13, 16, 0f, false),
        LABEL_SMALL(R.font.barlow_medium, 12, 15, 0f, false),
        /** Large numeric read-out (fleet health, risk, RUL). */
        READOUT(R.font.barlow_semicondensed_semibold, 48, 50, 0f, true),
        READOUT_MEDIUM(R.font.barlow_semicondensed_semibold, 30, 34, 0f, true),
        READOUT_SMALL(R.font.barlow_semicondensed_semibold, 20, 24, 0f, true),
        /** Severity and risk tags: HIGH, CRITICAL, NORMAL… */
        TAG(R.font.barlow_semicondensed_bold, 12, 14, 0.06f, false),
        NUMERIC(R.font.barlow_medium, 14, 18, 0f, true);

        public final int font;
        public final float sizeSp;
        public final float lineHeightSp;
        public final float letterSpacingEm;
        public final boolean tabular;

        Style(int font, float sizeSp, float lineHeightSp, float letterSpacingEm, boolean tabular) {
            this.font = font;
            this.sizeSp = sizeSp;
            this.lineHeightSp = lineHeightSp;
            this.letterSpacingEm = letterSpacingEm;
            this.tabular = tabular;
        }
    }

    private static final SparseArray<Typeface> CACHE = new SparseArray<>();

    /** Loads a font from res/font once and caches it. */
    public static Typeface font(Context context, int fontRes) {
        Typeface t = CACHE.get(fontRes);
        if (t == null) {
            try {
                t = context.getResources().getFont(fontRes);
            } catch (RuntimeException e) {
                t = Typeface.DEFAULT;
            }
            CACHE.put(fontRes, t);
        }
        return t;
    }

    /** Read-outs, tags and numbers are always one line. */
    public static boolean isSingleLine(Style style) {
        switch (style) {
            case READOUT:
            case READOUT_MEDIUM:
            case READOUT_SMALL:
            case TAG:
            case NUMERIC:
                return true;
            default:
                return false;
        }
    }

    public static void apply(TextView tv, Style style) {
        Context c = tv.getContext();
        tv.setTypeface(font(c, style.font));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.sizeSp);
        tv.setLetterSpacing(style.letterSpacingEm);
        tv.setFontFeatureSettings(style.tabular ? "tnum" : null);
        tv.setIncludeFontPadding(false);
        if (isSingleLine(style)) {
            // Natural font height, never wrapped: large digits must not be split or clipped.
            tv.setSingleLine(true);
            return;
        }
        int lineHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                style.lineHeightSp, c.getResources().getDisplayMetrics()));
        if (Build.VERSION.SDK_INT >= 28) {
            tv.setLineHeight(lineHeight);
        } else {
            int natural = tv.getPaint().getFontMetricsInt(null);
            tv.setLineSpacing(Math.max(0, lineHeight - natural), 1f);
        }
    }
}
