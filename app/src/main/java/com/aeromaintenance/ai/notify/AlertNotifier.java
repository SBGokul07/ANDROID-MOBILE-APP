package com.aeromaintenance.ai.notify;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.aeromaintenance.ai.MainActivity;
import com.aeromaintenance.ai.R;
import com.aeromaintenance.ai.data.Alert;
import com.aeromaintenance.ai.ui.Palette;

/** Posts AI alerts as Android notifications (the "mobile alert" stage of the pipeline). */
public final class AlertNotifier {

    private AlertNotifier() {
    }

    private static final String CHANNEL_ID = "ai_alerts";

    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notification_channel_alerts), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notification_channel_alerts_desc));
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    /** Android 13+ needs the POST_NOTIFICATIONS runtime permission. */
    public static boolean canPost(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission") // checked in canPost()
    public static void post(Context context, Alert alert) {
        if (!canPost(context)) return;
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("screen", "alerts");
        PendingIntent pending = PendingIntent.getActivity(context, alert.id.hashCode(), open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Palette.severity(alert.severity))
                .setContentTitle(alert.severity.label + ": " + alert.title)
                .setContentText(alert.aircraftId + ": " + alert.recommendedAction)
                .setStyle(new Notification.BigTextStyle().bigText(
                        alert.aircraftId + " – " + alert.component + "\n" + alert.explanation
                                + "\n\nRecommended: " + alert.recommendedAction))
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build();
        try {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.notify(alert.id.hashCode(), notification);
        } catch (SecurityException ignored) {
            // Permission revoked between the check and the call: the in-app alert still exists.
        }
    }
}
