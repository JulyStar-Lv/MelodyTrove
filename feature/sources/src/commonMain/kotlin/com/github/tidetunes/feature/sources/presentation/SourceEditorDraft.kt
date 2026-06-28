package com.github.tidetunes.feature.sources.presentation

data class SourceEditorDraft(
    val id: Long? = null,
    val address: String = "",
    val alias: String = "",
    val username: String = "",
    val secret: String = "",
    val isAnonymous: Boolean = true,
    val storageType: SourceEditorType = SourceEditorType.WebDav,
)

fun defaultSourceEditorDraft(): SourceEditorDraft {
    return SourceEditorDraft()
}
