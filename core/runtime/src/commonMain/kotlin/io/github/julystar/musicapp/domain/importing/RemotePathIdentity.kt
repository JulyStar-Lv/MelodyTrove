package io.github.julystar.musicapp.domain.importing

enum class RemotePathSemantics {
    Legacy,
    OpenListRaw,
}

fun normalizeRemotePath(
    path: String,
    pathSemantics: RemotePathSemantics = RemotePathSemantics.Legacy,
): String {
    if (path.isBlank()) return "/"
    val normalized = when (pathSemantics) {
        RemotePathSemantics.Legacy -> path.replace('\\', '/')
        RemotePathSemantics.OpenListRaw -> path
    }
    return if (normalized.startsWith('/')) normalized else "/$normalized"
}

fun String.toLegacyAndroidPrimaryStoragePath(): String? = when {
    this == "/" -> ANDROID_PRIMARY_STORAGE_PATH
    startsWith("$ANDROID_PRIMARY_STORAGE_PATH/") -> null
    else -> "$ANDROID_PRIMARY_STORAGE_PATH$this"
}

fun stableTrackId(
    storageId: Long,
    canonicalPath: String,
    pathSemantics: RemotePathSemantics = RemotePathSemantics.Legacy,
): Long {
    var hash = -3_750_763_034_362_895_579L
    val value = "track:$storageId:${normalizeRemotePath(canonicalPath, pathSemantics)}"
    value.forEach { ch ->
        hash = hash xor ch.code.toLong()
        hash *= 1_099_511_628_211L
    }
    val positive = hash and Long.MAX_VALUE
    return if (positive == 0L) 1L else positive
}

private const val ANDROID_PRIMARY_STORAGE_PATH = "/storage/emulated/0"

const val DURATION_MATCH_TOLERANCE_MS = 2_000L
