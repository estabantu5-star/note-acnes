package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

/**
 * Restores and recalculates scheduled reminders after phone reboot, app update,
 * manual clock change, or timezone change without creating duplicate alarms.
 */
class BootAndTimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (action !in supportedActions) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                NotificationHelper.createNotificationChannel(context)
                val database = SkinCareDatabase.getInstance(context)
                val prefsRepo = UserPreferencesRepository(context.skinCareDataStore)
                val repo = SkinCareRepository(database.skinCareDao(), prefsRepo)
                repo.ensureSeeded()

                val settings = prefsRepo.getSettingsSnapshot()
                val today = RoutineScheduleHelper.currentLocalDate()
                val isMorningDone = repo.isPeriodRoutineCompletedForDate(today, RoutinePeriod.MORNING)
                val isEveningDone = repo.isPeriodRoutineCompletedForDate(today, RoutinePeriod.EVENING)
                val activeTasks = database.skinCareDao().getActiveTasksSync()

                val scheduler = ReminderScheduler(context)
                scheduler.syncAllReminders(
                    settings = settings,
                    activeTasks = activeTasks,
                    isMorningCompletedToday = isMorningDone,
                    isEveningCompletedToday = isEveningDone
                )
                prefsRepo.recordReminderSyncTimestamp()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
