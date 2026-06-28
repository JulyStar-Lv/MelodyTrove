package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.LogFile
import com.github.tidetunes.core.domain.repository.LogRepository
import com.github.tidetunes.singleton.Bridge
import uniffi.tidetunes_core.ctTriggerError
import uniffi.tidetunes_core.ctsListLogFiles
import uniffi.tidetunes_core.ctsTriggerError
import uniffi.tidetunes_core.ctsTriggerPanic

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
