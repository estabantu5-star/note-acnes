package com.example.data.repository

import com.example.data.local.DailySnapshotEntity
import com.example.data.local.MedicationDoseLogEntity
import com.example.data.local.RoutineTaskEntity
import com.example.data.local.SkinCareDao
import com.example.data.local.TaskCompletionEntity
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserReminderAndAppSettings
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class SkinCareRepository(
    private val dao: SkinCareDao,
    private val preferencesRepository: UserPreferencesRepository
) {

    val activeTasksFlow: Flow<List<RoutineTaskEntity>> = dao.observeActiveTasks()
    val allTasksFlow: Flow<List<RoutineTaskEntity>> = dao.observeAllTasks()
    val allCompletionsFlow: Flow<List<TaskCompletionEntity>> = dao.observeAllCompletions()
    val allDailySnapshotsFlow: Flow<List<DailySnapshotEntity>> = dao.observeAllDailySnapshots()
    val allMedicationLogsFlow: Flow<List<MedicationDoseLogEntity>> = dao.observeAllMedicationLogs()
    val settingsFlow: Flow<UserReminderAndAppSettings> = preferencesRepository.settingsFlow

    fun observeCompletionsForDate(dateIso: String): Flow<List<TaskCompletionEntity>> =
        dao.observeCompletionsForDate(dateIso)

    fun observeMedicationLogsForDate(dateIso: String): Flow<List<MedicationDoseLogEntity>> =
        dao.observeMedicationLogsForDate(dateIso)

    /**
     * Ensures the dermatologist's initial routine is seeded into Room on first run.
     */
    suspend fun ensureSeeded() {
        val count = dao.getTaskCount()
        if (count == 0) {
            dao.upsertTasks(RoutineScheduleHelper.createInitialDermatologistTasks())
        }
    }

    /**
     * Toggles a task's completion status for a specific date and immediately updates the
     * persistent daily snapshot so historical records remain accurate even if tasks change later.
     */
    suspend fun setTaskCompleted(
        date: LocalDate,
        task: RoutineTaskEntity,
        completed: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val dateIso = RoutineScheduleHelper.toIsoString(date)
        val resolvedTask = RoutineScheduleHelper.resolveTaskForDate(task, date)

        val completion = TaskCompletionEntity(
            dateIso = dateIso,
            taskId = task.id,
            period = task.period,
            taskTitleSnapshot = resolvedTask.title,
            productSnapshot = resolvedTask.productName,
            isOptionalSnapshot = resolvedTask.isOptional,
            completed = completed,
            completedAtEpochMillis = if (completed) nowMillis else null
        )

        dao.upsertCompletion(completion)
        recalculateAndSaveSnapshotForDate(date, nowMillis)
    }

    /**
     * Resets only the specified date's checklist completions without touching any other day's history.
     */
    suspend fun resetChecklistForDate(date: LocalDate) {
        val dateIso = RoutineScheduleHelper.toIsoString(date)
        dao.clearCompletionsForDate(dateIso)
        dao.deleteDailySnapshotForDate(dateIso)
    }

    suspend fun recalculateAndSaveSnapshotForDate(
        date: LocalDate,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val dateIso = RoutineScheduleHelper.toIsoString(date)
        val activeTasks = dao.getActiveTasksSync().filter {
            RoutineScheduleHelper.isTaskScheduledForDate(it, date)
        }
        val completions = dao.getCompletionsForDateSync(dateIso)
        val completedIds = completions.filter { it.completed }.map { it.taskId }.toSet()

        val morningRequired = activeTasks.filter {
            it.period == RoutinePeriod.MORNING.id && !it.isOptional
        }
        val eveningRequired = activeTasks.filter {
            it.period == RoutinePeriod.EVENING.id && !it.isOptional
        }
        val optionalCompleted = activeTasks.count {
            it.isOptional && completedIds.contains(it.id)
        }

        val morningDone = morningRequired.count { completedIds.contains(it.id) }
        val eveningDone = eveningRequired.count { completedIds.contains(it.id) }
        val scheduledEvening = RoutineScheduleHelper.getScheduledEveningProductForDate(date)

        val snapshot = DailySnapshotEntity(
            dateIso = dateIso,
            morningTotalRequired = morningRequired.size,
            morningCompletedCount = morningDone,
            eveningTotalRequired = eveningRequired.size,
            eveningCompletedCount = eveningDone,
            optionalCompletedCount = optionalCompleted,
            scheduledEveningProduct = scheduledEvening.productName,
            updatedAtEpochMillis = nowMillis
        )
        dao.upsertDailySnapshot(snapshot)
    }

    suspend fun isPeriodRoutineCompletedForDate(
        date: LocalDate,
        period: RoutinePeriod
    ): Boolean {
        val dateIso = RoutineScheduleHelper.toIsoString(date)
        val activeRequired = dao.getActiveTasksSync().filter {
            it.period == period.id && !it.isOptional && RoutineScheduleHelper.isTaskScheduledForDate(it, date)
        }
        if (activeRequired.isEmpty()) return false
        val completions = dao.getCompletionsForDateSync(dateIso)
        val completedIds = completions.filter { it.completed }.map { it.taskId }.toSet()
        return activeRequired.all { completedIds.contains(it.id) }
    }

    // --- Routine Customization (Add, Edit, Reorder, Enable/Disable, Retire, Restore Default) ---

    suspend fun addCustomTask(
        period: RoutinePeriod,
        title: String,
        productName: String,
        instruction: String,
        estimatedMinutes: Int,
        isOptional: Boolean,
        reminderEnabled: Boolean,
        reminderOffsetMinutes: Int
    ) {
        val currentTasks = dao.getAllTasksSync().filter { it.period == period.id }
        val nextSortOrder = (currentTasks.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val customId = "custom_${UUID.randomUUID().toString().take(8)}"

        val entity = RoutineTaskEntity(
            id = customId,
            period = period.id,
            sortOrder = nextSortOrder,
            title = title.trim(),
            productName = productName.trim(),
            instruction = instruction.trim(),
            estimatedMinutes = estimatedMinutes.coerceIn(1, 30),
            isOptional = isOptional,
            isScheduledActive = false,
            scheduledDaysCsv = "1,2,3,4,5,6,7",
            isActive = true,
            isCustom = true,
            reminderEnabled = reminderEnabled,
            reminderOffsetMinutes = reminderOffsetMinutes.coerceIn(0, 60)
        )
        dao.upsertTask(entity)
    }

    suspend fun updateTaskDetails(
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
        val existing = dao.getAllTasksSync().firstOrNull { it.id == taskId } ?: return
        val updated = existing.copy(
            title = title.trim(),
            productName = productName.trim(),
            instruction = instruction.trim(),
            estimatedMinutes = estimatedMinutes.coerceIn(1, 30),
            isOptional = isOptional,
            isActive = isActive,
            reminderEnabled = reminderEnabled,
            reminderOffsetMinutes = reminderOffsetMinutes.coerceIn(0, 60)
        )
        dao.updateTask(updated)
    }

    suspend fun toggleTaskStepReminder(
        taskId: String,
        enabled: Boolean,
        offsetMinutes: Int
    ) {
        val existing = dao.getAllTasksSync().firstOrNull { it.id == taskId } ?: return
        dao.updateTask(
            existing.copy(
                reminderEnabled = enabled,
                reminderOffsetMinutes = offsetMinutes.coerceIn(0, 60)
            )
        )
    }

    /**
     * Retires a task (marks isActive = false) instead of deleting completion records,
     * preserving 100% of past completion history.
     */
    suspend fun retireTask(taskId: String) {
        dao.setTaskActive(taskId, false)
    }

    suspend fun setTaskActive(taskId: String, active: Boolean) {
        dao.setTaskActive(taskId, active)
    }

    suspend fun moveTaskOrder(taskId: String, moveUp: Boolean) {
        val allTasks = dao.getAllTasksSync()
        val target = allTasks.firstOrNull { it.id == taskId } ?: return
        val periodTasks = allTasks
            .filter { it.period == target.period && it.isActive }
            .sortedBy { it.sortOrder }

        val idx = periodTasks.indexOfFirst { it.id == taskId }
        if (idx == -1) return
        val swapIdx = if (moveUp) idx - 1 else idx + 1
        if (swapIdx !in periodTasks.indices) return

        val neighbor = periodTasks[swapIdx]
        val updatedTarget = target.copy(sortOrder = neighbor.sortOrder)
        val updatedNeighbor = neighbor.copy(sortOrder = target.sortOrder)
        dao.upsertTasks(listOf(updatedTarget, updatedNeighbor))
    }

    /**
     * Restores the original dermatologist-provided routine dated October 19, 2026
     * while retiring custom tasks and preserving all historical completion records.
     */
    suspend fun restoreDefaultDermatologistRoutine() {
        val allExisting = dao.getAllTasksSync()
        val retiredCustoms = allExisting.filter { it.isCustom }.map { it.copy(isActive = false) }
        if (retiredCustoms.isNotEmpty()) {
            dao.upsertTasks(retiredCustoms)
        }
        dao.upsertTasks(RoutineScheduleHelper.createInitialDermatologistTasks())
    }

    // --- Medication Dose Logging (Dohixat 100 mg) ---
    suspend fun logMedicationDose(
        date: LocalDate,
        slot: String, // "BREAKFAST" or "DINNER"
        status: String, // "TAKEN" ("Sudah diminum") or "MISSED" ("Belum/terlewat")
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val dateIso = RoutineScheduleHelper.toIsoString(date)
        dao.upsertMedicationLog(
            MedicationDoseLogEntity(
                dateIso = dateIso,
                slot = slot,
                status = status,
                loggedAtEpochMillis = nowMillis
            )
        )
    }

    // --- JSON Export & Import for Local Backup Control ---
    suspend fun exportAllDataToJson(): String {
        val tasks = dao.getAllTasksSync()
        val completions = dao.getAllCompletionsSync()
        val snapshots = dao.getAllDailySnapshotsSync()
        val medLogs = dao.getAllMedicationLogsSync()
        val settings = preferencesRepository.getSettingsSnapshot()

        val root = JSONObject()
        root.put("schemaVersion", 2)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val tasksArray = JSONArray()
        tasks.forEach { t ->
            tasksArray.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("period", t.period)
                    put("sortOrder", t.sortOrder)
                    put("title", t.title)
                    put("productName", t.productName)
                    put("instruction", t.instruction)
                    put("estimatedMinutes", t.estimatedMinutes)
                    put("isOptional", t.isOptional)
                    put("isScheduledActive", t.isScheduledActive)
                    put("scheduledDaysCsv", t.scheduledDaysCsv)
                    put("isActive", t.isActive)
                    put("isCustom", t.isCustom)
                    put("reminderEnabled", t.reminderEnabled)
                    put("reminderOffsetMinutes", t.reminderOffsetMinutes)
                }
            )
        }
        root.put("tasks", tasksArray)

        val completionsArray = JSONArray()
        completions.forEach { c ->
            completionsArray.put(
                JSONObject().apply {
                    put("dateIso", c.dateIso)
                    put("taskId", c.taskId)
                    put("period", c.period)
                    put("taskTitleSnapshot", c.taskTitleSnapshot)
                    put("productSnapshot", c.productSnapshot)
                    put("isOptionalSnapshot", c.isOptionalSnapshot)
                    put("completed", c.completed)
                    put("completedAtEpochMillis", c.completedAtEpochMillis ?: JSONObject.NULL)
                }
            )
        }
        root.put("completions", completionsArray)

        val snapshotsArray = JSONArray()
        snapshots.forEach { s ->
            snapshotsArray.put(
                JSONObject().apply {
                    put("dateIso", s.dateIso)
                    put("morningTotalRequired", s.morningTotalRequired)
                    put("morningCompletedCount", s.morningCompletedCount)
                    put("eveningTotalRequired", s.eveningTotalRequired)
                    put("eveningCompletedCount", s.eveningCompletedCount)
                    put("optionalCompletedCount", s.optionalCompletedCount)
                    put("scheduledEveningProduct", s.scheduledEveningProduct)
                    put("updatedAtEpochMillis", s.updatedAtEpochMillis)
                }
            )
        }
        root.put("dailySnapshots", snapshotsArray)

        val medLogsArray = JSONArray()
        medLogs.forEach { m ->
            medLogsArray.put(
                JSONObject().apply {
                    put("dateIso", m.dateIso)
                    put("slot", m.slot)
                    put("status", m.status)
                    put("loggedAtEpochMillis", m.loggedAtEpochMillis)
                }
            )
        }
        root.put("medicationLogs", medLogsArray)

        val settingsObj = JSONObject().apply {
            put("themeMode", settings.themeMode.id)
            put("hideSensitiveNotificationContent", settings.hideSensitiveNotificationContent)
            put("morningReminderEnabled", settings.morningReminderEnabled)
            put("morningHour", settings.morningHour)
            put("morningMinute", settings.morningMinute)
            put("eveningReminderEnabled", settings.eveningReminderEnabled)
            put("eveningHour", settings.eveningHour)
            put("eveningMinute", settings.eveningMinute)
            put("followUpReminderEnabled", settings.followUpReminderEnabled)
            put("followUpDelayMinutes", settings.followUpDelayMinutes)
            put("snoozeDurationMinutes", settings.snoozeDurationMinutes)
            put("useExactAlarm", settings.useExactAlarm)
            put("sunscreenReminderEnabled", settings.sunscreenReminderEnabled)
            put("sunscreenStartHour", settings.sunscreenStartHour)
            put("sunscreenStartMinute", settings.sunscreenStartMinute)
            put("sunscreenEndHour", settings.sunscreenEndHour)
            put("sunscreenIntervalHours", settings.sunscreenIntervalHours)
            put("medicationDiscrepancyConfirmed", settings.medicationDiscrepancyConfirmed)
            put("medicationReminderEnabled", settings.medicationReminderEnabled)
            put("medicationStartDateIso", settings.medicationStartDateIso)
            put("medicationDurationDays", settings.medicationDurationDays)
            put("medicationTotalCapsulesConfirmed", settings.medicationTotalCapsulesConfirmed)
            put("medicationBreakfastHour", settings.medicationBreakfastHour)
            put("medicationBreakfastMinute", settings.medicationBreakfastMinute)
            put("medicationDinnerHour", settings.medicationDinnerHour)
            put("medicationDinnerMinute", settings.medicationDinnerMinute)
        }
        root.put("settings", settingsObj)

        return root.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Result<Int> {
        return runCatching {
            val root = JSONObject(jsonString)
            var importedRecords = 0

            root.optJSONArray("tasks")?.let { arr ->
                val list = mutableListOf<RoutineTaskEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        RoutineTaskEntity(
                            id = o.getString("id"),
                            period = o.getString("period"),
                            sortOrder = o.getInt("sortOrder"),
                            title = o.getString("title"),
                            productName = o.getString("productName"),
                            instruction = o.getString("instruction"),
                            estimatedMinutes = o.optInt("estimatedMinutes", 2),
                            isOptional = o.optBoolean("isOptional", false),
                            isScheduledActive = o.optBoolean("isScheduledActive", false),
                            scheduledDaysCsv = o.optString("scheduledDaysCsv", "1,2,3,4,5,6,7"),
                            isActive = o.optBoolean("isActive", true),
                            isCustom = o.optBoolean("isCustom", false),
                            reminderEnabled = o.optBoolean("reminderEnabled", false),
                            reminderOffsetMinutes = o.optInt("reminderOffsetMinutes", 0)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    dao.upsertTasks(list)
                    importedRecords += list.size
                }
            }

            root.optJSONArray("completions")?.let { arr ->
                val list = mutableListOf<TaskCompletionEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val completedMillis = if (o.isNull("completedAtEpochMillis")) null else o.optLong("completedAtEpochMillis")
                    list.add(
                        TaskCompletionEntity(
                            dateIso = o.getString("dateIso"),
                            taskId = o.getString("taskId"),
                            period = o.getString("period"),
                            taskTitleSnapshot = o.optString("taskTitleSnapshot", ""),
                            productSnapshot = o.optString("productSnapshot", ""),
                            isOptionalSnapshot = o.optBoolean("isOptionalSnapshot", false),
                            completed = o.getBoolean("completed"),
                            completedAtEpochMillis = completedMillis
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    dao.upsertCompletions(list)
                    importedRecords += list.size
                }
            }

            root.optJSONArray("dailySnapshots")?.let { arr ->
                val list = mutableListOf<DailySnapshotEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        DailySnapshotEntity(
                            dateIso = o.getString("dateIso"),
                            morningTotalRequired = o.getInt("morningTotalRequired"),
                            morningCompletedCount = o.getInt("morningCompletedCount"),
                            eveningTotalRequired = o.getInt("eveningTotalRequired"),
                            eveningCompletedCount = o.getInt("eveningCompletedCount"),
                            optionalCompletedCount = o.optInt("optionalCompletedCount", 0),
                            scheduledEveningProduct = o.optString("scheduledEveningProduct", ""),
                            updatedAtEpochMillis = o.optLong("updatedAtEpochMillis", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    dao.upsertDailySnapshots(list)
                    importedRecords += list.size
                }
            }

            root.optJSONArray("medicationLogs")?.let { arr ->
                val list = mutableListOf<MedicationDoseLogEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        MedicationDoseLogEntity(
                            dateIso = o.getString("dateIso"),
                            slot = o.getString("slot"),
                            status = o.getString("status"),
                            loggedAtEpochMillis = o.optLong("loggedAtEpochMillis", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    dao.upsertMedicationLogs(list)
                    importedRecords += list.size
                }
            }

            root.optJSONObject("settings")?.let { s ->
                val current = preferencesRepository.getSettingsSnapshot()
                val discrepancyConfirmed = s.optBoolean("medicationDiscrepancyConfirmed", current.medicationDiscrepancyConfirmed)
                val updated = current.copy(
                    firstRunOnboardingDone = true,
                    themeMode = ThemeMode.fromId(s.optString("themeMode", current.themeMode.id)),
                    hideSensitiveNotificationContent = s.optBoolean("hideSensitiveNotificationContent", current.hideSensitiveNotificationContent),
                    morningReminderEnabled = s.optBoolean("morningReminderEnabled", current.morningReminderEnabled),
                    morningHour = s.optInt("morningHour", current.morningHour),
                    morningMinute = s.optInt("morningMinute", current.morningMinute),
                    eveningReminderEnabled = s.optBoolean("eveningReminderEnabled", current.eveningReminderEnabled),
                    eveningHour = s.optInt("eveningHour", current.eveningHour),
                    eveningMinute = s.optInt("eveningMinute", current.eveningMinute),
                    followUpReminderEnabled = s.optBoolean("followUpReminderEnabled", current.followUpReminderEnabled),
                    followUpDelayMinutes = s.optInt("followUpDelayMinutes", current.followUpDelayMinutes),
                    snoozeDurationMinutes = s.optInt("snoozeDurationMinutes", current.snoozeDurationMinutes),
                    useExactAlarm = s.optBoolean("useExactAlarm", current.useExactAlarm),
                    sunscreenReminderEnabled = s.optBoolean("sunscreenReminderEnabled", current.sunscreenReminderEnabled),
                    sunscreenStartHour = s.optInt("sunscreenStartHour", current.sunscreenStartHour),
                    sunscreenStartMinute = s.optInt("sunscreenStartMinute", current.sunscreenStartMinute),
                    sunscreenEndHour = s.optInt("sunscreenEndHour", current.sunscreenEndHour),
                    sunscreenIntervalHours = s.optInt("sunscreenIntervalHours", current.sunscreenIntervalHours),
                    medicationDiscrepancyConfirmed = discrepancyConfirmed,
                    medicationReminderEnabled = discrepancyConfirmed && s.optBoolean("medicationReminderEnabled", false),
                    medicationStartDateIso = s.optString("medicationStartDateIso", current.medicationStartDateIso),
                    medicationDurationDays = s.optInt("medicationDurationDays", current.medicationDurationDays),
                    medicationTotalCapsulesConfirmed = s.optInt("medicationTotalCapsulesConfirmed", current.medicationTotalCapsulesConfirmed),
                    medicationBreakfastHour = s.optInt("medicationBreakfastHour", current.medicationBreakfastHour),
                    medicationBreakfastMinute = s.optInt("medicationBreakfastMinute", current.medicationBreakfastMinute),
                    medicationDinnerHour = s.optInt("medicationDinnerHour", current.medicationDinnerHour),
                    medicationDinnerMinute = s.optInt("medicationDinnerMinute", current.medicationDinnerMinute)
                )
                preferencesRepository.replaceAllSettings(updated)
            }

            importedRecords
        }
    }
}
