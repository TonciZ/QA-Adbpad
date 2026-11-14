@file:OptIn(ExperimentalComposeLibrary::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ktlint)
}

group = "jp.kaleidot725"

// Bundled adb + scrcpy: both come from the official scrcpy release for the OS being built, which
// ships an adb that matches it. They are installed with the app and used whenever Settings has
// no custom path, so a new app release is also how the tools get updated.
// To update: bump the version and paste the checksums from SHA256SUMS.txt on
// https://github.com/Genymobile/scrcpy/releases
val bundledScrcpyVersion = "4.1"

// Compose resource folder for the OS -> scrcpy release asset and its SHA-256.
val bundledScrcpyAssets =
    mapOf(
        "windows-x64" to ("scrcpy-win64-v$bundledScrcpyVersion.zip" to "5b12172b3264b2889f4583ee64752ce832e29bc8b1089dca81093459697165db"),
        "linux-x64" to
            ("scrcpy-linux-x86_64-v$bundledScrcpyVersion.tar.gz" to "ad56ae8bfeedf41e824945c11dbf55fcb092b3e615b9b486f48a50e30d389635"),
        "macos-arm64" to
            ("scrcpy-macos-aarch64-v$bundledScrcpyVersion.tar.gz" to "20fd47c9014dd5e0fa77091f3cb7adbda8445a360c4584aeaa0150b5b3988ff3"),
        "macos-x64" to
            ("scrcpy-macos-x86_64-v$bundledScrcpyVersion.tar.gz" to "ee2a7223bc8dbdc4f482db1134bcf441178dafb833492b71ca4c22090c58ce72"),
    )
val bundledResourcesDir = layout.buildDirectory.dir("bundledResources")

val prepareBundledTools by tasks.registering {
    val version = bundledScrcpyVersion
    val assets = bundledScrcpyAssets
    val downloadsDir = layout.buildDirectory.dir("downloads")
    val rootDir = bundledResourcesDir
    inputs.property("version", version)
    inputs.property("assets", assets.toString())
    outputs.dir(rootDir)

    doLast {
        fun checksum(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        val root = rootDir.get().asFile
        root.deleteRecursively()
        root.mkdirs()

        val osName = System.getProperty("os.name").lowercase()
        val os =
            when {
                osName.startsWith("windows") -> "windows"
                osName.startsWith("mac") -> "macos"
                else -> "linux"
            }
        val arch = if (System.getProperty("os.arch") in setOf("aarch64", "arm64")) "arm64" else "x64"
        // No scrcpy release for this OS/CPU: the folder stays empty and the app falls back to
        // the paths in Settings or to `adb` on the PATH.
        val (assetName, sha256) = assets["$os-$arch"] ?: return@doLast

        val archive = downloadsDir.get().asFile.resolve(assetName)
        if (!archive.isFile || checksum(archive) != sha256) {
            archive.parentFile.mkdirs()
            val url = "https://github.com/Genymobile/scrcpy/releases/download/v$version/$assetName"
            logger.lifecycle("Downloading $url")
            URI(url).toURL().openStream().use { input -> archive.outputStream().use { input.copyTo(it) } }
        }
        val actual = checksum(archive)
        check(actual == sha256) { "Checksum mismatch for $assetName: expected $sha256 but got $actual" }

        // Compose merges <root>/<os>-<arch>/** into the installed app's resources folder.
        val toolsDir = root.resolve("$os-$arch/tools")
        toolsDir.mkdirs()
        if (assetName.endsWith(".zip")) {
            ZipFile(archive).use { zip ->
                for (entry in zip.entries()) {
                    val name = entry.name.substringAfterLast('/')
                    if (entry.isDirectory || name.isEmpty()) continue
                    zip.getInputStream(entry).use { input -> toolsDir.resolve(name).outputStream().use { input.copyTo(it) } }
                }
            }
        } else {
            // tar keeps the executable bits on adb and scrcpy, which a plain Java copy would drop.
            val tar =
                ProcessBuilder("tar", "-xzf", archive.absolutePath, "--strip-components=1", "-C", toolsDir.absolutePath)
                    .inheritIO()
                    .start()
            check(tar.waitFor() == 0) { "Could not extract $assetName" }
        }
        // Launcher scripts and the man page are for using scrcpy by hand; nothing here needs them.
        listOf("scrcpy-noconsole.vbs", "open_a_terminal_here.bat", "scrcpy.1").forEach { toolsDir.resolve(it).delete() }
    }
}

kotlin {
    jvm()
    jvmToolchain(17)

    composeCompiler {
        featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
    }

    sourceSets {
        val jvmMain by getting {
            kotlin.srcDirs("main/kotlin")
            resources.srcDirs("main/resources")
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.components.splitPane)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.lucide)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.kotlin.result)
                implementation(libs.koin)
                implementation(libs.ktor.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.jSystemThemeDetectorVer)
                implementation(libs.coil)
                implementation(libs.zoomable)
                implementation(libs.scrcpy.kt)
                implementation(libs.pulse.mvi)
                implementation(project(":core:util"))
                implementation(project(":core:data"))
                implementation(project(":core:domain"))
                implementation(project(":core:view"))
            }
        }
        val jvmTest by getting {
            kotlin.srcDirs("test/kotlin")
            resources.srcDirs("test/resources")
            dependencies {
                implementation(libs.junit5)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "jp.kaleidot725.adbpad.MainKt"
        buildTypes.release {
            proguard.isEnabled = false
        }

        nativeDistributions {
            packageName = "QA-Adbpad"
            packageVersion = "1.7.1"
            description = "Android Debug Bridge GUI Application for QA testing, including Android TV"
            copyright = "Copyright (c) 2026 TonciZ. Based on AdbPad (c) 2025 kaleidot725."
            vendor = "TonciZ"
            appResourcesRootDir.set(bundledResourcesDir)

            modules("jdk.management")
            modules("jdk.unsupported")
            // ponytail: on Windows machines with an accessibility/screen-reader flag enabled
            // (Ease of Access), AWT tries to load com.sun.java.accessibility.AccessBridge at
            // Toolkit init - if this module is missing from the packaged runtime, that throws
            // an uncaught AWTError and the whole process dies before any window appears,
            // surfacing as jpackage's generic "Failed to launch JVM" dialog.
            modules("jdk.accessibility")
            // Check for Updates uses java.net.http.HttpClient - missing from the module list,
            // HttpClient.newHttpClient() throws NoClassDefFoundError (an Error, uncaught by any
            // `catch (Exception ...)`), silently killing the check coroutine before it can ever
            // reset the "checking..." UI state, so the button spins forever.
            modules("java.net.http")

            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
            )

            jvmArgs(
                "-Dapple.awt.application.appearance=NSAppearanceNameDarkAqua",
            )

            macOS {
                bundleID = "jp.kaleidot725.adbpad"
                iconFile.set(project.file("icon.icns"))
                entitlementsFile.set(project.file("default.entitlements"))
            }

            windows {
                iconFile.set(project.file("icon.ico"))
                shortcut = true
                menu = true
                dirChooser = true
                upgradeUuid = "EB250A9E-5DA4-461F-8DB9-3BD5369B14AD"
            }

            linux {
                // jpackage wants a PNG on Linux; the .ico made the .deb build fall back to a default icon.
                iconFile.set(project.file("main/resources/icon.png"))
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Compose copies appResourcesRootDir in these tasks (one for `run`, one per packaged build type).
tasks.matching { it.name.startsWith("prepareAppResources") }.configureEach {
    dependsOn(prepareBundledTools)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        // Required to download KtLint
        mavenCentral()
    }

    // Optionally configure plugin
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(true)
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

// build.gradle.kts
tasks.register<ComposeHotRun>("runHot") {
    mainClass = "jp.kaleidot725.adbpad.MainKt"
}
