package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SkinCareDao {

    // --- Routine Tasks ---
    @Query("SELECT * FROM routine_tasks ORDER BY period ASC, sortOrder ASC")
    fun observeAllTasks(): Flow<List<RoutineTaskEntity>>

    @Query("SELECT * FROM routine_tasks WHERE isActive = 1 ORDER BY period ASC, sortOrder ASC")
    fun observeActiveTasks(): Flow<List<RoutineTaskEntity>>

    @Query("SELECT * FROM routine_tasks ORDER BY period ASC, sortOrder ASC")
    suspend fun getAllTasksSync(): List<RoutineTaskEntity>

    @Query("SELECT * FROM routine_tasks WHERE isActive = 1 ORDER BY period ASC, sortOrder ASC")
    suspend fun getActiveTasksSync(): List<RoutineTaskEntity>

    @Query("SELECT COUNT(*) FROM routine_tasks")
    suspend fun getTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<RoutineTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: RoutineTaskEntity)

    @Update
    suspend fun updateTask(task: RoutineTaskEntity)

    @Query("UPDATE routine_tasks SET isActive = :isActive WHERE id = :taskId")
    suspend fun setTaskActive(taskId: String, isActive: Boolean)

    // --- Task Completions ---
    @Query("SELECT * FROM task_completions WHERE dateIso = :dateIso")
    fun observeCompletionsForDate(dateIso: String): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE dateIso = :dateIso")
    suspend fun getCompletionsForDateSync(dateIso: String): List<TaskCompletionEntity>

    @Query("SELECT * FROM task_completions ORDER BY dateIso DESC")
    fun observeAllCompletions(): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions ORDER BY dateIso DESC")
    suspend fun getAllCompletionsSync(): List<TaskCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: TaskCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletions(completions: List<TaskCompletionEntity>)

    @Query("DELETE FROM task_completions WHERE dateIso = :dateIso")
    suspend fun clearCompletionsForDate(dateIso: String)

    // --- Daily Snapshots (History Preservation) ---
    @Query("SELECT * FROM daily_snapshots ORDER BY dateIso DESC")
    fun observeAllDailySnapshots(): Flow<List<DailySnapshotEntity>>

    @Query("SELECT * FROM daily_snapshots ORDER BY dateIso DESC")
    suspend fun getAllDailySnapshotsSync(): List<DailySnapshotEntity>

    @Query("SELECT * FROM daily_snapshots WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getDailySnapshotSync(dateIso: String): DailySnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySnapshot(snapshot: DailySnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySnapshots(snapshots: List<DailySnapshotEntity>)

    @Query("DELETE FROM daily_snapshots WHERE dateIso = :dateIso")
    suspend fun deleteDailySnapshotForDate(dateIso: String)

    // --- Medication Logs ---
    @Query("SELECT * FROM medication_dose_logs WHERE dateIso = :dateIso")
    fun observeMedicationLogsForDate(dateIso: String): Flow<List<MedicationDoseLogEntity>>

    @Query("SELECT * FROM medication_dose_logs ORDER BY dateIso DESC")
    fun observeAllMedicationLogs(): Flow<List<MedicationDoseLogEntity>>

    @Query("SELECT * FROM medication_dose_logs ORDER BY dateIso DESC")
    suspend fun getAllMedicationLogsSync(): List<MedicationDoseLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedicationLog(log: MedicationDoseLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedicationLogs(logs: List<MedicationDoseLogEntity>)

    @Transaction
    suspend fun recordTaskCompletionAndSnapshot(
        completion: TaskCompletionEntity,
        snapshot: DailySnapshotEntity
    ) {
        upsertCompletion(completion)
        upsertDailySnapshot(snapshot)
    }
}
