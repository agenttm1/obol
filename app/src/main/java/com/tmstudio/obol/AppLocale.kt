package com.tmstudio.obol

import android.content.Context
import android.content.res.Configuration
import com.tmstudio.obol.ui.format.ObolFormat

/**
 * Sučelje je u v1 samo na hrvatskom (spec, poglavlje 1). Tekstovi su u
 * zadanom `values/`, ali Android pravila množine (`plurals`) i nazive mjeseci
 * u biraču datuma uzima iz jezika uređaja — na engleskom telefonu „3" bi
 * pao u kategoriju „other" i dao „3 mjeseci". Zato se kontekst izričito
 * prebacuje na hr-HR.
 *
 * Isto treba primijeniti na svaki kontekst koji slaže tekstove izvan
 * aktivnosti, npr. pri slaganju obavijesti u WorkManageru.
 */
fun Context.withAppLocale(): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(ObolFormat.locale)
    return createConfigurationContext(configuration)
}
