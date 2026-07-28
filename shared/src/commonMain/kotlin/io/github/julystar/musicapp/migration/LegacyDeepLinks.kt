package io.github.julystar.musicapp.migration

internal object LegacyDeepLinks {
    const val SCHEME = "tidetunes"
    const val OAUTH_HOST = "oauth2redirect"
    const val OAUTH_REDIRECT_URI = "$SCHEME://$OAUTH_HOST/"
}
