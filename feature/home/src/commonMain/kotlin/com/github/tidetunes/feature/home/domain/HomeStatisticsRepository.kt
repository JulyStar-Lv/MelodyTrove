package com.github.tidetunes.feature.home.domain

import kotlinx.coroutines.flow.StateFlow

interface HomeStatisticsRepository {
    val statistics: StateFlow<HomeStatistics>
}
