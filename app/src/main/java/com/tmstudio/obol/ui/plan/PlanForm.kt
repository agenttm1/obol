package com.tmstudio.obol.ui.plan

import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.CatalogPlan
import com.tmstudio.obol.data.db.entity.Category
import com.tmstudio.obol.data.db.entity.PriceChange
import com.tmstudio.obol.data.db.entity.PromoPeriod
import com.tmstudio.obol.data.db.entity.Service
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import com.tmstudio.obol.data.db.entity.TrialPeriod
import com.tmstudio.obol.domain.billingDateAt
import com.tmstudio.obol.domain.defaultColorHex
import com.tmstudio.obol.domain.formatMoneyInput
import com.tmstudio.obol.domain.nextBillingOnOrAfter
import com.tmstudio.obol.domain.parseMoneyInput
import com.tmstudio.obol.domain.promoEndsOn
import java.time.Instant
import java.time.LocalDate

/** Nova pretplata spremna za `SubscriptionDao.insertWithPeriods`. */
data class NewSubscription(
    val subscription: Subscription,
    val trial: TrialPeriod?,
    val promo: PromoPeriod?,
)

/** Izmjena postojeće pretplate spremna za `SubscriptionDao.updateWithPeriods`. */
data class SubscriptionUpdate(
    val subscription: Subscription,
    val trial: TrialPeriod?,
    val promo: PromoPeriod?,
    val priceChange: PriceChange?,
)

enum class PlanFormError {
    NAME_MISSING,
    PRICE_INVALID,
    CYCLE_DAYS_INVALID,
    TRIAL_ENDED,
    PROMO_PRICE_INVALID,
    PROMO_NOT_LOWER,
}

/**
 * Stanje forme „Postavi plan", za novu pretplatu i za uređivanje postojeće.
 * Polja za iznose čuvaju tekst kako ga je korisnik upisao; u cente se
 * pretvaraju tek pri čitanju.
 *
 * Kad je uključen probni period, prva naplata je dan nakon njegova kraja —
 * tako naplata nikad ne pada na zadnji dan probnog perioda (spec, `priceOn`).
 * Promocija počinje prvom plaćenom naplatom.
 */
data class PlanForm(
    val serviceId: String?,
    val name: String,
    val catalogMonogram: String?,
    val catalogColorHex: String?,
    val category: Category,
    val plans: List<CatalogPlan>,
    val planLabel: String?,
    val priceText: String,
    val cycle: BillingCycle,
    val cycleDaysText: String,
    val billingDate: LocalDate,
    val trialEnabled: Boolean,
    val trialEndsOn: LocalDate,
    val promoEnabled: Boolean,
    val promoPriceText: String,
    val promoCycles: Int,
    /** Pretplata koja se uređuje; null za novu. */
    val original: SubscriptionWithDetails? = null,
    /** Od kada vrijedi nova puna cijena, ako se pri uređivanju promijenila. */
    val priceChangeFrom: LocalDate = billingDate,
) {
    val isManual: Boolean get() = serviceId == null

    val isEditing: Boolean get() = original != null

    val monogram: String
        get() = catalogMonogram ?: name.trim().take(1).uppercase().ifEmpty { "?" }

    val colorHex: String get() = catalogColorHex ?: category.defaultColorHex()

    val priceCents: Int? get() = parseMoneyInput(priceText)?.takeIf { it > 0 }

    /** Pri uređivanju: je li upisana puna cijena različita od dosadašnje. */
    val priceChanged: Boolean
        get() {
            val price = priceCents ?: return false
            return original != null && price != original.subscription.basePriceCents
        }

    val cycleDays: Int?
        get() = if (cycle == BillingCycle.CUSTOM_DAYS) {
            cycleDaysText.trim().toIntOrNull()?.takeIf { it in 1..MAX_CYCLE_DAYS }
        } else {
            null
        }

    private val cycleValid: Boolean
        get() = cycle != BillingCycle.CUSTOM_DAYS || cycleDays != null

    val firstBillingDate: LocalDate
        get() = if (trialEnabled) trialEndsOn.plusDays(1) else billingDate

    val promoPriceCents: Int? get() = parseMoneyInput(promoPriceText)

    /** Zadnji dan promocije; null dok promocija nije uključena ili ciklus nije ispravan. */
    val promoEndsOn: LocalDate?
        get() = if (promoEnabled && cycleValid) {
            promoEndsOn(firstBillingDate, cycle, cycleDays, promoCycles)
        } else {
            null
        }

    val fullPriceFrom: LocalDate? get() = promoEndsOn?.plusDays(1)

    fun withPlan(plan: CatalogPlan): PlanForm = copy(
        planLabel = plan.name,
        priceText = formatMoneyInput(plan.priceCents),
        cycle = plan.cycle,
    )

    fun errors(today: LocalDate): Set<PlanFormError> = buildSet {
        if (isManual && name.isBlank()) add(PlanFormError.NAME_MISSING)
        if (priceCents == null) add(PlanFormError.PRICE_INVALID)
        if (!cycleValid) add(PlanFormError.CYCLE_DAYS_INVALID)
        if (trialEnabled && trialEndsOn.isBefore(today)) add(PlanFormError.TRIAL_ENDED)
        if (promoEnabled) {
            val promo = promoPriceCents
            val full = priceCents
            when {
                promo == null -> add(PlanFormError.PROMO_PRICE_INVALID)
                full != null && promo >= full -> add(PlanFormError.PROMO_NOT_LOWER)
            }
        }
    }

    /** Entiteti za spremanje nove pretplate, ili null dok forma ima grešaka. */
    fun toNewSubscription(now: Instant, today: LocalDate): NewSubscription? {
        check(original == null) { "Forma uređuje postojeću pretplatu; koristi toUpdate" }
        if (errors(today).isNotEmpty()) return null
        val base = Subscription(
            serviceId = serviceId,
            name = "",
            monogram = "",
            colorHex = "",
            category = category,
            cycle = cycle,
            basePriceCents = 0,
            firstBillingDate = firstBillingDate,
            createdAt = now,
            updatedAt = now,
        )
        return NewSubscription(applyTo(base, now), trial(subscriptionId = 0), promo(subscriptionId = 0))
    }

    /** Izmjena postojeće pretplate, ili null dok forma ima grešaka. */
    fun toUpdate(now: Instant, today: LocalDate): SubscriptionUpdate? {
        val original = checkNotNull(original) { "Forma ne uređuje postojeću pretplatu" }
        if (errors(today).isNotEmpty()) return null
        val id = original.subscription.id
        val priceChange = if (priceChanged) {
            PriceChange(
                subscriptionId = id,
                effectiveFrom = priceChangeFrom,
                oldPriceCents = original.subscription.basePriceCents,
                newPriceCents = requireNotNull(priceCents),
                recordedAt = now,
            )
        } else {
            null
        }
        return SubscriptionUpdate(applyTo(original.subscription, now), trial(id), promo(id), priceChange)
    }

    private fun applyTo(subscription: Subscription, now: Instant) = subscription.copy(
        name = name.trim(),
        monogram = monogram,
        colorHex = colorHex,
        category = category,
        cycle = cycle,
        cycleDays = cycleDays,
        basePriceCents = requireNotNull(priceCents),
        firstBillingDate = firstBillingDate,
        planLabel = planLabel,
        updatedAt = now,
    )

    private fun trial(subscriptionId: Long) =
        if (trialEnabled) TrialPeriod(subscriptionId = subscriptionId, endsOn = trialEndsOn) else null

    private fun promo(subscriptionId: Long) = promoEndsOn?.let { endsOn ->
        PromoPeriod(
            subscriptionId = subscriptionId,
            priceCents = requireNotNull(promoPriceCents),
            startsOn = firstBillingDate,
            endsOn = endsOn,
        )
    }

    companion object {
        const val MAX_CYCLE_DAYS = 3 * 365
        const val MIN_PROMO_CYCLES = 1
        const val MAX_PROMO_CYCLES = 24
        private const val DEFAULT_PROMO_CYCLES = 3
        private const val DEFAULT_TRIAL_DAYS = 7L

        fun forService(service: Service, today: LocalDate): PlanForm {
            val base = blank(today).copy(
                serviceId = service.id,
                name = service.name,
                catalogMonogram = service.monogram,
                catalogColorHex = service.colorHex,
                category = service.category,
                plans = service.plans,
            )
            return service.plans.firstOrNull()?.let(base::withPlan) ?: base
        }

        fun manual(today: LocalDate): PlanForm = blank(today)

        /**
         * Forma za uređivanje. Probni period koji je već završio se ne nudi
         * za uređivanje — na cijene više ne utječe.
         */
        fun forSubscription(details: SubscriptionWithDetails, service: Service?, today: LocalDate): PlanForm {
            val sub = details.subscription
            val isCatalog = sub.serviceId != null
            val activeTrial = details.trial?.takeIf { !it.endsOn.isBefore(today) }
            val promo = details.promo
            return blank(today).copy(
                serviceId = sub.serviceId,
                name = sub.name,
                catalogMonogram = if (isCatalog) sub.monogram else null,
                catalogColorHex = if (isCatalog) sub.colorHex else null,
                category = sub.category,
                plans = service?.plans.orEmpty(),
                planLabel = sub.planLabel,
                priceText = formatMoneyInput(sub.basePriceCents),
                cycle = sub.cycle,
                cycleDaysText = sub.cycleDays?.toString().orEmpty(),
                billingDate = sub.firstBillingDate,
                trialEnabled = activeTrial != null,
                trialEndsOn = activeTrial?.endsOn ?: today.plusDays(DEFAULT_TRIAL_DAYS),
                promoEnabled = promo != null,
                promoPriceText = promo?.let { formatMoneyInput(it.priceCents) }.orEmpty(),
                promoCycles = promo?.let { promoCycles(it, sub) } ?: DEFAULT_PROMO_CYCLES,
                original = details,
                priceChangeFrom = details.nextBillingOnOrAfter(today),
            )
        }

        /** Broj ciklusa promocije, unatrag iz spremljenog `endsOn`. */
        private fun promoCycles(promo: PromoPeriod, sub: Subscription): Int =
            (MIN_PROMO_CYCLES..MAX_PROMO_CYCLES).firstOrNull { n ->
                billingDateAt(promo.startsOn, sub.cycle, sub.cycleDays, n.toLong()).isAfter(promo.endsOn)
            } ?: MAX_PROMO_CYCLES

        private fun blank(today: LocalDate) = PlanForm(
            serviceId = null,
            name = "",
            catalogMonogram = null,
            catalogColorHex = null,
            category = Category.OTHER,
            plans = emptyList(),
            planLabel = null,
            priceText = "",
            cycle = BillingCycle.MONTHLY,
            cycleDaysText = "",
            billingDate = today,
            trialEnabled = false,
            trialEndsOn = today.plusDays(DEFAULT_TRIAL_DAYS),
            promoEnabled = false,
            promoPriceText = "",
            promoCycles = DEFAULT_PROMO_CYCLES,
        )
    }
}
