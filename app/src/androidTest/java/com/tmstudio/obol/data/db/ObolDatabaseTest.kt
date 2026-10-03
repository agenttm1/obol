package com.tmstudio.obol.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.CatalogPlan
import com.tmstudio.obol.data.db.entity.Category
import com.tmstudio.obol.data.db.entity.PriceChange
import com.tmstudio.obol.data.db.entity.PromoPeriod
import com.tmstudio.obol.data.db.entity.Service
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.TrialPeriod
import com.tmstudio.obol.data.db.entity.UsageCheck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class ObolDatabaseTest {

    private lateinit var db: ObolDatabase
    private val now = Instant.parse("2026-10-03T10:00:00Z")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ObolDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() = db.close()

    private fun netflix() = Subscription(
        serviceId = "netflix",
        name = "Netflix",
        monogram = "N",
        colorHex = "#FF8168",
        category = Category.ENTERTAINMENT,
        cycle = BillingCycle.MONTHLY,
        basePriceCents = 1399,
        firstBillingDate = LocalDate.of(2026, 1, 31),
        planLabel = "Premium",
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun insertWithPeriods_isReadBackWithDetails() = runBlocking {
        val dao = db.subscriptionDao()
        val id = dao.insertWithPeriods(
            netflix(),
            trial = TrialPeriod(subscriptionId = 0, endsOn = LocalDate.of(2026, 10, 31)),
            promo = PromoPeriod(
                subscriptionId = 0,
                priceCents = 599,
                startsOn = LocalDate.of(2026, 11, 1),
                endsOn = LocalDate.of(2027, 1, 31),
            ),
        )

        val loaded = dao.observeWithDetails(id).first()!!
        assertEquals(LocalDate.of(2026, 1, 31), loaded.subscription.firstBillingDate)
        assertEquals(1399, loaded.subscription.basePriceCents)
        assertEquals("EUR", loaded.subscription.currency)
        assertEquals(now, loaded.subscription.createdAt)
        assertEquals(id, loaded.trial!!.subscriptionId)
        assertEquals(0, loaded.trial!!.priceCents)
        assertEquals(LocalDate.of(2027, 1, 31), loaded.promo!!.endsOn)
        assertTrue(loaded.priceChanges.isEmpty())
    }

    @Test
    fun updateWithPeriods_nullRemovesPeriod() = runBlocking {
        val dao = db.subscriptionDao()
        val id = dao.insertWithPeriods(
            netflix(),
            trial = TrialPeriod(subscriptionId = 0, endsOn = LocalDate.of(2026, 10, 31)),
            promo = null,
        )
        val sub = dao.observeWithDetails(id).first()!!.subscription

        dao.updateWithPeriods(sub.copy(basePriceCents = 1499), trial = null, promo = null)

        val loaded = dao.observeWithDetails(id).first()!!
        assertEquals(1499, loaded.subscription.basePriceCents)
        assertNull(loaded.trial)
        assertNull(loaded.promo)
    }

    @Test
    fun deletingSubscription_cascadesToChildren() = runBlocking {
        val dao = db.subscriptionDao()
        val id = dao.insertWithPeriods(
            netflix(),
            trial = TrialPeriod(subscriptionId = 0, endsOn = LocalDate.of(2026, 10, 31)),
            promo = null,
        )
        db.priceChangeDao().insert(
            PriceChange(
                subscriptionId = id,
                effectiveFrom = LocalDate.of(2026, 6, 1),
                oldPriceCents = 1299,
                newPriceCents = 1399,
                recordedAt = now,
            )
        )
        db.usageCheckDao().upsert(UsageCheck(id, YearMonth.of(2026, 9), used = true, answeredAt = now))
        val sub = dao.observeWithDetails(id).first()!!.subscription

        dao.delete(sub)

        assertNull(dao.observeWithDetails(id).first())
        assertTrue(db.priceChangeDao().observeFor(id).first().isEmpty())
        assertTrue(db.usageCheckDao().observeFor(id).first().isEmpty())
    }

    @Test
    fun usageCheck_sameMonthIsReplaced() = runBlocking {
        val id = db.subscriptionDao().insert(netflix())
        val usage = db.usageCheckDao()
        val period = YearMonth.of(2026, 9)

        usage.upsert(UsageCheck(id, period, used = true, answeredAt = now))
        usage.upsert(UsageCheck(id, period, used = false, answeredAt = now.plusSeconds(60)))

        assertEquals(false, usage.get(id, period)!!.used)
        assertEquals(1, usage.observeFor(id).first().size)
    }

    @Test
    fun service_plansRoundTrip() = runBlocking {
        val service = Service(
            id = "netflix",
            name = "Netflix",
            category = Category.ENTERTAINMENT,
            colorHex = "#FF8168",
            monogram = "N",
            plans = listOf(CatalogPlan("Standard", 1099), CatalogPlan("Premium", 1399, "4K + HDR")),
        )
        db.serviceDao().upsertAll(listOf(service))

        assertEquals(service, db.serviceDao().getById("netflix"))
        assertEquals(listOf(service), db.serviceDao().search("flix").first())
    }

    @Test
    fun serviceReplaceAll_dropsRemovedServices_andKeepsSubscriptions() = runBlocking {
        fun service(id: String, price: Int) = Service(
            id = id,
            name = id,
            category = Category.OTHER,
            colorHex = "#FFFFFF",
            monogram = "X",
            plans = listOf(CatalogPlan("Plan", price, cycle = BillingCycle.YEARLY)),
        )
        val services = db.serviceDao()
        services.replaceAll(listOf(service("netflix", 1399), service("removed", 100)))
        val subId = db.subscriptionDao().insert(netflix().copy(serviceId = "removed"))

        services.replaceAll(listOf(service("netflix", 1499), service("spotify", 699)))

        assertEquals(listOf("netflix", "spotify"), services.observeAll().first().map { it.id })
        assertEquals(1499, services.getById("netflix")!!.plans.single().priceCents)
        assertEquals(BillingCycle.YEARLY, services.getById("netflix")!!.plans.single().cycle)
        assertEquals("removed", db.subscriptionDao().observeWithDetails(subId).first()!!.subscription.serviceId)
    }
}
