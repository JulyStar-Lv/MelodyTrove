package io.github.julystar.musicapp

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import org.koin.core.context.GlobalContext

/**
 * Routes headset/Bluetooth/OEM media-button broadcasts to the Media3 playback service while this
 * app process still owns a resumable current item.
 *
 * Cold-start playback resumption is intentionally not enabled here. The receiver therefore avoids
 * starting a foreground service when the process has no current playback item, which keeps the
 * fallback focused on active/background playback and avoids an FGS start that cannot begin media.
 */
@OptIn(UnstableApi::class)
class BackgroundMediaButtonReceiver : MediaButtonReceiver() {
    override fun shouldStartForegroundService(context: Context, intent: Intent): Boolean {
        val playbackController = runCatching {
            GlobalContext.get().get<PlaybackController>()
        }.getOrNull() ?: return false

        return playbackController.state.value.currentItem != null
    }
}
