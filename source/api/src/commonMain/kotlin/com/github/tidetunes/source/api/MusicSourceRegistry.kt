package com.github.tidetunes.source.api

import com.github.tidetunes.core.domain.model.SourceId

class MusicSourceRegistry(
    sources: Collection<MusicSource>,
) {
    private val sourcesById = sources.associateBy { source ->
        source.descriptor.id
    }

    init {
        require(sourcesById.size == sources.size) {
            "MusicSourceRegistry source IDs must be unique"
        }
    }

    val sources: Collection<MusicSource> = sourcesById.values

    fun source(id: SourceId): MusicSource {
        return requireNotNull(sourcesById[id]) {
            "Unknown music source: ${id.value}"
        }
    }

    fun sourceOrNull(id: SourceId): MusicSource? {
        return sourcesById[id]
    }
}
