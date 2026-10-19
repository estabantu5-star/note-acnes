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
    version = 2,
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

        @Volatile
        private var INSTANCE: SkinCareDatabase? = null

        fun getInstance(context: Context): SkinCareDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SkinCareDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
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
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
