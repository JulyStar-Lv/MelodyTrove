package io.github.julystar.musicapp.core

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * Gives every Media3 controller the same queue semantics as MelodyTrove's in-app controls.
 *
 * The native queue handles adjacent items directly. Boundary callbacks bridge back to the full
 * application queue when the bounded Media3 window cannot satisfy a command. STOP remains a
 * resumable pause, matching the explicit headset/Bluetooth key behavior.
 */
@OptIn(UnstableApi::class)
internal class MelodyTroveSessionPlayer(
    player: Player,
    private val onNextBoundary: () -> Unit,
    private val onPreviousBoundary: () -> Unit,
) : ForwardingPlayer(player) {
    override fun stop() {
        pause()
    }

    override fun seekToNextMediaItem() {
        seekAdjacentOrBoundary(offset = 1, onBoundary = onNextBoundary)
    }

    override fun seekToNext() {
        seekAdjacentOrBoundary(offset = 1, onBoundary = onNextBoundary)
    }

    override fun seekToPreviousMediaItem() {
        seekAdjacentOrBoundary(offset = -1, onBoundary = onPreviousBoundary)
    }

    override fun seekToPrevious() {
        seekAdjacentOrBoundary(offset = -1, onBoundary = onPreviousBoundary)
    }

    private fun seekAdjacentOrBoundary(offset: Int, onBoundary: () -> Unit) {
        val index = currentMediaItemIndex
        val target = index + offset
        if (mediaItemCount > 0 && index in 0 until mediaItemCount && target in 0 until mediaItemCount) {
            seekToDefaultPosition(target)
            play()
        } else {
            onBoundary()
        }
    }
}
