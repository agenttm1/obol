package com.tmstudio.obol.ui.plan

import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.CatalogPlan
import com.tmstudio.obol.data.db.entity.Category
import com.tmstudio.obol.data.db.entity.PromoPeriod
import com.tmstudio.obol.data.db.entity.Service
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import com.tmstudio.obol.data.db.entity.TrialPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PlanFormTest {

    private val today = LocalDate.of(2026, 10, 3)
    private val now = Instant.parse("2026-10-03T10:00:00Z")

    private val disney = Service(
        id = "disney-plus",
        name = "Disney+",
        category = Category.ENTERTAINMENT,
        colorHex = "#A99CF5",
        monogram = "D",
        plans = listOf(
            CatalogPlan("Standard s reklamama", 599),
            CatalogPlan("Standard", 999),
            CatalogPlan("Godišnji", 9999, cycle = BillingCycle.YEARLY),
        ),
    )

    @Test
    fun forService_preselectsFirstPlan() {
        val form = PlanForm.forService(disney, today)
        assertEquals("Standard s reklamama", form.planLabel)
        assertEquals("5,99", form.priceText)
        assertEquals(BillingCycle.MONTHLY, form.cycle)
        assertEquals("D", form.monogram)
        assertEquals("#A99CF5", form.colorHex)
    }

    @Test
    fun withPlan_setsPriceAndCycle() {
        val form = PlanForm.forService(disney, today).withPlan(disney.plans[2])
        assertEquals("99,99", form.priceText)
        assertEquals(BillingCycle.YEARLY, form.cycle)
        assertEquals("Godišnji", form.planLabel)
    }

    @Test
    fun simpleCatalogSubscription_isBuilt() {
        val result = PlanForm.forService(disney, today).toNewSubscription(now, today)!!
        with(result.subscription) {
            assertEquals("disney-plus", serviceId)
            assertEquals("Disney+", name)
            assertEquals(599, basePriceCents)
            assertEquals(today, firstBillingDate)
            assertEquals("EUR", currency)
            assertEquals(now, createdAt)
        }
        assertNull(result.trial)
        assertNull(result.promo)
    }

    @Test
    fun trial_movesFirstBillingToDayAfterTrialEnds() {
        val form = PlanForm.forService(disney, today)
            .copy(trialEnabled = true, trialEndsOn = LocalDate.of(2026, 10, 5), billingDate = today)
        val result = form.toNewSubscription(now, today)!!
        assertEquals(LocalDate.of(2026, 10, 5), result.trial!!.endsOn)
        assertEquals(0, result.trial!!.priceCents)
        assertEquals(LocalDate.of(2026, 10, 6), result.subscription.firstBillingDate)
    }

    @Test
    fun promo_startsAtFirstPaidBilling_andEndsAfterCycles() {
        val form = PlanForm.forService(disney, today).withPlan(disney.plans[1]).copy(
            trialEnabled = true,
            trialEndsOn = LocalDate.of(2026, 10, 4),
            promoEnabled = true,
            promoPriceText = "2,99",
            promoCycles = 3,
        )
        val promo = form.toNewSubscription(now, today)!!.promo!!
        assertEquals(299, promo.priceCents)
        assertEquals(LocalDate.of(2026, 10, 5), promo.startsOn)
        assertEquals(LocalDate.of(2027, 1, 4), promo.endsOn)
        assertEquals(LocalDate.of(2027, 1, 5), form.fullPriceFrom)
    }

    @Test
    fun manual_requiresName_andUsesCategoryColor() {
        val blank = PlanForm.manual(today).copy(priceText = "4,99")
        assertTrue(PlanFormError.NAME_MISSING in blank.errors(today))
        assertNull(blank.toNewSubscription(now, today))

        val named = blank.copy(name = "  teretana ", category = Category.FITNESS)
        val sub = named.toNewSubscription(now, today)!!.subscription
        assertNull(sub.serviceId)
        assertEquals("teretana", sub.name)
        assertEquals("T", sub.monogram)
        assertEquals("#8FB8FF", sub.colorHex)
        assertEquals(Category.FITNESS, sub.category)
    }

    @Test
    fun price_mustBePositiveAndValid() {
        val form = PlanForm.forService(disney, today)
        assertTrue(PlanFormError.PRICE_INVALID in form.copy(priceText = "").errors(today))
        assertTrue(PlanFormError.PRICE_INVALID in form.copy(priceText = "0").errors(today))
        assertTrue(PlanFormError.PRICE_INVALID in form.copy(priceText = "9,999").errors(today))
    }

    @Test
    fun customDays_requiresValidDayCount() {
        val form = PlanForm.forService(disney, today).copy(cycle = BillingCycle.CUSTOM_DAYS)
        assertTrue(PlanFormError.CYCLE_DAYS_INVALID in form.copy(cycleDaysText = "").errors(today))
        assertTrue(PlanFormError.CYCLE_DAYS_INVALID in form.copy(cycleDaysText = "0").errors(today))
        val ok = form.copy(cycleDaysText = "28").toNewSubscription(now, today)!!
        assertEquals(28, ok.subscription.cycleDays)
    }

    @Test
    fun cycleDays_isIgnoredForOtherCycles() {
        val sub = PlanForm.forService(disney, today).copy(cycleDaysText = "28")
            .toNewSubscription(now, today)!!.subscription
        assertNull(sub.cycleDays)
    }

    @Test
    fun trialInPast_isRejected() {
        val form = PlanForm.forService(disney, today).copy(trialEnabled = true, trialEndsOn = today.minusDays(1))
        assertTrue(PlanFormError.TRIAL_ENDED in form.errors(today))
        // Probni period koji završava danas je još valjan.
        assertTrue(form.copy(trialEndsOn = today).errors(today).isEmpty())
    }

    @Test
    fun promo_mustBeValidAndLowerThanFullPrice() {
        val form = PlanForm.forService(disney, today).copy(promoEnabled = true)
        assertTrue(PlanFormError.PROMO_PRICE_INVALID in form.copy(promoPriceText = "").errors(today))
        assertTrue(PlanFormError.PROMO_NOT_LOWER in form.copy(promoPriceText = "5,99").errors(today))
        assertTrue(form.copy(promoPriceText = "0").errors(today).isEmpty())
    }

    @Test
    fun disabledSections_areNotSaved() {
        val form = PlanForm.forService(disney, today).copy(
            trialEnabled = false,
            trialEndsOn = today.minusDays(10),
            promoEnabled = false,
            promoPriceText = "nevaljano",
        )
        val result = form.toNewSubscription(now, today)!!
        assertNull(result.trial)
        assertNull(result.promo)
        assertEquals(today, result.subscription.firstBillingDate)
    }
}

class PlanFormEditTest {

    private val today = LocalDate.of(2026, 10, 3)
    private val now = Instant.parse("2026-10-03T10:00:00Z")
    private val created = Instant.parse("2026-01-01T09:00:00Z")

    private val netflix = Service(
        id = "netflix",
        name = "Netflix",
        category = Category.ENTERTAINMENT,
        colorHex = "#FF8168",
        monogram = "N",
        plans = listOf(CatalogPlan("Standard", 1099), CatalogPlan("Premium", 1399)),
    )

    private fun stored(
        trial: TrialPeriod? = null,
        promo: PromoPeriod? = null,
        serviceId: String? = "netflix",
    ) = SubscriptionWithDetails(
        subscription = Subscription(
            id = 42,
            serviceId = serviceId,
            name = "Netflix",
            monogram = "N",
            colorHex = "#FF8168",
            category = Category.ENTERTAINMENT,
            cycle = BillingCycle.MONTHLY,
            basePriceCents = 1399,
            firstBillingDate = LocalDate.of(2026, 1, 4),
            planLabel = "Premium",
            notes = "dijeljeni račun",
            createdAt = created,
            updatedAt = created,
        ),
        trial = trial,
        promo = promo,
        priceChanges = emptyList(),
    )

    @Test
    fun forSubscription_prefillsFromStoredData() {
        val form = PlanForm.forSubscription(stored(), netflix, today)
        assertTrue(form.isEditing)
        assertEquals("13,99", form.priceText)
        assertEquals("Premium", form.planLabel)
        assertEquals(LocalDate.of(2026, 1, 4), form.billingDate)
        assertEquals(2, form.plans.size)
        assertEquals(LocalDate.of(2026, 10, 4), form.priceChangeFrom)
    }

    @Test
    fun unchangedForm_updatesWithoutPriceChange_andKeepsOtherFields() {
        val update = PlanForm.forSubscription(stored(), netflix, today).toUpdate(now, today)!!
        assertNull(update.priceChange)
        with(update.subscription) {
            assertEquals(42L, id)
            assertEquals(1399, basePriceCents)
            assertEquals("dijeljeni račun", notes)
            assertEquals(created, createdAt)
            assertEquals(now, updatedAt)
        }
    }

    @Test
    fun changedPrice_recordsPriceChangeFromNextBilling() {
        val form = PlanForm.forSubscription(stored(), netflix, today).copy(priceText = "14,99")
        assertTrue(form.priceChanged)
        val change = form.toUpdate(now, today)!!.priceChange!!
        assertEquals(42L, change.subscriptionId)
        assertEquals(LocalDate.of(2026, 10, 4), change.effectiveFrom)
        assertEquals(1399, change.oldPriceCents)
        assertEquals(1499, change.newPriceCents)
    }

    @Test
    fun endedTrial_isNotOfferedAndDoesNotBlockSaving() {
        val form = PlanForm.forSubscription(
            stored(trial = TrialPeriod(42, endsOn = LocalDate.of(2026, 1, 3))),
            netflix,
            today,
        )
        assertTrue(!form.trialEnabled)
        val update = form.toUpdate(now, today)!!
        assertNull(update.trial)
        assertEquals(LocalDate.of(2026, 1, 4), update.subscription.firstBillingDate)
    }

    @Test
    fun storedPromo_cyclesAreRecovered() {
        val promo = PromoPeriod(
            subscriptionId = 42,
            priceCents = 699,
            startsOn = LocalDate.of(2026, 1, 4),
            endsOn = LocalDate.of(2026, 4, 3),
        )
        val form = PlanForm.forSubscription(stored(promo = promo), netflix, today)
        assertTrue(form.promoEnabled)
        assertEquals(3, form.promoCycles)
        assertEquals("6,99", form.promoPriceText)
        assertEquals(promo, form.toUpdate(now, today)!!.promo)
    }

    @Test
    fun manualSubscription_nameIsEditable() {
        val form = PlanForm.forSubscription(stored(serviceId = null), service = null, today)
        assertTrue(form.isManual)
        val update = form.copy(name = "Kino klub").toUpdate(now, today)!!
        assertEquals("Kino klub", update.subscription.name)
        assertEquals("K", update.subscription.monogram)
    }
}
