package com.tmstudio.obol.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tmstudio.obol.R
import kotlinx.serialization.Serializable

@Serializable
data object OverviewRoute

@Serializable
data object CalendarRoute

@Serializable
data object SettingsRoute

/** Odabir servisa iz kataloga ili ručni unos. */
@Serializable
data object AddRoute

/**
 * Postavljanje plana. Nova pretplata: [serviceId] iz kataloga ili null za ručni
 * unos. Uređivanje: [subscriptionId] postojeće pretplate.
 */
@Serializable
data class PlanRoute(val serviceId: String? = null, val subscriptionId: Long? = null)

/** Detalji jedne pretplate. */
@Serializable
data class DetailsRoute(val subscriptionId: Long)

/**
 * Tabovi donje navigacije u v1. Statistika dolazi u v1.1 i ide između
 * Kalendara i Postavki, kao u mockupu — ne dodaje se dok ekran ne postoji.
 */
enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Overview(OverviewRoute, R.string.nav_overview, R.drawable.ic_nav_overview),
    Calendar(CalendarRoute, R.string.nav_calendar, R.drawable.ic_nav_calendar),
    Settings(SettingsRoute, R.string.nav_settings, R.drawable.ic_nav_settings),
}
