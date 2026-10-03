package com.tmstudio.obol.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.tmstudio.obol.ObolApplication
import com.tmstudio.obol.data.db.dao.SubscriptionDao
import com.tmstudio.obol.data.db.dao.UsageCheckDao
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import com.tmstudio.obol.data.db.entity.UsageCheck
import com.tmstudio.obol.domain.BillingEvent
import com.tmstudio.obol.domain.fullPriceOn
import com.tmstudio.obol.domain.isInTrialOn
import com.tmstudio.obol.domain.monthlyEquivalent
import com.tmstudio.obol.domain.nextChargeOnOrAfter
import com.tmstudio.obol.domain.pastBillings
import com.tmstudio.obol.domain.priceOn
import com.tmstudio.obol.domain.usageQuestionPeriod
import com.tmstudio.obol.ui.navigation.DetailsRoute
import com.tmstudio.obol.ui.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

sealed interface DetailsUiState {
    data object Loading : DetailsUiState

    /** Pretplata ne postoji (obrisana je) — ekran se zatvara. */
    data object Missing : DetailsUiState

    data class Content(
        val details: SubscriptionWithDetails,
        val today: LocalDate,
        /** Cijena za jedan ciklus koja vrijedi danas. */
        val currentPriceCents: Int,
        /** Godišnji trošak po punoj cijeni, bez probnog perioda i promocije. */
        val yearlyFullPriceCents: Int,
        val nextCharge: BillingEvent,
        /** Zadnji dan probnog perioda, dok traje. */
        val trialEndsOn: LocalDate?,
        /** Zadnji dan promocije i puna cijena nakon nje, dok promocija nije završila. */
        val promoEndsOn: LocalDate?,
        val priceAfterPromoCents: Int?,
        val usageQuestion: YearMonth?,
        val history: List<BillingEvent>,
    ) : DetailsUiState
}

const val HISTORY_LIMIT = 6

internal fun buildDetailsState(
    details: SubscriptionWithDetails,
    checks: List<UsageCheck>,
    today: LocalDate,
): DetailsUiState.Content {
    val sub = details.subscription
    val promo = details.promo?.takeIf { !today.isAfter(it.endsOn) }
    return DetailsUiState.Content(
        details = details,
        today = today,
        currentPriceCents = details.priceOn(today),
        yearlyFullPriceCents = monthlyEquivalent(details.fullPriceOn(today), sub.cycle, sub.cycleDays) * 12,
        nextCharge = details.nextChargeOnOrAfter(today),
        trialEndsOn = details.trial?.endsOn?.takeIf { details.isInTrialOn(today) },
        promoEndsOn = promo?.endsOn,
        priceAfterPromoCents = promo?.let { details.fullPriceOn(it.endsOn.plusDays(1)) },
        usageQuestion = details.usageQuestionPeriod(today, checks.map { it.period }.toSet()),
        history = details.pastBillings(today, HISTORY_LIMIT),
    )
}

class DetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val subscriptionDao: SubscriptionDao,
    private val usageCheckDao: UsageCheckDao,
    private val clock: Clock,
) : ViewModel() {

    private val subscriptionId = savedStateHandle.toRoute<DetailsRoute>().subscriptionId

    val state: StateFlow<DetailsUiState> = combine(
        subscriptionDao.observeWithDetails(subscriptionId),
        usageCheckDao.observeFor(subscriptionId),
        todayFlow(clock),
    ) { details, checks, today ->
        if (details == null) DetailsUiState.Missing else buildDetailsState(details, checks, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState.Loading)

    fun answerUsage(period: YearMonth, used: Boolean) {
        viewModelScope.launch {
            usageCheckDao.upsert(UsageCheck(subscriptionId, period, used, Instant.now(clock)))
        }
    }

    /** Briše pretplatu; tok podataka tada javi [DetailsUiState.Missing] i ekran se zatvara. */
    fun delete() {
        val content = state.value as? DetailsUiState.Content ?: return
        viewModelScope.launch { subscriptionDao.delete(content.details.subscription) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ObolApplication
                val db = app.container.database
                DetailsViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    subscriptionDao = db.subscriptionDao(),
                    usageCheckDao = db.usageCheckDao(),
                    clock = Clock.systemDefaultZone(),
                )
            }
        }
    }
}
