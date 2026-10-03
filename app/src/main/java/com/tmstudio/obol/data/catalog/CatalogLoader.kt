package com.tmstudio.obol.data.catalog

import com.tmstudio.obol.data.db.dao.ServiceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Pamti koja je verzija kataloga zadnja upisana u bazu. */
interface CatalogVersionStore {
    suspend fun loadedCatalogVersion(): Int?
    suspend fun setLoadedCatalogVersion(version: Int)
}

/**
 * Upisuje katalog iz assetsa u Room pri prvom pokretanju i kad nova verzija
 * aplikacije donese noviju datoteku. Ekrani katalog uvijek čitaju iz baze.
 */
class CatalogLoader(
    private val serviceDao: ServiceDao,
    private val versionStore: CatalogVersionStore,
    private val readCatalogJson: () -> String,
) {

    /** @return true ako je baza osvježena. */
    suspend fun syncIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val catalog = parseCatalog(readCatalogJson())
        val loadedVersion = versionStore.loadedCatalogVersion()
        val upToDate = loadedVersion != null && loadedVersion >= catalog.version
        if (upToDate && serviceDao.count() > 0) return@withContext false

        serviceDao.replaceAll(catalog.services)
        versionStore.setLoadedCatalogVersion(catalog.version)
        true
    }
}
