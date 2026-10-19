package com.example

import com.example.domain.ProductCategory
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ExampleUnitTest {

    @Test
    fun `verify Pharmalene schedule on Monday Wednesday and Friday`() {
        val tasks = RoutineScheduleHelper.createInitialDermatologistTasks()
        val mwfDates = listOf(
            LocalDate.of(2026, 10, 19), // Monday
            LocalDate.of(2026, 10, 21), // Wednesday
            LocalDate.of(2026, 10, 23)  // Friday
        )
        for (date in mwfDates) {
            val scheduled = RoutineScheduleHelper.getScheduledEveningProductForDay(date.dayOfWeek)
            assertEquals(RoutineScheduleHelper.PHARMALENE_PRODUCT, scheduled.productName)
            assertEquals("Tipis sebanyak 1 biji jagung", scheduled.usageAmount)
            assertEquals("Seluruh wajah", scheduled.applicationArea)

            val eveningTasksForDate = RoutineScheduleHelper.getTasksForDate(
                allTasks = tasks,
                date = date
            ).filter { it.period == RoutinePeriod.EVENING.id }

            assertTrue(eveningTasksForDate.any { it.id == RoutineScheduleHelper.TASK_EVENING_PHARMALENE })
            assertFalse(eveningTasksForDate.any { it.id == RoutineScheduleHelper.TASK_EVENING_LABORE_SERUM })
        }
    }

    @Test
    fun `verify Labore serum schedule on Tuesday Thursday Saturday and Sunday`() {
        val tasks = RoutineScheduleHelper.createInitialDermatologistTasks()
        val ttssDates = listOf(
            LocalDate.of(2026, 10, 20), // Tuesday
            LocalDate.of(2026, 10, 22), // Thursday
            LocalDate.of(2026, 10, 24), // Saturday
            LocalDate.of(2026, 10, 25)  // Sunday
        )
        for (date in ttssDates) {
            val scheduled = RoutineScheduleHelper.getScheduledEveningProductForDay(date.dayOfWeek)
            assertEquals(RoutineScheduleHelper.LABORE_SERUM_PRODUCT, scheduled.productName)

            val eveningTasksForDate = RoutineScheduleHelper.getTasksForDate(
                allTasks = tasks,
                date = date
            ).filter { it.period == RoutinePeriod.EVENING.id }

            assertTrue(eveningTasksForDate.any { it.id == RoutineScheduleHelper.TASK_EVENING_LABORE_SERUM })
            assertFalse(eveningTasksForDate.any { it.id == RoutineScheduleHelper.TASK_EVENING_PHARMALENE })
        }
    }

    @Test
    fun `verify prescription audit rules for categories usage area and confirmation notes`() {
        val tasks = RoutineScheduleHelper.createInitialDermatologistTasks()
        assertEquals(8, tasks.size)

        // Morning required: Cleanser, Moisturizer, Bioderma Sebium Kerato+, Sunscreen Labore
        val morningRequired = tasks.filter { it.period == "MORNING" && !it.isOptional }
        assertEquals(4, morningRequired.size)

        val kerato = tasks.first { it.id == RoutineScheduleHelper.TASK_MORNING_KERATO }
        assertTrue(kerato.usageAmount.contains("Oles tipis"))
        assertEquals("Seluruh wajah", kerato.applicationArea)

        val sunscreen = tasks.first { it.id == RoutineScheduleHelper.TASK_MORNING_SUNSCREEN }
        assertEquals(ProductCategory.SUNSCREEN.id, sunscreen.category)
        assertTrue(sunscreen.durationNote.contains("20 menit sebelum keluar rumah"))
        assertTrue(sunscreen.durationNote.contains("2–3 jam"))

        val pharmalene = tasks.first { it.id == RoutineScheduleHelper.TASK_EVENING_PHARMALENE }
        assertEquals(ProductCategory.PRESCRIPTION_TOPICAL.id, pharmalene.category)
        assertEquals("1,3,5", pharmalene.scheduledDaysCsv)
        assertEquals("Tipis sebanyak 1 biji jagung", pharmalene.usageAmount)
        assertEquals("Seluruh wajah", pharmalene.applicationArea)

        val laboreSerum = tasks.first { it.id == RoutineScheduleHelper.TASK_EVENING_LABORE_SERUM }
        assertEquals("2,4,6,7", laboreSerum.scheduledDaysCsv)

        // Labore Biomerepair Topical Cream must be marked "Perlu konfirmasi ke dokter" for unspecified usage/area
        val biomerepairMorning = tasks.first { it.id == RoutineScheduleHelper.TASK_MORNING_MOISTURIZER }
        val biomerepairEvening = tasks.first { it.id == RoutineScheduleHelper.TASK_EVENING_MOISTURIZER }

        assertTrue(biomerepairMorning.confirmationNote.contains("Perlu konfirmasi ke dokter"))
        assertTrue(biomerepairEvening.confirmationNote.contains("Perlu konfirmasi ke dokter"))

        // No healing claims ("pasti menyembuhkan")
        val allText = tasks.joinToString(" ") { "${it.title} ${it.productName} ${it.instruction}" }
        assertFalse(RoutineScheduleHelper.containsProhibitedMedicalClaims(allText))
    }

    @Test
    fun `verify conflict detection when Pharmalene and Labore Serum are scheduled on the same evening`() {
        val monday = LocalDate.of(2026, 10, 19)
        val defaultTasks = RoutineScheduleHelper.createInitialDermatologistTasks()
        assertTrue(RoutineScheduleHelper.detectRoutineConflictsForDate(defaultTasks, monday).isEmpty())

        // Simulate user modifying Labore Serum to also run on Monday (1)
        val conflictingTasks = defaultTasks.map { task ->
            if (task.id == RoutineScheduleHelper.TASK_EVENING_LABORE_SERUM) {
                task.copy(scheduledDaysCsv = "1,2,4,6,7")
            } else {
                task
            }
        }
        val warnings = RoutineScheduleHelper.detectRoutineConflictsForDate(conflictingTasks, monday)
        assertFalse(warnings.isEmpty())
        assertTrue(warnings.first().contains("Perlu konfirmasi ke dokter", ignoreCase = true))
    }

    @Test
    fun `verify next daily alarm and 2-week doctor control trigger calculation`() {
        val zone = ZoneId.of("Asia/Jakarta")
        val nowZdt = ZonedDateTime.of(2026, 10, 19, 6, 30, 0, 0, zone)
        val nowMillis = nowZdt.toInstant().toEpochMilli()

        val triggerToday = RoutineScheduleHelper.computeNextDailyTriggerMillis(
            hour = 7,
            minute = 0,
            nowMillis = nowMillis,
            zoneId = zone,
            isTodayAlreadyCompleted = false
        )
        val expectedToday = ZonedDateTime.of(2026, 10, 19, 7, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expectedToday, triggerToday)

        // 2-week doctor control from 2026-10-19 -> 2026-11-02 (14 days later)
        val startDate = LocalDate.of(2026, 10, 19)
        val controlDate = RoutineScheduleHelper.computeDoctorControlDate(startDate)
        assertEquals(LocalDate.of(2026, 11, 2), controlDate)

        val controlTrigger = RoutineScheduleHelper.computeDoctorControlTriggerMillis(
            startDateIso = "2026-10-19",
            controlDays = 14,
            hour = 9,
            minute = 0,
            nowMillis = nowMillis,
            zoneId = zone
        )
        val expectedControlTrigger = ZonedDateTime.of(2026, 11, 2, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expectedControlTrigger, controlTrigger)
    }

    @Test
    fun `verify Doxihat 100 mg 2-week course boundary without auto extension`() {
        val startDate = LocalDate.of(2026, 10, 19)
        val startIso = RoutineScheduleHelper.toIsoString(startDate)

        // Active on day 1..14 (2 weeks) when confirmed
        assertTrue(
            RoutineScheduleHelper.isMedicationCourseActiveOnDate(
                isConfirmed = true,
                courseEnabled = true,
                startDateIso = startIso,
                durationDays = 14,
                targetDate = LocalDate.of(2026, 11, 1) // Day 14
            )
        )
        assertFalse(
            RoutineScheduleHelper.isMedicationCourseActiveOnDate(
                isConfirmed = true,
                courseEnabled = true,
                startDateIso = startIso,
                durationDays = 14,
                targetDate = LocalDate.of(2026, 11, 2) // Day 15 -> Course finished
            )
        )
    }

    @Test
    fun `verify SemanticVersionComparator handles v prefix multi-digit segments and short versions`() {
        // Normalization
        assertEquals(
            listOf(1, 0, 5),
            com.example.update.SemanticVersionComparator.parseVersionParts("v1.0.5")
        )
        assertEquals(
            listOf(1, 0, 5),
            com.example.update.SemanticVersionComparator.parseVersionParts("1.0.5")
        )
        assertEquals(
            listOf(1, 0, 10),
            com.example.update.SemanticVersionComparator.parseVersionParts("v1.0.10")
        )
        assertEquals(
            listOf(1, 1, 0),
            com.example.update.SemanticVersionComparator.parseVersionParts("v1.1")
        )
        assertEquals(
            listOf(1, 0, 5),
            com.example.update.SemanticVersionComparator.parseVersionParts("V1.0.5-release+build9")
        )

        // Numerical comparison (1.0.10 must be newer than 1.0.5!)
        assertTrue(com.example.update.SemanticVersionComparator.isRemoteNewer("1.0.5", "v1.0.10"))
        assertTrue(com.example.update.SemanticVersionComparator.isRemoteNewer("v1.0.5", "v1.0.6"))
        assertTrue(com.example.update.SemanticVersionComparator.isRemoteNewer("1.0.9", "v1.1"))
        assertFalse(com.example.update.SemanticVersionComparator.isRemoteNewer("1.0.5", "v1.0.5"))
        assertFalse(com.example.update.SemanticVersionComparator.isRemoteNewer("1.0.10", "v1.0.5"))
    }

    @Test
    fun `verify GitHubReleaseParser validates trusted HTTPS domains and extracts SHA-256 digests`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            com.example.update.GitHubReleaseParser.extractSha256Hex(
                "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            )
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            com.example.update.GitHubReleaseParser.extractSha256FromReleaseNotes(
                "### Perubahan\nSHA256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "SkinCare-Daily.apk"
            )
        )

        // Domain security checks
        assertTrue(
            com.example.update.GitHubReleaseParser.isTrustedGitHubDownloadUrl(
                "https://github.com/estabantu5-star/note-acnes/releases/download/v1.0.5/SkinCare-Daily.apk"
            )
        )
        assertTrue(
            com.example.update.GitHubReleaseParser.isTrustedGitHubDownloadUrl(
                "https://objects.githubusercontent.com/github-production-release-asset-2e65be/12345"
            )
        )
        // Reject HTTP (non-HTTPS) and untrusted third-party domains
        assertFalse(
            com.example.update.GitHubReleaseParser.isTrustedGitHubDownloadUrl(
                "http://github.com/estabantu5-star/note-acnes/releases/download/v1.0.5/SkinCare-Daily.apk"
            )
        )
        assertFalse(
            com.example.update.GitHubReleaseParser.isTrustedGitHubDownloadUrl(
                "https://malicious-mirror.example.com/SkinCare-Daily.apk"
            )
        )
    }
}
