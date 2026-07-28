package io.github.julystar.musicapp.core.presentation.theme

import androidx.compose.runtime.Immutable
import io.github.julystar.musicapp.core.domain.model.DEFAULT_MANUAL_THEME_SEED_ARGB
import io.github.julystar.musicapp.core.domain.model.normalizeThemeSeedArgb

enum class ArtworkThemeSeedStatus {
    Disabled,
    Loading,
    Available,
    Missing,
    Failed,
}

enum class ThemeSeedSource {
    Artwork,
    PreviousArtwork,
    Manual,
}

@Immutable
data class ThemeSeedResolution(
    val effectiveSeedArgb: Long,
    val source: ThemeSeedSource,
)

@Immutable
data class ThemeSeedState(
    val artworkThemeEnabled: Boolean,
    val manualSeedArgb: Long,
    val effectiveSeedArgb: Long,
    val artworkStatus: ArtworkThemeSeedStatus,
    val source: ThemeSeedSource,
) {
    companion object {
        val Default = ThemeSeedState(
            artworkThemeEnabled = true,
            manualSeedArgb = DEFAULT_MANUAL_THEME_SEED_ARGB,
            effectiveSeedArgb = DEFAULT_MANUAL_THEME_SEED_ARGB,
            artworkStatus = ArtworkThemeSeedStatus.Missing,
            source = ThemeSeedSource.Manual,
        )
    }
}

fun canSelectManualThemeColor(artworkThemeEnabled: Boolean): Boolean {
    return !artworkThemeEnabled
}

fun resolveThemeSeed(
    artworkThemeEnabled: Boolean,
    artworkStatus: ArtworkThemeSeedStatus,
    artworkSeedArgb: Long?,
    previousValidArtworkSeedArgb: Long?,
    manualSeedArgb: Long,
): ThemeSeedResolution {
    val manual = normalizeThemeSeedArgb(manualSeedArgb)
    if (!artworkThemeEnabled) {
        return ThemeSeedResolution(manual, ThemeSeedSource.Manual)
    }
    return when (artworkStatus) {
        ArtworkThemeSeedStatus.Available -> artworkSeedArgb
            ?.let(::normalizeThemeSeedArgb)
            ?.let { ThemeSeedResolution(it, ThemeSeedSource.Artwork) }
            ?: ThemeSeedResolution(manual, ThemeSeedSource.Manual)

        ArtworkThemeSeedStatus.Loading -> previousValidArtworkSeedArgb
            ?.let(::normalizeThemeSeedArgb)
            ?.let { ThemeSeedResolution(it, ThemeSeedSource.PreviousArtwork) }
            ?: ThemeSeedResolution(manual, ThemeSeedSource.Manual)

        ArtworkThemeSeedStatus.Disabled,
        ArtworkThemeSeedStatus.Missing,
        ArtworkThemeSeedStatus.Failed,
        -> ThemeSeedResolution(manual, ThemeSeedSource.Manual)
    }
}
