package com.github.tidetunes.feature.sources.presentation

import com.github.tidetunes.core.data.toArgUpsertStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.github.tidetunes.core.domain.model.OneDriveDriveInfo
import uniffi.tidetunes_backend.StorageId
import uniffi.tidetunes_backend.StorageConnectionTestResult
import uniffi.tidetunes_backend.StorageType

class SourceEditorStateTest {
    @Test
    fun webDavStateDoesNotExposePassword() {
        val state = sourceEditorState(
            draft = sourceEditorDraft(
                address = "https://dav.example.com/music",
                alias = "WebDAV",
                username = "alice",
                secret = "secret-password",
                isAnonymous = false,
                storageType = SourceEditorType.WebDav,
            ),
            title = "WebDAV",
            musicCount = 12u,
            validation = SourceEditorValidation(),
            removeDialogOpen = false,
            testResult = SourceConnectionTestStatus.Success,
        )

        assertEquals(SourceEditorType.WebDav, state.storageType)
        assertEquals(SourceConnectionTestStatus.Success, state.testStatus)
        assertFalse(
            listOf(
                state.webDav.alias,
                state.webDav.address,
                state.webDav.username,
                state.oneDrive.alias,
                state.oneDrive.selectedDriveId,
            ).contains("secret-password")
        )
    }

    @Test
    fun oneDriveStateKeepsOnlyConnectionStatusFromToken() {
        val state = sourceEditorState(
            draft = sourceEditorDraft(
                address = "drive-id",
                alias = "OneDrive",
                secret = "refresh-token",
                storageType = SourceEditorType.OneDrive,
            ),
            title = "OneDrive",
            musicCount = 0u,
            validation = SourceEditorValidation(),
            removeDialogOpen = false,
            testResult = SourceConnectionTestStatus.None,
        )

        assertEquals(SourceEditorType.OneDrive, state.storageType)
        assertTrue(state.oneDrive.connected)
        assertFalse(
            listOf(
                state.webDav.alias,
                state.webDav.address,
                state.webDav.username,
                state.oneDrive.alias,
                state.oneDrive.selectedDriveId,
            ).contains("refresh-token")
        )
    }

    @Test
    fun oneDriveDrivesMapToUiModels() {
        val state = sourceEditorState(
            draft = sourceEditorDraft(
                address = "drive-2",
                alias = "OneDrive",
                secret = "refresh-token",
                storageType = SourceEditorType.OneDrive,
            ),
            title = "OneDrive",
            musicCount = 0u,
            validation = SourceEditorValidation(),
            removeDialogOpen = false,
            testResult = SourceConnectionTestStatus.None,
            oneDriveDrives = listOf(
                oneDriveDrive(id = "drive-1", name = "Personal"),
                oneDriveDrive(id = "drive-2", name = "Music"),
            ),
            oneDriveDrivesLoading = true,
        )

        assertTrue(state.oneDrive.drivesLoading)
        assertEquals("drive-2", state.oneDrive.selectedDriveId)
        assertEquals(
            listOf(
                SourceEditorDriveUi(id = "drive-1", name = "Personal"),
                SourceEditorDriveUi(id = "drive-2", name = "Music"),
            ),
            state.oneDrive.drives,
        )
    }

    @Test
    fun draftConvertsToRepositoryArgumentOnlyAtBoundary() {
        val arg = sourceEditorDraft(
            id = 42,
            address = "drive-id",
            alias = "OneDrive",
            username = "user@example.com",
            secret = "refresh-token",
            isAnonymous = false,
            storageType = SourceEditorType.OneDrive,
        ).toArgUpsertStorage()

        assertEquals(StorageId(42), arg.id)
        assertEquals("drive-id", arg.addr)
        assertEquals("OneDrive", arg.alias)
        assertEquals("user@example.com", arg.username)
        assertEquals("refresh-token", arg.password)
        assertFalse(arg.isAnonymous)
        assertEquals(StorageType.ONE_DRIVE, arg.typ)
    }

    private fun sourceEditorDraft(
        id: Long? = null,
        address: String = "",
        alias: String = "",
        username: String = "",
        secret: String = "",
        isAnonymous: Boolean = true,
        storageType: SourceEditorType,
    ) = SourceEditorDraft(
        id = id,
        address = address,
        alias = alias,
        username = username,
        secret = secret,
        isAnonymous = isAnonymous,
        storageType = storageType,
    )

    private fun oneDriveDrive(
        id: String,
        name: String,
    ) = OneDriveDriveInfo(
        id = id,
        name = name,
    )
}
