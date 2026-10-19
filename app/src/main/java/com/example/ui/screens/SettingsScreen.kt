package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.RoutineTaskEntity
import com.example.data.preferences.ThemeMode
import com.example.domain.RoutinePeriod
import com.example.ui.SkinCareUiState

@Composable
fun SettingsScreen(
    uiState: SkinCareUiState,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleHideSensitiveNotifications: (Boolean) -> Unit,
    onOpenReminderSettings: () -> Unit,
    onAddCustomTask: (RoutinePeriod, String, String, String, Int, Boolean) -> Unit,
    onUpdateTask: (RoutineTaskEntity, String, String, String, Int, Boolean, Boolean) -> Unit,
    onMoveTaskOrder: (String, Boolean) -> Unit,
    onRetireTask: (RoutineTaskEntity) -> Unit,
    onRestoreTaskActive: (RoutineTaskEntity) -> Unit,
    onRestoreDermatologistDefaultRoutine: () -> Unit,
    onExportJson: ((String) -> Unit) -> Unit,
    onImportJson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<RoutineTaskEntity?>(null) }
    var retiringTask by remember { mutableStateOf<RoutineTaskEntity?>(null) }
    var showRestoreDefaultConfirm by rememberSaveable { mutableStateOf(false) }
    var exportedJsonText by rememberSaveable { mutableStateOf<String?>(null) }
    var showImportJsonDialog by rememberSaveable { mutableStateOf(false) }

    val activeTasks = uiState.allTasksForEditor.filter { it.isActive }
    val retiredTasks = uiState.allTasksForEditor.filter { !it.isActive }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "settings_header") {
            Column {
                Text(
                    text = "Pengaturan & Editor Rutinitas",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sesuaikan tampilan, urutan langkah perawatan, privasi notifikasi, dan cadangan data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Appearance & Lock-Screen Notification Privacy
        item(key = "appearance_privacy_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tampilan & Privasi Layar Kunci",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "Tema Aplikasi:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.settings.themeMode == mode,
                                onClick = { onSelectTheme(mode) },
                                label = { Text(mode.labelId) },
                                modifier = Modifier.testTag("theme_chip_${mode.id}")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sembunyikan Nama Produk & Obat di Layar Kunci",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Menampilkan pesan pengingat umum tanpa menyebutkan nama krim atau Dohixat 100 mg pada notifikasi terkunci.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.hideSensitiveNotificationContent,
                            onCheckedChange = onToggleHideSensitiveNotifications,
                            modifier = Modifier.testTag("hide_sensitive_notif_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenReminderSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_open_reminders_button")
                    ) {
                        Text("Buka Pengaturan Pengingat & Alarm")
                    }
                }
            }
        }

        // 2. Routine Editor Header & Actions
        item(key = "routine_editor_header") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Editor Rutinitas Skincare",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Anda dapat mengubah urutan langkah, menambah tugas khusus, atau menonaktifkan langkah tanpa merusak catatan riwayat masa lalu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddTaskDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_custom_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Langkah")
                        }

                        OutlinedButton(
                            onClick = { showRestoreDefaultConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_default_routine_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset ke Resep")
                        }
                    }
                }
            }
        }

        // 3. Active Routine Tasks List (Reorder / Edit / Retire)
        items(
            items = activeTasks,
            key = { "editor_active_${it.id}" }
        ) { task ->
            RoutineEditorTaskRow(
                task = task,
                onMoveUp = { onMoveTaskOrder(task.id, true) },
                onMoveDown = { onMoveTaskOrder(task.id, false) },
                onEdit = { editingTask = task },
                onRetire = { retiringTask = task }
            )
        }

        // 4. Retired / Inactive Tasks Section (Can be re-enabled anytime)
        if (retiredTasks.isNotEmpty()) {
            item(key = "retired_header") {
                Text(
                    text = "Langkah Nonaktif / Diarsipkan (${retiredTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(
                items = retiredTasks,
                key = { "editor_retired_${it.id}" }
            ) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                                text = task.productName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { onRestoreTaskActive(task) },
                            modifier = Modifier.testTag("restore_task_btn_${task.id}")
                        ) {
                            Text("Aktifkan")
                        }
                    }
                }
            }
        }

        // 5. Local Storage Explanation & JSON Backup Export/Import
        item(key = "storage_and_backup_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_storage_info_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Penyimpanan Data Lokal & Cadangan",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "Seluruh rutinitas, status centang harian, riwayat kepatuhan, dan jadwal pengingat disimpan secara permanen di database lokal perangkat (SQLite Room & Jetpack DataStore), bukan di direktori cache sementara.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Penting: Karena aplikasi bekerja 100% offline tanpa akun cloud demi privasi kesehatan Anda, data lokal dapat hilang apabila Anda memilih 'Hapus Data' (Clear Storage) di pengaturan sistem Android atau menghapus instalasi (uninstall) aplikasi. Gunakan fitur Ekspor JSON di bawah ini untuk menyimpan salinan cadangan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                onExportJson { json ->
                                    exportedJsonText = json
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor JSON")
                        }

                        OutlinedButton(
                            onClick = { showImportJsonDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor JSON")
                        }
                    }
                }
            }
        }

        // 6. Medical Disclaimer & Version Footer
        item(key = "disclaimer_footer") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SkinCare Daily v1.0.0 • Offline-First",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Text(
                        text = "Penafian Medis: Aplikasi ini merupakan alat bantu pencatatan dan pengingat pribadi berdasarkan instruksi resep dokter kulit tertanggal 19 Oktober 2026. Aplikasi ini tidak menggantikan konsultasi, diagnosis, atau advis medis profesional. Segera hubungi dokter kulit atau apoteker Anda bila terjadi iritasi atau keraguan dosis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAddTaskDialog) {
        TaskEditorDialog(
            initialTask = null,
            onDismiss = { showAddTaskDialog = false },
            onSave = { period, title, product, instruction, mins, isOptional ->
                showAddTaskDialog = false
                onAddCustomTask(period, title, product, instruction, mins, isOptional)
            }
        )
    }

    editingTask?.let { taskToEdit ->
        TaskEditorDialog(
            initialTask = taskToEdit,
            onDismiss = { editingTask = null },
            onSave = { _, title, product, instruction, mins, isOptional ->
                editingTask = null
                onUpdateTask(
                    taskToEdit,
                    title,
                    product,
                    instruction,
                    mins,
                    isOptional,
                    taskToEdit.isActive
                )
            }
        )
    }

    retiringTask?.let { taskToRetire ->
        AlertDialog(
            onDismissRequest = { retiringTask = null },
            title = { Text("Nonaktifkan Langkah Ini?") },
            text = {
                Text(
                    "Langkah \"${taskToRetire.title}\" akan disembunyikan dari daftar harian mulai hari ini, namun seluruh riwayat penyelesaian di hari-hari sebelumnya tetap tersimpan utuh."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        retiringTask = null
                        onRetireTask(taskToRetire)
                    },
                    modifier = Modifier.testTag("confirm_retire_task_button")
                ) {
                    Text("Nonaktifkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { retiringTask = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showRestoreDefaultConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreDefaultConfirm = false },
            title = { Text("Pulihkan Rutinitas Awal Dokter?") },
            text = {
                Text(
                    "Urutan dan daftar tugas akan dikembalikan sesuai resep dokter kulit tertanggal 19 Oktober 2026 (Pagi 4 langkah, Malam 3 langkah wajib + Effaclar Duo+M opsional). Catatan riwayat Anda tidak akan terhapus."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDefaultConfirm = false
                        onRestoreDermatologistDefaultRoutine()
                    },
                    modifier = Modifier.testTag("confirm_restore_default_button")
                ) {
                    Text("Pulihkan Resep Awal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDefaultConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    exportedJsonText?.let { json ->
        AlertDialog(
            onDismissRequest = { exportedJsonText = null },
            title = { Text("Cadangan Data JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Salin teks JSON di bawah ini dan simpan di tempat aman:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exported_json_text_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("skincare_daily_backup", json))
                        exportedJsonText = null
                    },
                    modifier = Modifier.testTag("copy_exported_json_button")
                ) {
                    Text("Salin & Tutup")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedJsonText = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showImportJsonDialog) {
        var jsonInput by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("Impor Cadangan JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tempelkan data JSON cadangan SkinCare Daily untuk memulihkan tugas, riwayat, dan pengaturan:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        placeholder = { Text("{ \"schemaVersion\": 2, ... }") },
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_json_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonInput.isNotBlank()) {
                            onImportJson(jsonInput)
                            showImportJsonDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_import_json_button")
                ) {
                    Text("Impor Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun RoutineEditorTaskRow(
    task: RoutineTaskEntity,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onRetire: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (task.period == RoutinePeriod.MORNING.id) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = if (task.period == RoutinePeriod.MORNING.id) "Pagi #${task.sortOrder}" else "Malam #${task.sortOrder}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (task.isOptional) {
                        Text(
                            text = "• Opsional",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (task.isScheduledActive) {
                        "Otomatis: Pharmalene Gel (Sen/Rab/Jum) / Serum Labore (Sel/Kam/Sab/Min)"
                    } else {
                        task.productName
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("move_up_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Geser ke atas",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("move_down_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Geser ke bawah",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("edit_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Ubah langkah",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRetire,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("retire_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Nonaktifkan langkah",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskEditorDialog(
    initialTask: RoutineTaskEntity?,
    onDismiss: () -> Unit,
    onSave: (RoutinePeriod, String, String, String, Int, Boolean) -> Unit
) {
    var period by rememberSaveable {
        mutableStateOf(
            if (initialTask != null) RoutinePeriod.fromId(initialTask.period) else RoutinePeriod.MORNING
        )
    }
    var title by rememberSaveable { mutableStateOf(initialTask?.title ?: "") }
    var productName by rememberSaveable { mutableStateOf(initialTask?.productName ?: "") }
    var instruction by rememberSaveable { mutableStateOf(initialTask?.instruction ?: "") }
    var estimatedMinutes by rememberSaveable {
        mutableStateOf((initialTask?.estimatedMinutes ?: 2).toString())
    }
    var isOptional by rememberSaveable { mutableStateOf(initialTask?.isOptional ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialTask == null) "Tambah Langkah Skincare" else "Ubah Langkah Skincare")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialTask == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoutinePeriod.entries.forEach { p ->
                            FilterChip(
                                selected = period == p,
                                onClick = { period = p },
                                label = { Text("Rutinitas ${p.labelId}") },
                                modifier = Modifier.testTag("task_dialog_period_${p.id}")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Langkah (mis. Pelembap)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_dialog_title_input")
                )

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nama Produk") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_dialog_product_input")
                )

                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("Petunjuk Penggunaan") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_dialog_instruction_input")
                )

                OutlinedTextField(
                    value = estimatedMinutes,
                    onValueChange = { estimatedMinutes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Estimasi Durasi (Menit)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_dialog_minutes_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isOptional,
                        onCheckedChange = { isOptional = it },
                        modifier = Modifier.testTag("task_dialog_optional_checkbox")
                    )
                    Text(
                        text = "Langkah Opsional (tidak mengurangi persentase harian bila dilewati)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && productName.isNotBlank()) {
                        val mins = estimatedMinutes.toIntOrNull()?.coerceIn(1, 30) ?: 2
                        onSave(
                            period,
                            title,
                            productName,
                            instruction.ifBlank { "Gunakan sesuai arahan dokter kulit." },
                            mins,
                            isOptional
                        )
                    }
                },
                enabled = title.isNotBlank() && productName.isNotBlank(),
                modifier = Modifier.testTag("task_dialog_save_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
