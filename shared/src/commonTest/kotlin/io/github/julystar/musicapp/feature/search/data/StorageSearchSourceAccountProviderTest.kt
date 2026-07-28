package io.github.julystar.musicapp.feature.search.data

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.SourceId
import io.github.julystar.musicapp.core.domain.model.StorageAccountInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageSearchSourceAccountProviderTest {
    @Test
    fun `only enabled accounts are exposed to source search`() {
        val accounts = listOf(
            account(id = "storage:1", sourceId = "local", enabled = true),
            account(id = "storage:2", sourceId = "webdav", enabled = false),
        )

        val sourceAccounts = accounts.toSearchSourceAccounts()

        assertEquals(listOf(SourceAccountId("storage:1")), sourceAccounts.map { it.accountId })
        assertEquals(listOf(SourceId("local")), sourceAccounts.map { it.sourceId })
    }

    private fun account(
        id: String,
        sourceId: String,
        enabled: Boolean,
    ) = StorageAccountInfo(
        accountId = SourceAccountId(id),
        sourceId = SourceId(sourceId),
        isLocal = sourceId == "local",
        isOneDrive = sourceId == "onedrive",
        title = sourceId,
        subtitle = "",
        musicCount = 0,
        enabled = enabled,
    )
}
