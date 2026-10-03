package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import java.time.LocalDate

/** Sve što ekran Pregled prikazuje, izračunato za jedan dan. */
data class Overview(
    /** Zbroj mjesečnih ekvivalenata aktivnih pretplata — dok traje trial ili promo, niža cijena. */
    val monthlyTotalCents: Int,
    val yearlyTotalCents: Int,
    val activeCount: Int,
    /** Najbliži događaji, sortirani po datumu. */
    val upcoming: List<UpcomingEvent>,
    /** Sve aktivne pretplate, po imenu. */
    val subscriptions: List<SubscriptionSummary>,
)

sealed interface UpcomingEvent {
    val subscription: Subscription
    val date: LocalDate

    /** Redovna naplata. */
    data class Billing(
        override val subscription: Subscription,
        override val date: LocalDate,
        val priceCents: Int,
    ) : UpcomingEvent

    /** Kraj probnog perioda; [date] je zadnji dan probnog perioda. */
    data class TrialEnding(
        override val subscription: Subscription,
        override val date: LocalDate,
        val firstChargeDate: LocalDate,
        val firstChargeCents: Int,
    ) : UpcomingEvent
}

data class SubscriptionSummary(
    val subscription: Subscription,
    /** Cijena za jedan ciklus koja vrijedi danas. */
    val currentPriceCents: Int,
    val inTrial: Boolean,
    /** Razlika zadnje promjene cijene ako je stupila na snagu nedavno, inače null. */
    val recentPriceChangeCents: Int?,
)

const val UPCOMING_LIMIT = 3

/** Koliko dana nakon stupanja na snagu se promjena cijene ističe u listi. */
const val RECENT_PRICE_CHANGE_DAYS = 30L

fun buildOverview(
    subscriptions: List<SubscriptionWithDetails>,
    today: LocalDate,
    upcomingLimit: Int = UPCOMING_LIMIT,
): Overview {
    val active = subscriptions.filter { it.subscription.isActive }
    val monthlyTotal = active.sumOf { it.monthlyEquivalentOn(today) }

    val upcoming = active
        .map { it.upcomingEvent(today) }
        .sortedWith(compareBy<UpcomingEvent>({ it.date }, { it.subscription.name.lowercase() }))
        .take(upcomingLimit)

    val summaries = active
        .sortedBy { it.subscription.name.lowercase() }
        .map { sub ->
            SubscriptionSummary(
                subscription = sub.subscription,
                currentPriceCents = sub.priceOn(today),
                inTrial = sub.isInTrialOn(today),
                recentPriceChangeCents = sub.recentPriceChangeCents(today),
            )
        }

    return Overview(
        monthlyTotalCents = monthlyTotal,
        yearlyTotalCents = monthlyTotal * 12,
        activeCount = active.size,
        upcoming = upcoming,
        subscriptions = summaries,
    )
}

fun SubscriptionWithDetails.isInTrialOn(date: LocalDate): Boolean =
    trial?.let { !date.isAfter(it.endsOn) } ?: false

/**
 * Dok traje probni period, važniji je njegov kraj od naplate po cijeni probnog
 * perioda, pa se prikazuje kraj i prva prava naplata nakon njega.
 */
private fun SubscriptionWithDetails.upcomingEvent(today: LocalDate): UpcomingEvent {
    val charge = nextChargeOnOrAfter(today)
    val trial = trial
    if (trial != null && !today.isAfter(trial.endsOn)) {
        return UpcomingEvent.TrialEnding(
            subscription = subscription,
            date = trial.endsOn,
            firstChargeDate = charge.date,
            firstChargeCents = charge.priceCents,
        )
    }
    return UpcomingEvent.Billing(subscription, charge.date, charge.priceCents)
}

private fun SubscriptionWithDetails.recentPriceChangeCents(today: LocalDate): Int? {
    val windowStart = today.minusDays(RECENT_PRICE_CHANGE_DAYS)
    return priceChanges
        .filter { !it.effectiveFrom.isAfter(today) && !it.effectiveFrom.isBefore(windowStart) }
        .maxWithOrNull(compareBy({ it.effectiveFrom }, { it.recordedAt }, { it.id }))
        ?.let { it.newPriceCents - it.oldPriceCents }
        ?.takeIf { it != 0 }
}
