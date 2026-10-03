package com.tmstudio.obol.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.ExistingWorkPolicy
import com.tmstudio.obol.ObolApplication
import com.tmstudio.obol.data.prefs.AppPreferences
import com.tmstudio.obol.domain.ReminderSettings
import com.tmstudio.obol.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(
    private val preferences: AppPreferences,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    /** null dok se postavke ne učitaju. */
    val settings: StateFlow<ReminderSettings?> =
        preferences.reminderSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setBillingDaysBefore(days: Int) = update { it.copy(billingDaysBefore = days) }

    fun setUsageChecks(enabled: Boolean) = update { it.copy(usageChecksEnabled = enabled) }

    fun setPriceIncreaseAlerts(enabled: Boolean) = update { it.copy(priceIncreaseAlertsEnabled = enabled) }

    /** Novo vrijeme mijenja odgodu već zakazanog workera, pa se lanac zakazuje iznova. */
    fun setReminderTime(time: LocalTime) {
        viewModelScope.launch {
            preferences.updateReminderSettings { it.copy(time = time) }
            scheduler.scheduleNext(ExistingWorkPolicy.REPLACE)
        }
    }

    private fun update(transform: (ReminderSettings) -> ReminderSettings) {
        viewModelScope.launch { preferences.updateReminderSettings(transform) }
    }

    companion object {
        /** Ponuđeni broj dana za „Javi mi prije naplate"; 0 isključuje podsjetnik. */
        val BILLING_DAY_OPTIONS = listOf(0, 1, 2, 3, 5, 7)

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ObolApplication).container
                SettingsViewModel(container.preferences, container.reminderScheduler)
            }
        }
    }
}
