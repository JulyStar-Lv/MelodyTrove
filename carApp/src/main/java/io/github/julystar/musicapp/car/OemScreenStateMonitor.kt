package io.github.julystar.musicapp.car

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileHint
import io.github.julystar.musicapp.car.presentation.layout.carLayoutHintFromOemState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class OemScreenStateMonitor(
    private val contentResolver: ContentResolver,
) {
    private val mutableProfileHint = MutableStateFlow(readProfileHint())
    val profileHint: StateFlow<CarLayoutProfileHint> = mutableProfileHint.asStateFlow()

    private var started = false
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    fun start() {
        if (started) return
        try {
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(KEY_SCREEN_SHOW),
                false,
                observer,
            )
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(KEY_VPA_CUI_SHOW_LEFT),
                false,
                observer,
            )
            started = true
        } catch (error: SecurityException) {
            contentResolver.unregisterContentObserver(observer)
            Log.w(TAG, "OEM screen state cannot be observed; using the current/default state", error)
        }
        refresh()
    }

    fun stop() {
        if (!started) return
        contentResolver.unregisterContentObserver(observer)
        started = false
    }

    private fun refresh() {
        mutableProfileHint.value = readProfileHint()
    }

    private fun readProfileHint(): CarLayoutProfileHint = try {
        carLayoutHintFromOemState(
            keyScreenShow = Settings.Global.getInt(contentResolver, KEY_SCREEN_SHOW, 0),
            keyVpaCuiShowLeft = Settings.Global.getInt(contentResolver, KEY_VPA_CUI_SHOW_LEFT, 0),
        )
    } catch (error: SecurityException) {
        Log.w(TAG, "OEM screen state cannot be read; using the expanded state", error)
        CarLayoutProfileHint.Expanded
    }

    private companion object {
        const val TAG = "TideCarWindow"
        const val KEY_SCREEN_SHOW = "key_screen_show"
        const val KEY_VPA_CUI_SHOW_LEFT = "key_vpa_cui_show_left"
    }
}
