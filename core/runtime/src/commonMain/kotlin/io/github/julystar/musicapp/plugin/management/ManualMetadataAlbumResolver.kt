package io.github.julystar.musicapp.plugin.management

import io.github.julystar.musicapp.database.AlbumEntity
import io.github.julystar.musicapp.database.MetadataDao

suspend fun MetadataDao.resolveManualMetadataAlbum(
    name: String,
    date: String?,
    currentAlbumId: Long?,
): Long {
    val normalized = name.trim().lowercase()
    val preservedArtworkId = currentAlbumId
        ?.let { albumId -> getArtworkForAlbum(albumId)?.id }
    insertAlbums(
        listOf(
            AlbumEntity(
                name = name,
                normalizedName = normalized,
                sortName = null,
                year = date?.take(4)?.toIntOrNull(),
                artworkId = preservedArtworkId,
            ),
        ),
    )
    val album = findAlbumsByNormalizedNames(listOf(normalized)).single()
    if (
        preservedArtworkId != null &&
        album.artworkId == null &&
        getArtworkForAlbum(album.id) == null
    ) {
        upsertAlbums(listOf(album.copy(artworkId = preservedArtworkId)))
    }
    return album.id
}
