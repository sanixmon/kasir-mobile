package com.kasir.mobile.data.remote

import com.kasir.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = ""
)

@Serializable
data class GitHubReleaseResponse(
    @SerialName("tag_name")
    val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<GitHubReleaseAsset> = emptyList()
)

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    companion object {
        fun parse(raw: String): SemVer? {
            val cleaned = raw.trim().removePrefix("v").split("-").firstOrNull() ?: return null
            val parts = cleaned.split(".").mapNotNull { it.toIntOrNull() }
            if (parts.size < 2) return null
            return SemVer(
                major = parts[0],
                minor = parts[1],
                patch = parts.getOrElse(2) { 0 }
            )
        }
    }
}

data class UpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val hasUpdate: Boolean
)

object UpdateChecker {
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/sanixmon/kasir-mobile/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("User-Agent", "KasirMobile-Android")
                .header("Accept", "application/vnd.github+json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("GitHub API returned HTTP ${response.code}")
                }

                val bodyStr = response.body?.string() ?: return@runCatching null
                val release = json.decodeFromString<GitHubReleaseResponse>(bodyStr)

                val remoteSemVer = SemVer.parse(release.tagName)
                val localSemVer = SemVer.parse(currentVersionName)

                val hasUpdate = if (remoteSemVer != null && localSemVer != null) {
                    remoteSemVer > localSemVer
                } else {
                    false
                }

                // Prefer release APK or any APK asset
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                val downloadUrl = apkAsset?.browserDownloadUrl
                    ?: "https://github.com/sanixmon/kasir-mobile/releases/latest"

                val cleanVersion = release.tagName.removePrefix("v")

                UpdateInfo(
                    latestVersion = cleanVersion,
                    releaseTitle = release.name.ifBlank { "Kasir Mobile v$cleanVersion" },
                    releaseNotes = release.body.trim(),
                    downloadUrl = downloadUrl,
                    hasUpdate = hasUpdate
                )
            }
        }
    }
}

object UpdateManager {
    var dismissedThisSession = false

    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate = _availableUpdate.asStateFlow()

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME,
        isManualCheck: Boolean = false
    ): Result<UpdateInfo?> {
        val result = UpdateChecker.checkForUpdates(currentVersion)
        result.onSuccess { info ->
            if (info?.hasUpdate == true) {
                _availableUpdate.value = info
            } else if (isManualCheck) {
                _availableUpdate.value = info
            }
        }
        return result
    }

    fun dismiss() {
        dismissedThisSession = true
        _availableUpdate.value = null
    }
}
