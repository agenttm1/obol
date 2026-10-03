package com.tmstudio.obol.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/**
 * Formatiranje iz poglavlja 8 specifikacije. Iznosi su uvijek u centima i
 * pretvaraju se preko BigDecimal, nikad preko Double.
 *
 * NumberFormat nije thread-safe, pa se instanca stvara pri svakom pozivu.
 */
object ObolFormat {

    val locale: Locale = Locale.forLanguageTag("hr-HR")

    private val dateLong = DateTimeFormatter.ofPattern("d. MMMM", locale)
    private val dateLongWithYear = DateTimeFormatter.ofPattern("d. MMMM yyyy.", locale)
    private val dateShort = DateTimeFormatter.ofPattern("d. M. yyyy.", locale)
    private val monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", locale)

    /** `13,99 €` — valuta se postavlja izričito, jer stariji ICU za hr-HR vraća kune. */
    fun money(cents: Int, currencyCode: String = "EUR"): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode)
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(cents.toBigDecimalAmount())

    /** `+1,00 €` / `−1,00 €` — za promjene cijene. */
    fun signedMoney(cents: Int, currencyCode: String = "EUR"): String {
        val sign = if (cents < 0) MINUS else PLUS
        return sign + money(kotlin.math.abs(cents), currencyCode)
    }

    /** `46,94` — iznos bez znaka valute, za veliki prikaz gdje je znak zaseban. */
    fun amount(cents: Int): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(cents.toBigDecimalAmount())

    /** Simbol valute, npr. `€`. */
    fun currencySymbol(currencyCode: String = "EUR"): String =
        Currency.getInstance(currencyCode).getSymbol(locale)

    /** `4. listopada` */
    fun dateLong(date: LocalDate): String = date.format(dateLong)

    /** `4. rujna 2026.` */
    fun dateLongWithYear(date: LocalDate): String = date.format(dateLongWithYear)

    /** `4. 10. 2026.` */
    fun dateShort(date: LocalDate): String = date.format(dateShort)

    /** `listopad 2026` — nominativ, malim slovom. */
    fun monthYear(month: YearMonth): String = month.format(monthYear)

    private fun Int.toBigDecimalAmount(): BigDecimal = BigDecimal.valueOf(toLong(), 2)

    private const val PLUS = "+"
    private const val MINUS = "−"
}
