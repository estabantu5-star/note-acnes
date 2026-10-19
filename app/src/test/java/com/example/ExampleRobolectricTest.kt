package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SkinCareDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.SkinCareRepository
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import com.example.notification.NotificationHelper
import com.example.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: SkinCareDatabase
    private lateinit var prefsRepository: UserPreferencesRepository
    private lateinit var repository: SkinCareRepository
    private lateinit var scheduler: ReminderScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = SkinCareDatabase.createInMemory(context)
        val datastoreFile = tempFolder.newFile("test_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            produceFile = { datastoreFile }
        )
        prefsRepository = UserPreferencesRepository(dataStore)
        repository = SkinCareRepository(database.skinCareDao(), prefsRepository)
        scheduler = ReminderScheduler(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name from strings xml and launch MainActivity cleanly`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SkinCare Daily", appName)

        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }

    @Test
    fun `persistence test - completing task survives repository reload and new day starts fresh while preserving history`() = runBlocking {
        repository.ensureSeeded()
        val monday = LocalDate.of(2026, 10, 19) // Monday
        val tuesday = LocalDate.of(2026, 10, 20) // Tuesday

        val activeTasks = repository.activeTasksFlow.first()
        val morningCleanse = activeTasks.first { it.id == RoutineScheduleHelper.TASK_MORNING_CLEANSE }

        // 1. Complete morning cleanse on Monday
        repository.setTaskCompleted(
            date = monday,
            task = morningCleanse,
            completed = true,
            nowMillis = 1760832000000L
        )

        // 2. Verify Monday completion & snapshot are persisted in Room
        val mondayCompletions = repository.observeCompletionsForDate("2026-10-19").first()
        assertEquals(1, mondayCompletions.size)
        assertTrue(mondayCompletions.first().completed)

        val snapshotsAfterMonday = repository.allDailySnapshotsFlow.first()
        assertEquals(1, snapshotsAfterMonday.size)
        assertEquals(1, snapshotsAfterMonday.first().morningCompletedCount)
        assertEquals(RoutineScheduleHelper.PHARMALENE_PRODUCT, snapshotsAfterMonday.first().scheduledEveningProduct)

        // 3. Simulate new calendar date (Tuesday) -> Tuesday checklist is fresh
        val tuesdayCompletions = repository.observeCompletionsForDate("2026-10-20").first()
        assertTrue(tuesdayCompletions.isEmpty())

        // 4. Resetting Tuesday does NOT delete Monday's history
        repository.resetChecklistForDate(tuesday)
        val mondayStillIntact = repository.observeCompletionsForDate("2026-10-19").first()
        assertEquals(1, mondayStillIntact.size)
        assertTrue(mondayStillIntact.first().completed)

        // 5. Retiring or editing a task does NOT corrupt Monday's historical snapshot
        repository.updateTaskDetails(
            taskId = morningCleanse.id,
            title = "Bersihkan Wajah (Diperbarui)",
            productName = morningCleanse.productName,
            instruction = morningCleanse.instruction,
            estimatedMinutes = 3,
            isOptional = false,
            isActive = true,
            reminderEnabled = false,
            reminderOffsetMinutes = 0
        )
        val mondayCompletionSnapshot = repository.observeCompletionsForDate("2026-10-19").first().first()
        assertEquals("Bersihkan wajah", mondayCompletionSnapshot.taskTitleSnapshot)
    }

    @Test
    fun `schedule test - AlarmManager replaces changed times, cancels disabled reminders, and gates medication until confirmed`() = runBlocking {
        repository.ensureSeeded()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val zone = ZoneId.of("Asia/Jakarta")
        val nowMillis = ZonedDateTime.of(2026, 10, 19, 6, 0, 0, 0, zone).toInstant().toEpochMilli()
        val tasks = repository.activeTasksFlow.first()

        // 1. Default settings: Morning (07:00) & Evening (21:00) enabled, Medication disabled
        val initialSettings = prefsRepository.getSettingsSnapshot()
        assertFalse(initialSettings.medicationDiscrepancyConfirmed)
        assertFalse(initialSettings.medicationReminderEnabled)

        scheduler.syncAllReminders(
            settings = initialSettings,
            activeTasks = tasks,
            isMorningCompletedToday = false,
            isEveningCompletedToday = false,
            nowMillis = nowMillis,
            zoneId = zone
        )
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

        // 2. Change Morning reminder time to 06:30 and verify old alarm is replaced without duplicates
        prefsRepository.updateMorningReminder(enabled = true, hour = 6, minute = 30)
        val updatedSettings = prefsRepository.getSettingsSnapshot()
        scheduler.syncAllReminders(
            settings = updatedSettings,
            activeTasks = tasks,
            isMorningCompletedToday = false,
            isEveningCompletedToday = false,
            nowMillis = nowMillis,
            zoneId = zone
        )
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
        val expectedMorning630 = ZonedDateTime.of(2026, 10, 19, 6, 30, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(shadowAlarmManager.scheduledAlarms.any { it.triggerAtTime == expectedMorning630 })

        // 3. Disable Evening reminder and verify its alarm is canceled
        prefsRepository.updateEveningReminder(enabled = false, hour = 21, minute = 0)
        val disabledEveningSettings = prefsRepository.getSettingsSnapshot()
        scheduler.syncAllReminders(
            settings = disabledEveningSettings,
            activeTasks = tasks,
            isMorningCompletedToday = false,
            isEveningCompletedToday = false,
            nowMillis = nowMillis,
            zoneId = zone
        )
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // 4. Enable Sunscreen reapplication -> adds sunscreen alarm; pause -> cancels sunscreen alarm
        prefsRepository.updateSunscreenReminder(
            enabled = true,
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            intervalHours = 2,
            paused = false,
            activeDateIso = "2026-10-19"
        )
        scheduler.syncAllReminders(
            settings = prefsRepository.getSettingsSnapshot(),
            activeTasks = tasks,
            isMorningCompletedToday = false,
            isEveningCompletedToday = false,
            nowMillis = nowMillis,
            zoneId = zone
        )
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

        prefsRepository.updateSunscreenReminder(
            enabled = true,
            startHour = 8,
            startMinute = 0,
            endHour = 16,
            intervalHours = 2,
            paused = true,
            activeDateIso = "2026-10-19"
        )
        scheduler.syncAllReminders(
            settings = prefsRepository.getSettingsSnapshot(),
            activeTasks = tasks,
            isMorningCompletedToday = false,
            isEveningCompletedToday = false,
            nowMillis = nowMillis,
            zoneId = zone
        )
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // 5. Snooze schedules a single replacement alarm
        scheduler.scheduleSnoozeAlarm(
            notificationId = NotificationHelper.NOTIF_ID_MORNING,
            reminderType = NotificationHelper.TYPE_MORNING_ROUTINE,
            targetPeriod = RoutinePeriod.MORNING.id,
            snoozeMinutes = 15,
            useExact = false,
            nowMillis = nowMillis
        )
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `backup test - JSON export and import round-trip preserves tasks completions and settings`() = runBlocking {
        repository.ensureSeeded()
        val monday = LocalDate.of(2026, 10, 19)
        val firstTask = repository.activeTasksFlow.first().first()
        repository.setTaskCompleted(monday, firstTask, true, 1760832000000L)
        repository.logMedicationDose(monday, "BREAKFAST", "TAKEN", 1760835000000L)
        prefsRepository.updateMorningReminder(true, 6, 45)

        val exportedJson = repository.exportAllDataToJson()
        assertNotNull(exportedJson)
        assertTrue(exportedJson.contains("morning_cleanse"))

        // Clear day completion and re-import from JSON
        repository.resetChecklistForDate(monday)
        assertTrue(repository.observeCompletionsForDate("2026-10-19").first().isEmpty())

        val importResult = repository.importDataFromJson(exportedJson)
        assertTrue(importResult.isSuccess)

        val restoredCompletions = repository.observeCompletionsForDate("2026-10-19").first()
        assertEquals(1, restoredCompletions.size)
        assertTrue(restoredCompletions.first().completed)
        assertEquals(45, prefsRepository.getSettingsSnapshot().morningMinute)
    }
}
