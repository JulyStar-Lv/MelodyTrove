package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.LogFile

interface LogRepository {
    fun listLogFiles(): List<LogFile>
    fun triggerTestError()
    fun triggerTestPanic()
}
