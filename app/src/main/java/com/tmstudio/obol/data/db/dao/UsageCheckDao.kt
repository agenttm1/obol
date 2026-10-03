package com.tmstudio.obol.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tmstudio.obol.data.db.entity.UsageCheck
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

@Dao
interface UsageCheckDao {

    @Query("SELECT * FROM usage_checks WHERE subscriptionId = :subscriptionId ORDER BY period DESC")
    fun observeFor(subscriptionId: Long): Flow<List<UsageCheck>>

    @Query("SELECT * FROM usage_checks WHERE subscriptionId = :subscriptionId AND period = :period")
    suspend fun get(subscriptionId: Long, period: YearMonth): UsageCheck?

    /** Svi odgovori za mjesec — worker iz njih vidi koga još treba pitati. */
    @Query("SELECT * FROM usage_checks WHERE period = :period")
    suspend fun getForPeriod(period: YearMonth): List<UsageCheck>

    /** Ponovni odgovor za isti mjesec zamjenjuje prethodni. */
    @Upsert
    suspend fun upsert(check: UsageCheck)
}
