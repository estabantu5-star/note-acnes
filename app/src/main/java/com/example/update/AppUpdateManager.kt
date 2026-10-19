package com.example.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.Locale

fun interface HttpConnectionFactory {
    @Throws(IOException::class)
    fun openConnection(url: String): HttpURLConnection
}

class AppUpdateManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectionFactory: HttpConnectionFactory = HttpConnectionFactory { urlString ->
        (URL(urlString).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
        }
    },
    private val installedVersionNameProvider: () -> String = { BuildConfig.VERSION_NAME },
    private val installedVersionCodeProvider: () -> Int = { BuildConfig.VERSION_CODE },
    private val archivePackageValidator: (Context, File) -> Boolean = ::defaultArchivePackageValidator
) {

    val installedVersionName: String
        get() = installedVersionNameProvider()

    val installedVersionCode: Int
        get() = installedVersionCodeProvider()

    /**
     * Directory for storing downloaded update APKs safely inside app-specific storage.
     */
    fun getUpdatesDirectory(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
            ?: context.cacheDir
        val updatesDir = File(baseDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }
        return updatesDir
    }

    /**
     * Builds the target APK file path for a specific release tag (e.g. SkinCare-Daily-v1.0.5.apk).
     */
    fun getTargetApkFile(tagName: String): File {
        val safeTag = tagName.trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "latest" }
        val fileName = if (safeTag.startsWith("v", ignoreCase = true)) {
            "SkinCare-Daily-$safeTag.apk"
        } else {
            "SkinCare-Daily-v$safeTag.apk"
        }
        return File(getUpdatesDirectory(), fileName)
    }

    /**
     * Checks if a valid, already-downloaded APK exists for the given release and asset.
     */
    fun getExistingValidApkFile(
        releaseInfo: GitHubReleaseInfo,
        apkAsset: GitHubReleaseAsset
    ): File? {
        val targetFile = getTargetApkFile(releaseInfo.tagName)
        if (!targetFile.exists() || targetFile.length() <= 0L) return null
        if (apkAsset.sizeBytes > 0L && targetFile.length() != apkAsset.sizeBytes) return null
        if (!archivePackageValidator(context, targetFile)) return null
        return targetFile
    }

    /**
     * Deletes old APK and temporary files in the updates directory so device storage stays clean.
     */
    fun cleanupOldUpdateFiles(keepFileName: String? = null) {
        val dir = getUpdatesDirectory()
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isFile && (file.name.endsWith(".apk", ignoreCase = true) || file.name.endsWith(".tmp", ignoreCase = true))) {
                if (keepFileName == null || !file.name.equals(keepFileName, ignoreCase = true)) {
                    file.delete()
                }
            }
        }
    }

    /**
     * Queries GitHub Releases API (`GET /repos/estabantu5-star/note-acnes/releases/latest`)
     * and determines whether an update is available.
     */
    suspend fun checkForUpdate(isManual: Boolean = true): UpdateStatus = withContext(ioDispatcher) {
        var connection: HttpURLConnection? = null
        try {
            connection = connectionFactory.openConnection(AppUpdateConstants.LATEST_RELEASE_API_URL).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                useCaches = false
                setRequestProperty("Accept", AppUpdateConstants.ACCEPT_HEADER)
                setRequestProperty("User-Agent", AppUpdateConstants.USER_AGENT)
            }

            val responseCode = connection.responseCode
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                        it.readText()
                    }
                    evaluateReleaseResponse(responseBody)
                }

                HttpURLConnection.HTTP_FORBIDDEN, 429 -> {
                    val remaining = connection.getHeaderField("X-RateLimit-Remaining")
                    val message = if (remaining == "0" || responseCode == 429) {
                        "Batas permintaan GitHub API tercapai (Rate Limit). Silakan tunggu beberapa menit lalu coba lagi."
                    } else {
                        "Akses ke GitHub Releases dibatasi sementara (HTTP $responseCode). Silakan coba lagi nanti."
                    }
                    UpdateStatus.Error(
                        errorType = UpdateErrorType.RATE_LIMITED,
                        userMessage = message,
                        technicalDetail = "HTTP $responseCode"
                    )
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    UpdateStatus.Error(
                        errorType = UpdateErrorType.NO_RELEASE_FOUND,
                        userMessage = "Belum ada rilis publik yang tersedia di repositori GitHub SkinCare Daily.",
                        technicalDetail = "HTTP 404 Not Found"
                    )
                }

                else -> {
                    UpdateStatus.Error(
                        errorType = UpdateErrorType.UNKNOWN,
                        userMessage = "Gagal memeriksa pembaruan dari GitHub (Kode HTTP $responseCode).",
                        technicalDetail = "HTTP $responseCode"
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnknownHostException) {
            UpdateStatus.Error(
                errorType = UpdateErrorType.NO_INTERNET,
                userMessage = "Tidak ada koneksi internet. Periksa jaringan Wi-Fi atau data seluler Anda lalu coba lagi.",
                technicalDetail = e.localizedMessage
            )
        } catch (e: SocketTimeoutException) {
            UpdateStatus.Error(
                errorType = UpdateErrorType.NO_INTERNET,
                userMessage = "Waktu koneksi ke server GitHub habis (Timeout). Pastikan koneksi internet stabil.",
                technicalDetail = e.localizedMessage
            )
        } catch (e: IOException) {
            UpdateStatus.Error(
                errorType = UpdateErrorType.NO_INTERNET,
                userMessage = "Gagal terhubung ke GitHub Releases. Periksa koneksi internet Anda.",
                technicalDetail = e.localizedMessage
            )
        } catch (e: Exception) {
            UpdateStatus.Error(
                errorType = UpdateErrorType.UNKNOWN,
                userMessage = "Terjadi kesalahan saat membaca informasi rilis terbaru.",
                technicalDetail = e.localizedMessage
            )
        } finally {
            connection?.disconnect()
        }
    }

    internal fun evaluateReleaseResponse(responseBody: String): UpdateStatus {
        val releaseInfo = GitHubReleaseParser.parseReleaseJson(responseBody)

        // Rule 1: Ignore release if draft == true or prerelease == true
        if (releaseInfo.isDraft || releaseInfo.isPrerelease) {
            return UpdateStatus.UpToDate(
                installedVersion = installedVersionName,
                latestRelease = releaseInfo
            )
        }

        val currentVersion = installedVersionName
        val isNewer = SemanticVersionComparator.isRemoteNewer(
            installedVersion = currentVersion,
            remoteVersion = releaseInfo.tagName
        )

        if (!isNewer) {
            return UpdateStatus.UpToDate(
                installedVersion = currentVersion,
                latestRelease = releaseInfo
            )
        }

        // Update is newer: verify APK asset exists
        val apkAsset = releaseInfo.apkAsset
            ?: return UpdateStatus.Error(
                errorType = UpdateErrorType.NO_APK_ASSET,
                userMessage = "Versi baru (${releaseInfo.tagName}) ditemukan, tetapi file installer APK belum tersedia pada rilis tersebut.",
                releaseInfo = releaseInfo
            )

        // Validate download URL domain & HTTPS
        if (!GitHubReleaseParser.isTrustedGitHubDownloadUrl(apkAsset.browserDownloadUrl)) {
            return UpdateStatus.Error(
                errorType = UpdateErrorType.UNTRUSTED_DOWNLOAD_URL,
                userMessage = "URL unduhan APK tidak berasal dari domain HTTPS resmi GitHub sehingga diblokir demi keamanan.",
                releaseInfo = releaseInfo
            )
        }

        val existingFile = getExistingValidApkFile(releaseInfo, apkAsset)
        return UpdateStatus.UpdateAvailable(
            releaseInfo = releaseInfo,
            apkAsset = apkAsset,
            downloadedFilePath = existingFile?.absolutePath
        )
    }

    /**
     * Downloads the APK from GitHub Releases with real-time progress updates and full integrity validation.
     */
    suspend fun downloadUpdateApk(
        releaseInfo: GitHubReleaseInfo,
        apkAsset: GitHubReleaseAsset,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, progressPercent: Int) -> Unit
    ): UpdateStatus = withContext(ioDispatcher) {
        if (!GitHubReleaseParser.isTrustedGitHubDownloadUrl(apkAsset.browserDownloadUrl)) {
            return@withContext UpdateStatus.Error(
                errorType = UpdateErrorType.UNTRUSTED_DOWNLOAD_URL,
                userMessage = "URL unduhan tidak valid atau bukan HTTPS resmi GitHub.",
                releaseInfo = releaseInfo
            )
        }

        // Reuse existing validated APK if already downloaded completely
        getExistingValidApkFile(releaseInfo, apkAsset)?.let { validFile ->
            onProgress(validFile.length(), validFile.length(), 100)
            return@withContext UpdateStatus.ReadyToInstall(
                releaseInfo = releaseInfo,
                apkAsset = apkAsset,
                apkFilePath = validFile.absolutePath,
                requiresInstallPermission = !canRequestPackageInstalls()
            )
        }

        val targetApkFile = getTargetApkFile(releaseInfo.tagName)
        val tempFile = File(getUpdatesDirectory(), "${targetApkFile.name}.tmp")

        // Delete old APK files before downloading new one
        cleanupOldUpdateFiles(keepFileName = null)

        var connection: HttpURLConnection? = null
        try {
            var currentUrl = apkAsset.browserDownloadUrl
            var redirectCount = 0

            // Follow redirects safely while verifying every redirect target is HTTPS and trusted
            while (redirectCount < 6) {
                currentCoroutineContext().ensureActive()
                if (!GitHubReleaseParser.isTrustedGitHubDownloadUrl(currentUrl)) {
                    tempFile.delete()
                    return@withContext UpdateStatus.Error(
                        errorType = UpdateErrorType.UNTRUSTED_DOWNLOAD_URL,
                        userMessage = "Pengalihan unduhan (redirect) mengarah ke domain di luar GitHub resmi.",
                        releaseInfo = releaseInfo
                    )
                }

                val conn = connectionFactory.openConnection(currentUrl).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", AppUpdateConstants.USER_AGENT)
                    setRequestProperty("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                }

                val code = conn.responseCode
                if (code in listOf(
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        HttpURLConnection.HTTP_SEE_OTHER,
                        307,
                        308
                    )
                ) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) {
                        throw IOException("Redirect tanpa header Location")
                    }
                    currentUrl = URL(URL(currentUrl), location).toString()
                    redirectCount++
                } else {
                    connection = conn
                    break
                }
            }

            val activeConnection = connection
                ?: return@withContext UpdateStatus.Error(
                    errorType = UpdateErrorType.DOWNLOAD_FAILED,
                    userMessage = "Terlalu banyak pengalihan (redirect) saat mengunduh APK.",
                    releaseInfo = releaseInfo
                )

            val responseCode = activeConnection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                tempFile.delete()
                return@withContext UpdateStatus.Error(
                    errorType = UpdateErrorType.DOWNLOAD_FAILED,
                    userMessage = "Gagal mengunduh file APK dari GitHub (HTTP $responseCode).",
                    technicalDetail = "HTTP $responseCode",
                    releaseInfo = releaseInfo
                )
            }

            val headerLength = activeConnection.contentLengthLong
            val expectedBytes = when {
                apkAsset.sizeBytes > 0L -> apkAsset.sizeBytes
                headerLength > 0L -> headerLength
                else -> -1L
            }

            onProgress(0L, expectedBytes, 0)

            BufferedInputStream(activeConnection.inputStream, 32 * 1024).use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReportedPercent = -1
                    var lastReportTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        currentCoroutineContext().ensureActive()
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val percent = if (expectedBytes > 0L) {
                            ((totalRead * 100L) / expectedBytes).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        val now = System.currentTimeMillis()
                        if (percent != lastReportedPercent || now - lastReportTime >= 150L) {
                            lastReportedPercent = percent
                            lastReportTime = now
                            onProgress(totalRead, expectedBytes, percent)
                        }
                    }
                    output.fd.sync()
                }
            }

            // Validate downloaded file integrity before renaming to .apk
            val downloadedLength = tempFile.length()
            if (!tempFile.exists() || downloadedLength <= 0L) {
                tempFile.delete()
                return@withContext UpdateStatus.Error(
                    errorType = UpdateErrorType.CORRUPTED_APK,
                    userMessage = "File APK yang diunduh kosong (0 byte). Silakan coba unduh kembali.",
                    releaseInfo = releaseInfo
                )
            }

            if (apkAsset.sizeBytes > 0L && downloadedLength != apkAsset.sizeBytes) {
                tempFile.delete()
                return@withContext UpdateStatus.Error(
                    errorType = UpdateErrorType.CORRUPTED_APK,
                    userMessage = "Ukuran file APK tidak sesuai (${GitHubReleaseAsset.formatBytesAsMb(downloadedLength)} dari ${apkAsset.formattedSizeMb}). Unduhan mungkin terputus.",
                    releaseInfo = releaseInfo
                )
            }

            // Optional SHA-256 verification when digest is provided by GitHub API or release notes
            if (!apkAsset.sha256Digest.isNullOrBlank()) {
                val actualHash = computeSha256Hex(tempFile)
                if (!actualHash.equals(apkAsset.sha256Digest, ignoreCase = true)) {
                    tempFile.delete()
                    return@withContext UpdateStatus.Error(
                        errorType = UpdateErrorType.CORRUPTED_APK,
                        userMessage = "Verifikasi keamanan SHA-256 gagal. File APK rusak dan telah dihapus.",
                        technicalDetail = "Expected ${apkAsset.sha256Digest}, got $actualHash",
                        releaseInfo = releaseInfo
                    )
                }
            }

            if (targetApkFile.exists()) {
                targetApkFile.delete()
            }
            val renamed = tempFile.renameTo(targetApkFile)
            val finalFile = if (renamed) targetApkFile else {
                tempFile.copyTo(targetApkFile, overwrite = true)
                tempFile.delete()
                targetApkFile
            }

            // Verify APK archive validity via PackageManager
            if (!archivePackageValidator(context, finalFile)) {
                finalFile.delete()
                return@withContext UpdateStatus.Error(
                    errorType = UpdateErrorType.CORRUPTED_APK,
                    userMessage = "File yang diunduh bukan paket APK SkinCare Daily yang valid atau file rusak.",
                    releaseInfo = releaseInfo
                )
            }

            onProgress(downloadedLength, downloadedLength, 100)

            UpdateStatus.ReadyToInstall(
                releaseInfo = releaseInfo,
                apkAsset = apkAsset,
                apkFilePath = finalFile.absolutePath,
                requiresInstallPermission = !canRequestPackageInstalls()
            )
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: UnknownHostException) {
            tempFile.delete()
            UpdateStatus.Error(
                errorType = UpdateErrorType.NO_INTERNET,
                userMessage = "Unduhan terputus karena koneksi internet hilang.",
                technicalDetail = e.localizedMessage,
                releaseInfo = releaseInfo
            )
        } catch (e: SocketTimeoutException) {
            tempFile.delete()
            UpdateStatus.Error(
                errorType = UpdateErrorType.DOWNLOAD_FAILED,
                userMessage = "Waktu unduhan habis (Timeout). Silakan coba lagi dengan koneksi yang lebih stabil.",
                technicalDetail = e.localizedMessage,
                releaseInfo = releaseInfo
            )
        } catch (e: IOException) {
            tempFile.delete()
            val isStorageFull = e.message?.contains("No space left", ignoreCase = true) == true ||
                e.message?.contains("ENOSPC", ignoreCase = true) == true
            UpdateStatus.Error(
                errorType = if (isStorageFull) UpdateErrorType.STORAGE_ERROR else UpdateErrorType.DOWNLOAD_FAILED,
                userMessage = if (isStorageFull) {
                    "Penyimpanan perangkat penuh. Kosongkan ruang penyimpanan lalu coba lagi."
                } else {
                    "Unduhan APK terputus di tengah jalan. Silakan tekan Coba Lagi."
                },
                technicalDetail = e.localizedMessage,
                releaseInfo = releaseInfo
            )
        } catch (e: Exception) {
            tempFile.delete()
            UpdateStatus.Error(
                errorType = UpdateErrorType.DOWNLOAD_FAILED,
                userMessage = "Gagal menyimpan atau memvalidasi file APK update.",
                technicalDetail = e.localizedMessage,
                releaseInfo = releaseInfo
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Checks whether the app is allowed to request APK installation on Android 8.0+ (API 26+).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Creates an Intent to open Android's "Install unknown apps" settings screen for this package.
     */
    fun createManageUnknownAppSourcesIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Creates an Intent using FileProvider to launch the system Android Package Installer for the downloaded APK.
     */
    fun createInstallApkIntent(apkFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Launches the Android system APK installer or returns an error status if the file is missing/invalid.
     */
    fun launchApkInstaller(apkFilePath: String): Result<Unit> {
        return try {
            val file = File(apkFilePath)
            if (!file.exists() || file.length() <= 0L) {
                return Result.failure(IllegalStateException("File APK tidak ditemukan. Silakan unduh ulang."))
            }
            val installIntent = createInstallApkIntent(file)
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun computeSha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(16 * 1024)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { byte ->
                String.format(Locale.US, "%02x", byte)
            }
        }

        fun defaultArchivePackageValidator(context: Context, apkFile: File): Boolean {
            if (!apkFile.exists() || apkFile.length() < 64L) return false
            // Verify ZIP/APK magic header ("PK\x03\x04")
            try {
                FileInputStream(apkFile).use { fis ->
                    val header = ByteArray(4)
                    if (fis.read(header) != 4) return false
                    if (header[0] != 0x50.toByte() ||
                        header[1] != 0x4B.toByte() ||
                        header[2] != 0x03.toByte() ||
                        header[3] != 0x04.toByte()
                    ) {
                        return false
                    }
                }
            } catch (_: Exception) {
                return false
            }

            return try {
                val pm = context.packageManager
                val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(
                        apkFile.absolutePath,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                }
                if (archiveInfo != null) {
                    archiveInfo.packageName == context.packageName
                } else {
                    // If PackageManager returned null (e.g., in lightweight JVM unit test), magic header already passed
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
