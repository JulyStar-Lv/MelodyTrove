package io.github.julystar.musicapp.feature.sources.presentation

typealias SourceEditorDraft = io.github.julystar.musicapp.core.domain.model.SourceEditorDraft
typealias SourceEditorType = io.github.julystar.musicapp.core.domain.model.SourceEditorType
typealias SourceConnectionTestStatus = io.github.julystar.musicapp.core.domain.model.SourceConnectionTestStatus

fun defaultSourceEditorDraft(): SourceEditorDraft {
    return io.github.julystar.musicapp.core.domain.model.defaultSourceEditorDraft()
}
