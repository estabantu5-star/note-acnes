package com.example.ui

import com.example.data.local.MedicationDoseLogEntity
import com.example.data.local.RoutineTaskEntity
import com.example.data.preferences.UserReminderAndAppSettings
import com.example.domain.ProductCategory
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import com.example.domain.ScheduledActiveProduct
import com.example.domain.SunscreenDailySchedule
import com.example.notification.NextReminderSummary
import java.time.LocalDate
import java.time.LocalTime

enum class MainNavTab(val route: String, val label: String) {
    TODAY("today", "Hari Ini"),
    REMINDERS("reminders", "Pengingat"),
    HISTORY("history", "Riwayat"),
    SETTINGS("settings", "Pengaturan")
}

data class TaskItemUiModel(
    val task: RoutineTaskEntity,
    val resolvedProductName: String,
    val resolvedInstruction: String,
    val resolvedCategory: ProductCategory,
    val resolvedUsageAmount: String,
    val resolvedApplicationArea: String,
    val resolvedDurationNote: String,
    val resolvedConfirmationNote: String,
    val scheduleBadge: String?,
    val isCompleted: Boolean,
    val completedAtMillis: Long?
) {
    val requiresDoctorConfirmation: Boolean
        get() = resolvedUsageAmount.contains(RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION, ignoreCase = true) ||
            resolvedApplicationArea.contains(RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION, ignoreCase = true) ||
            resolvedConfirmationNote.contains(RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION, ignoreCase = true)
}

data class StepTimerState(
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 180,
    val totalSeconds: Int = 180,
    val contextLabel: String = "Jeda Antar Langkah (~3 menit)"
) {
    val isActiveOrPaused: Boolean
        get() = isRunning || remainingSeconds < totalSeconds

    val formattedMmSs: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}

data class HistoryTaskDetailUi(
    val taskId: String,
    val period: String,
    val title: String,
    val productName: String,
    val isOptional: Boolean,
    val isCompleted: Boolean,
    val completedAtMillis: Long?
)

data class HistoryDayRecordUi(
    val dateIso: String,
    val formattedDate: String,
    val shortDate: String,
    val morningCompleted: Int,
    val morningTotal: Int,
    val eveningCompleted: Int,
    val eveningTotal: Int,
    val optionalCompleted: Int,
    val scheduledEveningProduct: String,
    val completionPercentage: Int,
    val isMorningFullyDone: Boolean,
    val isEveningFullyDone: Boolean,
    val completedTasks: List<HistoryTaskDetailUi>,
    val missedTasks: List<HistoryTaskDetailUi>,
    val medicationBreakfastStatus: String?, // "TAKEN", "MISSED", or null
    val medicationDinnerStatus: String?
)

data class WeeklyHistorySummaryUi(
    val recordedDaysCount: Int = 0,
    val averageCompletionPercentage: Int = 0,
    val fullMorningDaysCount: Int = 0,
    val fullEveningDaysCount: Int = 0,
    val totalTasksCompletedCount: Int = 0
)

data class SkinCareUiState(
    val isLoading: Boolean = true,
    val selectedTab: MainNavTab = MainNavTab.TODAY,
    val highlightedPeriod: RoutinePeriod? = null,
    val todayDate: LocalDate = LocalDate.now(),
    val todayDateIso: String = "",
    val todayFormattedIndonesian: String = "",
    val scheduledEveningProductToday: ScheduledActiveProduct,

    // Today's checklist (strictly separated into Morning, Evening, and Oral Medication)
    val morningTasks: List<TaskItemUiModel> = emptyList(),
    val eveningTasks: List<TaskItemUiModel> = emptyList(),
    val morningCompletedRequired: Int = 0,
    val morningTotalRequired: Int = 0,
    val eveningCompletedRequired: Int = 0,
    val eveningTotalRequired: Int = 0,
    val optionalCompletedToday: Int = 0,
    val totalCompletedRequiredToday: Int = 0,
    val totalRequiredToday: Int = 0,
    val todayCompletionPercentage: Int = 0,

    // Rule 6 & Rule 9: Active conflict & safety warnings
    val routineConflictWarnings: List<String> = emptyList(),

    // Sunscreen schedule (20 min before outdoor + reapply every 2–3h)
    val sunscreenScheduleToday: SunscreenDailySchedule = SunscreenDailySchedule(
        outdoorDepartureTime = LocalTime.of(9, 0),
        applyBeforeOutdoorTime = LocalTime.of(8, 40),
        reapplyTimes = listOf(LocalTime.of(11, 0), LocalTime.of(13, 0), LocalTime.of(15, 0)),
        intervalHours = 2
    ),

    // Non-blocking 3-minute step interval timer
    val stepTimer: StepTimerState = StepTimerState(),

    // Settings, Reminders, and Permissions
    val settings: UserReminderAndAppSettings = UserReminderAndAppSettings(),
    val hasNotificationPermission: Boolean = false,
    val canScheduleExactAlarms: Boolean = false,
    val nextReminder: NextReminderSummary? = null,
    val allTasksForEditor: List<RoutineTaskEntity> = emptyList(),

    // Oral Medication Today (Doxihat 100 mg — 2x/day after breakfast & dinner for 2 weeks)
    val isMedicationCourseActiveToday: Boolean = false,
    val medicationDayNumber: Int? = null,
    val todayBreakfastMedLog: MedicationDoseLogEntity? = null,
    val todayDinnerMedLog: MedicationDoseLogEntity? = null,

    // Rule 7: Two-Week Doctor Control Follow-up
    val doctorControlDateFormatted: String = "",
    val doctorControlDaysRemaining: Int? = null,

    // History
    val historyRecords: List<HistoryDayRecordUi> = emptyList(),
    val weeklySummary: WeeklyHistorySummaryUi = WeeklyHistorySummaryUi(),

    // Transient feedback message
    val feedbackMessage: String? = null
)
