package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.core.data.AlbumDetailRepositoryImpl
import io.github.julystar.musicapp.core.data.ArtistDetailRepositoryImpl
import io.github.julystar.musicapp.core.data.BrowseRepositoryImpl
import io.github.julystar.musicapp.core.data.DataStoreFavoritesRepository
import io.github.julystar.musicapp.core.data.LibraryRepositoryImpl
import io.github.julystar.musicapp.core.data.LyricsRepositoryImpl
import io.github.julystar.musicapp.core.data.NavidromeLyricsResolver
import io.github.julystar.musicapp.core.data.OpenSubsonicLyricsResolver
import io.github.julystar.musicapp.core.data.PlaylistImportTargetImpl
import io.github.julystar.musicapp.core.data.PlaylistRepositoryImpl
import io.github.julystar.musicapp.core.data.StubDownloadCollectionRepository
import io.github.julystar.musicapp.core.data.StubFolderRepository
import io.github.julystar.musicapp.core.data.StubGenreRepository
import io.github.julystar.musicapp.core.data.StubHistoryRepository
import io.github.julystar.musicapp.core.data.StubLosslessRepository
import io.github.julystar.musicapp.core.data.TrackBrowserRepositoryImpl
import io.github.julystar.musicapp.core.data.media.AssetRepository
import io.github.julystar.musicapp.core.data.media.LegacyArtworkRepository
import io.github.julystar.musicapp.core.data.media.NavidromeArtworkResolver
import io.github.julystar.musicapp.core.data.media.PluginArtworkResolver
import io.github.julystar.musicapp.core.domain.repository.AlbumDetailRepository
import io.github.julystar.musicapp.core.domain.repository.ArtistDetailRepository
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.core.domain.repository.BrowseRepository
import io.github.julystar.musicapp.core.domain.repository.DownloadCollectionRepository
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.repository.FolderRepository
import io.github.julystar.musicapp.core.domain.repository.GenreRepository
import io.github.julystar.musicapp.core.domain.repository.HistoryRepository
import io.github.julystar.musicapp.core.domain.repository.LibraryRepository
import io.github.julystar.musicapp.core.domain.repository.LosslessRepository
import io.github.julystar.musicapp.core.domain.repository.LyricsRepository
import io.github.julystar.musicapp.core.domain.repository.PlaylistRepository
import io.github.julystar.musicapp.core.domain.repository.TrackBrowserRepository
import io.github.julystar.musicapp.source.api.PlaylistImportTarget
import okio.FileSystem
import org.koin.dsl.module

val libraryDataModule = module {
    single<FileSystem> { FileSystem.SYSTEM }
    single { AssetRepository(get(), get(), get()) }
    single { PluginArtworkResolver(get(), get(), get(), get(), get(), get()) }
    single { NavidromeArtworkResolver(get(), get(), get(), get(), get(), get()) }
    single<ArtworkRepository> { LegacyArtworkRepository(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<LibraryRepository> { LibraryRepositoryImpl(get(), get(), get(), get()) }
    single<BrowseRepository> { BrowseRepositoryImpl(get(), get()) }
    single<TrackBrowserRepository> { TrackBrowserRepositoryImpl(get(), get()) }
    single { NavidromeLyricsResolver(get(), get(), get(), get(), get()) }
    single { OpenSubsonicLyricsResolver(get(), get(), get(), get(), get()) }
    single<LyricsRepository> { LyricsRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<AlbumDetailRepository> { AlbumDetailRepositoryImpl(get(), get()) }
    single<ArtistDetailRepository> { ArtistDetailRepositoryImpl(get(), get()) }
    single { PlaylistRepositoryImpl(get(), get(), get(), get(), get()) }
    single<PlaylistRepository> { get<PlaylistRepositoryImpl>() }
    single<PlaylistImportTarget> { PlaylistImportTargetImpl(get()) }
    single<GenreRepository> { StubGenreRepository() }
    single<FolderRepository> { StubFolderRepository() }
    single<FavoritesRepository> { DataStoreFavoritesRepository(get(), get()) }
    single<HistoryRepository> { StubHistoryRepository() }
    single<LosslessRepository> { StubLosslessRepository() }
    single<DownloadCollectionRepository> { StubDownloadCollectionRepository() }
}
