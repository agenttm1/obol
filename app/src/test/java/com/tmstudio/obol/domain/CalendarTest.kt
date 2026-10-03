package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CalendarTest {

    private val october = YearMonth.of(2026, 10)

    @Test
    fun billingsIn_monthly_oncePerMonth() {
        val billings = subscription(firstBillingDate = date(2026, 1, 4)).billingsIn(october)
        assertEquals(listOf(date(2026, 10, 4)), billings.map { it.date })
        assertEquals(1399, billings.single().priceCents)
    }

    @Test
    fun billingsIn_31st_fallsToLastDayOfShortMonth() {
        val sub = subscription(firstBillingDate = date(2026, 1, 31))
        assertEquals(listOf(date(2026, 2, 28)), sub.billingsIn(YearMonth.of(2026, 2)).map { it.date })
        assertEquals(listOf(date(2028, 2, 29)), subscription(firstBillingDate = date(2028, 1, 31))
            .billingsIn(YearMonth.of(2028, 2)).map { it.date })
        assertEquals(listOf(date(2026, 3, 31)), sub.billingsIn(YearMonth.of(2026, 3)).map { it.date })
    }

    @Test
    fun billingsIn_weekly_allOccurrences() {
        val sub = subscription(cycle = BillingCycle.WEEKLY, firstBillingDate = date(2026, 9, 28))
        assertEquals(
            listOf(5, 12, 19, 26).map { date(2026, 10, it) },
            sub.billingsIn(october).map { it.date },
        )
    }

    @Test
    fun billingsIn_yearly_onlyInItsMonth() {
        val sub = subscription(cycle = BillingCycle.YEARLY, firstBillingDate = date(2025, 10, 20))
        assertEquals(listOf(date(2026, 10, 20)), sub.billingsIn(october).map { it.date })
        assertTrue(sub.billingsIn(YearMonth.of(2026, 11)).isEmpty())
    }

    @Test
    fun billingsIn_beforeFirstBilling_isEmpty_andFirstMonthCountsFromFirstBilling() {
        val sub = subscription(firstBillingDate = date(2026, 10, 20))
        assertTrue(sub.billingsIn(YearMonth.of(2026, 9)).isEmpty())
        assertEquals(listOf(date(2026, 10, 20)), sub.billingsIn(october).map { it.date })
    }

    @Test
    fun billingsIn_usesPriceOnEachDay() {
        val sub = subscription(
            cycle = BillingCycle.WEEKLY,
            basePriceCents = 300,
            firstBillingDate = date(2026, 10, 1),
            priceChanges = listOf(priceChange(date(2026, 10, 15), 200, 300)),
            promo = promo(100, startsOn = date(2026, 10, 1), endsOn = date(2026, 10, 7)),
        )
        assertEquals(listOf(100, 200, 300, 300, 300), sub.billingsIn(october).map { it.priceCents })
    }

    @Test
    fun calendarMonth_sortsTotalsAndCountsDays() {
        val calendar = buildCalendarMonth(
            listOf(
                subscription(id = 1, name = "Spotify", basePriceCents = 699, firstBillingDate = date(2026, 1, 7)),
                subscription(id = 2, name = "Netflix", basePriceCents = 1399, firstBillingDate = date(2026, 1, 4)),
                subscription(id = 3, name = "HBO Max", basePriceCents = 999, firstBillingDate = date(2026, 1, 7)),
                subscription(id = 4, name = "Stari", isActive = false, firstBillingDate = date(2026, 1, 5)),
            ),
            october,
        )
        assertEquals(listOf("Netflix", "HBO Max", "Spotify"), calendar.billings.map { it.subscription.name })
        assertEquals(1399 + 699 + 999, calendar.totalCents)
        assertEquals(2, calendar.billingDays)
        assertEquals(2, calendar.billingsOn(date(2026, 10, 7)).size)
    }

    @Test
    fun calendarMonth_trialChargesNothingUntilItEnds() {
        val calendar = buildCalendarMonth(
            listOf(
                subscription(
                    basePriceCents = 999,
                    firstBillingDate = date(2026, 10, 6),
                    trial = trial(endsOn = date(2026, 10, 5)),
                )
            ),
            october,
        )
        assertEquals(listOf(date(2026, 10, 6)), calendar.billings.map { it.date })
        assertEquals(999, calendar.totalCents)
    }
}
