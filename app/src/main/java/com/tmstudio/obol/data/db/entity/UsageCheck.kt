package com.tmstudio.obol.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.Instant
import java.time.YearMonth

/** Odgovor na mjesečno pitanje „koristiš li ovo". */
@Entity(
    tableName = "usage_checks",
    primaryKeys = ["subscriptionId", "period"],
    foreignKeys = [
        ForeignKey(
            entity = Subscription::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class UsageCheck(
    val subscriptionId: Long,
    /** Mjesec na koji se odgovor odnosi; sprema se kao Int, npr. 202610. */
    val period: YearMonth,
    val used: Boolean,
    val answeredAt: Instant,
)
