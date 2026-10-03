package com.tmstudio.obol.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Probni period. Najviše jedan po pretplati — zato je subscriptionId primarni ključ. */
@Entity(
    tableName = "trial_periods",
    foreignKeys = [
        ForeignKey(
            entity = Subscription::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TrialPeriod(
    @PrimaryKey val subscriptionId: Long,
    /** Zadnji dan probnog perioda (uključivo). */
    val endsOn: LocalDate,
    /** Obično 0. */
    val priceCents: Int = 0,
)

/**
 * Promotivna cijena. Najviše jedna po pretplati.
 *
 * Korisnik unosi broj ciklusa, a [endsOn] se izračuna pri unosu i spremi,
 * da kasnije računanje ne mora ponovno simulirati cikluse.
 */
@Entity(
    tableName = "promo_periods",
    foreignKeys = [
        ForeignKey(
            entity = Subscription::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PromoPeriod(
    @PrimaryKey val subscriptionId: Long,
    val priceCents: Int,
    val startsOn: LocalDate,
    /** Zadnji dan promocije (uključivo). */
    val endsOn: LocalDate,
)
