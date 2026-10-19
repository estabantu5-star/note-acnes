package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RoutineTaskEntity::class,
        TaskCompletionEntity::class,
        DailySnapshotEntity::class,
        MedicationDoseLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SkinCareDatabase : RoomDatabase() {

    abstract fun skinCareDao(): SkinCareDao

    companion object {
        private const val DATABASE_NAME = "skincare_daily.db"

        /**
         * Explicit migration from schema v1 to v2 ensuring historical user data is preserved
         * without destructive fallback.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `medication_dose_logs` (
                        `dateIso` TEXT NOT NULL,
                        `slot` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `loggedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`dateIso`, `slot`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_medication_dose_logs_dateIso` ON `medication_dose_logs` (`dateIso`)"
                )
            }
        }

        /**
         * Explicit migration from schema v2 to v3 adding prescription metadata columns
         * while preserving all existing completion and history records.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `routine_tasks` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'SKINCARE_PENDUKUNG'"
                )
                db.execSQL(
                    "ALTER TABLE `routine_tasks` ADD COLUMN `usageAmount` TEXT NOT NULL DEFAULT 'Perlu konfirmasi ke dokter'"
                )
                db.execSQL(
                    "ALTER TABLE `routine_tasks` ADD COLUMN `applicationArea` TEXT NOT NULL DEFAULT 'Perlu konfirmasi ke dokter'"
                )
                db.execSQL(
                    "ALTER TABLE `routine_tasks` ADD COLUMN `durationNote` TEXT NOT NULL DEFAULT 'Sesuai resep dokter'"
                )
                db.execSQL(
                    "ALTER TABLE `routine_tasks` ADD COLUMN `confirmationNote` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        @Volatile
        private var INSTANCE: SkinCareDatabase? = null

        fun getInstance(context: Context): SkinCareDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SkinCareDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): SkinCareDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                SkinCareDatabase::class.java
            )
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
