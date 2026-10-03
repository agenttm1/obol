package com.tmstudio.obol.data.catalog

import com.tmstudio.obol.data.db.dao.ServiceDao
import com.tmstudio.obol.data.db.entity.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogLoaderTest {

    private class FakeServiceDao : ServiceDao() {
        val services = MutableStateFlow<Map<String, Service>>(emptyMap())
        var replaceCalls = 0

        override fun observeAll(): Flow<List<Service>> = throw NotImplementedError()
        override fun search(query: String): Flow<List<Service>> = throw NotImplementedError()
        override suspend fun getById(id: String) = services.value[id]
        override suspend fun count() = services.value.size
        override suspend fun upsertAll(services: List<Service>) {
            this.services.value += services.associateBy { it.id }
        }
        override suspend fun deleteAllExcept(keepIds: List<String>) {
            services.value = services.value.filterKeys { it in keepIds }
        }
        override suspend fun replaceAll(services: List<Service>) {
            replaceCalls++
            super.replaceAll(services)
        }
    }

    private class FakeVersionStore(var version: Int? = null) : CatalogVersionStore {
        override suspend fun loadedCatalogVersion() = version
        override suspend fun setLoadedCatalogVersion(version: Int) {
            this.version = version
        }
    }

    private fun catalogJson(version: Int, vararg ids: String) = """
        {"version":$version,"updatedOn":"2026-10-01","services":[${
            ids.joinToString(",") {
                """{"id":"$it","name":"$it","category":"OTHER","monogram":"X","colorHex":"#FFFFFF",
                   "plans":[{"name":"P","priceCents":100}]}"""
            }
        }]}
    """

    @Test
    fun firstRun_loadsCatalogAndStoresVersion() = runBlocking {
        val dao = FakeServiceDao()
        val store = FakeVersionStore()

        val loaded = CatalogLoader(dao, store) { catalogJson(1, "netflix", "spotify") }.syncIfNeeded()

        assertTrue(loaded)
        assertEquals(setOf("netflix", "spotify"), dao.services.value.keys)
        assertEquals(1, store.version)
    }

    @Test
    fun sameVersion_isSkipped() = runBlocking {
        val dao = FakeServiceDao()
        val store = FakeVersionStore()
        val loader = CatalogLoader(dao, store) { catalogJson(1, "netflix") }

        loader.syncIfNeeded()
        val loadedAgain = loader.syncIfNeeded()

        assertFalse(loadedAgain)
        assertEquals(1, dao.replaceCalls)
    }

    @Test
    fun newerVersion_replacesCatalogAndDropsRemovedServices() = runBlocking {
        val dao = FakeServiceDao()
        val store = FakeVersionStore()
        CatalogLoader(dao, store) { catalogJson(1, "netflix", "old-service") }.syncIfNeeded()

        val loaded = CatalogLoader(dao, store) { catalogJson(2, "netflix", "spotify") }.syncIfNeeded()

        assertTrue(loaded)
        assertEquals(setOf("netflix", "spotify"), dao.services.value.keys)
        assertEquals(2, store.version)
    }

    @Test
    fun emptyDatabase_isReloadedEvenIfVersionWasStored() = runBlocking {
        val dao = FakeServiceDao()
        val store = FakeVersionStore(version = 1)

        val loaded = CatalogLoader(dao, store) { catalogJson(1, "netflix") }.syncIfNeeded()

        assertTrue(loaded)
        assertEquals(setOf("netflix"), dao.services.value.keys)
    }
}
