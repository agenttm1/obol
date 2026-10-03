package com.tmstudio.obol.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.tmstudio.obol.ObolApplication
import com.tmstudio.obol.data.db.dao.ServiceDao
import com.tmstudio.obol.data.db.dao.SubscriptionDao
import com.tmstudio.obol.ui.navigation.PlanRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

data class PlanUiState(
    val form: PlanForm? = null,
    val today: LocalDate,
    /** Greške se prikazuju tek nakon prvog pokušaja spremanja. */
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    /** Spremljena je prva pretplata — tada se traži dozvola za obavijesti (spec, poglavlje 5). */
    val savedFirst: Boolean = false,
)

class PlanViewModel(
    savedStateHandle: SavedStateHandle,
    private val serviceDao: ServiceDao,
    private val subscriptionDao: SubscriptionDao,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlanRoute>()
    private val _state = MutableStateFlow(PlanUiState(today = LocalDate.now(clock)))
    val state: StateFlow<PlanUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val today = _state.value.today
            val editing = route.subscriptionId?.let { subscriptionDao.observeWithDetails(it).first() }
            val form = if (editing != null) {
                val service = editing.subscription.serviceId?.let { serviceDao.getById(it) }
                PlanForm.forSubscription(editing, service, today)
            } else {
                val service = route.serviceId?.let { serviceDao.getById(it) }
                service?.let { PlanForm.forService(it, today) } ?: PlanForm.manual(today)
            }
            _state.update { it.copy(form = form) }
        }
    }

    fun update(transform: (PlanForm) -> PlanForm) {
        _state.update { state -> state.copy(form = state.form?.let(transform)) }
    }

    fun save() {
        val current = _state.value
        val form = current.form ?: return
        if (current.saving || current.saved) return
        val now = Instant.now(clock)
        val update = if (form.isEditing) form.toUpdate(now, current.today) else null
        val new = if (form.isEditing) null else form.toNewSubscription(now, current.today)
        if (update == null && new == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            var first = false
            if (update != null) {
                subscriptionDao.updateWithPeriods(update.subscription, update.trial, update.promo, update.priceChange)
            } else if (new != null) {
                first = subscriptionDao.count() == 0
                subscriptionDao.insertWithPeriods(new.subscription, new.trial, new.promo)
            }
            _state.update { it.copy(saving = false, saved = true, savedFirst = first) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ObolApplication
                val db = app.container.database
                PlanViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    serviceDao = db.serviceDao(),
                    subscriptionDao = db.subscriptionDao(),
                    clock = Clock.systemDefaultZone(),
                )
            }
        }
    }
}
