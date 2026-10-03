package com.tmstudio.obol.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ObolFormatTest {

    // NumberFormat stavlja neprelomivi razmak prije znaka valute.
    private fun String.normalizeSpaces() = replace(' ', ' ').replace(' ', ' ')

    @Test
    fun money_usesCommaAndEuroAfterSpace() {
        assertEquals("13,99 €", ObolFormat.money(1399).normalizeSpaces())
        assertEquals("0,99 €", ObolFormat.money(99).normalizeSpaces())
        assertEquals("0,00 €", ObolFormat.money(0).normalizeSpaces())
        assertEquals("1.234,50 €", ObolFormat.money(123450).normalizeSpaces())
    }

    @Test
    fun signedMoney() {
        assertEquals("+1,00 €", ObolFormat.signedMoney(100).normalizeSpaces())
        assertEquals("−2,50 €", ObolFormat.signedMoney(-250).normalizeSpaces())
    }

    @Test
    fun amount_withoutCurrency() {
        assertEquals("46,94", ObolFormat.amount(4694))
        assertEquals("1.046,94", ObolFormat.amount(104694))
    }

    @Test
    fun dates_useLowercaseGenitiveMonths() {
        assertEquals("4. listopada", ObolFormat.dateLong(LocalDate.of(2026, 10, 4)))
        assertEquals("1. prosinca", ObolFormat.dateLong(LocalDate.of(2026, 12, 1)))
        assertEquals("4. 10. 2026.", ObolFormat.dateShort(LocalDate.of(2026, 10, 4)))
        assertEquals("4. rujna 2026.", ObolFormat.dateLongWithYear(LocalDate.of(2026, 9, 4)))
    }

    @Test
    fun monthYear_usesNominative() {
        assertEquals("listopad 2026", ObolFormat.monthYear(YearMonth.of(2026, 10)))
    }
}
