package com.example.update

import org.json.JSONObject
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

object AppUpdateConstants {
    const val GITHUB_OWNER = "estabantu5-star"
    const val GITHUB_REPO = "note-acnes"
    const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val RELEASES_WEB_URL =
        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    const val PREFERRED_APK_NAME = "SkinCare-Daily.apk"
    const val USER_AGENT = "SkinCare-Daily-Android"
    const val ACCEPT_HEADER = "application/vnd.github+json"

    // Automatic background check maximum once every 24 hours
    const val AUTO_CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L

    val TRUSTED_GITHUB_HOSTS = setOf(
        "github.com",
        "www.github.com",
        "api.github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "codeload.github.com",
        "raw.githubusercontent.com"
    )
}

data class GitHubReleaseAsset(
    val name: String,
    val browserDownloadUrl: String,
    val sizeBytes: Long,
    val contentType: String = "application/vnd.android.package-archive",
    val sha256Digest: String? = null
) {
    val formattedSizeMb: String
        get() = formatBytesAsMb(sizeBytes)

    companion object {
        fun formatBytesAsMb(bytes: Long): String {
            if (bytes <= 0L) return "Ukuran tidak diketahui"
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.2f MB", mb)
        }
    }
}

data class GitHubReleaseInfo(
    val tagName: String,
    val normalizedVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val isDraft: Boolean,
    val isPrerelease: Boolean,
    val publishedAtIso: String,
    val formattedPublishedDate: String,
    val htmlUrl: String,
    val apkAsset: GitHubReleaseAsset?
)

enum class UpdateErrorType {
    NO_INTERNET,
    RATE_LIMITED,
    NO_RELEASE_FOUND,
    NO_APK_ASSET,
    UNTRUSTED_DOWNLOAD_URL,
    DOWNLOAD_FAILED,
    CORRUPTED_APK,
    STORAGE_ERROR,
    INSTALL_FAILED,
    UNKNOWN
}

sealed interface UpdateStatus {
    data object Idle : UpdateStatus

    data class Checking(val isManual: Boolean = true) : UpdateStatus

    data class UpToDate(
        val installedVersion: String,
        val latestRelease: GitHubReleaseInfo? = null
    ) : UpdateStatus

    data class UpdateAvailable(
        val releaseInfo: GitHubReleaseInfo,
        val apkAsset: GitHubReleaseAsset,
        val downloadedFilePath: String? = null
    ) : UpdateStatus

    data class Downloading(
        val releaseInfo: GitHubReleaseInfo,
        val apkAsset: GitHubReleaseAsset,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Int
    ) : UpdateStatus

    data class ReadyToInstall(
        val releaseInfo: GitHubReleaseInfo,
        val apkAsset: GitHubReleaseAsset,
        val apkFilePath: String,
        val requiresInstallPermission: Boolean
    ) : UpdateStatus

    data class Error(
        val errorType: UpdateErrorType,
        val userMessage: String,
        val technicalDetail: String? = null,
        val releaseInfo: GitHubReleaseInfo? = null
    ) : UpdateStatus
}

data class AppUpdateUiState(
    val installedVersionName: String = "1.0.5",
    val installedVersionCode: Int = 5,
    val lastCheckedAtMillis: Long = 0L,
    val postponedVersionTag: String = "",
    val showUpdateDialog: Boolean = false,
    val showAutoUpdateBanner: Boolean = false,
    val canInstallUnknownApps: Boolean = true,
    val status: UpdateStatus = UpdateStatus.Idle
) {
    val displayInstalledVersion: String
        get() {
            val clean = installedVersionName.trim().removePrefix("v").removePrefix("V")
            return "v$clean (Build $installedVersionCode)"
        }
}

/**
 * Semantic Version parser & comparator for Android / GitHub Release tags.
 * Normalizes versions like "v1.0.5", "1.0.10", "v1.1", "1.0.5-beta+2" -> [1, 0, 5]
 * and compares numeric parts from left to right.
 */
object SemanticVersionComparator {

    fun normalizeVersionString(rawVersion: String): String {
        val trimmed = rawVersion.trim()
            .removePrefix("version")
            .removePrefix("Version")
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()

        // Strip suffix after '-' or '+'
        val withoutBuildMeta = trimmed
            .substringBefore("+")
            .substringBefore("-")
            .trim()

        val parts = parseVersionParts(withoutBuildMeta)
        return if (parts.isEmpty()) "0.0.0" else parts.joinToString(".")
    }

    fun parseVersionParts(rawVersion: String): List<Int> {
        val trimmed = rawVersion.trim()
            .removePrefix("version")
            .removePrefix("Version")
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()
            .substringBefore("+")
            .substringBefore("-")
            .trim()

        if (trimmed.isEmpty()) return emptyList()

        val parts = trimmed.split(".")
            .map { token ->
                val digitsOnly = token.trim().takeWhile { it.isDigit() }
                digitsOnly.toIntOrNull() ?: 0
            }
            .toMutableList()

        // Ensure at least 3 segments (Major.Minor.Patch) so "v1.1" -> [1, 1, 0]
        while (parts.size < 3) {
            parts.add(0)
        }
        return parts
    }

    /**
     * Returns:
     * > 0 if versionA > versionB
     * < 0 if versionA < versionB
     * == 0 if versionA == versionB
     */
    fun compare(versionA: String, versionB: String): Int {
        val partsA = parseVersionParts(versionA)
        val partsB = parseVersionParts(versionB)
        val maxLen = max(partsA.size, partsB.size)

        for (i in 0 until maxLen) {
            val a = partsA.getOrElse(i) { 0 }
            val b = partsB.getOrElse(i) { 0 }
            if (a != b) {
                return a.compareTo(b)
            }
        }
        return 0
    }

    fun isRemoteNewer(installedVersion: String, remoteVersion: String): Boolean {
        return compare(remoteVersion, installedVersion) > 0
    }
}

/**
 * Parser & security validator for GitHub Releases API JSON responses.
 */
object GitHubReleaseParser {

    private val indonesianDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("id", "ID"))

    /**
     * Validates that a download URL strictly uses HTTPS and points to an official GitHub domain.
     */
    fun isTrustedGitHubDownloadUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val uri = URI(url.trim())
            val scheme = uri.scheme?.lowercase(Locale.US) ?: return false
            val host = uri.host?.lowercase(Locale.US) ?: return false
            if (scheme != "https") return false
            host in AppUpdateConstants.TRUSTED_GITHUB_HOSTS ||
                host.endsWith(".github.com") ||
                host.endsWith(".githubusercontent.com")
        } catch (_: Exception) {
            false
        }
    }

    fun formatPublishedDate(publishedAtIso: String): String {
        if (publishedAtIso.isBlank()) return "Tanggal rilis tidak diketahui"
        return try {
            val instant = Instant.parse(publishedAtIso.trim())
            instant.atZone(ZoneId.systemDefault()).format(indonesianDateFormatter)
        } catch (_: Exception) {
            publishedAtIso.take(10)
        }
    }

    /**
     * Parses a GitHub Release JSON object from `/releases/latest` and selects the APK asset:
     * 1. Prefers exact name `SkinCare-Daily.apk` (case-insensitive)
     * 2. Falls back to the first asset ending with `.apk`
     */
    fun parseReleaseJson(jsonString: String): GitHubReleaseInfo {
        val root = JSONObject(jsonString)
        val tagName = root.optString("tag_name", "").trim()
        val rawName = root.optString("name", "").trim()
        val body = root.optString("body", "").trim()
        val isDraft = root.optBoolean("draft", false)
        val isPrerelease = root.optBoolean("prerelease", false)
        val publishedAt = root.optString("published_at", "").trim()
        val htmlUrl = root.optString("html_url", AppUpdateConstants.RELEASES_WEB_URL).trim()

        val normalizedVersion = SemanticVersionComparator.normalizeVersionString(
            tagName.ifBlank { rawName }
        )
        val releaseTitle = rawName.ifBlank {
            if (tagName.isNotBlank()) "SkinCare Daily $tagName" else "Rilis Terbaru SkinCare Daily"
        }

        val assetsArray = root.optJSONArray("assets")
        val parsedApkCandidates = mutableListOf<GitHubReleaseAsset>()

        if (assetsArray != null) {
            for (i in 0 until assetsArray.length()) {
                val assetObj = assetsArray.optJSONObject(i) ?: continue
                val assetName = assetObj.optString("name", "").trim()
                val downloadUrl = assetObj.optString("browser_download_url", "").trim()
                val sizeBytes = assetObj.optLong("size", 0L)
                val contentType = assetObj.optString(
                    "content_type",
                    "application/vnd.android.package-archive"
                )
                val rawDigest = assetObj.optString("digest", "").trim()
                val sha256FromAsset = extractSha256Hex(rawDigest)
                    ?: extractSha256FromReleaseNotes(body, assetName)

                if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                    parsedApkCandidates.add(
                        GitHubReleaseAsset(
                            name = assetName,
                            browserDownloadUrl = downloadUrl,
                            sizeBytes = sizeBytes,
                            contentType = contentType,
                            sha256Digest = sha256FromAsset
                        )
                    )
                }
            }
        }

        // Rule: 1) Find asset with name == "SkinCare-Daily.apk", 2) Else first asset ending with ".apk"
        val selectedAsset = parsedApkCandidates.firstOrNull {
            it.name.equals(AppUpdateConstants.PREFERRED_APK_NAME, ignoreCase = true)
        } ?: parsedApkCandidates.firstOrNull()

        return GitHubReleaseInfo(
            tagName = tagName.ifBlank { "v$normalizedVersion" },
            normalizedVersion = normalizedVersion,
            releaseTitle = releaseTitle,
            releaseNotes = body.ifBlank {
                "Pembaruan aplikasi SkinCare Daily tersedia untuk diunduh dan dipasang."
            },
            isDraft = isDraft,
            isPrerelease = isPrerelease,
            publishedAtIso = publishedAt,
            formattedPublishedDate = formatPublishedDate(publishedAt),
            htmlUrl = htmlUrl.ifBlank { AppUpdateConstants.RELEASES_WEB_URL },
            apkAsset = selectedAsset
        )
    }

    internal fun extractSha256Hex(rawDigest: String?): String? {
        if (rawDigest.isNullOrBlank()) return null
        val cleaned = rawDigest.trim()
            .removePrefix("sha256:")
            .removePrefix("SHA256:")
            .trim()
            .lowercase(Locale.US)
        return if (cleaned.length == 64 && cleaned.all { it in '0'..'9' || it in 'a'..'f' }) {
            cleaned
        } else {
            null
        }
    }

    internal fun extractSha256FromReleaseNotes(body: String, assetName: String): String? {
        if (body.isBlank()) return null
        val regex = Regex("(?i)(?:sha-?256)\\s*[:=]\\s*([a-f0-9]{64})")
        val match = regex.find(body)
        return match?.groupValues?.getOrNull(1)?.lowercase(Locale.US)
    }
}
