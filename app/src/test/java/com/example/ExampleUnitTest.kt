package com.example

import com.example.domain.RoutineScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ExampleUnitTest {

    @Test
    fun `verify Pharmalene schedule on Monday Wednesday and Friday`() {
        val mwf = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        for (day in mwf) {
            val scheduled = RoutineScheduleHelper.getScheduledEveningProductForDay(day)
            assertEquals(RoutineScheduleHelper.PHARMALENE_PRODUCT, scheduled.productName)
            assertTrue(scheduled.instruction.contains("Hindari sudut mata"))
        }
    }

    @Test
    fun `verify Labore serum schedule on Tuesday Thursday Saturday and Sunday`() {
        val ttss = listOf(
            DayOfWeek.TUESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
        for (day in ttss) {
            val scheduled = RoutineScheduleHelper.getScheduledEveningProductForDay(day)
            assertEquals(RoutineScheduleHelper.LABORE_SERUM_PRODUCT, scheduled.productName)
            assertTrue(scheduled.instruction.contains("tanpa Pharmalene"))
        }
    }

    @Test
    fun `verify initial dermatologist routine excludes old products and has stable IDs`() {
        val tasks = RoutineScheduleHelper.createInitialDermatologistTasks()
        assertEquals(8, tasks.size)

        val morningRequired = tasks.filter { it.period == "MORNING" && !it.isOptional }
        val eveningRequired = tasks.filter { it.period == "EVENING" && !it.isOptional }
        val eveningOptional = tasks.filter { it.period == "EVENING" && it.isOptional }

        assertEquals(4, morningRequired.size)
        assertEquals(3, eveningRequired.size)
        assertEquals(1, eveningOptional.size)
        assertTrue(eveningOptional.first().productName.contains("Effaclar Duo+M"))

        // Ensure old micellar water, clay mask, toner, and old serum are excluded
        val combinedText = tasks.joinToString(" ") { "${it.title} ${it.productName}" }.lowercase()
        assertFalse(combinedText.contains("micellar"))
        assertFalse(combinedText.contains("clay mask"))
        assertFalse(combinedText.contains("toner"))
    }

    @Test
    fun `verify next daily alarm schedules for today if future and tomorrow if already completed`() {
        val zone = ZoneId.of("Asia/Jakarta")
        val nowZdt = ZonedDateTime.of(2026, 10, 19, 6, 30, 0, 0, zone)
        val nowMillis = nowZdt.toInstant().toEpochMilli()

        // 07:00 is in the future today, not completed -> schedules for today 07:00
        val triggerToday = RoutineScheduleHelper.computeNextDailyTriggerMillis(
            hour = 7,
            minute = 0,
            nowMillis = nowMillis,
            zoneId = zone,
            isTodayAlreadyCompleted = false
        )
        val expectedToday = ZonedDateTime.of(2026, 10, 19, 7, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expectedToday, triggerToday)

        // If morning routine is already completed at 06:30 -> schedules for tomorrow 07:00
        val triggerTomorrow = RoutineScheduleHelper.computeNextDailyTriggerMillis(
            hour = 7,
            minute = 0,
            nowMillis = nowMillis,
            zoneId = zone,
            isTodayAlreadyCompleted = true
        )
        val expectedTomorrow = ZonedDateTime.of(2026, 10, 20, 7, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expectedTomorrow, triggerTomorrow)
    }

    @Test
    fun `verify medication safety gating and course boundary without auto extension`() {
        val startDate = LocalDate.of(2026, 10, 19)
        val startIso = RoutineScheduleHelper.toIsoString(startDate)

        // Unconfirmed discrepancy must ALWAYS return false even if courseEnabled = true
        assertFalse(
            RoutineScheduleHelper.isMedicationCourseActiveOnDate(
                isConfirmed = false,
                courseEnabled = true,
                startDateIso = startIso,
                durationDays = 14,
                targetDate = startDate
            )
        )

        // Confirmed 5-day course (10 capsules): active on day 1..5, inactive on day 6
        assertTrue(
            RoutineScheduleHelper.isMedicationCourseActiveOnDate(
                isConfirmed = true,
                courseEnabled = true,
                startDateIso = startIso,
                durationDays = 5,
                targetDate = LocalDate.of(2026, 10, 23) // Day 5
            )
        )
        assertFalse(
            RoutineScheduleHelper.isMedicationCourseActiveOnDate(
                isConfirmed = true,
                courseEnabled = true,
                startDateIso = startIso,
                durationDays = 5,
                targetDate = LocalDate.of(2026, 10, 24) // Day 6 -> Course finished
            )
        )
    }
}
