package com.example.domain

import com.example.data.local.RoutineTaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RoutinePeriod(val id: String, val labelId: String) {
    MORNING("MORNING", "Pagi"),
    EVENING("EVENING", "Malam");

    companion object {
        fun fromId(id: String): RoutinePeriod =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MORNING
    }
}

data class ScheduledActiveProduct(
    val dayOfWeek: DayOfWeek,
    val dayNameIndonesian: String,
    val productName: String,
    val shortBadge: String,
    val instruction: String
)

object RoutineScheduleHelper {

    const val TASK_MORNING_CLEANSE = "morning_cleanse"
    const val TASK_MORNING_MOISTURIZER = "morning_moisturizer"
    const val TASK_MORNING_KERATO = "morning_kerato"
    const val TASK_MORNING_SUNSCREEN = "morning_sunscreen"

    const val TASK_EVENING_CLEANSE = "evening_cleanse"
    const val TASK_EVENING_MOISTURIZER = "evening_moisturizer"
    const val TASK_EVENING_SCHEDULED_ACTIVE = "evening_scheduled_active"
    const val TASK_EVENING_EFFACLAR_OPTIONAL = "evening_effaclar_optional"

    const val PHARMALENE_PRODUCT = "Pharmalene Gel 10 g"
    const val PHARMALENE_INSTRUCTION =
        "Oleskan tipis sesuai anjuran dokter kulit. Hindari sudut mata, cuping hidung, dan bibir."

    const val LABORE_SERUM_PRODUCT = "Labore AcnePro Biome Intensive Acne Serum 7 ml"
    const val LABORE_SERUM_INSTRUCTION =
        "Gunakan pada malam terjadwal tanpa Pharmalene. Oleskan merata sesuai arahan dokter kulit."

    const val STEP_INTERVAL_MINUTES = 3

    private val indonesianLocale = Locale.forLanguageTag("id-ID")
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun currentLocalDate(zoneId: ZoneId = ZoneId.systemDefault(), nowMillis: Long = System.currentTimeMillis()): LocalDate {
        return Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    }

    fun toIsoString(date: LocalDate): String = date.format(isoFormatter)

    fun parseIsoDate(dateIso: String): LocalDate = LocalDate.parse(dateIso, isoFormatter)

    fun dayNameIndonesian(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Senin"
        DayOfWeek.TUESDAY -> "Selasa"
        DayOfWeek.WEDNESDAY -> "Rabu"
        DayOfWeek.THURSDAY -> "Kamis"
        DayOfWeek.FRIDAY -> "Jumat"
        DayOfWeek.SATURDAY -> "Sabtu"
        DayOfWeek.SUNDAY -> "Minggu"
    }

    fun monthNameIndonesian(monthValue: Int): String = when (monthValue) {
        1 -> "Januari"
        2 -> "Februari"
        3 -> "Maret"
        4 -> "April"
        5 -> "Mei"
        6 -> "Juni"
        7 -> "Juli"
        8 -> "Agustus"
        9 -> "September"
        10 -> "Oktober"
        11 -> "November"
        12 -> "Desember"
        else -> ""
    }

    fun formatFullIndonesianDate(date: LocalDate): String {
        val dayName = dayNameIndonesian(date.dayOfWeek)
        val monthName = monthNameIndonesian(date.monthValue)
        return "$dayName, ${date.dayOfMonth} $monthName ${date.year}"
    }

    fun formatShortIndonesianDate(date: LocalDate): String {
        val dayName = dayNameIndonesian(date.dayOfWeek)
        val monthName = monthNameIndonesian(date.monthValue).take(3)
        return "$dayName, ${date.dayOfMonth} $monthName"
    }

    fun formatTimeHm(hour: Int, minute: Int): String {
        return String.format(indonesianLocale, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    /**
     * Dermatologist weekly evening active schedule:
     * - Monday, Wednesday, Friday: Pharmalene Gel 10 g
     * - Tuesday, Thursday, Saturday, Sunday: Labore AcnePro Biome Intensive Acne Serum 7 ml
     */
    fun getScheduledEveningProductForDay(dayOfWeek: DayOfWeek): ScheduledActiveProduct {
        val isPharmaleneNight = when (dayOfWeek) {
            DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> true
            DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> false
        }
        return if (isPharmaleneNight) {
            ScheduledActiveProduct(
                dayOfWeek = dayOfWeek,
                dayNameIndonesian = dayNameIndonesian(dayOfWeek),
                productName = PHARMALENE_PRODUCT,
                shortBadge = "Jadwal Sen • Rab • Jum",
                instruction = PHARMALENE_INSTRUCTION
            )
        } else {
            ScheduledActiveProduct(
                dayOfWeek = dayOfWeek,
                dayNameIndonesian = dayNameIndonesian(dayOfWeek),
                productName = LABORE_SERUM_PRODUCT,
                shortBadge = "Jadwal Sel • Kam • Sab • Min",
                instruction = LABORE_SERUM_INSTRUCTION
            )
        }
    }

    fun getScheduledEveningProductForDate(date: LocalDate): ScheduledActiveProduct {
        return getScheduledEveningProductForDay(date.dayOfWeek)
    }

    fun getWeeklyScheduleSummary(): List<ScheduledActiveProduct> {
        return DayOfWeek.entries.map { getScheduledEveningProductForDay(it) }
    }

    fun isTaskScheduledForDate(task: RoutineTaskEntity, date: LocalDate): Boolean {
        if (!task.isActive) return false
        if (task.scheduledDaysCsv.isBlank()) return true
        val dayNumber = date.dayOfWeek.value.toString()
        val allowedDays = task.scheduledDaysCsv.split(",").map { it.trim() }
        return allowedDays.contains(dayNumber)
    }

    fun resolveTaskForDate(task: RoutineTaskEntity, date: LocalDate): RoutineTaskEntity {
        if (!task.isScheduledActive) return task
        val scheduled = getScheduledEveningProductForDate(date)
        return task.copy(
            productName = scheduled.productName,
            instruction = scheduled.instruction
        )
    }

    /**
     * Prepopulated routine from the dermatologist's instructions dated October 19, 2026.
     * Note: Old micellar water, clay mask, toner, and old serum are intentionally excluded.
     */
    fun createInitialDermatologistTasks(): List<RoutineTaskEntity> {
        return listOf(
            // MORNING — EVERY DAY
            RoutineTaskEntity(
                id = TASK_MORNING_CLEANSE,
                period = RoutinePeriod.MORNING.id,
                sortOrder = 1,
                title = "Bersihkan wajah",
                productName = "Labore Sensitive Skin Care AcnePro Biome Sebum & Acne Cleansing Mousse, 25 ml",
                instruction = "Bersihkan wajah dengan lembut, bilas hingga bersih, lalu tepuk-tepuk hingga kering. Beri jeda sekitar 3 menit sebelum langkah berikutnya.",
                estimatedMinutes = 2,
                isOptional = false,
                isScheduledActive = false,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 0
            ),
            RoutineTaskEntity(
                id = TASK_MORNING_MOISTURIZER,
                period = RoutinePeriod.MORNING.id,
                sortOrder = 2,
                title = "Pelembap (Moisturizer)",
                productName = "Labore Sensitive Skin Care Biomerepair Topicalm Cream, 20 ml",
                instruction = "Oleskan merata sesuai anjuran dokter kulit. Tunggu sekitar 3 menit sebelum mengoleskan perawatan topikal pagi.",
                estimatedMinutes = 1,
                isOptional = false,
                isScheduledActive = false,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 3
            ),
            RoutineTaskEntity(
                id = TASK_MORNING_KERATO,
                period = RoutinePeriod.MORNING.id,
                sortOrder = 3,
                title = "Perawatan topikal pagi",
                productName = "Bioderma Sébium Kerato+, 30 ml",
                instruction = "Oleskan tipis dan merata pada wajah seukuran sekitar satu biji jagung sesuai anjuran dokter kulit. Hindari sudut mata, cuping hidung, dan bibir. Tunggu sekitar 3 menit.",
                estimatedMinutes = 2,
                isOptional = false,
                isScheduledActive = false,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 6
            ),
            RoutineTaskEntity(
                id = TASK_MORNING_SUNSCREEN,
                period = RoutinePeriod.MORNING.id,
                sortOrder = 4,
                title = "Tabir surya (Sunscreen)",
                productName = "Labore Sensitive Skin Care Biomeprotect Physical Sunscreen, 30 ml",
                instruction = "Gunakan sekitar 20 menit sebelum beraktivitas di luar ruangan. Oleskan ulang setiap 2–3 jam saat berada di luar ruangan sesuai arahan dokter kulit.",
                estimatedMinutes = 2,
                isOptional = false,
                isScheduledActive = false,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 9
            ),

            // EVENING — EVERY DAY
            RoutineTaskEntity(
                id = TASK_EVENING_CLEANSE,
                period = RoutinePeriod.EVENING.id,
                sortOrder = 1,
                title = "Bersihkan wajah",
                productName = "Labore Sensitive Skin Care AcnePro Biome Sebum & Acne Cleansing Mousse, 25 ml",
                instruction = "Bersihkan wajah secara lembut di malam hari, bilas dengan air bersih, lalu tepuk-tepuk hingga kering. Beri jeda sekitar 3 menit.",
                estimatedMinutes = 2,
                isOptional = false,
                isScheduledActive = false,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 0
            ),
            RoutineTaskEntity(
                id = TASK_EVENING_MOISTURIZER,
                period = RoutinePeriod.EVENING.id,
                sortOrder = 2,
                title = "Pelembap (Moisturizer)",
                productName = "Labore Sensitive Skin Care Biomerepair Topicalm Cream, 20 ml",
                instruction = "Oleskan sesuai arahan dokter kulit. Tunggu sekitar 3 menit sebelum mengoleskan produk malam terjadwal.",
                estimatedMinutes = 1,
                isOptional = false,
                isScheduledActive = false,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 3
            ),
            RoutineTaskEntity(
                id = TASK_EVENING_SCHEDULED_ACTIVE,
                period = RoutinePeriod.EVENING.id,
                sortOrder = 3,
                title = "Produk malam terjadwal",
                productName = PHARMALENE_PRODUCT,
                instruction = PHARMALENE_INSTRUCTION,
                estimatedMinutes = 2,
                isOptional = false,
                isScheduledActive = true,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 6
            ),
            RoutineTaskEntity(
                id = TASK_EVENING_EFFACLAR_OPTIONAL,
                period = RoutinePeriod.EVENING.id,
                sortOrder = 4,
                title = "Totol jerawat aktif (Opsional)",
                productName = "La Roche-Posay Effaclar Duo+M",
                instruction = "Totolkan hanya pada jerawat aktif di malam hari sesuai anjuran dokter kulit. Tidak wajib digunakan setiap malam bila tidak ada jerawat aktif.",
                estimatedMinutes = 1,
                isOptional = true,
                isScheduledActive = false,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 9
            )
        )
    }

    /**
     * Computes the next alarm trigger epoch milliseconds for a daily clock-time reminder.
     * If today's time has already passed OR if today's routine for that period is already completed,
     * schedules for tomorrow at the target hour:minute.
     */
    fun computeNextDailyTriggerMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        isTodayAlreadyCompleted: Boolean = false
    ): Long {
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val targetTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        var candidateZdt = ZonedDateTime.of(nowZdt.toLocalDate(), targetTime, zoneId)
        if (!candidateZdt.isAfter(nowZdt) || isTodayAlreadyCompleted) {
            candidateZdt = ZonedDateTime.of(nowZdt.toLocalDate().plusDays(1), targetTime, zoneId)
        }
        return candidateZdt.toInstant().toEpochMilli()
    }

    /**
     * Checks whether a given date is within an explicitly confirmed medication course.
     * Never automatically extends or restarts a completed course.
     */
    fun isMedicationCourseActiveOnDate(
        isConfirmed: Boolean,
        courseEnabled: Boolean,
        startDateIso: String,
        durationDays: Int,
        targetDate: LocalDate
    ): Boolean {
        if (!isConfirmed || !courseEnabled || startDateIso.isBlank() || durationDays <= 0) {
            return false
        }
        val startDate = runCatching { parseIsoDate(startDateIso) }.getOrNull() ?: return false
        val endDateInclusive = startDate.plusDays((durationDays - 1).toLong())
        return !targetDate.isBefore(startDate) && !targetDate.isAfter(endDateInclusive)
    }
}
