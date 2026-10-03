package com.tmstudio.obol.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Servis iz kataloga (`assets/services.json`). Read-only za korisnika.
 * Isti oblik služi i za čitanje JSON-a i kao Room entitet.
 */
@Serializable
@Entity(tableName = "services")
data class Service(
    /** Slug, npr. "netflix". */
    @PrimaryKey val id: String,
    val name: String,
    val category: Category,
    /** Boja monograma, npr. "#FF8168". */
    val colorHex: String,
    val monogram: String,
    /** Sprema se kao JSON stupac — planovi se nikad ne pretražuju zasebno. */
    val plans: List<CatalogPlan>,
)

@Serializable
data class CatalogPlan(
    val name: String,
    /** Cijena za jedan [cycle], u centima. */
    val priceCents: Int,
    val note: String? = null,
    /**
     * Ciklus naplate plana. Nije u spec-u, ali bez njega bi godišnji plan
     * ušao u pretplatu kao mjesečni. Izostavljen u JSON-u znači mjesečno.
     */
    val cycle: BillingCycle = BillingCycle.MONTHLY,
)
