package com.tmstudio.obol.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceOnTest {

    @Test
    fun withoutTrialPromoOrChanges_returnsBasePrice() {
        val sub = subscription(basePriceCents = 1399)
        assertEquals(1399, sub.priceOn(date(2026, 10, 3)))
    }

    @Test
    fun trial_appliesUpToAndIncludingLastDay() {
        val sub = subscription(basePriceCents = 999, trial = trial(endsOn = date(2026, 10, 31)))
        assertEquals(0, sub.priceOn(date(2026, 10, 1)))
        assertEquals(0, sub.priceOn(date(2026, 10, 31)))
        assertEquals(999, sub.priceOn(date(2026, 11, 1)))
    }

    @Test
    fun trial_withNonZeroPrice() {
        val sub = subscription(basePriceCents = 999, trial = trial(endsOn = date(2026, 10, 31), priceCents = 99))
        assertEquals(99, sub.priceOn(date(2026, 10, 15)))
    }

    @Test
    fun trialEndingOnBillingDay_billingIsAtTrialPrice() {
        // Spec: date <= trial.endsOn → cijena probnog perioda, i na sam dan naplate.
        val sub = subscription(
            basePriceCents = 999,
            firstBillingDate = date(2026, 10, 31),
            trial = trial(endsOn = date(2026, 10, 31)),
        )
        val firstBilling = sub.nextBillingOnOrAfter(date(2026, 10, 1))
        assertEquals(date(2026, 10, 31), firstBilling)
        assertEquals(0, sub.priceOn(firstBilling))

        val secondBilling = sub.nextBillingOnOrAfter(date(2026, 11, 1))
        assertEquals(date(2026, 11, 30), secondBilling)
        assertEquals(999, sub.priceOn(secondBilling))
    }

    @Test
    fun promo_boundariesAreInclusive() {
        val sub = subscription(
            basePriceCents = 1099,
            promo = promo(599, startsOn = date(2026, 9, 1), endsOn = date(2026, 11, 30)),
        )
        assertEquals(1099, sub.priceOn(date(2026, 8, 31)))
        assertEquals(599, sub.priceOn(date(2026, 9, 1)))
        assertEquals(599, sub.priceOn(date(2026, 11, 30)))
        assertEquals(1099, sub.priceOn(date(2026, 12, 1)))
    }

    @Test
    fun promoStartingOnTrialLastDay_trialWinsThatDay() {
        val sub = subscription(
            basePriceCents = 1099,
            trial = trial(endsOn = date(2026, 10, 31)),
            promo = promo(599, startsOn = date(2026, 10, 31), endsOn = date(2027, 1, 31)),
        )
        assertEquals(0, sub.priceOn(date(2026, 10, 31)))
        assertEquals(599, sub.priceOn(date(2026, 11, 1)))
        assertEquals(599, sub.priceOn(date(2027, 1, 31)))
        assertEquals(1099, sub.priceOn(date(2027, 2, 1)))
    }

    @Test
    fun promoStartingDayAfterTrial_isContiguous() {
        val sub = subscription(
            basePriceCents = 1099,
            trial = trial(endsOn = date(2026, 10, 31)),
            promo = promo(599, startsOn = date(2026, 11, 1), endsOn = date(2027, 1, 31)),
        )
        assertEquals(0, sub.priceOn(date(2026, 10, 31)))
        assertEquals(599, sub.priceOn(date(2026, 11, 1)))
    }

    @Test
    fun priceChange_announcedIncrease_oldPriceUntilEffectiveDate() {
        // basePriceCents je već nova cijena; povijest daje staru do datuma promjene.
        val sub = subscription(
            basePriceCents = 1099,
            priceChanges = listOf(priceChange(date(2026, 12, 1), oldPriceCents = 599, newPriceCents = 1099)),
        )
        assertEquals(599, sub.priceOn(date(2026, 11, 30)))
        assertEquals(1099, sub.priceOn(date(2026, 12, 1)))
    }

    @Test
    fun priceChange_winsOverStaleBasePrice() {
        val sub = subscription(
            basePriceCents = 599,
            priceChanges = listOf(priceChange(date(2026, 12, 1), oldPriceCents = 599, newPriceCents = 1099)),
        )
        assertEquals(599, sub.priceOn(date(2026, 11, 30)))
        assertEquals(1099, sub.priceOn(date(2026, 12, 1)))
    }

    @Test
    fun priceChange_multiple_latestAppliedWins_regardlessOfListOrder() {
        val sub = subscription(
            basePriceCents = 1399,
            priceChanges = listOf(
                priceChange(date(2026, 6, 1), oldPriceCents = 1199, newPriceCents = 1399, id = 2),
                priceChange(date(2025, 6, 1), oldPriceCents = 999, newPriceCents = 1199, id = 1),
            ),
        )
        assertEquals(999, sub.priceOn(date(2025, 5, 31)))
        assertEquals(1199, sub.priceOn(date(2025, 6, 1)))
        assertEquals(1199, sub.priceOn(date(2026, 5, 31)))
        assertEquals(1399, sub.priceOn(date(2026, 6, 1)))
    }

    @Test
    fun priceChange_sameDay_laterRecordedWins() {
        val sub = subscription(
            basePriceCents = 1299,
            priceChanges = listOf(
                priceChange(date(2026, 6, 1), 999, 1299, id = 2, recordedAt = NOW.plusSeconds(60)),
                priceChange(date(2026, 6, 1), 999, 1199, id = 1, recordedAt = NOW),
            ),
        )
        assertEquals(1299, sub.priceOn(date(2026, 6, 1)))
    }

    @Test
    fun promo_winsOverPriceChange() {
        val sub = subscription(
            basePriceCents = 1099,
            promo = promo(599, startsOn = date(2026, 11, 1), endsOn = date(2026, 12, 31)),
            priceChanges = listOf(priceChange(date(2026, 12, 1), 999, 1099)),
        )
        assertEquals(599, sub.priceOn(date(2026, 12, 15)))
        assertEquals(1099, sub.priceOn(date(2027, 1, 1)))
    }
}
