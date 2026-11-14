package jp.kaleidot725.adbpad.ui.screen.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.zIndex
import jp.kaleidot725.adbpad.domain.model.MainCategory
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.setting.AccentColor
import jp.kaleidot725.adbpad.domain.model.setting.getWindowSize
import jp.kaleidot725.adbpad.ui.common.resource.UserColor
import jp.kaleidot725.adbpad.ui.component.rail.NavigationRail
import jp.kaleidot725.adbpad.ui.screen.ScreenLayout
import jp.kaleidot725.adbpad.ui.screen.error.AdbErrorScreen
import jp.kaleidot725.adbpad.ui.screen.main.state.MainAction
import jp.kaleidot725.adbpad.ui.screen.main.state.MainDialog
import jp.kaleidot725.adbpad.ui.screen.main.state.MainState
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.SplitPaneState
import org.jetbrains.compose.splitpane.rememberSplitPaneState

private fun createLightColorScheme(accentColor: AccentColor) =
    ColorScheme(
        primary = accentColor.lightColor,
        onPrimary = UserColor.Light.ON_PRIMARY,
        primaryContainer = accentColor.lightColor.copy(alpha = 0.1f),
        onPrimaryContainer = accentColor.lightColor,
        inversePrimary = accentColor.lightColor.copy(alpha = 0.8f),
        secondary = UserColor.Light.SECONDARY,
        onSecondary = UserColor.Light.ON_SECONDARY,
        secondaryContainer = UserColor.Light.SECONDARY_VARIANT,
        onSecondaryContainer = UserColor.Light.ON_SECONDARY,
        tertiary = UserColor.Light.SECONDARY,
        onTertiary = UserColor.Light.ON_SECONDARY,
        tertiaryContainer = UserColor.Light.SECONDARY_VARIANT,
        onTertiaryContainer = UserColor.Light.ON_SECONDARY,
        background = UserColor.Light.BACKGROUND,
        onBackground = UserColor.Light.ON_BACKGROUND,
        surface = UserColor.Light.SURFACE,
        onSurface = UserColor.Light.ON_SURFACE,
        surfaceVariant = UserColor.Light.SURFACE.copy(alpha = 0.8f),
        onSurfaceVariant = UserColor.Light.ON_SURFACE.copy(alpha = 0.8f),
        surfaceTint = accentColor.lightColor,
        inverseSurface = UserColor.Light.ON_SURFACE,
        inverseOnSurface = UserColor.Light.SURFACE,
        error = UserColor.Light.ERROR,
        onError = UserColor.Light.ON_ERROR,
        errorContainer = UserColor.Light.ERROR.copy(alpha = 0.1f),
        onErrorContainer = UserColor.Light.ERROR,
        outline = UserColor.Light.ON_SURFACE.copy(alpha = 0.5f),
        outlineVariant = UserColor.Light.ON_SURFACE.copy(alpha = 0.3f),
        scrim = UserColor.Light.ON_SURFACE.copy(alpha = 0.9f),
        surfaceBright = UserColor.Light.SURFACE,
        surfaceDim = UserColor.Light.SURFACE.copy(alpha = 0.9f),
        surfaceContainer = UserColor.Light.SURFACE.copy(alpha = 0.8f),
        surfaceContainerHigh = UserColor.Light.SURFACE.copy(alpha = 0.9f),
        surfaceContainerHighest = UserColor.Light.SURFACE,
        surfaceContainerLow = UserColor.Light.SURFACE.copy(alpha = 0.6f),
        surfaceContainerLowest = UserColor.Light.SURFACE.copy(alpha = 0.4f),
    )

private fun createDarkColorScheme(accentColor: AccentColor) =
    ColorScheme(
        primary = accentColor.darkColor,
        onPrimary = UserColor.Dark.ON_PRIMARY,
        primaryContainer = accentColor.darkColor.copy(alpha = 0.2f),
        onPrimaryContainer = accentColor.darkColor,
        inversePrimary = accentColor.darkColor.copy(alpha = 0.8f),
        secondary = UserColor.Dark.SECONDARY,
        onSecondary = UserColor.Dark.ON_SECONDARY,
        secondaryContainer = UserColor.Dark.SECONDARY_VARIANT,
        onSecondaryContainer = UserColor.Dark.ON_SECONDARY,
        tertiary = UserColor.Dark.SECONDARY,
        onTertiary = UserColor.Dark.ON_SECONDARY,
        tertiaryContainer = UserColor.Dark.SECONDARY_VARIANT,
        onTertiaryContainer = UserColor.Dark.ON_SECONDARY,
        background = UserColor.Dark.BACKGROUND,
        onBackground = UserColor.Dark.ON_BACKGROUND,
        surface = UserColor.Dark.SURFACE,
        onSurface = UserColor.Dark.ON_SURFACE,
        surfaceVariant = UserColor.Dark.SURFACE.copy(alpha = 0.8f),
        onSurfaceVariant = UserColor.Dark.ON_SURFACE.copy(alpha = 0.8f),
        surfaceTint = accentColor.darkColor,
        inverseSurface = UserColor.Dark.ON_SURFACE,
        inverseOnSurface = UserColor.Dark.SURFACE,
        error = UserColor.Dark.ERROR,
        onError = UserColor.Dark.ON_ERROR,
        errorContainer = UserColor.Dark.ERROR.copy(alpha = 0.1f),
        onErrorContainer = UserColor.Dark.ERROR,
        outline = UserColor.Dark.ON_SURFACE.copy(alpha = 0.5f),
        outlineVariant = UserColor.Dark.ON_SURFACE.copy(alpha = 0.3f),
        scrim = UserColor.Dark.ON_SURFACE.copy(alpha = 0.9f),
        surfaceBright = UserColor.Dark.SURFACE,
        surfaceDim = UserColor.Dark.SURFACE.copy(alpha = 0.9f),
        surfaceContainer = UserColor.Dark.SURFACE.copy(alpha = 0.8f),
        surfaceContainerHigh = UserColor.Dark.SURFACE.copy(alpha = 0.9f),
        surfaceContainerHighest = UserColor.Dark.SURFACE,
        surfaceContainerLow = UserColor.Dark.SURFACE.copy(alpha = 0.6f),
        surfaceContainerLowest = UserColor.Dark.SURFACE.copy(alpha = 0.4f),
    )

@Composable
fun MainScreen(
    state: MainState,
    onAction: (MainAction) -> Unit,
    onExitApplication: () -> Unit,
    topContent: @Composable () -> Unit,
    commandContent: @Composable (SplitPaneState) -> Unit,
    textCommandContent: @Composable (SplitPaneState, SplitPaneState) -> Unit,
    screenshotContent: @Composable (SplitPaneState, SplitPaneState) -> Unit,
    appContent: @Composable (SplitPaneState, SplitPaneState) -> Unit,
    logContent: @Composable () -> Unit,
    settingContent: @Composable () -> Unit,
    deviceSettingsContent: @Composable () -> Unit,
) {
    val windowState by remember(state.size.width, state.size.height) {
        mutableStateOf(WindowState(width = state.size.width.dp, height = state.size.height.dp))
    }

    Window(
        title = Language.windowTitle,
        icon = painterResource("icon.png"),
        onCloseRequest = {
            onAction(MainAction.Shutdown)
            onExitApplication()
        },
        state = windowState,
        alwaysOnTop = state.isAlwaysOnTop,
    ) {
        DisposableEffect(Unit) { onDispose { onAction(MainAction.SaveSetting(this@Window.getWindowSize())) } }
        MaterialTheme(
            colorScheme =
                if (state.isDark == true) {
                    createDarkColorScheme(state.accentColor)
                } else {
                    createLightColorScheme(state.accentColor)
                },
        ) {
            App(
                state = state,
                onMainAction = onAction,
                topContent = topContent,
                commandContent = commandContent,
                textCommandContent = textCommandContent,
                screenshotContent = screenshotContent,
                appContent = appContent,
                logContent = logContent,
                settingContent = settingContent,
                deviceSettingsContent = deviceSettingsContent,
            )
        }
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
private fun App(
    state: MainState,
    onMainAction: (MainAction) -> Unit,
    topContent: @Composable () -> Unit,
    commandContent: @Composable (SplitPaneState) -> Unit,
    textCommandContent: @Composable (SplitPaneState, SplitPaneState) -> Unit,
    screenshotContent: @Composable (SplitPaneState, SplitPaneState) -> Unit,
    appContent: @Composable (SplitPaneState, SplitPaneState) -> Unit,
    logContent: @Composable () -> Unit,
    settingContent: @Composable () -> Unit,
    deviceSettingsContent: @Composable () -> Unit,
) {
    val commandSplitPaneState =
        rememberSplitPaneState(
            initialPositionPercentage = 0.55f,
        )
    val textSplitPaneState =
        rememberSplitPaneState(
            initialPositionPercentage = 0.1f,
        )
    val textRightSplitPaneState =
        rememberSplitPaneState(
            initialPositionPercentage = 0.8f,
        )
    val screenshotSplitPaneState =
        rememberSplitPaneState(
            initialPositionPercentage = 0.1f,
        )
    val screenshotRightSplitPaneState =
        rememberSplitPaneState(
            initialPositionPercentage = 0.8f,
        )
    val appSplitPaneState =
        rememberSplitPaneState(
            initialPositionPercentage = 0.1f,
        )
    val appRightSplitPaneState =
        rememberSplitPaneState(
            initialPositionPercentage = 0.8f,
        )

    Crossfade(state.language) {
        val commandCategoryContent =
            remember {
                movableContentOf {
                    commandContent(commandSplitPaneState)
                }
            }
        val textCommandCategoryContent =
            remember {
                movableContentOf {
                    textCommandContent(textSplitPaneState, textRightSplitPaneState)
                }
            }
        val screenshotCategoryContent =
            remember {
                movableContentOf {
                    screenshotContent(screenshotSplitPaneState, screenshotRightSplitPaneState)
                }
            }
        val appCategoryContent =
            remember {
                movableContentOf {
                    appContent(appSplitPaneState, appRightSplitPaneState)
                }
            }
        val logCategoryContent =
            remember {
                movableContentOf {
                    logContent()
                }
            }
        Surface {
            ScreenLayout(
                top = topContent,
                navigationRail = {
                    NavigationRail(
                        category = state.category,
                        isCollapsed = state.isNavigationRailCollapsed,
                        onSelectCategory = { onMainAction(MainAction.ClickCategory(it)) },
                        onOpenSetting = { onMainAction(MainAction.OpenSetting) },
                    )
                },
                content = {
                    Column(Modifier.fillMaxSize()) {
                        state.availableUpdate?.let { available ->
                            UpdateBanner(
                                version = available.version,
                                isInstalling = state.isInstallingUpdate,
                                failed = state.updateFailed,
                                lastInstallFailed = state.lastInstallFailed,
                                onInstall = { onMainAction(MainAction.InstallUpdate) },
                                onDismiss = { onMainAction(MainAction.DismissUpdate) },
                            )
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            CategoryContentLayer(
                                isSelected = state.category == MainCategory.Command,
                                content = commandCategoryContent,
                            )
                            CategoryContentLayer(
                                isSelected = state.category == MainCategory.Text,
                                content = textCommandCategoryContent,
                            )
                            CategoryContentLayer(
                                isSelected = state.category == MainCategory.Screenshot,
                                content = screenshotCategoryContent,
                            )
                            CategoryContentLayer(
                                isSelected = state.category == MainCategory.App,
                                content = appCategoryContent,
                            )
                            CategoryContentLayer(
                                isSelected = state.category == MainCategory.Log,
                                content = logCategoryContent,
                            )
                        }
                    }
                },
                dialog = {
                    when (state.dialog) {
                        MainDialog.Setting -> {
                            settingContent()
                        }

                        is MainDialog.DeviceSettings -> {
                            deviceSettingsContent()
                        }

                        MainDialog.AdbError -> {
                            AdbErrorScreen(
                                onOpenSetting = { onMainAction(MainAction.OpenSetting) },
                            )
                        }

                        MainDialog.Empty -> {
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun UpdateBanner(
    version: String,
    isInstalling: Boolean,
    failed: Boolean,
    lastInstallFailed: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                text =
                    when {
                        isInstalling -> Language.updateBannerDownloading
                        failed -> Language.settingUpdateDownloadFailed
                        lastInstallFailed -> Language.updateInstallFailed
                        else -> Language.updateBannerFormat.format(version)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if ((failed || lastInstallFailed) &&
                        !isInstalling
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier = Modifier.weight(1f),
            )
            if (isInstalling) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onInstall) { Text(Language.updateBannerInstall) }
                TextButton(onClick = onDismiss) { Text(Language.updateBannerLater) }
            }
        }
    }
}

@Composable
private fun CategoryContentLayer(
    isSelected: Boolean,
    content: @Composable () -> Unit,
) {
    val inactiveModifier =
        if (isSelected) {
            Modifier
        } else {
            Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(if (isSelected) 1f else 0f)
                .zIndex(if (isSelected) 1f else 0f)
                .focusProperties { canFocus = isSelected }
                .then(inactiveModifier),
    ) {
        content()
    }
}
