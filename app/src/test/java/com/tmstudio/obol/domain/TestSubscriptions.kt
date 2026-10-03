package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.Category
import com.tmstudio.obol.data.db.entity.PriceChange
import com.tmstudio.obol.data.db.entity.PromoPeriod
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import com.tmstudio.obol.data.db.entity.TrialPeriod
import java.time.Instant
import java.time.LocalDate

internal val NOW: Instant = Instant.parse("2026-10-03T10:00:00Z")

internal fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)

internal fun subscription(
    id: Long = 1,
    name: String = "Netflix",
    isActive: Boolean = true,
    cycle: BillingCycle = BillingCycle.MONTHLY,
    firstBillingDate: LocalDate = date(2026, 1, 15),
    basePriceCents: Int = 1399,
    cycleDays: Int? = null,
    trial: TrialPeriod? = null,
    promo: PromoPeriod? = null,
    priceChanges: List<PriceChange> = emptyList(),
) = SubscriptionWithDetails(
    subscription = Subscription(
        id = id,
        serviceId = "netflix",
        name = name,
        monogram = "N",
        colorHex = "#FF8168",
        category = Category.ENTERTAINMENT,
        cycle = cycle,
        cycleDays = cycleDays,
        basePriceCents = basePriceCents,
        firstBillingDate = firstBillingDate,
        isActive = isActive,
        createdAt = NOW,
        updatedAt = NOW,
    ),
    trial = trial,
    promo = promo,
    priceChanges = priceChanges,
)

internal fun trial(endsOn: LocalDate, priceCents: Int = 0) =
    TrialPeriod(subscriptionId = 1, endsOn = endsOn, priceCents = priceCents)

internal fun promo(priceCents: Int, startsOn: LocalDate, endsOn: LocalDate) =
    PromoPeriod(subscriptionId = 1, priceCents = priceCents, startsOn = startsOn, endsOn = endsOn)

internal fun priceChange(
    effectiveFrom: LocalDate,
    oldPriceCents: Int,
    newPriceCents: Int,
    id: Long = 0,
    recordedAt: Instant = NOW,
) = PriceChange(
    id = id,
    subscriptionId = 1,
    effectiveFrom = effectiveFrom,
    oldPriceCents = oldPriceCents,
    newPriceCents = newPriceCents,
    recordedAt = recordedAt,
)
