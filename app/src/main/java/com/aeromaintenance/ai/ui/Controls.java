package com.aeromaintenance.ai.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aeromaintenance.ai.AppState;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.data.Aircraft;
import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.FleetRepository;

import java.util.function.Consumer;

/** Interactive controls shared by several screens. */
public final class Controls {

    private Controls() {
    }

    /** NORMAL DATA / ABNORMAL DATA switch (brief §13). */
    public static View conditionToggle(Context c, DataCondition current, Consumer<DataCondition> onChange) {
        LinearLayout box = Ui.row(c);
        box.setBackground(Ui.rounded(c, Palette.NIGHT, 12, Palette.HAIRLINE, 1));
        int p = Ui.dp(c, 4);
        box.setPadding(p, p, p, p);
        for (DataCondition dc : DataCondition.values()) {
            boolean selected = dc == current;
            int tint = dc == DataCondition.NORMAL ? Palette.GREEN : Palette.RED;
            LinearLayout seg = Ui.row(c);
            seg.setGravity(Gravity.CENTER);
            seg.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 10));
            seg.setBackground(Ui.rounded(c, selected ? Palette.alpha(tint, 0.18f) : 0x00000000, 9,
                    selected ? Palette.alpha(tint, 0.7f) : 0x00000000, 1));
            if (selected) {
                seg.addView(Ui.icon(c, R.drawable.ic_check, tint, 16));
                seg.addView(Ui.hspace(c, 6));
            }
            seg.addView(Ui.text(c, dc.label, Type.Style.LABEL_LARGE, selected ? tint : Palette.TEXT_MUTED));
            Ui.tappable(seg, 9, v -> {
                if (dc != current) onChange.accept(dc);
            });
            LinearLayout.LayoutParams lp = Ui.weight(1f);
            if (dc.ordinal() > 0) lp.leftMargin = Ui.dp(c, 4);
            box.addView(seg, lp);
        }
        return box;
    }

    /** Selected-aircraft picker shared by Monitoring, AI prediction, Maintenance and Reports. */
    public static View aircraftSelector(Context c, AppState state) {
        Aircraft a = state.aircraft(state.selectedId());
        LinearLayout row = Ui.row(c);
        row.setBackground(Ui.rounded(c, Palette.PANEL, 12, Palette.HAIRLINE, 1));
        row.setPadding(Ui.dp(c, 14), Ui.dp(c, 10), Ui.dp(c, 8), Ui.dp(c, 10));
        row.addView(Ui.dot(c, Palette.fleetStatus(a.status), 10));
        row.addView(Ui.hspace(c, 10));
        LinearLayout col = Ui.column(c);
        col.addView(Ui.text(c, a.id, Type.Style.TITLE_MEDIUM, Palette.TEXT));
        col.addView(Ui.text(c, a.model + ", " + a.base, Type.Style.BODY_SMALL, Palette.TEXT_MUTED));
        row.addView(col, Ui.weight(1f));
        row.addView(Ui.text(c, "Change", Type.Style.LABEL_MEDIUM, Palette.CYAN));
        row.addView(Ui.icon(c, R.drawable.ic_arrow_drop_down, Palette.CYAN, 24));
        row.setContentDescription("Selected aircraft " + a.id + ". Tap to change.");
        Ui.tappable(row, 12, v -> showAircraftMenu(v, state));
        return row;
    }

    private static void showAircraftMenu(View anchor, AppState state) {
        Context c = anchor.getContext();
        LinearLayout list = Ui.column(c);
        list.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
        ScrollView scroll = new ScrollView(c);
        scroll.addView(list);
        scroll.setBackground(Ui.rounded(c, Palette.PANEL_RAISED, 12, Palette.HAIRLINE_STRONG, 1));
        // Twelve rows do not fit on a small phone, so the list scrolls inside a fixed height.
        final PopupWindow popup = new PopupWindow(scroll, anchor.getWidth(), Ui.dp(c, 380), true);
        for (Aircraft item : FleetRepository.AIRCRAFT) {
            LinearLayout r = Ui.row(c);
            r.setPadding(Ui.dp(c, 14), Ui.dp(c, 11), Ui.dp(c, 14), Ui.dp(c, 11));
            r.addView(Ui.dot(c, Palette.fleetStatus(item.status), 8));
            r.addView(Ui.hspace(c, 10));
            boolean selected = item.id.equals(state.selectedId());
            TextView id = Ui.text(c, item.id, Type.Style.TITLE_SMALL, selected ? Palette.CYAN : Palette.TEXT);
            r.addView(id);
            r.addView(Ui.hspace(c, 8));
            r.addView(Ui.text(c, item.model, Type.Style.BODY_SMALL, Palette.TEXT_MUTED), Ui.weight(1f));
            r.addView(Ui.tag(c, item.risk.label, Palette.risk(item.risk)));
            Ui.tappable(r, 0, v -> {
                popup.dismiss();
                state.selectAircraft(item.id);
            });
            list.addView(r, Ui.matchWrap());
        }
        popup.setBackgroundDrawable(new ColorDrawable(0x00000000));
        popup.setElevation(Ui.dp(c, 8));
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(anchor, 0, Ui.dp(c, 4));
    }
}
