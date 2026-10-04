package com.tmstudio.obol.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tmstudio.obol.data.catalog.CatalogVersionStore
import com.tmstudio.obol.domain.ReminderSettings
import com.tmstudio.obol.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

val Context.obolDataStore: DataStore<Preferences> by preferencesDataStore(name = "obol_prefs")

/** Postavke aplikacije u DataStoreu. */
class AppPreferences(private val dataStore: DataStore<Preferences>) : CatalogVersionStore {

    override suspend fun loadedCatalogVersion(): Int? =
        dataStore.data.first()[Keys.catalogVersion]

    override suspend fun setLoadedCatalogVersion(version: Int) {
        dataStore.edit { it[Keys.catalogVersion] = version }
    }

    val reminderSettings: Flow<ReminderSettings> = dataStore.data.map { prefs ->
        val defaults = ReminderSettings()
        ReminderSettings(
            time = prefs[Keys.reminderMinuteOfDay]
                ?.let { LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) }
                ?: defaults.time,
            billingDaysBefore = prefs[Keys.billingDaysBefore] ?: defaults.billingDaysBefore,
            usageChecksEnabled = prefs[Keys.usageChecksEnabled] ?: defaults.usageChecksEnabled,
            priceIncreaseAlertsEnabled = prefs[Keys.priceIncreaseAlerts] ?: defaults.priceIncreaseAlertsEnabled,
        )
    }

    suspend fun updateReminderSettings(transform: (ReminderSettings) -> ReminderSettings) {
        val updated = transform(reminderSettings.first())
        dataStore.edit {
            it[Keys.reminderMinuteOfDay] = updated.time.hour * MINUTES_PER_HOUR + updated.time.minute
            it[Keys.billingDaysBefore] = updated.billingDaysBefore
            it[Keys.usageChecksEnabled] = updated.usageChecksEnabled
            it[Keys.priceIncreaseAlerts] = updated.priceIncreaseAlertsEnabled
        }
    }

    /** Odabrana tema; nepoznata ili nepostavljena vrijednost znači tamnu. */
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[Keys.themeMode]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.DARK
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    /** Dan kad je dnevni worker zadnji put poslao podsjetnike — da se isti dan ne šalju dvaput. */
    suspend fun lastReminderRun(): LocalDate? =
        dataStore.data.first()[Keys.lastReminderRun]?.let(LocalDate::ofEpochDay)

    suspend fun setLastReminderRun(date: LocalDate?) {
        dataStore.edit {
            if (date == null) it.remove(Keys.lastReminderRun) else it[Keys.lastReminderRun] = date.toEpochDay()
        }
    }

    private object Keys {
        val catalogVersion = intPreferencesKey("catalog_version")
        val reminderMinuteOfDay = intPreferencesKey("reminder_minute_of_day")
        val billingDaysBefore = intPreferencesKey("billing_days_before")
        val usageChecksEnabled = booleanPreferencesKey("usage_checks_enabled")
        val priceIncreaseAlerts = booleanPreferencesKey("price_increase_alerts")
        val lastReminderRun = longPreferencesKey("last_reminder_run")
        val themeMode = stringPreferencesKey("theme_mode")
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
