package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.skinCareDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "skincare_daily_preferences"
)

enum class ThemeMode(val id: String, val labelId: String) {
    SYSTEM("SYSTEM", "Ikuti Sistem"),
    LIGHT("LIGHT", "Terang"),
    DARK("DARK", "Gelap");

    companion object {
        fun fromId(id: String): ThemeMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
    }
}

data class UserReminderAndAppSettings(
    val firstRunOnboardingDone: Boolean = false,
    val notificationPermissionAskedOnce: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hideSensitiveNotificationContent: Boolean = false,

    // Morning Routine Reminder (Default: 07:00)
    val morningReminderEnabled: Boolean = true,
    val morningHour: Int = 7,
    val morningMinute: Int = 0,

    // Evening Routine Reminder (Default: 21:00)
    val eveningReminderEnabled: Boolean = true,
    val eveningHour: Int = 21,
    val eveningMinute: Int = 0,

    // Follow-up & Snooze settings
    val followUpReminderEnabled: Boolean = false,
    val followUpDelayMinutes: Int = 15,
    val snoozeDurationMinutes: Int = 15,
    val useExactAlarm: Boolean = false,

    // Sunscreen Reapplication (Optional, must be explicitly enabled by user)
    val sunscreenReminderEnabled: Boolean = false,
    val sunscreenStartHour: Int = 9,
    val sunscreenStartMinute: Int = 0,
    val sunscreenEndHour: Int = 16,
    val sunscreenIntervalHours: Int = 2, // 2 or 3 hours
    val sunscreenPaused: Boolean = false,
    val sunscreenActiveDateIso: String = "",

    // Dohixat 100 mg Medication Safety Settings (Disabled until discrepancy is explicitly confirmed)
    val medicationDiscrepancyConfirmed: Boolean = false,
    val medicationReminderEnabled: Boolean = false,
    val medicationStartDateIso: String = "",
    val medicationDurationDays: Int = 5, // Default 5 days (10 capsules / 2 per day) until confirmed by user
    val medicationTotalCapsulesConfirmed: Int = 10,
    val medicationBreakfastHour: Int = 8,
    val medicationBreakfastMinute: Int = 0,
    val medicationDinnerHour: Int = 19,
    val medicationDinnerMinute: Int = 30,

    // Last schedule sync timestamp
    val lastReminderSyncEpochMillis: Long = 0L
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val FIRST_RUN_ONBOARDING_DONE = booleanPreferencesKey("first_run_onboarding_done")
        val NOTIF_PERMISSION_ASKED_ONCE = booleanPreferencesKey("notif_permission_asked_once")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIDE_SENSITIVE_NOTIF = booleanPreferencesKey("hide_sensitive_notif")

        val MORNING_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
        val MORNING_HOUR = intPreferencesKey("morning_hour")
        val MORNING_MINUTE = intPreferencesKey("morning_minute")

        val EVENING_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
        val EVENING_HOUR = intPreferencesKey("evening_hour")
        val EVENING_MINUTE = intPreferencesKey("evening_minute")

        val FOLLOW_UP_ENABLED = booleanPreferencesKey("follow_up_reminder_enabled")
        val FOLLOW_UP_DELAY_MIN = intPreferencesKey("follow_up_delay_minutes")
        val SNOOZE_DURATION_MIN = intPreferencesKey("snooze_duration_minutes")
        val USE_EXACT_ALARM = booleanPreferencesKey("use_exact_alarm")

        val SUNSCREEN_ENABLED = booleanPreferencesKey("sunscreen_reminder_enabled")
        val SUNSCREEN_START_HOUR = intPreferencesKey("sunscreen_start_hour")
        val SUNSCREEN_START_MINUTE = intPreferencesKey("sunscreen_start_minute")
        val SUNSCREEN_END_HOUR = intPreferencesKey("sunscreen_end_hour")
        val SUNSCREEN_INTERVAL_HOURS = intPreferencesKey("sunscreen_interval_hours")
        val SUNSCREEN_PAUSED = booleanPreferencesKey("sunscreen_paused")
        val SUNSCREEN_ACTIVE_DATE_ISO = stringPreferencesKey("sunscreen_active_date_iso")

        val MED_DISCREPANCY_CONFIRMED = booleanPreferencesKey("med_discrepancy_confirmed")
        val MED_REMINDER_ENABLED = booleanPreferencesKey("med_reminder_enabled")
        val MED_START_DATE_ISO = stringPreferencesKey("med_start_date_iso")
        val MED_DURATION_DAYS = intPreferencesKey("med_duration_days")
        val MED_TOTAL_CAPSULES = intPreferencesKey("med_total_capsules")
        val MED_BREAKFAST_HOUR = intPreferencesKey("med_breakfast_hour")
        val MED_BREAKFAST_MINUTE = intPreferencesKey("med_breakfast_minute")
        val MED_DINNER_HOUR = intPreferencesKey("med_dinner_hour")
        val MED_DINNER_MINUTE = intPreferencesKey("med_dinner_minute")

        val LAST_REMINDER_SYNC_MILLIS = longPreferencesKey("last_reminder_sync_millis")
    }

    val settingsFlow: Flow<UserReminderAndAppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> mapPreferences(prefs) }

    suspend fun getSettingsSnapshot(): UserReminderAndAppSettings {
        return settingsFlow.first()
    }

    private fun mapPreferences(prefs: Preferences): UserReminderAndAppSettings {
        val discrepancyConfirmed = prefs[Keys.MED_DISCREPANCY_CONFIRMED] ?: false
        val rawMedEnabled = prefs[Keys.MED_REMINDER_ENABLED] ?: false
        return UserReminderAndAppSettings(
            firstRunOnboardingDone = prefs[Keys.FIRST_RUN_ONBOARDING_DONE] ?: false,
            notificationPermissionAskedOnce = prefs[Keys.NOTIF_PERMISSION_ASKED_ONCE] ?: false,
            themeMode = ThemeMode.fromId(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.id),
            hideSensitiveNotificationContent = prefs[Keys.HIDE_SENSITIVE_NOTIF] ?: false,
            morningReminderEnabled = prefs[Keys.MORNING_ENABLED] ?: true,
            morningHour = prefs[Keys.MORNING_HOUR] ?: 7,
            morningMinute = prefs[Keys.MORNING_MINUTE] ?: 0,
            eveningReminderEnabled = prefs[Keys.EVENING_ENABLED] ?: true,
            eveningHour = prefs[Keys.EVENING_HOUR] ?: 21,
            eveningMinute = prefs[Keys.EVENING_MINUTE] ?: 0,
            followUpReminderEnabled = prefs[Keys.FOLLOW_UP_ENABLED] ?: false,
            followUpDelayMinutes = prefs[Keys.FOLLOW_UP_DELAY_MIN] ?: 15,
            snoozeDurationMinutes = prefs[Keys.SNOOZE_DURATION_MIN] ?: 15,
            useExactAlarm = prefs[Keys.USE_EXACT_ALARM] ?: false,
            sunscreenReminderEnabled = prefs[Keys.SUNSCREEN_ENABLED] ?: false,
            sunscreenStartHour = prefs[Keys.SUNSCREEN_START_HOUR] ?: 9,
            sunscreenStartMinute = prefs[Keys.SUNSCREEN_START_MINUTE] ?: 0,
            sunscreenEndHour = prefs[Keys.SUNSCREEN_END_HOUR] ?: 16,
            sunscreenIntervalHours = (prefs[Keys.SUNSCREEN_INTERVAL_HOURS] ?: 2).coerceIn(2, 3),
            sunscreenPaused = prefs[Keys.SUNSCREEN_PAUSED] ?: false,
            sunscreenActiveDateIso = prefs[Keys.SUNSCREEN_ACTIVE_DATE_ISO] ?: "",
            medicationDiscrepancyConfirmed = discrepancyConfirmed,
            // Safety invariant: medication reminders can NEVER be enabled unless discrepancy is confirmed
            medicationReminderEnabled = discrepancyConfirmed && rawMedEnabled,
            medicationStartDateIso = prefs[Keys.MED_START_DATE_ISO] ?: "",
            medicationDurationDays = prefs[Keys.MED_DURATION_DAYS] ?: 5,
            medicationTotalCapsulesConfirmed = prefs[Keys.MED_TOTAL_CAPSULES] ?: 10,
            medicationBreakfastHour = prefs[Keys.MED_BREAKFAST_HOUR] ?: 8,
            medicationBreakfastMinute = prefs[Keys.MED_BREAKFAST_MINUTE] ?: 0,
            medicationDinnerHour = prefs[Keys.MED_DINNER_HOUR] ?: 19,
            medicationDinnerMinute = prefs[Keys.MED_DINNER_MINUTE] ?: 30,
            lastReminderSyncEpochMillis = prefs[Keys.LAST_REMINDER_SYNC_MILLIS] ?: 0L
        )
    }

    suspend fun completeFirstRunOnboarding(
        morningEnabled: Boolean,
        eveningEnabled: Boolean
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.FIRST_RUN_ONBOARDING_DONE] = true
            prefs[Keys.MORNING_ENABLED] = morningEnabled
            prefs[Keys.EVENING_ENABLED] = eveningEnabled
        }
    }

    suspend fun markNotificationPermissionAsked() {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIF_PERMISSION_ASKED_ONCE] = true
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.id
        }
    }

    suspend fun setHideSensitiveNotificationContent(hide: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.HIDE_SENSITIVE_NOTIF] = hide
        }
    }

    suspend fun updateMorningReminder(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.MORNING_ENABLED] = enabled
            prefs[Keys.MORNING_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.MORNING_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun updateEveningReminder(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.EVENING_ENABLED] = enabled
            prefs[Keys.EVENING_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.EVENING_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun updateFollowUpAndSnooze(
        followUpEnabled: Boolean,
        followUpDelayMinutes: Int,
        snoozeMinutes: Int,
        useExactAlarm: Boolean
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.FOLLOW_UP_ENABLED] = followUpEnabled
            prefs[Keys.FOLLOW_UP_DELAY_MIN] = followUpDelayMinutes.coerceIn(5, 60)
            prefs[Keys.SNOOZE_DURATION_MIN] = snoozeMinutes.coerceIn(5, 60)
            prefs[Keys.USE_EXACT_ALARM] = useExactAlarm
        }
    }

    suspend fun updateSunscreenReminder(
        enabled: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        intervalHours: Int,
        paused: Boolean,
        activeDateIso: String
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.SUNSCREEN_ENABLED] = enabled
            prefs[Keys.SUNSCREEN_START_HOUR] = startHour.coerceIn(5, 18)
            prefs[Keys.SUNSCREEN_START_MINUTE] = startMinute.coerceIn(0, 59)
            prefs[Keys.SUNSCREEN_END_HOUR] = endHour.coerceIn(10, 21)
            prefs[Keys.SUNSCREEN_INTERVAL_HOURS] = if (intervalHours == 3) 3 else 2
            prefs[Keys.SUNSCREEN_PAUSED] = paused
            prefs[Keys.SUNSCREEN_ACTIVE_DATE_ISO] = activeDateIso
        }
    }

    suspend fun confirmAndConfigureMedication(
        confirmed: Boolean,
        reminderEnabled: Boolean,
        startDateIso: String,
        durationDays: Int,
        totalCapsules: Int,
        breakfastHour: Int,
        breakfastMinute: Int,
        dinnerHour: Int,
        dinnerMinute: Int
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.MED_DISCREPANCY_CONFIRMED] = confirmed
            prefs[Keys.MED_REMINDER_ENABLED] = confirmed && reminderEnabled
            prefs[Keys.MED_START_DATE_ISO] = startDateIso
            prefs[Keys.MED_DURATION_DAYS] = durationDays.coerceIn(1, 30)
            prefs[Keys.MED_TOTAL_CAPSULES] = totalCapsules.coerceIn(1, 60)
            prefs[Keys.MED_BREAKFAST_HOUR] = breakfastHour.coerceIn(0, 23)
            prefs[Keys.MED_BREAKFAST_MINUTE] = breakfastMinute.coerceIn(0, 59)
            prefs[Keys.MED_DINNER_HOUR] = dinnerHour.coerceIn(0, 23)
            prefs[Keys.MED_DINNER_MINUTE] = dinnerMinute.coerceIn(0, 59)
        }
    }

    suspend fun setMedicationReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            val confirmed = prefs[Keys.MED_DISCREPANCY_CONFIRMED] ?: false
            prefs[Keys.MED_REMINDER_ENABLED] = confirmed && enabled
        }
    }

    suspend fun recordReminderSyncTimestamp(epochMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_REMINDER_SYNC_MILLIS] = epochMillis
        }
    }

    suspend fun replaceAllSettings(settings: UserReminderAndAppSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.FIRST_RUN_ONBOARDING_DONE] = settings.firstRunOnboardingDone
            prefs[Keys.NOTIF_PERMISSION_ASKED_ONCE] = settings.notificationPermissionAskedOnce
            prefs[Keys.THEME_MODE] = settings.themeMode.id
            prefs[Keys.HIDE_SENSITIVE_NOTIF] = settings.hideSensitiveNotificationContent
            prefs[Keys.MORNING_ENABLED] = settings.morningReminderEnabled
            prefs[Keys.MORNING_HOUR] = settings.morningHour
            prefs[Keys.MORNING_MINUTE] = settings.morningMinute
            prefs[Keys.EVENING_ENABLED] = settings.eveningReminderEnabled
            prefs[Keys.EVENING_HOUR] = settings.eveningHour
            prefs[Keys.EVENING_MINUTE] = settings.eveningMinute
            prefs[Keys.FOLLOW_UP_ENABLED] = settings.followUpReminderEnabled
            prefs[Keys.FOLLOW_UP_DELAY_MIN] = settings.followUpDelayMinutes
            prefs[Keys.SNOOZE_DURATION_MIN] = settings.snoozeDurationMinutes
            prefs[Keys.USE_EXACT_ALARM] = settings.useExactAlarm
            prefs[Keys.SUNSCREEN_ENABLED] = settings.sunscreenReminderEnabled
            prefs[Keys.SUNSCREEN_START_HOUR] = settings.sunscreenStartHour
            prefs[Keys.SUNSCREEN_START_MINUTE] = settings.sunscreenStartMinute
            prefs[Keys.SUNSCREEN_END_HOUR] = settings.sunscreenEndHour
            prefs[Keys.SUNSCREEN_INTERVAL_HOURS] = settings.sunscreenIntervalHours
            prefs[Keys.SUNSCREEN_PAUSED] = settings.sunscreenPaused
            prefs[Keys.SUNSCREEN_ACTIVE_DATE_ISO] = settings.sunscreenActiveDateIso
            prefs[Keys.MED_DISCREPANCY_CONFIRMED] = settings.medicationDiscrepancyConfirmed
            prefs[Keys.MED_REMINDER_ENABLED] =
                settings.medicationDiscrepancyConfirmed && settings.medicationReminderEnabled
            prefs[Keys.MED_START_DATE_ISO] = settings.medicationStartDateIso
            prefs[Keys.MED_DURATION_DAYS] = settings.medicationDurationDays
            prefs[Keys.MED_TOTAL_CAPSULES] = settings.medicationTotalCapsulesConfirmed
            prefs[Keys.MED_BREAKFAST_HOUR] = settings.medicationBreakfastHour
            prefs[Keys.MED_BREAKFAST_MINUTE] = settings.medicationBreakfastMinute
            prefs[Keys.MED_DINNER_HOUR] = settings.medicationDinnerHour
            prefs[Keys.MED_DINNER_MINUTE] = settings.medicationDinnerMinute
        }
    }
}
