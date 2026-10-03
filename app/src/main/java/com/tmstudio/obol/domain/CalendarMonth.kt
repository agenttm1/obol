package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import java.time.LocalDate
import java.time.YearMonth

/** Naplate jednog mjeseca za ekran Kalendar. */
data class CalendarMonth(
    val month: YearMonth,
    /** Po datumu, pa po imenu. */
    val billings: List<CalendarBilling>,
) {
    /** Zbroj stvarnih naplata u mjesecu — ne mjesečnih ekvivalenata. */
    val totalCents: Int get() = billings.sumOf { it.priceCents }

    val billingDays: Int get() = billings.map { it.date }.distinct().size

    fun billingsOn(date: LocalDate): List<CalendarBilling> = billings.filter { it.date == date }
}

data class CalendarBilling(
    val subscription: Subscription,
    val date: LocalDate,
    val priceCents: Int,
)

fun buildCalendarMonth(subscriptions: List<SubscriptionWithDetails>, month: YearMonth): CalendarMonth {
    val billings = subscriptions
        .filter { it.subscription.isActive }
        .flatMap { sub -> sub.billingsIn(month).map { CalendarBilling(sub.subscription, it.date, it.priceCents) } }
        .sortedWith(compareBy({ it.date }, { it.subscription.name.lowercase() }))
    return CalendarMonth(month, billings)
}
