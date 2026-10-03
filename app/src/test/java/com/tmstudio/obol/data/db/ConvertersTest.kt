package com.tmstudio.obol.data.db

import com.tmstudio.obol.data.db.entity.CatalogPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun localDate_roundTripsThroughEpochDay() {
        listOf(
            LocalDate.of(2026, 10, 3),
            LocalDate.of(2028, 2, 29),
            LocalDate.of(1970, 1, 1),
            LocalDate.of(1969, 12, 31),
        ).forEach { date ->
            assertEquals(date, converters.epochDayToLocalDate(converters.localDateToEpochDay(date)))
        }
        assertEquals(0L, converters.localDateToEpochDay(LocalDate.of(1970, 1, 1)))
    }

    @Test
    fun yearMonth_isStoredAsYyyyMm() {
        assertEquals(202610, converters.yearMonthToInt(YearMonth.of(2026, 10)))
        assertEquals(202601, converters.yearMonthToInt(YearMonth.of(2026, 1)))
        assertEquals(YearMonth.of(2026, 12), converters.intToYearMonth(202612))
    }

    @Test
    fun instant_roundTripsWithMillisecondPrecision() {
        val instant = Instant.parse("2026-10-03T19:15:30.123Z")
        assertEquals(instant, converters.epochMilliToInstant(converters.instantToEpochMilli(instant)))
    }

    @Test
    fun plans_roundTripThroughJson() {
        val plans = listOf(
            CatalogPlan("Standard s reklamama", 599),
            CatalogPlan("Premium", 1399, note = "4K + HDR"),
        )
        assertEquals(plans, converters.jsonToPlans(converters.plansToJson(plans)))
    }

    @Test
    fun nulls_passThrough() {
        assertNull(converters.localDateToEpochDay(null))
        assertNull(converters.epochDayToLocalDate(null))
        assertNull(converters.yearMonthToInt(null))
        assertNull(converters.intToYearMonth(null))
    }
}
