package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DailySnapshotEntity
import com.example.data.local.MedicationDoseLogEntity
import com.example.data.local.RoutineTaskEntity
import com.example.data.local.TaskCompletionEntity
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserReminderAndAppSettings
import com.example.data.repository.SkinCareRepository
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import com.example.notification.NotificationHelper
import com.example.notification.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SkinCareViewModel(
    private val appContext: Context,
    private val repository: SkinCareRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainNavTab.TODAY)
    private val _highlightedPeriod = MutableStateFlow<RoutinePeriod?>(null)
    private val _currentDate = MutableStateFlow(RoutineScheduleHelper.currentLocalDate())
    private var isSimulatedDateOverride = false

    private val _stepTimer = MutableStateFlow(StepTimerState())
    private var timerJob: Job? = null

    private val _permissionRefreshTrigger = MutableStateFlow(0L)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private data class CoreDataBundle(
        val allTasks: List<RoutineTaskEntity>,
        val allCompletions: List<TaskCompletionEntity>,
        val allSnapshots: List<DailySnapshotEntity>,
        val allMedLogs: List<MedicationDoseLogEntity>,
        val settings: UserReminderAndAppSettings
    )

    private data class UiTriggerBundle(
        val selectedTab: MainNavTab,
        val highlightedPeriod: RoutinePeriod?,
        val currentDate: LocalDate,
        val stepTimer: StepTimerState,
        val feedbackMessage: String?,
        val permTick: Long
    )

    private val coreDataFlow = combine(
        repository.allTasksFlow,
        repository.allCompletionsFlow,
        repository.allDailySnapshotsFlow,
        repository.allMedicationLogsFlow,
        repository.settingsFlow
    ) { tasks, completions, snapshots, medLogs, settings ->
        CoreDataBundle(tasks, completions, snapshots, medLogs, settings)
    }

    private val uiTriggerFlow = combine(
        _selectedTab,
        _highlightedPeriod,
        _currentDate,
        _stepTimer,
        combine(_feedbackMessage, _permissionRefreshTrigger) { msg, tick -> msg to tick }
    ) { tab, period, date, timer, (msg, tick) ->
        UiTriggerBundle(tab, period, date, timer, msg, tick)
    }

    val uiState: StateFlow<SkinCareUiState> = combine(
        coreDataFlow,
        uiTriggerFlow
    ) { core, trigger ->
        buildUiState(core, trigger)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SkinCareUiState(
            todayDate = _currentDate.value,
            todayDateIso = RoutineScheduleHelper.toIsoString(_currentDate.value),
            todayFormattedIndonesian = RoutineScheduleHelper.formatFullIndonesianDate(_currentDate.value),
            scheduledEveningProductToday = RoutineScheduleHelper.getScheduledEveningProductForDate(_currentDate.value)
        )
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            syncAlarmsWithCurrentState()
        }
        // Observe midnight / timezone boundary transitions automatically
        viewModelScope.launch {
            while (true) {
                delay(15_000L)
                if (!isSimulatedDateOverride) {
                    val actualNow = RoutineScheduleHelper.currentLocalDate()
                    if (actualNow != _currentDate.value) {
                        _currentDate.value = actualNow
                        syncAlarmsWithCurrentState()
                    }
                }
            }
        }
    }

    private fun buildUiState(
        core: CoreDataBundle,
        trigger: UiTriggerBundle
    ): SkinCareUiState {
        val today = trigger.currentDate
        val todayIso = RoutineScheduleHelper.toIsoString(today)
        val scheduledEvening = RoutineScheduleHelper.getScheduledEveningProductForDate(today)

        val activeTodayTasks = core.allTasks.filter {
            RoutineScheduleHelper.isTaskScheduledForDate(it, today)
        }.sortedBy { it.sortOrder }

        val todayCompletionsMap = core.allCompletions
            .filter { it.dateIso == todayIso }
            .associateBy { it.taskId }

        val morningModels = activeTodayTasks
            .filter { it.period == RoutinePeriod.MORNING.id }
            .map { task ->
                val resolved = RoutineScheduleHelper.resolveTaskForDate(task, today)
                val comp = todayCompletionsMap[task.id]
                TaskItemUiModel(
                    task = task,
                    resolvedProductName = resolved.productName,
                    resolvedInstruction = resolved.instruction,
                    scheduleBadge = "Setiap Pagi",
                    isCompleted = comp?.completed == true,
                    completedAtMillis = comp?.completedAtEpochMillis
                )
            }

        val eveningModels = activeTodayTasks
            .filter { it.period == RoutinePeriod.EVENING.id }
            .map { task ->
                val resolved = RoutineScheduleHelper.resolveTaskForDate(task, today)
                val comp = todayCompletionsMap[task.id]
                val badge = when {
                    task.isScheduledActive -> scheduledEvening.shortBadge
                    task.isOptional -> "Opsional (Saat ada jerawat aktif)"
                    else -> "Setiap Malam"
                }
                TaskItemUiModel(
                    task = task,
                    resolvedProductName = resolved.productName,
                    resolvedInstruction = resolved.instruction,
                    scheduleBadge = badge,
                    isCompleted = comp?.completed == true,
                    completedAtMillis = comp?.completedAtEpochMillis
                )
            }

        val morningReqTotal = morningModels.count { !it.task.isOptional }
        val morningReqDone = morningModels.count { !it.task.isOptional && it.isCompleted }
        val eveningReqTotal = eveningModels.count { !it.task.isOptional }
        val eveningReqDone = eveningModels.count { !it.task.isOptional && it.isCompleted }
        val optionalDone = (morningModels + eveningModels).count { it.task.isOptional && it.isCompleted }

        val totalReq = morningReqTotal + eveningReqTotal
        val totalDone = morningReqDone + eveningReqDone
        val percentage = if (totalReq > 0) ((totalDone * 100f) / totalReq).toInt().coerceIn(0, 100) else 0

        val isMorningFullyDone = morningReqTotal > 0 && morningReqDone == morningReqTotal
        val isEveningFullyDone = eveningReqTotal > 0 && eveningReqDone == eveningReqTotal

        val hasNotifPerm = NotificationHelper.hasNotificationPermission(appContext)
        val canExact = reminderScheduler.canScheduleExactAlarms()
        val nextReminder = reminderScheduler.getNextUpcomingReminderSummary(
            settings = core.settings,
            isMorningCompletedToday = isMorningFullyDone,
            isEveningCompletedToday = isEveningFullyDone
        )

        // Medication course calculation
        val isMedActiveToday = RoutineScheduleHelper.isMedicationCourseActiveOnDate(
            isConfirmed = core.settings.medicationDiscrepancyConfirmed,
            courseEnabled = core.settings.medicationReminderEnabled,
            startDateIso = core.settings.medicationStartDateIso,
            durationDays = core.settings.medicationDurationDays,
            targetDate = today
        )
        val medDayNum = if (core.settings.medicationDiscrepancyConfirmed && core.settings.medicationStartDateIso.isNotBlank()) {
            runCatching {
                val start = RoutineScheduleHelper.parseIsoDate(core.settings.medicationStartDateIso)
                val diff = ChronoUnit.DAYS.between(start, today).toInt() + 1
                if (diff in 1..core.settings.medicationDurationDays) diff else null
            }.getOrNull()
        } else null

        val todayMedLogs = core.allMedLogs.filter { it.dateIso == todayIso }
        val breakfastLog = todayMedLogs.firstOrNull { it.slot == "BREAKFAST" }
        val dinnerLog = todayMedLogs.firstOrNull { it.slot == "DINNER" }

        // Build History Records from real persisted snapshots & completions
        val historyList = buildHistoryRecords(
            allTasks = core.allTasks,
            allCompletions = core.allCompletions,
            allSnapshots = core.allSnapshots,
            allMedLogs = core.allMedLogs
        )
        val weeklySummary = buildWeeklySummary(historyList, today)

        return SkinCareUiState(
            isLoading = false,
            selectedTab = trigger.selectedTab,
            highlightedPeriod = trigger.highlightedPeriod,
            todayDate = today,
            todayDateIso = todayIso,
            todayFormattedIndonesian = RoutineScheduleHelper.formatFullIndonesianDate(today),
            scheduledEveningProductToday = scheduledEvening,
            morningTasks = morningModels,
            eveningTasks = eveningModels,
            morningCompletedRequired = morningReqDone,
            morningTotalRequired = morningReqTotal,
            eveningCompletedRequired = eveningReqDone,
            eveningTotalRequired = eveningReqTotal,
            optionalCompletedToday = optionalDone,
            totalCompletedRequiredToday = totalDone,
            totalRequiredToday = totalReq,
            todayCompletionPercentage = percentage,
            stepTimer = trigger.stepTimer,
            settings = core.settings,
            hasNotificationPermission = hasNotifPerm,
            canScheduleExactAlarms = canExact,
            nextReminder = nextReminder,
            allTasksForEditor = core.allTasks.sortedWith(compareBy({ it.period }, { it.sortOrder })),
            isMedicationCourseActiveToday = isMedActiveToday,
            medicationDayNumber = medDayNum,
            todayBreakfastMedLog = breakfastLog,
            todayDinnerMedLog = dinnerLog,
            historyRecords = historyList,
            weeklySummary = weeklySummary,
            feedbackMessage = trigger.feedbackMessage
        )
    }

    private fun buildHistoryRecords(
        allTasks: List<RoutineTaskEntity>,
        allCompletions: List<TaskCompletionEntity>,
        allSnapshots: List<DailySnapshotEntity>,
        allMedLogs: List<MedicationDoseLogEntity>
    ): List<HistoryDayRecordUi> {
        val allDates = (
            allSnapshots.map { it.dateIso } +
                allCompletions.map { it.dateIso } +
                allMedLogs.map { it.dateIso }
            ).distinct().sortedDescending()

        val snapshotsByDate = allSnapshots.associateBy { it.dateIso }
        val completionsByDate = allCompletions.groupBy { it.dateIso }
        val medLogsByDate = allMedLogs.groupBy { it.dateIso }

        return allDates.mapNotNull { dateIso ->
            val date = runCatching { RoutineScheduleHelper.parseIsoDate(dateIso) }.getOrNull()
                ?: return@mapNotNull null
            val dayCompletions = completionsByDate[dateIso].orEmpty()
            val snapshot = snapshotsByDate[dateIso]
            val medLogs = medLogsByDate[dateIso].orEmpty()

            val completionsByTaskId = dayCompletions.associateBy { it.taskId }
            val scheduledProduct = snapshot?.scheduledEveningProduct?.takeIf { it.isNotBlank() }
                ?: RoutineScheduleHelper.getScheduledEveningProductForDate(date).productName

            // Merge snapshot task records with active tasks so retired/edited tasks still display accurately
            val activeTasksForDate = allTasks.filter {
                RoutineScheduleHelper.isTaskScheduledForDate(it, date)
            }.sortedWith(compareBy({ it.period }, { it.sortOrder }))

            val detailItems = mutableListOf<HistoryTaskDetailUi>()
            val seenTaskIds = mutableSetOf<String>()

            activeTasksForDate.forEach { task ->
                seenTaskIds.add(task.id)
                val comp = completionsByTaskId[task.id]
                val resolved = RoutineScheduleHelper.resolveTaskForDate(task, date)
                detailItems.add(
                    HistoryTaskDetailUi(
                        taskId = task.id,
                        period = task.period,
                        title = comp?.taskTitleSnapshot?.takeIf { it.isNotBlank() } ?: resolved.title,
                        productName = comp?.productSnapshot?.takeIf { it.isNotBlank() } ?: resolved.productName,
                        isOptional = comp?.isOptionalSnapshot ?: resolved.isOptional,
                        isCompleted = comp?.completed == true,
                        completedAtMillis = comp?.completedAtEpochMillis
                    )
                )
            }

            // Include any completion records from tasks that were later retired/deleted
            dayCompletions.filter { it.taskId !in seenTaskIds }.forEach { comp ->
                detailItems.add(
                    HistoryTaskDetailUi(
                        taskId = comp.taskId,
                        period = comp.period,
                        title = comp.taskTitleSnapshot.ifBlank { "Langkah Rutinitas" },
                        productName = comp.productSnapshot,
                        isOptional = comp.isOptionalSnapshot,
                        isCompleted = comp.completed,
                        completedAtMillis = comp.completedAtEpochMillis
                    )
                )
            }

            val morningTotal = snapshot?.morningTotalRequired
                ?: detailItems.count { it.period == RoutinePeriod.MORNING.id && !it.isOptional }
            val morningDone = snapshot?.morningCompletedCount
                ?: detailItems.count { it.period == RoutinePeriod.MORNING.id && !it.isOptional && it.isCompleted }
            val eveningTotal = snapshot?.eveningTotalRequired
                ?: detailItems.count { it.period == RoutinePeriod.EVENING.id && !it.isOptional }
            val eveningDone = snapshot?.eveningCompletedCount
                ?: detailItems.count { it.period == RoutinePeriod.EVENING.id && !it.isOptional && it.isCompleted }
            val optionalDone = snapshot?.optionalCompletedCount
                ?: detailItems.count { it.isOptional && it.isCompleted }

            val totalReq = (morningTotal + eveningTotal).coerceAtLeast(1)
            val totalDone = morningDone + eveningDone
            val pct = ((totalDone * 100f) / totalReq).toInt().coerceIn(0, 100)

            val completedList = detailItems.filter { it.isCompleted }
            val missedList = detailItems.filter { !it.isCompleted && !it.isOptional }

            HistoryDayRecordUi(
                dateIso = dateIso,
                formattedDate = RoutineScheduleHelper.formatFullIndonesianDate(date),
                shortDate = RoutineScheduleHelper.formatShortIndonesianDate(date),
                morningCompleted = morningDone,
                morningTotal = morningTotal,
                eveningCompleted = eveningDone,
                eveningTotal = eveningTotal,
                optionalCompleted = optionalDone,
                scheduledEveningProduct = scheduledProduct,
                completionPercentage = pct,
                isMorningFullyDone = morningTotal > 0 && morningDone >= morningTotal,
                isEveningFullyDone = eveningTotal > 0 && eveningDone >= eveningTotal,
                completedTasks = completedList,
                missedTasks = missedList,
                medicationBreakfastStatus = medLogs.firstOrNull { it.slot == "BREAKFAST" }?.status,
                medicationDinnerStatus = medLogs.firstOrNull { it.slot == "DINNER" }?.status
            )
        }
    }

    private fun buildWeeklySummary(
        records: List<HistoryDayRecordUi>,
        today: LocalDate
    ): WeeklyHistorySummaryUi {
        val sevenDaysAgo = today.minusDays(6)
        val recentRecords = records.filter { rec ->
            val date = runCatching { RoutineScheduleHelper.parseIsoDate(rec.dateIso) }.getOrNull()
            date != null && !date.isBefore(sevenDaysAgo) && !date.isAfter(today)
        }
        if (recentRecords.isEmpty()) return WeeklyHistorySummaryUi()

        val avgPct = recentRecords.map { it.completionPercentage }.average().toInt()
        val morningFull = recentRecords.count { it.isMorningFullyDone }
        val eveningFull = recentRecords.count { it.isEveningFullyDone }
        val totalTasksDone = recentRecords.sumOf { it.completedTasks.size }

        return WeeklyHistorySummaryUi(
            recordedDaysCount = recentRecords.size,
            averageCompletionPercentage = avgPct,
            fullMorningDaysCount = morningFull,
            fullEveningDaysCount = eveningFull,
            totalTasksCompletedCount = totalTasksDone
        )
    }

    // --- Navigation & Intent Handling ---
    fun selectTab(tab: MainNavTab) {
        _selectedTab.value = tab
    }

    fun handleIncomingNotificationIntent(targetPeriod: String?) {
        if (!targetPeriod.isNullOrBlank()) {
            _selectedTab.value = MainNavTab.TODAY
            _highlightedPeriod.value = RoutinePeriod.fromId(targetPeriod)
        }
    }

    fun clearHighlightedPeriod() {
        _highlightedPeriod.value = null
    }

    fun refreshPermissionsAndDateOnResume() {
        _permissionRefreshTrigger.value = System.currentTimeMillis()
        if (!isSimulatedDateOverride) {
            val actualNow = RoutineScheduleHelper.currentLocalDate()
            if (actualNow != _currentDate.value) {
                _currentDate.value = actualNow
                viewModelScope.launch {
                    syncAlarmsWithCurrentState()
                }
            }
        }
    }

    /**
     * Used by acceptance tests to deterministically simulate a new calendar day transition.
     */
    fun simulateDateForTesting(newDate: LocalDate) {
        isSimulatedDateOverride = true
        _currentDate.value = newDate
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    private fun postFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    // --- Checklist Actions ---
    fun toggleTaskCompletion(taskModel: TaskItemUiModel, newCompleted: Boolean) {
        val targetDate = _currentDate.value
        viewModelScope.launch {
            repository.setTaskCompleted(
                date = targetDate,
                task = taskModel.task,
                completed = newCompleted
            )
            syncAlarmsWithCurrentState()
            if (newCompleted) {
                postFeedback("Tersimpan: ${taskModel.task.title} selesai.")
            }
        }
    }

    fun resetTodayChecklist() {
        val targetDate = _currentDate.value
        viewModelScope.launch {
            repository.resetChecklistForDate(targetDate)
            syncAlarmsWithCurrentState()
            postFeedback("Checklist hari ini telah direset. Riwayat hari sebelumnya tetap aman.")
        }
    }

    // --- Non-blocking 3-Minute Step Timer ---
    fun startStepTimer(stepTitle: String = "Jeda Antar Langkah (~3 menit)") {
        timerJob?.cancel()
        _stepTimer.value = StepTimerState(
            isRunning = true,
            remainingSeconds = 180,
            totalSeconds = 180,
            contextLabel = stepTitle
        )
        runTimerLoop()
    }

    fun togglePauseResumeStepTimer() {
        val current = _stepTimer.value
        if (current.isRunning) {
            timerJob?.cancel()
            _stepTimer.update { it.copy(isRunning = false) }
        } else if (current.remainingSeconds > 0) {
            _stepTimer.update { it.copy(isRunning = true) }
            runTimerLoop()
        }
    }

    fun resetAndDismissStepTimer() {
        timerJob?.cancel()
        _stepTimer.value = StepTimerState(isRunning = false, remainingSeconds = 180, totalSeconds = 180)
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_stepTimer.value.isRunning && _stepTimer.value.remainingSeconds > 0) {
                delay(1000L)
                _stepTimer.update { state ->
                    val next = (state.remainingSeconds - 1).coerceAtLeast(0)
                    state.copy(
                        remainingSeconds = next,
                        isRunning = next > 0
                    )
                }
            }
            if (_stepTimer.value.remainingSeconds == 0) {
                postFeedback("Jeda 3 menit selesai! Silakan lanjutkan ke langkah berikutnya.")
            }
        }
    }

    // --- Onboarding & Notification Settings ---
    fun completeOnboarding(morningEnabled: Boolean, eveningEnabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.completeFirstRunOnboarding(morningEnabled, eveningEnabled)
            syncAlarmsWithCurrentState()
            postFeedback("Pengaturan pengingat awal berhasil disimpan.")
        }
    }

    fun markPermissionPrompted() {
        viewModelScope.launch {
            preferencesRepository.markNotificationPermissionAsked()
            _permissionRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun updateMorningReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.updateMorningReminder(enabled, hour, minute)
            syncAlarmsWithCurrentState()
            val statusText = if (enabled) {
                "Pengingat Pagi dijadwalkan pukul ${RoutineScheduleHelper.formatTimeHm(hour, minute)}."
            } else {
                "Pengingat Pagi dinonaktifkan."
            }
            postFeedback(statusText)
        }
    }

    fun updateEveningReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.updateEveningReminder(enabled, hour, minute)
            syncAlarmsWithCurrentState()
            val statusText = if (enabled) {
                "Pengingat Malam dijadwalkan pukul ${RoutineScheduleHelper.formatTimeHm(hour, minute)}."
            } else {
                "Pengingat Malam dinonaktifkan."
            }
            postFeedback(statusText)
        }
    }

    fun updateFollowUpAndSnooze(
        followUpEnabled: Boolean,
        followUpDelayMinutes: Int,
        snoozeMinutes: Int,
        useExactAlarm: Boolean
    ) {
        viewModelScope.launch {
            preferencesRepository.updateFollowUpAndSnooze(
                followUpEnabled = followUpEnabled,
                followUpDelayMinutes = followUpDelayMinutes,
                snoozeMinutes = snoozeMinutes,
                useExactAlarm = useExactAlarm
            )
            syncAlarmsWithCurrentState()
            postFeedback("Pengaturan tunda dan jadwal pengingat diperbarui.")
        }
    }

    fun toggleStepReminder(taskId: String, enabled: Boolean, offsetMinutes: Int) {
        viewModelScope.launch {
            repository.toggleTaskStepReminder(taskId, enabled, offsetMinutes)
            syncAlarmsWithCurrentState()
            postFeedback(if (enabled) "Pengingat langkah diaktifkan (+${offsetMinutes} menit)." else "Pengingat langkah dinonaktifkan.")
        }
    }

    // --- Sunscreen Reapplication ---
    fun configureSunscreenReminder(
        enabled: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        intervalHours: Int,
        paused: Boolean
    ) {
        val todayIso = RoutineScheduleHelper.toIsoString(_currentDate.value)
        viewModelScope.launch {
            preferencesRepository.updateSunscreenReminder(
                enabled = enabled,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                intervalHours = intervalHours,
                paused = paused,
                activeDateIso = if (enabled) todayIso else ""
            )
            syncAlarmsWithCurrentState()
            val msg = when {
                !enabled -> "Pengingat sunscreen dihentikan."
                paused -> "Pengingat sunscreen dijeda sementara."
                else -> "Pengingat sunscreen aktif setiap $intervalHours jam untuk hari ini."
            }
            postFeedback(msg)
        }
    }

    // --- Medication Safety & Dose Tracking (Dohixat 100 mg) ---
    fun confirmAndSaveMedicationSchedule(
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
        viewModelScope.launch {
            val effectiveStart = startDateIso.ifBlank {
                RoutineScheduleHelper.toIsoString(_currentDate.value)
            }
            preferencesRepository.confirmAndConfigureMedication(
                confirmed = confirmed,
                reminderEnabled = reminderEnabled,
                startDateIso = effectiveStart,
                durationDays = durationDays,
                totalCapsules = totalCapsules,
                breakfastHour = breakfastHour,
                breakfastMinute = breakfastMinute,
                dinnerHour = dinnerHour,
                dinnerMinute = dinnerMinute
            )
            syncAlarmsWithCurrentState()
            postFeedback(
                if (confirmed && reminderEnabled) {
                    "Jadwal pengingat Dohixat 100 mg dikonfirmasi ($durationDays hari, $totalCapsules kapsul)."
                } else if (confirmed) {
                    "Konfirmasi resep disimpan (pengingat obat tidak diaktifkan)."
                } else {
                    "Pengingat obat dinonaktifkan."
                }
            )
        }
    }

    fun setMedicationReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMedicationReminderEnabled(enabled)
            syncAlarmsWithCurrentState()
            postFeedback(if (enabled) "Pengingat obat diaktifkan." else "Pengingat obat dinonaktifkan.")
        }
    }

    fun recordMedicationDose(slot: String, status: String) {
        val targetDate = _currentDate.value
        viewModelScope.launch {
            repository.logMedicationDose(targetDate, slot, status)
            val slotLabel = if (slot == "BREAKFAST") "Pagi (Sehabis Sarapan)" else "Malam (Sehabis Makan Malam)"
            val statusLabel = if (status == "TAKEN") "Sudah diminum" else "Belum/terlewat"
            postFeedback("Dohixat 100 mg $slotLabel dicatat: $statusLabel.")
        }
    }

    // --- Test & Reschedule Actions ---
    fun triggerTestNotification() {
        viewModelScope.launch {
            val settings = preferencesRepository.getSettingsSnapshot()
            val today = _currentDate.value
            val tonight = RoutineScheduleHelper.getScheduledEveningProductForDate(today)
            val sent = NotificationHelper.showReminderNotification(
                context = appContext,
                notificationId = NotificationHelper.NOTIF_ID_TEST,
                reminderType = NotificationHelper.TYPE_TEST_NOTIFICATION,
                targetPeriod = RoutinePeriod.MORNING.id,
                title = "Uji Notifikasi • SkinCare Daily",
                body = "Notifikasi berfungsi! Malam ini (${tonight.dayNameIndonesian}) jadwal aktif Anda: ${tonight.productName}.",
                hideSensitiveContent = settings.hideSensitiveNotificationContent,
                snoozeMinutes = settings.snoozeDurationMinutes
            )
            if (sent) {
                postFeedback("Notifikasi uji berhasil dikirim ke panel notifikasi Android.")
            } else {
                postFeedback("Gagal menampilkan notifikasi: izin notifikasi belum diberikan.")
            }
        }
    }

    fun rescheduleAllRemindersManually() {
        viewModelScope.launch {
            syncAlarmsWithCurrentState()
            preferencesRepository.recordReminderSyncTimestamp()
            postFeedback("Seluruh pengingat berhasil dijadwalkan ulang.")
        }
    }

    private suspend fun syncAlarmsWithCurrentState() {
        val today = _currentDate.value
        val settings = preferencesRepository.getSettingsSnapshot()
        val isMorningDone = repository.isPeriodRoutineCompletedForDate(today, RoutinePeriod.MORNING)
        val isEveningDone = repository.isPeriodRoutineCompletedForDate(today, RoutinePeriod.EVENING)
        val activeTasks = repository.activeTasksFlow.first()
        reminderScheduler.syncAllReminders(
            settings = settings,
            activeTasks = activeTasks,
            isMorningCompletedToday = isMorningDone,
            isEveningCompletedToday = isEveningDone
        )
    }

    // --- Theme, Privacy, and Routine Customization ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
            postFeedback("Tema aplikasi diubah ke: ${mode.labelId}.")
        }
    }

    fun setHideSensitiveNotifications(hide: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHideSensitiveNotificationContent(hide)
            postFeedback(
                if (hide) "Nama produk & obat disembunyikan di layar kunci."
                else "Detail produk ditampilkan pada notifikasi."
            )
        }
    }

    fun addCustomTask(
        period: RoutinePeriod,
        title: String,
        productName: String,
        instruction: String,
        estimatedMinutes: Int,
        isOptional: Boolean,
        reminderEnabled: Boolean,
        reminderOffsetMinutes: Int
    ) {
        viewModelScope.launch {
            repository.addCustomTask(
                period = period,
                title = title,
                productName = productName,
                instruction = instruction,
                estimatedMinutes = estimatedMinutes,
                isOptional = isOptional,
                reminderEnabled = reminderEnabled,
                reminderOffsetMinutes = reminderOffsetMinutes
            )
            repository.recalculateAndSaveSnapshotForDate(_currentDate.value)
            syncAlarmsWithCurrentState()
            postFeedback("Langkah baru \"$title\" ditambahkan ke rutinitas ${period.labelId}.")
        }
    }

    fun updateTaskDetails(
        taskId: String,
        title: String,
        productName: String,
        instruction: String,
        estimatedMinutes: Int,
        isOptional: Boolean,
        isActive: Boolean,
        reminderEnabled: Boolean,
        reminderOffsetMinutes: Int
    ) {
        viewModelScope.launch {
            repository.updateTaskDetails(
                taskId = taskId,
                title = title,
                productName = productName,
                instruction = instruction,
                estimatedMinutes = estimatedMinutes,
                isOptional = isOptional,
                isActive = isActive,
                reminderEnabled = reminderEnabled,
                reminderOffsetMinutes = reminderOffsetMinutes
            )
            repository.recalculateAndSaveSnapshotForDate(_currentDate.value)
            syncAlarmsWithCurrentState()
            postFeedback("Perubahan langkah \"$title\" berhasil disimpan.")
        }
    }

    fun moveTaskOrder(taskId: String, moveUp: Boolean) {
        viewModelScope.launch {
            repository.moveTaskOrder(taskId, moveUp)
        }
    }

    fun retireTask(taskId: String, title: String) {
        viewModelScope.launch {
            repository.retireTask(taskId)
            repository.recalculateAndSaveSnapshotForDate(_currentDate.value)
            syncAlarmsWithCurrentState()
            postFeedback("Langkah \"$title\" dinonaktifkan. Riwayat masa lalu tetap tersimpan.")
        }
    }

    fun restoreTaskActive(taskId: String, title: String) {
        viewModelScope.launch {
            repository.setTaskActive(taskId, true)
            repository.recalculateAndSaveSnapshotForDate(_currentDate.value)
            syncAlarmsWithCurrentState()
            postFeedback("Langkah \"$title\" diaktifkan kembali.")
        }
    }

    fun restoreDermatologistDefaultRoutine() {
        viewModelScope.launch {
            repository.restoreDefaultDermatologistRoutine()
            repository.recalculateAndSaveSnapshotForDate(_currentDate.value)
            syncAlarmsWithCurrentState()
            postFeedback("Rutinitas awal dokter kulit (19 Okt 2026) berhasil dipulihkan.")
        }
    }

    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportAllDataToJson()
            onResult(json)
        }
    }

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            val result = repository.importDataFromJson(jsonString)
            result.onSuccess { count ->
                syncAlarmsWithCurrentState()
                postFeedback("Berhasil mengimpor $count data catatan & pengaturan.")
            }.onFailure {
                postFeedback("Format JSON tidak valid. Pastikan salinan cadangan lengkap.")
            }
        }
    }

    companion object {
        fun provideFactory(
            context: Context,
            repository: SkinCareRepository,
            preferencesRepository: UserPreferencesRepository,
            reminderScheduler: ReminderScheduler
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SkinCareViewModel(
                    appContext = context.applicationContext,
                    repository = repository,
                    preferencesRepository = preferencesRepository,
                    reminderScheduler = reminderScheduler
                ) as T
            }
        }
    }
}
