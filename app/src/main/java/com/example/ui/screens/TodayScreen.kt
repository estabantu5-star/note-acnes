package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ProductCategory
import com.example.domain.RoutineScheduleHelper
import com.example.ui.SkinCareUiState
import com.example.ui.TaskItemUiModel
import com.example.ui.components.ProductCategoryBadge
import com.example.ui.components.TaskDetailDialog
import java.time.Instant
import java.time.ZoneId

@Composable
fun TodayScreen(
    uiState: SkinCareUiState,
    onToggleTask: (TaskItemUiModel, Boolean) -> Unit,
    onStartStepTimer: (String) -> Unit,
    onResetTodayChecklist: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    onPauseOrResumeSunscreen: (Boolean) -> Unit,
    onStopSunscreen: () -> Unit,
    onLogMedicationDose: (slot: String, status: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTaskForDetail by remember { mutableStateOf<TaskItemUiModel?>(null) }
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header: App Title, Indonesian Date, & Notification Status Pill
        item(key = "header") {
            TodayHeaderSection(
                formattedDate = uiState.todayFormattedIndonesian,
                hasNotificationPermission = uiState.hasNotificationPermission,
                remindersEnabled = uiState.settings.morningReminderEnabled || uiState.settings.eveningReminderEnabled,
                onClickNotificationStatus = onOpenReminderSettings
            )
        }

        // 2. Daily Progress & Scheduled Evening Active Product Card
        item(key = "progress_summary") {
            TodayProgressAndScheduleCard(
                uiState = uiState,
                onOpenReminderSettings = onOpenReminderSettings,
                onStartThreeMinuteTimer = { onStartStepTimer("Jeda Antar Langkah (~3 menit)") }
            )
        }

        // 3. Rule 6 & Rule 9: Active Conflict Warnings & Chemical Peeling Safety Banner
        item(key = "therapy_safety_and_conflicts") {
            TherapySafetyAndConflictCard(
                conflictWarnings = uiState.routineConflictWarnings
            )
        }

        // 4. Optional Active Sunscreen Reapplication Banner (20 min before outdoor + every 2–3h)
        if (uiState.settings.sunscreenReminderEnabled) {
            item(key = "sunscreen_active_banner") {
                val sched = uiState.sunscreenScheduleToday
                val preOutdoorHm = RoutineScheduleHelper.formatTimeHm(
                    sched.applyBeforeOutdoorTime.hour,
                    sched.applyBeforeOutdoorTime.minute
                )
                SunscreenQuickStatusCard(
                    preOutdoorTimeText = preOutdoorHm,
                    intervalHours = uiState.settings.sunscreenIntervalHours,
                    isPaused = uiState.settings.sunscreenPaused,
                    onTogglePause = { onPauseOrResumeSunscreen(!uiState.settings.sunscreenPaused) },
                    onStop = onStopSunscreen
                )
            }
        }

        // 5. Rule 1: MORNING SECTION (Rutinitas Pagi)
        item(key = "morning_header") {
            RoutineSectionHeader(
                title = "Rutinitas Pagi",
                subtitle = "1. Pembersih Labore AcnePro • 2. Moisturizer • 3. Bioderma Sébium Kerato+ • 4. Sunscreen Labore (${uiState.morningCompletedRequired}/${uiState.morningTotalRequired})",
                isMorning = true,
                isAllDone = uiState.morningTotalRequired > 0 && uiState.morningCompletedRequired == uiState.morningTotalRequired
            )
        }

        items(
            items = uiState.morningTasks,
            key = { "morning_${it.task.id}" }
        ) { taskModel ->
            RoutineTaskCard(
                item = taskModel,
                onToggleChecked = { checked -> onToggleTask(taskModel, checked) },
                onOpenDetail = { selectedTaskForDetail = taskModel },
                onStartTimer = { onStartStepTimer("Jeda setelah ${taskModel.task.title}") }
            )
        }

        // 6. Rule 1 & Rule 2: EVENING SECTION (Rutinitas Malam — Berdasarkan Hari Tertentu)
        item(key = "evening_header") {
            Spacer(modifier = Modifier.height(4.dp))
            RoutineSectionHeader(
                title = "Rutinitas Malam",
                subtitle = "Jadwal ${uiState.scheduledEveningProductToday.dayNameIndonesian}: ${uiState.scheduledEveningProductToday.productName} (${uiState.eveningCompletedRequired}/${uiState.eveningTotalRequired})",
                isMorning = false,
                isAllDone = uiState.eveningTotalRequired > 0 && uiState.eveningCompletedRequired == uiState.eveningTotalRequired
            )
        }

        items(
            items = uiState.eveningTasks,
            key = { "evening_${it.task.id}" }
        ) { taskModel ->
            RoutineTaskCard(
                item = taskModel,
                onToggleChecked = { checked -> onToggleTask(taskModel, checked) },
                onOpenDetail = { selectedTaskForDetail = taskModel },
                onStartTimer = { onStartStepTimer("Jeda setelah ${taskModel.task.title}") }
            )
        }

        // 7. Rule 1: ORAL MEDICATION SECTION (Obat Minum: Doxihat 100 mg — Terpisah dari Rutinitas Oles)
        item(key = "medication_section") {
            Spacer(modifier = Modifier.height(4.dp))
            MedicationTodaySectionCard(
                uiState = uiState,
                onLogDose = onLogMedicationDose,
                onOpenMedicationSettings = onOpenReminderSettings
            )
        }

        // 8. Rule 7: Two-Week Doctor Follow-Up Control Reminder Card (Pengingat Kontrol 2 Minggu)
        item(key = "doctor_control_section") {
            DoctorControlFollowUpCard(
                uiState = uiState,
                onOpenReminderSettings = onOpenReminderSettings
            )
        }

        // 9. Reset Today's Checklist Action (Protected by confirmation dialog)
        item(key = "reset_footer") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data tersimpan otomatis di perangkat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showResetConfirmation = true },
                    enabled = uiState.totalCompletedRequiredToday > 0 || uiState.optionalCompletedToday > 0,
                    modifier = Modifier.testTag("reset_today_checklist_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Hari Ini")
                }
            }
        }
    }

    selectedTaskForDetail?.let { detailItem ->
        TaskDetailDialog(
            item = detailItem,
            onDismiss = { selectedTaskForDetail = null },
            onStartThreeMinuteTimer = onStartStepTimer,
            onToggleComplete = { checked -> onToggleTask(detailItem, checked) }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset Checklist Hari Ini?") },
            text = {
                Text(
                    "Seluruh centang tugas untuk hari ini (${uiState.todayFormattedIndonesian}) akan dikosongkan kembali. Catatan riwayat dari hari-hari sebelumnya TIDAK akan dihapus."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        onResetTodayChecklist()
                    },
                    modifier = Modifier.testTag("confirm_reset_today_button")
                ) {
                    Text("Ya, Reset Hari Ini")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmation = false },
                    modifier = Modifier.testTag("cancel_reset_today_button")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun TodayHeaderSection(
    formattedDate: String,
    hasNotificationPermission: Boolean,
    remindersEnabled: Boolean,
    onClickNotificationStatus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SkinCare Daily",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("home_app_title")
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("home_today_date_text")
            )
        }

        val statusActive = hasNotificationPermission && remindersEnabled
        Surface(
            onClick = onClickNotificationStatus,
            shape = MaterialTheme.shapes.large,
            color = if (statusActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.testTag("notification_status_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (statusActive) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                    contentDescription = "Status Notifikasi",
                    tint = if (statusActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        !hasNotificationPermission -> "Izin Mati"
                        remindersEnabled -> "Pengingat Aktif"
                        else -> "Pengingat Nonaktif"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (statusActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayProgressAndScheduleCard(
    uiState: SkinCareUiState,
    onOpenReminderSettings: () -> Unit,
    onStartThreeMinuteTimer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_progress_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Progres Rutinitas Hari Ini",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.totalCompletedRequiredToday} dari ${uiState.totalRequiredToday} langkah wajib selesai" +
                            if (uiState.optionalCompletedToday > 0) " (+${uiState.optionalCompletedToday} opsional)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("today_completed_count_text")
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "${uiState.todayCompletionPercentage}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("today_completion_percentage_text")
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (uiState.todayCompletionPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("today_progress_bar"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Current Day's Scheduled Evening Product (Rule 2, 4, 8)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Jadwal Malam Ini (${uiState.scheduledEveningProductToday.dayNameIndonesian}):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        ProductCategoryBadge(category = uiState.scheduledEveningProductToday.category)
                    }
                    Text(
                        text = uiState.scheduledEveningProductToday.productName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("scheduled_evening_product_text")
                    )
                    Text(
                        text = "Takaran: ${uiState.scheduledEveningProductToday.usageAmount} • Area: ${uiState.scheduledEveningProductToday.applicationArea}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.scheduledEveningProductToday.instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Next Upcoming Reminder & 3-Minute Interval Timer Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenReminderSettings)
                ) {
                    Text(
                        text = "Pengingat Berikutnya",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val nextRem = uiState.nextReminder
                    Text(
                        text = if (nextRem != null) {
                            "${nextRem.label} • ${nextRem.formattedTime}"
                        } else {
                            "Tidak ada pengingat aktif"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("next_upcoming_reminder_text")
                    )
                }

                FilledTonalButton(
                    onClick = onStartThreeMinuteTimer,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("start_three_minute_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Jeda 3 Menit",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TherapySafetyAndConflictCard(
    conflictWarnings: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("therapy_safety_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Catatan Keamanan Terapi Resep Dokter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = RoutineScheduleHelper.CHEMICAL_PEELING_WARNING,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("chemical_peeling_warning_text")
            )
            conflictWarnings.forEach { warning ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SunscreenQuickStatusCard(
    preOutdoorTimeText: String,
    intervalHours: Int,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sunscreen_status_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPaused) "Pengingat Sunscreen (Dijeda)" else "Pengingat Sunscreen Aktif",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Oles 20 mnt sebelum keluar ($preOutdoorTimeText) & oles ulang tiap $intervalHours jam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = onTogglePause,
                    modifier = Modifier.testTag("sunscreen_pause_resume_button")
                ) {
                    Text(if (isPaused) "Lanjutkan" else "Jeda")
                }
                TextButton(
                    onClick = onStop,
                    modifier = Modifier.testTag("sunscreen_stop_button")
                ) {
                    Text("Hentikan")
                }
            }
        }
    }
}

@Composable
private fun RoutineSectionHeader(
    title: String,
    subtitle: String,
    isMorning: Boolean,
    isAllDone: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = if (isMorning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isMorning) Icons.Outlined.WbSunny else Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = if (isMorning) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isAllDone) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Selesai",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lengkap",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun RoutineTaskCard(
    item: TaskItemUiModel,
    onToggleChecked: (Boolean) -> Unit,
    onOpenDetail: () -> Unit,
    onStartTimer: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (item.isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "taskCardBg"
    )

    val borderColor = if (item.isCompleted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleChecked(!item.isCompleted) }
            .testTag("task_card_${item.task.id}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = onToggleChecked,
                modifier = Modifier.testTag("checkbox_${item.task.id}")
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProductCategoryBadge(category = item.resolvedCategory)
                        if (item.task.isOptional) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "Opsional",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.resolvedProductName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.resolvedInstruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Rule 4 & 5: Display amount, area, and "Perlu konfirmasi ke dokter" when unspecified
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Jumlah Pemakaian: ${item.resolvedUsageAmount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Area Aplikasi: ${item.resolvedApplicationArea}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.requiresDoctorConfirmation) {
                            Text(
                                text = "Status: ${RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item.scheduleBadge?.let { badge ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "~${item.task.estimatedMinutes} mnt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item.completedAtMillis?.let { millis ->
                            val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                            val timeStr = RoutineScheduleHelper.formatTimeHm(zdt.hour, zdt.minute)
                            Text(
                                text = "• Selesai $timeStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onStartTimer,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("timer_btn_${item.task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "Mulai timer jeda 3 menit",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenDetail,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("detail_btn_${item.task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Detail langkah",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationTodaySectionCard(
    uiState: SkinCareUiState,
    onLogDose: (slot: String, status: String) -> Unit,
    onOpenMedicationSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medication_today_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Obat Minum: ${RoutineScheduleHelper.ORAL_MEDICATION_NAME}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            ProductCategoryBadge(category = ProductCategory.PRESCRIPTION_ORAL)
                        }
                        Text(
                            text = RoutineScheduleHelper.ORAL_MEDICATION_INSTRUCTION,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onOpenMedicationSettings,
                    modifier = Modifier.testTag("open_medication_settings_button")
                ) {
                    Text("Atur")
                }
            }

            if (!uiState.settings.medicationDiscrepancyConfirmed) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = RoutineScheduleHelper.ORAL_MEDICATION_DISCREPANCY_WARNING,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else {
                uiState.medicationDayNumber?.let { dayNum ->
                    Text(
                        text = "Hari ke-$dayNum dari ${uiState.settings.medicationDurationDays} hari (Durasi resep: 2 minggu / 14 hari)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } ?: Text(
                    text = "Durasi terapi 2 minggu (${uiState.settings.medicationDurationDays} hari) telah selesai — tidak berulang otomatis.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                MedicationDoseRow(
                    slotLabel = "Setelah Makan Pagi",
                    currentStatus = uiState.todayBreakfastMedLog?.status,
                    onMarkTaken = { onLogDose("BREAKFAST", "TAKEN") },
                    onMarkMissed = { onLogDose("BREAKFAST", "MISSED") },
                    tagPrefix = "med_breakfast"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                MedicationDoseRow(
                    slotLabel = "Setelah Makan Malam",
                    currentStatus = uiState.todayDinnerMedLog?.status,
                    onMarkTaken = { onLogDose("DINNER", "TAKEN") },
                    onMarkMissed = { onLogDose("DINNER", "MISSED") },
                    tagPrefix = "med_dinner"
                )
            }
        }
    }
}

@Composable
private fun DoctorControlFollowUpCard(
    uiState: SkinCareUiState,
    onOpenReminderSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doctor_control_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Pengingat Kontrol Dokter (2 Minggu)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Jadwal kontrol: ${uiState.doctorControlDateFormatted}" +
                            (uiState.doctorControlDaysRemaining?.let { days ->
                                when {
                                    days > 0 -> " ($days hari lagi)"
                                    days == 0 -> " (Hari ini)"
                                    else -> " (Sudah lewat ${-days} hari)"
                                }
                            } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("doctor_control_date_text")
                    )
                    Text(
                        text = RoutineScheduleHelper.DOCTOR_CONTROL_NOTE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = onOpenReminderSettings,
                modifier = Modifier.testTag("open_doctor_control_settings_button")
            ) {
                Text("Atur")
            }
        }
    }
}

@Composable
private fun MedicationDoseRow(
    slotLabel: String,
    currentStatus: String?,
    onMarkTaken: () -> Unit,
    onMarkMissed: () -> Unit,
    tagPrefix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slotLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (currentStatus) {
                    "TAKEN" -> "Status: Sudah diminum"
                    "MISSED" -> "Status: Belum/terlewat"
                    else -> "Status: Belum dicatat"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (currentStatus) {
                    "TAKEN" -> MaterialTheme.colorScheme.primary
                    "MISSED" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag("${tagPrefix}_status_text")
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(
                onClick = onMarkTaken,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("${tagPrefix}_taken_button")
            ) {
                Text("Sudah diminum", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onMarkMissed,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("${tagPrefix}_missed_button")
            ) {
                Text("Belum/terlewat", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
