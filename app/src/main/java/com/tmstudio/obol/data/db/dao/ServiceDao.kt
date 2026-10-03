package com.tmstudio.obol.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tmstudio.obol.data.db.entity.Service
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ServiceDao {

    @Query("SELECT * FROM services ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<Service>>

    @Query("SELECT * FROM services WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE")
    abstract fun search(query: String): Flow<List<Service>>

    @Query("SELECT * FROM services WHERE id = :id")
    abstract suspend fun getById(id: String): Service?

    @Query("SELECT COUNT(*) FROM services")
    abstract suspend fun count(): Int

    @Upsert
    abstract suspend fun upsertAll(services: List<Service>)

    @Query("DELETE FROM services WHERE id NOT IN (:keepIds)")
    abstract suspend fun deleteAllExcept(keepIds: List<String>)

    /**
     * Zamjenjuje katalog novom verzijom. Pretplate nisu vezane stranim ključem
     * na servise, pa uklanjanje servisa iz kataloga ne dira korisnikove podatke.
     */
    @Transaction
    open suspend fun replaceAll(services: List<Service>) {
        deleteAllExcept(services.map { it.id })
        upsertAll(services)
    }
}
