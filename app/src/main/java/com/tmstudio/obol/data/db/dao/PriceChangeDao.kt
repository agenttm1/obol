package com.tmstudio.obol.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tmstudio.obol.data.db.entity.PriceChange
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceChangeDao {

    @Query("SELECT * FROM price_changes WHERE subscriptionId = :subscriptionId ORDER BY effectiveFrom DESC")
    fun observeFor(subscriptionId: Long): Flow<List<PriceChange>>

    @Insert
    suspend fun insert(change: PriceChange): Long
}
