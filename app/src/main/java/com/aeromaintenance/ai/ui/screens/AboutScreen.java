package com.aeromaintenance.ai.ui.screens;

import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.ui.Palette;
import com.aeromaintenance.ai.ui.Type;
import com.aeromaintenance.ai.ui.Ui;
import com.aeromaintenance.ai.ui.widget.PanelView;

/** About page with the academic-prototype disclaimer. */
public class AboutScreen extends BaseScreen {

    public AboutScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected boolean rendersOn(AppState.Change change) {
        return false;
    }

    @Override
    protected void build() {
        LinearLayout title = Ui.column(ctx);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, dp(12), 0, dp(12));
        ImageView logo = new ImageView(ctx);
        logo.setImageResource(R.drawable.ic_launcher_foreground);
        logo.setBackground(Ui.rounded(ctx, 0xFF0B1A2E, 24, 0, 0));
        logo.setClipToOutline(true);
        title.addView(logo, Ui.size(ctx, 96, 96));
        Ui.add(title, centred(Ui.text(ctx, "AeroMaintenance AI", Type.Style.HEADLINE_MEDIUM, Palette.TEXT)), 14);
        title.addView(centred(Ui.text(ctx, "Version 2.0 prototype (Java)", Type.Style.BODY_SMALL, Palette.TEXT_MUTED)), Ui.matchWrap());
        TextView desc = centred(Ui.text(ctx, "An AI-assisted predictive maintenance prototype for aircraft health "
                + "monitoring and component risk assessment.", Type.Style.BODY_LARGE, Palette.TEXT));
        desc.setPadding(dp(8), 0, dp(8), 0);
        Ui.add(title, desc, 12);
        add(title);

        PanelView disclaimer = new PanelView(ctx).setAccent(Palette.AMBER);
        LinearLayout head = Ui.row(ctx);
        head.addView(Ui.icon(ctx, R.drawable.ic_warning, Palette.AMBER, 24));
        head.addView(Ui.hspace(ctx, 10));
        head.addView(Ui.text(ctx, "IMPORTANT", Type.Style.TAG, Palette.AMBER));
        disclaimer.addView(head, Ui.matchWrap());
        Ui.add(disclaimer, Ui.text(ctx, "This application is an academic research prototype using simulated/synthetic "
                + "data. It is not certified for real-world aviation safety, maintenance, or flight-critical "
                + "decision-making.", Type.Style.BODY_LARGE, Palette.TEXT), 8);
        add(disclaimer);

        PanelView facts = new PanelView(ctx);
        fact(facts, "Data", "All aircraft, telemetry, alerts and work orders are generated on the phone. No aircraft or "
                + "airline data source is connected.");
        fact(facts, "AI", "Prototype / Simulated AI Inference: a deterministic statistical pipeline that stands in for a "
                + "trained time-series neural network.");
        fact(facts, "Fleet", "12 fictional aircraft, AERO-101 to AERO-112. Type names are used only to make flight hours "
                + "and cycles realistic.");
        fact(facts, "Built with", "Java and the Android SDK only, no third-party libraries. Every chart and dial is drawn "
                + "on a Canvas. Runs offline on Android 8.0 and later.");
        fact(facts, "Typeface", "Barlow by Jeremy Tribby, SIL Open Font License. Icons: Material Icons, Apache License 2.0.");
        add(facts);
    }

    private TextView centred(TextView t) {
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        return t;
    }

    private void fact(LinearLayout panel, String k, String v) {
        LinearLayout col = Ui.column(ctx);
        col.setPadding(0, dp(6), 0, dp(6));
        col.addView(Ui.text(ctx, k, Type.Style.LABEL_MEDIUM, Palette.CYAN));
        col.addView(Ui.text(ctx, v, Type.Style.BODY_MEDIUM, Palette.TEXT_MUTED));
        panel.addView(col, Ui.matchWrap());
    }
}
