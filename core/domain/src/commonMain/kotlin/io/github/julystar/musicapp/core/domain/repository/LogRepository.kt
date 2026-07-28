package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.LogFile

interface LogRepository {
    fun listLogFiles(): List<LogFile>
    fun triggerTestError()
    fun triggerTestPanic()
}
