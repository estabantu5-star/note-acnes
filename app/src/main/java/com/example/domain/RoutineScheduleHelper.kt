package com.example.domain

import com.example.data.local.RoutineTaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class RoutinePeriod(val id: String, val labelId: String) {
    MORNING("MORNING", "Pagi"),
    EVENING("EVENING", "Malam");

    companion object {
        fun fromId(id: String): RoutinePeriod =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MORNING
    }
}

/**
 * Rule 8: Bedakan obat resep, skincare pendukung, dan sunscreen.
 */
enum class ProductCategory(val id: String, val labelIndonesian: String) {
    PRESCRIPTION_TOPICAL("OBAT_RESEP_TOPIKAL", "Obat Resep (Oles)"),
    PRESCRIPTION_ORAL("OBAT_RESEP_MINUM", "Obat Resep (Minum)"),
    SUPPORTIVE_SKINCARE("SKINCARE_PENDUKUNG", "Skincare Pendukung"),
    SUNSCREEN("SUNSCREEN", "Sunscreen");

    companion object {
        fun fromId(id: String): ProductCategory =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SUPPORTIVE_SKINCARE
    }
}

data class ScheduledActiveProduct(
    val dayOfWeek: DayOfWeek,
    val dayNameIndonesian: String,
    val taskId: String,
    val productName: String,
    val category: ProductCategory,
    val usageAmount: String,
    val applicationArea: String,
    val shortBadge: String,
    val instruction: String,
    val confirmationNote: String? = null
)

data class SunscreenDailySchedule(
    val outdoorDepartureTime: LocalTime,
    val applyBeforeOutdoorTime: LocalTime,
    val reapplyTimes: List<LocalTime>,
    val intervalHours: Int
)

object RoutineScheduleHelper {

    const val TASK_MORNING_CLEANSE = "morning_cleanse"
    const val TASK_MORNING_MOISTURIZER = "morning_moisturizer"
    const val TASK_MORNING_KERATO = "morning_kerato"
    const val TASK_MORNING_SUNSCREEN = "morning_sunscreen"

    const val TASK_EVENING_CLEANSE = "evening_cleanse"
    const val TASK_EVENING_MOISTURIZER = "evening_moisturizer"
    const val TASK_EVENING_PHARMALENE = "evening_pharmalene"
    const val TASK_EVENING_LABORE_SERUM = "evening_labore_serum"

    // Legacy alias kept for backward compatibility
    const val TASK_EVENING_SCHEDULED_ACTIVE = TASK_EVENING_PHARMALENE

    const val NEEDS_DOCTOR_CONFIRMATION = "Perlu konfirmasi ke dokter"

    const val PHARMALENE_PRODUCT = "Pharmalene Gel 10 g"
    const val PHARMALENE_USAGE_AMOUNT = "Tipis sebanyak 1 biji jagung"
    const val PHARMALENE_APPLICATION_AREA = "Seluruh wajah"
    const val PHARMALENE_INSTRUCTION =
        "Gunakan setelah moisturizer pada malam Senin, Rabu, dan Jumat. Oleskan tipis sebanyak 1 biji jagung untuk seluruh wajah sesuai resep dokter."

    const val LABORE_SERUM_PRODUCT = "Labore AcnePro Biome Intensive Acne Serum 7 ml"
    const val LABORE_SERUM_USAGE_AMOUNT = "Perlu konfirmasi ke dokter (takaran spesifik tidak tercantum pada resep)"
    const val LABORE_SERUM_APPLICATION_AREA = "Perlu konfirmasi ke dokter (area spesifik tidak tercantum pada resep)"
    const val LABORE_SERUM_INSTRUCTION =
        "Gunakan setelah moisturizer pada malam selain jadwal Pharmalene Gel (Selasa, Kamis, Sabtu, dan Minggu) sesuai resep dokter."

    const val BIOMEREPAIR_PRODUCT = "Labore Sensitive Skin Care Biomerepair Topicalm Cream, 20 ml"
    const val BIOMEREPAIR_INSTRUCTION =
        "Digunakan dua kali sehari (pagi dan malam) untuk membantu meredakan iritasi, gatal, dan kemerahan sesuai catatan resep dokter."
    const val BIOMEREPAIR_USAGE_AMOUNT = "Perlu konfirmasi ke dokter (jumlah pemakaian tidak tercantum pada resep)"
    const val BIOMEREPAIR_APPLICATION_AREA = "Perlu konfirmasi ke dokter (area aplikasi tidak tercantum pada resep)"

    const val KERATO_PRODUCT = "Bioderma Sébium Kerato+, 30 ml"
    const val KERATO_USAGE_AMOUNT = "Oles tipis (jumlah takaran spesifik: Perlu konfirmasi ke dokter)"
    const val KERATO_APPLICATION_AREA = "Seluruh wajah"
    const val KERATO_INSTRUCTION =
        "Gunakan pada pagi hari setelah moisturizer dan sebelum sunscreen. Oleskan tipis pada seluruh wajah sesuai resep dokter."

    const val SUNSCREEN_PRODUCT = "Labore Sensitive Skin Care Biomeprotect Physical Sunscreen, 30 ml"
    const val SUNSCREEN_PRE_OUTDOOR_MINUTES = 20
    const val SUNSCREEN_INSTRUCTION =
        "Gunakan pada pagi hari setelah Bioderma Sébium Kerato+. Oleskan 20 menit sebelum keluar rumah dan aplikasikan ulang setiap 2–3 jam sesuai catatan dokter."

    const val ORAL_MEDICATION_NAME = "Doxihat 100 mg"
    const val ORAL_MEDICATION_INSTRUCTION =
        "Diminum setelah makan pagi dan malam, dua kali sehari selama dua minggu (14 hari) sesuai resep dokter. Tidak berulang otomatis setelah 2 minggu."
    const val ORAL_MEDICATION_DISCREPANCY_WARNING =
        "Peringatan Instruksi Bertentangan (Perlu konfirmasi ke dokter): Resep mencantumkan konsumsi 2 kali sehari selama 2 minggu (membutuhkan 28 kapsul), namun apabila jumlah kapsul fisik yang diterima berbeda (mis. 10 kapsul), mohon konfirmasi kepada dokter atau apoteker."

    const val CHEMICAL_PEELING_WARNING =
        "Peringatan Terapi Aktif: Jangan melakukan chemical peeling atau tindakan tambahan ketika sedang menjalani terapi tanpa meminta konfirmasi dokter."

    const val DOCTOR_CONTROL_NOTE =
        "Kontrol kembali ke dokter setelah dua minggu (14 hari) sejak memulai terapi sesuai catatan dokter."

    const val DEFAULT_MEDICATION_DURATION_DAYS = 14
    const val DEFAULT_MEDICATION_TOTAL_CAPSULES = 28
    const val DEFAULT_DOCTOR_CONTROL_DAYS = 14
    const val STEP_INTERVAL_MINUTES = 3

    private val indonesianLocale = Locale.forLanguageTag("id-ID")
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun currentLocalDate(
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): LocalDate {
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
        return String.format(
            indonesianLocale,
            "%02d:%02d",
            hour.coerceIn(0, 23),
            minute.coerceIn(0, 59)
        )
    }

    fun formatScheduledDaysLabel(scheduledDaysCsv: String, period: String): String {
        val days = scheduledDaysCsv
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .sorted()
            .distinct()
        if (days.isEmpty() || days.size == 7) {
            return if (period == RoutinePeriod.MORNING.id) "Setiap Pagi" else "Setiap Malam"
        }
        val names = days.mapNotNull { num ->
            runCatching { dayNameIndonesian(DayOfWeek.of(num)) }.getOrNull()
        }
        return "Jadwal ${names.joinToString(", ")}"
    }

    /**
     * Dermatologist weekly evening active schedule:
     * - Monday, Wednesday, Friday: Pharmalene Gel 10 g (tipis sebanyak 1 biji jagung untuk seluruh wajah)
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
                taskId = TASK_EVENING_PHARMALENE,
                productName = PHARMALENE_PRODUCT,
                category = ProductCategory.PRESCRIPTION_TOPICAL,
                usageAmount = PHARMALENE_USAGE_AMOUNT,
                applicationArea = PHARMALENE_APPLICATION_AREA,
                shortBadge = "Jadwal Senin, Rabu, Jumat",
                instruction = PHARMALENE_INSTRUCTION,
                confirmationNote = null
            )
        } else {
            ScheduledActiveProduct(
                dayOfWeek = dayOfWeek,
                dayNameIndonesian = dayNameIndonesian(dayOfWeek),
                taskId = TASK_EVENING_LABORE_SERUM,
                productName = LABORE_SERUM_PRODUCT,
                category = ProductCategory.SUPPORTIVE_SKINCARE,
                usageAmount = LABORE_SERUM_USAGE_AMOUNT,
                applicationArea = LABORE_SERUM_APPLICATION_AREA,
                shortBadge = "Jadwal Selasa, Kamis, Sabtu, Minggu",
                instruction = LABORE_SERUM_INSTRUCTION,
                confirmationNote = NEEDS_DOCTOR_CONFIRMATION
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

    /**
     * Returns the tasks scheduled for [date], strictly enforcing that:
     * 1. Pharmalene Gel never appears on nights scheduled for Labore Intensive Acne Serum (and vice versa).
     * 2. Two active evening products are NEVER displayed as mandatory on the same day.
     */
    fun getTasksForDate(
        allTasks: List<RoutineTaskEntity>,
        date: LocalDate
    ): List<RoutineTaskEntity> {
        val scheduledTonight = getScheduledEveningProductForDate(date)
        val filtered = allTasks
            .filter { isTaskScheduledForDate(it, date) }
            .sortedWith(compareBy({ it.period }, { it.sortOrder }))

        val result = mutableListOf<RoutineTaskEntity>()
        var eveningActiveIncluded = false

        for (task in filtered) {
            if (task.period == RoutinePeriod.EVENING.id && task.isScheduledActive && !task.isOptional) {
                // Only include the evening active product that matches tonight's prescription schedule,
                // and never include more than one mandatory evening active on the same day.
                if (eveningActiveIncluded) continue
                if (task.id == TASK_EVENING_PHARMALENE && scheduledTonight.taskId != TASK_EVENING_PHARMALENE) {
                    continue
                }
                if (task.id == TASK_EVENING_LABORE_SERUM && scheduledTonight.taskId != TASK_EVENING_LABORE_SERUM) {
                    continue
                }
                eveningActiveIncluded = true
                result.add(resolveTaskForDate(task, date))
            } else {
                result.add(resolveTaskForDate(task, date))
            }
        }
        return result
    }

    fun resolveTaskForDate(task: RoutineTaskEntity, date: LocalDate): RoutineTaskEntity {
        if (task.id == "evening_scheduled_active") {
            val scheduled = getScheduledEveningProductForDate(date)
            return task.copy(
                productName = scheduled.productName,
                category = scheduled.category.id,
                usageAmount = scheduled.usageAmount,
                applicationArea = scheduled.applicationArea,
                instruction = scheduled.instruction,
                confirmationNote = scheduled.confirmationNote.orEmpty()
            )
        }
        return task
    }

    /**
     * Rule 6 & Rule 9: Detects conflicting instructions or unconfirmed additional procedures
     * (e.g., chemical peeling, multiple evening actives scheduled on the same day)
     * and warns the user to confirm with their doctor.
     */
    fun detectRoutineConflictsForDate(
        allTasks: List<RoutineTaskEntity>,
        date: LocalDate
    ): List<String> {
        val warnings = mutableListOf<String>()
        val dayTasks = allTasks.filter { isTaskScheduledForDate(it, date) }

        val mandatoryEveningActives = dayTasks.filter {
            it.period == RoutinePeriod.EVENING.id && it.isScheduledActive && !it.isOptional
        }
        if (mandatoryEveningActives.size > 1) {
            warnings.add(
                "Instruksi bertentangan: Terdapat lebih dari satu produk aktif malam (${mandatoryEveningActives.joinToString { it.productName }}) pada hari yang sama. Aplikasi hanya mewajibkan satu produk sesuai jadwal resep. Perlu konfirmasi ke dokter."
            )
        }

        val peelingKeywords = listOf("peeling", "chemical peeling", "eksfoliasi", "scrub", "laser", "facial")
        dayTasks.forEach { task ->
            val combined = "${task.title} ${task.productName} ${task.instruction}".lowercase(indonesianLocale)
            if (peelingKeywords.any { combined.contains(it) }) {
                warnings.add(
                    "Perlu konfirmasi ke dokter: Langkah \"${task.title}\" mengandung tindakan tambahan/peeling. Jangan melakukan chemical peeling atau tindakan tambahan saat menjalani terapi tanpa konfirmasi dokter."
                )
            }
        }

        return warnings.distinct()
    }

    /**
     * Rule 10: Ensures no task or text claims that a product is guaranteed to cure acne or acne scars.
     */
    fun containsProhibitedMedicalClaims(text: String): Boolean {
        val lower = text.lowercase(indonesianLocale)
        val prohibitedPhrases = listOf(
            "pasti menyembuhkan",
            "pasti sembuh",
            "menjamin sembuh",
            "menyembuhkan jerawat 100%",
            "menyembuhkan bekas jerawat",
            "menghilangkan bekas jerawat permanen",
            "jaminan sembuh total"
        )
        return prohibitedPhrases.any { lower.contains(it) }
    }

    /**
     * Prepopulated routine strictly matching the dermatologist's prescription without inventing
     * dosage, frequency, application area, or medical cure claims.
     */
    fun createInitialDermatologistTasks(): List<RoutineTaskEntity> {
        return listOf(
            // MORNING — EVERY DAY (Pagi: 1. Pembersih, 2. Moisturizer, 3. Bioderma Sebium Kerato+, 4. Sunscreen)
            RoutineTaskEntity(
                id = TASK_MORNING_CLEANSE,
                period = RoutinePeriod.MORNING.id,
                sortOrder = 1,
                title = "Pembersih Wajah Pagi",
                productName = "Labore Sensitive Skin Care AcnePro Biome Sebum & Acne Cleansing Mousse, 25 ml",
                instruction = "Gunakan sebagai pembersih wajah pada pagi hari sebelum moisturizer sesuai urutan resep dokter.",
                category = ProductCategory.SUPPORTIVE_SKINCARE.id,
                usageAmount = "Perlu konfirmasi ke dokter (jumlah takaran spesifik tidak tercantum pada resep)",
                applicationArea = "Seluruh wajah",
                durationNote = "Setiap pagi sesuai urutan resep dokter",
                confirmationNote = "Jumlah takaran spesifik: Perlu konfirmasi ke dokter",
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
                title = "Moisturizer Pagi",
                productName = BIOMEREPAIR_PRODUCT,
                instruction = BIOMEREPAIR_INSTRUCTION,
                category = ProductCategory.SUPPORTIVE_SKINCARE.id,
                usageAmount = BIOMEREPAIR_USAGE_AMOUNT,
                applicationArea = BIOMEREPAIR_APPLICATION_AREA,
                durationNote = "Dua kali sehari (pagi dan malam) sesuai resep dokter",
                confirmationNote = "Jumlah pemakaian & area aplikasi spesifik: Perlu konfirmasi ke dokter",
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
                title = "Obat Oles Pagi",
                productName = KERATO_PRODUCT,
                instruction = KERATO_INSTRUCTION,
                category = ProductCategory.PRESCRIPTION_TOPICAL.id,
                usageAmount = KERATO_USAGE_AMOUNT,
                applicationArea = KERATO_APPLICATION_AREA,
                durationNote = "Setiap pagi setelah moisturizer dan sebelum sunscreen",
                confirmationNote = "Takaran angka spesifik: Perlu konfirmasi ke dokter (resep mencantumkan oles tipis seluruh wajah)",
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
                title = "Sunscreen Pagi",
                productName = SUNSCREEN_PRODUCT,
                instruction = SUNSCREEN_INSTRUCTION,
                category = ProductCategory.SUNSCREEN.id,
                usageAmount = "Perlu konfirmasi ke dokter (jumlah takaran spesifik tidak tercantum pada resep)",
                applicationArea = "Seluruh wajah",
                durationNote = "Pagi hari, oles 20 menit sebelum keluar rumah & aplikasikan ulang setiap 2–3 jam",
                confirmationNote = "Jumlah takaran spesifik: Perlu konfirmasi ke dokter",
                estimatedMinutes = 2,
                isOptional = false,
                isScheduledActive = false,
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 9
            ),

            // EVENING — (Malam: 1. Pembersih, 2. Moisturizer, 3. Pharmalene Gel [Sen, Rab, Jum] ATAU Labore Serum [Sel, Kam, Sab, Min])
            RoutineTaskEntity(
                id = TASK_EVENING_CLEANSE,
                period = RoutinePeriod.EVENING.id,
                sortOrder = 1,
                title = "Pembersih Wajah Malam",
                productName = "Labore Sensitive Skin Care AcnePro Biome Sebum & Acne Cleansing Mousse, 25 ml",
                instruction = "Gunakan sebagai pembersih wajah pada malam hari sebelum moisturizer sesuai urutan resep dokter.",
                category = ProductCategory.SUPPORTIVE_SKINCARE.id,
                usageAmount = "Perlu konfirmasi ke dokter (jumlah takaran spesifik tidak tercantum pada resep)",
                applicationArea = "Seluruh wajah",
                durationNote = "Setiap malam sesuai urutan resep dokter",
                confirmationNote = "Jumlah takaran spesifik: Perlu konfirmasi ke dokter",
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
                title = "Moisturizer Malam",
                productName = BIOMEREPAIR_PRODUCT,
                instruction = BIOMEREPAIR_INSTRUCTION,
                category = ProductCategory.SUPPORTIVE_SKINCARE.id,
                usageAmount = BIOMEREPAIR_USAGE_AMOUNT,
                applicationArea = BIOMEREPAIR_APPLICATION_AREA,
                durationNote = "Dua kali sehari (pagi dan malam) sesuai resep dokter",
                confirmationNote = "Jumlah pemakaian & area aplikasi spesifik: Perlu konfirmasi ke dokter",
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
                id = TASK_EVENING_PHARMALENE,
                period = RoutinePeriod.EVENING.id,
                sortOrder = 3,
                title = "Obat Oles Malam (Senin, Rabu, Jumat)",
                productName = PHARMALENE_PRODUCT,
                instruction = PHARMALENE_INSTRUCTION,
                category = ProductCategory.PRESCRIPTION_TOPICAL.id,
                usageAmount = PHARMALENE_USAGE_AMOUNT,
                applicationArea = PHARMALENE_APPLICATION_AREA,
                durationNote = "Khusus malam Senin, Rabu, dan Jumat (kontrol kembali setelah 2 minggu)",
                confirmationNote = "",
                estimatedMinutes = 2,
                isOptional = false,
                isScheduledActive = true,
                scheduledDaysCsv = "1,3,5", // Monday, Wednesday, Friday ONLY
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 6
            ),
            RoutineTaskEntity(
                id = TASK_EVENING_LABORE_SERUM,
                period = RoutinePeriod.EVENING.id,
                sortOrder = 3,
                title = "Serum Malam (Selasa, Kamis, Sabtu, Minggu)",
                productName = LABORE_SERUM_PRODUCT,
                instruction = LABORE_SERUM_INSTRUCTION,
                category = ProductCategory.SUPPORTIVE_SKINCARE.id,
                usageAmount = LABORE_SERUM_USAGE_AMOUNT,
                applicationArea = LABORE_SERUM_APPLICATION_AREA,
                durationNote = "Khusus malam Selasa, Kamis, Sabtu, dan Minggu (hari selain jadwal Pharmalene Gel)",
                confirmationNote = "Jumlah pemakaian & area aplikasi spesifik: Perlu konfirmasi ke dokter",
                estimatedMinutes = 2,
                isOptional = false,
                isScheduledActive = true,
                scheduledDaysCsv = "2,4,6,7", // Tuesday, Thursday, Saturday, Sunday ONLY
                isActive = true,
                isCustom = false,
                reminderEnabled = false,
                reminderOffsetMinutes = 6
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
     * Checks whether a given date is within the prescribed medication course (default 14 days / 2 weeks).
     * Never automatically extends or repeats a completed course after [durationDays].
     */
    fun isMedicationCourseActiveOnDate(
        isConfirmed: Boolean = true,
        courseEnabled: Boolean,
        startDateIso: String,
        durationDays: Int = DEFAULT_MEDICATION_DURATION_DAYS,
        targetDate: LocalDate
    ): Boolean {
        if (!isConfirmed || !courseEnabled || startDateIso.isBlank() || durationDays <= 0) {
            return false
        }
        val startDate = runCatching { parseIsoDate(startDateIso) }.getOrNull() ?: return false
        val endDateInclusive = startDate.plusDays((durationDays - 1).toLong())
        return !targetDate.isBefore(startDate) && !targetDate.isAfter(endDateInclusive)
    }

    /**
     * Computes the next alarm trigger for oral medication (Doxihat 100 mg) strictly within the
     * 14-day (2-week) course window. Returns null once the 2-week course finishes so it NEVER auto-repeats.
     */
    fun computeNextMedicationTriggerMillis(
        isConfirmed: Boolean = true,
        courseEnabled: Boolean,
        startDateIso: String,
        durationDays: Int = DEFAULT_MEDICATION_DURATION_DAYS,
        hour: Int,
        minute: Int,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        isTodaySlotTaken: Boolean = false
    ): Long? {
        if (!isConfirmed || !courseEnabled || startDateIso.isBlank() || durationDays <= 0) {
            return null
        }
        val startDate = runCatching { parseIsoDate(startDateIso) }.getOrNull() ?: return null
        val endDateInclusive = startDate.plusDays((durationDays - 1).toLong())
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val today = nowZdt.toLocalDate()
        val targetTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

        // If course hasn't started yet, schedule for startDate
        if (today.isBefore(startDate)) {
            return ZonedDateTime.of(startDate, targetTime, zoneId).toInstant().toEpochMilli()
        }

        // Check today's slot first
        val todayCandidate = ZonedDateTime.of(today, targetTime, zoneId)
        if (todayCandidate.isAfter(nowZdt) && !isTodaySlotTaken && !today.isAfter(endDateInclusive)) {
            return todayCandidate.toInstant().toEpochMilli()
        }

        // Otherwise check tomorrow's slot — only if tomorrow is still within the 2-week course
        val tomorrow = today.plusDays(1)
        if (!tomorrow.isAfter(endDateInclusive)) {
            return ZonedDateTime.of(tomorrow, targetTime, zoneId).toInstant().toEpochMilli()
        }

        // Course is finished -> do not repeat automatically
        return null
    }

    /**
     * Computes the sunscreen daily schedule according to doctor's note:
     * - Apply 20 minutes before going outside ([outdoorDepartureTime] minus 20 minutes)
     * - Reapply every [intervalHours] (2–3 hours) until [endHour].
     */
    fun computeSunscreenScheduleForDay(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        intervalHours: Int
    ): SunscreenDailySchedule {
        val departure = LocalTime.of(startHour.coerceIn(5, 18), startMinute.coerceIn(0, 59))
        val applyBefore = departure.minusMinutes(SUNSCREEN_PRE_OUTDOOR_MINUTES.toLong())
        val end = LocalTime.of(endHour.coerceIn(10, 21), 0)
        val step = intervalHours.coerceIn(2, 3).toLong()

        val reapplyList = mutableListOf<LocalTime>()
        var nextSlot = departure.plusHours(step)
        while (!nextSlot.isAfter(end)) {
            reapplyList.add(nextSlot)
            nextSlot = nextSlot.plusHours(step)
        }
        return SunscreenDailySchedule(
            outdoorDepartureTime = departure,
            applyBeforeOutdoorTime = applyBefore,
            reapplyTimes = reapplyList,
            intervalHours = step.toInt()
        )
    }

    /**
     * Rule 7: Computes the 2-week (14-day) doctor follow-up consultation date.
     */
    fun computeDoctorControlDate(
        startDate: LocalDate,
        controlDays: Int = DEFAULT_DOCTOR_CONTROL_DAYS
    ): LocalDate {
        return startDate.plusDays(controlDays.coerceAtLeast(1).toLong())
    }

    fun computeDoctorControlDaysRemaining(
        startDateIso: String,
        controlDays: Int = DEFAULT_DOCTOR_CONTROL_DAYS,
        today: LocalDate
    ): Int? {
        if (startDateIso.isBlank()) return null
        val startDate = runCatching { parseIsoDate(startDateIso) }.getOrNull() ?: return null
        val controlDate = computeDoctorControlDate(startDate, controlDays)
        return ChronoUnit.DAYS.between(today, controlDate).toInt()
    }

    /**
     * Computes the alarm trigger epoch milliseconds for the 2-week doctor follow-up reminder.
     * Returns null if the follow-up date/time has already passed.
     */
    fun computeDoctorControlTriggerMillis(
        startDateIso: String,
        controlDays: Int = DEFAULT_DOCTOR_CONTROL_DAYS,
        hour: Int = 9,
        minute: Int = 0,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        if (startDateIso.isBlank()) return null
        val startDate = runCatching { parseIsoDate(startDateIso) }.getOrNull() ?: return null
        val controlDate = computeDoctorControlDate(startDate, controlDays)
        val targetTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val candidateZdt = ZonedDateTime.of(controlDate, targetTime, zoneId)
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        return if (candidateZdt.isAfter(nowZdt)) {
            candidateZdt.toInstant().toEpochMilli()
        } else {
            null
        }
    }
}
