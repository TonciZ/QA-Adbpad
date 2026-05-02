package jp.kaleidot725.adbpad.domain.usecase.app

import jp.kaleidot725.adbpad.domain.model.app.InstalledApp
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.repository.InstalledAppRepository

class UninstallInstalledAppUseCase(
    private val installedAppRepository: InstalledAppRepository,
) {
    suspend operator fun invoke(
        device: Device,
        app: InstalledApp,
    ) {
        installedAppRepository.uninstallInstalledApp(device, app)
    }
}
