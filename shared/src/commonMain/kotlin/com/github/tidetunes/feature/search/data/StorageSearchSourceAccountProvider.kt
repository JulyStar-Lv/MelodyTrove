package com.github.tidetunes.feature.search.data

import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.feature.search.domain.SearchSourceAccount
import com.github.tidetunes.feature.search.domain.SearchSourceAccountProvider
import com.github.tidetunes.core.data.StorageRepositoryImpl

class StorageSearchSourceAccountProvider(
    private val storageRepository: StorageRepositoryImpl,
) : SearchSourceAccountProvider {
    override fun sourceAccounts(): List<SearchSourceAccount> {
        return storageRepository.storageAccounts.value.toSearchSourceAccounts()
    }
}

internal fun List<StorageAccountInfo>.toSearchSourceAccounts(): List<SearchSourceAccount> {
    return mapNotNull { storage ->
        SearchSourceAccount(
            sourceId = storage.sourceId,
            accountId = storage.accountId,
            displayName = storage.title,
        )
    }
}
