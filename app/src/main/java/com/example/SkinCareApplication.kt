package com.example

import android.app.Application
import com.example.data.local.SkinCareDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.skinCareDataStore
import com.example.data.repository.SkinCareRepository
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import com.example.notification.NotificationHelper
import com.example.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SkinCareApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: SkinCareDatabase by lazy {
        SkinCareDatabase.getInstance(this)
    }

    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this.skinCareDataStore)
    }

    val repository: SkinCareRepository by lazy {
        SkinCareRepository(database.skinCareDao(), preferencesRepository)
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(this)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        applicationScope.launch(Dispatchers.IO) {
            repository.ensureSeeded()
            val today = RoutineScheduleHelper.currentLocalDate()
            val settings = preferencesRepository.getSettingsSnapshot()
            val isMorningDone = repository.isPeriodRoutineCompletedForDate(today, RoutinePeriod.MORNING)
            val isEveningDone = repository.isPeriodRoutineCompletedForDate(today, RoutinePeriod.EVENING)
            val activeTasks = database.skinCareDao().getActiveTasksSync()
            reminderScheduler.syncAllReminders(
                settings = settings,
                activeTasks = activeTasks,
                isMorningCompletedToday = isMorningDone,
                isEveningCompletedToday = isEveningDone
            )
        }
    }
}
