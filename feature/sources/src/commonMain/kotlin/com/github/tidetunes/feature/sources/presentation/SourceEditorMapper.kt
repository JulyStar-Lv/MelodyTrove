package com.github.tidetunes.feature.sources.presentation

import com.github.tidetunes.core.domain.model.OneDriveDriveInfo

fun sourceEditorState(
    draft: SourceEditorDraft,
    title: String,
    musicCount: ULong,
    validation: SourceEditorValidation,
    removeDialogOpen: Boolean,
    testResult: SourceConnectionTestStatus,
    oneDriveDrives: List<OneDriveDriveInfo> = emptyList(),
    oneDriveDrivesLoading: Boolean = false,
): SourceEditorState {
    return SourceEditorState(
        title = title,
        musicCount = musicCount,
        isCreated = draft.id == null,
        storageType = draft.storageType,
        testStatus = testResult,
        validation = validation,
        removeDialogOpen = removeDialogOpen,
        webDav = WebDavSourceEditorState(
            alias = draft.alias,
            address = draft.address,
            username = draft.username,
            isAnonymous = draft.isAnonymous,
        ),
        oneDrive = OneDriveSourceEditorState(
            alias = draft.alias,
            selectedDriveId = draft.address,
            connected = draft.secret.isNotEmpty(),
            drives = oneDriveDrives.toSourceEditorDriveUiList(),
            drivesLoading = oneDriveDrivesLoading,
        ),
    )
}

private fun List<OneDriveDriveInfo>.toSourceEditorDriveUiList(): List<SourceEditorDriveUi> {
    return map { drive ->
        SourceEditorDriveUi(
            id = drive.id,
            name = drive.name,
        )
    }
}
