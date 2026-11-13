package jp.kaleidot725.adbpad.domain.usecase.adb

import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.interactor.StopAdbInteractor
import jp.kaleidot725.adbpad.domain.model.setting.SdkPath
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RestartAdbUseCase(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(
        oldServerPort: Int,
        oldAdbDirectory: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                StopAdbInteractor().execute(
                    adbBinary = SdkPath(oldAdbDirectory, oldServerPort).adbFile,
                    serverPort = oldServerPort,
                )

                val sdkPath = settingRepository.getSdkPath()
                return@withContext StartAdbInteractor().execute(
                    adbBinary = sdkPath.adbFile,
                    serverPort = sdkPath.adbServerPort,
                )
            } catch (e: Exception) {
                println(e)
                return@withContext false
            }
        }
    }
}
