package com.tmstudio.obol.data.catalog

import com.tmstudio.obol.data.db.entity.Service
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Sadržaj `assets/services.json`. */
@Serializable
data class CatalogFile(
    /** Povećava se pri svakoj izmjeni datoteke; tako aplikacija zna da treba osvježiti bazu. */
    val version: Int,
    /** ISO datum zadnje provjere cijena, npr. "2026-10-01". */
    val updatedOn: String,
    val services: List<Service>,
)

// Strogo parsiranje: tipfeler u ključu (npr. "priceCent") mora puknuti u testu,
// a ne tiho izgubiti podatak.
private val catalogJson = Json { ignoreUnknownKeys = false }

fun parseCatalog(json: String): CatalogFile = catalogJson.decodeFromString(json)
