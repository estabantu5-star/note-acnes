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
                            // Do not send unnecessary reminder if morning routine is already completed
                            if (!isMorningDone) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = notifId,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.MORNING.id,
                                    title = "Rutinitas Pagi • SkinCare Daily",
                                    body = "Waktunya perawatan pagi: Labore Cleansing Mousse, Topicalm Cream, Bioderma Sébium Kerato+, dan Physical Sunscreen.",
                                    hideSensitiveContent = settings.hideSensitiveNotificationContent,
                                    snoozeMinutes = settings.snoozeDurationMinutes
                                )
                            }
                        }

                        NotificationHelper.TYPE_EVENING_ROUTINE,
                        NotificationHelper.TYPE_EVENING_FOLLOWUP -> {
                            // Do not send unnecessary reminder if evening routine is already completed
                            if (!isEveningDone) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = notifId,
                                    reminderType = reminderType,
                                    targetPeriod = RoutinePeriod.EVENING.id,
                                    title = "Rutinitas Malam (${scheduledTonight.dayNameIndonesian})",
                                    body = "Jadwal produk aktif malam ini: ${scheduledTonight.productName}. Awali dengan pembersih dan pelembap Labore.",
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
                                    body = if (stepProduct.isNotBlank()) "Gunakan $stepProduct sesuai anjuran dokter kulit." else "Waktunya melanjutkan langkah rutinitas Anda.",
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
                                    title = "Pengingat sunscreen",
                                    body = "Saat beraktivitas di luar ruangan, oleskan ulang Labore Physical Sunscreen setiap ${settings.sunscreenIntervalHours} jam.",
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
                                    title = "Pengingat Obat Pagi (Sehabis Sarapan)",
                                    body = "Jadwal Dohixat 100 mg sehabis sarapan. Catat status konsumsi secara manual di aplikasi.",
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
                                    title = "Pengingat Obat Malam (Sehabis Makan Malam)",
                                    body = "Jadwal Dohixat 100 mg sehabis makan malam. Catat status konsumsi secara manual di aplikasi.",
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
