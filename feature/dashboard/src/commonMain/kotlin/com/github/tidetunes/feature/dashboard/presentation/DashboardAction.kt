package com.github.tidetunes.feature.dashboard.presentation

sealed interface DashboardAction {
    data object OpenSleepTimer : DashboardAction
    data object NavigateToDownloads : DashboardAction
    data object NavigateToAddDevice : DashboardAction
    data class PauseImport(val id: String) : DashboardAction
    data class ResumeImport(val id: String) : DashboardAction
    data class RetryImport(val id: String) : DashboardAction
    data class CancelImport(val id: String) : DashboardAction
}
