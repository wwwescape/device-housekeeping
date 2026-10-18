package com.wwwescape.devicehousekeeping.data.schedule

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
import com.wwwescape.devicehousekeeping.MainActivity
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.util.formatBytes

/** A quiet, low-importance "cleaned X in the background" note after each automatic run. */
object AutoCleanNotifier {

    private const val CHANNEL_ID = "auto_clean"
    private const val NOTIFICATION_ID = 1

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Checked by canNotify() just below.
    fun notifyCleaned(context: Context, freedBytes: Long, items: Int) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_auto_clean), NotificationManager.IMPORTANCE_LOW)
                .apply { description = context.getString(R.string.notification_channel_auto_clean_description) },
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clean)
            .setContentTitle(context.getString(R.string.notification_auto_clean_title, formatBytes(freedBytes)))
            .setContentText(context.resources.getQuantityString(R.plurals.notification_auto_clean_text, items, items))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}
