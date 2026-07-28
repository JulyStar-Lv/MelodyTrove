package io.github.julystar.musicapp.feature.search.data

import io.github.julystar.musicapp.core.domain.model.StorageAccountInfo
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.feature.search.domain.SearchSourceAccount
import io.github.julystar.musicapp.feature.search.domain.SearchSourceAccountProvider
import io.github.julystar.musicapp.core.data.StorageRepositoryImpl

class StorageSearchSourceAccountProvider(
    private val storageRepository: StorageRepositoryImpl,
) : SearchSourceAccountProvider {
    override fun sourceAccounts(): List<SearchSourceAccount> {
        return storageRepository.storageAccounts.value.toSearchSourceAccounts()
    }
}

internal fun List<StorageAccountInfo>.toSearchSourceAccounts(): List<SearchSourceAccount> {
    return asSequence()
        .filter(StorageAccountInfo::enabled)
        .mapNotNull { storage ->
            SearchSourceAccount(
                sourceId = storage.sourceId,
                accountId = storage.accountId,
                displayName = storage.title,
            )
        }
        .toList()
}
