package jp.kaleidot725.adbpad.domain.usecase.app

import jp.kaleidot725.adbpad.domain.repository.ScrcpyNewDisplayProcessRepository
import jp.kaleidot725.adbpad.domain.repository.ScrcpyProcessRepository
import kotlin.system.exitProcess

class ShutdownAppUseCase(
    private val scrcpyProcessRepository: ScrcpyProcessRepository,
    private val scrcpyNewDisplayProcessRepository: ScrcpyNewDisplayProcessRepository,
) {
    operator fun invoke() {
        // Terminate all running Scrcpy processes
        scrcpyProcessRepository.terminateAllProcesses()
        scrcpyNewDisplayProcessRepository.terminateAllProcesses()

        println("Application shutdown complete.")

        // ponytail: Compose Desktop's exitApplication() only tears down windows/composition and
        // relies on the JVM exiting naturally once every thread is a daemon thread - one of our
        // networking dependencies (scrcpy-kt pulls in Netty/gRPC/Vert.x) leaves a non-daemon
        // thread running, so the process never actually dies and keeps holding its full heap.
        // Force it.
        exitProcess(0)
    }
}
