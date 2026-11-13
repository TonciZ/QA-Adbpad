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

// Bundled adb + scrcpy: both come from the official scrcpy Windows release, which ships an adb
// that matches it. They are installed with the app and used whenever Settings has no custom
// path, so a new app release is also how the tools get updated.
// To update: bump the version and paste the win64 zip checksum from SHA256SUMS.txt on
// https://github.com/Genymobile/scrcpy/releases
val bundledScrcpyVersion = "4.1"
val bundledScrcpySha256 = "5b12172b3264b2889f4583ee64752ce832e29bc8b1089dca81093459697165db"
val bundledResourcesDir = layout.buildDirectory.dir("bundledResources")

val prepareBundledTools by tasks.registering {
    val version = bundledScrcpyVersion
    val sha256 = bundledScrcpySha256
    val zipFile = layout.buildDirectory.file("downloads/scrcpy-win64-v$version.zip")
    val rootDir = bundledResourcesDir
    inputs.property("version", version)
    inputs.property("sha256", sha256)
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
        // Windows-only bundle (the only platform this fork ships). Elsewhere the folder stays
        // empty and the app falls back to the paths in Settings or to `adb` on the PATH.
        if (!System.getProperty("os.name").startsWith("Windows")) return@doLast

        val zip = zipFile.get().asFile
        if (!zip.isFile || checksum(zip) != sha256) {
            zip.parentFile.mkdirs()
            val url = "https://github.com/Genymobile/scrcpy/releases/download/v$version/${zip.name}"
            logger.lifecycle("Downloading $url")
            URI(url).toURL().openStream().use { input -> zip.outputStream().use { input.copyTo(it) } }
        }
        val actual = checksum(zip)
        check(actual == sha256) { "Checksum mismatch for ${zip.name}: expected $sha256 but got $actual" }

        // Compose merges <root>/windows-x64/** into the installed app's resources folder.
        val toolsDir = root.resolve("windows-x64/tools")
        toolsDir.mkdirs()
        // The launcher scripts are for double-clicking scrcpy by hand; nothing here uses them.
        val skipped = setOf("scrcpy-noconsole.vbs", "open_a_terminal_here.bat")
        ZipFile(zip).use { archive ->
            for (entry in archive.entries()) {
                val name = entry.name.substringAfterLast('/')
                if (entry.isDirectory || name.isEmpty() || name in skipped) continue
                archive.getInputStream(entry).use { input -> toolsDir.resolve(name).outputStream().use { input.copyTo(it) } }
            }
        }
    }
}
version = "3.9.0"

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
            packageVersion = "1.6.0"
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
                iconFile.set(project.file("icon.ico"))
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
