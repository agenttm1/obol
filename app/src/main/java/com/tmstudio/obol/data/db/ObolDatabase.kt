package com.tmstudio.obol.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tmstudio.obol.data.db.dao.PriceChangeDao
import com.tmstudio.obol.data.db.dao.ServiceDao
import com.tmstudio.obol.data.db.dao.SubscriptionDao
import com.tmstudio.obol.data.db.dao.UsageCheckDao
import com.tmstudio.obol.data.db.entity.PriceChange
import com.tmstudio.obol.data.db.entity.PromoPeriod
import com.tmstudio.obol.data.db.entity.Service
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.TrialPeriod
import com.tmstudio.obol.data.db.entity.UsageCheck

/**
 * Sheme se izvoze u `app/schemas/` i idu u git. Svaka promjena entiteta znači
 * novu verziju i migraciju (AutoMigration ili ručnu) — nikad destruktivni
 * fallback, jer su ovo korisnikovi jedini podaci.
 */
@Database(
    entities = [
        Service::class,
        Subscription::class,
        TrialPeriod::class,
        PromoPeriod::class,
        PriceChange::class,
        UsageCheck::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ObolDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun priceChangeDao(): PriceChangeDao
    abstract fun usageCheckDao(): UsageCheckDao

    companion object {
        private const val NAME = "obol.db"

        fun build(context: Context): ObolDatabase =
            Room.databaseBuilder(context.applicationContext, ObolDatabase::class.java, NAME)
                .build()
    }
}
