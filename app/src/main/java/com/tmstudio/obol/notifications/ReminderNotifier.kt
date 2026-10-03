package com.tmstudio.obol.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.tmstudio.obol.MainActivity
import com.tmstudio.obol.R
import com.tmstudio.obol.domain.Reminder
import com.tmstudio.obol.ui.format.ObolFormat
import com.tmstudio.obol.withAppLocale
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Pretvara podsjetnike u obavijesti. Tekstovi su iz `strings.xml`, uvijek na hrvatskom. */
class ReminderNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /** @return broj prikazanih obavijesti; 0 kad korisnik nije dopustio obavijesti */
    fun show(reminders: List<Reminder>, today: LocalDate): Int {
        // Na Androidu 13+ ovo je false dok korisnik ne odobri POST_NOTIFICATIONS.
        if (!manager.areNotificationsEnabled()) return 0
        val localized = context.withAppLocale()
        reminders.forEach { reminder ->
            val content = content(localized, reminder, today)
            val notification = NotificationCompat.Builder(context, content.channel)
                .setSmallIcon(R.drawable.ic_stat_obol)
                .setColor(ContextCompat.getColor(context, R.color.obol_accent))
                .setContentTitle(content.title)
                .setContentText(content.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content.text))
                .setContentIntent(openSubscription(reminder.subscription.id, content.id))
                .setAutoCancel(true)
                .build()
            try {
                manager.notify(content.id, notification)
            } catch (e: SecurityException) {
                // Dozvola je opozvana između provjere i slanja; ništa se ne može.
                return 0
            }
        }
        return reminders.size
    }

    private data class Content(val id: Int, val channel: String, val title: String, val text: String)

    private fun content(context: Context, reminder: Reminder, today: LocalDate): Content {
        val sub = reminder.subscription
        val name = sub.name
        return when (reminder) {
            is Reminder.Billing -> Content(
                id = notificationId("billing", sub.id, reminder.date),
                channel = NotificationChannels.BILLING,
                title = context.getString(R.string.notification_billing_title),
                text = context.getString(
                    R.string.notification_billing_text,
                    name,
                    relativeDays(context, today, reminder.date),
                    ObolFormat.money(reminder.priceCents, sub.currency),
                ),
            )

            is Reminder.TrialEnding -> Content(
                id = notificationId("trial", sub.id, reminder.endsOn),
                channel = NotificationChannels.TRIALS,
                title = context.getString(R.string.notification_trial_title),
                text = context.getString(
                    R.string.notification_trial_text,
                    name,
                    relativeDays(context, today, reminder.endsOn),
                    ObolFormat.money(reminder.firstChargeCents, sub.currency),
                ),
            )

            is Reminder.PromoEnding -> Content(
                id = notificationId("promo", sub.id, reminder.fullPriceFrom),
                channel = NotificationChannels.TRIALS,
                title = context.getString(R.string.notification_promo_title),
                text = context.getString(
                    R.string.notification_promo_text,
                    name,
                    ObolFormat.dateLong(reminder.fullPriceFrom),
                    ObolFormat.amount(reminder.promoPriceCents),
                    ObolFormat.money(reminder.fullPriceCents, sub.currency),
                ),
            )

            is Reminder.UsageCheck -> Content(
                id = notificationId("usage", sub.id, reminder.period.atDay(1)),
                channel = NotificationChannels.CHECKS,
                title = context.getString(R.string.notification_usage_title),
                text = context.getString(R.string.notification_usage_text, name),
            )
        }
    }

    private fun relativeDays(context: Context, today: LocalDate, date: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, date).toInt()
        return when (days) {
            0 -> context.getString(R.string.relative_today)
            1 -> context.getString(R.string.relative_tomorrow)
            else -> context.resources.getQuantityString(R.plurals.relative_in_days, days, days)
        }
    }

    /** Dodir otvara Detalje pretplate preko deep linka, s Pregledom ispod na stogu. */
    private fun openSubscription(subscriptionId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, "$DEEP_LINK_BASE/$subscriptionId".toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        /** Mora odgovarati deep linku rute Detalji u `ObolApp`. */
        const val DEEP_LINK_BASE = "obol://subscription"

        /**
         * Stalan id po vrsti, pretplati i datumu: ponovljeno slanje istog
         * podsjetnika zamijeni postojeću obavijest umjesto da doda novu.
         */
        fun notificationId(type: String, subscriptionId: Long, date: LocalDate): Int =
            "$type:$subscriptionId:$date".hashCode()
    }
}
