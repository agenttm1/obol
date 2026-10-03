package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.SubscriptionWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class RemindersTest {

    private val settings = ReminderSettings()

    private fun remind(
        vararg subs: SubscriptionWithDetails,
        today: LocalDate,
        s: ReminderSettings = settings,
        answered: Set<Long> = emptySet(),
    ) =
        remindersFor(subs.toList(), today, s, answered)

    @Test
    fun billing_exactlyNDaysBefore() {
        val sub = subscription(firstBillingDate = date(2026, 1, 4))
        // 1. u mjesecu stiže i provjera korištenja, zato filtriramo.
        val reminder = remind(sub, today = date(2026, 10, 1)).filterIsInstance<Reminder.Billing>().single()
        assertEquals(date(2026, 10, 4), reminder.date)
        assertEquals(3, reminder.daysBefore)
        assertEquals(1399, reminder.priceCents)
        assertTrue(remind(sub, today = date(2026, 10, 2)).isEmpty())
        assertTrue(remind(sub, today = date(2026, 9, 30)).isEmpty())
    }

    @Test
    fun billing_respectsConfiguredDays_andZeroDisables() {
        val sub = subscription(firstBillingDate = date(2026, 1, 4))
        assertEquals(1, remind(sub, today = date(2026, 10, 3), s = settings.copy(billingDaysBefore = 1)).size)
        assertTrue(
            remind(sub, today = date(2026, 10, 1), s = settings.copy(billingDaysBefore = 0))
                .none { it is Reminder.Billing }
        )
    }

    @Test
    fun billing_usesPriceOnChargeDay() {
        val sub = subscription(
            basePriceCents = 1499,
            firstBillingDate = date(2026, 1, 4),
            priceChanges = listOf(priceChange(date(2026, 10, 4), 1399, 1499)),
        )
        assertEquals(1499, remind(sub, today = date(2026, 10, 1)).filterIsInstance<Reminder.Billing>().single().priceCents)
    }

    @Test
    fun billing_31stInShortMonth() {
        val sub = subscription(firstBillingDate = date(2026, 1, 31))
        val reminder = remind(sub, today = date(2026, 11, 27)).single() as Reminder.Billing
        assertEquals(date(2026, 11, 30), reminder.date)
    }

    @Test
    fun trialEnding_twoDaysBeforeLastDay_replacesFirstBillingReminder() {
        val sub = subscription(
            basePriceCents = 999,
            firstBillingDate = date(2026, 10, 6),
            trial = trial(endsOn = date(2026, 10, 5)),
        )
        // 3. 10.: kraj probnog perioda za 2 dana, prva naplata (6. 10.) za 3 dana.
        val reminder = remind(sub, today = date(2026, 10, 3)).single() as Reminder.TrialEnding
        assertEquals(date(2026, 10, 5), reminder.endsOn)
        assertEquals(2, reminder.daysBefore)
        assertEquals(999, reminder.firstChargeCents)
        // Druga naplata (6. 11.) dobiva običan podsjetnik.
        assertTrue(remind(sub, today = date(2026, 11, 3)).single() is Reminder.Billing)
    }

    @Test
    fun trialEnding_firstChargeIsPromoPrice() {
        val sub = subscription(
            basePriceCents = 999,
            firstBillingDate = date(2026, 10, 6),
            trial = trial(endsOn = date(2026, 10, 5)),
            promo = promo(299, startsOn = date(2026, 10, 6), endsOn = date(2027, 1, 5)),
        )
        assertEquals(299, (remind(sub, today = date(2026, 10, 3)).single() as Reminder.TrialEnding).firstChargeCents)
    }

    @Test
    fun promoEnding_threeDaysBeforeFirstFullCharge_replacesBillingReminder() {
        val sub = subscription(
            basePriceCents = 1099,
            firstBillingDate = date(2026, 9, 1),
            promo = promo(599, startsOn = date(2026, 9, 1), endsOn = date(2026, 11, 30)),
        )
        val reminder = remind(sub, today = date(2026, 11, 28)).single() as Reminder.PromoEnding
        assertEquals(date(2026, 12, 1), reminder.fullPriceFrom)
        assertEquals(599, reminder.promoPriceCents)
        assertEquals(1099, reminder.fullPriceCents)
    }

    @Test
    fun promoEnding_disabledFallsBackToBillingReminder() {
        val sub = subscription(
            basePriceCents = 1099,
            firstBillingDate = date(2026, 9, 1),
            promo = promo(599, startsOn = date(2026, 9, 1), endsOn = date(2026, 11, 30)),
        )
        val reminder = remind(
            sub,
            today = date(2026, 11, 28),
            s = settings.copy(priceIncreaseAlertsEnabled = false),
        ).single() as Reminder.Billing
        assertEquals(1099, reminder.priceCents)
    }

    @Test
    fun billingDuringPromo_hasPromoPrice() {
        val sub = subscription(
            basePriceCents = 1099,
            firstBillingDate = date(2026, 9, 1),
            promo = promo(599, startsOn = date(2026, 9, 1), endsOn = date(2026, 11, 30)),
        )
        assertEquals(599, (remind(sub, today = date(2026, 10, 29)).single() as Reminder.Billing).priceCents)
    }

    @Test
    fun usageCheck_onFirstOfMonth_onlyUnanswered_andWhenEnabled() {
        val netflix = subscription(id = 1, name = "Netflix", firstBillingDate = date(2026, 1, 15))
        val spotify = subscription(id = 2, name = "Spotify", firstBillingDate = date(2026, 1, 20))
        val today = date(2026, 10, 1)
        val usage = remind(netflix, spotify, today = today, answered = setOf(2L)).filterIsInstance<Reminder.UsageCheck>()
        assertEquals(listOf("Netflix"), usage.map { it.subscription.name })
        assertEquals(YearMonth.of(2026, 9), usage.single().period)

        assertTrue(remind(netflix, today = date(2026, 10, 2)).none { it is Reminder.UsageCheck })
        assertTrue(
            remind(netflix, today = today, s = settings.copy(usageChecksEnabled = false))
                .none { it is Reminder.UsageCheck }
        )
    }

    @Test
    fun usageCheck_notForSubscriptionStartedThisMonth() {
        val sub = subscription(firstBillingDate = date(2026, 10, 1))
        assertTrue(remind(sub, today = date(2026, 10, 1)).none { it is Reminder.UsageCheck })
    }

    @Test
    fun inactiveSubscriptions_getNothing() {
        val sub = subscription(isActive = false, firstBillingDate = date(2026, 1, 4))
        assertTrue(remind(sub, today = date(2026, 10, 1)).isEmpty())
    }

    @Test
    fun weekly_remindsBeforeEachCharge() {
        val sub = subscription(cycle = BillingCycle.WEEKLY, basePriceCents = 500, firstBillingDate = date(2026, 9, 28))
        assertTrue(remind(sub, today = date(2026, 10, 2)).single() is Reminder.Billing)
        assertTrue(remind(sub, today = date(2026, 10, 9)).single() is Reminder.Billing)
        assertTrue(remind(sub, today = date(2026, 10, 3)).isEmpty())
    }

    @Test
    fun nextReminderRun_todayIfNotYetPassed_elseTomorrow() {
        val nine = LocalTime.of(9, 0)
        assertEquals(
            LocalDateTime.of(2026, 10, 3, 9, 0),
            nextReminderRun(LocalDateTime.of(2026, 10, 3, 8, 59), nine),
        )
        assertEquals(
            LocalDateTime.of(2026, 10, 4, 9, 0),
            nextReminderRun(LocalDateTime.of(2026, 10, 3, 9, 0), nine),
        )
        assertEquals(
            LocalDateTime.of(2027, 1, 1, 9, 0),
            nextReminderRun(LocalDateTime.of(2026, 12, 31, 21, 0), nine),
        )
    }
}
