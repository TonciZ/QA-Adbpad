package jp.kaleidot725.adbpad.domain.usecase.update

import jp.kaleidot725.adbpad.domain.model.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// ponytail: no lightweight in-place patch mechanism exists for jpackage-built installers -
// this downloads the new installer and launches it, then quits so the installer can replace
// this app's files without file-lock conflicts. Not silent (the OS installer UI still runs),
// but it's one click instead of "find the release page, download, run it yourself".
class DownloadAndInstallUpdateUseCase {
    suspend operator fun invoke(update: UpdateInfo): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val fileName = update.downloadUrl.substringAfterLast("/")
                val tempFile = File(System.getProperty("java.io.tmpdir"), fileName)

                val client = HttpClient.newHttpClient()
                val request =
                    HttpRequest
                        .newBuilder()
                        .uri(URI.create(update.downloadUrl))
                        .GET()
                        .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile.toPath()))
                if (response.statusCode() != 200) return@withContext false

                when {
                    fileName.endsWith(".msi") -> ProcessBuilder("msiexec", "/i", tempFile.absolutePath).start()
                    fileName.endsWith(".dmg") -> ProcessBuilder("open", tempFile.absolutePath).start()
                    else -> return@withContext false
                }

                kotlin.system.exitProcess(0)
            } catch (_: Exception) {
                false
            }
        }
}
