package com.tmstudio.obol.notifications

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.tmstudio.obol.ObolApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Lanac dnevnog workera: posao je zakazan, izvrši se i zakaže sljedeći. */
@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val container = (context as ObolApplication).container

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .build(),
        )
    }

    private fun work(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME).get()

    @Test
    fun schedulesOneDelayedRun_andKeepDoesNotDuplicate() = runBlocking {
        container.reminderScheduler.scheduleNext(ExistingWorkPolicy.KEEP)
        container.reminderScheduler.scheduleNext(ExistingWorkPolicy.KEEP)

        val infos = work()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        assertTrue(infos.single().initialDelayMillis > 0)
    }

    @Test
    fun runningWorker_schedulesTheNextDay() = runBlocking {
        container.reminderScheduler.scheduleNext(ExistingWorkPolicy.KEEP)
        val first = work().single()

        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(first.id)
        // CoroutineWorker radi na vlastitom dispatcheru, pa SynchronousExecutor ne čeka njegov kraj.
        withTimeout(WORKER_TIMEOUT_MS) {
            while (!work().first { it.id == first.id }.state.isFinished) delay(POLL_MS)
        }

        val infos = work()
        assertEquals(WorkInfo.State.SUCCEEDED, infos.first { it.id == first.id }.state)
        val next = infos.filter { it.id != first.id }
        assertEquals(1, next.size)
        assertEquals(WorkInfo.State.ENQUEUED, next.single().state)
    }

    private companion object {
        const val WORKER_TIMEOUT_MS = 10_000L
        const val POLL_MS = 50L
    }
}
