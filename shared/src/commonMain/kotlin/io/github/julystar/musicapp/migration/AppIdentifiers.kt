package io.github.julystar.musicapp.migration

internal object AppIdentifiers {
    const val BRAND_NAME = "MelodyTrove"
    const val CHINESE_NAME = "旋律珍藏"
    const val BRAND_SLUG = "melodytrove"
    const val PACKAGE_ID = "io.github.julystar.musicapp"
    const val DEEP_LINK_SCHEME = BRAND_SLUG
    const val CREDENTIAL_SERVICE = "$PACKAGE_ID.credentials"
    const val DATABASE_FILE = "library.db"
    const val PREFERENCES_FILE = "settings.preferences_pb"
}
