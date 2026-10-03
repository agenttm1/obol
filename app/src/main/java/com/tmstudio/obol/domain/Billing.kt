package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.PriceChange
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Cijena koja vrijedi na zadani datum, u centima.
 *
 * Redoslijed provjere je obavezan:
 *  1. probni period: `date <= trial.endsOn` → `trial.priceCents`
 *  2. promo: `promo.startsOn <= date <= promo.endsOn` → `promo.priceCents`
 *  3. inače puna cijena prema povijesti cijena:
 *     - postoji promjena s `effectiveFrom <= date` → `newPriceCents` zadnje takve
 *     - sve promjene su nakon `date` → `oldPriceCents` prve od njih
 *     - nema promjena → `basePriceCents`
 *
 * Povijest ima prednost pred `basePriceCents` da bi najavljeno poskupljenje
 * (promjena s datumom u budućnosti) davalo staru cijenu do tog datuma, bez
 * obzira je li `basePriceCents` već postavljen na novu.
 */
fun SubscriptionWithDetails.priceOn(date: LocalDate): Int {
    trial?.let { if (!date.isAfter(it.endsOn)) return it.priceCents }
    promo?.let { if (!date.isBefore(it.startsOn) && !date.isAfter(it.endsOn)) return it.priceCents }
    return fullPriceOn(date)
}

/** Puna cijena na datum — korak 3 iz [priceOn], bez probnog perioda i promocije. */
fun SubscriptionWithDetails.fullPriceOn(date: LocalDate): Int {
    val lastApplied = priceChanges
        .filter { !it.effectiveFrom.isAfter(date) }
        .maxWithOrNull(priceChangeOrder)
    if (lastApplied != null) return lastApplied.newPriceCents

    val firstUpcoming = priceChanges.minWithOrNull(priceChangeOrder)
    return firstUpcoming?.oldPriceCents ?: subscription.basePriceCents
}

/**
 * Sljedeći datum naplate na ili nakon zadanog datuma. Za datume prije prve
 * naplate vraća [Subscription.firstBillingDate].
 *
 * Mjesečni ciklusi se uvijek računaju od prve naplate, a ne od prethodne:
 * naplata 31. siječnja daje 28. (ili 29.) veljače, pa opet 31. ožujka.
 * Isto vrijedi za 29. veljače kod godišnjeg ciklusa.
 *
 * Ne gleda [Subscription.isActive] ni [Subscription.cancelledOn] — o tome
 * odlučuje pozivatelj.
 */
fun SubscriptionWithDetails.nextBillingOnOrAfter(date: LocalDate): LocalDate =
    subscription.nextBillingOnOrAfter(date)

/**
 * Mjesečni ekvivalent cijene na zadani datum, u centima.
 *
 * YEARLY / 12, SEMIANNUAL / 6, QUARTERLY / 3, WEEKLY × 52 / 12,
 * CUSTOM_DAYS × 365 / cycleDays / 12. Zaokružuje se jednom, na kraju,
 * HALF_UP na cijeli cent.
 */
fun SubscriptionWithDetails.monthlyEquivalentOn(date: LocalDate): Int =
    monthlyEquivalent(
        priceCents = priceOn(date),
        cycle = subscription.cycle,
        cycleDays = if (subscription.cycle == BillingCycle.CUSTOM_DAYS) subscription.customCycleDays() else null,
    )

/** Mjesečni ekvivalent jedne cijene po ciklusu; pravila kao [monthlyEquivalentOn]. */
fun monthlyEquivalent(priceCents: Int, cycle: BillingCycle, cycleDays: Int? = null): Int {
    val price = priceCents.toLong()
    return when (cycle) {
        BillingCycle.WEEKLY -> divideHalfUp(price * 52, 12)
        BillingCycle.MONTHLY -> priceCents
        BillingCycle.QUARTERLY -> divideHalfUp(price, 3)
        BillingCycle.SEMIANNUAL -> divideHalfUp(price, 6)
        BillingCycle.YEARLY -> divideHalfUp(price, 12)
        BillingCycle.CUSTOM_DAYS -> {
            val days = requireNotNull(cycleDays) { "CUSTOM_DAYS bez cycleDays" }
            require(days > 0) { "cycleDays mora biti pozitivan, a je $days" }
            divideHalfUp(price * 365, days * 12L)
        }
    }
}

fun Subscription.nextBillingOnOrAfter(date: LocalDate): LocalDate {
    if (!date.isAfter(firstBillingDate)) return firstBillingDate

    val cycleMonths = when (cycle) {
        BillingCycle.MONTHLY -> 1L
        BillingCycle.QUARTERLY -> 3L
        BillingCycle.SEMIANNUAL -> 6L
        BillingCycle.YEARLY -> 12L
        BillingCycle.WEEKLY, BillingCycle.CUSTOM_DAYS -> null
    }

    if (cycleMonths == null) {
        val cycleLength = if (cycle == BillingCycle.WEEKLY) 7L else customCycleDays().toLong()
        val daysSinceFirst = ChronoUnit.DAYS.between(firstBillingDate, date)
        val cycles = (daysSinceFirst + cycleLength - 1) / cycleLength
        return firstBillingDate.plusDays(cycles * cycleLength)
    }

    // Procjena po kalendarskim mjesecima, pa najviše jedan korak naprijed
    // kad je dan u mjesecu naplate već prošao.
    val monthsSinceFirst = ChronoUnit.MONTHS.between(
        YearMonth.from(firstBillingDate),
        YearMonth.from(date),
    )
    var cycles = monthsSinceFirst / cycleMonths
    var billing = firstBillingDate.plusMonths(cycles * cycleMonths)
    while (billing.isBefore(date)) {
        cycles++
        billing = firstBillingDate.plusMonths(cycles * cycleMonths)
    }
    return billing
}

private fun Subscription.customCycleDays(): Int {
    val days = requireNotNull(cycleDays) { "CUSTOM_DAYS pretplata $id nema cycleDays" }
    require(days > 0) { "cycleDays mora biti pozitivan, a je $days (pretplata $id)" }
    return days
}

/** Promjene s istim datumom razdvaja vrijeme unosa, pa id. */
private val priceChangeOrder: Comparator<PriceChange> =
    compareBy<PriceChange>({ it.effectiveFrom }, { it.recordedAt }, { it.id })

/** Dijeljenje nenegativnog iznosa sa zaokruživanjem HALF_UP. */
private fun divideHalfUp(numerator: Long, denominator: Long): Int {
    require(numerator >= 0) { "Iznos ne smije biti negativan: $numerator" }
    return Math.toIntExact((2 * numerator + denominator) / (2 * denominator))
}

/**
 * Datum naplate s rednim brojem [index] (0 = prva), računat od [first].
 * Mjesečni ciklusi idu od sidra, pa 31. siječnja + 1 mjesec daje zadnji dan veljače.
 */
fun billingDateAt(first: LocalDate, cycle: BillingCycle, cycleDays: Int?, index: Long): LocalDate {
    require(index >= 0) { "index mora biti nenegativan: $index" }
    return when (cycle) {
        BillingCycle.WEEKLY -> first.plusDays(7 * index)
        BillingCycle.MONTHLY -> first.plusMonths(index)
        BillingCycle.QUARTERLY -> first.plusMonths(3 * index)
        BillingCycle.SEMIANNUAL -> first.plusMonths(6 * index)
        BillingCycle.YEARLY -> first.plusMonths(12 * index)
        BillingCycle.CUSTOM_DAYS -> {
            val days = requireNotNull(cycleDays) { "CUSTOM_DAYS bez cycleDays" }
            require(days > 0) { "cycleDays mora biti pozitivan, a je $days" }
            first.plusDays(days.toLong() * index)
        }
    }
}

/**
 * Zadnji dan promocije koja počinje naplatom [startsOn] i traje [cycles]
 * ciklusa. Puna cijena kreće s naplatom nakon toga.
 */
fun promoEndsOn(startsOn: LocalDate, cycle: BillingCycle, cycleDays: Int?, cycles: Int): LocalDate {
    require(cycles > 0) { "Promocija mora trajati barem jedan ciklus: $cycles" }
    return billingDateAt(startsOn, cycle, cycleDays, cycles.toLong()).minusDays(1)
}

/** Jedna naplata, izračunata u letu; ne sprema se u bazu. */
data class BillingEvent(val subscriptionId: Long, val date: LocalDate, val priceCents: Int)

/**
 * Sljedeća naplata koja se stvarno plaća. Dok traje probni period, to je prva
 * naplata nakon njegova kraja, a ne naplata po cijeni probnog perioda.
 */
fun SubscriptionWithDetails.nextChargeOnOrAfter(date: LocalDate): BillingEvent {
    val trial = trial
    val from = if (trial != null && !date.isAfter(trial.endsOn)) trial.endsOn.plusDays(1) else date
    val chargeDate = nextBillingOnOrAfter(from)
    return BillingEvent(subscription.id, chargeDate, priceOn(chargeDate))
}

/** Najviše [limit] naplata prije [today], od najnovije, s cijenom koja je tada vrijedila. */
fun SubscriptionWithDetails.pastBillings(today: LocalDate, limit: Int): List<BillingEvent> {
    val past = ArrayDeque<BillingEvent>(limit)
    var index = 0L
    while (true) {
        val date = billingDateAt(subscription.firstBillingDate, subscription.cycle, subscription.cycleDays, index)
        if (!date.isBefore(today)) break
        if (past.size == limit) past.removeFirst()
        past.addLast(BillingEvent(subscription.id, date, priceOn(date)))
        index++
    }
    return past.reversed()
}

/** Sve naplate u zadanom mjesecu, s iznosom koji vrijedi na taj dan (spec, poglavlje 4). */
fun SubscriptionWithDetails.billingsIn(month: YearMonth): List<BillingEvent> = buildList {
    var date = nextBillingOnOrAfter(month.atDay(1))
    while (YearMonth.from(date) == month) {
        add(BillingEvent(subscription.id, date, priceOn(date)))
        date = nextBillingOnOrAfter(date.plusDays(1))
    }
}
