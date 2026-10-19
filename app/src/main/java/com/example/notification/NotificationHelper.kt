package com.example.notification

import android.Manifest
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
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID = "skincare_reminders_channel"
    const val CHANNEL_NAME = "Pengingat Skincare"
    const val CHANNEL_DESCRIPTION =
        "Pengingat jadwal rutinitas perawatan kulit pagi, malam, tabir surya, obat minum Doxihat, dan kontrol 2 minggu sesuai resep dokter."

    const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.example.ACTION_SNOOZE_REMINDER"

    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val EXTRA_TARGET_PERIOD = "extra_target_period"
    const val EXTRA_STEP_TASK_ID = "extra_step_task_id"
    const val EXTRA_STEP_TITLE = "extra_step_title"
    const val EXTRA_STEP_PRODUCT = "extra_step_product"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    const val TYPE_MORNING_ROUTINE = "MORNING_ROUTINE"
    const val TYPE_EVENING_ROUTINE = "EVENING_ROUTINE"
    const val TYPE_MORNING_FOLLOWUP = "MORNING_FOLLOWUP"
    const val TYPE_EVENING_FOLLOWUP = "EVENING_FOLLOWUP"
    const val TYPE_STEP_REMINDER = "STEP_REMINDER"
    const val TYPE_SUNSCREEN_PRE_OUTDOOR = "SUNSCREEN_PRE_OUTDOOR"
    const val TYPE_SUNSCREEN_REAPPLY = "SUNSCREEN_REAPPLY"
    const val TYPE_MED_BREAKFAST = "MED_BREAKFAST"
    const val TYPE_MED_DINNER = "MED_DINNER"
    const val TYPE_DOCTOR_CONTROL = "DOCTOR_CONTROL"
    const val TYPE_TEST_NOTIFICATION = "TEST_NOTIFICATION"

    const val NOTIF_ID_MORNING = 1001
    const val NOTIF_ID_EVENING = 1002
    const val NOTIF_ID_MORNING_FOLLOWUP = 1003
    const val NOTIF_ID_EVENING_FOLLOWUP = 1004
    const val NOTIF_ID_SUNSCREEN = 1005
    const val NOTIF_ID_MED_BREAKFAST = 1006
    const val NOTIF_ID_MED_DINNER = 1007
    const val NOTIF_ID_DOCTOR_CONTROL = 1008
    const val NOTIF_ID_SUNSCREEN_PRE_OUTDOOR = 1009
    const val NOTIF_ID_TEST = 1099

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return runtimeGranted && appNotificationsEnabled
    }

    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        reminderType: String,
        targetPeriod: String,
        title: String,
        body: String,
        hideSensitiveContent: Boolean,
        snoozeMinutes: Int
    ): Boolean {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) {
            return false
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_PERIOD, targetPeriod)
            putExtra(EXTRA_REMINDER_TYPE, reminderType)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_TYPE, reminderType)
            putExtra(EXTRA_TARGET_PERIOD, targetPeriod)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 5000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayBody = if (hideSensitiveContent) {
            when (reminderType) {
                TYPE_MED_BREAKFAST, TYPE_MED_DINNER ->
                    "Waktunya jadwal obat minum harian Anda setelah makan. Ketuk untuk membuka aplikasi."
                TYPE_SUNSCREEN_PRE_OUTDOOR, TYPE_SUNSCREEN_REAPPLY ->
                    "Waktunya mengoleskan pelindung matahari sesuai catatan dokter."
                TYPE_DOCTOR_CONTROL ->
                    "Jadwal kontrol kembali ke dokter setelah 2 minggu terapi."
                else ->
                    "Waktunya menjalankan langkah perawatan kulit terjadwal Anda."
            }
        } else {
            body
        }

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SkinCare Daily")
            .setContentText("Pengingat jadwal rutinitas perawatan kulit.")
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(displayBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setVisibility(
                if (hideSensitiveContent) NotificationCompat.VISIBILITY_PRIVATE
                else NotificationCompat.VISIBILITY_PUBLIC
            )
            .setPublicVersion(publicVersion)
            .addAction(
                0,
                context.getString(R.string.action_open_routine),
                openPendingIntent
            )

        if (reminderType != TYPE_TEST_NOTIFICATION) {
            builder.addAction(
                0,
                "${context.getString(R.string.action_snooze)} (${snoozeMinutes}m)",
                snoozePendingIntent
            )
        }

        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        }.getOrDefault(false)
    }
}
