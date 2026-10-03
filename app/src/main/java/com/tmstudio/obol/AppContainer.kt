package com.tmstudio.obol

import android.content.Context
import com.tmstudio.obol.data.catalog.CatalogLoader
import com.tmstudio.obol.data.db.ObolDatabase
import com.tmstudio.obol.data.prefs.AppPreferences
import com.tmstudio.obol.data.prefs.obolDataStore
import com.tmstudio.obol.notifications.ReminderNotifier
import com.tmstudio.obol.notifications.ReminderRunner
import com.tmstudio.obol.notifications.ReminderScheduler
import java.time.Clock

/** Ručni DI: jedna instanca svega što živi koliko i aplikacija. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()

    val database: ObolDatabase by lazy { ObolDatabase.build(appContext) }

    val preferences: AppPreferences by lazy { AppPreferences(appContext.obolDataStore) }

    val catalogLoader: CatalogLoader by lazy {
        CatalogLoader(
            serviceDao = database.serviceDao(),
            versionStore = preferences,
            readCatalogJson = {
                appContext.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
            },
        )
    }

    val reminderRunner: ReminderRunner by lazy {
        ReminderRunner(database, preferences, ReminderNotifier(appContext), clock)
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext, preferences, clock)
    }

    private companion object {
        const val CATALOG_ASSET = "services.json"
    }
}
