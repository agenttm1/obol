package com.tmstudio.obol.ui.components

import androidx.annotation.DrawableRes
import com.tmstudio.obol.R

/**
 * Logotipi servisa iz kataloga, po `serviceId`. Izvor je Simple Icons 16.34.0
 * (CC0); znakovi su žigovi svojih vlasnika i služe samo za prepoznavanje servisa.
 *
 * Servisi kojih ovdje nema prikazuju monogram: Disney+, Xbox, Nintendo,
 * Microsoft, Canva i Adobe uklonjeni su iz Simple Icons na zahtjev vlasnika,
 * pa se ne uzimaju ni iz starijih verzija. Ručno unesene pretplate nemaju
 * `serviceId` i uvijek imaju monogram.
 *
 * Logotipi su ugrađeni u aplikaciju — za njih ne treba internet (spec, poglavlje 9).
 */
object ServiceLogos {

    private val logos: Map<String, Int> = mapOf(
        "netflix" to R.drawable.logo_netflix,
        "hbo-max" to R.drawable.logo_hbo_max,
        "youtube-premium" to R.drawable.logo_youtube_premium,
        "apple-tv-plus" to R.drawable.logo_apple_tv_plus,
        "spotify" to R.drawable.logo_spotify,
        "apple-music" to R.drawable.logo_apple_music,
        "playstation-plus" to R.drawable.logo_playstation_plus,
        "icloud-plus" to R.drawable.logo_icloud_plus,
        // Google One i Microsoft 365 nemaju vlastiti znak u Simple Icons; Google ga ima kao „G".
        "google-one" to R.drawable.logo_google_one,
        "duolingo" to R.drawable.logo_duolingo,
        "strava" to R.drawable.logo_strava,
    )

    val serviceIds: Set<String> get() = logos.keys

    @DrawableRes
    fun forService(serviceId: String?): Int? = serviceId?.let(logos::get)
}
