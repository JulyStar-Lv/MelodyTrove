package com.github.tidetunes.core.domain.model

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

enum class SourceEditorType {
    WebDav,
    OneDrive,
}

enum class SourceConnectionTestStatus {
    None,
    Testing,
    Success,
    Error,
}

data class SourceEditorStorageState(
    val accountId: SourceAccountId,
    val draft: SourceEditorDraft,
    val title: String,
    val musicCount: ULong,
    val isOneDrive: Boolean,
)

data class OneDriveDriveListResult(
    val drives: List<OneDriveDriveInfo>,
    val refreshedToken: String,
)
