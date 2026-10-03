package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

/** Postavke podsjetnika (spec, poglavlje 5; ekran Postavke u koraku 10). */
data class ReminderSettings(
    val time: LocalTime = DEFAULT_REMINDER_TIME,
    /** Koliko dana prije naplate javiti. */
    val billingDaysBefore: Int = DEFAULT_BILLING_DAYS_BEFORE,
    val usageChecksEnabled: Boolean = true,
    /** „Upozori na poskupljenje" — kraj promocije. */
    val priceIncreaseAlertsEnabled: Boolean = true,
) {
    companion object {
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)
        const val DEFAULT_BILLING_DAYS_BEFORE = 3
    }
}

/** Koliko dana prije kraja probnog perioda i prve pune naplate nakon promocije. */
const val TRIAL_END_DAYS_BEFORE = 2L
const val PROMO_END_DAYS_BEFORE = 3L

sealed interface Reminder {
    val subscription: Subscription

    /** „Netflix ti se naplaćuje za 3 dana — 13,99 €" */
    data class Billing(
        override val subscription: Subscription,
        val date: LocalDate,
        val daysBefore: Int,
        val priceCents: Int,
    ) : Reminder

    /** „Disney+ ti za 2 dana prestaje probni period. Prva naplata je 9,99 €." */
    data class TrialEnding(
        override val subscription: Subscription,
        val endsOn: LocalDate,
        val daysBefore: Int,
        val firstChargeCents: Int,
    ) : Reminder

    /** „Spotify ti od 1. prosinca ide s 5,99 na 10,99 €." */
    data class PromoEnding(
        override val subscription: Subscription,
        /** Prva naplata po punoj cijeni. */
        val fullPriceFrom: LocalDate,
        val promoPriceCents: Int,
        val fullPriceCents: Int,
    ) : Reminder

    /** „Jesi li prošli mjesec koristio Netflix?" */
    data class UsageCheck(
        override val subscription: Subscription,
        val period: YearMonth,
    ) : Reminder
}

/**
 * Podsjetnici koje treba poslati danas. Worker se pokreće jednom dnevno, pa
 * svaki događaj okida točno na jedan dan.
 *
 * Kad za istu naplatu postoji podsjetnik o kraju probnog perioda ili promocije,
 * obični podsjetnik o naplati se preskače — onaj drugi već kaže iznos.
 *
 * @param answeredUsage pretplate koje su već odgovorile za prošli mjesec
 */
fun remindersFor(
    subscriptions: List<SubscriptionWithDetails>,
    today: LocalDate,
    settings: ReminderSettings,
    answeredUsage: Set<Long>,
): List<Reminder> = subscriptions
    .filter { it.subscription.isActive }
    .flatMap { sub -> sub.remindersFor(today, settings, answeredUsage) }

private fun SubscriptionWithDetails.remindersFor(
    today: LocalDate,
    settings: ReminderSettings,
    answeredUsage: Set<Long>,
): List<Reminder> = buildList {
    val trial = trial
    val promo = promo
    val trialFirstCharge = trial?.let { nextBillingOnOrAfter(it.endsOn.plusDays(1)) }
    val promoFullPriceFrom = promo?.endsOn?.plusDays(1)

    if (trial != null && today == trial.endsOn.minusDays(TRIAL_END_DAYS_BEFORE)) {
        add(
            Reminder.TrialEnding(
                subscription = subscription,
                endsOn = trial.endsOn,
                daysBefore = TRIAL_END_DAYS_BEFORE.toInt(),
                firstChargeCents = priceOn(requireNotNull(trialFirstCharge)),
            )
        )
    }

    if (promo != null && promoFullPriceFrom != null && settings.priceIncreaseAlertsEnabled &&
        today == promoFullPriceFrom.minusDays(PROMO_END_DAYS_BEFORE)
    ) {
        val fullPrice = priceOn(promoFullPriceFrom)
        if (fullPrice > promo.priceCents) {
            add(Reminder.PromoEnding(subscription, promoFullPriceFrom, promo.priceCents, fullPrice))
        }
    }

    if (settings.billingDaysBefore > 0) {
        val chargeDate = today.plusDays(settings.billingDaysBefore.toLong())
        val isBillingDay = nextBillingOnOrAfter(chargeDate) == chargeDate
        val price = priceOn(chargeDate)
        val coveredByTrial = trial != null && (chargeDate == trialFirstCharge || !chargeDate.isAfter(trial.endsOn))
        val coveredByPromo = promoFullPriceFrom == chargeDate && any { it is Reminder.PromoEnding }
        if (isBillingDay && price > 0 && !coveredByTrial && !coveredByPromo) {
            add(Reminder.Billing(subscription, chargeDate, settings.billingDaysBefore, price))
        }
    }

    if (settings.usageChecksEnabled && today.dayOfMonth == 1 && subscription.id !in answeredUsage) {
        usageQuestionPeriod(today, emptySet())?.let { add(Reminder.UsageCheck(subscription, it)) }
    }
}

/** Sljedeće pokretanje dnevnog workera: danas u [time] ako još nije prošlo, inače sutra. */
fun nextReminderRun(now: LocalDateTime, time: LocalTime): LocalDateTime {
    val todayRun = now.toLocalDate().atTime(time)
    return if (now.isBefore(todayRun)) todayRun else todayRun.plusDays(1)
}
