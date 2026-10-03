package com.tmstudio.obol.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.tmstudio.obol.data.db.ObolDatabase
import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.Category
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.data.db.entity.TrialPeriod
import com.tmstudio.obol.data.prefs.AppPreferences
import com.tmstudio.obol.domain.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReminderRunnerTest {

    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Europe/Zagreb")
    // 1. 10. 2026. u 9:00 — prvi u mjesecu, pa stiže i provjera korištenja.
    private val clock = Clock.fixed(Instant.parse("2026-10-01T07:00:00Z"), zone)
    private val today = LocalDate.of(2026, 10, 1)

    private lateinit var db: ObolDatabase
    private lateinit var preferences: AppPreferences
    private lateinit var scope: CoroutineScope
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        NotificationChannels.createAll(context)
        manager.cancelAll()
        db = Room.inMemoryDatabaseBuilder(context, ObolDatabase::class.java).build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        preferences = AppPreferences(
            PreferenceDataStoreFactory.create(scope = scope) {
                context.preferencesDataStoreFile("test_${UUID.randomUUID()}")
            }
        )
    }

    @After
    fun tearDown() {
        manager.cancelAll()
        db.close()
        scope.cancel()
    }

    private fun sub(name: String, firstBilling: LocalDate, price: Int) = Subscription(
        serviceId = null,
        name = name,
        monogram = name.take(1),
        colorHex = "#FF8168",
        category = Category.ENTERTAINMENT,
        cycle = BillingCycle.MONTHLY,
        basePriceCents = price,
        firstBillingDate = firstBilling,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun runner() = ReminderRunner(db, preferences, ReminderNotifier(context), clock)

    private fun activeTexts(): List<String> = manager.activeNotifications.map {
        it.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            .replace(' ', ' ').replace(' ', ' ')
    }

    @Test
    fun postsBillingTrialAndUsageNotifications_inCroatian() = runBlocking {
        val dao = db.subscriptionDao()
        dao.insert(sub("Netflix", firstBilling = LocalDate.of(2026, 1, 4), price = 1399))
        dao.insertWithPeriods(
            sub("Disney+", firstBilling = LocalDate.of(2026, 10, 4), price = 999),
            trial = TrialPeriod(0, endsOn = LocalDate.of(2026, 10, 3)),
            promo = null,
        )

        val sent = runner().runForToday()

        assertEquals(3, sent.size)
        assertTrue(sent.any { it is Reminder.Billing })
        assertTrue(sent.any { it is Reminder.TrialEnding })
        assertTrue(sent.any { it is Reminder.UsageCheck })

        val texts = activeTexts()
        assertTrue(texts.toString(), "Netflix ti se naplaćuje za 3 dana — 13,99 €" in texts)
        assertTrue(texts.toString(), "Disney+ ti za 2 dana prestaje probni period. Prva naplata je 9,99 €." in texts)
        assertTrue(texts.toString(), "Jesi li prošli mjesec koristio Netflix?" in texts)

        val channels = manager.activeNotifications.map { it.notification.channelId }.toSet()
        assertEquals(setOf(NotificationChannels.BILLING, NotificationChannels.TRIALS, NotificationChannels.CHECKS), channels)
    }

    @Test
    fun secondRunOnSameDay_sendsNothing() = runBlocking {
        db.subscriptionDao().insert(sub("Netflix", firstBilling = LocalDate.of(2026, 1, 4), price = 1399))
        val first = runner().runForToday()
        val second = runner().runForToday()
        assertTrue(first.isNotEmpty())
        assertTrue(second.isEmpty())
        assertEquals(today, preferences.lastReminderRun())
    }

    @Test
    fun disabledUsageChecks_areNotSent() = runBlocking {
        db.subscriptionDao().insert(sub("Netflix", firstBilling = LocalDate.of(2026, 1, 20), price = 1399))
        preferences.updateReminderSettings { it.copy(usageChecksEnabled = false) }
        assertTrue(runner().runForToday().isEmpty())
        assertTrue(manager.activeNotifications.isEmpty())
    }
}
