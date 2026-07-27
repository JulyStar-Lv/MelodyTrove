package com.github.tidetunes.core.domain.model

data class SourceEditorDraft(
    val id: Long? = null,
    val address: String = "",
    val alias: String = "",
    val username: String = "",
    val secret: String = "",
    val isAnonymous: Boolean = false,
    val storageType: SourceEditorType = SourceEditorType.WebDav,
    val externalAccountId: String = "",
    val smbHost: String = "",
    val smbPort: Int = 445,
    val smbShare: String = "",
    val smbRootPath: String = "",
    val smbDomain: String = "",
    val smbRequireSigning: Boolean = false,
    val smbRequireEncryption: Boolean = false,
)

fun defaultSourceEditorDraft(): SourceEditorDraft {
    return SourceEditorDraft()
}

enum class SourceEditorType {
    WebDav,
    OneDrive,
    Smb,
    Navidrome,
    OpenSubsonic,
    Emby,
}

enum class SourceConnectionTestStatus {
    None,
    Testing,
    Success,
    Unauthorized,
    Timeout,
    PermissionDenied,
    NotFound,
    InvalidAddress,
    Unavailable,
    UnsupportedSecurityPolicy,
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
