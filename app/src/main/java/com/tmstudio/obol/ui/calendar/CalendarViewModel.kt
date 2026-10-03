package com.tmstudio.obol.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tmstudio.obol.ObolApplication
import com.tmstudio.obol.data.db.dao.SubscriptionDao
import com.tmstudio.obol.domain.CalendarMonth
import com.tmstudio.obol.domain.buildCalendarMonth
import com.tmstudio.obol.ui.todayFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val today: LocalDate,
    val calendar: CalendarMonth?,
    /** Nema nijedne aktivne pretplate — prikazuje se prazno stanje. */
    val isEmpty: Boolean = false,
)

class CalendarViewModel(subscriptionDao: SubscriptionDao, clock: Clock) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))

    val state: StateFlow<CalendarUiState> = combine(
        subscriptionDao.observeActiveWithDetails(),
        month,
        todayFlow(clock),
    ) { subscriptions, month, today ->
        CalendarUiState(today, buildCalendarMonth(subscriptions, month), isEmpty = subscriptions.isEmpty())
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(LocalDate.now(clock), null))

    fun showPreviousMonth() = month.update { it.minusMonths(1) }

    fun showNextMonth() = month.update { it.plusMonths(1) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ObolApplication
                CalendarViewModel(app.container.database.subscriptionDao(), Clock.systemDefaultZone())
            }
        }
    }
}
