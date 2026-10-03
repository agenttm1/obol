package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import java.time.LocalDate
import java.time.YearMonth

/**
 * Mjesec za koji treba pitati „koristiš li ovo": prošli mjesec, ako je
 * pretplata tada već bila naplaćivana i odgovor još ne postoji. Inače null.
 */
fun SubscriptionWithDetails.usageQuestionPeriod(today: LocalDate, answered: Set<YearMonth>): YearMonth? {
    if (!subscription.isActive) return null
    val previous = YearMonth.from(today).minusMonths(1)
    if (subscription.firstBillingDate.isAfter(previous.atEndOfMonth())) return null
    return previous.takeIf { it !in answered }
}
