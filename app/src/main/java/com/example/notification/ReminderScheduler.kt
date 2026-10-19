package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.RoutineTaskEntity
import com.example.data.preferences.UserReminderAndAppSettings
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class NextReminderSummary(
    val label: String,
    val formattedTime: String,
    val triggerEpochMillis: Long,
    val isExactEligible: Boolean
)

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val REQ_MORNING_MAIN = 1001
        const val REQ_EVENING_MAIN = 1002
        const val REQ_MORNING_FOLLOWUP = 1003
        const val REQ_EVENING_FOLLOWUP = 1004
        const val REQ_SUNSCREEN = 1005
        const val REQ_MED_BREAKFAST = 1006
        const val REQ_MED_DINNER = 1007
        const val REQ_DOCTOR_CONTROL = 1008
        const val REQ_SUNSCREEN_PRE_OUTDOOR = 1009
        const val REQ_STEP_BASE = 2000
        const val REQ_SNOOZE_BASE = 3000
    }

    fun canScheduleExactAlarms(): Boolean {
        val am = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Synchronizes all scheduled alarms with the user's current persisted settings and task completion state.
     * Cancels disabled or outdated alarms before scheduling active ones so no duplicates ever exist.
     */
    fun syncAllReminders(
        settings: UserReminderAndAppSettings,
        activeTasks: List<RoutineTaskEntity>,
        isMorningCompletedToday: Boolean,
        isEveningCompletedToday: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        // 1. Morning Main Reminder & Follow-up
        if (settings.morningReminderEnabled) {
            val triggerMillis = RoutineScheduleHelper.computeNextDailyTriggerMillis(
                hour = settings.morningHour,
                minute = settings.morningMinute,
                nowMillis = nowMillis,
                zoneId = zoneId,
                isTodayAlreadyCompleted = isMorningCompletedToday
            )
            scheduleAlarm(
                requestCode = REQ_MORNING_MAIN,
                triggerAtMillis = triggerMillis,
                useExact = settings.useExactAlarm,
                intent = buildReminderIntent(
                    reminderType = NotificationHelper.TYPE_MORNING_ROUTINE,
                    targetPeriod = RoutinePeriod.MORNING.id,
                    notificationId = NotificationHelper.NOTIF_ID_MORNING,
                    snoozeMinutes = settings.snoozeDurationMinutes
                )
            )
            if (settings.followUpReminderEnabled) {
                scheduleAlarm(
                    requestCode = REQ_MORNING_FOLLOWUP,
                    triggerAtMillis = triggerMillis + settings.followUpDelayMinutes * 60_000L,
                    useExact = settings.useExactAlarm,
                    intent = buildReminderIntent(
                        reminderType = NotificationHelper.TYPE_MORNING_FOLLOWUP,
                        targetPeriod = RoutinePeriod.MORNING.id,
                        notificationId = NotificationHelper.NOTIF_ID_MORNING_FOLLOWUP,
                        snoozeMinutes = settings.snoozeDurationMinutes
                    )
                )
            } else {
                cancelAlarm(REQ_MORNING_FOLLOWUP, NotificationHelper.TYPE_MORNING_FOLLOWUP)
            }
        } else {
            cancelAlarm(REQ_MORNING_MAIN, NotificationHelper.TYPE_MORNING_ROUTINE)
            cancelAlarm(REQ_MORNING_FOLLOWUP, NotificationHelper.TYPE_MORNING_FOLLOWUP)
        }

        // 2. Evening Main Reminder & Follow-up
        if (settings.eveningReminderEnabled) {
            val triggerMillis = RoutineScheduleHelper.computeNextDailyTriggerMillis(
                hour = settings.eveningHour,
                minute = settings.eveningMinute,
                nowMillis = nowMillis,
                zoneId = zoneId,
                isTodayAlreadyCompleted = isEveningCompletedToday
            )
            scheduleAlarm(
                requestCode = REQ_EVENING_MAIN,
                triggerAtMillis = triggerMillis,
                useExact = settings.useExactAlarm,
                intent = buildReminderIntent(
                    reminderType = NotificationHelper.TYPE_EVENING_ROUTINE,
                    targetPeriod = RoutinePeriod.EVENING.id,
                    notificationId = NotificationHelper.NOTIF_ID_EVENING,
                    snoozeMinutes = settings.snoozeDurationMinutes
                )
            )
            if (settings.followUpReminderEnabled) {
                scheduleAlarm(
                    requestCode = REQ_EVENING_FOLLOWUP,
                    triggerAtMillis = triggerMillis + settings.followUpDelayMinutes * 60_000L,
                    useExact = settings.useExactAlarm,
                    intent = buildReminderIntent(
                        reminderType = NotificationHelper.TYPE_EVENING_FOLLOWUP,
                        targetPeriod = RoutinePeriod.EVENING.id,
                        notificationId = NotificationHelper.NOTIF_ID_EVENING_FOLLOWUP,
                        snoozeMinutes = settings.snoozeDurationMinutes
                    )
                )
            } else {
                cancelAlarm(REQ_EVENING_FOLLOWUP, NotificationHelper.TYPE_EVENING_FOLLOWUP)
            }
        } else {
            cancelAlarm(REQ_EVENING_MAIN, NotificationHelper.TYPE_EVENING_ROUTINE)
            cancelAlarm(REQ_EVENING_FOLLOWUP, NotificationHelper.TYPE_EVENING_FOLLOWUP)
        }

        // 3. Optional Per-Step Reminders
        for (idx in 0 until 25) {
            cancelAlarm(REQ_STEP_BASE + idx, NotificationHelper.TYPE_STEP_REMINDER)
        }
        activeTasks.forEachIndexed { index, task ->
            if (task.reminderEnabled && index < 25) {
                val isMorning = task.period == RoutinePeriod.MORNING.id
                val baseEnabled = if (isMorning) settings.morningReminderEnabled else settings.eveningReminderEnabled
                if (baseEnabled) {
                    val baseHour = if (isMorning) settings.morningHour else settings.eveningHour
                    val baseMinute = if (isMorning) settings.morningMinute else settings.eveningMinute
                    val baseDone = if (isMorning) isMorningCompletedToday else isEveningCompletedToday
                    val baseTrigger = RoutineScheduleHelper.computeNextDailyTriggerMillis(
                        hour = baseHour,
                        minute = baseMinute,
                        nowMillis = nowMillis,
                        zoneId = zoneId,
                        isTodayAlreadyCompleted = baseDone
                    )
                    val stepTrigger = baseTrigger + (task.reminderOffsetMinutes.coerceAtLeast(1) * 60_000L)
                    val reqCode = REQ_STEP_BASE + index
                    scheduleAlarm(
                        requestCode = reqCode,
                        triggerAtMillis = stepTrigger,
                        useExact = settings.useExactAlarm,
                        intent = buildReminderIntent(
                            reminderType = NotificationHelper.TYPE_STEP_REMINDER,
                            targetPeriod = task.period,
                            notificationId = reqCode,
                            snoozeMinutes = settings.snoozeDurationMinutes,
                            stepTaskId = task.id,
                            stepTitle = task.title,
                            stepProduct = task.productName
                        )
                    )
                }
            }
        }

        // 4. Sunscreen Pre-Outdoor (20 minutes before going outside) & Reapplication Reminder (every 2–3 hours)
        val todayDate = RoutineScheduleHelper.currentLocalDate(zoneId, nowMillis)
        val todayIso = RoutineScheduleHelper.toIsoString(todayDate)
        if (settings.sunscreenReminderEnabled && !settings.sunscreenPaused && settings.sunscreenActiveDateIso == todayIso) {
            val preOutdoorMillis = computePreOutdoorSunscreenTriggerMillis(settings, nowMillis, zoneId)
            if (settings.sunscreenPreOutdoorEnabled && preOutdoorMillis != null) {
                scheduleAlarm(
                    requestCode = REQ_SUNSCREEN_PRE_OUTDOOR,
                    triggerAtMillis = preOutdoorMillis,
                    useExact = settings.useExactAlarm,
                    intent = buildReminderIntent(
                        reminderType = NotificationHelper.TYPE_SUNSCREEN_PRE_OUTDOOR,
                        targetPeriod = RoutinePeriod.MORNING.id,
                        notificationId = NotificationHelper.NOTIF_ID_SUNSCREEN_PRE_OUTDOOR,
                        snoozeMinutes = settings.snoozeDurationMinutes
                    )
                )
            } else {
                cancelAlarm(REQ_SUNSCREEN_PRE_OUTDOOR, NotificationHelper.TYPE_SUNSCREEN_PRE_OUTDOOR)
            }

            val nextSunscreenMillis = computeNextSunscreenTriggerMillis(settings, nowMillis, zoneId)
            if (nextSunscreenMillis != null) {
                scheduleAlarm(
                    requestCode = REQ_SUNSCREEN,
                    triggerAtMillis = nextSunscreenMillis,
                    useExact = settings.useExactAlarm,
                    intent = buildReminderIntent(
                        reminderType = NotificationHelper.TYPE_SUNSCREEN_REAPPLY,
                        targetPeriod = RoutinePeriod.MORNING.id,
                        notificationId = NotificationHelper.NOTIF_ID_SUNSCREEN,
                        snoozeMinutes = settings.snoozeDurationMinutes
                    )
                )
            } else {
                cancelAlarm(REQ_SUNSCREEN, NotificationHelper.TYPE_SUNSCREEN_REAPPLY)
            }
        } else {
            cancelAlarm(REQ_SUNSCREEN_PRE_OUTDOOR, NotificationHelper.TYPE_SUNSCREEN_PRE_OUTDOOR)
            cancelAlarm(REQ_SUNSCREEN, NotificationHelper.TYPE_SUNSCREEN_REAPPLY)
        }

        // 5. Oral Medication Reminders (Doxihat 100 mg) — Strictly within 2-week (14-day) course, never auto-repeating after 2 weeks
        val breakfastTrigger = RoutineScheduleHelper.computeNextMedicationTriggerMillis(
            isConfirmed = settings.medicationDiscrepancyConfirmed,
            courseEnabled = settings.medicationReminderEnabled,
            startDateIso = settings.medicationStartDateIso,
            durationDays = settings.medicationDurationDays,
            hour = settings.medicationBreakfastHour,
            minute = settings.medicationBreakfastMinute,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        if (breakfastTrigger != null) {
            scheduleAlarm(
                requestCode = REQ_MED_BREAKFAST,
                triggerAtMillis = breakfastTrigger,
                useExact = settings.useExactAlarm,
                intent = buildReminderIntent(
                    reminderType = NotificationHelper.TYPE_MED_BREAKFAST,
                    targetPeriod = RoutinePeriod.MORNING.id,
                    notificationId = NotificationHelper.NOTIF_ID_MED_BREAKFAST,
                    snoozeMinutes = settings.snoozeDurationMinutes
                )
            )
        } else {
            cancelAlarm(REQ_MED_BREAKFAST, NotificationHelper.TYPE_MED_BREAKFAST)
        }

        val dinnerTrigger = RoutineScheduleHelper.computeNextMedicationTriggerMillis(
            isConfirmed = settings.medicationDiscrepancyConfirmed,
            courseEnabled = settings.medicationReminderEnabled,
            startDateIso = settings.medicationStartDateIso,
            durationDays = settings.medicationDurationDays,
            hour = settings.medicationDinnerHour,
            minute = settings.medicationDinnerMinute,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        if (dinnerTrigger != null) {
            scheduleAlarm(
                requestCode = REQ_MED_DINNER,
                triggerAtMillis = dinnerTrigger,
                useExact = settings.useExactAlarm,
                intent = buildReminderIntent(
                    reminderType = NotificationHelper.TYPE_MED_DINNER,
                    targetPeriod = RoutinePeriod.EVENING.id,
                    notificationId = NotificationHelper.NOTIF_ID_MED_DINNER,
                    snoozeMinutes = settings.snoozeDurationMinutes
                )
            )
        } else {
            cancelAlarm(REQ_MED_DINNER, NotificationHelper.TYPE_MED_DINNER)
        }

        // 6. Rule 7: Two-Week Doctor Control Follow-up Reminder
        if (settings.doctorControlReminderEnabled && settings.therapyStartDateIso.isNotBlank()) {
            val controlTrigger = RoutineScheduleHelper.computeDoctorControlTriggerMillis(
                startDateIso = settings.therapyStartDateIso,
                controlDays = settings.doctorControlDays,
                hour = settings.doctorControlHour,
                minute = settings.doctorControlMinute,
                nowMillis = nowMillis,
                zoneId = zoneId
            )
            if (controlTrigger != null) {
                scheduleAlarm(
                    requestCode = REQ_DOCTOR_CONTROL,
                    triggerAtMillis = controlTrigger,
                    useExact = settings.useExactAlarm,
                    intent = buildReminderIntent(
                        reminderType = NotificationHelper.TYPE_DOCTOR_CONTROL,
                        targetPeriod = RoutinePeriod.MORNING.id,
                        notificationId = NotificationHelper.NOTIF_ID_DOCTOR_CONTROL,
                        snoozeMinutes = settings.snoozeDurationMinutes
                    )
                )
            } else {
                cancelAlarm(REQ_DOCTOR_CONTROL, NotificationHelper.TYPE_DOCTOR_CONTROL)
            }
        } else {
            cancelAlarm(REQ_DOCTOR_CONTROL, NotificationHelper.TYPE_DOCTOR_CONTROL)
        }
    }

    /**
     * Schedules a single replacement snooze alarm without creating an unlimited chain of duplicates.
     */
    fun scheduleSnoozeAlarm(
        notificationId: Int,
        reminderType: String,
        targetPeriod: String,
        snoozeMinutes: Int,
        useExact: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val requestCode = REQ_SNOOZE_BASE + (notificationId % 100)
        val triggerAtMillis = nowMillis + snoozeMinutes.coerceIn(5, 60) * 60_000L
        val intent = buildReminderIntent(
            reminderType = reminderType,
            targetPeriod = targetPeriod,
            notificationId = notificationId,
            snoozeMinutes = snoozeMinutes
        )
        scheduleAlarm(
            requestCode = requestCode,
            triggerAtMillis = triggerAtMillis,
            useExact = useExact,
            intent = intent
        )
    }

    fun computePreOutdoorSunscreenTriggerMillis(
        settings: UserReminderAndAppSettings,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val today = nowZdt.toLocalDate()
        if (settings.sunscreenActiveDateIso != RoutineScheduleHelper.toIsoString(today)) {
            return null
        }
        val schedule = RoutineScheduleHelper.computeSunscreenScheduleForDay(
            startHour = settings.sunscreenStartHour,
            startMinute = settings.sunscreenStartMinute,
            endHour = settings.sunscreenEndHour,
            intervalHours = settings.sunscreenIntervalHours
        )
        val preOutdoorZdt = ZonedDateTime.of(today, schedule.applyBeforeOutdoorTime, zoneId)
        return if (preOutdoorZdt.isAfter(nowZdt)) {
            preOutdoorZdt.toInstant().toEpochMilli()
        } else {
            null
        }
    }

    fun computeNextSunscreenTriggerMillis(
        settings: UserReminderAndAppSettings,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val today = nowZdt.toLocalDate()
        if (settings.sunscreenActiveDateIso != RoutineScheduleHelper.toIsoString(today)) {
            return null
        }
        val schedule = RoutineScheduleHelper.computeSunscreenScheduleForDay(
            startHour = settings.sunscreenStartHour,
            startMinute = settings.sunscreenStartMinute,
            endHour = settings.sunscreenEndHour,
            intervalHours = settings.sunscreenIntervalHours
        )
        for (slotTime in schedule.reapplyTimes) {
            val candidateZdt = ZonedDateTime.of(today, slotTime, zoneId)
            if (candidateZdt.isAfter(nowZdt)) {
                return candidateZdt.toInstant().toEpochMilli()
            }
        }
        return null
    }

    fun getNextUpcomingReminderSummary(
        settings: UserReminderAndAppSettings,
        isMorningCompletedToday: Boolean,
        isEveningCompletedToday: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): NextReminderSummary? {
        val candidates = mutableListOf<Pair<String, Long>>()

        if (settings.morningReminderEnabled) {
            val trigger = RoutineScheduleHelper.computeNextDailyTriggerMillis(
                hour = settings.morningHour,
                minute = settings.morningMinute,
                nowMillis = nowMillis,
                zoneId = zoneId,
                isTodayAlreadyCompleted = isMorningCompletedToday
            )
            candidates.add("Rutinitas Pagi" to trigger)
        }

        if (settings.eveningReminderEnabled) {
            val trigger = RoutineScheduleHelper.computeNextDailyTriggerMillis(
                hour = settings.eveningHour,
                minute = settings.eveningMinute,
                nowMillis = nowMillis,
                zoneId = zoneId,
                isTodayAlreadyCompleted = isEveningCompletedToday
            )
            candidates.add("Rutinitas Malam" to trigger)
        }

        val todayIso = RoutineScheduleHelper.toIsoString(RoutineScheduleHelper.currentLocalDate(zoneId, nowMillis))
        if (settings.sunscreenReminderEnabled && !settings.sunscreenPaused && settings.sunscreenActiveDateIso == todayIso) {
            if (settings.sunscreenPreOutdoorEnabled) {
                computePreOutdoorSunscreenTriggerMillis(settings, nowMillis, zoneId)?.let { trigger ->
                    candidates.add("Sunscreen (20m sebelum keluar)" to trigger)
                }
            }
            computeNextSunscreenTriggerMillis(settings, nowMillis, zoneId)?.let { trigger ->
                candidates.add("Pengingat sunscreen" to trigger)
            }
        }

        val next = candidates.minByOrNull { it.second } ?: return null
        val triggerZdt = Instant.ofEpochMilli(next.second).atZone(zoneId)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val dayPrefix = if (triggerZdt.toLocalDate() == today) "Hari ini" else "Besok"
        val hm = RoutineScheduleHelper.formatTimeHm(triggerZdt.hour, triggerZdt.minute)

        return NextReminderSummary(
            label = next.first,
            formattedTime = "$dayPrefix, $hm",
            triggerEpochMillis = next.second,
            isExactEligible = settings.useExactAlarm && canScheduleExactAlarms()
        )
    }

    private fun buildReminderIntent(
        reminderType: String,
        targetPeriod: String,
        notificationId: Int,
        snoozeMinutes: Int,
        stepTaskId: String = "",
        stepTitle: String = "",
        stepProduct: String = ""
    ): Intent {
        return Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationHelper.ACTION_TRIGGER_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_TYPE, reminderType)
            putExtra(NotificationHelper.EXTRA_TARGET_PERIOD, targetPeriod)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            if (stepTaskId.isNotEmpty()) putExtra(NotificationHelper.EXTRA_STEP_TASK_ID, stepTaskId)
            if (stepTitle.isNotEmpty()) putExtra(NotificationHelper.EXTRA_STEP_TITLE, stepTitle)
            if (stepProduct.isNotEmpty()) putExtra(NotificationHelper.EXTRA_STEP_PRODUCT, stepProduct)
        }
    }

    private fun scheduleAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        useExact: Boolean,
        intent: Intent
    ) {
        val am = alarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            if (useExact && canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.onFailure {
            runCatching {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    fun cancelAlarm(requestCode: Int, reminderType: String) {
        val am = alarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationHelper.ACTION_TRIGGER_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_TYPE, reminderType)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            runCatching {
                am.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
