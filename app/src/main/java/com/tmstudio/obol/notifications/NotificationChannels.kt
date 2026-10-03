package com.tmstudio.obol.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.tmstudio.obol.R
import com.tmstudio.obol.withAppLocale

/** Tri kanala iz specifikacije (poglavlje 5). minSdk 26, pa kanali uvijek postoje. */
object NotificationChannels {
    const val BILLING = "naplate"
    const val TRIALS = "probni_periodi"
    const val CHECKS = "provjere"

    /** Stvara ili osvježava kanale; sigurno je zvati pri svakom pokretanju. */
    fun createAll(context: Context) {
        val localized = context.withAppLocale()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                channel(localized, BILLING, R.string.channel_billing, R.string.channel_billing_description),
                channel(localized, TRIALS, R.string.channel_trials, R.string.channel_trials_description),
                channel(localized, CHECKS, R.string.channel_checks, R.string.channel_checks_description),
            )
        )
    }

    private fun channel(context: Context, id: String, name: Int, description: Int) =
        NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            this.description = context.getString(description)
        }
}
