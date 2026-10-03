package com.tmstudio.obol.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Korisnikova pretplata.
 *
 * [serviceId] namjerno nije strani ključ na `services`: katalog se pri
 * osvježavanju može zamijeniti, a to ne smije dirati korisnikove pretplate.
 * Ime, monogram, boja i kategorija se zato kopiraju u pretplatu pri unosu.
 */
@Entity(
    tableName = "subscriptions",
    indices = [Index("serviceId"), Index("isActive")],
)
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** null kad je pretplata unesena ručno. */
    val serviceId: String?,
    val name: String,
    val monogram: String,
    val colorHex: String,
    val category: Category,
    val cycle: BillingCycle,
    /** Samo za [BillingCycle.CUSTOM_DAYS], inače null. */
    val cycleDays: Int? = null,
    /** Puna cijena u centima. */
    val basePriceCents: Int,
    val currency: String = "EUR",
    val firstBillingDate: LocalDate,
    /** npr. "Premium 4K" */
    val planLabel: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val cancelledOn: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
