package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlyEquivalentTest {

    private val today = date(2026, 10, 3)

    private fun monthly(cycle: BillingCycle, price: Int, cycleDays: Int? = null) =
        subscription(cycle = cycle, basePriceCents = price, cycleDays = cycleDays)
            .monthlyEquivalentOn(today)

    @Test
    fun monthly_isUnchanged() {
        assertEquals(1399, monthly(BillingCycle.MONTHLY, 1399))
    }

    @Test
    fun yearly_dividedBy12() {
        assertEquals(1000, monthly(BillingCycle.YEARLY, 12000))
        assertEquals(833, monthly(BillingCycle.YEARLY, 9999)) // 833,25
        assertEquals(834, monthly(BillingCycle.YEARLY, 10002)) // 833,5 → HALF_UP
    }

    @Test
    fun semiannual_dividedBy6() {
        assertEquals(1000, monthly(BillingCycle.SEMIANNUAL, 5997)) // 999,5 → HALF_UP
        assertEquals(999, monthly(BillingCycle.SEMIANNUAL, 5996)) // 999,33
    }

    @Test
    fun quarterly_dividedBy3() {
        assertEquals(1000, monthly(BillingCycle.QUARTERLY, 2999)) // 999,67
        assertEquals(333, monthly(BillingCycle.QUARTERLY, 1000)) // 333,33
    }

    @Test
    fun weekly_times52Over12() {
        assertEquals(1296, monthly(BillingCycle.WEEKLY, 299)) // 15548 / 12 = 1295,67
        assertEquals(433, monthly(BillingCycle.WEEKLY, 100)) // 433,33
    }

    @Test
    fun customDays_times365OverDaysOver12() {
        assertEquals(1014, monthly(BillingCycle.CUSTOM_DAYS, 1000, cycleDays = 30)) // 1013,89
        assertEquals(304, monthly(BillingCycle.CUSTOM_DAYS, 100, cycleDays = 10)) // 304,17
    }

    @Test
    fun customDays_roundsOnceAtTheEnd() {
        // 500 × 365 / 60 / 12 = 253,47 → 253.
        // Zaokruživanje međurezultata dalo bi 3041,67 → 3042, pa 3042 / 12 = 253,5 → 254.
        assertEquals(253, monthly(BillingCycle.CUSTOM_DAYS, 500, cycleDays = 60))
        // 501 × 365 / 14 / 12 = 1088,48 → 1088 (s međuzaokruživanjem bilo bi 1089).
        assertEquals(1088, monthly(BillingCycle.CUSTOM_DAYS, 501, cycleDays = 14))
    }

    @Test
    fun usesPriceOnTheGivenDate_trialCountsAsZero() {
        val sub = subscription(
            cycle = BillingCycle.YEARLY,
            basePriceCents = 12000,
            trial = trial(endsOn = date(2026, 10, 31)),
        )
        assertEquals(0, sub.monthlyEquivalentOn(date(2026, 10, 3)))
        assertEquals(1000, sub.monthlyEquivalentOn(date(2026, 11, 1)))
    }

    @Test
    fun usesPriceOnTheGivenDate_promo() {
        val sub = subscription(
            cycle = BillingCycle.QUARTERLY,
            basePriceCents = 3000,
            promo = promo(1500, startsOn = date(2026, 10, 1), endsOn = date(2026, 12, 31)),
        )
        assertEquals(500, sub.monthlyEquivalentOn(date(2026, 10, 3)))
        assertEquals(1000, sub.monthlyEquivalentOn(date(2027, 1, 1)))
    }

    @Test
    fun usesPriceOnTheGivenDate_priceChange() {
        val sub = subscription(
            basePriceCents = 1099,
            priceChanges = listOf(priceChange(date(2026, 12, 1), 599, 1099)),
        )
        assertEquals(599, sub.monthlyEquivalentOn(date(2026, 11, 30)))
        assertEquals(1099, sub.monthlyEquivalentOn(date(2026, 12, 1)))
    }
}
