package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class DetailsDomainTest {

    private val today = date(2026, 10, 3)

    @Test
    fun pastBillings_newestFirst_strictlyBeforeToday_limited() {
        val sub = subscription(firstBillingDate = date(2026, 1, 3))
        val past = sub.pastBillings(today, limit = 3)
        assertEquals(listOf(date(2026, 9, 3), date(2026, 8, 3), date(2026, 7, 3)), past.map { it.date })
        assertTrue(past.all { it.priceCents == 1399 })
    }

    @Test
    fun pastBillings_usePriceValidOnThatDay() {
        val sub = subscription(
            basePriceCents = 1099,
            firstBillingDate = date(2026, 7, 1),
            priceChanges = listOf(priceChange(date(2026, 9, 1), 999, 1099)),
        )
        val past = sub.pastBillings(today, limit = 10)
        assertEquals(listOf(1099, 1099, 999, 999), past.map { it.priceCents })
        assertEquals(date(2026, 7, 1), past.last().date)
    }

    @Test
    fun pastBillings_emptyBeforeFirstBilling() {
        assertTrue(subscription(firstBillingDate = today).pastBillings(today, 5).isEmpty())
        assertTrue(subscription(firstBillingDate = today.plusDays(9)).pastBillings(today, 5).isEmpty())
    }

    @Test
    fun pastBillings_monthEndAnchorStaysOnLastDay() {
        val sub = subscription(firstBillingDate = date(2026, 1, 31))
        val dates = sub.pastBillings(date(2026, 5, 1), limit = 4).map { it.date }
        assertEquals(listOf(date(2026, 4, 30), date(2026, 3, 31), date(2026, 2, 28), date(2026, 1, 31)), dates)
    }

    @Test
    fun nextCharge_duringTrial_isFirstBillingAfterTrial() {
        val sub = subscription(
            basePriceCents = 999,
            firstBillingDate = date(2026, 10, 6),
            trial = trial(endsOn = date(2026, 10, 5)),
            promo = promo(299, startsOn = date(2026, 10, 6), endsOn = date(2027, 1, 5)),
        )
        val charge = sub.nextChargeOnOrAfter(today)
        assertEquals(date(2026, 10, 6), charge.date)
        assertEquals(299, charge.priceCents)
    }

    @Test
    fun nextCharge_withoutTrial_isNextBilling() {
        val charge = subscription(firstBillingDate = date(2026, 1, 4)).nextChargeOnOrAfter(today)
        assertEquals(date(2026, 10, 4), charge.date)
        assertEquals(1399, charge.priceCents)
    }

    @Test
    fun fullPriceOn_ignoresTrialAndPromo() {
        val sub = subscription(
            basePriceCents = 999,
            trial = trial(endsOn = date(2026, 10, 5)),
            promo = promo(299, startsOn = date(2026, 10, 6), endsOn = date(2027, 1, 5)),
        )
        assertEquals(0, sub.priceOn(today))
        assertEquals(999, sub.fullPriceOn(today))
        assertEquals(999, sub.fullPriceOn(date(2026, 11, 6)))
    }

    @Test
    fun usageQuestion_asksAboutPreviousMonthOnce() {
        val sub = subscription(firstBillingDate = date(2026, 1, 15))
        assertEquals(YearMonth.of(2026, 9), sub.usageQuestionPeriod(today, emptySet()))
        assertNull(sub.usageQuestionPeriod(today, setOf(YearMonth.of(2026, 9))))
    }

    @Test
    fun usageQuestion_notForSubscriptionStartedThisMonth() {
        assertNull(subscription(firstBillingDate = date(2026, 10, 1)).usageQuestionPeriod(today, emptySet()))
        assertEquals(
            YearMonth.of(2026, 9),
            subscription(firstBillingDate = date(2026, 9, 30)).usageQuestionPeriod(today, emptySet()),
        )
    }

    @Test
    fun usageQuestion_notForInactive() {
        assertNull(subscription(isActive = false).usageQuestionPeriod(today, emptySet()))
    }

    @Test
    fun usageQuestion_inJanuaryAsksAboutDecember() {
        val sub = subscription(cycle = BillingCycle.YEARLY, firstBillingDate = date(2025, 3, 1))
        assertEquals(YearMonth.of(2026, 12), sub.usageQuestionPeriod(date(2027, 1, 2), emptySet()))
    }
}
