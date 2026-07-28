package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.DomainAlbumDetail

interface AlbumDetailRepository {
    suspend fun loadAlbumDetail(albumId: Long): DomainAlbumDetail
}
