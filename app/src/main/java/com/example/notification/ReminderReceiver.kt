package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.SkinCareDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.skinCareDataStore
import com.example.data.repository.SkinCareRepository
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val database = SkinCareDatabase.getInstance(context)
                val prefsRepo = UserPreferencesRepository(context.skinCareDataStore)
                val repo = SkinCareRepository(database.skinCareDao(), prefsRepo)
                val scheduler = ReminderScheduler(context)
                val settings = prefsRepo.getSettingsSnapshot()
                val today = RoutineScheduleHelper.currentLocalDate()

                if (action == NotificationHelper.ACTION_SNOOZE_REMINDER) {
                    val notifId = intent.getIntExtra(
                        NotificationHelper.EXTRA_NOTIFICATION_ID,
                        NotificationHelper.NOTIF_ID_MORNING
                    )
                    val reminderType = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_TYPE)
                        ?: NotificationHelper.TYPE_MORNING_ROUTINE
                    val targetPeriod = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_PERIOD)
                        ?: RoutinePeriod.MORNING.id
                    val snoozeMinutes = intent.getIntExtra(
                        NotificationHelper.EXTRA_SNOOZE_MINUTES,
                        settings.snoozeDurationMinutes
                    )

                    NotificationManagerCompat.from(context).cancel(notifId)
                    scheduler.scheduleSnoozeAlarm(
                        notificationId = notifId,
                        reminderType = reminderType,
                        targetPeriod = targetPeriod,
                        snoozeMinutes = snoozeMinutes,
                        useExact = settings.useExactAlarm
                    )
                    return@launch
                }

                if (action == NotificationHelper.ACTION_TRIGGER_REMINDER) {
                    val reminderType = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_TYPE)
                        ?: NotificationHelper.TYPE_MORNING_ROUTINE
                    val targetPeriod = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_PERIOD)
                        ?: RoutinePeriod.MORNING.id
                    val notifId = intent.getIntExtra(
                        NotificationHelper.EXTRA_NOTIFICATION_ID,
                        NotificationHelper.NOTIF_ID_MORNING
                    )

                    val isMorningDone = repo.isPeriodRoutineCompletedForDate(today, RoutinePeriod.MORNING)
                    val isEveningDone = repo.isPeriodRoutineCompletedForDate(today, RoutinePeriod.EVENING)
                    val scheduledTonight = RoutineScheduleHelper.getScheduledEveningProductForDate(today)

                    when (reminderType) {
                        NotificationHelper.TYPE_MORNING_ROUTINE,
                        NotificationHelper.TYPE_MORNING_FOLLOWUP -> {
                            if (!isMorningDone) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = notifId,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.MORNING.id,
                                    title = "Rutinitas Pagi • SkinCare Daily",
                                    body = "Urutan resep pagi: 1. Pembersih Labore AcnePro, 2. Moisturizer, 3. Bioderma Sébium Kerato+ tipis seluruh wajah, 4. Sunscreen Labore.",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }

                        NotificationHelper.TYPE_EVENING_ROUTINE,
                        NotificationHelper.TYPE_EVENING_FOLLOWUP -> {
                            if (!isEveningDone) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = notifId,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.EVENING.id,
                                    title = "Rutinitas Malam (${scheduledTonight.dayNameIndonesian})",
                                    body = "Urutan resep malam: 1. Pembersih, 2. Moisturizer, 3. ${scheduledTonight.productName}.",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }

                        NotificationHelper.TYPE_STEP_REMINDER -> {
                            val periodEnum = RoutinePeriod.fromId(targetPeriod)
                            val periodDone = if (periodEnum == RoutinePeriod.MORNING) isMorningDone else isEveningDone
                            if (!periodDone) {
                                val stepTitle = intent.getStringExtra(NotificationHelper.EXTRA_STEP_TITLE)
                                    ?: "Langkah Skincare"
                                val stepProduct = intent.getStringExtra(NotificationHelper.EXTRA_STEP_PRODUCT)
                                    ?: ""
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = notifId,
                                    reminderType = reminderType,
                                    targetPeriod = targetPeriod,
                                    title = "Langkah Berikutnya: $stepTitle",
                                    body = if (stepProduct.isNotBlank()) "Gunakan $stepProduct sesuai resep dokter kulit." else "Waktunya melanjutkan langkah rutinitas Anda.",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }

                        NotificationHelper.TYPE_SUNSCREEN_PRE_OUTDOOR -> {
                            if (settings.sunscreenReminderEnabled && !settings.sunscreenPaused) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = NotificationHelper.NOTIF_ID_SUNSCREEN_PRE_OUTDOOR,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.MORNING.id,
                                    title = "Sunscreen (20 Menit Sebelum Keluar Rumah)",
                                    body = "Oleskan Labore Biomeprotect Physical Sunscreen 20 menit sebelum keluar rumah sesuai catatan dokter.",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }

                        NotificationHelper.TYPE_SUNSCREEN_REAPPLY -> {
                            if (settings.sunscreenReminderEnabled && !settings.sunscreenPaused) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = NotificationHelper.NOTIF_ID_SUNSCREEN,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.MORNING.id,
                                    title = "Pengingat sunscreen (Oles Ulang)",
                                    body = "Aplikasikan ulang Labore Biomeprotect Physical Sunscreen setiap ${settings.sunscreenIntervalHours} jam sesuai catatan dokter.",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }

                        NotificationHelper.TYPE_MED_BREAKFAST -> {
                            val activeCourse = RoutineScheduleHelper.isMedicationCourseActiveOnDate(
                                isConfirmed = settings.medicationDiscrepancyConfirmed,
                                courseEnabled = settings.medicationReminderEnabled,
                                startDateIso = settings.medicationStartDateIso,
                                durationDays = settings.medicationDurationDays,
                                targetDate = today
                            )
                            if (activeCourse) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = NotificationHelper.NOTIF_ID_MED_BREAKFAST,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.MORNING.id,
                                    title = "Obat Minum Pagi: Doxihat 100 mg",
                                    body = "Diminum setelah makan pagi (2x sehari selama 2 minggu sesuai resep dokter).",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }

                        NotificationHelper.TYPE_MED_DINNER -> {
                            val activeCourse = RoutineScheduleHelper.isMedicationCourseActiveOnDate(
                                isConfirmed = settings.medicationDiscrepancyConfirmed,
                                courseEnabled = settings.medicationReminderEnabled,
                                startDateIso = settings.medicationStartDateIso,
                                durationDays = settings.medicationDurationDays,
                                targetDate = today
                            )
                            if (activeCourse) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = NotificationHelper.NOTIF_ID_MED_DINNER,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.EVENING.id,
                                    title = "Obat Minum Malam: Doxihat 100 mg",
                                    body = "Diminum setelah makan malam (2x sehari selama 2 minggu sesuai resep dokter).",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }

                        NotificationHelper.TYPE_DOCTOR_CONTROL -> {
                            if (settings.doctorControlReminderEnabled) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = NotificationHelper.NOTIF_ID_DOCTOR_CONTROL,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.MORNING.id,
                                    title = "Pengingat Kontrol Dokter (2 Minggu)",
                                    body = "Jadwal kontrol kembali ke dokter kulit setelah dua minggu menjalani terapi sesuai catatan resep.",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }
                    }

                    // Reschedule future occurrences cleanly
                    val activeTasks = database.skinCareDao().getActiveTasksSync()
                    scheduler.syncAllReminders(
                        settings = settings,
                        activeTasks = activeTasks,
                        isMorningCompletedToday = isMorningDone,
                        isEveningCompletedToday = isEveningDone
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
