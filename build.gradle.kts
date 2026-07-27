@file:OptIn(ExperimentalComposeLibrary::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ktlint)
}

group = "jp.kaleidot725"
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
            packageVersion = "1.3.0"
            description = "Android Debug Bridge GUI Application for QA testing, including Android TV"
            copyright = "Copyright (c) 2026 TonciZ. Based on AdbPad (c) 2025 kaleidot725."
            vendor = "TonciZ"

            modules("jdk.management")
            modules("jdk.unsupported")

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
