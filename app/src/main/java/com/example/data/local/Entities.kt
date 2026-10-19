package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_tasks",
    indices = [Index(value = ["period", "sortOrder"])]
)
data class RoutineTaskEntity(
    @PrimaryKey
    val id: String,
    val period: String, // "MORNING" or "EVENING"
    val sortOrder: Int,
    val title: String,
    val productName: String,
    val instruction: String,
    val estimatedMinutes: Int = 2,
    val isOptional: Boolean = false,
    val isScheduledActive: Boolean = false,
    val scheduledDaysCsv: String = "1,2,3,4,5,6,7",
    val isActive: Boolean = true,
    val isCustom: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 0
)

@Entity(
    tableName = "task_completions",
    primaryKeys = ["dateIso", "taskId"],
    indices = [Index(value = ["dateIso"]), Index(value = ["taskId"])]
)
data class TaskCompletionEntity(
    val dateIso: String, // "YYYY-MM-DD" in local timezone
    val taskId: String,
    val period: String,
    val taskTitleSnapshot: String,
    val productSnapshot: String,
    val isOptionalSnapshot: Boolean,
    val completed: Boolean,
    val completedAtEpochMillis: Long?
)

@Entity(tableName = "daily_snapshots")
data class DailySnapshotEntity(
    @PrimaryKey
    val dateIso: String, // "YYYY-MM-DD"
    val morningTotalRequired: Int,
    val morningCompletedCount: Int,
    val eveningTotalRequired: Int,
    val eveningCompletedCount: Int,
    val optionalCompletedCount: Int,
    val scheduledEveningProduct: String,
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "medication_dose_logs",
    primaryKeys = ["dateIso", "slot"],
    indices = [Index(value = ["dateIso"])]
)
data class MedicationDoseLogEntity(
    val dateIso: String, // "YYYY-MM-DD"
    val slot: String, // "BREAKFAST" (Sehabis Sarapan) or "DINNER" (Sehabis Makan Malam)
    val status: String, // "TAKEN" (Sudah diminum) or "MISSED" (Belum/terlewat)
    val loggedAtEpochMillis: Long
)
