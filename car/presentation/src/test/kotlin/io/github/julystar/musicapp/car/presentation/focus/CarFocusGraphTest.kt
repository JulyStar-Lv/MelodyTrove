package io.github.julystar.musicapp.car.presentation.focus

import kotlin.test.Test
import kotlin.test.assertEquals

class CarFocusGraphTest {
    @Test
    fun restoreReturnsRememberedLiveTarget() {
        val first = CarFocusIds.item("song", 1)
        val second = CarFocusIds.item("song", 2)
        val registry = CarFocusRegistry()
            .register(first)
            .register(second)
            .remember("songs", second)

        assertEquals(second, registry.restore("songs", first))
    }

    @Test
    fun restoreFallsBackWhenRememberedTargetIsGone() {
        val fallback = CarFocusIds.Songs
        val transient = CarFocusIds.item("song", 7)
        val registry = CarFocusRegistry()
            .register(fallback)
            .register(transient)
            .remember("songs", transient)
            .unregister(transient)

        assertEquals(fallback, registry.restore("songs", fallback))
    }

    @Test
    fun unknownTargetIsNotRemembered() {
        val fallback = CarFocusIds.Home
        val registry = CarFocusRegistry()
            .register(fallback)
            .remember("home", CarFocusIds.item("album", 99))

        assertEquals(fallback, registry.restore("home", fallback))
    }
}
