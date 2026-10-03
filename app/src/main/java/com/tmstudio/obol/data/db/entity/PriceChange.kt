package com.tmstudio.obol.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/** Povijest promjena pune cijene pretplate. */
@Entity(
    tableName = "price_changes",
    foreignKeys = [
        ForeignKey(
            entity = Subscription::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subscriptionId", "effectiveFrom")],
)
data class PriceChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subscriptionId: Long,
    val effectiveFrom: LocalDate,
    val oldPriceCents: Int,
    val newPriceCents: Int,
    val recordedAt: Instant,
)
