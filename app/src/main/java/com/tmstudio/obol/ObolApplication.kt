package com.tmstudio.obol

import android.app.Application
import android.util.Log
import androidx.work.ExistingWorkPolicy
import com.tmstudio.obol.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ObolApplication : Application() {

    lateinit var container: AppContainer
        private set

    /** Za posao koji mora završiti neovisno o životu pojedinog ekrana. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.createAll(this)
        applicationScope.launch {
            // Neispravan katalog ne smije srušiti aplikaciju — korisnik i dalje
            // može dodati pretplatu ručno. Ispravnost datoteke čuvaju unit testovi.
            runCatching { container.catalogLoader.syncIfNeeded() }
                .onFailure { Log.e(TAG, "Učitavanje kataloga servisa nije uspjelo", it) }
        }
        applicationScope.launch {
            // KEEP: ako je lanac podsjetnika već zakazan, ne dira se; ako je pukao, obnavlja se.
            container.reminderScheduler.scheduleNext(ExistingWorkPolicy.KEEP)
        }
    }

    private companion object {
        const val TAG = "ObolApplication"
    }
}
