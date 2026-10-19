package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.RoutinePeriod
import com.example.ui.StepTimerState
import com.example.ui.TaskItemUiModel

@Composable
fun NonBlockingStepTimerBar(
    timerState: StepTimerState,
    onTogglePauseResume: () -> Unit,
    onResetAndDismiss: () -> Unit,
    onRestartThreeMinutes: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = timerState.isActiveOrPaused,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val progress = if (timerState.totalSeconds > 0) {
            timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
        } else 0f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("step_timer_banner"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Timer jeda langkah",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = timerState.contextLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (timerState.remainingSeconds == 0) {
                                    "Jeda 3 menit selesai — silakan lanjut ke langkah berikutnya"
                                } else {
                                    "Anda bebas berpindah layar selagi timer berjalan"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = timerState.formattedMmSs,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("step_timer_countdown_text")
                        )
                    }

                    IconButton(
                        onClick = onTogglePauseResume,
                        modifier = Modifier.testTag("step_timer_pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (timerState.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (timerState.isRunning) "Jeda timer" else "Lanjutkan timer",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    IconButton(
                        onClick = onRestartThreeMinutes,
                        modifier = Modifier.testTag("step_timer_restart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Ulangi 3 menit",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    IconButton(
                        onClick = onResetAndDismiss,
                        modifier = Modifier.testTag("step_timer_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup timer",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun FirstRunNotificationSetupDialog(
    onConfirmSetup: (morningEnabled: Boolean, eveningEnabled: Boolean, requestPermissionNow: Boolean) -> Unit,
    onDismissLater: () -> Unit
) {
    var morningChecked by rememberSaveable { mutableStateOf(true) }
    var eveningChecked by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismissLater,
        icon = {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = "Pengaturan pengingat awal",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Atur Pengingat Skincare Harian",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pengingat membantu Anda menjalankan urutan perawatan pagi dan malam (termasuk jadwal bergantian Pharmalene Gel & Serum Labore) secara konsisten sesuai resep dokter kulit.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Pilih pengingat awal yang Anda inginkan (jam dapat diubah kapan saja):",
                    style = MaterialTheme.typography.labelLarge
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = morningChecked,
                                onCheckedChange = { morningChecked = it },
                                modifier = Modifier.testTag("onboarding_morning_checkbox")
                            )
                            Column {
                                Text(
                                    text = "Rutinitas Pagi (07:00)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Pembersih, Topicalm, Sébium Kerato+, Sunscreen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = eveningChecked,
                                onCheckedChange = { eveningChecked = it },
                                modifier = Modifier.testTag("onboarding_evening_checkbox")
                            )
                            Column {
                                Text(
                                    text = "Rutinitas Malam (21:00)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Menyesuaikan jadwal malam Pharmalene / Serum Labore",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Catatan: Seluruh data dan jadwal disimpan 100% secara lokal di perangkat Anda tanpa memerlukan koneksi internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSetup(morningChecked, eveningChecked, morningChecked || eveningChecked)
                },
                modifier = Modifier.testTag("onboarding_confirm_button")
            ) {
                Text("Simpan & Aktifkan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onConfirmSetup(morningChecked, eveningChecked, false)
                },
                modifier = Modifier.testTag("onboarding_skip_permission_button")
            ) {
                Text("Nanti Saja")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModalDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                modifier = Modifier.testTag("time_picker_confirm_button")
            ) {
                Text("Simpan Waktu")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("time_picker_cancel_button")
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun TaskDetailDialog(
    item: TaskItemUiModel,
    onDismiss: () -> Unit,
    onStartThreeMinuteTimer: (String) -> Unit,
    onToggleComplete: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = item.task.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.task.period == RoutinePeriod.MORNING.id) "Rutinitas Pagi" else "Rutinitas Malam",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Produk Resep Dokter:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = item.resolvedProductName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column {
                    Text(
                        text = "Petunjuk Penggunaan:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.resolvedInstruction,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item.scheduleBadge?.let { badge ->
                    Text(
                        text = "Jadwal: $badge • Estimasi ~${item.task.estimatedMinutes} menit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Anjuran dokter kulit: Beri jeda sekitar 3 menit antar langkah skincare agar produk meresap optimal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                FilledTonalButton(
                    onClick = {
                        onStartThreeMinuteTimer("Jeda setelah ${item.task.title}")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_start_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mulai Timer Jeda 3 Menit")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onToggleComplete(!item.isCompleted)
                    onDismiss()
                },
                modifier = Modifier.testTag("detail_toggle_complete_button")
            ) {
                Text(if (item.isCompleted) "Tandai Belum Selesai" else "Tandai Selesai")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
