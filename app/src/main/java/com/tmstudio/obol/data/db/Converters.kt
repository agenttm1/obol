package com.tmstudio.obol.data.db

import androidx.room.TypeConverter
import com.tmstudio.obol.data.db.entity.CatalogPlan
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class Converters {

    /** LocalDate se sprema kao epochDay — bez vremenske zone, bez Date/Calendar. */
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)

    /** YearMonth se sprema kao yyyyMM, npr. listopad 2026. → 202610. */
    @TypeConverter
    fun yearMonthToInt(yearMonth: YearMonth?): Int? =
        yearMonth?.let { it.year * 100 + it.monthValue }

    @TypeConverter
    fun intToYearMonth(value: Int?): YearMonth? = value?.let { YearMonth.of(it / 100, it % 100) }

    @TypeConverter
    fun plansToJson(plans: List<CatalogPlan>?): String? = plans?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToPlans(value: String?): List<CatalogPlan>? = value?.let { json.decodeFromString(it) }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
