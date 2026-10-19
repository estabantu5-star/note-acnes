package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RoutineTaskEntity
import com.example.data.preferences.ThemeMode
import com.example.domain.ProductCategory
import com.example.domain.RoutinePeriod
import com.example.domain.RoutineScheduleHelper
import com.example.ui.SkinCareUiState
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidPrimaryActionButton
import com.example.ui.components.LiquidSecondaryGlassButton
import com.example.ui.components.LiquidSegmentedBar
import com.example.ui.components.ProductBottleGlyph
import com.example.ui.theme.LiquidGlassTokens

@Composable
fun SettingsScreen(
    uiState: SkinCareUiState,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleHideSensitiveNotifications: (Boolean) -> Unit,
    onOpenReminderSettings: () -> Unit,
    onAddCustomTask: (
        period: RoutinePeriod,
        title: String,
        productName: String,
        instruction: String,
        estimatedMinutes: Int,
        isOptional: Boolean,
        category: String,
        scheduledDaysCsv: String,
        usageAmount: String,
        applicationArea: String
    ) -> Unit,
    onUpdateTask: (
        task: RoutineTaskEntity,
        title: String,
        productName: String,
        instruction: String,
        estimatedMinutes: Int,
        isOptional: Boolean,
        isActive: Boolean,
        category: String,
        scheduledDaysCsv: String,
        usageAmount: String,
        applicationArea: String
    ) -> Unit,
    onMoveTaskOrder: (taskId: String, moveUp: Boolean) -> Unit,
    onRetireTask: (RoutineTaskEntity) -> Unit,
    onRestoreTaskActive: (RoutineTaskEntity) -> Unit,
    onRestoreDermatologistDefaultRoutine: () -> Unit,
    onExportJson: ((String) -> Unit) -> Unit,
    onImportJson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LiquidGlassTokens.colors

    var showEditorDialog by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }

    var exportedJsonPreview by rememberSaveable { mutableStateOf<String?>(null) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var importJsonInput by rememberSaveable { mutableStateOf("") }
    var showConfirmResetDefault by rememberSaveable { mutableStateOf(false) }

    val editingTask: RoutineTaskEntity? = editingTaskId?.let { id ->
        uiState.allTasksForEditor.firstOrNull { it.id == id }
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
            .testTag("app_settings_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Pengaturan Aplikasi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = glass.textPrimary
                )
                Text(
                    text = "Sesuaikan tampilan Liquid Glass, kelola langkah rutinitas resep dokter, cadangkan data, atau tinjau catatan keamanan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }

        // 1. Theme & Notification Privacy Card
        item(key = "theme_and_privacy_card") {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                isElevated = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(glass.accentSoftGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = glass.accentPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tema Tampilan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "Pilih suasana Blue Liquid Glass terang atau malam",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }

                    val themeModes = ThemeMode.entries
                    val selectedIndex = themeModes.indexOf(uiState.settings.themeMode).coerceAtLeast(0)

                    LiquidSegmentedBar(
                        options = themeModes.map { it.labelId },
                        selectedIndex = selectedIndex,
                        onSelect = { idx -> onSelectTheme(themeModes[idx]) },
                        optionTestTags = themeModes.map { "theme_chip_${it.name.lowercase()}" }
                    )

                    HorizontalDivider(color = glass.glassBorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sembunyikan Nama Produk di Layar Kunci",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "Menjaga privasi nama obat/produk pada isi notifikasi Android",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                        Switch(
                            checked = uiState.settings.hideSensitiveNotificationContent,
                            onCheckedChange = onToggleHideSensitiveNotifications,
                            colors = liquidSwitchColors,
                            modifier = Modifier.testTag("hide_sensitive_notif_switch")
                        )
                    }

                    LiquidSecondaryGlassButton(
                        text = "Buka Pengaturan Pengingat & Jadwal",
                        onClick = onOpenReminderSettings,
                        leadingIcon = Icons.Outlined.NotificationsActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_open_reminders_button")
                    )
                }
            }
        }

        // 2. Routine Task Editor Card
        item(key = "routine_editor_header") {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kelola Langkah Rutinitas Resep",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "Atur kategori (Obat Resep / Skincare Pendukung / Sunscreen), jadwal hari, takaran, dan area aplikasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LiquidPrimaryActionButton(
                            text = "Tambah",
                            onClick = {
                                editingTaskId = null
                                showEditorDialog = true
                            },
                            leadingIcon = Icons.Outlined.Add,
                            modifier = Modifier.testTag("add_custom_task_button")
                        )
                    }

                    val morningTasks = uiState.allTasksForEditor
                        .filter { it.period == RoutinePeriod.MORNING.id }
                        .sortedBy { it.sortOrder }
                    val eveningTasks = uiState.allTasksForEditor
                        .filter { it.period == RoutinePeriod.EVENING.id }
                        .sortedBy { it.sortOrder }

                    Text(
                        text = "Rutinitas Pagi (${morningTasks.size} langkah)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = glass.accentPrimaryBlue
                    )
                    morningTasks.forEachIndexed { idx, task ->
                        EditableTaskRow(
                            task = task,
                            canMoveUp = idx > 0,
                            canMoveDown = idx < morningTasks.lastIndex,
                            onMoveUp = { onMoveTaskOrder(task.id, true) },
                            onMoveDown = { onMoveTaskOrder(task.id, false) },
                            onToggleActive = { active ->
                                if (active) onRestoreTaskActive(task) else onRetireTask(task)
                            },
                            onEdit = {
                                editingTaskId = task.id
                                showEditorDialog = true
                            },
                            switchColors = liquidSwitchColors
                        )
                    }

                    HorizontalDivider(color = glass.glassBorderSubtle)

                    Text(
                        text = "Rutinitas Malam (${eveningTasks.size} langkah)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = glass.accentElectricCyan
                    )
                    eveningTasks.forEachIndexed { idx, task ->
                        EditableTaskRow(
                            task = task,
                            canMoveUp = idx > 0,
                            canMoveDown = idx < eveningTasks.lastIndex,
                            onMoveUp = { onMoveTaskOrder(task.id, true) },
                            onMoveDown = { onMoveTaskOrder(task.id, false) },
                            onToggleActive = { active ->
                                if (active) onRestoreTaskActive(task) else onRetireTask(task)
                            },
                            onEdit = {
                                editingTaskId = task.id
                                showEditorDialog = true
                            },
                            switchColors = liquidSwitchColors
                        )
                    }
                }
            }
        }

        // 3. Backup Export / Import & Factory Reset Card
        item(key = "backup_and_restore_card") {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                isElevated = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cadangkan, Pulihkan & Reset Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Ekspor seluruh riwayat dan daftar produk ke format teks JSON lokal atau pulihkan ke jadwal resep dokter awal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidPrimaryActionButton(
                            text = "Ekspor JSON",
                            onClick = {
                                onExportJson { json ->
                                    exportedJsonPreview = json
                                }
                            },
                            leadingIcon = Icons.Outlined.FileUpload,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        )
                        LiquidSecondaryGlassButton(
                            text = "Impor JSON",
                            onClick = { showImportDialog = true },
                            leadingIcon = Icons.Outlined.FileDownload,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_button")
                        )
                    }

                    LiquidSecondaryGlassButton(
                        text = "Kembalikan ke Resep Dokter Awal",
                        onClick = { showConfirmResetDefault = true },
                        leadingIcon = Icons.Outlined.RestartAlt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_default_prescription_button")
                    )
                }
            }
        }

        // 4. Medical Safety & No-Peeling / No-Cure-Claim Notice Card
        item(key = "medical_disclaimer_card") {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medical_disclaimer_card"),
                cornerRadius = 26.dp,
                tintColor = glass.warningSoftBg.copy(alpha = 0.35f),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(glass.accentSoftGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = glass.accentPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Catatan Keamanan Terapi & Disclaimer Medis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary
                        )
                    }
                    Text(
                        text = "• ${RoutineScheduleHelper.CHEMICAL_PEELING_WARNING}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = glass.warningAmber
                    )
                    Text(
                        text = "• Aplikasi ini hanya membantu mencatat jadwal resep dokter kulit dan tidak menjamin bahwa produk pasti menyembuhkan jerawat atau bekas jerawat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    Text(
                        text = "• Jadwal kontrol kembali: 2 minggu setelah mulai terapi sesuai catatan dokter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }
        }
    }

    // Export JSON Preview Dialog
    if (exportedJsonPreview != null) {
        val jsonText = exportedJsonPreview.orEmpty()
        AlertDialog(
            onDismissRequest = { exportedJsonPreview = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = glass.glassSurfaceElevated,
            title = {
                Text(
                    text = "Data Cadangan JSON",
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Salin teks JSON di bawah ini untuk disimpan sebagai cadangan:",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 10,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exported_json_textfield")
                    )
                }
            },
            confirmButton = {
                LiquidPrimaryActionButton(
                    text = "Salin ke Papan Klip",
                    leadingIcon = Icons.Outlined.ContentCopy,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("SkinCareDailyBackup", jsonText))
                        exportedJsonPreview = null
                    },
                    modifier = Modifier.testTag("copy_exported_json_button")
                )
            },
            dismissButton = {
                TextButton(onClick = { exportedJsonPreview = null }) {
                    Text("Tutup", color = glass.textSecondary)
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = glass.glassSurfaceElevated,
            title = {
                Text(
                    text = "Impor Cadangan JSON",
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tempelkan teks JSON cadangan Anda di bawah ini:",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("{\"version\":2,...}") },
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_json_textfield")
                    )
                }
            },
            confirmButton = {
                LiquidPrimaryActionButton(
                    text = "Pulihkan Data",
                    onClick = {
                        if (importJsonInput.isNotBlank()) {
                            onImportJson(importJsonInput)
                        }
                        showImportDialog = false
                    },
                    modifier = Modifier.testTag("confirm_import_json_button")
                )
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Batal", color = glass.textSecondary)
                }
            }
        )
    }

    // Confirm Reset Default Prescription Dialog
    if (showConfirmResetDefault) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDefault = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = glass.glassSurfaceElevated,
            title = {
                Text(
                    text = "Kembalikan ke Resep Dokter Awal?",
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
            },
            text = {
                Text(
                    text = "Tindakan ini akan mengembalikan langkah rutinitas sesuai resep dokter (Labore AcnePro, Moisturizer, Bioderma Kerato+, Sunscreen Labore, Pharmalene Gel Sen/Rab/Jum, Serum Labore Sel/Kam/Sab/Min).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                LiquidPrimaryActionButton(
                    text = "Ya, Kembalikan",
                    onClick = {
                        showConfirmResetDefault = false
                        onRestoreDermatologistDefaultRoutine()
                    },
                    modifier = Modifier.testTag("confirm_reset_default_button")
                )
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetDefault = false }) {
                    Text("Batal", color = glass.textSecondary)
                }
            }
        )
    }

    // Add / Edit Task Dialog
    if (showEditorDialog) {
        TaskEditorDialog(
            initialTask = editingTask,
            onDismiss = { showEditorDialog = false },
            onSave = { periodStr, title, product, instruction, mins, isOptional, isActive, category, daysCsv, amount, area ->
                showEditorDialog = false
                val currentEditing = editingTask
                if (currentEditing == null) {
                    onAddCustomTask(
                        RoutinePeriod.fromId(periodStr),
                        title,
                        product,
                        instruction,
                        mins,
                        isOptional,
                        category,
                        daysCsv,
                        amount,
                        area
                    )
                } else {
                    onUpdateTask(
                        currentEditing,
                        title,
                        product,
                        instruction,
                        mins,
                        isOptional,
                        isActive,
                        category,
                        daysCsv,
                        amount,
                        area
                    )
                }
            }
        )
    }
}

@Composable
private fun EditableTaskRow(
    task: RoutineTaskEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    switchColors: androidx.compose.material3.SwitchColors
) {
    val glass = LiquidGlassTokens.colors
    val categoryLabel = ProductCategory.fromId(task.category).labelIndonesian
    val scheduleText = RoutineScheduleHelper.formatScheduledDaysLabel(task.scheduledDaysCsv, task.period)

    Surface(
        color = glass.glassSurfaceSecondary,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, glass.glassBorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_task_row_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProductBottleGlyph(
                productCategory = task.category,
                isActiveHighlight = task.category == ProductCategory.PRESCRIPTION_TOPICAL.id,
                size = 40.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$categoryLabel • $scheduleText",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.accentPrimaryBlue
                )
                Text(
                    text = "Takaran: ${task.usageAmount} | Area: ${task.applicationArea}",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.confirmationNote.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = glass.warningAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION,
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.warningAmber
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.testTag("move_up_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Geser ke atas",
                        tint = if (canMoveUp) glass.textPrimary else glass.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.testTag("move_down_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Geser ke bawah",
                        tint = if (canMoveDown) glass.textPrimary else glass.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Ubah langkah",
                        tint = glass.accentPrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Switch(
                    checked = task.isActive,
                    onCheckedChange = onToggleActive,
                    colors = switchColors,
                    modifier = Modifier.testTag("active_switch_${task.id}")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
    initialTask: RoutineTaskEntity?,
    onDismiss: () -> Unit,
    onSave: (
        period: String,
        title: String,
        productName: String,
        instruction: String,
        estimatedMinutes: Int,
        isOptional: Boolean,
        isActive: Boolean,
        category: String,
        scheduledDaysCsv: String,
        usageAmount: String,
        applicationArea: String
    ) -> Unit
) {
    val glass = LiquidGlassTokens.colors
    var period by rememberSaveable { mutableStateOf(initialTask?.period ?: RoutinePeriod.MORNING.id) }
    var title by rememberSaveable { mutableStateOf(initialTask?.title ?: "") }
    var productName by rememberSaveable { mutableStateOf(initialTask?.productName ?: "") }
    var instruction by rememberSaveable { mutableStateOf(initialTask?.instruction ?: "") }
    var estimatedMinsInput by rememberSaveable {
        mutableStateOf((initialTask?.estimatedMinutes ?: 2).toString())
    }
    var isOptional by rememberSaveable { mutableStateOf(initialTask?.isOptional ?: false) }
    var isActive by rememberSaveable { mutableStateOf(initialTask?.isActive ?: true) }
    var category by rememberSaveable {
        mutableStateOf(initialTask?.category ?: ProductCategory.SUPPORTIVE_SKINCARE.id)
    }

    val initialDaysSet: Set<Int> = (initialTask?.scheduledDaysCsv ?: "1,2,3,4,5,6,7")
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .toSet()
    var day1 by rememberSaveable { mutableStateOf(1 in initialDaysSet) }
    var day2 by rememberSaveable { mutableStateOf(2 in initialDaysSet) }
    var day3 by rememberSaveable { mutableStateOf(3 in initialDaysSet) }
    var day4 by rememberSaveable { mutableStateOf(4 in initialDaysSet) }
    var day5 by rememberSaveable { mutableStateOf(5 in initialDaysSet) }
    var day6 by rememberSaveable { mutableStateOf(6 in initialDaysSet) }
    var day7 by rememberSaveable { mutableStateOf(7 in initialDaysSet) }

    var usageAmount by rememberSaveable {
        mutableStateOf(initialTask?.usageAmount ?: RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION)
    }
    var applicationArea by rememberSaveable {
        mutableStateOf(initialTask?.applicationArea ?: RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = glass.glassSurfaceElevated,
        title = {
            Text(
                text = if (initialTask == null) "Tambah Langkah Baru" else "Edit Langkah Resep",
                fontWeight = FontWeight.Bold,
                color = glass.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = glass.warningSoftBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, glass.warningAmber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = RoutineScheduleHelper.CHEMICAL_PEELING_WARNING,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.warningAmber,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = period == RoutinePeriod.MORNING.id,
                        onClick = { period = RoutinePeriod.MORNING.id },
                        label = { Text("Pagi") },
                        modifier = Modifier.testTag("editor_period_morning_chip")
                    )
                    FilterChip(
                        selected = period == RoutinePeriod.EVENING.id,
                        onClick = { period = RoutinePeriod.EVENING.id },
                        label = { Text("Malam") },
                        modifier = Modifier.testTag("editor_period_evening_chip")
                    )
                }

                Text(
                    text = "Kategori Produk (Aturan 8):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ProductCategory.PRESCRIPTION_TOPICAL,
                        ProductCategory.SUPPORTIVE_SKINCARE,
                        ProductCategory.SUNSCREEN
                    ).forEach { cat ->
                        FilterChip(
                            selected = category == cat.id,
                            onClick = { category = cat.id },
                            label = { Text(cat.labelIndonesian) },
                            modifier = Modifier.testTag("editor_category_${cat.id.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Langkah") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input")
                )

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nama Produk Resep") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_product_input")
                )

                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("Instruksi Resep Dokter") },
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_instructions_input")
                )

                Text(
                    text = "Jadwal Hari Pemakaian (Aturan 2):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = glass.textPrimary
                )
                val dayItems = listOf(
                    Triple(1, "Sen", day1) to { v: Boolean -> day1 = v },
                    Triple(2, "Sel", day2) to { v: Boolean -> day2 = v },
                    Triple(3, "Rab", day3) to { v: Boolean -> day3 = v },
                    Triple(4, "Kam", day4) to { v: Boolean -> day4 = v },
                    Triple(5, "Jum", day5) to { v: Boolean -> day5 = v },
                    Triple(6, "Sab", day6) to { v: Boolean -> day6 = v },
                    Triple(7, "Min", day7) to { v: Boolean -> day7 = v }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayItems.forEach { (triple, setter) ->
                        val (dayNum, label, selected) = triple
                        FilterChip(
                            selected = selected,
                            onClick = { setter(!selected) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("editor_day_chip_$dayNum")
                        )
                    }
                }

                OutlinedTextField(
                    value = usageAmount,
                    onValueChange = { usageAmount = it },
                    label = { Text("Jumlah Pemakaian (Tulis 'Perlu konfirmasi ke dokter' bila belum jelas)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_usage_amount_input")
                )

                OutlinedTextField(
                    value = applicationArea,
                    onValueChange = { applicationArea = it },
                    label = { Text("Area Aplikasi (Tulis 'Perlu konfirmasi ke dokter' bila belum jelas)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_application_area_input")
                )

                OutlinedTextField(
                    value = estimatedMinsInput,
                    onValueChange = { estimatedMinsInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Estimasi Durasi / Jeda (Menit)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_wait_seconds_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = !isOptional,
                        onCheckedChange = { isOptional = !it },
                        colors = CheckboxDefaults.colors(checkedColor = glass.accentPrimaryBlue),
                        modifier = Modifier.testTag("editor_required_checkbox")
                    )
                    Text(
                        text = "Langkah wajib (dihitung dalam progres harian)",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textPrimary
                    )
                }
            }
        },
        confirmButton = {
            LiquidPrimaryActionButton(
                text = "Simpan",
                onClick = {
                    if (title.isNotBlank()) {
                        val selectedDays = buildList {
                            if (day1) add(1)
                            if (day2) add(2)
                            if (day3) add(3)
                            if (day4) add(4)
                            if (day5) add(5)
                            if (day6) add(6)
                            if (day7) add(7)
                        }
                        val daysCsv = if (selectedDays.isEmpty() || selectedDays.size == 7) {
                            "1,2,3,4,5,6,7"
                        } else {
                            selectedDays.joinToString(",")
                        }
                        onSave(
                            period,
                            title.trim(),
                            productName.trim().ifBlank { title.trim() },
                            instruction.trim(),
                            estimatedMinsInput.toIntOrNull() ?: 2,
                            isOptional,
                            isActive,
                            category,
                            daysCsv,
                            usageAmount.trim().ifBlank { RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION },
                            applicationArea.trim().ifBlank { RoutineScheduleHelper.NEEDS_DOCTOR_CONFIRMATION }
                        )
                    }
                },
                modifier = Modifier.testTag("editor_save_task_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = glass.textSecondary)
            }
        }
    )
}
