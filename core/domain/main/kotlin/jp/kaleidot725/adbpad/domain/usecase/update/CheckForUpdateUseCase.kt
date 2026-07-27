package jp.kaleidot725.adbpad.domain.usecase.update

import jp.kaleidot725.adbpad.domain.model.update.AppVersion
import jp.kaleidot725.adbpad.domain.model.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
private data class GithubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GithubReleaseAsset> = emptyList(),
)

class CheckForUpdateUseCase {
    suspend operator fun invoke(): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val client = HttpClient.newHttpClient()
                val request =
                    HttpRequest
                        .newBuilder()
                        .uri(URI.create("https://api.github.com/repos/$REPO/releases/latest"))
                        .header("Accept", "application/vnd.github+json")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() != 200) return@withContext null

                val json = Json { ignoreUnknownKeys = true }
                val release = json.decodeFromString(GithubRelease.serializer(), response.body())
                val latestVersion = release.tagName.removePrefix("v")
                if (!isNewer(latestVersion, AppVersion.CURRENT)) return@withContext null

                val assetExt = if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) ".dmg" else ".msi"
                val asset = release.assets.firstOrNull { it.name.endsWith(assetExt) } ?: return@withContext null

                UpdateInfo(version = latestVersion, downloadUrl = asset.browserDownloadUrl, htmlUrl = release.htmlUrl)
            } catch (_: Exception) {
                null
            }
        }

    private fun isNewer(
        latest: String,
        current: String,
    ): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    companion object {
        private const val REPO = "TonciZ/QA-Adbpad"
    }
}
