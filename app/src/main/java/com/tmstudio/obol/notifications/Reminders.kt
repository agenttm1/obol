package com.tmstudio.obol.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tmstudio.obol.ObolApplication
import com.tmstudio.obol.data.db.ObolDatabase
import com.tmstudio.obol.data.prefs.AppPreferences
import com.tmstudio.obol.domain.Reminder
import com.tmstudio.obol.domain.nextReminderRun
import com.tmstudio.obol.domain.remindersFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Jedan dnevni prolaz: što danas treba javiti, i slanje obavijesti. */
class ReminderRunner(
    private val database: ObolDatabase,
    private val preferences: AppPreferences,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {
    /**
     * Šalje današnje podsjetnike, osim ako su već poslani danas.
     * @return poslani podsjetnici
     */
    suspend fun runForToday(): List<Reminder> {
        val today = LocalDate.now(clock)
        if (preferences.lastReminderRun() == today) return emptyList()

        val settings = preferences.reminderSettings.first()
        val subscriptions = database.subscriptionDao().getActiveWithDetails()
        val answered = database.usageCheckDao()
            .getForPeriod(YearMonth.from(today).minusMonths(1))
            .map { it.subscriptionId }
            .toSet()
        val reminders = remindersFor(subscriptions, today, settings, answered)
        notifier.show(reminders, today)
        preferences.setLastReminderRun(today)
        return reminders
    }
}

/**
 * Zakazuje dnevni worker kao lanac jednokratnih poslova: svaki prolaz zakaže
 * sljedeći za točno vrijeme iz postavki. Periodični posao od 24 h s vremenom
 * odluta i ne prati promjenu vremena ni ljetno računanje vremena.
 */
class ReminderScheduler(
    private val context: Context,
    private val preferences: AppPreferences,
    private val clock: Clock,
) {
    suspend fun scheduleNext(policy: ExistingWorkPolicy) {
        val time = preferences.reminderSettings.first().time
        val now = ZonedDateTime.now(clock)
        val next = nextReminderRun(now.toLocalDateTime(), time).atZone(now.zone)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    companion object {
        const val WORK_NAME = "daily_reminders"
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ObolApplication).container
        return try {
            container.reminderRunner.runForToday()
            Result.success()
        } catch (e: Exception) {
            // Neuspjeh jednog dana ne smije prekinuti lanac; sutra se pokušava ponovno.
            Log.e(TAG, "Slanje podsjetnika nije uspjelo", e)
            Result.success()
        } finally {
            // APPEND jer ovaj posao još radi: REPLACE bi ga otkazao usred slanja.
            container.reminderScheduler.scheduleNext(ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    private companion object {
        const val TAG = "ReminderWorker"
    }
}

/**
 * Ponovno zakazuje dnevni worker nakon restarta uređaja (spec, poglavlje 5)
 * i nakon promjene vremena ili vremenske zone, jer je odgoda izračunata unaprijed.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val app = context.applicationContext as ObolApplication
        val pending = goAsync()
        app.applicationScope.launch {
            try {
                app.container.reminderScheduler.scheduleNext(ExistingWorkPolicy.REPLACE)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
