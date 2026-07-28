package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.LogFile
import io.github.julystar.musicapp.core.domain.repository.LogRepository
import io.github.julystar.musicapp.singleton.Bridge
import uniffi.app_backend.ctTriggerError
import uniffi.app_backend.ctsListLogFiles
import uniffi.app_backend.ctsTriggerError
import uniffi.app_backend.ctsTriggerPanic

class RustLogRepository(
    private val bridge: Bridge,
) : LogRepository {

    override fun listLogFiles(): List<LogFile> {
        val result = bridge.runSync { ctsListLogFiles(it) }
        return result?.files?.map { LogFile(name = it.name, path = it.path) } ?: emptyList()
    }

    override fun triggerTestError() {
        bridge.runSyncRaw { ctsTriggerError(it) }
    }

    override fun triggerTestPanic() {
        bridge.runSyncRaw { ctsTriggerPanic(it) }
    }
}
