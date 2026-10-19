package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.RoutineScheduleHelper
import com.example.ui.SkinCareUiState
import com.example.ui.components.TimePickerModalDialog

private enum class ActiveTimePickerTarget {
    NONE,
    MORNING_MAIN,
    EVENING_MAIN,
    SUNSCREEN_START,
    MED_BREAKFAST,
    MED_DINNER
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
    onTriggerTestNotification: () -> Unit,
    onRescheduleAllReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = uiState.settings
    var activePicker by rememberSaveable { mutableStateOf(ActiveTimePickerTarget.NONE) }

    // Local editable state for Medication safety confirmation form
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reminder_settings_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "title") {
            Column {
                Text(
                    text = "Pengaturan Pengingat & Jadwal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Kelola alarm rutinitas pagi, malam, tabir surya, dan verifikasi resep obat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Permission & System Status Card
        item(key = "permission_status_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permission_status_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.hasNotificationPermission) {
                                    Icons.Outlined.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (uiState.hasNotificationPermission) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Status Izin Notifikasi",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (uiState.hasNotificationPermission) {
                                        "Aktif (Diizinkan oleh sistem Android)"
                                    } else {
                                        "Ditolak atau dibatasi — ketuk untuk mengaktifkan"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.hasNotificationPermission) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.testTag("notif_permission_status_text")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!uiState.hasNotificationPermission && !settings.notificationPermissionAskedOnce) {
                            Button(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_notif_permission_button")
                            ) {
                                Text("Minta Izin Notifikasi")
                            }
                        }

                        OutlinedButton(
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_system_notif_settings_button")
                        ) {
                            Text("Pengaturan Sistem")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onTriggerTestNotification,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_notification_button")
                        ) {
                            Text("Uji Notifikasi")
                        }

                        OutlinedButton(
                            onClick = onRescheduleAllReminders,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reschedule_reminders_button")
                        ) {
                            Text("Jadwalkan ulang pengingat")
                        }
                    }
                }
            }
        }

        // 2. Main Morning & Evening Routine Reminders
        item(key = "main_routine_reminders") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pengingat Utama Harian",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Morning Routine
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rutinitas Pagi",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Saran awal: 07:00 • Otomatis dilewati bila sudah selesai",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { activePicker = ActiveTimePickerTarget.MORNING_MAIN },
                            modifier = Modifier.testTag("morning_time_picker_button")
                        ) {
                            Text(RoutineScheduleHelper.formatTimeHm(settings.morningHour, settings.morningMinute))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.morningReminderEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateMorningReminder(enabled, settings.morningHour, settings.morningMinute)
                            },
                            modifier = Modifier.testTag("morning_reminder_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Evening Routine
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rutinitas Malam",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Saran awal: 21:00 • Menampilkan jadwal Pharmalene / Serum Labore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { activePicker = ActiveTimePickerTarget.EVENING_MAIN },
                            modifier = Modifier.testTag("evening_time_picker_button")
                        ) {
                            Text(RoutineScheduleHelper.formatTimeHm(settings.eveningHour, settings.eveningMinute))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.eveningReminderEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateEveningReminder(enabled, settings.eveningHour, settings.eveningMinute)
                            },
                            modifier = Modifier.testTag("evening_reminder_switch")
                        )
                    }

                    // Weekly Evening Active Product Reference Table
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Jadwal Produk Aktif Malam (Resep Dokter):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• Senin, Rabu, Jumat: Pharmalene Gel 10 g",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Selasa, Kamis, Sabtu, Minggu: Labore AcnePro Biome Intensive Acne Serum 7 ml",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // 3. Sunscreen Reapplication Reminder ("Pengingat sunscreen")
        item(key = "sunscreen_reapplication_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sunscreen_settings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WbSunny,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pengingat sunscreen (Oles Ulang)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Opsional saat beraktivitas di luar ruangan • Berakhir otomatis sore hari",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier.testTag("sunscreen_enable_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Waktu mulai keluar ruangan:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = { activePicker = ActiveTimePickerTarget.SUNSCREEN_START },
                            modifier = Modifier.testTag("sunscreen_start_time_button")
                        ) {
                            Text(RoutineScheduleHelper.formatTimeHm(settings.sunscreenStartHour, settings.sunscreenStartMinute))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interval pengulangan:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
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
                                label = { Text("Setiap 2 Jam") },
                                modifier = Modifier.testTag("sunscreen_interval_2h_chip")
                            )
                            FilterChip(
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
                                label = { Text("Setiap 3 Jam") },
                                modifier = Modifier.testTag("sunscreen_interval_3h_chip")
                            )
                        }
                    }

                    if (settings.sunscreenReminderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
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
                            ) {
                                Text(if (settings.sunscreenPaused) "Lanjutkan Hari Ini" else "Jeda Sementara")
                            }
                            OutlinedButton(
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
                            ) {
                                Text("Hentikan")
                            }
                        }
                    }
                }
            }
        }

        // 4. Snooze, Follow-Up, Per-Step Reminders, and Exact Alarm Option
        item(key = "advanced_reminder_options") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Opsi Tunda, Pengingat Lanjutan & Langkah",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Configurable Snooze Duration
                    Text(
                        text = "Durasi tombol \"Tunda\" pada notifikasi:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = settings.snoozeDurationMinutes == mins,
                                onClick = {
                                    onUpdateFollowUpAndSnooze(
                                        settings.followUpReminderEnabled,
                                        settings.followUpDelayMinutes,
                                        mins,
                                        settings.useExactAlarm
                                    )
                                },
                                label = { Text("$mins menit") },
                                modifier = Modifier.testTag("snooze_chip_${mins}m")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Follow-up reminder a few minutes after initial reminder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengingat Susulan (+${settings.followUpDelayMinutes} menit)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Mengingatkan satu kali lagi bila rutinitas belum dicentang",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier.testTag("followup_reminder_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Exact Alarm Option ("Alarm tepat waktu")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alarm tepat waktu (Opsional)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (uiState.canScheduleExactAlarms) {
                                    "Akses alarm tepat waktu diizinkan. Catatan: sistem penghemat baterai Android tetap dapat menggeser waktu pengiriman beberapa saat."
                                } else {
                                    "Akses 'Alarms & reminders' belum diizinkan; menggunakan jadwal hemat baterai (pengiriman dapat sedikit tertunda)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            modifier = Modifier.testTag("exact_alarm_switch")
                        )
                    }

                    if (settings.useExactAlarm && !uiState.canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_exact_alarm_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Izin Alarms & Reminders Android")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Optional Per-Step Reminders
                    Text(
                        text = "Pengingat Per Langkah (Opsional)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Secara bawaan hanya 2 pengingat utama yang aktif agar tidak berlebihan. Anda dapat mengaktifkan pengingat khusus langkah tertentu di bawah ini:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "+${task.reminderOffsetMinutes.coerceAtLeast(3)} menit setelah pengingat utama",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = task.reminderEnabled,
                                onCheckedChange = { enabled ->
                                    val offset = if (task.reminderOffsetMinutes <= 0) 3 else task.reminderOffsetMinutes
                                    onToggleStepReminder(task.id, enabled, offset)
                                },
                                modifier = Modifier.testTag("step_reminder_switch_${task.id}")
                            )
                        }
                    }
                }
            }
        }

        // 5. MEDICATION REMINDER SAFETY SECTION (Dohixat 100 mg)
        item(key = "medication_safety_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medication_safety_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Keamanan Pengingat Obat: Dohixat 100 mg",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Sehabis sarapan & makan malam (2x sehari)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mandatory Discrepancy Notice
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "Perhatian Resep",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Perbedaan Keterangan Resep Perlu Dikonfirmasi",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Catatan resep menyebutkan konsumsi 2 kali sehari selama 2 minggu (membutuhkan 28 kapsul/dosis), namun jumlah yang tertera adalah 10 kapsul (cukup untuk 5 hari).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Demi keamanan penggunaan antibiotik, harap konfirmasi jumlah kapsul dan durasi yang dimaksud kepada dokter kulit atau apoteker sebelum mengaktifkan pengingat obat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
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
                            modifier = Modifier.testTag("medication_discrepancy_confirm_checkbox")
                        )
                        Text(
                            text = "Saya telah mengonfirmasi durasi & jumlah kapsul Dohixat 100 mg dengan dokter kulit / apoteker.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (medConfirmedChecked) {
                        OutlinedTextField(
                            value = medStartDateInput,
                            onValueChange = { medStartDateInput = it },
                            label = { Text("Tanggal Mulai (YYYY-MM-DD)") },
                            singleLine = true,
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
                                label = { Text("Durasi (Hari)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("med_duration_days_input")
                            )
                            OutlinedTextField(
                                value = medTotalCapsulesInput,
                                onValueChange = { medTotalCapsulesInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Jumlah Kapsul") },
                                singleLine = true,
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
                            Text("Pengingat Pagi (Sehabis Sarapan):", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = { activePicker = ActiveTimePickerTarget.MED_BREAKFAST },
                                modifier = Modifier.testTag("med_breakfast_time_button")
                            ) {
                                Text(RoutineScheduleHelper.formatTimeHm(medBreakfastH, medBreakfastM))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pengingat Malam (Sehabis Makan Malam):", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = { activePicker = ActiveTimePickerTarget.MED_DINNER },
                                modifier = Modifier.testTag("med_dinner_time_button")
                            ) {
                                Text(RoutineScheduleHelper.formatTimeHm(medDinnerH, medDinnerM))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aktifkan Pengingat Dohixat 100 mg",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Switch(
                                checked = medReminderSwitch,
                                onCheckedChange = { medReminderSwitch = it },
                                modifier = Modifier.testTag("medication_reminder_enable_switch")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val parsedDays = medDurationDaysInput.toIntOrNull()?.coerceIn(1, 30) ?: 5
                            val parsedCaps = medTotalCapsulesInput.toIntOrNull()?.coerceIn(1, 60) ?: 10
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
                    ) {
                        Text("Simpan Pengaturan Obat")
                    }
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
                title = "Waktu Mulai Aktivitas Luar Ruangan",
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
                title = "Waktu Obat Pagi (Sehabis Sarapan)",
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
                title = "Waktu Obat Malam (Sehabis Makan Malam)",
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
        ActiveTimePickerTarget.NONE -> Unit
    }
}
