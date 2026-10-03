package com.tmstudio.obol.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

/** Pretplata sa svime što treba za izračun cijene na neki datum. */
data class SubscriptionWithDetails(
    @Embedded val subscription: Subscription,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val trial: TrialPeriod?,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val promo: PromoPeriod?,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val priceChanges: List<PriceChange>,
)
