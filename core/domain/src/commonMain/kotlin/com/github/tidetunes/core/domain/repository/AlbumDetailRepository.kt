package com.github.tidetunes.core.domain.repository

import com.github.tidetunes.core.domain.model.DomainAlbumDetail

interface AlbumDetailRepository {
    suspend fun loadAlbumDetail(albumId: Long): DomainAlbumDetail
}
