package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.CatalogPlan
import com.tmstudio.obol.data.db.entity.Category
import com.tmstudio.obol.data.db.entity.Service
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddFlowDomainTest {

    @Test
    fun parseMoneyInput_acceptsCommaDotAndWholeNumbers() {
        assertEquals(999, parseMoneyInput("9,99"))
        assertEquals(999, parseMoneyInput("9.99"))
        assertEquals(990, parseMoneyInput("9,9"))
        assertEquals(1000, parseMoneyInput("10"))
        assertEquals(1000, parseMoneyInput("10,"))
        assertEquals(1399, parseMoneyInput(" 13,99 € "))
        assertEquals(0, parseMoneyInput("0"))
        assertEquals(99_999_999, parseMoneyInput("999999,99"))
    }

    @Test
    fun parseMoneyInput_rejectsInvalid() {
        listOf("", ",", "9,999", "abc", "1,2,3", "-5", "1000000", "9 99x").forEach {
            assertNull("„$it\" bi trebao biti neispravan", parseMoneyInput(it))
        }
    }

    @Test
    fun formatMoneyInput_roundTrips() {
        assertEquals("9,99", formatMoneyInput(999))
        assertEquals("0,05", formatMoneyInput(5))
        assertEquals("110,99", formatMoneyInput(11099))
        listOf(0, 5, 999, 11099).forEach { assertEquals(it, parseMoneyInput(formatMoneyInput(it))) }
    }

    @Test
    fun billingDateAt_countsFromAnchor() {
        val jan31 = date(2026, 1, 31)
        assertEquals(jan31, billingDateAt(jan31, BillingCycle.MONTHLY, null, 0))
        assertEquals(date(2026, 2, 28), billingDateAt(jan31, BillingCycle.MONTHLY, null, 1))
        assertEquals(date(2026, 3, 31), billingDateAt(jan31, BillingCycle.MONTHLY, null, 2))
        assertEquals(date(2026, 2, 14), billingDateAt(jan31, BillingCycle.WEEKLY, null, 2))
        assertEquals(date(2026, 2, 20), billingDateAt(jan31, BillingCycle.CUSTOM_DAYS, 10, 2))
        assertEquals(date(2027, 1, 31), billingDateAt(jan31, BillingCycle.YEARLY, null, 1))
    }

    @Test
    fun promoEndsOn_isDayBeforeFirstFullPriceBilling() {
        // Mockup Plan.html: promo 3 mjeseca od 5. 10. → puna cijena od 5. 1. 2027.
        assertEquals(date(2027, 1, 4), promoEndsOn(date(2026, 10, 5), BillingCycle.MONTHLY, null, 3))
        assertEquals(date(2026, 2, 27), promoEndsOn(date(2026, 1, 31), BillingCycle.MONTHLY, null, 1))
    }

    @Test
    fun promoEndsOn_agreesWithPriceOn() {
        val start = date(2026, 10, 6)
        val sub = subscription(
            basePriceCents = 1099,
            firstBillingDate = start,
            promo = promo(299, startsOn = start, endsOn = promoEndsOn(start, BillingCycle.MONTHLY, null, 3)),
        )
        val prices = (0L..3L).map { sub.priceOn(billingDateAt(start, BillingCycle.MONTHLY, null, it)) }
        assertEquals(listOf(299, 299, 299, 1099), prices)
    }

    @Test
    fun monthlyEquivalent_ofPlainPrice() {
        assertEquals(925, monthlyEquivalent(11099, BillingCycle.YEARLY))
        assertEquals(1014, monthlyEquivalent(1000, BillingCycle.CUSTOM_DAYS, 30))
    }

    @Test
    fun cheapestPlan_prefersMonthlyPlans() {
        val service = service(
            "google-one", "Google One",
            CatalogPlan("Premium 2 TB", 999),
            CatalogPlan("Basic 100 GB", 199),
            CatalogPlan("Basic 100 GB", 1999, cycle = BillingCycle.YEARLY),
        )
        assertEquals(199, service.cheapestPlan()!!.priceCents)
    }

    @Test
    fun cheapestPlan_withoutMonthly_comparesByMonthlyEquivalent() {
        val service = service(
            "nintendo", "Nintendo",
            CatalogPlan("Obiteljski", 3499, cycle = BillingCycle.YEARLY),
            CatalogPlan("Pojedinačni", 799, cycle = BillingCycle.QUARTERLY),
            CatalogPlan("Pojedinačni", 1999, cycle = BillingCycle.YEARLY),
        )
        assertEquals(1999, service.cheapestPlan()!!.priceCents)
    }

    @Test
    fun search_ignoresCaseAndDiacritics() {
        val services = listOf(
            service("disney-plus", "Disney+"),
            service("netflix", "Netflix"),
            service("zivot", "Život"),
            service("dj", "Đir"),
        )
        assertEquals(listOf("Disney+"), services.search("disney").map { it.name })
        assertEquals(listOf("Netflix"), services.search(" FLIX ").map { it.name })
        assertEquals(listOf("Život"), services.search("zivot").map { it.name })
        assertEquals(listOf("Đir"), services.search("dir").map { it.name })
        assertEquals(4, services.search("").size)
    }

    private fun service(id: String, name: String, vararg plans: CatalogPlan) = Service(
        id = id,
        name = name,
        category = Category.OTHER,
        colorHex = "#FFFFFF",
        monogram = name.take(1),
        plans = plans.toList(),
    )
}
