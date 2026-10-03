package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import org.junit.Assert.assertEquals
import org.junit.Test

class NextBillingTest {

    @Test
    fun beforeOrOnFirstBilling_returnsFirstBilling() {
        val sub = subscription(firstBillingDate = date(2026, 10, 15))
        assertEquals(date(2026, 10, 15), sub.nextBillingOnOrAfter(date(2026, 9, 1)))
        assertEquals(date(2026, 10, 15), sub.nextBillingOnOrAfter(date(2026, 10, 15)))
    }

    @Test
    fun monthly_billingDayIsInclusive() {
        val sub = subscription(firstBillingDate = date(2026, 1, 15))
        assertEquals(date(2026, 2, 15), sub.nextBillingOnOrAfter(date(2026, 1, 16)))
        assertEquals(date(2026, 2, 15), sub.nextBillingOnOrAfter(date(2026, 2, 15)))
        assertEquals(date(2026, 3, 15), sub.nextBillingOnOrAfter(date(2026, 2, 16)))
    }

    @Test
    fun monthly_on31st_fallsToLastDayOfShortMonths() {
        val sub = subscription(firstBillingDate = date(2026, 1, 31))
        assertEquals(date(2026, 2, 28), sub.nextBillingOnOrAfter(date(2026, 2, 1)))
        assertEquals(date(2026, 4, 30), sub.nextBillingOnOrAfter(date(2026, 4, 1)))
        assertEquals(date(2026, 6, 30), sub.nextBillingOnOrAfter(date(2026, 6, 30)))
    }

    @Test
    fun monthly_on31st_returnsTo31stAfterShortMonth() {
        // Računa se od prve naplate, ne od prethodne — inače bi 28. 2. "zalijepio" dan.
        val sub = subscription(firstBillingDate = date(2026, 1, 31))
        assertEquals(date(2026, 3, 31), sub.nextBillingOnOrAfter(date(2026, 3, 1)))
        assertEquals(date(2026, 3, 31), sub.nextBillingOnOrAfter(date(2026, 3, 29)))
        assertEquals(date(2026, 5, 31), sub.nextBillingOnOrAfter(date(2026, 5, 1)))
    }

    @Test
    fun monthly_on30th_inFebruary() {
        val sub = subscription(firstBillingDate = date(2026, 1, 30))
        assertEquals(date(2026, 2, 28), sub.nextBillingOnOrAfter(date(2026, 2, 1)))
        assertEquals(date(2026, 3, 30), sub.nextBillingOnOrAfter(date(2026, 3, 1)))
    }

    @Test
    fun monthly_on31st_inLeapYearFebruary() {
        val sub = subscription(firstBillingDate = date(2028, 1, 31))
        assertEquals(date(2028, 2, 29), sub.nextBillingOnOrAfter(date(2028, 2, 1)))
        assertEquals(date(2028, 3, 31), sub.nextBillingOnOrAfter(date(2028, 3, 1)))
    }

    @Test
    fun yearly_on29February() {
        val sub = subscription(cycle = BillingCycle.YEARLY, firstBillingDate = date(2028, 2, 29))
        assertEquals(date(2029, 2, 28), sub.nextBillingOnOrAfter(date(2028, 3, 1)))
        assertEquals(date(2030, 2, 28), sub.nextBillingOnOrAfter(date(2029, 3, 1)))
        assertEquals(date(2032, 2, 29), sub.nextBillingOnOrAfter(date(2031, 3, 1)))
        assertEquals(date(2032, 2, 29), sub.nextBillingOnOrAfter(date(2032, 2, 29)))
    }

    @Test
    fun yearly_sameDayNextYear() {
        val sub = subscription(cycle = BillingCycle.YEARLY, firstBillingDate = date(2025, 10, 3))
        assertEquals(date(2026, 10, 3), sub.nextBillingOnOrAfter(date(2026, 10, 3)))
        assertEquals(date(2027, 10, 3), sub.nextBillingOnOrAfter(date(2026, 10, 4)))
    }

    @Test
    fun quarterly_fromEndOfMonth() {
        val sub = subscription(cycle = BillingCycle.QUARTERLY, firstBillingDate = date(2025, 11, 30))
        assertEquals(date(2026, 2, 28), sub.nextBillingOnOrAfter(date(2025, 12, 1)))
        assertEquals(date(2026, 5, 30), sub.nextBillingOnOrAfter(date(2026, 3, 1)))
        assertEquals(date(2026, 8, 30), sub.nextBillingOnOrAfter(date(2026, 5, 31)))
    }

    @Test
    fun semiannual_fromEndOfMonth() {
        val sub = subscription(cycle = BillingCycle.SEMIANNUAL, firstBillingDate = date(2025, 8, 31))
        assertEquals(date(2026, 2, 28), sub.nextBillingOnOrAfter(date(2025, 9, 1)))
        assertEquals(date(2026, 8, 31), sub.nextBillingOnOrAfter(date(2026, 3, 1)))
    }

    @Test
    fun weekly() {
        val sub = subscription(cycle = BillingCycle.WEEKLY, firstBillingDate = date(2026, 10, 1))
        assertEquals(date(2026, 10, 8), sub.nextBillingOnOrAfter(date(2026, 10, 2)))
        assertEquals(date(2026, 10, 8), sub.nextBillingOnOrAfter(date(2026, 10, 8)))
        assertEquals(date(2026, 10, 15), sub.nextBillingOnOrAfter(date(2026, 10, 9)))
    }

    @Test
    fun customDays() {
        val sub = subscription(
            cycle = BillingCycle.CUSTOM_DAYS,
            cycleDays = 10,
            firstBillingDate = date(2026, 10, 1),
        )
        assertEquals(date(2026, 10, 11), sub.nextBillingOnOrAfter(date(2026, 10, 11)))
        assertEquals(date(2026, 10, 21), sub.nextBillingOnOrAfter(date(2026, 10, 12)))
    }

    @Test
    fun firstBillingInPast_monthly() {
        val sub = subscription(firstBillingDate = date(2024, 3, 15))
        assertEquals(date(2026, 10, 15), sub.nextBillingOnOrAfter(date(2026, 10, 3)))
        assertEquals(date(2026, 11, 15), sub.nextBillingOnOrAfter(date(2026, 10, 16)))
    }

    @Test
    fun firstBillingInPast_weeklyAndYearly() {
        val weekly = subscription(cycle = BillingCycle.WEEKLY, firstBillingDate = date(2024, 1, 1))
        // 2024-01-01 je ponedjeljak, 2026-10-03 subota → sljedeći ponedjeljak.
        assertEquals(date(2026, 10, 5), weekly.nextBillingOnOrAfter(date(2026, 10, 3)))

        val yearly = subscription(cycle = BillingCycle.YEARLY, firstBillingDate = date(2020, 6, 1))
        assertEquals(date(2027, 6, 1), yearly.nextBillingOnOrAfter(date(2026, 10, 3)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun customDays_withoutCycleDays_fails() {
        subscription(cycle = BillingCycle.CUSTOM_DAYS, cycleDays = null)
            .nextBillingOnOrAfter(date(2026, 10, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun customDays_withZeroCycleDays_fails() {
        subscription(cycle = BillingCycle.CUSTOM_DAYS, cycleDays = 0)
            .nextBillingOnOrAfter(date(2026, 10, 3))
    }
}
