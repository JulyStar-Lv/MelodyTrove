package io.github.julystar.musicapp.source.api

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MusicSourceRegistryTest {
    @Test
    fun returnsRegisteredSourcesById() {
        val localSource = fakeMusicSource(id = SourceId("local"), name = "Local")
        val webDavSource = fakeMusicSource(id = SourceId("webdav"), name = "WebDAV")
        val registry = MusicSourceRegistry(listOf(localSource, webDavSource))

        assertEquals(localSource, registry.source(BuiltInSourceIds.Local))
        assertEquals(webDavSource, registry.sourceOrNull(BuiltInSourceIds.WebDav))
        assertNull(registry.sourceOrNull(SourceId("missing")))
        assertEquals(2, registry.sources.size)
    }

    @Test
    fun rejectsDuplicateSourceIds() {
        assertFailsWith<IllegalArgumentException> {
            MusicSourceRegistry(
                listOf(
                    fakeMusicSource(id = SourceId("duplicate"), name = "One"),
                    fakeMusicSource(id = SourceId("duplicate"), name = "Two"),
                )
            )
        }
    }

    private fun fakeMusicSource(
        id: SourceId,
        name: String,
    ) = object : MusicSource {
        override val descriptor = MusicSourceDescriptor(
            id = id,
            displayName = name,
        )
        override val capabilities = emptySet<SourceCapability>()

        override suspend fun authenticate(configuration: SourceConfiguration): SourceAuthResult {
            return SourceAuthResult.Success
        }

        override suspend fun list(
            accountId: SourceAccountId,
            directoryId: String?,
        ): SourceListResult {
            return SourceListResult.Success(emptyList())
        }

        override suspend fun resolvePlayback(mediaId: MediaId): SourcePlaybackResult {
            return SourcePlaybackResult.Failure(SourcePlaybackFailureReason.Unavailable)
        }
    }
}
