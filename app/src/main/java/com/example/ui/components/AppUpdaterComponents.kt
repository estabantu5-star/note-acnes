package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LiquidGlassTokens
import com.example.update.AppUpdateConstants
import com.example.update.AppUpdateUiState
import com.example.update.GitHubReleaseAsset
import com.example.update.GitHubReleaseInfo
import com.example.update.UpdateStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val lastCheckedFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("id", "ID"))

private fun formatEpochMillisReadable(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Belum pernah diperiksa"
    return try {
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(lastCheckedFormatter)
    } catch (_: Exception) {
        "Baru saja"
    }
}

/**
 * Non-intrusive Blue Liquid Glass banner shown when the 24-hour background check detects a new version.
 */
@Composable
fun NonIntrusiveUpdateAvailableBanner(
    appUpdateState: AppUpdateUiState,
    onOpenUpdateDialog: () -> Unit,
    onPostponeUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LiquidGlassTokens.colors
    val updateAvailable = appUpdateState.status as? UpdateStatus.UpdateAvailable
    val readyToInstall = appUpdateState.status as? UpdateStatus.ReadyToInstall
    val releaseInfo = updateAvailable?.releaseInfo ?: readyToInstall?.releaseInfo
    val apkAsset = updateAvailable?.apkAsset ?: readyToInstall?.apkAsset

    val visible = appUpdateState.showAutoUpdateBanner && releaseInfo != null && apkAsset != null

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        if (releaseInfo != null && apkAsset != null) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("auto_update_banner"),
                cornerRadius = 22.dp,
                isElevated = true,
                tintColor = glass.accentSoftGlow,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = "Update Tersedia",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Tersedia • ${releaseInfo.tagName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "${apkAsset.name} (${apkAsset.formattedSizeMb}) siap diunduh",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = onPostponeUpdate,
                        modifier = Modifier.testTag("banner_postpone_update_button")
                    ) {
                        Text(
                            text = "Nanti",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textSecondary
                        )
                    }

                    LiquidPrimaryActionButton(
                        text = "Lihat",
                        onClick = onOpenUpdateDialog,
                        modifier = Modifier.testTag("banner_open_update_button")
                    )
                }
            }
        }
    }
}

/**
 * Blue Liquid Glass card for "Tentang Aplikasi & Pembaruan APK (GitHub Releases)" inside SettingsScreen.
 */
@Composable
fun AboutAndUpdateGlassCard(
    appUpdateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstallDownloadedApk: () -> Unit,
    onOpenUnknownSourcesSettings: () -> Unit,
    onOpenUpdateDialog: () -> Unit,
    onPostponeUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LiquidGlassTokens.colors
    val status = appUpdateState.status

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("about_and_update_card"),
        cornerRadius = 26.dp,
        isElevated = true,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Card Header: App Name, Installed Version, and GitHub Releases badge
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        glass.accentPrimaryBlue.copy(alpha = 0.22f),
                                        glass.accentElectricCyan.copy(alpha = 0.28f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = glass.accentElectricCyan.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = glass.accentPrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tentang & Pembaruan Aplikasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "SkinCare Daily • Distribusi GitHub Releases",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
            }

            // Installed Version & Last Checked Metadata Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = glass.glassSurfaceSecondary,
                    border = BorderStroke(1.dp, glass.glassBorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Versi Terpasang",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Text(
                            text = appUpdateState.displayInstalledVersion,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = glass.accentPrimaryBlue,
                            modifier = Modifier.testTag("installed_version_text")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = glass.glassSurfaceSecondary,
                    border = BorderStroke(1.dp, glass.glassBorderSubtle),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Pengecekan Terakhir",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Text(
                            text = formatEpochMillisReadable(appUpdateState.lastCheckedAtMillis),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("last_update_check_text")
                        )
                    }
                }
            }

            // Dynamic Status Display for all 7 Updater States
            when (status) {
                is UpdateStatus.Idle -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = glass.glassSurfaceSecondary.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, glass.glassBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_status_idle")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = glass.accentPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Tekan Cek Update Aplikasi untuk memeriksa rilis APK terbaru dari repositori GitHub (${AppUpdateConstants.GITHUB_OWNER}/${AppUpdateConstants.GITHUB_REPO}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }
                }

                is UpdateStatus.Checking -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = glass.accentSoftGlow,
                        border = BorderStroke(1.dp, glass.accentElectricCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_status_checking")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = glass.accentPrimaryBlue
                            )
                            Column {
                                Text(
                                    text = "Sedang memeriksa versi terbaru...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "Menghubungi GitHub Releases API (${AppUpdateConstants.GITHUB_REPO})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }

                is UpdateStatus.UpToDate -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = glass.successSoftBg,
                        border = BorderStroke(1.dp, glass.successMint.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_status_up_to_date")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = glass.successMint,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aplikasi sudah menggunakan versi terbaru (v${status.installedVersion.removePrefix("v")}).",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = glass.textPrimary
                                )
                                status.latestRelease?.let { rel ->
                                    Text(
                                        text = "Rilis publik terakhir di GitHub: ${rel.tagName} (${rel.formattedPublishedDate})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                is UpdateStatus.UpdateAvailable -> {
                    UpdateAvailableInlineSection(
                        releaseInfo = status.releaseInfo,
                        apkAsset = status.apkAsset,
                        isAlreadyDownloaded = !status.downloadedFilePath.isNullOrBlank(),
                        onDownloadAndInstall = onStartDownload,
                        onOpenDetailsDialog = onOpenUpdateDialog,
                        onPostpone = onPostponeUpdate
                    )
                }

                is UpdateStatus.Downloading -> {
                    DownloadingProgressSection(
                        status = status,
                        onCancelDownload = onCancelDownload
                    )
                }

                is UpdateStatus.ReadyToInstall -> {
                    ReadyToInstallSection(
                        status = status,
                        onInstallNow = onInstallDownloadedApk,
                        onOpenPermissionSettings = onOpenUnknownSourcesSettings,
                        onOpenDetailsDialog = onOpenUpdateDialog
                    )
                }

                is UpdateStatus.Error -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = glass.dangerSoftBg,
                        border = BorderStroke(1.dp, glass.dangerCoral.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_status_error")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = glass.dangerCoral,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Gagal Memproses Pembaruan",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = glass.textPrimary
                                    )
                                    Text(
                                        text = status.userMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiquidPrimaryActionButton(
                                    text = if (status.releaseInfo?.apkAsset != null) "Coba Unduh Lagi" else "Coba Lagi",
                                    onClick = {
                                        if (status.releaseInfo?.apkAsset != null) {
                                            onStartDownload()
                                        } else {
                                            onCheckForUpdate()
                                        }
                                    },
                                    leadingIcon = Icons.Outlined.Refresh,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("retry_update_button")
                                )
                                LiquidSecondaryGlassButton(
                                    text = "Buka GitHub",
                                    onClick = {
                                        runCatching {
                                            val url = status.releaseInfo?.htmlUrl ?: AppUpdateConstants.RELEASES_WEB_URL
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }
                                    },
                                    leadingIcon = Icons.Outlined.OpenInBrowser,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Primary Check for Updates Button (enabled whenever not actively checking or downloading)
            val isBusy = status is UpdateStatus.Checking || status is UpdateStatus.Downloading
            if (!isBusy) {
                LiquidPrimaryActionButton(
                    text = "Cek Update Aplikasi",
                    onClick = onCheckForUpdate,
                    leadingIcon = Icons.Outlined.Refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_update_button")
                )
            }
        }
    }
}

@Composable
private fun UpdateAvailableInlineSection(
    releaseInfo: GitHubReleaseInfo,
    apkAsset: GitHubReleaseAsset,
    isAlreadyDownloaded: Boolean,
    onDownloadAndInstall: () -> Unit,
    onOpenDetailsDialog: () -> Unit,
    onPostpone: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = glass.accentSoftGlow,
        border = BorderStroke(1.dp, glass.accentElectricCyan.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("update_status_available")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = glass.accentPrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Update Tersedia: ${releaseInfo.tagName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "${apkAsset.name} • ${apkAsset.formattedSizeMb} • Rilis ${releaseInfo.formattedPublishedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }

            Text(
                text = releaseInfo.releaseNotes,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidPrimaryActionButton(
                    text = if (isAlreadyDownloaded) "Pasang Update" else "Unduh & Pasang Update",
                    onClick = onDownloadAndInstall,
                    leadingIcon = if (isAlreadyDownloaded) Icons.Outlined.InstallMobile else Icons.Outlined.CloudDownload,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("download_install_update_button")
                )
                LiquidSecondaryGlassButton(
                    text = "Detail",
                    onClick = onOpenDetailsDialog,
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("open_update_details_button")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onPostpone,
                    modifier = Modifier.testTag("inline_postpone_update_button")
                ) {
                    Text(
                        text = "Nanti Saja",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadingProgressSection(
    status: UpdateStatus.Downloading,
    onCancelDownload: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    val downloadedMb = GitHubReleaseAsset.formatBytesAsMb(status.bytesDownloaded)
    val totalMb = if (status.totalBytes > 0L) {
        GitHubReleaseAsset.formatBytesAsMb(status.totalBytes)
    } else {
        "..."
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = glass.accentSoftGlow,
        border = BorderStroke(1.dp, glass.accentElectricCyan.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("update_status_downloading")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mengunduh ${status.apkAsset.name} (${status.releaseInfo.tagName})...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "$downloadedMb / $totalMb",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
                Text(
                    text = "${status.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = glass.accentPrimaryBlue,
                    modifier = Modifier.testTag("download_progress_percent_text")
                )
            }

            if (status.totalBytes > 0L) {
                LinearProgressIndicator(
                    progress = { (status.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = glass.accentPrimaryBlue,
                    trackColor = glass.glassSurfaceSecondary
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = glass.accentPrimaryBlue,
                    trackColor = glass.glassSurfaceSecondary
                )
            }

            LiquidSecondaryGlassButton(
                text = "Batalkan Unduhan",
                onClick = onCancelDownload,
                leadingIcon = Icons.Outlined.Cancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cancel_update_download_button")
            )
        }
    }
}

@Composable
private fun ReadyToInstallSection(
    status: UpdateStatus.ReadyToInstall,
    onInstallNow: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onOpenDetailsDialog: () -> Unit
) {
    val glass = LiquidGlassTokens.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (status.requiresInstallPermission) glass.warningSoftBg else glass.successSoftBg,
        border = BorderStroke(
            1.dp,
            if (status.requiresInstallPermission) {
                glass.warningAmber.copy(alpha = 0.5f)
            } else {
                glass.successMint.copy(alpha = 0.5f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("update_status_ready_to_install")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (status.requiresInstallPermission) {
                        Icons.Outlined.Security
                    } else {
                        Icons.Outlined.InstallMobile
                    },
                    contentDescription = null,
                    tint = if (status.requiresInstallPermission) glass.warningAmber else glass.successMint,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (status.requiresInstallPermission) {
                            "Izin Instalasi APK Diperlukan (${status.releaseInfo.tagName})"
                        } else {
                            "APK ${status.releaseInfo.tagName} Siap Dipasang"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = if (status.requiresInstallPermission) {
                            "File APK telah diverifikasi utuh (${status.apkAsset.formattedSizeMb}). Aktifkan izin \"Instal aplikasi yang tidak dikenal\" pada Android agar update dapat dipasang."
                        } else {
                            "File ${status.apkAsset.name} (${status.apkAsset.formattedSizeMb}) telah terunduh dan lolos verifikasi. Data rutinitas Anda tidak akan hilang."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }

            if (status.requiresInstallPermission) {
                LiquidPrimaryActionButton(
                    text = "Buka Pengaturan Izin Install",
                    onClick = onOpenPermissionSettings,
                    leadingIcon = Icons.Outlined.Security,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_install_permission_button")
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidPrimaryActionButton(
                        text = "Pasang Update Sekarang",
                        onClick = onInstallNow,
                        leadingIcon = Icons.Outlined.InstallMobile,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("install_ready_apk_button")
                    )
                    LiquidSecondaryGlassButton(
                        text = "Detail",
                        onClick = onOpenDetailsDialog,
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Blue Liquid Glass Modal Dialog displaying complete GitHub Release details, release notes,
 * download progress, and install/permission controls.
 */
@Composable
fun AppUpdateAvailableDialog(
    appUpdateState: AppUpdateUiState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstallDownloadedApk: () -> Unit,
    onOpenUnknownSourcesSettings: () -> Unit,
    onPostponeUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!appUpdateState.showUpdateDialog) return

    val status = appUpdateState.status
    val releaseInfo: GitHubReleaseInfo? = when (status) {
        is UpdateStatus.UpdateAvailable -> status.releaseInfo
        is UpdateStatus.Downloading -> status.releaseInfo
        is UpdateStatus.ReadyToInstall -> status.releaseInfo
        is UpdateStatus.Error -> status.releaseInfo
        else -> null
    }
    val apkAsset: GitHubReleaseAsset? = when (status) {
        is UpdateStatus.UpdateAvailable -> status.apkAsset
        is UpdateStatus.Downloading -> status.apkAsset
        is UpdateStatus.ReadyToInstall -> status.apkAsset
        is UpdateStatus.Error -> status.releaseInfo?.apkAsset
        else -> null
    }

    if (releaseInfo == null || apkAsset == null) return

    val glass = LiquidGlassTokens.colors

    AlertDialog(
        onDismissRequest = {
            if (status !is UpdateStatus.Downloading) {
                onDismiss()
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = glass.glassSurfaceElevated,
        modifier = Modifier.testTag("app_update_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(glass.accentPrimaryBlue, glass.accentElectricCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Update Tersedia",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = releaseInfo.releaseTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Version Comparison Row (Installed vs Latest)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = glass.glassSurfaceSecondary,
                        border = BorderStroke(1.dp, glass.glassBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Versi Saat Ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary
                            )
                            Text(
                                text = "v${appUpdateState.installedVersionName.removePrefix("v")}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = glass.textPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = glass.accentSoftGlow,
                        border = BorderStroke(1.dp, glass.accentElectricCyan.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Versi Terbaru",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentPrimaryBlue
                            )
                            Text(
                                text = releaseInfo.tagName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = glass.accentPrimaryBlue,
                                modifier = Modifier.testTag("dialog_latest_version_text")
                            )
                        }
                    }
                }

                // File Size & Release Date metadata
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = glass.glassSurfaceSecondary.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, glass.glassBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "File APK: ${apkAsset.name} (${apkAsset.formattedSizeMb})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "Tanggal Rilis: ${releaseInfo.formattedPublishedDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }

                // Release Notes (body)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Catatan Perubahan (Release Notes)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = glass.glassSurfaceSecondary.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, glass.glassBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp, max = 180.dp)
                    ) {
                        Text(
                            text = releaseInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Live Download Progress inside Dialog if currently downloading
                if (status is UpdateStatus.Downloading) {
                    val dlMb = GitHubReleaseAsset.formatBytesAsMb(status.bytesDownloaded)
                    val totMb = GitHubReleaseAsset.formatBytesAsMb(status.totalBytes)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Mengunduh APK ($dlMb / $totMb)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "${status.progressPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = glass.accentPrimaryBlue
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (status.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = glass.accentPrimaryBlue,
                            trackColor = glass.glassSurfaceSecondary
                        )
                    }
                }

                // Unknown App Sources Permission Notice on Android 8.0+
                if (status is UpdateStatus.ReadyToInstall && status.requiresInstallPermission) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = glass.warningSoftBg,
                        border = BorderStroke(1.dp, glass.warningAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Android memerlukan izin \"Instal aplikasi yang tidak dikenal\" untuk memasang pembaruan dari luar Play Store. Tekan tombol di bawah untuk mengaktifkannya, lalu instalasi akan dilanjutkan otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = glass.warningAmber,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                HorizontalDivider(color = glass.glassBorderSubtle)

                // Data Preservation Reassurance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = glass.successMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Data rutinitas pagi/malam, riwayat, timer, dan pengingat Anda tetap aman dan tidak akan terhapus.",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textSecondary
                    )
                }
            }
        },
        confirmButton = {
            when (status) {
                is UpdateStatus.Downloading -> {
                    LiquidSecondaryGlassButton(
                        text = "Batalkan Unduhan",
                        onClick = onCancelDownload,
                        leadingIcon = Icons.Outlined.Cancel,
                        modifier = Modifier.testTag("dialog_cancel_download_button")
                    )
                }

                is UpdateStatus.ReadyToInstall -> {
                    if (status.requiresInstallPermission) {
                        LiquidPrimaryActionButton(
                            text = "Izinkan Instalasi APK",
                            onClick = onOpenUnknownSourcesSettings,
                            leadingIcon = Icons.Outlined.Security,
                            modifier = Modifier.testTag("dialog_grant_install_permission_button")
                        )
                    } else {
                        LiquidPrimaryActionButton(
                            text = "Pasang Update Sekarang",
                            onClick = onInstallDownloadedApk,
                            leadingIcon = Icons.Outlined.InstallMobile,
                            modifier = Modifier.testTag("dialog_install_now_button")
                        )
                    }
                }

                else -> {
                    LiquidPrimaryActionButton(
                        text = "Unduh & Pasang Update",
                        onClick = onStartDownload,
                        leadingIcon = Icons.Outlined.CloudDownload,
                        modifier = Modifier.testTag("dialog_download_install_update_button")
                    )
                }
            }
        },
        dismissButton = {
            if (status !is UpdateStatus.Downloading) {
                TextButton(
                    onClick = onPostponeUpdate,
                    modifier = Modifier.testTag("dialog_postpone_update_button")
                ) {
                    Text(
                        text = "Nanti",
                        fontWeight = FontWeight.SemiBold,
                        color = glass.textSecondary
                    )
                }
            }
        }
    )
}
