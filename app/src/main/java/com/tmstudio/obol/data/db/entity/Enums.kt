package com.tmstudio.obol.data.db.entity

// Room enume sprema po imenu (TEXT), pa se konstante ne smiju preimenovati bez migracije.

enum class Category {
    ENTERTAINMENT,
    MUSIC,
    GAMING,
    STORAGE,
    TOOLS,
    EDUCATION,
    FITNESS,
    NEWS,
    OTHER,
}

enum class BillingCycle {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    SEMIANNUAL,
    YEARLY,

    /** Duljina ciklusa u danima je u [Subscription.cycleDays]. */
    CUSTOM_DAYS,
}
