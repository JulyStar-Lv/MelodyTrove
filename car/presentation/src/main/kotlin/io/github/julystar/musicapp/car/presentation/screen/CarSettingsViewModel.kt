package io.github.julystar.musicapp.car.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.julystar.musicapp.core.domain.model.AppLanguageMode
import io.github.julystar.musicapp.core.domain.model.AppSettings
import io.github.julystar.musicapp.core.domain.model.AppThemeMode
import io.github.julystar.musicapp.core.domain.model.AudioFocusMode
import io.github.julystar.musicapp.core.domain.repository.PermissionChecker
import io.github.julystar.musicapp.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CarSettingsUiState(
    val settings: AppSettings = AppSettings(),
    val localLibraryState: CarLocalLibraryState = CarLocalLibraryState.Idle,
    val mutationError: String? = null,
)

sealed interface CarSettingsAction {
    data object ToggleTheme : CarSettingsAction
    data object ToggleArtworkTheme : CarSettingsAction
    data object CycleLanguage : CarSettingsAction
    data object ToggleLyricTranslation : CarSettingsAction
    data object ToggleLyricWordLift : CarSettingsAction
    data object ToggleLyricBlur : CarSettingsAction
    data object ToggleLyricTapToSeek : CarSettingsAction
    data object CycleAudioFocus : CarSettingsAction
    data object TogglePauseOnDisconnect : CarSettingsAction
    data object ToggleGaplessPlayback : CarSettingsAction
    data object TogglePlaybackRetry : CarSettingsAction
    data object ToggleResumeAfterNetworkRecovery : CarSettingsAction
    data object ToggleMeteredNetwork : CarSettingsAction
    data object ToggleListenAndCache : CarSettingsAction
    data object ToggleScanSubdirectories : CarSettingsAction
    data object ImportLocalMusic : CarSettingsAction
}

class CarSettingsViewModel(
    private val repository: SettingsRepository,
    private val permissionChecker: PermissionChecker,
    private val importer: CarLocalLibraryImporter,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CarSettingsUiState())
    private var importJob: Job? = null
    private var importAfterPermission = false

    val state: StateFlow<CarSettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                mutableState.value = mutableState.value.copy(settings = settings)
            }
        }
        viewModelScope.launch {
            permissionChecker.havePermission.collectLatest { granted ->
                if (granted && importAfterPermission) {
                    importAfterPermission = false
                    startImport()
                }
            }
        }
    }

    fun onAction(action: CarSettingsAction) {
        when (action) {
            CarSettingsAction.ImportLocalMusic -> importLocalMusic()
            else -> mutateSettings(action)
        }
    }

    private fun mutateSettings(action: CarSettingsAction) {
        val settings = mutableState.value.settings
        viewModelScope.launch {
            try {
                when (action) {
                    CarSettingsAction.ToggleTheme -> repository.setThemeMode(
                        if (settings.themeMode == AppThemeMode.Dark) AppThemeMode.Light else AppThemeMode.Dark,
                    )
                    CarSettingsAction.ToggleArtworkTheme ->
                        repository.setArtworkThemeEnabled(!settings.artworkThemeEnabled)
                    CarSettingsAction.CycleLanguage -> repository.setLanguageMode(
                        when (settings.languageMode) {
                            AppLanguageMode.System -> AppLanguageMode.Chinese
                            AppLanguageMode.Chinese -> AppLanguageMode.English
                            AppLanguageMode.English -> AppLanguageMode.System
                        },
                    )
                    CarSettingsAction.ToggleLyricTranslation ->
                        repository.setLyricTranslationVisible(!settings.lyrics.showTranslation)
                    CarSettingsAction.ToggleLyricWordLift ->
                        repository.setLyricWordLiftEnabled(!settings.lyrics.wordLiftEnabled)
                    CarSettingsAction.ToggleLyricBlur ->
                        repository.setLyricBlurEffectEnabled(!settings.lyrics.blurEffectEnabled)
                    CarSettingsAction.ToggleLyricTapToSeek ->
                        repository.setLyricTapToSeekEnabled(!settings.lyrics.tapToSeekEnabled)
                    CarSettingsAction.CycleAudioFocus -> repository.setAudioFocusMode(
                        when (settings.audioFocusMode) {
                            AudioFocusMode.Pause -> AudioFocusMode.Duck
                            AudioFocusMode.Duck -> AudioFocusMode.Mix
                            AudioFocusMode.Mix -> AudioFocusMode.Pause
                        },
                    )
                    CarSettingsAction.TogglePauseOnDisconnect ->
                        repository.setPauseOnDisconnect(!settings.pauseOnDisconnect)
                    CarSettingsAction.ToggleGaplessPlayback ->
                        repository.setGaplessPlaybackEnabled(!settings.gaplessPlaybackEnabled)
                    CarSettingsAction.TogglePlaybackRetry ->
                        repository.setRetryPlaybackOnFailure(!settings.retryPlaybackOnFailure)
                    CarSettingsAction.ToggleResumeAfterNetworkRecovery ->
                        repository.setResumePlaybackAfterNetworkRecovery(!settings.resumePlaybackAfterNetworkRecovery)
                    CarSettingsAction.ToggleMeteredNetwork ->
                        repository.setAllowMeteredNetworkUsage(!settings.allowMeteredNetworkUsage)
                    CarSettingsAction.ToggleListenAndCache ->
                        repository.setListenAndCacheEnabled(!settings.listenAndCacheEnabled)
                    CarSettingsAction.ToggleScanSubdirectories ->
                        repository.setScanSubdirectories(!settings.scanSubdirectories)
                    CarSettingsAction.ImportLocalMusic -> Unit
                }
                mutableState.value = mutableState.value.copy(mutationError = null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    mutationError = error.message ?: "设置更新失败",
                )
            }
        }
    }

    private fun importLocalMusic() {
        if (importJob?.isActive == true) return
        if (!permissionChecker.havePermission.value) {
            importAfterPermission = true
            mutableState.value = mutableState.value.copy(
                localLibraryState = CarLocalLibraryState.AwaitingPermission,
            )
            permissionChecker.requestStoragePermission()
            return
        }
        startImport()
    }

    private fun startImport() {
        if (importJob?.isActive == true) return
        importJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(localLibraryState = CarLocalLibraryState.Scanning)
            val result = try {
                CarLocalLibraryState.Complete(importer.importMusicFolder())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                CarLocalLibraryState.Failed(error.message ?: "无法扫描本地音乐")
            }
            mutableState.value = mutableState.value.copy(localLibraryState = result)
        }
    }
}
