package com.tmstudio.obol.domain

/** Najveći iznos koji se može upisati: 999.999,99 €. */
private val MONEY_INPUT = Regex("""^(\d{1,6})(?:[.,](\d{0,2}))?$""")

/**
 * Pretvara iznos koji je korisnik upisao („9,99", „9.9", „10", „10 €") u cente.
 * Bez Double — znamenke se slažu izravno. Vraća null za neispravan unos.
 */
fun parseMoneyInput(text: String): Int? {
    val cleaned = text.replace("€", "").filterNot { it.isWhitespace() }
    val match = MONEY_INPUT.matchEntire(cleaned) ?: return null
    val whole = match.groupValues[1].toInt()
    val fraction = match.groupValues[2].padEnd(2, '0').toInt()
    return whole * 100 + fraction
}

/** Iznos u obliku za uređivanje u polju: „9,99". */
fun formatMoneyInput(cents: Int): String =
    String.format(java.util.Locale.ROOT, "%d,%02d", cents / 100, cents % 100)
