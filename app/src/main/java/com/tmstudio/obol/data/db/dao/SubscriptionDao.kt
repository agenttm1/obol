package com.tmstudio.obol.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.tmstudio.obol.data.db.entity.PriceChange
import com.tmstudio.obol.data.db.entity.PromoPeriod
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import com.tmstudio.obol.data.db.entity.TrialPeriod
import kotlinx.coroutines.flow.Flow

/** Pretplate zajedno s probnim periodom i promocijom, koji žive i umiru s njom. */
@Dao
abstract class SubscriptionDao {

    @Transaction
    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE")
    abstract fun observeAllWithDetails(): Flow<List<SubscriptionWithDetails>>

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY name COLLATE NOCASE")
    abstract fun observeActiveWithDetails(): Flow<List<SubscriptionWithDetails>>

    /** Za pozadinski worker, koji ne treba Flow. */
    @Transaction
    @Query("SELECT * FROM subscriptions WHERE isActive = 1")
    abstract suspend fun getActiveWithDetails(): List<SubscriptionWithDetails>

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    abstract fun observeWithDetails(id: Long): Flow<SubscriptionWithDetails?>

    @Query("SELECT COUNT(*) FROM subscriptions")
    abstract suspend fun count(): Int

    @Insert
    abstract suspend fun insert(subscription: Subscription): Long

    @Update
    abstract suspend fun update(subscription: Subscription)

    /** Kaskadno briše i probni period, promociju, povijest cijena i provjere korištenja. */
    @Delete
    abstract suspend fun delete(subscription: Subscription)

    @Upsert
    abstract suspend fun upsertTrial(trial: TrialPeriod)

    @Query("DELETE FROM trial_periods WHERE subscriptionId = :subscriptionId")
    abstract suspend fun deleteTrial(subscriptionId: Long)

    @Upsert
    abstract suspend fun upsertPromo(promo: PromoPeriod)

    @Query("DELETE FROM promo_periods WHERE subscriptionId = :subscriptionId")
    abstract suspend fun deletePromo(subscriptionId: Long)

    /**
     * Sprema novu pretplatu s opcionalnim probnim periodom i promocijom u jednoj
     * transakciji. `subscriptionId` u [trial] i [promo] se zanemaruje i
     * postavlja na id nove pretplate.
     */
    @Transaction
    open suspend fun insertWithPeriods(
        subscription: Subscription,
        trial: TrialPeriod?,
        promo: PromoPeriod?,
    ): Long {
        val id = insert(subscription)
        trial?.let { upsertTrial(it.copy(subscriptionId = id)) }
        promo?.let { upsertPromo(it.copy(subscriptionId = id)) }
        return id
    }

    @Insert
    abstract suspend fun insertPriceChange(change: PriceChange): Long

    /**
     * Ažurira pretplatu; null za [trial] ili [promo] briše postojeći zapis.
     * [priceChange] se, ako postoji, upisuje u istoj transakciji.
     */
    @Transaction
    open suspend fun updateWithPeriods(
        subscription: Subscription,
        trial: TrialPeriod?,
        promo: PromoPeriod?,
        priceChange: PriceChange? = null,
    ) {
        update(subscription)
        priceChange?.let { insertPriceChange(it.copy(subscriptionId = subscription.id)) }
        if (trial != null) {
            upsertTrial(trial.copy(subscriptionId = subscription.id))
        } else {
            deleteTrial(subscription.id)
        }
        if (promo != null) {
            upsertPromo(promo.copy(subscriptionId = subscription.id))
        } else {
            deletePromo(subscription.id)
        }
    }
}
