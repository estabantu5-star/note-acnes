package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ProductCategory
import com.example.domain.RoutineScheduleHelper
import com.example.ui.SkinCareUiState
import com.example.ui.TaskItemUiModel
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidPrimaryActionButton
import com.example.ui.components.LiquidProgressRing
import com.example.ui.components.LiquidSecondaryGlassButton
import com.example.ui.components.ProductBottleGlyph
import com.example.ui.components.TaskDetailDialog
import com.example.ui.theme.LiquidGlassTokens

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
    val glass = LiquidGlassTokens.colors
    var selectedTaskIdForDetail by rememberSaveable { mutableStateOf<String?>(null) }
    var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val detailItem: TaskItemUiModel? = selectedTaskIdForDetail?.let { id ->
        (uiState.morningTasks + uiState.eveningTasks).firstOrNull { it.task.id == id }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_routine_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header Bar (Title + Date + Frosted Circular Bell & Reset Action Buttons)
        item(key = "header") {
            TodayHeaderSection(
                dateText = uiState.todayFormattedIndonesian,
                onOpenReminders = onOpenReminderSettings,
                onRequestReset = { showResetConfirmDialog = true }
            )
        }

        // 2. Daily Progress Hero Glass Card + Scheduled Evening Active Card + Next Reminder Chip
        item(key = "daily_progress_card") {
            DailyProgressHeroCard(
                uiState = uiState,
                onOpenReminderSettings = onOpenReminderSettings
            )
        }

        // 3. Rule 6 & Rule 9: Conflict & Safety Alerts (if any)
        if (uiState.routineConflictWarnings.isNotEmpty()) {
            item(key = "conflict_warnings_card") {
                RoutineConflictWarningBanner(warnings = uiState.routineConflictWarnings)
            }
        }

        // 4. Morning & Evening Split Summary Mini Glass Cards
        item(key = "period_summary_split") {
            MorningEveningSplitSummaryRow(
                morningDone = uiState.morningCompletedRequired,
                morningTotal = uiState.morningTotalRequired,
                eveningDone = uiState.eveningCompletedRequired,
                eveningTotal = uiState.eveningTotalRequired,
                optionalDone = uiState.optionalCompletedToday
            )
        }

        // 5. Rule 7: Two-Week Doctor Control Follow-up Card
        if (uiState.settings.doctorControlReminderEnabled) {
            item(key = "doctor_control_banner") {
                DoctorControlSummaryCard(
                    controlDateFormatted = uiState.doctorControlDateFormatted,
                    daysRemaining = uiState.doctorControlDaysRemaining ?: 14,
                    onOpenSettings = onOpenReminderSettings
                )
            }
        }

        // 6. Active Sunscreen Reapplication Card (20 min before outdoor + every 2–3h)
        if (uiState.settings.sunscreenReminderEnabled) {
            item(key = "sunscreen_active_banner") {
                SunscreenActiveStatusCard(
                    uiState = uiState,
                    onPauseOrResume = onPauseOrResumeSunscreen,
                    onStop = onStopSunscreen
                )
            }
        }

        // 7. MORNING ROUTINE SECTION
        item(key = "morning_section_header") {
            RoutinePeriodSectionHeader(
                title = "Rutinitas Pagi",
                subtitle = "Urutan resep: Pembersih Labore AcnePro ➔ Moisturizer ➔ Bioderma Kerato+ ➔ Sunscreen",
                doneCount = uiState.morningCompletedRequired,
                totalCount = uiState.morningTotalRequired,
                isMorning = true
            )
        }

        itemsIndexed(
            items = uiState.morningTasks,
            key = { _, item -> "morning_${item.task.id}" }
        ) { index, item ->
            RoutineStepGlassCard(
                stepNumber = index + 1,
                item = item,
                onToggleCheck = { onToggleTask(item, !item.isCompleted) },
                onShowDetail = { selectedTaskIdForDetail = item.task.id },
                onStartTimer = {
                    onStartStepTimer("Jeda setelah ${item.task.title} (~3 menit)")
                }
            )
        }

        // 8. EVENING ROUTINE SECTION
        item(key = "evening_section_header") {
            Spacer(modifier = Modifier.height(4.dp))
            RoutinePeriodSectionHeader(
                title = "Rutinitas Malam",
                subtitle = "Malam ini (${uiState.scheduledEveningProductToday.dayNameIndonesian}): ${uiState.scheduledEveningProductToday.productName}",
                doneCount = uiState.eveningCompletedRequired,
                totalCount = uiState.eveningTotalRequired,
                isMorning = false
            )
        }

        itemsIndexed(
            items = uiState.eveningTasks,
            key = { _, item -> "evening_${item.task.id}" }
        ) { index, item ->
            RoutineStepGlassCard(
                stepNumber = index + 1,
                item = item,
                onToggleCheck = { onToggleTask(item, !item.isCompleted) },
                onShowDetail = { selectedTaskIdForDetail = item.task.id },
                onStartTimer = {
                    onStartStepTimer("Jeda setelah ${item.task.title} (~3 menit)")
                }
            )
        }

        // 9. SEPARATED ORAL MEDICATION SECTION (Rule 1: Doxihat 100 mg)
        item(key = "oral_medication_section") {
            Spacer(modifier = Modifier.height(4.dp))
            OralMedicationTodayCard(
                uiState = uiState,
                onLogMedicationDose = onLogMedicationDose,
                onOpenReminderSettings = onOpenReminderSettings
            )
        }

        // 10. Medical Safety & No-Peeling / No-Cure-Claim Footer Card
        item(key = "safety_footer_card") {
            SafetyAndDisclaimerFooterCard()
        }
    }

    if (detailItem != null) {
        TaskDetailDialog(
            item = detailItem,
            onDismiss = { selectedTaskIdForDetail = null },
            onStartTimer = {
                onStartStepTimer("Jeda setelah ${detailItem.task.title} (~3 menit)")
            },
            onToggleComplete = {
                onToggleTask(detailItem, !detailItem.isCompleted)
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = glass.glassSurfaceElevated,
            title = {
                Text(
                    text = "Reset Checklist Hari Ini?",
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
            },
            text = {
                Text(
                    text = "Seluruh centang rutinitas pagi dan malam untuk hari ini (${uiState.todayFormattedIndonesian}) akan dikosongkan. Riwayat hari sebelumnya tetap tersimpan aman.",
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                LiquidPrimaryActionButton(
                    text = "Reset Hari Ini",
                    onClick = {
                        showResetConfirmDialog = false
                        onResetTodayChecklist()
                    },
                    modifier = Modifier.testTag("confirm_reset_today_button")
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_reset_today_button")
                ) {
                    Text("Batal", color = glass.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun TodayHeaderSection(
    dateText: String,
    onOpenReminders: () -> Unit,
    onRequestReset: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SkinCare Daily",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = glass.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = glass.textSecondary,
                modifier = Modifier.testTag("today_date_text")
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(
                onClick = onOpenReminders,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(glass.glassSurfaceElevated)
                    .border(1.dp, glass.glassBorderStrong, CircleShape)
                    .testTag("header_reminders_shortcut_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = "Pengaturan Pengingat",
                    tint = glass.accentPrimaryBlue
                )
            }
            IconButton(
                onClick = onRequestReset,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(glass.glassSurfaceElevated)
                    .border(1.dp, glass.glassBorderStrong, CircleShape)
                    .testTag("reset_today_checklist_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = "Reset Checklist Hari Ini",
                    tint = glass.accentPrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun DailyProgressHeroCard(
    uiState: SkinCareUiState,
    onOpenReminderSettings: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val scheduled = uiState.scheduledEveningProductToday
    val nextRem = uiState.nextReminder

    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_progress_card"),
        cornerRadius = 28.dp,
        isElevated = true,
        isHighlighted = true,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top Row: Circular Liquid Ring + Today's Completion Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LiquidProgressRing(
                    percentage = uiState.todayCompletionPercentage,
                    size = 104.dp,
                    strokeWidth = 10.dp,
                    subtitle = "${uiState.totalCompletedRequiredToday}/${uiState.totalRequiredToday} wajib",
                    modifier = Modifier.testTag("progress_percentage_text")
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidGlassPill(
                        text = "Progres Rutinitas Hari Ini",
                        containerColor = glass.accentSoftGlow,
                        contentColor = glass.accentPrimaryBlue
                    )
                    Text(
                        text = when {
                            uiState.todayCompletionPercentage >= 100 -> "Seluruh langkah wajib selesai!"
                            uiState.todayCompletionPercentage >= 50 -> "Separuh rutinitas hari ini tuntas"
                            else -> "Mari lengkapi jadwal resep hari ini"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Pagi ${uiState.morningCompletedRequired}/${uiState.morningTotalRequired} • Malam ${uiState.eveningCompletedRequired}/${uiState.eveningTotalRequired}",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }

            // Highlighted Glass Sub-card: Scheduled Evening Active Product (Pharmalene Gel / Labore Serum)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                glass.accentPrimaryBlue.copy(alpha = if (glass.isDark) 0.28f else 0.14f),
                                glass.accentElectricCyan.copy(alpha = if (glass.isDark) 0.22f else 0.18f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = glass.accentElectricCyan.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductBottleGlyph(
                        productCategory = ProductCategory.PRESCRIPTION_TOPICAL.id,
                        isActiveHighlight = true,
                        size = 46.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "JADWAL MALAM INI • ${scheduled.dayNameIndonesian.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = glass.accentPrimaryBlue
                            )
                        }
                        Text(
                            text = scheduled.productName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.textPrimary,
                            modifier = Modifier.testTag("scheduled_evening_product_text")
                        )
                        Text(
                            text = scheduled.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Next Reminder Row Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenReminderSettings),
                color = glass.glassSurfaceSecondary,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, glass.glassBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = glass.accentPrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (nextRem != null) {
                                "Pengingat berikutnya: ${nextRem.label} (${nextRem.formattedTime})"
                            } else {
                                "Belum ada pengingat aktif — ketuk untuk atur jadwal"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("next_reminder_summary_text")
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Atur",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = glass.accentPrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun MorningEveningSplitSummaryRow(
    morningDone: Int,
    morningTotal: Int,
    eveningDone: Int,
    eveningTotal: Int,
    optionalDone: Int
) {
    val glass = LiquidGlassTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Morning Summary Mini Glass Card
        LiquidGlassSurface(
            modifier = Modifier.weight(1f),
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                Column {
                    Text(
                        text = "Pagi",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.textSecondary
                    )
                    Text(
                        text = "$morningDone/$morningTotal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = glass.textPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    if (morningTotal > 0) (morningDone.toFloat() / morningTotal.toFloat()).coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = glass.accentPrimaryBlue,
                trackColor = glass.progressTrack
            )
        }

        // Evening Summary Mini Glass Card
        LiquidGlassSurface(
            modifier = Modifier.weight(1f),
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                Column {
                    Text(
                        text = "Malam",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.textSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$eveningDone/$eveningTotal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.textPrimary
                        )
                        if (optionalDone > 0) {
                            Text(
                                text = "+$optionalDone ops",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentPrimaryBlue
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    if (eveningTotal > 0) (eveningDone.toFloat() / eveningTotal.toFloat()).coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = glass.accentElectricCyan,
                trackColor = glass.progressTrack
            )
        }
    }
}

@Composable
private fun RoutineConflictWarningBanner(warnings: List<String>) {
    val glass = LiquidGlassTokens.colors
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conflict_warning_card"),
        cornerRadius = 22.dp,
        tintColor = glass.warningSoftBg,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Peringatan Konflik Instruksi",
                    tint = glass.warningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Peringatan Instruksi — ${RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = glass.warningAmber
                )
            }
            warnings.forEach { msg ->
                Text(
                    text = "• $msg",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textPrimary
                )
            }
        }
    }
}

@Composable
private fun DoctorControlSummaryCard(
    controlDateFormatted: String,
    daysRemaining: Int,
    onOpenSettings: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doctor_control_today_card"),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pengingat Kontrol 2 Minggu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Jadwal kontrol: $controlDateFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    val countdownLabel = when {
                        daysRemaining > 0 -> "$daysRemaining hari lagi menuju jadwal kontrol"
                        daysRemaining == 0 -> "Hari ini adalah jadwal kontrol dokter Anda!"
                        else -> "Jadwal kontrol telah lewat ${-daysRemaining} hari lalu"
                    }
                    Text(
                        text = countdownLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = glass.accentPrimaryBlue
                    )
                }
            }
            TextButton(onClick = onOpenSettings) {
                Text("Ubah", color = glass.accentPrimaryBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SunscreenActiveStatusCard(
    uiState: SkinCareUiState,
    onPauseOrResume: (Boolean) -> Unit,
    onStop: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val settings = uiState.settings
    val schedule = uiState.sunscreenScheduleToday
    val preOutdoorStr = RoutineScheduleHelper.formatTimeHm(
        schedule.applyBeforeOutdoorTime.hour,
        schedule.applyBeforeOutdoorTime.minute
    )
    val slotsStr = schedule.reapplyTimes.joinToString(", ") {
        RoutineScheduleHelper.formatTimeHm(it.hour, it.minute)
    }

    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sunscreen_active_banner"),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            .size(38.dp)
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
                    Column {
                        Text(
                            text = if (settings.sunscreenPaused) {
                                "Sunscreen Labore Biomeprotect (Dijeda)"
                            } else {
                                "Pengingat Sunscreen Aktif (${settings.sunscreenIntervalHours} Jam)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "20 mnt sebelum keluar ($preOutdoorStr) • Re-apply: $slotsStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidSecondaryGlassButton(
                    text = if (settings.sunscreenPaused) "Lanjutkan" else "Jeda Hari Ini",
                    leadingIcon = if (settings.sunscreenPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    onClick = { onPauseOrResume(!settings.sunscreenPaused) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sunscreen_banner_pause_resume_button")
                )
                LiquidSecondaryGlassButton(
                    text = "Hentikan",
                    leadingIcon = Icons.Outlined.Stop,
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sunscreen_banner_stop_button")
                )
            }
        }
    }
}

@Composable
private fun RoutinePeriodSectionHeader(
    title: String,
    subtitle: String,
    doneCount: Int,
    totalCount: Int,
    isMorning: Boolean
) {
    val glass = LiquidGlassTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (isMorning) {
                                listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                            } else {
                                listOf(glass.accentDeepBlue, glass.accentPrimaryBlue)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMorning) Icons.Outlined.WbSunny else Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = glass.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        LiquidGlassPill(
            text = "$doneCount/$totalCount Selesai",
            containerColor = if (doneCount == totalCount && totalCount > 0) glass.successSoftBg else glass.accentSoftGlow,
            contentColor = if (doneCount == totalCount && totalCount > 0) glass.successMint else glass.accentPrimaryBlue
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineStepGlassCard(
    stepNumber: Int,
    item: TaskItemUiModel,
    onToggleCheck: () -> Unit,
    onShowDetail: () -> Unit,
    onStartTimer: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val task = item.task
    val categoryEnum = item.resolvedCategory
    val isScheduledEveningActive = task.isScheduledActive

    val categoryPillBg = when (categoryEnum) {
        ProductCategory.PRESCRIPTION_TOPICAL -> glass.accentSoftGlow
        ProductCategory.SUNSCREEN -> glass.accentElectricCyan.copy(alpha = 0.18f)
        ProductCategory.SUPPORTIVE_SKINCARE -> glass.glassSurfaceSecondary
        ProductCategory.PRESCRIPTION_ORAL -> glass.warningSoftBg
    }
    val categoryPillText = when (categoryEnum) {
        ProductCategory.PRESCRIPTION_TOPICAL -> glass.accentPrimaryBlue
        ProductCategory.SUNSCREEN -> glass.accentDeepBlue
        ProductCategory.SUPPORTIVE_SKINCARE -> glass.textSecondary
        ProductCategory.PRESCRIPTION_ORAL -> glass.warningAmber
    }

    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        cornerRadius = 24.dp,
        isElevated = isScheduledEveningActive,
        isHighlighted = isScheduledEveningActive && !item.isCompleted,
        contentPadding = PaddingValues(15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Liquid Glass Check Circle (minimum 48dp touch target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleCheck)
                        .padding(4.dp)
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                brush = if (item.isCompleted) {
                                    Brush.linearGradient(
                                        listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(glass.glassSurfaceSecondary, glass.glassSurfaceElevated)
                                    )
                                }
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (item.isCompleted) Color.White.copy(alpha = 0.7f) else glass.accentPrimaryBlue.copy(alpha = 0.55f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isCompleted) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selesai",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = stepNumber.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = glass.accentPrimaryBlue
                            )
                        }
                    }
                }

                // Step Content Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onShowDetail),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category & Day Schedule Pills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LiquidGlassPill(
                            text = categoryEnum.labelIndonesian,
                            containerColor = categoryPillBg,
                            contentColor = categoryPillText,
                            modifier = Modifier.testTag("task_category_badge_${task.id}")
                        )

                        if (!item.scheduleBadge.isNullOrBlank()) {
                            LiquidGlassPill(
                                text = item.scheduleBadge,
                                containerColor = if (isScheduledEveningActive) glass.accentSoftGlow else glass.glassSurfaceSecondary,
                                contentColor = if (isScheduledEveningActive) glass.accentPrimaryBlue else glass.textSecondary
                            )
                        }

                        if (task.isOptional) {
                            LiquidGlassPill(
                                text = "Opsional (Jika Iritasi)",
                                containerColor = glass.warningSoftBg,
                                contentColor = glass.warningAmber
                            )
                        }
                    }

                    // Step Title & Resolved Product Name
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCompleted) glass.textSecondary else glass.textPrimary,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = item.resolvedProductName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = glass.accentPrimaryBlue
                    )

                    // Instruction text
                    Text(
                        text = item.resolvedInstruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )

                    // Prescription Metadata Chips (Takaran, Area, Durasi)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAmountNeedsConfirm = item.resolvedUsageAmount.contains(
                            RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION,
                            ignoreCase = true
                        )
                        LiquidGlassPill(
                            text = "Takaran: ${item.resolvedUsageAmount}",
                            containerColor = if (isAmountNeedsConfirm) glass.warningSoftBg else glass.glassSurfaceSecondary,
                            contentColor = if (isAmountNeedsConfirm) glass.warningAmber else glass.textPrimary
                        )

                        val isAreaNeedsConfirm = item.resolvedApplicationArea.contains(
                            RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION,
                            ignoreCase = true
                        )
                        LiquidGlassPill(
                            text = "Area: ${item.resolvedApplicationArea}",
                            containerColor = if (isAreaNeedsConfirm) glass.warningSoftBg else glass.glassSurfaceSecondary,
                            contentColor = if (isAreaNeedsConfirm) glass.warningAmber else glass.textPrimary
                        )

                        if (item.resolvedDurationNote.isNotBlank()) {
                            LiquidGlassPill(
                                text = item.resolvedDurationNote,
                                containerColor = glass.glassSurfaceSecondary,
                                contentColor = glass.textSecondary
                            )
                        }
                    }

                    // Doctor confirmation note (Rule 5)
                    if (item.resolvedConfirmationNote.isNotBlank()) {
                        Surface(
                            color = glass.warningSoftBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, glass.warningAmber.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = glass.warningAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.resolvedConfirmationNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textPrimary,
                                    modifier = Modifier.testTag("task_confirm_warning_${task.id}")
                                )
                            }
                        }
                    }
                }

                // Product Bottle Visual Thumbnail on the right
                ProductBottleGlyph(
                    productCategory = task.category,
                    isActiveHighlight = isScheduledEveningActive,
                    size = 42.dp,
                    modifier = Modifier.clickable(onClick = onShowDetail)
                )
            }

            // Bottom Row: Completion Timestamp + 3-Minute Timer Pill & Detail Button
            HorizontalDivider(color = glass.glassBorderSubtle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeDone = item.completedAtMillis?.let { millis ->
                    val lt = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime()
                    RoutineScheduleHelper.formatTimeHm(lt.hour, lt.minute)
                }
                if (item.isCompleted && timeDone != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = glass.successMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Selesai pukul $timeDone",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.successMint,
                            modifier = Modifier.testTag("task_completed_time_${task.id}")
                        )
                    }
                } else {
                    Text(
                        text = if (task.isOptional) "Opsional sesuai kondisi kulit" else "Langkah wajib resep",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onStartTimer)
                            .testTag("start_timer_button_${task.id}"),
                        color = glass.accentSoftGlow,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, glass.glassBorderStrong)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "Mulai Timer Jeda 3 Menit",
                                tint = glass.accentPrimaryBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Timer 3m",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = glass.accentPrimaryBlue
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onShowDetail)
                            .testTag("task_detail_button_${task.id}"),
                        color = glass.glassSurfaceSecondary,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, glass.glassBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Detail Langkah",
                                tint = glass.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Detail",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OralMedicationTodayCard(
    uiState: SkinCareUiState,
    onLogMedicationDose: (slot: String, status: String) -> Unit,
    onOpenReminderSettings: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val settings = uiState.settings

    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("oral_medication_today_card"),
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
                    ProductBottleGlyph(
                        productCategory = ProductCategory.PRESCRIPTION_ORAL.id,
                        isActiveHighlight = true,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Obat Minum Resep (Terpisah dari Oles)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = glass.accentPrimaryBlue
                        )
                        Text(
                            text = RoutineScheduleHelper.ORAL_MEDICATION_NAME,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "Setelah makan pagi & malam • 2x sehari selama 2 minggu (14 hari)",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }

            // Discrepancy Warning Box (Rule 5 & Rule 9)
            Surface(
                color = glass.warningSoftBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, glass.warningAmber.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = glass.warningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION,
                            style = MaterialTheme.typography.labelMedium,
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

            if (uiState.medicationDayNumber != null) {
                LiquidGlassPill(
                    text = "Hari ke-${uiState.medicationDayNumber} dari ${settings.medicationDurationDays} hari terapi (${settings.medicationTotalCapsulesConfirmed} kapsul)",
                    containerColor = glass.accentSoftGlow,
                    contentColor = glass.accentPrimaryBlue
                )
            }

            // Dose Logging Slots: Breakfast & Dinner
            MedicationDoseRow(
                label = "Dosis Pagi (Setelah Makan Pagi)",
                scheduledTime = RoutineScheduleHelper.formatTimeHm(
                    settings.medicationBreakfastHour,
                    settings.medicationBreakfastMinute
                ),
                currentStatus = uiState.todayBreakfastMedLog?.status,
                onMarkTaken = { onLogMedicationDose("BREAKFAST", "TAKEN") },
                onMarkMissed = { onLogMedicationDose("BREAKFAST", "MISSED") },
                takenButtonTag = "med_breakfast_taken_button",
                missedButtonTag = "med_breakfast_missed_button"
            )

            MedicationDoseRow(
                label = "Dosis Malam (Setelah Makan Malam)",
                scheduledTime = RoutineScheduleHelper.formatTimeHm(
                    settings.medicationDinnerHour,
                    settings.medicationDinnerMinute
                ),
                currentStatus = uiState.todayDinnerMedLog?.status,
                onMarkTaken = { onLogMedicationDose("DINNER", "TAKEN") },
                onMarkMissed = { onLogMedicationDose("DINNER", "MISSED") },
                takenButtonTag = "med_dinner_taken_button",
                missedButtonTag = "med_dinner_missed_button"
            )

            if (!settings.medicationDiscrepancyConfirmed) {
                LiquidSecondaryGlassButton(
                    text = "Konfirmasi & Aktifkan Pengingat Doxihat 100 mg",
                    leadingIcon = Icons.Outlined.Schedule,
                    onClick = onOpenReminderSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("configure_medication_from_today_button")
                )
            }
        }
    }
}

@Composable
private fun MedicationDoseRow(
    label: String,
    scheduledTime: String,
    currentStatus: String?,
    onMarkTaken: () -> Unit,
    onMarkMissed: () -> Unit,
    takenButtonTag: String,
    missedButtonTag: String
) {
    val glass = LiquidGlassTokens.colors
    Surface(
        color = glass.glassSurfaceSecondary,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, glass.glassBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
                val statusLabel = when (currentStatus) {
                    "TAKEN" -> "Sudah diminum (Jadwal $scheduledTime)"
                    "MISSED" -> "Terlewat / Belum diminum"
                    else -> "Jadwal: pukul $scheduledTime"
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (currentStatus) {
                        "TAKEN" -> glass.successMint
                        "MISSED" -> glass.dangerCoral
                        else -> glass.textSecondary
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onMarkTaken)
                        .testTag(takenButtonTag),
                    color = if (currentStatus == "TAKEN") glass.accentPrimaryBlue else glass.glassSurfaceElevated,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, glass.glassBorderStrong)
                ) {
                    Text(
                        text = "Sudah",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentStatus == "TAKEN") Color.White else glass.accentPrimaryBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onMarkMissed)
                        .testTag(missedButtonTag),
                    color = if (currentStatus == "MISSED") glass.dangerSoftBg else glass.glassSurfaceElevated,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, glass.glassBorderSubtle)
                ) {
                    Text(
                        text = "Terlewat",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (currentStatus == "MISSED") glass.dangerCoral else glass.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyAndDisclaimerFooterCard() {
    val glass = LiquidGlassTokens.colors
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medical_safety_footer_card"),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = glass.accentPrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Catatan Keamanan Terapi Dokter",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
            }
            Text(
                text = "• ${RoutineScheduleHelper.CHEMICAL_PEELING_WARNING}",
                style = MaterialTheme.typography.bodySmall,
                color = glass.textPrimary
            )
            Text(
                text = "• Aplikasi ini hanya membantu mencatat jadwal resep dokter kulit dan tidak menjamin bahwa produk pasti menyembuhkan jerawat atau bekas jerawat.",
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )
        }
    }
}
