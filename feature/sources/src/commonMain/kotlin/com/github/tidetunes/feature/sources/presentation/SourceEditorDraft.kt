package com.github.tidetunes.feature.sources.presentation

typealias SourceEditorDraft = com.github.tidetunes.core.domain.model.SourceEditorDraft
typealias SourceEditorType = com.github.tidetunes.core.domain.model.SourceEditorType
typealias SourceConnectionTestStatus = com.github.tidetunes.core.domain.model.SourceConnectionTestStatus

fun defaultSourceEditorDraft(): SourceEditorDraft {
    return com.github.tidetunes.core.domain.model.defaultSourceEditorDraft()
}
