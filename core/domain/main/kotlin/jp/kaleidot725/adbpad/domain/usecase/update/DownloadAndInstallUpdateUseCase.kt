package jp.kaleidot725.adbpad.domain.usecase.update

import jp.kaleidot725.adbpad.domain.model.update.UpdateInfo
import jp.kaleidot725.adbpad.domain.repository.ScrcpyProcessRepository
import jp.kaleidot725.adbpad.domain.usecase.adb.StopBundledAdbServerUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

// ponytail: no lightweight in-place patch mechanism exists for jpackage-built installers -
// this downloads the new installer and launches it, then quits so the installer can replace
// this app's files without file-lock conflicts. On Windows the installer runs with only a
// progress bar and the app is reopened afterwards; on macOS the disk image is opened for the
// user to finish; on Linux the release page is opened.
class DownloadAndInstallUpdateUseCase(
    private val scrcpyProcessRepository: ScrcpyProcessRepository,
    private val stopBundledAdbServerUseCase: StopBundledAdbServerUseCase,
) {
    /** True where "update" can only mean opening the download page (Linux): the app keeps running. */
    val opensDownloadPage: Boolean = osName().let { !it.startsWith("windows") && !it.startsWith("mac") }

    /**
     * Returns false when the update could not be started. On Windows and macOS a successful
     * start ends this process, so `true` is only ever returned on Linux.
     */
    suspend operator fun invoke(update: UpdateInfo): Boolean =
        withContext(Dispatchers.IO) {
            // The banner and Settings > Updates can both trigger this; two runs would write the
            // same installer file at once.
            if (!running.compareAndSet(false, true)) return@withContext false
            try {
                if (opensDownloadPage) return@withContext openDownloadPage(update.htmlUrl)

                val fileName = update.downloadUrl.substringAfterLast("/")
                val isMsi = fileName.endsWith(".msi")
                val isDmg = fileName.endsWith(".dmg")
                if (!isMsi && !isDmg) return@withContext false

                val installer = File(System.getProperty("java.io.tmpdir"), fileName)
                if (!download(update.downloadUrl, installer)) return@withContext false

                // Start the installer first: if that fails the app carries on untouched. Only
                // then let go of the bundled adb and scrcpy, which the installer replaces too.
                if (isMsi) startWindowsInstaller(installer) else ProcessBuilder("open", installer.absolutePath).start()
                scrcpyProcessRepository.terminateAllProcesses()
                stopBundledAdbServerUseCase()

                kotlin.system.exitProcess(0)
            } catch (e: CancellationException) {
                throw e
                // ponytail: Throwable, not Exception - a JDK module missing from the packaged
                // runtime surfaces as an Error and would otherwise leave "Downloading..." up forever.
            } catch (_: Throwable) {
                false
            } finally {
                running.set(false)
            }
        }

    /**
     * How the last Windows update attempt ended: the installer's exit code, or null when there
     * is nothing to report. Reading it clears it.
     */
    fun consumeLastInstallerExitCode(): Int? =
        try {
            val file = resultFile()
            if (!file.isFile) {
                null
            } else {
                val code = file.readText().trim().toIntOrNull()
                file.delete()
                code
            }
        } catch (_: Exception) {
            null
        }

    private fun download(
        url: String,
        target: File,
    ): Boolean {
        // ponytail: GitHub answers a release download with a redirect to its file servers. The
        // default client does not follow redirects, so every download "failed" with a 302.
        val client =
            HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build()
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .timeout(DOWNLOAD_TIMEOUT)
                .GET()
                .build()
        target.delete()
        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(target.toPath()))
        return response.statusCode() == 200 && target.length() > 0L
    }

    private fun openDownloadPage(url: String): Boolean =
        try {
            Desktop.getDesktop().browse(URI.create(url))
            true
        } catch (_: Exception) {
            ProcessBuilder("xdg-open", url).start()
            true
        }

    // ponytail: msiexec cannot replace this app's files while it is still running, and it does
    // not start the app again when it is done. A small script that outlives this process does
    // the steps in order: wait for us to exit, install with a progress bar only (/passive -
    // Windows may still ask for permission), note how it went, reopen the app.
    private fun startWindowsInstaller(installer: File) {
        // Set by the packaged launcher; absent when run from the IDE or gradle.
        val appExe = System.getProperty("jpackage.app-path").orEmpty()
        // Keep a custom install folder: without this an upgrade goes back to the default one.
        val installDir = File(appExe).parentFile?.absolutePath.orEmpty()
        val script = File(installer.parentFile, "qa-adbpad-update.cmd")
        resultFile().delete()
        script.writeText(
            listOf(
                "@echo off",
                // ponytail: full paths on purpose. A PATH with Git or MSYS tools in front makes a
                // bare `find` the Unix one, and the wait below silently stops waiting.
                "set SYS=%SystemRoot%\\System32",
                "set TRIES=0",
                ":wait",
                // Wait for this app (by process id) to be gone, at most about 30 seconds.
                "\"%SYS%\\tasklist.exe\" /fi \"PID eq %~3\" 2>nul | \"%SYS%\\find.exe\" \"%~3\" >nul || goto install",
                "set /a TRIES+=1",
                "if %TRIES% geq 30 goto install",
                // `timeout` refuses to run without a console, ping is the classic stand-in.
                "\"%SYS%\\ping.exe\" -n 2 127.0.0.1 >nul",
                "goto wait",
                ":install",
                "if \"%~4\"==\"\" (\"%SYS%\\msiexec.exe\" /i \"%~1\" /passive /norestart) else (\"%SYS%\\msiexec.exe\" /i \"%~1\" /passive /norestart INSTALLDIR=\"%~4\")",
                "echo %ERRORLEVEL%> \"%~5\"",
                "if not \"%~2\"==\"\" if exist \"%~2\" start \"\" \"%~2\"",
                "",
            ).joinToString("\r\n"),
        )
        // `call` first: when the first thing after `cmd /c` is a quote (a temp path with a
        // space) and more quoted arguments follow, cmd strips the outer quotes and the path breaks.
        ProcessBuilder(
            "cmd",
            "/c",
            "call",
            script.absolutePath,
            installer.absolutePath,
            appExe,
            ProcessHandle.current().pid().toString(),
            installDir,
            resultFile().absolutePath,
        ).start()
    }

    private fun resultFile() = File(System.getProperty("java.io.tmpdir"), "qa-adbpad-update.result")

    companion object {
        private val CONNECT_TIMEOUT = Duration.ofSeconds(15)
        private val DOWNLOAD_TIMEOUT = Duration.ofMinutes(10)

        // Windows Installer: 0 = done, 3010 = done, restart needed to finish.
        val INSTALLER_SUCCESS_CODES = setOf(0, 3010)

        // One guard for every instance: Koin hands each screen its own use case object.
        private val running = AtomicBoolean(false)

        private fun osName() = System.getProperty("os.name").lowercase()
    }
}
