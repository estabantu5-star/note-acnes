package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.RoutineScheduleHelper
import com.example.ui.HistoryDayRecordUi
import com.example.ui.SkinCareUiState
import com.example.ui.WeeklyHistorySummaryUi
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidSegmentedBar
import com.example.ui.theme.LiquidGlassTokens

@Composable
fun HistoryScreen(
    uiState: SkinCareUiState,
    modifier: Modifier = Modifier
) {
    val glass = LiquidGlassTokens.colors
    var expandedDates by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedFilterIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = 7 Hari, 1 = 30 Hari, 2 = Semua

    val filteredRecords = when (selectedFilterIndex) {
        0 -> uiState.historyRecords.take(7)
        1 -> uiState.historyRecords.take(30)
        else -> uiState.historyRecords
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Riwayat Kepatuhan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = glass.textPrimary
                )
                Text(
                    text = "Catatan harian rutinitas pagi, malam, jadwal aktif malam, dan obat minum Doxihat 100 mg.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }

        // Segmented Filter Pill Bar: 7 Hari | 30 Hari | Semua
        item(key = "filter_segmented_bar") {
            LiquidSegmentedBar(
                options = listOf("7 Hari", "30 Hari", "Semua"),
                selectedIndex = selectedFilterIndex,
                onSelect = { selectedFilterIndex = it },
                optionTestTags = listOf(
                    "history_filter_7d",
                    "history_filter_30d",
                    "history_filter_all"
                )
            )
        }

        // Weekly Summary 2x2 Frosted Glass Grid + 7-Day Adherence Bar Strip
        item(key = "weekly_summary_card") {
            WeeklyAdherenceSummaryCard(
                summary = uiState.weeklySummary,
                recentRecords = uiState.historyRecords.take(7)
            )
        }

        if (filteredRecords.isEmpty()) {
            item(key = "empty_history") {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_history_card"),
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum Ada Catatan Riwayat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "Centang langkah rutinitas Anda di tab Hari Ini untuk mulai merekam riwayat kepatuhan harian secara otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredRecords,
                key = { it.dateIso }
            ) { record ->
                val isExpanded = record.dateIso in expandedDates
                DailyHistoryRecordCard(
                    record = record,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedDates = if (isExpanded) {
                            expandedDates - record.dateIso
                        } else {
                            expandedDates + record.dateIso
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WeeklyAdherenceSummaryCard(
    summary: WeeklyHistorySummaryUi,
    recentRecords: List<HistoryDayRecordUi>
) {
    val glass = LiquidGlassTokens.colors
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_summary_card"),
        cornerRadius = 26.dp,
        isElevated = true,
        isHighlighted = true,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ringkasan 7 Hari Terakhir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
                LiquidGlassPill(
                    text = "${summary.totalTasksCompletedCount} langkah selesai",
                    containerColor = glass.accentSoftGlow,
                    contentColor = glass.accentPrimaryBlue
                )
            }

            // 2x2 Frosted Glass Stat Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatGlassTile(
                    icon = Icons.Outlined.Insights,
                    label = "Rata-rata Progres",
                    value = "${summary.averageCompletionPercentage}%",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatGlassTile(
                    icon = Icons.Outlined.CalendarToday,
                    label = "Hari Tercatat",
                    value = "${summary.recordedDaysCount} hari",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatGlassTile(
                    icon = Icons.Outlined.WbSunny,
                    label = "Pagi Tuntas",
                    value = "${summary.fullMorningDaysCount} hari",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatGlassTile(
                    icon = Icons.Outlined.Bedtime,
                    label = "Malam Tuntas",
                    value = "${summary.fullEveningDaysCount} hari",
                    modifier = Modifier.weight(1f)
                )
            }

            if (recentRecords.isNotEmpty()) {
                Surface(
                    color = glass.glassSurfaceSecondary,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, glass.glassBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Grafik Kepatuhan Harian",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = glass.textSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            recentRecords.reversed().forEach { rec ->
                                val pct = rec.completionPercentage.coerceIn(0, 100)
                                val barFraction = (pct / 100f).coerceIn(0.12f, 1f)
                                val shortLabel = rec.dateIso.takeLast(5)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .height((40.dp * barFraction).coerceAtLeast(6.dp))
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = if (pct >= 100) {
                                                        listOf(glass.accentElectricCyan, glass.accentPrimaryBlue)
                                                    } else {
                                                        listOf(
                                                            glass.accentPrimaryBlue.copy(alpha = 0.75f),
                                                            glass.accentElectricCyan.copy(alpha = 0.55f)
                                                        )
                                                    }
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = shortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatGlassTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val glass = LiquidGlassTokens.colors
    Surface(
        modifier = modifier,
        color = glass.glassSurfaceSecondary,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, glass.glassBorderStrong)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                glass.accentPrimaryBlue.copy(alpha = 0.2f),
                                glass.accentElectricCyan.copy(alpha = 0.25f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = glass.accentPrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = glass.textPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DailyHistoryRecordCard(
    record: HistoryDayRecordUi,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val totalRequired = (record.morningTotal + record.eveningTotal).coerceAtLeast(1)
    val totalDone = record.morningCompleted + record.eveningCompleted
    val progress = (totalDone.toFloat() / totalRequired.toFloat()).coerceIn(0f, 1f)

    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .testTag("history_day_card_${record.dateIso}"),
        cornerRadius = 24.dp,
        isElevated = isExpanded,
        isHighlighted = record.completionPercentage >= 100,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.formattedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Produk malam terjadwal: ${record.scheduledEveningProduct}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = glass.accentPrimaryBlue
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidGlassPill(
                        text = "${record.completionPercentage}%",
                        containerColor = if (record.completionPercentage >= 100) glass.successSoftBg else glass.accentSoftGlow,
                        contentColor = if (record.completionPercentage >= 100) glass.successMint else glass.accentPrimaryBlue
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Tutup rincian" else "Lihat rincian",
                        tint = glass.textSecondary
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = if (record.completionPercentage >= 100) glass.successMint else glass.accentElectricCyan,
                trackColor = glass.progressTrack
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassPill(
                    text = "Pagi: ${record.morningCompleted}/${record.morningTotal}",
                    containerColor = glass.glassSurfaceSecondary,
                    contentColor = glass.textPrimary
                )
                LiquidGlassPill(
                    text = "Malam: ${record.eveningCompleted}/${record.eveningTotal}",
                    containerColor = glass.glassSurfaceSecondary,
                    contentColor = glass.textPrimary
                )
                if (record.optionalCompleted > 0) {
                    LiquidGlassPill(
                        text = "Opsional: +${record.optionalCompleted}",
                        containerColor = glass.accentSoftGlow,
                        contentColor = glass.accentPrimaryBlue
                    )
                }
            }

            // Oral medication badges if logged on that day
            if (record.medicationBreakfastStatus != null || record.medicationDinnerStatus != null) {
                Surface(
                    color = glass.glassSurfaceSecondary,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, glass.glassBorderSubtle)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MedicalServices,
                            contentDescription = null,
                            tint = glass.accentPrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        val bStatus = when (record.medicationBreakfastStatus) {
                            "TAKEN" -> "Pagi: Diminum"
                            "MISSED" -> "Pagi: Terlewat"
                            else -> "Pagi: -"
                        }
                        val dStatus = when (record.medicationDinnerStatus) {
                            "TAKEN" -> "Malam: Diminum"
                            "MISSED" -> "Malam: Terlewat"
                            else -> "Malam: -"
                        }
                        Text(
                            text = "Doxihat 100 mg • $bStatus | $dStatus",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.textPrimary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = glass.glassBorderSubtle)
                    Text(
                        text = "Rincian Langkah (${record.dateIso}):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = glass.accentPrimaryBlue
                    )

                    val allTasksForDay = record.completedTasks + record.missedTasks
                    allTasksForDay.forEach { taskItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (taskItem.isCompleted) {
                                        Icons.Outlined.CheckCircle
                                    } else {
                                        Icons.Outlined.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (taskItem.isCompleted) {
                                        glass.successMint
                                    } else {
                                        glass.textMuted
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${if (taskItem.period == "MORNING") "[Pagi]" else "[Malam]"} ${taskItem.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (taskItem.isCompleted) {
                                        glass.textPrimary
                                    } else {
                                        glass.textSecondary
                                    }
                                )
                            }

                            val timeText = taskItem.completedAtMillis?.let { millis ->
                                val lt = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalTime()
                                RoutineScheduleHelper.formatTimeHm(lt.hour, lt.minute)
                            }
                            Text(
                                text = if (taskItem.isCompleted) (timeText ?: "Selesai") else "Terlewat",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (taskItem.isCompleted) {
                                    glass.successMint
                                } else {
                                    glass.textMuted
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
