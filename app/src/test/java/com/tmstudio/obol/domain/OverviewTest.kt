package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewTest {

    private val today = date(2026, 10, 3)

    @Test
    fun empty() {
        val overview = buildOverview(emptyList(), today)
        assertEquals(0, overview.monthlyTotalCents)
        assertEquals(0, overview.activeCount)
        assertTrue(overview.upcoming.isEmpty())
        assertTrue(overview.subscriptions.isEmpty())
    }

    @Test
    fun totals_sumMonthlyEquivalents_andYearlyIsTwelveTimes() {
        val overview = buildOverview(
            listOf(
                subscription(id = 1, name = "Netflix", basePriceCents = 1399),
                subscription(id = 2, name = "iCloud+", cycle = BillingCycle.YEARLY, basePriceCents = 12000),
            ),
            today,
        )
        assertEquals(2399, overview.monthlyTotalCents)
        assertEquals(28788, overview.yearlyTotalCents)
        assertEquals(2, overview.activeCount)
    }

    @Test
    fun inactiveSubscriptions_areIgnoredEverywhere() {
        val overview = buildOverview(
            listOf(
                subscription(id = 1, name = "Netflix"),
                subscription(id = 2, name = "Spotify", isActive = false, basePriceCents = 699),
            ),
            today,
        )
        assertEquals(1399, overview.monthlyTotalCents)
        assertEquals(1, overview.activeCount)
        assertEquals(listOf("Netflix"), overview.subscriptions.map { it.subscription.name })
        assertEquals(listOf("Netflix"), overview.upcoming.map { it.subscription.name })
    }

    @Test
    fun trial_countsAtTrialPriceInTotal() {
        val overview = buildOverview(
            listOf(subscription(basePriceCents = 999, trial = trial(endsOn = date(2026, 10, 5)))),
            today,
        )
        assertEquals(0, overview.monthlyTotalCents)
        assertTrue(overview.subscriptions.single().inTrial)
        assertEquals(0, overview.subscriptions.single().currentPriceCents)
    }

    @Test
    fun upcoming_isSortedByDate_tiesByName_andLimited() {
        val overview = buildOverview(
            listOf(
                subscription(id = 1, name = "Zeta", firstBillingDate = date(2026, 1, 4)),
                subscription(id = 2, name = "alfa", firstBillingDate = date(2026, 1, 4)),
                subscription(id = 3, name = "Beta", firstBillingDate = date(2026, 1, 20)),
                subscription(id = 4, name = "Gama", firstBillingDate = date(2026, 1, 3)),
            ),
            today,
        )
        assertEquals(listOf("Gama", "alfa", "Zeta"), overview.upcoming.map { it.subscription.name })
        assertEquals(
            listOf(date(2026, 10, 3), date(2026, 10, 4), date(2026, 10, 4)),
            overview.upcoming.map { it.date },
        )
    }

    @Test
    fun upcoming_billingCarriesPriceOnThatDay() {
        val overview = buildOverview(
            listOf(
                subscription(
                    basePriceCents = 1099,
                    firstBillingDate = date(2026, 1, 7),
                    priceChanges = listOf(priceChange(date(2026, 10, 5), 999, 1099)),
                )
            ),
            today,
        )
        val billing = overview.upcoming.single() as UpcomingEvent.Billing
        assertEquals(date(2026, 10, 7), billing.date)
        assertEquals(1099, billing.priceCents)
    }

    @Test
    fun upcoming_duringTrial_showsTrialEndAndFirstRealCharge() {
        val overview = buildOverview(
            listOf(
                subscription(
                    basePriceCents = 999,
                    firstBillingDate = date(2026, 10, 6),
                    trial = trial(endsOn = date(2026, 10, 5)),
                )
            ),
            today,
        )
        val event = overview.upcoming.single() as UpcomingEvent.TrialEnding
        assertEquals(date(2026, 10, 5), event.date)
        assertEquals(date(2026, 10, 6), event.firstChargeDate)
        assertEquals(999, event.firstChargeCents)
    }

    @Test
    fun upcoming_trialFollowedByPromo_firstChargeIsPromoPrice() {
        val overview = buildOverview(
            listOf(
                subscription(
                    basePriceCents = 1099,
                    firstBillingDate = date(2026, 10, 6),
                    trial = trial(endsOn = date(2026, 10, 5)),
                    promo = promo(299, startsOn = date(2026, 10, 6), endsOn = date(2027, 1, 5)),
                )
            ),
            today,
        )
        val event = overview.upcoming.single() as UpcomingEvent.TrialEnding
        assertEquals(299, event.firstChargeCents)
    }

    @Test
    fun upcoming_trialEndedYesterday_isRegularBilling() {
        val overview = buildOverview(
            listOf(
                subscription(
                    firstBillingDate = date(2026, 10, 3),
                    trial = trial(endsOn = date(2026, 10, 2)),
                )
            ),
            today,
        )
        assertTrue(overview.upcoming.single() is UpcomingEvent.Billing)
        assertFalse(overview.subscriptions.single().inTrial)
    }

    @Test
    fun recentPriceChange_onlyWithinLast30Days() {
        fun changeOn(effectiveFrom: java.time.LocalDate) = buildOverview(
            listOf(subscription(priceChanges = listOf(priceChange(effectiveFrom, 699, 799)))),
            today,
        ).subscriptions.single().recentPriceChangeCents

        assertEquals(100, changeOn(today))
        assertEquals(100, changeOn(today.minusDays(30)))
        assertNull(changeOn(today.minusDays(31)))
        assertNull(changeOn(today.plusDays(1)))
    }

    @Test
    fun recentPriceChange_decreaseIsNegative() {
        val summary = buildOverview(
            listOf(subscription(priceChanges = listOf(priceChange(today.minusDays(3), 1399, 1099)))),
            today,
        ).subscriptions.single()
        assertEquals(-300, summary.recentPriceChangeCents)
    }

    @Test
    fun subscriptions_areSortedByNameIgnoringCase() {
        val overview = buildOverview(
            listOf(
                subscription(id = 1, name = "Spotify"),
                subscription(id = 2, name = "iCloud+"),
                subscription(id = 3, name = "Netflix"),
            ),
            today,
        )
        assertEquals(listOf("iCloud+", "Netflix", "Spotify"), overview.subscriptions.map { it.subscription.name })
    }
}
