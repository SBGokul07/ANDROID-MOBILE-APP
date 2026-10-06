package com.aeromaintenance.ai.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aeromaintenance.ai.MainActivity
import com.aeromaintenance.ai.R
import com.aeromaintenance.ai.data.Alert
import com.aeromaintenance.ai.data.Severity

/** Posts AI alerts as Android notifications ("mobile alert" stage of the pipeline). */
object AlertNotifier {
    private const val CHANNEL_ID = "ai_alerts"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.notification_channel_alerts_desc) }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked in canPost()
    fun post(context: Context, alert: Alert) {
        if (!canPost(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_SCREEN, "alerts")
        }
        val pending = PendingIntent.getActivity(
            context, alert.id.hashCode(), open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val color = when (alert.severity) {
            Severity.CRITICAL -> 0xFFFF5A4F.toInt()
            Severity.WARNING -> 0xFFFF9A3C.toInt()
            Severity.MONITOR -> 0xFFF2C744.toInt()
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(color)
            .setContentTitle("${alert.severity.label}: ${alert.title}")
            .setContentText("${alert.aircraftId}: ${alert.recommendedAction}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${alert.aircraftId} – ${alert.component}\n${alert.explanation}\n\nRecommended: ${alert.recommendedAction}",
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: the in-app alert still exists.
        }
    }
}
