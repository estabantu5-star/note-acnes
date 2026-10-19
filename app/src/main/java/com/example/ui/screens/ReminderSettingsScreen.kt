package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.RoutineScheduleHelper
import com.example.ui.SkinCareUiState
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidPrimaryActionButton
import com.example.ui.components.LiquidSecondaryGlassButton
import com.example.ui.components.TimePickerModalDialog
import com.example.ui.theme.LiquidGlassTokens

private enum class ActiveTimePickerTarget {
    NONE,
    MORNING_MAIN,
    EVENING_MAIN,
    SUNSCREEN_START,
    MED_BREAKFAST,
    MED_DINNER,
    DOCTOR_CONTROL
}

@Composable
fun ReminderSettingsScreen(
    uiState: SkinCareUiState,
    onRequestNotificationPermission: () -> Unit,
    onUpdateMorningReminder: (Boolean, Int, Int) -> Unit,
    onUpdateEveningReminder: (Boolean, Int, Int) -> Unit,
    onUpdateFollowUpAndSnooze: (Boolean, Int, Int, Boolean) -> Unit,
    onToggleStepReminder: (taskId: String, enabled: Boolean, offsetMinutes: Int) -> Unit,
    onConfigureSunscreenReminder: (enabled: Boolean, startHour: Int, startMinute: Int, endHour: Int, intervalHours: Int, paused: Boolean) -> Unit,
    onConfirmAndSaveMedication: (confirmed: Boolean, reminderEnabled: Boolean, startDateIso: String, durationDays: Int, totalCapsules: Int, breakfastHour: Int, breakfastMinute: Int, dinnerHour: Int, dinnerMinute: Int) -> Unit,
    onUpdateDoctorControlReminder: (enabled: Boolean, therapyStartDateIso: String, controlDays: Int, hour: Int, minute: Int) -> Unit,
    onTriggerTestNotification: () -> Unit,
    onRescheduleAllReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LiquidGlassTokens.colors
    val settings = uiState.settings
    var activePicker by rememberSaveable { mutableStateOf(ActiveTimePickerTarget.NONE) }

    // Local editable state for Medication safety confirmation form (Doxihat 100 mg)
    var medConfirmedChecked by rememberSaveable(settings.medicationDiscrepancyConfirmed) {
        mutableStateOf(settings.medicationDiscrepancyConfirmed)
    }
    var medReminderSwitch by rememberSaveable(settings.medicationReminderEnabled) {
        mutableStateOf(settings.medicationReminderEnabled)
    }
    var medStartDateInput by rememberSaveable(settings.medicationStartDateIso, uiState.todayDateIso) {
        mutableStateOf(settings.medicationStartDateIso.ifBlank { uiState.todayDateIso })
    }
    var medDurationDaysInput by rememberSaveable(settings.medicationDurationDays) {
        mutableStateOf(settings.medicationDurationDays.toString())
    }
    var medTotalCapsulesInput by rememberSaveable(settings.medicationTotalCapsulesConfirmed) {
        mutableStateOf(settings.medicationTotalCapsulesConfirmed.toString())
    }
    var medBreakfastH by rememberSaveable(settings.medicationBreakfastHour) {
        mutableIntStateOf(settings.medicationBreakfastHour)
    }
    var medBreakfastM by rememberSaveable(settings.medicationBreakfastMinute) {
        mutableIntStateOf(settings.medicationBreakfastMinute)
    }
    var medDinnerH by rememberSaveable(settings.medicationDinnerHour) {
        mutableIntStateOf(settings.medicationDinnerHour)
    }
    var medDinnerM by rememberSaveable(settings.medicationDinnerMinute) {
        mutableIntStateOf(settings.medicationDinnerMinute)
    }

    // Local editable state for Two-Week Doctor Control Follow-up Reminder
    var controlEnabledSwitch by rememberSaveable(settings.doctorControlReminderEnabled) {
        mutableStateOf(settings.doctorControlReminderEnabled)
    }
    var therapyStartDateInput by rememberSaveable(settings.therapyStartDateIso, uiState.todayDateIso) {
        mutableStateOf(settings.therapyStartDateIso.ifBlank { uiState.todayDateIso })
    }
    var controlHour by rememberSaveable(settings.doctorControlHour) {
        mutableIntStateOf(settings.doctorControlHour)
    }
    var controlMinute by rememberSaveable(settings.doctorControlMinute) {
        mutableIntStateOf(settings.doctorControlMinute)
    }

    val liquidSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = glass.accentElectricCyan,
        uncheckedThumbColor = glass.textSecondary,
        uncheckedTrackColor = glass.glassSurfaceSecondary,
        uncheckedBorderColor = glass.glassBorderSubtle
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reminder_settings_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "title") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Pengaturan Pengingat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = glass.textPrimary
                )
                Text(
                    text = "Kelola alarm rutinitas pagi, malam, tabir surya, Doxihat 100 mg, dan kontrol 2 minggu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }

        // 1. Permission & System Status Glass Card
        item(key = "permission_status_card") {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permission_status_card"),
                cornerRadius = 26.dp,
                isElevated = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (uiState.hasNotificationPermission) {
                                            glass.successSoftBg
                                        } else {
                                            glass.dangerSoftBg
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.hasNotificationPermission) {
                                        Icons.Outlined.NotificationsActive
                                    } else {
                                        Icons.Outlined.NotificationsOff
                                    },
                                    contentDescription = null,
                                    tint = if (uiState.hasNotificationPermission) {
                                        glass.successMint
                                    } else {
                                        glass.dangerCoral
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Status Izin Notifikasi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = if (uiState.hasNotificationPermission) {
                                        "Aktif (Diizinkan oleh sistem Android)"
                                    } else {
                                        "Ditolak atau dibatasi — ketuk untuk mengaktifkan"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.hasNotificationPermission) {
                                        glass.successMint
                                    } else {
                                        glass.dangerCoral
                                    },
                                    modifier = Modifier.testTag("notif_permission_status_text")
                                )
                            }
                        }

                        if (uiState.hasNotificationPermission) {
                            LiquidGlassPill(
                                text = "Aktif",
                                containerColor = glass.successSoftBg,
                                contentColor = glass.successMint,
                                borderColor = glass.successMint.copy(alpha = 0.4f),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = glass.successMint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!uiState.hasNotificationPermission && !settings.notificationPermissionAskedOnce) {
                            LiquidPrimaryActionButton(
                                text = "Minta Izin Notifikasi",
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_notif_permission_button")
                            )
                        }

                        LiquidSecondaryGlassButton(
                            text = "Pengaturan Sistem",
                            onClick = {
                                val intent = Intent().apply {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    } else {
                                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            leadingIcon = Icons.Outlined.Settings,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_system_notif_settings_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidSecondaryGlassButton(
                            text = "Uji Notifikasi",
                            onClick = onTriggerTestNotification,
                            leadingIcon = Icons.Outlined.NotificationsActive,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_notification_button")
                        )

                        LiquidSecondaryGlassButton(
                            text = "Jadwalkan Ulang",
                            onClick = onRescheduleAllReminders,
                            leadingIcon = Icons.Outlined.Refresh,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reschedule_reminders_button")
                        )
                    }
                }
            }
        }

        // 2. Main Morning & Evening Routine Reminders + Weekly Schedule Panel
        item(key = "main_routine_reminders") {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                isElevated = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Pengingat Utama Harian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )

                    // Morning Routine Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(glass.accentSoftGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WbSunny,
                                    contentDescription = null,
                                    tint = glass.accentPrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rutinitas Pagi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "Pembersih, Moisturizer, Kerato+, Sunscreen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LiquidTimePillButton(
                            timeText = RoutineScheduleHelper.formatTimeHm(settings.morningHour, settings.morningMinute),
                            onClick = { activePicker = ActiveTimePickerTarget.MORNING_MAIN },
                            modifier = Modifier.testTag("morning_time_picker_button")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.morningReminderEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateMorningReminder(enabled, settings.morningHour, settings.morningMinute)
                            },
                            colors = liquidSwitchColors,
                            modifier = Modifier.testTag("morning_reminder_switch")
                        )
                    }

                    HorizontalDivider(color = glass.glassBorderSubtle)

                    // Evening Routine Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(glass.accentSoftGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bedtime,
                                    contentDescription = null,
                                    tint = glass.accentElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rutinitas Malam",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "Menyesuaikan hari: Pharmalene Gel / Serum Labore",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LiquidTimePillButton(
                            timeText = RoutineScheduleHelper.formatTimeHm(settings.eveningHour, settings.eveningMinute),
                            onClick = { activePicker = ActiveTimePickerTarget.EVENING_MAIN },
                            modifier = Modifier.testTag("evening_time_picker_button")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.eveningReminderEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateEveningReminder(enabled, settings.eveningHour, settings.eveningMinute)
                            },
                            colors = liquidSwitchColors,
                            modifier = Modifier.testTag("evening_reminder_switch")
                        )
                    }

                    // Weekly Evening Active Product Reference Panel (Mon–Sun capsules + details)
                    Surface(
                        color = glass.glassSurfaceSecondary,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, glass.glassBorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Jadwal Produk Aktif Malam (Resep Dokter)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = glass.accentPrimaryBlue
                            )

                            val days = listOf(
                                Triple(1, "Sen", "Pharmalene"),
                                Triple(2, "Sel", "Serum"),
                                Triple(3, "Rab", "Pharmalene"),
                                Triple(4, "Kam", "Serum"),
                                Triple(5, "Jum", "Pharmalene"),
                                Triple(6, "Sab", "Serum"),
                                Triple(7, "Min", "Serum")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                days.forEach { (isoDay, dayLabel, shortTag) ->
                                    val isPharmalene = isoDay in setOf(1, 3, 5)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isPharmalene) {
                                                    Brush.verticalGradient(
                                                        listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                                                    )
                                                } else {
                                                    Brush.verticalGradient(
                                                        listOf(glass.glassSurfaceElevated, glass.glassSurfacePrimary)
                                                    )
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isPharmalene) Color.White.copy(alpha = 0.45f) else glass.glassBorderSubtle,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = dayLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPharmalene) Color.White else glass.textPrimary
                                        )
                                        Text(
                                            text = shortTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPharmalene) Color.White.copy(alpha = 0.9f) else glass.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "• Senin, Rabu, Jumat: Pharmalene Gel 10 g (tipis sebanyak 1 biji jagung seluruh wajah)",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                            Text(
                                text = "• Selasa, Kamis, Sabtu, Minggu: Labore AcnePro Biome Intensive Acne Serum 7 ml",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Sunscreen Reapplication Reminder Glass Card (20 min before outdoor + every 2–3h)
        item(key = "sunscreen_reapplication_card") {
            val sched = uiState.sunscreenScheduleToday
            val preOutdoorHm = RoutineScheduleHelper.formatTimeHm(
                sched.applyBeforeOutdoorTime.hour,
                sched.applyBeforeOutdoorTime.minute
            )
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sunscreen_settings_card"),
                cornerRadius = 26.dp,
                isElevated = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(glass.accentSoftGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WbSunny,
                                    contentDescription = null,
                                    tint = glass.accentPrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pengingat Sunscreen (Labore Biomeprotect)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "Oles 20 menit sebelum keluar rumah & aplikasikan ulang setiap 2–3 jam sesuai catatan dokter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = settings.sunscreenReminderEnabled,
                            onCheckedChange = { enabled ->
                                onConfigureSunscreenReminder(
                                    enabled,
                                    settings.sunscreenStartHour,
                                    settings.sunscreenStartMinute,
                                    settings.sunscreenEndHour,
                                    settings.sunscreenIntervalHours,
                                    false
                                )
                            },
                            colors = liquidSwitchColors,
                            modifier = Modifier.testTag("sunscreen_enable_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Waktu keluar rumah:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "Pengingat oles 20 menit sebelumnya: pukul $preOutdoorHm",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentPrimaryBlue
                            )
                        }
                        LiquidTimePillButton(
                            timeText = RoutineScheduleHelper.formatTimeHm(settings.sunscreenStartHour, settings.sunscreenStartMinute),
                            onClick = { activePicker = ActiveTimePickerTarget.SUNSCREEN_START },
                            modifier = Modifier.testTag("sunscreen_start_time_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interval oles ulang:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiquidChoicePill(
                                label = "Setiap 2 Jam",
                                selected = settings.sunscreenIntervalHours == 2,
                                onClick = {
                                    onConfigureSunscreenReminder(
                                        settings.sunscreenReminderEnabled,
                                        settings.sunscreenStartHour,
                                        settings.sunscreenStartMinute,
                                        settings.sunscreenEndHour,
                                        2,
                                        settings.sunscreenPaused
                                    )
                                },
                                modifier = Modifier.testTag("sunscreen_interval_2h_chip")
                            )
                            LiquidChoicePill(
                                label = "Setiap 3 Jam",
                                selected = settings.sunscreenIntervalHours == 3,
                                onClick = {
                                    onConfigureSunscreenReminder(
                                        settings.sunscreenReminderEnabled,
                                        settings.sunscreenStartHour,
                                        settings.sunscreenStartMinute,
                                        settings.sunscreenEndHour,
                                        3,
                                        settings.sunscreenPaused
                                    )
                                },
                                modifier = Modifier.testTag("sunscreen_interval_3h_chip")
                            )
                        }
                    }

                    if (settings.sunscreenReminderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidSecondaryGlassButton(
                                text = if (settings.sunscreenPaused) "Lanjutkan Hari Ini" else "Jeda Sementara",
                                onClick = {
                                    onConfigureSunscreenReminder(
                                        true,
                                        settings.sunscreenStartHour,
                                        settings.sunscreenStartMinute,
                                        settings.sunscreenEndHour,
                                        settings.sunscreenIntervalHours,
                                        !settings.sunscreenPaused
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sunscreen_settings_pause_button")
                            )
                            LiquidSecondaryGlassButton(
                                text = "Hentikan",
                                onClick = {
                                    onConfigureSunscreenReminder(
                                        false,
                                        settings.sunscreenStartHour,
                                        settings.sunscreenStartMinute,
                                        settings.sunscreenEndHour,
                                        settings.sunscreenIntervalHours,
                                        false
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sunscreen_settings_stop_button")
                            )
                        }
                    }
                }
            }
        }

        // 4. Snooze, Follow-Up, Per-Step Reminders, and Exact Alarm Option
        item(key = "advanced_reminder_options") {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Opsi Tunda & Pengingat Lanjutan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )

                    Text(
                        text = "Durasi tombol \"Tunda\" pada notifikasi:",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 30).forEach { mins ->
                            LiquidChoicePill(
                                label = "$mins menit",
                                selected = settings.snoozeDurationMinutes == mins,
                                onClick = {
                                    onUpdateFollowUpAndSnooze(
                                        settings.followUpReminderEnabled,
                                        settings.followUpDelayMinutes,
                                        mins,
                                        settings.useExactAlarm
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("snooze_chip_${mins}m")
                            )
                        }
                    }

                    HorizontalDivider(color = glass.glassBorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengingat Susulan (+${settings.followUpDelayMinutes} menit)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "Mengingatkan satu kali lagi bila rutinitas belum dicentang",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                        Switch(
                            checked = settings.followUpReminderEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateFollowUpAndSnooze(
                                    enabled,
                                    settings.followUpDelayMinutes,
                                    settings.snoozeDurationMinutes,
                                    settings.useExactAlarm
                                )
                            },
                            colors = liquidSwitchColors,
                            modifier = Modifier.testTag("followup_reminder_switch")
                        )
                    }

                    HorizontalDivider(color = glass.glassBorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alarm Tepat Waktu (Opsional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = if (uiState.canScheduleExactAlarms) {
                                    "Akses alarm tepat waktu diizinkan. Catatan: sistem penghemat baterai Android tetap dapat menggeser waktu pengiriman beberapa saat."
                                } else {
                                    "Akses 'Alarms & reminders' belum diizinkan; menggunakan jadwal hemat baterai (pengiriman dapat sedikit tertunda)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
                                modifier = Modifier.testTag("exact_alarm_status_text")
                            )
                        }
                        Switch(
                            checked = settings.useExactAlarm,
                            onCheckedChange = { enabled ->
                                onUpdateFollowUpAndSnooze(
                                    settings.followUpReminderEnabled,
                                    settings.followUpDelayMinutes,
                                    settings.snoozeDurationMinutes,
                                    enabled
                                )
                            },
                            colors = liquidSwitchColors,
                            modifier = Modifier.testTag("exact_alarm_switch")
                        )
                    }

                    if (settings.useExactAlarm && !uiState.canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        LiquidSecondaryGlassButton(
                            text = "Buka Izin Alarms & Reminders Android",
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            leadingIcon = Icons.Outlined.Alarm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_exact_alarm_settings_button")
                        )
                    }

                    HorizontalDivider(color = glass.glassBorderSubtle)

                    Text(
                        text = "Pengingat Per Langkah (Opsional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Secara bawaan hanya 2 pengingat utama yang aktif agar tidak berlebihan. Anda dapat mengaktifkan pengingat khusus langkah tertentu di bawah ini:",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )

                    uiState.allTasksForEditor.filter { it.isActive }.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (task.period == "MORNING") "[Pagi]" else "[Malam]"} ${task.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "+${task.reminderOffsetMinutes.coerceAtLeast(3)} menit setelah pengingat utama",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                            Switch(
                                checked = task.reminderEnabled,
                                onCheckedChange = { enabled ->
                                    val offset = if (task.reminderOffsetMinutes <= 0) 3 else task.reminderOffsetMinutes
                                    onToggleStepReminder(task.id, enabled, offset)
                                },
                                colors = liquidSwitchColors,
                                modifier = Modifier.testTag("step_reminder_switch_${task.id}")
                            )
                        }
                    }
                }
            }
        }

        // 5. ORAL MEDICATION REMINDER SECTION (Doxihat 100 mg — 2x sehari selama 2 minggu, tidak berulang otomatis)
        item(key = "medication_safety_card") {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medication_safety_card"),
                cornerRadius = 26.dp,
                isElevated = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(glass.warningSoftBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MedicalServices,
                                contentDescription = null,
                                tint = glass.warningAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengingat Obat Minum: ${RoutineScheduleHelper.ORAL_MEDICATION_NAME}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "Setelah makan pagi dan malam, 2x sehari selama 2 minggu (14 hari) sesuai resep dokter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }

                    Surface(
                        color = glass.warningSoftBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, glass.warningAmber.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "Perhatian Resep",
                                tint = glass.warningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.warningAmber
                                )
                                Text(
                                    text = RoutineScheduleHelper.ORAL_MEDICATION_DISCREPANCY_WARNING,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textPrimary
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = medConfirmedChecked,
                            onCheckedChange = { checked ->
                                medConfirmedChecked = checked
                                if (!checked) {
                                    medReminderSwitch = false
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = glass.accentPrimaryBlue,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.testTag("medication_discrepancy_confirm_checkbox")
                        )
                        Text(
                            text = "Saya telah mengonfirmasi durasi 2 minggu (14 hari) & jumlah kapsul Doxihat 100 mg sesuai resep dokter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (medConfirmedChecked) {
                        OutlinedTextField(
                            value = medStartDateInput,
                            onValueChange = { medStartDateInput = it },
                            label = { Text("Tanggal Mulai Terapi (YYYY-MM-DD)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("med_start_date_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = medDurationDaysInput,
                                onValueChange = { medDurationDaysInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Durasi (Hari, Resep: 14)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("med_duration_days_input")
                            )
                            OutlinedTextField(
                                value = medTotalCapsulesInput,
                                onValueChange = { medTotalCapsulesInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Jumlah Kapsul") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("med_total_capsules_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Setelah Makan Pagi:",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                            LiquidTimePillButton(
                                timeText = RoutineScheduleHelper.formatTimeHm(medBreakfastH, medBreakfastM),
                                onClick = { activePicker = ActiveTimePickerTarget.MED_BREAKFAST },
                                modifier = Modifier.testTag("med_breakfast_time_button")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Setelah Makan Malam:",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                            LiquidTimePillButton(
                                timeText = RoutineScheduleHelper.formatTimeHm(medDinnerH, medDinnerM),
                                onClick = { activePicker = ActiveTimePickerTarget.MED_DINNER },
                                modifier = Modifier.testTag("med_dinner_time_button")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aktifkan Alarm Pengingat Doxihat 100 mg",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = glass.textPrimary
                            )
                            Switch(
                                checked = medReminderSwitch,
                                onCheckedChange = { medReminderSwitch = it },
                                colors = liquidSwitchColors,
                                modifier = Modifier.testTag("medication_reminder_enable_switch")
                            )
                        }
                    }

                    LiquidPrimaryActionButton(
                        text = "Simpan Pengaturan Obat Minum",
                        onClick = {
                            val parsedDays = medDurationDaysInput.toIntOrNull()?.coerceIn(1, 30)
                                ?: RoutineScheduleHelper.DEFAULT_MEDICATION_DURATION_DAYS
                            val parsedCaps = medTotalCapsulesInput.toIntOrNull()?.coerceIn(1, 60)
                                ?: RoutineScheduleHelper.DEFAULT_MEDICATION_TOTAL_CAPSULES
                            onConfirmAndSaveMedication(
                                medConfirmedChecked,
                                medConfirmedChecked && medReminderSwitch,
                                medStartDateInput.trim(),
                                parsedDays,
                                parsedCaps,
                                medBreakfastH,
                                medBreakfastM,
                                medDinnerH,
                                medDinnerM
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_medication_settings_button")
                    )
                }
            }
        }

        // 6. Rule 7: Two-Week Doctor Control Follow-up Reminder Card
        item(key = "doctor_control_reminder_card") {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doctor_control_settings_card"),
                cornerRadius = 26.dp,
                isElevated = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(glass.accentSoftGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = glass.accentPrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pengingat Kontrol Dokter (2 Minggu)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = RoutineScheduleHelper.DOCTOR_CONTROL_NOTE,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = controlEnabledSwitch,
                            onCheckedChange = { controlEnabledSwitch = it },
                            colors = liquidSwitchColors,
                            modifier = Modifier.testTag("doctor_control_enable_switch")
                        )
                    }

                    OutlinedTextField(
                        value = therapyStartDateInput,
                        onValueChange = { therapyStartDateInput = it },
                        label = { Text("Tanggal Mulai Terapi Resep (YYYY-MM-DD)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("therapy_start_date_input")
                    )

                    Text(
                        text = "Jadwal kontrol (+14 hari): ${uiState.doctorControlDateFormatted}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = glass.accentPrimaryBlue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jam Pengingat Kontrol:",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                        LiquidTimePillButton(
                            timeText = RoutineScheduleHelper.formatTimeHm(controlHour, controlMinute),
                            onClick = { activePicker = ActiveTimePickerTarget.DOCTOR_CONTROL },
                            modifier = Modifier.testTag("doctor_control_time_button")
                        )
                    }

                    LiquidPrimaryActionButton(
                        text = "Simpan Pengingat Kontrol 2 Minggu",
                        onClick = {
                            onUpdateDoctorControlReminder(
                                controlEnabledSwitch,
                                therapyStartDateInput.trim(),
                                RoutineScheduleHelper.DEFAULT_DOCTOR_CONTROL_DAYS,
                                controlHour,
                                controlMinute
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_doctor_control_button")
                    )
                }
            }
        }
    }

    when (activePicker) {
        ActiveTimePickerTarget.MORNING_MAIN -> {
            TimePickerModalDialog(
                title = "Pilih Waktu Pengingat Pagi",
                initialHour = settings.morningHour,
                initialMinute = settings.morningMinute,
                onConfirm = { h, m ->
                    activePicker = ActiveTimePickerTarget.NONE
                    onUpdateMorningReminder(settings.morningReminderEnabled, h, m)
                },
                onDismiss = { activePicker = ActiveTimePickerTarget.NONE }
            )
        }
        ActiveTimePickerTarget.EVENING_MAIN -> {
            TimePickerModalDialog(
                title = "Pilih Waktu Pengingat Malam",
                initialHour = settings.eveningHour,
                initialMinute = settings.eveningMinute,
                onConfirm = { h, m ->
                    activePicker = ActiveTimePickerTarget.NONE
                    onUpdateEveningReminder(settings.eveningReminderEnabled, h, m)
                },
                onDismiss = { activePicker = ActiveTimePickerTarget.NONE }
            )
        }
        ActiveTimePickerTarget.SUNSCREEN_START -> {
            TimePickerModalDialog(
                title = "Waktu Keluar Rumah (Sunscreen)",
                initialHour = settings.sunscreenStartHour,
                initialMinute = settings.sunscreenStartMinute,
                onConfirm = { h, m ->
                    activePicker = ActiveTimePickerTarget.NONE
                    onConfigureSunscreenReminder(
                        settings.sunscreenReminderEnabled,
                        h,
                        m,
                        settings.sunscreenEndHour,
                        settings.sunscreenIntervalHours,
                        settings.sunscreenPaused
                    )
                },
                onDismiss = { activePicker = ActiveTimePickerTarget.NONE }
            )
        }
        ActiveTimePickerTarget.MED_BREAKFAST -> {
            TimePickerModalDialog(
                title = "Waktu Obat Pagi (Setelah Makan Pagi)",
                initialHour = medBreakfastH,
                initialMinute = medBreakfastM,
                onConfirm = { h, m ->
                    medBreakfastH = h
                    medBreakfastM = m
                    activePicker = ActiveTimePickerTarget.NONE
                },
                onDismiss = { activePicker = ActiveTimePickerTarget.NONE }
            )
        }
        ActiveTimePickerTarget.MED_DINNER -> {
            TimePickerModalDialog(
                title = "Waktu Obat Malam (Setelah Makan Malam)",
                initialHour = medDinnerH,
                initialMinute = medDinnerM,
                onConfirm = { h, m ->
                    medDinnerH = h
                    medDinnerM = m
                    activePicker = ActiveTimePickerTarget.NONE
                },
                onDismiss = { activePicker = ActiveTimePickerTarget.NONE }
            )
        }
        ActiveTimePickerTarget.DOCTOR_CONTROL -> {
            TimePickerModalDialog(
                title = "Waktu Pengingat Kontrol 2 Minggu",
                initialHour = controlHour,
                initialMinute = controlMinute,
                onConfirm = { h, m ->
                    controlHour = h
                    controlMinute = m
                    activePicker = ActiveTimePickerTarget.NONE
                },
                onDismiss = { activePicker = ActiveTimePickerTarget.NONE }
            )
        }
        ActiveTimePickerTarget.NONE -> Unit
    }
}

@Composable
private fun LiquidTimePillButton(
    timeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LiquidGlassTokens.colors
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = glass.accentSoftGlow,
        shape = CircleShape,
        border = BorderStroke(1.dp, glass.accentElectricCyan.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Alarm,
                contentDescription = null,
                tint = glass.accentPrimaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = glass.accentPrimaryBlue
            )
        }
    }
}

@Composable
private fun LiquidChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LiquidGlassTokens.colors
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(glass.glassSurfaceSecondary, glass.glassSurfacePrimary)
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.45f) else glass.glassBorderSubtle,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else glass.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
