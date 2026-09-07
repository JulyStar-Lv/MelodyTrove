package io.github.julystar.musicapp.car.presentation.focus

import androidx.compose.runtime.Immutable
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

@JvmInline
value class CarFocusId(val value: String)

object CarFocusIds {
    val Exit = CarFocusId("shell.exit")
    val Home = CarFocusId("navigation.home")
    val Library = CarFocusId("navigation.library")
    val MiniPlayer = CarFocusId("shell.mini_player")
}

@Immutable
data class CarFocusRegistry(
    val ids: Set<CarFocusId> = setOf(CarFocusIds.Exit, CarFocusIds.Home, CarFocusIds.Library, CarFocusIds.MiniPlayer),
    val rememberedByRoute: Map<String, CarFocusId> = emptyMap(),
) {
    fun remember(route: String, id: CarFocusId): CarFocusRegistry =
        copy(rememberedByRoute = rememberedByRoute + (route to id))

    fun restore(route: String): CarFocusId? = rememberedByRoute[route]?.takeIf(ids::contains)
}

val CarFocusIdSemanticsKey = SemanticsPropertyKey<String>("CarFocusId")

var SemanticsPropertyReceiver.carFocusId by CarFocusIdSemanticsKey
