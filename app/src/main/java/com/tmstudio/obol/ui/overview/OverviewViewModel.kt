package com.tmstudio.obol.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tmstudio.obol.ObolApplication
import com.tmstudio.obol.data.db.dao.SubscriptionDao
import com.tmstudio.obol.domain.Overview
import com.tmstudio.obol.domain.buildOverview
import com.tmstudio.obol.ui.todayFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data object Empty : OverviewUiState
    data class Content(val overview: Overview, val today: LocalDate) : OverviewUiState
}

class OverviewViewModel(
    subscriptionDao: SubscriptionDao,
    clock: Clock,
) : ViewModel() {

    val state: StateFlow<OverviewUiState> =
        combine(subscriptionDao.observeActiveWithDetails(), todayFlow(clock)) { subscriptions, today ->
            if (subscriptions.isEmpty()) {
                OverviewUiState.Empty
            } else {
                OverviewUiState.Content(buildOverview(subscriptions, today), today)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OverviewUiState.Loading)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ObolApplication
                OverviewViewModel(app.container.database.subscriptionDao(), Clock.systemDefaultZone())
            }
        }
    }
}

