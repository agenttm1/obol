package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.CatalogPlan
import com.tmstudio.obol.data.db.entity.Service
import java.text.Normalizer
import java.util.Locale

/**
 * Plan za oznaku „od X €": najjeftiniji mjesečni, jer ga ljudi tako uspoređuju.
 * Servis bez mjesečnih planova daje najjeftiniji po mjesečnom ekvivalentu.
 */
fun Service.cheapestPlan(): CatalogPlan? =
    plans.filter { it.cycle == BillingCycle.MONTHLY }.minByOrNull { it.priceCents }
        ?: plans.minByOrNull { monthlyEquivalent(it.priceCents, it.cycle) }

/**
 * Servisi čije ime sadrži upit, bez obzira na velika slova i dijakritike
 * („disney" nalazi „Disney+", „zivot" nalazi „Život"). Prazan upit vraća sve.
 */
fun List<Service>.search(query: String): List<Service> {
    val key = searchKey(query)
    if (key.isEmpty()) return this
    return filter { searchKey(it.name).contains(key) }
}

private val DIACRITICS = Regex("\\p{M}+")

internal fun searchKey(text: String): String =
    Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .replace('đ', 'd')
        .replace('Đ', 'D')
        .lowercase(Locale.ROOT)
